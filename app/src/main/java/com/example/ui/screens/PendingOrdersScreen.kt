package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.BillingCalculator
import com.example.data.CartLineInput
import com.example.model.Customer
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedSoft
import com.example.ui.theme.AmberGoldSoft
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenSoft
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils

enum class OrderFilterTab(val label: String) {
  PENDING("Pending"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
}

private data class EditableOrderLine(
  val productId: String?,
  val name: String,
  val unit: String,
  val gstPercent: Double,
  val rateInput: String,
  val qtyInput: String,
)

@Composable
fun OrdersScreen(
  orders: List<Order>,
  products: List<Product>,
  customers: List<Customer>,
  invoices: List<Invoice>,
  defaultTaxType: GstTaxType,
  defaultTaxMode: GstTaxMode,
  autoRoundOff: Boolean,
  currencySymbol: String,
  isShopOpen: Boolean,
  openNewOrderImmediately: Boolean = false,
  onConsumeOpenNewOrder: () -> Unit = {},
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onCreateInlineCustomer: (name: String, mobile: String, onSaved: (Customer) -> Unit) -> Unit,
  onCreateOrder: (Customer?, String, String, List<CartLineInput>, String) -> Unit,
  onUpdatePendingOrder: (String, Customer?, String, String, List<CartLineInput>, Double, Boolean, String) -> Unit,
  onCancelOrder: (String) -> Unit,
  onConfirmOrder: (
    order: Order,
    finalItems: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    paymentMethod: PaymentMethod,
    amountPaidPaise: Long,
    notes: String,
  ) -> Unit,
  onOpenInvoicePreview: (Invoice) -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedTab by remember { mutableStateOf(OrderFilterTab.PENDING) }
  var searchQuery by remember { mutableStateOf("") }
  var showNewOrderDialog by remember { mutableStateOf(openNewOrderImmediately) }
  var editingOrder by remember { mutableStateOf<Order?>(null) }
  var settlingOrder by remember { mutableStateOf<Order?>(null) }
  var viewingHistoryOrder by remember { mutableStateOf<Order?>(null) }

  if (openNewOrderImmediately) {
    onConsumeOpenNewOrder()
  }

  val pendingCount =
    remember(orders) {
      orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.PROCESSING }
    }
  val completedCount = remember(orders) { orders.count { it.status == OrderStatus.COMPLETED } }
  val cancelledCount =
    remember(orders) {
      orders.count { it.status == OrderStatus.CANCELLED || it.status == OrderStatus.REFUNDED }
    }

