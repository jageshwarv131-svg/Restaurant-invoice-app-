package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.AppSettings
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Expense
import com.example.model.FullBackupBundle
import com.example.model.InventoryMovement
import com.example.model.Invoice
import com.example.model.Order
import com.example.model.Product
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JsonStorageManager(private val context: Context) {

  val rootDir: File by lazy { File(context.filesDir, "VyaparBilling").apply { mkdirs() } }
  val backupsDir: File by lazy { File(rootDir, "backups").apply { mkdirs() } }
  val invoicesDir: File by lazy { File(rootDir, "invoices").apply { mkdirs() } }
  val logosDir: File by lazy { File(rootDir, "logos").apply { mkdirs() } }

  val businessFile: File get() = File(rootDir, "business.json")
  val productsFile: File get() = File(rootDir, "products.json")
  val customersFile: File get() = File(rootDir, "customers.json")
  val invoicesFile: File get() = File(rootDir, "invoices.json")
  val ordersFile: File get() = File(rootDir, "orders.json")
  val expensesFile: File get() = File(rootDir, "expenses.json")
  val inventoryFile: File get() = File(rootDir, "inventory.json")
  val settingsFile: File get() = File(rootDir, "settings.json")

  private val ioMutex = Mutex()
  val moshi: Moshi = Moshi.Builder().build()

  private val businessAdapter: JsonAdapter<BusinessProfile> =
    moshi.adapter(BusinessProfile::class.java).indent("  ")
  private val settingsAdapter: JsonAdapter<AppSettings> =
    moshi.adapter(AppSettings::class.java).indent("  ")
  private val productsAdapter: JsonAdapter<List<Product>> =
    moshi.adapter<List<Product>>(Types.newParameterizedType(List::class.java, Product::class.java))
      .indent("  ")
  private val customersAdapter: JsonAdapter<List<Customer>> =
    moshi
      .adapter<List<Customer>>(Types.newParameterizedType(List::class.java, Customer::class.java))
      .indent("  ")
  private val invoicesAdapter: JsonAdapter<List<Invoice>> =
    moshi.adapter<List<Invoice>>(Types.newParameterizedType(List::class.java, Invoice::class.java))
      .indent("  ")
  private val ordersAdapter: JsonAdapter<List<Order>> =
    moshi.adapter<List<Order>>(Types.newParameterizedType(List::class.java, Order::class.java))
      .indent("  ")
  private val expensesAdapter: JsonAdapter<List<Expense>> =
    moshi.adapter<List<Expense>>(Types.newParameterizedType(List::class.java, Expense::class.java))
      .indent("  ")
  private val inventoryAdapter: JsonAdapter<List<InventoryMovement>> =
    moshi
      .adapter<List<InventoryMovement>>(
        Types.newParameterizedType(List::class.java, InventoryMovement::class.java)
      )
      .indent("  ")
  val backupBundleAdapter: JsonAdapter<FullBackupBundle> =
    moshi.adapter(FullBackupBundle::class.java).indent("  ")

  suspend fun readBusiness(): BusinessProfile =
    readSafe(businessFile, businessAdapter) { BusinessProfile() }

  suspend fun writeBusiness(profile: BusinessProfile) =
    writeAtomic(businessFile, businessAdapter, profile)

  suspend fun readProducts(): List<Product> = readSafe(productsFile, productsAdapter) { emptyList() }

  suspend fun writeProducts(items: List<Product>) =
    writeAtomic(productsFile, productsAdapter, items)

  suspend fun readCustomers(): List<Customer> =
    readSafe(customersFile, customersAdapter) { emptyList() }

  suspend fun writeCustomers(items: List<Customer>) =
    writeAtomic(customersFile, customersAdapter, items)

  suspend fun readInvoices(): List<Invoice> = readSafe(invoicesFile, invoicesAdapter) { emptyList() }

  suspend fun writeInvoices(items: List<Invoice>) =
    writeAtomic(invoicesFile, invoicesAdapter, items)

  suspend fun readOrders(): List<Order> = readSafe(ordersFile, ordersAdapter) { emptyList() }

  suspend fun writeOrders(items: List<Order>) = writeAtomic(ordersFile, ordersAdapter, items)

  suspend fun readExpenses(): List<Expense> = readSafe(expensesFile, expensesAdapter) { emptyList() }

  suspend fun writeExpenses(items: List<Expense>) =
    writeAtomic(expensesFile, expensesAdapter, items)

  suspend fun readInventoryMovements(): List<InventoryMovement> =
    readSafe(inventoryFile, inventoryAdapter) { emptyList() }

  suspend fun writeInventoryMovements(items: List<InventoryMovement>) =
    writeAtomic(inventoryFile, inventoryAdapter, items)

  suspend fun readSettings(): AppSettings = readSafe(settingsFile, settingsAdapter) { AppSettings() }

  suspend fun writeSettings(settings: AppSettings) =
    writeAtomic(settingsFile, settingsAdapter, settings)

  suspend fun exportFullBundleJson(bundle: FullBackupBundle): String =
    withContext(Dispatchers.IO) { backupBundleAdapter.toJson(bundle) }

  suspend fun parseFullBundleJson(jsonText: String): Result<FullBackupBundle> =
    withContext(Dispatchers.IO) {
      runCatching {
        val parsed =
          backupBundleAdapter.fromJson(jsonText)
            ?: throw IllegalArgumentException("Backup JSON is empty or invalid.")
        if (parsed.schemaVersion <= 0 || parsed.appName.isBlank()) {
          throw IllegalArgumentException("Unrecognized Vyapar Billing Pro backup format.")
        }
        parsed
      }
    }

  suspend fun exportSingleJson(type: String): String =
    withContext(Dispatchers.IO) {
      when (type) {
        "business" -> businessAdapter.toJson(readBusiness())
        "products" -> productsAdapter.toJson(readProducts())
        "customers" -> customersAdapter.toJson(readCustomers())
        "invoices" -> invoicesAdapter.toJson(readInvoices())
        "orders" -> ordersAdapter.toJson(readOrders())
        "expenses" -> expensesAdapter.toJson(readExpenses())
        "inventory" -> inventoryAdapter.toJson(readInventoryMovements())
        "settings" -> settingsAdapter.toJson(readSettings())
        else -> "{}"
      }
    }

  suspend fun createPreRestoreSnapshot(): File =
    withContext(Dispatchers.IO) {
      ioMutex.withLock {
        val snapshot =
          FullBackupBundle(
            business = readUnlocked(businessFile, businessAdapter) { BusinessProfile() },
            products = readUnlocked(productsFile, productsAdapter) { emptyList() },
            customers = readUnlocked(customersFile, customersAdapter) { emptyList() },
            invoices = readUnlocked(invoicesFile, invoicesAdapter) { emptyList() },
            orders = readUnlocked(ordersFile, ordersAdapter) { emptyList() },
            expenses = readUnlocked(expensesFile, expensesAdapter) { emptyList() },
            inventoryMovements =
              readUnlocked(inventoryFile, inventoryAdapter) { emptyList() },
            settings = readUnlocked(settingsFile, settingsAdapter) { AppSettings() },
          )
        val file = File(backupsDir, "pre_restore_${System.currentTimeMillis()}.json")
        writeTextAtomic(file, backupBundleAdapter.toJson(snapshot))
        file
      }
    }

  suspend fun copyPickedImageToInternal(uri: Uri, prefix: String): String? =
    withContext(Dispatchers.IO) {
      runCatching {
          val destFile = File(logosDir, "${prefix}_${UUID.randomUUID().toString().take(8)}.jpg")
          context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
          } ?: return@runCatching null
          destFile.absolutePath
        }
        .getOrNull()
    }

  private suspend fun <T> readSafe(
    file: File,
    adapter: JsonAdapter<T>,
    defaultValue: () -> T,
  ): T =
    withContext(Dispatchers.IO) {
      ioMutex.withLock { readUnlocked(file, adapter, defaultValue) }
    }

  private fun <T> readUnlocked(
    file: File,
    adapter: JsonAdapter<T>,
    defaultValue: () -> T,
  ): T {
    if (file.exists() && file.length() > 0) {
      val primary = runCatching { adapter.fromJson(file.readText(Charsets.UTF_8)) }.getOrNull()
      if (primary != null) return primary
    }
    val backupFile = File(backupsDir, "${file.name}.bak")
    if (backupFile.exists() && backupFile.length() > 0) {
      val recovered =
        runCatching { adapter.fromJson(backupFile.readText(Charsets.UTF_8)) }.getOrNull()
      if (recovered != null) return recovered
    }
    return defaultValue()
  }

  private suspend fun <T> writeAtomic(file: File, adapter: JsonAdapter<T>, data: T) {
    withContext(Dispatchers.IO) {
      ioMutex.withLock {
        rootDir.mkdirs()
        backupsDir.mkdirs()
        val json = adapter.toJson(data)
        if (file.exists() && file.length() > 0) {
          val validExisting = runCatching { adapter.fromJson(file.readText(Charsets.UTF_8)) }.isSuccess
          if (validExisting) {
            val backupFile = File(backupsDir, "${file.name}.bak")
            runCatching {
              Files.copy(
                file.toPath(),
                backupFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
              )
            }
          }
        }
        writeTextAtomic(file, json)
      }
    }
  }

  private fun writeTextAtomic(target: File, content: String) {
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, "${target.name}.tmp")
    FileOutputStream(tmp).use { fos ->
      fos.write(content.toByteArray(Charsets.UTF_8))
      fos.fd.sync()
    }
    try {
      Files.move(
        tmp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE,
      )
    } catch (_: Exception) {
      Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }
}
