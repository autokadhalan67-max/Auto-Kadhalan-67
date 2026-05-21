package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AruppukottaiMapData
import com.example.data.MapLocation
import com.example.data.Ride
import com.example.ui.ActiveCall
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun AutoKadhalanMainUi(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (currentUser == null) {
            OnboardingLoginScreen(viewModel)
        } else {
            MainTabsNavigation(viewModel)
        }

        // Overlay of calling screen if call is active
        activeCall?.let { call ->
            CallOverlayScreen(call = call, viewModel = viewModel)
        }
    }
}

@Composable
fun MainTabsNavigation(viewModel: AppViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()
    val currentRide by viewModel.currentRide.collectAsState()

    // Determine tabs based on role
    val tabs = remember(currentUser?.role) {
        if (currentUser?.role == "driver") {
            listOf("Driver Job", "Call Logs", "Earnings & Ratings", "Share & Install")
        } else {
            listOf("Book Ride", "My Journeys", "Call Logs", "Share & Install")
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val icon = when (label) {
                        "Book Ride" -> Icons.Default.Directions
                        "Driver Job" -> Icons.Default.TaxiAlert
                        "My Journeys", "Earnings & Ratings" -> Icons.Default.History
                        "Call Logs" -> Icons.Default.Phone
                        else -> Icons.Default.Share
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentUser?.role) {
                "driver" -> {
                    when (selectedTab) {
                        0 -> DriverDashboardScreen(viewModel)
                        1 -> CallLogsDashboard(viewModel)
                        2 -> DriverEarningsRatingsScreen(viewModel)
                        3 -> DownloadShareScreen()
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> CustomerRideBookingScreen(viewModel)
                        1 -> CustomerJourneyHistoryScreen(viewModel)
                        2 -> CallLogsDashboard(viewModel)
                        3 -> DownloadShareScreen()
                    }
                }
            }
        }
    }
}

