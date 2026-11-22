package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.model.OrderStatus
import com.example.model.ThemePreference
import com.example.ui.components.AppSafeInsets
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.LocalAppSafeInsets
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
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
import com.example.ui.theme.LocalVyaparClayPalette
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

private enum class AppStage {
  SPLASH,
  PIN_LOCK,
  ONBOARDING,
  BUSINESS_SETUP,
  MAIN,
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
  var openNewOrderDialog by remember { mutableStateOf(false) }
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

  val systemDark = isSystemInDarkTheme()
  val isDarkTheme =
    when (settings.themePreference) {
      ThemePreference.SYSTEM -> systemDark
      ThemePreference.LIGHT -> false
      ThemePreference.DARK -> true
    }

  VyaparBillingTheme(
    darkTheme = isDarkTheme,
    lightStatusBar = !isDarkTheme,
  ) {
    val palette = LocalVyaparClayPalette.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val stage =
      when {
        showSplash || !isLoaded -> AppStage.SPLASH
        settings.appPinEnabled && !isUnlocked -> AppStage.PIN_LOCK
        !settings.onboardingCompleted -> AppStage.ONBOARDING
        !settings.businessSetupCompleted -> AppStage.BUSINESS_SETUP
        else -> AppStage.MAIN
      }

    CompositionLocalProvider(
      LocalAppSafeInsets provides AppSafeInsets(
        statusTop = statusTop,
        navBottom = navBottom,
        imeBottom = imeBottom,
      )
    ) {
      Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Crossfade(
          targetState = stage,
          animationSpec = tween(durationMillis = 320),
          label = "app_stage_crossfade",
        ) { currentStage ->
          when (currentStage) {
            AppStage.SPLASH -> {
              SplashScreen(
                isDataLoaded = isLoaded,
                onSplashFinished = { showSplash = false },
              )
            }
            AppStage.PIN_LOCK -> {
              PinLockScreen(onVerifyPin = { vm.unlockWithPin(it) })
            }
            AppStage.ONBOARDING -> {
              OnboardingScreen(onFinish = { vm.completeOnboarding() })
            }
            AppStage.BUSINESS_SETUP -> {
              BusinessSetupScreen(
                existingProfile = business,
                canNavigateBack = false,
                onBack = {},
                onSave = { profile, uri -> vm.saveBusinessProfile(profile, uri) },
              )
            }
            AppStage.MAIN -> {
              val drawerState = rememberDrawerState(DrawerValue.Closed)
              val scope = rememberCoroutineScope()
              val sym = business.currencySymbol.ifBlank { "₹" }

              ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = activeSubScreen == SubScreen.NONE,
                drawerContent = {
                  ModalDrawerSheet(
                    drawerContainerColor = palette.background,
                    modifier = Modifier.width(300.dp),
                  ) {
                    Column(
                      modifier =
                        Modifier.fillMaxHeight()
                          .background(palette.background)
                          .statusBarsPadding()
                          .navigationBarsPadding()
                          .verticalScroll(rememberScrollState())
                          .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                      // 3D Glossy Orange Brand Header Card
                      Card3D(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 26.dp,
                        tone = ClayTones.HeroOrange,
                        shadowDepth = 6.dp,
                        contentPadding = PaddingValues(18.dp),
                      ) {
                        Box(
                          modifier =
                            Modifier.size(64.dp)
                              .clip(CircleShape)
                              .background(Color.White.copy(alpha = 0.22f))
                              .padding(4.dp)
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
                        Spacer(Modifier.height(12.dp))
                        Text(
                          text = business.businessName.ifBlank { "Vyapar Billing Pro" },
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = Color.White,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = business.category.ifBlank { "Offline POS & GST Billing" },
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          color = Color.White.copy(alpha = 0.88f),
                        )
                      }

                      Spacer(Modifier.height(14.dp))

                      fun navFromDrawer(tab: BottomTab? = null, sub: SubScreen? = null) {
                        scope.launch { drawerState.close() }
                        backStack.clear()
                        if (tab != null) currentTab = tab
                        if (sub != null) pushSubScreen(sub)
                      }

                      DrawerItem3D(
                        label = "Dashboard",
                        icon = Icons.Filled.Home,
                        tone = ClayTones.Orange,
                        selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.HOME,
                        onClick = { navFromDrawer(tab = BottomTab.HOME) },
                      )
                      DrawerItem3D(
                        label = "New Bill (POS)",
                        icon = Icons.Filled.AddShoppingCart,
                        tone = ClayTones.Blue,
                        selected = activeSubScreen == SubScreen.POS_BILLING,
                        onClick = { navFromDrawer(sub = SubScreen.POS_BILLING) },
                      )
                      DrawerItem3D(
                        label = "Invoices / Bills",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        tone = ClayTones.Orange,
                        selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.BILLS,
                        onClick = { navFromDrawer(tab = BottomTab.BILLS) },
                      )
                      DrawerItem3D(
                        label = "Products",
                        icon = Icons.Filled.Inventory2,
                        tone = ClayTones.Amber,
                        selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.PRODUCTS,
                        onClick = { navFromDrawer(tab = BottomTab.PRODUCTS) },
                      )
                      DrawerItem3D(
                        label = "Customers",
                        icon = Icons.Filled.Group,
                        tone = ClayTones.Green,
                        selected = activeSubScreen == SubScreen.CUSTOMERS,
                        onClick = { navFromDrawer(sub = SubScreen.CUSTOMERS) },
                      )
                      DrawerItem3D(
                        label = "Pending Orders",
                        icon = Icons.Filled.LocalShipping,
                        tone = ClayTones.Orange,
                        selected = activeSubScreen == SubScreen.ORDERS,
                        onClick = { navFromDrawer(sub = SubScreen.ORDERS) },
                      )
                      DrawerItem3D(
                        label = "Inventory & Stock",
                        icon = Icons.Filled.Inventory,
                        tone = ClayTones.Amber,
                        selected = activeSubScreen == SubScreen.INVENTORY,
                        onClick = { navFromDrawer(sub = SubScreen.INVENTORY) },
                      )
                      DrawerItem3D(
                        label = "Expenses",
                        icon = Icons.Filled.AccountBalanceWallet,
                        tone = ClayTones.Purple,
                        selected = activeSubScreen == SubScreen.EXPENSES,
                        onClick = { navFromDrawer(sub = SubScreen.EXPENSES) },
                      )
                      DrawerItem3D(
                        label = "Reports",
                        icon = Icons.Filled.Assessment,
                        tone = ClayTones.Green,
                        selected = activeSubScreen == SubScreen.NONE && currentTab == BottomTab.REPORTS,
                        onClick = { navFromDrawer(tab = BottomTab.REPORTS) },
                      )

                      HorizontalDivider(
                        color = palette.glossBorderBottom.copy(alpha = 0.45f),
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                      )

                      DrawerItem3D(
                        label = "Business Profile",
                        icon = Icons.Filled.Business,
                        tone = ClayTones.Blue,
                        selected = activeSubScreen == SubScreen.BUSINESS_PROFILE,
                        onClick = { navFromDrawer(sub = SubScreen.BUSINESS_PROFILE) },
                      )
                      DrawerItem3D(
                        label = "Invoice & GST Settings",
                        icon = Icons.Filled.Receipt,
                        tone = ClayTones.Orange,
                        selected = activeSubScreen == SubScreen.INVOICE_SETTINGS,
                        onClick = { navFromDrawer(sub = SubScreen.INVOICE_SETTINGS) },
                      )
                      DrawerItem3D(
                        label = "Backup & Restore",
                        icon = Icons.Filled.Backup,
                        tone = ClayTones.Purple,
                        selected = activeSubScreen == SubScreen.BACKUP_RESTORE,
                        onClick = { navFromDrawer(sub = SubScreen.BACKUP_RESTORE) },
                      )
                      DrawerItem3D(
                        label = "Help & Support",
                        icon = Icons.Filled.HelpOutline,
                        tone = ClayTones.Blue,
                        selected = activeSubScreen == SubScreen.HELP,
                        onClick = { navFromDrawer(sub = SubScreen.HELP) },
                      )
                      Spacer(Modifier.height(14.dp))
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
                      invoices = invoices,
                      defaultTaxType = settings.defaultTaxType,
                      defaultTaxMode = settings.defaultTaxMode,
                      autoRoundOff = settings.autoRoundOff,
                      currencySymbol = sym,
                      isShopOpen = isShopOpen,
                      openNewOrderImmediately = openNewOrderDialog,
                      onConsumeOpenNewOrder = { openNewOrderDialog = false },
                      onToggleShopStatus = { vm.toggleShopOpen() },
                      onBack = { popSubScreen() },
                      onCreateInlineCustomer = { name, mobile, onSaved ->
                        vm.createInlineCustomer(name, mobile, onSaved)
                      },
                      onCreateOrder = { cust, name, mobile, items, notes ->
                        vm.createOrder(cust, name, mobile, items, notes)
                      },
                      onUpdatePendingOrder = { orderId, cust, name, mobile, items, discountVal, isPct, notes ->
                        vm.updatePendingOrder(
                          orderId = orderId,
                          customer = cust,
                          customerName = name,
                          customerMobile = mobile,
                          items = items,
                          discountValue = discountVal,
                          isPercentageDiscount = isPct,
                          notes = notes,
                        )
                      },
                      onCancelOrder = { id -> vm.updateOrderStatus(id, OrderStatus.CANCELLED) },
                      onConfirmOrder = { order, finalItems, discountVal, isPct, taxType, taxMode, method, paidPaise, notes ->
                        vm.confirmOrderAndOpenInvoice(
                          order = order,
                          finalItems = finalItems,
                          discountValue = discountVal,
                          isPercentageDiscount = isPct,
                          taxType = taxType,
                          taxMode = taxMode,
                          paymentMethod = method,
                          amountPaidPaise = paidPaise,
                          notes = notes,
                        ) {
                          pushSubScreen(SubScreen.INVOICE_PREVIEW)
                        }
                      },
                      onOpenInvoicePreview = { inv ->
                        vm.openInvoicePreview(inv) { pushSubScreen(SubScreen.INVOICE_PREVIEW) }
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

                    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(palette.background)) {
                      val isWide = maxWidth >= 700.dp
                      Row(modifier = Modifier.fillMaxSize()) {
                        if (isWide) {
                          ClayNavigationRail(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it },
                          )
                        }

                        Scaffold(
                          modifier = Modifier.weight(1f),
                          containerColor = palette.background,
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
                              ClayBottomNavigationBar(
                                currentTab = currentTab,
                                onSelectTab = { currentTab = it },
                              )
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
                                  onOpenOrders = { pushSubScreen(SubScreen.ORDERS) },
                                )
                              }
                              BottomTab.BILLS -> {
                                BillsHistoryScreen(
                                  invoices = invoices,
                                  pendingOrdersCount = metrics.pendingOrdersCount,
                                  currencySymbol = sym,
                                  onNewBill = { pushSubScreen(SubScreen.POS_BILLING) },
                                  onNewOrder = {
                                    openNewOrderDialog = true
                                    pushSubScreen(SubScreen.ORDERS)
                                  },
                                  onOpenOrders = { pushSubScreen(SubScreen.ORDERS) },
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
                                  isDarkActive = isDarkTheme,
                                  onOpenCustomers = { pushSubScreen(SubScreen.CUSTOMERS) },
                                  onOpenOrders = { pushSubScreen(SubScreen.ORDERS) },
                                  onOpenExpenses = { pushSubScreen(SubScreen.EXPENSES) },
                                  onOpenInventory = { pushSubScreen(SubScreen.INVENTORY) },
                                  onOpenBusinessProfile = { pushSubScreen(SubScreen.BUSINESS_PROFILE) },
                                  onOpenInvoiceSettings = { pushSubScreen(SubScreen.INVOICE_SETTINGS) },
                                  onOpenBackupRestore = { pushSubScreen(SubScreen.BACKUP_RESTORE) },
                                  onSetThemePreference = { vm.setThemePreference(it) },
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
        }

        // 3D Raised Toast / Snackbar Overlay
        SnackbarHost(
          hostState = snackbarHostState,
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .imePadding()
              .padding(horizontal = 16.dp, vertical = 92.dp),
        ) { data ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .raised3D(cornerRadius = 20.dp, tone = ClayTones.HeroOrange, shadowDepth = 6.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Notifications,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
              text = data.visuals.message,
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (showAlertsDialog) {
          val lowProducts = products.filter { it.stockQty <= it.lowStockThreshold }
          val dueCustomers = customers.filter { it.outstandingBalancePaise > 0 }
          VyaparFormDialog(
            title = "Business Alerts",
            onDismiss = { showAlertsDialog = false },
            scrollContent = {
              Card3D(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                shadowDepth = 3.dp,
                contentPadding = PaddingValues(14.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ClayIconPad(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tone = ClayTones.Amber,
                    padSize = 38.dp,
                    iconSize = 20.dp,
                    cornerRadius = 12.dp,
                  )
                  Spacer(Modifier.width(10.dp))
                  Text(
                    "Low Stock Products (${lowProducts.size})",
                    fontWeight = FontWeight.ExtraBold,
                    color = SaffronOrange,
                  )
                }
                Spacer(Modifier.height(8.dp))
                if (lowProducts.isEmpty()) {
                  Text(
                    "All products are well-stocked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                } else {
                  lowProducts.take(4).forEach {
                    Text(
                      "• ${it.name}: ${CurrencyUtils.formatQty(it.stockQty)} ${it.unit} left",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textPrimary,
                      fontWeight = FontWeight.Medium,
                    )
                  }
                }
              }

              Card3D(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                shadowDepth = 3.dp,
                contentPadding = PaddingValues(14.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ClayIconPad(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tone = ClayTones.Red,
                    padSize = 38.dp,
                    iconSize = 20.dp,
                    cornerRadius = 12.dp,
                  )
                  Spacer(Modifier.width(10.dp))
                  Text(
                    "Outstanding Customer Dues (${dueCustomers.size})",
                    fontWeight = FontWeight.ExtraBold,
                    color = SaffronOrange,
                  )
                }
                Spacer(Modifier.height(8.dp))
                if (dueCustomers.isEmpty()) {
                  Text(
                    "No pending customer credit dues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                } else {
                  dueCustomers.take(4).forEach {
                    Text(
                      "• ${it.name}: ${CurrencyUtils.formatPaise(it.outstandingBalancePaise, business.currencySymbol.ifBlank { "₹" })}",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textPrimary,
                      fontWeight = FontWeight.Medium,
                    )
                  }
                }
              }
            },
            bottomButtons = {
              Button3D(
                onClick = { showAlertsDialog = false },
                isPrimary = false,
                cornerRadius = 16.dp,
              ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
              }
              Spacer(Modifier.width(10.dp))
              Button3D(
                onClick = {
                  showAlertsDialog = false
                  pushSubScreen(SubScreen.INVENTORY)
                },
                tone = ClayTones.Orange,
                cornerRadius = 16.dp,
              ) {
                Text("Open Inventory", color = Color.White, fontWeight = FontWeight.Bold)
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun DrawerItem3D(
  label: String,
  icon: ImageVector,
  tone: ClayTone,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.fillMaxWidth()
        .padding(vertical = 3.dp)
        .defaultMinSize(minHeight = 48.dp)
        .then(
          if (selected) {
            Modifier.raised3D(
              cornerRadius = 18.dp,
              isPressed = isPressed,
              tone = ClayTones.HeroOrange,
              shadowDepth = 4.dp,
            )
          } else {
            Modifier.raised3D(
              cornerRadius = 18.dp,
              isPressed = isPressed,
              shadowDepth = 2.dp,
            )
          }
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 12.dp, vertical = 9.dp),
  ) {
    if (selected) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(22.dp),
      )
    } else {
      Icon3D(
        imageVector = icon,
        contentDescription = null,
        tone = tone,
        size = 22.dp,
      )
    }
    Spacer(Modifier.width(12.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = if (selected) Color.White else palette.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ClayBottomNavigationBar(
  currentTab: BottomTab,
  onSelectTab: (BottomTab) -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val inactiveIconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textSecondary, palette.neutralIconBottom)
  }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .background(palette.background)
        .navigationBarsPadding()
        .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .raised3D(cornerRadius = 26.dp, shadowDepth = 6.dp)
          .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BottomTab.entries.forEach { tab ->
        val selected = currentTab == tab
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier =
            Modifier.weight(1f)
              .defaultMinSize(minHeight = 56.dp)
              .then(
                if (selected) {
                  Modifier.raised3D(
                    cornerRadius = 20.dp,
                    isPressed = isPressed,
                    tone = ClayTones.HeroOrange,
                    shadowDepth = 4.dp,
                  )
                } else {
                  Modifier.clip(RoundedCornerShape(20.dp))
                }
              )
              .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelectTab(tab) },
              )
              .padding(vertical = 6.dp, horizontal = 2.dp)
              .testTag("nav_${tab.name.lowercase()}"),
        ) {
          if (selected) {
            Icon(
              imageVector = tab.selectedIcon,
              contentDescription = tab.label,
              tint = Color.White,
              modifier = Modifier.size(22.dp),
            )
          } else {
            Icon3D(
              imageVector = tab.unselectedIcon,
              contentDescription = tab.label,
              tone = inactiveIconTone,
              size = 22.dp,
            )
          }
          Spacer(Modifier.height(3.dp))
          Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) Color.White else palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun ClayNavigationRail(
  currentTab: BottomTab,
  onSelectTab: (BottomTab) -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val inactiveIconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textSecondary, palette.neutralIconBottom)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier =
      Modifier.fillMaxHeight()
        .width(88.dp)
        .background(palette.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(vertical = 12.dp, horizontal = 8.dp)
        .raised3D(cornerRadius = 26.dp, shadowDepth = 5.dp)
        .padding(vertical = 14.dp, horizontal = 6.dp),
  ) {
    BottomTab.entries.forEach { tab ->
      val selected = currentTab == tab
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(
              if (selected) {
                Modifier.raised3D(
                  cornerRadius = 18.dp,
                  isPressed = isPressed,
                  tone = ClayTones.HeroOrange,
                  shadowDepth = 4.dp,
                )
              } else {
                Modifier.clip(RoundedCornerShape(18.dp))
              }
            )
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = { onSelectTab(tab) },
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("nav_${tab.name.lowercase()}"),
      ) {
        if (selected) {
          Icon(
            imageVector = tab.selectedIcon,
            contentDescription = tab.label,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
          )
        } else {
          Icon3D(
            imageVector = tab.unselectedIcon,
            contentDescription = tab.label,
            tone = inactiveIconTone,
            size = 22.dp,
          )
        }
        Spacer(Modifier.height(3.dp))
        Text(
          text = tab.label,
          fontSize = 10.sp,
          fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
          color = if (selected) Color.White else palette.textSecondary,
          maxLines = 1,
        )
      }
    }
  }
}
