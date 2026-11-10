package com.anish.veda.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleConstants
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.LogEntry
import com.anish.veda.ui.components.CyberHeader
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.GlowingOutlinedButton
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
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.DiagnosticsViewModel

@Composable
fun DiagnosticsScreen(
    navController: NavController,
    viewModel: DiagnosticsViewModel
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val errorLogs by viewModel.errorLogs.collectAsState()
    val customPayload by viewModel.customPayload.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isSendingTest by viewModel.isSendingTest.collectAsState()

    val telemetry = viewModel.getTelemetrySnapshot()
    var selectedLogTab by remember { mutableStateOf(0) } // 0 = All Logs, 1 = Error Log

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "Diagnostics & Terminal",
            subtitle = "Live Protocol Telemetry & Packet Sniffer",
            onBackClick = { navController.popBackStack() },
            actions = {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("VEDA Diagnostics", viewModel.getExportText())
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. LIVE HARDWARE & PROTOCOL TELEMETRY CARD
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HARDWARE & GATT TELEMETRY",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    TechBadge(
                        text = if (connectionState.isConnected) "LINK ONLINE" else "LINK IDLE",
                        color = if (connectionState.isConnected) MatrixGreen else AlertRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DiagRow(
                    label = "Bluetooth Status",
                    value = if (telemetry.bluetoothEnabled) "Enabled (Hardware OK)" else "Disabled on Phone",
                    valueColor = if (telemetry.bluetoothEnabled) MatrixGreen else AlertRed
                )

                DiagRow(
                    label = "BLE Scan Status",
                    value = if (telemetry.isScanning) "Active Scanning..." else "Idle (Standby)",
                    valueColor = if (telemetry.isScanning) NeonCyan else TextMuted
                )

                DiagRow(
                    label = "Connected Device",
                    value = "${telemetry.connectedDeviceName} [${telemetry.connectedDeviceAddress}]",
                    valueColor = if (connectionState.isConnected) TextWhite else TextMuted
                )

                DiagRow(
                    label = "GATT State",
                    value = telemetry.gattState,
                    valueColor = if (connectionState.isConnected) MatrixGreen else WarningAmber
                )

                DiagRow(
                    label = "Service Discovery",
                    value = if (telemetry.isServiceDiscovered) "Discovered (${BleConstants.SERVICE_UUID_STRING})" else "Pending / Unlinked",
                    valueColor = if (telemetry.isServiceDiscovered) MatrixGreen else TextMuted
                )

                DiagRow(
                    label = "Characteristic Discovery",
                    value = if (telemetry.isCharacteristicDiscovered) "Discovered Writable (${BleConstants.WRITE_CHARACTERISTIC_UUID_STRING})" else "Pending / Unlinked",
                    valueColor = if (telemetry.isCharacteristicDiscovered) MatrixGreen else TextMuted
                )

                DiagRow(
                    label = "Negotiated MTU",
                    value = "${telemetry.negotiatedMtu} bytes (${telemetry.negotiatedMtu - 3} bytes ATT payload)",
                    valueColor = if (telemetry.negotiatedMtu > 23) NeonCyan else TextMuted
                )

                DiagRow(
                    label = "Last Successful Write",
                    value = if (telemetry.lastSuccessfulWriteTime > 0) "${telemetry.lastSuccessfulWrite} (${TimeUtils.formatSyncTime(telemetry.lastSuccessfulWriteTime)})" else "None yet",
                    valueColor = if (telemetry.lastSuccessfulWriteTime > 0) TextWhite else TextMuted
                )

                DiagRow(
                    label = "Last Notification",
                    value = if (telemetry.lastNotificationTime > 0) "${telemetry.lastNotification} (${TimeUtils.formatSyncTime(telemetry.lastNotificationTime)})" else "None yet",
                    valueColor = if (telemetry.lastNotificationTime > 0) TextWhite else TextMuted
                )

                DiagRow(
                    label = "Last Call Event",
                    value = if (telemetry.lastCallEventTime > 0) "${telemetry.lastCallEvent} (${TimeUtils.formatSyncTime(telemetry.lastCallEventTime)})" else "No call events detected",
                    valueColor = if (telemetry.lastCallEventTime > 0) CyberBlue else TextMuted
                )
            }

            // 2. PROTOCOL TEST BUTTONS (Sends real packets to watch!)
            FuturisticCard {
                Text(
                    text = "REAL PROTOCOL TEST TRANSMITTERS",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "These buttons stream actual formatted UTF-8 packets to the ESP32-C3 watch writable characteristic",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test Button 1: Send Test Notification
                GlowingButton(
                    onClick = { viewModel.sendTestNotification() },
                    enabled = connectionState.isConnected && !isSendingTest
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "1. SEND TEST NOTIFICATION (N|0|...)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Button 2: Send Test Incoming Call
                GlowingButton(
                    onClick = { viewModel.sendTestIncomingCall() },
                    enabled = connectionState.isConnected && !isSendingTest,
                    containerColor = CyberBlue
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "2. SEND TEST INCOMING CALL (N|1|...)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Button 3: Send Test Missed Call
                GlowingOutlinedButton(
                    onClick = { viewModel.sendTestMissedCall() },
                    enabled = connectionState.isConnected && !isSendingTest,
                    borderColor = AlertRed,
                    contentColor = AlertRed
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.CallMissed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "3. SEND TEST MISSED CALL (N|2|...)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Button 4: Sync Time
                GlowingOutlinedButton(
                    onClick = { viewModel.sendTimeSync() },
                    enabled = connectionState.isConnected && !isSendingTest
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "4. SYNC TIME (T|YEAR|MONTH|...)")
                }

                if (!connectionState.isConnected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notice: Connect watch to transmit real protocol messages",
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }

            // 3. RAW GATT PACKET TRANSMITTER
            FuturisticCard {
                Text(
                    text = "CUSTOM RAW GATT TRANSMITTER",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customPayload,
                        onValueChange = { viewModel.updateCustomPayload(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(text = "T|2026|... or N|cat|app|title|msg", color = TextDim, fontSize = 12.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    GlowingButton(
                        onClick = { viewModel.sendPayload() },
                        enabled = connectionState.isConnected,
                        modifier = Modifier.width(80.dp),
                        height = 54.dp
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 4. TELEMETRY LOGS & ERROR LOG TABS
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLogTab == 0) NeonCyan else VedaSurfaceNavy)
                                .clickable { selectedLogTab = 0 }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ALL LOGS (${logs.size})",
                                color = if (selectedLogTab == 0) VedaBlack else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLogTab == 1) AlertRed else VedaSurfaceNavy)
                                .clickable { selectedLogTab = 1 }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ERRORS (${errorLogs.size})",
                                color = if (selectedLogTab == 1) TextWhite else AlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "AUTO-SCROLL",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayedLogs = if (selectedLogTab == 0) logs else errorLogs

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VedaBlack.copy(alpha = 0.8f))
                        .padding(8.dp)
                ) {
                    if (displayedLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (selectedLogTab == 0) "No telemetry recorded yet" else "Zero errors recorded. All systems nominal.",
                                color = if (selectedLogTab == 0) TextMuted else MatrixGreen,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            reverseLayout = true
                        ) {
                            items(displayedLogs.reversed()) { entry ->
                                LogItemRow(entry = entry)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            maxLines = 2
        )
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    val dirColor = when (entry.direction) {
        LogDirection.INBOUND -> CyberBlue
        LogDirection.OUTBOUND -> NeonCyan
        LogDirection.SYSTEM -> MatrixGreen
        LogDirection.ERROR -> AlertRed
    }

    val dirIcon = when (entry.direction) {
        LogDirection.INBOUND -> Icons.Default.ArrowDownward
        LogDirection.OUTBOUND -> Icons.Default.ArrowUpward
        LogDirection.SYSTEM -> Icons.Default.Info
        LogDirection.ERROR -> Icons.Default.Error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = dirIcon,
            contentDescription = null,
            tint = dirColor,
            modifier = Modifier
                .size(12.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = TimeUtils.formatDetailedTimestamp(entry.timestamp),
            color = TextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "[${entry.tag}]",
            color = dirColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.message,
            color = TextWhite,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
