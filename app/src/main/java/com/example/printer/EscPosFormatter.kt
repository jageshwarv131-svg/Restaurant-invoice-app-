package com.example.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.pdf.UpiQrGenerator
import com.example.util.AppStrings
import com.example.util.CurrencyUtils
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object EscPosFormatter {

  private val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
  private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
  private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
  private val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
  private val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
  private val CMD_SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0
  private val CMD_SIZE_DOUBLE_H = byteArrayOf(0x1D, 0x21, 0x01) // GS ! 1 (2x height)
  private val CMD_FEED_4 = byteArrayOf(0x1B, 0x64, 0x04) // ESC d 4
  val CMD_PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0

  fun buildEscPosReceipt(
    business: BusinessProfile,
    invoice: Invoice,
    paperWidth: ThermalPaperWidth,
    language: AppLanguage = AppLanguage.ENGLISH,
    logoBitmap: Bitmap? = null,
    upiQrBitmap: Bitmap? = null,
    autoCut: Boolean = true,
  ): ByteArray {
    val cols = paperWidth.charsPerLine
    val labels = AppStrings.pdfLabels(language)
    val sym = currencySafe(business.currencySymbol.ifBlank { "Rs." })
    val out = ByteArrayOutputStream()

    fun writeBytes(bytes: ByteArray) = out.write(bytes)
    fun writeLine(text: String) {
      val clean = sanitizeForEscPos(text)
      out.write(clean.toByteArray(Charsets.US_ASCII))
      out.write('\n'.code)
    }

    fun writeWrapped(text: String, maxCols: Int = cols) {
      wrapPlainText(sanitizeForEscPos(text), maxCols).forEach { writeLine(it) }
    }

    fun writeSeparator(ch: Char = '-') {
      writeLine(ch.toString().repeat(cols))
    }

    writeBytes(CMD_INIT)

    // Store Logo (optional, scaled proportionally)
    if (logoBitmap != null) {
      val maxLogoW = if (paperWidth == ThermalPaperWidth.MM_58) 200 else 280
      val maxLogoH = if (paperWidth == ThermalPaperWidth.MM_58) 120 else 150
      val raster = encodeRasterBitmap(logoBitmap, maxLogoW, maxLogoH)
      if (raster != null) {
        writeBytes(CMD_ALIGN_CENTER)
        writeBytes(raster)
        out.write('\n'.code)
      }
    }

    // Header
    writeBytes(CMD_ALIGN_CENTER)
    writeBytes(CMD_BOLD_ON)
    writeBytes(CMD_SIZE_DOUBLE_H)
    writeWrapped(business.businessName.ifBlank { "Vyapar Billing Pro" })
    writeBytes(CMD_SIZE_NORMAL)
    writeBytes(CMD_BOLD_OFF)

    if (business.tagline.isNotBlank()) {
      writeWrapped(business.tagline)
    }
    val addressLine =
      listOf(business.address, business.city, business.state, business.pinCode)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
    if (addressLine.isNotBlank()) {
      writeWrapped(addressLine)
    }
    if (business.mobile.isNotBlank()) {
      writeWrapped("Ph: ${business.mobile.trim()}")
    }
    if (business.email.isNotBlank()) {
      writeWrapped("Email: ${business.email.trim()}")
    }
    if (business.gstin.isNotBlank()) {
      writeWrapped("GSTIN: ${business.gstin.trim()}")
    }

    writeBytes(CMD_ALIGN_LEFT)
    writeSeparator('=')

    // Bill Type & Metadata
    val hasGst = invoice.taxType != GstTaxType.NO_GST && invoice.totalTaxPaise > 0L
    val billHeader = if (hasGst) labels.taxInvoice else labels.retailInvoice
    writeBytes(CMD_ALIGN_CENTER)
    writeBytes(CMD_BOLD_ON)
    writeWrapped(billHeader)
    writeBytes(CMD_BOLD_OFF)
    writeBytes(CMD_ALIGN_LEFT)

    writeWrapped("${labels.invoiceNo}: ${invoice.invoiceNumber}")
    writeWrapped("${CurrencyUtils.formatDate(invoice.timestamp)} ${CurrencyUtils.formatTime(invoice.timestamp)}")
    writeWrapped("${labels.billTo}: ${invoice.customerName}")
    if (invoice.customerMobile.isNotBlank()) {
      writeWrapped("Ph: ${invoice.customerMobile.trim()}")
    }
    if (invoice.customerAddress.isNotBlank()) {
      writeWrapped("Addr: ${invoice.customerAddress.trim()}")
    }
    if (invoice.customerGstin.isNotBlank()) {
      writeWrapped("GSTIN: ${invoice.customerGstin.trim()}")
    }

    writeSeparator('-')

    // Item Header
    writeBytes(CMD_BOLD_ON)
    formatLeftRightLines(labels.item, labels.amount, cols).forEach { writeLine(it) }
    writeBytes(CMD_BOLD_OFF)
    writeSeparator('-')

    // Items (zero truncation: full item name wrapped cleanly across multiple lines)
    invoice.items.forEachIndexed { idx, item ->
      writeWrapped("${idx + 1}. ${item.name}", cols)
      val rateText = CurrencyUtils.formatPaise(item.unitPricePaise, sym)
      val gstTag = if (item.gstPercent > 0.0) " (${CurrencyUtils.formatQty(item.gstPercent)}%)" else ""
      val qtyDetail = "  ${CurrencyUtils.formatQty(item.quantity)} ${item.unit} x $rateText$gstTag"
      val amtText = CurrencyUtils.formatPaise(item.lineTotalPaise, sym)
      formatLeftRightLines(qtyDetail, amtText, cols).forEach { writeLine(it) }
    }

    writeSeparator('-')

    // Summary Totals
    fun writeTotalRow(label: String, value: String, bold: Boolean = false) {
      if (bold) writeBytes(CMD_BOLD_ON)
      formatLeftRightLines(label, value, cols).forEach { writeLine(it) }
      if (bold) writeBytes(CMD_BOLD_OFF)
    }

    writeTotalRow(labels.subtotal, CurrencyUtils.formatPaise(invoice.subtotalPaise, sym, true))
    if (invoice.discountPaise > 0L) {
      writeTotalRow(
        labels.discount,
        "-${CurrencyUtils.formatPaise(invoice.discountPaise, sym, true)}",
      )
    }
    if (invoice.totalTaxPaise > 0L) {
      if (invoice.taxType == GstTaxType.INTRA_STATE_CGST_SGST) {
        writeTotalRow(labels.cgst, CurrencyUtils.formatPaise(invoice.cgstPaise, sym, true))
        writeTotalRow(labels.sgst, CurrencyUtils.formatPaise(invoice.sgstPaise, sym, true))
      } else if (invoice.taxType == GstTaxType.INTER_STATE_IGST) {
        writeTotalRow(labels.igst, CurrencyUtils.formatPaise(invoice.igstPaise, sym, true))
      }
    }
    if (invoice.roundOffPaise != 0L) {
      writeTotalRow(labels.roundOff, CurrencyUtils.formatPaise(invoice.roundOffPaise, sym, true))
    }

    writeSeparator('=')
    writeTotalRow(
      labels.grandTotal,
      CurrencyUtils.formatPaise(invoice.grandTotalPaise, sym, true),
      bold = true,
    )
    writeSeparator('-')
    writeTotalRow(labels.amountPaid, CurrencyUtils.formatPaise(invoice.amountPaidPaise, sym, true))
    writeTotalRow(
      labels.balanceDue,
      CurrencyUtils.formatPaise(invoice.balanceDuePaise, sym, true),
      bold = invoice.balanceDuePaise > 0L,
    )
    writeTotalRow(labels.paymentMode, invoice.paymentMethod.label)

    // Real UPI QR only when business has valid UPI ID
    val validUpiUri = UpiQrGenerator.buildUpiPaymentUri(business, invoice)
    if (validUpiUri != null && upiQrBitmap != null) {
      val qrTargetSize = if (paperWidth == ThermalPaperWidth.MM_58) 192 else 240
      val qrRaster = encodeRasterBitmap(upiQrBitmap, qrTargetSize, qrTargetSize)
      if (qrRaster != null) {
        writeSeparator('-')
        writeBytes(CMD_ALIGN_CENTER)
        writeWrapped(labels.scanToPay)
        writeBytes(qrRaster)
        writeWrapped(business.upiId.trim())
        writeBytes(CMD_ALIGN_LEFT)
      }
    }

    writeSeparator('-')
    writeBytes(CMD_ALIGN_CENTER)
    writeWrapped(business.thankYouMessage.ifBlank { "Thank you for your business!" })
    if (business.termsAndConditions.isNotBlank()) {
      writeWrapped(business.termsAndConditions)
    }

    writeBytes(CMD_ALIGN_LEFT)
    writeBytes(CMD_FEED_4)
    if (autoCut) {
      writeBytes(CMD_PARTIAL_CUT)
    }
    return out.toByteArray()
  }

  /**
   * Encodes an arbitrary ARGB bitmap into ESC/POS GS v 0 monochrome raster command bytes,
   * preserving original aspect ratio and compositing against white.
   */
  fun encodeRasterBitmap(
    source: Bitmap,
    maxWidthPx: Int,
    maxHeightPx: Int,
  ): ByteArray? {
    if (source.width <= 0 || source.height <= 0 || maxWidthPx < 8 || maxHeightPx <= 0) return null
    val scale = minOf(maxWidthPx.toFloat() / source.width, maxHeightPx.toFloat() / source.height, 1f)
    val rawW = (source.width * scale).roundToInt().coerceAtLeast(8)
    val targetW = ((rawW + 7) / 8) * 8 // GS v 0 requires width in multiples of 8 pixels
    val targetH = (source.height * (targetW.toFloat() / source.width)).roundToInt().coerceAtLeast(1)

    val normalized = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    try {
      val canvas = Canvas(normalized)
      canvas.drawColor(Color.WHITE)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(
        source,
        Rect(0, 0, source.width, source.height),
        Rect(0, 0, targetW, targetH),
        paint,
      )

      val widthBytes = targetW / 8
      val xL = (widthBytes and 0xFF).toByte()
      val xH = ((widthBytes shr 8) and 0xFF).toByte()
      val yL = (targetH and 0xFF).toByte()
      val yH = ((targetH shr 8) and 0xFF).toByte()

      val pixels = IntArray(targetW * targetH)
      normalized.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

      val rasterBytes = ByteArray(8 + widthBytes * targetH)
      rasterBytes[0] = 0x1D
      rasterBytes[1] = 0x76
      rasterBytes[2] = 0x30
      rasterBytes[3] = 0x00
      rasterBytes[4] = xL
      rasterBytes[5] = xH
      rasterBytes[6] = yL
      rasterBytes[7] = yH

      var offset = 8
      for (y in 0 until targetH) {
        val rowOffset = y * targetW
        for (bx in 0 until widthBytes) {
          var slice = 0
          for (bit in 0 until 8) {
            val px = pixels[rowOffset + bx * 8 + bit]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val luminance = (r * 299 + g * 587 + b * 114) / 1000
            if (luminance < 160) {
              slice = slice or (1 shl (7 - bit))
            }
          }
          rasterBytes[offset++] = slice.toByte()
        }
      }
      return rasterBytes
    } finally {
      normalized.recycle()
    }
  }

  internal fun wrapPlainText(text: String, maxCols: Int): List<String> {
    if (maxCols <= 0) return listOf(text)
    val cleaned = text.trim()
    if (cleaned.isEmpty()) return emptyList()

    val lines = mutableListOf<String>()
    for (paragraph in cleaned.split('\n')) {
      val trimmed = paragraph.trim()
      if (trimmed.isEmpty()) continue
      val words = trimmed.split(Regex("\\s+"))
      var current = StringBuilder()
      for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (candidate.length <= maxCols) {
          current = StringBuilder(candidate)
        } else {
          if (current.isNotEmpty()) {
            lines.add(current.toString())
            current = StringBuilder()
          }
          if (word.length <= maxCols) {
            current.append(word)
          } else {
            var i = 0
            while (i < word.length) {
              val end = (i + maxCols).coerceAtMost(word.length)
              val slice = word.substring(i, end)
              if (end < word.length) lines.add(slice) else current.append(slice)
              i = end
            }
          }
        }
      }
      if (current.isNotEmpty()) lines.add(current.toString())
    }
    return lines
  }

  internal fun formatLeftRightLines(left: String, right: String, maxCols: Int): List<String> {
    val cleanLeft = sanitizeForEscPos(left.trimEnd())
    val cleanRight = sanitizeForEscPos(right.trim())
    if (cleanLeft.length + 1 + cleanRight.length <= maxCols) {
      val spaces = (maxCols - cleanLeft.length - cleanRight.length).coerceAtLeast(1)
      return listOf(cleanLeft + " ".repeat(spaces) + cleanRight)
    }
    val wrappedLeft = wrapPlainText(cleanLeft, maxCols).toMutableList()
    if (wrappedLeft.isEmpty()) {
      return listOf(cleanRight.padStart(maxCols))
    }
    val last = wrappedLeft.last()
    return if (last.length + 1 + cleanRight.length <= maxCols) {
      val spaces = maxCols - last.length - cleanRight.length
      wrappedLeft[wrappedLeft.lastIndex] = last + " ".repeat(spaces) + cleanRight
      wrappedLeft
    } else {
      wrappedLeft.add(cleanRight.padStart(maxCols))
      wrappedLeft
    }
  }

  private fun currencySafe(symbol: String): String =
    if (symbol.contains('₹')) symbol.replace("₹", "Rs.") else sanitizeForEscPos(symbol)

  internal fun sanitizeForEscPos(input: String): String {
    val replaced = input.replace("₹", "Rs.").replace("•", "-").replace("×", "x")
    val sb = StringBuilder(replaced.length)
    for (ch in replaced) {
      when {
        ch == '\n' || ch == '\t' -> sb.append(ch)
        ch.code in 32..126 -> sb.append(ch)
        else -> sb.append(' ')
      }
    }
    return sb.toString()
  }
}
