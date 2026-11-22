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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class VyaparClayPalette(
  val isDark: Boolean,
  val background: Color,
  val surface: Color,
  val surfaceTop: Color,
  val surfaceBottom: Color,
  val surfaceElevated: Color,
  val insetBg: Color,
  val insetShadow: Color,
  val shadowDark: Color,
  val highlightLight: Color,
  val glossBorderTop: Color,
  val glossBorderBottom: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val neutralIconTop: Color,
  val neutralIconBottom: Color,
)

val DarkClayPalette =
  VyaparClayPalette(
    isDark = true,
    background = CharcoalBlack,
    surface = CharcoalSurface,
    surfaceTop = CharcoalSurfaceTop,
    surfaceBottom = CharcoalSurfaceBottom,
    surfaceElevated = CharcoalElevated,
    insetBg = CharcoalInsetBg,
    insetShadow = Color(0xFF090A0D),
    shadowDark = CharcoalShadowDark,
    highlightLight = Color(0x2BFFFFFF),
    glossBorderTop = Color(0x36FFFFFF),
    glossBorderBottom = Color(0x88000000),
    textPrimary = Color(0xFFF9FAFB),
    textSecondary = Color(0xFF9CA3AF),
    neutralIconTop = Color(0xFFE5E7EB),
    neutralIconBottom = Color(0xFF8B929E),
  )

val LightClayPalette =
  VyaparClayPalette(
    isDark = false,
    background = SoftGreyBg,
    surface = CardWhite,
    surfaceTop = CreamSurfaceTop,
    surfaceBottom = CreamSurfaceBottom,
    surfaceElevated = CreamElevated,
    insetBg = CreamInsetBg,
    insetShadow = Color(0xFFBDB3A6),
    shadowDark = CreamShadowDark,
    highlightLight = CreamHighlight,
    glossBorderTop = Color(0xF0FFFFFF),
    glossBorderBottom = Color(0x29887765),
    textPrimary = SlateText,
    textSecondary = MutedText,
    neutralIconTop = Color(0xFF6B7280),
    neutralIconBottom = Color(0xFF374151),
  )

val LocalVyaparDarkTheme = compositionLocalOf { false }
val LocalVyaparClayPalette = compositionLocalOf { LightClayPalette }

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
    outlineVariant = Color(0xFF2D313B),
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
    surfaceVariant = CreamInsetBg,
    onSurfaceVariant = MutedText,
    outline = WarmCreamBorder,
    outlineVariant = DividerGrey,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedSoft,
    onErrorContainer = AlertRed,
  )

val VyaparShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun VyaparBillingTheme(
  darkTheme: Boolean = false,
  lightStatusBar: Boolean = !darkTheme,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) VyaparDarkColorScheme else VyaparLightColorScheme
  val clayPalette = if (darkTheme) DarkClayPalette else LightClayPalette
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

  CompositionLocalProvider(
    LocalVyaparDarkTheme provides darkTheme,
    LocalVyaparClayPalette provides clayPalette,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = VyaparTypography,
      shapes = VyaparShapes,
      content = content,
    )
  }
}
