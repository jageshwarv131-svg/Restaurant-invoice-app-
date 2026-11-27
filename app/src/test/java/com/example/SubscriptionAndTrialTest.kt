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
import com.example.data.LicenseStatus
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

  private fun <T> runBlockingHelper(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }

  // Case 1 & Case 3 (restart): New user signup automatically activates 48h backend trial & persists across restart
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
        path.endsWith("/auth/v1/signup") ->
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-new-1","user":{"id":"$userId","created_at":"$trialStartIso"}}""",
            httpDateHeader = serverDateHdr,
          )
        path.endsWith("/auth/v1/token") ->
          jsonResponse(
            req,
            200,
            """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-new-2","user":{"id":"$userId","created_at":"$trialStartIso"}}""",
            httpDateHeader = serverDateHdr,
          )
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
    assertEquals(1, rpcActivationCount)
    assertEquals(trialEndIso, vmRestarted.subscriptionState.value.trialExpiresAt)
  }

  // Case 2 & Case 3 (reinstall/relogin): Expired trial locks premium features, preserves data & core features, never resets
  @Test
  fun `trial expires after 48h locks premium features and does not reset on reinstall or re-login`() {
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
    vm.supabaseSession = InMemorySessionStorage() // Fresh reinstall (empty local session/prefs)
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
    assertFalse(vm.evaluateFeature(FeatureKey.REPORTS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.POS_BILLING).allowed)
    assertEquals(
      SubscriptionTimeUtils.TRIAL_EXPIRED_USER_MESSAGE,
      vm.evaluateFeature(FeatureKey.REPORTS).reasonMessage,
    )
  }

  // Case 4: Pre-rollout existing user does NOT receive a fresh automatic trial
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

    val result =
      SupabaseTrialService(client).fetchOrActivateTrial(
        accessToken = buildJwt(userId),
        userId = userId,
        isNewSignup = false,
        userCreatedAt = preRolloutCreated,
      )

    assertTrue(result.success)
    assertFalse(result.hasTrial)
    assertFalse(activateCalled)
  }

  // Case 3 (clock tampering): Local clock manipulation cannot reset or spoof trial time
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

    val tamperedSystemTime = serverAnchor - 315_360_000_000L
    val trustedNow = anchored.trustedNowMillis(systemNowMillis = tamperedSystemTime, elapsedNowMillis = elapsedAnchor + 1_000L)
    assertEquals(serverAnchor + 1_000L, trustedNow)
    assertEquals(9_999_000L, anchored.trialRemainingMillis(trustedNow))
  }

  // Case 5, Case 6, Case 7: Only Monthly ₹499 and Yearly ₹1,500 purchasable; Lifetime rejected/absent
  @Test
  fun `only Monthly 499 and Yearly 1500 are purchasable and Lifetime or tampered amounts are rejected`() {
    assertEquals(listOf(SubscriptionPlan.MONTHLY, SubscriptionPlan.YEARLY), SubscriptionPlan.purchasablePlans)
    assertFalse(SubscriptionPlan.LIFETIME.isPurchasable)
    assertEquals("₹499 / Month", SubscriptionPlan.MONTHLY.displayPriceLabel)
    assertEquals("₹1,500 / Year", SubscriptionPlan.YEARLY.displayPriceLabel)
    assertEquals("Monthly Subscription", SubscriptionPlan.MONTHLY.featureLabel)
    assertEquals("Yearly Subscription", SubscriptionPlan.YEARLY.featureLabel)

    assertEquals("499.00", SupabasePaymentService.buildUpiPaymentUri(amountInr = 499).getQueryParameter("am"))
    assertEquals("1500.00", SupabasePaymentService.buildUpiPaymentUri(amountInr = 1500).getQueryParameter("am"))

    val userId = "buyer-user-77"
    val token = buildJwt(userId)

    // Monthly ₹499 & Yearly ₹1,500 succeed with exact payload
    SubscriptionPlan.purchasablePlans.forEach { plan ->
      var capturedBody = ""
      val client = mockHttpClient { req ->
        val buf = okio.Buffer()
        req.body?.writeTo(buf)
        capturedBody = buf.readUtf8()
        jsonResponse(
          req,
          201,
          """[{"id":"pay_${plan.apiValue}","user_id":"$userId","plan_type":"${plan.apiValue}","amount_inr":${plan.priceInr},"payment_method":"phonepe","utr":"998877665544","status":"pending","customer_note":"Test","created_at":"2026-11-27T08:00:00Z"}]""",
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

    // Attempting Lifetime or mismatched price is strictly rejected before hitting network
    var networkCalled = false
    val guardService = SupabasePaymentService(mockHttpClient { req ->
      networkCalled = true
      jsonResponse(req, 500, "{}")
    })
    val rejectedLifetime = guardService.submitPaymentRequest(
      accessToken = token,
      userId = userId,
      utrInput = "998877665544",
      customerNote = "Hack",
      plan = SubscriptionPlan.LIFETIME,
    )
    assertFalse(rejectedLifetime.success)
    assertFalse(networkCalled)

    val rejectedMismatch = guardService.submitPaymentRequest(
      accessToken = token,
      userId = userId,
      utrInput = "998877665544",
      customerNote = "Hack",
      plan = SubscriptionPlan.YEARLY,
      amountInrOverride = 499,
    )
    assertFalse(rejectedMismatch.success)
    assertFalse(networkCalled)
  }

  // Case 8, Case 9, Case 10, Case 11, Case 12: Calendar Monthly/Yearly math, expiry, renewal extension, Legacy ₹499 Lifetime
  @Test
  fun `calendar Monthly Yearly expiry renewal extension and Legacy 499 Lifetime preservation work accurately`() {
    // 1. Real IST calendar month: 15 Jan 10:30 IST -> 15 Feb 10:30 IST
    val jan15Ist = "2027-01-15T10:30:00+05:30"
    val feb15Exp = SubscriptionTimeUtils.computeCalendarExpiryIso(jan15Ist, SubscriptionPlan.MONTHLY)
    assertEquals(Instant.parse("2027-02-15T10:30:00+05:30").toString(), feb15Exp)

    // Month-end clamping: 31 Jan 2027 IST -> 28 Feb 2027 IST (and 2028 leap -> 29 Feb 2028)
    assertEquals(
      Instant.parse("2027-02-28T10:30:00+05:30").toString(),
      SubscriptionTimeUtils.computeCalendarExpiryIso("2027-01-31T10:30:00+05:30", SubscriptionPlan.MONTHLY),
    )
    assertEquals(
      Instant.parse("2028-02-29T10:30:00+05:30").toString(),
      SubscriptionTimeUtils.computeCalendarExpiryIso("2028-01-31T10:30:00+05:30", SubscriptionPlan.MONTHLY),
    )

    // 2. Real IST calendar year: 15 Jan 2027 -> 15 Jan 2028
    val jan15NextYearExp = SubscriptionTimeUtils.computeCalendarExpiryIso(jan15Ist, SubscriptionPlan.YEARLY)
    assertEquals(Instant.parse("2028-01-15T10:30:00+05:30").toString(), jan15NextYearExp)

    // 3. Active plan renewal extends from existing expiresAt (no wasted days)
    val renewedFromExisting =
      SubscriptionTimeUtils.computeCalendarExpiryIso(
        activatedAtIso = "2027-02-01T09:00:00+05:30",
        plan = SubscriptionPlan.YEARLY,
        existingExpiresAtIso = "2027-02-15T10:30:00+05:30",
      )
    assertEquals(Instant.parse("2028-02-15T10:30:00+05:30").toString(), renewedFromExisting)

    // Expired renewal extends from new activation date (not past expiry)
    val renewedAfterExpiry =
      SubscriptionTimeUtils.computeCalendarExpiryIso(
        activatedAtIso = "2027-03-01T09:00:00+05:30",
        plan = SubscriptionPlan.MONTHLY,
        existingExpiresAtIso = "2027-02-15T10:30:00+05:30",
      )
    assertEquals(Instant.parse("2027-04-01T09:00:00+05:30").toString(), renewedAfterExpiry)

    val userId = "plan-user-1"
    val token = buildJwt(userId)
    val now = 1_800_000_000_000L
    val serverDateHdr = rfc1123Date(now)
    val futureIso = Instant.ofEpochMilli(now + 20L * 86_400_000L).toString()
    val futureExtendedIso = Instant.ofEpochMilli(now + 385L * 86_400_000L).toString()
    val pastIso = Instant.ofEpochMilli(now - 60_000L).toString()

    // Active Monthly
    val monthlyActive =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l1","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"active","activated_at":"2027-01-15T05:00:00Z","expires_at":"$futureIso"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(monthlyActive.hasActiveLicense)
    assertEquals(SubscriptionPlan.MONTHLY, monthlyActive.plan)
    assertFalse(monthlyActive.isLegacyLifetime)
    assertEquals(EntitlementStatus.MONTHLY_ACTIVE, FeatureEntitlements.resolveSubscriptionState(true, userId, monthlyActive, null, emptyList()).status)

    // Expired Monthly locks premium features
    val monthlyExpired =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l1","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"active","expires_at":"$pastIso"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertFalse(monthlyExpired.hasActiveLicense)
    assertEquals(LicenseStatus.EXPIRED, monthlyExpired.status)
    val expiredSub = FeatureEntitlements.resolveSubscriptionState(true, userId, monthlyExpired, null, emptyList())
    assertEquals(EntitlementStatus.MONTHLY_EXPIRED, expiredSub.status)
    assertFalse(FeatureEntitlements.evaluate(FeatureKey.REPORTS, expiredSub, true, now).allowed)

    // Active Yearly (₹1,500) + multiple license history picks highest active expiry
    val yearlyRenewed =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(
          req,
          200,
          """[
            {"id":"l_yr","user_id":"$userId","plan_type":"yearly","amount_inr":1500,"status":"active","expires_at":"$futureExtendedIso"},
            {"id":"l_mo","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"expired","expires_at":"$pastIso"}
          ]""",
          serverDateHdr,
        )
      }).verifySubscriptionLicense(token, userId)
    assertTrue(yearlyRenewed.hasActiveLicense)
    assertEquals(SubscriptionPlan.YEARLY, yearlyRenewed.plan)
    assertEquals(1500, yearlyRenewed.paidAmountInr)
    assertEquals(EntitlementStatus.YEARLY_ACTIVE, FeatureEntitlements.resolveSubscriptionState(true, userId, yearlyRenewed, null, emptyList()).status)

    // Legacy ₹499 Lifetime holder (no expires_at) stays permanently Lifetime and is NEVER confused with Monthly
    val legacyLifetime =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l_legacy","user_id":"$userId","license_type":"lifetime","amount_inr":499,"status":"active","activated_at":"2026-10-15T10:00:00Z","expires_at":null}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertTrue(legacyLifetime.hasActiveLicense)
    assertEquals(SubscriptionPlan.LIFETIME, legacyLifetime.plan)
    assertTrue(legacyLifetime.isLegacyLifetime)
    val legacySub = FeatureEntitlements.resolveSubscriptionState(true, userId, legacyLifetime, null, emptyList())
    assertEquals(EntitlementStatus.LIFETIME_ACTIVE, legacySub.status)
    assertEquals(499, legacySub.displayPaidAmountInr)

    // Revoked license never grants premium access
    val revoked =
      SupabaseLicenseService(mockHttpClient { req ->
        jsonResponse(req, 200, """[{"id":"l_rev","user_id":"$userId","plan_type":"yearly","amount_inr":1500,"status":"revoked"}]""", serverDateHdr)
      }).verifySubscriptionLicense(token, userId)
    assertFalse(revoked.hasActiveLicense)
    assertEquals(LicenseStatus.REVOKED, revoked.status)
    assertEquals(EntitlementStatus.REVOKED, FeatureEntitlements.resolveSubscriptionState(true, userId, revoked, null, emptyList()).status)
  }

  // Case 11 (ViewModel renewal submission): Active Monthly/Yearly user CAN submit a renewal UTR for Monthly or Yearly
  @Test
  fun `active Monthly subscriber can submit a Yearly 1500 renewal payment request`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val userId = "active-monthly-renew"
    val now = System.currentTimeMillis()
    val serverDateHdr = rfc1123Date(now)
    val futureIso = Instant.ofEpochMilli(now + 10L * 86_400_000L).toString()
    val session = InMemorySessionStorage(buildJwt(userId), "ref-renew", userId)

    var submittedPlan = ""
    var submittedAmount = 0

    val client = mockHttpClient { req ->
      val path = req.url.encodedPath
      when {
        path.endsWith("/auth/v1/token") ->
          jsonResponse(req, 200, """{"access_token":"${buildJwt(userId)}","refresh_token":"ref-renew-2","user":{"id":"$userId"}}""", serverDateHdr)
        path.endsWith("/rest/v1/licenses") ->
          jsonResponse(req, 200, """[{"id":"l_m1","user_id":"$userId","plan_type":"monthly","amount_inr":499,"status":"active","activated_at":"2026-11-10T00:00:00Z","expires_at":"$futureIso"}]""", serverDateHdr)
        path.endsWith("/rest/v1/user_trials") -> jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/payment_requests") && req.method == "GET" ->
          jsonResponse(req, 200, "[]", serverDateHdr)
        path.endsWith("/rest/v1/payment_requests") && req.method == "POST" -> {
          val buf = okio.Buffer()
          req.body?.writeTo(buf)
          val payload = JSONObject(buf.readUtf8())
          submittedPlan = payload.getString("plan_type")
          submittedAmount = payload.getInt("amount_inr")
          jsonResponse(
            req,
            201,
            """[{"id":"pr_renew_1","user_id":"$userId","plan_type":"$submittedPlan","amount_inr":$submittedAmount,"payment_method":"phonepe","utr":"778899112233","status":"pending","customer_note":"Renew yearly","created_at":"2026-11-27T01:00:00Z"}]""",
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

    vm.restoreSupabaseSession()
    waitUntil { vm.subscriptionState.value.status == EntitlementStatus.MONTHLY_ACTIVE }

    vm.submitSubscriptionPayment(SubscriptionPlan.YEARLY, "778899112233", "Renew yearly")
    waitUntil { !vm.isPaymentSubmitting.value && vm.paymentSubmissionMessage.value == "Payment Under Verification" }

    assertEquals("yearly", submittedPlan)
    assertEquals(1500, submittedAmount)
    assertEquals(PaymentRequestStatus.PENDING, vm.paymentRequests.value.first().status)
  }

  // Case 14: Offline mode keeps Core billing + saved local data intact, never falsely marks approved or expired
  @Test
  fun `offline mode with saved session keeps Core billing unlocked and does not grant false approval`() {
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

    assertEquals(EntitlementStatus.ERROR, vm.subscriptionState.value.status)
    assertTrue(vm.subscriptionState.value.isNetworkError)
    assertFalse(vm.licenseState.value.hasActiveLicense)
    assertTrue(vm.evaluateFeature(FeatureKey.POS_BILLING).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.BILLS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.INVOICE_PREVIEW).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.PRODUCTS).allowed)
    assertTrue(vm.evaluateFeature(FeatureKey.CUSTOMERS).allowed)
    assertFalse(vm.evaluateFeature(FeatureKey.REPORTS).allowed)
    assertFalse(vm.evaluateFeature(FeatureKey.BACKUP_RESTORE).allowed)

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

  // Existing PDF logo test preserved cleanly
  @Test
  fun `invoice PDF centers logo with strict aspect ratio and skips logo cleanly when missing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val outDir = File(context.cacheDir, "pdf_logo_test").apply { mkdirs() }

    assertNull(InvoicePdfGenerator.loadLogoBitmap(null))
    assertNull(InvoicePdfGenerator.loadLogoBitmap(""))
    assertNull(InvoicePdfGenerator.loadLogoBitmap(File(outDir, "does_not_exist.png").absolutePath))

    val wideRect = InvoicePdfGenerator.computeTopCenteredLogoRect(595f, 26f, 240, 80, 156f, 76f)!!
    assertEquals(3f, wideRect.width() / wideRect.height(), 0.01f)
    assertEquals(297.5f, (wideRect.left + wideRect.right) / 2f, 0.1f)

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

  // Case 13: Logout & account switching clears previous user license/trial/payment state cleanly
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
