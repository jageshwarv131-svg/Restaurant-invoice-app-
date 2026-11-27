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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.example.data.SubscriptionTimeUtils
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
  subscriptionState: SubscriptionState? = null,
  trialRemainingMillis: Long = 0L,
  selectedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
  onSelectPlan: (SubscriptionPlan) -> Unit = {},
  onSubmitPlanPayment: ((SubscriptionPlan, String, String) -> Unit)? = null,
) {
  BackHandler(enabled = !isSubmitting, onBack = onBack)

  val palette = LocalVyaparClayPalette.current
  val neutralTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

  val isUserMatched =
    licenseState.userId.isNullOrBlank() || authUserId.isNullOrBlank() || licenseState.userId == authUserId

  val isConfirmedActiveLicense =
    isAuthenticated &&
      licenseState.hasActiveLicense &&
      licenseState.status == LicenseStatus.ACTIVE &&
      !licenseState.isChecking &&
      isUserMatched

  val activePlan = when {
    subscriptionState?.status == EntitlementStatus.LIFETIME_ACTIVE -> SubscriptionPlan.LIFETIME
    subscriptionState?.status == EntitlementStatus.MONTHLY_ACTIVE -> SubscriptionPlan.MONTHLY
    subscriptionState?.status == EntitlementStatus.YEARLY_ACTIVE -> SubscriptionPlan.YEARLY
    isConfirmedActiveLicense && (licenseState.plan == SubscriptionPlan.MONTHLY || licenseState.plan == SubscriptionPlan.YEARLY) ->
      licenseState.plan
    isConfirmedActiveLicense -> SubscriptionPlan.LIFETIME
    else -> SubscriptionPlan.FREE
  }

  val isLifetimeApproved = isConfirmedActiveLicense && activePlan == SubscriptionPlan.LIFETIME

  val isLoadingState =
    isAuthenticated &&
      (licenseState.status == LicenseStatus.CHECKING || (licenseState.isChecking && !isConfirmedActiveLicense))

  val isErrorState =
    isAuthenticated &&
      !isLoadingState &&
      !isConfirmedActiveLicense &&
      subscriptionState?.status != EntitlementStatus.TRIAL_ACTIVE &&
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
    if (!errorMessage.isNullOrBlank() && !isLifetimeApproved && !isErrorState) {
      onShowToast(errorMessage)
    }
  }

  val screenBackground =
    if (isLifetimeApproved && !palette.isDark) PaperBackdropLight else palette.background

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
          .defaultMinSize(minHeight = 56.dp)
          .padding(horizontal = 8.dp, vertical = 4.dp),
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
        text = if (isLifetimeApproved) "Lifetime License Agreement" else "Plans & License",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = palette.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
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

      isLifetimeApproved -> {
        val approvedReq = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.APPROVED }
        val approvedUtr =
          approvedReq?.utr?.takeIf { it.isNotBlank() }
            ?: licenseState.utr?.takeIf { it.isNotBlank() }
        val resolvedCustomerId =
          licenseState.userId?.takeIf { it.isNotBlank() } ?: authUserId.orEmpty()
        val resolvedAmountInr =
          subscriptionState?.displayPaidAmountInr
            ?: approvedReq?.amountInr?.takeIf { it > 0 }
            ?: licenseState.paidAmountInr
            ?: 499

        LifetimeLicenseAgreementDocument(
          customerId = resolvedCustomerId,
          licenseId = licenseState.licenseId,
          activatedAt = licenseState.activatedAt,
          verifiedUtr = approvedUtr,
          paidAmountInr = resolvedAmountInr,
        )
      }

      isErrorState -> {
        LicenseVerificationErrorSection(
          message =
            licenseState.message
              ?: "Unable to verify your subscription with the server. Please check your internet connection and try again.",
          isRetrying = isRefreshingAny,
          onRetry = onRefreshStatus,
        )
      }

      else -> {
        LifetimeLicensePaymentFormSection(
          isAuthenticated = isAuthenticated,
          isSubmitting = isSubmitting,
          isRefreshingAny = isRefreshingAny,
          licenseState = licenseState,
          subscriptionState = subscriptionState,
          trialRemainingMillis = trialRemainingMillis,
          activePlan = activePlan,
          selectedPlan = selectedPlan,
          onSelectPlan = onSelectPlan,
          paymentRequests = paymentRequests,
          submissionMessage = submissionMessage,
          errorMessage = errorMessage,
          onOpenAccount = onOpenAccount,
          onRefreshStatus = onRefreshStatus,
          onSubmitPlanPayment = { plan, utr, note ->
            if (onSubmitPlanPayment != null) {
              onSubmitPlanPayment(plan, utr, note)
            } else {
              onSubmitPayment(utr, note)
            }
          },
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
        text = "Verifying Subscription...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = LocalVyaparClayPalette.current.textPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Checking your active plan and free trial status on our secure cloud server.",
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
        text = "Subscription Verification Failed",
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
  paidAmountInr: Int = 499,
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
        AgreementDetailRow(label = "Payment Amount", value = "₹$paidAmountInr")
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
            "This Lifetime License is issued exclusively for the authenticated Vyapar Billing Pro user account shown under Customer ID. Active status is validated directly against the server-side license registry for this account.",
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
            "This license remains valid indefinitely so long as the ₹$paidAmountInr payment remains verified and unreversed and these terms are observed. Vyapar Billing Pro reserves the right to suspend or revoke license activation in cases of payment reversal, chargeback, fraudulent UTR submission, unauthorized account sharing, or breach of these conditions; upon revocation, licensed features are automatically restricted.",
        )

        AgreementClauseSection(
          number = "5.",
          title = "Support Information",
          body =
            "For assistance with billing setup, GST invoices, backups, or license verification, please refer to the in-app Help & Support guide (More → Help & Support) and the My Account & License screen inside Vyapar Billing Pro.",
        )
      }

      HorizontalDivider(thickness = 1.dp, color = PaperBorder)

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
  licenseState: LicenseAccessState,
  subscriptionState: SubscriptionState?,
  trialRemainingMillis: Long,
  activePlan: SubscriptionPlan,
  selectedPlan: SubscriptionPlan,
  onSelectPlan: (SubscriptionPlan) -> Unit,
  paymentRequests: List<PaymentRequestRecord>,
  submissionMessage: String?,
  errorMessage: String?,
  onOpenAccount: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSubmitPlanPayment: (plan: SubscriptionPlan, utr: String, customerNote: String) -> Unit,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  @Suppress("DEPRECATION")
  val clipboard = LocalClipboardManager.current
  val palette = LocalVyaparClayPalette.current

  var activeSelectedPlan by remember(selectedPlan) {
    mutableStateOf(if (selectedPlan.isPurchasable) selectedPlan else SubscriptionPlan.MONTHLY)
  }
  var utrInput by remember { mutableStateOf("") }
  var customerNote by remember { mutableStateOf("") }
  var localLaunchError by remember { mutableStateOf<String?>(null) }
  var showRenewalForm by remember { mutableStateOf(false) }

  LaunchedEffect(submissionMessage) {
    if (submissionMessage == "Payment Under Verification") {
      utrInput = ""
      customerNote = ""
      showRenewalForm = false
    }
  }

  val approvedRequest = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.APPROVED }
  val pendingRequest = paymentRequests.firstOrNull { it.status == PaymentRequestStatus.PENDING }
  val isCurrentlyPaidActive =
    activePlan == SubscriptionPlan.MONTHLY || activePlan == SubscriptionPlan.YEARLY
  val hasBlockingRequest = pendingRequest != null
  val showVerificationBanner =
    submissionMessage == "Payment Under Verification" || pendingRequest != null

  val liveUtrValidationError =
    remember(utrInput) {
      if (utrInput.isBlank()) null else SupabasePaymentService.validateUtr(utrInput)
    }

  val subStatus = subscriptionState?.status
  val effectiveTrialRemaining =
    if (trialRemainingMillis > 0L) trialRemainingMillis
    else subscriptionState?.trialRemainingMillis() ?: 0L
  val planRemainingMillis = subscriptionState?.planRemainingMillis() ?: 0L

  val currentPlanLabel = when (subStatus) {
    EntitlementStatus.TRIAL_ACTIVE -> "Free Trial"
    EntitlementStatus.MONTHLY_ACTIVE, EntitlementStatus.MONTHLY_EXPIRED -> "Monthly Plan"
    EntitlementStatus.YEARLY_ACTIVE, EntitlementStatus.YEARLY_EXPIRED -> "Yearly Plan"
    EntitlementStatus.LIFETIME_ACTIVE -> "Legacy Lifetime"
    else -> if (subscriptionState?.hasActiveTrial == true) "Free Trial" else "Free Core Plan"
  }

  val licenseStatusLabel = when (subStatus) {
    EntitlementStatus.TRIAL_ACTIVE -> "Trial Active"
    EntitlementStatus.PAYMENT_PENDING -> "Payment Pending"
    EntitlementStatus.MONTHLY_ACTIVE,
    EntitlementStatus.YEARLY_ACTIVE,
    EntitlementStatus.LIFETIME_ACTIVE -> "Active"
    EntitlementStatus.TRIAL_EXPIRED,
    EntitlementStatus.MONTHLY_EXPIRED,
    EntitlementStatus.YEARLY_EXPIRED -> "Expired"
    EntitlementStatus.REVOKED -> "Revoked"
    EntitlementStatus.PAYMENT_REJECTED -> "Payment Rejected"
    else -> "Inactive"
  }

  val heroTone = when (subStatus) {
    EntitlementStatus.MONTHLY_ACTIVE, EntitlementStatus.YEARLY_ACTIVE -> ClayTones.Green
    EntitlementStatus.TRIAL_ACTIVE -> ClayTones.HeroOrange
    EntitlementStatus.TRIAL_EXPIRED,
    EntitlementStatus.MONTHLY_EXPIRED,
    EntitlementStatus.YEARLY_EXPIRED,
    EntitlementStatus.REVOKED -> ClayTones.Red
    EntitlementStatus.PAYMENT_PENDING -> ClayTones.Amber
    else -> ClayTones.HeroOrange
  }

  val heroTitle = when (subStatus) {
    EntitlementStatus.MONTHLY_ACTIVE -> "Monthly Plan Active"
    EntitlementStatus.YEARLY_ACTIVE -> "Yearly Plan Active"
    EntitlementStatus.TRIAL_ACTIVE -> "48-Hour Free Trial Active"
    EntitlementStatus.TRIAL_EXPIRED -> "Free Trial Expired"
    EntitlementStatus.MONTHLY_EXPIRED -> "Monthly Plan Expired"
    EntitlementStatus.YEARLY_EXPIRED -> "Yearly Plan Expired"
    EntitlementStatus.PAYMENT_PENDING -> "Payment Pending Verification"
    EntitlementStatus.PAYMENT_REJECTED -> "Previous Payment Rejected"
    EntitlementStatus.REVOKED -> "Subscription Revoked"
    else -> "Subscription Plans"
  }

  val heroSubtitle = when (subStatus) {
    EntitlementStatus.MONTHLY_ACTIVE, EntitlementStatus.YEARLY_ACTIVE -> {
      val exp = subscriptionState?.expiresAt ?: licenseState.expiresAt
      if (!exp.isNullOrBlank()) {
        val remText = SubscriptionTimeUtils.formatRemainingDaysHours(planRemainingMillis)
        "Valid until ${SubscriptionTimeUtils.formatReadableDateTime(exp)} • $remText"
      } else {
        "Active paid subscription verified"
      }
    }
    EntitlementStatus.TRIAL_ACTIVE -> {
      val countdown = SubscriptionTimeUtils.formatRemainingDaysHours(effectiveTrialRemaining)
      val expFormatted = SubscriptionTimeUtils.formatReadableDateTime(subscriptionState?.trialExpiresAt)
      "$countdown • Auto-expires $expFormatted"
    }
    EntitlementStatus.TRIAL_EXPIRED ->
      SubscriptionTimeUtils.TRIAL_EXPIRED_USER_MESSAGE
    EntitlementStatus.MONTHLY_EXPIRED, EntitlementStatus.YEARLY_EXPIRED ->
      "Your subscription has expired. Choose a plan below to renew."
    else -> "Choose Monthly (₹499 / Month) or Yearly (₹1,500 / Year)"
  }

  Column(
    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Current Plan Hero Status Card
    Card3D(
      modifier = Modifier.fillMaxWidth().testTag("subscription_hero_card"),
      cornerRadius = 24.dp,
      tone = heroTone,
      shadowDepth = 5.dp,
      contentPadding = PaddingValues(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = heroTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = heroSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.94f),
          )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          tonalElevation = 0.dp,
        ) {
          Text(
            text = when {
              subStatus == EntitlementStatus.TRIAL_ACTIVE ->
                SubscriptionTimeUtils.formatCountdown(effectiveTrialRemaining)
              isCurrentlyPaidActive -> "ACTIVE"
              else -> activeSelectedPlan.formattedPrice
            },
            color = if (isCurrentlyPaidActive) EmeraldGreen else SaffronOrange,
            fontSize = if (subStatus == EntitlementStatus.TRIAL_ACTIVE) 12.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }
    }

    // Account & License Details Card
    if (isAuthenticated) {
      val displayActivation = when (subStatus) {
        EntitlementStatus.TRIAL_ACTIVE, EntitlementStatus.TRIAL_EXPIRED -> subscriptionState?.trialStartedAt
        else -> subscriptionState?.activatedAt ?: licenseState.activatedAt
      }
      val displayExpiry = when (subStatus) {
        EntitlementStatus.TRIAL_ACTIVE, EntitlementStatus.TRIAL_EXPIRED -> subscriptionState?.trialExpiresAt
        else -> subscriptionState?.expiresAt ?: licenseState.expiresAt
      }
      val remainingLabel = when {
        subStatus == EntitlementStatus.TRIAL_ACTIVE ->
          SubscriptionTimeUtils.formatRemainingDaysHours(effectiveTrialRemaining)
        isCurrentlyPaidActive && planRemainingMillis > 0L ->
          SubscriptionTimeUtils.formatRemainingDaysHours(planRemainingMillis)
        subStatus == EntitlementStatus.TRIAL_EXPIRED ||
          subStatus == EntitlementStatus.MONTHLY_EXPIRED ||
          subStatus == EntitlementStatus.YEARLY_EXPIRED -> "Expired"
        else -> null
      }

      Surface(
        modifier = Modifier.fillMaxWidth().testTag("active_timed_plan_card"),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(
          width = 1.dp,
          color = if (isCurrentlyPaidActive) EmeraldGreen.copy(alpha = 0.55f)
          else palette.glossBorderBottom.copy(alpha = 0.45f),
        ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "License Status Details",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Surface(
              shape = RoundedCornerShape(50.dp),
              color = when {
                isCurrentlyPaidActive -> EmeraldGreen.copy(alpha = 0.14f)
                subStatus == EntitlementStatus.TRIAL_ACTIVE -> SaffronOrange.copy(alpha = 0.14f)
                subStatus == EntitlementStatus.PAYMENT_PENDING -> AmberGold.copy(alpha = 0.18f)
                else -> AlertRed.copy(alpha = 0.14f)
              },
            ) {
              Text(
                text = licenseStatusLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                  isCurrentlyPaidActive -> EmeraldGreen
                  subStatus == EntitlementStatus.TRIAL_ACTIVE -> SaffronOrange
                  subStatus == EntitlementStatus.PAYMENT_PENDING -> AmberGold
                  else -> AlertRed
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.28f))

          LicenseMetaRow(label = "Current Plan", value = currentPlanLabel)
          LicenseMetaRow(label = "License Status", value = licenseStatusLabel)
          if (!displayActivation.isNullOrBlank()) {
            LicenseMetaRow(
              label = "Activation Date",
              value = SubscriptionTimeUtils.formatReadableDateTime(displayActivation),
            )
          }
          if (!displayExpiry.isNullOrBlank()) {
            LicenseMetaRow(
              label = "Expiry Date",
              value = SubscriptionTimeUtils.formatReadableDateTime(displayExpiry),
            )
          }
          if (!remainingLabel.isNullOrBlank()) {
            LicenseMetaRow(
              label = "Remaining Validity",
              value = remainingLabel,
              valueHighlight = if (remainingLabel == "Expired") AlertRed else EmeraldGreen,
            )
          }

          if (subStatus == EntitlementStatus.TRIAL_EXPIRED) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AlertRed.copy(alpha = 0.08f))
                .padding(12.dp)
                .testTag("trial_expired_notice"),
            ) {
              Text(
                text = SubscriptionTimeUtils.TRIAL_EXPIRED_USER_MESSAGE,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = AlertRed,
              )
            }
          }

          if (isCurrentlyPaidActive) {
            Spacer(Modifier.height(4.dp))
            Button3D(
              onClick = { showRenewalForm = !showRenewalForm },
              isPrimary = !showRenewalForm,
              tone = ClayTones.Orange,
              cornerRadius = 14.dp,
              modifier = Modifier.fillMaxWidth().testTag("renew_subscription_btn"),
            ) {
              Text(
                text = if (showRenewalForm) "Hide Renewal Options" else "Renew / Extend Subscription",
                color = if (!showRenewalForm) Color.White else palette.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    }

    // Login Required Card if unauthenticated
    if (!isAuthenticated) {
      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("payment_login_required_card"),
        cornerRadius = 22.dp,
        accentBorderColor = SaffronOrange,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Text(
          text = "Login Required to Subscribe",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "Please sign in or create an account before selecting a plan and submitting your payment UTR.",
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

    if (approvedRequest != null && !isCurrentlyPaidActive &&
      subStatus != EntitlementStatus.MONTHLY_EXPIRED &&
      subStatus != EntitlementStatus.YEARLY_EXPIRED
    ) {
      Card3D(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentBorderColor = AmberGold,
        shadowDepth = 4.dp,
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
              text = "Payment Approved — Syncing License",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Text(
              text = "Tap Refresh above to verify your active subscription with the server.",
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
        shadowDepth = 4.dp,
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
            val reqAmount = pendingRequest?.amountInr ?: activeSelectedPlan.priceInr
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
                "Your ${SubscriptionTimeUtils.formatPriceInr(reqAmount)} payment is pending manual admin verification. Subscription activates once approved.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
        }
      }
    }

    val shouldShowPlanAndPaymentSection = !isCurrentlyPaidActive || showRenewalForm

    if (shouldShowPlanAndPaymentSection) {
      if (isCurrentlyPaidActive && showRenewalForm) {
        val currentExp = subscriptionState?.expiresAt ?: licenseState.expiresAt
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("renewal_extension_notice"),
          shape = RoundedCornerShape(16.dp),
          color = EmeraldGreen.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
        ) {
          Text(
            text = if (!currentExp.isNullOrBlank()) {
              "Renewing now extends your validity directly from your current expiry (${SubscriptionTimeUtils.formatReadableDateTime(currentExp)}). No paid days are wasted."
            } else {
              "Renewing now extends your active subscription period cleanly."
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      // Two Separate Subscription Plan Cards (Monthly ₹499 / Yearly ₹1,500)
      Column(
        modifier = Modifier.fillMaxWidth().testTag("plan_selector_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = if (isCurrentlyPaidActive) "Choose Renewal Plan" else "Choose Subscription Plan",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )

        SubscriptionPlan.purchasablePlans.forEach { plan ->
          val isSelected = activeSelectedPlan == plan
          val borderColor = if (isSelected) SaffronOrange else palette.glossBorderBottom.copy(alpha = 0.55f)
          val borderWidth = if (isSelected) 1.75.dp else 1.dp

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("plan_card_${plan.apiValue}")
              .clickable {
                activeSelectedPlan = plan
                onSelectPlan(plan)
              },
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) palette.surfaceElevated else palette.surface,
            tonalElevation = if (isSelected) 2.dp else 0.dp,
            border = BorderStroke(borderWidth, borderColor),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = plan.featureLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SaffronOrange,
                  )
                  Text(
                    text = "Validity: ${plan.durationLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = plan.displayPriceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.End,
                  )
                  if (!plan.badge.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = SaffronOrange.copy(alpha = 0.12f),
                    ) {
                      Text(
                        text = plan.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  }
                }
              }

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    activeSelectedPlan = plan
                    onSelectPlan(plan)
                  }
                  .testTag("select_plan_btn_${plan.apiValue}"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) SaffronOrange else palette.insetBg,
                border = if (isSelected) null else BorderStroke(1.dp, palette.glossBorderBottom.copy(alpha = 0.4f)),
              ) {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = if (isSelected) "Selected • ${plan.displayPriceLabel}" else "Select Plan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Color.White else palette.textPrimary,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          }
        }
      }

      // Step 1: Pay via PhonePe / UPI
      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("payment_step1_card"),
        cornerRadius = 24.dp,
        shadowDepth = 4.dp,
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
              text = "Step 1: Pay ${activeSelectedPlan.formattedPrice} via UPI",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Text(
              text = "${activeSelectedPlan.title} (${activeSelectedPlan.durationLabel}) • ${SupabasePaymentService.PHONEPE_UPI_ID}",
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
          Column(modifier = Modifier.weight(1f)) {
            Text("PhonePe / UPI ID", fontSize = 11.sp, color = palette.textSecondary)
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
            val upiUri = SupabasePaymentService.buildUpiPaymentUri(
              amountInr = activeSelectedPlan.priceInr,
              note = "Vyapar Billing Pro ${activeSelectedPlan.title}",
            )
            val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
            val chooser = Intent.createChooser(
              upiIntent,
              "Pay ${activeSelectedPlan.formattedPrice} via UPI",
            )
            runCatching { context.startActivity(chooser) }
              .onFailure {
                localLaunchError =
                  "No UPI app found on this device. Pay ${activeSelectedPlan.formattedPrice} to ${SupabasePaymentService.PHONEPE_UPI_ID} and enter your UTR below."
              }
          },
          enabled = !isSubmitting && !hasBlockingRequest,
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
            text = "Pay with PhonePe (${activeSelectedPlan.formattedPrice})",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
          )
        }
      }

      // Step 2: Enter UTR & Submit
      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("payment_step2_card"),
        cornerRadius = 24.dp,
        shadowDepth = 4.dp,
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
              text = "Enter the UPI Reference / UTR after paying ${activeSelectedPlan.formattedPrice} for ${activeSelectedPlan.title}",
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
            onSubmitPlanPayment(activeSelectedPlan, utrInput, customerNote)
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
                  pendingRequest != null -> "Verification Pending"
                  isCurrentlyPaidActive -> "Submit ${activeSelectedPlan.formattedPrice} Renewal Request"
                  else -> "Submit ${activeSelectedPlan.formattedPrice} Payment Request"
                },
              color =
                if (isAuthenticated && !hasBlockingRequest && utrInput.isNotBlank()) Color.White
                else palette.textSecondary,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    // Payment Status History
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
          text = "Payment Request History",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
          modifier = Modifier.weight(1f),
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
            "No payment requests submitted yet. Choose a plan, pay via PhonePe, and submit your UTR above.",
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
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(Modifier.width(8.dp))
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
                  text = "${req.resolvedPlan.title} • ${SubscriptionTimeUtils.formatPriceInr(req.amountInr)} • ${req.paymentMethod}",
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

@Composable
private fun LicenseMetaRow(
  label: String,
  value: String,
  valueHighlight: Color? = null,
) {
  val palette = LocalVyaparClayPalette.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = palette.textSecondary,
      modifier = Modifier.weight(0.44f),
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = valueHighlight ?: palette.textPrimary,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(0.56f),
    )
  }
}

