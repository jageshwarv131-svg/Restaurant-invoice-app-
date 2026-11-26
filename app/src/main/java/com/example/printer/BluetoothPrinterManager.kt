package com.example.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.Invoice
import com.example.pdf.InvoicePdfGenerator
import com.example.pdf.UpiQrGenerator
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface PrinterTransport {
  val connectedAddress: String?
  val isConnected: Boolean

  @Throws(IOException::class, SecurityException::class)
  fun connect(address: String)

  @Throws(IOException::class)
  fun writeAndFlush(data: ByteArray)

  fun close()
}

interface BluetoothSystemBridge {
  fun isBluetoothSupported(): Boolean
  fun isBluetoothEnabled(): Boolean
  fun hasBluetoothPermissions(): Boolean
  fun getBondedDevices(): List<BluetoothPrinterDevice>
  fun startDiscovery(onDeviceFound: (BluetoothPrinterDevice) -> Unit, onFinished: () -> Unit): Boolean
  fun stopDiscovery()
}

class AndroidBluetoothSystemBridge(private val context: Context) : BluetoothSystemBridge {

  private val adapter: BluetoothAdapter?
    get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

  private var discoveryReceiver: BroadcastReceiver? = null

  override fun isBluetoothSupported(): Boolean = adapter != null

  override fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

  override fun hasBluetoothPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
        PackageManager.PERMISSION_GRANTED
    }
  }

  @SuppressLint("MissingPermission")
  override fun getBondedDevices(): List<BluetoothPrinterDevice> {
    val btAdapter = adapter ?: return emptyList()
    if (!btAdapter.isEnabled || !hasBluetoothPermissions()) return emptyList()
    return runCatching {
        btAdapter.bondedDevices
          ?.filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
          ?.map { it.toPrinterDevice(isBonded = true) }
          ?.sortedWith(compareByDescending<BluetoothPrinterDevice> { it.isLikelyPrinter }.thenBy { it.name })
          .orEmpty()
      }
      .getOrDefault(emptyList())
  }

  @SuppressLint("MissingPermission")
  override fun startDiscovery(
    onDeviceFound: (BluetoothPrinterDevice) -> Unit,
    onFinished: () -> Unit,
  ): Boolean {
    val btAdapter = adapter ?: return false
    if (!btAdapter.isEnabled || !hasBluetoothPermissions()) return false
    stopDiscovery()

    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
              val device: BluetoothDevice? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                  @Suppress("DEPRECATION")
                  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
              if (device != null && device.type != BluetoothDevice.DEVICE_TYPE_LE) {
                onDeviceFound(device.toPrinterDevice(isBonded = device.bondState == BluetoothDevice.BOND_BONDED))
              }
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
              onFinished()
            }
          }
        }
      }
    discoveryReceiver = receiver
    val filter =
      IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      context.registerReceiver(receiver, filter)
    }

    return runCatching {
        if (btAdapter.isDiscovering) btAdapter.cancelDiscovery()
        btAdapter.startDiscovery()
      }
      .getOrDefault(false)
  }

  @SuppressLint("MissingPermission")
  override fun stopDiscovery() {
    runCatching {
      val btAdapter = adapter
      if (btAdapter != null && hasBluetoothPermissions() && btAdapter.isDiscovering) {
        btAdapter.cancelDiscovery()
      }
    }
    discoveryReceiver?.let { receiver ->
      runCatching { context.unregisterReceiver(receiver) }
    }
    discoveryReceiver = null
  }

  @SuppressLint("MissingPermission")
  private fun BluetoothDevice.toPrinterDevice(isBonded: Boolean): BluetoothPrinterDevice {
    val safeName = runCatching { name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Bluetooth Device"
    val majorClass = runCatching { bluetoothClass?.majorDeviceClass }.getOrNull()
    val likelyPrinter =
      majorClass == BluetoothClass.Device.Major.IMAGING ||
        PRINTER_NAME_HINTS.any { safeName.contains(it, ignoreCase = true) }
    return BluetoothPrinterDevice(
      name = safeName,
      address = address.orEmpty(),
      isBonded = isBonded,
      isLikelyPrinter = likelyPrinter,
    )
  }

  private companion object {
    val PRINTER_NAME_HINTS =
      listOf("printer", "pos", "thermal", "mpt", "rpp", "xprinter", "epson", "star", "bixolon", "tvs", "tsc", "58", "80")
  }
}

