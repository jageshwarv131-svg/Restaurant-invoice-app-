package com.example.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Expense
import com.example.model.Invoice
import com.example.model.Product
import com.example.util.CurrencyUtils
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

  fun exportReportPdf(
    outputDir: File,
    title: String,
    business: BusinessProfile,
    summaryLines: List<Pair<String, String>>,
    tableHeaders: List<String>,
    tableRows: List<List<String>>,
  ): File {
    outputDir.mkdirs()
    val safeTitle = title.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(outputDir, "VYAPAR_REPORT_${safeTitle}.pdf")

    val pdf = PdfDocument()
    var nativeOk = true
    try {
      val pageW = 595
      val pageH = 842
      val margin = 36f
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(20, 21, 24)
          textSize = 18f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val orangePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(249, 115, 22)
          textSize = 12f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val boldPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(20, 21, 24)
          textSize = 10f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(51, 65, 85)
          textSize = 9.5f
        }
      val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(226, 232, 240)
          strokeWidth = 1f
        }

      var pageNum = 1
      var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
      var canvas = page.canvas
      var y = margin

      canvas.drawText(business.businessName.ifBlank { "Vyapar Billing Pro" }, margin, y + 14f, titlePaint)
      y += 24f
      canvas.drawText(title, margin, y + 10f, orangePaint)
      y += 18f
      canvas.drawText(
        "Generated: ${CurrencyUtils.formatDateTime(System.currentTimeMillis())}",
        margin,
        y + 8f,
        bodyPaint,
      )
      y += 18f
      canvas.drawLine(margin, y, pageW - margin, y, linePaint)
      y += 16f

      for ((k, v) in summaryLines) {
        canvas.drawText(k, margin, y, boldPaint)
        canvas.drawText(v, margin + 220f, y, bodyPaint)
        y += 16f
      }
      y += 8f
      canvas.drawLine(margin, y, pageW - margin, y, linePaint)
      y += 18f

      val colCount = tableHeaders.size.coerceAtLeast(1)
      val colWidth = (pageW - margin * 2) / colCount

      fun drawTableHeader(c: android.graphics.Canvas, startY: Float): Float {
        tableHeaders.forEachIndexed { idx, h ->
          c.drawText(h.take(18), margin + idx * colWidth, startY, boldPaint)
        }
        c.drawLine(margin, startY + 6f, pageW - margin, startY + 6f, linePaint)
        return startY + 20f
      }

      y = drawTableHeader(canvas, y)
      for (row in tableRows) {
        if (y > pageH - 50f) {
          pdf.finishPage(page)
          pageNum++
          page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
          canvas = page.canvas
          y = drawTableHeader(canvas, margin + 12f)
        }
        row.forEachIndexed { idx, cell ->
          canvas.drawText(cell.take(20), margin + idx * colWidth, y, bodyPaint)
        }
        y += 16f
      }

      pdf.finishPage(page)
      FileOutputStream(file).use { out -> pdf.writeTo(out) }
    } catch (_: IllegalStateException) {
      nativeOk = false
    } catch (_: UnsatisfiedLinkError) {
      nativeOk = false
    } finally {
      runCatching { pdf.close() }
    }
    if (!nativeOk) {
      val lines =
        buildList {
          add(business.businessName.ifBlank { "Vyapar Billing Pro" })
          add(title)
          summaryLines.forEach { (k, v) -> add("$k: $v") }
          add(tableHeaders.joinToString(" | "))
          tableRows.forEach { add(it.joinToString(" | ")) }
        }
      JvmPdfFallbackWriter.writeTextPdf(file, lines)
    }
    return file
  }

  fun buildInvoicesCsv(invoices: List<Invoice>, sym: String): String {
    val sb = StringBuilder()
    sb.appendLine("Invoice No,Date,Customer,Subtotal,Discount,CGST,SGST,IGST,Grand Total,Paid,Balance Due,Payment Method")
    for (inv in invoices) {
      sb.appendLine(
        listOf(
          inv.invoiceNumber,
          CurrencyUtils.formatDate(inv.timestamp),
          csvEscape(inv.customerName),
          CurrencyUtils.formatPaise(inv.subtotalPaise, "", true),
          CurrencyUtils.formatPaise(inv.discountPaise, "", true),
          CurrencyUtils.formatPaise(inv.cgstPaise, "", true),
          CurrencyUtils.formatPaise(inv.sgstPaise, "", true),
          CurrencyUtils.formatPaise(inv.igstPaise, "", true),
          CurrencyUtils.formatPaise(inv.grandTotalPaise, "", true),
          CurrencyUtils.formatPaise(inv.amountPaidPaise, "", true),
          CurrencyUtils.formatPaise(inv.balanceDuePaise, "", true),
          inv.paymentMethod.label,
        ).joinToString(",")
      )
    }
    return sb.toString()
  }

  fun buildExpensesCsv(expenses: List<Expense>): String {
    val sb = StringBuilder()
    sb.appendLine("Date,Title,Category,Amount,Payment Method,Notes")
    for (e in expenses) {
      sb.appendLine(
        listOf(
          CurrencyUtils.formatDate(e.timestamp),
          csvEscape(e.title),
          e.category.label,
          CurrencyUtils.formatPaise(e.amountPaise, "", true),
          e.paymentMethod.label,
          csvEscape(e.notes),
        ).joinToString(",")
      )
    }
    return sb.toString()
  }

  fun buildProductsCsv(products: List<Product>): String {
    val sb = StringBuilder()
    sb.appendLine("Name,SKU,Category,Purchase Price,Selling Price,MRP,Stock,Unit,GST%")
    for (p in products) {
      sb.appendLine(
        listOf(
          csvEscape(p.name),
          csvEscape(p.sku),
          csvEscape(p.category),
          CurrencyUtils.formatPaise(p.purchasePricePaise, "", true),
          CurrencyUtils.formatPaise(p.sellingPricePaise, "", true),
          CurrencyUtils.formatPaise(p.mrpPaise, "", true),
          CurrencyUtils.formatQty(p.stockQty),
          p.unit,
          CurrencyUtils.formatQty(p.gstPercent),
        ).joinToString(",")
      )
    }
    return sb.toString()
  }

  fun buildCustomersCsv(customers: List<Customer>): String {
    val sb = StringBuilder()
    sb.appendLine("Name,Mobile,WhatsApp,GSTIN,Total Purchases,Outstanding Balance")
    for (c in customers) {
      sb.appendLine(
        listOf(
          csvEscape(c.name),
          c.mobile,
          c.whatsapp,
          c.gstin,
          CurrencyUtils.formatPaise(c.totalPurchasesPaise, "", true),
          CurrencyUtils.formatPaise(c.outstandingBalancePaise, "", true),
        ).joinToString(",")
      )
    }
    return sb.toString()
  }

  private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
  }
}
