package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.CartLineInput
import com.example.data.EntitlementStatus
import com.example.data.FeatureEntitlements
import com.example.data.FeatureKey
import com.example.data.JsonStorageManager
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SessionStorage
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState
import com.example.data.SubscriptionTimeUtils
import com.example.data.SupabaseAuth
import com.example.data.SupabaseLicenseService
import com.example.data.SupabasePaymentService
import com.example.data.SupabaseTrialService
import com.example.data.VyaparRepository
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.PaperFormat
import com.example.model.PaymentMethod
import com.example.pdf.InvoicePdfGenerator
import com.example.ui.VyaparViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionAndTrialTest {

  private class InMemorySessionStorage(
    initialAccess: String? = null,
    initialRefresh: String? = null,
    initialUserId: String? = null,
  ) : SessionStorage {
    private var access: String? = initialAccess
    private var refresh: String? = initialRefresh
    private var uid: String? = initialUserId

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

  private fun buildJwt(sub: String, expEpochSeconds: Long = (System.currentTimeMillis() / 1000L) + 3600L): String {
    val enc = Base64.getUrlEncoder().withoutPadding()
    val hdr = enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
    val body =
      enc.encodeToString(
        JSONObject().put("sub", sub).put("role", "authenticated").put("exp", expEpochSeconds).toString().toByteArray(Charsets.UTF_8)
      )
    return "$hdr.$body.sig"
  }

  private fun mockHttpClient(handler: (Request) -> Response): OkHttpClient =
    OkHttpClient.Builder().addInterceptor { chain -> handler(chain.request()) }.build()

  private fun jsonResponse(
    request: Request,
    code: Int,
    body: String,
    httpDateHeader: String? = null,
  ): Response {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val builder =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code in 200..299) "OK" else "Error")
        .body(body.toResponseBody(mediaType))
    if (httpDateHeader != null) {
      builder.header("Date", httpDateHeader)
    }
    return builder.build()
  }

  private fun rfc1123Date(epochMillis: Long): String =
    DateTimeFormatter.RFC_1123_DATE_TIME.format(
      Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
    )

  private fun waitUntil(timeoutMs: Long = 5000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!predicate() && System.currentTimeMillis() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(25)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertTrue("Condition not met within ${timeoutMs}ms", predicate())
  }

  @Test
  fun `new user signup automatically activates 48h backend trial and persists across restart`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val session = InMemorySessionStorage()
    val serverNow = System.currentTimeMillis()
    val trialStartIso = Instant.ofEpochMilli(serverNow).toString()
    val trialEndIso = Instant.ofEpochMilli(serverNow + SubscriptionTimeUtils.TRIAL_DURATION_MILLIS).toString()
    val serverDateHdr = rfc1123Date(serverNow)
    val userId = "new-signup-user-001"

    var rpcActivationCount = 0
    var rowExistsOnServer = false

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      when {
        path.endsWith("/auth/v1/signup") -> {
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-new-1","user":{"id":"$userId","created_at":"$trialStartIso"}}""",
            httpDateHeader = serverDateHdr,
          )
        }
        path.endsWith("/auth/v1/token") -> {
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-new-2","user":{"id":"$userId","created_at":"$trialStartIso"}}""",
            httpDateHeader = serverDateHdr,
          )
        }
        path.endsWith("/rest/v1/licenses") -> jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/payment_requests") -> jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/user_trials") && req.method == "GET" -> {
          val body =
            if (rowExistsOnServer) {
              """[{"user_id":"$userId","trial_started_at":"$trialStartIso","trial_expires_at":"$trialEndIso"}]"""
            } else {
              "[]"
            }
          jsonResponse(req, 200, body, serverDateHdr)
        }
        path.endsWith("/rest/v1/rpc/activate_user_trial") -> {
          rpcActivationCount++
          rowExistsOnServer = true
          jsonResponse(
            req,
            200,
            """{"user_id":"$userId","trial_started_at":"$trialStartIso","trial_expires_at":"$trialEndIso"}""",
            serverDateHdr,
          )
        }
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = session
    vm.supabaseAuth = SupabaseAuth(client)
    vm.supabaseLicenseService = SupabaseLicenseService(client)
    vm.supabasePaymentService = SupabasePaymentService(client)
    vm.supabaseTrialService = SupabaseTrialService(client)

    vm.signUpWithSupabase("freshowner@shop.in", "StrongPass123")
    waitUntil {
      vm.isAuthenticated.value &&
        !vm.isAuthLoading.value &&
        vm.subscriptionState.value.status == EntitlementStatus.TRIAL_ACTIVE
    }

    assertEquals(1, rpcActivationCount)
    assertEquals(EntitlementStatus.TRIAL_ACTIVE, vm.subscriptionState.value.status)
    assertEquals(SubscriptionPlan.TRIAL, vm.subscriptionState.value.plan)
    assertTrue(vm.trialRemainingMillis.value > 47L * 60L * 60L * 1000L)

    // Simulated app restart: must restore saved session & read existing server trial without reactivating
    val vmRestarted = VyaparViewModel(app)
    vmRestarted.supabaseSession = session
    vmRestarted.supabaseAuth = SupabaseAuth(client)
    vmRestarted.supabaseLicenseService = SupabaseLicenseService(client)
    vmRestarted.supabasePaymentService = SupabasePaymentService(client)
    vmRestarted.supabaseTrialService = SupabaseTrialService(client)
    vmRestarted.restoreSupabaseSession()

    waitUntil {
      vmRestarted.isSessionCheckDone.value &&
        vmRestarted.isAuthenticated.value &&
        !vmRestarted.isAuthLoading.value &&
        vmRestarted.subscriptionState.value.status == EntitlementStatus.TRIAL_ACTIVE
    }
    assertEquals(1, rpcActivationCount) // Never reactivated again
    assertEquals(trialEndIso, vmRestarted.subscriptionState.value.trialExpiresAt)
  }

  @Test
  fun `trial does not reset on reinstall or re-login when expired row exists`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val userId = "returning-user-200"
    val serverNow = System.currentTimeMillis()
    val startedIso = Instant.ofEpochMilli(serverNow - 72L * 3600_000L).toString()
    val expiredIso = Instant.ofEpochMilli(serverNow - 24L * 3600_000L).toString()
    val serverDateHdr = rfc1123Date(serverNow)
    var activateAttempted = false

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      when {
        path.endsWith("/auth/v1/token") ->
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-reinstall","user":{"id":"$userId","created_at":"$startedIso"}}""",
            serverDateHdr,
          )
        path.endsWith("/rest/v1/licenses") -> jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/payment_requests") -> jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/user_trials") && req.method == "GET" ->
          jsonResponse(
            req,
            200,
            """[{"user_id":"$userId","trial_started_at":"$startedIso","trial_expires_at":"$expiredIso"}]""",
            serverDateHdr,
          )
        path.endsWith("/rest/v1/rpc/activate_user_trial") ||
          (path.endsWith("/rest/v1/user_trials") && req.method == "POST") -> {
          activateAttempted = true
          jsonResponse(req, 500, "{}")
        }
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = InMemorySessionStorage() // Simulates fresh reinstall (empty local storage)
    vm.supabaseAuth = SupabaseAuth(client)
    vm.supabaseLicenseService = SupabaseLicenseService(client)
    vm.supabasePaymentService = SupabasePaymentService(client)
    vm.supabaseTrialService = SupabaseTrialService(client)

    vm.loginWithSupabase("returning@shop.in", "Pass1234")
    waitUntil {
      vm.isAuthenticated.value && !vm.isAuthLoading.value && !vm.subscriptionState.value.isChecking
    }

    assertFalse(activateAttempted)
    assertEquals(EntitlementStatus.TRIAL_EXPIRED, vm.subscriptionState.value.status)
    assertFalse(vm.evaluateFeature(FeatureKey.PENDING_ORDERS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.POS_BILLING).allowed)
  }

  @Test
  fun `existing pre-rollout user never receives a fresh free trial`() {
    val preRolloutCreated = "2026-01-10T08:00:00Z"
    val userId = "pre-rollout-user-1"
    var activateCalled = false

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      when {
        path.endsWith("/rest/v1/user_trials") && req.method == "GET" ->
          jsonResponse(req, 200, "[]", rfc1123Date(System.currentTimeMillis()))
        path.endsWith("/rest/v1/rpc/activate_user_trial") ||
          (path.endsWith("/rest/v1/user_trials") && req.method == "POST") -> {
          activateCalled = true
          jsonResponse(req, 200, "{}")
        }
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val service = SupabaseTrialService(client)
    val result =
      service.fetchOrActivateTrial(
        accessToken = buildJwt(userId),
        userId = userId,
        isNewSignup = false,
        userCreatedAt = preRolloutCreated,
      )

    assertTrue(result.success)
    assertFalse(result.hasTrial)
    assertFalse(activateCalled)
  }

  @Test
  fun `feature entitlements enforce core free trial-unlocked and paid-only tiers cleanly`() {
    val now = 1_800_000_000_000L
    val futureIso = Instant.ofEpochMilli(now + 10_000_000L).toString()
    val pastIso = Instant.ofEpochMilli(now - 10_000L).toString()

    val trialState =
      SubscriptionState(
        userId = "u1",
        plan = SubscriptionPlan.TRIAL,
        status = EntitlementStatus.TRIAL_ACTIVE,
        trialExpiresAt = futureIso,
      )
    // Trial: Core + Trial-unlocked allowed; Reports & Backup require Paid plan
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.DASHBOARD, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.POS_BILLING, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.BILLS, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.INVOICE_PREVIEW, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.PRODUCTS, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.CUSTOMERS, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.PENDING_ORDERS, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.EXPENSES, trialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.INVENTORY, trialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.REPORTS, trialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.BACKUP_RESTORE, trialState, true, now).allowed)

    // Expired Trial: Core stays unlocked, Trial+Paid & Paid-only locked
    val expiredTrialState =
      SubscriptionState(
        userId = "u1",
        plan = SubscriptionPlan.FREE,
        status = EntitlementStatus.TRIAL_EXPIRED,
        trialExpiresAt = pastIso,
      )
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.POS_BILLING, expiredTrialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.BILLS, expiredTrialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.INVOICE_PREVIEW, expiredTrialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.PRODUCTS, expiredTrialState, true, now).allowed)
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.CUSTOMERS, expiredTrialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.PENDING_ORDERS, expiredTrialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.EXPENSES, expiredTrialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.INVENTORY, expiredTrialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.REPORTS, expiredTrialState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.BACKUP_RESTORE, expiredTrialState, true, now).allowed)

    // Paid Monthly / Yearly / Lifetime unlocks everything
    val paidState =
      SubscriptionState(
        userId = "u1",
        plan = SubscriptionPlan.MONTHLY,
        status = EntitlementStatus.MONTHLY_ACTIVE,
        expiresAt = futureIso,
      )
    FeatureKey.entries.forEach { key ->
      assertTrue("${key.name} should be allowed on active Monthly", FeatureEntitlements.evaluate(key, paidState, true, now).allowed)
    }

    // Revoked blocks non-core, preserves core
    val revokedState =
      SubscriptionState(
        userId = "u1",
        plan = SubscriptionPlan.LIFETIME,
        status = EntitlementStatus.REVOKED,
      )
    assertTrue(FeatureEntitlements.evaluate(FeatureKey.POS_BILLING, revokedState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.REPORTS, revokedState, true, now).allowed)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.PENDING_ORDERS, revokedState, true, now).allowed)
  }

  @Test
  fun `offline mode with saved session keeps Core billing and saved data unlocked`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val userId = "offline-user-1"
    val session = InMemorySessionStorage(buildJwt(userId), "offline-refresh", userId)
    val offlineClient = mockHttpClient { _ -> throw IOException("Offline") }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = session
    vm.supabaseAuth = SupabaseAuth(offlineClient)
    vm.supabaseLicenseService = SupabaseLicenseService(offlineClient)
    vm.supabasePaymentService = SupabasePaymentService(offlineClient)
    vm.supabaseTrialService = SupabaseTrialService(offlineClient)

    vm.restoreSupabaseSession()
    waitUntil { vm.isSessionCheckDone.value && vm.isAuthenticated.value && !vm.isAuthLoading.value }

    assertTrue(vm.evaluateFeature(FeatureKey.POS_BILLING).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.BILLS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.INVOICE_PREVIEW).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.PRODUCTS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.CUSTOMERS).allowed)
    assertFalse(vm.evaluateFeature(FeatureKey.REPORTS).allowed)
    assertFalse(vm.evaluateFeature(FeatureKey.BACKUP_RESTORE).allowed)

    // Saved local data and core invoice creation work offline
    val repo = VyaparRepository(JsonStorageManager(app))
    val inv = runBlockingHelper {
      repo.loadAll()
      repo.completeSaleAndCreateInvoice(
        customer = null,
        cartItems = listOf(CartLineInput(name = "Tea", quantity = 2.0, unitPricePaise = 2000L, gstPercent = 5.0)),
        discountValue = 0.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTRA_STATE_CGST_SGST,
        taxMode = GstTaxMode.EXCLUSIVE,
        paymentMethod = PaymentMethod.CASH,
        amountPaidPaise = 4200L,
      )
    }
    assertEquals(4200L, inv.grandTotalPaise)
    assertTrue(repo.invoices.value.any { it.id == inv.id })
  }

  @Test
  fun `multi-plan PhonePe payment submission sends exact Monthly 499 Yearly 999 and Lifetime 2000 payloads`() {
    assertEquals("499.00", SupabasePaymentService.buildUpiPaymentUri(amountInr = 499).getQueryParameter("am"))
    assertEquals("999.00", SupabasePaymentService.buildUpiPaymentUri(amountInr = 999).getQueryParameter("am"))
    assertEquals("2000.00", SupabasePaymentService.buildUpiPaymentUri(amountInr = 2000).getQueryParameter("am"))

    val userId = "buyer-user-77"
    val token = buildJwt(userId)

    SubscriptionPlan.purchasablePlans.forEach { plan ->
      var capturedBody = ""
      val client = mockHttpClient { req ->
        val buf = okio.Buffer()
        req.body?.writeTo(buf)
        capturedBody = buf.readUtf8()
        jsonResponse(
          req,
          201,
          """[{"id":"pay_${plan.apiValue}","user_id":"$userId","plan_type":"${plan.apiValue}","amount_inr":${plan.priceInr},"payment_method":"phonepe","utr":"998877665544","status":"pending","customer_note":"Test","created_at":"2026-11-26T08:00:00Z"}]""",
        )
      }

      val res =
        SupabasePaymentService(client).submitPaymentRequest(
          accessToken = token,
          userId = userId,
          utrInput = "998877665544",
          customerNote = "Test",
          plan = plan,
        )
      assertTrue(res.success)
      val sent = JSONObject(capturedBody)
      assertEquals(plan.apiValue, sent.getString("plan_type"))
      assertEquals(plan.priceInr, sent.getInt("amount_inr"))
      assertEquals("phonepe", sent.getString("payment_method"))
      assertEquals("pending", sent.getString("status"))
      assertEquals(plan, res.request?.resolvedPlan)
    }
  }

  @Test
  fun `server verified Monthly Yearly Lifetime expiry revocation and Legacy 499 Lifetime preservation`() {
    val userId = "plan-user-1"
    val token = buildJwt(userId)
    val now = 1_800_000_000_000L
    val serverDateHdr = rfc1123Date(now)
    val futureIso = Instant.ofEpochMilli(now + 20L * 86_400_000L).toString()
    val pastIso = Instant.ofEpochMilli(now - 60_000L).toString()

    // 1. Active Monthly
    val monthlyActive =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l1","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"active","activated_at":"2026-11-20T00:00:00Z","expires_at":"$futureIso"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(monthlyActive.hasActiveLicense)
    assertEquals(SubscriptionPlan.MONTHLY, monthlyActive.plan)
    assertEquals(EntitlementStatus.MONTHLY_ACTIVE, FeatureEntitlements.resolveSubscriptionState(true, userId, monthlyActive, null, emptyList()).status)

    // 2. Expired Monthly (server Date >= expires_at)
    val monthlyExpired =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l1","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"active","expires_at":"$pastIso"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertFalse(monthlyExpired.hasActiveLicense)
    assertEquals(LicenseStatus.EXPIRED, monthlyExpired.status)
    assertEquals(EntitlementStatus.MONTHLY_EXPIRED, FeatureEntitlements.resolveSubscriptionState(true, userId, monthlyExpired, null, emptyList()).status)

    // 3. Active Yearly -> Expired Yearly
    val yearlyActive =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l2","user_id":"$userId","plan_type":"yearly","amount_inr":999,"status":"active","expires_at":"$futureIso"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(yearlyActive.hasActiveLicense)
    assertEquals(SubscriptionPlan.YEARLY, yearlyActive.plan)
    assertEquals(EntitlementStatus.YEARLY_ACTIVE, FeatureEntitlements.resolveSubscriptionState(true, userId, yearlyActive, null, emptyList()).status)

    // 4. New ₹2000 Lifetime
    val newLifetime =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l3","user_id":"$userId","plan_type":"lifetime","amount_inr":2000,"status":"active","expires_at":null}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(newLifetime.hasActiveLicense)
    assertEquals(SubscriptionPlan.LIFETIME, newLifetime.plan)
    assertFalse(newLifetime.isLegacyLifetime)
    assertEquals(2000, FeatureEntitlements.resolveSubscriptionState(true, userId, newLifetime, null, emptyList()).displayPaidAmountInr)

    // 5. Legacy ₹499 Lifetime holder (no plan_type / null expires_at)
    val legacyLifetime =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l_legacy","user_id":"$userId","status":"active","activated_at":"2026-11-25T10:00:00Z"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(legacyLifetime.hasActiveLicense)
    assertEquals(SubscriptionPlan.LIFETIME, legacyLifetime.plan)
    assertTrue(legacyLifetime.isLegacyLifetime)
    val legacySub = FeatureEntitlements.resolveSubscriptionState(true, userId, legacyLifetime, null, emptyList())
    assertEquals(EntitlementStatus.LIFETIME_ACTIVE, legacySub.status)
    assertEquals(499, legacySub.displayPaidAmountInr)

    // 6. Revoked license
    val revoked =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l_rev","user_id":"$userId","plan_type":"yearly","status":"revoked"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertFalse(revoked.hasActiveLicense)
    assertEquals(LicenseStatus.REVOKED, revoked.status)
    assertEquals(EntitlementStatus.REVOKED, FeatureEntitlements.resolveSubscriptionState(true, userId, revoked, null, emptyList()).status)
  }

  @Test
  fun `clock tampering back or forward does not spoof trial remaining time when server anchored`() {
    val serverAnchor = 1_800_000_000_000L
    val elapsedAnchor = 50_000L
    val trialEnd = Instant.ofEpochMilli(serverAnchor + 10_000_000L).toString()

    val anchored =
      SubscriptionState(
        userId = "u1",
        plan = SubscriptionPlan.TRIAL,
        status = EntitlementStatus.TRIAL_ACTIVE,
        trialExpiresAt = trialEnd,
        serverAnchorEpochMillis = serverAnchor,
        clientAnchorElapsedMillis = elapsedAnchor,
      )

    // Even if user turns device clock back 10 years, elapsed monotonic clock controls trustedNowMillis
    val tamperedSystemTime = serverAnchor - 315_360_000_000L
    val trustedNow = anchored.trustedNowMillis(systemNowMillis = tamperedSystemTime, elapsedNowMillis = elapsedAnchor + 1_000L)
    assertEquals(serverAnchor + 1_000L, trustedNow)
    assertEquals(9_999_000L, anchored.trialRemainingMillis(trustedNow))
  }

  @Test
  fun `session persistence restores authenticated state directly and logout clears everything`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val userId = "persisted-user-88"
    val session = InMemorySessionStorage(buildJwt(userId), "refresh-persist", userId)

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      when {
        path.endsWith("/auth/v1/token") ->
          jsonResponse(req, 200, """{"access_token":"${buildJwt(userId)}","refresh_token":"refresh-persist-2","user":{"id":"$userId"}}""")
        path.endsWith("/auth/v1/logout") -> jsonResponse(req, 200, "{}")
        path.endsWith("/rest/v1/licenses") -> jsonResponse(req, 200, "[]")
        path.endsWith("/rest/v1/user_trials") -> jsonResponse(req, 200, "[]")
        path.endsWith("/rest/v1/payment_requests") -> jsonResponse(req, 200, "[]")
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = session
    vm.supabaseAuth = SupabaseAuth(client)
    vm.supabaseLicenseService = SupabaseLicenseService(client)
    vm.supabasePaymentService = SupabasePaymentService(client)
    vm.supabaseTrialService = SupabaseTrialService(client)

    vm.restoreSupabaseSession()
    waitUntil { vm.isSessionCheckDone.value && vm.isAuthenticated.value && !vm.isAuthLoading.value }
    assertEquals(userId, vm.authUserId.value)

    vm.logoutFromSupabase()
    waitUntil { !vm.isAuthenticated.value && !vm.isAuthLoading.value }
    assertNull(vm.authUserId.value)
    assertNull(session.getAccessToken())
    assertEquals(EntitlementStatus.UNAUTHENTICATED, vm.subscriptionState.value.status)
    assertEquals(0L, vm.trialRemainingMillis.value)
  }

  @Test
  fun `invoice PDF centers logo with strict aspect ratio and skips logo cleanly when missing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val outDir = File(context.cacheDir, "pdf_logo_test").apply { mkdirs() }

    // 1. Missing logoPath must return null (no Vyapar fallback logo)
    assertNull(InvoicePdfGenerator.loadLogoBitmap(null))
    assertNull(InvoicePdfGenerator.loadLogoBitmap(""))
    assertNull(InvoicePdfGenerator.loadLogoBitmap(File(outDir, "does_not_exist.png").absolutePath))

    // 2. Wide 240x80 (3:1) and Tall 80x240 (1:3) aspect ratio & centering math
    val wideRect = InvoicePdfGenerator.computeTopCenteredLogoRect(595f, 26f, 240, 80, 156f, 76f)!!
    assertEquals(3f, wideRect.width() / wideRect.height(), 0.01f)
    assertEquals(297.5f, (wideRect.left + wideRect.right) / 2f, 0.1f)

    val tallRect = InvoicePdfGenerator.computeTopCenteredLogoRect(595f, 26f, 80, 240, 156f, 76f)!!
    assertEquals(1f / 3f, tallRect.width() / tallRect.height(), 0.01f)
    assertEquals(297.5f, (tallRect.left + tallRect.right) / 2f, 0.1f)

    // 3. Generate A4 & 80mm PDFs with real PNG shop logo and without shop logo
    val logoFile = File(outDir, "shop_logo.png")
    val bmp = Bitmap.createBitmap(240, 80, Bitmap.Config.ARGB_8888)
    FileOutputStream(logoFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    assertNotNull(InvoicePdfGenerator.loadLogoBitmap(logoFile.absolutePath))

    val repo = VyaparRepository(JsonStorageManager(context))
    runBlockingHelper { repo.loadAll() }
    val inv =
      runBlockingHelper {
        repo.completeSaleAndCreateInvoice(
          customer = null,
          cartItems = listOf(CartLineInput(name = "Coffee", quantity = 1.0, unitPricePaise = 10000L, gstPercent = 5.0)),
          discountValue = 0.0,
          isPercentageDiscount = false,
          taxType = GstTaxType.INTRA_STATE_CGST_SGST,
          taxMode = GstTaxMode.EXCLUSIVE,
          paymentMethod = PaymentMethod.UPI,
          amountPaidPaise = 10500L,
        )
      }

    val withLogoA4 =
      InvoicePdfGenerator.generateInvoicePdf(
        context,
        outDir,
        BusinessProfile(businessName = "Cafe Aroma", logoPath = logoFile.absolutePath),
        inv,
        PaperFormat.A4,
        AppLanguage.ENGLISH,
      )
    val noLogoThermal =
      InvoicePdfGenerator.generateInvoicePdf(
        context,
        outDir,
        BusinessProfile(businessName = "Cafe Aroma", logoPath = null),
        inv,
        PaperFormat.THERMAL_80MM,
        AppLanguage.ENGLISH,
      )

    assertTrue(withLogoA4.exists() && withLogoA4.length() > 400)
    assertTrue(noLogoThermal.exists() && noLogoThermal.length() > 400)
    assertTrue(withLogoA4.inputStream().bufferedReader().use { it.readLine() }.startsWith("%PDF-"))
    assertTrue(noLogoThermal.inputStream().bufferedReader().use { it.readLine() }.startsWith("%PDF-"))
  }

  private fun <T> runBlockingHelper(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }

  @Test
  fun `multi user switch clears previous user license trial and payment history completely`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val session = InMemorySessionStorage()
    val userA = "user-a-paid"
    val userB = "user-b-free"
    val now = System.currentTimeMillis()
    val serverDateHdr = rfc1123Date(now)
    val futureIso = Instant.ofEpochMilli(now + 15L * 86_400_000L).toString()
    val pastTrialStart = Instant.ofEpochMilli(now - 96L * 3600_000L).toString()
    val pastTrialEnd = Instant.ofEpochMilli(now - 48L * 3600_000L).toString()

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      val query = req.url.encodedQuery.orEmpty()
      when {
        path.endsWith("/auth/v1/token") -> {
          val buf = okio.Buffer()
          req.body?.writeTo(buf)
          val uid = if (buf.readUtf8().contains("usera@")) userA else userB
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(uid)}","refresh_token":"ref-$uid","user":{"id":"$uid","created_at":"2026-01-01T00:00:00Z"}}""",
            serverDateHdr,
          )
        }
        path.endsWith("/auth/v1/logout") -> jsonResponse(req, 200, "{}")
        path.endsWith("/rest/v1/licenses") -> {
          if (query.contains("user_id=eq.$userA")) {
            jsonResponse(req, 200, """[{"id":"lic_a","user_id":"$userA","plan_type":"monthly","amount_inr":499,"status":"active","expires_at":"$futureIso"}]""", serverDateHdr)
          } else {
            jsonResponse(req, 200, "[]", serverDateHdr)
          }
        }
        path.endsWith("/rest/v1/user_trials") -> {
          if (query.contains("user_id=eq.$userB")) {
            jsonResponse(req, 200, """[{"user_id":"$userB","trial_started_at":"$pastTrialStart","trial_expires_at":"$pastTrialEnd"}]""", serverDateHdr)
          } else {
            jsonResponse(req, 200, "[]", serverDateHdr)
          }
        }
        path.endsWith("/rest/v1/payment_requests") -> {
          if (query.contains("user_id=eq.$userA")) {
            jsonResponse(req, 200, """[{"id":"pr_a","user_id":"$userA","plan_type":"monthly","amount_inr":499,"payment_method":"phonepe","utr":"123456789012","status":"approved","created_at":"2026-11-25T10:00:00Z"}]""", serverDateHdr)
          } else {
            jsonResponse(req, 200, "[]", serverDateHdr)
          }
        }
        else -> jsonResponse(req, 404, "{}")
      }
    }

    val vm = VyaparViewModel(app)
    vm.supabaseSession = session
    vm.supabaseAuth = SupabaseAuth(client)
    vm.supabaseLicenseService = SupabaseLicenseService(client)
    vm.supabasePaymentService = SupabasePaymentService(client)
    vm.supabaseTrialService = SupabaseTrialService(client)

    vm.loginWithSupabase("usera@shop.in", "pass123")
    waitUntil { vm.isAuthenticated.value && vm.subscriptionState.value.status == EntitlementStatus.MONTHLY_ACTIVE }
    assertTrue(vm.evaluateFeature(FeatureKey.REPORTS).allowed)
    assertEquals(1, vm.paymentRequests.value.size)

    vm.logoutFromSupabase()
    waitUntil { !vm.isAuthenticated.value && !vm.isAuthLoading.value }
    assertEquals(EntitlementStatus.UNAUTHENTICATED, vm.subscriptionState.value.status)
    assertTrue(vm.paymentRequests.value.isEmpty())

    vm.loginWithSupabase("userb@shop.in", "pass123")
    waitUntil { vm.isAuthenticated.value && !vm.isAuthLoading.value && !vm.subscriptionState.value.isChecking }
    assertEquals(userB, vm.authUserId.value)
    assertEquals(EntitlementStatus.TRIAL_EXPIRED, vm.subscriptionState.value.status)
    assertFalse(vm.evaluateFeature(FeatureKey.REPORTS).allowed)
    assertFalse(vm.evaluateFeature(FeatureKey.PENDING_ORDERS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.POS_BILLING).allowed)
    assertTrue(vm.paymentRequests.value.isEmpty())
  }
}
