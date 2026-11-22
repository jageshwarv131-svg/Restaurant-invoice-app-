package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderGreyDark
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.LocalVyaparDarkTheme
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SlateText
import com.example.ui.theme.WarmCreamBorder
import com.example.ui.theme.WarmCreamHeader

@Immutable
data class AppSafeInsets(
  val statusTop: Dp = 0.dp,
  val navBottom: Dp = 48.dp,
  val imeBottom: Dp = 0.dp,
)

val LocalAppSafeInsets = compositionLocalOf { AppSafeInsets() }

@Composable
fun rememberSafeBottomPadding(extraPadding: Dp = 20.dp): Dp {
  val appInsets = LocalAppSafeInsets.current
  val localNav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val localIme = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
  val detectedNav = maxOf(localNav, appInsets.navBottom)
  val effectiveNav = if (detectedNav > 0.dp) detectedNav else 48.dp
  val effectiveIme = maxOf(localIme, appInsets.imeBottom)
  return maxOf(effectiveNav + extraPadding, effectiveIme + 16.dp)
}

@Composable
fun vyaparTopBarContainerColor(): Color =
  if (LocalVyaparDarkTheme.current) CharcoalSurface else WarmCreamHeader

@Composable
fun vyaparTopBarContentColor(): Color =
  if (LocalVyaparDarkTheme.current) Color(0xFFF9FAFB) else SlateText

@Composable
fun vyaparTopBarDividerColor(): Color =
  if (LocalVyaparDarkTheme.current) BorderGreyDark else WarmCreamBorder

