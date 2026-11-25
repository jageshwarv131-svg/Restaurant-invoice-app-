package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BillCalculationResult
import com.example.data.BillingCalculator
import com.example.data.CartLineInput
import com.example.data.JsonStorageManager
import com.example.data.VyaparRepository
import com.example.model.AppLanguage
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
import com.example.model.PaperFormat
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.ThemePreference
import com.example.pdf.InvoicePdfGenerator
import com.example.util.CurrencyUtils
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.SupabaseAuth
import com.example.data.SupabaseSession

data class PosCartState(
  val items: List<CartLineInput> = emptyList(),
  val selectedCustomer: Customer? = null,
  val customerName: String = "",
  val customerMobile: String = "",
  val customerAddress: String = "",
  val customerGstin: String = "",
  val saveCustomerToDirectory: Boolean = true,
  val discountInput: String = "",
  val isPercentageDiscount: Boolean = false,
  val taxType: GstTaxType = GstTaxType.INTRA_STATE_CGST_SGST,
  val taxMode: GstTaxMode = GstTaxMode.EXCLUSIVE,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val amountPaidInput: String = "",
  val amountPaidManuallyEdited: Boolean = false,
  val notes: String = "",
) {
  val calculation: BillCalculationResult
    get() =
      BillingCalculator.calculate(
        items = items,
        discountValue = discountInput.toDoubleOrNull() ?: 0.0,
        isPercentageDiscount = isPercentageDiscount,
        taxType = taxType,
        taxMode = taxMode,
        autoRoundOff = true,
      )
}

data class DashboardMetrics(
  val todaySalesPaise: Long = 0L,
  val monthlySalesPaise: Long = 0L,
  val totalBillsCount: Int = 0,
  val totalCustomersCount: Int = 0,
  val totalProductsCount: Int = 0,
  val totalExpensesPaise: Long = 0L,
  val outstandingDuesPaise: Long = 0L,
  val lowStockCount: Int = 0,
  val salesGrowthPercent: Int = 0,
  val pendingOrdersCount: Int = 0,
)

class VyaparViewModel(application: Application) : AndroidViewModel(application) {

  private val storage = JsonStorageManager(application.applicationContext)
  val repository = VyaparRepository(storage)
private val supabaseAuth = SupabaseAuth()
private val supabaseSession = SupabaseSession(application.applicationContext)

private val _isAuthLoading = MutableStateFlow(false)
val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

private val _isAuthenticated = MutableStateFlow(false)
val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

private val _authMessage = MutableStateFlow<String?>(null)
val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

private val _authUserId = MutableStateFlow<String?>(null)
val authUserId: StateFlow<String?> = _authUserId.asStateFlow()

  val isLoaded: StateFlow<Boolean> = repository.isLoaded
  val business: StateFlow<BusinessProfile> = repository.business
  val products: StateFlow<List<Product>> = repository.products
  val customers: StateFlow<List<Customer>> = repository.customers
  val invoices: StateFlow<List<Invoice>> = repository.invoices
  val orders: StateFlow<List<Order>> = repository.orders
  val expenses: StateFlow<List<Expense>> = repository.expenses
  val inventoryMovements: StateFlow<List<InventoryMovement>> = repository.inventoryMovements
  val settings: StateFlow<AppSettings> = repository.settings

  private val _isOnline = MutableStateFlow(checkCurrentConnectivity(application))
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val _isUnlocked = MutableStateFlow(false)
  val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

  private val _cart = MutableStateFlow(PosCartState())
  val cart: StateFlow<PosCartState> = _cart.asStateFlow()

  private val _activePreviewInvoice = MutableStateFlow<Invoice?>(null)
  val activePreviewInvoice: StateFlow<Invoice?> = _activePreviewInvoice.asStateFlow()

