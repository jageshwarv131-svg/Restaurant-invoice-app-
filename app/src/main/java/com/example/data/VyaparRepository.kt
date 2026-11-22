package com.example.data

import android.net.Uri
import com.example.model.AppSettings
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Expense
import com.example.model.FullBackupBundle
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.InventoryMovement
import com.example.model.Invoice
import com.example.model.MovementType
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VyaparRepository(private val storage: JsonStorageManager) {

  private val _isLoaded = MutableStateFlow(false)
  val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

  private val _business = MutableStateFlow(BusinessProfile())
  val business: StateFlow<BusinessProfile> = _business.asStateFlow()

  private val _products = MutableStateFlow<List<Product>>(emptyList())
  val products: StateFlow<List<Product>> = _products.asStateFlow()

  private val _customers = MutableStateFlow<List<Customer>>(emptyList())
  val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

  private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
  val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

  private val _orders = MutableStateFlow<List<Order>>(emptyList())
  val orders: StateFlow<List<Order>> = _orders.asStateFlow()

  private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
  val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

  private val _inventoryMovements = MutableStateFlow<List<InventoryMovement>>(emptyList())
  val inventoryMovements: StateFlow<List<InventoryMovement>> = _inventoryMovements.asStateFlow()

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  val invoicesDirectory: File get() = storage.invoicesDir

  suspend fun loadAll() {
    _business.value = storage.readBusiness()
    _products.value = storage.readProducts()
    _customers.value = storage.readCustomers()
    _invoices.value = storage.readInvoices()
    _orders.value = storage.readOrders()
    _expenses.value = storage.readExpenses()
    _inventoryMovements.value = storage.readInventoryMovements()
    _settings.value = storage.readSettings()
    _isLoaded.value = true
  }

  suspend fun copyPickedLogo(uri: Uri): String? =
    storage.copyPickedImageToInternal(uri, "shop_logo")

  suspend fun copyPickedProductImage(uri: Uri): String? =
    storage.copyPickedImageToInternal(uri, "product")

  suspend fun saveBusinessProfile(profile: BusinessProfile, markSetupDone: Boolean = true) {
    val updated = profile.copy(updatedAt = System.currentTimeMillis())
    _business.value = updated
    storage.writeBusiness(updated)
    if (markSetupDone && !_settings.value.businessSetupCompleted) {
      updateSettings(_settings.value.copy(businessSetupCompleted = true))
    }
  }

  suspend fun updateSettings(newSettings: AppSettings) {
    _settings.value = newSettings
    storage.writeSettings(newSettings)
  }

  suspend fun completeOnboarding() {
    updateSettings(_settings.value.copy(onboardingCompleted = true))
  }

  suspend fun upsertProduct(product: Product) {
    val now = System.currentTimeMillis()
    val current = _products.value.toMutableList()
    val existingIdx = current.indexOfFirst { it.id == product.id }
    if (existingIdx >= 0) {
      current[existingIdx] = product.copy(updatedAt = now)
    } else {
      val finalProduct =
        product.copy(
          id = product.id.ifBlank { UUID.randomUUID().toString() },
          createdAt = now,
          updatedAt = now,
        )
      current.add(0, finalProduct)
      if (finalProduct.stockQty > 0.0) {
        recordInventoryMovementInternal(
          productId = finalProduct.id,
          productName = finalProduct.name,
          type = MovementType.STOCK_IN,
          delta = finalProduct.stockQty,
          stockAfter = finalProduct.stockQty,
          reason = "Opening Stock",
        )
      }
    }
    _products.value = current
    storage.writeProducts(current)
  }

  suspend fun deleteProduct(productId: String) {
    val updated = _products.value.filterNot { it.id == productId }
    _products.value = updated
    storage.writeProducts(updated)
  }

  suspend fun adjustProductStock(
    productId: String,
    type: MovementType,
    quantity: Double,
    reason: String,
  ) {
    if (quantity < 0) return
    val current = _products.value.toMutableList()
    val idx = current.indexOfFirst { it.id == productId }
    if (idx < 0) return
    val product = current[idx]
    val newStock: Double
    val delta: Double
    when (type) {
      MovementType.STOCK_IN -> {
        newStock = product.stockQty + quantity
        delta = quantity
      }
      MovementType.STOCK_OUT -> {
        newStock = (product.stockQty - quantity).coerceAtLeast(0.0)
        delta = newStock - product.stockQty
      }
      MovementType.ADJUSTMENT -> {
        newStock = quantity.coerceAtLeast(0.0)
        delta = newStock - product.stockQty
      }
      MovementType.SALE -> {
        newStock = (product.stockQty - quantity).coerceAtLeast(0.0)
        delta = newStock - product.stockQty
      }
    }
    current[idx] = product.copy(stockQty = newStock, updatedAt = System.currentTimeMillis())
    _products.value = current
    storage.writeProducts(current)

    recordInventoryMovementInternal(
      productId = product.id,
      productName = product.name,
      type = type,
      delta = delta,
      stockAfter = newStock,
      reason = reason.ifBlank { type.label },
    )
  }

  private suspend fun recordInventoryMovementInternal(
    productId: String,
    productName: String,
    type: MovementType,
    delta: Double,
    stockAfter: Double,
    reason: String,
  ) {
    val movement =
      InventoryMovement(
        id = UUID.randomUUID().toString(),
        productId = productId,
        productName = productName,
        type = type,
        quantityDelta = delta,
        stockAfter = stockAfter,
        reason = reason,
        timestamp = System.currentTimeMillis(),
      )
    val updated = listOf(movement) + _inventoryMovements.value
    _inventoryMovements.value = updated
    storage.writeInventoryMovements(updated)
  }

  fun normalizeMobile10(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length >= 10) digits.takeLast(10) else digits
  }

  suspend fun findOrCreateCustomerByMobile(
    name: String,
    mobile: String,
    address: String = "",
    gstin: String = "",
  ): Customer? {
    val normMobile = normalizeMobile10(mobile)
    val trimmedName = name.trim()
    if (trimmedName.isBlank() && normMobile.isBlank()) return null

    if (normMobile.length == 10) {
      val existing =
        _customers.value.firstOrNull { normalizeMobile10(it.mobile) == normMobile }
      if (existing != null) {
        if (existing.name.isBlank() && trimmedName.isNotBlank()) {
          return upsertCustomer(existing.copy(name = trimmedName))
        }
        return existing
      }
    }

    return upsertCustomer(
      Customer(
        id = "",
        name = trimmedName.ifBlank { normMobile },
        mobile = normMobile.ifBlank { mobile.trim() },
        whatsapp = normMobile.ifBlank { mobile.trim() },
        address = address.trim(),
        gstin = gstin.trim().uppercase(Locale.US),
      )
    )
  }

  suspend fun upsertCustomer(customer: Customer): Customer {
    val now = System.currentTimeMillis()
    val current = _customers.value.toMutableList()
    val normMobile = normalizeMobile10(customer.mobile)
    val idx =
      if (customer.id.isNotBlank()) {
        current.indexOfFirst { it.id == customer.id }
      } else if (normMobile.length == 10) {
        current.indexOfFirst { normalizeMobile10(it.mobile) == normMobile }
      } else {
        -1
      }
    val saved =
      if (idx >= 0) {
        val existing = current[idx]
        val updated =
          if (customer.id.isBlank()) {
            existing.copy(
              name = customer.name.ifBlank { existing.name },
              mobile = normMobile.ifBlank { customer.mobile.ifBlank { existing.mobile } },
              whatsapp = existing.whatsapp.ifBlank { normMobile.ifBlank { customer.whatsapp } },
              address = customer.address.ifBlank { existing.address },
              gstin = customer.gstin.ifBlank { existing.gstin },
              updatedAt = now,
            )
          } else {
            customer.copy(updatedAt = now)
          }
        current[idx] = updated
        updated
      } else {
        val created =
          customer.copy(
            id = customer.id.ifBlank { UUID.randomUUID().toString() },
            mobile = if (normMobile.length == 10) normMobile else customer.mobile.trim(),
            whatsapp =
              customer.whatsapp.ifBlank {
                if (normMobile.length == 10) normMobile else customer.mobile.trim()
              },
            createdAt = now,
            updatedAt = now,
          )
        current.add(0, created)
        created
      }
    _customers.value = current
    storage.writeCustomers(current)
    return saved
  }

  suspend fun deleteCustomer(customerId: String) {
    val updated = _customers.value.filterNot { it.id == customerId }
    _customers.value = updated
    storage.writeCustomers(updated)
  }

  suspend fun collectCustomerPayment(customerId: String, amountPaise: Long) {
    if (amountPaise <= 0L) return
    val current = _customers.value.toMutableList()
    val idx = current.indexOfFirst { it.id == customerId }
    if (idx >= 0) {
      val c = current[idx]
      current[idx] =
        c.copy(
          outstandingBalancePaise = (c.outstandingBalancePaise - amountPaise).coerceAtLeast(0L),
          updatedAt = System.currentTimeMillis(),
        )
      _customers.value = current
      storage.writeCustomers(current)
    }
  }

  suspend fun completeSaleAndCreateInvoice(
    customer: Customer?,
    cartItems: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    paymentMethod: PaymentMethod,
    amountPaidPaise: Long,
    notes: String = "",
    customCustomerName: String = "",
    customCustomerMobile: String = "",
    customCustomerAddress: String = "",
    customCustomerGstin: String = "",
  ): Invoice {
    val calc =
      BillingCalculator.calculate(
        items = cartItems,
        discountValue = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        taxType = taxType,
        taxMode = taxMode,
        autoRoundOff = _settings.value.autoRoundOff,
      )

    val biz = _business.value
    val prefix = biz.invoicePrefix.ifBlank { "VBP" }.uppercase(Locale.US)
    val seqNum = biz.nextInvoiceNumber.coerceAtLeast(1)
    val invoiceNo = "$prefix-%04d".format(Locale.US, seqNum)
    val now = System.currentTimeMillis()
    val clampedPaid = amountPaidPaise.coerceIn(0L, calc.grandTotalPaise)
    val balanceDue = (calc.grandTotalPaise - clampedPaid).coerceAtLeast(0L)

    val resolvedName =
      customer?.name?.takeIf { it.isNotBlank() }
        ?: customCustomerName.trim().ifBlank { "Walk-in Customer" }
    val resolvedMobile =
      customer?.mobile?.takeIf { it.isNotBlank() } ?: customCustomerMobile.trim()
    val resolvedGstin =
      customer?.gstin?.takeIf { it.isNotBlank() } ?: customCustomerGstin.trim().uppercase(Locale.US)
    val resolvedAddress =
      customer?.address?.takeIf { it.isNotBlank() } ?: customCustomerAddress.trim()

    val invoice =
      Invoice(
        id = UUID.randomUUID().toString(),
        invoiceNumber = invoiceNo,
        timestamp = now,
        customerId = customer?.id,
        customerName = resolvedName,
        customerMobile = resolvedMobile,
        customerGstin = resolvedGstin,
        customerAddress = resolvedAddress,
        items = calc.computedItems,
        subtotalPaise = calc.subtotalPaise,
        discountPaise = calc.discountPaise,
        discountInput = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        taxType = taxType,
        taxMode = taxMode,
        cgstPaise = calc.cgstPaise,
        sgstPaise = calc.sgstPaise,
        igstPaise = calc.igstPaise,
        totalTaxPaise = calc.totalTaxPaise,
        roundOffPaise = calc.roundOffPaise,
        grandTotalPaise = calc.grandTotalPaise,
        amountPaidPaise = clampedPaid,
        balanceDuePaise = balanceDue,
        paymentMethod = paymentMethod,
        notes = notes,
      )

    val updatedInvoices = listOf(invoice) + _invoices.value
    _invoices.value = updatedInvoices
    storage.writeInvoices(updatedInvoices)

    // Increment invoice counter
    val updatedBiz = biz.copy(nextInvoiceNumber = seqNum + 1, updatedAt = now)
    _business.value = updatedBiz
    storage.writeBusiness(updatedBiz)

    // Deduct stock ONLY after invoice is persisted
    val currentProducts = _products.value.toMutableList()
    val newMovements = mutableListOf<InventoryMovement>()
    var productsModified = false
    for (line in calc.computedItems) {
      val prodId = line.productId ?: continue
      val idx = currentProducts.indexOfFirst { it.id == prodId }
      if (idx >= 0) {
        val prod = currentProducts[idx]
        val after = (prod.stockQty - line.quantity).coerceAtLeast(0.0)
        currentProducts[idx] = prod.copy(stockQty = after, updatedAt = now)
        productsModified = true
        newMovements.add(
          InventoryMovement(
            id = UUID.randomUUID().toString(),
            productId = prod.id,
            productName = prod.name,
            type = MovementType.SALE,
            quantityDelta = after - prod.stockQty,
            stockAfter = after,
            reason = "Sold on $invoiceNo",
            timestamp = now,
          )
        )
      }
    }
    if (productsModified) {
      _products.value = currentProducts
      storage.writeProducts(currentProducts)
      val updatedMoves = newMovements + _inventoryMovements.value
      _inventoryMovements.value = updatedMoves
      storage.writeInventoryMovements(updatedMoves)
    }

    // Update customer ledger
    if (customer != null) {
      val currentCustomers = _customers.value.toMutableList()
      val cIdx = currentCustomers.indexOfFirst { it.id == customer.id }
      if (cIdx >= 0) {
        val existing = currentCustomers[cIdx]
        currentCustomers[cIdx] =
          existing.copy(
            totalPurchasesPaise = existing.totalPurchasesPaise + calc.grandTotalPaise,
            outstandingBalancePaise = existing.outstandingBalancePaise + balanceDue,
            updatedAt = now,
          )
        _customers.value = currentCustomers
        storage.writeCustomers(currentCustomers)
      }
    }

    return invoice
  }

  suspend fun updateInvoicePdfPath(invoiceId: String, pdfPath: String) {
    val current = _invoices.value.toMutableList()
    val idx = current.indexOfFirst { it.id == invoiceId }
    if (idx >= 0) {
      current[idx] = current[idx].copy(pdfPath = pdfPath)
      _invoices.value = current
      storage.writeInvoices(current)
    }
  }

  suspend fun deleteInvoice(invoiceId: String) {
    val updated = _invoices.value.filterNot { it.id == invoiceId }
    _invoices.value = updated
    storage.writeInvoices(updated)
  }

  suspend fun createOrder(
    customer: Customer?,
    customerName: String = "",
    customerMobile: String = "",
    items: List<CartLineInput>,
    notes: String,
  ): Order {
    val resolvedCustomer =
      customer
        ?: findOrCreateCustomerByMobile(
          name = customerName,
          mobile = customerMobile,
        )
    val calc =
      BillingCalculator.calculate(
        items = items,
        discountValue = 0.0,
        isPercentageDiscount = false,
        taxType = _settings.value.defaultTaxType,
        taxMode = _settings.value.defaultTaxMode,
        autoRoundOff = _settings.value.autoRoundOff,
      )
    val now = System.currentTimeMillis()
    val maxNum =
      _orders.value.mapNotNull { it.orderNumber.substringAfterLast("-").toIntOrNull() }.maxOrNull() ?: 0
    val orderNumber = "ORD-%04d".format(Locale.US, maxNum + 1)
    val finalName =
      resolvedCustomer?.name?.takeIf { it.isNotBlank() }
        ?: customerName.trim().ifBlank { "Walk-in Customer" }
    val finalMobile =
      resolvedCustomer?.mobile?.takeIf { it.isNotBlank() }
        ?: normalizeMobile10(customerMobile).ifBlank { customerMobile.trim() }

    val order =
      Order(
        id = UUID.randomUUID().toString(),
        orderNumber = orderNumber,
        timestamp = now,
        customerId = resolvedCustomer?.id,
        customerName = finalName,
        customerMobile = finalMobile,
        items = calc.computedItems,
        totalPaise = calc.grandTotalPaise,
        status = OrderStatus.PENDING,
        notes = notes,
        updatedAt = now,
      )
    val updated = listOf(order) + _orders.value
    _orders.value = updated
    storage.writeOrders(updated)
    return order
  }

  suspend fun updatePendingOrder(
    orderId: String,
    customer: Customer?,
    customerName: String,
    customerMobile: String,
    items: List<CartLineInput>,
    discountValue: Double = 0.0,
    isPercentageDiscount: Boolean = false,
    notes: String,
  ): Order? {
    val current = _orders.value.toMutableList()
    val idx = current.indexOfFirst { it.id == orderId }
    if (idx < 0) return null
    val existing = current[idx]
    if (existing.status == OrderStatus.COMPLETED || existing.status == OrderStatus.CANCELLED) {
      return existing
    }

    val resolvedCustomer =
      customer
        ?: findOrCreateCustomerByMobile(
          name = customerName,
          mobile = customerMobile,
        )
    val calc =
      BillingCalculator.calculate(
        items = items,
        discountValue = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        taxType = _settings.value.defaultTaxType,
        taxMode = _settings.value.defaultTaxMode,
        autoRoundOff = _settings.value.autoRoundOff,
      )
    val finalName =
      resolvedCustomer?.name?.takeIf { it.isNotBlank() }
        ?: customerName.trim().ifBlank { existing.customerName }
    val finalMobile =
      resolvedCustomer?.mobile?.takeIf { it.isNotBlank() }
        ?: normalizeMobile10(customerMobile).ifBlank { customerMobile.trim() }

    val updatedOrder =
      existing.copy(
        customerId = resolvedCustomer?.id ?: existing.customerId,
        customerName = finalName,
        customerMobile = finalMobile,
        items = calc.computedItems,
        discountInput = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        totalPaise = calc.grandTotalPaise,
        notes = notes,
        updatedAt = System.currentTimeMillis(),
      )
    current[idx] = updatedOrder
    _orders.value = current
    storage.writeOrders(current)
    return updatedOrder
  }

  suspend fun confirmOrderAndCreateInvoice(
    order: Order,
    finalItems: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    paymentMethod: PaymentMethod,
    amountPaidPaise: Long,
    notes: String,
  ): Invoice {
    val resolvedCustomer =
      order.customerId?.let { id -> _customers.value.firstOrNull { it.id == id } }
        ?: findOrCreateCustomerByMobile(order.customerName, order.customerMobile)

    val invoice =
      completeSaleAndCreateInvoice(
        customer = resolvedCustomer,
        cartItems = finalItems,
        discountValue = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        taxType = taxType,
        taxMode = taxMode,
        paymentMethod = paymentMethod,
        amountPaidPaise = amountPaidPaise,
        notes = notes.ifBlank { "Order ${order.orderNumber}" },
        customCustomerName = order.customerName,
        customCustomerMobile = order.customerMobile,
      )

    val current = _orders.value.toMutableList()
    val idx = current.indexOfFirst { it.id == order.id }
    if (idx >= 0) {
      current[idx] =
        current[idx].copy(
          customerId = resolvedCustomer?.id ?: current[idx].customerId,
          customerName = invoice.customerName,
          customerMobile = invoice.customerMobile,
          items = invoice.items,
          discountInput = discountValue,
          isPercentageDiscount = isPercentageDiscount,
          totalPaise = invoice.grandTotalPaise,
          status = OrderStatus.COMPLETED,
          linkedInvoiceId = invoice.id,
          notes = notes,
          updatedAt = System.currentTimeMillis(),
        )
      _orders.value = current
      storage.writeOrders(current)
    }

    return invoice
  }

  suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
    val current = _orders.value.toMutableList()
    val idx = current.indexOfFirst { it.id == orderId }
    if (idx >= 0) {
      current[idx] = current[idx].copy(status = newStatus, updatedAt = System.currentTimeMillis())
      _orders.value = current
      storage.writeOrders(current)
    }
  }

  suspend fun linkOrderToInvoice(orderId: String, invoiceId: String) {
    val current = _orders.value.toMutableList()
    val idx = current.indexOfFirst { it.id == orderId }
    if (idx >= 0) {
      current[idx] =
        current[idx].copy(
          status = OrderStatus.COMPLETED,
          linkedInvoiceId = invoiceId,
          updatedAt = System.currentTimeMillis(),
        )
      _orders.value = current
      storage.writeOrders(current)
    }
  }

  suspend fun upsertExpense(expense: Expense) {
    val current = _expenses.value.toMutableList()
    val idx = current.indexOfFirst { it.id == expense.id }
    if (idx >= 0) {
      current[idx] = expense
    } else {
      current.add(
        0,
        expense.copy(id = expense.id.ifBlank { UUID.randomUUID().toString() }),
      )
    }
    _expenses.value = current
    storage.writeExpenses(current)
  }

  suspend fun deleteExpense(expenseId: String) {
    val updated = _expenses.value.filterNot { it.id == expenseId }
    _expenses.value = updated
    storage.writeExpenses(updated)
  }

  suspend fun buildFullBackupJson(): String {
    val now = System.currentTimeMillis()
    val bundle =
      FullBackupBundle(
        exportedAt = now,
        business = _business.value,
        products = _products.value,
        customers = _customers.value,
        invoices = _invoices.value,
        orders = _orders.value,
        expenses = _expenses.value,
        inventoryMovements = _inventoryMovements.value,
        settings = _settings.value.copy(lastBackupTimestamp = now),
      )
    updateSettings(_settings.value.copy(lastBackupTimestamp = now))
    return storage.exportFullBundleJson(bundle)
  }

  suspend fun exportSingleJson(type: String): String = storage.exportSingleJson(type)

  suspend fun validateBackupJson(jsonText: String): Result<FullBackupBundle> =
    storage.parseFullBundleJson(jsonText)

  suspend fun restoreFromBackup(bundle: FullBackupBundle) {
    storage.createPreRestoreSnapshot()
    _business.value = bundle.business
    _products.value = bundle.products
    _customers.value = bundle.customers
    _invoices.value = bundle.invoices
    _orders.value = bundle.orders
    _expenses.value = bundle.expenses
    _inventoryMovements.value = bundle.inventoryMovements
    val restoredSettings =
      bundle.settings.copy(onboardingCompleted = true, businessSetupCompleted = true)
    _settings.value = restoredSettings

    storage.writeBusiness(bundle.business)
    storage.writeProducts(bundle.products)
    storage.writeCustomers(bundle.customers)
    storage.writeInvoices(bundle.invoices)
    storage.writeOrders(bundle.orders)
    storage.writeExpenses(bundle.expenses)
    storage.writeInventoryMovements(bundle.inventoryMovements)
    storage.writeSettings(restoredSettings)
  }
}
