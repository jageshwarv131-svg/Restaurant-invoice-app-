package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.CartLineInput
import com.example.model.Product
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VyaparFormDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedSoft
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeSoft
import com.example.util.CurrencyUtils
import java.io.File

@Composable
fun ProductsScreen(
  products: List<Product>,
  cartItems: List<CartLineInput>,
  currencySymbol: String,
  openAddDialogImmediately: Boolean = false,
  onConsumeOpenAdd: () -> Unit = {},
  onUpsertProduct: (Product, Uri?) -> Unit,
  onDeleteProduct: (String) -> Unit,
  onIncrementProductInCart: (Product) -> Unit,
  onDecrementProductInCart: (Product) -> Unit,
  onProceedToBill: () -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showEditor by remember { mutableStateOf(openAddDialogImmediately) }
  var pendingDeleteProduct by remember { mutableStateOf<Product?>(null) }

  if (openAddDialogImmediately) {
    onConsumeOpenAdd()
  }

  val categories =
    remember(products) {
      listOf("All") + products.map { it.category.ifBlank { "General" } }.distinct().sorted()
    }

  val filteredProducts =
    remember(products, searchQuery, selectedCategory) {
      products.filter { p ->
        val catMatch =
          selectedCategory == "All" ||
            p.category.equals(selectedCategory, ignoreCase = true)
        val q = searchQuery.trim()
        val queryMatch =
          q.isEmpty() ||
            p.name.contains(q, ignoreCase = true) ||
            p.sku.contains(q, ignoreCase = true) ||
            p.barcode.contains(q, ignoreCase = true)
        catMatch && queryMatch
      }
    }

  val totalCartQty = remember(cartItems) { cartItems.sumOf { it.quantity } }
  val cartSubtotalPaise =
    remember(cartItems) { cartItems.sumOf { kotlin.math.round(it.unitPricePaise * it.quantity).toLong() } }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingProduct = null
          showEditor = true
        },
        containerColor = SaffronOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_product_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add Product")
      }
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        Surface(
          color = CharcoalHeader,
          contentColor = Color.White,
          shadowElevation = 8.dp,
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .testTag("products_cart_summary_bar"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = SaffronOrange)
              Spacer(Modifier.width(10.dp))
              Column {
                Text(
                  text = "${cartItems.size} item(s) • ${CurrencyUtils.formatQty(totalCartQty)} qty",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  text = CurrencyUtils.formatPaise(cartSubtotalPaise, currencySymbol),
                  color = Color.White,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }
            Button(
              onClick = onProceedToBill,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
              modifier = Modifier.height(44.dp).testTag("products_make_bill_btn"),
            ) {
              Text("Make Bill", color = Color.White, fontWeight = FontWeight.Bold)
              Spacer(Modifier.width(6.dp))
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
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
      // Search + Category Filter Chips (matching Screen 5 in mockup)
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Product, SKU or Barcode...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_products_input"),
          )
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            categories.forEach { cat ->
              val selected = selectedCategory == cat
              FilterChip(
                selected = selected,
                onClick = { selectedCategory = cat },
                label = { Text(cat) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaffronOrange,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
        }
      }

      if (filteredProducts.isEmpty()) {
        EmptyStatePanel(
          icon = Icons.Filled.Inventory2,
          title = if (products.isEmpty()) "No Products Yet" else "No Matching Products",
          subtitle =
            if (products.isEmpty())
              "Add your shop items with price, stock & GST% to start instant billing."
            else "Try a different search keyword or category.",
          actionLabel = if (products.isEmpty()) "Add First Product" else null,
          onAction =
            if (products.isEmpty()) {
              {
                editingProduct = null
                showEditor = true
              }
            } else null,
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredProducts, key = { it.id }) { product ->
            val isLowStock = product.stockQty <= product.lowStockThreshold
            val qtyInCart = cartItems.find { it.productId == product.id }?.quantity ?: 0.0
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              border = if (qtyInCart > 0.0) BorderStroke(1.5.dp, SaffronOrange) else null,
              tonalElevation = 1.dp,
              shadowElevation = 2.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = SaffronOrangeSoft,
                  modifier = Modifier.size(58.dp),
                ) {
                  if (!product.imagePath.isNullOrBlank() && File(product.imagePath).exists()) {
                    AsyncImage(
                      model = File(product.imagePath),
                      contentDescription = product.name,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                  } else {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = SaffronOrange,
                        modifier = Modifier.size(28.dp),
                      )
                    }
                  }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = CurrencyUtils.formatPaise(product.sellingPricePaise, currencySymbol),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = SaffronOrange,
                    )
                    if (product.gstPercent > 0) {
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = "• GST ${CurrencyUtils.formatQty(product.gstPercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Spacer(Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color =
                        if (isLowStock) AlertRedSoft
                        else EmeraldGreen.copy(alpha = 0.12f),
                    ) {
                      Text(
                        text =
                          "Stock: ${CurrencyUtils.formatQty(product.stockQty)} ${product.unit}" +
                            if (isLowStock) " (Low)" else "",
                        color = if (isLowStock) AlertRed else EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                    if (product.category.isNotBlank()) {
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      editingProduct = product
                      showEditor = true
                    },
                    modifier = Modifier.size(34.dp),
                  ) {
                    Icon(
                      Icons.Filled.Edit,
                      contentDescription = "Edit ${product.name}",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  IconButton(
                    onClick = { pendingDeleteProduct = product },
                    modifier = Modifier.size(34.dp),
                  ) {
                    Icon(
                      Icons.Filled.DeleteOutline,
                      contentDescription = "Delete ${product.name}",
                      tint = AlertRed,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  Spacer(Modifier.width(4.dp))
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
                              .clickable { onDecrementProductInCart(product) }
                              .testTag("quick_dec_bill_${product.id}"),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Remove one ${product.name}",
                            tint = SaffronOrange,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                        Text(
                          text = CurrencyUtils.formatQty(qtyInCart),
                          fontWeight = FontWeight.Bold,
                          color = SaffronOrange,
                          fontSize = 13.sp,
                          modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier =
                            Modifier.size(32.dp)
                              .background(SaffronOrange)
                              .clickable { onIncrementProductInCart(product) }
                              .testTag("quick_add_bill_${product.id}"),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add more ${product.name}",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                      }
                    }
                  } else {
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = SaffronOrange,
                      modifier =
                        Modifier.height(36.dp)
                          .clickable { onIncrementProductInCart(product) }
                          .testTag("quick_add_bill_${product.id}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Add,
                          contentDescription = "Select ${product.name} for Bill",
                          tint = Color.White,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                          text = "Add",
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
          }
          item { Spacer(Modifier.height(72.dp)) }
        }
      }
    }
  }

  if (showEditor) {
    ProductEditorDialog(
      initial = editingProduct,
      onDismiss = { showEditor = false },
      onSave = { product, uri ->
        onUpsertProduct(product, uri)
        showEditor = false
      },
    )
  }

  pendingDeleteProduct?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteProduct = null },
      properties = DialogProperties(decorFitsSystemWindows = false),
      modifier = Modifier.statusBarsPadding().navigationBarsPadding().imePadding(),
      title = { Text("Delete Product?") },
      text = { Text("Remove \"${target.name}\" from your catalog? Past invoices remain intact.") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteProduct(target.id)
            pendingDeleteProduct = null
          }
        ) {
          Text("Delete", color = AlertRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteProduct = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
fun ProductEditorDialog(
  initial: Product?,
  onDismiss: () -> Unit,
  onSave: (Product, Uri?) -> Unit,
) {
  var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
  var sku by remember(initial?.id) { mutableStateOf(initial?.sku ?: "") }
  var barcode by remember(initial?.id) { mutableStateOf(initial?.barcode ?: "") }
  var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "General") }
  var purchasePrice by remember(initial?.id) {
    mutableStateOf(CurrencyUtils.paiseToEditString(initial?.purchasePricePaise ?: 0L))
  }
  var sellingPrice by remember(initial?.id) {
    mutableStateOf(CurrencyUtils.paiseToEditString(initial?.sellingPricePaise ?: 0L))
  }
  var mrp by remember(initial?.id) {
    mutableStateOf(CurrencyUtils.paiseToEditString(initial?.mrpPaise ?: 0L))
  }
  var stockQty by remember(initial?.id) {
    mutableStateOf(
      if (initial != null) CurrencyUtils.formatQty(initial.stockQty) else "10"
    )
  }
  var lowThreshold by remember(initial?.id) {
    mutableStateOf(
      if (initial != null) CurrencyUtils.formatQty(initial.lowStockThreshold) else "5"
    )
  }
  var unit by remember(initial?.id) { mutableStateOf(initial?.unit ?: "Pcs") }
  var gstPercent by remember(initial?.id) {
    mutableStateOf(
      if (initial != null) CurrencyUtils.formatQty(initial.gstPercent) else "0"
    )
  }
  var pickedUri by remember(initial?.id) { mutableStateOf<Uri?>(null) }
  var nameError by remember { mutableStateOf(false) }
  var priceError by remember { mutableStateOf(false) }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pickedUri = uri
    }

  val gstPresets = listOf("0", "5", "12", "18", "28")
  val unitPresets = listOf("Pcs", "Kg", "Gm", "Ltr", "Box", "Pack", "Plate")

  VyaparFormDialog(
    title = if (initial == null) "Add Product" else "Edit Product",
    onDismiss = onDismiss,
    scrollContent = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SaffronOrangeSoft,
              border = BorderStroke(1.dp, SaffronOrange),
              modifier =
                Modifier.size(64.dp).clickable {
                  picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) {
              when {
                pickedUri != null ->
                  AsyncImage(
                    model = pickedUri,
                    contentDescription = "Selected Product Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                !initial?.imagePath.isNullOrBlank() && File(initial!!.imagePath!!).exists() ->
                  AsyncImage(
                    model = File(initial.imagePath!!),
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                else ->
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Pick Image", tint = SaffronOrange)
                  }
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Product Image", style = MaterialTheme.typography.titleSmall)
              Text(
                "Tap to choose from gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              nameError = false
            },
            label = { Text("Product Name *") },
            isError = nameError,
            supportingText = if (nameError) ({ Text("Enter product name") }) else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
          )

          OutlinedTextField(
            value = sellingPrice,
            onValueChange = {
              sellingPrice = it
              priceError = false
            },
            label = { Text("Selling Price *") },
            prefix = { Text("₹ ") },
            placeholder = { Text("e.g. 120") },
            isError = priceError,
            supportingText = if (priceError) ({ Text("Enter a valid selling price") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("product_selling_price_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = purchasePrice,
              onValueChange = { purchasePrice = it },
              label = { Text("Purchase Price") },
              prefix = { Text("₹ ") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = mrp,
              onValueChange = { mrp = it },
              label = { Text("MRP") },
              prefix = { Text("₹ ") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = stockQty,
              onValueChange = { stockQty = it },
              label = { Text("Stock Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = lowThreshold,
              onValueChange = { lowThreshold = it },
              label = { Text("Low Stock Alert") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          Text("Unit", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            unitPresets.forEach { u ->
              FilterChip(
                selected = unit == u,
                onClick = { unit = u },
                label = { Text(u, fontSize = 12.sp) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaffronOrange,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }

          Text("GST Percentage (%)", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            gstPresets.forEach { g ->
              FilterChip(
                selected = gstPercent == g,
                onClick = { gstPercent = g },
                label = { Text("$g%", fontSize = 12.sp) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SaffronOrange,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
          OutlinedTextField(
            value = gstPercent,
            onValueChange = { gstPercent = it },
            label = { Text("Custom GST %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = sku,
              onValueChange = { sku = it },
              label = { Text("SKU") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = barcode,
              onValueChange = { barcode = it },
              label = { Text("Barcode") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
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
          val trimmedName = name.trim()
          val parsedSelling = CurrencyUtils.rupeesToPaise(sellingPrice)
          if (trimmedName.isEmpty()) {
            nameError = true
            return@Button
          }
          if (sellingPrice.trim().isEmpty() || parsedSelling <= 0L) {
            priceError = true
            return@Button
          }
          val product =
            Product(
              id = initial?.id ?: "",
              name = trimmedName,
              sku = sku.trim(),
              barcode = barcode.trim(),
              category = category.trim().ifBlank { "General" },
              purchasePricePaise = CurrencyUtils.rupeesToPaise(purchasePrice),
              sellingPricePaise = parsedSelling,
              mrpPaise = CurrencyUtils.rupeesToPaise(mrp),
              stockQty = (stockQty.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              lowStockThreshold = (lowThreshold.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0),
              unit = unit.ifBlank { "Pcs" },
              gstPercent = (gstPercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
              imagePath = initial?.imagePath,
              createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            )
          onSave(product, pickedUri)
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
        modifier = Modifier.heightIn(min = 48.dp).testTag("save_product_button"),
      ) {
        Text("Save Product", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
  )
}
