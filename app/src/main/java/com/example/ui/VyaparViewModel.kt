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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.EntitlementStatus
import com.example.data.FeatureDecision
import com.example.data.FeatureEntitlements
import com.example.data.FeatureKey
import com.example.data.LicenseAccessState
import com.example.data.LicenseStatus
import com.example.data.LicenseVerificationResult
import com.example.data.PaymentRequestRecord
import com.example.data.PaymentRequestStatus
import com.example.data.SessionStorage
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState
import com.example.data.SupabaseAuth
import com.example.data.SupabaseLicenseService
import com.example.data.SupabasePaymentService
import com.example.data.SupabaseSession
import com.example.data.SupabaseTrialService
import com.example.data.TrialStatusResult

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
internal var supabaseAuth = SupabaseAuth()
internal var supabasePaymentService = SupabasePaymentService()
internal var supabaseLicenseService = SupabaseLicenseService()
internal var supabaseTrialService = SupabaseTrialService()
internal var supabaseSession: SessionStorage = SupabaseSession(application.applicationContext)

private val _isSessionCheckDone = MutableStateFlow(false)
val isSessionCheckDone: StateFlow<Boolean> = _isSessionCheckDone.asStateFlow()

private val _isAuthLoading = MutableStateFlow(false)
val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

private val _isAuthenticated = MutableStateFlow(false)
val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

private val _authMessage = MutableStateFlow<String?>(null)
val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

private val _authUserId = MutableStateFlow<String?>(null)
val authUserId: StateFlow<String?> = _authUserId.asStateFlow()

private val _licenseState = MutableStateFlow(LicenseAccessState())
val licenseState: StateFlow<LicenseAccessState> = _licenseState.asStateFlow()

private val _lastLicenseResult = MutableStateFlow<LicenseVerificationResult?>(null)
private val _lastTrialResult = MutableStateFlow<TrialStatusResult?>(null)

private val _subscriptionState = MutableStateFlow(SubscriptionState())
val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

private val _trialRemainingMillis = MutableStateFlow(0L)
val trialRemainingMillis: StateFlow<Long> = _trialRemainingMillis.asStateFlow()

private val _selectedSubscriptionPlan = MutableStateFlow(SubscriptionPlan.MONTHLY)
val selectedSubscriptionPlan: StateFlow<SubscriptionPlan> = _selectedSubscriptionPlan.asStateFlow()

private var pendingSignupEmail: String? = null
private var trialTickerJob: Job? = null

private val _isPaymentSubmitting = MutableStateFlow(false)
val isPaymentSubmitting: StateFlow<Boolean> = _isPaymentSubmitting.asStateFlow()

private val _isPaymentStatusLoading = MutableStateFlow(false)
val isPaymentStatusLoading: StateFlow<Boolean> = _isPaymentStatusLoading.asStateFlow()

private val _paymentRequests = MutableStateFlow<List<PaymentRequestRecord>>(emptyList())
val paymentRequests: StateFlow<List<PaymentRequestRecord>> = _paymentRequests.asStateFlow()

private val _paymentSubmissionMessage = MutableStateFlow<String?>(null)
val paymentSubmissionMessage: StateFlow<String?> = _paymentSubmissionMessage.asStateFlow()

