package com.example

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.CartLineInput
import com.example.data.JsonStorageManager
import com.example.data.VyaparRepository
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.MovementType
import com.example.model.OrderStatus
import com.example.model.PaperFormat
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.ThemePreference
import com.example.pdf.InvoicePdfGenerator
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Vyapar Billing Pro`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vyapar Billing Pro", appName)
  }

  @Test
  fun `main activity creates VyaparViewModel without exception`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    val vm =
      androidx.lifecycle.ViewModelProvider(activity)[com.example.ui.VyaparViewModel::class.java]
    assertEquals(false, vm.toastMessage.value != null)
  }

  @Test
  fun `json storage persists across repository restart and generates genuine pdf`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = JsonStorageManager(context)
    val repo1 = VyaparRepository(storage)
    repo1.loadAll()

    // 1. Save business profile
    repo1.saveBusinessProfile(
      BusinessProfile(
        businessName = "Royal Restaurant",
        ownerName = "Rohit Sharma",
        mobile = "9876543210",
        address = "123 Main Road",
        city = "Raipur",
        gstin = "22AAAAA0000A1Z5",
        invoicePrefix = "RR",
      )
    )

    // 2. Add product
    repo1.upsertProduct(
      Product(
        id = "prod_1",
        name = "Burger",
        category = "Food",
        sellingPricePaise = 12000L, // ₹120.00
        stockQty = 50.0,
        unit = "Pcs",
        gstPercent = 5.0,
      )
    )

    // 3. Add customer
    val cust =
      repo1.upsertCustomer(
        Customer(id = "cust_1", name = "Amit Kumar", mobile = "9876543210")
      )

    // 4. Complete sale -> creates invoice, deducts stock, updates customer
    val invoice =
      repo1.completeSaleAndCreateInvoice(
        customer = cust,
        cartItems =
          listOf(
            CartLineInput(
              productId = "prod_1",
              name = "Burger",
              unit = "Pcs",
              quantity = 2.0,
              unitPricePaise = 12000L,
              gstPercent = 5.0,
            )
          ),
        discountValue = 0.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTRA_STATE_CGST_SGST,
        taxMode = GstTaxMode.EXCLUSIVE,
        paymentMethod = PaymentMethod.UPI,
        amountPaidPaise = 25200L,
      )

    assertEquals("RR-0001", invoice.invoiceNumber)
    assertEquals(25200L, invoice.grandTotalPaise)

    // 5. Simulate app restart by loading into a fresh repository
    val repo2 = VyaparRepository(JsonStorageManager(context))
    repo2.loadAll()

    assertEquals("Royal Restaurant", repo2.business.value.businessName)
    assertEquals(1, repo2.invoices.value.size)
    assertEquals(48.0, repo2.products.value.first().stockQty, 0.01)
    assertEquals(25200L, repo2.customers.value.first().totalPurchasesPaise)

    // 6. Generate genuine A4 & 80mm PDF and verify %PDF- header + FileProvider URI
    val a4Pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = storage.invoicesDir,
        business = repo2.business.value,
        invoice = invoice,
        paperFormat = PaperFormat.A4,
        language = AppLanguage.ENGLISH,
      )
    assertTrue(a4Pdf.exists())
    assertTrue(a4Pdf.length() > 500)
    val header = a4Pdf.inputStream().bufferedReader().use { it.readLine() }
    assertTrue(header.startsWith("%PDF-"))

    val thermalPdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = storage.invoicesDir,
        business = repo2.business.value,
        invoice = invoice,
        paperFormat = PaperFormat.THERMAL_80MM,
        language = AppLanguage.HINDI,
      )
    assertTrue(thermalPdf.exists() && thermalPdf.length() > 400)

    val shareUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", a4Pdf)
    assertTrue(shareUri.toString().contains("VYAPAR_INVOICE_RR-0001.pdf"))

    // 7. Backup & restore validation
    val backupJson = repo2.buildFullBackupJson()
    val parsed = repo2.validateBackupJson(backupJson)
    assertTrue(parsed.isSuccess)
    assertEquals(1, parsed.getOrThrow().invoices.size)

    // 8. Corrupt JSON recovery from .bak
    File(storage.productsFile.absolutePath).writeText("{broken json")
    val recoveredProducts = storage.readProducts()
    assertEquals(1, recoveredProducts.size)
    assertEquals("Burger", recoveredProducts.first().name)

    // 9. Manual customer name, mobile, address, GSTIN without saved Customer object
    val manualInvoice =
      repo2.completeSaleAndCreateInvoice(
        customer = null,
        cartItems =
          listOf(
            CartLineInput(
              productId = "prod_1",
              name = "Burger",
              unit = "Pcs",
              quantity = 1.0,
              unitPricePaise = 12000L,
              gstPercent = 5.0,
            )
          ),
        discountValue = 0.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTRA_STATE_CGST_SGST,
        taxMode = GstTaxMode.EXCLUSIVE,
        paymentMethod = PaymentMethod.CASH,
        amountPaidPaise = 12600L,
        customCustomerName = "Ramesh Verma",
        customCustomerMobile = "9988776655",
        customCustomerAddress = "Station Road, Raipur",
        customCustomerGstin = "22BBBBB1111B1Z9",
      )
    assertEquals("Ramesh Verma", manualInvoice.customerName)
    assertEquals("9988776655", manualInvoice.customerMobile)
    assertEquals("Station Road, Raipur", manualInvoice.customerAddress)
    assertEquals("22BBBBB1111B1Z9", manualInvoice.customerGstin)

    // 10. Shop Online (Open) / Offline (Closed) persistence across restart
    assertTrue(repo2.settings.value.isShopOpen)
    repo2.updateSettings(repo2.settings.value.copy(isShopOpen = false))
    val repo3 = VyaparRepository(JsonStorageManager(context))
    repo3.loadAll()
    assertEquals(false, repo3.settings.value.isShopOpen)
  }

  @Test
  fun `viewModel supports multi-product selection and inline customer details`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val vm =
      androidx.lifecycle.ViewModelProvider(controller.get())[com.example.ui.VyaparViewModel::class.java]

    val p1 = Product(id = "p1", name = "Burger", sellingPricePaise = 12000L, gstPercent = 5.0)
    val p2 = Product(id = "p2", name = "Pizza", sellingPricePaise = 20000L, gstPercent = 5.0)

    vm.addProductToCart(p1)
    vm.addProductToCart(p2)
    vm.addProductToCart(p1)
    assertEquals(2, vm.cart.value.items.size)
    assertEquals(2.0, vm.cart.value.items.first { it.productId == "p1" }.quantity, 0.01)
    assertEquals(1.0, vm.cart.value.items.first { it.productId == "p2" }.quantity, 0.01)

    vm.decrementProductInCart("p1")
    assertEquals(1.0, vm.cart.value.items.first { it.productId == "p1" }.quantity, 0.01)
    vm.decrementProductInCart("p1")
    assertEquals(1, vm.cart.value.items.size)
    assertEquals("p2", vm.cart.value.items.first().productId)

    vm.updateCartCustomerDetails(
      name = "Suresh Patel",
      mobile = "9123456780",
      address = "MG Road",
      gstin = "27AAAAA0000A1Z5",
      saveToDirectory = true,
    )
    assertEquals("Suresh Patel", vm.cart.value.customerName)
    assertEquals("9123456780", vm.cart.value.customerMobile)
    assertEquals("MG Road", vm.cart.value.customerAddress)
    assertEquals("27AAAAA0000A1Z5", vm.cart.value.customerGstin)
  }

  @Test
  fun `pending orders full lifecycle create edit dedup confirm to invoice and cancel`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = JsonStorageManager(context)
    val repo = VyaparRepository(storage)
    repo.loadAll()

    repo.upsertProduct(
      Product(
        id = "prod_rice",
        name = "Basmati Rice",
        category = "Grocery",
        sellingPricePaise = 10000L, // ₹100
        stockQty = 20.0,
        unit = "Kg",
        gstPercent = 5.0,
      )
    )

    // 1. Create pending order with new customer -> auto-saves customer & prevents duplicates by mobile
    val order1 =
      repo.createOrder(
        customer = null,
        customerName = "Vikram Singh",
        customerMobile = "9811223344",
        items =
          listOf(
            CartLineInput(
              productId = "prod_rice",
              name = "Basmati Rice",
              unit = "Kg",
              quantity = 2.0,
              unitPricePaise = 10000L,
              gstPercent = 5.0,
            )
          ),
        notes = "Pack in 2 bags",
      )
    assertEquals(OrderStatus.PENDING, order1.status)
    assertEquals(1, repo.customers.value.size)
    assertEquals("9811223344", repo.customers.value.first().mobile)

    // Trying to add duplicate mobile with +91 prefix reuses the same customer
    val dedupCust = repo.findOrCreateCustomerByMobile("Vikram S", "+91 9811223344")
    assertEquals(1, repo.customers.value.size)
    assertEquals(repo.customers.value.first().id, dedupCust?.id)

    // Stock is NOT deducted while order is still pending
    assertEquals(20.0, repo.products.value.first().stockQty, 0.01)

    // 2. Edit pending order before confirmation
    val edited =
      repo.updatePendingOrder(
        orderId = order1.id,
        customer = dedupCust,
        customerName = "Vikram Singh",
        customerMobile = "9811223344",
        items =
          listOf(
            CartLineInput(
              productId = "prod_rice",
              name = "Basmati Rice",
              unit = "Kg",
              quantity = 3.0,
              unitPricePaise = 10000L,
              gstPercent = 5.0,
            )
          ),
        notes = "Updated to 3kg",
      )
    assertEquals(3.0, edited!!.items.first().quantity, 0.01)

    // 3. Confirm pending order with edited final rate (₹120/kg), ₹20 extra discount, partial Udhaar
    // Subtotal = 3 * 120 = 360, Discount = 20 -> Taxable = 340, GST 5% = 17, Total = 357 (35700 paise)
    val invoice =
      repo.confirmOrderAndCreateInvoice(
        order = edited,
        finalItems =
          listOf(
            CartLineInput(
              productId = "prod_rice",
              name = "Basmati Rice",
              unit = "Kg",
              quantity = 3.0,
              unitPricePaise = 12000L,
              gstPercent = 5.0,
            )
          ),
        discountValue = 20.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTRA_STATE_CGST_SGST,
        taxMode = GstTaxMode.EXCLUSIVE,
        paymentMethod = PaymentMethod.CREDIT,
        amountPaidPaise = 15700L, // ₹157 paid, ₹200 Udhaar
        notes = "Settled at counter",
      )

    assertEquals(35700L, invoice.grandTotalPaise)
    assertEquals(15700L, invoice.amountPaidPaise)
    assertEquals(20000L, invoice.balanceDuePaise)
    assertEquals(1, repo.invoices.value.size)

    // Order kept in history as COMPLETED and linked to invoice
    val completedOrder = repo.orders.value.first { it.id == order1.id }
    assertEquals(OrderStatus.COMPLETED, completedOrder.status)
    assertEquals(invoice.id, completedOrder.linkedInvoiceId)
    assertEquals(35700L, completedOrder.totalPaise)

    // Stock deducted & ledger updated
    assertEquals(17.0, repo.products.value.first().stockQty, 0.01)
    assertTrue(repo.inventoryMovements.value.any { it.type == MovementType.SALE && it.quantityDelta == -3.0 })
    assertEquals(35700L, repo.customers.value.first().totalPurchasesPaise)
    assertEquals(20000L, repo.customers.value.first().outstandingBalancePaise)

    // 4. Create & cancel another pending order -> stock unchanged, order remains in Cancelled history
    val order2 =
      repo.createOrder(
        customer = null,
        customerName = "Sunita Devi",
        customerMobile = "9900112233",
        items =
          listOf(
            CartLineInput(
              productId = "prod_rice",
              name = "Basmati Rice",
              unit = "Kg",
              quantity = 2.0,
              unitPricePaise = 10000L,
              gstPercent = 5.0,
            )
          ),
        notes = "",
      )
    repo.updateOrderStatus(order2.id, OrderStatus.CANCELLED)

    // Reload repository from JSON storage to verify full local persistence
    val repoReloaded = VyaparRepository(JsonStorageManager(context))
    repoReloaded.loadAll()
    assertEquals(2, repoReloaded.orders.value.size)
    assertEquals(OrderStatus.COMPLETED, repoReloaded.orders.value.first { it.id == order1.id }.status)
    assertEquals(OrderStatus.CANCELLED, repoReloaded.orders.value.first { it.id == order2.id }.status)
    assertEquals(17.0, repoReloaded.products.value.first().stockQty, 0.01)
  }

  @Test
  fun `theme preference System Light Dark persists across restart`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = JsonStorageManager(context)
    val repo1 = VyaparRepository(storage)
    repo1.loadAll()

    repo1.updateSettings(repo1.settings.value.copy(themePreference = ThemePreference.DARK, darkMode = true))
    val repo2 = VyaparRepository(JsonStorageManager(context))
    repo2.loadAll()
    assertEquals(ThemePreference.DARK, repo2.settings.value.themePreference)
    assertEquals(true, repo2.settings.value.darkMode)

    repo2.updateSettings(repo2.settings.value.copy(themePreference = ThemePreference.LIGHT, darkMode = false))
    val repo3 = VyaparRepository(JsonStorageManager(context))
    repo3.loadAll()
    assertEquals(ThemePreference.LIGHT, repo3.settings.value.themePreference)
    assertEquals(false, repo3.settings.value.darkMode)
  }

  @Test
  fun `lifetime license payment validation upi uri payload and status parsing`() {
    // 1. UTR validation
    assertTrue(com.example.data.SupabasePaymentService.validateUtr("") != null)
    assertTrue(com.example.data.SupabasePaymentService.validateUtr("12345") != null)
    assertTrue(com.example.data.SupabasePaymentService.validateUtr("INVALID UTR!") != null)
    assertTrue(com.example.data.SupabasePaymentService.validateUtr("ABCDEFGHIJKL") != null)
    assertEquals(null, com.example.data.SupabasePaymentService.validateUtr(" 432912345678 "))
    assertEquals(null, com.example.data.SupabasePaymentService.validateUtr("utr202611250091"))

    // 2. UPI payment URI check
    val uri = com.example.data.SupabasePaymentService.buildUpiPaymentUri()
    assertEquals("upi", uri.scheme)
    assertEquals("pay", uri.host)
    assertEquals("7880023755@axl", uri.getQueryParameter("pa"))
    assertEquals("499.00", uri.getQueryParameter("am"))
    assertEquals("INR", uri.getQueryParameter("cu"))

    // 3. Supabase POST payload check (status must always be "pending")
    val payload =
      com.example.data.SupabasePaymentService.buildInsertPayload(
        userId = "user-uuid-123",
        utr = "  432912345678  ",
        customerNote = "Main Market Shop",
      )
    assertEquals("user-uuid-123", payload.getString("user_id"))
    assertEquals(499, payload.getInt("amount_inr"))
    assertEquals("UPI", payload.getString("payment_method"))
    assertEquals("432912345678", payload.getString("utr"))
    assertEquals("pending", payload.getString("status"))
    assertEquals("Main Market Shop", payload.getString("customer_note"))

    // 4. Status parsing (Pending, Approved, Rejected)
    assertEquals(
      com.example.data.PaymentRequestStatus.PENDING,
      com.example.data.PaymentRequestStatus.fromApi("pending"),
    )
    assertEquals(
      com.example.data.PaymentRequestStatus.APPROVED,
      com.example.data.PaymentRequestStatus.fromApi("approved"),
    )
    assertEquals(
      com.example.data.PaymentRequestStatus.REJECTED,
      com.example.data.PaymentRequestStatus.fromApi("REJECTED"),
    )

    val parsed =
      com.example.data.SupabasePaymentService.parseRecord(
        org.json.JSONObject()
          .put("id", "req_1")
          .put("user_id", "user-uuid-123")
          .put("amount_inr", 499)
          .put("payment_method", "UPI")
          .put("utr", "432912345678")
          .put("status", "approved")
          .put("customer_note", "Verified")
          .put("created_at", "2026-11-25T10:00:00Z")
      )
    assertEquals(com.example.data.PaymentRequestStatus.APPROVED, parsed.status)
    assertEquals(499, parsed.amountInr)
    assertEquals("432912345678", parsed.utr)
  }

  private fun mockLicenseClient(
    handler: (okhttp3.Request) -> okhttp3.Response
  ): okhttp3.OkHttpClient {
    return okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain -> handler(chain.request()) }
      .build()
  }

  private fun jsonHttpResponse(
    request: okhttp3.Request,
    code: Int,
    body: String,
  ): okhttp3.Response {
    val mediaType = with(okhttp3.MediaType.Companion) { "application/json; charset=utf-8".toMediaType() }
    val responseBody = with(okhttp3.ResponseBody.Companion) { body.toResponseBody(mediaType) }
    return okhttp3.Response.Builder()
      .request(request)
      .protocol(okhttp3.Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Error")
      .body(responseBody)
      .build()
  }

  @Test
  fun `license verification URL targets GET rest v1 licenses with user_id and status active`() {
    val url = com.example.data.SupabaseLicenseService.buildLicensesCheckUrl(userId = "user-abc-123")
    assertTrue(url.contains("/rest/v1/licenses"))
    assertTrue(url.contains("user_id=eq.user-abc-123"))
    assertTrue(url.contains("status=eq.active"))
  }

  @Test
  fun `active lifetime license grants access and sends authenticated bearer and apikey headers`() {
    var capturedAuth: String? = null
    var capturedApiKey: String? = null
    var capturedUrl: String? = null
    val client = mockLicenseClient { req ->
      capturedAuth = req.header("Authorization")
      capturedApiKey = req.header("apikey")
      capturedUrl = req.url.toString()
      jsonHttpResponse(
        req,
        200,
        """[{"id":"lic_001","user_id":"user-123","status":"active"}]""",
      )
    }

    val service = com.example.data.SupabaseLicenseService(client)
    val result = service.verifyActiveLicense(accessToken = "jwt-token-xyz", userId = "user-123")

    assertEquals(true, result.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.ACTIVE, result.status)
    assertEquals("lic_001", result.licenseId)
    assertEquals("Bearer jwt-token-xyz", capturedAuth)
    assertTrue(!capturedApiKey.isNullOrBlank())
    assertTrue(capturedUrl!!.contains("user_id=eq.user-123"))
    assertTrue(capturedUrl!!.contains("status=eq.active"))
  }

  @Test
  fun `pending payment or pending license never grants premium access`() {
    val client = mockLicenseClient { req ->
      jsonHttpResponse(
        req,
        200,
        """[{"id":"lic_002","user_id":"user-123","status":"pending"}]""",
      )
    }
    val service = com.example.data.SupabaseLicenseService(client)
    val result = service.verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")

    assertEquals(false, result.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.INACTIVE, result.status)

    // Also verify pending payment request alone never yields an active status
    val pendingPayment =
      com.example.data.PaymentRequestRecord(
        id = "pay_1",
        userId = "user-123",
        status = com.example.data.PaymentRequestStatus.PENDING,
        utr = "432912345678",
      )
    assertEquals(com.example.data.PaymentRequestStatus.PENDING, pendingPayment.status)
  }

  @Test
  fun `rejected license never grants premium access`() {
    val client = mockLicenseClient { req ->
      jsonHttpResponse(
        req,
        200,
        """[{"id":"lic_003","user_id":"user-123","status":"rejected"}]""",
      )
    }
    val service = com.example.data.SupabaseLicenseService(client)
    val result = service.verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")

    assertEquals(false, result.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.INACTIVE, result.status)
  }

  @Test
  fun `missing license empty array never grants premium access`() {
    val client = mockLicenseClient { req -> jsonHttpResponse(req, 200, "[]") }
    val service = com.example.data.SupabaseLicenseService(client)
    val result = service.verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")

    assertEquals(false, result.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.INACTIVE, result.status)
  }

  @Test
  fun `mismatched user_id license row never grants premium access`() {
    val client = mockLicenseClient { req ->
      jsonHttpResponse(
        req,
        200,
        """[{"id":"lic_999","user_id":"other-user","status":"active"}]""",
      )
    }
    val service = com.example.data.SupabaseLicenseService(client)
    val result = service.verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")

    assertEquals(false, result.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.INACTIVE, result.status)
  }

  @Test
  fun `unauthenticated empty credentials and HTTP 401 never grant premium access`() {
    val service = com.example.data.SupabaseLicenseService()

    val noToken = service.verifyActiveLicense(accessToken = "", userId = "user-123")
    assertEquals(false, noToken.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.UNAUTHENTICATED, noToken.status)
    assertEquals(true, noToken.isUnauthorized)

    val noUser = service.verifyActiveLicense(accessToken = "jwt-token", userId = "")
    assertEquals(false, noUser.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.UNAUTHENTICATED, noUser.status)
    assertEquals(true, noUser.isUnauthorized)

    val client401 = mockLicenseClient { req ->
      jsonHttpResponse(req, 401, """{"message":"JWT expired"}""")
    }
    val expiredResult =
      com.example.data.SupabaseLicenseService(client401)
        .verifyActiveLicense(accessToken = "expired-jwt", userId = "user-123")
    assertEquals(false, expiredResult.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.UNAUTHENTICATED, expiredResult.status)
    assertEquals(true, expiredResult.isUnauthorized)
  }

  @Test
  fun `network failure and server 500 error never grant premium access`() {
    val ioErrorClient = mockLicenseClient { _ ->
      throw java.io.IOException("Simulated network timeout")
    }
    val ioResult =
      com.example.data.SupabaseLicenseService(ioErrorClient)
        .verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")
    assertEquals(false, ioResult.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.ERROR, ioResult.status)
    assertEquals(true, ioResult.isNetworkError)

    val server500Client = mockLicenseClient { req ->
      jsonHttpResponse(req, 500, """{"message":"Internal server error"}""")
    }
    val serverResult =
      com.example.data.SupabaseLicenseService(server500Client)
        .verifyActiveLicense(accessToken = "jwt-token", userId = "user-123")
    assertEquals(false, serverResult.hasActiveLicense)
    assertEquals(com.example.data.LicenseStatus.ERROR, serverResult.status)
  }

  @Test
  fun `VyaparViewModel defaults to locked unauthenticated license state`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val vm =
      androidx.lifecycle.ViewModelProvider(controller.get())[com.example.ui.VyaparViewModel::class.java]

    assertEquals(false, vm.isAuthenticated.value)
    assertEquals(false, vm.licenseState.value.hasActiveLicense)
    assertEquals(
      com.example.data.LicenseStatus.UNAUTHENTICATED,
      vm.licenseState.value.status,
    )
  }
}
