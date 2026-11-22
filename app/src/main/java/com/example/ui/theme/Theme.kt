package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalVyaparDarkTheme = compositionLocalOf { false }

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private val VyaparDarkColorScheme =
  darkColorScheme(
    primary = SaffronOrange,
    onPrimary = Color.White,
    primaryContainer = SaffronOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = EmeraldGreenSoft,
    tertiary = InfoBlue,
    onTertiary = Color.White,
    background = CharcoalBlack,
    onBackground = Color(0xFFF9FAFB),
    surface = CharcoalSurface,
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = BorderGreyDark,
    outlineVariant = Color(0xFF2C303A),
    error = Color(0xFFF87171),
    onError = CharcoalBlack,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = AlertRedSoft,
  )

private val VyaparLightColorScheme =
  lightColorScheme(
    primary = SaffronOrange,
    onPrimary = Color.White,
    primaryContainer = SaffronOrangeLight,
    onPrimaryContainer = SaffronOrangeDark,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = EmeraldGreenSoft,
    onSecondaryContainer = EmeraldGreenDark,
    tertiary = InfoBlue,
    onTertiary = Color.White,
    background = SoftGreyBg,
    onBackground = SlateText,
    surface = CardWhite,
    onSurface = SlateText,
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = MutedText,
    outline = DividerGrey,
    outlineVariant = Color(0xFFF1F5F9),
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedSoft,
    onErrorContainer = AlertRed,
  )

val VyaparShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun VyaparBillingTheme(
  darkTheme: Boolean = false,
  lightStatusBar: Boolean = !darkTheme,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) VyaparDarkColorScheme else VyaparLightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      view.context.findActivity()?.window?.let { window ->
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = lightStatusBar
        controller.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  CompositionLocalProvider(LocalVyaparDarkTheme provides darkTheme) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = VyaparTypography,
      shapes = VyaparShapes,
      content = content,
    )
  }
}
