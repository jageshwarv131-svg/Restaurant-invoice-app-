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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
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

    val statusHeadline = when {
        licenseState.isChecking -> "Verifying Lifetime License..."
        !isAuthenticated -> "Account & Lifetime License Required"
        licenseState.status == LicenseStatus.ERROR -> "License Verification Unavailable"
        pendingPayment != null -> "Payment Under Verification"
        rejectedPayment != null -> "Payment Request Rejected"
        else -> "Lifetime License Required"
    }

    val statusDescription = when {
        licenseState.isChecking ->
            "Connecting to Supabase to verify your active lifetime license for $featureTitle."
        !isAuthenticated ->
            "Login or create an account and activate your ₹499 Lifetime License to unlock $featureTitle, POS Billing, Invoices, Orders, Products, Customers & Reports."
        licenseState.status == LicenseStatus.ERROR ->
            licenseState.message
                ?: "Unable to verify your license right now. Connect to the internet and tap Refresh Status."
        pendingPayment != null ->
            "Your payment (UTR: ${pendingPayment.utr}) is pending manual admin approval. $featureTitle will unlock once your license becomes active."
        rejectedPayment != null ->
            "Your last payment request (UTR: ${rejectedPayment.utr}) was rejected. Please submit a valid ₹499 PhonePe payment UTR to unlock $featureTitle."
        else ->
            "Unlock $featureTitle and all Vyapar Billing Pro business features forever with a one-time ₹499 Lifetime License."
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
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
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
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        Card3D(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .testTag("license_locked_card"),
            cornerRadius = 28.dp,
            shadowDepth = 6.dp,
            contentPadding = PaddingValues(22.dp),
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
                        licenseState.status == LicenseStatus.ERROR -> ClayTones.Red
                        else -> ClayTones.HeroOrange
                    },
                    padSize = 72.dp,
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .raised3D(cornerRadius = 18.dp, tone = ClayTones.HeroOrange, shadowDepth = 3.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vyapar Billing Pro",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "POS Billing • GST Invoices • Orders • Reports",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            text = "₹499",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
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
                        text = "Activate Lifetime License ₹499",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
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
                        text = if (isAuthenticated) "My Account & License Status" else "Login / Create Account",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
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
                                text = "Verifying License...",
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
                                text = "Refresh License Status",
                                color = if (pendingPayment != null) AmberGold else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
