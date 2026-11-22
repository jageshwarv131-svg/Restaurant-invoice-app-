package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
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

  val palette = LocalVyaparClayPalette.current
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
    containerColor = palette.background,
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
              onClick = { showNewOrderDialog = true },
            )
            .testTag("new_order_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New Order", tint = Color.White, modifier = Modifier.size(28.dp))
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
    ) {
      // 3D Raised Search & Filter Tabs Card
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          contentPadding = PaddingValues(12.dp),
        ) {
          TextField3D(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search Customer Name, Mobile or Order #...",
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SaffronOrange) },
            modifier = Modifier.fillMaxWidth().testTag("orders_search_input"),
          )
          Spacer(Modifier.height(10.dp))
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
              val tabTone =
                when (tab) {
                  OrderFilterTab.PENDING -> ClayTones.Orange
                  OrderFilterTab.COMPLETED -> ClayTones.Green
                  OrderFilterTab.CANCELLED -> ClayTones.Red
                }
              Chip3D(
                selected = selected,
                onClick = { selectedTab = tab },
                label = "${tab.label} ($count)",
                selectedTone = tabTone,
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
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filtered, key = { it.id }) { order ->
            val linkedInvoice =
              order.linkedInvoiceId?.let { invId -> invoices.firstOrNull { it.id == invId } }
            val statusTone =
              when (order.status) {
                OrderStatus.COMPLETED -> ClayTones.Green
                OrderStatus.CANCELLED, OrderStatus.REFUNDED -> ClayTones.Red
                OrderStatus.PENDING, OrderStatus.PROCESSING -> ClayTones.Orange
              }

            Card3D(
              cornerRadius = 22.dp,
              shadowDepth = 4.dp,
              onClick = {
                if (order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING) {
                  settlingOrder = order
                } else {
                  viewingHistoryOrder = order
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("order_card_${order.orderNumber}"),
              contentPadding = PaddingValues(14.dp),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon3D(
                      imageVector = Icons.Filled.LocalShipping,
                      contentDescription = null,
                      tone = statusTone,
                      size = 24.dp,
                    )
                    Text(
                      text = order.orderNumber,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = palette.textPrimary,
                    )
                    Box(
                      modifier =
                        Modifier.raised3D(cornerRadius = 50.dp, tone = statusTone, shadowDepth = 2.dp)
                          .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                      Text(
                        text = order.status.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = if (order.status == OrderStatus.COMPLETED) "Final Total" else "Est. Total",
                      fontSize = 10.sp,
                      color = palette.textSecondary,
                    )
                    Text(
                      text = CurrencyUtils.formatPaise(order.totalPaise, currencySymbol),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = SaffronOrange,
                    )
                  }
                }

                HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Customer", fontSize = 10.sp, color = palette.textSecondary)
                    Text(
                      text = order.customerName.ifBlank { "Walk-in Customer" },
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = palette.textPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = order.customerMobile.ifBlank { "No mobile" },
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                  }
                  Spacer(Modifier.width(10.dp))
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "${order.items.size} item(s)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = CurrencyUtils.formatDateTime(order.timestamp),
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                  }
                }

                if (order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Button3D(
                      onClick = { editingOrder = order },
                      isPrimary = false,
                      cornerRadius = 14.dp,
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button3D(
                      onClick = { settlingOrder = order },
                      tone = ClayTones.Green,
                      cornerRadius = 14.dp,
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      modifier = Modifier.weight(1.5f),
                    ) {
                      Icon(Icons.Filled.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("Bill & Confirm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                  }
                } else if (order.status == OrderStatus.COMPLETED && linkedInvoice != null) {
                  Button3D(
                    onClick = { onOpenInvoicePreview(linkedInvoice) },
                    isPrimary = false,
                    cornerRadius = 14.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                      text = "View Bill (${linkedInvoice.invoiceNumber})",
                      color = SaffronOrange,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.ExtraBold,
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
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )
        if (order.customerMobile.isNotBlank()) {
          Text("Mobile: ${order.customerMobile}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
        Text("Date: ${CurrencyUtils.formatDateTime(order.timestamp)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
              Text(
                "${CurrencyUtils.formatQty(item.quantity)} ${item.unit} × ${CurrencyUtils.formatPaise(item.unitPricePaise, currencySymbol)}",
                fontSize = 11.sp,
                color = palette.textSecondary,
              )
            }
            Text(
              CurrencyUtils.formatPaise(item.lineTotalPaise, currencySymbol),
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = palette.textPrimary,
            )
          }
        }
        HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Total", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
          Text(
            CurrencyUtils.formatPaise(order.totalPaise, currencySymbol),
            fontWeight = FontWeight.ExtraBold,
            color = SaffronOrange,
          )
        }
        if (order.notes.isNotBlank()) {
          Text("Notes: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
      },
      bottomButtons = {
        Button3D(
          onClick = { viewingHistoryOrder = null },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Close", fontWeight = FontWeight.SemiBold)
        }
        if (linkedInvoice != null) {
          Spacer(Modifier.width(10.dp))
          Button3D(
            onClick = {
              viewingHistoryOrder = null
              onOpenInvoicePreview(linkedInvoice)
            },
            cornerRadius = 16.dp,
          ) {
            Text("View Invoice", color = Color.White, fontWeight = FontWeight.ExtraBold)
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
  val palette = LocalVyaparClayPalette.current
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon3D(Icons.Filled.Person, contentDescription = null, tone = ClayTones.Orange, size = 20.dp)
          Spacer(Modifier.width(6.dp))
          Text("Customer Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        }
        Button3D(
          onClick = { showCustomerPicker = !showCustomerPicker },
          isPrimary = false,
          cornerRadius = 12.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.testTag("order_pick_customer_btn"),
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
        Card3D(
          cornerRadius = 18.dp,
          shadowDepth = 3.dp,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(12.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField3D(
              value = customerSearch,
              onValueChange = { customerSearch = it },
              placeholder = "Search saved customers...",
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(18.dp)) },
              modifier = Modifier.fillMaxWidth(),
            )
            if (filteredCustomers.isEmpty()) {
              Text(
                "No saved customers match.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredCustomers.take(5).forEach { cust ->
                  Card3D(
                    cornerRadius = 14.dp,
                    shadowDepth = 2.dp,
                    accentBorderColor = if (selectedCustomer?.id == cust.id) SaffronOrange else null,
                    onClick = {
                      selectedCustomer = cust
                      customerName = cust.name
                      customerMobile = cust.mobile.filter { it.isDigit() }.takeLast(10)
                      nameError = false
                      mobileError = false
                      showCustomerPicker = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
                      Text(
                        cust.mobile.ifBlank { "No mobile" },
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                      )
                    }
                  }
                }
              }
            }

            HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))
            Text("Add New Customer Here", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = SaffronOrange)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextField3D(
                value = quickNewCustName,
                onValueChange = {
                  quickNewCustName = it
                  quickCustError = null
                },
                label = "Name",
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = quickNewCustMobile,
                onValueChange = {
                  quickNewCustMobile = it.filter { c -> c.isDigit() }.take(10)
                  quickCustError = null
                },
                label = "10-digit Mobile",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
              )
            }
            if (quickCustError != null) {
              Text(quickCustError!!, color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Button3D(
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
              cornerRadius = 14.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Save & Select Customer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }

      TextField3D(
        value = customerName,
        onValueChange = {
          customerName = it
          if (selectedCustomer != null && it.trim() != selectedCustomer!!.name) {
            selectedCustomer = null
          }
          nameError = false
        },
        label = "Customer Name *",
        isError = nameError,
        supportingText = if (nameError) ({ Text("Enter customer name") }) else null,
        modifier = Modifier.fillMaxWidth().testTag("order_customer_name"),
      )

      TextField3D(
        value = customerMobile,
        onValueChange = {
          customerMobile = it.filter { ch -> ch.isDigit() }.take(10)
          if (selectedCustomer != null && customerMobile != selectedCustomer!!.mobile.filter { c -> c.isDigit() }.takeLast(10)) {
            selectedCustomer = null
          }
          mobileError = false
        },
        label = "Mobile Number (10 digits) *",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError = mobileError,
        supportingText =
          if (mobileError) ({ Text("Enter valid 10-digit mobile number") }) else null,
        modifier = Modifier.fillMaxWidth().testTag("order_customer_mobile"),
      )

      HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))

      Text(
        text = "Select Products (${selectedItems.size} selected)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = palette.textPrimary,
      )
      TextField3D(
        value = productSearch,
        onValueChange = { productSearch = it },
        placeholder = "Search products...",
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SaffronOrange) },
        modifier = Modifier.fillMaxWidth().testTag("order_product_search"),
      )

      if (products.isEmpty()) {
        Text(
          "No products in catalog. Add products in Products tab first.",
          color = AlertRed,
          fontSize = 12.sp,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredProducts.take(12).forEach { prod ->
            val existingIdx = selectedItems.indexOfFirst { it.productId == prod.id }
            val currentQty = if (existingIdx >= 0) selectedItems[existingIdx].quantity else 0.0
            Card3D(
              cornerRadius = 16.dp,
              shadowDepth = 3.dp,
              accentBorderColor = if (currentQty > 0.0) SaffronOrange else null,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
                  Text(
                    "${CurrencyUtils.formatPaise(prod.sellingPricePaise, currencySymbol)} • Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                  )
                }
                Spacer(Modifier.width(8.dp))
                if (currentQty > 0.0) {
                  Row(
                    modifier = Modifier.inset3D(cornerRadius = 10.dp).padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier =
                        Modifier.size(28.dp)
                          .raised3D(cornerRadius = 8.dp, shadowDepth = 2.dp)
                          .clickable {
                            if (currentQty <= 1.0) selectedItems.removeAt(existingIdx)
                            else selectedItems[existingIdx] = selectedItems[existingIdx].copy(quantity = currentQty - 1.0)
                          },
                    ) {
                      Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = SaffronOrange, modifier = Modifier.size(16.dp))
                    }
                    Text(
                      text = CurrencyUtils.formatQty(currentQty),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 13.sp,
                      color = SaffronOrange,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier =
                        Modifier.size(28.dp)
                          .raised3D(cornerRadius = 8.dp, tone = ClayTones.Orange, shadowDepth = 2.dp)
                          .clickable {
                            selectedItems[existingIdx] = selectedItems[existingIdx].copy(quantity = currentQty + 1.0)
                            itemsError = false
                          },
                    ) {
                      Icon(Icons.Filled.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                  }
                } else {
                  Box(
                    modifier =
                      Modifier.raised3D(cornerRadius = 10.dp, tone = ClayTones.Orange, shadowDepth = 2.dp)
                        .clickable {
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
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Text(
                      "+ Add",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.ExtraBold,
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (selectedItems.isNotEmpty()) {
        HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))
        Text("Selected Quantities", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
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
              Text(line.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
              Text(
                "${CurrencyUtils.formatPaise(line.unitPricePaise, currencySymbol)} / ${line.unit}",
                fontSize = 11.sp,
                color = palette.textSecondary,
              )
            }
            TextField3D(
              value = qtyStr,
              onValueChange = { raw ->
                qtyStr = raw
                val parsed = raw.toDoubleOrNull()
                if (parsed != null && parsed > 0.0 && idx < selectedItems.size) {
                  selectedItems[idx] = selectedItems[idx].copy(quantity = parsed)
                }
              },
              label = "Qty",
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.width(86.dp),
            )
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(34.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { if (idx < selectedItems.size) selectedItems.removeAt(idx) },
            ) {
              Icon3D(Icons.Filled.DeleteOutline, contentDescription = "Remove", tone = ClayTones.Red, size = 18.dp)
            }
          }
        }
        Card3D(
          cornerRadius = 16.dp,
          tone = ClayTones.Orange,
          shadowDepth = 3.dp,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Estimated Total", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
              CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol),
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
            )
          }
        }
      }

      if (itemsError) {
        Text("Please select at least 1 product", color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      TextField3D(
        value = notes,
        onValueChange = { notes = it },
        label = "Order Notes (Optional)",
        modifier = Modifier.fillMaxWidth(),
      )
    },
    bottomButtons = {
      Button3D(
        onClick = onDismiss,
        isPrimary = false,
        cornerRadius = 16.dp,
      ) {
        Text("Cancel", fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.width(10.dp))
      Button3D(
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
        cornerRadius = 16.dp,
        modifier = Modifier.testTag("save_order_btn"),
      ) {
        Text(
          text = if (initialOrder == null) "Save Order" else "Update Order",
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
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
  val palette = LocalVyaparClayPalette.current
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
      Card3D(
        cornerRadius = 18.dp,
        shadowDepth = 3.dp,
        accentBorderColor = SaffronOrange.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(order.customerName, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            if (order.customerMobile.isNotBlank()) {
              Text(order.customerMobile, fontSize = 12.sp, color = palette.textSecondary)
            }
          }
          Button3D(
            onClick = onEditFullOrder,
            isPrimary = false,
            cornerRadius = 12.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Edit Items", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Text("Items (Edit Rate & Quantity)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
      lines.forEachIndexed { idx, line ->
        val ratePaise = CurrencyUtils.rupeesToPaise(line.rateInput)
        val qtyVal = (line.qtyInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val estLinePaise = kotlin.math.round(ratePaise * qtyVal).toLong()
        Card3D(
          cornerRadius = 16.dp,
          shadowDepth = 3.dp,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(10.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = line.name + if (line.gstPercent > 0) " (GST ${CurrencyUtils.formatQty(line.gstPercent)}%)" else "",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = CurrencyUtils.formatPaise(estLinePaise, currencySymbol),
                fontWeight = FontWeight.ExtraBold,
                color = SaffronOrange,
                fontSize = 13.sp,
              )
              if (lines.size > 1) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.size(28.dp).clickable { lines.removeAt(idx) },
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "Remove item", tint = AlertRed, modifier = Modifier.size(16.dp))
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextField3D(
                value = line.rateInput,
                onValueChange = { lines[idx] = line.copy(rateInput = it) },
                label = "Rate ($currencySymbol)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = line.qtyInput,
                onValueChange = { lines[idx] = line.copy(qtyInput = it) },
                label = "Qty (${line.unit})",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(0.85f),
              )
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        TextField3D(
          value = discountInput,
          onValueChange = { discountInput = it },
          label = if (isPctDiscount) "Extra Discount %" else "Extra Discount ($currencySymbol)",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
        Chip3D(
          selected = !isPctDiscount,
          onClick = { isPctDiscount = false },
          label = currencySymbol,
        )
        Chip3D(
          selected = isPctDiscount,
          onClick = { isPctDiscount = true },
          label = "%",
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("GST:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        listOf(
          GstTaxType.INTRA_STATE_CGST_SGST to "CGST+SGST",
          GstTaxType.INTER_STATE_IGST to "IGST",
          GstTaxType.NO_GST to "No GST",
        ).forEach { (tt, label) ->
          Chip3D(
            selected = taxType == tt,
            onClick = { taxType = tt },
            label = label,
          )
        }
      }

      Text("Payment Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
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
          Chip3D(
            selected = paymentMethod == method,
            onClick = {
              paymentMethod = method
              amountPaidManuallyEdited = false
            },
            label = label,
            selectedTone = if (method == PaymentMethod.CREDIT) ClayTones.Red else ClayTones.Green,
          )
        }
      }

      TextField3D(
        value = effectivePaidInput,
        onValueChange = {
          customPaidInput = it
          amountPaidManuallyEdited = true
        },
        label = "Amount Paid ($currencySymbol)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
      )

      Card3D(
        cornerRadius = 18.dp,
        shadowDepth = 3.dp,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", fontSize = 12.sp, color = palette.textSecondary)
            Text(CurrencyUtils.formatPaise(calc.subtotalPaise, currencySymbol, true), fontSize = 12.sp, color = palette.textPrimary)
          }
          if (calc.discountPaise > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Discount", fontSize = 12.sp, color = palette.textSecondary)
              Text("-${CurrencyUtils.formatPaise(calc.discountPaise, currencySymbol, true)}", fontSize = 12.sp, color = palette.textPrimary)
            }
          }
          if (calc.totalTaxPaise > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("GST", fontSize = 12.sp, color = palette.textSecondary)
              Text(CurrencyUtils.formatPaise(calc.totalTaxPaise, currencySymbol, true), fontSize = 12.sp, color = palette.textPrimary)
            }
          }
          HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.35f))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Final Total", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            Text(
              CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol, true),
              fontWeight = FontWeight.ExtraBold,
              color = SaffronOrange,
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

      TextField3D(
        value = notes,
        onValueChange = { notes = it },
        label = "Bill Notes (Optional)",
        modifier = Modifier.fillMaxWidth(),
      )

      Button3D(
        onClick = { showCancelConfirm = true },
        isPrimary = false,
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Cancel This Order", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    },
    bottomButtons = {
      Button3D(
        onClick = onDismiss,
        isPrimary = false,
        cornerRadius = 16.dp,
      ) {
        Text("Close", fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.width(10.dp))
      Button3D(
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
        tone = ClayTones.Green,
        cornerRadius = 16.dp,
        modifier = Modifier.testTag("confirm_order_bill_btn"),
      ) {
        Text("Confirm", color = Color.White, fontWeight = FontWeight.ExtraBold)
      }
    },
  )

  if (showCancelConfirm) {
    VyaparFormDialog(
      title = "Cancel ${order.orderNumber}?",
      onDismiss = { showCancelConfirm = false },
      scrollContent = {
        Text(
          "This order will be marked Cancelled and kept in your Cancelled Orders history.",
          color = palette.textSecondary,
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { showCancelConfirm = false },
          isPrimary = false,
          cornerRadius = 14.dp,
        ) {
          Text("Keep Pending", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            showCancelConfirm = false
            onCancelOrder()
          },
          tone = ClayTones.Red,
          cornerRadius = 14.dp,
        ) {
          Text("Yes, Cancel Order", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}
