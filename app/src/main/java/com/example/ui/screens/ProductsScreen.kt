package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CartLineInput
import com.example.model.Product
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.VyaparFormDialog
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
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
  val palette = LocalVyaparClayPalette.current
  val mutedIconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textSecondary, palette.neutralIconBottom)
  }

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
    containerColor = palette.background,
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
                editingProduct = null
                showEditor = true
              },
            )
            .testTag("add_product_fab"),
      ) {
        Icon(
          Icons.Filled.Add,
          contentDescription = "Add Product",
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
      }
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(palette.background)
              .navigationBarsPadding()
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("products_cart_summary_bar"),
        ) {
          Card3D(
            cornerRadius = 22.dp,
            shadowDepth = 6.dp,
            accentBorderColor = SaffronOrange.copy(alpha = 0.45f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon3D(
                  imageVector = Icons.Filled.ShoppingCart,
                  contentDescription = null,
                  tone = ClayTones.Orange,
                  size = 30.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                  Text(
                    text = "${cartItems.size} item(s) • ${CurrencyUtils.formatQty(totalCartQty)} qty",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    text = CurrencyUtils.formatPaise(cartSubtotalPaise, currencySymbol),
                    color = palette.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                  )
                }
              }
              Button3D(
                onClick = onProceedToBill,
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.testTag("products_make_bill_btn"),
              ) {
                Text("Make Bill", color = Color.White, fontWeight = FontWeight.ExtraBold)
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
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
    ) {
      // 3D Raised Search & Category Filter Card
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Card3D(
          cornerRadius = 22.dp,
          shadowDepth = 4.dp,
          contentPadding = PaddingValues(12.dp),
        ) {
          TextField3D(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search Product, SKU or Barcode...",
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SaffronOrange) },
            modifier = Modifier.fillMaxWidth().testTag("search_products_input"),
          )
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            categories.forEach { cat ->
              Chip3D(
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = cat,
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
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filteredProducts, key = { it.id }) { product ->
            val isLowStock = product.stockQty <= product.lowStockThreshold
            val qtyInCart = cartItems.find { it.productId == product.id }?.quantity ?: 0.0
            Card3D(
              cornerRadius = 22.dp,
              shadowDepth = 4.dp,
              accentBorderColor = if (qtyInCart > 0.0) SaffronOrange else null,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier =
                    Modifier.size(58.dp)
                      .inset3D(cornerRadius = 16.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  if (!product.imagePath.isNullOrBlank() && File(product.imagePath).exists()) {
                    AsyncImage(
                      model = File(product.imagePath),
                      contentDescription = product.name,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(13.dp)),
                    )
                  } else {
                    Icon3D(
                      imageVector = Icons.Filled.Inventory2,
                      contentDescription = null,
                      tone = ClayTones.Amber,
                      size = 30.dp,
                    )
                  }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = CurrencyUtils.formatPaise(product.sellingPricePaise, currencySymbol),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = SaffronOrange,
                    )
                    if (product.gstPercent > 0) {
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = "• GST ${CurrencyUtils.formatQty(product.gstPercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                      )
                    }
                  }
                  Spacer(Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier =
                        Modifier.inset3D(cornerRadius = 8.dp)
                          .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                      Text(
                        text =
                          "Stock: ${CurrencyUtils.formatQty(product.stockQty)} ${product.unit}" +
                            if (isLowStock) " (Low)" else "",
                        color = if (isLowStock) AlertRed else EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    if (product.category.isNotBlank()) {
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier.size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                          editingProduct = product
                          showEditor = true
                        },
                  ) {
                    Icon3D(
                      imageVector = Icons.Filled.Edit,
                      contentDescription = "Edit ${product.name}",
                      tone = mutedIconTone,
                      size = 19.dp,
                    )
                  }
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier.size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { pendingDeleteProduct = product },
                  ) {
                    Icon3D(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "Delete ${product.name}",
                      tone = ClayTones.Red,
                      size = 19.dp,
                    )
                  }
                  Spacer(Modifier.width(4.dp))
                  if (qtyInCart > 0.0) {
                    Row(
                      modifier = Modifier.inset3D(cornerRadius = 12.dp).padding(2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                          Modifier.size(30.dp)
                            .raised3D(cornerRadius = 9.dp, shadowDepth = 2.dp)
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
                        fontWeight = FontWeight.ExtraBold,
                        color = SaffronOrange,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                      )
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                          Modifier.size(30.dp)
                            .raised3D(cornerRadius = 9.dp, tone = ClayTones.Orange, shadowDepth = 2.dp)
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
                  } else {
                    Row(
                      modifier =
                        Modifier.height(36.dp)
                          .raised3D(cornerRadius = 12.dp, tone = ClayTones.Orange, shadowDepth = 3.dp)
                          .clickable { onIncrementProductInCart(product) }
                          .padding(horizontal = 10.dp)
                          .testTag("quick_add_bill_${product.id}"),
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
                        fontWeight = FontWeight.ExtraBold,
                      )
                    }
                  }
                }
              }
            }
          }
          item { Spacer(Modifier.height(78.dp)) }
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
    VyaparFormDialog(
      title = "Delete Product?",
      onDismiss = { pendingDeleteProduct = null },
      scrollContent = {
        Text(
          "Remove \"${target.name}\" from your catalog? Past invoices remain intact.",
          color = palette.textSecondary,
        )
      },
      bottomButtons = {
        Button3D(
          onClick = { pendingDeleteProduct = null },
          isPrimary = false,
          cornerRadius = 14.dp,
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Button3D(
          onClick = {
            onDeleteProduct(target.id)
            pendingDeleteProduct = null
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
fun ProductEditorDialog(
  initial: Product?,
  onDismiss: () -> Unit,
  onSave: (Product, Uri?) -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
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
        Box(
          modifier =
            Modifier.size(66.dp)
              .raised3D(cornerRadius = 18.dp, accentBorderColor = SaffronOrange, shadowDepth = 4.dp)
              .clickable {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
          contentAlignment = Alignment.Center,
        ) {
          when {
            pickedUri != null ->
              AsyncImage(
                model = pickedUri,
                contentDescription = "Selected Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
              )
            !initial?.imagePath.isNullOrBlank() && File(initial!!.imagePath!!).exists() ->
              AsyncImage(
                model = File(initial.imagePath!!),
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
              )
            else ->
              Icon3D(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Pick Image",
                tone = ClayTones.Orange,
                size = 28.dp,
              )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Product Image", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
          Text(
            "Tap to choose from gallery",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      TextField3D(
        value = name,
        onValueChange = {
          name = it
          nameError = false
        },
        label = "Product Name *",
        isError = nameError,
        supportingText = if (nameError) ({ Text("Enter product name") }) else null,
        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
      )

      TextField3D(
        value = sellingPrice,
        onValueChange = {
          sellingPrice = it
          priceError = false
        },
        label = "Selling Price *",
        prefix = { Text("₹ ") },
        placeholder = "e.g. 120",
        isError = priceError,
        supportingText = if (priceError) ({ Text("Enter a valid selling price") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("product_selling_price_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextField3D(
          value = purchasePrice,
          onValueChange = { purchasePrice = it },
          label = "Purchase Price",
          prefix = { Text("₹ ") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
        TextField3D(
          value = mrp,
          onValueChange = { mrp = it },
          label = "MRP",
          prefix = { Text("₹ ") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
      }

      TextField3D(
        value = category,
        onValueChange = { category = it },
        label = "Category",
        modifier = Modifier.fillMaxWidth(),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextField3D(
          value = stockQty,
          onValueChange = { stockQty = it },
          label = "Stock Qty",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
        TextField3D(
          value = lowThreshold,
          onValueChange = { lowThreshold = it },
          label = "Low Stock Alert",
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
      }

      Text("Unit", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        unitPresets.forEach { u ->
          Chip3D(
            selected = unit == u,
            onClick = { unit = u },
            label = u,
          )
        }
      }

      Text("GST Percentage (%)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        gstPresets.forEach { g ->
          Chip3D(
            selected = gstPercent == g,
            onClick = { gstPercent = g },
            label = "$g%",
          )
        }
      }
      TextField3D(
        value = gstPercent,
        onValueChange = { gstPercent = it },
        label = "Custom GST %",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextField3D(
          value = sku,
          onValueChange = { sku = it },
          label = "SKU",
          modifier = Modifier.weight(1f),
        )
        TextField3D(
          value = barcode,
          onValueChange = { barcode = it },
          label = "Barcode",
          modifier = Modifier.weight(1f),
        )
      }
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
          val trimmedName = name.trim()
          val parsedSelling = CurrencyUtils.rupeesToPaise(sellingPrice)
          if (trimmedName.isEmpty()) {
            nameError = true
            return@Button3D
          }
          if (sellingPrice.trim().isEmpty() || parsedSelling <= 0L) {
            priceError = true
            return@Button3D
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
        cornerRadius = 16.dp,
        modifier = Modifier.testTag("save_product_button"),
      ) {
        Text("Save Product", color = Color.White, fontWeight = FontWeight.ExtraBold)
      }
    },
  )
}
