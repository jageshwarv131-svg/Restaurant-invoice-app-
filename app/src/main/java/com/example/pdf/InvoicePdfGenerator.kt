package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.model.PaperFormat
import com.example.util.AppStrings
import com.example.util.CurrencyUtils
import com.example.util.PdfLabels
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToLong

object InvoicePdfGenerator {

  fun generateInvoicePdf(
    context: Context,
    outputDir: File,
    business: BusinessProfile,
    invoice: Invoice,
    paperFormat: PaperFormat,
    language: AppLanguage,
  ): File {
    outputDir.mkdirs()
    val sanitizedNumber = invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val suffix =
      when (paperFormat) {
        PaperFormat.A4 -> ""
        PaperFormat.THERMAL_80MM -> "_80mm"
        PaperFormat.THERMAL_58MM -> "_58mm"
      }
    val file = File(outputDir, "VYAPAR_INVOICE_${sanitizedNumber}${suffix}.pdf")

    val pdf = PdfDocument()
    var nativeOk = true
    val logoBitmap = loadLogoBitmap(business.logoPath)
    val upiQrBitmap = UpiQrGenerator.generateQrBitmap(business, invoice, sizePx = 220)
    try {
      try {
        when (paperFormat) {
          PaperFormat.A4 -> renderA4Invoice(pdf, business, invoice, language, logoBitmap, upiQrBitmap)
          PaperFormat.THERMAL_80MM ->
            renderThermalReceipt(pdf, business, invoice, language, logoBitmap, upiQrBitmap, pageW = 226)
          PaperFormat.THERMAL_58MM ->
            renderThermalReceipt(pdf, business, invoice, language, logoBitmap, upiQrBitmap, pageW = 164)
        }
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
      } catch (_: IllegalStateException) {
        nativeOk = false
      } catch (_: UnsatisfiedLinkError) {
        nativeOk = false
      }
    } finally {
      logoBitmap?.recycle()
      upiQrBitmap?.recycle()
      runCatching { pdf.close() }
    }

    if (!nativeOk) {
      val fallbackLines = buildFallbackTextLines(business, invoice, language, paperFormat)
      val (w, h) =
        when (paperFormat) {
          PaperFormat.A4 -> 595 to 842
          PaperFormat.THERMAL_80MM -> 226 to 842
          PaperFormat.THERMAL_58MM -> 164 to 842
        }
      JvmPdfFallbackWriter.writeTextPdf(file, fallbackLines, pageWidth = w, pageHeight = h)
    }
    return file
  }

