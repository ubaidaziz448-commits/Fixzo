package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.theme.FixzoTheme
import com.example.ui.viewmodel.FixzoViewModel
import com.example.ui.viewmodel.Language
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

val PAKISTAN_CITIES = listOf(
    "Islamabad", "Lahore", "Karachi", "Rawalpindi", "Peshawar", "Faisalabad",
    "Multan", "Gujranwala", "Sialkot", "Quetta", "Hyderabad", "Sargodha",
    "Bahawalpur", "Sukkur", "Jhang", "Sheikhupura", "Larkana", "Gujrat",
    "Mardan", "Rahim Yar Khan", "Kasur", "Sahiwal", "Okara", "Abbottabad",
    "Swat", "Gwadar", "Mirpur", "Muzaffarabad"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FixzoTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val viewModel: FixzoViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val language by viewModel.language.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
    val showSmsNotification by viewModel.showSmsNotification.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // For Direct Call Simulated Popup
    var showDirectCallDialog by remember { mutableStateOf<String?>(null) }
    
    // For Rating Prompter
    var showRatingPrompt by remember { mutableStateOf(false) }

    // Observe active booking status to trigger the prompt when complete
    LaunchedEffect(activeBooking) {
        if (activeBooking != null && activeBooking?.status == "Completed" && activeBooking?.ratingSubmitted == false) {
            showRatingPrompt = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fixzo",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("app_brand_title")
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dual Language Button (Urdu / English Slider Toggle)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { viewModel.toggleLanguage() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == Language.EN) "🇵🇰 اردو" else "🇬🇧 EN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (currentScreen != Screen.Welcome) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Screen switching with Animated Fade / Slide transition
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Welcome -> WelcomeScreen(viewModel)
                    is Screen.VerificationSim -> VerificationSimScreen(viewModel)
                    is Screen.CustomerDashboard -> CustomerDashboardScreen(viewModel, onCallWorker = { showDirectCallDialog = it })
                    is Screen.WorkerDashboard -> WorkerDashboardScreen(viewModel)
                    is Screen.ChatScreen -> ChatScreenView(viewModel, screen.bookingId)
                    is Screen.CostCalculatorScreen -> CostCalculatorScreen(viewModel)
                    is Screen.EmergencyPortalScreen -> EmergencyPortalScreen(viewModel)
                }
            }

            // Simulated SMS Push Notification Banner sliding down
            AnimatedVisibility(
                visible = showSmsNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Autofill the OTP instantly upon clicking the notification!
                            viewModel.smsVerificationCode.value = generatedOtp
                            viewModel.showSmsNotification.value = false
                            Toast.makeText(context, "Verification code auto-copied & filled!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SMS • Fixzo OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Now", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your 4-digit verification code is: $generatedOtp. Tap here to instantly auto-fill & verify.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }
                }
            }

            // Global Direct Call Simulated Intent Popup
            showDirectCallDialog?.let { workerPhone ->
                AlertDialog(
                    onDismissRequest = { showDirectCallDialog = null },
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Calling", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
                    title = { Text(viewModel.getString("Simulated Dialer Intent", "فون ڈائلر کا نمونہ"), fontWeight = FontWeight.Bold) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(viewModel.getString("Initiating direct call to worker's registered SIM number in Pakistan:", "پاکستان میں کاریگر کے رجسٹرڈ سم نمبر پر براہ راست کال شروع کریں:"))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = workerPhone,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDirectCallDialog = null
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$workerPhone"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Call dialed to $workerPhone", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("dialer_confirm_button")
                        ) {
                            Text(viewModel.getString("Open Phone Dial", "فون ڈائل کھولیں"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDirectCallDialog = null }) {
                            Text(viewModel.getString("Cancel", "منسوخ کریں"))
                        }
                    }
                )
            }

            // Star Rating Dialog prompt that overlays the active completion
            if (showRatingPrompt && activeBooking != null) {
                var ratingValue by remember { mutableStateOf(5f) }
                AlertDialog(
                    onDismissRequest = { /* Force rating completion for trust system demonstration */ },
                    title = { 
                        Text(
                            text = viewModel.getString("Rate Outstanding Service", "خدمت کی ریٹنگ کریں"),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = viewModel.getString(
                                    "Your job with ${activeBooking?.workerName} is completed. Please rate their honesty & skill!",
                                    "آپ کا کام ${activeBooking?.workerName} کے ساتھ مکمل ہو گیا ہے۔ برائے مہربانی ریٹنگ دیں"
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual Star Selector Layout
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star $star",
                                        tint = if (star <= ratingValue) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clickable { ratingValue = star.toFloat() }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "$ratingValue / 5.0 Stars",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_rating_button"),
                            onClick = {
                                viewModel.submitWorkerRating(ratingValue)
                                showRatingPrompt = false
                                Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(viewModel.getString("Submit Reviews & Close", "ریٹنگ جمع کریں اور بند کریں"))
                        }
                    }
                )
            }
        }
    }
}

