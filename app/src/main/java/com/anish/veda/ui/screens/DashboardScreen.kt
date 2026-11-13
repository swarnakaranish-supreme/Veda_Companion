package com.anish.veda.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.ui.components.CommandCoreMode
import com.anish.veda.ui.components.VedaBatteryIndicator
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaCommandCore
import com.anish.veda.ui.components.VedaEmergencyButton
import com.anish.veda.ui.components.VedaMetricCard
import com.anish.veda.ui.components.VedaPillButton
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaPulseDot
import com.anish.veda.ui.components.VedaRssiBars
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.components.VedaSensorQualityMeter
import com.anish.veda.ui.components.VedaStatusChip
import com.anish.veda.ui.components.VedaTopAppBar
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.AccentVioletDim
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
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
import com.anish.veda.utils.Constants
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectionState by viewModel.connectionState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isSyncingTime by viewModel.isSyncingTime.collectAsState()

    val batteryPercent by viewModel.batteryPercent.collectAsState()
    val heartRate by viewModel.heartRateBpm.collectAsState()
    val spo2 by viewModel.spo2Percent.collectAsState()
    val sensorQuality by viewModel.sensorQuality.collectAsState()
    val isDndActive by viewModel.isDndActive.collectAsState()

    var hasBluetoothPermission by remember {
        mutableStateOf(PermissionUtils.hasBluetoothPermissions(context))
    }
    var permissionDeniedNotice by remember {
        mutableStateOf(false)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasBluetoothPermission = granted || PermissionUtils.hasBluetoothPermissions(context)
        if (hasBluetoothPermission) {
            permissionDeniedNotice = false
            viewModel.connect()
        } else {
            permissionDeniedNotice = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasBluetoothPermission = PermissionUtils.hasBluetoothPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val watchDisplayName = when (val state = connectionState) {
        is BleConnectionState.Connected -> state.deviceName
        is BleConnectionState.Connecting -> state.deviceName ?: settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
        is BleConnectionState.ConnectedGatt -> state.deviceName ?: settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
        is BleConnectionState.NegotiatingMtu -> state.deviceName ?: settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
        is BleConnectionState.DiscoveringServices -> state.deviceName ?: settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
        is BleConnectionState.Reconnecting -> state.deviceName ?: settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
        else -> settings.lastDeviceName ?: Constants.DEFAULT_WATCH_NAME
    }

    val watchAddress = when (val state = connectionState) {
        is BleConnectionState.Connected -> state.deviceAddress
        is BleConnectionState.Connecting -> state.deviceAddress
        is BleConnectionState.Reconnecting -> state.deviceAddress
        else -> settings.lastDeviceAddress
    }

    val connectionStatusText = when (connectionState) {
        is BleConnectionState.Connected -> "ONLINE"
        is BleConnectionState.Connecting -> "CONNECTING..."
        is BleConnectionState.ConnectedGatt -> "GATT READY"
        is BleConnectionState.NegotiatingMtu -> "CONFIGURING..."
        is BleConnectionState.DiscoveringServices -> "HANDSHAKE..."
        is BleConnectionState.Reconnecting -> "RECONNECTING..."
        is BleConnectionState.Scanning -> "SCANNING..."
        is BleConnectionState.Disconnecting -> "DISCONNECTING..."
        is BleConnectionState.Disconnected -> "OFFLINE"
        is BleConnectionState.Error -> "LINK FAULT"
    }

    val connectionStatusColor = when (connectionState) {
        is BleConnectionState.Connected -> StatusSuccess
        is BleConnectionState.Connecting,
        is BleConnectionState.ConnectedGatt,
        is BleConnectionState.NegotiatingMtu,
        is BleConnectionState.DiscoveringServices,
        is BleConnectionState.Reconnecting,
        is BleConnectionState.Scanning -> StatusWarning
        is BleConnectionState.Error -> StatusError
        else -> TextTertiary
    }

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "VEDA V2.1",
                statusText = connectionStatusText,
                statusColor = connectionStatusColor,
                pulsing = connectionState.isConnected,
                onStatusClick = { navController.navigate(Screen.Diagnostics.route) }
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Dashboard.route,
                onNavigate = { route ->
                    if (route != Screen.Dashboard.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        containerColor = CanvasDeepBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // Permission Banner (if required)
            AnimatedVisibility(visible = !hasBluetoothPermission) {
                VedaCard(
                    modifier = Modifier.padding(bottom = VedaSpacing.md),
                    accentColor = StatusWarning
                ) {
                    VedaCardHeader(
                        label = "Bluetooth Permissions Required",
                        statusText = "ACTION REQUIRED",
                        statusColor = StatusWarning
                    )
                    Text(
                        text = "Android 15 requires Bluetooth permissions to connect to your VEDA smartwatch.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(VedaSpacing.md))
                    VedaPrimaryButton(
                        text = "Grant Bluetooth Access",
                        onClick = {
                            bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                        }
                    )
                }
            }

            // ====================================================================
            // VEDA V2.1 COMMAND CORE (6 Interactive Telemetry & Control Modes)
            // ====================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = VedaSpacing.xs),
                contentAlignment = Alignment.Center
            ) {
                val connectedState = connectionState as? BleConnectionState.Connected
                VedaCommandCore(
                    connectionState = connectionState,
                    watchName = watchDisplayName,
                    initialMode = settings.startingDialMode,
                    heartRate = heartRate,
                    spo2 = spo2,
                    sensorQuality = sensorQuality ?: 0,
                    isMeasuringHealth = false,
                    rssi = connectedState?.rssi,
                    lastSyncTimestamp = settings.lastSyncTimestamp,
                    reducedMotion = settings.reducedMotion,
                    onCenterClick = { mode ->
                        when (mode) {
                            CommandCoreMode.NEURAL -> {
                                if (connectionState.isConnected) viewModel.syncTime()
                                else if (!connectionState.isBusy) {
                                    if (hasBluetoothPermission) viewModel.connect()
                                    else bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                                }
                            }
                            CommandCoreMode.HEALTH -> navController.navigate(Screen.Health.route)
                            CommandCoreMode.ENERGY -> navController.navigate(Screen.Diagnostics.route)
                            CommandCoreMode.CHRONOS -> viewModel.syncTime()
                            CommandCoreMode.AI -> navController.navigate(Screen.AiAssistant.route)
                            CommandCoreMode.COMMAND -> {} // Handled via radial overlay
                        }
                    },
                    onDoubleTap = {
                        navController.navigate(Screen.Diagnostics.route)
                    },
                    onRadialAction = { actionKey ->
                        when (actionKey) {
                            "WATCH_FACE" -> navController.navigate(Screen.WatchStudio.route)
                            "BRIGHTNESS" -> navController.navigate(Screen.WatchStudio.route)
                            "DND" -> viewModel.toggleDnd()
                            "HEALTH" -> navController.navigate(Screen.Health.route)
                            "HAPTIC" -> navController.navigate(Screen.HapticStudio.route)
                            "SYNC" -> viewModel.syncTime()
                            "POWER" -> viewModel.toggleDnd()
                            "DIAGNOSTICS" -> navController.navigate(Screen.Diagnostics.route)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // PRIMARY HERO CARD: Watch Connection & Battery Telemetry
            VedaCard(
                accentColor = if (connectionState.isConnected) StatusSuccess else null
            ) {
                VedaCardHeader(
                    label = "Wearable Peripheral Link",
                    statusText = connectionStatusText,
                    statusColor = connectionStatusColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = watchDisplayName,
                            style = Typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = watchAddress ?: "No paired MAC address",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                    }

                    // Signal bars & live pulse
                    if (connectionState.isConnected) {
                        val connectedState = connectionState as? BleConnectionState.Connected
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VedaRssiBars(rssi = connectedState?.rssi)
                            Spacer(modifier = Modifier.width(8.dp))
                            VedaPulseDot(color = StatusSuccess, size = 8.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Telemetry Data Bar: Battery & Last Synchronization
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VedaRadii.md))
                        .background(SurfaceElevated)
                        .padding(horizontal = VedaSpacing.md, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WATCH BATTERY",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        VedaBatteryIndicator(batteryPercent = batteryPercent)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LAST SYNC",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.lastSyncTimestamp > 0)
                                TimeUtils.formatSyncTime(settings.lastSyncTimestamp)
                            else
                                "Never synced",
                            style = Typography.labelMedium.copy(fontSize = 11.sp),
                            color = if (settings.lastSyncTimestamp > 0) TextPrimary else TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Primary Connection Action Button
                if (!connectionState.isConnected && !connectionState.isBusy) {
                    VedaPrimaryButton(
                        text = "Connect VEDA Watch",
                        onClick = {
                            if (hasBluetoothPermission) {
                                viewModel.connect()
                            } else {
                                bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                            }
                        },
                        icon = Icons.Default.Bluetooth
                    )
                } else if (connectionState.isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            VedaSecondaryButton(
                                text = if (isSyncingTime) "Syncing..." else "Quick Sync",
                                onClick = { viewModel.syncTime() },
                                icon = Icons.Default.Sync,
                                enabled = !isSyncingTime
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            VedaSecondaryButton(
                                text = "Disconnect",
                                onClick = { viewModel.disconnect() }
                            )
                        }
                    }
                } else {
                    VedaSecondaryButton(
                        text = "Abort Connection",
                        onClick = { viewModel.disconnect() }
                    )
                }
            }

            // ====================================================================
            // PERSONALIZED WIDGET: BIOMETRIC TELEMETRY (if enabled)
            // ====================================================================
            if (settings.dashboardWidgetHealth) {
                Spacer(modifier = Modifier.height(VedaSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        VedaMetricCard(
                            label = "Heart Rate",
                            value = heartRate?.toString(),
                            unit = "BPM",
                            icon = Icons.Default.Favorite,
                            accentColor = StatusError,
                            onClick = { navController.navigate(Screen.Health.route) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VedaMetricCard(
                            label = "Blood O2",
                            value = spo2?.toString(),
                            unit = "%",
                            icon = Icons.Default.WaterDrop,
                            accentColor = AccentCyan,
                            onClick = { navController.navigate(Screen.Health.route) }
                        )
                    }
                }
            }

            // ====================================================================
            // PERSONALIZED WIDGET: VEDA AI BRIEFING HERO CARD (if enabled)
            // ====================================================================
            if (settings.dashboardWidgetAi) {
                Spacer(modifier = Modifier.height(VedaSpacing.md))
                VedaCard(
                    accentColor = AccentViolet,
                    onClick = { navController.navigate(Screen.AiAssistant.route) }
                ) {
                    VedaCardHeader(
                        label = "VEDA Neural Engine",
                        statusText = "GEMINI ACTIVE",
                        statusColor = AccentViolet
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(AccentVioletDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = AccentViolet
                            )
                        }
                        Spacer(modifier = Modifier.width(VedaSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Smart Summary & Actions",
                                style = Typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Generate daily briefing, smart replies and summarize alerts",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ====================================================================
            // PERSONALIZED WIDGET: 3 CUSTOMIZABLE QUICK ACTIONS (if enabled)
            // ====================================================================
            if (settings.dashboardWidgetQuickActions) {
                Spacer(modifier = Modifier.height(VedaSpacing.md))
                VedaCard {
                    VedaCardHeader(
                        label = "Tactical Quick Actions",
                        statusText = "CUSTOMIZABLE",
                        statusColor = AccentCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val actions = listOf(settings.quickAction1, settings.quickAction2, settings.quickAction3)
                        actions.forEach { actionCode ->
                            Box(modifier = Modifier.weight(1f)) {
                                renderQuickAction(
                                    actionCode = actionCode,
                                    isDndActive = isDndActive,
                                    onFindWatch = { viewModel.findMyWatch() },
                                    onWatchStudio = { navController.navigate(Screen.WatchStudio.route) },
                                    onToggleDnd = { viewModel.toggleDnd() },
                                    onEmergency = { navController.navigate(Screen.Guardian.route) },
                                    onHaptics = { navController.navigate(Screen.HapticStudio.route) },
                                    onHealth = { navController.navigate(Screen.Health.route) },
                                    onMenu = { navController.navigate(Screen.GeneralSettings.route) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.lg))
        }
    }
}

@Composable
private fun renderQuickAction(
    actionCode: String,
    isDndActive: Boolean,
    onFindWatch: () -> Unit,
    onWatchStudio: () -> Unit,
    onToggleDnd: () -> Unit,
    onEmergency: () -> Unit,
    onHaptics: () -> Unit,
    onHealth: () -> Unit,
    onMenu: () -> Unit
) {
    val (title, icon, color, click) = when (actionCode) {
        "WATCH_STUDIO" -> Quad("Faces", Icons.Default.Watch, AccentCyan, onWatchStudio)
        "DND" -> Quad(if (isDndActive) "DND: ON" else "DND: OFF", Icons.Default.DoNotDisturbOn, if (isDndActive) StatusWarning else TextSecondary, onToggleDnd)
        "EMERGENCY" -> Quad("SOS", Icons.Default.Security, StatusError, onEmergency)
        "HAPTIC" -> Quad("Haptic", Icons.Default.Vibration, AccentCyan, onHaptics)
        "HEALTH" -> Quad("Health", Icons.Default.Favorite, StatusError, onHealth)
        "MENU" -> Quad("Menu", Icons.Default.Sync, AccentViolet, onMenu)
        else -> Quad("Find", Icons.Default.FindReplace, AccentCyan, onFindWatch)
    }

    QuickActionPill(
        title = title,
        icon = icon,
        accentColor = color,
        onClick = click
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun QuickActionPill(
    title: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VedaRadii.md))
            .background(SurfaceElevated)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = accentColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = Typography.labelMedium.copy(fontSize = 11.sp),
            color = TextPrimary,
            maxLines = 1
        )
    }
}