class AndroidBluetoothPrinterTransport(private val context: Context) : PrinterTransport {

  private var socket: BluetoothSocket? = null
  private var outputStream: OutputStream? = null
  private var currentAddress: String? = null

  override val connectedAddress: String?
    get() = currentAddress

  override val isConnected: Boolean
    get() = socket?.isConnected == true && outputStream != null

  @SuppressLint("MissingPermission")
  override fun connect(address: String) {
    close()
    val adapter =
      (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        ?: throw IOException("Bluetooth adapter is unavailable.")
    if (!adapter.isEnabled) throw IOException("Bluetooth is turned off.")

    val device = adapter.getRemoteDevice(address)
    runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }

    val rfcommSocket =
      try {
        device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
      } catch (primaryError: IOException) {
        // Fallback RFCOMM channel 1 used by several budget 58mm/80mm ESC/POS thermal printers
        runCatching {
          val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
          (m.invoke(device, 1) as BluetoothSocket).also { it.connect() }
        }.getOrElse {
          throw primaryError
        }
      }

    socket = rfcommSocket
    outputStream = rfcommSocket.outputStream
    currentAddress = address
  }

  override fun writeAndFlush(data: ByteArray) {
    val out = outputStream ?: throw IOException("Printer socket is not open.")
    val sock = socket ?: throw IOException("Printer is disconnected.")
    if (!sock.isConnected) throw IOException("Printer disconnected before write.")

    val chunkSize = 1024
    var offset = 0
    while (offset < data.size) {
      val len = minOf(chunkSize, data.size - offset)
      out.write(data, offset, len)
      offset += len
      if (offset < data.size) {
        out.flush()
        runCatching { Thread.sleep(10) }
      }
    }
    out.flush()
  }

  override fun close() {
    runCatching { outputStream?.close() }
    runCatching { socket?.close() }
    outputStream = null
    socket = null
    currentAddress = null
  }

  companion object {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  }
}