// 1. Language Choice & Welcome / Screen
@Composable
fun WelcomeScreen(viewModel: FixzoViewModel) {
    val language by viewModel.language.collectAsState()
    val nameInput by viewModel.regNameInput.collectAsState()
    val phoneInput by viewModel.regPhoneInput.collectAsState()
    val cityInput by viewModel.regCityInput.collectAsState()
    val categoryInput by viewModel.regCategoryInput.collectAsState()
    val context = LocalContext.current

    val cities = PAKISTAN_CITIES
    val categories = listOf("Plumber", "Electrician", "Carpenter", "AC Technician", "Painter", "Welder")

    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "Fixzo Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.getString("Fixzo — Trusted Workers", "فکسو — قابل اعتماد کاریگر"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = viewModel.getString("One Tap Away", "بس ایک کلک کے فاصلے پر"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = viewModel.getString("User & Worker Registration", "صارف اور کاریگر کی رجسٹریشن"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.regNameInput.value = it },
                        label = { Text(viewModel.getString("Full Name", "پورا نام")) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { viewModel.regPhoneInput.value = it },
                        label = { Text(viewModel.getString("Phone Number (e.g., 03001234567)", "فون نمبر")) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                        singleLine = true
                    )

                    // City choice drop down
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(viewModel.getString("Work/Service City", "سروس کا شہر")) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            trailingIcon = { IconButton(onClick = { cityDropdownExpanded = !cityDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }},
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = cityDropdownExpanded,
                            onDismissRequest = { cityDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        viewModel.regCityInput.value = city
                                        cityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Worker trade selector - only setup if registering as specialist
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getString("If you are a job worker, pick trade category below:", "اگر آپ کاریگر ہیں تو نیچے اپنی قسم منتخب کریں:"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(viewModel.getString("Trade Category", "کام کی قسم")) },
                            leadingIcon = { Icon(Icons.Default.DesignServices, contentDescription = null) },
                            trailingIcon = { IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }},
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.regCategoryInput.value = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggles as Customer or Specialist
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Register as Customer
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && phoneInput.isNotBlank()) {
                            if (viewModel.isValidPakistanPhone(phoneInput)) {
                                viewModel.startRegistration(isWorker = false)
                            } else {
                                Toast.makeText(
                                    context,
                                    viewModel.getString(
                                        "Please enter a valid Pakistan mobile phone number (starts with 03xx or +923xx/923xx)",
                                        "برائے مہربانی درست پاکستانی موبائل نمبر درج کریں (مثال کے طور پر 03001234567)"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, viewModel.getString("Ensure all fields are entered", "تمام معلومات درج کریں"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("submit_registration_customer_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text(
                            text = viewModel.getString("I need service", "مجھے کام کروانا ہے"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Register as Worker
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && phoneInput.isNotBlank()) {
                            if (viewModel.isValidPakistanPhone(phoneInput)) {
                                viewModel.startRegistration(isWorker = true)
                            } else {
                                Toast.makeText(
                                    context,
                                    viewModel.getString(
                                        "Please enter a valid Pakistan mobile phone number (starts with 03xx or +923xx/923xx)",
                                        "برائے مہربانی درست پاکستانی موبائل نمبر درج کریں (مثال کے طور پر 03001234567)"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, viewModel.getString("Ensure all fields are entered", "تمام معلومات درج کریں"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("submit_registration_worker_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Engineering, contentDescription = null)
                        Text(
                            text = viewModel.getString("I am a Worker", "میں کاریگر ہوں"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 2. VerificationSim Screen (OTP Countdown simulator)
@Composable
fun VerificationSimScreen(viewModel: FixzoViewModel) {
    val phoneInput by viewModel.regPhoneInput.collectAsState()
    val isWorker by viewModel.isWorkerMode.collectAsState()
    val code by viewModel.smsVerificationCode.collectAsState()
    val ticks by viewModel.smsSentTicks.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = "SMS Authentication",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = viewModel.getString("Secure Device Verification", "ڈیوائس کی تصدیق"),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.getString(
                    "We have transmitted a secure 4-digit verification code to $phoneInput",
                    "ہم نے آپ کے نمبر $phoneInput پر ۴ ہندسوں کا کوڈ بھیجا ہے"
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Device SMS Carrier visual simulated card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.getString("SIMULATED SMS GATEWAY (PAKISTAN)", "موبائل ایس ایم ایس تصدیقی الرٹ"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.getString(
                            "Delivery Code Received: $generatedOtp",
                            "موصول شدہ تصدیقی کوڈ ہے: $generatedOtp"
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.smsVerificationCode.value = generatedOtp
                            Toast.makeText(context, "Code loaded!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = viewModel.getString("Instant Autofill", "خودکار درج کریں"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 4) viewModel.smsVerificationCode.value = it },
                label = { Text(viewModel.getString("4-Digit Security Code", "تصدیقی کوڈ")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .testTag("otp_input"),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, letterSpacing = 8.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (ticks > 0) {
                Text(
                    text = viewModel.getString("Resend code in $ticks seconds", "نیا کوڈ $ticks سیکنڈ کے بعد"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                TextButton(
                    onClick = {
                        viewModel.resendSmsCode()
                        Toast.makeText(context, "New SMS Code Sent!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = viewModel.getString("Didn't get code? Resend SMS", "کوڈ نہیں ملا؟ دوبارہ بھیجیں"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val verified = viewModel.verifySMSCode(code)
                    if (!verified) {
                        Toast.makeText(context, "Incorrect code! Active code is $generatedOtp", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_registration_button")
            ) {
                Text(viewModel.getString("Verify & Complete", "تصدیق کریں"))
            }
        }
    }
}

// 3. Customer Dashboard Screen
@Composable
fun CustomerDashboardScreen(viewModel: FixzoViewModel, onCallWorker: (String) -> Unit) {
    val workers by viewModel.filteredWorkers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedCityFilter by viewModel.selectedCityFilter.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()

    var selectedWorkerDetail by remember { mutableStateOf<WorkerEntity?>(null) }
    var cityDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("All", "Plumber", "Electrician", "Carpenter", "AC Technician", "Painter", "Welder")
    val cities = PAKISTAN_CITIES

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Search & City selectors headers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Text box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text(viewModel.getString("Search for services (e.g. Plumber)...", "کام تلاش کریں (جیسے پلمبر)..."), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                            .height(48.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    // City Dropdown button
                    Box {
                        Button(
                            onClick = { cityDropdownExpanded = !cityDropdownExpanded },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedCityFilter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = cityDropdownExpanded,
                            onDismissRequest = { cityDropdownExpanded = false }
                        ) {
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        viewModel.selectedCityFilter.value = city
                                        cityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category scrollable selection row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        AssistChip(
                            onClick = { viewModel.selectedCategoryFilter.value = cat },
                            label = { Text(cat, fontWeight = FontWeight.SemiBold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // Interactive top navigation tools: Est Cost Calculator, Emergency Center, etc.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calculator Pill
                Button(
                    onClick = { viewModel.navigateTo(Screen.CostCalculatorScreen) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("Price Calc", "قیمت کا اندازہ"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Emergency Center Pill
                Button(
                    onClick = { viewModel.navigateTo(Screen.EmergencyPortalScreen) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("Emergency", "ہنگامی الرٹ"), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            // Active order quick tracker card (shows if booking Accepted or In_Progress)
            activeBooking?.let { active ->
                if (active.status != "Completed" && active.status != "Cancelled") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { viewModel.navigateTo(Screen.ChatScreen(active.id)) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${active.workerName} (${active.workerCategory})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = viewModel.getString("Status: ${active.status}", "حیثیت: ${active.status}"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = viewModel.getString("VIEW CHAT / TRACK MAP ➔", "بات کریں کریں ➔"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Main Listing results
            if (workers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = viewModel.getString("No Workers Found Nearby", "قریب کوئی کاریگر نہیں ملا"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = viewModel.getString("Select another category or location city filter above.", "برائے مہربانی کوئی اور شہر یا کٹیگری اوپر منتخب کریں"),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workers) { worker ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWorkerDetail = worker }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Worker Avatar/Icon
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Engineering, contentDescription = "Specialist", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Details layout
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = worker.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        
                                        // Trust Verified Badge
                                        if (worker.isVerified) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified Badge",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .testTag("verified_badge")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${worker.category} • ${worker.location}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Reviews", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${String.format("%.1f", worker.rating)} (${worker.ratingCount} reviews)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Interactive Quick Buttons
                                Column(horizontalAlignment = Alignment.End) {
                                    Button(
                                        onClick = { onCallWorker(worker.phone) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("direct_call_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(viewModel.getString("Call", "کال"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Worker details popup sheet / card in dialog form
        selectedWorkerDetail?.let { worker ->
            AlertDialog(
                onDismissRequest = { selectedWorkerDetail = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(worker.name, fontWeight = FontWeight.Bold)
                        if (worker.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, contentDescription = "Verified Badge", tint = Color(0xFF10B981))
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DesignServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.getString("Service Range: ${worker.category}", "خدمت کی قسم: ${worker.category}"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.getString("Active City: ${worker.location}", "فعال شہر: ${worker.location}"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.getString("Average Rating: ${String.format("%.1f", worker.rating)} (${worker.ratingCount} reviews)", "شرح ریٹنگ: ${String.format("%.1f", worker.rating)}"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (worker.isVerified) Color(0xFF10B981) else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (worker.isVerified) {
                                    viewModel.getString("CNIC Identity Verified", "قومی شناختی کارڈ تصدیق شدہ")
                                } else {
                                    viewModel.getString("CNIC Identity Pending", "قومی شناختی کارڈ کی تصدیق باقی ہے")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Divider()
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(viewModel.getString("Transacting Options:", "ادائیگی کے طریقہ کار:"), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("EasyPaisa") })
                            AssistChip(onClick = {}, label = { Text("JazzCash") })
                            AssistChip(onClick = {}, label = { Text("Cash") })
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedWorkerDetail = null
                                viewModel.bookWorker(worker)
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_booking_button")
                        ) {
                            Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(viewModel.getString("Book Now (500 PKR)", "بک کریں"))
                        }
                    }
                }
            )
        }
    }
}

// 4. Worker Dashboard Screen
@Composable
fun WorkerDashboardScreen(viewModel: FixzoViewModel) {
    val language by viewModel.language.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()
    val walletBal by viewModel.walletBalance.collectAsState()
    val transactions by viewModel.walletTransactions.collectAsState()
    val cnicInput by viewModel.cnicInput.collectAsState()
    val isVerified by viewModel.isCNICVerified.collectAsState()
    val context = LocalContext.current

    var amountWithdrawInput by remember { mutableStateOf("") }
    var accountChannelInput by remember { mutableStateOf("JazzCash") }
    var channelDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Online Specialized Profile card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = viewModel.registeredName.value.ifEmpty { "M. Asif Khan" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "ID Certified", tint = Color(0xFF10B981))
                                }
                            }
                            Text(
                                text = "${viewModel.registeredCategory.value} • ${viewModel.registeredCity.value}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Online green dot indicator badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Booking live status management
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.getString("Current Active Customer Booking", "موجودہ کسٹمر بکنگ کی تفصیلات"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeBooking == null || activeBooking?.status == "Completed" || activeBooking?.status == "Cancelled") {
                        Text(
                            text = viewModel.getString("No current active service requests. You are in queue to receive orders.", "فی الحال کوئی آرڈر نہیں ہے۔ نیا آرڈر حاصل کرنے کے لیے تیار رہیں۔"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        val active = activeBooking!!
                        Text(
                            text = "Customer: ${active.customerName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(text = "Job Required: ${active.workerCategory}", fontSize = 13.sp)
                        Text(text = "Pay Mode: ${active.paymentMethod}", fontSize = 13.sp)
                        Text(text = "Amount: ${active.amount} PKR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress workflow control actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (active.status) {
                                "Accepted" -> {
                                    Button(
                                        onClick = { viewModel.updateActiveBookingStatus("In_Progress") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(viewModel.getString("Mark Arrived & Begin", "پہنچ گئے اور شروع کریں"))
                                    }
                                }
                                "In_Progress" -> {
                                    Button(
                                        onClick = { viewModel.updateActiveBookingStatus("Completed") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text(viewModel.getString("Mark Completed", "کام مکمل کریں"))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ChatScreen(active.id)) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("Direct Chat with Customer", "کسٹمر سے چیٹ کریں"))
                        }
                    }
                }
            }
        }

        // CNIC ID verification system
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = viewModel.getString("CNIC/National ID Certificate", "قومی شناختی کارڈ کی تصدیق"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TaskAlt, contentDescription = "Verified Status", tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.getString("ID Verified (CNIC status Active)", "شناختی کارڈ تصدیق شدہ ہو چکا ہے"),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    } else {
                        Text(
                            text = viewModel.getString("Upload 13-digit Pakistani CNIC number (e.g. 3740512345671) to receive the 'Verified Badge' for client trust.", "اپنا 13 ہندسوں کا شناختی کارڈ نمبر لکھیں تاکہ گاہک کی تسلی کے لیے آپ کو ویریفائیڈ بیج دیا جا سکے۔"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = cnicInput,
                            onValueChange = { if (it.length <= 13) viewModel.cnicInput.value = it },
                            label = { Text("CNIC Number (13-digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("cnic_input"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (cnicInput.length >= 13) {
                                    viewModel.verifyCNIC(cnicInput)
                                } else {
                                    Toast.makeText(context, "Kindly write full 13 digits CNIC ID", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("verification_submit_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("Verify Government ID", "شناختی کارڈ تصدیق کریں"))
                        }
                    }
                }
            }
        }

        // Wallet & Withdrawals layout
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.getString("Fixzo Wallet Dashboard", "فکسو والٹ اور آمدنی"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(viewModel.getString("Available Bal", "موجودہ بیلنس"), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            Text("${String.format("%.1f", walletBal)} PKR", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(viewModel.getString("Withdraw Payout Account", "رقم نکالیں (ایزی پیسہ / جیز کیش)"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountWithdrawInput,
                        onValueChange = { amountWithdrawInput = it },
                        label = { Text("Withdraw Amount (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel choice
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = accountChannelInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Transfer Mode") },
                                trailingIcon = { IconButton(onClick = { channelDropdownExpanded = !channelDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }},
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = channelDropdownExpanded,
                                onDismissRequest = { channelDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text("JazzCash") }, onClick = { accountChannelInput = "JazzCash"; channelDropdownExpanded = false })
                                DropdownMenuItem(text = { Text("EasyPaisa") }, onClick = { accountChannelInput = "EasyPaisa"; channelDropdownExpanded = false })
                            }
                        }

                        Button(
                            onClick = {
                                val amtNum = amountWithdrawInput.toDoubleOrNull()
                                if (amtNum != null && amtNum > 0) {
                                    val success = viewModel.requestWithdrawal(amtNum, accountChannelInput)
                                    if (success) {
                                        amountWithdrawInput = ""
                                        Toast.makeText(context, "Withdrawal approved to $accountChannelInput!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Invalid Amount entered", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(top = 6.dp)
                                .testTag("wallet_withdraw_button")
                        ) {
                            Text(viewModel.getString("Withdraw", "رقم نکالیں"))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(viewModel.getString("Wallet Transaction History", "لین دین کی ہسٹری"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (transactions.isEmpty()) {
                        Text("No transactions logged yet", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    } else {
                        transactions.forEach { trans ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(trans.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${trans.referenceChannel} • ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(trans.timestamp))}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(
                                    text = if (trans.type == "Deposit") {
                                        "+${trans.amount} PKR"
                                    } else {
                                        "-${trans.amount} PKR"
                                    },
                                    color = if (trans.type == "Deposit") Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// 5. In-App Chat View Screen with Live tracking Map animation
@Composable
fun ChatScreenView(viewModel: FixzoViewModel, bookingId: Int) {
    val messages by viewModel.getChatMessages(bookingId).collectAsState(initial = emptyList())
    val activeBooking by viewModel.activeBooking.collectAsState()
    val isWorkerMoving by viewModel.isWorkerMoving.collectAsState()
    val workerLat by viewModel.workerLat.collectAsState()
    val workerLng by viewModel.workerLng.collectAsState()
    val customerLat by viewModel.customerLat.collectAsState()
    val customerLng by viewModel.customerLng.collectAsState()

    var textTypedInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Custom GPS Track Animator Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF0F172A))
        ) {
            // Live Tracking map background and lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw simple network streets/roads grid
                val gridPaint = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                for (x in 0..width.toInt() step 60) {
                    drawLine(color = Color(0xFF334155), start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), height), strokeWidth = 1f)
                }
                for (y in 0..height.toInt() step 60) {
                    drawLine(color = Color(0xFF334155), start = Offset(0f, y.toFloat()), end = Offset(width, y.toFloat()), strokeWidth = 1f)
                }

                // Dotted route connecting customer and worker
                val routePathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                
                // Coordinates map values locally (convert LatLng limits relative to frame)
                // Normalize lat long bounds
                val normalizedWorkX = (workerLng - 73.03) / (73.07 - 73.03) * width
                val normalizedWorkY = (1.0 - (workerLat - 33.67) / (33.70 - 33.67)) * height

                val normalizedCustX = (customerLng - 73.03) / (73.07 - 73.03) * width
                val normalizedCustY = (1.0 - (customerLat - 33.67) / (33.70 - 33.67)) * height

                // Route Line
                drawLine(
                    color = Color(0xFF2DD4BF),
                    start = Offset(normalizedWorkX.toFloat(), normalizedWorkY.toFloat()),
                    end = Offset(normalizedCustX.toFloat(), normalizedCustY.toFloat()),
                    strokeWidth = 4f,
                    pathEffect = routePathEffect
                )

                // Customer Pin representation
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 24f,
                    center = Offset(normalizedCustX.toFloat(), normalizedCustY.toFloat())
                )
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.3f),
                    radius = 42f, // pulsating effect simulated with static outer ring
                    center = Offset(normalizedCustX.toFloat(), normalizedCustY.toFloat())
                )

                // Worker dynamic Pin
                drawCircle(
                    color = Color(0xFF14B8A6),
                    radius = 24f,
                    center = Offset(normalizedWorkX.toFloat(), normalizedWorkY.toFloat())
                )
            }

            // High readability overlays
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShareLocation, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWorkerMoving) {
                                viewModel.getString("LIVE: Specialist riding to your home...", "لائیو: کاریگر موٹر سائیکل پر آرہا ہے...")
                            } else {
                                viewModel.getString("Worker arrived near doorstep!", "کاریگر آپ کے پتے پر پہنچ چکا ہے!")
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = viewModel.getString("🟢 Worker Pin", "🟢 کاریگر کا نشان"),
                        color = Color(0xFF14B8A6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp)
                    )
                    Text(
                        text = viewModel.getString("🔴 Your Home", "🔴 آپ کا گھر"),
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp)
                    )
                }
            }
        }

        // Messages box scroll view
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.sender == "Customer"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        ),
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.message,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(msg.timestamp)),
                                fontSize = 9.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Input bottom bar toolbar
        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textTypedInput,
                    onValueChange = { textTypedInput = it },
                    placeholder = { Text(viewModel.getString("Type message to worker...", "پیغام لکھیں...")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        if (textTypedInput.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(bookingId, textTypedInput)
                            textTypedInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_chat_button")
                        .size(46.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

// 6. Estimated Cost Calculator Screen
@Composable
fun CostCalculatorScreen(viewModel: FixzoViewModel) {
    val language by viewModel.language.collectAsState()
    val context = LocalContext.current

    // Sample list of Pakistani market transparent service tasks
    val tradesChecklist = listOf(
        RecipeItem("Complete Plumbing Diagnosis", 500.0, "Plumbing"),
        RecipeItem("Tap & Leakage Repair", 350.0, "Plumbing"),
        RecipeItem("Washbasin Sink Installation", 1200.0, "Plumbing"),
        RecipeItem("Switchboard Switch Replacement", 300.0, "Electrical"),
        RecipeItem("Ceiling Fan Rewinding Repair", 900.0, "Electrical"),
        RecipeItem("Inverter UPS Setup Installation", 2000.0, "Electrical"),
        RecipeItem("AC Deep Cooling Wash Cleanse", 1500.0, "AC Technician"),
        RecipeItem("Gas Leak Seal Refilling", 3500.0, "AC Technician"),
        RecipeItem("Main Door Cylinder Lock Replace", 1100.0, "Carpenter"),
        RecipeItem("Room Wall Oil Paint (per sq.ft)", 150.0, "Painter")
    )

    val selectedRecipes = remember { mutableStateListOf<RecipeItem>() }
    var sumTotal = remember(selectedRecipes.size) { selectedRecipes.sumOf { it.pkrCharge } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = viewModel.getString("Transparent Cost Calculator", "قیمت کا شفاف تخمینہ"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.getString("Predefined market rates standard across Pakistan. No hidden extra fee.", "پورے پاکستان میں مستند اور فکسڈ چارجز کی لسٹ۔ کاریگر فالتو پیسے نہیں مانگ سکتا۔"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tradesChecklist) { item ->
                val isChecked = selectedRecipes.any { it.label == item.label }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isChecked) {
                                selectedRecipes.removeAll { it.label == item.label }
                            } else {
                                selectedRecipes.add(item)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            "${item.pkrCharge.toInt()} PKR",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Receipt Summary
        Surface(
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        viewModel.getString("Estimated Receipt Total:", "کل اندازاً رقم:"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "$sumTotal PKR",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFFD97706),
                        modifier = Modifier.testTag("calculator_estimate_amount")
                    )
                }

                Text(
                    text = viewModel.getString("Estimated tax/visits elements are included.", "اس میں آمد کے تمام چارجز شامل ہیں۔"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Button(
                    onClick = {
                        if (sumTotal > 0) {
                            Toast.makeText(context, "Redirecting to worker booking with PKR $sumTotal estimate...", Toast.LENGTH_LONG).show()
                            viewModel.navigateBack()
                        } else {
                            Toast.makeText(context, "Select at least one task", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(viewModel.getString("Apply Estimate to Search", "تخمینہ شدہ قیمت لاگو کریں"))
                }
            }
        }
    }
}

data class RecipeItem(val label: String, val pkrCharge: Double, val category: String)

// 7. Emergency Panic Broadcast Portal Screen
@Composable
fun EmergencyPortalScreen(viewModel: FixzoViewModel) {
    val language by viewModel.language.collectAsState()
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
    val stage by viewModel.emergencyStage.collectAsState()
    val logs by viewModel.emergencyLog.collectAsState()

    // Pulsating animation scale for the panic buzzer button
    val infiniteTransition = rememberInfiniteTransition(label = "BuzzerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = viewModel.getString("Urgent Emergency Dispatch", "ہنگامی مدد کا سینٹر"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color(0xFFEF4444)
            )
            Text(
                text = viewModel.getString(
                    "Broadcast immediate GPS distress signals to all registered plumbers & technicians within 5km for rapid SOS visit.",
                    "اپنے ۵ کلومیٹر کے دائرے میں تمام رجسٹرڈ کاریگروں کو مدد کا ہنگامی سگنل بھیجیں"
                ),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large animated push button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                modifier = Modifier.size(170.dp * pulseScale)
            ) {}
            Surface(
                shape = CircleShape,
                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                modifier = Modifier.size(140.dp * pulseScale)
            ) {}
            Surface(
                shape = CircleShape,
                color = Color(0xFFEF4444),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(110.dp)
                    .clickable { viewModel.triggerEmergencyPanic() }
                    .testTag("emergency_panic_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = "Distress SOS", tint = Color.White, modifier = Modifier.size(36.dp))
                        Text(
                            text = "SOS",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Live logs displaying responses
        if (isEmergencyActive) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        viewModel.getString("SOS BROADCAST RADAR LOG:", "ہنگامی الرٹ کی تفصیلات:"),
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(logs) { log ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("➔", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(log, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    if (stage == "broadcasting") {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(color = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth())
                    }

                    if (stage == "responded") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetEmergency() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(viewModel.getString("Deactivate Alert & Close", "الرٹ بند کریں"))
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Tap SOS to begin search",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
