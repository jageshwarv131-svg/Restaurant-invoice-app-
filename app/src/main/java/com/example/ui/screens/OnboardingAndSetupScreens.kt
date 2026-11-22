package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BusinessProfile
import com.example.ui.components.Button3D
import com.example.ui.components.Card3D
import com.example.ui.components.Chip3D
import com.example.ui.components.ClayIconPad
import com.example.ui.components.ClayTone
import com.example.ui.components.ClayTones
import com.example.ui.components.Icon3D
import com.example.ui.components.TextField3D
import com.example.ui.components.inset3D
import com.example.ui.components.raised3D
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalVyaparClayPalette
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  isDataLoaded: Boolean = true,
  onSplashFinished: () -> Unit,
) {
  val palette = LocalVyaparClayPalette.current
  val currentLoaded by rememberUpdatedState(isDataLoaded)

  val logoScale = remember { Animatable(0.85f) }
  val logoAlpha = remember { Animatable(0f) }
  val logoLift = remember { Animatable(26f) }
  val glossSweep = remember { Animatable(-0.5f) }
  val textAlpha = remember { Animatable(0f) }
  var minDelayElapsed by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    launch {
      logoAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
    }
    launch {
      logoScale.animateTo(1f, animationSpec = tween(620, easing = FastOutSlowInEasing))
    }
    launch {
      logoLift.animateTo(0f, animationSpec = tween(620, easing = FastOutSlowInEasing))
    }
    launch {
      delay(220)
      textAlpha.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
    }
    launch {
      delay(340)
      glossSweep.animateTo(1.5f, animationSpec = tween(720, easing = FastOutSlowInEasing))
    }
    delay(1800)
    minDelayElapsed = true
  }

  LaunchedEffect(minDelayElapsed, isDataLoaded) {
    if (minDelayElapsed && currentLoaded) {
      onSplashFinished()
    }
  }

  val infinite = rememberInfiniteTransition(label = "splash_dots")
  val dotPhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 3f,
    animationSpec = infiniteRepeatable(tween(950), RepeatMode.Restart),
    label = "dot_phase",
  )

  Box(
    modifier = Modifier.fillMaxSize().background(palette.background),
    contentAlignment = Alignment.Center,
  ) {
    // Subtle saffron 3D radial aura
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                SaffronOrange.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                Color.Transparent,
              ),
            center = Offset(size.width * 0.5f, size.height * 0.40f),
            radius = size.minDimension * 0.62f,
          ),
        center = Offset(size.width * 0.5f, size.height * 0.40f),
        radius = size.minDimension * 0.62f,
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(Modifier.height(24.dp))

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.graphicsLayer {
            alpha = logoAlpha.value
            scaleX = logoScale.value
            scaleY = logoScale.value
            translationY = logoLift.value
          },
      ) {
        // 3D Glossy Orange Clay Receipt + Growth Logo
        ClayBrandLogoMark(
          glossProgress = glossSweep.value,
          modifier = Modifier.size(134.dp),
        )

        Spacer(Modifier.height(28.dp))

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
        ) {
          Text(
            text = "VYAPAR",
            style =
              TextStyle(
                color = palette.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
                shadow =
                  Shadow(
                    color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.7f else 0.22f),
                    offset = Offset(0f, 5f),
                    blurRadius = 10f,
                  ),
              ),
          )
          Text(
            text = "BILLING PRO",
            style =
              TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFAE52), SaffronOrange, SaffronOrangeDark)),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
                shadow =
                  Shadow(
                    color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.6f else 0.18f),
                    offset = Offset(0f, 4f),
                    blurRadius = 8f,
                  ),
              ),
          )
          Spacer(Modifier.height(10.dp))
          Box(
            modifier =
              Modifier.raised3D(cornerRadius = 50.dp, shadowDepth = 3.dp)
                .padding(horizontal = 16.dp, vertical = 7.dp)
          ) {
            Text(
              text = "Smart Billing Made Easy",
              color = palette.textSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      // Bottom 3 bouncing 3D orange clay dots
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (i in 0..2) {
            val active = kotlin.math.abs(dotPhase - i) < 0.75f || (i == 0 && dotPhase > 2.4f)
            val liftY = if (active) -6f else 0f
            Box(
              modifier =
                Modifier.size(if (active) 13.dp else 10.dp)
                  .graphicsLayer { translationY = liftY }
                  .raised3D(
                    cornerRadius = 8.dp,
                    tone = if (active) ClayTones.HeroOrange else null,
                    shadowDepth = 2.dp,
                  )
            )
          }
        }
        Spacer(Modifier.height(12.dp))
        Text(
          text = "Loading...",
          color = palette.textSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
private fun ClayBrandLogoMark(
  glossProgress: Float,
  modifier: Modifier = Modifier,
) {
  val palette = LocalVyaparClayPalette.current
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val corner = w * 0.27f
    val cr = CornerRadius(corner, corner)

    // 1. Deep bottom-right 3D drop shadow
    drawRoundRect(
      color = palette.shadowDark.copy(alpha = if (palette.isDark) 0.58f else 0.28f),
      topLeft = Offset(w * 0.04f, h * 0.08f),
      size = Size(w * 0.96f, h * 0.96f),
      cornerRadius = cr,
    )
    // 2. Top-left soft rim highlight glow
    drawRoundRect(
      color = Color(0xFFFFB86C).copy(alpha = if (palette.isDark) 0.25f else 0.55f),
      topLeft = Offset(-w * 0.025f, -h * 0.025f),
      size = Size(w * 0.96f, h * 0.96f),
      cornerRadius = cr,
    )
    // 3. Extruded dark-orange lower lip
    drawRoundRect(
      color = Color(0xFFB84106),
      topLeft = Offset(0f, h * 0.03f),
      size = Size(w * 0.96f, h * 0.95f),
      cornerRadius = cr,
    )
    // 4. Main glossy orange clay rounded-square
    drawRoundRect(
      brush =
        Brush.linearGradient(
          colors = listOf(Color(0xFFFFB259), Color(0xFFF46B17), Color(0xFFE04F07)),
          start = Offset.Zero,
          end = Offset(w, h),
        ),
      topLeft = Offset.Zero,
      size = Size(w * 0.96f, h * 0.95f),
      cornerRadius = cr,
    )

    // 5. One-time diagonal gloss sheen sweep
    if (glossProgress in -0.45f..1.45f) {
      val bodyPath =
        Path().apply {
          addRoundRect(
            androidx.compose.ui.geometry.RoundRect(0f, 0f, w * 0.96f, h * 0.95f, corner, corner)
          )
        }
      clipPath(bodyPath) {
        val sx = glossProgress * w
        drawLine(
          brush =
            Brush.linearGradient(
              listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent),
              start = Offset(sx - w * 0.24f, 0f),
              end = Offset(sx + w * 0.24f, h),
            ),
          start = Offset(sx - w * 0.35f, -h * 0.1f),
          end = Offset(sx + w * 0.35f, h * 1.1f),
          strokeWidth = w * 0.26f,
        )
      }
    }

    // 6. Specular 1.5dp bevel border
    val sw = 2.dp.toPx()
    drawRoundRect(
      brush =
        Brush.linearGradient(
          listOf(Color.White.copy(alpha = 0.72f), Color.Transparent, Color.Black.copy(alpha = 0.24f))
        ),
      topLeft = Offset(sw / 2f, sw / 2f),
      size = Size(w * 0.96f - sw, h * 0.95f - sw),
      cornerRadius = cr,
      style = Stroke(width = sw),
    )

    // 7. Crisp 3D White Receipt Card inside badge
    val rx = w * 0.22f
    val ry = h * 0.20f
    val rw = w * 0.43f
    val rh = h * 0.54f
    val recPath =
      Path().apply {
        moveTo(rx + rw * 0.12f, ry)
        lineTo(rx + rw * 0.88f, ry)
        quadraticTo(rx + rw, ry, rx + rw, ry + rw * 0.12f)
        lineTo(rx + rw, ry + rh)
        lineTo(rx + rw * 0.83f, ry + rh * 0.93f)
        lineTo(rx + rw * 0.66f, ry + rh)
        lineTo(rx + rw * 0.50f, ry + rh * 0.93f)
        lineTo(rx + rw * 0.33f, ry + rh)
        lineTo(rx + rw * 0.16f, ry + rh * 0.93f)
        lineTo(rx, ry + rh)
        lineTo(rx, ry + rw * 0.12f)
        quadraticTo(rx, ry, rx + rw * 0.12f, ry)
        close()
      }
    // Receipt shadow
    drawRoundRect(
      color = Color.Black.copy(alpha = 0.24f),
      topLeft = Offset(rx + w * 0.02f, ry + h * 0.03f),
      size = Size(rw, rh * 0.96f),
      cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
    )
    drawPath(
      path = recPath,
      brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF6EFE7))),
    )
    // Receipt decorative lines
    drawRoundRect(
      color = Color(0xFFD5DAE3),
      topLeft = Offset(rx + rw * 0.16f, ry + rh * 0.22f),
      size = Size(rw * 0.55f, h * 0.042f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = Color(0xFFE3E7EE),
      topLeft = Offset(rx + rw * 0.16f, ry + rh * 0.36f),
      size = Size(rw * 0.40f, h * 0.040f),
      cornerRadius = CornerRadius(6f, 6f),
    )

    // 8. Rising 3D Growth Arrow
    val arrowPath =
      Path().apply {
        moveTo(w * 0.28f, h * 0.61f)
        lineTo(w * 0.41f, h * 0.49f)
        lineTo(w * 0.50f, h * 0.55f)
        lineTo(w * 0.74f, h * 0.33f)
      }
    val arrowShadow =
      Path().apply {
        moveTo(w * 0.28f, h * 0.64f)
        lineTo(w * 0.41f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.58f)
        lineTo(w * 0.74f, h * 0.36f)
      }
    drawPath(
      path = arrowShadow,
      color = Color.Black.copy(alpha = 0.25f),
      style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
      path = arrowPath,
      brush = Brush.linearGradient(listOf(Color(0xFFE8590C), Color(0xFF22C55E))),
      style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val head =
      Path().apply {
        moveTo(w * 0.63f, h * 0.31f)
        lineTo(w * 0.79f, h * 0.29f)
        lineTo(w * 0.76f, h * 0.45f)
        close()
      }
    drawPath(path = head, color = Color(0xFF22C55E))
  }
}

