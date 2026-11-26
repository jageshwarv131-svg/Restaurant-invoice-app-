package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.BluetoothPrinterDevice
import com.example.printer.PrinterConnectionStatus
import com.example.printer.ThermalPaperWidth
import com.example.printer.ThermalPrinterUiState
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.inset3D
import com.example.ui.components.rememberSafeBottomPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrinterPickerSheet(
  state: ThermalPrinterUiState,
  requiredPermissions: Array<String>,
  canPrintInvoice: Boolean,
  onDismiss: () -> Unit,
  onRefreshPrinters: () -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onSelectPrinter: (BluetoothPrinterDevice) -> Unit,
  onPaperWidthChange: (ThermalPaperWidth) -> Unit,
  onAutoCutChange: (Boolean) -> Unit,
  onPrintLogoChange: (Boolean) -> Unit,
  onPrintUpiQrChange: (Boolean) -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onPrintNow: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val context = LocalContext.current
  val safeBottom = rememberSafeBottomPadding(extraPadding = 20.dp)

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      onRefreshPrinters()
    }

  val enableBluetoothLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      onRefreshPrinters()
    }

  LaunchedEffect(Unit) {
    onRefreshPrinters()
  }

  ModalBottomSheet(
    onDismissRequest = {
      if (state.isScanning) onStopScan()
      onDismiss()
    },
    containerColor = palette.surfaceTop,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = safeBottom),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Bluetooth Thermal Printer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Text(
            text = "ESC/POS 58mm & 80mm receipt printing",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
        IconButton(
          onClick = {
            if (state.isScanning) onStopScan()
            onDismiss()
          },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close Printer Sheet",
            tint = palette.textSecondary,
          )
        }
      }

      // Connection Status Banner
      val statusTone =
        when (state.connectionStatus) {
          PrinterConnectionStatus.CONNECTED -> ClayTones.Green
          PrinterConnectionStatus.ERROR -> ClayTones.Red
          PrinterConnectionStatus.CONNECTING, PrinterConnectionStatus.PRINTING -> ClayTones.Orange
          PrinterConnectionStatus.DISCONNECTED -> ClayTones.Blue
        }
      val statusIcon =
        when {
          !state.isBluetoothSupported || !state.isBluetoothEnabled -> Icons.Filled.BluetoothDisabled
          state.connectionStatus == PrinterConnectionStatus.CONNECTED -> Icons.Filled.BluetoothConnected
          else -> Icons.Filled.Bluetooth
        }

      Card3D(
        modifier = Modifier.fillMaxWidth().testTag("printer_status_card"),
        cornerRadius = 20.dp,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon3D(imageVector = statusIcon, contentDescription = null, tone = statusTone, size = 26.dp)
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (state.selectedPrinter.hasPrinter) state.selectedPrinter.name
                else "No Printer Selected",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            if (state.selectedPrinter.hasPrinter) {
              Text(
                text = "${state.selectedPrinter.address} • ${state.selectedPrinter.paperWidth.label}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
              )
            }
            Text(
              text = "Status: ${state.connectionStatus.label}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color =
                when (state.connectionStatus) {
                  PrinterConnectionStatus.CONNECTED -> EmeraldGreen
                  PrinterConnectionStatus.ERROR -> AlertRed
                  PrinterConnectionStatus.CONNECTING, PrinterConnectionStatus.PRINTING -> SaffronOrange
                  PrinterConnectionStatus.DISCONNECTED -> palette.textSecondary
                },
              modifier = Modifier.testTag("printer_status_label"),
            )
          }
          if (state.selectedPrinter.hasPrinter && state.hasBluetoothPermissions && state.isBluetoothEnabled) {
            if (state.connectionStatus == PrinterConnectionStatus.CONNECTED) {
              Button3D(
                onClick = onDisconnect,
                isPrimary = false,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("printer_disconnect_btn"),
              ) {
                Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertRed)
              }
            } else {
              Button3D(
                onClick = onConnect,
                enabled =
                  state.connectionStatus != PrinterConnectionStatus.CONNECTING &&
                    state.connectionStatus != PrinterConnectionStatus.PRINTING,
                tone = ClayTones.Orange,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("printer_connect_btn"),
              ) {
                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        if (!state.errorMessage.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = state.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = AlertRed,
            fontWeight = FontWeight.SemiBold,
          )
        } else if (!state.statusMessage.isNullOrBlank()) {
          Spacer(Modifier.height(6.dp))
          Text(
            text = state.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      // Permission / Bluetooth Adapter Gate
      if (!state.isBluetoothSupported) {
        Card3D(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 18.dp,
          shadowDepth = 3.dp,
          contentPadding = PaddingValues(14.dp),
        ) {
          Text(
            text = "This device does not have Classic Bluetooth hardware. You can still share, save, or print standard PDFs.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      } else if (!state.hasBluetoothPermissions && requiredPermissions.isNotEmpty()) {
        Button3D(
          onClick = { permissionLauncher.launch(requiredPermissions) },
          tone = ClayTones.Orange,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("grant_bt_permission_btn"),
        ) {
          Text(
            text = "Allow Bluetooth Printer Permission",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
          )
        }
      } else if (!state.isBluetoothEnabled) {
        Button3D(
          onClick = {
            runCatching {
              enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
          },
          tone = ClayTones.Blue,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("enable_bt_btn"),
        ) {
          Text(
            text = "Turn On Bluetooth",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
        }
      }

      // Paper Width & Print Preferences
      Card3D(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(14.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Thermal Paper Width",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ThermalPaperWidth.entries.forEach { width ->
              Chip3D(
                selected = state.selectedPrinter.paperWidth == width,
                onClick = { onPaperWidthChange(width) },
                label = width.label,
                selectedTone = ClayTones.Orange,
                modifier = Modifier.weight(1f).testTag("paper_width_${width.mm}"),
              )
            }
          }

          HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.4f))

          PrinterToggleRow(
            label = "Auto Paper Cut (Supported Printers)",
            checked = state.selectedPrinter.autoCutEnabled,
            onCheckedChange = onAutoCutChange,
          )
          PrinterToggleRow(
            label = "Print Store Logo",
            checked = state.selectedPrinter.printLogoEnabled,
            onCheckedChange = onPrintLogoChange,
          )
          PrinterToggleRow(
            label = "Print UPI Payment QR (When UPI ID Set)",
            checked = state.selectedPrinter.printUpiQrEnabled,
            onCheckedChange = onPrintUpiQrChange,
          )
        }
      }

      // Paired & Nearby Printer Discovery
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button3D(
          onClick = onRefreshPrinters,
          isPrimary = false,
          cornerRadius = 16.dp,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("refresh_printers_btn"),
        ) {
          Icon3D(imageVector = Icons.Filled.Refresh, contentDescription = null, tone = ClayTones.Blue, size = 18.dp)
          Spacer(Modifier.width(6.dp))
          Text(
            text = "Paired Devices",
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
          )
        }

        Button3D(
          onClick = { if (state.isScanning) onStopScan() else onStartScan() },
          tone = if (state.isScanning) ClayTones.Red else ClayTones.Orange,
          cornerRadius = 16.dp,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("scan_printers_btn"),
        ) {
          if (state.isScanning) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = Color.White,
              strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(6.dp))
            Text("Stop Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          } else {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Scan Nearby",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              textAlign = TextAlign.Center,
              fontSize = 12.sp,
            )
          }
        }
      }

      if (state.availableDevices.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().inset3D(cornerRadius = 18.dp).padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              if (!state.hasBluetoothPermissions || !state.isBluetoothEnabled) {
                "Enable Bluetooth and allow permissions to view paired thermal printers."
              } else {
                "No printers listed yet. Pair your 58mm/80mm Bluetooth printer in Android settings or tap Scan Nearby."
              },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(state.availableDevices, key = { it.address }) { dev ->
            val isSelected = state.selectedPrinter.address == dev.address
            Card3D(
              modifier = Modifier.fillMaxWidth().testTag("bt_device_${dev.address}"),
              cornerRadius = 18.dp,
              shadowDepth = if (isSelected) 5.dp else 3.dp,
              accentBorderColor = if (isSelected) SaffronOrange else null,
              onClick = { onSelectPrinter(dev) },
              contentPadding = PaddingValues(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon3D(
                  imageVector = Icons.Filled.Print,
                  contentDescription = null,
                  tone = if (dev.isLikelyPrinter) ClayTones.Orange else ClayTones.Blue,
                  size = 22.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = dev.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                  )
                  Text(
                    text =
                      buildString {
                        append(dev.address)
                        if (dev.isBonded) append(" • Paired")
                        if (dev.isLikelyPrinter) append(" • Printer")
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected Printer",
                    tint = SaffronOrange,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }
      }

      if (canPrintInvoice) {
        Button3D(
          onClick = onPrintNow,
          enabled =
            state.selectedPrinter.hasPrinter &&
              state.isBluetoothEnabled &&
              state.hasBluetoothPermissions &&
              state.connectionStatus != PrinterConnectionStatus.PRINTING,
          tone = ClayTones.Green,
          cornerRadius = 20.dp,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("thermal_print_now_btn"),
        ) {
          Icon(Icons.Filled.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(
            text =
              if (state.connectionStatus == PrinterConnectionStatus.PRINTING) "Printing Receipt..."
              else "Print Thermal Receipt (${state.selectedPrinter.paperWidth.mm}mm)",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
          )
        }
      }
    }
  }
}

@Composable
private fun PrinterToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .clickable { onCheckedChange(!checked) },
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.textPrimary,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(1f).padding(end = 10.dp),
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SaffronOrange),
    )
  }
}
