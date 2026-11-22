package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfile
import com.example.model.Customer
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.Invoice
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.pdf.ReportExporter
import com.example.pdf.ShareAndPrintHelper
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils
import java.io.File
import java.util.Calendar

enum class ReportTab(val label: String) {
  SALES("Sales"),
  PRODUCTS("Products"),
  CUSTOMERS("Customers"),
  PROFIT_LOSS("P&L"),
  GST("GST"),
  DUES("Dues"),
}

enum class ReportPeriod(val label: String) {
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly"),
  YEARLY("Yearly"),
}

@Composable
fun ReportsScreen(
  business: BusinessProfile,
  invoices: List<Invoice>,
  products: List<Product>,
  customers: List<Customer>,
  expenses: List<Expense>,
  exportDir: File,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val sym = business.currencySymbol.ifBlank { "₹" }
  var selectedTab by remember { mutableStateOf(ReportTab.SALES) }
  var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
  var pendingPdfFile by remember { mutableStateOf<File?>(null) }
  var pendingCsvContent by remember { mutableStateOf<String?>(null) }

  val savePdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      val f = pendingPdfFile
      if (uri != null && f != null && f.exists()) {
        val ok = ShareAndPrintHelper.writeFileToSafUri(context, f, uri)
        onShowToast(if (ok) "Report PDF saved" else "Failed to save PDF")
      }
    }

  val saveCsvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      val csv = pendingCsvContent
      if (uri != null && csv != null) {
        val ok = ShareAndPrintHelper.writeTextToSafUri(context, csv, uri)
        onShowToast(if (ok) "Report CSV exported" else "Failed to export CSV")
      }
    }

  val periodStart =
    remember(selectedPeriod) {
      val cal = Calendar.getInstance()
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      when (selectedPeriod) {
        ReportPeriod.DAILY -> cal.timeInMillis
        ReportPeriod.WEEKLY -> {
          cal.add(Calendar.DAY_OF_YEAR, -6)
          cal.timeInMillis
        }
        ReportPeriod.MONTHLY -> {
          cal.set(Calendar.DAY_OF_MONTH, 1)
          cal.timeInMillis
        }
        ReportPeriod.YEARLY -> {
          cal.set(Calendar.DAY_OF_YEAR, 1)
          cal.timeInMillis
        }
      }
    }

  val periodInvoices = remember(invoices, periodStart) { invoices.filter { it.timestamp >= periodStart } }
  val periodExpenses = remember(expenses, periodStart) { expenses.filter { it.timestamp >= periodStart } }
  val totalSalesPaise = periodInvoices.sumOf { it.grandTotalPaise }
  val totalExpensesPaise = periodExpenses.sumOf { it.amountPaise }
  val netProfitPaise = totalSalesPaise - totalExpensesPaise
  val totalCgst = periodInvoices.sumOf { it.cgstPaise }
  val totalSgst = periodInvoices.sumOf { it.sgstPaise }
  val totalIgst = periodInvoices.sumOf { it.igstPaise }
  val totalTax = periodInvoices.sumOf { it.totalTaxPaise }
  val topProducts = remember(periodInvoices) { computeTopProducts(periodInvoices) }

  val chartRange =
    when (selectedPeriod) {
      ReportPeriod.DAILY, ReportPeriod.WEEKLY -> ChartRange.SEVEN_DAYS
      ReportPeriod.MONTHLY -> ChartRange.THIRTY_DAYS
      ReportPeriod.YEARLY -> ChartRange.ONE_YEAR
    }
  val chartPoints = remember(invoices, chartRange) { computeSalesSeries(invoices, chartRange) }

  fun buildCurrentReportPdf(): File {
    val summary =
      listOf(
        "Period" to selectedPeriod.label,
        "Total Sales" to CurrencyUtils.formatPaise(totalSalesPaise, sym, true),
        "Total Bills" to periodInvoices.size.toString(),
        "Total Expenses" to CurrencyUtils.formatPaise(totalExpensesPaise, sym, true),
        "Net Profit / Loss" to CurrencyUtils.formatPaise(netProfitPaise, sym, true),
        "Total GST Collected" to CurrencyUtils.formatPaise(totalTax, sym, true),
      )
    val (headers, rows) =
      when (selectedTab) {
        ReportTab.SALES ->
          listOf("Invoice", "Date", "Customer", "Mode", "Total") to
            periodInvoices.map {
              listOf(
                it.invoiceNumber,
                CurrencyUtils.formatDate(it.timestamp),
                it.customerName,
                it.paymentMethod.label,
                CurrencyUtils.formatPaise(it.grandTotalPaise, sym, true),
              )
            }
        ReportTab.PRODUCTS ->
          listOf("Product", "Qty Sold", "Revenue") to
            topProducts.map {
              listOf(
                it.name,
                CurrencyUtils.formatQty(it.qtySold),
                CurrencyUtils.formatPaise(it.revenuePaise, sym, true),
              )
            }
        ReportTab.CUSTOMERS ->
          listOf("Customer", "Mobile", "Total", "Due") to
            customers.map {
              listOf(
                it.name,
                it.mobile,
                CurrencyUtils.formatPaise(it.totalPurchasesPaise, sym, true),
                CurrencyUtils.formatPaise(it.outstandingBalancePaise, sym, true),
              )
            }
        ReportTab.PROFIT_LOSS ->
          listOf("Date", "Expense", "Category", "Amount") to
            periodExpenses.map {
              listOf(
                CurrencyUtils.formatDate(it.timestamp),
                it.title,
                it.category.label,
                CurrencyUtils.formatPaise(it.amountPaise, sym, true),
              )
            }
        ReportTab.GST ->
          listOf("Invoice", "Taxable", "CGST", "SGST", "IGST") to
            periodInvoices.map {
              listOf(
                it.invoiceNumber,
                CurrencyUtils.formatPaise(it.subtotalPaise - it.discountPaise, sym, true),
                CurrencyUtils.formatPaise(it.cgstPaise, sym, true),
                CurrencyUtils.formatPaise(it.sgstPaise, sym, true),
                CurrencyUtils.formatPaise(it.igstPaise, sym, true),
              )
            }
        ReportTab.DUES ->
          listOf("Customer", "Mobile", "Outstanding Due") to
            customers
              .filter { it.outstandingBalancePaise > 0 }
              .map {
                listOf(
                  it.name,
                  it.mobile,
                  CurrencyUtils.formatPaise(it.outstandingBalancePaise, sym, true),
                )
              }
      }
    return ReportExporter.exportReportPdf(
      outputDir = exportDir,
      title = "${selectedTab.label}_${selectedPeriod.label}_Report",
      business = business,
      summaryLines = summary,
      tableHeaders = headers,
      tableRows = rows,
    )
  }

  fun buildCurrentCsv(): String =
    when (selectedTab) {
      ReportTab.PRODUCTS -> ReportExporter.buildProductsCsv(products)
      ReportTab.CUSTOMERS, ReportTab.DUES -> ReportExporter.buildCustomersCsv(customers)
      ReportTab.PROFIT_LOSS -> ReportExporter.buildExpensesCsv(periodExpenses)
      ReportTab.SALES, ReportTab.GST -> ReportExporter.buildInvoicesCsv(periodInvoices, sym)
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Segmented Report Tabs (matching Screen 9)
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReportTab.entries.forEach { tab ->
            FilterChip(
              selected = selectedTab == tab,
              onClick = { selectedTab = tab },
              label = { Text(tab.label, fontSize = 12.sp) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SaffronOrange,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ReportPeriod.entries.forEach { p ->
            FilterChip(
              selected = selectedPeriod == p,
              onClick = { selectedPeriod = p },
              label = { Text(p.label, fontSize = 11.sp) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CharcoalHeader,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Export PDF & CSV Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = {
              val f = buildCurrentReportPdf()
              pendingPdfFile = f
              savePdfLauncher.launch(f.name)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
            modifier = Modifier.weight(1f).testTag("export_report_pdf"),
          ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = {
              val csv = buildCurrentCsv()
              pendingCsvContent = csv
              saveCsvLauncher.launch("VYAPAR_${selectedTab.label}_${selectedPeriod.label}.csv")
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("export_report_csv"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = {
              val f = buildCurrentReportPdf()
              ShareAndPrintHelper.sharePdfDocument(context, f, "Share ${selectedTab.label} Report")
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Share", fontSize = 12.sp)
          }
        }
      }

      when (selectedTab) {
        ReportTab.SALES -> {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  "Total Sales (${selectedPeriod.label})",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = CurrencyUtils.formatPaise(totalSalesPaise, sym),
                  style = MaterialTheme.typography.displaySmall,
                  fontWeight = FontWeight.ExtraBold,
                )
                Text(
                  text = "${periodInvoices.size} bill(s) recorded",
                  color = EmeraldGreen,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                SalesBarChart(
                  points = chartPoints,
                  barColor = EmeraldGreen,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.fillMaxWidth().height(145.dp),
                )
              }
            }
          }
          items(periodInvoices, key = { it.id }) { inv ->
            ReportRowCard(
              title = "${inv.invoiceNumber} • ${inv.customerName}",
              subtitle = "${CurrencyUtils.formatDateTime(inv.timestamp)} • ${inv.paymentMethod.label}",
              amount = CurrencyUtils.formatPaise(inv.grandTotalPaise, sym),
              amountColor = SaffronOrange,
            )
          }
        }
        ReportTab.PRODUCTS -> {
          if (topProducts.isEmpty()) {
            item {
              EmptyStatePanel(
                icon = Icons.Filled.Assessment,
                title = "No Product Sales in Period",
                subtitle = "Create bills to analyze best-selling products.",
              )
            }
          } else {
            items(topProducts) { stat ->
              ReportRowCard(
                title = stat.name,
                subtitle = "Qty Sold: ${CurrencyUtils.formatQty(stat.qtySold)}",
                amount = CurrencyUtils.formatPaise(stat.revenuePaise, sym),
                amountColor = EmeraldGreen,
              )
            }
          }
        }
        ReportTab.CUSTOMERS -> {
          items(customers.sortedByDescending { it.totalPurchasesPaise }, key = { it.id }) { c ->
            ReportRowCard(
              title = c.name,
              subtitle = "${c.mobile.ifBlank { "No phone" }} • Due: ${CurrencyUtils.formatPaise(c.outstandingBalancePaise, sym)}",
              amount = CurrencyUtils.formatPaise(c.totalPurchasesPaise, sym),
              amountColor = SaffronOrange,
            )
          }
        }
        ReportTab.PROFIT_LOSS -> {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profit & Loss (${selectedPeriod.label})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total Sales Revenue")
                  Text(CurrencyUtils.formatPaise(totalSalesPaise, sym, true), fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total Expenses")
                  Text("-${CurrencyUtils.formatPaise(totalExpensesPaise, sym, true)}", fontWeight = FontWeight.Bold, color = AlertRed)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Net Profit / Loss", fontWeight = FontWeight.Bold)
                  Text(
                    CurrencyUtils.formatPaise(netProfitPaise, sym, true),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (netProfitPaise >= 0) EmeraldGreen else AlertRed,
                  )
                }
              }
            }
          }
          items(periodExpenses, key = { it.id }) { exp ->
            ReportRowCard(
              title = exp.title,
              subtitle = "${exp.category.label} • ${CurrencyUtils.formatDate(exp.timestamp)}",
              amount = CurrencyUtils.formatPaise(exp.amountPaise, sym),
              amountColor = AlertRed,
            )
          }
        }
        ReportTab.GST -> {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GST Summary (${selectedPeriod.label})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("CGST Collected")
                  Text(CurrencyUtils.formatPaise(totalCgst, sym, true), fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("SGST Collected")
                  Text(CurrencyUtils.formatPaise(totalSgst, sym, true), fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("IGST Collected")
                  Text(CurrencyUtils.formatPaise(totalIgst, sym, true), fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total GST Liability", fontWeight = FontWeight.Bold)
                  Text(CurrencyUtils.formatPaise(totalTax, sym, true), fontWeight = FontWeight.ExtraBold, color = SaffronOrange)
                }
              }
            }
          }
        }
        ReportTab.DUES -> {
          val dueCustomers = customers.filter { it.outstandingBalancePaise > 0 }
          val walkInDueInvoices = invoices.filter { it.customerId == null && it.balanceDuePaise > 0 }
          if (dueCustomers.isEmpty() && walkInDueInvoices.isEmpty()) {
            item {
              EmptyStatePanel(
                icon = Icons.Filled.Assessment,
                title = "Zero Outstanding Dues",
                subtitle = "All customer payments are settled.",
              )
            }
          } else {
            items(dueCustomers, key = { it.id }) { c ->
              ReportRowCard(
                title = c.name,
                subtitle = c.mobile.ifBlank { "Credit Balance" },
                amount = CurrencyUtils.formatPaise(c.outstandingBalancePaise, sym),
                amountColor = AlertRed,
              )
            }
            items(walkInDueInvoices, key = { it.id }) { inv ->
              ReportRowCard(
                title = "${inv.invoiceNumber} • ${inv.customerName}",
                subtitle = CurrencyUtils.formatDate(inv.timestamp),
                amount = CurrencyUtils.formatPaise(inv.balanceDuePaise, sym),
                amountColor = AlertRed,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReportRowCard(title: String, subtitle: String, amount: String, amountColor: Color) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = amountColor)
    }
  }
}

@Composable
fun ExpensesScreen(
  expenses: List<Expense>,
  currencySymbol: String,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onUpsertExpense: (Expense) -> Unit,
  onDeleteExpense: (String) -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedCat by remember { mutableStateOf<ExpenseCategory?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  val filtered =
    remember(expenses, selectedCat) {
      if (selectedCat == null) expenses else expenses.filter { it.category == selectedCat }
    }
  val totalPaise = filtered.sumOf { it.amountPaise }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Expenses",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = SaffronOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_expense_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add Expense")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
          Text(
            text = "Total Expenses: ${CurrencyUtils.formatPaise(totalPaise, currencySymbol)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlertRed,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedCat == null,
              onClick = { selectedCat = null },
              label = { Text("All") },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SaffronOrange,
                  selectedLabelColor = Color.White,
                ),
            )
            ExpenseCategory.entries.forEach { cat ->
              FilterChip(
                selected = selectedCat == cat,
                onClick = { selectedCat = cat },
                label = { Text(cat.label, fontSize = 12.sp) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CharcoalHeader,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
        }
      }

      if (filtered.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.AccountBalanceWallet,
          title = "No Expenses Recorded",
          subtitle = "Track Rent, Electricity, Salary, Transport, Stock Purchase & Maintenance.",
          actionLabel = "Add Expense",
          onAction = { showAddDialog = true },
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { exp ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = SaffronOrangeSoft,
                  modifier = Modifier.size(40.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = SaffronOrange)
                  }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(exp.title, fontWeight = FontWeight.Bold)
                  Text(
                    "${exp.category.label} • ${exp.paymentMethod.label} • ${CurrencyUtils.formatDate(exp.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  CurrencyUtils.formatPaise(exp.amountPaise, currencySymbol),
                  fontWeight = FontWeight.Bold,
                  color = AlertRed,
                )
                IconButton(onClick = { onDeleteExpense(exp.id) }) {
                  Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Expense", tint = AlertRed)
                }
              }
            }
          }
          item { Spacer(Modifier.height(72.dp)) }
        }
      }
    }
  }

  if (showAddDialog) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

    VyaparFormDialog(
      title = "Add Expense",
      onDismiss = { showAddDialog = false },
      scrollContent = {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Expense Title *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amount,
          onValueChange = { amount = it },
          label = { Text("Amount ($currencySymbol) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Category", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.label, fontSize = 11.sp) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SaffronOrange,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD).forEach { m ->
            FilterChip(
              selected = method == m,
              onClick = { method = m },
              label = { Text(m.label, fontSize = 11.sp) },
            )
          }
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      bottomButtons = {
        TextButton(
          onClick = { showAddDialog = false },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Cancel")
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onUpsertExpense(
                Expense(
                  id = "",
                  title = title.trim(),
                  category = category,
                  amountPaise = CurrencyUtils.rupeesToPaise(amount),
                  paymentMethod = method,
                  notes = notes.trim(),
                )
              )
              showAddDialog = false
            }
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}
