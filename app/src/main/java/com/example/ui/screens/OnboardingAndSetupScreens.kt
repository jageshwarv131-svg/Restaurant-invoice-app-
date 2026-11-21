package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BusinessProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.CharcoalHeader
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SaffronOrange
import com.example.ui.theme.SaffronOrangeDark
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(1600)
    onSplashFinished()
  }

  Box(
    modifier = Modifier.fillMaxSize().background(CharcoalBlack),
    contentAlignment = Alignment.Center,
  ) {
    // Subtle saffron ambient swooshes matching brand mockup
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(SaffronOrange.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.36f),
            radius = size.minDimension * 0.55f,
          ),
        center = Offset(size.width * 0.5f, size.height * 0.36f),
        radius = size.minDimension * 0.55f,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(SaffronOrangeDark.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.88f),
            radius = size.minDimension * 0.5f,
          ),
        center = Offset(size.width * 0.85f, size.height * 0.88f),
        radius = size.minDimension * 0.5f,
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(Modifier.height(24.dp))

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = Color(0xFF151517),
          border = BorderStroke(1.5.dp, SaffronOrange.copy(alpha = 0.55f)),
          shadowElevation = 16.dp,
          modifier = Modifier.size(136.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_vyapar_logo),
            contentDescription = "Vyapar Billing Pro Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }

        Spacer(Modifier.height(28.dp))

        Text(
          text = "VYAPAR",
          color = Color.White,
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp,
        )
        Text(
          text = "BILLING PRO",
          color = SaffronOrange,
          fontSize = 26.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
          text = "Bill Smart. Grow Smart.",
          color = Color(0xFFD1D5DB),
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
          color = SaffronOrange,
          trackColor = Color(0xFF2D313B),
          strokeWidth = 3.5.dp,
          modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = "Loading...",
          color = Color(0xFF9CA3AF),
          fontSize = 13.sp,
        )
      }
    }
  }
}