class BluetoothPrinterManager(
  private val prefs: ThermalPrinterPrefs,
  private val bridge: BluetoothSystemBridge,
  private val transport: PrinterTransport,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val opMutex = Mutex()
  private val _state = MutableStateFlow(ThermalPrinterUiState(selectedPrinter = prefs.load()))
  val state: StateFlow<ThermalPrinterUiState> = _state.asStateFlow()

  init {
    refreshEnvironmentAndPaired()
  }

  fun requiredRuntimePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
      emptyArray()
    }
  }

  fun refreshEnvironmentAndPaired() {
    val supported = bridge.isBluetoothSupported()
    val enabled = supported && bridge.isBluetoothEnabled()
    val perms = bridge.hasBluetoothPermissions()
    val bonded = if (supported && enabled && perms) bridge.getBondedDevices() else emptyList()

    _state.update { current ->
      val merged = mergeDevices(bonded, current.availableDevices.filter { !it.isBonded })
      val status =
        if (!supported || !enabled) {
          if (current.connectionStatus == PrinterConnectionStatus.CONNECTED) {
            transport.close()
          }
          PrinterConnectionStatus.DISCONNECTED
        } else if (transport.isConnected && transport.connectedAddress == current.selectedPrinter.address) {
          PrinterConnectionStatus.CONNECTED
        } else if (current.connectionStatus == PrinterConnectionStatus.CONNECTED) {
          PrinterConnectionStatus.DISCONNECTED
        } else {
          current.connectionStatus
        }

      current.copy(
        isBluetoothSupported = supported,
        isBluetoothEnabled = enabled,
        hasBluetoothPermissions = perms,
        availableDevices = merged,
        connectionStatus = status,
      )
    }
  }

  fun startDiscovery() {
    refreshEnvironmentAndPaired()
    val snap = _state.value
    if (!snap.isBluetoothSupported) {
      setError("Bluetooth hardware is not available on this device.")
      return
    }
    if (!snap.hasBluetoothPermissions) {
      setError("Bluetooth permission is required to scan for thermal printers.")
      return
    }
    if (!snap.isBluetoothEnabled) {
      setError("Please turn on Bluetooth to scan for thermal printers.")
      return
    }

    _state.update { it.copy(isScanning = true, errorMessage = null, statusMessage = "Scanning for Bluetooth printers...") }
    val started =
      bridge.startDiscovery(
        onDeviceFound = { found ->
          _state.update { cur ->
            val list = mergeDevices(cur.availableDevices, listOf(found))
            cur.copy(availableDevices = list)
          }
        },
        onFinished = {
          _state.update { cur ->
            cur.copy(
              isScanning = false,
              statusMessage = if (cur.availableDevices.isEmpty()) "No Bluetooth printers found nearby" else "Scan completed",
            )
          }
        },
      )
    if (!started) {
      _state.update {
        it.copy(
          isScanning = false,
          statusMessage = null,
          errorMessage = "Could not start Bluetooth printer discovery. Check Bluetooth settings.",
        )
      }
    }
  }

  fun stopDiscovery() {
    bridge.stopDiscovery()
    _state.update { it.copy(isScanning = false) }
  }

  fun selectPrinter(device: BluetoothPrinterDevice) {
    val wasDifferent = _state.value.selectedPrinter.address != device.address
    if (wasDifferent && transport.isConnected) {
      transport.close()
    }
    val updated =
      _state.value.selectedPrinter.copy(
        name = device.name,
        address = device.address,
      )
    prefs.save(updated)
    _state.update {
      it.copy(
        selectedPrinter = updated,
        connectionStatus =
          if (!wasDifferent && transport.isConnected) PrinterConnectionStatus.CONNECTED
          else PrinterConnectionStatus.DISCONNECTED,
        errorMessage = null,
        statusMessage = "Selected ${device.name}",
      )
    }
  }

  fun updatePaperWidth(paperWidth: ThermalPaperWidth) {
    val updated = _state.value.selectedPrinter.copy(paperWidth = paperWidth)
    prefs.save(updated)
    _state.update { it.copy(selectedPrinter = updated) }
  }

  fun updateAutoCut(enabled: Boolean) {
    val updated = _state.value.selectedPrinter.copy(autoCutEnabled = enabled)
    prefs.save(updated)
    _state.update { it.copy(selectedPrinter = updated) }
  }

  fun updatePrintLogo(enabled: Boolean) {
    val updated = _state.value.selectedPrinter.copy(printLogoEnabled = enabled)
    prefs.save(updated)
    _state.update { it.copy(selectedPrinter = updated) }
  }

  fun updatePrintUpiQr(enabled: Boolean) {
    val updated = _state.value.selectedPrinter.copy(printUpiQrEnabled = enabled)
    prefs.save(updated)
    _state.update { it.copy(selectedPrinter = updated) }
  }

  suspend fun connectSelectedPrinter(): Result<Unit> =
    withContext(ioDispatcher) {
      opMutex.withLock {
        connectSelectedInternal()
      }
    }

  private fun connectSelectedInternal(): Result<Unit> {
    refreshEnvironmentAndPaired()
    val snap = _state.value
    if (!snap.isBluetoothSupported) {
      return failConnection("Bluetooth is not supported on this device.")
    }
    if (!snap.hasBluetoothPermissions) {
      return failConnection("Bluetooth permission denied. Allow Nearby devices permission to connect.")
    }
    if (!snap.isBluetoothEnabled) {
      return failConnection("Bluetooth is turned off. Please enable Bluetooth.")
    }
    val selected = snap.selectedPrinter
    if (!selected.hasPrinter) {
      return failConnection("No thermal printer selected. Please choose a paired printer first.")
    }

    if (transport.isConnected && transport.connectedAddress == selected.address) {
      _state.update {
        it.copy(
          connectionStatus = PrinterConnectionStatus.CONNECTED,
          errorMessage = null,
          statusMessage = "Connected to ${selected.name}",
        )
      }
      return Result.success(Unit)
    }

    bridge.stopDiscovery()
    _state.update {
      it.copy(
        isScanning = false,
        connectionStatus = PrinterConnectionStatus.CONNECTING,
        errorMessage = null,
        statusMessage = "Connecting to ${selected.name}...",
      )
    }

    return try {
      transport.connect(selected.address)
      _state.update {
        it.copy(
          connectionStatus = PrinterConnectionStatus.CONNECTED,
          errorMessage = null,
          statusMessage = "Connected to ${selected.name}",
        )
      }
      Result.success(Unit)
    } catch (sec: SecurityException) {
      transport.close()
      failConnection("Bluetooth permission missing: ${sec.message ?: "Access denied"}")
    } catch (io: IOException) {
      transport.close()
      failConnection("Failed to connect to ${selected.name}: ${io.message ?: "Printer unreachable"}")
    } catch (e: Exception) {
      transport.close()
      failConnection("Printer connection error: ${e.message ?: "Unknown error"}")
    }
  }

  suspend fun disconnect() {
    withContext(ioDispatcher) {
      opMutex.withLock {
        transport.close()
        _state.update {
          it.copy(
            connectionStatus = PrinterConnectionStatus.DISCONNECTED,
            statusMessage = "Printer disconnected",
            errorMessage = null,
          )
        }
      }
    }
  }

  suspend fun printInvoice(
    business: BusinessProfile,
    invoice: Invoice,
    language: AppLanguage,
  ): Result<Unit> =
    withContext(ioDispatcher) {
      opMutex.withLock {
        val currentConfig = _state.value.selectedPrinter
        if (!currentConfig.hasPrinter) {
          return@withLock failConnection("Select a Bluetooth thermal printer first.")
        }

        if (!transport.isConnected || transport.connectedAddress != currentConfig.address) {
          val connectRes = connectSelectedInternal()
          if (connectRes.isFailure) return@withLock connectRes
        }

        _state.update {
          it.copy(
            connectionStatus = PrinterConnectionStatus.PRINTING,
            errorMessage = null,
            statusMessage = "Printing ${invoice.invoiceNumber}...",
          )
        }

        val logoBitmap =
          if (currentConfig.printLogoEnabled) InvoicePdfGenerator.loadLogoBitmap(business.logoPath)
          else null
        val qrBitmap =
          if (currentConfig.printUpiQrEnabled) UpiQrGenerator.generateQrBitmap(business, invoice, sizePx = 240)
          else null

        return@withLock try {
          val payload =
            EscPosFormatter.buildEscPosReceipt(
              business = business,
              invoice = invoice,
              paperWidth = currentConfig.paperWidth,
              language = language,
              logoBitmap = logoBitmap,
              upiQrBitmap = qrBitmap,
              autoCut = currentConfig.autoCutEnabled,
            )
          transport.writeAndFlush(payload)
          _state.update {
            it.copy(
              connectionStatus = PrinterConnectionStatus.CONNECTED,
              errorMessage = null,
              statusMessage = "Printed ${invoice.invoiceNumber} on ${currentConfig.name}",
            )
          }
          Result.success(Unit)
        } catch (io: IOException) {
          transport.close()
          failConnection("Printer disconnected during printing: ${io.message ?: "I/O error"}")
        } catch (sec: SecurityException) {
          transport.close()
          failConnection("Bluetooth permission error during printing.")
        } catch (e: Exception) {
          transport.close()
          failConnection("Thermal print failed: ${e.message ?: "Unknown error"}")
        } finally {
          logoBitmap?.recycle()
          qrBitmap?.recycle()
        }
      }
    }

  fun clearMessages() {
    _state.update { it.copy(errorMessage = null, statusMessage = null) }
  }

  fun close() {
    bridge.stopDiscovery()
    transport.close()
  }

  private fun failConnection(message: String): Result<Unit> {
    _state.update {
      it.copy(
        connectionStatus = PrinterConnectionStatus.ERROR,
        errorMessage = message,
        statusMessage = null,
      )
    }
    return Result.failure(IOException(message))
  }

  private fun setError(message: String) {
    _state.update { it.copy(errorMessage = message, statusMessage = null) }
  }

  private fun mergeDevices(
    existing: List<BluetoothPrinterDevice>,
    incoming: List<BluetoothPrinterDevice>,
  ): List<BluetoothPrinterDevice> {
    val map = LinkedHashMap<String, BluetoothPrinterDevice>()
    existing.forEach { if (it.address.isNotBlank()) map[it.address] = it }
    incoming.forEach { dev ->
      if (dev.address.isNotBlank()) {
        val prev = map[dev.address]
        map[dev.address] =
          if (prev == null) dev
          else prev.copy(
            name = if (dev.name != "Bluetooth Device") dev.name else prev.name,
            isBonded = prev.isBonded || dev.isBonded,
            isLikelyPrinter = prev.isLikelyPrinter || dev.isLikelyPrinter,
          )
      }
    }
    return map.values.sortedWith(
      compareByDescending<BluetoothPrinterDevice> { it.isLikelyPrinter }
        .thenByDescending { it.isBonded }
        .thenBy { it.name }
    )
  }
}