private data class OnboardingSlide(
  val title: String,
  val subtitle: String,
  val badge: String,
  val icon: ImageVector,
  val tone: ClayTone,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
  val palette = LocalVyaparClayPalette.current
  val slides = remember {
    listOf(
      OnboardingSlide(
        title = "Smart Billing\nfor your Business",
        subtitle = "Manage Sales, Products and Customers Easily — 100% Free & Offline.",
        badge = "Zero Investment • Works Offline Anytime",
        icon = Icons.Filled.ReceiptLong,
        tone = ClayTones.Orange,
      ),
      OnboardingSlide(
        title = "GST Invoices &\nGenuine PDF",
        subtitle = "Instant CGST, SGST, IGST calculations with A4 & 80mm thermal PDF receipts.",
        badge = "Shop Logo • Hindi & English PDF",
        icon = Icons.Filled.PictureAsPdf,
        tone = ClayTones.Blue,
      ),
      OnboardingSlide(
        title = "One-Tap\nWhatsApp PDF Share",
        subtitle = "Send actual PDF tax invoices directly to customers on WhatsApp anytime.",
        badge = "Real PDF Attachment • Instant Share",
        icon = Icons.Filled.Share,
        tone = ClayTones.Green,
      ),
      OnboardingSlide(
        title = "Private & Safe\nOffline Backups",
        subtitle = "Your business data stays safely on your phone with one-tap backup & restore.",
        badge = "100% Private • Instant Backup & Restore",
        icon = Icons.Filled.Security,
        tone = ClayTones.Purple,
      ),
    )
  }

  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      Box(
        modifier =
          Modifier.raised3D(cornerRadius = 50.dp, shadowDepth = 3.dp)
            .clickable(onClick = onFinish)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("onboarding_skip")
      ) {
        Text("Skip", color = palette.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
      val slide = slides[page]
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        if (page == 0) {
          Card3D(
            cornerRadius = 30.dp,
            shadowDepth = 7.dp,
            contentPadding = PaddingValues(10.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_onboarding_shop),
              contentDescription = "Smart Billing Shopkeeper",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(214.dp).clip(RoundedCornerShape(22.dp)),
            )
          }
        } else {
          ClayIconPad(
            imageVector = slide.icon,
            contentDescription = slide.title,
            tone = slide.tone,
            padSize = 152.dp,
            iconSize = 76.dp,
            cornerRadius = 38.dp,
          )
        }

        Spacer(Modifier.height(24.dp))

        Row(
          modifier =
            Modifier.raised3D(cornerRadius = 50.dp, tone = ClayTones.Green, shadowDepth = 3.dp)
              .padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = slide.badge,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
          )
        }

        Spacer(Modifier.height(20.dp))

        Text(
          text = slide.title,
          color = palette.textPrimary,
          fontSize = 26.sp,
          lineHeight = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
          text = slide.subtitle,
          color = palette.textSecondary,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        )
      }
    }

    // 3D Pager dots
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(slides.size) { index ->
        val selected = pagerState.currentPage == index
        Box(
          modifier =
            Modifier.height(9.dp)
              .width(if (selected) 26.dp else 9.dp)
              .then(
                if (selected) Modifier.raised3D(cornerRadius = 50.dp, tone = ClayTones.Orange, shadowDepth = 2.dp)
                else Modifier.inset3D(cornerRadius = 50.dp)
              )
        )
      }
    }

    Spacer(Modifier.height(22.dp))

    val isLast = pagerState.currentPage == slides.lastIndex
    Button3D(
      onClick = {
        if (isLast) onFinish()
        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
      },
      cornerRadius = 20.dp,
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .height(54.dp)
          .testTag("onboarding_next_button"),
    ) {
      Text(
        text = if (isLast) "Get Started" else "Next",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
      )
    }

    Spacer(Modifier.height(8.dp))
  }
}

