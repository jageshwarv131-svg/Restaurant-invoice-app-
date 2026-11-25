package com.example.data

import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Base64

enum class PaymentRequestStatus(val apiValue: String, val label: String) {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromApi(value: String?): PaymentRequestStatus =
            entries.firstOrNull { it.apiValue.equals(value?.trim(), ignoreCase = true) } ?: PENDING
    }
}

data class JwtClaims(
    val userId: String? = null,
    val role: String? = null,
    val expiresAtEpochSeconds: Long? = null,
)

data class PaymentRequestRecord(
    val id: String = "",
    val userId: String = "",
    val amountInr: Int = SupabasePaymentService.LICENSE_AMOUNT_INR,
    val paymentMethod: String = SupabasePaymentService.PAYMENT_METHOD_UPI,
    val utr: String = "",
    val status: PaymentRequestStatus = PaymentRequestStatus.PENDING,
    val customerNote: String = "",
    val createdAt: String = "",
)

data class PaymentSubmitResult(
    val success: Boolean,
    val message: String,
    val request: PaymentRequestRecord? = null,
    val httpStatusCode: Int? = null,
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
    val isDuplicate: Boolean = false,
)

data class PaymentStatusFetchResult(
    val success: Boolean,
    val message: String? = null,
    val requests: List<PaymentRequestRecord> = emptyList(),
    val httpStatusCode: Int? = null,
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
)

