package com.example.data

import android.os.SystemClock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

enum class LicenseStatus {
    UNAUTHENTICATED,
    CHECKING,
    ACTIVE,
    EXPIRED,
    REVOKED,
    INACTIVE,
    ERROR,
}

data class LicenseVerificationResult(
    val hasActiveLicense: Boolean,
    val status: LicenseStatus,
    val plan: SubscriptionPlan = SubscriptionPlan.LIFETIME,
    val licenseId: String? = null,
    val userId: String? = null,
    val activatedAt: String? = null,
    val expiresAt: String? = null,
    val utr: String? = null,
    val paidAmountInr: Int? = null,
    val isLegacyLifetime: Boolean = false,
    val serverEpochMillis: Long? = null,
    val clientAnchorElapsedMillis: Long? = null,
    val message: String? = null,
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
)

data class LicenseAccessState(
    val hasActiveLicense: Boolean = false,
    val status: LicenseStatus = LicenseStatus.UNAUTHENTICATED,
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val isChecking: Boolean = false,
    val licenseId: String? = null,
    val userId: String? = null,
    val activatedAt: String? = null,
    val expiresAt: String? = null,
    val utr: String? = null,
    val paidAmountInr: Int? = null,
    val isLegacyLifetime: Boolean = false,
    val message: String? = null,
    val isNetworkError: Boolean = false,
)