@Composable
fun BusinessSetupScreen(
  existingProfile: BusinessProfile,
  canNavigateBack: Boolean,
  onBack: () -> Unit,
  onSave: (BusinessProfile, Uri?) -> Unit,
) {
  if (canNavigateBack) {
    BackHandler(onBack = onBack)
  }

  val palette = LocalVyaparClayPalette.current
  val iconTone = remember(palette.isDark) {
    ClayTone(palette.neutralIconTop, palette.textPrimary, palette.neutralIconBottom)
  }

  var businessName by remember(existingProfile) { mutableStateOf(existingProfile.businessName) }
  var ownerName by remember(existingProfile) { mutableStateOf(existingProfile.ownerName) }
  var mobile by remember(existingProfile) { mutableStateOf(existingProfile.mobile) }
  var whatsapp by remember(existingProfile) { mutableStateOf(existingProfile.whatsapp) }
  var email by remember(existingProfile) { mutableStateOf(existingProfile.email) }
  var address by remember(existingProfile) { mutableStateOf(existingProfile.address) }
  var city by remember(existingProfile) { mutableStateOf(existingProfile.city) }
  var state by remember(existingProfile) { mutableStateOf(existingProfile.state) }
  var pinCode by remember(existingProfile) { mutableStateOf(existingProfile.pinCode) }
  var gstin by remember(existingProfile) { mutableStateOf(existingProfile.gstin) }
  var pan by remember(existingProfile) { mutableStateOf(existingProfile.pan) }
  var category by remember(existingProfile) { mutableStateOf(existingProfile.category) }
  var currencySymbol by remember(existingProfile) { mutableStateOf(existingProfile.currencySymbol) }
  var invoicePrefix by remember(existingProfile) { mutableStateOf(existingProfile.invoicePrefix) }
  var tagline by remember(existingProfile) { mutableStateOf(existingProfile.tagline) }
  var terms by remember(existingProfile) { mutableStateOf(existingProfile.termsAndConditions) }
  var thankYou by remember(existingProfile) { mutableStateOf(existingProfile.thankYouMessage) }
  var pickedLogoUri by remember { mutableStateOf<Uri?>(null) }
  var nameError by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pickedLogoUri = uri
    }

  val categories = remember {
    listOf(
      "Restaurant / Cafe",
      "Grocery / Kirana",
      "Retail Store",
      "Medical / Pharmacy",
      "Mobile / Electronics",
      "Hotel / Bakery",
      "Hardware / General",
    )
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
              .height(56.dp)
              .padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (canNavigateBack) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(40.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable(onClick = onBack)
                  .testTag("business_back"),
            ) {
              Icon3D(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tone = iconTone,
                size = 24.dp,
              )
            }
          } else {
            Spacer(Modifier.width(8.dp))
          }
          Spacer(Modifier.width(6.dp))
          Text(
            text = "Business Setup",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
          )
          if (!canNavigateBack) {
            TextButton(
              onClick = {
                val fallbackName = businessName.trim().ifBlank { "My Business" }
                onSave(
                  existingProfile.copy(
                    businessName = fallbackName,
                    ownerName = ownerName.trim(),
                    mobile = mobile.trim(),
                  ),
                  pickedLogoUri,
                )
              }
            ) {
              Text("Skip", color = SaffronOrange, fontWeight = FontWeight.ExtraBold)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Button3D(
          onClick = {
            if (businessName.trim().isEmpty()) {
              nameError = true
              return@Button3D
            }
            onSave(
              existingProfile.copy(
                businessName = businessName.trim(),
                ownerName = ownerName.trim(),
                mobile = mobile.trim(),
                whatsapp = whatsapp.trim().ifBlank { mobile.trim() },
                email = email.trim(),
                address = address.trim(),
                city = city.trim(),
                state = state.trim(),
                pinCode = pinCode.trim(),
                gstin = gstin.trim().uppercase(),
                pan = pan.trim().uppercase(),
                category = category,
                currencySymbol = currencySymbol.trim().ifBlank { "₹" },
                invoicePrefix = invoicePrefix.trim().uppercase().ifBlank { "VBP" },
                tagline = tagline.trim(),
                termsAndConditions = terms.trim(),
                thankYouMessage = thankYou.trim(),
              ),
              pickedLogoUri,
            )
          },
          cornerRadius = 20.dp,
          modifier =
            Modifier.widthIn(max = 560.dp)
              .fillMaxWidth()
              .height(52.dp)
              .testTag("save_business_button"),
        ) {
          Text(
            "Save Business",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background)
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        // 3D Circular Shop Logo Socket
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(112.dp)
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
                .testTag("pick_shop_logo")
          ) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .raised3D(
                    cornerRadius = 56.dp,
                    accentBorderColor = SaffronOrange,
                    shadowDepth = 6.dp,
                  )
                  .padding(5.dp)
                  .clip(CircleShape),
            ) {
              when {
                pickedLogoUri != null ->
                  AsyncImage(
                    model = pickedLogoUri,
                    contentDescription = "Selected Shop Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
                !existingProfile.logoPath.isNullOrBlank() && File(existingProfile.logoPath).exists() ->
                  AsyncImage(
                    model = File(existingProfile.logoPath),
                    contentDescription = "Saved Shop Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
                else ->
                  Image(
                    painter = painterResource(id = R.drawable.img_vyapar_logo),
                    contentDescription = "Default Shop Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
              }
            }
            Box(
              modifier =
                Modifier.align(Alignment.BottomEnd)
                  .size(34.dp)
                  .raised3D(cornerRadius = 17.dp, tone = ClayTones.Orange, shadowDepth = 3.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Pick Shop Logo from Gallery",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        Text(
          text = "Tap logo to pick from gallery (printed on PDF invoices)",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        )

        Card3D(
          cornerRadius = 26.dp,
          shadowDepth = 5.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            TextField3D(
              value = businessName,
              onValueChange = {
                businessName = it
                nameError = false
              },
              label = "Business Name *",
              placeholder = "e.g. Royal Restaurant",
              isError = nameError,
              supportingText =
                if (nameError) {
                  { Text("Business name is required") }
                } else null,
              modifier = Modifier.fillMaxWidth().testTag("input_business_name"),
            )

            TextField3D(
              value = ownerName,
              onValueChange = { ownerName = it },
              label = "Owner Name",
              placeholder = "e.g. Rohit Sharma",
              modifier = Modifier.fillMaxWidth(),
            )

            Text(
              text = "Business Category",
              style = MaterialTheme.typography.labelMedium,
              color = palette.textSecondary,
              fontWeight = FontWeight.SemiBold,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              categories.forEach { cat ->
                Chip3D(
                  selected = category == cat,
                  onClick = { category = cat },
                  label = cat,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              TextField3D(
                value = mobile,
                onValueChange = { mobile = it },
                label = "Mobile Number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = "WhatsApp Number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
              )
            }

            TextField3D(
              value = email,
              onValueChange = { email = it },
              label = "Email Address",
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              modifier = Modifier.fillMaxWidth(),
            )

            TextField3D(
              value = address,
              onValueChange = { address = it },
              label = "Shop Address",
              placeholder = "123 Main Road",
              modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              TextField3D(
                value = city,
                onValueChange = { city = it },
                label = "City",
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = state,
                onValueChange = { state = it },
                label = "State",
                modifier = Modifier.weight(1f),
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              TextField3D(
                value = pinCode,
                onValueChange = { pinCode = it.take(6) },
                label = "PIN Code",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = currencySymbol,
                onValueChange = { currencySymbol = it.take(3) },
                label = "Currency",
                modifier = Modifier.weight(0.7f),
              )
              TextField3D(
                value = invoicePrefix,
                onValueChange = { invoicePrefix = it.take(6) },
                label = "Prefix",
                modifier = Modifier.weight(0.8f),
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              TextField3D(
                value = gstin,
                onValueChange = { gstin = it.take(15) },
                label = "GSTIN (Optional)",
                modifier = Modifier.weight(1f),
              )
              TextField3D(
                value = pan,
                onValueChange = { pan = it.take(10) },
                label = "PAN (Optional)",
                modifier = Modifier.weight(1f),
              )
            }

            TextField3D(
              value = tagline,
              onValueChange = { tagline = it },
              label = "Invoice Tagline",
              modifier = Modifier.fillMaxWidth(),
            )

            TextField3D(
              value = terms,
              onValueChange = { terms = it },
              label = "Invoice Terms & Conditions",
              singleLine = false,
              minLines = 2,
              modifier = Modifier.fillMaxWidth(),
            )

            TextField3D(
              value = thankYou,
              onValueChange = { thankYou = it },
              label = "Thank You Message",
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun PinLockScreen(onVerifyPin: (String) -> Boolean) {
  val palette = LocalVyaparClayPalette.current
  var pinInput by remember { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card3D(
      cornerRadius = 28.dp,
      shadowDepth = 7.dp,
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
      contentPadding = PaddingValues(24.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ClayIconPad(
          imageVector = Icons.Filled.Lock,
          contentDescription = "App Locked",
          tone = ClayTones.Orange,
          padSize = 68.dp,
          iconSize = 34.dp,
          cornerRadius = 22.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
          "Vyapar Billing Pro Locked",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = palette.textPrimary,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          "Enter your security PIN to continue",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        TextField3D(
          value = pinInput,
          onValueChange = {
            pinInput = it.filter { ch -> ch.isDigit() }.take(6)
            error = false
          },
          label = "PIN",
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = error,
          supportingText = if (error) ({ Text("Incorrect PIN") }) else null,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Button3D(
          onClick = {
            if (!onVerifyPin(pinInput)) {
              error = true
            }
          },
          cornerRadius = 18.dp,
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          Text("Unlock", color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}
