package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.BillingCalculator
import com.example.data.CartLineInput
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.PaperFormat
import com.example.model.PaymentMethod
import com.example.pdf.InvoicePdfGenerator
import com.example.pdf.ShareAndPrintHelper
import com.example.pdf.UpiQrGenerator
import com.example.printer.BluetoothPrinterDevice
import com.example.printer.BluetoothPrinterManager
import com.example.printer.BluetoothSystemBridge
import com.example.printer.EscPosFormatter
import com.example.printer.PrinterConnectionStatus
import com.example.printer.PrinterTransport
import com.example.printer.ThermalPaperWidth
import com.example.printer.ThermalPrinterPrefs
import com.example.printer.ThermalPrinterUiState
import com.example.ui.screens.InvoicePreviewScreen
import com.example.ui.theme.VyaparBillingTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InvoiceAndPrinterTest {

  @get:Rule val composeRule = createComposeRule()

  private class FakeBluetoothBridge(
    var supported: Boolean = true,
    var enabled: Boolean = true,
    var permissions: Boolean = true,
    var bonded: List<BluetoothPrinterDevice> = emptyList(),
    var nearbyOnScan: List<BluetoothPrinterDevice> = emptyList(),
  ) : BluetoothSystemBridge {
    override fun isBluetoothSupported(): Boolean = supported
    override fun isBluetoothEnabled(): Boolean = enabled
    override fun hasBluetoothPermissions(): Boolean = permissions
    override fun getBondedDevices(): List<BluetoothPrinterDevice> = bonded
    override fun startDiscovery(
      onDeviceFound: (BluetoothPrinterDevice) -> Unit,
      onFinished: () -> Unit,
    ): Boolean {
      if (!supported || !enabled || !permissions) return false
      nearbyOnScan.forEach(onDeviceFound)
      onFinished()
      return true
    }

    override fun stopDiscovery() {}
  }

  private class FakePrinterTransport(
    var failConnect: Boolean = false,
    var failDuringWrite: Boolean = false,
  ) : PrinterTransport {
    override var connectedAddress: String? = null
    override var isConnected: Boolean = false
    val writtenPayloads = mutableListOf<ByteArray>()

    override fun connect(address: String) {
      if (failConnect) throw IOException("Simulated unreachable printer")
      connectedAddress = address
      isConnected = true
    }

    override fun writeAndFlush(data: ByteArray) {
      if (failDuringWrite) {
        isConnected = false
        throw IOException("Simulated socket disconnect mid-print")
      }
      writtenPayloads.add(data)
    }

    override fun close() {
      isConnected = false
      connectedAddress = null
    }
  }

  private fun createSampleLogoFile(dir: File, width: Int = 300, height: Int = 100): File {
    dir.mkdirs()
    val file = File(dir, "test_store_logo.png")
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(android.graphics.Color.rgb(249, 115, 22))
    FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
    bmp.recycle()
    return file
  }

  private fun buildRichInvoice(
    itemCount: Int = 3,
    longNames: Boolean = false,
    taxType: GstTaxType = GstTaxType.INTRA_STATE_CGST_SGST,
    discountValue: Double = 50.0,
  ): Invoice {
    val lines =
      (1..itemCount).map { idx ->
        val name =
          if (longNames) {
            "Item $idx Ultra Premium Organic Handcrafted Extra Virgin Olive Oil Special Kitchen Reserve Pack 1000ml"
          } else {
            "Sample Product Item $idx"
          }
        CartLineInput(
          productId = "p_$idx",
          name = name,
          unit = "Pcs",
          quantity = 2.0,
          unitPricePaise = 25000L,
          gstPercent = if (taxType == GstTaxType.NO_GST) 0.0 else 18.0,
        )
      }
    val calc =
      BillingCalculator.calculate(
        items = lines,
        discountValue = discountValue,
        isPercentageDiscount = false,
        taxType = taxType,
        taxMode = GstTaxMode.EXCLUSIVE,
        autoRoundOff = true,
      )
    return Invoice(
      id = "inv_test_1",
      invoiceNumber = "VBP-2026-0042",
      timestamp = 1764060000000L,
      customerName =
        if (longNames) "Shri Raghavendra Enterprise & Super Market Wholesale Distributors Pvt Ltd"
        else "Amit Kumar",
      customerMobile = "9876543210",
      customerAddress =
        if (longNames) "Plot 44, Near Clock Tower Square, Pandri Commercial Complex, Raipur, Chhattisgarh - 492001"
        else "12 MG Road, Raipur",
      customerGstin = "22ABCDE1234F1Z5",
      items = calc.computedItems,
      subtotalPaise = calc.subtotalPaise,
      discountPaise = calc.discountPaise,
      discountInput = discountValue,
      isPercentageDiscount = false,
      taxType = taxType,
      taxMode = GstTaxMode.EXCLUSIVE,
      cgstPaise = calc.cgstPaise,
      sgstPaise = calc.sgstPaise,
      igstPaise = calc.igstPaise,
      totalTaxPaise = calc.totalTaxPaise,
      roundOffPaise = calc.roundOffPaise,
      grandTotalPaise = calc.grandTotalPaise,
      amountPaidPaise = calc.grandTotalPaise - 10000L,
      balanceDuePaise = 10000L,
      paymentMethod = PaymentMethod.UPI,
    )
  }

  // 1. Invoice with store logo (strict aspect ratio & top-centered)
  @Test
  fun `1 store logo rect preserves original aspect ratio and centers horizontally`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val logoFile = createSampleLogoFile(File(context.filesDir, "logos_test"), width = 300, height = 100)
    val rect =
      InvoicePdfGenerator.computeTopCenteredLogoRect(
        pageWidth = 595f,
        topY = 20f,
        bitmapWidth = 300,
        bitmapHeight = 100,
        maxWidth = 176f,
        maxHeight = 82f,
      )
    assertNotNull(rect)
    val aspect = rect!!.width() / rect.height()
    assertEquals(3.0f, aspect, 0.01f)
    assertEquals(595f / 2f, rect.centerX(), 0.5f)

    val pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = File(context.filesDir, "pdf_test_1"),
        business = BusinessProfile(businessName = "Logo Shop", logoPath = logoFile.absolutePath),
        invoice = buildRichInvoice(),
        paperFormat = PaperFormat.A4,
        language = AppLanguage.ENGLISH,
      )
    assertTrue(pdf.exists() && pdf.length() > 500L)
    assertTrue(pdf.readText().startsWith("%PDF-"))
  }

  // 2. Invoice without store logo (no placeholder)
  @Test
  fun `2 invoice without logo renders cleanly with null logo bitmap`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNull(InvoicePdfGenerator.loadLogoBitmap(null))
    assertNull(InvoicePdfGenerator.loadLogoBitmap("/non/existent/logo.png"))

    val pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = File(context.filesDir, "pdf_test_2"),
        business = BusinessProfile(businessName = "No Logo Mart", logoPath = null),
        invoice = buildRichInvoice(),
        paperFormat = PaperFormat.A4,
        language = AppLanguage.ENGLISH,
      )
    assertTrue(pdf.exists() && pdf.readText().startsWith("%PDF-"))
  }

  // 3, 4, 5. Long business name, long customer name & multi-line address, long product names
  @Test
  fun `3 4 5 long business customer address and product names wrap without truncation`() {
    val longBiz =
      "Shree Ganesh Mega Mart & Departmental Superstore Enterprises Private Limited India"
    val invoice = buildRichInvoice(itemCount = 2, longNames = true)
    val business =
      BusinessProfile(
        businessName = longBiz,
        address = "Shop 101-108, Ground Floor, Landmark Tower, Opposite Railway Station",
        city = "Raipur",
        state = "Chhattisgarh",
        pinCode = "492001",
        mobile = "9876543210",
        email = "billing@shreeganeshmegamart.in",
        gstin = "22AAAAA0000A1Z5",
      )

    val paint = Paint().apply { textSize = 10f }
    val wrappedBiz = InvoicePdfGenerator.wrapTextLines(longBiz, paint, 160f)
    assertTrue(wrappedBiz.size >= 2)
    assertTrue(wrappedBiz.joinToString(" ").contains("India"))

    val fallbackLines =
      InvoicePdfGenerator.buildFallbackTextLines(business, invoice, AppLanguage.ENGLISH, PaperFormat.A4)
    val fullDump = fallbackLines.joinToString("\n")
    assertTrue(fullDump.contains(longBiz))
    assertTrue(fullDump.contains(invoice.customerName))
    assertTrue(fullDump.contains(invoice.customerAddress))
    assertTrue(fullDump.contains("Reserve Pack 1000ml"))

    // ESC/POS 58mm wrapping preserves every word cleanly
    val rawThermal =
      EscPosFormatter.buildEscPosReceipt(
        business = business,
        invoice = invoice,
        paperWidth = ThermalPaperWidth.MM_58,
      ).toString(Charsets.US_ASCII)
    assertTrue(rawThermal.contains("Private Limited"))
    assertTrue(rawThermal.contains("Chhattisgarh"))
    assertTrue(rawThermal.contains("Reserve Pack 1000ml"))
  }

  // 6. Multiple products and multi-page pagination
  @Test
  fun `6 many products trigger clean multi page A4 pdf generation without crashing or dropping items`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val invoice = buildRichInvoice(itemCount = 60, longNames = true)
    val business = BusinessProfile(businessName = "Bulk Mart", mobile = "9998887776")

    val pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = File(context.filesDir, "pdf_test_multipage"),
        business = business,
        invoice = invoice,
        paperFormat = PaperFormat.A4,
        language = AppLanguage.ENGLISH,
      )
    assertTrue(pdf.exists())
    val content = pdf.readText(Charsets.ISO_8859_1)
    assertTrue(content.startsWith("%PDF-"))
    // Either native Skia or JvmPdfFallbackWriter emits multiple /Page objects for 60 items
    val pageCount = Regex("/Type /Page\\b").findAll(content).count()
    assertTrue("Expected multi-page PDF for 60 items, got $pageCount", pageCount >= 2)
  }

  // 7. GST (CGST/SGST & IGST) and Discounts
  @Test
  fun `7 cgst sgst igst and discounts appear accurately in pdf and escpos`() {
    val intraInv = buildRichInvoice(itemCount = 2, taxType = GstTaxType.INTRA_STATE_CGST_SGST, discountValue = 100.0)
    val interInv = buildRichInvoice(itemCount = 2, taxType = GstTaxType.INTER_STATE_IGST, discountValue = 50.0)
    val biz = BusinessProfile(businessName = "GST Store")

    val intraText =
      EscPosFormatter.buildEscPosReceipt(biz, intraInv, ThermalPaperWidth.MM_80).toString(Charsets.US_ASCII)
    assertTrue(intraText.contains("TAX INVOICE"))
    assertTrue(intraText.contains("Discount"))
    assertTrue(intraText.contains("CGST"))
    assertTrue(intraText.contains("SGST"))

    val interText =
      EscPosFormatter.buildEscPosReceipt(biz, interInv, ThermalPaperWidth.MM_80).toString(Charsets.US_ASCII)
    assertTrue(interText.contains("IGST"))
  }

  // 8. Real UPI QR code when valid UPI ID present; hidden completely when absent/invalid
  @Test
  fun `8 upi qr generated only when valid business upi id exists and hidden otherwise`() {
    val invoice = buildRichInvoice()

    val noUpiBiz = BusinessProfile(businessName = "No UPI Shop", upiId = "")
    assertNull(UpiQrGenerator.buildUpiPaymentUri(noUpiBiz, invoice))
    assertNull(UpiQrGenerator.generateQrBitmap(noUpiBiz, invoice))

    val badUpiBiz = BusinessProfile(businessName = "Bad UPI Shop", upiId = "not a valid upi")
    assertFalse(UpiQrGenerator.isValidUpiId(badUpiBiz.upiId))
    assertNull(UpiQrGenerator.generateQrBitmap(badUpiBiz, invoice))

    val validUpiBiz = BusinessProfile(businessName = "Valid UPI Shop", upiId = "myshop@okicici")
    val uri = UpiQrGenerator.buildUpiPaymentUri(validUpiBiz, invoice)
    assertNotNull(uri)
    assertTrue(uri!!.startsWith("upi://pay?pa=myshop%40okicici"))
    assertTrue(uri.contains("am=100.00"))
    assertTrue(uri.contains("cu=INR"))

    val qrBitmap = UpiQrGenerator.generateQrBitmap(validUpiBiz, invoice, sizePx = 180)
    assertNotNull(qrBitmap)
    assertEquals(180, qrBitmap!!.width)
    assertEquals(180, qrBitmap.height)
    qrBitmap.recycle()

    val escNoUpi =
      EscPosFormatter.buildEscPosReceipt(noUpiBiz, invoice, ThermalPaperWidth.MM_80)
        .toString(Charsets.US_ASCII)
    assertFalse(escNoUpi.contains("Scan to Pay"))
  }

  // 9 & 10. PDF preview, download SAF copy, and FileProvider PDF share URI
  @Test
  fun `9 10 pdf saf export and fileprovider share uri work cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dir = File(context.filesDir, "pdf_share_test")
    val pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = dir,
        business = BusinessProfile(businessName = "Share Mart"),
        invoice = buildRichInvoice(),
        paperFormat = PaperFormat.THERMAL_58MM,
        language = AppLanguage.ENGLISH,
      )
    assertTrue(pdf.exists())
    assertTrue(pdf.name.endsWith("_58mm.pdf"))

    val exported = File(dir, "downloaded_copy.pdf")
    val ok = ShareAndPrintHelper.writeFileToSafUri(context, pdf, Uri.fromFile(exported))
    assertTrue(ok)
    assertTrue(exported.exists() && exported.length() == pdf.length())

    val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
    assertTrue(shareUri.toString().contains("VYAPAR_INVOICE_VBP-2026-0042_58mm.pdf"))
  }

  // 11, 12, 13. Discovery, persistent selection, connection states, 58mm & 80mm ESC/POS
  @Test
  fun `11 12 13 bluetooth printer discovery selection persistence connect disconnect and 58mm 80mm print`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val prefs = ThermalPrinterPrefs(context)
      prefs.clearSelectedPrinter()

      val printer58 = BluetoothPrinterDevice("RPP02N-58mm", "00:11:22:33:44:55", isBonded = true, isLikelyPrinter = true)
      val printer80 = BluetoothPrinterDevice("XPrinter-80mm", "AA:BB:CC:DD:EE:FF", isBonded = false, isLikelyPrinter = true)

      val bridge = FakeBluetoothBridge(bonded = listOf(printer58), nearbyOnScan = listOf(printer80))
      val transport = FakePrinterTransport()
      val manager = BluetoothPrinterManager(prefs, bridge, transport, Dispatchers.Unconfined)

      // Paired device populated
      assertEquals(1, manager.state.value.availableDevices.size)
      manager.startDiscovery()
      assertEquals(2, manager.state.value.availableDevices.size)

      // Select & persist
      manager.selectPrinter(printer80)
      manager.updatePaperWidth(ThermalPaperWidth.MM_58)
      assertEquals("00:11:22:33:44:55".let { printer80.address }, prefs.load().address)
      assertEquals(ThermalPaperWidth.MM_58, prefs.load().paperWidth)

      // Connect
      val connRes = manager.connectSelectedPrinter()
      assertTrue(connRes.isSuccess)
      assertEquals(PrinterConnectionStatus.CONNECTED, manager.state.value.connectionStatus)

      val business = BusinessProfile(businessName = "Thermal Shop", upiId = "shop@ybl")
      val invoice = buildRichInvoice()

      // Print 58mm with UPI QR raster + logo absent + autoCut enabled
      val res58 = manager.printInvoice(business, invoice, AppLanguage.ENGLISH)
      assertTrue(res58.isSuccess)
      assertEquals(PrinterConnectionStatus.CONNECTED, manager.state.value.connectionStatus)
      val payload58 = transport.writtenPayloads.last()
      val text58 = payload58.toString(Charsets.US_ASCII)
      assertTrue(text58.contains("Scan to Pay"))
      assertTrue(text58.contains("shop@ybl"))
      // Verify GS v 0 raster command present for UPI QR
      assertTrue(payload58.indices.any { i -> i + 2 < payload58.size && payload58[i] == 0x1D.toByte() && payload58[i + 1] == 0x76.toByte() && payload58[i + 2] == 0x30.toByte() })

      // Switch to 80mm and print
      manager.updatePaperWidth(ThermalPaperWidth.MM_80)
      val res80 = manager.printInvoice(business, invoice, AppLanguage.ENGLISH)
      assertTrue(res80.isSuccess)
      val payload80 = transport.writtenPayloads.last()
      // Verify 48-char separator for 80mm and partial cut command GS V 66 0
      assertTrue(payload80.toString(Charsets.US_ASCII).contains("-".repeat(48)))
      assertTrue(payload80.indices.any { i -> i + 3 < payload80.size && payload80[i] == 0x1D.toByte() && payload80[i + 1] == 0x56.toByte() && payload80[i + 2] == 0x42.toByte() })

      manager.disconnect()
      assertEquals(PrinterConnectionStatus.DISCONNECTED, manager.state.value.connectionStatus)
    }

  // 14, 15, 16. Permission denied, Bluetooth disabled/unsupported, mid-print disconnect
  @Test
  fun `14 15 16 bluetooth errors and mid print disconnect fail safely without false success`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val prefs = ThermalPrinterPrefs(context)
      val dev = BluetoothPrinterDevice("PosPrinter", "11:22:33:44:55:66", isBonded = true, isLikelyPrinter = true)

      // 14. Permission denied
      val noPermBridge = FakeBluetoothBridge(supported = true, enabled = true, permissions = false)
      val managerNoPerm =
        BluetoothPrinterManager(prefs, noPermBridge, FakePrinterTransport(), Dispatchers.Unconfined)
      managerNoPerm.selectPrinter(dev)
      val permFail = managerNoPerm.connectSelectedPrinter()
      assertTrue(permFail.isFailure)
      assertEquals(PrinterConnectionStatus.ERROR, managerNoPerm.state.value.connectionStatus)
      assertTrue(managerNoPerm.state.value.errorMessage!!.contains("permission", ignoreCase = true))

      // 15. Bluetooth disabled & unsupported
      val disabledBridge = FakeBluetoothBridge(supported = true, enabled = false, permissions = true)
      val managerDisabled =
        BluetoothPrinterManager(prefs, disabledBridge, FakePrinterTransport(), Dispatchers.Unconfined)
      managerDisabled.selectPrinter(dev)
      val disabledFail = managerDisabled.printInvoice(BusinessProfile(businessName = "X"), buildRichInvoice(), AppLanguage.ENGLISH)
      assertTrue(disabledFail.isFailure)
      assertEquals(PrinterConnectionStatus.ERROR, managerDisabled.state.value.connectionStatus)
      assertTrue(managerDisabled.state.value.errorMessage!!.contains("turned off", ignoreCase = true))

      val unsupportedBridge = FakeBluetoothBridge(supported = false, enabled = false, permissions = true)
      val managerUnsupported =
        BluetoothPrinterManager(prefs, unsupportedBridge, FakePrinterTransport(), Dispatchers.Unconfined)
      managerUnsupported.selectPrinter(dev)
      val unsupportedFail = managerUnsupported.connectSelectedPrinter()
      assertTrue(unsupportedFail.isFailure)
      assertTrue(managerUnsupported.state.value.errorMessage!!.contains("not supported", ignoreCase = true))

      // 16. Printer disconnects during printing
      val okBridge = FakeBluetoothBridge(supported = true, enabled = true, permissions = true, bonded = listOf(dev))
      val flakyTransport = FakePrinterTransport(failDuringWrite = true)
      val managerFlaky =
        BluetoothPrinterManager(prefs, okBridge, flakyTransport, Dispatchers.Unconfined)
      managerFlaky.selectPrinter(dev)
      assertTrue(managerFlaky.connectSelectedPrinter().isSuccess)

      val printFail =
        managerFlaky.printInvoice(BusinessProfile(businessName = "Test"), buildRichInvoice(), AppLanguage.ENGLISH)
      assertTrue(printFail.isFailure)
      assertEquals(PrinterConnectionStatus.ERROR, managerFlaky.state.value.connectionStatus)
      assertFalse(flakyTransport.isConnected)
      assertTrue(managerFlaky.state.value.errorMessage!!.contains("disconnected", ignoreCase = true))
    }

  // 17. UI preserves all existing invoice action buttons and has NO Save & Close button
  @Test
  fun `17 invoice preview keeps existing buttons shows thermal print and omits save and close`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pdf =
      InvoicePdfGenerator.generateInvoicePdf(
        context = context,
        outputDir = File(context.filesDir, "ui_preview_test"),
        business = BusinessProfile(businessName = "Preview Store", upiId = "store@upi"),
        invoice = buildRichInvoice(),
        paperFormat = PaperFormat.A4,
        language = AppLanguage.ENGLISH,
      )

    composeRule.setContent {
      VyaparBillingTheme(darkTheme = false) {
        InvoicePreviewScreen(
          invoice = buildRichInvoice(),
          pdfFile = pdf,
          initialFormat = PaperFormat.A4,
          initialLanguage = AppLanguage.ENGLISH,
          onRegeneratePdf = { _, _ -> },
          onShowToast = {},
          onBack = {},
          printerState =
            ThermalPrinterUiState(
              isBluetoothSupported = true,
              isBluetoothEnabled = true,
              hasBluetoothPermissions = true,
            ),
        )
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("whatsapp_share_pdf_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("download_pdf_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("print_pdf_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("open_pdf_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("thermal_printer_select_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("thermal_print_bar_btn").assertIsDisplayed()

    composeRule.onNodeWithText("58mm Receipt").assertIsDisplayed()
    composeRule.onNodeWithText("80mm Receipt").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithText("Save & Close", substring = true).fetchSemanticsNodes().isEmpty())
  }
}
