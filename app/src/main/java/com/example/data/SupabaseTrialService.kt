package com.example.data

import android.os.SystemClock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class TrialStatusResult(
    val success: Boolean,
    val hasTrial: Boolean,
    val userId: String? = null,
    val trialStartedAt: String? = null,
    val trialExpiresAt: String? = null,
    val trialExpired: Boolean = false,
    val isNewlyActivated: Boolean = false,
    val serverEpochMillis: Long? = null,
    val clientAnchorElapsedMillis: Long? = null,
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
    val message: String? = null,
)

class SupabaseTrialService(
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = SupabaseConfig.SUPABASE_URL,
    private val elapsedRealtimeProvider: () -> Long = {
        runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
    },
) {
    private val jsonType = "application/json".toMediaType()

    companion object {
        const val USER_TRIALS_PATH = "/rest/v1/user_trials"
        const val ACTIVATE_TRIAL_RPC_PATH = "/rest/v1/rpc/activate_user_trial"
        const val TRIAL_ROLLOUT_ISO = "2026-11-26T00:00:00Z"
        private const val NEW_ACCOUNT_ELIGIBILITY_WINDOW_MS = 24L * 60L * 60L * 1000L

        fun buildTrialCheckUrl(
            baseUrl: String = SupabaseConfig.SUPABASE_URL,
            userId: String,
        ): String {
            val encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8")
            return baseUrl.trimEnd('/') +
                "$USER_TRIALS_PATH?user_id=eq.$encodedUserId&select=*"
        }

        fun isNewAccountEligibleForFirstTrial(
            isNewSignup: Boolean,
            userCreatedAtIso: String?,
            serverNowMillis: Long = System.currentTimeMillis(),
        ): Boolean {
            if (isNewSignup) return true
            val createdMs = SubscriptionTimeUtils.parseIsoEpochMillis(userCreatedAtIso) ?: return false
            val rolloutMs = SubscriptionTimeUtils.parseIsoEpochMillis(TRIAL_ROLLOUT_ISO) ?: 0L
            if (createdMs < rolloutMs) return false
            return (serverNowMillis - createdMs) in 0L..NEW_ACCOUNT_ELIGIBILITY_WINDOW_MS
        }

        internal fun parseTrialResponse(
            bodyText: String,
            expectedUserId: String,
            serverEpochMillis: Long?,
            clientAnchorElapsedMillis: Long?,
            isNewlyActivated: Boolean = false,
        ): TrialStatusResult {
            val trimmed = bodyText.trim()
            if (trimmed.isBlank() || trimmed == "[]") {
                return TrialStatusResult(
                    success = true,
                    hasTrial = false,
                    userId = expectedUserId.trim(),
                    serverEpochMillis = serverEpochMillis,
                    clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                )
            }
            val obj = when {
                trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0)
                trimmed.startsWith("{") -> JSONObject(trimmed)
                else -> null
            } ?: return TrialStatusResult(
                success = true,
                hasTrial = false,
                userId = expectedUserId.trim(),
                serverEpochMillis = serverEpochMillis,
                clientAnchorElapsedMillis = clientAnchorElapsedMillis,
            )

            val rowUserId = obj.optString("user_id").trim()
            val cleanExpected = expectedUserId.trim()
            if (rowUserId.isNotBlank() && rowUserId != cleanExpected) {
                return TrialStatusResult(
                    success = true,
                    hasTrial = false,
                    userId = cleanExpected,
                    serverEpochMillis = serverEpochMillis,
                    clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                )
            }

            val startedAt = obj.optString("trial_started_at")
                .trim()
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: obj.optString("trial_start_at")
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val expiresAt = obj.optString("trial_expires_at")
                .trim()
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            if (startedAt == null || expiresAt == null) {
                return TrialStatusResult(
                    success = true,
                    hasTrial = false,
                    userId = cleanExpected,
                    serverEpochMillis = serverEpochMillis,
                    clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                )
            }

            val nowMs = serverEpochMillis ?: System.currentTimeMillis()
            val expiresMs = SubscriptionTimeUtils.parseIsoEpochMillis(expiresAt)
            val expired = expiresMs != null && nowMs >= expiresMs

            return TrialStatusResult(
                success = true,
                hasTrial = true,
                userId = rowUserId.ifBlank { cleanExpected },
                trialStartedAt = startedAt,
                trialExpiresAt = expiresAt,
                trialExpired = expired,
                isNewlyActivated = isNewlyActivated,
                serverEpochMillis = serverEpochMillis,
                clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                message = if (expired) {
                    SubscriptionTimeUtils.TRIAL_EXPIRED_USER_MESSAGE
                } else {
                    "2-Day Free Trial Active"
                },
            )
        }
    }

    fun fetchOrActivateTrial(
        accessToken: String,
        userId: String,
        isNewSignup: Boolean = false,
        userCreatedAt: String? = null,
    ): TrialStatusResult {
        val cleanToken = accessToken.trim()
        val cleanUserId = userId.trim()
        if (cleanToken.isBlank() || cleanUserId.isBlank()) {
            return TrialStatusResult(
                success = false,
                hasTrial = false,
                isUnauthorized = true,
                message = "Please login to check your free trial status.",
            )
        }

        val jwtSub = SupabasePaymentService.extractJwtClaims(cleanToken)?.userId
        if (!jwtSub.isNullOrBlank() && jwtSub != cleanUserId) {
            return TrialStatusResult(
                success = false,
                hasTrial = false,
                isUnauthorized = true,
                message = "Session mismatch. Please login again.",
            )
        }

        val existing = fetchTrialRow(cleanToken, cleanUserId)
        if (!existing.success || existing.hasTrial) {
            return existing
        }

        val serverNow = existing.serverEpochMillis ?: System.currentTimeMillis()
        val eligible = isNewAccountEligibleForFirstTrial(
            isNewSignup = isNewSignup,
            userCreatedAtIso = userCreatedAt,
            serverNowMillis = serverNow,
        )
        if (!eligible) {
            return existing
        }

        return activateTrialOnServer(cleanToken, cleanUserId)
    }

    fun fetchTrialRow(
        accessToken: String,
        userId: String,
    ): TrialStatusResult {
        val url = buildTrialCheckUrl(baseUrl = baseUrl, userId = userId)
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${accessToken.trim()}")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val serverEpoch = SubscriptionTimeUtils.parseHttpDateMillis(response.header("Date"))
                val anchorElapsed = elapsedRealtimeProvider()

                if (!response.isSuccessful) {
                    val isUnauthorized = response.code == 401
                    return TrialStatusResult(
                        success = false,
                        hasTrial = false,
                        serverEpochMillis = serverEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        isUnauthorized = isUnauthorized,
                        message = if (isUnauthorized) {
                            "Session expired. Please login again."
                        } else {
                            "Unable to verify trial status right now."
                        },
                    )
                }

                parseTrialResponse(
                    bodyText = bodyText,
                    expectedUserId = userId,
                    serverEpochMillis = serverEpoch,
                    clientAnchorElapsedMillis = anchorElapsed,
                    isNewlyActivated = false,
                )
            }
        } catch (_: IOException) {
            TrialStatusResult(
                success = false,
                hasTrial = false,
                isNetworkError = true,
                message = "Network error while checking trial status.",
            )
        } catch (e: Exception) {
            TrialStatusResult(
                success = false,
                hasTrial = false,
                message = "Unable to check trial status: ${e.message ?: "Unknown error"}",
            )
        }
    }

    private fun activateTrialOnServer(
        accessToken: String,
        userId: String,
    ): TrialStatusResult {
        val rpcUrl = baseUrl.trimEnd('/') + ACTIVATE_TRIAL_RPC_PATH
        var allowDirectInsertFallback = false
        try {
            val rpcRequest = Request.Builder()
                .url(rpcUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post("{}".toRequestBody(jsonType))
                .build()

            client.newCall(rpcRequest).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val serverEpoch = SubscriptionTimeUtils.parseHttpDateMillis(response.header("Date"))
                val anchorElapsed = elapsedRealtimeProvider()

                if (response.isSuccessful) {
                    val parsed = parseTrialResponse(
                        bodyText = bodyText,
                        expectedUserId = userId,
                        serverEpochMillis = serverEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        isNewlyActivated = true,
                    )
                    if (parsed.hasTrial) return parsed
                    return fetchTrialRow(accessToken, userId).copy(isNewlyActivated = true)
                }

                if (response.code == 401) {
                    return TrialStatusResult(
                        success = false,
                        hasTrial = false,
                        isUnauthorized = true,
                        message = "Session expired. Please login again.",
                    )
                }

                if (response.code == 404) {
                    allowDirectInsertFallback = true
                } else {
                    return TrialStatusResult(
                        success = true,
                        hasTrial = false,
                        serverEpochMillis = serverEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        message = "Trial not eligible or unavailable for this account.",
                    )
                }
            }
        } catch (_: IOException) {
            return TrialStatusResult(
                success = false,
                hasTrial = false,
                isNetworkError = true,
                message = "Network error while starting trial.",
            )
        } catch (_: Exception) {
            allowDirectInsertFallback = false
        }

        if (!allowDirectInsertFallback) {
            return TrialStatusResult(
                success = true,
                hasTrial = false,
            )
        }

        return try {
            val insertUrl = baseUrl.trimEnd('/') + USER_TRIALS_PATH
            val payload = JSONObject().put("user_id", userId)
            val insertReq = Request.Builder()
                .url(insertUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonType))
                .build()

            client.newCall(insertReq).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val serverEpoch = SubscriptionTimeUtils.parseHttpDateMillis(response.header("Date"))
                val anchorElapsed = elapsedRealtimeProvider()

                if (response.isSuccessful) {
                    val parsed = parseTrialResponse(
                        bodyText = bodyText,
                        expectedUserId = userId,
                        serverEpochMillis = serverEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        isNewlyActivated = true,
                    )
                    if (parsed.hasTrial) return parsed
                }
                if (response.code == 409) {
                    return fetchTrialRow(accessToken, userId)
                }
                TrialStatusResult(
                    success = false,
                    hasTrial = false,
                    serverEpochMillis = serverEpoch,
                    clientAnchorElapsedMillis = anchorElapsed,
                    isUnauthorized = response.code == 401,
                    message = "Unable to activate 2-day free trial right now.",
                )
            }
        } catch (_: IOException) {
            TrialStatusResult(
                success = false,
                hasTrial = false,
                isNetworkError = true,
                message = "Network error while activating trial.",
            )
        } catch (e: Exception) {
            TrialStatusResult(
                success = false,
                hasTrial = false,
                message = "Unable to activate trial: ${e.message ?: "Unknown error"}",
            )
        }
    }
}
