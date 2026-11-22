package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessProfile
import com.example.model.Invoice
import com.example.model.Product
import com.example.ui.DashboardMetrics
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MetricGridCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedSoft
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldSoft
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenSoft
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueSoft
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
  val sym = business.currencySymbol.ifBlank { "₹" }
  var chartRange by remember { mutableStateOf(ChartRange.SEVEN_DAYS) }

  val chartPoints =
    remember(invoices, chartRange) { computeSalesSeries(invoices, chartRange) }
  val topProducts = remember(invoices) { computeTopProducts(invoices) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Hero Orange Today Sales Card (exact mockup style)
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SaffronOrange,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card"),
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.linearGradient(
                  listOf(SaffronOrange, SaffronOrangeDark)
                )
              )
              .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1.3f)) {
              Text(
                text = "Today Sales",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = CurrencyUtils.formatPaise(metrics.todaySalesPaise, sym),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color.White.copy(alpha = 0.2f),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "Month: ${CurrencyUtils.formatPaise(metrics.monthlySalesPaise, sym)}",
                  color = Color.White.copy(alpha = 0.92f),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                )
              }
            }

            // Sparkline + Mini Bars matching hero right side
            MiniHeroChart(
              values = chartPoints.map { it.second.toFloat() },
              modifier = Modifier.weight(0.9f).height(66.dp),
            )
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
            modifier = Modifier.weight(1f),
            onClick = onOpenInvoices,
          )
          MetricGridCard(
            title = "Customers",
            value = metrics.totalCustomersCount.toString(),
            icon = Icons.Filled.Group,
            iconTint = EmeraldGreen,
            iconBg = EmeraldGreenSoft,
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
            modifier = Modifier.weight(1f),
            onClick = onOpenProducts,
          )
          MetricGridCard(
            title = "Expenses",
            value = CurrencyUtils.formatPaise(metrics.totalExpensesPaise, sym),
            icon = Icons.Filled.AccountBalanceWallet,
            iconTint = PurpleAccent,
            iconBg = PurpleAccentSoft,
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
          title = "Outstanding Dues",
          value = CurrencyUtils.formatPaise(metrics.outstandingDuesPaise, sym),
          icon = Icons.Filled.Payments,
          iconTint = AlertRed,
          iconBg = AlertRedSoft,
          modifier = Modifier.weight(1f),
          onClick = onOpenCustomers,
        )
        MetricGridCard(
          title = "Low Stock Alerts",
          value = "${metrics.lowStockCount} items",
          icon = Icons.Filled.WarningAmber,
          iconTint = SaffronOrangeDark,
          iconBg = AmberGoldSoft,
          modifier = Modifier.weight(1f),
          onClick = onOpenInventory,
        )
      }
    }

    // 3b. Pending Orders Card (tap to open Pending Orders)
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border =
          BorderStroke(
            1.dp,
            if (metrics.pendingOrdersCount > 0) SaffronOrange.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          ),
        modifier =
          Modifier.fillMaxWidth()
            .clickable(onClick = onOpenOrders)
            .testTag("dashboard_pending_orders_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmberGoldSoft),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.LocalShipping,
              contentDescription = "Pending Orders",
              tint = SaffronOrangeDark,
              modifier = Modifier.size(23.dp),
            )
          }
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pending Orders",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (metrics.pendingOrdersCount > 0)
                  "${metrics.pendingOrdersCount} order(s) waiting for final bill & payment"
                else "Take order first, finalize rate & bill later",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (metrics.pendingOrdersCount > 0) SaffronOrange
              else MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = metrics.pendingOrdersCount.toString(),
              color =
                if (metrics.pendingOrdersCount > 0) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Spacer(Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open Pending Orders",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // 4. Sales Overview Bar Chart Card (matching mockup)
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Sales Overview",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              ChartRange.entries.forEach { range ->
                val selected = chartRange == range
                Surface(
                  shape = RoundedCornerShape(50),
                  color =
                    if (selected) SaffronOrange
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier.clickable { chartRange = range },
                ) {
                  Text(
                    text = range.label,
                    color =
                      if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            SalesBarChart(
              points = chartPoints,
              barColor = SaffronOrange,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().height(140.dp),
            )
          }
        }
      }
    }

    // 5. Quick Actions Grid (New Bill, Add Product, Add Customer, Reports, Expenses, Invoices)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Quick Actions",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCard(
            label = "New Bill",
            icon = Icons.Filled.AddShoppingCart,
            tint = InfoBlue,
            onClick = onNewBill,
            modifier = Modifier.weight(1f).testTag("qa_new_bill"),
          )
          QuickActionCard(
            label = "Add Product",
            icon = Icons.Filled.AddBox,
            tint = SaffronOrange,
            onClick = onAddProduct,
            modifier = Modifier.weight(1f).testTag("qa_add_product"),
          )
          QuickActionCard(
            label = "Add Customer",
            icon = Icons.Filled.PersonAdd,
            tint = EmeraldGreen,
            onClick = onAddCustomer,
            modifier = Modifier.weight(1f).testTag("qa_add_customer"),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCard(
            label = "Reports",
            icon = Icons.Filled.Assessment,
            tint = PurpleAccent,
            onClick = onOpenReports,
            modifier = Modifier.weight(1f),
          )
          QuickActionCard(
            label = "Expenses",
            icon = Icons.Filled.AccountBalanceWallet,
            tint = AlertRed,
            onClick = onOpenExpenses,
            modifier = Modifier.weight(1f),
          )
          QuickActionCard(
            label = "Invoices",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            tint = AmberGold,
            onClick = onOpenInvoices,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 6. Recent Bills
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Recent Bills",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "View All",
              color = SaffronOrange,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
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
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().clickable { onOpenInvoicePreview(inv) },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${inv.invoiceNumber} • ${inv.customerName}",
                      style = MaterialTheme.typography.titleSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${CurrencyUtils.formatDateTime(inv.timestamp)} • ${inv.paymentMethod.label}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = CurrencyUtils.formatPaise(inv.grandTotalPaise, sym),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = SaffronOrangeDark,
                    )
                    if (inv.balanceDuePaise > 0) {
                      Text(
                        text = "Due ${CurrencyUtils.formatPaise(inv.balanceDuePaise, sym)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertRed,
                      )
                    } else {
                      Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
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
    }

    // 7. Top Products
    if (topProducts.isNotEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              "Top Selling Products",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            topProducts.take(4).forEachIndexed { i, itemStat ->
              if (i > 0) Spacer(Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(itemStat.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "Qty sold: ${CurrencyUtils.formatQty(itemStat.qtySold)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  CurrencyUtils.formatPaise(itemStat.revenuePaise, sym),
                  style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun QuickActionCard(
  label: String,
  icon: ImageVector,
  tint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    modifier = modifier.clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier =
          Modifier.size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MiniHeroChart(values: List<Float>, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val pts = if (values.isEmpty() || values.all { it == 0f }) listOf(2f, 4f, 3f, 5f, 4f, 6f, 7f) else values
    val maxVal = (pts.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val count = pts.size.coerceAtLeast(2)
    val barW = (size.width / count) * 0.52f
    val step = size.width / count

    pts.forEachIndexed { idx, v ->
      val h = (v / maxVal) * (size.height * 0.72f)
      val x = idx * step + (step - barW) / 2f
      drawRoundRect(
        color = Color.White.copy(alpha = 0.28f),
        topLeft = Offset(x, size.height - h),
        size = Size(barW, h),
        cornerRadius = CornerRadius(4f, 4f),
      )
    }

    val path = Path()
    pts.forEachIndexed { idx, v ->
      val x = idx * step + step / 2f
      val y = size.height - ((v / maxVal) * (size.height * 0.85f)) - 4f
      if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = Color.White, style = Stroke(width = 4f, cap = StrokeCap.Round))
  }
}

@Composable
fun SalesBarChart(
  points: List<Pair<String, Long>>,
  barColor: Color,
  labelColor: Color,
  modifier: Modifier = Modifier,
) {
  val maxVal = (points.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
  Column(modifier = modifier) {
    Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
      val count = points.size.coerceAtLeast(1)
      val slotW = size.width / count
      val barW = (slotW * 0.56f).coerceIn(10f, 44f)

      points.forEachIndexed { idx, (_, value) ->
        val ratio = value.toFloat() / maxVal.toFloat()
        val barH = (ratio * (size.height - 12f)).coerceAtLeast(if (value > 0) 8f else 2f)
        val x = idx * slotW + (slotW - barW) / 2f
        val y = size.height - barH
        drawRoundRect(
          color = if (value > 0) barColor else barColor.copy(alpha = 0.16f),
          topLeft = Offset(x, y),
          size = Size(barW, barH),
          cornerRadius = CornerRadius(10f, 10f),
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
      points.forEach { (label, _) ->
        Text(
          text = label,
          color = labelColor,
          fontSize = 10.sp,
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.weight(1f),
        )
      }
    }
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
