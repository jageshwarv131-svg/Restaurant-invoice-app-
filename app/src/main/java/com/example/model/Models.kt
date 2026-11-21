package com.example.model

import com.squareup.moshi.JsonClass

enum class GstTaxType(val label: String) {
  INTRA_STATE_CGST_SGST("CGST + SGST (Intra-State)"),
  INTER_STATE_IGST("IGST (Inter-State)"),
  NO_GST("No GST (Exempt)"),
}

enum class GstTaxMode(val label: String) {
  EXCLUSIVE("Tax Exclusive"),
  INCLUSIVE("Tax Inclusive"),
}

enum class PaymentMethod(val label: String) {
  CASH("Cash"),
  UPI("UPI"),
  CARD("Card"),
  BANK_TRANSFER("Bank Transfer"),
  CREDIT("Credit / Udhar"),
}

enum class OrderStatus(val label: String) {
  PENDING("Pending"),
  PROCESSING("Processing"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
  REFUNDED("Refunded"),
}

enum class ExpenseCategory(val label: String) {
  RENT("Rent"),
  ELECTRICITY("Electricity"),
  SALARY("Salary"),
  TRANSPORT("Transport"),
  STOCK_PURCHASE("Stock Purchase"),
  MAINTENANCE("Maintenance"),
  OTHER("Other"),
}

enum class MovementType(val label: String) {
  STOCK_IN("Stock In"),
  STOCK_OUT("Stock Out"),
  ADJUSTMENT("Adjustment"),
  SALE("Sale"),
}

enum class PaperFormat(val label: String) {
  A4("A4 Invoice"),
  THERMAL_80MM("80mm Receipt"),
}

enum class AppLanguage(val code: String, val label: String) {
  ENGLISH("en", "English"),
  HINDI("hi", "हिन्दी (Hindi)"),
}

@JsonClass(generateAdapter = true)
data class BusinessProfile(
  val businessName: String = "",
  val ownerName: String = "",
  val logoPath: String? = null,
  val mobile: String = "",
  val whatsapp: String = "",
  val email: String = "",
  val address: String = "",
  val city: String = "",
  val state: String = "",
  val pinCode: String = "",
  val gstin: String = "",
  val pan: String = "",
  val category: String = "Retail Store",
  val currencySymbol: String = "₹",
  val invoicePrefix: String = "VBP",
  val tagline: String = "Bill Smart. Grow Smart.",
  val termsAndConditions: String =
    "1. Goods once sold will not be taken back.\n2. Subject to local jurisdiction.",
  val thankYouMessage: String = "Thank you for your business! Visit again!",
  val nextInvoiceNumber: Int = 1,
  val updatedAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class Product(
  val id: String,
  val name: String,
  val sku: String = "",
  val barcode: String = "",
  val category: String = "General",
  val purchasePricePaise: Long = 0L,
  val sellingPricePaise: Long = 0L,
  val mrpPaise: Long = 0L,
  val stockQty: Double = 0.0,
  val lowStockThreshold: Double = 5.0,
  val unit: String = "Pcs",
  val gstPercent: Double = 0.0,
  val imagePath: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class Customer(
  val id: String,
  val name: String,
  val mobile: String = "",
  val whatsapp: String = "",
  val address: String = "",
  val gstin: String = "",
  val totalPurchasesPaise: Long = 0L,
  val outstandingBalancePaise: Long = 0L,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class InvoiceItem(
  val productId: String? = null,
  val name: String,
  val unit: String = "Pcs",
  val quantity: Double = 1.0,
  val unitPricePaise: Long = 0L,
  val gstPercent: Double = 0.0,
  val taxableAmountPaise: Long = 0L,
  val taxAmountPaise: Long = 0L,
  val lineTotalPaise: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class Invoice(
  val id: String,
  val invoiceNumber: String,
  val timestamp: Long = System.currentTimeMillis(),
  val customerId: String? = null,
  val customerName: String = "Walk-in Customer",
  val customerMobile: String = "",
  val customerGstin: String = "",
  val customerAddress: String = "",
  val items: List<InvoiceItem> = emptyList(),
  val subtotalPaise: Long = 0L,
  val discountPaise: Long = 0L,
  val discountInput: Double = 0.0,
  val isPercentageDiscount: Boolean = false,
  val taxType: GstTaxType = GstTaxType.INTRA_STATE_CGST_SGST,
  val taxMode: GstTaxMode = GstTaxMode.EXCLUSIVE,
  val cgstPaise: Long = 0L,
  val sgstPaise: Long = 0L,
  val igstPaise: Long = 0L,
  val totalTaxPaise: Long = 0L,
  val roundOffPaise: Long = 0L,
  val grandTotalPaise: Long = 0L,
  val amountPaidPaise: Long = 0L,
  val balanceDuePaise: Long = 0L,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val notes: String = "",
  val pdfPath: String? = null,
)

@JsonClass(generateAdapter = true)
data class Order(
  val id: String,
  val orderNumber: String,
  val timestamp: Long = System.currentTimeMillis(),
  val customerId: String? = null,
  val customerName: String = "Walk-in Customer",
  val customerMobile: String = "",
  val items: List<InvoiceItem> = emptyList(),
  val totalPaise: Long = 0L,
  val status: OrderStatus = OrderStatus.PENDING,
  val linkedInvoiceId: String? = null,
  val notes: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class Expense(
  val id: String,
  val title: String,
  val category: ExpenseCategory = ExpenseCategory.OTHER,
  val amountPaise: Long = 0L,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class InventoryMovement(
  val id: String,
  val productId: String,
  val productName: String,
  val type: MovementType,
  val quantityDelta: Double,
  val stockAfter: Double,
  val reason: String = "",
  val timestamp: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class AppSettings(
  val onboardingCompleted: Boolean = false,
  val businessSetupCompleted: Boolean = false,
  val darkMode: Boolean = false,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val defaultPaperFormat: PaperFormat = PaperFormat.A4,
  val defaultTaxType: GstTaxType = GstTaxType.INTRA_STATE_CGST_SGST,
  val defaultTaxMode: GstTaxMode = GstTaxMode.EXCLUSIVE,
  val autoRoundOff: Boolean = true,
  val appPinEnabled: Boolean = false,
  val appPin: String = "",
  val lastBackupTimestamp: Long? = null,
  val isShopOpen: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class FullBackupBundle(
  val schemaVersion: Int = 1,
  val exportedAt: Long = System.currentTimeMillis(),
  val appName: String = "Vyapar Billing Pro",
  val business: BusinessProfile,
  val products: List<Product>,
  val customers: List<Customer>,
  val invoices: List<Invoice>,
  val orders: List<Order>,
  val expenses: List<Expense>,
  val inventoryMovements: List<InventoryMovement>,
  val settings: AppSettings,
)
