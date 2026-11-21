package com.example.data

import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.InvoiceItem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

data class CartLineInput(
  val productId: String? = null,
  val name: String,
  val unit: String = "Pcs",
  val quantity: Double,
  val unitPricePaise: Long,
  val gstPercent: Double,
)

data class BillCalculationResult(
  val computedItems: List<InvoiceItem>,
  val subtotalPaise: Long,
  val discountPaise: Long,
  val taxableValuePaise: Long,
  val cgstPaise: Long,
  val sgstPaise: Long,
  val igstPaise: Long,
  val totalTaxPaise: Long,
  val roundOffPaise: Long,
  val grandTotalPaise: Long,
)

object BillingCalculator {

  fun calculate(
    items: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    autoRoundOff: Boolean = true,
  ): BillCalculationResult {
    if (items.isEmpty()) {
      return BillCalculationResult(
        computedItems = emptyList(),
        subtotalPaise = 0L,
        discountPaise = 0L,
        taxableValuePaise = 0L,
        cgstPaise = 0L,
        sgstPaise = 0L,
        igstPaise = 0L,
        totalTaxPaise = 0L,
        roundOffPaise = 0L,
        grandTotalPaise = 0L,
      )
    }

    val rawLineAmounts =
      items.map { item ->
        BigDecimal.valueOf(item.quantity)
          .multiply(BigDecimal.valueOf(item.unitPricePaise))
          .setScale(0, RoundingMode.HALF_UP)
          .toLong()
      }

    val rawSubtotalPaise = rawLineAmounts.sum()

    val discountPaise =
      if (isPercentageDiscount) {
        val clampedPct = discountValue.coerceIn(0.0, 100.0)
        BigDecimal.valueOf(rawSubtotalPaise)
          .multiply(BigDecimal.valueOf(clampedPct))
          .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
          .toLong()
      } else {
        BigDecimal.valueOf(discountValue.coerceAtLeast(0.0))
          .multiply(BigDecimal(100))
          .setScale(0, RoundingMode.HALF_UP)
          .toLong()
          .coerceAtMost(rawSubtotalPaise)
      }

    val discountRatio =
      if (rawSubtotalPaise > 0) {
        (rawSubtotalPaise - discountPaise).toDouble() / rawSubtotalPaise.toDouble()
      } else {
        1.0
      }

    var totalTaxablePaise = 0L
    var totalTaxPaise = 0L

    val computedItems =
      items.mapIndexed { index, item ->
        val grossLinePaise = rawLineAmounts[index]
        val netAfterDiscountPaise =
          (grossLinePaise * discountRatio).roundToLong().coerceAtLeast(0L)
        val effectiveGst = if (taxType == GstTaxType.NO_GST) 0.0 else item.gstPercent.coerceAtLeast(0.0)

        val (taxablePaise, taxPaise, lineTotalPaise) =
          if (effectiveGst == 0.0) {
            Triple(grossLinePaise, 0L, grossLinePaise)
          } else if (taxMode == GstTaxMode.INCLUSIVE) {
            val divisor = BigDecimal.ONE.add(BigDecimal.valueOf(effectiveGst).divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
            val taxableAfterDiscount =
              BigDecimal.valueOf(netAfterDiscountPaise)
                .divide(divisor, 0, RoundingMode.HALF_UP)
                .toLong()
            val tax = (netAfterDiscountPaise - taxableAfterDiscount).coerceAtLeast(0L)
            Triple(taxableAfterDiscount, tax, netAfterDiscountPaise)
          } else {
            val tax =
              BigDecimal.valueOf(netAfterDiscountPaise)
                .multiply(BigDecimal.valueOf(effectiveGst))
                .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
                .toLong()
            Triple(netAfterDiscountPaise, tax, netAfterDiscountPaise + tax)
          }

        totalTaxablePaise += taxablePaise
        totalTaxPaise += taxPaise

        InvoiceItem(
          productId = item.productId,
          name = item.name,
          unit = item.unit,
          quantity = item.quantity,
          unitPricePaise = item.unitPricePaise,
          gstPercent = effectiveGst,
          taxableAmountPaise = taxablePaise,
          taxAmountPaise = taxPaise,
          lineTotalPaise = grossLinePaise,
        )
      }

    val cgstPaise: Long
    val sgstPaise: Long
    val igstPaise: Long
    when (taxType) {
      GstTaxType.INTRA_STATE_CGST_SGST -> {
        cgstPaise = totalTaxPaise / 2L
        sgstPaise = totalTaxPaise - cgstPaise
        igstPaise = 0L
      }
      GstTaxType.INTER_STATE_IGST -> {
        cgstPaise = 0L
        sgstPaise = 0L
        igstPaise = totalTaxPaise
      }
      GstTaxType.NO_GST -> {
        cgstPaise = 0L
        sgstPaise = 0L
        igstPaise = 0L
      }
    }

    val preRoundTotal =
      if (taxMode == GstTaxMode.INCLUSIVE && taxType != GstTaxType.NO_GST) {
        (rawSubtotalPaise - discountPaise).coerceAtLeast(0L)
      } else {
        (rawSubtotalPaise - discountPaise + totalTaxPaise).coerceAtLeast(0L)
      }

    val (roundOffPaise, grandTotalPaise) =
      if (autoRoundOff) {
        val roundedRupees =
          BigDecimal.valueOf(preRoundTotal)
            .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toLong()
        Pair(roundedRupees - preRoundTotal, roundedRupees)
      } else {
        Pair(0L, preRoundTotal)
      }

    val finalTaxable =
      if (taxMode == GstTaxMode.INCLUSIVE && taxType != GstTaxType.NO_GST) {
        (preRoundTotal - totalTaxPaise).coerceAtLeast(0L)
      } else {
        (rawSubtotalPaise - discountPaise).coerceAtLeast(0L)
      }

    return BillCalculationResult(
      computedItems = computedItems,
      subtotalPaise = rawSubtotalPaise,
      discountPaise = discountPaise,
      taxableValuePaise = finalTaxable,
      cgstPaise = cgstPaise,
      sgstPaise = sgstPaise,
      igstPaise = igstPaise,
      totalTaxPaise = totalTaxPaise,
      roundOffPaise = roundOffPaise,
      grandTotalPaise = grandTotalPaise,
    )
  }
}
