package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange

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
  Surface(color = CharcoalHeader, contentColor = Color.White, tonalElevation = 0.dp) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .statusBarsPadding()
          .height(60.dp)
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBackOrMenuClick,
        modifier = Modifier.testTag(if (showBack) "top_bar_back" else "top_bar_menu"),
      ) {
        Icon(
          imageVector =
            if (showBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
          contentDescription = if (showBack) "Back" else "Open navigation menu",
          tint = Color.White,
        )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(start = 4.dp),
      )

      // Interactive Shop Status Toggle: Online (Shop Open) vs Offline (Dukan Close)
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
          Modifier.padding(end = 6.dp)
            .clip(RoundedCornerShape(50))
            .then(
              if (onToggleShopStatus != null) Modifier.clickable(onClick = onToggleShopStatus)
              else Modifier
            )
            .testTag("shop_status_toggle"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .clip(CircleShape)
                .background(if (isShopOpen) Color(0xFF4ADE80) else Color(0xFFFCA5A5))
          )
          Spacer(Modifier.width(5.dp))
          Icon(
            imageVector = Icons.Filled.Storefront,
            contentDescription = if (isShopOpen) "Shop Online Open, tap to close" else "Shop Offline Closed, tap to open",
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

      if (trailingContent != null) {
        trailingContent()
      }

      if (onAlertClick != null) {
        Box {
          IconButton(onClick = onAlertClick, modifier = Modifier.testTag("top_bar_alerts")) {
            Icon(
              imageVector = Icons.Filled.NotificationsNone,
              contentDescription = "Inventory and Payment Alerts",
              tint = Color.White,
            )
          }
          if (alertCount > 0) {
            Box(
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .padding(top = 8.dp, end = 8.dp)
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(SaffronOrange),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (alertCount > 9) "9+" else "$alertCount",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
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
  onClick: (() -> Unit)? = null,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    shadowElevation = 2.dp,
    modifier =
      modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconTint,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
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
  Column(
    modifier = Modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.size(72.dp)
          .clip(CircleShape)
          .background(SaffronOrange.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = SaffronOrange,
        modifier = Modifier.size(36.dp),
      )
    }
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(18.dp))
      Button(
        onClick = onAction,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
      ) {
        Text(actionLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
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
          .padding(start = 16.dp, end = 16.dp, top = safeTop, bottom = safeBottom),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier =
          Modifier.widthIn(max = 560.dp)
            .fillMaxWidth()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {},
            ),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
          )

          Column(
            modifier =
              Modifier.weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = scrollContent,
          )

          Spacer(Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = bottomButtons,
          )
        }
      }
    }
  }
}
