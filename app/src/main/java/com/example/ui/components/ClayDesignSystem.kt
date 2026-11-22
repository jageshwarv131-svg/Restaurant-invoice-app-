package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedDeep
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldDeep
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueDeep
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentDeep
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeBright
import com.example.ui.theme.SaffronOrangeDark
import com.example.ui.theme.SaffronOrangeDeep
import com.example.ui.theme.VyaparClayPalette

@Immutable
data class ClayTone(
  val top: Color,
  val mid: Color,
  val bottom: Color,
)

object ClayTones {
  val Orange = ClayTone(SaffronOrangeBright, SaffronOrange, SaffronOrangeDark)
  val HeroOrange = ClayTone(Color(0xFFFF9A3C), Color(0xFFF46B17), Color(0xFFE8590C))
  val Blue = ClayTone(InfoBlueLight, InfoBlue, InfoBlueDeep)
  val Green = ClayTone(EmeraldGreenLight, EmeraldGreen, EmeraldGreenDark)
  val Amber = ClayTone(AmberGoldLight, AmberGold, AmberGoldDeep)
  val Purple = ClayTone(PurpleAccentLight, PurpleAccent, PurpleAccentDeep)
  val Red = ClayTone(AlertRedLight, AlertRed, AlertRedDeep)
}

/**
 * Lightweight 3D soft-clay raised modifier:
 * - Bottom-right deep drop shadow + top-left soft highlight glow
 * - Vertical glossy surface gradient
 * - 1dp diagonal gloss bevel border
 * - Smooth pressed inset + subtle scale-down (0.97f)
 */
fun Modifier.raised3D(
  cornerRadius: Dp = 24.dp,
  isPressed: Boolean = false,
  tone: ClayTone? = null,
  accentBorderColor: Color? = null,
  shadowDepth: Dp = 5.dp,
): Modifier = composed {
  val palette = LocalVyaparClayPalette.current
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "clay_scale",
  )

  val topFill = tone?.top ?: palette.surfaceTop
  val midFill = tone?.mid ?: palette.surface
  val bottomFill = tone?.bottom ?: palette.surfaceBottom
  val isAccent = tone != null

  val surfaceBrush = remember(topFill, midFill, bottomFill, isPressed) {
    if (isPressed) {
      Brush.verticalGradient(listOf(bottomFill, midFill, topFill))
    } else {
      Brush.verticalGradient(listOf(topFill, midFill, bottomFill))
    }
  }
  val borderTop =
    when {
      isAccent -> Color.White.copy(alpha = 0.55f)
      else -> palette.glossBorderTop
    }
  val borderBottom =
    when {
      isAccent -> Color.Black.copy(alpha = 0.25f)
      else -> palette.glossBorderBottom
    }
  val bevelBrush = remember(borderTop, borderBottom, isPressed) {
    if (isPressed) {
      Brush.linearGradient(listOf(borderBottom, Color.Transparent, borderTop))
    } else {
      Brush.linearGradient(listOf(borderTop, Color.Transparent, borderBottom))
    }
  }

  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .drawBehind {
      val r = cornerRadius.toPx()
      val cr = CornerRadius(r, r)
      val d = shadowDepth.toPx()

      if (!isPressed) {
        // Outer soft bottom-right shadow (2 layered passes for smooth depth without lag)
        drawRoundRect(
          color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.48f else 0.20f),
          topLeft = Offset(d * 0.45f, d * 0.65f),
          size = Size(size.width, size.height),
          cornerRadius = cr,
        )
        drawRoundRect(
          color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.24f else 0.11f),
          topLeft = Offset(d * 0.9f, d * 1.25f),
          size = Size(size.width, size.height),
          cornerRadius = cr,
        )
        // Outer soft top-left light highlight rim
        drawRoundRect(
          color =
            if (isAccent) topFill.copy(alpha = if (palette.isDark) 0.26f else 0.30f)
            else palette.highlightLight.copy(alpha = if (palette.isDark) 0.10f else 0.72f),
          topLeft = Offset(-d * 0.35f, -d * 0.45f),
          size = Size(size.width, size.height),
          cornerRadius = cr,
        )
      }

      // Main glossy clay surface
      drawRoundRect(brush = surfaceBrush, cornerRadius = cr)

      if (isPressed) {
        // Inner top-left pressed shadow
        drawRoundRect(
          color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.45f else 0.22f),
          topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
          size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
          cornerRadius = cr,
          style = Stroke(width = 2.dp.toPx()),
        )
      }

      // 1dp Gloss bevel border
      val strokeW = 1.1.dp.toPx()
      drawRoundRect(
        brush = bevelBrush,
        topLeft = Offset(strokeW / 2f, strokeW / 2f),
        size = Size(size.width - strokeW, size.height - strokeW),
        cornerRadius = cr,
        style = Stroke(width = strokeW),
      )

      if (accentBorderColor != null) {
        drawRoundRect(
          color = accentBorderColor,
          topLeft = Offset(strokeW, strokeW),
          size = Size(size.width - strokeW * 2, size.height - strokeW * 2),
          cornerRadius = cr,
          style = Stroke(width = 1.4.dp.toPx()),
        )
      }
    }
    .clip(RoundedCornerShape(cornerRadius))
}

