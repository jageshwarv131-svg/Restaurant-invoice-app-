package com.example.data

import android.os.SystemClock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SubscriptionPlan(
    val title: String,
    val priceInr: Int,
    val durationLabel: String,
    val apiValue: String,
    val badge: String? = null,
) {
    FREE(
        title = "Free Core Plan",
        priceInr = 0,
        durationLabel = "Always Free",
        apiValue = "free",
    ),
    TRIAL(
        title = "2-Day Free Trial",
        priceInr = 0,
        durationLabel = "48 Hours",
        apiValue = "trial",
        badge = "48h Free",
    ),
    MONTHLY(
        title = "Monthly License",
        priceInr = 499,
        durationLabel = "1 Month",
        apiValue = "monthly",
        badge = "Starter",
    ),
    YEARLY(
        title = "Yearly License",
        priceInr = 999,
        durationLabel = "1 Year",
        apiValue = "yearly",
        badge = "Best Value",
    ),
    LIFETIME(
        title = "Lifetime License",
        priceInr = 2000,
        durationLabel = "Lifetime",
        apiValue = "lifetime",
        badge = "One-Time",
    );

    val isPaidPlan: Boolean
        get() = this == MONTHLY || this == YEARLY || this == LIFETIME

    companion object {
        val purchasablePlans: List<SubscriptionPlan> = listOf(MONTHLY, YEARLY, LIFETIME)

        fun fromApi(
            planType: String?,
            amountInr: Int? = null,
            hasExpiry: Boolean? = null,
        ): SubscriptionPlan {
            val normalized = planType?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return when (normalized) {
                "monthly", "month" -> MONTHLY
                "yearly", "annual", "year" -> YEARLY
                "lifetime" -> LIFETIME
                "trial" -> TRIAL
                "free" -> FREE
                else -> when {
                    hasExpiry == false -> LIFETIME
                    amountInr == 999 -> YEARLY
                    amountInr == 2000 -> LIFETIME
                    amountInr == 499 && hasExpiry == true -> MONTHLY
                    amountInr == 499 && hasExpiry == null -> MONTHLY
                    else -> LIFETIME
                }
            }
        }
    }
}

enum class EntitlementStatus(val label: String) {
    UNAUTHENTICATED("Login Required"),
    CHECKING("Verifying Subscription..."),
    TRIAL_ACTIVE("Free Trial Active"),
    TRIAL_EXPIRED("Trial Expired"),
    PAYMENT_PENDING("Payment Pending"),
    PAYMENT_REJECTED("Payment Rejected"),
    MONTHLY_ACTIVE("Monthly Active"),
    MONTHLY_EXPIRED("Monthly Expired"),
    YEARLY_ACTIVE("Yearly Active"),
    YEARLY_EXPIRED("Yearly Expired"),
    LIFETIME_ACTIVE("Lifetime Active"),
    REVOKED("Access Revoked"),
    ERROR("Verification Error"),
}

enum class FeatureKey(val title: String, val tier: FeatureTier) {
    DASHBOARD("Dashboard", FeatureTier.CORE_FREE),
    POS_BILLING("POS Quick Billing", FeatureTier.CORE_FREE),
    BILLS("Bills & Invoices", FeatureTier.CORE_FREE),
    INVOICE_PREVIEW("Invoice Preview & PDF", FeatureTier.CORE_FREE),
    PRODUCTS("Products Catalog", FeatureTier.CORE_FREE),
    CUSTOMERS("Customers", FeatureTier.CORE_FREE),
    BUSINESS_PROFILE("Business Profile", FeatureTier.CORE_FREE),
    INVOICE_SETTINGS("Invoice Settings", FeatureTier.CORE_FREE),
    HELP("Help & Support", FeatureTier.CORE_FREE),
    PENDING_ORDERS("Pending Orders", FeatureTier.TRIAL_AND_PAID),
    EXPENSES("Expense Tracker", FeatureTier.TRIAL_AND_PAID),
    INVENTORY("Inventory & Low Stock", FeatureTier.TRIAL_AND_PAID),
    REPORTS("Reports & Analytics", FeatureTier.PAID_ONLY),
    BACKUP_RESTORE("Backup & Restore", FeatureTier.PAID_ONLY),
}