class SupabasePaymentService(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonType = "application/json".toMediaType()

    companion object {
        private const val TAG = "SupabasePaymentService"
        const val LICENSE_AMOUNT_INR = 499
        const val PHONEPE_UPI_ID = "7880023755@axl"
        const val PAYMENT_METHOD_UPI = "UPI"
        const val STATUS_PENDING = "pending"
        const val PAYMENT_REQUESTS_PATH = "/rest/v1/payment_requests"

        fun sanitizeUtr(raw: String): String = raw.trim().uppercase()

        fun validateUtr(rawUtr: String): String? {
            val utr = sanitizeUtr(rawUtr)
            if (utr.isBlank()) {
                return "Please enter your UTR / Transaction Reference Number."
            }
            if (utr.length < 10 || utr.length > 35) {
                return "Invalid UTR. Enter a valid 10 to 35 character alphanumeric reference number."
            }
            if (!utr.all { it.isLetterOrDigit() }) {
                return "Invalid UTR. Only letters and numbers are allowed (no spaces or symbols)."
            }
            if (utr.count { it.isDigit() } < 6) {
                return "Invalid UTR. Please enter the numeric/alphanumeric UPI transaction reference number."
            }
            return null
        }

        fun extractJwtClaims(accessToken: String): JwtClaims? {
            return try {
                val parts = accessToken.trim().split(".")
                if (parts.size != 3 || parts[1].isBlank()) return null
                val decodedBytes = Base64.getUrlDecoder().decode(parts[1])
                val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                JwtClaims(
                    userId = json.optString("sub").takeIf { it.isNotBlank() },
                    role = json.optString("role").takeIf { it.isNotBlank() },
                    expiresAtEpochSeconds = if (json.has("exp")) json.optLong("exp") else null,
                )
            } catch (_: Exception) {
                null
            }
        }

        fun buildUpiPaymentUri(
            upiId: String = PHONEPE_UPI_ID,
            payeeName: String = "Vyapar Billing Pro",
            amountInr: Int = LICENSE_AMOUNT_INR,
            note: String = "Vyapar Billing Pro Lifetime License",
        ): Uri {
            return Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", payeeName)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", "$amountInr.00")
                .appendQueryParameter("cu", "INR")
                .build()
        }

        internal fun buildInsertPayload(
            userId: String,
            utr: String,
            customerNote: String,
        ): JSONObject {
            return JSONObject()
                .put("user_id", userId.trim())
                .put("amount_inr", LICENSE_AMOUNT_INR)
                .put("payment_method", PAYMENT_METHOD_UPI)
                .put("utr", sanitizeUtr(utr))
                .put("status", STATUS_PENDING)
                .put("customer_note", customerNote.trim())
        }

        internal fun parseRecord(json: JSONObject): PaymentRequestRecord {
            return PaymentRequestRecord(
                id = json.optString("id"),
                userId = json.optString("user_id"),
                amountInr = json.optInt("amount_inr", LICENSE_AMOUNT_INR),
                paymentMethod = json.optString("payment_method", PAYMENT_METHOD_UPI),
                utr = json.optString("utr"),
                status = PaymentRequestStatus.fromApi(json.optString("status")),
                customerNote = json.optString("customer_note"),
                createdAt = json.optString("created_at"),
            )
        }

        internal fun parseConfirmedInsertedRecord(
            bodyText: String,
            expectedUserId: String,
            expectedUtr: String,
        ): PaymentRequestRecord? {
            return try {
                val trimmed = bodyText.trim()
                if (trimmed.isBlank()) return null
                val obj = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0)
                    trimmed.startsWith("{") -> JSONObject(trimmed)
                    else -> null
                } ?: return null

                val record = parseRecord(obj)
                val cleanExpectedUtr = sanitizeUtr(expectedUtr)
                val hasConfirmedIdentity =
                    record.id.isNotBlank() || record.utr.equals(cleanExpectedUtr, ignoreCase = true)
                val matchesUser =
                    record.userId.isBlank() || record.userId == expectedUserId.trim()
                val matchesUtr =
                    record.utr.isBlank() || record.utr.equals(cleanExpectedUtr, ignoreCase = true)

                if (hasConfirmedIdentity && matchesUser && matchesUtr) {
                    record
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        internal fun sanitizeSafeErrorText(raw: String): String {
            return raw
                .replace(Regex("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
                .replace(Regex("eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]*"), "[JWT_REDACTED]")
                .replace(Regex("sb_[A-Za-z0-9_\\-]+"), "[KEY_REDACTED]")
                .trim()
        }

        internal fun formatSupabaseError(bodyText: String, statusCode: Int): String {
            val safeBody = sanitizeSafeErrorText(bodyText)
            return try {
                val json = JSONObject(safeBody)
                val code = json.optString("code").takeIf { it.isNotBlank() }
                val msg = json.optString("message").takeIf { it.isNotBlank() }
                    ?: json.optString("msg").takeIf { it.isNotBlank() }
                    ?: json.optString("error_description").takeIf { it.isNotBlank() }
                    ?: json.optString("error").takeIf { it.isNotBlank() }
                val details = json.optString("details").takeIf { it.isNotBlank() && it != "null" }
                val hint = json.optString("hint").takeIf { it.isNotBlank() && it != "null" }

                val prefix = if (code != null) "HTTP $statusCode ($code)" else "HTTP $statusCode"
                val parts = buildList {
                    if (msg != null) add(msg)
                    if (details != null) add("Details: $details")
                    if (hint != null) add("Hint: $hint")
                }
                if (parts.isNotEmpty()) "$prefix: ${parts.joinToString(" | ")}"
                else "$prefix: Request failed"
            } catch (_: Exception) {
                val snippet = safeBody.take(180).ifBlank { "Empty error response" }
                "HTTP $statusCode: $snippet"
            }
        }
    }

    fun submitPaymentRequest(
        accessToken: String,
        userId: String,
        utrInput: String,
        customerNote: String,
        currentEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): PaymentSubmitResult {
        val cleanToken = accessToken.trim()
        val cleanUserId = userId.trim()

        if (cleanToken.isBlank()) {
            return PaymentSubmitResult(
                success = false,
                message = "Not authenticated: access token is missing. Please login again.",
                isUnauthorized = true,
            )
        }
        if (cleanUserId.isBlank()) {
            return PaymentSubmitResult(
                success = false,
                message = "Authenticated user ID is missing. Please login again.",
                isUnauthorized = true,
            )
        }

        val claims = extractJwtClaims(cleanToken)
        if (claims == null || claims.userId.isNullOrBlank()) {
            return PaymentSubmitResult(
                success = false,
                message = "Invalid authentication token (missing JWT user ID). Please login again.",
                isUnauthorized = true,
            )
        }
        if (claims.userId != cleanUserId) {
            safeLogWarn("POST $PAYMENT_REQUESTS_PATH aborted: authenticated userId does not match JWT sub")
            return PaymentSubmitResult(
                success = false,
                message = "User session mismatch: authenticated user ID does not match JWT user ID. Please login again.",
                isUnauthorized = true,
            )
        }
        if (!claims.role.isNullOrBlank() && claims.role != "authenticated") {
            return PaymentSubmitResult(
                success = false,
                message = "Invalid Supabase session role (${claims.role}). Please login again.",
                isUnauthorized = true,
            )
        }
        if (claims.expiresAtEpochSeconds != null && claims.expiresAtEpochSeconds <= currentEpochSeconds) {
            return PaymentSubmitResult(
                success = false,
                message = "Session token expired. Refreshing authentication...",
                isUnauthorized = true,
            )
        }

        val utrError = validateUtr(utrInput)
        if (utrError != null) {
            return PaymentSubmitResult(success = false, message = utrError)
        }

        val payload = buildInsertPayload(userId = cleanUserId, utr = utrInput, customerNote = customerNote)
        val url = SupabaseConfig.SUPABASE_URL.trimEnd('/') + PAYMENT_REQUESTS_PATH

        return try {
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $cleanToken")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Prefer", "return=representation")
                .post(payload.toString().toByteArray(Charsets.UTF_8).toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val formattedError = formatSupabaseError(bodyText, response.code)
                    val isUnauthorized = response.code == 401
                    val isDuplicate = response.code == 409 ||
                        bodyText.contains("23505") ||
                        formattedError.contains("duplicate", ignoreCase = true)

                    safeLogWarn("POST $PAYMENT_REQUESTS_PATH failed: $formattedError")

                    val finalMessage = when {
                        isUnauthorized -> "Session expired ($formattedError). Refreshing authentication..."
                        isDuplicate -> "$formattedError — This UTR has already been submitted."
                        else -> formattedError
                    }
                    return PaymentSubmitResult(
                        success = false,
                        message = finalMessage,
                        httpStatusCode = response.code,
                        isUnauthorized = isUnauthorized,
                        isDuplicate = isDuplicate,
                    )
                }

                val confirmed = parseConfirmedInsertedRecord(
                    bodyText = bodyText,
                    expectedUserId = cleanUserId,
                    expectedUtr = utrInput,
                )
                if (confirmed == null) {
                    val safePreview = sanitizeSafeErrorText(bodyText).take(160)
                        .ifBlank { "empty response body" }
                    val msg =
                        "HTTP ${response.code}: Supabase did not confirm inserted row ($safePreview). Please verify RLS insert/select policy."
                    safeLogWarn("POST $PAYMENT_REQUESTS_PATH unconfirmed representation: $msg")
                    return PaymentSubmitResult(
                        success = false,
                        message = msg,
                        httpStatusCode = response.code,
                    )
                }

                PaymentSubmitResult(
                    success = true,
                    message = "Payment Under Verification",
                    request = confirmed,
                    httpStatusCode = response.code,
                )
            }
        } catch (e: IOException) {
            val detail = sanitizeSafeErrorText(e.message ?: "Connection failed")
            safeLogWarn("POST $PAYMENT_REQUESTS_PATH network error: $detail")
            PaymentSubmitResult(
                success = false,
                message = "Network error ($detail). Please check your internet connection and try again.",
                isNetworkError = true,
            )
        } catch (e: Exception) {
            val detail = sanitizeSafeErrorText(e.message ?: "Unknown error")
            safeLogWarn("POST $PAYMENT_REQUESTS_PATH exception: $detail")
            PaymentSubmitResult(
                success = false,
                message = "Unable to submit payment request: $detail",
            )
        }
    }

    fun fetchUserPaymentRequests(
        accessToken: String,
        userId: String,
    ): PaymentStatusFetchResult {
        val cleanToken = accessToken.trim()
        val cleanUserId = userId.trim()
        if (cleanToken.isBlank() || cleanUserId.isBlank()) {
            return PaymentStatusFetchResult(
                success = false,
                message = "Please login to view your payment status.",
                isUnauthorized = true,
            )
        }

        val encodedUserId = URLEncoder.encode(cleanUserId, "UTF-8")
        val url =
            SupabaseConfig.SUPABASE_URL.trimEnd('/') +
                "$PAYMENT_REQUESTS_PATH?user_id=eq.$encodedUserId" +
                "&select=id,user_id,amount_inr,payment_method,utr,status,customer_note,created_at" +
                "&order=created_at.desc"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val isUnauthorized = response.code == 401
                    val formatted = formatSupabaseError(bodyText, response.code)
                    safeLogWarn("GET $PAYMENT_REQUESTS_PATH failed: $formatted")
                    val message = if (isUnauthorized) {
                        "Session expired ($formatted). Please login again."
                    } else {
                        formatted
                    }
                    return PaymentStatusFetchResult(
                        success = false,
                        message = message,
                        httpStatusCode = response.code,
                        isUnauthorized = isUnauthorized,
                    )
                }

                val array = if (bodyText.isBlank()) JSONArray() else JSONArray(bodyText)
                val list = buildList {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.let { add(parseRecord(it)) }
                    }
                }
                PaymentStatusFetchResult(
                    success = true,
                    requests = list,
                    httpStatusCode = response.code,
                )
            }
        } catch (e: IOException) {
            val detail = sanitizeSafeErrorText(e.message ?: "Connection failed")
            PaymentStatusFetchResult(
                success = false,
                message = "Network error while checking payment status ($detail).",
                isNetworkError = true,
            )
        } catch (e: Exception) {
            PaymentStatusFetchResult(
                success = false,
                message = "Failed to load payment status: ${sanitizeSafeErrorText(e.message ?: "Unknown error")}",
            )
        }
    }

    private fun safeLogWarn(message: String) {
        runCatching { Log.w(TAG, sanitizeSafeErrorText(message)) }
    }
}
