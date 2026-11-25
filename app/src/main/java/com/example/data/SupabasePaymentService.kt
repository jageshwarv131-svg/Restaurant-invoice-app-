package com.example.data

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

enum class PaymentRequestStatus(val apiValue: String, val label: String) {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromApi(value: String?): PaymentRequestStatus =
            entries.firstOrNull { it.apiValue.equals(value?.trim(), ignoreCase = true) } ?: PENDING
    }
}

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
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
    val isDuplicate: Boolean = false,
)

data class PaymentStatusFetchResult(
    val success: Boolean,
    val message: String? = null,
    val requests: List<PaymentRequestRecord> = emptyList(),
    val isUnauthorized: Boolean = false,
    val isNetworkError: Boolean = false,
)

class SupabasePaymentService(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val LICENSE_AMOUNT_INR = 499
        const val PHONEPE_UPI_ID = "7880023755@axl"
        const val PAYMENT_METHOD_UPI = "UPI"
        const val STATUS_PENDING = "pending"

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
    }

    fun submitPaymentRequest(
        accessToken: String,
        userId: String,
        utrInput: String,
        customerNote: String,
    ): PaymentSubmitResult {
        if (accessToken.isBlank()) {
            return PaymentSubmitResult(
                success = false,
                message = "Your session expired. Please login again.",
                isUnauthorized = true,
            )
        }
        if (userId.isBlank()) {
            return PaymentSubmitResult(
                success = false,
                message = "Authenticated user ID is missing. Please login again.",
                isUnauthorized = true,
            )
        }
        val utrError = validateUtr(utrInput)
        if (utrError != null) {
            return PaymentSubmitResult(success = false, message = utrError)
        }

        val payload = buildInsertPayload(userId = userId, utr = utrInput, customerNote = customerNote)
        val url = SupabaseConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/payment_requests"

        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val parsedError = extractSupabaseError(bodyText, response.code)
                    val isUnauthorized = response.code == 401
                    val isDuplicate = response.code == 409 ||
                        bodyText.contains("23505") ||
                        parsedError.contains("duplicate", ignoreCase = true)
                    val finalMessage = when {
                        isUnauthorized -> "Session expired. Refreshing authentication..."
                        isDuplicate -> "This UTR has already been submitted. Please check your payment status below."
                        else -> parsedError
                    }
                    return PaymentSubmitResult(
                        success = false,
                        message = finalMessage,
                        isUnauthorized = isUnauthorized,
                        isDuplicate = isDuplicate,
                    )
                }

                val created = parseSubmittedRecord(bodyText, userId, utrInput, customerNote)
                PaymentSubmitResult(
                    success = true,
                    message = "Payment Under Verification",
                    request = created,
                )
            }
        } catch (_: IOException) {
            PaymentSubmitResult(
                success = false,
                message = "Network error. Please check your internet connection and try again.",
                isNetworkError = true,
            )
        } catch (e: Exception) {
            PaymentSubmitResult(
                success = false,
                message = "Unable to submit payment request: ${e.message ?: "Unknown error"}",
            )
        }
    }

    fun fetchUserPaymentRequests(
        accessToken: String,
        userId: String,
    ): PaymentStatusFetchResult {
        if (accessToken.isBlank() || userId.isBlank()) {
            return PaymentStatusFetchResult(
                success = false,
                message = "Please login to view your payment status.",
                isUnauthorized = true,
            )
        }

        val encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8")
        val url =
            SupabaseConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/payment_requests?user_id=eq.$encodedUserId" +
                "&select=id,user_id,amount_inr,payment_method,utr,status,customer_note,created_at" +
                "&order=created_at.desc"

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
                        "Session expired. Please refresh or login again."
                    } else {
                        extractSupabaseError(bodyText, response.code)
                    }
                    return PaymentStatusFetchResult(
                        success = false,
                        message = message,
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
                )
            }
        } catch (_: IOException) {
            PaymentStatusFetchResult(
                success = false,
                message = "Network error while checking payment status.",
                isNetworkError = true,
            )
        } catch (e: Exception) {
            PaymentStatusFetchResult(
                success = false,
                message = "Failed to load payment status: ${e.message ?: "Unknown error"}",
            )
        }
    }

    private fun parseSubmittedRecord(
        bodyText: String,
        userId: String,
        utrInput: String,
        customerNote: String,
    ): PaymentRequestRecord {
        return try {
            val trimmed = bodyText.trim()
            val obj = when {
                trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0)
                trimmed.startsWith("{") -> JSONObject(trimmed)
                else -> null
            }
            if (obj != null) {
                parseRecord(obj)
            } else {
                PaymentRequestRecord(
                    userId = userId.trim(),
                    amountInr = LICENSE_AMOUNT_INR,
                    paymentMethod = PAYMENT_METHOD_UPI,
                    utr = sanitizeUtr(utrInput),
                    status = PaymentRequestStatus.PENDING,
                    customerNote = customerNote.trim(),
                )
            }
        } catch (_: Exception) {
            PaymentRequestRecord(
                userId = userId.trim(),
                amountInr = LICENSE_AMOUNT_INR,
                paymentMethod = PAYMENT_METHOD_UPI,
                utr = sanitizeUtr(utrInput),
                status = PaymentRequestStatus.PENDING,
                customerNote = customerNote.trim(),
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
            val hint = json.optString("hint").takeIf { it.isNotBlank() }
            when {
                msg != null && hint != null -> "$msg ($hint)"
                msg != null -> msg
                else -> "Request failed ($statusCode)"
            }
        } catch (_: Exception) {
            "Request failed ($statusCode)"
        }
    }
}
