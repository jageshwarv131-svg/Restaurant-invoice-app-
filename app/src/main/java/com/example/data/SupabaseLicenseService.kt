package com.example.data

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
    INACTIVE,
    ERROR,
}

data class LicenseVerificationResult(
    val hasActiveLicense: Boolean,
    val status: LicenseStatus,
    val licenseId: String? = null,
    val message: String? = null,
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
)

data class LicenseAccessState(
    val hasActiveLicense: Boolean = false,
    val status: LicenseStatus = LicenseStatus.UNAUTHENTICATED,
    val isChecking: Boolean = false,
    val licenseId: String? = null,
    val message: String? = null,
    val isNetworkError: Boolean = false,
)

class SupabaseLicenseService(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        fun buildLicensesCheckUrl(
            baseUrl: String = SupabaseConfig.SUPABASE_URL,
            userId: String,
        ): String {
            val encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8")
            return baseUrl.trimEnd('/') +
                "/rest/v1/licenses?user_id=eq.$encodedUserId&status=eq.active&select=*"
        }

        internal fun parseActiveLicenseResponse(
            bodyText: String,
            expectedUserId: String,
        ): LicenseVerificationResult {
            val trimmed = bodyText.trim()
            if (trimmed.isBlank()) {
                return LicenseVerificationResult(
                    hasActiveLicense = false,
                    status = LicenseStatus.INACTIVE,
                    message = "No active lifetime license found.",
                )
            }
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rowStatus = obj.optString("status").trim()
                val rowUserId = obj.optString("user_id").trim()
                val matchesUser = rowUserId.isBlank() || rowUserId == expectedUserId.trim()
                if (rowStatus.equals("active", ignoreCase = true) && matchesUser) {
                    return LicenseVerificationResult(
                        hasActiveLicense = true,
                        status = LicenseStatus.ACTIVE,
                        licenseId = obj.optString("id").takeIf { it.isNotBlank() },
                        message = "Lifetime License Active",
                    )
                }
            }
            return LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.INACTIVE,
                message = "No active lifetime license found.",
            )
        }
    }

    fun verifyActiveLicense(
        accessToken: String,
        userId: String,
    ): LicenseVerificationResult {
        if (accessToken.isBlank() || userId.isBlank()) {
            return LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.UNAUTHENTICATED,
                message = "Please login to verify your lifetime license.",
                isUnauthorized = true,
            )
        }

        val url = buildLicensesCheckUrl(userId = userId)
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
                if (!response.isSuccessful) {
                    val isUnauthorized = response.code == 401
                    val message = if (isUnauthorized) {
                        "Session expired. Please login again to verify license."
                    } else {
                        extractSupabaseError(bodyText, response.code)
                    }
                    return LicenseVerificationResult(
                        hasActiveLicense = false,
                        status = if (isUnauthorized) LicenseStatus.UNAUTHENTICATED else LicenseStatus.ERROR,
                        message = message,
                        isUnauthorized = isUnauthorized,
                    )
                }

                parseActiveLicenseResponse(bodyText = bodyText, expectedUserId = userId)
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

    private fun extractSupabaseError(bodyText: String, statusCode: Int): String {
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