enum class FeatureTier {
    CORE_FREE,
    TRIAL_AND_PAID,
    PAID_ONLY,
}

data class FeatureDecision(
    val allowed: Boolean,
    val tier: FeatureTier,
    val reasonTitle: String = "",
    val reasonMessage: String = "",
)

data class SubscriptionState(
    val userId: String? = null,
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val status: EntitlementStatus = EntitlementStatus.UNAUTHENTICATED,
    val activatedAt: String? = null,
    val expiresAt: String? = null,
    val trialStartedAt: String? = null,
    val trialExpiresAt: String? = null,
    val paymentId: String? = null,
    val licenseId: String? = null,
    val utr: String? = null,
    val paidAmountInr: Int? = null,
    val isLegacyLifetime: Boolean = false,
    val serverClockOffsetMillis: Long = 0L,
    val serverAnchorEpochMillis: Long? = null,
    val clientAnchorElapsedMillis: Long? = null,
    val isChecking: Boolean = false,
    val isNetworkError: Boolean = false,
    val message: String? = null,
) {
    val hasActivePaidPlan: Boolean
        get() = status == EntitlementStatus.MONTHLY_ACTIVE ||
            status == EntitlementStatus.YEARLY_ACTIVE ||
            status == EntitlementStatus.LIFETIME_ACTIVE

    val hasActiveTrial: Boolean
        get() = status == EntitlementStatus.TRIAL_ACTIVE

    val hasActiveAccess: Boolean
        get() = hasActivePaidPlan || hasActiveTrial

    val isLifetimeActive: Boolean
        get() = status == EntitlementStatus.LIFETIME_ACTIVE

    val isExpiredState: Boolean
        get() = status == EntitlementStatus.TRIAL_EXPIRED ||
            status == EntitlementStatus.MONTHLY_EXPIRED ||
            status == EntitlementStatus.YEARLY_EXPIRED

    val displayPaidAmountInr: Int
        get() = paidAmountInr ?: when {
            isLegacyLifetime -> 499
            plan == SubscriptionPlan.LIFETIME -> SubscriptionPlan.LIFETIME.priceInr
            plan == SubscriptionPlan.YEARLY -> SubscriptionPlan.YEARLY.priceInr
            plan == SubscriptionPlan.MONTHLY -> SubscriptionPlan.MONTHLY.priceInr
            else -> SubscriptionPlan.MONTHLY.priceInr
        }

    fun trustedNowMillis(
        systemNowMillis: Long = System.currentTimeMillis(),
        elapsedNowMillis: Long = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L),
    ): Long {
        val anchorEpoch = serverAnchorEpochMillis
        val anchorElapsed = clientAnchorElapsedMillis
        if (anchorEpoch != null && anchorElapsed != null && elapsedNowMillis >= anchorElapsed) {
            return anchorEpoch + (elapsedNowMillis - anchorElapsed)
        }
        return systemNowMillis + serverClockOffsetMillis
    }

    fun trialRemainingMillis(nowMillis: Long = trustedNowMillis()): Long {
        val expiryMillis = SubscriptionTimeUtils.parseIsoEpochMillis(trialExpiresAt) ?: return 0L
        return (expiryMillis - nowMillis).coerceAtLeast(0L)
    }

    fun planRemainingMillis(nowMillis: Long = trustedNowMillis()): Long {
        val expiryMillis = SubscriptionTimeUtils.parseIsoEpochMillis(expiresAt) ?: return 0L
        return (expiryMillis - nowMillis).coerceAtLeast(0L)
    }
}

object SubscriptionTimeUtils {
    const val TRIAL_DURATION_MILLIS: Long = 48L * 60L * 60L * 1000L

