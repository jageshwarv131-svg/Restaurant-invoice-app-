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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.model.InventoryMovement
import com.example.model.Invoice
import com.example.model.MovementType
import com.example.model.Product
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.VyaparTopBar
import com.example.ui.components.raised3D
import com.example.ui.components.rememberSafeBottomPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
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

  val palette = LocalVyaparClayPalette.current
  val mutedIconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textSecondary, palette.neutralIconBottom)
  }

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
    containerColor = palette.background,
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
              onClick = {
                editingCustomer = null
                showEditor = true
              },
            )
            .testTag("add_customer_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add Customer", tint = Color.White, modifier = Modifier.size(28.dp))
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
          contentPadding = PaddingValues(12.dp),
        ) {
          TextField3D(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search Customer by name or mobile...",
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SaffronOrange) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
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
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filtered, key = { it.id }) { customer ->
            Card3D(
              cornerRadius = 22.dp,
              shadowDepth = 4.dp,
              onClick = { selectedCustomerForHistory = customer },
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier =
                    Modifier.size(48.dp)
                      .raised3D(cornerRadius = 24.dp, tone = ClayTones.Orange, shadowDepth = 3.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = customer.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (customer.mobile.isNotBlank()) {
                    Text(
                      text = customer.mobile,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                  }
                  Text(
                    text =
                      "Total: ${CurrencyUtils.formatPaise(customer.totalPurchasesPaise, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold,
                  )
                  if (customer.outstandingBalancePaise > 0) {
                    Text(
                      text =
                        "Due: ${CurrencyUtils.formatPaise(customer.outstandingBalancePaise, currencySymbol)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = AlertRed,
                      fontWeight = FontWeight.ExtraBold,
                    )
                  }
                }
                if (customer.outstandingBalancePaise > 0) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier.size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { collectingForCustomer = customer },
                  ) {
                    Icon3D(
                      imageVector = Icons.Filled.Payments,
                      contentDescription = "Collect Payment",
                      tone = ClayTones.Green,
                      size = 22.dp,
                    )
                  }
                }
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(38.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .clickable {
                        editingCustomer = customer
                        showEditor = true
                      },
                ) {
                  Icon3D(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Customer",
                    tone = mutedIconTone,
                    size = 20.dp,
                  )
                }
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(38.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { pendingDelete = customer },
                ) {
                  Icon3D(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Customer",
                    tone = ClayTones.Red,
                    size = 20.dp,
                  )
                }
              }
            }
          }
          item { Spacer(Modifier.height(74.dp)) }
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
    VyaparFormDialog(
      title = "Collect Due Payment",
      onDismiss = { collectingForCustomer = null },
      scrollContent = {
        Text("Customer: ${c.name}", fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Text(
          "Outstanding: ${CurrencyUtils.formatPaise(c.outstandingBalancePaise, currencySymbol)}",
          color = AlertRed,
          fontWeight = FontWeight.ExtraBold,
        )
        TextField3D(
          value = amountText,
          onValueChange = { amountText = it },
          label = "Received Amount ($currencySymbol)",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { collectingForCustomer = null },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            onCollectPayment(c.id, CurrencyUtils.rupeesToPaise(amountText))
            collectingForCustomer = null
          },
          tone = ClayTones.Green,
          cornerRadius = 16.dp,
        ) {
          Text("Record Payment", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      },
    )
  }

  selectedCustomerForHistory?.let { customer ->
    val custInvoices =
      remember(invoices, customer.id) { invoices.filter { it.customerId == customer.id } }
    ModalBottomSheet(
      onDismissRequest = { selectedCustomerForHistory = null },
      containerColor = palette.surfaceTop,
      contentColor = palette.textPrimary,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
      val safeBottom = rememberSafeBottomPadding(extraPadding = 20.dp)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = safeBottom)
      ) {
        Text("${customer.name} • Purchase History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
          "Total Purchases: ${CurrencyUtils.formatPaise(customer.totalPurchasesPaise, currencySymbol)}  |  Due: ${CurrencyUtils.formatPaise(customer.outstandingBalancePaise, currencySymbol)}",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
        Spacer(Modifier.height(14.dp))
        if (custInvoices.isEmpty()) {
          Text("No invoices for this customer yet.", color = palette.textSecondary)
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(custInvoices, key = { it.id }) { inv ->
              Card3D(
                cornerRadius = 18.dp,
                shadowDepth = 3.dp,
                onClick = {
                  selectedCustomerForHistory = null
                  onOpenInvoicePreview(inv)
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(inv.invoiceNumber, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text(
                      CurrencyUtils.formatDateTime(inv.timestamp),
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                  }
                  Text(
                    CurrencyUtils.formatPaise(inv.grandTotalPaise, currencySymbol),
                    fontWeight = FontWeight.ExtraBold,
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
    VyaparFormDialog(
      title = "Delete Customer?",
      onDismiss = { pendingDelete = null },
      scrollContent = {
        Text("Remove \"${c.name}\" from your customers list?", color = palette.textSecondary)
      },
      bottomButtons = {
        Button3D(
          onClick = { pendingDelete = null },
          isPrimary = false,
          cornerRadius = 14.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            onDeleteCustomer(c.id)
            pendingDelete = null
          },
          tone = ClayTones.Red,
          cornerRadius = 14.dp,
        ) {
          Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
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

  VyaparFormDialog(
    title = if (initial == null) "Add Customer" else "Edit Customer",
    onDismiss = onDismiss,
    scrollContent = {
      TextField3D(
        value = name,
        onValueChange = {
          name = it
          nameError = false
        },
        label = "Customer Name *",
        isError = nameError,
        supportingText = if (nameError) ({ Text("Enter customer name") }) else null,
        modifier = Modifier.fillMaxWidth(),
      )
      TextField3D(
        value = mobile,
        onValueChange = { mobile = it },
        label = "Mobile Number",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
      )
      TextField3D(
        value = whatsapp,
        onValueChange = { whatsapp = it },
        label = "WhatsApp Number",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
      )
      TextField3D(
        value = address,
        onValueChange = { address = it },
        label = "Address",
        singleLine = false,
        modifier = Modifier.fillMaxWidth(),
      )
      TextField3D(
        value = gstin,
        onValueChange = { gstin = it.take(15) },
        label = "Customer GSTIN (Optional)",
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
          if (name.trim().isEmpty()) {
            nameError = true
            return@Button3D
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
        cornerRadius = 16.dp,
      ) {
        Text("Save", color = Color.White, fontWeight = FontWeight.ExtraBold)
      }
    },
  )
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

  val palette = LocalVyaparClayPalette.current
  var showLowStockOnly by remember { mutableStateOf(false) }
  var showHistoryTab by remember { mutableStateOf(false) }
  var adjustingProduct by remember { mutableStateOf<Product?>(null) }

  val displayedProducts =
    remember(products, showLowStockOnly) {
      if (showLowStockOnly) products.filter { it.stockQty <= it.lowStockThreshold } else products
    }

  Scaffold(
    containerColor = palette.background,
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
          .background(palette.background)
          .padding(innerPadding)
    ) {
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          contentPadding = PaddingValues(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Chip3D(
              selected = !showHistoryTab && !showLowStockOnly,
              onClick = {
                showHistoryTab = false
                showLowStockOnly = false
              },
              label = "All Stock (${products.size})",
            )
            Chip3D(
              selected = !showHistoryTab && showLowStockOnly,
              onClick = {
                showHistoryTab = false
                showLowStockOnly = true
              },
              label = "Low Alerts (${products.count { it.stockQty <= it.lowStockThreshold }})",
              selectedTone = ClayTones.Red,
            )
            Chip3D(
              selected = showHistoryTab,
              onClick = { showHistoryTab = true },
              label = "History (${movements.size})",
              selectedTone = ClayTones.Blue,
            )
          }
        }
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
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(displayedProducts, key = { it.id }) { prod ->
              val low = prod.stockQty <= prod.lowStockThreshold
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
                  Column(modifier = Modifier.weight(1f)) {
                    Text(prod.name, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text(
                      "Current: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}  •  Min Alert: ${CurrencyUtils.formatQty(prod.lowStockThreshold)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = if (low) AlertRed else palette.textSecondary,
                      fontWeight = if (low) FontWeight.Bold else FontWeight.Normal,
                    )
                  }
                  Spacer(Modifier.width(8.dp))
                  Button3D(
                    onClick = { adjustingProduct = prod },
                    cornerRadius = 14.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adjust", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(movements, key = { it.id }) { move ->
              val positive = move.quantityDelta >= 0
              Card3D(
                cornerRadius = 18.dp,
                shadowDepth = 3.dp,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(move.productName, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text(
                      "${move.type.label} • ${move.reason}",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                    Text(
                      CurrencyUtils.formatDateTime(move.timestamp),
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textSecondary,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text =
                        (if (positive) "+" else "") + CurrencyUtils.formatQty(move.quantityDelta),
                      fontWeight = FontWeight.ExtraBold,
                      color = if (positive) EmeraldGreen else AlertRed,
                    )
                    Text(
                      "Bal: ${CurrencyUtils.formatQty(move.stockAfter)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textSecondary,
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

    VyaparFormDialog(
      title = "Update Stock: ${prod.name}",
      onDismiss = { adjustingProduct = null },
      scrollContent = {
        Text(
          "Current Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}",
          fontWeight = FontWeight.SemiBold,
          color = palette.textPrimary,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(MovementType.STOCK_IN, MovementType.STOCK_OUT, MovementType.ADJUSTMENT).forEach { m ->
            Chip3D(
              selected = type == m,
              onClick = { type = m },
              label = m.label,
            )
          }
        }
        TextField3D(
          value = qtyText,
          onValueChange = { qtyText = it },
          label =
            if (type == MovementType.ADJUSTMENT) "New Exact Stock Qty" else "Quantity (${prod.unit})",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        TextField3D(
          value = reason,
          onValueChange = { reason = it },
          label = "Reason / Note",
          modifier = Modifier.fillMaxWidth(),
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { adjustingProduct = null },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            val q = qtyText.toDoubleOrNull() ?: return@Button3D
            onAdjustStock(prod.id, type, q, reason.trim())
            adjustingProduct = null
          },
          cornerRadius = 16.dp,
        ) {
          Text("Update", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      },
    )
  }
}
