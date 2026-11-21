package com.example

import com.example.data.BillingCalculator
import com.example.data.CartLineInput
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingCalculatorTest {

  @Test
  fun `exclusive 18 percent CGST SGST with flat discount matches exact paise`() {
    val items =
      listOf(
        CartLineInput(name = "Burger", quantity = 2.0, unitPricePaise = 12000L, gstPercent = 18.0),
        CartLineInput(name = "Pizza", quantity = 1.0, unitPricePaise = 20000L, gstPercent = 18.0),
      )
    // Subtotal = 440.00, Discount = 40.00 -> Taxable = 400.00, 18% GST = 72.00 -> Total = 472.00
    val result =
      BillingCalculator.calculate(
        items = items,
        discountValue = 40.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTRA_STATE_CGST_SGST,
        taxMode = GstTaxMode.EXCLUSIVE,
        autoRoundOff = true,
      )

    assertEquals(44000L, result.subtotalPaise)
    assertEquals(4000L, result.discountPaise)
    assertEquals(40000L, result.taxableValuePaise)
    assertEquals(3600L, result.cgstPaise)
    assertEquals(3600L, result.sgstPaise)
    assertEquals(0L, result.igstPaise)
    assertEquals(7200L, result.totalTaxPaise)
    assertEquals(47200L, result.grandTotalPaise)
  }

  @Test
  fun `inclusive GST splits tax out of gross total without inflating grand total`() {
    val items =
      listOf(
        CartLineInput(name = "Item A", quantity = 1.0, unitPricePaise = 11800L, gstPercent = 18.0)
      )
    val result =
      BillingCalculator.calculate(
        items = items,
        discountValue = 0.0,
        isPercentageDiscount = false,
        taxType = GstTaxType.INTER_STATE_IGST,
        taxMode = GstTaxMode.INCLUSIVE,
        autoRoundOff = true,
      )

    assertEquals(11800L, result.grandTotalPaise)
    assertEquals(10000L, result.taxableValuePaise)
    assertEquals(1800L, result.igstPaise)
    assertEquals(0L, result.cgstPaise)
  }

  @Test
  fun `rupeesToPaise parses plain numbers, commas, and currency symbols`() {
    assertEquals(12000L, com.example.util.CurrencyUtils.rupeesToPaise("120"))
    assertEquals(12050L, com.example.util.CurrencyUtils.rupeesToPaise("₹ 120.50"))
    assertEquals(125000L, com.example.util.CurrencyUtils.rupeesToPaise("1,250"))
  }
}
