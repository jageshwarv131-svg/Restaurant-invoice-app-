package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun LifetimeLicensePaymentScreen(
  isSubmitting: Boolean,
  isStatusLoading: Boolean,
  paymentRequests: List<PaymentRequestRecord>,
  submissionMessage: String?,
  errorMessage: String?,
  onRefreshStatus: () -> Unit,
  onSubmitPayment: (utr: String, customerNote: String) -> Unit,
  onShowToast: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(enabled = !isSubmitting, onBack = onBack)

  val context = LocalContext.current
  @Suppress("DEPRECATION")
  val clipboard = LocalClipboardManager.current
  val palette = LocalVyaparClayPalette.current
  val neutralTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

  var utrInput by remember { mutableStateOf("") }
  var customerNote by remember { mutableStateOf("") }
  var localLaunchError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) { onRefreshStatus() }
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
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top 3D Navigation Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
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
        text = "Lifetime License",
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
            .clickable(enabled = !isStatusLoading, onClick = onRefreshStatus)
            .testTag("refresh_payment_status_btn"),
      ) {
        if (isStatusLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = SaffronOrange,
          )
        } else {
          Icon3D(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh payment status",
            tone = ClayTones.Orange,
            size = 22.dp,
          )
        }
      }
    }

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

    // Approved Banner
    if (approvedRequest != null) {
      Card3D(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        tone = ClayTones.Green,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Approved",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
              text = "Lifetime License Approved",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
            )
            Text(
              text = "Verified UTR: ${approvedRequest.utr}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.92f),
            )
          }
        }
      }
    } else if (showVerificationBanner) {
      // Pending / Payment Under Verification Banner
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
      modifier = Modifier.fillMaxWidth(),
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
      modifier = Modifier.fillMaxWidth(),
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
        supportingText =
          liveUtrValidationError?.let { msg -> { Text(msg) } },
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

      Spacer(Modifier.height(14.dp))

      Button3D(
        onClick = {
          localLaunchError = null
          onSubmitPayment(utrInput, customerNote)
        },
        enabled = !isSubmitting && !hasBlockingRequest && utrInput.isNotBlank(),
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
                approvedRequest != null -> "License Already Approved"
                pendingRequest != null -> "Verification Pending"
                else -> "Submit Payment Request"
              },
            color = if (!hasBlockingRequest && utrInput.isNotBlank()) Color.White else palette.textSecondary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
          )
        }
      }
    }

    // Error Feedback
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
              .clickable(enabled = !isStatusLoading, onClick = onRefreshStatus)
              .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isStatusLoading) "Checking..." else "Refresh",
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

    Spacer(Modifier.height(12.dp))
  }
}
