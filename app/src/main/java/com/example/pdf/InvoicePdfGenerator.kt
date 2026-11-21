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
import com.example.R
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.model.PaperFormat
import com.example.util.AppStrings
import com.example.util.CurrencyUtils
import java.io.File
import java.io.FileOutputStream

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
    val suffix = if (paperFormat == PaperFormat.THERMAL_80MM) "_80mm" else ""
    val file = File(outputDir, "VYAPAR_INVOICE_${sanitizedNumber}${suffix}.pdf")

    val pdf = PdfDocument()
    var nativeOk = true
    try {
      val logoBitmap = loadLogoBitmap(context, business.logoPath)
      try {
        when (paperFormat) {
          PaperFormat.A4 -> renderA4Invoice(pdf, business, invoice, language, logoBitmap)
          PaperFormat.THERMAL_80MM -> renderThermal80mm(pdf, business, invoice, language, logoBitmap)
        }
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
      } catch (_: IllegalStateException) {
        nativeOk = false
      } catch (_: UnsatisfiedLinkError) {
        nativeOk = false
      } finally {
        logoBitmap?.recycle()
      }
    } finally {
      runCatching { pdf.close() }
    }

    if (!nativeOk) {
      val sym = "Rs."
      val fallbackLines =
        buildList {
          add(business.businessName.ifBlank { "Vyapar Billing Pro" })
          add("TAX INVOICE: ${invoice.invoiceNumber}")
          add("Date: ${CurrencyUtils.formatDate(invoice.timestamp)} | Customer: ${invoice.customerName}")
          invoice.items.forEach {
            add("${it.name} x ${CurrencyUtils.formatQty(it.quantity)} = ${CurrencyUtils.formatPaise(it.lineTotalPaise, sym)}")
          }
          add("Grand Total: ${CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true)}")
          add("Paid: ${CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true)} | Due: ${CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true)}")
        }
      JvmPdfFallbackWriter.writeTextPdf(file, fallbackLines)
    }
    return file
  }

  private fun loadLogoBitmap(context: Context, logoPath: String?): Bitmap? {
    if (!logoPath.isNullOrBlank()) {
      val file = File(logoPath)
      if (file.exists()) {
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
        if (decoded != null) return decoded
      }
    }
    return runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.img_vyapar_logo) }
      .getOrNull()
  }

  private fun renderA4Invoice(
    pdf: PdfDocument,
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
    logoBitmap: Bitmap?,
  ) {
    val pageW = 595
    val pageH = 842
    val margin = 36f
    val contentW = pageW - margin * 2
    val labels = AppStrings.pdfLabels(language)
    val sym = business.currencySymbol.ifBlank { "₹" }

    val orangeColor = Color.rgb(249, 115, 22)
    val darkColor = Color.rgb(20, 21, 24)
    val mutedColor = Color.rgb(100, 116, 139)
    val lineColor = Color.rgb(226, 232, 240)
    val lightBg = Color.rgb(248, 250, 252)

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkColor
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val boldPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkColor
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val bodyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkColor
        textSize = 10f
      }
    val mutedPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedColor
        textSize = 9.5f
      }
    val orangePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = orangeColor
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val rightBoldPaint =
      Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
    val rightBodyPaint =
      Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
    val linePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 1f
      }
    val fillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    var pageNum = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var canvas = page.canvas

    fun drawHeader(c: Canvas): Float {
      var y = margin
      // Top orange accent bar
      fillPaint.color = orangeColor
      c.drawRect(0f, 0f, pageW.toFloat(), 6f, fillPaint)

      // Logo
      var textStartX = margin
      if (logoBitmap != null) {
        val logoSize = 54f
        val destRect = RectF(margin, y, margin + logoSize, y + logoSize)
        c.drawBitmap(logoBitmap, Rect(0, 0, logoBitmap.width, logoBitmap.height), destRect, null)
        textStartX = margin + logoSize + 12f
      }

      val bizName = business.businessName.ifBlank { "Vyapar Billing Pro" }
      c.drawText(bizName, textStartX, y + 16f, titlePaint)
      var infoY = y + 30f
      if (business.tagline.isNotBlank()) {
        c.drawText(business.tagline, textStartX, infoY, mutedPaint)
        infoY += 13f
      }
      val fullAddr =
        listOf(business.address, business.city, business.state, business.pinCode)
          .filter { it.isNotBlank() }
          .joinToString(", ")
      if (fullAddr.isNotBlank()) {
        c.drawText(fullAddr.take(58), textStartX, infoY, mutedPaint)
        infoY += 13f
      }
      val contactLine =
        buildString {
          if (business.mobile.isNotBlank()) append("Ph: ${business.mobile}  ")
          if (business.gstin.isNotBlank()) append("GSTIN: ${business.gstin}")
        }
      if (contactLine.isNotBlank()) {
        c.drawText(contactLine, textStartX, infoY, mutedPaint)
        infoY += 13f
      }

      // Invoice badge on top right
      fillPaint.color = orangeColor
      val badgeRect = RectF(pageW - margin - 140f, y, pageW - margin, y + 26f)
      c.drawRoundRect(badgeRect, 6f, 6f, fillPaint)
      val badgeTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 11f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textAlign = Paint.Align.CENTER
        }
      c.drawText(labels.taxInvoice, badgeRect.centerX(), badgeRect.centerY() + 4f, badgeTextPaint)

      y = maxOf(y + 62f, infoY + 8f)
      c.drawLine(margin, y, pageW - margin, y, linePaint)
      y += 14f

      // Customer + Invoice metadata box
      val boxHeight =
        if (invoice.customerAddress.isNotBlank() && invoice.customerGstin.isNotBlank()) 76f else 62f
      fillPaint.color = lightBg
      c.drawRoundRect(RectF(margin, y, pageW - margin, y + boxHeight), 8f, 8f, fillPaint)
      c.drawText("${labels.billTo}: ${invoice.customerName}", margin + 10f, y + 18f, boldPaint)
      var custY = y + 33f
      if (invoice.customerMobile.isNotBlank()) {
        c.drawText("Ph: ${invoice.customerMobile}", margin + 10f, custY, bodyPaint)
        custY += 14f
      }
      if (invoice.customerAddress.isNotBlank()) {
        c.drawText(invoice.customerAddress.take(44), margin + 10f, custY, mutedPaint)
        custY += 13f
      }
      if (invoice.customerGstin.isNotBlank()) {
        c.drawText("GSTIN: ${invoice.customerGstin}", margin + 10f, custY, mutedPaint)
      }

      val rightX = pageW - margin - 10f
      c.drawText("${labels.invoiceNo}: ${invoice.invoiceNumber}", rightX, y + 18f, rightBoldPaint)
      c.drawText(
        "${labels.date}: ${CurrencyUtils.formatDate(invoice.timestamp)}",
        rightX,
        y + 34f,
        rightBodyPaint,
      )
      c.drawText(
        "${labels.time}: ${CurrencyUtils.formatTime(invoice.timestamp)}",
        rightX,
        y + 49f,
        rightBodyPaint,
      )

      y += boxHeight + 14f

      // Table Header
      fillPaint.color = darkColor
      c.drawRoundRect(RectF(margin, y, pageW - margin, y + 24f), 4f, 4f, fillPaint)
      val thPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 9.5f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
      c.drawText("#", margin + 8f, y + 15.5f, thPaint)
      c.drawText(labels.item, margin + 28f, y + 15.5f, thPaint)
      c.drawText(labels.qty, margin + 275f, y + 15.5f, thRight)
      c.drawText(labels.rate, margin + 355f, y + 15.5f, thRight)
      c.drawText(labels.gst, margin + 415f, y + 15.5f, thRight)
      c.drawText(labels.amount, pageW - margin - 8f, y + 15.5f, thRight)

      return y + 30f
    }

    var y = drawHeader(canvas)
    val rowBottomLimit = pageH - 185f

    invoice.items.forEachIndexed { idx, item ->
      if (y > rowBottomLimit) {
        cFooterPageNum(canvas, pageNum, pageW, pageH, mutedPaint)
        pdf.finishPage(page)
        pageNum++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        canvas = page.canvas
        y = drawHeader(canvas)
      }

      if (idx % 2 == 1) {
        fillPaint.color = lightBg
        canvas.drawRect(margin, y - 12f, pageW - margin, y + 8f, fillPaint)
      }

      canvas.drawText("${idx + 1}", margin + 8f, y, bodyPaint)
      canvas.drawText(item.name.take(34), margin + 28f, y, bodyPaint)
      canvas.drawText(
        "${CurrencyUtils.formatQty(item.quantity)} ${item.unit}",
        margin + 275f,
        y,
        rightBodyPaint,
      )
      canvas.drawText(
        CurrencyUtils.formatPaise(item.unitPricePaise, sym, showDecimalsIfZero = false),
        margin + 355f,
        y,
        rightBodyPaint,
      )
      val gstText =
        if (item.gstPercent > 0) "${CurrencyUtils.formatQty(item.gstPercent)}%" else "-"
      canvas.drawText(gstText, margin + 415f, y, rightBodyPaint)
      canvas.drawText(
        CurrencyUtils.formatPaise(item.lineTotalPaise, sym, showDecimalsIfZero = false),
        pageW - margin - 8f,
        y,
        rightBoldPaint,
      )

      y += 20f
      canvas.drawLine(margin, y - 10f, pageW - margin, y - 10f, linePaint)
    }

    // Ensure summary fits on page
    if (y > pageH - 210f) {
      cFooterPageNum(canvas, pageNum, pageW, pageH, mutedPaint)
      pdf.finishPage(page)
      pageNum++
      page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
      canvas = page.canvas
      y = margin + 20f
    }

    y += 8f
    val summaryLeft = pageW - margin - 220f
    val summaryRight = pageW - margin - 8f

    fun drawSummaryRow(label: String, value: String, isBold: Boolean = false) {
      val lp = if (isBold) boldPaint else bodyPaint
      val rp = if (isBold) rightBoldPaint else rightBodyPaint
      canvas.drawText(label, summaryLeft, y, lp)
      canvas.drawText(value, summaryRight, y, rp)
      y += 16f
    }

    drawSummaryRow(labels.subtotal, CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true))
    if (invoice.discountPaise > 0) {
      drawSummaryRow(labels.discount, "-${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}")
    }
    if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST && invoice.totalTaxPaise > 0) {
      drawSummaryRow(labels.cgst, CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true))
      drawSummaryRow(labels.sgst, CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true))
    } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST && invoice.totalTaxPaise > 0) {
      drawSummaryRow(labels.igst, CurrencyUtils.formatPaise(invoice.igstPaise, sym, true))
    }
    if (invoice.roundOffPaise != 0L) {
      drawSummaryRow(labels.roundOff, CurrencyUtils.formatPaise(invoice.roundOffPaise, sym, true))
    }

    // Grand Total Banner
    fillPaint.color = Color.rgb(255, 247, 237)
    canvas.drawRoundRect(RectF(summaryLeft - 8f, y - 4f, pageW - margin, y + 24f), 6f, 6f, fillPaint)
    canvas.drawText(labels.grandTotal, summaryLeft, y + 14f, orangePaint)
    val orangeRight = Paint(orangePaint).apply { textAlign = Paint.Align.RIGHT }
    canvas.drawText(
      CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true),
      summaryRight,
      y + 14f,
      orangeRight,
    )
    y += 34f

    drawSummaryRow(labels.amountPaid, CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true))
    drawSummaryRow(labels.balanceDue, CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true), true)
    drawSummaryRow(labels.paymentMode, invoice.paymentMethod.label)

    // Footer: Terms + Thank you + Signatory
    val footerY = maxOf(y + 14f, pageH - 110f)
    canvas.drawLine(margin, footerY, pageW - margin, footerY, linePaint)
    canvas.drawText(labels.terms, margin, footerY + 14f, boldPaint)
    business.termsAndConditions.lines().take(3).forEachIndexed { i, line ->
      canvas.drawText(line.take(55), margin, footerY + 28f + i * 12f, mutedPaint)
    }

    canvas.drawText(
      business.thankYouMessage.ifBlank { "Thank you for your business!" },
      pageW - margin,
      footerY + 18f,
      rightBoldPaint,
    )
    canvas.drawText(labels.authorizedSignatory, pageW - margin, footerY + 62f, rightBodyPaint)
    cFooterPageNum(canvas, pageNum, pageW, pageH, mutedPaint)
    pdf.finishPage(page)
  }

  private fun cFooterPageNum(c: Canvas, pageNum: Int, pageW: Int, pageH: Int, paint: Paint) {
    val p = Paint(paint).apply { textAlign = Paint.Align.CENTER }
    c.drawText("Generated by Vyapar Billing Pro  |  Page $pageNum", pageW / 2f, pageH - 18f, p)
  }

  private fun renderThermal80mm(
    pdf: PdfDocument,
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
    logoBitmap: Bitmap?,
  ) {
    val pageW = 226 // 80mm at 72 dpi
    val margin = 10f
    val sym = business.currencySymbol.ifBlank { "₹" }
    val labels = AppStrings.pdfLabels(language)
    val extraCustLines =
      (if (invoice.customerMobile.isNotBlank()) 11 else 0) +
        (if (invoice.customerAddress.isNotBlank()) 11 else 0) +
        (if (invoice.customerGstin.isNotBlank()) 11 else 0)
    val pageH = 310 + extraCustLines + (invoice.items.size * 26)

    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
    val c = page.canvas

    val centerBold =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val centerSmall =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 8f
        textAlign = Paint.Align.CENTER
      }
    val leftSmall =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8.2f
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

    var y = 14f
    if (logoBitmap != null) {
      val sz = 34f
      val left = (pageW - sz) / 2f
      c.drawBitmap(
        logoBitmap,
        Rect(0, 0, logoBitmap.width, logoBitmap.height),
        RectF(left, y, left + sz, y + sz),
        null,
      )
      y += sz + 6f
    }

    c.drawText(business.businessName.ifBlank { "Vyapar Billing Pro" }, pageW / 2f, y + 8f, centerBold)
    y += 18f
    if (business.address.isNotBlank()) {
      c.drawText("${business.address} ${business.city}".take(34), pageW / 2f, y, centerSmall)
      y += 11f
    }
    if (business.mobile.isNotBlank()) {
      c.drawText("Ph: ${business.mobile}", pageW / 2f, y, centerSmall)
      y += 11f
    }
    if (business.gstin.isNotBlank()) {
      c.drawText("GSTIN: ${business.gstin}", pageW / 2f, y, centerSmall)
      y += 11f
    }

    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f
    c.drawText(labels.taxInvoice, pageW / 2f, y, centerBold)
    y += 13f
    c.drawText("${labels.invoiceNo}: ${invoice.invoiceNumber}", margin, y, leftBold)
    y += 11f
    c.drawText(
      "${CurrencyUtils.formatDate(invoice.timestamp)} ${CurrencyUtils.formatTime(invoice.timestamp)}",
      margin,
      y,
      leftSmall,
    )
    y += 11f
    c.drawText("${labels.billTo}: ${invoice.customerName.take(24)}", margin, y, leftSmall)
    if (invoice.customerMobile.isNotBlank()) {
      y += 10f
      c.drawText("Ph: ${invoice.customerMobile.take(22)}", margin, y, leftSmall)
    }
    if (invoice.customerAddress.isNotBlank()) {
      y += 10f
      c.drawText(invoice.customerAddress.take(30), margin, y, leftSmall)
    }
    if (invoice.customerGstin.isNotBlank()) {
      y += 10f
      c.drawText("GSTIN: ${invoice.customerGstin.take(20)}", margin, y, leftSmall)
    }
    y += 8f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f

    c.drawText(labels.item, margin, y, leftBold)
    c.drawText(labels.qty, pageW - 74f, y, rightBold)
    c.drawText(labels.amount, pageW - margin, y, rightBold)
    y += 6f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 11f

    for (item in invoice.items) {
      c.drawText(item.name.take(20), margin, y, leftSmall)
      y += 10f
      val rateStr = CurrencyUtils.formatPaise(item.unitPricePaise, sym)
      c.drawText("${CurrencyUtils.formatQty(item.quantity)} x $rateStr", margin + 6f, y, leftSmall)
      c.drawText(CurrencyUtils.formatPaise(item.lineTotalPaise, sym), pageW - margin, y, rightBold)
      y += 13f
    }

    c.drawLine(margin, y - 3f, pageW - margin, y - 3f, dashPaint)
    y += 9f

    fun row(k: String, v: String, bold: Boolean = false) {
      c.drawText(k, margin, y, if (bold) leftBold else leftSmall)
      c.drawText(v, pageW - margin, y, if (bold) rightBold else rightSmall)
      y += 12f
    }

    row(labels.subtotal, CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true))
    if (invoice.discountPaise > 0) {
      row(labels.discount, "-${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}")
    }
    if (invoice.totalTaxPaise > 0) {
      if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST) {
        row(labels.cgst, CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true))
        row(labels.sgst, CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true))
      } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST) {
        row(labels.igst, CurrencyUtils.formatPaise(invoice.igstPaise, sym, true))
      }
    }
    row(labels.grandTotal, CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true), true)
    row(labels.amountPaid, CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true))
    row(labels.balanceDue, CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true))
    row(labels.paymentMode, invoice.paymentMethod.label)

    y += 6f
    c.drawLine(margin, y, pageW - margin, y, dashPaint)
    y += 14f
    c.drawText(business.thankYouMessage.take(36), pageW / 2f, y, centerSmall)
    pdf.finishPage(page)
  }
}
