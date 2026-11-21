package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pdf.ShareAndPrintHelper
import com.example.ui.components.VyaparTopBar
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
  settings: AppSettings,
  onOpenCustomers: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenExpenses: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenBusinessProfile: () -> Unit,
  onOpenInvoiceSettings: () -> Unit,
  onOpenBackupRestore: () -> Unit,
  onToggleDarkMode: (Boolean) -> Unit,
  onSetLanguage: (AppLanguage) -> Unit,
  onUpdatePinSecurity: (Boolean, String) -> Unit,
  onOpenHelp: () -> Unit,
) {
  var showPinDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "Business Operations",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
      ) {
        Column {
          SettingsRow("Customers", "Manage customer ledger & dues", Icons.Filled.Group, onClick = onOpenCustomers)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Orders", "Pending, processing & completed orders", Icons.Filled.LocalShipping, onClick = onOpenOrders)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Expenses", "Rent, electricity, salary & purchases", Icons.Filled.AccountBalanceWallet, onClick = onOpenExpenses)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Inventory", "Stock In, Stock Out & low alerts", Icons.Filled.Inventory, onClick = onOpenInventory)
        }
      }
    }

    item {
      Text(
        text = "Settings & Data",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(6.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
      ) {
        Column {
          SettingsRow("Shop Profile", "Logo, GSTIN, address & prefix", Icons.Filled.Business, onClick = onOpenBusinessProfile)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Invoice & GST Settings", "A4 / 80mm, CGST/SGST/IGST defaults", Icons.Filled.Receipt, onClick = onOpenInvoiceSettings)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Backup & Restore", "Export & restore your business backup", Icons.Filled.Backup, onClick = onOpenBackupRestore)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow(
            title = "Security & App PIN",
            subtitle = if (settings.appPinEnabled) "PIN Lock Enabled" else "Tap to enable PIN lock",
            icon = Icons.Filled.Lock,
            onClick = { showPinDialog = true },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = SaffronOrange)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
              Text(
                if (settings.darkMode) "Dark Mode" else "Light Mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = settings.darkMode,
              onCheckedChange = onToggleDarkMode,
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SaffronOrange),
              modifier = Modifier.testTag("theme_mode_switch"),
            )
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  val next =
                    if (settings.language == AppLanguage.ENGLISH) AppLanguage.HINDI
                    else AppLanguage.ENGLISH
                  onSetLanguage(next)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = SaffronOrange)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Invoice & App Language", style = MaterialTheme.typography.titleSmall)
              Text(
                settings.language.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("Help & Support", "Billing, PDF & backup guide", Icons.Filled.HelpOutline, onClick = onOpenHelp)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = EmeraldGreen)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Vyapar Billing Pro", style = MaterialTheme.typography.titleSmall)
              Text(
                "Version 1.0.0 • 100% Free & Offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showPinDialog) {
    var pinEnabled by remember { mutableStateOf(settings.appPinEnabled) }
    var pinText by remember { mutableStateOf(settings.appPin) }

    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text("App PIN Security") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Enable Startup PIN Lock")
            Switch(
              checked = pinEnabled,
              onCheckedChange = { pinEnabled = it },
              colors = SwitchDefaults.colors(checkedTrackColor = SaffronOrange),
            )
          }
          if (pinEnabled) {
            OutlinedTextField(
              value = pinText,
              onValueChange = { pinText = it.filter { c -> c.isDigit() }.take(6) },
              label = { Text("Set 4-6 Digit PIN") },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (!pinEnabled || pinText.length >= 4) {
              onUpdatePinSecurity(pinEnabled, if (pinEnabled) pinText else "")
              showPinDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        ) {
          Text("Save", color = Color.White)
        }
      },
      dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun SettingsRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = SaffronOrange)
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
  }
}

