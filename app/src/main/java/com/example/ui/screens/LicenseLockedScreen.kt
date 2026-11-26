package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EntitlementStatus
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberGold
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange

@Composable
fun LicenseLockedScreen(
    featureTitle: String,
    isAuthenticated: Boolean,
    licenseState: LicenseAccessState,
    paymentRequests: List<PaymentRequestRecord>,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onActivateLicense: () -> Unit,
    onOpenAccount: () -> Unit,
    onRefreshLicense: () -> Unit,
    subscriptionState: SubscriptionState? = null,
    reasonTitle: String? = null,
    reasonMessage: String? = null,
    onSelectPlanAndUpgrade: ((SubscriptionPlan) -> Unit)? = null,
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    val palette = LocalVyaparClayPalette.current
    val neutralTone = remember(palette.isDark) {
        ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
    }

    val pendingPayment = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.PENDING }
    val rejectedPayment = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.REJECTED }
    val subStatus = subscriptionState?.status

    val statusHeadline = when {
        licenseState.isChecking -> "Verifying Subscription..."
        !isAuthenticated -> "Account & License Required"
        licenseState.status == LicenseStatus.ERROR -> "Subscription Verification Unavailable"
        subStatus == EntitlementStatus.REVOKED || licenseState.status == LicenseStatus.REVOKED ->
            "License Access Revoked"
        !reasonTitle.isNullOrBlank() -> reasonTitle
        pendingPayment != null -> "Payment Under Verification"
        rejectedPayment != null -> "Payment Request Rejected"
        subStatus == EntitlementStatus.MONTHLY_EXPIRED || subStatus == EntitlementStatus.YEARLY_EXPIRED ->
            "Subscription Expired"
        subStatus == EntitlementStatus.TRIAL_EXPIRED -> "Free Trial Expired"
        else -> "Pro Plan Required"
    }

    val statusDescription = when {
        licenseState.isChecking ->
            "Connecting to secure cloud server to verify your subscription for $featureTitle."
        !isAuthenticated ->
            "Login or create an account to start your 2-day free trial or activate Monthly (₹499), Yearly (₹999), or Lifetime (₹2000) to unlock $featureTitle."
        licenseState.status == LicenseStatus.ERROR ->
            licenseState.message
                ?: "Unable to verify your subscription right now. Connect to the internet and tap Refresh Status."
        !reasonMessage.isNullOrBlank() -> reasonMessage
        pendingPayment != null ->
            "Your ${pendingPayment.resolvedPlan.title} payment (UTR: ${pendingPayment.utr}) is pending admin approval. $featureTitle unlocks once approved."
        rejectedPayment != null ->
            "Your last payment request (UTR: ${rejectedPayment.utr}) was rejected. Please submit a valid PhonePe payment UTR to unlock $featureTitle."
        else ->
            "Unlock $featureTitle and all Vyapar Billing Pro tools with Monthly ₹499, Yearly ₹999, or Lifetime ₹2000. Your core billing & saved records stay safe."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .then(
                if (showBackButton) Modifier.statusBarsPadding().navigationBarsPadding()
                else Modifier
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showBackButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .raised3D(cornerRadius = 22.dp, shadowDepth = 4.dp)
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack)
                        .testTag("locked_back_btn"),
                ) {
                    Icon3D(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tone = neutralTone,
                        size = 24.dp,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = featureTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        Card3D(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .testTag("license_locked_card"),
            cornerRadius = 28.dp,
            shadowDepth = 6.dp,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClayIconPad(
                    imageVector = when {
                        pendingPayment != null -> Icons.Filled.HourglassTop
                        else -> Icons.Filled.Lock
                    },
                    contentDescription = "Locked Feature",
                    tone = when {
                        pendingPayment != null -> ClayTones.Amber
                        licenseState.status == LicenseStatus.ERROR ||
                            subStatus == EntitlementStatus.REVOKED -> ClayTones.Red
                        else -> ClayTones.HeroOrange
                    },
                    padSize = 70.dp,
                    iconSize = 34.dp,
                    cornerRadius = 22.dp,
                )

                Text(
                    text = statusHeadline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .inset3D(cornerRadius = 14.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (licenseState.status == LicenseStatus.ERROR) AlertRed else palette.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                // Quick Plan Options Summary (Monthly ₹499 / Yearly ₹999 / Lifetime ₹2000)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubscriptionPlan.purchasablePlans.forEach { plan ->
                        val isBest = plan == SubscriptionPlan.YEARLY
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .raised3D(
                                    cornerRadius = 16.dp,
                                    tone = if (isBest) ClayTones.HeroOrange else null,
                                    shadowDepth = 3.dp,
                                )
                                .clickable {
                                    if (onSelectPlanAndUpgrade != null) {
                                        onSelectPlanAndUpgrade(plan)
                                    } else {
                                        onActivateLicense()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = when (plan) {
                                    SubscriptionPlan.MONTHLY -> "Monthly"
                                    SubscriptionPlan.YEARLY -> "Yearly"
                                    SubscriptionPlan.LIFETIME -> "Lifetime"
                                    else -> plan.title
                                },
                                color = if (isBest) Color.White else palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "₹${plan.priceInr}",
                                color = if (isBest) Color.White else SaffronOrange,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = plan.durationLabel,
                                color = if (isBest) Color.White.copy(alpha = 0.88f) else palette.textSecondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Button3D(
                    onClick = onActivateLicense,
                    tone = ClayTones.Orange,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("locked_activate_license_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "View Plans & Upgrade",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Button3D(
                    onClick = onOpenAccount,
                    isPrimary = false,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("locked_account_btn"),
                ) {
                    Icon3D(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tone = ClayTones.Blue,
                        size = 18.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isAuthenticated) "My Account & Plan Status" else "Login / Create Account",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                if (isAuthenticated) {
                    Button3D(
                        onClick = onRefreshLicense,
                        enabled = !licenseState.isChecking,
                        isPrimary = false,
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("locked_refresh_license_btn"),
                    ) {
                        if (licenseState.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SaffronOrange,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Verifying Subscription...",
                                color = SaffronOrange,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Icon3D(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tone = ClayTones.Green,
                                size = 18.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Refresh Subscription Status",
                                color = if (pendingPayment != null) AmberGold else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
