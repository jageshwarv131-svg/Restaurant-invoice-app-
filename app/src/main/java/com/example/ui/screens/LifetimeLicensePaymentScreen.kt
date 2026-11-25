package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SupabasePaymentService
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PaperBackground = Color(0xFFFFFEF8)
private val PaperSectionTint = Color(0xFFF7F4EA)
private val PaperBackdropLight = Color(0xFFF1EDE2)
private val PaperBorder = Color(0xFFD8D0BE)
private val PaperHairline = Color(0xFFE6DFCE)
private val InkPrimary = Color(0xFF1A1F26)
private val InkSecondary = Color(0xFF4F5966)
private val StampGreen = Color(0xFF15803D)
private val StampGreenFill = Color(0xFFDCFCE7)

@Composable
fun LifetimeLicensePaymentScreen(
  isAuthenticated: Boolean,
  authUserId: String?,
  licenseState: LicenseAccessState,
  isSubmitting: Boolean,
  isStatusLoading: Boolean,
  paymentRequests: List<PaymentRequestRecord>,
  submissionMessage: String?,
  errorMessage: String?,
  onOpenAccount: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSubmitPayment: (utr: String, customerNote: String) -> Unit,
  onShowToast: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(enabled = !isSubmitting, onBack = onBack)

  val palette = LocalVyaparClayPalette.current
  val neutralTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

  val isConfirmedActiveLicense =
    isAuthenticated &&
      licenseState.hasActiveLicense &&
      licenseState.status == LicenseStatus.ACTIVE &&
      !licenseState.isChecking &&
      (licenseState.userId.isNullOrBlank() || authUserId.isNullOrBlank() || licenseState.userId == authUserId)

  val isLoadingState =
    isAuthenticated &&
      (licenseState.status == LicenseStatus.CHECKING || (licenseState.isChecking && !isConfirmedActiveLicense))

  val isErrorState =
    isAuthenticated &&
      !isLoadingState &&
      !isConfirmedActiveLicense &&
      licenseState.status == LicenseStatus.ERROR

  val isRefreshingAny = isStatusLoading || licenseState.isChecking

  LaunchedEffect(isAuthenticated, authUserId) {
    if (
      isAuthenticated &&
        !isConfirmedActiveLicense &&
        !licenseState.isChecking &&
        !isStatusLoading
    ) {
      onRefreshStatus()
    }
  }

  LaunchedEffect(errorMessage) {
    if (!errorMessage.isNullOrBlank() && !isConfirmedActiveLicense && !isErrorState) {
      onShowToast(errorMessage)
    }
  }

  val screenBackground =
    if (isConfirmedActiveLicense && !palette.isDark) PaperBackdropLight else palette.background

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(screenBackground)
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top Navigation Header
    Row(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxWidth()
          .raised3D(cornerRadius = 22.dp, shadowDepth = 4.dp)
          .height(56.dp)
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isSubmitting, onClick = onBack)
            .testTag("license_back_btn"),
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
        text = if (isConfirmedActiveLicense) "Lifetime License Agreement" else "Lifetime License",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = palette.textPrimary,
        modifier = Modifier.weight(1f),
      )
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isRefreshingAny && isAuthenticated, onClick = onRefreshStatus)
            .testTag("refresh_payment_status_btn"),
      ) {
        if (isRefreshingAny) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = SaffronOrange,
          )
        } else {
          Icon3D(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh license status",
            tone = ClayTones.Orange,
            size = 22.dp,
          )
        }
      }
    }

    when {
      isLoadingState -> {
        LicenseVerificationLoadingSection()
      }

      isConfirmedActiveLicense -> {
        val approvedUtr =
          paymentRequests.firstOrNull { it.status == PaymentRequestStatus.APPROVED }?.utr
            ?.takeIf { it.isNotBlank() }
            ?: licenseState.utr?.takeIf { it.isNotBlank() }
        val resolvedCustomerId =
          licenseState.userId?.takeIf { it.isNotBlank() } ?: authUserId.orEmpty()

        LifetimeLicenseAgreementDocument(
          customerId = resolvedCustomerId,
          licenseId = licenseState.licenseId,
          activatedAt = licenseState.activatedAt,
          verifiedUtr = approvedUtr,
        )
      }

      isErrorState -> {
        LicenseVerificationErrorSection(
          message =
            licenseState.message
              ?: "Unable to verify your lifetime license with Supabase. Please check your internet connection and try again.",
          isRetrying = isRefreshingAny,
          onRetry = onRefreshStatus,
        )
      }

      else -> {
        LifetimeLicensePaymentFormSection(
          isAuthenticated = isAuthenticated,
          isSubmitting = isSubmitting,
          isRefreshingAny = isRefreshingAny,
          paymentRequests = paymentRequests,
          submissionMessage = submissionMessage,
          errorMessage = errorMessage,
          onOpenAccount = onOpenAccount,
          onRefreshStatus = onRefreshStatus,
          onSubmitPayment = onSubmitPayment,
          onShowToast = onShowToast,
        )
      }
    }

    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun LicenseVerificationLoadingSection() {
  Card3D(
    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("license_loading_card"),
    cornerRadius = 24.dp,
    shadowDepth = 5.dp,
    contentPadding = PaddingValues(28.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      CircularProgressIndicator(
        color = SaffronOrange,
        strokeWidth = 3.dp,
        modifier = Modifier.size(38.dp),
      )
      Text(
        text = "Verifying Lifetime License...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = LocalVyaparClayPalette.current.textPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Checking your active license record with Supabase for this account.",
        style = MaterialTheme.typography.bodySmall,
        color = LocalVyaparClayPalette.current.textSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun LicenseVerificationErrorSection(
  message: String,
  isRetrying: Boolean,
  onRetry: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  Card3D(
    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("license_network_error_card"),
    cornerRadius = 24.dp,
    accentBorderColor = AlertRed,
    shadowDepth = 5.dp,
    contentPadding = PaddingValues(22.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ClayIconPad(
        imageVector = Icons.Filled.CloudOff,
        contentDescription = "Verification Error",
        tone = ClayTones.Red,
        padSize = 58.dp,
        iconSize = 28.dp,
        cornerRadius = 18.dp,
      )
      Text(
        text = "License Verification Failed",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .inset3D(cornerRadius = 14.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = AlertRed,
          textAlign = TextAlign.Center,
        )
      }
      Button3D(
        onClick = onRetry,
        enabled = !isRetrying,
        tone = ClayTones.Orange,
        cornerRadius = 16.dp,
        modifier = Modifier.fillMaxWidth().testTag("license_retry_btn"),
      ) {
        if (isRetrying) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text("Retrying...", color = Color.White, fontWeight = FontWeight.Bold)
        } else {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text("Retry Verification", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
private fun LifetimeLicenseAgreementDocument(
  customerId: String,
  licenseId: String?,
  activatedAt: String?,
  verifiedUtr: String?,
) {
  val formattedActivationDate = remember(activatedAt) { formatActivationDate(activatedAt) }

  Surface(
    modifier =
      Modifier.widthIn(max = 640.dp)
        .fillMaxWidth()
        .testTag("lifetime_license_agreement_screen"),
    shape = RoundedCornerShape(12.dp),
    color = PaperBackground,
    tonalElevation = 0.dp,
    shadowElevation = 4.dp,
    border = BorderStroke(1.dp, PaperBorder),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(10.dp)
          .border(1.dp, PaperHairline, RoundedCornerShape(8.dp))
          .padding(horizontal = 18.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Document Header & Green APPROVED Stamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "VYAPAR BILLING PRO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            color = InkSecondary,
          )
          Text(
            text = "LIFETIME LICENSE AGREEMENT",
            fontFamily = FontFamily.Serif,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
          Text(
            text = "Official Digital License Record",
            fontSize = 12.sp,
            color = InkSecondary,
          )
        }

        Spacer(Modifier.width(12.dp))

        // Green APPROVED Stamp / Seal
        Box(
          modifier =
            Modifier.rotate(-6f)
              .border(2.dp, StampGreen, RoundedCornerShape(10.dp))
              .padding(2.dp)
              .border(1.dp, StampGreen.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
              .background(StampGreenFill.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("license_approved_stamp"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Verified,
              contentDescription = "Approved License Seal",
              tint = StampGreen,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "APPROVED",
              color = StampGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.2.sp,
            )
          }
        }
      }

      HorizontalDivider(thickness = 1.5.dp, color = PaperBorder)

      // Schedule of License Details
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(PaperSectionTint, RoundedCornerShape(8.dp))
            .border(1.dp, PaperHairline, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "LICENSE CERTIFICATE DETAILS",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp,
          color = InkSecondary,
        )
        HorizontalDivider(thickness = 1.dp, color = PaperHairline)

        AgreementDetailRow(
          label = "Status",
          value = "APPROVED",
          valueColor = StampGreen,
          boldValue = true,
        )
        AgreementDetailRow(label = "License Type", value = "Lifetime")
        AgreementDetailRow(
          label = "Customer ID",
          value = customerId.ifBlank { "Unavailable" },
          monospace = true,
        )
        AgreementDetailRow(
          label = "Activation Date",
          value = formattedActivationDate,
        )
        AgreementDetailRow(label = "Payment Amount", value = "₹499")
        AgreementDetailRow(label = "Payment Method", value = "PhonePe")
        if (!verifiedUtr.isNullOrBlank()) {
          AgreementDetailRow(
            label = "Payment Reference",
            value = verifiedUtr,
            monospace = true,
            boldValue = true,
          )
        }
        if (!licenseId.isNullOrBlank()) {
          AgreementDetailRow(
            label = "License ID",
            value = licenseId,
            monospace = true,
          )
        }
      }

      HorizontalDivider(thickness = 1.dp, color = PaperHairline)

      // Legal Terms & Conditions
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AgreementClauseSection(
          number = "1.",
          title = "License Grant & Usage Rights",
          body =
            "Vyapar Billing Pro grants the registered Customer ID identified above a non-exclusive, non-transferable lifetime right to access and operate all Vyapar Billing Pro application modules, including POS billing, GST invoice generation, pending orders, inventory management, customer ledgers, expenses, reports, and offline PDF/backup utilities.",
        )

        AgreementClauseSection(
          number = "2.",
          title = "Single-Account Usage Conditions",
          body =
            "This Lifetime License is issued exclusively for the authenticated Supabase user account shown under Customer ID. Active status is validated directly against the server-side license registry for this account.",
        )

        AgreementClauseSection(
          number = "3.",
          title = "Prohibition on Redistribution, Resale, or Sharing",
          body =
            "You may not redistribute, sublicense, rent, lease, resell, commercially transfer, or share your licensed account credentials with third parties, nor attempt to circumvent, tamper with, or spoof server-side license verification.",
        )

        AgreementClauseSection(
          number = "4.",
          title = "License Validity & Revocation Conditions",
          body =
            "This license remains valid indefinitely so long as the ₹499 payment remains verified and unreversed and these terms are observed. Vyapar Billing Pro reserves the right to suspend or revoke license activation in cases of payment reversal, chargeback, fraudulent UTR submission, unauthorized account sharing, or breach of these conditions; upon revocation, licensed features are automatically restricted.",
        )

        AgreementClauseSection(
          number = "5.",
          title = "Support Information",
          body =
            "For assistance with billing setup, GST invoices, backups, or license verification, please refer to the in-app Help & Support guide (More → Help & Support) and the My Account & License screen inside Vyapar Billing Pro.",
        )
      }

      HorizontalDivider(thickness = 1.dp, color = PaperBorder)

      // Digital Authorization Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "DIGITAL AUTHORIZATION",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = InkSecondary,
          )
          Text(
            text = "Vyapar Billing Pro",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
          Text(
            text = "Server-Verified Electronic License Agreement",
            fontSize = 11.sp,
            color = InkSecondary,
          )
        }
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = "Digital Authorization",
          tint = StampGreen,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

@Composable
private fun AgreementDetailRow(
  label: String,
  value: String,
  valueColor: Color = InkPrimary,
  boldValue: Boolean = false,
  monospace: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = InkSecondary,
      modifier = Modifier.width(126.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = value,
      fontSize = 12.sp,
      fontFamily = if (monospace) FontFamily.Monospace else FontFamily.SansSerif,
      fontWeight = if (boldValue) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = valueColor,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun AgreementClauseSection(
  number: String,
  title: String,
  body: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = "$number $title",
      fontFamily = FontFamily.Serif,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = InkPrimary,
    )
    Text(
      text = body,
      fontSize = 12.5.sp,
      lineHeight = 19.sp,
      color = InkSecondary,
    )
  }
}

internal fun formatActivationDate(raw: String?): String {
  val trimmed = raw?.trim().orEmpty()
  if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
    return "Not specified on license record"
  }
  val formatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
      .withZone(ZoneId.systemDefault())
  return runCatching { formatter.format(OffsetDateTime.parse(trimmed).toInstant()) }
    .recoverCatching { formatter.format(Instant.parse(trimmed)) }
    .getOrElse { trimmed }
}

@Composable
private fun LifetimeLicensePaymentFormSection(
  isAuthenticated: Boolean,
  isSubmitting: Boolean,
  isRefreshingAny: Boolean,
  paymentRequests: List<PaymentRequestRecord>,
  submissionMessage: String?,
  errorMessage: String?,
  onOpenAccount: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSubmitPayment: (utr: String, customerNote: String) -> Unit,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  @Suppress("DEPRECATION")
  val clipboard = LocalClipboardManager.current
  val palette = LocalVyaparClayPalette.current

  var utrInput by remember { mutableStateOf("") }
  var customerNote by remember { mutableStateOf("") }
  var localLaunchError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(submissionMessage) {
    if (submissionMessage == "Payment Under Verification") {
      utrInput = ""
      customerNote = ""
    }
  }

  val approvedRequest = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.APPROVED }
  val pendingRequest = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.PENDING }
  val hasBlockingRequest = approvedRequest != null || pendingRequest != null
  val showVerificationBanner =
    submissionMessage == "Payment Under Verification" || pendingRequest != null

  val liveUtrValidationError =
    remember(utrInput) {
      if (utrInput.isBlank()) null else SupabasePaymentService.validateUtr(utrInput)
    }

  Column(
    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Card: ₹499 Lifetime License
    Card3D(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 26.dp,
      tone = ClayTones.HeroOrange,
      shadowDepth = 6.dp,
      contentPadding = PaddingValues(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "₹499 Lifetime License",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "One-time UPI payment • Manual admin verification",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
          )
        }
        Spacer(Modifier.width(12.dp))
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.raised3D(cornerRadius = 16.dp, shadowDepth = 3.dp)
              .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            text = "₹499",
            color = SaffronOrange,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }

    // Login Required Card if unauthenticated
    if (!isAuthenticated) {
      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("payment_login_required_card"),
        cornerRadius = 22.dp,
        accentBorderColor = SaffronOrange,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Text(
          text = "Login Required to Activate",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "Please sign in or create an account before submitting your payment UTR.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Button3D(
          onClick = onOpenAccount,
          tone = ClayTones.Orange,
          cornerRadius = 16.dp,
          modifier = Modifier.fillMaxWidth().testTag("payment_go_to_login_btn"),
        ) {
          Text(
            text = "Login / Create Account",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }

    if (approvedRequest != null) {
      Card3D(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentBorderColor = AmberGold,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ClayIconPad(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = "Syncing License",
            tone = ClayTones.Amber,
            padSize = 46.dp,
            iconSize = 24.dp,
            cornerRadius = 15.dp,
          )
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Payment Approved — License Pending Activation",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Text(
              text = "Your active license record is not active yet. Tap Refresh to verify with server.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
        }
      }
    } else if (showVerificationBanner) {
      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("payment_under_verification_banner"),
        cornerRadius = 22.dp,
        accentBorderColor = AmberGold,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ClayIconPad(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = "Pending Verification",
            tone = ClayTones.Amber,
            padSize = 46.dp,
            iconSize = 24.dp,
            cornerRadius = 15.dp,
          )
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Payment Under Verification",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            val utrText = pendingRequest?.utr?.takeIf { it.isNotBlank() }
            if (utrText != null) {
              Text(
                text = "Submitted UTR: $utrText",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = SaffronOrange,
              )
            }
            Text(
              text =
                "Your ₹499 payment is pending manual admin verification. License activates once approved.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
        }
      }
    }

    // Step 1: Pay via PhonePe / UPI
    Card3D(
      modifier = Modifier.fillMaxWidth().testTag("payment_step1_card"),
      cornerRadius = 24.dp,
      shadowDepth = 5.dp,
      contentPadding = PaddingValues(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ClayIconPad(
          imageVector = Icons.Filled.AccountBalanceWallet,
          contentDescription = null,
          tone = ClayTones.Purple,
          padSize = 44.dp,
          iconSize = 22.dp,
          cornerRadius = 14.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Step 1: Pay ₹499 via UPI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Text(
            text = "PhonePe / UPI ID: ${SupabasePaymentService.PHONEPE_UPI_ID}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .inset3D(cornerRadius = 16.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("UPI ID", fontSize = 11.sp, color = palette.textSecondary)
          Text(
            text = SupabasePaymentService.PHONEPE_UPI_ID,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            fontSize = 15.sp,
          )
        }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.defaultMinSize(minHeight = 40.dp)
              .raised3D(cornerRadius = 12.dp, shadowDepth = 2.dp)
              .clickable {
                clipboard.setText(AnnotatedString(SupabasePaymentService.PHONEPE_UPI_ID))
                onShowToast("UPI ID copied")
              }
              .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3D(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = "Copy UPI ID",
              tone = ClayTones.Orange,
              size = 16.dp,
            )
            Spacer(Modifier.width(4.dp))
            Text(
              "Copy",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      Button3D(
        onClick = {
          localLaunchError = null
          val upiUri = SupabasePaymentService.buildUpiPaymentUri()
          val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
          val chooser = Intent.createChooser(upiIntent, "Pay ₹499 via UPI")
          runCatching { context.startActivity(chooser) }
            .onFailure {
              localLaunchError =
                "No UPI app found on this device. Pay ₹499 to ${SupabasePaymentService.PHONEPE_UPI_ID} and enter your UTR below."
            }
        },
        enabled = !isSubmitting && approvedRequest == null,
        tone = ClayTones.Purple,
        cornerRadius = 18.dp,
        modifier = Modifier.fillMaxWidth().testTag("pay_with_phonepe_btn"),
      ) {
        Icon(
          imageVector = Icons.Filled.AccountBalanceWallet,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Pay with PhonePe (₹499)",
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
        )
      }
    }

    // Step 2: Enter UTR & Submit
    Card3D(
      modifier = Modifier.fillMaxWidth().testTag("payment_step2_card"),
      cornerRadius = 24.dp,
      shadowDepth = 5.dp,
      contentPadding = PaddingValues(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ClayIconPad(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          tone = ClayTones.Orange,
          padSize = 44.dp,
          iconSize = 22.dp,
          cornerRadius = 14.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column {
          Text(
            text = "Step 2: Submit Payment UTR",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Text(
            text = "Enter the UPI Reference / UTR number after paying ₹499",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      TextField3D(
        value = utrInput,
        onValueChange = { raw ->
          utrInput = raw.filter { !it.isWhitespace() }.take(35)
        },
        label = "UTR / Transaction Reference Number *",
        placeholder = "e.g. 432912345678",
        isError = liveUtrValidationError != null,
        supportingText = liveUtrValidationError?.let { msg -> { Text(msg) } },
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
          ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("utr_input"),
      )

      Spacer(Modifier.height(10.dp))

      TextField3D(
        value = customerNote,
        onValueChange = { customerNote = it.take(150) },
        label = "Shop Name / Note (Optional)",
        placeholder = "Optional note for verification",
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("customer_note_input"),
      )

      Spacer(Modifier.height(12.dp))

      val displayError = localLaunchError ?: errorMessage
      if (!displayError.isNullOrBlank()) {
        Card3D(
          modifier = Modifier.fillMaxWidth().testTag("payment_error_card"),
          cornerRadius = 18.dp,
          accentBorderColor = AlertRed,
          shadowDepth = 3.dp,
          contentPadding = PaddingValues(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3D(
              imageVector = Icons.Filled.ErrorOutline,
              contentDescription = "Error",
              tone = ClayTones.Red,
              size = 24.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
              text = displayError,
              color = AlertRed,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Spacer(Modifier.height(12.dp))
      }

      Button3D(
        onClick = {
          localLaunchError = null
          onSubmitPayment(utrInput, customerNote)
        },
        enabled = isAuthenticated && !isSubmitting && !hasBlockingRequest && utrInput.isNotBlank(),
        tone = ClayTones.Orange,
        cornerRadius = 18.dp,
        modifier = Modifier.fillMaxWidth().testTag("submit_payment_btn"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(10.dp))
          Text("Submitting...", color = Color.White, fontWeight = FontWeight.Bold)
        } else {
          Text(
            text =
              when {
                !isAuthenticated -> "Login First to Submit UTR"
                approvedRequest != null -> "Payment Approved — Tap Refresh"
                pendingRequest != null -> "Verification Pending"
                else -> "Submit Payment Request"
              },
            color =
              if (isAuthenticated && !hasBlockingRequest && utrInput.isNotBlank()) Color.White
              else palette.textSecondary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
          )
        }
      }
    }

    // Payment Status History (Pending, Approved, Rejected)
    Card3D(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      shadowDepth = 5.dp,
      contentPadding = PaddingValues(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Payment Request Status",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )
        Box(
          modifier =
            Modifier.defaultMinSize(minHeight = 36.dp)
              .raised3D(cornerRadius = 12.dp, shadowDepth = 2.dp)
              .clickable(enabled = !isRefreshingAny && isAuthenticated, onClick = onRefreshStatus)
              .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isRefreshingAny) "Checking..." else "Refresh",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SaffronOrange,
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      if (paymentRequests.isEmpty()) {
        Text(
          text =
            "No payment requests submitted yet. Pay ₹499 via PhonePe and submit your UTR above.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          paymentRequests.forEach { req ->
            val statusColor =
              when (req.status) {
                PaymentRequestStatus.PENDING -> AmberGold
                PaymentRequestStatus.APPROVED -> EmeraldGreen
                PaymentRequestStatus.REJECTED -> AlertRed
              }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .inset3D(cornerRadius = 16.dp)
                  .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "UTR: ${req.utr}",
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                  Box(
                    modifier =
                      Modifier.raised3D(
                        cornerRadius = 50.dp,
                        tone =
                          when (req.status) {
                            PaymentRequestStatus.PENDING -> ClayTones.Amber
                            PaymentRequestStatus.APPROVED -> ClayTones.Green
                            PaymentRequestStatus.REJECTED -> ClayTones.Red
                          },
                        shadowDepth = 2.dp,
                      )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = req.status.label,
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                Text(
                  text = "₹${req.amountInr} • ${req.paymentMethod}",
                  style = MaterialTheme.typography.bodySmall,
                  color = statusColor,
                  fontWeight = FontWeight.SemiBold,
                )
                if (req.customerNote.isNotBlank()) {
                  Text(
                    text = "Note: ${req.customerNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                if (req.createdAt.isNotBlank()) {
                  Text(
                    text = "Submitted: ${req.createdAt.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
