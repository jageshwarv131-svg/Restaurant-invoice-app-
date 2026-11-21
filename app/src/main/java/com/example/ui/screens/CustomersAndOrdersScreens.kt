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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartLineInput
import com.example.model.Customer
import com.example.model.InventoryMovement
import com.example.model.Invoice
import com.example.model.MovementType
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VyaparTopBar
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedSoft
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldSoft
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenSoft
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueSoft
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
  customers: List<Customer>,
  invoices: List<Invoice>,
  currencySymbol: String,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  openAddImmediately: Boolean = false,
  onConsumeOpenAdd: () -> Unit = {},
  onBack: () -> Unit,
  onUpsertCustomer: (Customer) -> Unit,
  onDeleteCustomer: (String) -> Unit,
  onCollectPayment: (String, Long) -> Unit,
  onOpenInvoicePreview: (Invoice) -> Unit,
) {
  BackHandler(onBack = onBack)

  var searchQuery by remember { mutableStateOf("") }
  var showEditor by remember { mutableStateOf(openAddImmediately) }
  var editingCustomer by remember { mutableStateOf<Customer?>(null) }
  var selectedCustomerForHistory by remember { mutableStateOf<Customer?>(null) }
  var collectingForCustomer by remember { mutableStateOf<Customer?>(null) }
  var pendingDelete by remember { mutableStateOf<Customer?>(null) }

  if (openAddImmediately) onConsumeOpenAdd()

  val filtered =
    remember(customers, searchQuery) {
      val q = searchQuery.trim()
      if (q.isEmpty()) customers
      else
        customers.filter {
          it.name.contains(q, ignoreCase = true) ||
            it.mobile.contains(q, ignoreCase = true) ||
            it.whatsapp.contains(q, ignoreCase = true)
        }
    }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Customers",
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        showBack = true,
        onBackOrMenuClick = onBack,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingCustomer = null
          showEditor = true
        },
        containerColor = SaffronOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_customer_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add Customer")
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
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search Customer by name or mobile...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
      }

      if (filtered.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.Group,
          title = if (customers.isEmpty()) "No Customers Yet" else "No Matching Customers",
          subtitle = "Add regular customers to track purchase history and credit dues.",
          actionLabel = if (customers.isEmpty()) "Add Customer" else null,
          onAction =
            if (customers.isEmpty()) {
              {
                editingCustomer = null
                showEditor = true
              }
            } else null,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { customer ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              shadowElevation = 2.dp,
              modifier =
                Modifier.fillMaxWidth().clickable { selectedCustomerForHistory = customer },
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = CircleShape,
                  color = SaffronOrangeSoft,
                  modifier = Modifier.size(46.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = customer.name.take(1).uppercase(),
                      color = SaffronOrange,
                      fontWeight = FontWeight.Bold,
                      fontSize = 18.sp,
                    )
                  }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (customer.mobile.isNotBlank()) {
                    Text(
                      text = customer.mobile,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text =
                      "Total: ${CurrencyUtils.formatPaise(customer.totalPurchasesPaise, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.SemiBold,
                  )
                  if (customer.outstandingBalancePaise > 0) {
                    Text(
                      text =
                        "Due: ${CurrencyUtils.formatPaise(customer.outstandingBalancePaise, currencySymbol)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = AlertRed,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                if (customer.outstandingBalancePaise > 0) {
                  IconButton(onClick = { collectingForCustomer = customer }) {
                    Icon(
                      Icons.Filled.Payments,
                      contentDescription = "Collect Payment",
                      tint = EmeraldGreen,
                    )
                  }
                }
                IconButton(
                  onClick = {
                    editingCustomer = customer
                    showEditor = true
                  }
                ) {
                  Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit Customer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }
                IconButton(onClick = { pendingDelete = customer }) {
                  Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Customer",
                    tint = AlertRed,
                    modifier = Modifier.size(20.dp),
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

  if (showEditor) {
    CustomerEditorDialog(
      initial = editingCustomer,
      onDismiss = { showEditor = false },
      onSave = {
        onUpsertCustomer(it)
        showEditor = false
      },
    )
  }

  collectingForCustomer?.let { c ->
    var amountText by remember {
      mutableStateOf(CurrencyUtils.paiseToEditString(c.outstandingBalancePaise))
    }
    AlertDialog(
      onDismissRequest = { collectingForCustomer = null },
      title = { Text("Collect Due Payment") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Customer: ${c.name}")
          Text(
            "Outstanding: ${CurrencyUtils.formatPaise(c.outstandingBalancePaise, currencySymbol)}",
            color = AlertRed,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Received Amount ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onCollectPayment(c.id, CurrencyUtils.rupeesToPaise(amountText))
            collectingForCustomer = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
        ) {
          Text("Record Payment", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { collectingForCustomer = null }) { Text("Cancel") }
      },
    )
  }

  selectedCustomerForHistory?.let { customer ->
    val custInvoices =
      remember(invoices, customer.id) { invoices.filter { it.customerId == customer.id } }
    ModalBottomSheet(onDismissRequest = { selectedCustomerForHistory = null }) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("${customer.name} • Purchase History", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
          "Total Purchases: ${CurrencyUtils.formatPaise(customer.totalPurchasesPaise, currencySymbol)}  |  Due: ${CurrencyUtils.formatPaise(customer.outstandingBalancePaise, currencySymbol)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        if (custInvoices.isEmpty()) {
          Text("No invoices for this customer yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          LazyColumn(
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(custInvoices, key = { it.id }) { inv ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                modifier =
                  Modifier.fillMaxWidth().clickable {
                    selectedCustomerForHistory = null
                    onOpenInvoicePreview(inv)
                  },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold)
                    Text(
                      CurrencyUtils.formatDateTime(inv.timestamp),
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                  Text(
                    CurrencyUtils.formatPaise(inv.grandTotalPaise, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    color = SaffronOrange,
                  )
                }
              }
            }
          }
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }

  pendingDelete?.let { c ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("Delete Customer?") },
      text = { Text("Remove \"${c.name}\" from your customers list?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteCustomer(c.id)
            pendingDelete = null
          }
        ) {
          Text("Delete", color = AlertRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun CustomerEditorDialog(
  initial: Customer?,
  onDismiss: () -> Unit,
  onSave: (Customer) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
  var whatsapp by remember { mutableStateOf(initial?.whatsapp ?: "") }
  var address by remember { mutableStateOf(initial?.address ?: "") }
  var gstin by remember { mutableStateOf(initial?.gstin ?: "") }
  var nameError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Customer" else "Edit Customer") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            nameError = false
          },
          label = { Text("Customer Name *") },
          isError = nameError,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = mobile,
          onValueChange = { mobile = it },
          label = { Text("Mobile Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = whatsapp,
          onValueChange = { whatsapp = it },
          label = { Text("WhatsApp Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Address") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = gstin,
          onValueChange = { gstin = it.take(15) },
          label = { Text("Customer GSTIN (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().isEmpty()) {
            nameError = true
            return@Button
          }
          onSave(
            Customer(
              id = initial?.id ?: "",
              name = name.trim(),
              mobile = mobile.trim(),
              whatsapp = whatsapp.trim().ifBlank { mobile.trim() },
              address = address.trim(),
              gstin = gstin.trim().uppercase(),
              totalPurchasesPaise = initial?.totalPurchasesPaise ?: 0L,
              outstandingBalancePaise = initial?.outstandingBalancePaise ?: 0L,
              createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
      ) {
        Text("Save", color = Color.White)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun OrdersScreen(
  orders: List<Order>,
  products: List<Product>,
  customers: List<Customer>,
  currencySymbol: String,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onCreateOrder: (Customer?, List<CartLineInput>, String) -> Unit,
  onUpdateStatus: (String, OrderStatus) -> Unit,
  onConvertOrderToInvoice: (Order) -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
  var showNewOrderDialog by remember { mutableStateOf(false) }

  val filtered =
    remember(orders, selectedStatusFilter) {
      if (selectedStatusFilter == null) orders else orders.filter { it.status == selectedStatusFilter }
    }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Orders",
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
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Create Order")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedStatusFilter == null,
          onClick = { selectedStatusFilter = null },
          label = { Text("All (${orders.size})") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = SaffronOrange,
              selectedLabelColor = Color.White,
            ),
        )
        OrderStatus.entries.forEach { st ->
          FilterChip(
            selected = selectedStatusFilter == st,
            onClick = { selectedStatusFilter = st },
            label = { Text(st.label) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = CharcoalHeader,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      if (filtered.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.LocalShipping,
          title = "No Orders Found",
          subtitle = "Track pending, processing, completed, cancelled, and refunded orders offline.",
          actionLabel = "Create Order",
          onAction = { showNewOrderDialog = true },
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { order ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              shadowElevation = 2.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("${order.orderNumber} • ${order.customerName}", fontWeight = FontWeight.Bold)
                    Text(
                      "${CurrencyUtils.formatDateTime(order.timestamp)} • ${order.items.size} item(s)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    CurrencyUtils.formatPaise(order.totalPaise, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SaffronOrange,
                  )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  OrderStatus.entries.forEach { st ->
                    FilterChip(
                      selected = order.status == st,
                      onClick = { onUpdateStatus(order.id, st) },
                      label = { Text(st.label, fontSize = 11.sp) },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor =
                            when (st) {
                              OrderStatus.COMPLETED -> EmeraldGreen
                              OrderStatus.CANCELLED, OrderStatus.REFUNDED -> AlertRed
                              OrderStatus.PROCESSING -> InfoBlue
                              OrderStatus.PENDING -> AmberGold
                            },
                          selectedLabelColor = Color.White,
                        ),
                    )
                  }
                }
                if (order.linkedInvoiceId == null && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.REFUNDED) {
                  Spacer(Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = { onConvertOrderToInvoice(order) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SaffronOrange),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Convert to Invoice & PDF", color = SaffronOrange, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
          item { Spacer(Modifier.height(72.dp)) }
        }
      }
    }
  }

  if (showNewOrderDialog) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedItems by remember { mutableStateOf<List<CartLineInput>>(emptyList()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showNewOrderDialog = false },
      title = { Text("Create New Order") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Customer: ${selectedCustomer?.name ?: "Walk-in Customer"}", fontWeight = FontWeight.SemiBold)
          if (customers.isNotEmpty()) {
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FilterChip(
                selected = selectedCustomer == null,
                onClick = { selectedCustomer = null },
                label = { Text("Walk-in") },
              )
              customers.take(8).forEach { c ->
                FilterChip(
                  selected = selectedCustomer?.id == c.id,
                  onClick = { selectedCustomer = c },
                  label = { Text(c.name) },
                )
              }
            }
          }
          Text("Tap products to add (${selectedItems.size} added):", style = MaterialTheme.typography.labelMedium)
          if (products.isEmpty()) {
            Text("Add products first in the Products tab.", color = AlertRed, fontSize = 12.sp)
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              products.take(6).forEach { p ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.background,
                  modifier =
                    Modifier.fillMaxWidth().clickable {
                      selectedItems =
                        selectedItems +
                          CartLineInput(
                            productId = p.id,
                            name = p.name,
                            unit = p.unit,
                            quantity = 1.0,
                            unitPricePaise = p.sellingPricePaise,
                            gstPercent = p.gstPercent,
                          )
                    },
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(p.name, fontSize = 13.sp)
                    Text(
                      "+ ${CurrencyUtils.formatPaise(p.sellingPricePaise, currencySymbol)}",
                      color = SaffronOrange,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    )
                  }
                }
              }
            }
          }
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Order Notes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (selectedItems.isNotEmpty()) {
              onCreateOrder(selectedCustomer, selectedItems, notes.trim())
              showNewOrderDialog = false
            }
          },
          enabled = selectedItems.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        ) {
          Text("Save Order", color = Color.White)
        }
      },
      dismissButton = { TextButton(onClick = { showNewOrderDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
fun InventoryScreen(
  products: List<Product>,
  movements: List<InventoryMovement>,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onAdjustStock: (productId: String, type: MovementType, qty: Double, reason: String) -> Unit,
) {
  BackHandler(onBack = onBack)

  var showLowStockOnly by remember { mutableStateOf(false) }
  var showHistoryTab by remember { mutableStateOf(false) }
  var adjustingProduct by remember { mutableStateOf<Product?>(null) }

  val displayedProducts =
    remember(products, showLowStockOnly) {
      if (showLowStockOnly) products.filter { it.stockQty <= it.lowStockThreshold } else products
    }

  Scaffold(
    topBar = {
      VyaparTopBar(
        title = "Inventory & Stock",
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
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = !showHistoryTab && !showLowStockOnly,
          onClick = {
            showHistoryTab = false
            showLowStockOnly = false
          },
          label = { Text("All Stock (${products.size})") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = SaffronOrange,
              selectedLabelColor = Color.White,
            ),
        )
        FilterChip(
          selected = !showHistoryTab && showLowStockOnly,
          onClick = {
            showHistoryTab = false
            showLowStockOnly = true
          },
          label = { Text("Low Alerts (${products.count { it.stockQty <= it.lowStockThreshold }})") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = AlertRed,
              selectedLabelColor = Color.White,
            ),
        )
        FilterChip(
          selected = showHistoryTab,
          onClick = { showHistoryTab = true },
          label = { Text("History (${movements.size})") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = CharcoalHeader,
              selectedLabelColor = Color.White,
            ),
        )
      }

      if (!showHistoryTab) {
        if (displayedProducts.isEmpty()) {
          EmptyStatePanel(
            icon = Icons.Filled.Inventory,
            title = if (showLowStockOnly) "All Stock Healthy" else "No Products in Inventory",
            subtitle = "Stock updates automatically when sales invoices are completed.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(displayedProducts, key = { it.id }) { prod ->
              val low = prod.stockQty <= prod.lowStockThreshold
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
                  Column(modifier = Modifier.weight(1f)) {
                    Text(prod.name, fontWeight = FontWeight.Bold)
                    Text(
                      "Current: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}  •  Min Alert: ${CurrencyUtils.formatQty(prod.lowStockThreshold)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = if (low) AlertRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Button(
                    onClick = { adjustingProduct = prod },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                  ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adjust", color = Color.White, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      } else {
        if (movements.isEmpty()) {
          EmptyStatePanel(
            icon = Icons.Filled.SwapVert,
            title = "No Stock Movements Yet",
            subtitle = "Stock In, Stock Out, Adjustments, and Sale deductions appear here.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(movements, key = { it.id }) { move ->
              val positive = move.quantityDelta >= 0
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(move.productName, fontWeight = FontWeight.Bold)
                    Text(
                      "${move.type.label} • ${move.reason}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      CurrencyUtils.formatDateTime(move.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text =
                        (if (positive) "+" else "") + CurrencyUtils.formatQty(move.quantityDelta),
                      fontWeight = FontWeight.Bold,
                      color = if (positive) EmeraldGreen else AlertRed,
                    )
                    Text(
                      "Bal: ${CurrencyUtils.formatQty(move.stockAfter)}",
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  adjustingProduct?.let { prod ->
    var type by remember { mutableStateOf(MovementType.STOCK_IN) }
    var qtyText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { adjustingProduct = null },
      title = { Text("Update Stock: ${prod.name}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Current Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}")
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(MovementType.STOCK_IN, MovementType.STOCK_OUT, MovementType.ADJUSTMENT).forEach { m ->
              FilterChip(
                selected = type == m,
                onClick = { type = m },
                label = { Text(m.label, fontSize = 12.sp) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaffronOrange,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it },
            label = {
              Text(
                if (type == MovementType.ADJUSTMENT) "New Exact Stock Qty" else "Quantity (${prod.unit})"
              )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason / Note") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val q = qtyText.toDoubleOrNull() ?: return@Button
            onAdjustStock(prod.id, type, q, reason.trim())
            adjustingProduct = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        ) {
          Text("Update", color = Color.White)
        }
      },
      dismissButton = { TextButton(onClick = { adjustingProduct = null }) { Text("Cancel") } },
    )
  }
}
