package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.GstTaxMode
import com.example.model.GstTaxType
import com.example.model.Invoice
import com.example.model.PaperFormat
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.pdf.ShareAndPrintHelper
import com.example.ui.PosCartState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils
import java.io.File

@Composable
fun BillsHistoryScreen(
  invoices: List<Invoice>,
  currencySymbol: String,
  onNewBill: () -> Unit,
  onOpenInvoice: (Invoice) -> Unit,
  onDeleteInvoice: (String) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var pendingDelete by remember { mutableStateOf<Invoice?>(null) }

  val filtered =
    remember(invoices, searchQuery) {
      val q = searchQuery.trim()
      if (q.isEmpty()) invoices
      else
        invoices.filter {
          it.invoiceNumber.contains(q, ignoreCase = true) ||
            it.customerName.contains(q, ignoreCase = true) ||
            it.customerMobile.contains(q, ignoreCase = true)
        }
    }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNewBill,
        containerColor = SaffronOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("bills_new_bill_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New Bill")
      }
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Invoice # or Customer...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          )
        }
      }

      if (filtered.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = if (invoices.isEmpty()) "No Invoices Generated" else "No Matching Invoices",
          subtitle = "Create a POS bill to generate GST-compliant PDF invoices.",
          actionLabel = "Create New Bill",
          onAction = onNewBill,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filtered, key = { it.id }) { inv ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              shadowElevation = 2.dp,
              modifier = Modifier.fillMaxWidth().clickable { onOpenInvoice(inv) },
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = SaffronOrangeSoft,
                  modifier = Modifier.size(46.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                      contentDescription = null,
                      tint = SaffronOrange,
                    )
                  }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = inv.invoiceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = inv.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "${CurrencyUtils.formatDateTime(inv.timestamp)} • ${inv.items.size} items • ${inv.paymentMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = CurrencyUtils.formatPaise(inv.grandTotalPaise, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SaffronOrangeDark,
                  )
                  Text(
                    text =
                      if (inv.balanceDuePaise > 0)
                        "Due ${CurrencyUtils.formatPaise(inv.balanceDuePaise, currencySymbol)}"
                      else "Paid",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (inv.balanceDuePaise > 0) AlertRed else EmeraldGreen,
                  )
                }
                IconButton(onClick = { pendingDelete = inv }) {
                  Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Invoice",
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

  pendingDelete?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("Delete Invoice ${target.invoiceNumber}?") },
      text = { Text("This permanently deletes the selected invoice record.") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteInvoice(target.id)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosBillingScreen(
  cart: PosCartState,
  products: List<Product>,
  customers: List<Customer>,
  currencySymbol: String,
  isShopOpen: Boolean,
  onToggleShopStatus: () -> Unit,
  onBack: () -> Unit,
  onAddProduct: (Product) -> Unit,
  onDecrementProduct: (Product) -> Unit,
  onAddCustomItem: (name: String, qty: Double, pricePaise: Long, gst: Double, unit: String) -> Unit,
  onUpdateQty: (Int, Double) -> Unit,
  onRemoveItem: (Int) -> Unit,
  onSelectCustomer: (Customer?) -> Unit,
  onUpdateCustomerDetails: (name: String, mobile: String, address: String, gstin: String, saveToDirectory: Boolean) -> Unit,
  onQuickCreateCustomer: (String, String, (Customer) -> Unit) -> Unit,
  onSetDiscount: (String, Boolean) -> Unit,
  onSetTaxConfig: (GstTaxType, GstTaxMode) -> Unit,
  onSetPaymentMethod: (PaymentMethod) -> Unit,
  onSetAmountPaid: (String) -> Unit,
  onClearCart: () -> Unit,
  onGenerateInvoice: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var showProductPicker by remember { mutableStateOf(false) }
  var showCustomerPicker by remember { mutableStateOf(false) }
  var showCustomItemDialog by remember { mutableStateOf(false) }
  var showExtraCustomerFields by remember {
    mutableStateOf(cart.customerAddress.isNotBlank() || cart.customerGstin.isNotBlank())
  }
  val calc = cart.calculation

  Scaffold(
    topBar = {
      Surface(color = CharcoalHeader, contentColor = Color.White) {
        Row(
          modifier =
            Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("pos_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Text(
            text = "New Bill",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
          )
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (isShopOpen) EmeraldGreen.copy(alpha = 0.20f)
              else AlertRed.copy(alpha = 0.24f),
            border =
              BorderStroke(
                1.dp,
                if (isShopOpen) EmeraldGreen.copy(alpha = 0.75f)
                else AlertRed.copy(alpha = 0.85f),
              ),
            modifier =
              Modifier.padding(end = 4.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggleShopStatus)
                .testTag("pos_shop_status_toggle"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = if (isShopOpen) "Shop Online" else "Shop Offline Closed",
                tint = if (isShopOpen) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (isShopOpen) "Online" else "Offline • Closed",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          IconButton(onClick = onClearCart, modifier = Modifier.testTag("pos_clear")) {
            Icon(Icons.Filled.Refresh, contentDescription = "Clear Cart", tint = Color.White)
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        modifier = Modifier.navigationBarsPadding(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          // Total Orange Pill matching Screen 6
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SaffronOrange,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Total :",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol, true),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("pos_grand_total"),
              )
            }
          }
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentMethod.entries.forEach { method ->
              FilterChip(
                selected = cart.paymentMethod == method,
                onClick = { onSetPaymentMethod(method) },
                label = { Text(method.label, fontSize = 12.sp) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CharcoalHeader,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          Button(
            onClick = onGenerateInvoice,
            enabled = cart.items.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrangeDark),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_invoice_button"),
          ) {
            Text(
              "Generate Invoice",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Customer Details Card (Direct Name, Mobile, Address, GSTIN + Saved Picker)
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = SaffronOrangeSoft,
                modifier = Modifier.size(34.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = SaffronOrange,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Customer Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
              )
              if (
                cart.selectedCustomer != null ||
                  cart.customerName.isNotBlank() ||
                  cart.customerMobile.isNotBlank()
              ) {
                TextButton(
                  onClick = { onSelectCustomer(null) },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                  Text("Clear", fontSize = 12.sp, color = AlertRed)
                }
              }
              OutlinedButton(
                onClick = { showCustomerPicker = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SaffronOrange),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("pos_pick_saved_customer"),
              ) {
                Text(
                  text = if (customers.isNotEmpty()) "Saved (${customers.size})" else "Select",
                  color = SaffronOrange,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = cart.customerName,
                onValueChange = {
                  onUpdateCustomerDetails(
                    it,
                    cart.customerMobile,
                    cart.customerAddress,
                    cart.customerGstin,
                    cart.saveCustomerToDirectory,
                  )
                },
                label = { Text("Customer Name") },
                placeholder = { Text("Walk-in Customer") },
                singleLine = true,
                modifier = Modifier.weight(1.1f).testTag("pos_customer_name_input"),
              )
              OutlinedTextField(
                value = cart.customerMobile,
                onValueChange = {
                  onUpdateCustomerDetails(
                    cart.customerName,
                    it,
                    cart.customerAddress,
                    cart.customerGstin,
                    cart.saveCustomerToDirectory,
                  )
                },
                label = { Text("Mobile No.") },
                placeholder = { Text("10-digit no.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(0.9f).testTag("pos_customer_mobile_input"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (showExtraCustomerFields) "Hide Address & GSTIN"
                  else "+ Address / GSTIN (Optional)",
                color = SaffronOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier =
                  Modifier.clickable { showExtraCustomerFields = !showExtraCustomerFields }
                    .padding(vertical = 4.dp),
              )
              if (
                cart.selectedCustomer == null &&
                  (cart.customerName.isNotBlank() || cart.customerMobile.isNotBlank())
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.clickable {
                      onUpdateCustomerDetails(
                        cart.customerName,
                        cart.customerMobile,
                        cart.customerAddress,
                        cart.customerGstin,
                        !cart.saveCustomerToDirectory,
                      )
                    },
                ) {
                  Checkbox(
                    checked = cart.saveCustomerToDirectory,
                    onCheckedChange = { checked ->
                      onUpdateCustomerDetails(
                        cart.customerName,
                        cart.customerMobile,
                        cart.customerAddress,
                        cart.customerGstin,
                        checked,
                      )
                    },
                    colors = CheckboxDefaults.colors(checkedColor = SaffronOrange),
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text("Save Customer", fontSize = 12.sp)
                }
              }
            }

            if (showExtraCustomerFields) {
              OutlinedTextField(
                value = cart.customerAddress,
                onValueChange = {
                  onUpdateCustomerDetails(
                    cart.customerName,
                    cart.customerMobile,
                    it,
                    cart.customerGstin,
                    cart.saveCustomerToDirectory,
                  )
                },
                label = { Text("Address / City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pos_customer_address_input"),
              )
              OutlinedTextField(
                value = cart.customerGstin,
                onValueChange = {
                  onUpdateCustomerDetails(
                    cart.customerName,
                    cart.customerMobile,
                    cart.customerAddress,
                    it.take(15).uppercase(),
                    cart.saveCustomerToDirectory,
                  )
                },
                label = { Text("Customer GSTIN (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pos_customer_gstin_input"),
              )
            }
          }
        }
      }

      // Add Product / Add Custom Item action row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { showProductPicker = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
            modifier = Modifier.weight(1f).height(46.dp).testTag("pos_add_product_btn"),
          ) {
            Icon(Icons.Filled.AddShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Product", color = Color.White, fontWeight = FontWeight.SemiBold)
          }
          OutlinedButton(
            onClick = { showCustomItemDialog = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SaffronOrange),
            modifier = Modifier.weight(1f).height(46.dp).testTag("pos_add_custom_btn"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = SaffronOrange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Custom Item", color = SaffronOrange, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      // Cart Line Items
      if (cart.items.isEmpty()) {
        item {
          EmptyStatePanel(
            icon = Icons.Filled.AddShoppingCart,
            title = "Cart is Empty",
            subtitle = "Add products from inventory or add a custom item to start billing.",
          )
        }
      } else {
        itemsIndexed(cart.items) { index, item ->
          val computed = calc.computedItems.getOrNull(index)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "${CurrencyUtils.formatPaise(item.unitPricePaise, currencySymbol)} × ${CurrencyUtils.formatQty(item.quantity)} ${item.unit}" +
                      if (item.gstPercent > 0) " (GST ${CurrencyUtils.formatQty(item.gstPercent)}%)" else "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              // Stepper
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.size(32.dp).clickable { onUpdateQty(index, -1.0) },
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                  }
                }
                Text(
                  text = CurrencyUtils.formatQty(item.quantity),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp),
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = SaffronOrange,
                  modifier = Modifier.size(32.dp).clickable { onUpdateQty(index, 1.0) },
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Filled.Add,
                      contentDescription = "Increase quantity",
                      tint = Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }

              Spacer(Modifier.width(10.dp))

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text =
                    CurrencyUtils.formatPaise(
                      computed?.lineTotalPaise ?: (item.unitPricePaise * item.quantity.toLong()),
                      currencySymbol,
                    ),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Remove",
                  color = AlertRed,
                  fontSize = 11.sp,
                  modifier = Modifier.clickable { onRemoveItem(index) },
                )
              }
            }
          }
        }

        // Discount, GST Config & Summary Card
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = cart.discountInput,
                  onValueChange = { onSetDiscount(it, cart.isPercentageDiscount) },
                  label = { Text(if (cart.isPercentageDiscount) "Discount %" else "Discount ($currencySymbol)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                FilterChip(
                  selected = !cart.isPercentageDiscount,
                  onClick = { onSetDiscount(cart.discountInput, false) },
                  label = { Text(currencySymbol) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = SaffronOrange,
                      selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                  selected = cart.isPercentageDiscount,
                  onClick = { onSetDiscount(cart.discountInput, true) },
                  label = { Text("%") },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = SaffronOrange,
                      selectedLabelColor = Color.White,
                    ),
                )
              }

              // Tax Type & Pricing Mode
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                GstTaxType.entries.forEach { t ->
                  FilterChip(
                    selected = cart.taxType == t,
                    onClick = { onSetTaxConfig(t, cart.taxMode) },
                    label = { Text(t.label, fontSize = 11.sp) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CharcoalHeader,
                        selectedLabelColor = Color.White,
                      ),
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GstTaxMode.entries.forEach { m ->
                  FilterChip(
                    selected = cart.taxMode == m,
                    onClick = { onSetTaxConfig(cart.taxType, m) },
                    label = { Text(m.label, fontSize = 11.sp) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SaffronOrange,
                        selectedLabelColor = Color.White,
                      ),
                  )
                }
              }

              OutlinedTextField(
                value = cart.amountPaidInput,
                onValueChange = onSetAmountPaid,
                label = { Text("Amount Paid ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )

              HorizontalDivider()

              SummaryRow("Subtotal", CurrencyUtils.formatPaise(calc.subtotalPaise, currencySymbol, true))
              if (calc.discountPaise > 0) {
                SummaryRow("Discount", "-${CurrencyUtils.formatPaise(calc.discountPaise, currencySymbol, true)}")
              }
              SummaryRow("Taxable Value", CurrencyUtils.formatPaise(calc.taxableValuePaise, currencySymbol, true))
              if (calc.cgstPaise > 0) {
                SummaryRow("CGST", CurrencyUtils.formatPaise(calc.cgstPaise, currencySymbol, true))
                SummaryRow("SGST", CurrencyUtils.formatPaise(calc.sgstPaise, currencySymbol, true))
              }
              if (calc.igstPaise > 0) {
                SummaryRow("IGST", CurrencyUtils.formatPaise(calc.igstPaise, currencySymbol, true))
              }
              if (calc.roundOffPaise != 0L) {
                SummaryRow("Round Off", CurrencyUtils.formatPaise(calc.roundOffPaise, currencySymbol, true))
              }
              val paidPaise = CurrencyUtils.rupeesToPaise(cart.amountPaidInput)
              val duePaise = (calc.grandTotalPaise - paidPaise).coerceAtLeast(0L)
              if (duePaise > 0) {
                SummaryRow("Balance Due", CurrencyUtils.formatPaise(duePaise, currencySymbol, true), AlertRed)
              }
            }
          }
        }
      }
    }
  }

  if (showProductPicker) {
    ModalBottomSheet(onDismissRequest = { showProductPicker = false }) {
      var q by remember { mutableStateOf("") }
      val matches =
        remember(products, q) {
          if (q.isBlank()) products
          else products.filter { it.name.contains(q, ignoreCase = true) || it.sku.contains(q, ignoreCase = true) }
        }
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Select Products for Bill", style = MaterialTheme.typography.titleLarge)
          if (cart.items.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(50), color = SaffronOrangeSoft) {
              Text(
                text = "${cart.items.size} selected",
                color = SaffronOrangeDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
          value = q,
          onValueChange = { q = it },
          placeholder = { Text("Search product...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.height(310.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(matches, key = { it.id }) { prod ->
            val qtyInCart = cart.items.find { it.productId == prod.id }?.quantity ?: 0.0
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.background,
              border = if (qtyInCart > 0.0) BorderStroke(1.5.dp, SaffronOrange) else null,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  onAddProduct(prod)
                },
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(
                    "${CurrencyUtils.formatPaise(prod.sellingPricePaise, currencySymbol)} • Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}" +
                      if (prod.gstPercent > 0) " • GST ${CurrencyUtils.formatQty(prod.gstPercent)}%" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.width(8.dp))
                if (qtyInCart > 0.0) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SaffronOrangeSoft,
                    border = BorderStroke(1.dp, SaffronOrange),
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                          Modifier.size(32.dp)
                            .clickable { onDecrementProduct(prod) }
                            .testTag("picker_dec_${prod.id}"),
                      ) {
                        Icon(
                          Icons.Filled.Remove,
                          contentDescription = "Decrease ${prod.name}",
                          tint = SaffronOrange,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                      Text(
                        text = CurrencyUtils.formatQty(qtyInCart),
                        fontWeight = FontWeight.Bold,
                        color = SaffronOrange,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                      )
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                          Modifier.size(32.dp)
                            .background(SaffronOrange)
                            .clickable { onAddProduct(prod) }
                            .testTag("picker_inc_${prod.id}"),
                      ) {
                        Icon(
                          Icons.Filled.Add,
                          contentDescription = "Add more ${prod.name}",
                          tint = Color.White,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SaffronOrange,
                  ) {
                    Text(
                      text = "+ Add",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                  }
                }
              }
            }
          }
        }
        Spacer(Modifier.height(12.dp))
        Button(
          onClick = { showProductPicker = false },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("picker_done_btn"),
        ) {
          Text(
            text =
              if (cart.items.isEmpty()) "Close"
              else "Done (${cart.items.size} items • ${CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol)})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(Modifier.height(8.dp))
      }
    }
  }

  if (showCustomerPicker) {
    ModalBottomSheet(onDismissRequest = { showCustomerPicker = false }) {
      var quickName by remember { mutableStateOf("") }
      var quickMobile by remember { mutableStateOf("") }
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Select Customer", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SaffronOrangeSoft,
          modifier =
            Modifier.fillMaxWidth().clickable {
              onSelectCustomer(null)
              showCustomerPicker = false
            },
        ) {
          Text(
            "Walk-in Customer (Default)",
            fontWeight = FontWeight.Bold,
            color = SaffronOrangeDark,
            modifier = Modifier.padding(14.dp),
          )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(customers, key = { it.id }) { cust ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.background,
              modifier =
                Modifier.fillMaxWidth().clickable {
                  onSelectCustomer(cust)
                  showCustomerPicker = false
                },
            ) {
              Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                  Text(cust.name, style = MaterialTheme.typography.titleSmall)
                  if (cust.mobile.isNotBlank()) {
                    Text(cust.mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                if (cust.outstandingBalancePaise > 0) {
                  Text(
                    "Due ${CurrencyUtils.formatPaise(cust.outstandingBalancePaise, currencySymbol)}",
                    color = AlertRed,
                    fontSize = 12.sp,
                  )
                }
              }
            }
          }
        }
        Spacer(Modifier.height(10.dp))
        Text("Quick Add New Customer", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = quickName,
            onValueChange = { quickName = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = quickMobile,
            onValueChange = { quickMobile = it },
            label = { Text("Mobile") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(Modifier.height(8.dp))
        Button(
          onClick = {
            if (quickName.isNotBlank()) {
              onQuickCreateCustomer(quickName.trim(), quickMobile.trim()) { created ->
                onSelectCustomer(created)
                showCustomerPicker = false
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Save & Select Customer", color = Color.White)
        }
      }
    }
  }

  if (showCustomItemDialog) {
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("") }
    var itemGst by remember { mutableStateOf("0") }
    var itemUnit by remember { mutableStateOf("Pcs") }

    AlertDialog(
      onDismissRequest = { showCustomItemDialog = false },
      title = { Text("Add Custom Item") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = itemQty,
              onValueChange = { itemQty = it },
              label = { Text("Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = itemPrice,
              onValueChange = { itemPrice = it },
              label = { Text("Unit Price *") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = itemGst,
              onValueChange = { itemGst = it },
              label = { Text("GST %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = itemUnit,
              onValueChange = { itemUnit = it },
              label = { Text("Unit") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (itemName.isNotBlank()) {
              onAddCustomItem(
                itemName.trim(),
                itemQty.toDoubleOrNull() ?: 1.0,
                CurrencyUtils.rupeesToPaise(itemPrice),
                itemGst.toDoubleOrNull() ?: 0.0,
                itemUnit.trim().ifBlank { "Pcs" },
              )
              showCustomItemDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        ) {
          Text("Add", color = Color.White)
        }
      },
      dismissButton = { TextButton(onClick = { showCustomItemDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
  }
}

@Composable
fun InvoicePreviewScreen(
  invoice: Invoice?,
  pdfFile: File?,
  initialFormat: PaperFormat,
  initialLanguage: AppLanguage,
  onRegeneratePdf: (PaperFormat, AppLanguage) -> Unit,
  onShowToast: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var format by remember { mutableStateOf(initialFormat) }
  var language by remember { mutableStateOf(initialLanguage) }

  val savePdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      if (uri != null && pdfFile != null && pdfFile.exists()) {
        val ok = ShareAndPrintHelper.writeFileToSafUri(context, pdfFile, uri)
        onShowToast(if (ok) "PDF downloaded successfully" else "Failed to save PDF")
      }
    }

  Scaffold(
    topBar = {
      Surface(color = CharcoalHeader, contentColor = Color.White) {
        Row(
          modifier =
            Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Text(
            text = "Invoice Preview",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
          )
          if (pdfFile != null && pdfFile.exists()) {
            IconButton(
              onClick = {
                ShareAndPrintHelper.sharePdfDocument(
                  context,
                  pdfFile,
                  "Share ${invoice?.invoiceNumber ?: "Invoice"} via WhatsApp or Apps",
                )
              },
              modifier = Modifier.testTag("preview_top_share"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = "Share PDF", tint = Color.White)
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        modifier = Modifier.navigationBarsPadding(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  ShareAndPrintHelper.sharePdfDocument(
                    context,
                    pdfFile,
                    "Share Invoice PDF on WhatsApp",
                  )
                }
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
              modifier = Modifier.weight(1.3f).height(48.dp).testTag("whatsapp_share_pdf_btn"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("WhatsApp PDF", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(
              onClick = {
                val defaultName = pdfFile?.name ?: "VYAPAR_INVOICE.pdf"
                savePdfLauncher.launch(defaultName)
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("download_pdf_btn"),
            ) {
              Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Save", maxLines = 1)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  ShareAndPrintHelper.printPdf(
                    context,
                    pdfFile,
                    invoice?.invoiceNumber ?: "Vyapar Invoice",
                  )
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(44.dp),
            ) {
              Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Print PDF")
            }
            OutlinedButton(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  if (!ShareAndPrintHelper.reopenPdfExternal(context, pdfFile)) {
                    onShowToast("No external PDF viewer installed")
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(44.dp),
            ) {
              Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Open PDF")
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      // Paper format & Language switcher bar
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PaperFormat.entries.forEach { pf ->
            FilterChip(
              selected = format == pf,
              onClick = {
                format = pf
                onRegeneratePdf(pf, language)
              },
              label = { Text(pf.label, fontSize = 12.sp) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SaffronOrange,
                  selectedLabelColor = Color.White,
                ),
            )
          }
          Spacer(Modifier.width(8.dp))
          AppLanguage.entries.forEach { lang ->
            FilterChip(
              selected = language == lang,
              onClick = {
                language = lang
                onRegeneratePdf(format, lang)
              },
              label = { Text(lang.label, fontSize = 12.sp) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CharcoalHeader,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
      }

      if (pdfFile != null && pdfFile.exists()) {
        PdfPagesViewer(
          pdfFile = pdfFile,
          modifier = Modifier.fillMaxSize().padding(14.dp),
        )
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Generating Invoice PDF...")
        }
      }
    }
  }
}

@Composable
private fun PdfPagesViewer(pdfFile: File, modifier: Modifier = Modifier) {
  val renderedPages =
    remember(pdfFile.absolutePath, pdfFile.lastModified()) {
      val list = mutableListOf<Bitmap>()
      runCatching {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd.use { descriptor ->
          val renderer = PdfRenderer(descriptor)
          renderer.use { r ->
            for (i in 0 until r.pageCount) {
              val page = r.openPage(i)
              page.use { p ->
                val scale = 2
                val bmp = Bitmap.createBitmap(p.width * scale, p.height * scale, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                list.add(bmp)
              }
            }
          }
        }
      }
      list
    }

  DisposableEffect(renderedPages) {
    onDispose { renderedPages.forEach { if (!it.isRecycled) it.recycle() } }
  }

  Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "${pdfFile.name} • ${renderedPages.size} page(s)",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    renderedPages.forEach { bmp ->
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Generated PDF Invoice Page",
          contentScale = ContentScale.FillWidth,
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat()),
        )
      }
    }
  }
}