// ==========================================
// ONBOARDING & LOGIN SCREEN
// ==========================================
@Composable
fun OnboardingLoginScreen(viewModel: AppViewModel) {
    var isRegistering by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("customer") } // "customer" or "driver"
    
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }

    val loginError by viewModel.loginError.collectAsState()
    val registerStatus by viewModel.registerStatus.collectAsState()

    // Earthy Natural Tones branding colors
    val naturalCream = Color(0xFFF7F2EE)
    val mauveRose = Color(0xFF7A5C61)
    val earthChocolate = Color(0xFF5D4037)
    val oliveGreen = Color(0xFF4A6741)
    val softSand = Color(0xFFE0D7D0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(naturalCream)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Brand logo banner with Forest Olive color background
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(mauveRose, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🛺", fontSize = 48.sp, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "AUTO KADHALAN 67",
            color = earthChocolate,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "அருப்புக்கோட்டை தாலுகா • Aruppukottai",
            color = Color(0xFF8D6E63),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Toggle tabs for Login vs Register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(softSand.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Button(
                onClick = { isRegistering = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRegistering) mauveRose else Color.Transparent,
                    contentColor = if (!isRegistering) Color.White else earthChocolate
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null
            ) {
                Text("Login", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { isRegistering = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRegistering) mauveRose else Color.Transparent,
                    contentColor = if (isRegistering) Color.White else earthChocolate
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null
            ) {
                Text("Register", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role Selector for User
        Text(
            text = "I WANT TO LOG IN AS:",
            color = earthChocolate.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "customer" },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "customer") mauveRose.copy(alpha = 0.12f) else Color.White
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (selectedRole == "customer") mauveRose else softSand
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧑‍💼", fontSize = 28.sp)
                    Text("Customer", color = earthChocolate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedRole = "driver" },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == "driver") mauveRose.copy(alpha = 0.12f) else Color.White
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (selectedRole == "driver") mauveRose else softSand
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛺", fontSize = 28.sp)
                    Text("Driver", color = earthChocolate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Fields
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number", color = earthChocolate.copy(alpha = 0.8f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = earthChocolate,
                unfocusedTextColor = earthChocolate,
                focusedBorderColor = mauveRose,
                unfocusedBorderColor = softSand,
                focusedLabelColor = mauveRose,
                unfocusedLabelColor = earthChocolate.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isRegistering) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = earthChocolate.copy(alpha = 0.8f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = earthChocolate,
                    unfocusedTextColor = earthChocolate,
                    focusedBorderColor = mauveRose,
                    unfocusedBorderColor = softSand,
                    focusedLabelColor = mauveRose,
                    unfocusedLabelColor = earthChocolate.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Secret Login PIN", color = earthChocolate.copy(alpha = 0.8f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = earthChocolate,
                unfocusedTextColor = earthChocolate,
                focusedBorderColor = mauveRose,
                unfocusedBorderColor = softSand,
                focusedLabelColor = mauveRose,
                unfocusedLabelColor = earthChocolate.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isRegistering && selectedRole == "driver") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = { vehicleNumber = it },
                placeholder = { Text("e.g. TN-67-A-6700", color = Color.Gray) },
                label = { Text("Aruppukottai Auto Registration Number", color = earthChocolate.copy(alpha = 0.8f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = earthChocolate,
                    unfocusedTextColor = earthChocolate,
                    focusedBorderColor = mauveRose,
                    unfocusedBorderColor = softSand,
                    focusedLabelColor = mauveRose,
                    unfocusedLabelColor = earthChocolate.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Errors or Status
        loginError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }

        registerStatus?.let { status ->
            if (status == "SUCCESS") {
                Text("Account created successfully! Logging in...", color = oliveGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            } else {
                Text(status, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            }
        }

        // CTA Submit Button
        Button(
            onClick = {
                if (isRegistering) {
                    viewModel.register(phone, name, pin, selectedRole, vehicleNumber)
                } else {
                    viewModel.login(phone, pin, selectedRole)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = mauveRose, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = if (isRegistering) "REGISTER & START DRIVING" else "LOG IN NOW",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Simulated quick login links for quick testing
        Text(
            "QUICK SEED DEV ACCOUNTS:",
            color = earthChocolate.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    phone = "8888888888"
                    pin = "1234"
                    selectedRole = "customer"
                    isRegistering = false
                    viewModel.login("8888888888", "1234", "customer")
                },
                colors = ButtonDefaults.buttonColors(containerColor = softSand),
                modifier = Modifier.weight(1f)
            ) {
                Text("Demo Customer", fontSize = 11.sp, color = earthChocolate)
            }

            Button(
                onClick = {
                    phone = "9876543210"
                    pin = "6700"
                    selectedRole = "driver"
                    isRegistering = false
                    viewModel.login("9876543210", "6700", "driver")
                },
                colors = ButtonDefaults.buttonColors(containerColor = softSand),
                modifier = Modifier.weight(1f)
            ) {
                Text("Kadhalan Driver", fontSize = 11.sp, color = earthChocolate)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ==========================================
// ARUPPUKOTTAI TALUK INTERACTIVE VECTOR DESIGN MAP
// ==========================================
@Composable
fun AruppukottaiInteractiveMap(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onPinClicked: (MapLocation) -> Unit = {}
) {
    val locations = AruppukottaiMapData.locations
    val roads = AruppukottaiMapData.keyRoads
    
    val selectedPickup by viewModel.selectedPickup.collectAsState()
    val selectedDrop by viewModel.selectedDrop.collectAsState()
    val currentRide by viewModel.currentRide.collectAsState()
    
    val autoX by viewModel.autoX.collectAsState()
    val autoY by viewModel.autoY.collectAsState()
    val progress by viewModel.navProgress.collectAsState()

    // Infinitely pulsing ripple animation around selected places and auto
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by transition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE6DFD6)) // Warm sand/clay ground tone
            .border(2.dp, Color(0xFFE0D7D0), RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locations) {
                    detectTapGestures { offset ->
                        // Find if user clicked near any landmark in Aruppukottai
                        var closestLoc: MapLocation? = null
                        var minDistance = Float.MAX_VALUE
                        for (loc in locations) {
                            val lx = loc.x * size.width
                            val ly = loc.y * size.height
                            val clickedDist = sqrt((offset.x - lx) * (offset.x - lx) + (offset.y - ly) * (offset.y - ly))
                            if (clickedDist < 60f && clickedDist < minDistance) {
                                closestLoc = loc
                                minDistance = clickedDist
                            }
                        }
                        closestLoc?.let {
                            onPinClicked(it)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw grid backdrop lines mimicking natural map textures
            val gridStep = 40f
            for (currX in 0..(w.toInt()) step gridStep.toInt()) {
                drawLine(
                    color = Color(0xFF7A5C61).copy(alpha = 0.08f),
                    start = Offset(currX.toFloat(), 0f),
                    end = Offset(currX.toFloat(), h),
                    strokeWidth = 1f
                )
            }
            for (currY in 0..(h.toInt()) step gridStep.toInt()) {
                drawLine(
                    color = Color(0xFF7A5C61).copy(alpha = 0.08f),
                    start = Offset(0f, currY.toFloat()),
                    end = Offset(w, currY.toFloat()),
                    strokeWidth = 1f
                )
            }

            // 2. Draw agricultural/nature green zones with soft sage colors
            drawCircle(color = Color(0xFFD3E2CD), radius = 100f, center = Offset(w * 0.2f, h * 0.15f))
            drawCircle(color = Color(0xFFD3E2CD), radius = 120f, center = Offset(w * 0.85f, h * 0.6f))
            drawCircle(color = Color(0xFFEFE4DA), radius = 80f, center = Offset(w * 0.3f, h * 0.75f)) // Warm clay highlight

            // 3. Draw Road network in dark chocolate slate gray
            roads.forEach { road ->
                val path = androidx.compose.ui.graphics.Path()
                road.points.forEachIndexed { i, loc ->
                    val lx = loc.x * w
                    val ly = loc.y * h
                    if (i == 0) path.moveTo(lx, ly) else path.lineTo(lx, ly)
                }

                // Asphalt road base representation
                drawPath(
                    path = path,
                    color = Color(0xFF8D6E63), // Soft wood brown map roads
                    style = Stroke(width = 12f, pathEffect = PathEffect.cornerPathEffect(40f))
                )

                // Lane divider center line
                drawPath(
                    path = path,
                    color = Color(0xFFF7F2EE), // Off-white lane markers
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )
            }

            // 4. Draw dynamic navigation route in Terracotta Rose
            if (selectedPickup != null && selectedDrop != null) {
                val px = selectedPickup!!.x * w
                val py = selectedPickup!!.y * h
                val dx = selectedDrop!!.x * w
                val dy = selectedDrop!!.y * h

                // Navigation route highlighted in rich terracotta rose
                drawLine(
                    color = Color(0xFF7A5C61),
                    start = Offset(px, py),
                    end = Offset(dx, dy),
                    strokeWidth = 6.0f
                )
            }

            // 5. Draw Location Pins and Labels in chocolate brown text
            val paintText = Paint().apply {
                color = android.graphics.Color.parseColor("#5D4037")
                textSize = 24f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            locations.forEach { loc ->
                val lx = loc.x * w
                val ly = loc.y * h

                val isPickup = selectedPickup?.id == loc.id
                val isDrop = selectedDrop?.id == loc.id

                // Pulse effects for selected spots
                if (isPickup || isDrop) {
                    drawCircle(
                        color = if (isPickup) Color(0xFF4A6741).copy(alpha = pulseAlpha) else Color(0xFF7A5C61).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = Offset(lx, ly)
                    )
                }

                // Pin backdrop: Olive green for pickup, Mauve for drop, soft wood brown for others
                drawCircle(
                    color = when {
                        isPickup -> Color(0xFF4A6741)
                        isDrop -> Color(0xFF7A5C61)
                        else -> Color(0xFF8D6E63)
                    },
                    radius = if (isPickup || isDrop) 11f else 7f,
                    center = Offset(lx, ly)
                )

                drawCircle(
                    color = Color.White,
                    radius = if (isPickup || isDrop) 4f else 3f,
                    center = Offset(lx, ly)
                )

                // Render tiny labels text nicely using native canvas
                drawContext.canvas.nativeCanvas.drawText(
                    loc.name,
                    lx,
                    ly - 16f,
                    paintText
                )
            }

            // 6. Draw Simulated Auto "Real Tracking Navigator" on canvas
            if (selectedPickup != null && selectedDrop != null && (currentRide?.status == "TRIP_STARTED" || currentRide?.status == "ACCEPTED" || currentRide?.status == "ARRIVED")) {
                val ax = autoX * w
                val ay = autoY * h

                // Ripple indicating auto vehicle radio beacon GPS
                drawCircle(
                    color = Color(0xFF7A5C61).copy(alpha = pulseAlpha),
                    radius = pulseRadius + 6f,
                    center = Offset(ax, ay)
                )

                // Rickshaw outer chassis
                drawCircle(
                    color = Color(0xFF5D4037),
                    radius = 12f,
                    center = Offset(ax, ay)
                )

                // Yellow canopy of our Auto Rickshaw Kadhalan 67
                drawCircle(
                    color = Color(0xFFEFE4DA),
                    radius = 9f,
                    center = Offset(ax, ay)
                )

                // Front indicator triangle pointer
                val pointerPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(ax, ay - 14f)
                    lineTo(ax - 6f, ay - 6f)
                    lineTo(ax + 6f, ay - 6f)
                    close()
                }
                drawPath(pointerPath, Color(0xFF7A5C61))
            }
        }

        // Overlay floating indicators
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5D4037).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.TopEnd)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4A6741), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("GPS Live: Aruppukottai", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// CUSTOMER SIDE: BOOKING VIEW
// ==========================================
@Composable
fun CustomerRideBookingScreen(viewModel: AppViewModel) {
    val selectedPickup by viewModel.selectedPickup.collectAsState()
    val selectedDrop by viewModel.selectedDrop.collectAsState()
    
    val currentRide by viewModel.currentRide.collectAsState()
    val navInstructions by viewModel.navInstructions.collectAsState()
    val autoX by viewModel.autoX.collectAsState()
    val autoY by viewModel.autoY.collectAsState()
    val flowDrivers by viewModel.allDrivers.collectAsState()
    
    val speed by viewModel.simulatedSpeed.collectAsState()
    val eta by viewModel.simulatedEta.collectAsState()

    var paymentMethod by remember { mutableStateOf("Wallet") } // Wallet, UPI, Card, Cash
    var showPaymentGateway by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    
    val pickupLocations = AruppukottaiMapData.locations
    var pickupExpanded by remember { mutableStateOf(false) }
    var dropExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header intro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto Kadhalan 67 🛺",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Book safe, reliable local rides", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            // Drop wallet chip
            val currentUser by viewModel.currentUser.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.clickable { viewModel.triggerQuickDeposit(200.0) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rs. ${String.format("%.1f", currentUser?.balance ?: 0.0)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Live guidance instruction box
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📢", fontSize = 20.sp)
                Text(
                    text = navInstructions,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Map
        AruppukottaiInteractiveMap(
            viewModel = viewModel,
            onPinClicked = { loc ->
                if (selectedPickup == null) {
                    viewModel.selectPickup(loc)
                } else if (selectedDrop == null && loc.id != selectedPickup?.id) {
                    viewModel.selectDrop(loc)
                } else {
                    // Reset selection toggle and select this as new pickup
                    viewModel.selectPickup(loc)
                }
            }
        )

        if (currentRide == null) {
            // PICKUP AND DROP SELECTION FORMS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📍 Select Location (Tap Map or Dropdown)", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    // Pickup select dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { pickupExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = selectedPickup?.name ?: "🟢 Choose Pickup Point",
                                color = if (selectedPickup != null) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = pickupExpanded,
                            onDismissRequest = { pickupExpanded = false }
                        ) {
                            pickupLocations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        viewModel.selectPickup(loc)
                                        pickupExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Drop select dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = selectedDrop?.name ?: "🔴 Choose Destination Drop",
                                color = if (selectedDrop != null) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = dropExpanded,
                            onDismissRequest = { dropExpanded = false }
                        ) {
                            pickupLocations.filter { it.id != selectedPickup?.id }.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        viewModel.selectDrop(loc)
                                        dropExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Fare computation
                    if (selectedPickup != null && selectedDrop != null) {
                        val distance = AruppukottaiMapData.calculateDistanceKm(selectedPickup!!, selectedDrop!!)
                        val fare = AruppukottaiMapData.calculateFare(selectedPickup!!, selectedDrop!!)
                        
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Aruppukottai Auto Tariff", fontSize = 12.sp, color = Color.Gray)
                                Text("${String.format("%.2f", distance)} Km route distance", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Rs. ${String.format("%.1f", fare)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }

                        // Payment Methods selection
                        Text("Payment Gateway Options:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Wallet", "UPI", "Card", "Cash").forEach { method ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { paymentMethod = method },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (paymentMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (paymentMethod == method) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Text(
                                        text = method,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { viewModel.bookRide(paymentMethod) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A5C61), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = "book")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BOOK ARUPPUKOTTAI AUTO NOW", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            // BOOKING IS ACTIVE (TRACKING & NAVIGATOR IN REALTIME)
            val ride = currentRide!!
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE TRIP ID: #${ride.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Auto Kadhalan Navigator Tracking", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                        
                        // Status badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (ride.status) {
                                    "PENDING" -> Color(0xFFFF9800)
                                    "ACCEPTED" -> Color(0xFF2196F3)
                                    "ARRIVED" -> Color(0xFF4CAF50)
                                    "TRIP_STARTED" -> Color(0xFF9C27B0)
                                    else -> Color.Gray
                                }
                            )
                        ) {
                            Text(
                                text = ride.status,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM", fontSize = 11.sp, color = Color.Gray)
                            Text(ride.pickupName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TO", fontSize = 11.sp, color = Color.Gray)
                            Text(ride.dropName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Simulated live odometer telemetry data
                    if (ride.status == "TRIP_STARTED") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SIMULATED SPEED", fontSize = 10.sp, color = Color.Gray)
                                Text("$speed km/h", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ESTIMATED ETA", fontSize = 10.sp, color = Color.Gray)
                                Text("$eta mins remaining", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Display details of the assigned driver
                    if (ride.driverName.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🛺", fontSize = 24.sp)
                                    Column {
                                        Text(ride.driverName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kadhalan Auto: $speed KMPH", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                // Interactive Simulated Phone Call Button
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            viewModel.initiatePhoneCall(ride.driverPhone, ride.driverName)
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call Driver", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // PAYMENT AND RECEIPT GATEWAYS OVERLAY CHECKS
                    if (ride.status == "COMPLETED") {
                        if (ride.paymentStatus == "PENDING") {
                            Text(
                                "💳 COMPLETE TRANSACTION PAYMENT GATEWAY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Button(
                                onClick = { showPaymentGateway = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.White)
                            ) {
                                Text("Pay Fare: Rs. ${String.format("%.1f", ride.fare)}", fontWeight = FontWeight.Black)
                            }
                        } else {
                            // Paid: show driver review stars!
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Text("Rate and Review Driver", fontSize = 13.sp, fontWeight = FontWeight.Black)
                            
                            var ratingValue by remember { mutableStateOf(5f) }
                            var userFeedback by remember { mutableStateOf("") }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star",
                                        tint = if (star <= ratingValue) Color(0xFFFFB300) else Color.LightGray,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable { ratingValue = star.toFloat() }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${ratingValue.toInt()} Stars", fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = userFeedback,
                                onValueChange = { userFeedback = it },
                                placeholder = { Text("e.g. Safe driving, clean auto!") },
                                label = { Text("Enter Review Comments") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.submitDriverRating(ratingValue, userFeedback)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("SUBMIT REVIEW & RECEIPT", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Cancel button
                        OutlinedButton(
                            onClick = { viewModel.cancelRide() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("CANCEL THIS JOURNEY REQ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Available Drivers List
        Text("🚀 TOP AUTO KADHALAN DRIVERS IN ARUPPUKOTTAI:", fontSize = 14.sp, fontWeight = FontWeight.Black)
        
        flowDrivers.forEach { dr ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🛺", fontSize = 24.sp)
                        Column {
                            Text(dr.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Plate: ${dr.vehicleNumber}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${String.format("%.1f", dr.rating)}", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Interactive simulated pop up Payment Gateway Dialog
    if (showPaymentGateway && currentRide != null) {
        AlertDialog(
            onDismissRequest = { showPaymentGateway = false },
            title = {
                Text(
                    text = "Auto Kadhalan Pay Gate Integration",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                var cardNumber by remember { mutableStateOf("") }
                var upiPin by remember { mutableStateOf("") }
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Securely Process Simulated payment transactions:", fontSize = 12.sp, color = Color.Gray)
                    
                    if (paymentMethod == "Card") {
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { if (it.length <= 16) cardNumber = it },
                            label = { Text("Secure Debit/Credit Card Number") },
                            placeholder = { Text("xxxx xxxx xxxx xxxx") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = "08/29",
                                onValueChange = {},
                                label = { Text("EXP") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "123",
                                onValueChange = {},
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (paymentMethod == "UPI") {
                        OutlinedTextField(
                            value = "kumarskrishna985@ybl",
                            onValueChange = {},
                            label = { Text("UPI Virtual Payment Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = upiPin,
                            onValueChange = { if (it.length <= 6) upiPin = it },
                            label = { Text("Secure UPI Passcode PIN") },
                            placeholder = { Text("******") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Please pay cash Rs. ${String.format("%.1f", currentRide!!.fare)} directly to Auto driver once journey concludes.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processGatewayPayment(currentRide!!.id, paymentMethod, "SIM_SECURE")
                        showPaymentGateway = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.White)
                ) {
                    Text("AUTHORIZE TRANSACTION")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentGateway = false }) {
                    Text("Cancel Payment")
                }
            }
        )
    }
}


// ==========================================
// DRIVER SIDE: DASHBOARD & NAVIGATOR
// ==========================================
@Composable
fun DriverDashboardScreen(viewModel: AppViewModel) {
    val pendingOffers by viewModel.pendingRides.collectAsState()
    val currentRide by viewModel.currentRide.collectAsState()
    val navInstructions by viewModel.navInstructions.collectAsState()
    val speed by viewModel.simulatedSpeed.collectAsState()
    val eta by viewModel.simulatedEta.collectAsState()

    var isOnline by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aruppukottai Driver Panel 🛺",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isOnline) "🟢 ONLINE - Listening for passengers" else "⚫ OFFLINE - Sleeping Mode",
                    color = if (isOnline) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Duty toggler
            Switch(
                checked = isOnline,
                onCheckedChange = { isOnline = it }
            )
        }

        // Interactive Map
        AruppukottaiInteractiveMap(viewModel = viewModel)

        if (currentRide == null) {
            Text("⚡ INBOX PENDING OFFERS (ARUPPUKOTTAI REGION):", fontSize = 14.sp, fontWeight = FontWeight.Black)

            if (!isOnline) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You are currently OFFLINE. Toggle online duty switch at top to find auto jobs.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else if (pendingOffers.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Waiting for passenger requests. Standard matching response takes 5-10 secs in Aruppukottai...",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                pendingOffers.forEach { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("New Local Booking!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Rs. ${String.format("%.1f", offer.fare)}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PICKUP POINT", fontSize = 10.sp, color = Color.Gray)
                                    Text(offer.pickupName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DESTINATION DROP", fontSize = 10.sp, color = Color.Gray)
                                    Text(offer.dropName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.acceptRideRequest(offer) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6741), contentColor = Color.White)
                            ) {
                                Text("ACCEPT & GO NAVIGATOR", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        } else {
            // ACTIVE DRIVER NAVIGATING TRIP PANEL
            val ride = currentRide!!
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("JOB INSTANCE: #${ride.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Active Passenger GPS Tracker", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                "Fare: Rs.${String.format("%.1f", ride.fare)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Instruction Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(navInstructions, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rider", fontSize = 10.sp, color = Color.Gray)
                            Text(ride.customerName, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        // Simulated Calling
                        IconButton(
                            onClick = { viewModel.initiatePhoneCall(ride.customerPhone, ride.customerName) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "call customer", tint = Color.White)
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Simulated live navigator telemetry
                    if (ride.status == "TRIP_STARTED") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Odometer speed", fontSize = 11.sp, color = Color.Gray)
                                Text("$speed km/h", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duration ETA", fontSize = 11.sp, color = Color.Gray)
                                Text("$eta minutes", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ACTION SYSTEM FLOW TRIGGERS
                    when (ride.status) {
                        "ACCEPTED" -> {
                            Button(
                                onClick = { viewModel.driverMarkArrived() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
                            ) {
                                Text("I HAVE ARRIVED AT PICKUP POINT", fontWeight = FontWeight.Black)
                            }
                        }
                        "ARRIVED" -> {
                            Button(
                                onClick = { viewModel.driverStartTrip() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White)
                            ) {
                                Text("PASSENGER BOARDED • START GPS TRIP", fontWeight = FontWeight.Black)
                            }
                        }
                        "TRIP_STARTED" -> {
                            Button(
                                onClick = { viewModel.driverCompleteTrip() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0), contentColor = Color.White)
                            ) {
                                Text("COMPLETE JOURNEY & GET RECEIVED ACC", fontWeight = FontWeight.Black)
                            }
                        }
                        "COMPLETED" -> {
                            Text(
                                text = "Trip concluded. Waiting for customer score and payout payment clearance.",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Let driver drop the dashboard cache easily
                            Button(
                                onClick = { viewModel.cancelRide() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("CLEAR ORDER RECEIPT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ==========================================
// CUSTOMER JOURNEYS HISTORY SCREEN
// ==========================================
@Composable
fun CustomerJourneyHistoryScreen(viewModel: AppViewModel) {
    val histories by viewModel.customerRides.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛺 MY ARUPPUKOTTAI JOURNEYS:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)

        if (histories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No history available yet.\nBook a ride with Auto Kadhalan to get listed!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(histories) { ride ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Order #${ride.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("fare: Rs. ${String.format("%.1f", ride.fare)}", fontWeight = FontWeight.Bold)
                            }

                            Text("Route: ${ride.pickupName} ➡️ ${ride.dropName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            
                            if (ride.driverName.isNotEmpty()) {
                                Text("Driver: ${ride.driverName}", fontSize = 12.sp, color = Color.DarkGray)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Status: ${ride.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                if (ride.driverRating > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "*", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                        Text("${ride.driverRating.toInt()} ★", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// DRIVER SIDE: EARNINGS & RATINGS TABLE
// ==========================================
@Composable
fun DriverEarningsRatingsScreen(viewModel: AppViewModel) {
    val histories by viewModel.driverRides.collectAsState()
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📈 PERFORMANCE METRICS:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)

        // Statistics Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.weight(1.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("WALLET BALANCE", fontSize = 10.sp, color = Color.Gray)
                    Text("Rs. ${String.format("%.1f", currentUser?.balance ?: 0.0)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("+100% Secure Bank Settlements", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("REPUTATION SCORE", fontSize = 10.sp, color = Color.Gray)
                    Text("★ ${String.format("%.1f", currentUser?.rating ?: 4.8f)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${currentUser?.ratingsCount ?: 5} Total Feedbacks", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Text("📋 DRIVEN JOB HISTORY LOGS:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No rides driven yet. Start accepting passengers!", color = Color.Gray)
            }
        } else {
            histories.forEach { ride ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Job ID: #${ride.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Earnings: +Rs. ${String.format("%.1f", ride.fare)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }

                        Text("Route: ${ride.pickupName} 🔵 ${ride.dropName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Passenger: ${ride.customerName}", fontSize = 11.sp, color = Color.DarkGray)

                        if (ride.driverRating > 0) {
                            Text(
                                "Passenger Rated: ${ride.driverRating.toInt()} ★ (\"${ride.feedback.ifEmpty { "No comment" }}\")",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// CALL HISTORY MODULES
// ==========================================
@Composable
fun CallLogsDashboard(viewModel: AppViewModel) {
    val callLogs by viewModel.callLogsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📞 CALL LOGS & DIALS:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your voice call history is clean.\nUse the 'Call' icons during ride bookings to initiate secure dials.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(callLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "phone",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text(
                                        text = log.toName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text("Duration: ${log.durationSeconds}s", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.initiatePhoneCall(log.toPhone, log.toName) }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Redial", tint = Color.Green)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// EASY DOWNLOAD & SHARE APP FLYER
// ==========================================
@Composable
fun DownloadShareScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Easy Distribution Panel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(2.dp, Color(0xFF7A5C61))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("QR CODE DOWNLOAD INSTALLER", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Artful Canvas Drawing of QR Code since we want premium layout!
                Canvas(modifier = Modifier.size(160.dp)) {
                    val side = size.width
                    
                    // Outer square
                    drawRect(color = Color.Black, size = size)
                    drawRect(color = Color.White, topLeft = Offset(8f, 8f), size = size.copy(side - 16f, side - 16f))

                    // QR Corners patterns
                    val cornerSize = 45f
                    // Top-Left
                    drawRect(color = Color.Black, topLeft = Offset(16f, 16f), size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize))
                    drawRect(color = Color.White, topLeft = Offset(24f, 24f), size = androidx.compose.ui.geometry.Size(cornerSize - 16f, cornerSize - 16f))
                    drawRect(color = Color.Black, topLeft = Offset(28f, 28f), size = androidx.compose.ui.geometry.Size(cornerSize - 24f, cornerSize - 24f))

                    // Top-Right
                    drawRect(color = Color.Black, topLeft = Offset(side - 16f - cornerSize, 16f), size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize))
                    drawRect(color = Color.White, topLeft = Offset(side - 8f - cornerSize, 24f), size = androidx.compose.ui.geometry.Size(cornerSize - 16f, cornerSize - 16f))
                    drawRect(color = Color.Black, topLeft = Offset(side - 4f - cornerSize, 28f), size = androidx.compose.ui.geometry.Size(cornerSize - 24f, cornerSize - 24f))

                    // Bottom-Left
                    drawRect(color = Color.Black, topLeft = Offset(16f, side - 16f - cornerSize), size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize))
                    drawRect(color = Color.White, topLeft = Offset(24f, side - 8f - cornerSize), size = androidx.compose.ui.geometry.Size(cornerSize - 16f, cornerSize - 16f))
                    drawRect(color = Color.Black, topLeft = Offset(28f, side - 4f - cornerSize), size = androidx.compose.ui.geometry.Size(cornerSize - 24f, cornerSize - 24f))

                    // Simulated matrix dot noises inside
                    for(i in 1..40) {
                        val rx = 24f + (1..10).random() * 11f
                        val ry = 24f + (1..10).random() * 11f
                        
                        if (!(rx < 65f && ry < 65f) && !(rx > side - 65f && ry < 65f) && !(rx < 65f && ry > side - 65f)) {
                            drawCircle(color = Color.Black, radius = 6f, center = Offset(rx, ry))
                        }
                    }
                }

                Text(
                    text = "Scan QR to get APK distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📍 ARUPPUKOTTAI TALUK REACH INFO:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "• Serve 100+ local Auto Operators in Aruppukottai, Virudhunagar DT.\n" +
                "• 1-click installer links for seamless offline Bluetooth & Shareit delivery.\n" +
                "• Live support group directly connected.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A5C61), contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "share")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SHARE APK ON WHATSAPP", fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ==========================================
// CALL RINGING/CONNECTED OVERLAY COMPOSABLE
// ==========================================
@Composable
fun CallOverlayScreen(call: ActiveCall, viewModel: AppViewModel) {
    // Beautiful full-screen call pager
    val transitionSpec = remember { fadeIn(tween(400)) + slideInVertically(initialOffsetY = { it }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)) // Full deep dark background for visual call
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Pulse wave circle animation around profile avatar
            val transition = rememberInfiniteTransition(label = "pulse_ringing")
            val ringRadius by transition.animateFloat(
                initialValue = 40f,
                targetValue = 100f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radius"
            )
            val ringAlpha by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        if (call.status == "RINGING") {
                            drawCircle(
                                color = Color(0xFF7A5C61).copy(alpha = ringAlpha),
                                radius = ringRadius + 40f
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFF7A5C61), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (call.peerRole == "Driver") "🛺" else "🧑‍💼",
                        fontSize = 54.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = call.peerName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "${call.peerRole.uppercase()} • ${call.peerPhone}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = when (call.status) {
                        "RINGING" -> "DIALING SECURE LINK..."
                        "CONNECTED" -> "CONNECTED: ${String.format("%02d:%02d", call.durationSeconds / 60, call.durationSeconds % 60)}"
                        else -> "CALL ENDED"
                    },
                    color = if (call.status == "CONNECTED") Color(0xFF4A6741) else Color(0xFF7A5C61),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action call panel
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (call.isIncoming && call.status == "RINGING") {
                    // Accept call button
                    Button(
                        onClick = { viewModel.connectPhoneCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Decline/Hangup key
                Button(
                    onClick = { viewModel.hangUpPhoneCall() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang Up",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
