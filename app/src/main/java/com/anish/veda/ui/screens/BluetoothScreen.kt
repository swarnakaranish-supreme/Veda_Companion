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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleDeviceModel
import com.anish.veda.ui.components.CyberHeader
import com.anish.veda.ui.components.DestructiveOutlinedButton
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.GlowingOutlinedButton
import com.anish.veda.ui.components.PulseIndicator
import com.anish.veda.ui.components.TechBadge
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextDim
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy
import com.anish.veda.ui.theme.VedaSurfaceNavy
import com.anish.veda.ui.theme.WarningAmber
import com.anish.veda.utils.Constants
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.viewmodel.BluetoothViewModel

@Composable
fun BluetoothScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()

    var hasBluetoothPermission by remember {
        mutableStateOf(PermissionUtils.hasBluetoothPermissions(context))
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasBluetoothPermission = granted || PermissionUtils.hasBluetoothPermissions(context)
        if (hasBluetoothPermission && isBluetoothEnabled && !connectionState.isConnected && !isScanning) {
            viewModel.startScan()
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

    LaunchedEffect(hasBluetoothPermission, isBluetoothEnabled) {
        if (hasBluetoothPermission && isBluetoothEnabled && !connectionState.isConnected && !isScanning) {
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "Bluetooth Radar",
            subtitle = "Target UUID: ${Constants.SERVICE_UUID_STRING.substring(0, 8)}...",
            onBackClick = { navController.popBackStack() },
            actions = {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasBluetoothPermission) {
                item {
                    FuturisticCard(
                        glowBorder = true,
                        borderColor = NeonCyan
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
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bluetooth Permission Required",
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Android 15 requires Nearby Devices permission to scan and connect with your VEDA smartwatch.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            GlowingButton(
                                onClick = {
                                    btPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                                }
                            ) {
                                Text(
                                    text = "AUTHORIZE BLUETOOTH",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!isBluetoothEnabled) {
                item {
                    FuturisticCard(
                        glowBorder = true,
                        borderColor = AlertRed
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bluetooth is Turned Off",
                                    color = AlertRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enable Bluetooth in phone settings to scan and connect to VEDA.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Live Connection Status Card
            item {
                FuturisticCard(glowBorder = connectionState.isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GATT Connection",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            val statusLabel = when (val state = connectionState) {
                                is BleConnectionState.Connected -> "Connected to ${state.deviceName}"
                                is BleConnectionState.Connecting -> "Connecting to ${state.deviceName ?: "VEDA"}..."
                                is BleConnectionState.ConnectedGatt -> "GATT Channel Open (${state.deviceName ?: "VEDA"})"
                                is BleConnectionState.NegotiatingMtu -> "Negotiating MTU (512 bytes)..."
                                is BleConnectionState.DiscoveringServices -> "Discovering GATT Characteristics..."
                                is BleConnectionState.Reconnecting -> "Reconnecting (Attempt ${state.attempt}/${state.maxAttempts})..."
                                is BleConnectionState.Scanning -> "Scanning Nearby BLE..."
                                is BleConnectionState.Disconnecting -> "Disconnecting..."
                                is BleConnectionState.Disconnected -> state.reason?.let { "Disconnected: $it" } ?: "Disconnected"
                                is BleConnectionState.Error -> "Error: ${state.message}"
                            }
                            Text(
                                text = statusLabel,
                                color = if (connectionState.isConnected) MatrixGreen else TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        PulseIndicator(
                            color = if (connectionState.isConnected) MatrixGreen else if (connectionState.isBusy) WarningAmber else AlertRed,
                            isPulsing = connectionState.isBusy || connectionState.isConnected
                        )
                    }

                    if (connectionState is BleConnectionState.Connected) {
                        val connected = connectionState as BleConnectionState.Connected
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MAC: ${connected.deviceAddress}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "MTU: ${connected.mtu}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Scan Action Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isScanning) {
                        GlowingOutlinedButton(
                            onClick = { viewModel.stopScan() },
                            modifier = Modifier.weight(1f),
                            borderColor = AlertRed,
                            contentColor = AlertRed
                        ) {
                            Text(text = "STOP SCAN")
                        }
                    } else {
                        GlowingButton(
                            onClick = { viewModel.startScan() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "SCAN FOR VEDA")
                        }
                    }

                    if (connectionState.isConnected) {
                        DestructiveOutlinedButton(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "DISCONNECT")
                        }
                    }
                }
            }

            // Scanned Devices Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVERED PERIPHERALS (${scannedDevices.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (isScanning) {
                        Text(
                            text = "Searching...",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (scannedDevices.isEmpty()) {
                item {
                    FuturisticCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = BorderNavy,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isScanning) "Searching for ESP32-C3 'ANISH-WATCH'..." else "No BLE devices found yet",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Ensure watch Bluetooth is ON in the watch menu",
                                    color = TextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(scannedDevices, key = { it.address }) { device ->
                    DeviceListItem(
                        device = device,
                        isCurrentConnected = (connectionState as? BleConnectionState.Connected)?.deviceAddress == device.address,
                        onConnectClick = {
                            if (!hasBluetoothPermission) {
                                btPermissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
                            } else {
                                viewModel.connectDevice(device)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BleDeviceModel,
    isCurrentConnected: Boolean,
    onConnectClick: () -> Unit
) {
    FuturisticCard(
        glowBorder = device.isVedaWatch || isCurrentConnected,
        borderColor = if (isCurrentConnected) MatrixGreen else if (device.isVedaWatch) NeonCyan else BorderNavy,
        modifier = Modifier.clickable { onConnectClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (device.isVedaWatch) NeonCyan.copy(alpha = 0.15f) else VedaSurfaceNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (device.isVedaWatch) Icons.Default.Watch else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isVedaWatch) NeonCyan else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.displayName,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (device.isVedaWatch) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TechBadge(text = "VEDA C3", color = NeonCyan)
                        }
                    }
                    Text(
                        text = device.address,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    color = if (device.rssi > -65) MatrixGreen else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isCurrentConnected) {
                    TechBadge(
                        text = "LINKED",
                        icon = Icons.Default.CheckCircle,
                        color = MatrixGreen
                    )
                } else {
                    Text(
                        text = "TAP TO PAIR",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
