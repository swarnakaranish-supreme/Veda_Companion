package com.anish.veda.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.data.model.ConnectionHistoryEventType
import com.anish.veda.data.model.SyncStatus
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaEmergencyButton
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaPulseDot
import com.anish.veda.ui.components.VedaRssiBars
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
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.BluetoothViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BluetoothScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()

    val metadata by viewModel.deviceMetadata.collectAsState()
    val history by viewModel.connectionHistory.collectAsState()
    val isAutoReconnect by viewModel.isAutoReconnectEnabled.collectAsState()
    val pingLatency by viewModel.pingLatency.collectAsState()
    val isPinging by viewModel.isPingInProgress.collectAsState()
    val troubleshootingGuides = viewModel.troubleshootingGuides

    var expandedTroubleshootIdx by remember { mutableStateOf<Int?>(null) }
    var showHistorySection by remember { mutableStateOf(false) }

    var hasBluetoothPermission by remember {
        mutableStateOf(PermissionUtils.hasBluetoothPermissions(context))
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermission = results.values.all { it } || PermissionUtils.hasBluetoothPermissions(context)
        if (hasBluetoothPermission && isBluetoothEnabled && !connectionState.isConnected && !isScanning) {
            viewModel.startScan()
        }
    }

    val isConnected = connectionState is BleConnectionState.Connected
    val isBusy = connectionState.isBusy

    val headerStatusText = when (connectionState) {
        is BleConnectionState.Connected -> "LINKED"
        is BleConnectionState.Connecting -> "CONNECTING..."
        is BleConnectionState.ConnectedGatt -> "GATT OPEN"
        is BleConnectionState.NegotiatingMtu -> "MTU CONFIG"
        is BleConnectionState.DiscoveringServices -> "DISCOVERING..."
        is BleConnectionState.Reconnecting -> "RECONNECTING"
        is BleConnectionState.Scanning -> "SCANNING"
        is BleConnectionState.Disconnecting -> "DISCONNECTING"
        is BleConnectionState.Disconnected -> "STANDBY"
        is BleConnectionState.Error -> "GATT ERROR"
    }

    val headerStatusColor = when (connectionState) {
        is BleConnectionState.Connected -> StatusSuccess
        is BleConnectionState.Connecting, is BleConnectionState.Reconnecting, is BleConnectionState.Scanning -> AccentCyan
        is BleConnectionState.Error -> StatusError
        else -> TextTertiary
    }

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "Device Hub",
                statusText = headerStatusText,
                statusColor = headerStatusColor,
                pulsing = isConnected || isBusy
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Bluetooth.route,
                onNavigate = { route ->
                    if (route != Screen.Bluetooth.route) {
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
            // PERMISSION / BLUETOOTH ADAPTER BANNER (IF DISABLED)
            // ================================================================
            if (!isBluetoothEnabled) {
                VedaCard(
                    accentColor = StatusError,
                    containerColor = StatusErrorDim.copy(alpha = 0.25f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(VedaSpacing.md))
                        Column {
                            Text(
                                text = "BLUETOOTH HARDWARE IS OFF",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusError
                            )
                            Text(
                                text = "Turn ON Bluetooth in phone quick settings to enable smartwatch connection.",
                                style = Typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(VedaSpacing.md))
            } else if (!hasBluetoothPermission) {
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
                                text = "BLUETOOTH PERMISSIONS REQUIRED",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusWarning
                            )
                            Text(
                                text = "Grant Nearby Devices permission to scan and connect to your watch.",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VedaSpacing.sm))
                    VedaSecondaryButton(
                        text = "GRANT PERMISSION",
                        onClick = {
                            btPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        },
                        height = 38.dp
                    )
                }
                Spacer(modifier = Modifier.height(VedaSpacing.md))
            }

            // ================================================================
            // 1. BLE CONNECTION DASHBOARD
            // ================================================================
            VedaCard(
                accentColor = if (isConnected) StatusSuccess else if (isBusy) AccentCyan else StrokeSubtle
            ) {
                VedaCardHeader(
                    label = "GATT Connection Link",
                    statusText = if (isConnected) "ACTIVE LINK" else if (isBusy) "TRANSIENT" else "IDLE",
                    statusColor = if (isConnected) StatusSuccess else AccentCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(if (isConnected) StatusSuccessDim else CanvasMidnightNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Watch,
                            contentDescription = null,
                            tint = if (isConnected) StatusSuccess else AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(VedaSpacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata.watchName,
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = metadata.macAddress,
                            style = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextSecondary
                        )
                    }

                    if (isConnected && metadata.rssi != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            VedaRssiBars(rssi = metadata.rssi)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${metadata.rssi} dBm",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Connection Telemetry Row (MTU + Latency + Link state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .padding(horizontal = VedaSpacing.md, vertical = VedaSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "LINK STATE", style = Typography.labelSmall, color = TextTertiary)
                        Text(
                            text = headerStatusText,
                            style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = headerStatusColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ATT MTU", style = Typography.labelSmall, color = TextTertiary)
                        Text(
                            text = "${metadata.negotiatedMtu} bytes",
                            style = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "ROUND-TRIP", style = Typography.labelSmall, color = TextTertiary)
                        Text(
                            text = pingLatency?.let { "${it}ms" } ?: "--",
                            style = Typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (pingLatency != null) AccentCyan else TextTertiary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Connection Action Buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (isConnected) {
                        VedaEmergencyButton(
                            text = "DISCONNECT",
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LinkOff,
                            height = 42.dp
                        )
                        Spacer(modifier = Modifier.width(VedaSpacing.sm))
                        VedaSecondaryButton(
                            text = if (isPinging) "PINGING..." else "PING WATCH",
                            onClick = { viewModel.pingWatch() },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Speed,
                            enabled = !isPinging,
                            height = 42.dp
                        )
                    } else {
                        VedaPrimaryButton(
                            text = if (isBusy) "CONNECTING..." else "CONNECT WATCH",
                            onClick = {
                                if (metadata.macAddress != "--") {
                                    viewModel.connectAddress(metadata.macAddress, metadata.watchName)
                                } else {
                                    viewModel.startScan()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Link,
                            loading = isBusy,
                            height = 42.dp
                        )
                        Spacer(modifier = Modifier.width(VedaSpacing.sm))
                        VedaSecondaryButton(
                            text = if (isScanning) "STOP SCAN" else "SCAN RADAR",
                            onClick = {
                                if (isScanning) viewModel.stopScan() else viewModel.startScan()
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                            height = 42.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 2. AUTOMATIC RECONNECTION & STANDBY POLICY
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Auto-Reconnection Engine",
                    statusText = if (isAutoReconnect) "ENABLED" else "DISABLED",
                    statusColor = if (isAutoReconnect) StatusSuccess else TextTertiary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Progressive Backoff Reconnection",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Exponential intervals (1s, 2s, 4s, 8s, 15s, 30s) + standby watchdog monitoring RF link",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isAutoReconnect,
                        onCheckedChange = { viewModel.setAutoReconnect(it) },
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
            // 3. SYNCHRONIZATION STATUS CARD
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Time & Data Synchronization",
                    statusText = metadata.syncStatus.label,
                    statusColor = when (metadata.syncStatus) {
                        SyncStatus.SYNCED -> StatusSuccess
                        SyncStatus.SYNCING -> AccentCyan
                        SyncStatus.FAILED -> StatusError
                        SyncStatus.NEVER_SYNCED -> StatusWarning
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RTC Software Clock Synchronization",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Last synced: ${TimeUtils.formatSyncTime(metadata.lastSyncTimestamp)}",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    VedaSecondaryButton(
                        text = "SYNC NOW",
                        onClick = { viewModel.triggerSyncNow() },
                        enabled = isConnected,
                        modifier = Modifier.width(110.dp),
                        height = 36.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 4. BATTERY INFORMATION (HARDWARE AUDIT COMPLIANT)
            // ================================================================
            VedaCard(accentColor = StatusWarning) {
                VedaCardHeader(
                    label = "Battery Telemetry Circuitry",
                    statusText = "HARDWARE UNAVAILABLE",
                    statusColor = StatusWarning
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(StatusWarningDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(VedaSpacing.md))
                    Column {
                        Text(
                            text = "450mAh 3.7V LiPo Battery Power Supply",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(VedaSpacing.xs))
                        Text(
                            text = metadata.batteryInfo.statusMessage,
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(VedaSpacing.xs))
                        Text(
                            text = "To preserve technical telemetry integrity, VEDA V2 never shows simulated or fabricated battery percentages.",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 5. DEVICE & PROTOCOL SPECIFICATIONS
            // ================================================================
            VedaCard {
                VedaCardHeader(label = "Hardware Architecture & Protocol")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .padding(VedaSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecRow(label = "MICROCONTROLLER", value = metadata.hardwareArchitecture)
                    SpecRow(label = "DISPLAY ENGINE", value = metadata.displaySpecs)
                    SpecRow(label = "FIRMWARE BUILD", value = metadata.firmwareVersion)
                    SpecRow(label = "BLE PROTOCOL", value = metadata.protocolVersion)
                    SpecRow(label = "SERVICE UUID", value = "6c9d0001-2c9f-4e4e-9a9b-1b0c3f25a001")
                    SpecRow(label = "WRITE/NOTIFY UUID", value = "6c9d0002-2c9f-4e4e-9a9b-1b0c3f25a001")
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 6. SENSOR HARDWARE STATUS
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Integrated Sensor & Peripheral Bus",
                    statusText = "4/4 INITIALIZED",
                    statusColor = StatusSuccess
                )

                metadata.sensors.forEachIndexed { index, sensor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(CanvasMidnightNavy)
                            .padding(VedaSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(VedaSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sensor.name,
                                style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${sensor.bus} • ${sensor.description}",
                                style = Typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                        VedaStatusChip(
                            statusText = sensor.statusText,
                            statusColor = StatusSuccess
                        )
                    }
                    if (index < metadata.sensors.size - 1) {
                        Spacer(modifier = Modifier.height(VedaSpacing.xs))
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 7. RADAR SCANNER (WHEN SCANNING OR DISCONNECTED)
            // ================================================================
            if (isScanning || scannedDevices.isNotEmpty()) {
                VedaCard(accentColor = AccentCyan) {
                    VedaCardHeader(
                        label = "BLE Radar Discovery",
                        statusText = if (isScanning) "SCANNING (${scannedDevices.size} FOUND)" else "${scannedDevices.size} DEVICES",
                        statusColor = AccentCyan
                    )

                    if (scannedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = VedaSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        scannedDevices.forEachIndexed { idx, dev ->
                            val isTargetWatch = dev.name?.contains("ANISH", ignoreCase = true) == true ||
                                    dev.name?.contains("VEDA", ignoreCase = true) == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(VedaRadii.sm))
                                    .background(if (isTargetWatch) AccentCyan.copy(alpha = 0.12f) else SurfaceElevated)
                                    .border(1.dp, if (isTargetWatch) AccentCyan else StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                                    .padding(VedaSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTargetWatch) Icons.Default.Watch else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isTargetWatch) AccentCyan else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(VedaSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.name ?: "Unnamed Device",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = dev.address,
                                        style = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = TextTertiary
                                    )
                                }
                                VedaSecondaryButton(
                                    text = "CONNECT",
                                    onClick = { viewModel.connectDevice(dev) },
                                    modifier = Modifier.width(100.dp),
                                    height = 36.dp
                                )
                            }
                            if (idx < scannedDevices.size - 1) {
                                Spacer(modifier = Modifier.height(VedaSpacing.xs))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(VedaSpacing.md))
            }

            // ================================================================
            // 8. CONNECTION HISTORY TIMELINE
            // ================================================================
            VedaCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHistorySection = !showHistorySection },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        VedaCardHeader(
                            label = "Connection History Log",
                            statusText = "${history.size} EVENTS",
                            statusColor = TextSecondary
                        )
                    }
                    IconButton(onClick = { showHistorySection = !showHistorySection }) {
                        Icon(
                            imageVector = if (showHistorySection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }

                AnimatedVisibility(visible = showHistorySection) {
                    Column {
                        if (history.isEmpty()) {
                            Text(
                                text = "No connection events logged yet in current session.",
                                style = Typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(vertical = VedaSpacing.sm)
                            )
                        } else {
                            history.take(10).forEachIndexed { index, entry ->
                                val eventColor = when (entry.eventType) {
                                    ConnectionHistoryEventType.CONNECTED -> StatusSuccess
                                    ConnectionHistoryEventType.DISCONNECTED -> TextTertiary
                                    ConnectionHistoryEventType.CONNECTION_LOST -> StatusWarning
                                    ConnectionHistoryEventType.RECONNECTING -> AccentCyan
                                    ConnectionHistoryEventType.FAILED -> StatusError
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VedaPulseDot(color = eventColor, size = 6.dp, pulsing = false)
                                    Spacer(modifier = Modifier.width(VedaSpacing.sm))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.eventType.label,
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = eventColor
                                        )
                                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                                        val durationStr = entry.durationSeconds?.let { " • Duration: ${it}s" } ?: ""
                                        Text(
                                            text = "$timeStr$durationStr ${entry.notes?.let { "($it)" } ?: ""}",
                                            style = Typography.bodySmall.copy(fontSize = 11.sp),
                                            color = TextTertiary
                                        )
                                    }
                                }
                                if (index < history.size - 1) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(VedaSpacing.sm))
                            VedaSecondaryButton(
                                text = "CLEAR CONNECTION HISTORY",
                                onClick = { viewModel.clearConnectionHistory() },
                                icon = Icons.Default.DeleteSweep,
                                height = 36.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 9. TROUBLESHOOTING & DIAGNOSTIC GUIDANCE ACCORDION
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Diagnostic & Troubleshooting Guides",
                    statusText = "ASSISTANCE",
                    statusColor = AccentCyan
                )

                troubleshootingGuides.forEachIndexed { index, guide ->
                    val isExpanded = expandedTroubleshootIdx == index
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(SurfaceElevated)
                            .border(1.dp, if (isExpanded) AccentCyan.copy(alpha = 0.5f) else StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                            .clickable {
                                expandedTroubleshootIdx = if (isExpanded) null else index
                            }
                            .padding(VedaSpacing.md)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(VedaSpacing.sm))
                            Text(
                                text = guide.title,
                                style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = VedaSpacing.sm)) {
                                Text(
                                    text = "Symptoms: ${guide.symptoms}",
                                    style = Typography.bodySmall,
                                    color = StatusWarning
                                )
                                Spacer(modifier = Modifier.height(VedaSpacing.xs))
                                Text(
                                    text = "Recommended Remediation Steps:",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                guide.steps.forEachIndexed { sIdx, step ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${sIdx + 1}. ",
                                            style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = step,
                                            style = Typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (index < troubleshootingGuides.size - 1) {
                        Spacer(modifier = Modifier.height(VedaSpacing.xs))
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // 10. EXPORT DIAGNOSTICS REPORT
            // ================================================================
            VedaSecondaryButton(
                text = "EXPORT DIAGNOSTIC REPORT",
                onClick = {
                    val report = viewModel.exportDiagnostics()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("VEDA Diagnostics", report))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Diagnostic report copied to clipboard.")
                    }
                },
                icon = Icons.Default.ContentCopy,
                height = 42.dp
            )

            Spacer(modifier = Modifier.height(VedaSpacing.xl))
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.labelSmall.copy(fontSize = 10.sp),
            color = TextTertiary
        )
        Text(
            text = value,
            style = Typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary
        )
    }
}
