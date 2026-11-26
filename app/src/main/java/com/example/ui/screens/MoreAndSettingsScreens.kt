package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.FullBackupBundle
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.PaperFormat
import com.example.model.ThemePreference
import com.example.pdf.ShareAndPrintHelper
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.data.EntitlementStatus
import com.example.data.SubscriptionState
import com.example.data.SubscriptionTimeUtils
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
import com.example.util.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
  settings: AppSettings,
  isDarkActive: Boolean,
  isAuthenticated: Boolean,
  hasActiveLicense: Boolean,
  onOpenAccount: () -> Unit,
  onOpenLifetimeLicense: () -> Unit,
  onOpenCustomers: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenExpenses: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenBusinessProfile: () -> Unit,
  onOpenInvoiceSettings: () -> Unit,
  onOpenBackupRestore: () -> Unit,
  onSetThemePreference: (ThemePreference) -> Unit,
  onToggleDarkMode: (Boolean) -> Unit,
  onSetLanguage: (AppLanguage) -> Unit,
  onUpdatePinSecurity: (Boolean, String) -> Unit,
  onOpenHelp: () -> Unit,
  subscriptionState: SubscriptionState? = null,
  trialRemainingMillis: Long = 0L,
) {
  val palette = LocalVyaparClayPalette.current
  var showPinDialog by remember { mutableStateOf(false) }
  val subStatus = subscriptionState?.status
  val isTrialActive = subStatus == EntitlementStatus.TRIAL_ACTIVE
  val effectiveTrialRemaining =
    if (trialRemainingMillis > 0L) trialRemainingMillis
    else subscriptionState?.trialRemainingMillis() ?: 0L

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(palette.background),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 22.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Account & Plans",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = palette.textSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
      )
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(10.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsRow3D(
            title = "My Account & Subscription",
            subtitle = when {
              subStatus == EntitlementStatus.LIFETIME_ACTIVE || hasActiveLicense ->
                "${subscriptionState?.plan?.title ?: "Lifetime License"} Active"
              isTrialActive ->
                "Free Trial Active • ${SubscriptionTimeUtils.formatCountdown(effectiveTrialRemaining)}"
              isAuthenticated ->
                subStatus?.label ?: "Signed in • Choose Monthly, Yearly or Lifetime"
              else -> "Login / Create Account to activate"
            },
            icon = Icons.Filled.AccountCircle,
            tone = when {
              hasActiveLicense -> ClayTones.Green
              isTrialActive -> ClayTones.Blue
              else -> ClayTones.Orange
            },
            onClick = onOpenAccount,
          )
          SettingsRow3D(
            title = when {
              subStatus == EntitlementStatus.LIFETIME_ACTIVE -> "Lifetime License Active"
              subStatus == EntitlementStatus.MONTHLY_ACTIVE -> "Monthly License Active"
              subStatus == EntitlementStatus.YEARLY_ACTIVE -> "Yearly License Active"
              isTrialActive -> "Free Trial (${SubscriptionTimeUtils.formatCountdown(effectiveTrialRemaining)})"
              else -> "Upgrade Plans (₹499 / ₹999 / ₹2000)"
            },
            subtitle = when {
              hasActiveLicense -> "Verified active server subscription"
              isTrialActive -> "Monthly ₹499 • Yearly ₹999 • Lifetime ₹2000"
              else -> "Pay via PhonePe UPI & submit UTR"
            },
            icon = Icons.Filled.VerifiedUser,
            tone = if (hasActiveLicense) ClayTones.Green else ClayTones.Purple,
            onClick = onOpenLifetimeLicense,
          )
        }
      }
    }
    item {
      Text(
        text = "Business Operations",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = palette.textSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
      )
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(10.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsRow3D("Customers", "Manage customer ledger & dues", Icons.Filled.Group, ClayTones.Blue, onClick = onOpenCustomers)
          SettingsRow3D("Pending Orders", "Pending, processing & completed orders", Icons.Filled.LocalShipping, ClayTones.Orange, onClick = onOpenOrders)
          SettingsRow3D("Expenses", "Rent, electricity, salary & purchases", Icons.Filled.AccountBalanceWallet, ClayTones.Purple, onClick = onOpenExpenses)
          SettingsRow3D("Inventory", "Stock In, Stock Out & low alerts", Icons.Filled.Inventory, ClayTones.Amber, onClick = onOpenInventory)
        }
      }
    }

    item {
      Text(
        text = "Settings & Data",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = palette.textSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
      )
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(10.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsRow3D("Shop Profile", "Logo, GSTIN, address & prefix", Icons.Filled.Business, ClayTones.Orange, onClick = onOpenBusinessProfile)
          SettingsRow3D("Invoice & GST Settings", "A4 / 80mm, CGST/SGST/IGST defaults", Icons.Filled.Receipt, ClayTones.Blue, onClick = onOpenInvoiceSettings)
          SettingsRow3D("Backup & Restore", "Export & restore your business backup", Icons.Filled.Backup, ClayTones.Green, onClick = onOpenBackupRestore)
          SettingsRow3D(
            title = "Security & App PIN",
            subtitle = if (settings.appPinEnabled) "PIN Lock Enabled" else "Tap to enable PIN lock",
            icon = Icons.Filled.Lock,
            tone = ClayTones.Red,
            onClick = { showPinDialog = true },
          )

          // Theme Mode Card: System | Light | Dark + Quick Dark Toggle
          Card3D(
            cornerRadius = 18.dp,
            shadowDepth = 3.dp,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon3D(Icons.Filled.DarkMode, contentDescription = null, tone = ClayTones.Purple, size = 28.dp)
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("Appearance Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(
                  text = "Active: ${settings.themePreference.label} (${if (isDarkActive) "Dark" else "Light"})",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }
              Switch(
                checked = isDarkActive,
                onCheckedChange = onToggleDarkMode,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SaffronOrange),
                modifier = Modifier.testTag("theme_mode_switch"),
              )
            }
            Spacer(Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ThemePreference.entries.forEach { pref ->
                Chip3D(
                  selected = settings.themePreference == pref,
                  onClick = { onSetThemePreference(pref) },
                  label = pref.label,
                  modifier = Modifier.weight(1f).testTag("theme_pref_${pref.name.lowercase()}"),
                )
              }
            }
          }

          SettingsRow3D(
            title = "Invoice & App Language",
            subtitle = settings.language.label,
            icon = Icons.Filled.Language,
            tone = ClayTones.Amber,
            onClick = {
              val next =
                if (settings.language == AppLanguage.ENGLISH) AppLanguage.HINDI
                else AppLanguage.ENGLISH
              onSetLanguage(next)
            },
          )

          SettingsRow3D("Help & Support", "Billing, PDF & backup guide", Icons.Filled.HelpOutline, ClayTones.Blue, onClick = onOpenHelp)

          Card3D(
            cornerRadius = 18.dp,
            shadowDepth = 3.dp,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon3D(Icons.Filled.Info, contentDescription = null, tone = ClayTones.Green, size = 28.dp)
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("Vyapar Billing Pro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(
                  "Version 1.0.0 • Offline POS Billing & Cloud Plans",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showPinDialog) {
    var pinEnabled by remember { mutableStateOf(settings.appPinEnabled) }
    var pinText by remember { mutableStateOf(settings.appPin) }

    VyaparFormDialog(
      title = "App PIN Security",
      onDismiss = { showPinDialog = false },
      scrollContent = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Enable Startup PIN Lock", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
          Switch(
            checked = pinEnabled,
            onCheckedChange = { pinEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SaffronOrange),
          )
        }
        if (pinEnabled) {
          TextField3D(
            value = pinText,
            onValueChange = { pinText = it.filter { c -> c.isDigit() }.take(6) },
            label = "Set 4-6 Digit PIN",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      bottomButtons = {
        Button3D(
          onClick = { showPinDialog = false },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            if (!pinEnabled || pinText.length >= 4) {
              onUpdatePinSecurity(pinEnabled, if (pinEnabled) pinText else "")
              showPinDialog = false
            }
          },
          cornerRadius = 16.dp,
        ) {
          Text("Save", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      },
    )
  }
}

@Composable
private fun SettingsRow3D(
  title: String,
  subtitle: String,
  icon: ImageVector,
  tone: ClayTone,
  onClick: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  Card3D(
    cornerRadius = 18.dp,
    shadowDepth = 3.dp,
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon3D(imageVector = icon, contentDescription = null, tone = tone, size = 28.dp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      }
      Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = palette.textSecondary,
      )
    }
  }
}