/**
 * Recessed 3D soft-clay inset modifier for search inputs, text fields, and unselected pills.
 */
fun Modifier.inset3D(
  cornerRadius: Dp = 18.dp,
  focused: Boolean = false,
  isError: Boolean = false,
  paletteOverride: VyaparClayPalette? = null,
): Modifier = composed {
  val palette = paletteOverride ?: LocalVyaparClayPalette.current
  val borderColor by animateColorAsState(
    targetValue =
      when {
        isError -> AlertRed
        focused -> SaffronOrange
        else -> palette.glossBorderBottom.copy(alpha = if (palette.isDark) 0.65f else 0.35f)
      },
    label = "inset_border",
  )

  this
    .drawBehind {
      val r = cornerRadius.toPx()
      val cr = CornerRadius(r, r)

      // Recessed cavity fill
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            listOf(
              palette.insetBg,
              if (palette.isDark) palette.insetBg.copy(alpha = 0.92f) else palette.surfaceBottom,
            )
          ),
        cornerRadius = cr,
      )

      // Inner top-left shadow lip
      val innerStroke = 2.dp.toPx()
      drawRoundRect(
        brush =
          Brush.linearGradient(
            listOf(
              palette.insetShadow.copy(alpha = if (palette.isDark) 0.85f else 0.55f),
              Color.Transparent,
              palette.highlightLight.copy(alpha = if (palette.isDark) 0.14f else 0.78f),
            )
          ),
        topLeft = Offset(innerStroke / 2f, innerStroke / 2f),
        size = Size(size.width - innerStroke, size.height - innerStroke),
        cornerRadius = cr,
        style = Stroke(width = innerStroke),
      )

      // Focus glow or subtle outline
      if (focused || isError) {
        drawRoundRect(
          color = borderColor.copy(alpha = 0.25f),
          topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
          size = Size(size.width + 2.dp.toPx(), size.height + 2.dp.toPx()),
          cornerRadius = cr,
          style = Stroke(width = 3.dp.toPx()),
        )
      }
      drawRoundRect(
        color = borderColor,
        topLeft = Offset(0.6.dp.toPx(), 0.6.dp.toPx()),
        size = Size(size.width - 1.2.dp.toPx(), size.height - 1.2.dp.toPx()),
        cornerRadius = cr,
        style = Stroke(width = if (focused || isError) 1.6.dp.toPx() else 0.9.dp.toPx()),
      )
    }
    .clip(RoundedCornerShape(cornerRadius))
}

@Composable
fun Card3D(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 24.dp,
  tone: ClayTone? = null,
  accentBorderColor: Color? = null,
  shadowDepth: Dp = 5.dp,
  onClick: (() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val palette = LocalVyaparClayPalette.current
  val contentColor = if (tone != null) Color.White else palette.textPrimary

  CompositionLocalProvider(LocalContentColor provides contentColor) {
    Column(
      modifier =
        modifier
          .raised3D(
            cornerRadius = cornerRadius,
            isPressed = onClick != null && isPressed,
            tone = tone,
            accentBorderColor = accentBorderColor,
            shadowDepth = shadowDepth,
          )
          .then(
            if (onClick != null) {
              Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
              )
            } else Modifier
          )
          .padding(contentPadding),
      content = content,
    )
  }
}

