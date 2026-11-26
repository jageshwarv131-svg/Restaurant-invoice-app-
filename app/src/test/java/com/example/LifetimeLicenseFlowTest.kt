package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SessionStorage
import com.example.data.SupabaseAuth
import com.example.data.SupabaseLicenseService
import com.example.data.SupabasePaymentService
import com.example.ui.VyaparViewModel
import com.example.ui.screens.LifetimeLicensePaymentScreen
import com.example.ui.theme.VyaparBillingTheme
import java.io.IOException
import java.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LifetimeLicenseFlowTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun buildJwt(sub: String): String {
    val enc = Base64.getUrlEncoder().withoutPadding()
    val hdr = enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
    val exp = (System.currentTimeMillis() / 1000L) + 3600L
    val body =
      enc.encodeToString(
        JSONObject().put("sub", sub).put("role", "authenticated").put("exp", exp).toString().toByteArray(Charsets.UTF_8)
      )
    return "$hdr.$body.sig"
  }

  private fun mockHttpClient(handler: (Request) -> Response): OkHttpClient {
    return OkHttpClient.Builder().addInterceptor { chain -> handler(chain.request()) }.build()
  }

  private fun jsonResponse(request: Request, code: Int, body: String): Response {
    val mediaType = "application/json".toMediaType()
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Error")
      .body(body.toResponseBody(mediaType))
      .build()
  }

  private fun waitUntil(timeoutMs: Long = 4000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!predicate() && System.currentTimeMillis() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(25)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertTrue("Condition not met within ${timeoutMs}ms", predicate())
  }

  @Test
  fun `case A new user sees PhonePe payment form and no agreement`() {
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-new-1",
          licenseState = LicenseAccessState(hasActiveLicense = false, status = LicenseStatus.INACTIVE),
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

    composeTestRule.onNodeWithTag("payment_step1_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("pay_with_phonepe_btn").assertExists()
    composeTestRule.onNodeWithTag("utr_input").assertExists()
    composeTestRule.onNodeWithTag("submit_payment_btn").assertExists()
    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertDoesNotExist()
  }

  @Test
  fun `case A pending user sees pending verification banner and payment section`() {
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-pending-1",
          licenseState = LicenseAccessState(hasActiveLicense = false, status = LicenseStatus.INACTIVE),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests =
            listOf(
              PaymentRequestRecord(
                id = "req_1",
                userId = "user-pending-1",
                utr = "987654321012",
                status = PaymentRequestStatus.PENDING,
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

    composeTestRule.onNodeWithTag("payment_under_verification_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Submitted UTR: 987654321012").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertDoesNotExist()
  }

  @Test
  fun `case B approved active user sees ONLY Lifetime License Agreement without UTR or PhonePe form`() {
    val userId = "a1b2c3d4-0000-1111-2222-333344445555"
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = userId,
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              licenseId = "lic_active_99",
              userId = userId,
              activatedAt = "2026-11-25T10:30:00Z",
            ),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests =
            listOf(
              PaymentRequestRecord(
                id = "pay_1",
                userId = userId,
                utr = "987654321012",
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
    composeTestRule.onNodeWithTag("license_approved_stamp").assertIsDisplayed()
    composeTestRule.onNodeWithText("VYAPAR BILLING PRO").assertExists()
    composeTestRule.onNodeWithText("LIFETIME LICENSE AGREEMENT").assertExists()
    composeTestRule.onNodeWithText(userId).assertExists()
    composeTestRule.onNodeWithText("987654321012").assertExists()
    composeTestRule.onNodeWithText("₹499").assertExists()
    composeTestRule.onNodeWithText("PhonePe").assertExists()

    // Payment UI must be completely hidden
    composeTestRule.onNodeWithTag("payment_step1_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("payment_step2_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("pay_with_phonepe_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("utr_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("customer_note_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("submit_payment_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("payment_under_verification_banner").assertDoesNotExist()
  }

  @Test
  fun `case B omits payment reference when no verified UTR exists instead of inventing one`() {
    val userId = "user-no-utr"
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = userId,
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              licenseId = "lic_1",
              userId = userId,
              activatedAt = "2026-11-25T10:30:00Z",
            ),
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

    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("Payment Reference").assertDoesNotExist()
  }

  @Test
  fun `loading and network error states render cleanly without falsely showing approval`() {
    var retried = false
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-err",
          licenseState =
            LicenseAccessState(
              hasActiveLicense = false,
              status = LicenseStatus.ERROR,
              message = "Network error while verifying license.",
              isNetworkError = true,
            ),
          isSubmitting = false,
          isStatusLoading = false,
          paymentRequests = emptyList(),
          submissionMessage = null,
          errorMessage = null,
          onOpenAccount = {},
          onRefreshStatus = { retried = true },
          onSubmitPayment = { _, _ -> },
          onShowToast = {},
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("license_network_error_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("license_retry_btn").performClick()
    assertTrue(retried)
  }

  @Test
  fun `different user never sees another user's license in UI or service`() {
    // UI defense-in-depth: authUserId != licenseState.userId must never render agreement
    composeTestRule.setContent {
      VyaparBillingTheme {
        LifetimeLicensePaymentScreen(
          isAuthenticated = true,
          authUserId = "user-B",
          licenseState =
            LicenseAccessState(
              hasActiveLicense = true,
              status = LicenseStatus.ACTIVE,
              licenseId = "lic_A",
              userId = "user-A",
            ),
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

    composeTestRule.onNodeWithTag("lifetime_license_agreement_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("payment_step1_card").performScrollTo().assertIsDisplayed()

    // Service defense: row missing user_id or belonging to another user or revoked status never grants access
    val service =
      SupabaseLicenseService(
        mockHttpClient { req ->
          jsonResponse(
            req,
            200,
            """[{"id":"lic_1","user_id":"user-A","status":"active","activated_at":"2026-11-25T10:00:00Z"}]""",
          )
        }
      )
    val mismatch = service.verifyActiveLicense(accessToken = buildJwt("user-B"), userId = "user-B")
    assertEquals(false, mismatch.hasActiveLicense)
    assertEquals(LicenseStatus.INACTIVE, mismatch.status)

    // Revoked status never grants access
    val revokedService =
      SupabaseLicenseService(
        mockHttpClient { req ->
          jsonResponse(
            req,
            200,
            """[{"id":"lic_1","user_id":"user-A","status":"revoked","activated_at":"2026-11-25T10:00:00Z"}]""",
          )
        }
      )
    val revoked = revokedService.verifyActiveLicense(accessToken = buildJwt("user-A"), userId = "user-A")
    assertEquals(false, revoked.hasActiveLicense)
    assertEquals(LicenseStatus.REVOKED, revoked.status)
  }

  @Test
  fun `viewModel login logout switch user and restart restore user-specific Supabase license state`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val sharedSession =
      object : SessionStorage {
        private var access: String? = null
        private var refresh: String? = null
        private var uid: String? = null

        override fun saveSession(accessToken: String, refreshToken: String, userId: String?) {
          access = accessToken
          refresh = refreshToken
          uid = userId
        }

        override fun getAccessToken(): String? = access

        override fun getRefreshToken(): String? = refresh

        override fun getUserId(): String? = uid

        override fun clearSession() {
          access = null
          refresh = null
          uid = null
        }
      }

    var activeUserAServerStatus = "active"
    val userA = "user-approved-aaa"
    val userB = "user-fresh-bbb"

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      val query = req.url.encodedQuery.orEmpty()
      when {
        path.endsWith("/auth/v1/token") -> {
          val buf = okio.Buffer()
          req.body?.writeTo(buf)
          val body = buf.readUtf8()
          val uid =
            when {
              body.contains("usera@test.com") || body.contains("refresh-a") -> userA
              else -> userB
            }
          val refresh = if (uid == userA) "refresh-a" else "refresh-b"
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(uid)}","refresh_token":"$refresh","user":{"id":"$uid"}}""",
          )
        }
        path.endsWith("/auth/v1/logout") -> jsonResponse(req, 200, "{}")
        path.endsWith("/rest/v1/licenses") -> {
          when {
            query.contains("user_id=eq.$userA") && activeUserAServerStatus == "active" ->
              jsonResponse(
                req,
                200,
                """[{"id":"lic_aaa","user_id":"$userA","status":"active","activated_at":"2026-11-25T09:15:00Z"}]""",
              )
            query.contains("user_id=eq.$userA") && activeUserAServerStatus == "revoked" ->
              jsonResponse(
                req,
                200,
                """[{"id":"lic_aaa","user_id":"$userA","status":"revoked","activated_at":"2026-11-25T09:15:00Z"}]""",
              )
            else -> jsonResponse(req, 200, "[]")
          }
        }
        path.endsWith("/rest/v1/payment_requests") -> {
          if (query.contains("user_id=eq.$userA")) {
            jsonResponse(
              req,
              200,
              """[{"id":"pr_a","user_id":"$userA","amount_inr":499,"payment_method":"phonepe","utr":"111122223333","status":"approved","customer_note":"","created_at":"2026-11-25T09:00:00Z"}]""",
            )
          } else {
            jsonResponse(req, 200, "[]")
          }
        }
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = sharedSession
    vm.supabaseAuth = SupabaseAuth(client)
    vm.supabaseLicenseService = SupabaseLicenseService(client)
    vm.supabasePaymentService = SupabasePaymentService(client)

    // 1. Login as approved User A
    vm.loginWithSupabase("usera@test.com", "pass123")
    waitUntil { vm.isAuthenticated.value && !vm.isAuthLoading.value && !vm.licenseState.value.isChecking }
    assertEquals(userA, vm.authUserId.value)
    assertEquals(true, vm.licenseState.value.hasActiveLicense)
    assertEquals(LicenseStatus.ACTIVE, vm.licenseState.value.status)
    assertEquals("2026-11-25T09:15:00Z", vm.licenseState.value.activatedAt)
    assertEquals("111122223333", vm.paymentRequests.value.firstOrNull()?.utr)

    // 2. App restart simulation restores User A active license from Supabase
    val vmRestarted = VyaparViewModel(app)
    vmRestarted.supabaseSession = sharedSession
    vmRestarted.supabaseAuth = SupabaseAuth(client)
    vmRestarted.supabaseLicenseService = SupabaseLicenseService(client)
    vmRestarted.supabasePaymentService = SupabasePaymentService(client)
    vmRestarted.restoreSupabaseSession()
    waitUntil {
      vmRestarted.isAuthenticated.value &&
        !vmRestarted.isAuthLoading.value &&
        !vmRestarted.licenseState.value.isChecking
    }
    assertEquals(userA, vmRestarted.authUserId.value)
    assertEquals(true, vmRestarted.licenseState.value.hasActiveLicense)

    // 3. Revocation on Supabase removes active access upon refresh
    activeUserAServerStatus = "revoked"
    vmRestarted.refreshLicenseStatus()
    waitUntil { !vmRestarted.licenseState.value.isChecking && vmRestarted.licenseState.value.status == LicenseStatus.REVOKED }
    assertEquals(false, vmRestarted.licenseState.value.hasActiveLicense)

    // 4. Logout clears User A state completely, then login as User B stays inactive
    vmRestarted.logoutFromSupabase()
    waitUntil { !vmRestarted.isAuthenticated.value && !vmRestarted.isAuthLoading.value }
    assertEquals(null, vmRestarted.authUserId.value)
    assertEquals(false, vmRestarted.licenseState.value.hasActiveLicense)
    assertTrue(vmRestarted.paymentRequests.value.isEmpty())

    vmRestarted.loginWithSupabase("userb@test.com", "pass123")
    waitUntil {
      vmRestarted.isAuthenticated.value &&
        !vmRestarted.isAuthLoading.value &&
        !vmRestarted.licenseState.value.isChecking
    }
    assertEquals(userB, vmRestarted.authUserId.value)
    assertEquals(false, vmRestarted.licenseState.value.hasActiveLicense)
    assertEquals(LicenseStatus.INACTIVE, vmRestarted.licenseState.value.status)
  }

  @Test
  fun `network error during license check never grants active access`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val failingLicenseClient = mockHttpClient { _ -> throw IOException("No internet") }
    val vm = VyaparViewModel(app)
    vm.supabaseLicenseService = SupabaseLicenseService(failingLicenseClient)
    val res = vm.supabaseLicenseService.verifyActiveLicense(buildJwt("user-net"), "user-net")
    assertEquals(false, res.hasActiveLicense)
    assertEquals(LicenseStatus.ERROR, res.status)
    assertEquals(true, res.isNetworkError)
  }
}