class SupabaseLicenseService(
    private val client: OkHttpClient = OkHttpClient(),
    private val elapsedRealtimeProvider: () -> Long = {
        runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
    },
) {
    companion object {
        fun buildLicensesCheckUrl(
            baseUrl: String = SupabaseConfig.SUPABASE_URL,
            userId: String,
            activeOnly: Boolean = true,
        ): String {
            val encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8")
            return if (activeOnly) {
                baseUrl.trimEnd('/') +
                    "/rest/v1/licenses?user_id=eq.$encodedUserId&status=eq.active&select=*"
            } else {
                baseUrl.trimEnd('/') +
                    "/rest/v1/licenses?user_id=eq.$encodedUserId&select=*"
            }
        }

        internal fun parseActiveLicenseResponse(
            bodyText: String,
            expectedUserId: String,
            serverEpochMillis: Long? = null,
            clientAnchorElapsedMillis: Long? = null,
        ): LicenseVerificationResult {
            val trimmed = bodyText.trim()
            if (trimmed.isBlank()) {
                return LicenseVerificationResult(
                    hasActiveLicense = false,
                    status = LicenseStatus.INACTIVE,
                    serverEpochMillis = serverEpochMillis,
                    clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                    message = "No active license found.",
                )
            }
            val cleanExpectedUserId = expectedUserId.trim()
            val nowMillis = serverEpochMillis ?: System.currentTimeMillis()
            val array = JSONArray(trimmed)

            var expiredCandidate: LicenseVerificationResult? = null
            var revokedCandidate: LicenseVerificationResult? = null

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rowStatus = obj.optString("status").trim()
                val rowUserId = obj.optString("user_id").trim()
                val matchesUser = rowUserId.isNotBlank() && rowUserId == cleanExpectedUserId
                if (!matchesUser) continue

                val licenseId = obj.optString("id")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val activatedAt = obj.optString("activated_at")
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val expiresAt = obj.optString("expires_at")
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val utr = obj.optString("utr")
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val rawPlanType = obj.optString("plan_type")
                    .trim()
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val rawAmount = obj.optInt("amount_inr", 0).takeIf { it > 0 }

                val hasExpiry = expiresAt != null
                val resolvedPlan = SubscriptionPlan.fromApi(
                    planType = rawPlanType,
                    amountInr = rawAmount,
                    hasExpiry = hasExpiry,
                )
                val isLegacyLifetime =
                    resolvedPlan == SubscriptionPlan.LIFETIME &&
                        !hasExpiry &&
                        (rawPlanType == null || rawAmount == null || rawAmount == 499)

                when {
                    rowStatus.equals("active", ignoreCase = true) -> {
                        val expiresMillis = SubscriptionTimeUtils.parseIsoEpochMillis(expiresAt)
                        val isTimeExpired =
                            resolvedPlan != SubscriptionPlan.LIFETIME &&
                                expiresMillis != null &&
                                nowMillis >= expiresMillis

                        if (isTimeExpired) {
                            if (expiredCandidate == null) {
                                expiredCandidate = LicenseVerificationResult(
                                    hasActiveLicense = false,
                                    status = LicenseStatus.EXPIRED,
                                    plan = resolvedPlan,
                                    licenseId = licenseId,
                                    userId = rowUserId,
                                    activatedAt = activatedAt,
                                    expiresAt = expiresAt,
                                    utr = utr,
                                    paidAmountInr = rawAmount,
                                    isLegacyLifetime = false,
                                    serverEpochMillis = serverEpochMillis,
                                    clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                                    message = "${resolvedPlan.title} expired.",
                                )
                            }
                            continue
                        }

                        return LicenseVerificationResult(
                            hasActiveLicense = true,
                            status = LicenseStatus.ACTIVE,
                            plan = resolvedPlan,
                            licenseId = licenseId,
                            userId = rowUserId,
                            activatedAt = activatedAt,
                            expiresAt = expiresAt,
                            utr = utr,
                            paidAmountInr = rawAmount ?: if (isLegacyLifetime) 499 else resolvedPlan.priceInr,
                            isLegacyLifetime = isLegacyLifetime,
                            serverEpochMillis = serverEpochMillis,
                            clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                            message = "${resolvedPlan.title} Active",
                        )
                    }

                    rowStatus.equals("revoked", ignoreCase = true) -> {
                        if (revokedCandidate == null) {
                            revokedCandidate = LicenseVerificationResult(
                                hasActiveLicense = false,
                                status = LicenseStatus.REVOKED,
                                plan = resolvedPlan,
                                licenseId = licenseId,
                                userId = rowUserId,
                                activatedAt = activatedAt,
                                expiresAt = expiresAt,
                                utr = utr,
                                paidAmountInr = rawAmount,
                                isLegacyLifetime = isLegacyLifetime,
                                serverEpochMillis = serverEpochMillis,
                                clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                                message = "License access revoked.",
                            )
                        }
                    }

                    rowStatus.equals("expired", ignoreCase = true) -> {
                        if (expiredCandidate == null) {
                            expiredCandidate = LicenseVerificationResult(
                                hasActiveLicense = false,
                                status = LicenseStatus.EXPIRED,
                                plan = resolvedPlan,
                                licenseId = licenseId,
                                userId = rowUserId,
                                activatedAt = activatedAt,
                                expiresAt = expiresAt,
                                utr = utr,
                                paidAmountInr = rawAmount,
                                isLegacyLifetime = false,
                                serverEpochMillis = serverEpochMillis,
                                clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                                message = "${resolvedPlan.title} expired.",
                            )
                        }
                    }
                }
            }

            if (revokedCandidate != null) return revokedCandidate
            if (expiredCandidate != null) return expiredCandidate

            return LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.INACTIVE,
                serverEpochMillis = serverEpochMillis,
                clientAnchorElapsedMillis = clientAnchorElapsedMillis,
                message = "No active license found.",
            )
        }
    }

    fun verifyActiveLicense(
        accessToken: String,
        userId: String,
    ): LicenseVerificationResult = verifyLicenseInternal(
        accessToken = accessToken,
        userId = userId,
        activeOnly = true,
    )

    fun verifySubscriptionLicense(
        accessToken: String,
        userId: String,
    ): LicenseVerificationResult = verifyLicenseInternal(
        accessToken = accessToken,
        userId = userId,
        activeOnly = false,
    )

    private fun verifyLicenseInternal(
        accessToken: String,
        userId: String,
        activeOnly: Boolean,
    ): LicenseVerificationResult {
        val cleanToken = accessToken.trim()
        val cleanUserId = userId.trim()
        if (cleanToken.isBlank() || cleanUserId.isBlank()) {
            return LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.UNAUTHENTICATED,
                message = "Please login to verify your subscription license.",
                isUnauthorized = true,
            )
        }

        val jwtSub = SupabasePaymentService.extractJwtClaims(cleanToken)?.userId
        if (!jwtSub.isNullOrBlank() && jwtSub != cleanUserId) {
            return LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.UNAUTHENTICATED,
                message = "Session user mismatch. Please login again to verify license.",
                isUnauthorized = true,
            )
        }

        val url = buildLicensesCheckUrl(userId = cleanUserId, activeOnly = activeOnly)
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val serverEpoch = SubscriptionTimeUtils.parseHttpDateMillis(response.header("Date"))
                val anchorElapsed = elapsedRealtimeProvider()

                if (!response.isSuccessful) {
                    val isUnauthorized = response.code == 401
                    val message = if (isUnauthorized) {
                        "Session expired. Please login again to verify license."
                    } else {
                        extractError(bodyText, response.code)
                    }
                    return LicenseVerificationResult(
                        hasActiveLicense = false,
                        status = if (isUnauthorized) LicenseStatus.UNAUTHENTICATED else LicenseStatus.ERROR,
                        serverEpochMillis = serverEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        message = message,
                        isUnauthorized = isUnauthorized,
                    )
                }

                parseActiveLicenseResponse(
                    bodyText = bodyText,
                    expectedUserId = userId,
                    serverEpochMillis = serverEpoch,
                    clientAnchorElapsedMillis = anchorElapsed,
                )
            }
        } catch (_: IOException) {
            LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.ERROR,
                message = "Network error while verifying license. Please check internet and retry.",
                isNetworkError = true,
            )
        } catch (e: Exception) {
            LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.ERROR,
                message = "Unable to verify license: ${e.message ?: "Unknown error"}",
            )
        }
    }

    private fun extractError(bodyText: String, statusCode: Int): String {
        return try {
            val json = JSONObject(bodyText)
            val msg = json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("msg").takeIf { it.isNotBlank() }
                ?: json.optString("error_description").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
            msg ?: "License verification failed ($statusCode)"
        } catch (_: Exception) {
            "License verification failed ($statusCode)"
        }
    }
}
