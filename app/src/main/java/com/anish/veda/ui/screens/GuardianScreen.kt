package com.anish.veda.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anish.veda.viewmodel.GuardianViewModel
import com.anish.veda.data.model.EmergencyContact
import com.anish.veda.data.model.GuardianConfig
import com.anish.veda.data.model.SosStatus
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaEmergencyButton
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaPulseDot
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.components.VedaStatusChip
import com.anish.veda.ui.components.VedaTopAppBar
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.CanvasMidnightNavy
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusErrorDim
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusSuccessDim
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StatusWarningDim
import com.anish.veda.ui.theme.StrokeFocused
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuardianScreen(
    navController: NavController,
    viewModel: GuardianViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val sosStatus by viewModel.sosStatus.collectAsState()
    val countdown by viewModel.remainingCountdown.collectAsState()
    val lastDispatchMessage by viewModel.lastDispatchMessage.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val config by viewModel.config.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val isWatchConnected by viewModel.isWatchConnected.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }

    // Dynamic Permission Checks
    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPerm = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasSmsPerm = results[Manifest.permission.SEND_SMS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    // Handle Disconnection Alert Events
    LaunchedEffect(Unit) {
        viewModel.disconnectionAlertEvent.collect { alertText ->
            snackbarHostState.showSnackbar(alertText)
        }
    }

    // Refresh location on entry if permitted
    LaunchedEffect(hasLocationPerm) {
        if (hasLocationPerm) {
            viewModel.refreshLocation()
        }
    }

    val isSosActive = sosStatus == SosStatus.COUNTDOWN ||
            sosStatus == SosStatus.FETCHING_LOCATION ||
            sosStatus == SosStatus.DISPATCHING_SMS

    val headerStatusColor = when (sosStatus) {
        SosStatus.IDLE -> StatusSuccess
        SosStatus.COUNTDOWN -> StatusError
        SosStatus.FETCHING_LOCATION, SosStatus.DISPATCHING_SMS -> StatusWarning
        SosStatus.SENT_CONFIRMED -> StatusSuccess
        SosStatus.FAILED -> StatusError
        SosStatus.CANCELLED -> StatusWarning
    }

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "Guardian Safety",
                statusText = sosStatus.label,
                statusColor = headerStatusColor,
                pulsing = isSosActive
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Guardian.route,
                onNavigate = { route ->
                    if (route != Screen.Guardian.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CanvasDeepBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // ================================================================
            // PERMISSION WARNING BANNER (IF NEEDED)
            // ================================================================
            if (!hasLocationPerm || !hasSmsPerm) {
                VedaCard(
                    accentColor = StatusWarning,
                    containerColor = StatusWarningDim.copy(alpha = 0.2f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(VedaSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CRITICAL PERMISSIONS MISSING",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusWarning
                            )
                            val missing = buildList {
                                if (!hasLocationPerm) add("Location (GPS)")
                                if (!hasSmsPerm) add("Send SMS")
                            }.joinToString(", ")
                            Text(
                                text = "Grant $missing to enable automated emergency beacon broadcasts.",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VedaSpacing.sm))
                    VedaSecondaryButton(
                        text = "GRANT PERMISSIONS",
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.SEND_SMS
                                )
                            )
                        },
                        height = 38.dp
                    )
                }
                Spacer(modifier = Modifier.height(VedaSpacing.md))
            }

            // ================================================================
            // HERO SOS BEACON PANEL
            // ================================================================
            when (sosStatus) {
                SosStatus.COUNTDOWN -> {
                    // Pulsing Countdown Hero Panel
                    val pulseTransition = rememberInfiniteTransition(label = "CountdownPulse")
                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    VedaCard(
                        accentColor = StatusError,
                        containerColor = StatusErrorDim.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EMERGENCY COUNTDOWN ACTIVE",
                                style = Typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = StatusError
                            )
                            Spacer(modifier = Modifier.height(VedaSpacing.sm))

                            // Large Digital Timer Counter
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, StatusError, CircleShape)
                                    .background(CanvasMidnightNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d", countdown),
                                    style = Typography.displayLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 44.sp
                                    ),
                                    color = StatusError
                                )
                            }

                            Spacer(modifier = Modifier.height(VedaSpacing.md))
                            Text(
                                text = "Watch and phone vibrating! Automated SMS broadcast will dispatch when countdown completes.",
                                style = Typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = VedaSpacing.md)
                            )

                            Spacer(modifier = Modifier.height(VedaSpacing.lg))
                            VedaEmergencyButton(
                                text = "ABORT & CANCEL SOS",
                                onClick = { viewModel.cancelSos() },
                                icon = Icons.Default.Close,
                                height = 52.dp
                            )
                        }
                    }
                }

                SosStatus.FETCHING_LOCATION, SosStatus.DISPATCHING_SMS -> {
                    VedaCard(
                        accentColor = StatusWarning,
                        containerColor = StatusWarningDim.copy(alpha = 0.25f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = StatusWarning,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(VedaSpacing.md))
                            Text(
                                text = if (sosStatus == SosStatus.FETCHING_LOCATION)
                                    "ACQUIRING SATELLITE GPS FIX..."
                                else
                                    "DISPATCHING EMERGENCY SMS...",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusWarning
                            )
                            Spacer(modifier = Modifier.height(VedaSpacing.xs))
                            Text(
                                text = "Embedding coordinates and broadcasting to ${contacts.size} contact(s)",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(VedaSpacing.md))
                            VedaEmergencyButton(
                                text = "ABORT DISPATCH",
                                onClick = { viewModel.cancelSos() },
                                height = 44.dp
                            )
                        }
                    }
                }

                SosStatus.SENT_CONFIRMED -> {
                    VedaCard(
                        accentColor = StatusSuccess,
                        containerColor = StatusSuccessDim.copy(alpha = 0.25f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(VedaRadii.sm))
                                    .background(StatusSuccess.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(VedaSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EMERGENCY BEACON DISPATCHED",
                                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusSuccess
                                )
                                Text(
                                    text = lastDispatchMessage ?: "SMS delivered to priority contacts with verified GPS fix.",
                                    style = Typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(VedaSpacing.md))
                        VedaSecondaryButton(
                            text = "DISMISS & RE-ARM GUARDIAN",
                            onClick = { viewModel.resetSos() },
                            height = 42.dp
                        )
                    }
                }

                SosStatus.FAILED -> {
                    VedaCard(
                        accentColor = StatusError,
                        containerColor = StatusErrorDim.copy(alpha = 0.25f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(VedaSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DISPATCH FAILED",
                                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusError
                                )
                                Text(
                                    text = lastDispatchMessage ?: "Carrier transmission encountered an error.",
                                    style = Typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(VedaSpacing.md))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            VedaEmergencyButton(
                                text = "RETRY SOS",
                                onClick = { viewModel.triggerSos() },
                                modifier = Modifier.weight(1f),
                                height = 42.dp
                            )
                            Spacer(modifier = Modifier.width(VedaSpacing.sm))
                            VedaSecondaryButton(
                                text = "DISMISS",
                                onClick = { viewModel.resetSos() },
                                modifier = Modifier.weight(1f),
                                height = 42.dp
                            )
                        }
                    }
                }

                SosStatus.CANCELLED -> {
                    VedaCard(accentColor = StatusWarning) {
                        Text(
                            text = "SOS ABORTED BY USER",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusWarning
                        )
                        Text(
                            text = "No emergency SMS broadcast was dispatched.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(VedaSpacing.sm))
                        VedaSecondaryButton(
                            text = "RE-ARM NOW",
                            onClick = { viewModel.resetSos() },
                            height = 38.dp
                        )
                    }
                }

                SosStatus.IDLE -> {
                    // Armed Standby Mode
                    VedaCard(accentColor = StatusError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(VedaRadii.sm))
                                    .background(StatusErrorDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = StatusError
                                )
                            }
                            Spacer(modifier = Modifier.width(VedaSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GUARDIAN SOS BEACON",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Wearable Panic Trigger & Auto-Broadcaster",
                                    style = Typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                            VedaStatusChip(
                                statusText = if (isWatchConnected) "WATCH LINKED" else "OFFLINE",
                                statusColor = if (isWatchConnected) AccentCyan else TextTertiary,
                                pulsing = isWatchConnected
                            )
                        }

                        Spacer(modifier = Modifier.height(VedaSpacing.md))
                        Text(
                            text = "Trigger from watch via ${if (config.gestureMode == 0) "3s Button Hold" else "Triple-Click"} or tap below. Starts a ${config.countdownSeconds}s safety countdown with emergency vibration before broadcasting live GPS.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(VedaSpacing.lg))
                        VedaEmergencyButton(
                            text = "TRIGGER EMERGENCY SOS",
                            onClick = { viewModel.triggerSos() },
                            icon = Icons.Default.Warning,
                            height = 50.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // REAL GPS SATELLITE TELEMETRY CARD
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Live GPS Satellite Telemetry",
                    statusText = if (currentLocation != null) "FIX ACQUIRED" else "NO FIX",
                    statusColor = if (currentLocation != null) StatusSuccess else StatusWarning
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(if (currentLocation != null) StatusSuccessDim else StatusWarningDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (currentLocation != null) StatusSuccess else StatusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(VedaSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentLocation?.displayCoords ?: "Acquiring real location fix...",
                            style = Typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (currentLocation != null) TextPrimary else TextTertiary
                        )
                        if (currentLocation != null) {
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(currentLocation!!.timestamp))
                            Text(
                                text = "Acquired at $timeStr via Android Location Services",
                                style = Typography.bodySmall,
                                color = TextTertiary
                            )
                        } else {
                            Text(
                                text = "Ensure GPS is enabled and location permission granted.",
                                style = Typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VedaSecondaryButton(
                        text = if (isLocating) "ACQUIRING..." else "REFRESH FIX",
                        onClick = { viewModel.refreshLocation() },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh,
                        enabled = !isLocating && hasLocationPerm,
                        height = 40.dp
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.sm))
                    VedaSecondaryButton(
                        text = "VIEW ON MAP",
                        onClick = {
                            currentLocation?.let { loc ->
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(loc.mapsUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Map,
                        enabled = currentLocation != null,
                        height = 40.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // HARDWARE TRIGGER SETTINGS (ESP32-C3 SUPERMINI)
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Smartwatch Hardware Trigger",
                    statusText = if (isWatchConnected) "SYNCED" else "OFFLINE",
                    statusColor = if (isWatchConnected) AccentCyan else TextTertiary
                )

                Text(
                    text = "Select how the Enter push button (GPIO 9) activates emergency mode on your watch:",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(VedaSpacing.sm))

                // Gesture selection
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isHold = config.gestureMode == GuardianConfig.GESTURE_HOLD_3S
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(if (isHold) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                            .border(1.dp, if (isHold) AccentCyan else StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                            .clickable { viewModel.setGestureMode(GuardianConfig.GESTURE_HOLD_3S) }
                            .padding(vertical = VedaSpacing.sm, horizontal = VedaSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HOLD 3 SECONDS",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isHold) AccentCyan else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(VedaSpacing.sm))

                    val isTriple = config.gestureMode == GuardianConfig.GESTURE_TRIPLE_CLICK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(if (isTriple) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                            .border(1.dp, if (isTriple) AccentCyan else StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                            .clickable { viewModel.setGestureMode(GuardianConfig.GESTURE_TRIPLE_CLICK) }
                            .padding(vertical = VedaSpacing.sm, horizontal = VedaSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TRIPLE-CLICK (450ms)",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isTriple) AccentCyan else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))
                Text(
                    text = "COUNTDOWN TIMER DURATION",
                    style = Typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(VedaSpacing.xs))

                // Countdown duration selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VedaSpacing.sm)
                ) {
                    listOf(5, 10, 15, 30).forEach { secs ->
                        val selected = config.countdownSeconds == secs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (selected) AccentViolet.copy(alpha = 0.2f) else SurfaceElevated)
                                .border(1.dp, if (selected) AccentViolet else StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                                .clickable { viewModel.setCountdownSeconds(secs) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${secs}s",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) AccentViolet else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))
                VedaSecondaryButton(
                    text = "SYNC SETTINGS TO WATCH",
                    onClick = { viewModel.syncConfigToWatch() },
                    icon = Icons.Default.Sync,
                    enabled = isWatchConnected,
                    height = 40.dp
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // PREFERENCES & ALERTS
            // ================================================================
            VedaCard {
                VedaCardHeader(label = "Guardian Security Policies")

                // Disconnection Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Watch Link Loss Warning",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Trigger phone alert if watch drops Bluetooth connection unexpectedly",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = config.disconnectionAlertEnabled,
                        onCheckedChange = { viewModel.setDisconnectionAlert(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Vibrate Phone on SOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibrate Phone During SOS",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Haptic pulsing on phone during active countdown phase",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = config.vibratePhoneOnSos,
                        onCheckedChange = { viewModel.setVibratePhone(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // EMERGENCY MESSAGE TEMPLATE
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Emergency SMS Broadcast Template",
                    statusText = "CARRIER SMS",
                    statusColor = AccentCyan
                )

                Text(
                    text = "The message below will be sent via cellular network to all registered emergency contacts upon trigger:",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(VedaSpacing.xs))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(CanvasMidnightNavy)
                        .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                        .padding(VedaSpacing.md)
                ) {
                    Text(
                        text = config.messageTemplate.replace(
                            "{LOCATION}",
                            currentLocation?.let { "${it.mapsUrl} (${it.displayCoords})" }
                                ?: "https://maps.google.com/?q=... (Acquiring GPS fix)"
                        ),
                        style = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(VedaSpacing.xs))
                Text(
                    text = "Token {LOCATION} is automatically replaced with live Google Maps coordinate URL.",
                    style = Typography.labelSmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // EMERGENCY CONTACTS DIRECTORY
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Priority Emergency Contacts",
                    statusText = "${contacts.size} REGISTERED",
                    statusColor = if (contacts.isNotEmpty()) StatusSuccess else StatusWarning
                )

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VedaSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emergency contacts configured.",
                            style = Typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                } else {
                    contacts.forEachIndexed { index, contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (contact.isPrimary) AccentCyan.copy(alpha = 0.5f) else StrokeSubtle,
                                    RoundedCornerShape(VedaRadii.sm)
                                )
                                .padding(VedaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (contact.isPrimary) AccentCyan.copy(alpha = 0.2f) else CanvasMidnightNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (contact.isPrimary) Icons.Default.Star else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (contact.isPrimary) AccentCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(VedaSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    if (contact.isPrimary) {
                                        Spacer(modifier = Modifier.width(VedaSpacing.xs))
                                        Text(
                                            text = "PRIMARY",
                                            style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = AccentCyan
                                        )
                                    }
                                }
                                Text(
                                    text = "${contact.phoneNumber} • ${contact.relationship}",
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeContact(contact.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove contact",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < contacts.size - 1) {
                            Spacer(modifier = Modifier.height(VedaSpacing.sm))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))
                VedaSecondaryButton(
                    text = "ADD EMERGENCY CONTACT",
                    onClick = { showAddContactDialog = true },
                    icon = Icons.Default.Add,
                    height = 42.dp
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.xl))
        }
    }

    // ========================================================================
    // ADD CONTACT MODAL DIALOG
    // ========================================================================
    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var relationship by remember { mutableStateOf("") }
        var isPrimary by remember { mutableStateOf(contacts.isEmpty()) }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = SurfaceGraphite,
            title = {
                Text(
                    text = "Add Emergency Contact",
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = StrokeSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = TextTertiary
                        )
                    )
                    Spacer(modifier = Modifier.height(VedaSpacing.sm))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = StrokeSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = TextTertiary
                        )
                    )
                    Spacer(modifier = Modifier.height(VedaSpacing.sm))
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Relationship (e.g. Spouse, Parent, Doctor)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = StrokeSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = TextTertiary
                        )
                    )
                    Spacer(modifier = Modifier.height(VedaSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set as Primary Contact",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isPrimary,
                            onCheckedChange = { isPrimary = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(VedaSpacing.xs))
                        Text(
                            text = validationError!!,
                            style = Typography.bodySmall,
                            color = StatusError
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanPhone = phone.trim()
                        if (cleanName.isEmpty()) {
                            validationError = "Please enter a valid contact name."
                            return@TextButton
                        }
                        if (cleanPhone.isEmpty() || cleanPhone.length < 3) {
                            validationError = "Please enter a valid telephone number."
                            return@TextButton
                        }
                        viewModel.addContact(
                            name = cleanName,
                            phone = cleanPhone,
                            relationship = relationship.trim().ifEmpty { "Emergency Contact" },
                            isPrimary = isPrimary
                        )
                        showAddContactDialog = false
                    }
                ) {
                    Text(
                        text = "SAVE CONTACT",
                        color = AccentCyan,
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text(text = "CANCEL", color = TextTertiary)
                }
            }
        )
    }
}