@Composable
fun ShopStatusBadge(
  isShopOpen: Boolean,
  onToggleShopStatus: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val tone = if (isShopOpen) ClayTones.Green else ClayTones.Red
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Row(
    modifier =
      modifier
        .raised3D(
          cornerRadius = 50.dp,
          isPressed = onToggleShopStatus != null && isPressed,
          tone = tone,
          shadowDepth = 3.dp,
        )
        .then(
          if (onToggleShopStatus != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = onToggleShopStatus,
            )
          } else Modifier
        )
        .padding(horizontal = 11.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(8.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(
                Color.White,
                if (isShopOpen) EmeraldGreenLight else Color(0xFFFCA5A5),
              )
            )
          )
    )
    Spacer(Modifier.width(5.dp))
    Icon(
      imageVector = Icons.Filled.Storefront,
      contentDescription =
        if (isShopOpen) "Shop Online Open, tap to close"
        else "Shop Offline Closed, tap to open",
      tint = Color.White,
      modifier = Modifier.size(14.dp),
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

@Composable
fun VyaparTopBar(
  title: String,
  isShopOpen: Boolean,
  onToggleShopStatus: (() -> Unit)? = null,
  showBack: Boolean = false,
  onBackOrMenuClick: () -> Unit,
  onAlertClick: (() -> Unit)? = null,
  alertCount: Int = 0,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  val palette = LocalVyaparClayPalette.current
  val iconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

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
          .height(56.dp)
          .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // 3D Hamburger / Back button
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onBackOrMenuClick)
            .testTag(if (showBack) "top_bar_back" else "top_bar_menu"),
      ) {
        Icon3D(
          imageVector =
            if (showBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
          contentDescription = if (showBack) "Back" else "Open navigation menu",
          tone = iconTone,
          size = 24.dp,
        )
      }

      Spacer(Modifier.width(6.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )

      ShopStatusBadge(
        isShopOpen = isShopOpen,
        onToggleShopStatus = onToggleShopStatus,
        modifier = Modifier.padding(end = 6.dp).testTag("shop_status_toggle"),
      )

      if (trailingContent != null) {
        trailingContent()
      }

      if (onAlertClick != null) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(42.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable(onClick = onAlertClick)
              .testTag("top_bar_alerts"),
        ) {
          Icon3D(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Inventory and Payment Alerts",
            tone = iconTone,
            size = 24.dp,
          )
          if (alertCount > 0) {
            Box(
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .padding(top = 3.dp, end = 3.dp)
                  .size(18.dp)
                  .raised3D(cornerRadius = 9.dp, tone = ClayTones.Orange, shadowDepth = 2.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (alertCount > 9) "9+" else "$alertCount",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MetricGridCard(
  title: String,
  value: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  modifier: Modifier = Modifier,
  tone: ClayTone? = null,
  onClick: (() -> Unit)? = null,
) {
  val palette = LocalVyaparClayPalette.current
  val resolvedTone =
    tone
      ?: remember(iconTint) {
        when (iconTint) {
          com.example.ui.theme.InfoBlue -> ClayTones.Blue
          com.example.ui.theme.EmeraldGreen -> ClayTones.Green
          com.example.ui.theme.AmberGold -> ClayTones.Amber
          com.example.ui.theme.PurpleAccent -> ClayTones.Purple
          AlertRed -> ClayTones.Red
          else -> ClayTones.Orange
        }
      }

  Card3D(
    modifier = modifier,
    cornerRadius = 22.dp,
    shadowDepth = 4.dp,
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon3D(
        imageVector = icon,
        contentDescription = title,
        tone = resolvedTone,
        size = 34.dp,
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun EmptyStatePanel(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  val palette = LocalVyaparClayPalette.current
  Column(
    modifier = Modifier.fillMaxWidth().padding(28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    ClayIconPad(
      imageVector = icon,
      contentDescription = title,
      tone = ClayTones.Orange,
      padSize = 78.dp,
      iconSize = 38.dp,
      cornerRadius = 26.dp,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = palette.textPrimary,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = palette.textSecondary,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(18.dp))
      Button3D(
        onClick = onAction,
        cornerRadius = 18.dp,
      ) {
        Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun VyaparFormDialog(
  title: String,
  onDismiss: () -> Unit,
  scrollContent: @Composable ColumnScope.() -> Unit,
  bottomButtons: @Composable RowScope.() -> Unit,
) {
  val appInsets = LocalAppSafeInsets.current
  val palette = LocalVyaparClayPalette.current

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
      dialogWindow?.let { w ->
        WindowCompat.setDecorFitsSystemWindows(w, false)
        w.setLayout(
          android.view.WindowManager.LayoutParams.MATCH_PARENT,
          android.view.WindowManager.LayoutParams.MATCH_PARENT,
        )
        w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
      }
    }

    val localStatusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val localNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val localImeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val safeTop = maxOf(localStatusTop, appInsets.statusTop, 24.dp) + 12.dp
    val detectedNav = maxOf(localNavBottom, appInsets.navBottom)
    val effectiveNav = if (detectedNav > 0.dp) detectedNav else 48.dp
    val effectiveIme = maxOf(localImeBottom, appInsets.imeBottom)
    val safeBottom = maxOf(effectiveNav + 28.dp, effectiveIme + 16.dp)

    Box(
      modifier =
        Modifier.fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss,
          )
          .padding(start = 14.dp, end = 14.dp, top = safeTop, bottom = safeBottom),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 560.dp)
            .fillMaxWidth()
            .raised3D(cornerRadius = 28.dp, shadowDepth = 8.dp)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {},
            ),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
          modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        )

        Column(
          modifier =
            Modifier.weight(1f, fill = false)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          content = scrollContent,
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
          color = palette.glossBorderBottom.copy(alpha = 0.45f),
          modifier = Modifier.padding(horizontal = 16.dp),
        )

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          content = bottomButtons,
        )
      }
    }
  }
}

/**
 * 3D Clay Bar Chart with glossy orange bars, rounded tops, soft drop shadows, and subtle axis labels.
 */
@Composable
fun ClayBarChart(
  points: List<Pair<String, Long>>,
  tone: ClayTone = ClayTones.HeroOrange,
  labelColor: Color,
  modifier: Modifier = Modifier,
) {
  val palette = LocalVyaparClayPalette.current
  val maxVal = (points.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
  Column(modifier = modifier) {
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .drawBehind {
            val count = points.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.56f).coerceIn(14.dp.toPx(), 34.dp.toPx())
            val baselineY = size.height - 2.dp.toPx()

            // Subtle baseline line
            drawLine(
              color = palette.textSecondary.copy(alpha = 0.22f),
              start = Offset(6.dp.toPx(), baselineY),
              end = Offset(size.width - 6.dp.toPx(), baselineY),
              strokeWidth = 1.dp.toPx(),
            )

            points.forEachIndexed { idx, (_, value) ->
              val ratio = value.toFloat() / maxVal.toFloat()
              val barH =
                if (value > 0) (ratio * (baselineY - 12.dp.toPx())).coerceAtLeast(14.dp.toPx())
                else 8.dp.toPx()
              val x = idx * slotW + (slotW - barW) / 2f
              val y = baselineY - barH
              val cr = CornerRadius(9.dp.toPx(), 9.dp.toPx())

              if (value > 0) {
                // Drop shadow under/right of bar
                drawRoundRect(
                  color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.55f else 0.22f),
                  topLeft = Offset(x + 2.5.dp.toPx(), y + 3.dp.toPx()),
                  size = Size(barW, barH),
                  cornerRadius = cr,
                )
                // Glossy vertical clay bar
                drawRoundRect(
                  brush = Brush.verticalGradient(listOf(tone.top, tone.mid, tone.bottom)),
                  topLeft = Offset(x, y),
                  size = Size(barW, barH),
                  cornerRadius = cr,
                )
                // Inner gloss highlight rim
                val sw = 1.dp.toPx()
                drawRoundRect(
                  brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.65f), Color.Transparent)
                  ),
                  topLeft = Offset(x + sw / 2f, y + sw / 2f),
                  size = Size(barW - sw, barH - sw),
                  cornerRadius = cr,
                  style = Stroke(width = sw),
                )
              } else {
                drawRoundRect(
                  color = palette.insetBg,
                  topLeft = Offset(x, y),
                  size = Size(barW, barH),
                  cornerRadius = cr,
                )
              }
            }
          }
    )
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
      points.forEach { (label, _) ->
        Text(
          text = label,
          color = labelColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