private val _paymentErrorMessage = MutableStateFlow<String?>(null)
val paymentErrorMessage: StateFlow<String?> = _paymentErrorMessage.asStateFlow()

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
    restoreSupabaseSession()
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
  fun selectSubscriptionPlan(plan: SubscriptionPlan) {
    if (plan.isPaidPlan) {
      _selectedSubscriptionPlan.value = plan
    }
  }

  fun evaluateFeature(feature: FeatureKey): FeatureDecision =
    FeatureEntitlements.evaluate(
      feature = feature,
      subscriptionState = _subscriptionState.value,
      isAuthenticated = _isAuthenticated.value,
    )

  private fun recomputeSubscriptionState(isChecking: Boolean = false) {
    val resolved =
      FeatureEntitlements.resolveSubscriptionState(
        isAuthenticated = _isAuthenticated.value,
        authUserId = _authUserId.value,
        licenseResult = _lastLicenseResult.value,
        trialResult = _lastTrialResult.value,
        paymentRequests = _paymentRequests.value,
        isChecking = isChecking,
      )
    _subscriptionState.value = resolved
    _trialRemainingMillis.value =
      if (resolved.status == EntitlementStatus.TRIAL_ACTIVE) resolved.trialRemainingMillis() else 0L

    val mappedLicenseStatus = when (resolved.status) {
      EntitlementStatus.UNAUTHENTICATED -> LicenseStatus.UNAUTHENTICATED
      EntitlementStatus.CHECKING -> LicenseStatus.CHECKING
      EntitlementStatus.MONTHLY_ACTIVE,
      EntitlementStatus.YEARLY_ACTIVE,
      EntitlementStatus.LIFETIME_ACTIVE -> LicenseStatus.ACTIVE
      EntitlementStatus.MONTHLY_EXPIRED,
      EntitlementStatus.YEARLY_EXPIRED -> LicenseStatus.EXPIRED
      EntitlementStatus.REVOKED -> LicenseStatus.REVOKED
      EntitlementStatus.ERROR -> LicenseStatus.ERROR
      else -> LicenseStatus.INACTIVE
    }
    _licenseState.value =
      LicenseAccessState(
        hasActiveLicense = resolved.hasActivePaidPlan,
        status = mappedLicenseStatus,
        plan = resolved.plan,
        isChecking = isChecking,
        licenseId = resolved.licenseId,
        userId = resolved.userId,
        activatedAt = resolved.activatedAt,
        expiresAt = resolved.expiresAt,
        utr = resolved.utr,
        paidAmountInr = resolved.paidAmountInr,
        isLegacyLifetime = resolved.isLegacyLifetime,
        message = resolved.message,
        isNetworkError = resolved.isNetworkError,
      )
    updateTrialTicker(resolved)
  }

  private fun updateTrialTicker(state: SubscriptionState) {
    if (state.status != EntitlementStatus.TRIAL_ACTIVE) {
      trialTickerJob?.cancel()
      trialTickerJob = null
      return
    }
    if (trialTickerJob?.isActive == true) return
    trialTickerJob = viewModelScope.launch {
      while (isActive) {
        val current = _subscriptionState.value
        if (current.status != EntitlementStatus.TRIAL_ACTIVE) break
        val rem = current.trialRemainingMillis()
        _trialRemainingMillis.value = rem
        if (rem <= 0L) {
          recomputeSubscriptionState(isChecking = false)
          break
        }
        delay(30_000L)
      }
    }
  }

  private fun clearAccountAccessState() {
    trialTickerJob?.cancel()
    trialTickerJob = null
    _authUserId.value = null
    _isAuthenticated.value = false
    _lastLicenseResult.value = null
    _lastTrialResult.value = null
    _paymentRequests.value = emptyList()
    _paymentSubmissionMessage.value = null
    _paymentErrorMessage.value = null
    _trialRemainingMillis.value = 0L
    _subscriptionState.value =
      SubscriptionState(
        userId = null,
        plan = SubscriptionPlan.FREE,
        status = EntitlementStatus.UNAUTHENTICATED,
      )
    _licenseState.value =
      LicenseAccessState(hasActiveLicense = false, status = LicenseStatus.UNAUTHENTICATED)
  }

  fun restoreSupabaseSession() {
    viewModelScope.launch {
      try {
        val (savedAccess, savedRefresh, savedUserId) =
          withContext(Dispatchers.IO) {
            Triple(
              supabaseSession.getAccessToken(),
              supabaseSession.getRefreshToken(),
              supabaseSession.getUserId(),
            )
          }

        if (savedRefresh.isNullOrBlank()) {
          if (!savedAccess.isNullOrBlank() && !savedUserId.isNullOrBlank()) {
            _authUserId.value = savedUserId
            _isAuthenticated.value = true
            syncUserAccessInternal(
              accessToken = savedAccess,
              refreshToken = null,
              userId = savedUserId,
              isNewSignup = false,
              userCreatedAt = null,
              preserveExistingAccessWhileChecking = false,
            )
          } else {
            clearAccountAccessState()
          }
          return@launch
        }

        _isAuthLoading.value = true
        recomputeSubscriptionState(isChecking = true)
        try {
          val result = withContext(Dispatchers.IO) { supabaseAuth.refreshSession(savedRefresh) }
          if (result.success && result.accessToken != null) {
            val nextRefresh = result.refreshToken ?: savedRefresh
            val nextUserId = result.userId ?: savedUserId
            withContext(Dispatchers.IO) {
              supabaseSession.saveSession(result.accessToken, nextRefresh, nextUserId)
            }
            _authUserId.value = nextUserId
            _isAuthenticated.value = true
            if (!nextUserId.isNullOrBlank()) {
              syncUserAccessInternal(
                accessToken = result.accessToken,
                refreshToken = nextRefresh,
                userId = nextUserId,
                isNewSignup = false,
                userCreatedAt = result.userCreatedAt,
                preserveExistingAccessWhileChecking = false,
              )
            } else {
              clearAccountAccessState()
            }
          } else if (result.isNetworkError && !savedAccess.isNullOrBlank()) {
            _authUserId.value = savedUserId
            _isAuthenticated.value = true
            _lastLicenseResult.value =
              LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.ERROR,
                userId = savedUserId,
                message = "Offline mode: Core billing active. Connect to internet to sync subscription.",
                isNetworkError = true,
              )
            recomputeSubscriptionState(isChecking = false)
          } else {
            withContext(Dispatchers.IO) { supabaseSession.clearSession() }
            clearAccountAccessState()
          }
        } catch (e: Exception) {
          if (!savedAccess.isNullOrBlank()) {
            _authUserId.value = savedUserId
            _isAuthenticated.value = true
            _lastLicenseResult.value =
              LicenseVerificationResult(
                hasActiveLicense = false,
                status = LicenseStatus.ERROR,
                userId = savedUserId,
                message = "Unable to verify subscription: ${e.message ?: "Unknown error"}",
              )
            recomputeSubscriptionState(isChecking = false)
          } else {
            clearAccountAccessState()
          }
        } finally {
          _isAuthLoading.value = false
        }
      } finally {
        _isSessionCheckDone.value = true
      }
    }
  }

  fun refreshSupabaseSession() {
    restoreSupabaseSession()
  }

  fun onAppResumed() {
    if (_isAuthLoading.value || _licenseState.value.isChecking) return
    viewModelScope.launch {
      val (savedAccess, savedRefresh, savedUserId) =
        withContext(Dispatchers.IO) {
          Triple(
            supabaseSession.getAccessToken(),
            supabaseSession.getRefreshToken(),
            supabaseSession.getUserId(),
          )
        }

      if (savedRefresh.isNullOrBlank() && savedAccess.isNullOrBlank()) {
        clearAccountAccessState()
        return@launch
      }

      if (!_isAuthenticated.value || _licenseState.value.isNetworkError || _licenseState.value.status == LicenseStatus.ERROR) {
        restoreSupabaseSession()
        return@launch
      }

      val userId = (_authUserId.value ?: savedUserId).orEmpty()
      if (savedAccess.isNullOrBlank() || userId.isBlank()) {
        if (!savedRefresh.isNullOrBlank()) {
          restoreSupabaseSession()
        } else {
          clearAccountAccessState()
        }
        return@launch
      }

      syncUserAccessInternal(
        accessToken = savedAccess,
        refreshToken = savedRefresh,
        userId = userId,
        isNewSignup = false,
        userCreatedAt = null,
        preserveExistingAccessWhileChecking = _subscriptionState.value.hasActiveAccess,
        clearPaymentError = false,
      )
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
        val result = withContext(Dispatchers.IO) { supabaseAuth.signUp(email, password) }

        if (
          result.success &&
            !result.requiresEmailConfirmation &&
            result.accessToken != null &&
            result.refreshToken != null
        ) {
          withContext(Dispatchers.IO) {
            supabaseSession.saveSession(result.accessToken, result.refreshToken, result.userId)
          }
          pendingSignupEmail = null
          _lastLicenseResult.value = null
          _lastTrialResult.value = null
          _paymentRequests.value = emptyList()
          _paymentSubmissionMessage.value = null
          _paymentErrorMessage.value = null
          _authUserId.value = result.userId
          _isAuthenticated.value = true
          recomputeSubscriptionState(isChecking = true)
          if (!result.userId.isNullOrBlank()) {
            syncUserAccessInternal(
              accessToken = result.accessToken,
              refreshToken = result.refreshToken,
              userId = result.userId,
              isNewSignup = true,
              userCreatedAt = result.userCreatedAt,
              preserveExistingAccessWhileChecking = false,
            )
          } else {
            clearAccountAccessState()
          }
        } else if (result.requiresEmailConfirmation) {
          pendingSignupEmail = email.trim().lowercase()
          clearAccountAccessState()
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
        val result = withContext(Dispatchers.IO) { supabaseAuth.login(email, password) }

        if (result.success && result.accessToken != null && result.refreshToken != null) {
          val normalizedEmail = email.trim().lowercase()
          val matchesPendingSignup = pendingSignupEmail == normalizedEmail
          pendingSignupEmail = null
          withContext(Dispatchers.IO) {
            supabaseSession.saveSession(result.accessToken, result.refreshToken, result.userId)
          }
          _lastLicenseResult.value = null
          _lastTrialResult.value = null
          _paymentRequests.value = emptyList()
          _paymentSubmissionMessage.value = null
          _paymentErrorMessage.value = null
          _authUserId.value = result.userId
          _isAuthenticated.value = true
          recomputeSubscriptionState(isChecking = true)
          if (!result.userId.isNullOrBlank()) {
            syncUserAccessInternal(
              accessToken = result.accessToken,
              refreshToken = result.refreshToken,
              userId = result.userId,
              isNewSignup = matchesPendingSignup,
              userCreatedAt = result.userCreatedAt,
              preserveExistingAccessWhileChecking = false,
            )
          } else {
            clearAccountAccessState()
          }
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
    viewModelScope.launch {
      _isAuthLoading.value = true
      try {
        withContext(Dispatchers.IO) {
          val accessToken = supabaseSession.getAccessToken()
          val refreshToken = supabaseSession.getRefreshToken()
          if (!accessToken.isNullOrBlank()) {
            val loggedOut = supabaseAuth.logout(accessToken)
            if (!loggedOut && !refreshToken.isNullOrBlank()) {
              val refreshed = supabaseAuth.refreshSession(refreshToken)
              if (refreshed.success && !refreshed.accessToken.isNullOrBlank()) {
                supabaseAuth.logout(refreshed.accessToken)
              }
            }
          }
        }
      } catch (_: Exception) {
        // Proceed to clear local session even when offline or logout request fails
      } finally {
        withContext(Dispatchers.IO) { supabaseSession.clearSession() }
        pendingSignupEmail = null
        clearAccountAccessState()
        _authMessage.value = "Logged out successfully"
        _isAuthLoading.value = false
      }
    }
  }

  fun clearAuthMessage() {
    _authMessage.value = null
  }

  fun clearPaymentFeedback() {
    _paymentSubmissionMessage.value = null
    _paymentErrorMessage.value = null
  }

  fun refreshLicenseStatus() {
    if (_isPaymentStatusLoading.value || _licenseState.value.isChecking || _isAuthLoading.value) return
    viewModelScope.launch {
      val (accessToken, refreshToken, savedUserId) =
        withContext(Dispatchers.IO) {
          Triple(
            supabaseSession.getAccessToken(),
            supabaseSession.getRefreshToken(),
            supabaseSession.getUserId(),
          )
        }
      val userId = (_authUserId.value ?: savedUserId).orEmpty()
      if (accessToken.isNullOrBlank() || userId.isBlank()) {
        if (!refreshToken.isNullOrBlank()) {
          restoreSupabaseSession()
        } else {
          clearAccountAccessState()
        }
        return@launch
      }
      syncUserAccessInternal(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        isNewSignup = false,
        userCreatedAt = null,
        preserveExistingAccessWhileChecking = false,
      )
    }
  }

  fun fetchPaymentRequests() {
    refreshLicenseStatus()
  }

  private suspend fun syncUserAccessInternal(
    accessToken: String,
    refreshToken: String?,
    userId: String,
    isNewSignup: Boolean,
    userCreatedAt: String?,
    preserveExistingAccessWhileChecking: Boolean,
    clearPaymentError: Boolean = true,
  ) {
    if (!preserveExistingAccessWhileChecking) {
      recomputeSubscriptionState(isChecking = true)
    } else {
      _licenseState.value = _licenseState.value.copy(isChecking = true, isNetworkError = false)
    }

    var effectiveAccess = accessToken
    var effectiveRefresh = refreshToken
    var effectiveUserId = userId

    var licenseResult =
      withContext(Dispatchers.IO) {
        supabaseLicenseService.verifySubscriptionLicense(
          accessToken = effectiveAccess,
          userId = effectiveUserId,
        )
      }

    if (licenseResult.isUnauthorized && !effectiveRefresh.isNullOrBlank()) {
      val refreshed = withContext(Dispatchers.IO) { supabaseAuth.refreshSession(effectiveRefresh) }
      if (refreshed.success && !refreshed.accessToken.isNullOrBlank()) {
        val nextRefresh = refreshed.refreshToken ?: effectiveRefresh
        val nextUserId = refreshed.userId ?: effectiveUserId
        withContext(Dispatchers.IO) {
          supabaseSession.saveSession(refreshed.accessToken, nextRefresh, nextUserId)
        }
        effectiveAccess = refreshed.accessToken
        effectiveRefresh = nextRefresh
        effectiveUserId = nextUserId
        _authUserId.value = nextUserId
        _isAuthenticated.value = true
        licenseResult =
          withContext(Dispatchers.IO) {
            supabaseLicenseService.verifySubscriptionLicense(
              accessToken = effectiveAccess,
              userId = effectiveUserId,
            )
          }
      } else if (refreshed.isNetworkError) {
        _lastLicenseResult.value =
          LicenseVerificationResult(
            hasActiveLicense = false,
            status = LicenseStatus.ERROR,
            userId = effectiveUserId,
            message = "Network error while verifying subscription.",
            isNetworkError = true,
          )
        recomputeSubscriptionState(isChecking = false)
        return
      } else {
        withContext(Dispatchers.IO) { supabaseSession.clearSession() }
        clearAccountAccessState()
        return
      }
    }

    if (licenseResult.isUnauthorized) {
      withContext(Dispatchers.IO) { supabaseSession.clearSession() }
      clearAccountAccessState()
      return
    }

    _lastLicenseResult.value = licenseResult

    val trialResult =
      withContext(Dispatchers.IO) {
        supabaseTrialService.fetchOrActivateTrial(
          accessToken = effectiveAccess,
          userId = effectiveUserId,
          isNewSignup = isNewSignup,
          userCreatedAt = userCreatedAt,
        )
      }
    if (trialResult.success || trialResult.hasTrial) {
      _lastTrialResult.value = trialResult
    }

    fetchPaymentRequestsInternal(
      accessToken = effectiveAccess,
      refreshToken = effectiveRefresh,
      userId = effectiveUserId,
      clearExistingError = clearPaymentError,
    )

    recomputeSubscriptionState(isChecking = false)
  }

  private suspend fun fetchPaymentRequestsInternal(
    accessToken: String,
    refreshToken: String?,
    userId: String,
    clearExistingError: Boolean = true,
  ) {
    if (_isPaymentStatusLoading.value) return
    _isPaymentStatusLoading.value = true
    if (clearExistingError) {
      _paymentErrorMessage.value = null
    }

    try {
      var result =
        withContext(Dispatchers.IO) {
          supabasePaymentService.fetchUserPaymentRequests(accessToken, userId)
        }

      if (result.isUnauthorized && !refreshToken.isNullOrBlank()) {
        val refreshed = withContext(Dispatchers.IO) { supabaseAuth.refreshSession(refreshToken) }
        if (refreshed.success && !refreshed.accessToken.isNullOrBlank()) {
          val nextRefresh = refreshed.refreshToken ?: refreshToken
          val nextUserId = refreshed.userId ?: userId
          withContext(Dispatchers.IO) {
            supabaseSession.saveSession(refreshed.accessToken, nextRefresh, nextUserId)
          }
          _authUserId.value = nextUserId
          result =
            withContext(Dispatchers.IO) {
              supabasePaymentService.fetchUserPaymentRequests(refreshed.accessToken, nextUserId)
            }
        }
      }

      if (result.success) {
        _paymentRequests.value = result.requests
        if (result.requests.none { it.status == PaymentRequestStatus.PENDING }) {
          _paymentSubmissionMessage.value = null
        }
        recomputeSubscriptionState(isChecking = false)
      } else if (!result.message.isNullOrBlank()) {
        _paymentErrorMessage.value = result.message
      }
    } catch (e: Exception) {
      _paymentErrorMessage.value = "Failed to load payment status: ${e.message ?: "Unknown error"}"
    } finally {
      _isPaymentStatusLoading.value = false
    }
  }

  fun submitLifetimeLicensePayment(utrInput: String, customerNote: String) {
    submitSubscriptionPayment(
      plan = _selectedSubscriptionPlan.value,
      utrInput = utrInput,
      customerNote = customerNote,
    )
  }

  fun submitSubscriptionPayment(
    plan: SubscriptionPlan,
    utrInput: String,
    customerNote: String,
  ) {
    // Duplicate-submit protection while request is already in progress
    if (_isPaymentSubmitting.value) return

    val targetPlan = if (plan.isPaidPlan) plan else SubscriptionPlan.MONTHLY
    _selectedSubscriptionPlan.value = targetPlan

    _paymentErrorMessage.value = null
    _paymentSubmissionMessage.value = null

    val utrError = SupabasePaymentService.validateUtr(utrInput)
    if (utrError != null) {
      _paymentErrorMessage.value = utrError
      return
    }

    val cleanUtr = SupabasePaymentService.sanitizeUtr(utrInput)
    val currentSub = _subscriptionState.value
    if (currentSub.status == EntitlementStatus.LIFETIME_ACTIVE) {
      _paymentErrorMessage.value = "Your Lifetime License is already active."
      return
    }
    if (currentSub.hasActivePaidPlan && currentSub.plan == targetPlan) {
      _paymentErrorMessage.value = "Your ${targetPlan.title} is already active."
      return
    }
    val existing = _paymentRequests.value
    if (existing.any { it.status == PaymentRequestStatus.PENDING }) {
      _paymentErrorMessage.value =
        "A payment request is already pending verification. Duplicate submissions are disabled."
      return
    }
    if (existing.any { it.utr.equals(cleanUtr, ignoreCase = true) }) {
      _paymentErrorMessage.value = "This UTR has already been submitted."
      return
    }

    _isPaymentSubmitting.value = true
    viewModelScope.launch {
      try {
        val (savedAccess, savedRefresh, savedUserId) =
          withContext(Dispatchers.IO) {
            Triple(
              supabaseSession.getAccessToken(),
              supabaseSession.getRefreshToken(),
              supabaseSession.getUserId(),
            )
          }

        var effectiveAccess = savedAccess
        var effectiveRefresh = savedRefresh
        var effectiveUserId = (_authUserId.value ?: savedUserId).orEmpty().trim()

        // Recover session via refresh token if access token or userId was lost/unavailable
        if ((effectiveAccess.isNullOrBlank() || effectiveUserId.isBlank()) && !effectiveRefresh.isNullOrBlank()) {
          val refreshed = withContext(Dispatchers.IO) { supabaseAuth.refreshSession(effectiveRefresh) }
          if (refreshed.success && !refreshed.accessToken.isNullOrBlank() && !refreshed.userId.isNullOrBlank()) {
            val nextRefresh = refreshed.refreshToken ?: effectiveRefresh
            withContext(Dispatchers.IO) {
              supabaseSession.saveSession(refreshed.accessToken, nextRefresh, refreshed.userId)
            }
            effectiveAccess = refreshed.accessToken
            effectiveRefresh = nextRefresh
            effectiveUserId = refreshed.userId
            _authUserId.value = refreshed.userId
            _isAuthenticated.value = true
          } else if (refreshed.isNetworkError) {
            _paymentErrorMessage.value =
              "Network error while refreshing session (${refreshed.message}). Please try again."
            return@launch
          }
        }

        if (!_isAuthenticated.value || effectiveAccess.isNullOrBlank() || effectiveUserId.isBlank()) {
          if (effectiveAccess.isNullOrBlank() && effectiveRefresh.isNullOrBlank()) {
            clearAccountAccessState()
          }
          _paymentErrorMessage.value = "Please login to your account before submitting payment."
          return@launch
        }

        var result =
          withContext(Dispatchers.IO) {
            supabasePaymentService.submitPaymentRequest(
              accessToken = effectiveAccess,
              userId = effectiveUserId,
              utrInput = cleanUtr,
              customerNote = customerNote,
              plan = targetPlan,
            )
          }

        if (result.isUnauthorized) {
          if (!effectiveRefresh.isNullOrBlank()) {
            val refreshed = withContext(Dispatchers.IO) { supabaseAuth.refreshSession(effectiveRefresh) }
            if (refreshed.success && !refreshed.accessToken.isNullOrBlank()) {
              val nextRefresh = refreshed.refreshToken ?: effectiveRefresh
              val nextUserId = (refreshed.userId ?: effectiveUserId).trim()
              withContext(Dispatchers.IO) {
                supabaseSession.saveSession(refreshed.accessToken, nextRefresh, nextUserId)
              }
              effectiveAccess = refreshed.accessToken
              effectiveRefresh = nextRefresh
              effectiveUserId = nextUserId
              _authUserId.value = nextUserId
              _isAuthenticated.value = true
              result =
                withContext(Dispatchers.IO) {
                  supabasePaymentService.submitPaymentRequest(
                    accessToken = refreshed.accessToken,
                    userId = nextUserId,
                    utrInput = cleanUtr,
                    customerNote = customerNote,
                    plan = targetPlan,
                  )
                }
            } else if (refreshed.isNetworkError) {
              _paymentErrorMessage.value =
                "Network error while refreshing expired session: ${refreshed.message}"
              return@launch
            } else {
              withContext(Dispatchers.IO) { supabaseSession.clearSession() }
              clearAccountAccessState()
              _paymentErrorMessage.value =
                "Your session expired (${refreshed.message}). Please login again."
              return@launch
            }
          } else {
            withContext(Dispatchers.IO) { supabaseSession.clearSession() }
            clearAccountAccessState()
            _paymentErrorMessage.value = result.message
            return@launch
          }
        }

        val confirmedRecord = result.request
        if (result.success && confirmedRecord != null) {
          _paymentRequests.value =
            listOf(confirmedRecord) + _paymentRequests.value.filterNot { it.utr == confirmedRecord.utr }
          _paymentSubmissionMessage.value = "Payment Under Verification"
          recomputeSubscriptionState(isChecking = false)
          val latestAccess = withContext(Dispatchers.IO) { supabaseSession.getAccessToken() } ?: effectiveAccess
          if (!latestAccess.isNullOrBlank() && effectiveUserId.isNotBlank()) {
            val latest =
              withContext(Dispatchers.IO) {
                supabasePaymentService.fetchUserPaymentRequests(latestAccess, effectiveUserId)
              }
            if (latest.success) {
              _paymentRequests.value = latest.requests
              recomputeSubscriptionState(isChecking = false)
            }
          }
        } else {
          _paymentErrorMessage.value =
            result.message.ifBlank {
              "Payment submission was not confirmed by payment server. Please try again."
            }
        }
      } catch (e: Exception) {
        _paymentErrorMessage.value = "Submission failed: ${e.message ?: "Unknown error"}"
      } finally {
        _isPaymentSubmitting.value = false
      }
    }
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
