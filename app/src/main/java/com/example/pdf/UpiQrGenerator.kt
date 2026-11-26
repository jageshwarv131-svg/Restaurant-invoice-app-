package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.BusinessProfile
import com.example.model.Invoice
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.util.Locale

object UpiQrGenerator {

  private val UPI_ID_REGEX = Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$")

  fun isValidUpiId(upiId: String?): Boolean {
    val cleaned = upiId?.trim().orEmpty()
    return cleaned.isNotEmpty() && UPI_ID_REGEX.matches(cleaned)
  }

  /**
   * Builds a valid NPCI UPI deep-link URI strictly from the business's real UPI ID.
   * Returns null when the business has no valid UPI ID so a fake QR is never rendered.
   */
  fun buildUpiPaymentUri(business: BusinessProfile, invoice: Invoice): String? {
    val pa = business.upiId.trim()
    if (!isValidUpiId(pa)) return null

    val pn = business.businessName.trim().ifBlank { "Merchant" }
    val payablePaise =
      if (invoice.balanceDuePaise > 0L) invoice.balanceDuePaise else invoice.grandTotalPaise
    val note = "Invoice ${invoice.invoiceNumber}".trim()

    return buildString {
      append("upi://pay?")
      append("pa=").append(encodeParam(pa))
      append("&pn=").append(encodeParam(pn))
      if (invoice.invoiceNumber.isNotBlank()) {
        append("&tr=").append(encodeParam(invoice.invoiceNumber.trim()))
      }
      append("&tn=").append(encodeParam(note))
      if (payablePaise > 0L) {
        val amountStr = String.format(Locale.US, "%.2f", payablePaise / 100.0)
        append("&am=").append(amountStr)
      }
      append("&cu=INR")
    }
  }

  fun encodeQrMatrix(content: String, sizePx: Int = 220): BitMatrix? {
    if (content.isBlank() || sizePx <= 0) return null
    val hints =
      mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8",
      )
    return runCatching {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      }
      .getOrNull()
  }

  fun generateQrBitmap(business: BusinessProfile, invoice: Invoice, sizePx: Int = 220): Bitmap? {
    val uri = buildUpiPaymentUri(business, invoice) ?: return null
    return generateQrBitmapFromUri(uri, sizePx)
  }

  fun generateQrBitmapFromUri(uri: String, sizePx: Int = 220): Bitmap? {
    val matrix = encodeQrMatrix(uri, sizePx) ?: return null
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
      val offset = y * width
      for (x in 0 until width) {
        pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
      }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, width, 0, 0, width, height)
    }
  }

  private fun encodeParam(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