@Composable
fun InvoiceSettingsScreen(
  settings: AppSettings,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onSaveInvoiceDefaults: (PaperFormat, GstTaxType, GstTaxMode, Boolean) -> Unit,
) {
  BackHandler(onBack = onBack)

  var paperFormat by remember(settings) { mutableStateOf(settings.defaultPaperFormat) }
  var taxType by remember(settings) { mutableStateOf(settings.defaultTaxType) }
  var taxMode by remember(settings) { mutableStateOf(settings.defaultTaxMode) }
  var autoRound by remember(settings) { mutableStateOf(settings.autoRoundOff) }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Invoice & GST Settings",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Default PDF Paper Format", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperFormat.entries.forEach { pf ->
              FilterChip(
                selected = paperFormat == pf,
                onClick = { paperFormat = pf },
                label = { Text(pf.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
              )
            }
          }

          Text("Default GST Type", style = MaterialTheme.typography.titleSmall)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GstTaxType.entries.forEach { tt ->
              FilterChip(
                selected = taxType == tt,
                onClick = { taxType = tt },
                label = { Text(tt.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
              )
            }
          }

          Text("Default Pricing Mode", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GstTaxMode.entries.forEach { tm ->
              FilterChip(
                selected = taxMode == tm,
                onClick = { taxMode = tm },
                label = { Text(tm.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Auto Round-Off to Nearest ₹1")
            Switch(
              checked = autoRound,
              onCheckedChange = { autoRound = it },
              colors = SwitchDefaults.colors(checkedTrackColor = SaffronOrange),
            )
          }
        }
      }

      Button(
        onClick = {
          onSaveInvoiceDefaults(paperFormat, taxType, taxMode, autoRound)
          onBack()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
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
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SaffronOrangeSoft,
          border = BorderStroke(1.dp, SaffronOrange.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = SaffronOrange)
            Spacer(Modifier.width(12.dp))
            Column {
              Text("100% Offline Backup", fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
              Text(
                if (lastBackupTimestamp != null)
                  "Last backup: ${CurrencyUtils.formatDateTime(lastBackupTimestamp)}"
                else "No full backup exported yet.",
                fontSize = 12.sp,
                color = Color(0xFF7C2D12),
              )
            }
          }
        }
      }

      item {
        Button(
          onClick = { exportFullLauncher.launch("VyaparBilling_FullBackup.json") },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("export_full_backup_btn"),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
          Spacer(Modifier.width(8.dp))
          Text("Export All Business Data", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }

      item {
        OutlinedButton(
          onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.5.dp, EmeraldGreen),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("restore_backup_btn"),
        ) {
          Icon(Icons.Filled.Restore, contentDescription = null, tint = EmeraldGreen)
          Spacer(Modifier.width(8.dp))
          Text("Restore From Backup File", color = EmeraldGreen, fontWeight = FontWeight.Bold)
        }
      }

      item {
        Text(
          "Export Individual Sections",
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
          Column {
            listOf(
              "business" to "Export Business Profile",
              "products" to "Export Products Catalog",
              "customers" to "Export Customers List",
              "invoices" to "Export Invoices History",
              "settings" to "Export App Settings",
            ).forEachIndexed { idx, (key, label) ->
              if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      pendingSingleType = key
                      exportSingleLauncher.launch("vyapar_$key.json")
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Filled.Download, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  pendingRestoreBundle?.let { bundle ->
    AlertDialog(
      onDismissRequest = { pendingRestoreBundle = null },
      title = { Text("Confirm Backup Restore?") },
      text = {
        Text(
          "Verified backup from ${CurrencyUtils.formatDateTime(bundle.exportedAt)}:\n" +
            "• Business: ${bundle.business.businessName.ifBlank { "Unnamed" }}\n" +
            "• Products: ${bundle.products.size}\n" +
            "• Customers: ${bundle.customers.size}\n" +
            "• Invoices: ${bundle.invoices.size}\n\n" +
            "Existing data will be replaced with the selected backup."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRestoreValidated(bundle)
            pendingRestoreBundle = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
        ) {
          Text("Restore Now", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestoreBundle = null }) { Text("Cancel") }
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

  Scaffold(
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
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.CloudDone, contentDescription = null, tint = EmeraldGreen)
              Spacer(Modifier.width(8.dp))
              Text("100% Free & Offline Business App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
              "Your business profile, shop logo, products, customers, invoices, orders, expenses, and PDF bills work anytime without internet.",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      item {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "Quick User Guide",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SaffronOrange,
            )
            val tips =
              listOf(
                "1. Shop Setup: Add your shop name, logo, GSTIN, and invoice prefix under Shop Profile.",
                "2. Products: Add items with selling price, stock quantity, unit, and GST%.",
                "3. Fast POS Billing: Tap New Bill, add products or custom items, apply discount & GST, and generate an invoice.",
                "4. WhatsApp PDF Share: Open any invoice preview and tap WhatsApp PDF to share the document directly.",
                "5. Reports & Backup: Export PDF or CSV reports anytime and keep a safe backup file under Backup & Restore.",
              )
            tips.forEach { tip ->
              Text(tip, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}