  private val _activePreviewPdf = MutableStateFlow<File?>(null)
  val activePreviewPdf: StateFlow<File?> = _activePreviewPdf.asStateFlow()

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  val dashboardMetrics: StateFlow<DashboardMetrics> =
    combine(products, customers, invoices, expenses, orders) { prods, custs, invs, exps, ords ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        val todaySales = invs.filter { it.timestamp >= todayStart }.sumOf { it.grandTotalPaise }
        val yesterdaySales =
          invs
            .filter { it.timestamp in yesterdayStart until todayStart }
            .sumOf { it.grandTotalPaise }
        val monthlySales = invs.filter { it.timestamp >= monthStart }.sumOf { it.grandTotalPaise }
        val totalExpenses = exps.sumOf { it.amountPaise }
        val customerDues = custs.sumOf { it.outstandingBalancePaise }
        val walkInDues =
          invs.filter { it.customerId == null }.sumOf { it.balanceDuePaise }
        val lowStock = prods.count { it.stockQty <= it.lowStockThreshold }
        val pendingOrders =
          ords.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.PROCESSING }
        val growth =
          when {
            yesterdaySales > 0L -> (((todaySales - yesterdaySales) * 100L) / yesterdaySales).toInt()
            todaySales > 0L -> 100
            else -> 0
          }

        DashboardMetrics(
          todaySalesPaise = todaySales,
          monthlySalesPaise = monthlySales,
          totalBillsCount = invs.size,
          totalCustomersCount = custs.size,
          totalProductsCount = prods.size,
          totalExpensesPaise = totalExpenses,
          outstandingDuesPaise = customerDues + walkInDues,
          lowStockCount = lowStock,
          salesGrowthPercent = growth,
          pendingOrdersCount = pendingOrders,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

  init {
    viewModelScope.launch {
      repository.loadAll()
      val loadedSettings = repository.settings.value
      _cart.value =
        _cart.value.copy(
          taxType = loadedSettings.defaultTaxType,
          taxMode = loadedSettings.defaultTaxMode,
        )
      if (!loadedSettings.appPinEnabled || loadedSettings.appPin.isBlank()) {
        _isUnlocked.value = true
      }
    }
    registerNetworkObserver(application)
  }

  private fun checkCurrentConnectivity(context: Context): Boolean =
    runCatching {
        val cm =
          context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      }
      .getOrDefault(false)

  private fun registerNetworkObserver(context: Context) {
    runCatching {
      val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            _isOnline.value = true
          }

          override fun onLost(network: Network) {
            _isOnline.value = checkCurrentConnectivity(context)
          }
        }
      cm.registerDefaultNetworkCallback(callback)
    }
  }
fun signUpWithSupabase(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
        _authMessage.value = "Email and password are required"
        return
    }

    viewModelScope.launch {
        _isAuthLoading.value = true
        _authMessage.value = null

        try {
            val result = withContext(Dispatchers.IO) {
                supabaseAuth.signUp(email, password)
            }

            if (result.success &&
                result.accessToken != null &&
                result.refreshToken != null
            ) {
                supabaseSession.saveSession(
                    result.accessToken,
                    result.refreshToken
                )
                _authUserId.value = result.userId
                _isAuthenticated.value = true
            }

            _authMessage.value = result.message

        } catch (e: Exception) {
            _authMessage.value = "Signup failed: ${e.message}"
        } finally {
            _isAuthLoading.value = false
        }
    }
}

fun loginWithSupabase(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
        _authMessage.value = "Email and password are required"
        return
    }

    viewModelScope.launch {
        _isAuthLoading.value = true
        _authMessage.value = null

        try {
            val result = withContext(Dispatchers.IO) {
                supabaseAuth.login(email, password)
            }

            if (result.success &&
                result.accessToken != null &&
                result.refreshToken != null
            ) {
                supabaseSession.saveSession(
                    result.accessToken,
                    result.refreshToken
                )
                _authUserId.value = result.userId
                _isAuthenticated.value = true
            }

            _authMessage.value = result.message

        } catch (e: Exception) {
            _authMessage.value = "Login failed: ${e.message}"
        } finally {
            _isAuthLoading.value = false
        }
    }
}

fun logoutFromSupabase() {
    supabaseSession.clearSession()
    _authUserId.value = null
    _isAuthenticated.value = false
    _authMessage.value = "Logged out successfully"
}

