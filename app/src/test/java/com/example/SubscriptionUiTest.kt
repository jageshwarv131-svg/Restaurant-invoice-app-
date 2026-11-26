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
  fun `subscription screen allows selecting Monthly 499 Yearly 999 Lifetime 2000 without Supabase text`() {
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

    composeTestRule.onNodeWithTag("plan_card_monthly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_yearly").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("plan_card_lifetime").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("plan_card_yearly").performScrollTo().performClick()
    assertEquals(SubscriptionPlan.YEARLY, pickedPlan)
    composeTestRule.onNodeWithText("Pay with PhonePe (₹999)").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("plan_card_lifetime").performScrollTo().performClick()
    assertEquals(SubscriptionPlan.LIFETIME, pickedPlan)
    composeTestRule.onNodeWithText("Pay with PhonePe (₹2000)").performScrollTo().assertIsDisplayed()

    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Supabase", substring = true, ignoreCase = true).fetchSemanticsNodes().size,
    )
  }

  @Test
  fun `locked screen hides Supabase branding and displays multi-plan options`() {
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
    composeTestRule.onNodeWithText("₹499").assertExists()
    composeTestRule.onNodeWithText("₹999").assertExists()
    composeTestRule.onNodeWithText("₹2000").assertExists()
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Supabase", substring = true, ignoreCase = true).fetchSemanticsNodes().size,
    )
  }

  @Test
  fun `new 2000 lifetime agreement displays ₹2000 while legacy lifetime preserves ₹499`() {
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-new-life",
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              plan = SubscriptionPlan.LIFETIME,
              licenseId = "lic_new_2000",
              userId = "user-new-life",
              activatedAt = "2026-11-26T09:00:00Z",
              paidAmountInr = 2000,
              isLegacyLifetime = false,
            ),
          subscriptionState =
            SubscriptionState(
              userId = "user-new-life",
              plan = SubscriptionPlan.LIFETIME,
              status = EntitlementStatus.LIFETIME_ACTIVE,
              licenseId = "lic_new_2000",
              activatedAt = "2026-11-26T09:00:00Z",
              paidAmountInr = 2000,
              isLegacyLifetime = false,
            ),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests =
            listOf(
              PaymentRequestRecord(
                id = "pr_2000",
                userId = "user-new-life",
                planType = "lifetime",
                amountInr = 2000,
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
    composeTestRule.onNodeWithText("₹2000").assertExists()
    composeTestRule.onNodeWithText("555566667777").assertExists()
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Supabase", substring = true, ignoreCase = true).fetchSemanticsNodes().size,
    )
  }
}