@Composable
fun InvoiceSettingsScreen(
  settings: AppSettings,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onSaveInvoiceDefaults: (PaperFormat, GstTaxType, GstTaxMode, Boolean) -> Unit,
  printerState: com.example.printer.ThermalPrinterUiState? = null,
  requiredBluetoothPermissions: Array<String> = emptyArray(),
  onRefreshPrinters: () -> Unit = {},
  onStartPrinterScan: () -> Unit = {},
  onStopPrinterScan: () -> Unit = {},
  onSelectPrinter: (com.example.printer.BluetoothPrinterDevice) -> Unit = {},
  onPrinterPaperWidthChange: (com.example.printer.ThermalPaperWidth) -> Unit = {},
  onPrinterAutoCutChange: (Boolean) -> Unit = {},
  onPrinterLogoChange: (Boolean) -> Unit = {},
  onPrinterQrChange: (Boolean) -> Unit = {},
  onConnectPrinter: () -> Unit = {},
  onDisconnectPrinter: () -> Unit = {},
) {
  BackHandler(onBack = onBack)

  val palette = LocalVyaparClayPalette.current
  var paperFormat by remember(settings) { mutableStateOf(settings.defaultPaperFormat) }
  var taxType by remember(settings) { mutableStateOf(settings.defaultTaxType) }
  var taxMode by remember(settings) { mutableStateOf(settings.defaultTaxMode) }
  var autoRound by remember(settings) { mutableStateOf(settings.autoRoundOff) }
  var showPrinterSheet by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = palette.background,
    topBar = {
      VyaparTopBar(
        title = "Invoice & GST Settings",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    },
    bottomBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)
      ) {
        Button3D(
          onClick = {
            onSaveInvoiceDefaults(paperFormat, taxType, taxMode, autoRound)
            onBack()
          },
          cornerRadius = 20.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
          Text("Save Settings", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text("Default PDF Paper Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaperFormat.entries.forEach { pf ->
              Chip3D(
                selected = paperFormat == pf,
                onClick = { paperFormat = pf },
                label = pf.label,
              )
            }
          }

          Text("Default GST Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GstTaxType.entries.forEach { tt ->
              Chip3D(
                selected = taxType == tt,
                onClick = { taxType = tt },
                label = tt.label,
              )
            }
          }

          Text("Default Pricing Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GstTaxMode.entries.forEach { tm ->
              Chip3D(
                selected = taxMode == tm,
                onClick = { taxMode = tm },
                label = tm.label,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Auto Round-Off to Nearest ₹1", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            Switch(
              checked = autoRound,
              onCheckedChange = { autoRound = it },
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SaffronOrange),
            )
          }
        }
      }

      if (printerState != null) {
        Card3D(
          cornerRadius = 24.dp,
          shadowDepth = 5.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Bluetooth Thermal Printer",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            Text(
              text =
                if (printerState.selectedPrinter.hasPrinter) {
                  "${printerState.selectedPrinter.name} (${printerState.selectedPrinter.paperWidth.label}) • ${printerState.connectionStatus.label}"
                } else {
                  "No Bluetooth thermal printer configured yet."
                },
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
            Button3D(
              onClick = { showPrinterSheet = true },
              isPrimary = false,
              cornerRadius = 16.dp,
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
              Text("Configure Thermal Printer", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  if (showPrinterSheet && printerState != null) {
    ThermalPrinterPickerSheet(
      state = printerState,
      requiredPermissions = requiredBluetoothPermissions,
      canPrintInvoice = false,
      onDismiss = { showPrinterSheet = false },
      onRefreshPrinters = onRefreshPrinters,
      onStartScan = onStartPrinterScan,
      onStopScan = onStopPrinterScan,
      onSelectPrinter = onSelectPrinter,
      onPaperWidthChange = onPrinterPaperWidthChange,
      onAutoCutChange = onPrinterAutoCutChange,
      onPrintLogoChange = onPrinterLogoChange,
      onPrintUpiQrChange = onPrinterQrChange,
      onConnect = onConnectPrinter,
      onDisconnect = onDisconnectPrinter,
      onPrintNow = {},
    )
  }
}

@Composable
fun BackupRestoreScreen(
  lastBackupTimestamp: Long?,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onBuildFullBackupJson: suspend () -> String,
  onExportSingleJson: suspend (String) -> String,
  onValidateBackup: suspend (String) -> Result<FullBackupBundle>,
  onRestoreValidated: (FullBackupBundle) -> Unit,
  onShowToast: (String) -> Unit,
) {
  BackHandler(onBack = onBack)

  val palette = LocalVyaparClayPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var pendingSingleType by remember { mutableStateOf<String?>(null) }
  var pendingRestoreBundle by remember { mutableStateOf<FullBackupBundle?>(null) }

  val exportFullLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          val json = onBuildFullBackupJson()
          val ok = ShareAndPrintHelper.writeTextToSafUri(context, json, uri)
          onShowToast(if (ok) "Full backup exported" else "Export failed")
        }
      }
    }

  val exportSingleLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      val type = pendingSingleType
      if (uri != null && type != null) {
        scope.launch {
          val json = onExportSingleJson(type)
          val ok = ShareAndPrintHelper.writeTextToSafUri(context, json, uri)
          onShowToast(if (ok) "Exported $type backup" else "Export failed")
        }
      }
    }

  val importBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          val raw = ShareAndPrintHelper.readTextFromSafUri(context, uri)
          if (raw == null) {
            onShowToast("Could not read selected file")
            return@launch
          }
          onValidateBackup(raw)
            .onSuccess { bundle -> pendingRestoreBundle = bundle }
            .onFailure { err -> onShowToast("Invalid backup: ${err.message}") }
        }
      }
    }

  Scaffold(
    containerColor = palette.background,
    topBar = {
      VyaparTopBar(
        title = "Backup & Restore",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          accentBorderColor = SaffronOrange.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3D(Icons.Filled.Security, contentDescription = null, tone = ClayTones.Orange, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column {
              Text("100% Offline Backup", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
              Text(
                if (lastBackupTimestamp != null)
                  "Last backup: ${CurrencyUtils.formatDateTime(lastBackupTimestamp)}"
                else "No full backup exported yet.",
                fontSize = 12.sp,
                color = palette.textSecondary,
              )
            }
          }
        }
      }

      item {
        Button3D(
          onClick = { exportFullLauncher.launch("VyaparBilling_FullBackup.json") },
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("export_full_backup_btn"),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
          Spacer(Modifier.width(8.dp))
          Text("Export All Business Data", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      }

      item {
        Button3D(
          onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
          tone = ClayTones.Green,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("restore_backup_btn"),
        ) {
          Icon(Icons.Filled.Restore, contentDescription = null, tint = Color.White)
          Spacer(Modifier.width(8.dp))
          Text("Restore From Backup File", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      }

      item {
        Text(
          "Export Individual Sections",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textSecondary,
          modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
        )
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          contentPadding = PaddingValues(8.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              "business" to "Export Business Profile",
              "products" to "Export Products Catalog",
              "customers" to "Export Customers List",
              "invoices" to "Export Invoices History",
              "settings" to "Export App Settings",
            ).forEachIndexed { idx, (key, label) ->
              if (idx > 0) HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.25f))
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      pendingSingleType = key
                      exportSingleLauncher.launch("vyapar_$key.json")
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                Icon3D(Icons.Filled.Download, contentDescription = null, tone = ClayTones.Orange, size = 20.dp)
              }
            }
          }
        }
      }
    }
  }

  pendingRestoreBundle?.let { bundle ->
    VyaparFormDialog(
      title = "Confirm Backup Restore?",
      onDismiss = { pendingRestoreBundle = null },
      scrollContent = {
        Text(
          "Verified backup from ${CurrencyUtils.formatDateTime(bundle.exportedAt)}:\n" +
            "• Business: ${bundle.business.businessName.ifBlank { "Unnamed" }}\n" +
            "• Products: ${bundle.products.size}\n" +
            "• Customers: ${bundle.customers.size}\n" +
            "• Invoices: ${bundle.invoices.size}\n\n" +
            "Existing data will be replaced with the selected backup.",
          color = palette.textSecondary,
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { pendingRestoreBundle = null },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            onRestoreValidated(bundle)
            pendingRestoreBundle = null
          },
          tone = ClayTones.Green,
          cornerRadius = 16.dp,
        ) {
          Text("Restore Now", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      },
    )
  }
}