    fun parseIsoEpochMillis(raw: String?): Long? {
        val clean = raw?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: return null
        return runCatching { Instant.parse(clean).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(clean).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                val normalized = if (clean.contains(' ')) clean.replace(' ', 'T') else clean
                OffsetDateTime.parse(normalized).toInstant().toEpochMilli()
            }.getOrNull()
    }

    fun parseHttpDateMillis(httpDateHeader: String?): Long? {
        val clean = httpDateHeader?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            ZonedDateTime.parse(clean, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.getOrNull()
    }

    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "Expired"
        val totalMinutes = (remainingMillis / 60_000L).coerceAtLeast(1L)
        val totalHours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return when {
            totalHours >= 24L -> {
                val days = totalHours / 24L
                val remHours = totalHours % 24L
                "${days}d ${remHours}h ${mins}m left"
            }
            totalHours >= 1L -> "${totalHours}h ${mins}m left"
            else -> "${totalMinutes}m left"
        }
    }

    fun formatReadableDateTime(iso: String?): String {
        val millis = parseIsoEpochMillis(iso) ?: return iso?.takeIf { it.isNotBlank() } ?: "-"
        return runCatching {
            val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(zdt)
        }.getOrElse { iso ?: "-" }
    }
}

object FeatureEntitlements {

    fun evaluate(
        feature: FeatureKey,
        subscriptionState: SubscriptionState,
        isAuthenticated: Boolean,
        nowMillis: Long = subscriptionState.trustedNowMillis(),
    ): FeatureDecision {
        if (!isAuthenticated) {
            return FeatureDecision(
                allowed = false,
                tier = feature.tier,
                reasonTitle = "Account Sign-In Required",
                reasonMessage = "Please sign in to access ${feature.title}.",
            )
        }

        if (feature.tier == FeatureTier.CORE_FREE) {
            return FeatureDecision(allowed = true, tier = FeatureTier.CORE_FREE)
        }

        if (subscriptionState.status == EntitlementStatus.REVOKED) {
            return FeatureDecision(
                allowed = false,
                tier = feature.tier,
                reasonTitle = "Subscription Access Revoked",
                reasonMessage = "Your license access was revoked. Upgrade or contact support to unlock ${feature.title}.",
            )
        }

        val hasPaid = when (subscriptionState.status) {
            EntitlementStatus.LIFETIME_ACTIVE -> true
            EntitlementStatus.MONTHLY_ACTIVE, EntitlementStatus.YEARLY_ACTIVE -> {
                val exp = SubscriptionTimeUtils.parseIsoEpochMillis(subscriptionState.expiresAt)
                exp == null || nowMillis < exp
            }
            else -> false
        }

        if (hasPaid) {
            return FeatureDecision(allowed = true, tier = feature.tier)
        }

        if (feature.tier == FeatureTier.TRIAL_AND_PAID &&
            subscriptionState.status == EntitlementStatus.TRIAL_ACTIVE
        ) {
            val trialExp = SubscriptionTimeUtils.parseIsoEpochMillis(subscriptionState.trialExpiresAt)
            if (trialExp != null && nowMillis < trialExp) {
                return FeatureDecision(allowed = true, tier = feature.tier)
            }
        }

        val (title, message) = when (subscriptionState.status) {
            EntitlementStatus.TRIAL_ACTIVE ->
                "Paid Plan Required for ${feature.title}" to
                    "${feature.title} requires an active Monthly (₹499), Yearly (₹999), or Lifetime (₹2000) plan. Your core billing and trial tools stay active."
            EntitlementStatus.TRIAL_EXPIRED ->
                "2-Day Free Trial Ended" to
                    "Your 48-hour trial has ended. Core POS billing, bills & customer records stay safe — upgrade to Monthly ₹499, Yearly ₹999, or Lifetime ₹2000 to unlock ${feature.title}."
            EntitlementStatus.MONTHLY_EXPIRED, EntitlementStatus.YEARLY_EXPIRED ->
                "Subscription Expired" to
                    "Your ${subscriptionState.plan.title} has expired. Renew to unlock ${feature.title}."
            EntitlementStatus.PAYMENT_PENDING ->
                "Payment Under Verification" to
                    "Your payment is pending admin approval. ${feature.title} unlocks once activated."
            EntitlementStatus.PAYMENT_REJECTED ->
                "Payment Rejected" to
                    "Your previous payment request was rejected. Submit a valid UTR to unlock ${feature.title}."
            else ->
                "Upgrade Required" to
                    "Choose Monthly (₹499), Yearly (₹999), or Lifetime (₹2000) to unlock ${feature.title}."
        }

        return FeatureDecision(
            allowed = false,
            tier = feature.tier,
            reasonTitle = title,
            reasonMessage = message,
        )
    }

