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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.anish.veda.ui.components.CircularStatusGauge
import com.anish.veda.ui.components.DestructiveOutlinedButton
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.GlowingOutlinedButton
import com.anish.veda.ui.components.PulseIndicator
import com.anish.veda.ui.components.TechBadge
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy
import com.anish.veda.ui.theme.VedaSurfaceNavy
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Top Bar: VEDA Branding & Fast Navigation Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VEDA",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPANION",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "ESP32-C3 PROTOCOL v1.0",
                    color = TextMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.navigate(Screen.Diagnostics.route) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VedaCardNavy)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { navController.navigate(Screen.GeneralSettings.route) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VedaCardNavy)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Centerpiece: Circular Status Gauge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularStatusGauge(
                connectionState = connectionState,
                watchName = watchDisplayName
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Last Synchronization Banner
        FuturisticCard(glowBorder = connectionState.isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VedaSurfaceNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (connectionState.isConnected) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Last Time Sync",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = TimeUtils.formatSyncTime(settings.lastSyncTimestamp),
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (connectionState.isConnected) {
                    TechBadge(
                        text = "RTC OK",
                        icon = Icons.Default.Refresh,
                        color = MatrixGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Forwarding Status Cards (Notifications & Calls)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Notification Forwarding Status Card
            FuturisticCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate(Screen.NotificationSettings.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (settings.forwardNotifications) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    PulseIndicator(
                        color = if (settings.forwardNotifications) MatrixGreen else AlertRed,
                        size = 8.dp,
                        isPulsing = settings.forwardNotifications && connectionState.isConnected
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Notifications",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (settings.forwardNotifications) "Active (${settings.notificationForwardedCount})" else "Paused",
                    color = if (settings.forwardNotifications) MatrixGreen else TextMuted,
                    fontSize = 12.sp
                )
            }

            // Call Forwarding Status Card
            FuturisticCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate(Screen.CallSettings.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = if (settings.forwardCalls) CyberBlue else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    PulseIndicator(
                        color = if (settings.forwardCalls) MatrixGreen else AlertRed,
                        size = 8.dp,
                        isPulsing = settings.forwardCalls && connectionState.isConnected
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Calls Alert",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (settings.forwardCalls) "Forwarding (${settings.callsForwardedCount})" else "Disabled",
                    color = if (settings.forwardCalls) MatrixGreen else TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bluetooth Permission Informational Card (When required permissions are missing)
        if (!hasBluetoothPermission) {
            FuturisticCard(
                glowBorder = true,
                borderColor = if (permissionDeniedNotice) AlertRed else NeonCyan
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VedaSurfaceNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (permissionDeniedNotice) AlertRed else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (permissionDeniedNotice) "BLUETOOTH ACCESS REQUIRED" else "NEARBY DEVICES PERMISSION",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (permissionDeniedNotice)
                                    "Permission was denied. Please allow Nearby Devices / Bluetooth access in Settings to connect to VEDA."
                                else
                                    "Android 15 requires Bluetooth permissions to scan, connect, and sync with your VEDA smartwatch.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlowingButton(
                            onClick = {
                                if (permissionDeniedNotice) {
                                    context.startActivity(PermissionUtils.createAppDetailsSettingsIntent(context))
                                } else {
                                    bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (permissionDeniedNotice) "OPEN SETTINGS" else "GRANT PERMISSION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        GlowingOutlinedButton(
                            onClick = { navController.navigate(Screen.PermissionsOnboarding.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "VIEW DETAILS",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Primary Connection & Sync Control Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (connectionState.isConnected) {
                // Disconnect Button
                DestructiveOutlinedButton(
                    onClick = { viewModel.disconnect() }
                ) {
                    Text(
                        text = "DISCONNECT WATCH",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Sync Time Button
                GlowingButton(
                    onClick = { viewModel.syncTime() },
                    enabled = !isSyncingTime
                ) {
                    if (isSyncingTime) {
                        CircularProgressIndicator(
                            color = VedaBlack,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSyncingTime) "SYNCING TIME..." else "SYNC TIME NOW",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Quick Test Notification Button
                GlowingOutlinedButton(
                    onClick = { viewModel.sendTestNotification() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEND TEST ALERT TO WATCH",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                // Connect Button
                GlowingButton(
                    onClick = {
                        if (!hasBluetoothPermission) {
                            bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                        } else {
                            viewModel.connect()
                        }
                    },
                    enabled = !connectionState.isBusy
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (connectionState.isBusy) "SEARCHING VEDA..." else "CONNECT VEDA WATCH",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Bluetooth Discovery & Scan Screen Shortcut
                GlowingOutlinedButton(
                    onClick = {
                        if (!hasBluetoothPermission) {
                            bluetoothPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                        } else {
                            navController.navigate(Screen.Bluetooth.route)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEARCH NEARBY DEVICES",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Settings Hub Button
            GlowingOutlinedButton(
                onClick = { navController.navigate(Screen.GeneralSettings.route) },
                borderColor = BorderNavy,
                contentColor = TextWhite
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS & HARDWARE INFO",
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Quick Action Grid
        Text(
            text = "QUICK CONFIGURATION",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickNavChip(
                icon = Icons.Default.Bluetooth,
                label = "Bluetooth",
                onClick = { navController.navigate(Screen.Bluetooth.route) },
                modifier = Modifier.weight(1f)
            )
            QuickNavChip(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                onClick = { navController.navigate(Screen.NotificationSettings.route) },
                modifier = Modifier.weight(1f)
            )
            QuickNavChip(
                icon = Icons.Default.Call,
                label = "Calls",
                onClick = { navController.navigate(Screen.CallSettings.route) },
                modifier = Modifier.weight(1f)
            )
            QuickNavChip(
                icon = Icons.Default.Shield,
                label = "Permissions",
                onClick = { navController.navigate(Screen.PermissionsOnboarding.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuickNavChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VedaCardNavy)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
