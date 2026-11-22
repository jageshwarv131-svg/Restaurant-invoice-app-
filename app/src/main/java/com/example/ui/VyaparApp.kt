package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.AppSafeInsets
import com.example.ui.components.LocalAppSafeInsets
import com.example.ui.components.VyaparTopBar
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BillsHistoryScreen
import com.example.ui.screens.BusinessSetupScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoicePreviewScreen
import com.example.ui.screens.InvoiceSettingsScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.PosBillingScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.VyaparBillingTheme
import com.example.util.CurrencyUtils
import java.io.File
import kotlinx.coroutines.launch

enum class BottomTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
  BILLS("Bills", Icons.AutoMirrored.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
  PRODUCTS("Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
  REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
  MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz),
}

enum class SubScreen {
  NONE,
  POS_BILLING,
  INVOICE_PREVIEW,
  CUSTOMERS,
  ORDERS,
  EXPENSES,
  INVENTORY,
  BUSINESS_PROFILE,
  INVOICE_SETTINGS,
  BACKUP_RESTORE,
  HELP,
}

@Composable
fun VyaparBillingApp(vm: VyaparViewModel = viewModel()) {
  val isLoaded by vm.isLoaded.collectAsStateWithLifecycle()
  val business by vm.business.collectAsStateWithLifecycle()
  val products by vm.products.collectAsStateWithLifecycle()
  val customers by vm.customers.collectAsStateWithLifecycle()
  val invoices by vm.invoices.collectAsStateWithLifecycle()
  val orders by vm.orders.collectAsStateWithLifecycle()
  val expenses by vm.expenses.collectAsStateWithLifecycle()
  val movements by vm.inventoryMovements.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  val metrics by vm.dashboardMetrics.collectAsStateWithLifecycle()
  val cart by vm.cart.collectAsStateWithLifecycle()
  val isShopOpen = settings.isShopOpen
  val isUnlocked by vm.isUnlocked.collectAsStateWithLifecycle()
  val activeInvoice by vm.activePreviewInvoice.collectAsStateWithLifecycle()
  val activePdf by vm.activePreviewPdf.collectAsStateWithLifecycle()
  val toastMessage by vm.toastMessage.collectAsStateWithLifecycle()

  var showSplash by remember { mutableStateOf(true) }
  var currentTab by remember { mutableStateOf(BottomTab.HOME) }
  val backStack = remember { mutableStateListOf<SubScreen>() }
  val activeSubScreen = backStack.lastOrNull() ?: SubScreen.NONE

  var openAddProductDialog by remember { mutableStateOf(false) }
  var openAddCustomerDialog by remember { mutableStateOf(false) }
  var showAlertsDialog by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(toastMessage) {
    toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      vm.clearToast()
    }
  }

  fun pushSubScreen(screen: SubScreen) {
    if (backStack.lastOrNull() != screen) backStack.add(screen)
  }

  fun popSubScreen() {
    if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
  }

  VyaparBillingTheme(darkTheme = settings.darkMode) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(
      LocalAppSafeInsets provides AppSafeInsets(
        statusTop = statusTop,
        navBottom = navBottom,
        imeBottom = imeBottom,
      )
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
      when {
        showSplash || !isLoaded -> {
          SplashScreen(onSplashFinished = { showSplash = false })
        }
        settings.appPinEnabled && !isUnlocked -> {
          PinLockScreen(onVerifyPin = { vm.unlockWithPin(it) })
        }
        !settings.onboardingCompleted -> {
          OnboardingScreen(onFinish = { vm.completeOnboarding() })
        }
        !settings.businessSetupCompleted -> {
          BusinessSetupScreen(
            existingProfile = business,
            canNavigateBack = false,
            onBack = {},
            onSave = { profile, uri -> vm.saveBusinessProfile(profile, uri) },
          )
        }
        else -> {
          val drawerState = rememberDrawerState(DrawerValue.Closed)
          val scope = rememberCoroutineScope()
          val sym = business.currencySymbol.ifBlank { "₹" }

          ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = activeSubScreen == SubScreen.NONE,
            drawerContent = {
              ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                Column(
                  modifier =
                    Modifier.fillMaxHeight()
                      .navigationBarsPadding()
                      .verticalScroll(rememberScrollState())
                ) {
                  // Charcoal Brand Header
                  Surface(color = CharcoalBlack, contentColor = Color.White) {
                    Column(
                      modifier =
                        Modifier.fillMaxWidth()
                          .statusBarsPadding()
                          .padding(20.dp)
                    ) {
                      Surface(
                        shape = CircleShape,
                        color = Color(0xFF151517),
                        modifier = Modifier.size(62.dp),
                      ) {
                        if (!business.logoPath.isNullOrBlank() && File(business.logoPath!!).exists()) {
                          AsyncImage(
                            model = File(business.logoPath!!),
                            contentDescription = "Shop Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                          )
                        } else {
                          Image(
                            painter = painterResource(id = R.drawable.img_vyapar_logo),
                            contentDescription = "Vyapar Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                          )
                        }
                      }
                      Spacer(Modifier.height(10.dp))
                      Text(
                        text = business.businessName.ifBlank { "Vyapar Billing Pro" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                      )
                      Text(
                        text = business.category.ifBlank { "Offline POS & GST Billing" },
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronOrange,
                      )
                    }
                  }

                  Spacer(Modifier.height(8.dp))

                  fun navFromDrawer(tab: BottomTab? = null, sub: SubScreen? = null) {
                    scope.launch { drawerState.close() }
                    backStack.clear()
                    if (tab != null) currentTab = tab
                    if (sub != null) pushSubScreen(sub)
                  }

                  NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.HOME,
                    onClick = { navFromDrawer(tab = BottomTab.HOME) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("New Bill (POS)") },
                    selected = activeSubScreen == SubScreen.POS_BILLING,
                    onClick = { navFromDrawer(sub = SubScreen.POS_BILLING) },
                    icon = { Icon(Icons.Filled.AddShoppingCart, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Invoices / Bills") },
                    selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.BILLS,
                    onClick = { navFromDrawer(tab = BottomTab.BILLS) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Products") },
                    selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.PRODUCTS,
                    onClick = { navFromDrawer(tab = BottomTab.PRODUCTS) },
                    icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Customers") },
                    selected = activeSubScreen == SubScreen.CUSTOMERS,
                    onClick = { navFromDrawer(sub = SubScreen.CUSTOMERS) },
                    icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Orders") },
                    selected = activeSubScreen == SubScreen.ORDERS,
                    onClick = { navFromDrawer(sub = SubScreen.ORDERS) },
                    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Inventory & Stock") },
                    selected = activeSubScreen == SubScreen.INVENTORY,
                    onClick = { navFromDrawer(sub = SubScreen.INVENTORY) },
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Expenses") },
                    selected = activeSubScreen == SubScreen.EXPENSES,
                    onClick = { navFromDrawer(sub = SubScreen.EXPENSES) },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Reports") },
                    selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.REPORTS,
                    onClick = { navFromDrawer(tab = BottomTab.REPORTS) },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
                  )
                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                  NavigationDrawerItem(
                    label = { Text("Business Profile") },
                    selected = activeSubScreen == SubScreen.BUSINESS_PROFILE,
                    onClick = { navFromDrawer(sub = SubScreen.BUSINESS_PROFILE) },
                    icon = { Icon(Icons.Filled.Business, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Invoice & GST Settings") },
                    selected = activeSubScreen == SubScreen.INVOICE_SETTINGS,
                    onClick = { navFromDrawer(sub = SubScreen.INVOICE_SETTINGS) },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Backup & Restore") },
                    selected = activeSubScreen == SubScreen.BACKUP_RESTORE,
                    onClick = { navFromDrawer(sub = SubScreen.BACKUP_RESTORE) },
                    icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                  )
                  NavigationDrawerItem(
                    label = { Text("Help & Support") },
                    selected = activeSubScreen == SubScreen.HELP,
                    onClick = { navFromDrawer(sub = SubScreen.HELP) },
                    icon = { Icon(Icons.Filled.HelpOutline, contentDescription = null) },
                  )
                  Spacer(Modifier.height(16.dp))
                }
              }
            },
          ) {
            when (activeSubScreen) {
              SubScreen.POS_BILLING -> {
                PosBillingScreen(
                  cart = cart,
                  products = products,
                  customers = customers,
                  currencySymbol = sym,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onAddProduct = { vm.addProductToCart(it) },
                  onDecrementProduct = { vm.decrementProductInCart(it.id) },
                  onAddCustomItem = { name, qty, pricePaise, gst, unit ->
                    vm.addCustomItemToCart(name, qty, pricePaise, gst, unit)
                  },
                  onUpdateQty = { idx, delta -> vm.updateCartItemQty(idx, delta) },
                  onRemoveItem = { idx -> vm.removeCartItem(idx) },
                  onSelectCustomer = { vm.setCartCustomer(it) },
                  onUpdateCustomerDetails = { name, mobile, address, gstin, saveToDir ->
                    vm.updateCartCustomerDetails(name, mobile, address, gstin, saveToDir)
                  },
                  onQuickCreateCustomer = { name, mobile, onCreated ->
                    vm.upsertCustomer(
                      com.example.model.Customer(id = "", name = name, mobile = mobile),
                      onSaved = onCreated,
                    )
                  },
                  onSetDiscount = { input, isPct -> vm.setCartDiscount(input, isPct) },
                  onSetTaxConfig = { tt, tm -> vm.setCartTaxConfig(tt, tm) },
                  onSetPaymentMethod = { vm.setCartPaymentMethod(it) },
                  onSetAmountPaid = { vm.setCartAmountPaid(it) },
                  onClearCart = { vm.clearCart() },
                  onGenerateInvoice = {
                    vm.completeSaleAndOpenInvoice {
                      popSubScreen()
                      pushSubScreen(SubScreen.INVOICE_PREVIEW)
                    }
                  },
                )
              }
              SubScreen.INVOICE_PREVIEW -> {
                InvoicePreviewScreen(
                  invoice = activeInvoice,
                  pdfFile = activePdf,
                  initialFormat = settings.defaultPaperFormat,
                  initialLanguage = settings.language,
                  onRegeneratePdf = { pf, lang -> vm.regeneratePreviewPdf(pf, lang) },
                  onShowToast = { vm.showToast(it) },
                  onBack = { popSubScreen() },
                )
              }
              SubScreen.CUSTOMERS -> {
                CustomersScreen(
                  customers = customers,
                  invoices = invoices,
                  currencySymbol = sym,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  openAddImmediately = openAddCustomerDialog,
                  onConsumeOpenAdd = { openAddCustomerDialog = false },
                  onBack = { popSubScreen() },
                  onUpsertCustomer = { vm.upsertCustomer(it) },
                  onDeleteCustomer = { vm.deleteCustomer(it) },
                  onCollectPayment = { id, amt -> vm.collectCustomerPayment(id, amt) },
                  onOpenInvoicePreview = { inv ->
                    vm.openInvoicePreview(inv) { pushSubScreen(SubScreen.INVOICE_PREVIEW) }
                  },
                )
              }
              SubScreen.ORDERS -> {
                OrdersScreen(
                  orders = orders,
                  products = products,
                  customers = customers,
                  currencySymbol = sym,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onCreateOrder = { cust, items, notes -> vm.createOrder(cust, items, notes) },
                  onUpdateStatus = { id, st -> vm.updateOrderStatus(id, st) },
                  onConvertOrderToInvoice = { order ->
                    vm.convertOrderToInvoice(order) {
                      pushSubScreen(SubScreen.INVOICE_PREVIEW)
                    }
                  },
                )
              }
              SubScreen.EXPENSES -> {
                ExpensesScreen(
                  expenses = expenses,
                  currencySymbol = sym,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onUpsertExpense = { vm.upsertExpense(it) },
                  onDeleteExpense = { vm.deleteExpense(it) },
                )
              }
              SubScreen.INVENTORY -> {
                InventoryScreen(
                  products = products,
                  movements = movements,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onAdjustStock = { id, type, qty, reason -> vm.adjustStock(id, type, qty, reason) },
                )
              }
              SubScreen.BUSINESS_PROFILE -> {
                BusinessSetupScreen(
                  existingProfile = business,
                  canNavigateBack = true,
                  onBack = { popSubScreen() },
                  onSave = { profile, uri ->
                    vm.saveBusinessProfile(profile, uri) { popSubScreen() }
                  },
                )
              }
              SubScreen.INVOICE_SETTINGS -> {
                InvoiceSettingsScreen(
                  settings = settings,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onSaveInvoiceDefaults = { pf, tt, tm, round ->
                    vm.setDefaultPaperFormat(pf)
                    vm.updateDefaultGstSettings(tt, tm, round)
                  },
                )
              }
              SubScreen.BACKUP_RESTORE -> {
                BackupRestoreScreen(
                  lastBackupTimestamp = settings.lastBackupTimestamp,
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                  onBuildFullBackupJson = { vm.buildFullBackupJson() },
                  onExportSingleJson = { vm.exportSingleJson(it) },
                  onValidateBackup = { vm.validateBackupJson(it) },
                  onRestoreValidated = { vm.restoreFromValidatedBackup(it) },
                  onShowToast = { vm.showToast(it) },
                )
              }
              SubScreen.HELP -> {
                HelpSupportScreen(
                  isShopOpen = isShopOpen,
                  onToggleShopStatus = { vm.toggleShopOpen() },
                  onBack = { popSubScreen() },
                )
              }
              SubScreen.NONE -> {
                val topTitle =
                  when (currentTab) {
                    BottomTab.HOME -> "Dashboard"
                    BottomTab.BILLS -> "Bills & Invoices"
                    BottomTab.PRODUCTS -> "Products"
                    BottomTab.REPORTS -> "Reports"
                    BottomTab.MORE -> "Settings & More"
                  }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                  val isWide = maxWidth >= 700.dp
                  Row(modifier = Modifier.fillMaxSize()) {
                    if (isWide) {
                      NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Spacer(Modifier.height(16.dp))
                        BottomTab.entries.forEach { tab ->
                          val selected = currentTab == tab
                          NavigationRailItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                              Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                              )
                            },
                            label = { Text(tab.label, fontSize = 11.sp) },
                          )
                        }
                      }
                    }

                    Scaffold(
                      modifier = Modifier.weight(1f),
                      topBar = {
                        VyaparTopBar(
                          title = topTitle,
                          isShopOpen = isShopOpen,
                          onToggleShopStatus = { vm.toggleShopOpen() },
                          showBack = false,
                          onBackOrMenuClick = { scope.launch { drawerState.open() } },
                          onAlertClick = { showAlertsDialog = true },
                          alertCount = metrics.lowStockCount,
                        )
                      },
                      bottomBar = {
                        if (!isWide) {
                          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            BottomTab.entries.forEach { tab ->
                              val selected = currentTab == tab
                              NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                  Icon(
                                    imageVector =
                                      if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                  )
                                },
                                label = {
                                  Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight =
                                      if (selected) FontWeight.Bold else FontWeight.Medium,
                                  )
                                },
                                colors =
                                  NavigationBarItemDefaults.colors(
                                    selectedIconColor = SaffronOrange,
                                    selectedTextColor = SaffronOrange,
                                    indicatorColor = SaffronOrange.copy(alpha = 0.14f),
                                  ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                              )
                            }
                          }
                        }
                      },
                    ) { innerPadding ->
                      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentTab) {
                          BottomTab.HOME -> {
                            DashboardScreen(
                              business = business,
                              metrics = metrics,
                              invoices = invoices,
                              products = products,
                              onNewBill = { pushSubScreen(SubScreen.POS_BILLING) },
                              onAddProduct = {
                                openAddProductDialog = true
                                currentTab = BottomTab.PRODUCTS
                              },
                              onAddCustomer = {
                                openAddCustomerDialog = true
                                pushSubScreen(SubScreen.CUSTOMERS)
                              },
                              onOpenReports = { currentTab = BottomTab.REPORTS },
                              onOpenExpenses = { pushSubScreen(SubScreen.EXPENSES) },
                              onOpenInvoices = { currentTab = BottomTab.BILLS },
                              onOpenInventory = { pushSubScreen(SubScreen.INVENTORY) },
                              onOpenInvoicePreview = { inv ->
                                vm.openInvoicePreview(inv) {
                                  pushSubScreen(SubScreen.INVOICE_PREVIEW)
                                }
                              },
                              onOpenCustomers = { pushSubScreen(SubScreen.CUSTOMERS) },
                              onOpenProducts = { currentTab = BottomTab.PRODUCTS },
                            )
                          }
                          BottomTab.BILLS -> {
                            BillsHistoryScreen(
                              invoices = invoices,
                              currencySymbol = sym,
                              onNewBill = { pushSubScreen(SubScreen.POS_BILLING) },
                              onOpenInvoice = { inv ->
                                vm.openInvoicePreview(inv) {
                                  pushSubScreen(SubScreen.INVOICE_PREVIEW)
                                }
                              },
                              onDeleteInvoice = { vm.deleteInvoice(it) },
                            )
                          }
                          BottomTab.PRODUCTS -> {
                            ProductsScreen(
                              products = products,
                              cartItems = cart.items,
                              currencySymbol = sym,
                              openAddDialogImmediately = openAddProductDialog,
                              onConsumeOpenAdd = { openAddProductDialog = false },
                              onUpsertProduct = { prod, uri -> vm.upsertProduct(prod, uri) },
                              onDeleteProduct = { vm.deleteProduct(it) },
                              onIncrementProductInCart = { prod -> vm.addProductToCart(prod) },
                              onDecrementProductInCart = { prod ->
                                vm.decrementProductInCart(prod.id)
                              },
                              onProceedToBill = { pushSubScreen(SubScreen.POS_BILLING) },
                            )
                          }
                          BottomTab.REPORTS -> {
                            ReportsScreen(
                              business = business,
                              invoices = invoices,
                              products = products,
                              customers = customers,
                              expenses = expenses,
                              exportDir = vm.repository.invoicesDirectory,
                              onShowToast = { vm.showToast(it) },
                            )
                          }
                          BottomTab.MORE -> {
                            MoreScreen(
                              settings = settings,
                              onOpenCustomers = { pushSubScreen(SubScreen.CUSTOMERS) },
                              onOpenOrders = { pushSubScreen(SubScreen.ORDERS) },
                              onOpenExpenses = { pushSubScreen(SubScreen.EXPENSES) },
                              onOpenInventory = { pushSubScreen(SubScreen.INVENTORY) },
                              onOpenBusinessProfile = { pushSubScreen(SubScreen.BUSINESS_PROFILE) },
                              onOpenInvoiceSettings = { pushSubScreen(SubScreen.INVOICE_SETTINGS) },
                              onOpenBackupRestore = { pushSubScreen(SubScreen.BACKUP_RESTORE) },
                              onToggleDarkMode = { vm.toggleDarkMode(it) },
                              onSetLanguage = { vm.setLanguage(it) },
                              onUpdatePinSecurity = { en, pin -> vm.updatePinSecurity(en, pin) },
                              onOpenHelp = { pushSubScreen(SubScreen.HELP) },
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 84.dp),
      )

      if (showAlertsDialog) {
        val lowProducts = products.filter { it.stockQty <= it.lowStockThreshold }
        val dueCustomers = customers.filter { it.outstandingBalancePaise > 0 }
        AlertDialog(
          onDismissRequest = { showAlertsDialog = false },
          properties = DialogProperties(decorFitsSystemWindows = false),
          modifier = Modifier.statusBarsPadding().navigationBarsPadding().imePadding(),
          title = { Text("Business Alerts") },
          text = {
            Column(
              modifier = Modifier.verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                "Low Stock Products (${lowProducts.size})",
                fontWeight = FontWeight.Bold,
                color = SaffronOrange,
              )
              if (lowProducts.isEmpty()) {
                Text("All products are well-stocked.", style = MaterialTheme.typography.bodySmall)
              } else {
                lowProducts.take(4).forEach {
                  Text(
                    "• ${it.name}: ${CurrencyUtils.formatQty(it.stockQty)} ${it.unit} left",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
              Spacer(Modifier.height(4.dp))
              Text(
                "Outstanding Customer Dues (${dueCustomers.size})",
                fontWeight = FontWeight.Bold,
                color = SaffronOrange,
              )
              if (dueCustomers.isEmpty()) {
                Text("No pending customer credit dues.", style = MaterialTheme.typography.bodySmall)
              } else {
                dueCustomers.take(4).forEach {
                  Text(
                    "• ${it.name}: ${CurrencyUtils.formatPaise(it.outstandingBalancePaise, business.currencySymbol.ifBlank { "₹" })}",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          },
          confirmButton = {
            TextButton(
              onClick = {
                showAlertsDialog = false
                pushSubScreen(SubScreen.INVENTORY)
              }
            ) {
              Text("Open Inventory", color = SaffronOrange, fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(onClick = { showAlertsDialog = false }) { Text("Close") }
          },
        )
      }
      }
    }
  }
}