    fun resolveSubscriptionState(
        isAuthenticated: Boolean,
        authUserId: String?,
        licenseResult: LicenseVerificationResult?,
        trialResult: TrialStatusResult?,
        paymentRequests: List<PaymentRequestRecord>,
        isChecking: Boolean = false,
        systemNowMillis: Long = System.currentTimeMillis(),
        elapsedNowMillis: Long = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L),
    ): SubscriptionState {
        val cleanUserId = authUserId?.trim().takeIf { !it.isNullOrBlank() }
        if (!isAuthenticated || cleanUserId == null) {
            return SubscriptionState(
                userId = null,
                plan = SubscriptionPlan.FREE,
                status = EntitlementStatus.UNAUTHENTICATED,
                message = "Login required to verify your account and subscription.",
            )
        }

        val serverAnchorEpoch = licenseResult?.serverEpochMillis ?: trialResult?.serverEpochMillis
        val anchorElapsed = licenseResult?.clientAnchorElapsedMillis ?: trialResult?.clientAnchorElapsedMillis
        val offset = if (serverAnchorEpoch != null) serverAnchorEpoch - systemNowMillis else 0L
        val trustedNow = if (serverAnchorEpoch != null && anchorElapsed != null && elapsedNowMillis >= anchorElapsed) {
            serverAnchorEpoch + (elapsedNowMillis - anchorElapsed)
        } else {
            systemNowMillis + offset
        }

        val userPayments = paymentRequests.filter {
            it.userId.isBlank() || it.userId == cleanUserId
        }
        val approvedReq = userPayments.firstOrNull { it.status == PaymentRequestStatus.APPROVED }
        val pendingReq = userPayments.firstOrNull { it.status == PaymentRequestStatus.PENDING }
        val rejectedReq = userPayments.firstOrNull { it.status == PaymentRequestStatus.REJECTED }

        val validTrial = trialResult?.takeIf { it.userId == null || it.userId == cleanUserId }
        val trialExpiresMs = SubscriptionTimeUtils.parseIsoEpochMillis(validTrial?.trialExpiresAt)
        val hasValidActiveTrial = validTrial?.hasTrial == true &&
            trialExpiresMs != null &&
            trustedNow < trialExpiresMs

        val validLicense = licenseResult?.takeIf { it.userId.isNullOrBlank() || it.userId == cleanUserId }

        if (validLicense != null) {
            when {
                validLicense.status == LicenseStatus.REVOKED -> {
                    return SubscriptionState(
                        userId = cleanUserId,
                        plan = validLicense.plan,
                        status = EntitlementStatus.REVOKED,
                        activatedAt = validLicense.activatedAt,
                        expiresAt = validLicense.expiresAt,
                        trialStartedAt = validTrial?.trialStartedAt,
                        trialExpiresAt = validTrial?.trialExpiresAt,
                        licenseId = validLicense.licenseId,
                        utr = validLicense.utr,
                        paidAmountInr = validLicense.paidAmountInr,
                        isLegacyLifetime = validLicense.isLegacyLifetime,
                        serverClockOffsetMillis = offset,
                        serverAnchorEpochMillis = serverAnchorEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        message = validLicense.message ?: "Your subscription access has been revoked.",
                    )
                }

                validLicense.hasActiveLicense -> {
                    val expiresMs = SubscriptionTimeUtils.parseIsoEpochMillis(validLicense.expiresAt)
                    val resolvedPlan = validLicense.plan
                    val resolvedAmount = approvedReq?.amountInr?.takeIf { it > 0 }
                        ?: validLicense.paidAmountInr
                        ?: if (validLicense.isLegacyLifetime) 499 else resolvedPlan.priceInr

                    val isTimeExpired = resolvedPlan != SubscriptionPlan.LIFETIME &&
                        expiresMs != null &&
                        trustedNow >= expiresMs

                    val resolvedStatus = when {
                        resolvedPlan == SubscriptionPlan.LIFETIME -> EntitlementStatus.LIFETIME_ACTIVE
                        resolvedPlan == SubscriptionPlan.MONTHLY ->
                            if (isTimeExpired) EntitlementStatus.MONTHLY_EXPIRED else EntitlementStatus.MONTHLY_ACTIVE
                        resolvedPlan == SubscriptionPlan.YEARLY ->
                            if (isTimeExpired) EntitlementStatus.YEARLY_EXPIRED else EntitlementStatus.YEARLY_ACTIVE
                        else -> EntitlementStatus.LIFETIME_ACTIVE
                    }

                    return SubscriptionState(
                        userId = cleanUserId,
                        plan = resolvedPlan,
                        status = resolvedStatus,
                        activatedAt = validLicense.activatedAt,
                        expiresAt = validLicense.expiresAt,
                        trialStartedAt = validTrial?.trialStartedAt,
                        trialExpiresAt = validTrial?.trialExpiresAt,
                        paymentId = approvedReq?.id,
                        licenseId = validLicense.licenseId,
                        utr = validLicense.utr ?: approvedReq?.utr,
                        paidAmountInr = resolvedAmount,
                        isLegacyLifetime = validLicense.isLegacyLifetime,
                        serverClockOffsetMillis = offset,
                        serverAnchorEpochMillis = serverAnchorEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        message = when (resolvedStatus) {
                            EntitlementStatus.LIFETIME_ACTIVE -> "Lifetime License Active"
                            EntitlementStatus.MONTHLY_ACTIVE -> "Monthly License Active"
                            EntitlementStatus.YEARLY_ACTIVE -> "Yearly License Active"
                            EntitlementStatus.MONTHLY_EXPIRED -> "Monthly License Expired"
                            EntitlementStatus.YEARLY_EXPIRED -> "Yearly License Expired"
                            else -> validLicense.message
                        },
                    )
                }

                validLicense.status == LicenseStatus.EXPIRED -> {
                    val expPlan = validLicense.plan
                    val expStatus = when (expPlan) {
                        SubscriptionPlan.YEARLY -> EntitlementStatus.YEARLY_EXPIRED
                        else -> EntitlementStatus.MONTHLY_EXPIRED
                    }
                    return SubscriptionState(
                        userId = cleanUserId,
                        plan = expPlan,
                        status = expStatus,
                        activatedAt = validLicense.activatedAt,
                        expiresAt = validLicense.expiresAt,
                        trialStartedAt = validTrial?.trialStartedAt,
                        trialExpiresAt = validTrial?.trialExpiresAt,
                        licenseId = validLicense.licenseId,
                        utr = validLicense.utr,
                        paidAmountInr = validLicense.paidAmountInr,
                        serverClockOffsetMillis = offset,
                        serverAnchorEpochMillis = serverAnchorEpoch,
                        clientAnchorElapsedMillis = anchorElapsed,
                        message = "${expPlan.title} expired. Renew to restore premium access.",
                    )
                }
            }
        }

        if (hasValidActiveTrial) {
            return SubscriptionState(
                userId = cleanUserId,
                plan = SubscriptionPlan.TRIAL,
                status = EntitlementStatus.TRIAL_ACTIVE,
                trialStartedAt = validTrial?.trialStartedAt,
                trialExpiresAt = validTrial?.trialExpiresAt,
                paymentId = pendingReq?.id,
                utr = pendingReq?.utr,
                serverClockOffsetMillis = offset,
                serverAnchorEpochMillis = serverAnchorEpoch,
                clientAnchorElapsedMillis = anchorElapsed,
                message = if (pendingReq != null) {
                    "Free Trial Active • Payment verification pending"
                } else {
                    "2-Day Free Trial Active"
                },
            )
        }

        if (isChecking) {
            return SubscriptionState(
                userId = cleanUserId,
                plan = SubscriptionPlan.FREE,
                status = EntitlementStatus.CHECKING,
                trialStartedAt = validTrial?.trialStartedAt,
                trialExpiresAt = validTrial?.trialExpiresAt,
                isChecking = true,
                serverClockOffsetMillis = offset,
                serverAnchorEpochMillis = serverAnchorEpoch,
                clientAnchorElapsedMillis = anchorElapsed,
                message = "Verifying subscription status...",
            )
        }

        if (validLicense?.status == LicenseStatus.ERROR && validLicense.isNetworkError && pendingReq == null) {
            return SubscriptionState(
                userId = cleanUserId,
                plan = SubscriptionPlan.FREE,
                status = EntitlementStatus.ERROR,
                trialStartedAt = validTrial?.trialStartedAt,
                trialExpiresAt = validTrial?.trialExpiresAt,
                isNetworkError = true,
                serverClockOffsetMillis = offset,
                serverAnchorEpochMillis = serverAnchorEpoch,
                clientAnchorElapsedMillis = anchorElapsed,
                message = validLicense.message ?: "Unable to verify subscription status right now.",
            )
        }

        if (pendingReq != null) {
            val reqPlan = SubscriptionPlan.fromApi(pendingReq.planType, pendingReq.amountInr)
            return SubscriptionState(
                userId = cleanUserId,
                plan = reqPlan,
                status = EntitlementStatus.PAYMENT_PENDING,
                trialStartedAt = validTrial?.trialStartedAt,
                trialExpiresAt = validTrial?.trialExpiresAt,
                paymentId = pendingReq.id,
                utr = pendingReq.utr,
                paidAmountInr = pendingReq.amountInr,
                serverClockOffsetMillis = offset,
                serverAnchorEpochMillis = serverAnchorEpoch,
                clientAnchorElapsedMillis = anchorElapsed,
                message = "${reqPlan.title} payment (₹${pendingReq.amountInr}) is pending admin verification.",
            )
        }

        if (rejectedReq != null) {
            val reqPlan = SubscriptionPlan.fromApi(rejectedReq.planType, rejectedReq.amountInr)
            return SubscriptionState(
                userId = cleanUserId,
                plan = reqPlan,
                status = EntitlementStatus.PAYMENT_REJECTED,
                trialStartedAt = validTrial?.trialStartedAt,
                trialExpiresAt = validTrial?.trialExpiresAt,
                paymentId = rejectedReq.id,
                utr = rejectedReq.utr,
                paidAmountInr = rejectedReq.amountInr,
                serverClockOffsetMillis = offset,
                serverAnchorEpochMillis = serverAnchorEpoch,
                clientAnchorElapsedMillis = anchorElapsed,
                message = "Last payment request was rejected. Please submit a valid UTR.",
            )
        }

        return SubscriptionState(
            userId = cleanUserId,
            plan = SubscriptionPlan.FREE,
            status = EntitlementStatus.TRIAL_EXPIRED,
            trialStartedAt = validTrial?.trialStartedAt,
            trialExpiresAt = validTrial?.trialExpiresAt,
            serverClockOffsetMillis = offset,
            serverAnchorEpochMillis = serverAnchorEpoch,
            clientAnchorElapsedMillis = anchorElapsed,
            message = if (validTrial?.hasTrial == true) {
                "Your 2-day free trial has expired. Upgrade to continue using Pro features."
            } else {
                "Core billing active. Upgrade to Monthly ₹499, Yearly ₹999, or Lifetime ₹2000 for full Pro access."
            },
        )
    }
}
