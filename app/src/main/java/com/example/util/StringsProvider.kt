package com.example.util

import com.example.model.AppLanguage

data class PdfLabels(
  val taxInvoice: String,
  val invoiceNo: String,
  val date: String,
  val time: String,
  val billTo: String,
  val item: String,
  val qty: String,
  val rate: String,
  val gst: String,
  val amount: String,
  val subtotal: String,
  val discount: String,
  val taxableValue: String,
  val cgst: String,
  val sgst: String,
  val igst: String,
  val roundOff: String,
  val grandTotal: String,
  val amountPaid: String,
  val balanceDue: String,
  val paymentMode: String,
  val terms: String,
  val authorizedSignatory: String,
)

object AppStrings {
  fun pdfLabels(language: AppLanguage): PdfLabels {
    return when (language) {
      AppLanguage.ENGLISH ->
        PdfLabels(
          taxInvoice = "TAX INVOICE",
          invoiceNo = "Invoice No.",
          date = "Date",
          time = "Time",
          billTo = "Customer",
          item = "Item",
          qty = "Qty",
          rate = "Rate",
          gst = "GST",
          amount = "Amount",
          subtotal = "Subtotal",
          discount = "Discount",
          taxableValue = "Taxable Value",
          cgst = "CGST",
          sgst = "SGST",
          igst = "IGST",
          roundOff = "Round Off",
          grandTotal = "Grand Total",
          amountPaid = "Amount Paid",
          balanceDue = "Balance Due",
          paymentMode = "Payment Mode",
          terms = "Terms & Conditions",
          authorizedSignatory = "Authorized Signatory",
        )
      AppLanguage.HINDI ->
        PdfLabels(
          taxInvoice = "कर चालान (TAX INVOICE)",
          invoiceNo = "बिल संख्या",
          date = "दिनांक",
          time = "समय",
          billTo = "ग्राहक",
          item = "वस्तु",
          qty = "मात्रा",
          rate = "दर",
          gst = "जीएसटी",
          amount = "राशि",
          subtotal = "उप-योग",
          discount = "छूट",
          taxableValue = "कर योग्य मूल्य",
          cgst = "सीजीएसटी",
          sgst = "एसजीएसटी",
          igst = "आईजीएसटी",
          roundOff = "पूर्णांकित",
          grandTotal = "कुल राशि",
          amountPaid = "भुगतान राशि",
          balanceDue = "शेष राशि",
          paymentMode = "भुगतान माध्यम",
          terms = "नियम एवं शर्तें",
          authorizedSignatory = "अधिकृत हस्ताक्षरकर्ता",
        )
    }
  }
}
