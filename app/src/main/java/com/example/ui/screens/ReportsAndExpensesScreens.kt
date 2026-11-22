package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayBarChart
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
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
  val palette = LocalVyaparClayPalette.current
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

  Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
    // 3D Raised Segmented Tabs Card
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
      Card3D(
        cornerRadius = 22.dp,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(vertical = 10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReportTab.entries.forEach { tab ->
            Chip3D(
              selected = selectedTab == tab,
              onClick = { selectedTab = tab },
              label = tab.label,
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReportPeriod.entries.forEach { p ->
            Chip3D(
              selected = selectedPeriod == p,
              onClick = { selectedPeriod = p },
              label = p.label,
              selectedTone = ClayTones.Blue,
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Export PDF, CSV & Share 3D Buttons
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button3D(
            onClick = {
              val f = buildCurrentReportPdf()
              pendingPdfFile = f
              savePdfLauncher.launch(f.name)
            },
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).testTag("export_report_pdf"),
          ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
          }
          Button3D(
            onClick = {
              val csv = buildCurrentCsv()
              pendingCsvContent = csv
              saveCsvLauncher.launch("VYAPAR_${selectedTab.label}_${selectedPeriod.label}.csv")
            },
            isPrimary = false,
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).testTag("export_report_csv"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Button3D(
            onClick = {
              val f = buildCurrentReportPdf()
              ShareAndPrintHelper.sharePdfDocument(context, f, "Share ${selectedTab.label} Report")
            },
            isPrimary = false,
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      when (selectedTab) {
        ReportTab.SALES -> {
          item {
            Card3D(
              cornerRadius = 24.dp,
              shadowDepth = 5.dp,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(16.dp),
            ) {
              Text(
                "Total Sales (${selectedPeriod.label})",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = CurrencyUtils.formatPaise(totalSalesPaise, sym),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
              )
              Text(
                text = "${periodInvoices.size} bill(s) recorded",
                color = EmeraldGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
              Spacer(Modifier.height(14.dp))
              ClayBarChart(
                points = chartPoints,
                tone = ClayTones.Green,
                labelColor = palette.textSecondary,
                modifier = Modifier.fillMaxWidth().height(150.dp),
              )
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
            Card3D(
              cornerRadius = 24.dp,
              shadowDepth = 5.dp,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(16.dp),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Profit & Loss (${selectedPeriod.label})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = palette.textPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total Sales Revenue", color = palette.textSecondary)
                  Text(CurrencyUtils.formatPaise(totalSalesPaise, sym, true), fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total Expenses", color = palette.textSecondary)
                  Text("-${CurrencyUtils.formatPaise(totalExpensesPaise, sym, true)}", fontWeight = FontWeight.Bold, color = AlertRed)
                }
                HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.4f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Net Profit / Loss", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
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
            Card3D(
              cornerRadius = 24.dp,
              shadowDepth = 5.dp,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(16.dp),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GST Summary (${selectedPeriod.label})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = palette.textPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("CGST Collected", color = palette.textSecondary)
                  Text(CurrencyUtils.formatPaise(totalCgst, sym, true), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("SGST Collected", color = palette.textSecondary)
                  Text(CurrencyUtils.formatPaise(totalSgst, sym, true), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("IGST Collected", color = palette.textSecondary)
                  Text(CurrencyUtils.formatPaise(totalIgst, sym, true), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                }
                HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.4f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Total GST Liability", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
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
  val palette = LocalVyaparClayPalette.current
  Card3D(
    cornerRadius = 18.dp,
    shadowDepth = 3.dp,
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      }
      Text(amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = amountColor)
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

  val palette = LocalVyaparClayPalette.current
  var selectedCat by remember { mutableStateOf<ExpenseCategory?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  val filtered =
    remember(expenses, selectedCat) {
      if (selectedCat == null) expenses else expenses.filter { it.category == selectedCat }
    }
  val totalPaise = filtered.sumOf { it.amountPaise }

  Scaffold(
    containerColor = palette.background,
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
      val fabInteraction = remember { MutableInteractionSource() }
      val fabPressed by fabInteraction.collectIsPressedAsState()
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(58.dp)
            .raised3D(
              cornerRadius = 20.dp,
              isPressed = fabPressed,
              tone = ClayTones.Orange,
              shadowDepth = 6.dp,
            )
            .clickable(
              interactionSource = fabInteraction,
              indication = null,
              onClick = { showAddDialog = true },
            )
            .testTag("add_expense_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add Expense", tint = Color.White, modifier = Modifier.size(28.dp))
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
    ) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          contentPadding = PaddingValues(vertical = 12.dp),
        ) {
          Text(
            text = "Total Expenses: ${CurrencyUtils.formatPaise(totalPaise, currencySymbol)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AlertRed,
            modifier = Modifier.padding(horizontal = 14.dp),
          )
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Chip3D(
              selected = selectedCat == null,
              onClick = { selectedCat = null },
              label = "All",
            )
            ExpenseCategory.entries.forEach { cat ->
              Chip3D(
                selected = selectedCat == cat,
                onClick = { selectedCat = cat },
                label = cat.label,
                selectedTone = ClayTones.Purple,
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
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { exp ->
            Card3D(
              cornerRadius = 20.dp,
              shadowDepth = 4.dp,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon3D(
                  imageVector = Icons.Filled.AccountBalanceWallet,
                  contentDescription = null,
                  tone = ClayTones.Purple,
                  size = 32.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(exp.title, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                  Text(
                    "${exp.category.label} • ${exp.paymentMethod.label} • ${CurrencyUtils.formatDate(exp.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                Text(
                  CurrencyUtils.formatPaise(exp.amountPaise, currencySymbol),
                  fontWeight = FontWeight.ExtraBold,
                  color = AlertRed,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(34.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { onDeleteExpense(exp.id) },
                ) {
                  Icon3D(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Expense",
                    tone = ClayTones.Red,
                    size = 20.dp,
                  )
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
        TextField3D(
          value = title,
          onValueChange = { title = it },
          label = "Expense Title *",
          modifier = Modifier.fillMaxWidth(),
        )
        TextField3D(
          value = amount,
          onValueChange = { amount = it },
          label = "Amount ($currencySymbol) *",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Category", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ExpenseCategory.entries.forEach { cat ->
            Chip3D(
              selected = category == cat,
              onClick = { category = cat },
              label = cat.label,
            )
          }
        }
        Text("Payment Mode", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD).forEach { m ->
            Chip3D(
              selected = method == m,
              onClick = { method = m },
              label = m.label,
              selectedTone = ClayTones.Green,
            )
          }
        }
        TextField3D(
          value = notes,
          onValueChange = { notes = it },
          label = "Notes",
          modifier = Modifier.fillMaxWidth(),
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { showAddDialog = false },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
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
          cornerRadius = 16.dp,
        ) {
          Text("Save", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      },
    )
  }
}
