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
    val userId: String? = null
)

class SupabaseAuth {

    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun signUp(email: String, password: String): AuthResult {
        return requestAuth(
            endpoint = "/auth/v1/signup",
            body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
        )
    }

    fun login(email: String, password: String): AuthResult {
        return requestAuth(
            endpoint = "/auth/v1/token?grant_type=password",
            body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
        )
    }

    private fun requestAuth(
        endpoint: String,
        body: JSONObject
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

                val userId = user?.optString("id")
                    ?.takeIf { it.isNotBlank() }

                if (accessToken == null || userId == null) {
                    return AuthResult(
                        false,
                        "Account created. Email confirmation may be required before login."
                    )
                }

                AuthResult(
                    success = true,
                    message = "Authentication successful",
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId
                )
            }
        } catch (e: IOException) {
            AuthResult(false, "Network error. Please check your internet connection.")
        } catch (e: Exception) {
            AuthResult(false, "Something went wrong: ${e.message ?: "Unknown error"}")
        }
    }
}