fun clearAuthMessage() {
    _authMessage.value = null
}

  fun clearToast() {
    _toastMessage.value = null
  }

  fun showToast(msg: String) {
    _toastMessage.value = msg
  }

  fun unlockWithPin(pin: String): Boolean {
    val valid = pin == settings.value.appPin
    if (valid) _isUnlocked.value = true
    return valid
  }

  fun completeOnboarding() {
    viewModelScope.launch { repository.completeOnboarding() }
  }

  fun saveBusinessProfile(
    profile: BusinessProfile,
    pickedLogoUri: Uri? = null,
    onSaved: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val finalLogoPath =
        if (pickedLogoUri != null) {
          repository.copyPickedLogo(pickedLogoUri) ?: profile.logoPath
        } else {
          profile.logoPath
        }
      repository.saveBusinessProfile(profile.copy(logoPath = finalLogoPath), markSetupDone = true)
      showToast("Business profile saved")
      onSaved()
    }
  }

  fun setThemePreference(preference: ThemePreference) {
    viewModelScope.launch {
      repository.updateSettings(
        settings.value.copy(
          themePreference = preference,
          darkMode = preference == ThemePreference.DARK,
        )
      )
    }
  }

  fun toggleDarkMode(enabled: Boolean) {
    setThemePreference(if (enabled) ThemePreference.DARK else ThemePreference.LIGHT)
  }

  fun toggleShopOpen() {
    viewModelScope.launch {
      val next = !settings.value.isShopOpen
      repository.updateSettings(settings.value.copy(isShopOpen = next))
      showToast(if (next) "Shop is now Online (Open)" else "Shop is now Offline (Dukan Close)")
    }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { repository.updateSettings(settings.value.copy(language = language)) }
  }

  fun setDefaultPaperFormat(format: PaperFormat) {
    viewModelScope.launch {
      repository.updateSettings(settings.value.copy(defaultPaperFormat = format))
    }
  }

  fun updateDefaultGstSettings(taxType: GstTaxType, taxMode: GstTaxMode, autoRoundOff: Boolean) {
    viewModelScope.launch {
      repository.updateSettings(
        settings.value.copy(
          defaultTaxType = taxType,
          defaultTaxMode = taxMode,
          autoRoundOff = autoRoundOff,
        )
      )
      _cart.value = _cart.value.copy(taxType = taxType, taxMode = taxMode)
      showToast("GST & Invoice settings updated")
    }
  }

  fun updatePinSecurity(enabled: Boolean, pin: String) {
    viewModelScope.launch {
      repository.updateSettings(settings.value.copy(appPinEnabled = enabled, appPin = pin))
      showToast(if (enabled) "App PIN lock enabled" else "App PIN lock disabled")
    }
  }

  fun upsertProduct(product: Product, pickedImageUri: Uri? = null) {
    viewModelScope.launch {
      val imagePath =
        if (pickedImageUri != null) {
          repository.copyPickedProductImage(pickedImageUri) ?: product.imagePath
        } else {
          product.imagePath
        }
      repository.upsertProduct(product.copy(imagePath = imagePath))
      showToast("${product.name} saved")
    }
  }

  fun deleteProduct(productId: String) {
    viewModelScope.launch {
      repository.deleteProduct(productId)
      showToast("Product deleted")
    }
  }

  fun adjustStock(productId: String, type: MovementType, qty: Double, reason: String) {
    viewModelScope.launch {
      repository.adjustProductStock(productId, type, qty, reason)
      showToast("Inventory updated")
    }
  }

  fun upsertCustomer(customer: Customer, onSaved: (Customer) -> Unit = {}) {
    viewModelScope.launch {
      val saved = repository.upsertCustomer(customer)
      showToast("Customer saved")
      onSaved(saved)
    }
  }

  fun deleteCustomer(customerId: String) {
    viewModelScope.launch {
      repository.deleteCustomer(customerId)
      showToast("Customer deleted")
    }
  }

  fun collectCustomerPayment(customerId: String, amountPaise: Long) {
    viewModelScope.launch {
      repository.collectCustomerPayment(customerId, amountPaise)
      showToast("Payment recorded")
    }
  }

  fun addProductToCart(product: Product) {
    val current = _cart.value
    val list = current.items.toMutableList()
    val idx = list.indexOfFirst { it.productId == product.id }
    if (idx >= 0) {
      val existing = list[idx]
      list[idx] = existing.copy(quantity = existing.quantity + 1.0)
    } else {
      list.add(
        CartLineInput(
          productId = product.id,
          name = product.name,
          unit = product.unit,
          quantity = 1.0,
          unitPricePaise = product.sellingPricePaise,
          gstPercent = product.gstPercent,
        )
      )
    }
    updateCart(current.copy(items = list))
  }

  fun decrementProductInCart(productId: String) {
    val current = _cart.value
    val idx = current.items.indexOfFirst { it.productId == productId }
    if (idx < 0) return
    val list = current.items.toMutableList()
    val newQty = list[idx].quantity - 1.0
    if (newQty <= 0.0) {
      list.removeAt(idx)
    } else {
      list[idx] = list[idx].copy(quantity = newQty)
    }
    updateCart(current.copy(items = list))
  }

  fun addCustomItemToCart(
    name: String,
    qty: Double,
    pricePaise: Long,
    gstPercent: Double,
    unit: String,
  ) {
    val current = _cart.value
    val list = current.items.toMutableList()
    list.add(
      CartLineInput(
        productId = null,
        name = name,
        unit = unit.ifBlank { "Pcs" },
        quantity = qty.coerceAtLeast(0.01),
        unitPricePaise = pricePaise.coerceAtLeast(0L),
        gstPercent = gstPercent.coerceAtLeast(0.0),
      )
    )
    updateCart(current.copy(items = list))
  }

  fun updateCartItemQty(index: Int, delta: Double) {
    val current = _cart.value
    if (index !in current.items.indices) return
    val list = current.items.toMutableList()
    val newQty = list[index].quantity + delta
    if (newQty <= 0.0) {
      list.removeAt(index)
    } else {
      list[index] = list[index].copy(quantity = newQty)
    }
    updateCart(current.copy(items = list))
  }

  fun removeCartItem(index: Int) {
    val current = _cart.value
    if (index !in current.items.indices) return
    val list = current.items.toMutableList()
    list.removeAt(index)
    updateCart(current.copy(items = list))
  }

  fun setCartCustomer(customer: Customer?) {
    updateCart(
      _cart.value.copy(
        selectedCustomer = customer,
        customerName = customer?.name ?: "",
        customerMobile = customer?.mobile ?: "",
        customerAddress = customer?.address ?: "",
        customerGstin = customer?.gstin ?: "",
      )
    )
  }

  fun updateCartCustomerDetails(
    name: String,
    mobile: String,
    address: String,
    gstin: String,
    saveToDirectory: Boolean,
  ) {
    val current = _cart.value
    val stillMatchesSelected =
      current.selectedCustomer != null && name.trim() == current.selectedCustomer.name
    _cart.value =
      current.copy(
        selectedCustomer = if (stillMatchesSelected) current.selectedCustomer else null,
        customerName = name,
        customerMobile = mobile,
        customerAddress = address,
        customerGstin = gstin,
        saveCustomerToDirectory = saveToDirectory,
      )
  }

  fun setCartDiscount(input: String, isPercentage: Boolean) {
    updateCart(_cart.value.copy(discountInput = input, isPercentageDiscount = isPercentage))
  }

  fun setCartTaxConfig(taxType: GstTaxType, taxMode: GstTaxMode) {
    updateCart(_cart.value.copy(taxType = taxType, taxMode = taxMode))
  }

  fun setCartPaymentMethod(method: PaymentMethod) {
    val current = _cart.value
    val updated =
      if (method == PaymentMethod.CREDIT) {
        current.copy(
          paymentMethod = method,
          amountPaidInput = "0",
          amountPaidManuallyEdited = false,
        )
      } else {
        current.copy(paymentMethod = method, amountPaidManuallyEdited = false)
      }
    updateCart(updated)
  }

  fun setCartAmountPaid(input: String) {
    _cart.value = _cart.value.copy(amountPaidInput = input, amountPaidManuallyEdited = true)
  }

  fun setCartNotes(notes: String) {
    _cart.value = _cart.value.copy(notes = notes)
  }

  fun clearCart() {
    val s = settings.value
    _cart.value = PosCartState(taxType = s.defaultTaxType, taxMode = s.defaultTaxMode)
  }

  private fun updateCart(next: PosCartState) {
    val synced =
      if (!next.amountPaidManuallyEdited) {
        val totalPaise = next.calculation.grandTotalPaise
        val defaultPaid =
          if (next.paymentMethod == PaymentMethod.CREDIT) "0"
          else CurrencyUtils.paiseToEditString(totalPaise)
        next.copy(amountPaidInput = defaultPaid)
      } else {
        next
      }
    _cart.value = synced
  }

  fun completeSaleAndOpenInvoice(onInvoiceReady: (Invoice) -> Unit) {
    val current = _cart.value
    if (current.items.isEmpty()) {
      showToast("Add at least one item to generate bill")
      return
    }
    viewModelScope.launch {
      val calc = current.calculation
      val paidPaise =
        if (current.amountPaidInput.isBlank() && current.paymentMethod != PaymentMethod.CREDIT) {
          calc.grandTotalPaise
        } else {
          CurrencyUtils.rupeesToPaise(current.amountPaidInput)
        }
      val trimmedName = current.customerName.trim()
      val trimmedMobile = current.customerMobile.trim()
      val trimmedAddress = current.customerAddress.trim()
      val trimmedGstin = current.customerGstin.trim().uppercase()

      val resolvedCustomer: Customer? =
        when {
          current.selectedCustomer != null -> {
            val sc = current.selectedCustomer
            if (
              trimmedMobile != sc.mobile ||
                trimmedAddress != sc.address ||
                trimmedGstin != sc.gstin
            ) {
              repository.upsertCustomer(
                sc.copy(
                  mobile = trimmedMobile,
                  whatsapp = sc.whatsapp.ifBlank { trimmedMobile },
                  address = trimmedAddress,
                  gstin = trimmedGstin,
                )
              )
            } else {
              sc
            }
          }
          (trimmedName.isNotBlank() || trimmedMobile.isNotBlank()) &&
            current.saveCustomerToDirectory -> {
            repository.upsertCustomer(
              Customer(
                id = "",
                name = trimmedName.ifBlank { trimmedMobile },
                mobile = trimmedMobile,
                whatsapp = trimmedMobile,
                address = trimmedAddress,
                gstin = trimmedGstin,
              )
            )
          }
          else -> null
        }

      val invoice =
        repository.completeSaleAndCreateInvoice(
          customer = resolvedCustomer,
          cartItems = current.items,
          discountValue = current.discountInput.toDoubleOrNull() ?: 0.0,
          isPercentageDiscount = current.isPercentageDiscount,
          taxType = current.taxType,
          taxMode = current.taxMode,
          paymentMethod = current.paymentMethod,
          amountPaidPaise = paidPaise,
          notes = current.notes,
          customCustomerName = trimmedName,
          customCustomerMobile = trimmedMobile,
          customCustomerAddress = trimmedAddress,
          customCustomerGstin = trimmedGstin,
        )
      val pdfFile = generatePdfForInvoiceInternal(invoice, settings.value.defaultPaperFormat, settings.value.language)
      val finalInvoice = invoice.copy(pdfPath = pdfFile.absolutePath)
      repository.updateInvoicePdfPath(invoice.id, pdfFile.absolutePath)
      _activePreviewInvoice.value = finalInvoice
      _activePreviewPdf.value = pdfFile
      clearCart()
      onInvoiceReady(finalInvoice)
    }
  }

  fun openInvoicePreview(
    invoice: Invoice,
    format: PaperFormat = settings.value.defaultPaperFormat,
    language: AppLanguage = settings.value.language,
    onReady: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _activePreviewInvoice.value = invoice
      val pdfFile = generatePdfForInvoiceInternal(invoice, format, language)
      _activePreviewPdf.value = pdfFile
      onReady()
    }
  }

  fun regeneratePreviewPdf(format: PaperFormat, language: AppLanguage) {
    val inv = _activePreviewInvoice.value ?: return
    viewModelScope.launch {
      val pdfFile = generatePdfForInvoiceInternal(inv, format, language)
      _activePreviewPdf.value = pdfFile
    }
  }

  private suspend fun generatePdfForInvoiceInternal(
    invoice: Invoice,
    format: PaperFormat,
    language: AppLanguage,
  ): File =
    withContext(Dispatchers.IO) {
      InvoicePdfGenerator.generateInvoicePdf(
        context = getApplication(),
        outputDir = repository.invoicesDirectory,
        business = business.value,
        invoice = invoice,
        paperFormat = format,
        language = language,
      )
    }

  fun deleteInvoice(invoiceId: String) {
    viewModelScope.launch {
      repository.deleteInvoice(invoiceId)
      showToast("Invoice deleted")
    }
  }

  fun createInlineCustomer(name: String, mobile: String, onSaved: (Customer) -> Unit) {
    viewModelScope.launch {
      val saved = repository.findOrCreateCustomerByMobile(name = name, mobile = mobile)
      if (saved != null) {
        val norm = repository.normalizeMobile10(mobile)
        val existed =
          customers.value.any { it.id == saved.id && repository.normalizeMobile10(it.mobile) == norm }
        showToast(if (existed) "Existing customer selected (${saved.name})" else "Customer added")
        onSaved(saved)
      }
    }
  }

  fun createOrder(
    customer: Customer?,
    customerName: String = customer?.name ?: "",
    customerMobile: String = customer?.mobile ?: "",
    items: List<CartLineInput>,
    notes: String,
  ) {
    viewModelScope.launch {
      val created = repository.createOrder(customer, customerName, customerMobile, items, notes)
      showToast("Pending order ${created.orderNumber} saved")
    }
  }

  fun updatePendingOrder(
    orderId: String,
    customer: Customer?,
    customerName: String,
    customerMobile: String,
    items: List<CartLineInput>,
    discountValue: Double = 0.0,
    isPercentageDiscount: Boolean = false,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.updatePendingOrder(
        orderId = orderId,
        customer = customer,
        customerName = customerName,
        customerMobile = customerMobile,
        items = items,
        discountValue = discountValue,
        isPercentageDiscount = isPercentageDiscount,
        notes = notes,
      )
      showToast("Pending order updated")
    }
  }

  fun updateOrderStatus(orderId: String, status: OrderStatus) {
    viewModelScope.launch {
      repository.updateOrderStatus(orderId, status)
      showToast("Order marked ${status.label}")
    }
  }

  fun confirmOrderAndOpenInvoice(
    order: Order,
    finalItems: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    paymentMethod: PaymentMethod,
    amountPaidPaise: Long,
    notes: String,
    onReady: (Invoice) -> Unit,
  ) {
    viewModelScope.launch {
      val invoice =
        repository.confirmOrderAndCreateInvoice(
          order = order,
          finalItems = finalItems,
          discountValue = discountValue,
          isPercentageDiscount = isPercentageDiscount,
          taxType = taxType,
          taxMode = taxMode,
          paymentMethod = paymentMethod,
          amountPaidPaise = amountPaidPaise,
          notes = notes,
        )
      val pdfFile =
        generatePdfForInvoiceInternal(
          invoice,
          settings.value.defaultPaperFormat,
          settings.value.language,
        )
      val finalInv = invoice.copy(pdfPath = pdfFile.absolutePath)
      repository.updateInvoicePdfPath(invoice.id, pdfFile.absolutePath)
      _activePreviewInvoice.value = finalInv
      _activePreviewPdf.value = pdfFile
      showToast("${order.orderNumber} completed • Invoice ${invoice.invoiceNumber} created")
      onReady(finalInv)
    }
  }

  fun convertOrderToInvoice(order: Order, onReady: (Invoice) -> Unit) {
    val cartInputs =
      order.items.map {
        CartLineInput(
          productId = it.productId,
          name = it.name,
          unit = it.unit,
          quantity = it.quantity,
          unitPricePaise = it.unitPricePaise,
          gstPercent = it.gstPercent,
        )
      }
    confirmOrderAndOpenInvoice(
      order = order,
      finalItems = cartInputs,
      discountValue = order.discountInput,
      isPercentageDiscount = order.isPercentageDiscount,
      taxType = settings.value.defaultTaxType,
      taxMode = settings.value.defaultTaxMode,
      paymentMethod = PaymentMethod.CASH,
      amountPaidPaise = order.totalPaise,
      notes = order.notes.ifBlank { "Converted from ${order.orderNumber}" },
      onReady = onReady,
    )
  }

  fun upsertExpense(expense: Expense) {
    viewModelScope.launch {
      repository.upsertExpense(expense)
      showToast("Expense saved")
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      repository.deleteExpense(expenseId)
      showToast("Expense deleted")
    }
  }

  suspend fun buildFullBackupJson(): String = repository.buildFullBackupJson()

  suspend fun exportSingleJson(type: String): String = repository.exportSingleJson(type)

  suspend fun validateBackupJson(json: String): Result<FullBackupBundle> =
    repository.validateBackupJson(json)

  fun restoreFromValidatedBackup(bundle: FullBackupBundle) {
    viewModelScope.launch {
      repository.restoreFromBackup(bundle)
      showToast("Backup restored successfully")
    }
  }
}
