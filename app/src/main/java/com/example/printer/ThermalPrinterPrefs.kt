package com.example.printer

import android.content.Context
import android.content.SharedPreferences

class ThermalPrinterPrefs(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun load(): SavedPrinterConfig {
    val widthName = prefs.getString(KEY_PAPER_WIDTH, ThermalPaperWidth.MM_80.name)
    val paperWidth =
      runCatching { ThermalPaperWidth.valueOf(widthName ?: ThermalPaperWidth.MM_80.name) }
        .getOrDefault(ThermalPaperWidth.MM_80)
    return SavedPrinterConfig(
      name = prefs.getString(KEY_PRINTER_NAME, "") ?: "",
      address = prefs.getString(KEY_PRINTER_ADDRESS, "") ?: "",
      paperWidth = paperWidth,
      autoCutEnabled = prefs.getBoolean(KEY_AUTO_CUT, true),
      printLogoEnabled = prefs.getBoolean(KEY_PRINT_LOGO, true),
      printUpiQrEnabled = prefs.getBoolean(KEY_PRINT_UPI_QR, true),
    )
  }

  fun save(config: SavedPrinterConfig) {
    prefs
      .edit()
      .putString(KEY_PRINTER_NAME, config.name)
      .putString(KEY_PRINTER_ADDRESS, config.address)
      .putString(KEY_PAPER_WIDTH, config.paperWidth.name)
      .putBoolean(KEY_AUTO_CUT, config.autoCutEnabled)
      .putBoolean(KEY_PRINT_LOGO, config.printLogoEnabled)
      .putBoolean(KEY_PRINT_UPI_QR, config.printUpiQrEnabled)
      .apply()
  }

  fun clearSelectedPrinter() {
    prefs
      .edit()
      .remove(KEY_PRINTER_NAME)
      .remove(KEY_PRINTER_ADDRESS)
      .apply()
  }

  private companion object {
    const val PREFS_NAME = "vyapar_thermal_printer_prefs"
    const val KEY_PRINTER_NAME = "printer_name"
    const val KEY_PRINTER_ADDRESS = "printer_address"
    const val KEY_PAPER_WIDTH = "paper_width"
    const val KEY_AUTO_CUT = "auto_cut_enabled"
    const val KEY_PRINT_LOGO = "print_logo_enabled"
    const val KEY_PRINT_UPI_QR = "print_upi_qr_enabled"
  }
}