@Composable
fun Button3D(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true,
  tone: ClayTone = ClayTones.Orange,
  cornerRadius: Dp = 18.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val activeTone =
    when {
      !enabled -> null
      isPrimary -> tone
      else -> null
    }
  val contentColor =
    when {
      !enabled -> palette.textSecondary.copy(alpha = 0.6f)
      isPrimary -> Color.White
      else -> palette.textPrimary
    }

  CompositionLocalProvider(LocalContentColor provides contentColor) {
    Row(
      modifier =
        modifier
          .defaultMinSize(minHeight = 48.dp)
          .then(
            if (!enabled) {
              Modifier.inset3D(cornerRadius = cornerRadius)
            } else {
              Modifier.raised3D(
                cornerRadius = cornerRadius,
                isPressed = isPressed,
                tone = activeTone,
                shadowDepth = if (isPrimary) 5.dp else 4.dp,
              )
            }
          )
          .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
          )
          .padding(contentPadding),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}

@Composable
fun Chip3D(
  selected: Boolean,
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  selectedTone: ClayTone = ClayTones.Orange,
) {
  val palette = LocalVyaparClayPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minHeight = 36.dp)
        .then(
          if (selected) {
            Modifier.raised3D(
              cornerRadius = 50.dp,
              isPressed = isPressed,
              tone = selectedTone,
              shadowDepth = 3.dp,
            )
          } else {
            Modifier.inset3D(cornerRadius = 50.dp)
          }
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 14.dp, vertical = 7.dp),
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else palette.textSecondary,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun TextField3D(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  placeholder: String? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  prefix: (@Composable () -> Unit)? = null,
  isError: Boolean = false,
  supportingText: (@Composable () -> Unit)? = null,
  singleLine: Boolean = true,
  minLines: Int = 1,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  cornerRadius: Dp = 18.dp,
) {
  val palette = LocalVyaparClayPalette.current
  var focused by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .inset3D(cornerRadius = cornerRadius, focused = focused, isError = isError)
    ) {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = palette.textSecondary.copy(alpha = 0.7f)) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(cornerRadius),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            focusedLabelColor = SaffronOrange,
            unfocusedLabelColor = palette.textSecondary,
            cursorColor = SaffronOrange,
          ),
        modifier =
          Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
      )
    }
    if (supportingText != null) {
      Box(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
        CompositionLocalProvider(LocalContentColor provides if (isError) AlertRed else palette.textSecondary) {
          supportingText()
        }
      }
    }
  }
}

/**
 * 3D Clay / Glossy Icon:
 * Renders a soft offset drop shadow underneath, a darkened 3D depth edge,
 * and a vertical glossy gradient + diagonal specular shine clipped to the icon shape.
 */
@Composable
fun Icon3D(
  imageVector: ImageVector,
  contentDescription: String?,
  tone: ClayTone,
  modifier: Modifier = Modifier,
  size: Dp = 30.dp,
) {
  val palette = LocalVyaparClayPalette.current
  val gradientBrush = remember(tone) {
    Brush.verticalGradient(listOf(tone.top, tone.mid, tone.bottom))
  }
  val sheenBrush = remember {
    Brush.linearGradient(
      colors = listOf(Color.White.copy(alpha = 0.48f), Color.Transparent),
      start = Offset.Zero,
      end = Offset(90f, 90f),
    )
  }

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    // 1. Soft drop shadow underneath
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      tint = palette.shadowDark.copy(alpha = if (palette.isDark) 0.58f else 0.28f),
      modifier =
        Modifier.size(size)
          .graphicsLayer {
            translationX = 1.6.dp.toPx()
            translationY = 2.6.dp.toPx()
          },
    )
    // 2. Extruded lower clay rim
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      tint = tone.bottom.copy(alpha = 0.85f),
      modifier =
        Modifier.size(size)
          .graphicsLayer {
            translationX = 0.4.dp.toPx()
            translationY = 1.1.dp.toPx()
          },
    )
    // 3. Main glossy clay gradient + top-left specular highlight
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier =
        Modifier.size(size)
          .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
          .drawWithContent {
            drawContent()
            drawRect(brush = gradientBrush, blendMode = BlendMode.SrcAtop)
            drawRect(brush = sheenBrush, blendMode = BlendMode.SrcAtop)
          },
    )
  }
}

/**
 * Raised or recessed 3D clay square pad housing a glowing 3D icon (like Quick Actions in reference).
 */
@Composable
fun ClayIconPad(
  imageVector: ImageVector,
  contentDescription: String?,
  tone: ClayTone,
  modifier: Modifier = Modifier,
  padSize: Dp = 54.dp,
  iconSize: Dp = 28.dp,
  cornerRadius: Dp = 18.dp,
  useRecessedPad: Boolean = false,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(padSize)
        .then(
          if (useRecessedPad) Modifier.inset3D(cornerRadius = cornerRadius)
          else Modifier.raised3D(cornerRadius = cornerRadius, shadowDepth = 4.dp)
        ),
  ) {
    Icon3D(
      imageVector = imageVector,
      contentDescription = contentDescription,
      tone = tone,
      size = iconSize,
    )
  }
}
