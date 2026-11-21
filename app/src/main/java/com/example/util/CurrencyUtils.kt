package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {
  fun formatPaise(
    paise: Long,
    symbol: String = "₹",
    showDecimalsIfZero: Boolean = false,
  ): String {
    val negative = paise < 0
    val absPaise = abs(paise)
    val rupees = absPaise / 100
    val remPaise = absPaise % 100
    val formattedRupees = formatIndianInteger(rupees)
    val decimalPart =
      if (remPaise > 0 || showDecimalsIfZero) {
        ".%02d".format(Locale.US, remPaise)
      } else {
        ""
      }
    val prefix = if (negative) "-$symbol" else symbol
    return "$prefix$formattedRupees$decimalPart"
  }

  private fun formatIndianInteger(value: Long): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val lastThree = s.takeLast(3)
    val rest = s.dropLast(3)
    val grouped = StringBuilder()
    var count = 0
    for (i in rest.length - 1 downTo 0) {
      grouped.append(rest[i])
      count++
      if (count == 2 && i != 0) {
        grouped.append(',')
        count = 0
      }
    }
    return "${grouped.reverse()},$lastThree"
  }

  fun rupeesToPaise(input: String): Long {
    val cleaned = input.trim().replace(",", "").replace(Regex("[^0-9.-]"), "")
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return 0L
    return runCatching {
        BigDecimal(cleaned)
          .multiply(BigDecimal(100))
          .setScale(0, RoundingMode.HALF_UP)
          .toLong()
      }
      .getOrDefault(0L)
  }

  fun paiseToEditString(paise: Long): String {
    if (paise == 0L) return ""
    val bd = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    return bd.stripTrailingZeros().toPlainString()
  }

  fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toLong().toString()
    else BigDecimal.valueOf(qty).stripTrailingZeros().toPlainString()
  }

  fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(timestamp))
  }

  fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
  }

  fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
  }
}
