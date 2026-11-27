package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.EntitlementStatus
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState
import com.example.data.SubscriptionTimeUtils
import com.example.ui.screens.LicenseLockedScreen
import com.example.ui.screens.LifetimeLicensePaymentScreen
import com.example.ui.theme.VyaparBillingTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `subscription screen displays ONLY Monthly 499 and Yearly 1500 cards with Select Plan button and no Lifetime or Supabase`() {
    var pickedPlan: SubscriptionPlan? = null
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "u-ui-1",
          licenseState = LicenseAccessState(hasActiveLicense = false, status = LicenseStatus.INACTIVE),
          subscriptionState =
            SubscriptionState(
              userId = "u-ui-1",
              plan = SubscriptionPlan.TRIAL,
              status = EntitlementStatus.TRIAL_ACTIVE,
              trialExpiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + 3600_000L).toString(),
            ),
          trialRemainingMillis = 3600_000L,
          selectedPlan = SubscriptionPlan.MONTHLY,
          onSelectPlan = { pickedPlan = it },
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests = emptyList(),
          submissionMessage = null,
          errorMessage = null,
          onOpenAccount = {},
          onRefreshStatus = {},
          onSubmitPayment = { _, _ -> },
          onShowToast = {},
          onBack = {},
        )
      }
    }

    // Two separate plan cards ONLY
    composeTestRule.onNodeWithTag("plan_card_monthly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_yearly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_lifetime").assertDoesNotExist()
    assertEquals(0, composeTestRule.onAllNodesWithText("₹999", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("₹2000", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("₹2,000", substring = true).fetchSemanticsNodes().size)

    // Verify required labels on both cards
    composeTestRule.onNodeWithText("Monthly Plan").assertExists()
    composeTestRule.onNodeWithText("₹499 / Month").assertExists()
    composeTestRule.onNodeWithText("Validity: 1 Calendar Month").assertExists()
    composeTestRule.onNodeWithText("Monthly Subscription").assertExists()

    composeTestRule.onNodeWithText("Yearly Plan").assertExists()
    composeTestRule.onNodeWithText("₹1,500 / Year").assertExists()
    composeTestRule.onNodeWithText("Validity: 1 Calendar Year").assertExists()
    composeTestRule.onNodeWithText("Yearly Subscription").assertExists()

    // Select Monthly explicitly -> shows ₹499 PhonePe action
    composeTestRule.onNodeWithTag("select_plan_btn_monthly").performScrollTo().performClick()
    assertEquals(SubscriptionPlan.MONTHLY, pickedPlan)
    composeTestRule.onNodeWithText("Pay with PhonePe (₹499)").performScrollTo().assertIsDisplayed()

    // Select Yearly via Select Plan button -> shows ₹1,500 PhonePe action
    composeTestRule.onNodeWithTag("select_plan_btn_yearly").performScrollTo().performClick()
    assertEquals(SubscriptionPlan.YEARLY, pickedPlan)
    composeTestRule.onNodeWithText("Pay with PhonePe (₹1,500)").performScrollTo().assertIsDisplayed()

    // No customer-facing Supabase text
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Supabase", substring = true, ignoreCase = true).fetchSemanticsNodes().size,
    )
  }

  @Test
  fun `locked screen shows exact expired trial message and ONLY Monthly 499 and Yearly 1500 plans`() {
    composeTestRule.setContent {
      VyaparBillingTheme {
        LicenseLockedScreen(
          featureTitle = "Reports & Analytics",
          isAuthenticated = true,
          licenseState = LicenseAccessState(hasActiveLicense = false, status = LicenseStatus.INACTIVE),
          paymentRequests = emptyList(),
          onActivateLicense = {},
          onOpenAccount = {},
          onRefreshLicense = {},
          subscriptionState =
            SubscriptionState(
              userId = "u-lock-1",
              plan = SubscriptionPlan.FREE,
              status = EntitlementStatus.TRIAL_EXPIRED,
            ),
        )
      }
    }

    composeTestRule.onNodeWithTag("license_locked_card").assertIsDisplayed()
    composeTestRule.onNodeWithText(SubscriptionTimeUtils.TRIAL_EXPIRED_USER_MESSAGE).assertExists()
    composeTestRule.onNodeWithText("₹499").assertExists()
    composeTestRule.onNodeWithText("₹1,500").assertExists()
    assertEquals(0, composeTestRule.onAllNodesWithText("₹999", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("₹2000", substring = true).fetchSemanticsNodes().size)
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Supabase", substring = true, ignoreCase = true).fetchSemanticsNodes().size,
    )
  }

  @Test
  fun `active Monthly or Yearly user sees license status card with expiry and can reveal renewal plans`() {
    val now = System.currentTimeMillis()
    val activatedIso = Instant.ofEpochMilli(now - 5L * 86_400_000L).toString()
    val expiresIso = Instant.ofEpochMilli(now + 25L * 86_400_000L).toString()

    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-yearly-1",
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              plan = SubscriptionPlan.YEARLY,
              licenseId = "lic_yr_1",
              userId = "user-yearly-1",
              activatedAt = activatedIso,
              expiresAt = expiresIso,
              paidAmountInr = 1500,
              isLegacyLifetime = false,
            ),
          subscriptionState =
            SubscriptionState(
              userId = "user-yearly-1",
              plan = SubscriptionPlan.YEARLY,
              status = EntitlementStatus.YEARLY_ACTIVE,
              licenseId = "lic_yr_1",
              activatedAt = activatedIso,
              expiresAt = expiresIso,
              paidAmountInr = 1500,
              isLegacyLifetime = false,
            ),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests =
            listOf(
              PaymentRequestRecord(
                id = "pr_yr_1",
                userId = "user-yearly-1",
                planType = "yearly",
                amountInr = 1500,
                utr = "445566778899",
                status = PaymentRequestStatus.APPROVED,
              )
            ),
          submissionMessage = null,
          errorMessage = null,
          onOpenAccount = {},
          onRefreshStatus = {},
          onSubmitPayment = { _, _ -> },
          onShowToast = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("active_timed_plan_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Yearly Plan").assertExists()
    composeTestRule.onNodeWithText("Activation Date").assertExists()
    composeTestRule.onNodeWithText("Expiry Date").assertExists()
    composeTestRule.onNodeWithText("Remaining Validity").assertExists()
    composeTestRule.onNodeWithTag("renew_subscription_btn").assertExists()

    // UTR payment form is hidden by default for active subscriber
    composeTestRule.onNodeWithTag("payment_step1_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("utr_input").assertDoesNotExist()

    // Tapping Renew / Extend Subscription reveals the 2 plan cards & renewal notice cleanly
    composeTestRule.onNodeWithTag("renew_subscription_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("renewal_extension_notice").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_monthly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_yearly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("utr_input").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `legacy 499 lifetime holder preserves Lifetime License Agreement cleanly`() {
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-legacy-life",
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              plan = SubscriptionPlan.LIFETIME,
              licenseId = "lic_legacy_499",
              userId = "user-legacy-life",
              activatedAt = "2026-10-15T09:00:00Z",
              paidAmountInr = 499,
              isLegacyLifetime = true,
            ),
          subscriptionState =
            SubscriptionState(
              userId = "user-legacy-life",
              plan = SubscriptionPlan.LIFETIME,
              status = EntitlementStatus.LIFETIME_ACTIVE,
              licenseId = "lic_legacy_499",
              activatedAt = "2026-10-15T09:00:00Z",
              paidAmountInr = 499,
              isLegacyLifetime = true,
            ),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests =
            listOf(
              PaymentRequestRecord(
                id = "pr_499_life",
                userId = "user-legacy-life",
                planType = "lifetime",
                amountInr = 499,
                utr = "555566667777",
                status = PaymentRequestStatus.APPROVED,
              )
            ),
          submissionMessage = null,
          errorMessage = null,
          onOpenAccount = {},
          onRefreshStatus = {},
          onSubmitPayment = { _, _ -> },
          onShowToast = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("₹499").assertExists()
    composeTestRule.onNodeWithText("555566667777").assertExists()
    composeTestRule.onNodeWithTag("utr_input").assertDoesNotExist()
  }
}