@Composable
fun HelpSupportScreen(
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val palette = LocalVyaparClayPalette.current

  Scaffold(
    containerColor = palette.background,
    topBar = {
      VyaparTopBar(
        title = "Help & Support",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Card3D(
          cornerRadius = 24.dp,
          shadowDepth = 5.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon3D(Icons.Filled.CloudDone, contentDescription = null, tone = ClayTones.Green, size = 30.dp)
              Spacer(Modifier.width(10.dp))
              Text(
                "100% Free & Offline Business App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
              )
            }
            Text(
              "Your business profile, shop logo, products, customers, invoices, orders, expenses, and PDF bills work anytime without internet.",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
            )
          }
        }
      }

      item {
        Card3D(
          cornerRadius = 24.dp,
          shadowDepth = 5.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              "Quick User Guide",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = SaffronOrange,
            )
            val tips =
              listOf(
                "1. Shop Setup: Add your shop name, logo, GSTIN, and invoice prefix under Shop Profile.",
                "2. Products: Add items with selling price, stock quantity, unit, and GST%.",
                "3. Fast POS Billing: Tap New Bill, add products or custom items, apply discount & GST, and generate an invoice.",
                "4. Pending Orders: Save customer orders first, edit final rates & quantities later, then Confirm to generate the bill.",
                "5. WhatsApp PDF Share: Open any invoice preview and tap WhatsApp PDF to share the document directly.",
                "6. Reports & Backup: Export PDF or CSV reports anytime and keep a safe backup file under Backup & Restore.",
              )
            tips.forEach { tip ->
              Text(tip, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
            }
          }
        }
      }
    }
  }
}
