package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.ShopStatusBadge
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.components.rememberSafeBottomPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import com.example.util.CurrencyUtils
import java.io.File

@Composable
fun BillsHistoryScreen(
  invoices: List<Invoice>,
  pendingOrdersCount: Int,
  currencySymbol: String,
  onNewBill: () -> Unit,
  onNewOrder: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenInvoice: (Invoice) -> Unit,
  onDeleteInvoice: (String) -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
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
    containerColor = palette.background,
    floatingActionButton = {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        val orderInteraction = remember { MutableInteractionSource() }
        val orderPressed by orderInteraction.collectIsPressedAsState()
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.defaultMinSize(minHeight = 48.dp)
              .raised3D(cornerRadius = 50.dp, isPressed = orderPressed, shadowDepth = 5.dp)
              .clickable(
                interactionSource = orderInteraction,
                indication = null,
                onClick = onNewOrder,
              )
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("bills_fab_new_order"),
        ) {
          Icon3D(
            imageVector = Icons.Filled.AddShoppingCart,
            contentDescription = null,
            tone = ClayTones.Orange,
            size = 20.dp,
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = "New Order",
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
        }

        val billInteraction = remember { MutableInteractionSource() }
        val billPressed by billInteraction.collectIsPressedAsState()
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(58.dp)
              .raised3D(
                cornerRadius = 20.dp,
                isPressed = billPressed,
                tone = ClayTones.HeroOrange,
                shadowDepth = 6.dp,
              )
              .clickable(
                interactionSource = billInteraction,
                indication = null,
                onClick = onNewBill,
              )
              .testTag("bills_new_bill_fab"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "New Bill",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().background(palette.background).padding(innerPadding)
    ) {
      Card3D(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        cornerRadius = 24.dp,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(12.dp),
      ) {
        TextField3D(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = "Search Invoice # or Customer...",
          leadingIcon = {
            Icon3D(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tone = ClayTones.Orange,
              size = 20.dp,
            )
          },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button3D(
            onClick = onNewOrder,
            tone = ClayTones.Orange,
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("bills_new_order_btn"),
          ) {
            Icon(
              Icons.Filled.Add,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("New Order", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Button3D(
            onClick = onOpenOrders,
            isPrimary = false,
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("bills_pending_orders_btn"),
          ) {
            Icon3D(
              imageVector = Icons.Filled.LocalShipping,
              contentDescription = null,
              tone = ClayTones.Amber,
              size = 16.dp,
            )
            Spacer(Modifier.width(5.dp))
            Text(
              text = "Pending ($pendingOrdersCount)",
              color = palette.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
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
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 140.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filtered, key = { it.id }) { inv ->
            Card3D(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 22.dp,
              shadowDepth = 4.dp,
              onClick = { onOpenInvoice(inv) },
              contentPadding = PaddingValues(14.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ClayIconPad(
                  imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                  contentDescription = null,
                  tone = ClayTones.Orange,
                  padSize = 48.dp,
                  iconSize = 24.dp,
                  cornerRadius = 16.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = inv.invoiceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                  )
                  Text(
                    text = inv.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "${CurrencyUtils.formatDateTime(inv.timestamp)} • ${inv.items.size} items • ${inv.paymentMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = CurrencyUtils.formatPaise(inv.grandTotalPaise, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (palette.isDark) SaffronOrange else SaffronOrangeDark,
                  )
                  Spacer(Modifier.height(3.dp))
                  Box(
                    modifier =
                      Modifier.inset3D(cornerRadius = 50.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text =
                        if (inv.balanceDuePaise > 0)
                          "Due ${CurrencyUtils.formatPaise(inv.balanceDuePaise, currencySymbol)}"
                        else "Paid",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (inv.balanceDuePaise > 0) AlertRed else EmeraldGreen,
                    )
                  }
                }
                Spacer(Modifier.width(4.dp))
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(48.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { pendingDelete = inv },
                ) {
                  Icon3D(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Invoice",
                    tone = ClayTones.Red,
                    size = 20.dp,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  pendingDelete?.let { target ->
    VyaparFormDialog(
      title = "Delete Invoice ${target.invoiceNumber}?",
      onDismiss = { pendingDelete = null },
      scrollContent = {
        Text(
          text = "This permanently deletes the selected invoice record.",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { pendingDelete = null },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            onDeleteInvoice(target.id)
            pendingDelete = null
          },
          tone = ClayTones.Red,
          cornerRadius = 16.dp,
        ) {
          Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
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

  val palette = LocalVyaparClayPalette.current
  val neutralTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

  var showProductPicker by remember { mutableStateOf(false) }
  var showCustomerPicker by remember { mutableStateOf(false) }
  var showCustomItemDialog by remember { mutableStateOf(false) }
  var showExtraCustomerFields by remember {
    mutableStateOf(cart.customerAddress.isNotBlank() || cart.customerGstin.isNotBlank())
  }
  val calc = cart.calculation

  Scaffold(
    containerColor = palette.background,
    topBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .raised3D(cornerRadius = 22.dp, shadowDepth = 4.dp)
              .defaultMinSize(minHeight = 56.dp)
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onBack)
                .testTag("pos_back"),
          ) {
            Icon3D(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tone = neutralTone,
              size = 24.dp,
            )
          }
          Text(
            text = "New Bill",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
          )
          ShopStatusBadge(
            isShopOpen = isShopOpen,
            onToggleShopStatus = onToggleShopStatus,
            modifier = Modifier.padding(end = 4.dp).testTag("pos_shop_status_toggle"),
          )
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClearCart)
                .testTag("pos_clear"),
          ) {
            Icon3D(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Clear Cart",
              tone = neutralTone,
              size = 22.dp,
            )
          }
        }
      }
    },
    bottomBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .raised3D(cornerRadius = 26.dp, shadowDepth = 7.dp)
              .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
          // Glowing 3D Orange Total Pill
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .raised3D(cornerRadius = 18.dp, tone = ClayTones.HeroOrange, shadowDepth = 4.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Total :",
              color = Color.White,
              fontSize = 17.sp,
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
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentMethod.entries.forEach { method ->
              Chip3D(
                selected = cart.paymentMethod == method,
                onClick = { onSetPaymentMethod(method) },
                label = method.label,
                selectedTone = ClayTones.Orange,
              )
            }
          }
          Spacer(Modifier.height(10.dp))
          Button3D(
            onClick = onGenerateInvoice,
            enabled = cart.items.isNotEmpty(),
            tone = ClayTones.Orange,
            cornerRadius = 18.dp,
            modifier = Modifier.fillMaxWidth().testTag("generate_invoice_button"),
          ) {
            Text(
              "Generate Invoice",
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .imePadding(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Customer Details Card (Direct Name, Mobile, Address, GSTIN + Saved Picker)
      item {
        Card3D(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 24.dp,
          shadowDepth = 5.dp,
          contentPadding = PaddingValues(14.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ClayIconPad(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tone = ClayTones.Blue,
                padSize = 40.dp,
                iconSize = 22.dp,
                cornerRadius = 14.dp,
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = "Customer Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
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
                  Text("Clear", fontSize = 12.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                }
              }
              Button3D(
                onClick = { showCustomerPicker = true },
                isPrimary = false,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("pos_pick_saved_customer"),
              ) {
                Text(
                  text = if (customers.isNotEmpty()) "Saved (${customers.size})" else "Select",
                  color = SaffronOrange,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              TextField3D(
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
                label = "Customer Name",
                placeholder = "Walk-in Customer",
                singleLine = true,
                modifier = Modifier.weight(1.1f).testTag("pos_customer_name_input"),
              )
              TextField3D(
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
                label = "Mobile No.",
                placeholder = "10-digit no.",
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
                fontWeight = FontWeight.Bold,
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
                  Text("Save Customer", fontSize = 12.sp, color = palette.textSecondary)
                }
              }
            }

            if (showExtraCustomerFields) {
              TextField3D(
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
                label = "Address / City",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pos_customer_address_input"),
              )
              TextField3D(
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
                label = "Customer GSTIN (Optional)",
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
          Button3D(
            onClick = { showProductPicker = true },
            tone = ClayTones.Orange,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).testTag("pos_add_product_btn"),
          ) {
            Icon(
              Icons.Filled.AddShoppingCart,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Add Product", color = Color.White, fontWeight = FontWeight.Bold)
          }
          Button3D(
            onClick = { showCustomItemDialog = true },
            isPrimary = false,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).testTag("pos_add_custom_btn"),
          ) {
            Icon3D(
              imageVector = Icons.Filled.Add,
              contentDescription = null,
              tone = ClayTones.Orange,
              size = 18.dp,
            )
            Spacer(Modifier.width(6.dp))
            Text("Custom Item", color = palette.textPrimary, fontWeight = FontWeight.Bold)
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
          Card3D(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            shadowDepth = 4.dp,
            contentPadding = PaddingValues(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = palette.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text =
                    "${CurrencyUtils.formatPaise(item.unitPricePaise, currencySymbol)} × ${CurrencyUtils.formatQty(item.quantity)} ${item.unit}" +
                      if (item.gstPercent > 0) " (GST ${CurrencyUtils.formatQty(item.gstPercent)}%)" else "",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }

              // 3D Stepper
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.inset3D(cornerRadius = 14.dp).padding(4.dp),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(34.dp)
                      .raised3D(cornerRadius = 10.dp, shadowDepth = 2.dp)
                      .clickable { onUpdateQty(index, -1.0) },
                ) {
                  Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Decrease quantity",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(16.dp),
                  )
                }
                Text(
                  text = CurrencyUtils.formatQty(item.quantity),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = palette.textPrimary,
                  modifier = Modifier.padding(horizontal = 10.dp),
                )
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(34.dp)
                      .raised3D(cornerRadius = 10.dp, tone = ClayTones.Orange, shadowDepth = 3.dp)
                      .clickable { onUpdateQty(index, 1.0) },
                ) {
                  Icon(
                    Icons.Filled.Add,
                    contentDescription = "Increase quantity",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                  )
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
                  fontWeight = FontWeight.ExtraBold,
                  color = palette.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text = "Remove",
                  color = AlertRed,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.clickable { onRemoveItem(index) }.padding(2.dp),
                )
              }
            }
          }
        }

        // Discount, GST Config & Summary Card
        item {
          Card3D(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            shadowDepth = 5.dp,
            contentPadding = PaddingValues(16.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TextField3D(
                  value = cart.discountInput,
                  onValueChange = { onSetDiscount(it, cart.isPercentageDiscount) },
                  label = if (cart.isPercentageDiscount) "Discount %" else "Discount ($currencySymbol)",
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                Chip3D(
                  selected = !cart.isPercentageDiscount,
                  onClick = { onSetDiscount(cart.discountInput, false) },
                  label = currencySymbol,
                )
                Chip3D(
                  selected = cart.isPercentageDiscount,
                  onClick = { onSetDiscount(cart.discountInput, true) },
                  label = "%",
                )
              }

              // Tax Type & Pricing Mode
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                GstTaxType.entries.forEach { t ->
                  Chip3D(
                    selected = cart.taxType == t,
                    onClick = { onSetTaxConfig(t, cart.taxMode) },
                    label = t.label,
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GstTaxMode.entries.forEach { m ->
                  Chip3D(
                    selected = cart.taxMode == m,
                    onClick = { onSetTaxConfig(cart.taxType, m) },
                    label = m.label,
                  )
                }
              }

              TextField3D(
                value = cart.amountPaidInput,
                onValueChange = onSetAmountPaid,
                label = "Amount Paid ($currencySymbol)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )

              HorizontalDivider(color = palette.glossBorderBottom.copy(alpha = 0.45f))

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
    ModalBottomSheet(
      onDismissRequest = { showProductPicker = false },
      containerColor = palette.surfaceTop,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
      var q by remember { mutableStateOf("") }
      val matches =
        remember(products, q) {
          if (q.isBlank()) products
          else products.filter { it.name.contains(q, ignoreCase = true) || it.sku.contains(q, ignoreCase = true) }
        }
      val safeBottom = rememberSafeBottomPadding(extraPadding = 20.dp)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = safeBottom)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Select Products for Bill",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          if (cart.items.isNotEmpty()) {
            Box(
              modifier =
                Modifier.raised3D(cornerRadius = 50.dp, tone = ClayTones.Orange, shadowDepth = 3.dp)
                  .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
              Text(
                text = "${cart.items.size} selected",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
        Spacer(Modifier.height(12.dp))
        TextField3D(
          value = q,
          onValueChange = { q = it },
          placeholder = "Search product...",
          leadingIcon = {
            Icon3D(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tone = ClayTones.Orange,
              size = 20.dp,
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false).heightIn(max = 310.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(matches, key = { it.id }) { prod ->
            val qtyInCart = cart.items.find { it.productId == prod.id }?.quantity ?: 0.0
            Card3D(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 20.dp,
              shadowDepth = 4.dp,
              accentBorderColor = if (qtyInCart > 0.0) SaffronOrange else null,
              onClick = { onAddProduct(prod) },
              contentPadding = PaddingValues(12.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    prod.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                  )
                  Text(
                    "${CurrencyUtils.formatPaise(prod.sellingPricePaise, currencySymbol)} • Stock: ${CurrencyUtils.formatQty(prod.stockQty)} ${prod.unit}" +
                      if (prod.gstPercent > 0) " • GST ${CurrencyUtils.formatQty(prod.gstPercent)}%" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                Spacer(Modifier.width(8.dp))
                if (qtyInCart > 0.0) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.inset3D(cornerRadius = 14.dp).padding(4.dp),
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier =
                        Modifier.size(32.dp)
                          .raised3D(cornerRadius = 10.dp, shadowDepth = 2.dp)
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
                      fontWeight = FontWeight.ExtraBold,
                      color = palette.textPrimary,
                      fontSize = 13.sp,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier =
                        Modifier.size(32.dp)
                          .raised3D(cornerRadius = 10.dp, tone = ClayTones.Orange, shadowDepth = 3.dp)
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
                } else {
                  Box(
                    modifier =
                      Modifier.raised3D(
                        cornerRadius = 12.dp,
                        tone = ClayTones.Orange,
                        shadowDepth = 3.dp,
                      )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                  ) {
                    Text(
                      text = "+ Add",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
        Spacer(Modifier.height(14.dp))
        Button3D(
          onClick = { showProductPicker = false },
          tone = ClayTones.Orange,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().testTag("picker_done_btn"),
        ) {
          Text(
            text =
              if (cart.items.isEmpty()) "Close"
              else "Done (${cart.items.size} items • ${CurrencyUtils.formatPaise(calc.grandTotalPaise, currencySymbol)})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }

  if (showCustomerPicker) {
    ModalBottomSheet(
      onDismissRequest = { showCustomerPicker = false },
      containerColor = palette.surfaceTop,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
      var quickName by remember { mutableStateOf("") }
      var quickMobile by remember { mutableStateOf("") }
      val safeBottom = rememberSafeBottomPadding(extraPadding = 20.dp)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = safeBottom)
      ) {
        Text(
          "Select Customer",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Card3D(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 18.dp,
          tone = ClayTones.Orange,
          shadowDepth = 4.dp,
          onClick = {
            onSelectCustomer(null)
            showCustomerPicker = false
          },
          contentPadding = PaddingValues(14.dp),
        ) {
          Text(
            "Walk-in Customer (Default)",
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(customers, key = { it.id }) { cust ->
            Card3D(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 18.dp,
              shadowDepth = 3.dp,
              onClick = {
                onSelectCustomer(cust)
                showCustomerPicker = false
              },
              contentPadding = PaddingValues(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    cust.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                  )
                  if (cust.mobile.isNotBlank()) {
                    Text(
                      cust.mobile,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textSecondary,
                    )
                  }
                }
                if (cust.outstandingBalancePaise > 0) {
                  Text(
                    "Due ${CurrencyUtils.formatPaise(cust.outstandingBalancePaise, currencySymbol)}",
                    color = AlertRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
        Spacer(Modifier.height(12.dp))
        Text(
          "Quick Add New Customer",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TextField3D(
            value = quickName,
            onValueChange = { quickName = it },
            label = "Name",
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          TextField3D(
            value = quickMobile,
            onValueChange = { quickMobile = it },
            label = "Mobile",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(Modifier.height(12.dp))
        Button3D(
          onClick = {
            if (quickName.isNotBlank()) {
              onQuickCreateCustomer(quickName.trim(), quickMobile.trim()) { created ->
                onSelectCustomer(created)
                showCustomerPicker = false
              }
            }
          },
          tone = ClayTones.Orange,
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Save & Select Customer", color = Color.White, fontWeight = FontWeight.Bold)
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

    VyaparFormDialog(
      title = "Add Custom Item",
      onDismiss = { showCustomItemDialog = false },
      scrollContent = {
        TextField3D(
          value = itemName,
          onValueChange = { itemName = it },
          label = "Item Name *",
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TextField3D(
            value = itemQty,
            onValueChange = { itemQty = it },
            label = "Qty",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          TextField3D(
            value = itemPrice,
            onValueChange = { itemPrice = it },
            label = "Unit Price *",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TextField3D(
            value = itemGst,
            onValueChange = { itemGst = it },
            label = "GST %",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          TextField3D(
            value = itemUnit,
            onValueChange = { itemUnit = it },
            label = "Unit",
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      },
      bottomButtons = {
        Button3D(
          onClick = { showCustomItemDialog = false },
          isPrimary = false,
          cornerRadius = 16.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
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
          tone = ClayTones.Orange,
          cornerRadius = 16.dp,
        ) {
          Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
  valueColor: Color = LocalVyaparClayPalette.current.textPrimary,
) {
  val palette = LocalVyaparClayPalette.current
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    Text(
      value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = valueColor,
    )
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

  val palette = LocalVyaparClayPalette.current
  val neutralTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }
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
    containerColor = palette.background,
    topBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .raised3D(cornerRadius = 22.dp, shadowDepth = 4.dp)
              .defaultMinSize(minHeight = 56.dp)
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onBack)
                .testTag("preview_back"),
          ) {
            Icon3D(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tone = neutralTone,
              size = 24.dp,
            )
          }
          Text(
            text = "Invoice Preview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
          )
          if (pdfFile != null && pdfFile.exists()) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    ShareAndPrintHelper.sharePdfDocument(
                      context,
                      pdfFile,
                      "Share ${invoice?.invoiceNumber ?: "Invoice"} via WhatsApp or Apps",
                    )
                  }
                  .testTag("preview_top_share"),
            ) {
              Icon3D(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share PDF",
                tone = ClayTones.Orange,
                size = 22.dp,
              )
            }
          }
        }
      }
    },
    bottomBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .raised3D(cornerRadius = 26.dp, shadowDepth = 7.dp)
              .padding(horizontal = 12.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button3D(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  ShareAndPrintHelper.sharePdfDocument(
                    context,
                    pdfFile,
                    "Share Invoice PDF on WhatsApp",
                  )
                }
              },
              tone = ClayTones.Green,
              cornerRadius = 18.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
              modifier = Modifier.weight(1.35f).testTag("whatsapp_share_pdf_btn"),
            ) {
              Icon(
                Icons.Filled.Share,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("WhatsApp PDF", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button3D(
              onClick = {
                val defaultName = pdfFile?.name ?: "VYAPAR_INVOICE.pdf"
                savePdfLauncher.launch(defaultName)
              },
              isPrimary = false,
              cornerRadius = 18.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
              modifier = Modifier.weight(1f).testTag("download_pdf_btn"),
            ) {
              Icon3D(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tone = ClayTones.Blue,
                size = 18.dp,
              )
              Spacer(Modifier.width(5.dp))
              Text("Save", fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button3D(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  ShareAndPrintHelper.printPdf(
                    context,
                    pdfFile,
                    invoice?.invoiceNumber ?: "Vyapar Invoice",
                  )
                }
              },
              isPrimary = false,
              cornerRadius = 18.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon3D(
                imageVector = Icons.Filled.Print,
                contentDescription = null,
                tone = ClayTones.Purple,
                size = 17.dp,
              )
              Spacer(Modifier.width(5.dp))
              Text("Print PDF", fontWeight = FontWeight.SemiBold)
            }
            Button3D(
              onClick = {
                if (pdfFile != null && pdfFile.exists()) {
                  if (!ShareAndPrintHelper.reopenPdfExternal(context, pdfFile)) {
                    onShowToast("No external PDF viewer installed")
                  }
                }
              },
              isPrimary = false,
              cornerRadius = 18.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon3D(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                tone = ClayTones.Orange,
                size = 17.dp,
              )
              Spacer(Modifier.width(5.dp))
              Text("Open PDF", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().background(palette.background).padding(innerPadding)
    ) {
      // Paper format & Language switcher bar
      Card3D(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        cornerRadius = 20.dp,
        shadowDepth = 4.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PaperFormat.entries.forEach { pf ->
            Chip3D(
              selected = format == pf,
              onClick = {
                format = pf
                onRegeneratePdf(pf, language)
              },
              label = pf.label,
              selectedTone = ClayTones.Orange,
            )
          }
          Spacer(Modifier.width(6.dp))
          AppLanguage.entries.forEach { lang ->
            Chip3D(
              selected = language == lang,
              onClick = {
                language = lang
                onRegeneratePdf(format, lang)
              },
              label = lang.label,
              selectedTone = ClayTones.Blue,
            )
          }
        }
      }

      if (pdfFile != null && pdfFile.exists()) {
        PdfPagesViewer(
          pdfFile = pdfFile,
          modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        )
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Generating Invoice PDF...", color = palette.textSecondary)
        }
      }
    }
  }
}

@Composable
private fun PdfPagesViewer(pdfFile: File, modifier: Modifier = Modifier) {
  val palette = LocalVyaparClayPalette.current
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
      color = palette.textSecondary,
    )
    renderedPages.forEach { bmp ->
      Card3D(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        shadowDepth = 6.dp,
        contentPadding = PaddingValues(6.dp),
      ) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Generated PDF Invoice Page",
          contentScale = ContentScale.FillWidth,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White)
              .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat()),
        )
      }
    }
    Spacer(Modifier.height(10.dp))
  }
}
