package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfile
import com.example.model.Invoice
import com.example.model.Product
import com.example.ui.DashboardMetrics
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayBarChart
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.Icon3D
import com.example.ui.components.MetricGridCard
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedSoft
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldSoft
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenSoft
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueSoft
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentSoft
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChartRange(val label: String) {
  SEVEN_DAYS("7D"),
  THIRTY_DAYS("30D"),
  ONE_YEAR("1Y"),
}

@Composable
fun DashboardScreen(
  business: BusinessProfile,
  metrics: DashboardMetrics,
  invoices: List<Invoice>,
  products: List<Product>,
  onNewBill: () -> Unit,
  onAddProduct: () -> Unit,
  onAddCustomer: () -> Unit,
  onOpenReports: () -> Unit,
  onOpenExpenses: () -> Unit,
  onOpenInvoices: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenInvoicePreview: (Invoice) -> Unit,
  onOpenCustomers: () -> Unit,
  onOpenProducts: () -> Unit,
  onOpenOrders: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val sym = business.currencySymbol.ifBlank { "₹" }
  var chartRange by remember { mutableStateOf(ChartRange.SEVEN_DAYS) }

  val chartPoints =
    remember(invoices, chartRange) { computeSalesSeries(invoices, chartRange) }
  val topProducts = remember(invoices) { computeTopProducts(invoices) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(palette.background),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Hero Orange 3D Today Sales Card
    item {
      Card3D(
        cornerRadius = 26.dp,
        tone = ClayTones.HeroOrange,
        shadowDepth = 6.dp,
        modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card"),
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1.2f)) {
            Text(
              text = "Today Sales",
              color = Color.White.copy(alpha = 0.94f),
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = CurrencyUtils.formatPaise(metrics.todaySalesPaise, sym),
              style =
                TextStyle(
                  color = Color.White,
                  fontSize = 32.sp,
                  fontWeight = FontWeight.ExtraBold,
                  shadow =
                    Shadow(
                      color = Color(0x66000000),
                      offset = Offset(0f, 5f),
                      blurRadius = 10f,
                    ),
                ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Row(
                modifier =
                  Modifier.raised3D(
                    cornerRadius = 50.dp,
                    tone = ClayTone(Color(0xFFFFB25E), Color(0xFFF68A29), Color(0xFFE06410)),
                    shadowDepth = 2.dp,
                  )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.TrendingUp,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text =
                    if (metrics.salesGrowthPercent >= 0) "+${metrics.salesGrowthPercent}%"
                    else "${metrics.salesGrowthPercent}%",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Month: ${CurrencyUtils.formatPaise(metrics.monthlySalesPaise, sym)}",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(Modifier.width(12.dp))

          // Embossed right-side 3D orange clay panel with white rising arrow
          Box(
            modifier =
              Modifier.weight(0.95f)
                .height(86.dp)
                .raised3D(
                  cornerRadius = 20.dp,
                  tone = ClayTone(Color(0xFFFF963E), Color(0xFFF06D16), Color(0xFFD9530A)),
                  shadowDepth = 4.dp,
                )
                .padding(10.dp)
          ) {
            HeroTrendGraphic(modifier = Modifier.fillMaxSize())
          }
        }
      }
    }

    // 2. 2x2 Metric Grid (Total Bills, Customers, Products, Expenses)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricGridCard(
            title = "Total Bills",
            value = metrics.totalBillsCount.toString(),
            icon = Icons.Filled.Receipt,
            iconTint = InfoBlue,
            iconBg = InfoBlueSoft,
            tone = ClayTones.Blue,
            modifier = Modifier.weight(1f),
            onClick = onOpenInvoices,
          )
          MetricGridCard(
            title = "Customers",
            value = metrics.totalCustomersCount.toString(),
            icon = Icons.Filled.Group,
            iconTint = EmeraldGreen,
            iconBg = EmeraldGreenSoft,
            tone = ClayTones.Green,
            modifier = Modifier.weight(1f),
            onClick = onOpenCustomers,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricGridCard(
            title = "Products",
            value = metrics.totalProductsCount.toString(),
            icon = Icons.Filled.Inventory2,
            iconTint = AmberGold,
            iconBg = AmberGoldSoft,
            tone = ClayTones.Amber,
            modifier = Modifier.weight(1f),
            onClick = onOpenProducts,
          )
          MetricGridCard(
            title = "Expenses",
            value = CurrencyUtils.formatPaise(metrics.totalExpensesPaise, sym),
            icon = Icons.Filled.AccountBalanceWallet,
            iconTint = PurpleAccent,
            iconBg = PurpleAccentSoft,
            tone = ClayTones.Purple,
            modifier = Modifier.weight(1f),
            onClick = onOpenExpenses,
          )
        }
      }
    }

    // 3. Outstanding Dues & Low Stock Alerts Strip
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricGridCard(
          title = "Outstanding Due",
          value = CurrencyUtils.formatPaise(metrics.outstandingDuesPaise, sym),
          icon = Icons.Filled.Payments,
          iconTint = AlertRed,
          iconBg = AlertRedSoft,
          tone = ClayTones.Red,
          modifier = Modifier.weight(1f),
          onClick = onOpenCustomers,
        )
        MetricGridCard(
          title = "Low Stock Alerts",
          value = "${metrics.lowStockCount} items",
          icon = Icons.Filled.WarningAmber,
          iconTint = AmberGold,
          iconBg = AmberGoldSoft,
          tone = ClayTones.Amber,
          modifier = Modifier.weight(1f),
          onClick = onOpenInventory,
        )
      }
    }

    // 3b. Pending Orders 3D Card
    item {
      Card3D(
        cornerRadius = 22.dp,
        shadowDepth = 4.dp,
        accentBorderColor =
          if (metrics.pendingOrdersCount > 0) SaffronOrange.copy(alpha = 0.5f) else null,
        onClick = onOpenOrders,
        modifier = Modifier.fillMaxWidth().testTag("dashboard_pending_orders_card"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon3D(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = "Pending Orders",
            tone = ClayTones.Orange,
            size = 34.dp,
          )
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pending Orders",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Text(
              text =
                if (metrics.pendingOrdersCount > 0)
                  "${metrics.pendingOrdersCount} order(s) waiting for final bill & payment"
                else "Take order first, finalize rate & bill later",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(Modifier.width(8.dp))
          Row(
            modifier =
              Modifier.then(
                if (metrics.pendingOrdersCount > 0)
                  Modifier.raised3D(cornerRadius = 50.dp, tone = ClayTones.Orange, shadowDepth = 3.dp)
                else Modifier.inset3D(cornerRadius = 50.dp)
              )
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = metrics.pendingOrdersCount.toString(),
              color = if (metrics.pendingOrdersCount > 0) Color.White else palette.textPrimary,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
            )
            Spacer(Modifier.width(3.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Open Pending Orders",
              tint = if (metrics.pendingOrdersCount > 0) Color.White else palette.textSecondary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    // 4. Sales Overview 3D Bar Chart Card
    item {
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Sales Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChartRange.entries.forEach { range ->
              Chip3D(
                selected = chartRange == range,
                onClick = { chartRange = range },
                label = range.label,
              )
            }
          }
        }

        Spacer(Modifier.height(14.dp))

        if (chartPoints.all { it.second == 0L }) {
          Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Create your first bill to see live sales trends",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
            )
          }
        } else {
          ClayBarChart(
            points = chartPoints,
            tone = ClayTones.HeroOrange,
            labelColor = palette.textSecondary,
            modifier = Modifier.fillMaxWidth().height(145.dp),
          )
        }
      }
    }

    // 5. Recent Bills 3D Card
    item {
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Recent Bills",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Text(
            text = "View All",
            color = SaffronOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onOpenInvoices),
          )
        }
        Spacer(Modifier.height(10.dp))
        if (invoices.isEmpty()) {
          EmptyStatePanel(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "No Bills Yet",
            subtitle = "Tap New Bill to create your first GST invoice.",
            actionLabel = "Create First Bill",
            onAction = onNewBill,
          )
        } else {
          invoices.take(4).forEachIndexed { index, inv ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Card3D(
              cornerRadius = 18.dp,
              shadowDepth = 3.dp,
              onClick = { onOpenInvoicePreview(inv) },
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${inv.invoiceNumber} • ${inv.customerName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = "${CurrencyUtils.formatDateTime(inv.timestamp)} • ${inv.paymentMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = CurrencyUtils.formatPaise(inv.grandTotalPaise, sym),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SaffronOrange,
                  )
                  if (inv.balanceDuePaise > 0) {
                    Text(
                      text = "Due ${CurrencyUtils.formatPaise(inv.balanceDuePaise, sym)}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = AlertRed,
                    )
                  } else {
                    Text(
                      text = "Paid",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = EmeraldGreen,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 6. Quick Actions 3D Grid Card
    item {
      Card3D(
        cornerRadius = 24.dp,
        shadowDepth = 5.dp,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
          )
          Text(
            text = "POS & Masters",
            color = SaffronOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCard3D(
            label = "New Bill",
            icon = Icons.Filled.AddShoppingCart,
            tone = ClayTones.Blue,
            onClick = onNewBill,
            modifier = Modifier.weight(1f).testTag("qa_new_bill"),
          )
          QuickActionCard3D(
            label = "Add Product",
            icon = Icons.Filled.AddBox,
            tone = ClayTones.Orange,
            onClick = onAddProduct,
            modifier = Modifier.weight(1f).testTag("qa_add_product"),
          )
          QuickActionCard3D(
            label = "Add Customer",
            icon = Icons.Filled.PersonAdd,
            tone = ClayTones.Green,
            onClick = onAddCustomer,
            modifier = Modifier.weight(1f).testTag("qa_add_customer"),
          )
        }
        Spacer(Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCard3D(
            label = "Reports",
            icon = Icons.Filled.Assessment,
            tone = ClayTones.Purple,
            onClick = onOpenReports,
            modifier = Modifier.weight(1f),
          )
          QuickActionCard3D(
            label = "Expenses",
            icon = Icons.Filled.AccountBalanceWallet,
            tone = ClayTones.Red,
            onClick = onOpenExpenses,
            modifier = Modifier.weight(1f),
          )
          QuickActionCard3D(
            label = "Invoices",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            tone = ClayTones.Amber,
            onClick = onOpenInvoices,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 7. Top Selling Products 3D Card
    if (topProducts.isNotEmpty()) {
      item {
        Card3D(
          cornerRadius = 24.dp,
          shadowDepth = 5.dp,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Top Selling Products",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.textPrimary,
            )
            Text(
              text = "View All",
              color = SaffronOrange,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable(onClick = onOpenProducts),
            )
          }
          Spacer(Modifier.height(10.dp))
          topProducts.take(4).forEachIndexed { i, itemStat ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Card3D(
              cornerRadius = 18.dp,
              shadowDepth = 3.dp,
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    itemStat.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                  )
                  Text(
                    "Qty sold: ${CurrencyUtils.formatQty(itemStat.qtySold)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                Text(
                  CurrencyUtils.formatPaise(itemStat.revenuePaise, sym),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = EmeraldGreen,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionCard3D(
  label: String,
  icon: ImageVector,
  tone: ClayTone,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalVyaparClayPalette.current
  Card3D(
    cornerRadius = 18.dp,
    shadowDepth = 3.dp,
    onClick = onClick,
    modifier = modifier,
    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      ClayIconPad(
        imageVector = icon,
        contentDescription = label,
        tone = tone,
        padSize = 46.dp,
        iconSize = 24.dp,
        cornerRadius = 15.dp,
        useRecessedPad = true,
      )
      Spacer(Modifier.height(7.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HeroTrendGraphic(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Subtle glowing orange pill bar on bottom right (like screenshot)
    drawRoundRect(
      color = Color.Black.copy(alpha = 0.20f),
      topLeft = Offset(w * 0.78f + 2.dp.toPx(), h * 0.56f + 3.dp.toPx()),
      size = Size(w * 0.16f, h * 0.36f),
      cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    )
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFFB868), Color(0xFFF46B17))),
      topLeft = Offset(w * 0.78f, h * 0.56f),
      size = Size(w * 0.16f, h * 0.36f),
      cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    )

    // 3D white rising zig-zag trend arrow with soft drop shadow
    val linePath =
      Path().apply {
        moveTo(w * 0.08f, h * 0.78f)
        lineTo(w * 0.28f, h * 0.50f)
        lineTo(w * 0.48f, h * 0.64f)
        lineTo(w * 0.86f, h * 0.16f)
      }
    val shadowPath =
      Path().apply {
        moveTo(w * 0.08f, h * 0.83f)
        lineTo(w * 0.28f, h * 0.55f)
        lineTo(w * 0.48f, h * 0.69f)
        lineTo(w * 0.86f, h * 0.21f)
      }
    drawPath(
      path = shadowPath,
      color = Color.Black.copy(alpha = 0.28f),
      style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
      path = linePath,
      color = Color.White,
      style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Crisp arrow head
    val arrowHeadShadow =
      Path().apply {
        moveTo(w * 0.68f, h * 0.19f)
        lineTo(w * 0.90f, h * 0.15f)
        lineTo(w * 0.86f, h * 0.39f)
        close()
      }
    drawPath(path = arrowHeadShadow, color = Color.Black.copy(alpha = 0.22f))
    val arrowHead =
      Path().apply {
        moveTo(w * 0.67f, h * 0.15f)
        lineTo(w * 0.90f, h * 0.10f)
        lineTo(w * 0.86f, h * 0.35f)
        close()
      }
    drawPath(path = arrowHead, color = Color.White)
  }
}

data class TopProductStat(val name: String, val qtySold: Double, val revenuePaise: Long)

fun computeTopProducts(invoices: List<Invoice>): List<TopProductStat> {
  val map = linkedMapOf<String, TopProductStat>()
  for (inv in invoices) {
    for (item in inv.items) {
      val prev = map[item.name]
      if (prev == null) {
        map[item.name] = TopProductStat(item.name, item.quantity, item.lineTotalPaise)
      } else {
        map[item.name] =
          prev.copy(
            qtySold = prev.qtySold + item.quantity,
            revenuePaise = prev.revenuePaise + item.lineTotalPaise,
          )
      }
    }
  }
  return map.values.sortedByDescending { it.revenuePaise }
}

fun computeSalesSeries(invoices: List<Invoice>, range: ChartRange): List<Pair<String, Long>> {
  val cal = Calendar.getInstance()
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)

  return when (range) {
    ChartRange.SEVEN_DAYS -> {
      val fmt = SimpleDateFormat("dd", Locale.getDefault())
      (6 downTo 0).map { daysAgo ->
        val dayStartCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val start = dayStartCal.timeInMillis
        val end = start + 24L * 60L * 60L * 1000L
        val sum = invoices.filter { it.timestamp in start until end }.sumOf { it.grandTotalPaise }
        fmt.format(Date(start)) to sum
      }
    }
    ChartRange.THIRTY_DAYS -> {
      // 6 buckets of 5 days each
      (5 downTo 0).map { bucket ->
        val startCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(bucket * 5 + 4)) }
        val endCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(bucket * 5 - 1)) }
        val start = startCal.timeInMillis
        val end = endCal.timeInMillis
        val label = SimpleDateFormat("d/M", Locale.getDefault()).format(Date(start))
        val sum = invoices.filter { it.timestamp in start until end }.sumOf { it.grandTotalPaise }
        label to sum
      }
    }
    ChartRange.ONE_YEAR -> {
      val fmt = SimpleDateFormat("MMM", Locale.getDefault())
      (5 downTo 0).map { monthsAgo ->
        val mCal =
          (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -monthsAgo)
          }
        val start = mCal.timeInMillis
        val nextCal = (mCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val end = nextCal.timeInMillis
        val sum = invoices.filter { it.timestamp in start until end }.sumOf { it.grandTotalPaise }
        fmt.format(Date(start)) to sum
      }
    }
  }
}