  internal fun buildFallbackTextLines(
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
    paperFormat: PaperFormat,
  ): List<String> {
    val sym = "Rs."
    val labels = AppStrings.pdfLabels(language)
    val hasGst = invoice.taxType != GstTaxType.NO_GST && invoice.totalTaxPaise > 0L
    val billTypeLabel = if (hasGst) labels.taxInvoice else labels.retailInvoice
    val upiUri = UpiQrGenerator.buildUpiPaymentUri(business, invoice)

    return buildList {
      add(business.businessName.ifBlank { "Vyapar Billing Pro" })
      if (business.tagline.isNotBlank()) add(business.tagline)
      val addr = buildFullBusinessAddress(business)
      if (addr.isNotBlank()) add(addr)
      if (business.mobile.isNotBlank()) add("Ph: ${business.mobile}")
      if (business.email.isNotBlank()) add("Email: ${business.email}")
      if (business.gstin.isNotBlank()) add("GSTIN: ${business.gstin}")
      add("$billTypeLabel (${paperFormat.label}): ${invoice.invoiceNumber}")
      add("Date: ${CurrencyUtils.formatDate(invoice.timestamp)} ${CurrencyUtils.formatTime(invoice.timestamp)}")
      add("Customer: ${invoice.customerName}")
      if (invoice.customerMobile.isNotBlank()) add("Customer Ph: ${invoice.customerMobile}")
      if (invoice.customerAddress.isNotBlank()) add("Customer Addr: ${invoice.customerAddress}")
      if (invoice.customerGstin.isNotBlank()) add("Customer GSTIN: ${invoice.customerGstin}")
      invoice.items.forEachIndexed { idx, item ->
        add(
          "${idx + 1}. ${item.name} | ${CurrencyUtils.formatQty(item.quantity)} ${item.unit} x ${CurrencyUtils.formatPaise(item.unitPricePaise, sym)} = ${CurrencyUtils.formatPaise(item.lineTotalPaise, sym)}"
        )
      }
      add("Subtotal: ${CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true)}")
      if (invoice.discountPaise > 0) {
        add("Discount: -${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}")
      }
      if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST && invoice.totalTaxPaise > 0) {
        add("CGST: ${CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true)}")
        add("SGST: ${CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true)}")
      } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST && invoice.totalTaxPaise > 0) {
        add("IGST: ${CurrencyUtils.formatPaise(invoice.igstPaise, sym, true)}")
      }
      if (invoice.roundOffPaise != 0L) {
        add("Round Off: ${CurrencyUtils.formatPaise(invoice.roundOffPaise, sym, true)}")
      }
      add("Grand Total: ${CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true)}")
      add("Paid: ${CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true)} | Due: ${CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true)}")
      add("Payment Mode: ${invoice.paymentMethod.label}")
      if (upiUri != null) {
        add("Scan to Pay UPI: ${business.upiId.trim()}")
      }
      if (business.thankYouMessage.isNotBlank()) add(business.thankYouMessage)
    }
  }

  internal fun computeTopCenteredLogoRect(
    pageWidth: Float,
    topY: Float,
    bitmapWidth: Int,
    bitmapHeight: Int,
    maxWidth: Float,
    maxHeight: Float,
  ): RectF? {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || maxWidth <= 0f || maxHeight <= 0f) return null
    val scale = minOf(maxWidth / bitmapWidth.toFloat(), maxHeight / bitmapHeight.toFloat())
    val drawW = bitmapWidth * scale
    val drawH = bitmapHeight * scale
    val left = (pageWidth - drawW) / 2f
    return RectF(left, topY, left + drawW, topY + drawH)
  }

  internal fun loadLogoBitmap(logoPath: String?): Bitmap? {
    if (logoPath.isNullOrBlank()) return null
    val file = File(logoPath)
    if (!file.exists() || !file.isFile) return null
    return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
  }

  internal fun wrapTextLines(text: String, paint: Paint, maxWidth: Float): List<String> {
    val clean = text.trim()
    if (clean.isEmpty()) return emptyList()
    if (maxWidth <= 4f) return listOf(clean)

    val result = mutableListOf<String>()
    val paragraphs = clean.split('\n')
    for (paragraph in paragraphs) {
      val trimmedPara = paragraph.trim()
      if (trimmedPara.isEmpty()) {
        if (result.isNotEmpty()) result.add("")
        continue
      }
      val words = trimmedPara.split(Regex("\\s+"))
      var currentLine = StringBuilder()

      for (word in words) {
        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (measureSafe(paint, candidate) <= maxWidth) {
          currentLine = StringBuilder(candidate)
        } else {
          if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
            currentLine = StringBuilder()
          }
          if (measureSafe(paint, word) <= maxWidth) {
            currentLine.append(word)
          } else {
            // Hard break exceptionally long token cleanly across lines without truncation
            var start = 0
            while (start < word.length) {
              val count = breakTextSafe(paint, word, start, maxWidth)
              val end = (start + count).coerceAtMost(word.length)
              val chunk = word.substring(start, end)
              if (end < word.length) {
                result.add(chunk)
              } else {
                currentLine.append(chunk)
              }
              start = end
            }
          }
        }
      }
      if (currentLine.isNotEmpty()) {
        result.add(currentLine.toString())
      }
    }
    return result
  }

  private fun measureSafe(paint: Paint, text: String): Float {
    val measured = paint.measureText(text)
    if (measured > 0f || text.isEmpty()) return measured
    // Fallback heuristic if Robolectric shadows return 0 for empty/custom font metrics
    return text.length * paint.textSize * 0.55f
  }

  private fun breakTextSafe(paint: Paint, text: String, start: Int, maxWidth: Float): Int {
    val sub = text.substring(start)
    val measured = paint.measureText(sub)
    if (measured <= 0f) {
      val approxChars = (maxWidth / (paint.textSize * 0.55f)).toInt().coerceAtLeast(1)
      return approxChars.coerceAtMost(sub.length)
    }
    val count = paint.breakText(sub, true, maxWidth, null)
    return count.coerceAtLeast(1)
  }

  private fun buildFullBusinessAddress(business: BusinessProfile): String =
    listOf(business.address, business.city, business.state, business.pinCode)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .joinToString(", ")

  private fun drawWrappedLines(
    canvas: Canvas,
    lines: List<String>,
    x: Float,
    startY: Float,
    lineHeight: Float,
    paint: Paint,
  ): Float {
    var y = startY
    for (line in lines) {
      if (line.isNotEmpty()) {
        canvas.drawText(line, x, y, paint)
      }
      y += lineHeight
    }
    return y
  }

  private fun renderA4Invoice(
    pdf: PdfDocument,
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
    logoBitmap: Bitmap?,
    upiQrBitmap: Bitmap?,
  ) {
    val pageW = 595
    val pageH = 842
    val margin = 32f
    val contentW = pageW - margin * 2f
    val labels = AppStrings.pdfLabels(language)
    val sym = business.currencySymbol.ifBlank { "₹" }

    val primaryAccent = Color.rgb(249, 115, 22) // Saffron Orange
    val accentSoftBg = Color.rgb(255, 247, 237)
    val navyDark = Color.rgb(30, 41, 59)
    val darkText = Color.rgb(15, 23, 42)
    val mutedText = Color.rgb(71, 85, 105)
    val borderColor = Color.rgb(226, 232, 240)
    val altRowBg = Color.rgb(248, 250, 252)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkText
        textSize = 17.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val centerTitlePaint = Paint(titlePaint).apply { textAlign = Paint.Align.CENTER }
    val boldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkText
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val bodyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkText
        textSize = 9.2f
      }
    val mutedPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedText
        textSize = 8.6f
      }
    val centerMutedPaint = Paint(mutedPaint).apply { textAlign = Paint.Align.CENTER }
    val accentBoldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryAccent
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val rightAccentBoldPaint = Paint(accentBoldPaint).apply { textAlign = Paint.Align.RIGHT }
    val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
    val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
    val linePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        strokeWidth = 0.9f
      }
    val borderStrokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Determine optional table columns cleanly
    val showDiscountCol = invoice.discountPaise > 0L
    val showGstCol =
      invoice.taxType != GstTaxType.NO_GST &&
        (invoice.totalTaxPaise > 0L || invoice.items.any { it.gstPercent > 0.0 })

    // Intelligent column widths summing to contentW (531f)
    val colSnW = 24f
    val colQtyW = 54f
    val colRateW = 66f
    val colDiscW = if (showDiscountCol) 50f else 0f
    val colGstW = if (showGstCol) 44f else 0f
    val colAmtW = 74f
    val colNameW = contentW - colSnW - colQtyW - colRateW - colDiscW - colGstW - colAmtW

    val colSnLeft = margin
    val colNameLeft = colSnLeft + colSnW
    val colQtyRight = colNameLeft + colNameW + colQtyW - 6f
    val colRateRight = colNameLeft + colNameW + colQtyW + colRateW - 6f
    val colDiscRight = colRateRight + colDiscW
    val colGstRight = colDiscRight + colGstW
    val colAmtRight = pageW - margin - 6f

    val hasGstTax = invoice.taxType != GstTaxType.NO_GST && invoice.totalTaxPaise > 0L
    val invoiceHeaderLabel = if (hasGstTax) labels.taxInvoice else labels.retailInvoice
    val billTypeValue =
      when {
        invoice.taxType == GstTaxType.NO_GST -> "Retail / Exempt"
        hasGstTax -> "GST (${invoice.taxType.label.substringBefore(" (")})"
        else -> "Retail Invoice"
      }

    fun drawTableHeader(c: Canvas, topY: Float): Float {
      fillPaint.color = navyDark
      c.drawRoundRect(RectF(margin, topY, pageW - margin, topY + 24f), 5f, 5f, fillPaint)
      val thPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 8.8f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
      val baseline = topY + 15.5f
      c.drawText(labels.serialNo, colSnLeft + 7f, baseline, thPaint)
      c.drawText(labels.item, colNameLeft + 4f, baseline, thPaint)
      c.drawText(labels.qty, colQtyRight, baseline, thRight)
      c.drawText(labels.rate, colRateRight, baseline, thRight)
      if (showDiscountCol) {
        c.drawText(labels.discColumn, colDiscRight, baseline, thRight)
      }
      if (showGstCol) {
        c.drawText(labels.gst, colGstRight, baseline, thRight)
      }
      c.drawText(labels.amount, colAmtRight, baseline, thRight)
      return topY + 28f
    }

    fun drawFullFirstPageHeader(c: Canvas): Float {
      // Top colored accent bar + soft header tint
      fillPaint.color = primaryAccent
      c.drawRect(0f, 0f, pageW.toFloat(), 6f, fillPaint)

      var y = 20f
      val centerX = pageW / 2f

      // Top-centered uploaded store logo preserving exact original aspect ratio
      if (logoBitmap != null) {
        val destRect =
          computeTopCenteredLogoRect(
            pageWidth = pageW.toFloat(),
            topY = y,
            bitmapWidth = logoBitmap.width,
            bitmapHeight = logoBitmap.height,
            maxWidth = 176f,
            maxHeight = 82f,
          )
        if (destRect != null) {
          val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
          c.drawBitmap(
            logoBitmap,
            Rect(0, 0, logoBitmap.width, logoBitmap.height),
            destRect,
            imgPaint,
          )
          y = destRect.bottom + 12f
        }
      }

      val bizName = business.businessName.ifBlank { "Vyapar Billing Pro" }
      val bizNameLines = wrapTextLines(bizName, centerTitlePaint, contentW - 24f)
      y += 14f
      y = drawWrappedLines(c, bizNameLines, centerX, y, 20f, centerTitlePaint)

      if (business.tagline.isNotBlank()) {
        val taglineLines = wrapTextLines(business.tagline, centerMutedPaint, contentW - 36f)
        y = drawWrappedLines(c, taglineLines, centerX, y, 12f, centerMutedPaint)
      }

      val fullAddr = buildFullBusinessAddress(business)
      if (fullAddr.isNotBlank()) {
        val addrLines = wrapTextLines(fullAddr, centerMutedPaint, contentW - 30f)
        y = drawWrappedLines(c, addrLines, centerX, y, 12f, centerMutedPaint)
      }

      val contactParts =
        buildList {
          if (business.mobile.isNotBlank()) add("Ph: ${business.mobile.trim()}")
          if (business.email.isNotBlank()) add("Email: ${business.email.trim()}")
          if (business.gstin.isNotBlank()) add("GSTIN: ${business.gstin.trim()}")
        }
      if (contactParts.isNotEmpty()) {
        val contactLines = wrapTextLines(contactParts.joinToString("  |  "), centerMutedPaint, contentW - 20f)
        y = drawWrappedLines(c, contactLines, centerX, y, 12f, centerMutedPaint)
      }

      y += 4f
      c.drawLine(margin, y, pageW - margin, y, linePaint)
      y += 10f

      // Invoice Banner Pill
      fillPaint.color = primaryAccent
      val badgeW = 168f
      val badgeRect = RectF(centerX - badgeW / 2f, y, centerX + badgeW / 2f, y + 23f)
      c.drawRoundRect(badgeRect, 6f, 6f, fillPaint)
      val badgeTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 10.5f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textAlign = Paint.Align.CENTER
        }
      c.drawText(invoiceHeaderLabel, badgeRect.centerX(), badgeRect.centerY() + 3.5f, badgeTextPaint)
      y = badgeRect.bottom + 12f

      // Structured Customer & Invoice Metadata Box with dynamic wrapped height
      val leftColW = contentW * 0.54f - 14f
      val rightColW = contentW * 0.44f - 14f
      val customerNameLines =
        wrapTextLines("${labels.billTo}: ${invoice.customerName}", boldPaint, leftColW)
      val customerMobileLines =
        if (invoice.customerMobile.isNotBlank()) {
          wrapTextLines("Ph: ${invoice.customerMobile.trim()}", bodyPaint, leftColW)
        } else emptyList()
      val customerAddrLines =
        if (invoice.customerAddress.isNotBlank()) {
          wrapTextLines(invoice.customerAddress.trim(), mutedPaint, leftColW)
        } else emptyList()
      val customerGstinLines =
        if (invoice.customerGstin.isNotBlank()) {
          wrapTextLines("GSTIN: ${invoice.customerGstin.trim()}", mutedPaint, leftColW)
        } else emptyList()

      val invNoLines =
        wrapTextLines("${labels.invoiceNo}: ${invoice.invoiceNumber}", rightBoldPaint, rightColW)
      val dateText = "${labels.date}: ${CurrencyUtils.formatDate(invoice.timestamp)}  ${CurrencyUtils.formatTime(invoice.timestamp)}"
      val dateLines = wrapTextLines(dateText, rightBodyPaint, rightColW)
      val billTypeLines = wrapTextLines("${labels.billType}: $billTypeValue", rightBodyPaint, rightColW)
      val payLines =
        wrapTextLines("${labels.paymentMode}: ${invoice.paymentMethod.label}", rightBodyPaint, rightColW)

      val leftHeight =
        customerNameLines.size * 13f +
          customerMobileLines.size * 12.5f +
          customerAddrLines.size * 11.5f +
          customerGstinLines.size * 11.5f
      val rightHeight =
        invNoLines.size * 13f +
          dateLines.size * 12.5f +
          billTypeLines.size * 12.5f +
          payLines.size * 12.5f

      val boxHeight = maxOf(leftHeight, rightHeight, 44f) + 18f
      val boxRect = RectF(margin, y, pageW - margin, y + boxHeight)
      fillPaint.color = altRowBg
      c.drawRoundRect(boxRect, 8f, 8f, fillPaint)
      c.drawRoundRect(boxRect, 8f, 8f, borderStrokePaint)

      var ly = y + 17f
      ly = drawWrappedLines(c, customerNameLines, margin + 10f, ly, 13f, boldPaint)
      ly = drawWrappedLines(c, customerMobileLines, margin + 10f, ly, 12.5f, bodyPaint)
      ly = drawWrappedLines(c, customerAddrLines, margin + 10f, ly, 11.5f, mutedPaint)
      drawWrappedLines(c, customerGstinLines, margin + 10f, ly, 11.5f, mutedPaint)

      val rightX = pageW - margin - 10f
      var ry = y + 17f
      ry = drawWrappedLines(c, invNoLines, rightX, ry, 13f, rightBoldPaint)
      ry = drawWrappedLines(c, dateLines, rightX, ry, 12.5f, rightBodyPaint)
      ry = drawWrappedLines(c, billTypeLines, rightX, ry, 12.5f, rightBodyPaint)
      drawWrappedLines(c, payLines, rightX, ry, 12.5f, rightBodyPaint)

      y = boxRect.bottom + 12f
      return drawTableHeader(c, y)
    }

    fun drawContinuationHeader(c: Canvas, includeTableHeader: Boolean): Float {
      fillPaint.color = primaryAccent
      c.drawRect(0f, 0f, pageW.toFloat(), 5f, fillPaint)
      var y = 24f
      val bizName = business.businessName.ifBlank { "Vyapar Billing Pro" }
      val leftCont = wrapTextLines(bizName, boldPaint, contentW * 0.6f)
      val contY = drawWrappedLines(c, leftCont, margin, y, 12f, boldPaint)
      c.drawText("${labels.invoiceNo}: ${invoice.invoiceNumber}", pageW - margin, y, rightBodyPaint)
      y = maxOf(contY, y + 14f)
      c.drawLine(margin, y, pageW - margin, y, linePaint)
      y += 10f
      return if (includeTableHeader) drawTableHeader(c, y) else y
    }

    fun drawPageFooter(c: Canvas, pageNum: Int) {
      val footerPaint = Paint(mutedPaint).apply {
        textSize = 8f
        textAlign = Paint.Align.CENTER
      }
      c.drawText(
        "Vyapar Billing Pro  |  ${invoice.invoiceNumber}  |  Page $pageNum",
        pageW / 2f,
        pageH - 16f,
        footerPaint,
      )
    }

    var pageNum = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var canvas = page.canvas
    var y = drawFullFirstPageHeader(canvas)

    val pageContentBottomLimit = pageH - 36f
    val discountRatio =
      if (invoice.subtotalPaise > 0L && invoice.discountPaise > 0L) {
        invoice.discountPaise.toDouble() / invoice.subtotalPaise.toDouble()
      } else 0.0

    invoice.items.forEachIndexed { idx, item ->
      val nameLines = wrapTextLines(item.name, bodyPaint, colNameW - 8f).ifEmpty { listOf("-") }
      val qtyLines =
        wrapTextLines("${CurrencyUtils.formatQty(item.quantity)} ${item.unit}", rightBodyPaint, colQtyW - 4f)
      val lineCount = maxOf(nameLines.size, qtyLines.size, 1)
      val rowHeight = maxOf(22f, lineCount * 12.5f + 9f)

      if (y + rowHeight > pageContentBottomLimit) {
        drawPageFooter(canvas, pageNum)
        pdf.finishPage(page)
        pageNum++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        canvas = page.canvas
        y = drawContinuationHeader(canvas, includeTableHeader = true)
      }

      if (idx % 2 == 1) {
        fillPaint.color = altRowBg
        canvas.drawRect(margin, y - 4f, pageW - margin, y + rowHeight - 4f, fillPaint)
      }

      val textTopY = y + 9f
      canvas.drawText("${idx + 1}", colSnLeft + 7f, textTopY, bodyPaint)
      drawWrappedLines(canvas, nameLines, colNameLeft + 4f, textTopY, 12.5f, bodyPaint)
      drawWrappedLines(canvas, qtyLines, colQtyRight, textTopY, 12.5f, rightBodyPaint)
      canvas.drawText(
        CurrencyUtils.formatPaise(item.unitPricePaise, sym, showDecimalsIfZero = false),
        colRateRight,
        textTopY,
        rightBodyPaint,
      )
      if (showDiscountCol) {
        val itemDiscPaise = (item.lineTotalPaise * discountRatio).roundToLong()
        val discStr =
          if (itemDiscPaise > 0L) "-${CurrencyUtils.formatPaise(itemDiscPaise, sym, false)}"
          else "-"
        canvas.drawText(discStr, colDiscRight, textTopY, rightBodyPaint)
      }
      if (showGstCol) {
        val gstStr = if (item.gstPercent > 0.0) "${CurrencyUtils.formatQty(item.gstPercent)}%" else "-"
        canvas.drawText(gstStr, colGstRight, textTopY, rightBodyPaint)
      }
      canvas.drawText(
        CurrencyUtils.formatPaise(item.lineTotalPaise, sym, showDecimalsIfZero = false),
        colAmtRight,
        textTopY,
        rightBoldPaint,
      )

      y += rowHeight
      canvas.drawLine(margin, y - 4f, pageW - margin, y - 4f, linePaint)
    }

    // Prepare Summary + UPI QR + Footer and measure combined height so Totals/Summary NEVER split across pages
    val summaryRows =
      buildList {
        add(Triple(labels.subtotal, CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true), false))
        if (invoice.discountPaise > 0L) {
          add(
            Triple(
              labels.discount,
              "-${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}",
              false,
            )
          )
        }
        if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST && invoice.totalTaxPaise > 0L) {
          add(Triple(labels.cgst, CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true), false))
          add(Triple(labels.sgst, CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true), false))
        } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST && invoice.totalTaxPaise > 0L) {
          add(Triple(labels.igst, CurrencyUtils.formatPaise(invoice.igstPaise, sym, true), false))
        }
        if (invoice.roundOffPaise != 0L) {
          add(Triple(labels.roundOff, CurrencyUtils.formatPaise(invoice.roundOffPaise, sym, true), false))
        }
      }
    val postTotalRows =
      listOf(
        Triple(labels.amountPaid, CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true), false),
        Triple(labels.balanceDue, CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true), true),
        Triple(labels.paymentMode, invoice.paymentMethod.label, false),
      )

    val summaryHeight = summaryRows.size * 15.5f + 34f + postTotalRows.size * 15.5f + 12f
    val qrBlockHeight = if (upiQrBitmap != null) 128f else 0f
    val topSummarySectionHeight = maxOf(summaryHeight, qrBlockHeight)

    val termsMaxW = contentW * 0.56f
    val rightFooterW = contentW * 0.38f
    val termsLines =
      if (business.termsAndConditions.isNotBlank()) {
        wrapTextLines(business.termsAndConditions, mutedPaint, termsMaxW)
      } else emptyList()
    val thankYouLines =
      wrapTextLines(
        business.thankYouMessage.ifBlank { "Thank you for your business!" },
        rightBoldPaint,
        rightFooterW,
      )
    val leftFooterH = 18f + termsLines.size * 11.5f
    val rightFooterH = thankYouLines.size * 13f + 42f
    val footerSectionHeight = maxOf(leftFooterH, rightFooterH) + 20f

    val combinedBottomHeight = topSummarySectionHeight + 16f + footerSectionHeight

    if (y + combinedBottomHeight > pageContentBottomLimit) {
      drawPageFooter(canvas, pageNum)
      pdf.finishPage(page)
      pageNum++
      page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
      canvas = page.canvas
      y = drawContinuationHeader(canvas, includeTableHeader = false)
    }

    y += 10f
    val summaryStartY = y

    // Optional Scannable UPI QR Code on the Left alongside Summary
    var qrBottomY = summaryStartY
    if (upiQrBitmap != null) {
      val qrCardW = 122f
      val qrSize = 84f
      val qrCardH = 122f
      val qrCardRect = RectF(margin, summaryStartY, margin + qrCardW, summaryStartY + qrCardH)
      fillPaint.color = Color.WHITE
      cDrawCard(canvas, qrCardRect, fillPaint, borderStrokePaint)
      val qrLeft = qrCardRect.centerX() - qrSize / 2f
      val qrTop = summaryStartY + 8f
      val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(
        upiQrBitmap,
        Rect(0, 0, upiQrBitmap.width, upiQrBitmap.height),
        RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
        imgPaint,
      )
      val smallCenterBold = Paint(boldPaint).apply {
        textSize = 8.4f
        textAlign = Paint.Align.CENTER
      }
      val smallCenterMuted = Paint(mutedPaint).apply {
        textSize = 7.4f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(labels.scanToPay, qrCardRect.centerX(), qrTop + qrSize + 12f, smallCenterBold)
      val upiLines = wrapTextLines(business.upiId.trim(), smallCenterMuted, qrCardW - 10f)
      drawWrappedLines(canvas, upiLines.take(2), qrCardRect.centerX(), qrTop + qrSize + 22f, 9f, smallCenterMuted)
      qrBottomY = qrCardRect.bottom
    }

    // Summary Box on Right
    val summaryLeft = pageW - margin - 235f
    val summaryRight = pageW - margin - 8f
    var sy = summaryStartY + 12f

    fun drawSumRow(label: String, value: String, isBold: Boolean) {
      canvas.drawText(label, summaryLeft, sy, if (isBold) boldPaint else bodyPaint)
      canvas.drawText(value, summaryRight, sy, if (isBold) rightBoldPaint else rightBodyPaint)
      sy += 15.5f
    }

    summaryRows.forEach { (k, v, b) -> drawSumRow(k, v, b) }

    // Highlighted Grand Total Banner
    fillPaint.color = accentSoftBg
    val gtRect = RectF(summaryLeft - 8f, sy - 4f, pageW - margin, sy + 23f)
    canvas.drawRoundRect(gtRect, 6f, 6f, fillPaint)
    cDrawStrokeOnly(canvas, gtRect, primaryAccent)
    canvas.drawText(labels.grandTotal, summaryLeft, sy + 13.5f, accentBoldPaint)
    canvas.drawText(
      CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true),
      summaryRight,
      sy + 13.5f,
      rightAccentBoldPaint,
    )
    sy += 34f

    postTotalRows.forEach { (k, v, b) -> drawSumRow(k, v, b) }

    val footerTopY = maxOf(sy, qrBottomY) + 14f
    canvas.drawLine(margin, footerTopY, pageW - margin, footerTopY, linePaint)

    // Footer Left: Terms & Conditions (full wrapped lines)
    var fyLeft = footerTopY + 15f
    if (termsLines.isNotEmpty()) {
      canvas.drawText(labels.terms, margin, fyLeft, boldPaint)
      fyLeft += 13f
      drawWrappedLines(canvas, termsLines, margin, fyLeft, 11.5f, mutedPaint)
    }

    // Footer Right: Thank You & Signatory (never overlapping)
    var fyRight = footerTopY + 16f
    fyRight = drawWrappedLines(canvas, thankYouLines, pageW - margin, fyRight, 13f, rightBoldPaint)
    canvas.drawText(
      labels.authorizedSignatory,
      pageW - margin,
      maxOf(fyRight + 18f, footerTopY + 56f),
      rightBodyPaint,
    )

    drawPageFooter(canvas, pageNum)
    pdf.finishPage(page)
  }

  private fun cDrawCard(c: Canvas, rect: RectF, fillPaint: Paint, strokePaint: Paint) {
    c.drawRoundRect(rect, 7f, 7f, fillPaint)
    c.drawRoundRect(rect, 7f, 7f, strokePaint)
  }

  private fun cDrawStrokeOnly(c: Canvas, rect: RectF, color: Int) {
    val p =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
      }
    c.drawRoundRect(rect, 6f, 6f, p)
  }

  private fun renderThermalReceipt(
    pdf: PdfDocument,
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
    logoBitmap: Bitmap?,
    upiQrBitmap: Bitmap?,
    pageW: Int,
  ) {
    val margin = if (pageW <= 170) 7f else 10f
    val contentW = pageW - margin * 2f
    val sym = business.currencySymbol.ifBlank { "₹" }
    val labels = AppStrings.pdfLabels(language)
    val is58 = pageW <= 170

    val centerBold =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = if (is58) 9.5f else 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val centerSmall =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = if (is58) 7f else 8f
        textAlign = Paint.Align.CENTER
      }
    val leftSmall =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = if (is58) 7.2f else 8.2f
      }
    val rightSmall = Paint(leftSmall).apply { textAlign = Paint.Align.RIGHT }
    val leftBold =
      Paint(leftSmall).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val rightBold = Paint(leftBold).apply { textAlign = Paint.Align.RIGHT }
    val dashPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 0.8f
      }

    // Pre-wrap every single text block to calculate exact dynamic page height (ZERO clipping)
    val bizName = business.businessName.ifBlank { "Vyapar Billing Pro" }
    val bizNameLines = wrapTextLines(bizName, centerBold, contentW)
    val addrStr = buildFullBusinessAddress(business)
    val addrLines = if (addrStr.isNotBlank()) wrapTextLines(addrStr, centerSmall, contentW) else emptyList()
    val phoneLines =
      if (business.mobile.isNotBlank()) wrapTextLines("Ph: ${business.mobile.trim()}", centerSmall, contentW)
      else emptyList()
    val emailLines =
      if (business.email.isNotBlank()) wrapTextLines(business.email.trim(), centerSmall, contentW)
      else emptyList()
    val gstinLines =
      if (business.gstin.isNotBlank()) wrapTextLines("GSTIN: ${business.gstin.trim()}", centerSmall, contentW)
      else emptyList()

    val hasGst = invoice.taxType != GstTaxType.NO_GST && invoice.totalTaxPaise > 0L
    val headerLabel = if (hasGst) labels.taxInvoice else labels.retailInvoice

    val invNoLines = wrapTextLines("${labels.invoiceNo}: ${invoice.invoiceNumber}", leftBold, contentW)
    val custNameLines = wrapTextLines("${labels.billTo}: ${invoice.customerName}", leftSmall, contentW)
    val custPhoneLines =
      if (invoice.customerMobile.isNotBlank())
        wrapTextLines("Ph: ${invoice.customerMobile.trim()}", leftSmall, contentW)
      else emptyList()
    val custAddrLines =
      if (invoice.customerAddress.isNotBlank())
        wrapTextLines(invoice.customerAddress.trim(), leftSmall, contentW)
      else emptyList()
    val custGstinLines =
      if (invoice.customerGstin.isNotBlank())
        wrapTextLines("GSTIN: ${invoice.customerGstin.trim()}", leftSmall, contentW)
      else emptyList()

    val itemBlocks =
      invoice.items.mapIndexed { idx, item ->
        val nameWrapped = wrapTextLines("${idx + 1}. ${item.name}", leftSmall, contentW)
        val gstTag = if (item.gstPercent > 0.0) " (${CurrencyUtils.formatQty(item.gstPercent)}% GST)" else ""
        val rateLine =
          "${CurrencyUtils.formatQty(item.quantity)} ${item.unit} x ${CurrencyUtils.formatPaise(item.unitPricePaise, sym)}$gstTag"
        val amtLine = CurrencyUtils.formatPaise(item.lineTotalPaise, sym)
        Triple(nameWrapped, rateLine, amtLine)
      }

    val thankLines =
      wrapTextLines(
        business.thankYouMessage.ifBlank { "Thank you for your business!" },
        centerSmall,
        contentW,
      )

    val logoRect =
      if (logoBitmap != null) {
        computeTopCenteredLogoRect(
          pageWidth = pageW.toFloat(),
          topY = 12f,
          bitmapWidth = logoBitmap.width,
          bitmapHeight = logoBitmap.height,
          maxWidth = if (is58) 76f else 108f,
          maxHeight = if (is58) 38f else 50f,
        )
      } else null

    val lineStepSmall = if (is58) 9.5f else 10.5f
    val lineStepBold = if (is58) 11.5f else 13f

    var totalH = 14f
    if (logoRect != null) totalH += logoRect.height() + 10f
    totalH += bizNameLines.size * lineStepBold + 4f
    totalH += (addrLines.size + phoneLines.size + emailLines.size + gstinLines.size) * lineStepSmall
    totalH += 24f // divider + TAX INVOICE
    totalH += invNoLines.size * lineStepSmall + lineStepSmall // date
    totalH += (custNameLines.size + custPhoneLines.size + custAddrLines.size + custGstinLines.size) * lineStepSmall
    totalH += 30f // table header + dividers
    itemBlocks.forEach { (nameWrapped, _, _) ->
      totalH += nameWrapped.size * lineStepSmall + lineStepSmall + 4f
    }
    var summaryLineCount = 4 // subtotal, grandTotal, paid, balanceDue, paymentMode -> 5
    summaryLineCount = 5
    if (invoice.discountPaise > 0L) summaryLineCount++
    if (invoice.totalTaxPaise > 0L) {
      summaryLineCount += if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST) 2 else 1
    }
    if (invoice.roundOffPaise != 0L) summaryLineCount++
    totalH += 14f + summaryLineCount * (lineStepSmall + 1.5f)
    if (upiQrBitmap != null) {
      totalH += (if (is58) 78f else 92f) + 26f
    }
    totalH += 20f + thankLines.size * lineStepSmall + 16f

    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, totalH.toInt().coerceAtLeast(180), 1).create())
    val c = page.canvas

    var y = 12f
    if (logoBitmap != null && logoRect != null) {
      val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      c.drawBitmap(
        logoBitmap,
        Rect(0, 0, logoBitmap.width, logoBitmap.height),
        logoRect,
        imgPaint,
      )
      y = logoRect.bottom + 10f
    }

    y += 8f
    y = drawWrappedLines(c, bizNameLines, pageW / 2f, y, lineStepBold, centerBold)
    y = drawWrappedLines(c, addrLines, pageW / 2f, y, lineStepSmall, centerSmall)
    y = drawWrappedLines(c, phoneLines, pageW / 2f, y, lineStepSmall, centerSmall)
    y = drawWrappedLines(c, emailLines, pageW / 2f, y, lineStepSmall, centerSmall)
    y = drawWrappedLines(c, gstinLines, pageW / 2f, y, lineStepSmall, centerSmall)

    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f
    c.drawText(headerLabel, pageW / 2f, y, centerBold)
    y += 13f

    y = drawWrappedLines(c, invNoLines, margin, y, lineStepSmall, leftBold)
    c.drawText(
      "${CurrencyUtils.formatDate(invoice.timestamp)} ${CurrencyUtils.formatTime(invoice.timestamp)}",
      margin,
      y,
      leftSmall,
    )
    y += lineStepSmall
    y = drawWrappedLines(c, custNameLines, margin, y, lineStepSmall, leftSmall)
    y = drawWrappedLines(c, custPhoneLines, margin, y, lineStepSmall, leftSmall)
    y = drawWrappedLines(c, custAddrLines, margin, y, lineStepSmall, leftSmall)
    y = drawWrappedLines(c, custGstinLines, margin, y, lineStepSmall, leftSmall)

    y += 4f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f

    c.drawText(labels.item, margin, y, leftBold)
    c.drawText(labels.amount, pageW - margin, y, rightBold)
    y += 5f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f

    for ((nameWrapped, rateLine, amtLine) in itemBlocks) {
      y = drawWrappedLines(c, nameWrapped, margin, y, lineStepSmall, leftSmall)
      c.drawText(rateLine, margin + 4f, y, leftSmall)
      c.drawText(amtLine, pageW - margin, y, rightBold)
      y += lineStepSmall + 3.5f
    }

    c.drawLine(margin, y - 2f, pageW - margin, y - 2f, dashPaint)
    y += 10f

    fun row(k: String, v: String, bold: Boolean = false) {
      c.drawText(k, margin, y, if (bold) leftBold else leftSmall)
      c.drawText(v, pageW - margin, y, if (bold) rightBold else rightSmall)
      y += lineStepSmall + 1.5f
    }

    row(labels.subtotal, CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true))
    if (invoice.discountPaise > 0L) {
      row(labels.discount, "-${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}")
    }
    if (invoice.totalTaxPaise > 0L) {
      if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST) {
        row(labels.cgst, CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true))
        row(labels.sgst, CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true))
      } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST) {
        row(labels.igst, CurrencyUtils.formatPaise(invoice.igstPaise, sym, true))
      }
    }
    if (invoice.roundOffPaise != 0L) {
      row(labels.roundOff, CurrencyUtils.formatPaise(invoice.roundOffPaise, sym, true))
    }
    row(labels.grandTotal, CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true), true)
    row(labels.amountPaid, CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true))
    row(labels.balanceDue, CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true))
    row(labels.paymentMode, invoice.paymentMethod.label)

    if (upiQrBitmap != null) {
      y += 6f
      val qrSize = if (is58) 72f else 86f
      val qrLeft = (pageW - qrSize) / 2f
      val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      c.drawBitmap(
        upiQrBitmap,
        Rect(0, 0, upiQrBitmap.width, upiQrBitmap.height),
        RectF(qrLeft, y, qrLeft + qrSize, y + qrSize),
        imgPaint,
      )
      y += qrSize + 10f
      c.drawText(labels.scanToPay, pageW / 2f, y, centerSmall)
      y += 10f
    }

    y += 4f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 12f
    drawWrappedLines(c, thankLines, pageW / 2f, y, lineStepSmall, centerSmall)
    pdf.finishPage(page)
  }
}