private data class OnboardingSlide(
  val title: String,
  val subtitle: String,
  val badge: String,
  val icon: ImageVector,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
  val slides = remember {
    listOf(
      OnboardingSlide(
        title = "Smart Billing\nfor your Business",
        subtitle = "Manage Sales, Products and Customers Easily — 100% Free & Offline.",
        badge = "Zero Investment • Works in Airplane Mode",
        icon = Icons.Filled.ReceiptLong,
      ),
      OnboardingSlide(
        title = "GST Invoices &\nGenuine PDF",
        subtitle = "Instant CGST, SGST, IGST calculations with A4 & 80mm thermal PDF receipts.",
        badge = "Shop Logo • Hindi & English PDF",
        icon = Icons.Filled.PictureAsPdf,
      ),
      OnboardingSlide(
        title = "One-Tap\nWhatsApp PDF Share",
        subtitle = "Send actual PDF tax invoices directly to customers on WhatsApp anytime.",
        badge = "Real PDF Attachment • Zero Cloud Cost",
        icon = Icons.Filled.Share,
      ),
      OnboardingSlide(
        title = "Private & Safe\nOffline Backups",
        subtitle = "Your business data stays safely on your phone with one-tap backup & restore.",
        badge = "100% Private • Instant Backup & Restore",
        icon = Icons.Filled.Security,
      ),
    )
  }

  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(CharcoalBlack)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = onFinish, modifier = Modifier.testTag("onboarding_skip")) {
        Text("Skip", color = Color(0xFF9CA3AF), fontWeight = FontWeight.SemiBold)
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
          Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF151517),
            modifier = Modifier.size(230.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_onboarding_shop),
              contentDescription = "Smart Billing Shopkeeper",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        } else {
          Surface(
            shape = CircleShape,
            color = SaffronOrange.copy(alpha = 0.14f),
            border = BorderStroke(2.dp, SaffronOrange.copy(alpha = 0.45f)),
            modifier = Modifier.size(160.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = slide.icon,
                contentDescription = slide.title,
                tint = SaffronOrange,
                modifier = Modifier.size(76.dp),
              )
            }
          }
        }

        Spacer(Modifier.height(24.dp))

        Surface(
          shape = RoundedCornerShape(50),
          color = EmeraldGreen.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF4ADE80),
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = slide.badge,
              color = Color(0xFFDCFCE7),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }

        Spacer(Modifier.height(20.dp))

        Text(
          text = slide.title,
          color = Color.White,
          fontSize = 26.sp,
          lineHeight = 32.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
          text = slide.subtitle,
          color = Color(0xFF9CA3AF),
          fontSize = 14.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        )
      }
    }

    // Pager dots
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(slides.size) { index ->
        val selected = pagerState.currentPage == index
        Box(
          modifier =
            Modifier.height(8.dp)
              .width(if (selected) 24.dp else 8.dp)
              .clip(CircleShape)
              .background(if (selected) SaffronOrange else Color(0xFF3F4451))
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    val isLast = pagerState.currentPage == slides.lastIndex
    Button(
      onClick = {
        if (isLast) onFinish()
        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
      },
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .height(52.dp)
          .testTag("onboarding_next_button"),
    ) {
      Text(
        text = if (isLast) "Get Started" else "Next",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
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
    topBar = {
      Surface(color = CharcoalHeader, contentColor = Color.White) {
        Row(
          modifier =
            Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (canNavigateBack) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("business_back")) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
              )
            }
          } else {
            Spacer(Modifier.width(12.dp))
          }
          Text(
            text = "Business Setup",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
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
              Text("Skip", color = SaffronOrange, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding(),
      ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
          Button(
            onClick = {
              if (businessName.trim().isEmpty()) {
                nameError = true
                return@Button
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
            modifier =
              Modifier.widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_business_button"),
          ) {
            Text(
              "Save Business",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            )
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
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        // Circular Shop Logo Picker
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(104.dp)
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
                .testTag("pick_shop_logo")
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(2.5.dp, SaffronOrange),
              shadowElevation = 4.dp,
              modifier = Modifier.fillMaxSize(),
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
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(CharcoalBlack)
                  .border(2.dp, Color.White, CircleShape),
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
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        )

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedTextField(
              value = businessName,
              onValueChange = {
                businessName = it
                nameError = false
              },
              label = { Text("Business Name *") },
              placeholder = { Text("e.g. Royal Restaurant") },
              isError = nameError,
              supportingText =
                if (nameError) {
                  { Text("Business name is required") }
                } else null,
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_business_name"),
            )

            OutlinedTextField(
              value = ownerName,
              onValueChange = { ownerName = it },
              label = { Text("Owner Name") },
              placeholder = { Text("e.g. Rohit Sharma") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            Text(
              text = "Business Category",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              categories.forEach { cat ->
                FilterChip(
                  selected = category == cat,
                  onClick = { category = cat },
                  label = { Text(cat, fontSize = 12.sp) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = SaffronOrange,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }

            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email Address") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
              value = address,
              onValueChange = { address = it },
              label = { Text("Shop Address") },
              placeholder = { Text("123 Main Road") },
              modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("State") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = pinCode,
                onValueChange = { pinCode = it.take(6) },
                label = { Text("PIN Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = currencySymbol,
                onValueChange = { currencySymbol = it.take(3) },
                label = { Text("Currency") },
                singleLine = true,
                modifier = Modifier.weight(0.7f),
              )
              OutlinedTextField(
                value = invoicePrefix,
                onValueChange = { invoicePrefix = it.take(6) },
                label = { Text("Prefix") },
                singleLine = true,
                modifier = Modifier.weight(0.8f),
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = gstin,
                onValueChange = { gstin = it.take(15) },
                label = { Text("GSTIN (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = pan,
                onValueChange = { pan = it.take(10) },
                label = { Text("PAN (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }

            OutlinedTextField(
              value = tagline,
              onValueChange = { tagline = it },
              label = { Text("Invoice Tagline") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
              value = terms,
              onValueChange = { terms = it },
              label = { Text("Invoice Terms & Conditions") },
              minLines = 2,
              modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
              value = thankYou,
              onValueChange = { thankYou = it },
              label = { Text("Thank You Message") },
              singleLine = true,
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
  var pinInput by remember { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier.fillMaxSize().background(CharcoalBlack).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = "App Locked",
          tint = SaffronOrange,
          modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Vyapar Billing Pro Locked", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
          "Enter your 4-digit security PIN",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
          value = pinInput,
          onValueChange = {
            pinInput = it.filter { ch -> ch.isDigit() }.take(6)
            error = false
          },
          label = { Text("PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = error,
          supportingText = if (error) ({ Text("Incorrect PIN", color = AlertRed) }) else null,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
          onClick = {
            if (!onVerifyPin(pinInput)) {
              error = true
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
