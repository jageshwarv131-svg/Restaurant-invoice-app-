package com.example.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class AuthResult(
    val success: Boolean,
    val message: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val userCreatedAt: String? = null,
    val requiresEmailConfirmation: Boolean = false,
    val isNetworkError: Boolean = false
)

class SupabaseAuth(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun signUp(email: String, password: String): AuthResult {
        return requestAuth(
            endpoint = "/auth/v1/signup",
            body = JSONObject()
                .put("email", email.trim())
                .put("password", password),
            isSignUp = true
        )
    }

    fun login(email: String, password: String): AuthResult {
        return requestAuth(
            endpoint = "/auth/v1/token?grant_type=password",
            body = JSONObject()
                .put("email", email.trim())
                .put("password", password),
            isSignUp = false
        )
    }

    fun refreshSession(refreshToken: String): AuthResult {
        return requestAuth(
            endpoint = "/auth/v1/token?grant_type=refresh_token",
            body = JSONObject()
                .put("refresh_token", refreshToken),
            isSignUp = false
        )
    }

    fun logout(accessToken: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/logout")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post("".toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun requestAuth(
        endpoint: String,
        body: JSONObject,
        isSignUp: Boolean
    ): AuthResult {
        return try {
            val request = Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL.trimEnd('/') + endpoint)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val error = try {
                        val json = JSONObject(responseText)
                        json.optString(
                            "msg",
                            json.optString(
                                "message",
                                json.optString("error_description", "Authentication failed")
                            )
                        )
                    } catch (_: Exception) {
                        "Authentication failed (${response.code})"
                    }

                    return AuthResult(false, error)
                }

                val json = JSONObject(responseText)
                val user = json.optJSONObject("user")

                val accessToken = json.optString("access_token")
                    .takeIf { it.isNotBlank() }

                val refreshToken = json.optString("refresh_token")
                    .takeIf { it.isNotBlank() }

                val jwtUserId = accessToken?.let {
                    SupabasePaymentService.extractJwtClaims(it)?.userId
                }
                val userId = (jwtUserId
                    ?: user?.optString("id")?.takeIf { it.isNotBlank() }
                    ?: json.optString("id").takeIf { it.isNotBlank() })

                val userCreatedAt = (user?.optString("created_at")
                    ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: json.optString("created_at")
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) })

                if (accessToken == null || userId == null) {
                    val pendingEmailConfirmation = isSignUp && (
                        userId != null ||
                            json.has("confirmation_sent_at") ||
                            json.optString("email").isNotBlank()
                        )
                    if (pendingEmailConfirmation) {
                        return AuthResult(
                            success = true,
                            message = "Account created. Please confirm your email address before logging in.",
                            userId = userId,
                            userCreatedAt = userCreatedAt,
                            requiresEmailConfirmation = true
                        )
                    }
                    return AuthResult(
                        success = false,
                        message = "Authentication failed: missing session token."
                    )
                }

                AuthResult(
                    success = true,
                    message = "Authentication successful",
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    userCreatedAt = userCreatedAt
                )
            }
        } catch (e: IOException) {
            AuthResult(
                success = false,
                message = "Network error. Please check your internet connection.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            AuthResult(false, "Something went wrong: ${e.message ?: "Unknown error"}")
        }
    }
}