  val filtered =
    remember(orders, selectedTab, searchQuery) {
      val byTab =
        orders.filter { ord ->
          when (selectedTab) {
            OrderFilterTab.PENDING ->
              ord.status == OrderStatus.PENDING || ord.status == OrderStatus.PROCESSING
            OrderFilterTab.COMPLETED -> ord.status == OrderStatus.COMPLETED
            OrderFilterTab.CANCELLED ->
              ord.status == OrderStatus.CANCELLED || ord.status == OrderStatus.REFUNDED
          }
        }
      val q = searchQuery.trim()
      if (q.isEmpty()) byTab
      else
        byTab.filter {
          it.customerName.contains(q, ignoreCase = true) ||
            it.customerMobile.contains(q, ignoreCase = true) ||
            it.orderNumber.contains(q, ignoreCase = true)
        }
    }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Pending Orders",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showNewOrderDialog = true },
        containerColor = SaffronOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("new_order_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New Order")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      // Search & Filter Tabs (Pending | Completed | Cancelled)
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Customer Name, Mobile or Order #...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("orders_search_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OrderFilterTab.entries.forEach { tab ->
              val count =
                when (tab) {
                  OrderFilterTab.PENDING -> pendingCount
                  OrderFilterTab.COMPLETED -> completedCount
                  OrderFilterTab.CANCELLED -> cancelledCount
                }
              val selected = selectedTab == tab
              val selectedColor =
                when (tab) {
                  OrderFilterTab.PENDING -> SaffronOrange
                  OrderFilterTab.COMPLETED -> EmeraldGreen
                  OrderFilterTab.CANCELLED -> AlertRed
                }
              FilterChip(
                selected = selected,
                onClick = { selectedTab = tab },
                label = { Text("${tab.label} ($count)", fontWeight = FontWeight.SemiBold) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor,
                    selectedLabelColor = Color.White,
                  ),
                modifier = Modifier.testTag("orders_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }
      }

      if (filtered.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.LocalShipping,
          title =
            when (selectedTab) {
              OrderFilterTab.PENDING -> "No Pending Orders"
              OrderFilterTab.COMPLETED -> "No Completed Orders"
              OrderFilterTab.CANCELLED -> "No Cancelled Orders"
            },
          subtitle =
            when (selectedTab) {
              OrderFilterTab.PENDING ->
                "Tap + to take a customer order now and finalize price & bill later."
              OrderFilterTab.COMPLETED ->
                "Confirmed orders converted into bills stay recorded here."
              OrderFilterTab.CANCELLED ->
                "Cancelled orders stay archived here for reference."
            },
          actionLabel = if (selectedTab == OrderFilterTab.PENDING) "New Order" else null,
          onAction = if (selectedTab == OrderFilterTab.PENDING) ({ showNewOrderDialog = true }) else null,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { order ->
            val linkedInvoice =
              order.linkedInvoiceId?.let { invId -> invoices.firstOrNull { it.id == invId } }
            val statusBg =
              when (order.status) {
                OrderStatus.COMPLETED -> EmeraldGreenSoft
                OrderStatus.CANCELLED, OrderStatus.REFUNDED -> AlertRedSoft
                OrderStatus.PENDING, OrderStatus.PROCESSING -> AmberGoldSoft
              }
            val statusFg =
              when (order.status) {
                OrderStatus.COMPLETED -> EmeraldGreenDark
                OrderStatus.CANCELLED, OrderStatus.REFUNDED -> AlertRed
                OrderStatus.PENDING, OrderStatus.PROCESSING -> SaffronOrangeDark
              }

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              shadowElevation = 2.dp,
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    if (order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING) {
                      settlingOrder = order
                    } else {
                      viewingHistoryOrder = order
                    }
                  }
                  .testTag("order_card_${order.orderNumber}"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                // Row 1: Order No + Status Badge + Estimated/Final Total
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = order.orderNumber,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.ExtraBold,
                    )
                    Surface(shape = RoundedCornerShape(50), color = statusBg) {
                      Text(
                        text = order.status.label,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = if (order.status == OrderStatus.COMPLETED) "Final Total" else "Est. Total",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = CurrencyUtils.formatPaise(order.totalPaise, currencySymbol),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = SaffronOrangeDark,
                    )
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Table columns grid: Customer Name, Mobile | Items Count, Date/Time
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Customer",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = order.customerName.ifBlank { "Walk-in Customer" },
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = order.customerMobile.ifBlank { "No mobile" },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(Modifier.width(10.dp))
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "${order.items.size} item(s)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = CurrencyUtils.formatDateTime(order.timestamp),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                if (order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = { editingOrder = order },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                      Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                      onClick = { settlingOrder = order },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                      modifier = Modifier.weight(1.5f).height(40.dp),
                    ) {
                      Icon(Icons.Filled.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("Bill & Confirm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                } else if (order.status == OrderStatus.COMPLETED && linkedInvoice != null) {
                  OutlinedButton(
                    onClick = { onOpenInvoicePreview(linkedInvoice) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SaffronOrange),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                  ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                      text = "View Bill (${linkedInvoice.invoiceNumber})",
                      color = SaffronOrange,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
          item { Spacer(Modifier.height(76.dp)) }
        }
      }
    }
  }

  // Create New Order OR Edit Pending Order Dialog
  if (showNewOrderDialog || editingOrder != null) {
    OrderEditorDialog(
      initialOrder = editingOrder,
      products = products,
      customers = customers,
      defaultTaxType = defaultTaxType,
      defaultTaxMode = defaultTaxMode,
      autoRoundOff = autoRoundOff,
      currencySymbol = currencySymbol,
      onDismiss = {
        showNewOrderDialog = false
        editingOrder = null
      },
      onCreateInlineCustomer = onCreateInlineCustomer,
      onSave = { selectedCust, custName, custMobile, lines, notes ->
        val existing = editingOrder
        if (existing == null) {
          onCreateOrder(selectedCust, custName, custMobile, lines, notes)
          selectedTab = OrderFilterTab.PENDING
        } else {
          onUpdatePendingOrder(
            existing.id,
            selectedCust,
            custName,
            custMobile,
            lines,
            existing.discountInput,
            existing.isPercentageDiscount,
            notes,
          )
        }
        showNewOrderDialog = false
        editingOrder = null
      },
    )
  }

  // Settle / Finalize Price / Confirm / Cancel Pending Order Dialog
  settlingOrder?.let { targetOrder ->
    PendingOrderSettleDialog(
      order = targetOrder,
      defaultTaxType = defaultTaxType,
      defaultTaxMode = defaultTaxMode,
      autoRoundOff = autoRoundOff,
      currencySymbol = currencySymbol,
      onDismiss = { settlingOrder = null },
      onEditFullOrder = {
        settlingOrder = null
        editingOrder = targetOrder
      },
      onCancelOrder = {
        onCancelOrder(targetOrder.id)
        settlingOrder = null
      },
      onConfirm = { finalItems, discountVal, isPct, taxType, taxMode, method, paidPaise, notes ->
        settlingOrder = null
        onConfirmOrder(
          targetOrder,
          finalItems,
          discountVal,
          isPct,
          taxType,
          taxMode,
          method,
          paidPaise,
          notes,
        )
      },
    )
  }

  // Completed / Cancelled Order Details Viewer
  viewingHistoryOrder?.let { order ->
    val linkedInvoice =
      order.linkedInvoiceId?.let { id -> invoices.firstOrNull { it.id == id } }
    VyaparFormDialog(
      title = "${order.orderNumber} • ${order.status.label}",
      onDismiss = { viewingHistoryOrder = null },
      scrollContent = {
        Text(
          text = "Customer: ${order.customerName}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        if (order.customerMobile.isNotBlank()) {
          Text(
            text = "Mobile: ${order.customerMobile}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "Date: ${CurrencyUtils.formatDateTime(order.timestamp)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text(
                "${CurrencyUtils.formatQty(item.quantity)} ${item.unit} × ${CurrencyUtils.formatPaise(item.unitPricePaise, currencySymbol)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              CurrencyUtils.formatPaise(item.lineTotalPaise, currencySymbol),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            )
          }
        }
        HorizontalDivider()
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Total", fontWeight = FontWeight.Bold)
          Text(
            CurrencyUtils.formatPaise(order.totalPaise, currencySymbol),
            fontWeight = FontWeight.ExtraBold,
            color = SaffronOrangeDark,
          )
        }
        if (order.notes.isNotBlank()) {
          Text(
            "Notes: ${order.notes}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      bottomButtons = {
        TextButton(
          onClick = { viewingHistoryOrder = null },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Close")
        }
        if (linkedInvoice != null) {
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              viewingHistoryOrder = null
              onOpenInvoicePreview(linkedInvoice)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text("View Invoice", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      },
    )
  }
}

@Composable
private fun OrderEditorDialog(
  initialOrder: Order?,
  products: List<Product>,
  customers: List<Customer>,
  defaultTaxType: GstTaxType,
  defaultTaxMode: GstTaxMode,
  autoRoundOff: Boolean,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onCreateInlineCustomer: (name: String, mobile: String, onSaved: (Customer) -> Unit) -> Unit,
  onSave: (Customer?, String, String, List<CartLineInput>, String) -> Unit,
) {
  var selectedCustomer by remember(initialOrder?.id) {
    mutableStateOf(
      initialOrder?.customerId?.let { cid -> customers.firstOrNull { it.id == cid } }
    )
  }
  var customerName by remember(initialOrder?.id) {
    mutableStateOf(initialOrder?.customerName?.takeIf { it != "Walk-in Customer" } ?: "")
  }
  var customerMobile by remember(initialOrder?.id) {
    mutableStateOf(
      initialOrder?.customerMobile?.filter { it.isDigit() }?.takeLast(10) ?: ""
    )
  }
  var showCustomerPicker by remember { mutableStateOf(false) }
  var customerSearch by remember { mutableStateOf("") }
  var quickNewCustName by remember { mutableStateOf("") }
  var quickNewCustMobile by remember { mutableStateOf("") }
  var quickCustError by remember { mutableStateOf<String?>(null) }

  var productSearch by remember { mutableStateOf("") }
  val selectedItems = remember(initialOrder?.id) {
    mutableStateListOf<CartLineInput>().apply {
      initialOrder?.items?.forEach {
        add(
          CartLineInput(
            productId = it.productId,
            name = it.name,
            unit = it.unit,
            quantity = it.quantity,
            unitPricePaise = it.unitPricePaise,
            gstPercent = it.gstPercent,
          )
        )
      }
    }
  }
  var notes by remember(initialOrder?.id) { mutableStateOf(initialOrder?.notes ?: "") }
  var nameError by remember { mutableStateOf(false) }
  var mobileError by remember { mutableStateOf(false) }
  var itemsError by remember { mutableStateOf(false) }

  val filteredProducts =
    remember(products, productSearch) {
      val q = productSearch.trim()
      if (q.isEmpty()) products
      else products.filter { it.name.contains(q, ignoreCase = true) || it.sku.contains(q, ignoreCase = true) }
    }

  val filteredCustomers =
    remember(customers, customerSearch) {
      val q = customerSearch.trim()
      if (q.isEmpty()) customers
      else customers.filter { it.name.contains(q, ignoreCase = true) || it.mobile.contains(q, ignoreCase = true) }
    }

  val calc =
    BillingCalculator.calculate(
      items = selectedItems.toList(),
      discountValue = initialOrder?.discountInput ?: 0.0,
      isPercentageDiscount = initialOrder?.isPercentageDiscount ?: false,
      taxType = defaultTaxType,
      taxMode = defaultTaxMode,
      autoRoundOff = autoRoundOff,
    )

  VyaparFormDialog(
    title = if (initialOrder == null) "New Pending Order" else "Edit ${initialOrder.orderNumber}",
    onDismiss = onDismiss,
    scrollContent = {
      // Customer Header + Pick / Inline Add toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Person, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Customer Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = { showCustomerPicker = !showCustomerPicker },
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, SaffronOrange),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp).testTag("order_pick_customer_btn"),
        ) {
          Text(
            text = if (showCustomerPicker) "Hide List" else "Pick / Add Customer",
            color = SaffronOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (showCustomerPicker) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.background,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = customerSearch,
              onValueChange = { customerSearch = it },
              placeholder = { Text("Search saved customers...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            if (filteredCustomers.isEmpty()) {
              Text(
                "No saved customers match.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filteredCustomers.take(5).forEach { cust ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border =
                      if (selectedCustomer?.id == cust.id) BorderStroke(1.dp, SaffronOrange)
                      else null,
                    modifier =
                      Modifier.fillMaxWidth().clickable {
                        selectedCustomer = cust
                        customerName = cust.name
                        customerMobile = cust.mobile.filter { it.isDigit() }.takeLast(10)
                        nameError = false
                        mobileError = false
                        showCustomerPicker = false
                      },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                      Text(
                        cust.mobile.ifBlank { "No mobile" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }

            HorizontalDivider()
            Text("Add New Customer Here", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaffronOrangeDark)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = quickNewCustName,
                onValueChange = {
                  quickNewCustName = it
                  quickCustError = null
                },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = quickNewCustMobile,
                onValueChange = {
                  quickNewCustMobile = it.filter { c -> c.isDigit() }.take(10)
                  quickCustError = null
                },
                label = { Text("10-digit Mobile") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }
            if (quickCustError != null) {
              Text(quickCustError!!, color = AlertRed, fontSize = 11.sp)
            }
            Button(
              onClick = {
                val n = quickNewCustName.trim()
                val m = quickNewCustMobile.filter { it.isDigit() }
                when {
                  n.isEmpty() -> quickCustError = "Enter customer name"
                  m.length != 10 -> quickCustError = "Enter 10-digit mobile number"
                  else -> {
                    onCreateInlineCustomer(n, m) { saved ->
                      selectedCustomer = saved
                      customerName = saved.name
                      customerMobile = saved.mobile.filter { it.isDigit() }.takeLast(10)
                      quickNewCustName = ""
                      quickNewCustMobile = ""
                      nameError = false
                      mobileError = false
                      showCustomerPicker = false
                    }
                  }
                }
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
              modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
              Text("Save & Select Customer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      OutlinedTextField(
        value = customerName,
        onValueChange = {
          customerName = it
          if (selectedCustomer != null && it.trim() != selectedCustomer!!.name) {
            selectedCustomer = null
          }
          nameError = false
        },
        label = { Text("Customer Name *") },
        isError = nameError,
        supportingText = if (nameError) ({ Text("Enter customer name") }) else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("order_customer_name"),
      )

      OutlinedTextField(
        value = customerMobile,
        onValueChange = {
          customerMobile = it.filter { ch -> ch.isDigit() }.take(10)
          if (selectedCustomer != null && customerMobile != selectedCustomer!!.mobile.filter { c -> c.isDigit() }.takeLast(10)) {
            selectedCustomer = null
          }
          mobileError = false
        },
        label = { Text("Mobile Number (10 digits) *") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError = mobileError,
        supportingText =
          if (mobileError) ({ Text("Enter valid 10-digit mobile number") }) else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("order_customer_mobile"),
      )

      HorizontalDivider()

      Text(
        text = "Select Products (${selectedItems.size} selected)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      OutlinedTextField(
        value = productSearch,
        onValueChange = { productSearch = it },
        placeholder = { Text("Search products...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("order_product_search"),
      )

      if (products.isEmpty()) {
        Text(
          "No products in catalog. Add products in Products tab first.",
          color = AlertRed,
          fontSize = 12.sp,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          filteredProducts.take(12).forEach { prod ->
            val existingIdx = selectedItems.indexOfFirst { it.productId == prod.id }
            val currentQty = if (existingIdx >= 0) selectedItems[existingIdx].quantity else 0.0
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.background,
              border = if (currentQty > 0.0) BorderStroke(1.dp, SaffronOrange) else null,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text(
                    "${CurrencyUtils.formatPaise(prod.sellingPricePaise, currencySymbol)} • Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.width(8.dp))
                if (currentQty > 0.0) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = SaffronOrangeSoft,
                      modifier =
                        Modifier.size(30.dp).clickable {
                          if (currentQty <= 1.0) selectedItems.removeAt(existingIdx)
                          else selectedItems[existingIdx] = selectedItems[existingIdx].copy(quantity = currentQty - 1.0)
                        },
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = SaffronOrange, modifier = Modifier.size(16.dp))
                      }
                    }
                    Text(
                      text = CurrencyUtils.formatQty(currentQty),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = SaffronOrange,
                      modifier =
                        Modifier.size(30.dp).clickable {
                          selectedItems[existingIdx] = selectedItems[existingIdx].copy(quantity = currentQty + 1.0)
                          itemsError = false
                        },
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SaffronOrange,
                    modifier =
                      Modifier.clickable {
                        selectedItems.add(
                          CartLineInput(
                            productId = prod.id,
                            name = prod.name,
                            unit = prod.unit,
                            quantity = 1.0,
                            unitPricePaise = prod.sellingPricePaise,
                            gstPercent = prod.gstPercent,
                          )
                        )
                        itemsError = false
                      },
                  ) {
                    Text(
                      "+ Add",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (selectedItems.isNotEmpty()) {
        HorizontalDivider()
        Text("Selected Quantities", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        selectedItems.forEachIndexed { idx, line ->
          var qtyStr by remember(idx, line.productId, line.name) {
            mutableStateOf(CurrencyUtils.formatQty(line.quantity))
          }
          LaunchedEffect(line.quantity) {
            if (qtyStr.toDoubleOrNull() != line.quantity) {
              qtyStr = CurrencyUtils.formatQty(line.quantity)
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(line.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text(
                "${CurrencyUtils.formatPaise(line.unitPricePaise, currencySymbol)} / ${line.unit}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedTextField(
              value = qtyStr,
              onValueChange = { raw ->
                qtyStr = raw
                val parsed = raw.toDoubleOrNull()
                if (parsed != null && parsed > 0.0 && idx < selectedItems.size) {
                  selectedItems[idx] = selectedItems[idx].copy(quantity = parsed)
                }
              },
              label = { Text("Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.width(86.dp),
            )
            IconButton(
              onClick = { if (idx < selectedItems.size) selectedItems.removeAt(idx) },
              modifier = Modifier.size(32.dp),
            ) {
              Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = AlertRed, modifier = Modifier.size(18.dp))
            }
          }
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SaffronOrangeSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Estimated Total", fontWeight = FontWeight.Bold, color = SaffronOrangeDark)
            Text(
              CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol),
              fontWeight = FontWeight.ExtraBold,
              color = SaffronOrangeDark,
            )
          }
        }
      }

      if (itemsError) {
        Text("Please select at least 1 product", color = AlertRed, fontSize = 12.sp)
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Order Notes (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    bottomButtons = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Cancel")
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = {
          val trimmedName = customerName.trim()
          val digits = customerMobile.filter { it.isDigit() }
          var valid = true
          if (trimmedName.isEmpty()) {
            nameError = true
            valid = false
          }
          if (digits.length != 10) {
            mobileError = true
            valid = false
          }
          if (selectedItems.isEmpty()) {
            itemsError = true
            valid = false
          }
          if (valid) {
            onSave(selectedCustomer, trimmedName, digits, selectedItems.toList(), notes.trim())
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        modifier = Modifier.heightIn(min = 48.dp).testTag("save_order_btn"),
      ) {
        Text(
          text = if (initialOrder == null) "Save Order" else "Update Order",
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }
    },
  )
}

@Composable
private fun PendingOrderSettleDialog(
  order: Order,
  defaultTaxType: GstTaxType,
  defaultTaxMode: GstTaxMode,
  autoRoundOff: Boolean,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onEditFullOrder: () -> Unit,
  onCancelOrder: () -> Unit,
  onConfirm: (
    finalItems: List<CartLineInput>,
    discountValue: Double,
    isPercentageDiscount: Boolean,
    taxType: GstTaxType,
    taxMode: GstTaxMode,
    paymentMethod: PaymentMethod,
    amountPaidPaise: Long,
    notes: String,
  ) -> Unit,
) {
  val lines = remember(order.id) {
    mutableStateListOf<EditableOrderLine>().apply {
      order.items.forEach {
        add(
          EditableOrderLine(
            productId = it.productId,
            name = it.name,
            unit = it.unit,
            gstPercent = it.gstPercent,
            rateInput = CurrencyUtils.paiseToEditString(it.unitPricePaise),
            qtyInput = CurrencyUtils.formatQty(it.quantity),
          )
        )
      }
    }
  }
  var discountInput by remember(order.id) {
    mutableStateOf(
      if (order.discountInput > 0.0) CurrencyUtils.formatQty(order.discountInput) else ""
    )
  }
  var isPctDiscount by remember(order.id) { mutableStateOf(order.isPercentageDiscount) }
  var taxType by remember(order.id) { mutableStateOf(defaultTaxType) }
  var taxMode by remember(order.id) { mutableStateOf(defaultTaxMode) }
  var paymentMethod by remember(order.id) { mutableStateOf(PaymentMethod.CASH) }
  var amountPaidManuallyEdited by remember(order.id) { mutableStateOf(false) }
  var customPaidInput by remember(order.id) { mutableStateOf("") }
  var notes by remember(order.id) { mutableStateOf(order.notes) }
  var showCancelConfirm by remember { mutableStateOf(false) }

  val cartInputs =
    lines.mapNotNull { line ->
      val qty = line.qtyInput.toDoubleOrNull() ?: 0.0
      if (qty <= 0.0) null
      else
        CartLineInput(
          productId = line.productId,
          name = line.name,
          unit = line.unit,
          quantity = qty,
          unitPricePaise = CurrencyUtils.rupeesToPaise(line.rateInput),
          gstPercent = line.gstPercent,
        )
    }

  val calc =
    BillingCalculator.calculate(
      items = cartInputs,
      discountValue = discountInput.toDoubleOrNull() ?: 0.0,
      isPercentageDiscount = isPctDiscount,
      taxType = taxType,
      taxMode = taxMode,
      autoRoundOff = autoRoundOff,
    )

  val effectivePaidInput =
    if (amountPaidManuallyEdited) customPaidInput
    else if (paymentMethod == PaymentMethod.CREDIT) "0"
    else CurrencyUtils.paiseToEditString(calc.grandTotalPaise)

  val paidPaise = CurrencyUtils.rupeesToPaise(effectivePaidInput).coerceIn(0L, calc.grandTotalPaise)
  val balanceDue = (calc.grandTotalPaise - paidPaise).coerceAtLeast(0L)

  VyaparFormDialog(
    title = "${order.orderNumber} • Finalize & Bill",
    onDismiss = onDismiss,
    scrollContent = {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = SaffronOrangeSoft,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(order.customerName, fontWeight = FontWeight.Bold, color = SaffronOrangeDark)
            if (order.customerMobile.isNotBlank()) {
              Text(order.customerMobile, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          OutlinedButton(
            onClick = onEditFullOrder,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Edit Items", fontSize = 11.sp)
          }
        }
      }

      Text("Items (Edit Rate & Quantity)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      lines.forEachIndexed { idx, line ->
        val ratePaise = CurrencyUtils.rupeesToPaise(line.rateInput)
        val qtyVal = (line.qtyInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val estLinePaise = kotlin.math.round(ratePaise * qtyVal).toLong()
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = line.name + if (line.gstPercent > 0) " (GST ${CurrencyUtils.formatQty(line.gstPercent)}%)" else "",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = CurrencyUtils.formatPaise(estLinePaise, currencySymbol),
                fontWeight = FontWeight.Bold,
                color = SaffronOrangeDark,
                fontSize = 13.sp,
              )
              if (lines.size > 1) {
                IconButton(
                  onClick = { lines.removeAt(idx) },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "Remove item", tint = AlertRed, modifier = Modifier.size(16.dp))
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = line.rateInput,
                onValueChange = { lines[idx] = line.copy(rateInput = it) },
                label = { Text("Rate ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = line.qtyInput,
                onValueChange = { lines[idx] = line.copy(qtyInput = it) },
                label = { Text("Qty (${line.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(0.85f),
              )
            }
          }
        }
      }

      // Extra Discount
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OutlinedTextField(
          value = discountInput,
          onValueChange = { discountInput = it },
          label = { Text(if (isPctDiscount) "Extra Discount %" else "Extra Discount ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        FilterChip(
          selected = !isPctDiscount,
          onClick = { isPctDiscount = false },
          label = { Text(currencySymbol) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
        )
        FilterChip(
          selected = isPctDiscount,
          onClick = { isPctDiscount = true },
          label = { Text("%") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
        )
      }

      // GST Type quick toggle
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("GST:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        listOf(
          GstTaxType.INTRA_STATE_CGST_SGST to "CGST+SGST",
          GstTaxType.INTER_STATE_IGST to "IGST",
          GstTaxType.NO_GST to "No GST",
        ).forEach { (tt, label) ->
          FilterChip(
            selected = taxType == tt,
            onClick = { taxType = tt },
            label = { Text(label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SaffronOrange, selectedLabelColor = Color.White),
          )
        }
      }

      // Payment Mode
      Text("Payment Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      val modes =
        listOf(
          PaymentMethod.CASH to "Cash",
          PaymentMethod.UPI to "UPI / Online",
          PaymentMethod.CARD to "Card",
          PaymentMethod.CREDIT to "Udhaar",
        )
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        modes.forEach { (method, label) ->
          FilterChip(
            selected = paymentMethod == method,
            onClick = {
              paymentMethod = method
              amountPaidManuallyEdited = false
            },
            label = { Text(label, fontSize = 12.sp) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                  if (method == PaymentMethod.CREDIT) AlertRed else EmeraldGreen,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      OutlinedTextField(
        value = effectivePaidInput,
        onValueChange = {
          customPaidInput = it
          amountPaidManuallyEdited = true
        },
        label = { Text("Amount Paid ($currencySymbol)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      // Recalculated Totals Summary Card
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(CurrencyUtils.formatPaise(calc.subtotalPaise, currencySymbol, true), fontSize = 12.sp)
          }
          if (calc.discountPaise > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Discount", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("-${CurrencyUtils.formatPaise(calc.discountPaise, currencySymbol, true)}", fontSize = 12.sp)
            }
          }
          if (calc.totalTaxPaise > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("GST", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(CurrencyUtils.formatPaise(calc.totalTaxPaise, currencySymbol, true), fontSize = 12.sp)
            }
          }
          HorizontalDivider()
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Final Total", fontWeight = FontWeight.Bold)
            Text(
              CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol, true),
              fontWeight = FontWeight.ExtraBold,
              color = SaffronOrangeDark,
            )
          }
          if (balanceDue > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Udhaar / Balance Due", fontSize = 12.sp, color = AlertRed, fontWeight = FontWeight.Bold)
              Text(
                CurrencyUtils.formatPaise(balanceDue, currencySymbol, true),
                fontSize = 12.sp,
                color = AlertRed,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Bill Notes (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedButton(
        onClick = { showCancelConfirm = true },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AlertRed),
        modifier = Modifier.fillMaxWidth().height(42.dp),
      ) {
        Text("Cancel This Order", color = AlertRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      }
    },
    bottomButtons = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Close")
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = {
          if (cartInputs.isNotEmpty()) {
            onConfirm(
              cartInputs,
              discountInput.toDoubleOrNull() ?: 0.0,
              isPctDiscount,
              taxType,
              taxMode,
              paymentMethod,
              paidPaise,
              notes.trim(),
            )
          }
        },
        enabled = cartInputs.isNotEmpty(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
        modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_order_bill_btn"),
      ) {
        Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
  )

  if (showCancelConfirm) {
    AlertDialog(
      onDismissRequest = { showCancelConfirm = false },
      properties = DialogProperties(decorFitsSystemWindows = false),
      modifier = Modifier.statusBarsPadding().navigationBarsPadding().imePadding(),
      title = { Text("Cancel ${order.orderNumber}?") },
      text = { Text("This order will be marked Cancelled and kept in your Cancelled Orders history.") },
      confirmButton = {
        TextButton(
          onClick = {
            showCancelConfirm = false
            onCancelOrder()
          }
        ) {
          Text("Yes, Cancel Order", color = AlertRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Pending") }
      },
    )
  }
}
