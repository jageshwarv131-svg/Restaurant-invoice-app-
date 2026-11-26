package com.example.printer

enum class ThermalPaperWidth(
  val label: String,
  val mm: Int,
  val charsPerLine: Int,
  val maxBitmapWidthPx: Int,
) {
  MM_58(label = "58mm (2 inch)", mm = 58, charsPerLine = 32, maxBitmapWidthPx = 384),
  MM_80(label = "80mm (3 inch)", mm = 80, charsPerLine = 48, maxBitmapWidthPx = 576),
}

enum class PrinterConnectionStatus(val label: String) {
  DISCONNECTED("Disconnected"),
  CONNECTING("Connecting..."),
  CONNECTED("Connected"),
  PRINTING("Printing..."),
  ERROR("Error"),
}

data class BluetoothPrinterDevice(
  val name: String,
  val address: String,
  val isBonded: Boolean = false,
  val isLikelyPrinter: Boolean = false,
)

data class SavedPrinterConfig(
  val name: String = "",
  val address: String = "",
  val paperWidth: ThermalPaperWidth = ThermalPaperWidth.MM_80,
  val autoCutEnabled: Boolean = true,
  val printLogoEnabled: Boolean = true,
  val printUpiQrEnabled: Boolean = true,
) {
  val hasPrinter: Boolean
    get() = address.isNotBlank()
}

data class ThermalPrinterUiState(
  val isBluetoothSupported: Boolean = true,
  val isBluetoothEnabled: Boolean = false,
  val hasBluetoothPermissions: Boolean = false,
  val isScanning: Boolean = false,
  val availableDevices: List<BluetoothPrinterDevice> = emptyList(),
  val selectedPrinter: SavedPrinterConfig = SavedPrinterConfig(),
  val connectionStatus: PrinterConnectionStatus = PrinterConnectionStatus.DISCONNECTED,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
)
