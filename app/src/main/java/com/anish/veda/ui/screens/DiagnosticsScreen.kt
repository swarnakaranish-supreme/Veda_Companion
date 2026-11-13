package com.anish.veda.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.LogEntry
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
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.DiagnosticsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    navController: NavController,
    viewModel: DiagnosticsViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val logs by viewModel.logs.collectAsState()
    val errorLogs by viewModel.errorLogs.collectAsState()
    val customPayload by viewModel.customPayload.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isSendingTest by viewModel.isSendingTest.collectAsState()

    val telemetry = viewModel.getTelemetrySnapshot()
    var selectedLogTab by remember { mutableStateOf(0) } // 0 = All Logs, 1 = Error Log

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "Diagnostics & Terminal",
                statusText = telemetry.gattState,
                statusColor = if (connectionState is BleConnectionState.Connected) StatusSuccess else AccentCyan,
                pulsing = connectionState is BleConnectionState.Connected
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Diagnostics.route,
                onNavigate = { route ->
                    if (route != Screen.Diagnostics.route) {
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
            // TELEMETRY SNAPSHOT CARD
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Live Hardware & GATT Telemetry",
                    statusText = telemetry.gattState,
                    statusColor = if (connectionState is BleConnectionState.Connected) StatusSuccess else AccentCyan
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .padding(VedaSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagRow("CONNECTED DEVICE", "${telemetry.connectedDeviceName} (${telemetry.connectedDeviceAddress})")
                    DiagRow("GATT SERVICE", if (telemetry.isServiceDiscovered) "DISCOVERED (READY)" else "NOT FOUND")
                    DiagRow("WRITE CHARACTERISTIC", if (telemetry.isCharacteristicDiscovered) "WRITABLE + NOTIFY" else "NOT FOUND")
                    DiagRow("ATT MTU PAYLOAD", "${telemetry.negotiatedMtu} bytes")
                    DiagRow("LAST WRITE PAYLOAD", telemetry.lastSuccessfulWrite)
                    DiagRow("LAST FORWARDED ALERT", telemetry.lastNotification)
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VedaSecondaryButton(
                        text = "COPY LOGS",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("VEDA Logs", viewModel.getExportText()))
                            coroutineScope.launch { snackbarHostState.showSnackbar("Logs copied to clipboard.") }
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ContentCopy,
                        height = 38.dp
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.sm))
                    VedaSecondaryButton(
                        text = "CLEAR TERMINAL",
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ClearAll,
                        height = 38.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // PROTOCOL INJECTOR & TEST CENTER
            // ================================================================
            VedaCard {
                VedaCardHeader(
                    label = "Protocol Injector & Validation",
                    statusText = if (isSendingTest) "TRANSMITTING..." else "READY",
                    statusColor = if (isSendingTest) AccentCyan else StatusSuccess
                )

                Text(
                    text = "Inject real GATT command packets directly into the serialized Bluetooth transmission queue:",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(VedaSpacing.sm))

                // Custom Payload TextField
                OutlinedTextField(
                    value = customPayload,
                    onValueChange = { viewModel.updateCustomPayload(it) },
                    label = { Text("Raw Protocol Payload") },
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

                VedaPrimaryButton(
                    text = "TRANSMIT RAW PACKET",
                    onClick = { viewModel.sendPayload() },
                    icon = Icons.AutoMirrored.Filled.Send,
                    enabled = connectionState is BleConnectionState.Connected && !isSendingTest,
                    loading = isSendingTest,
                    height = 42.dp
                )

                Spacer(modifier = Modifier.height(VedaSpacing.md))
                Text(
                    text = "TEST PAYLOAD PRESETS",
                    style = Typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(VedaSpacing.xs))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VedaSecondaryButton(
                        text = "NOTIFICATION",
                        onClick = { viewModel.sendTestNotification() },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Notifications,
                        enabled = connectionState is BleConnectionState.Connected,
                        height = 36.dp
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.xs))
                    VedaSecondaryButton(
                        text = "CALL ALERT",
                        onClick = { viewModel.sendTestIncomingCall() },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Phone,
                        enabled = connectionState is BleConnectionState.Connected,
                        height = 36.dp
                    )
                }

                Spacer(modifier = Modifier.height(VedaSpacing.xs))

                Row(modifier = Modifier.fillMaxWidth()) {
                    VedaSecondaryButton(
                        text = "MISSED CALL",
                        onClick = { viewModel.sendTestMissedCall() },
                        modifier = Modifier.weight(1f),
                        enabled = connectionState is BleConnectionState.Connected,
                        height = 36.dp
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.xs))
                    VedaSecondaryButton(
                        text = "SYNC TIME (T|...)",
                        onClick = { viewModel.sendTimeSync() },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Sync,
                        enabled = connectionState is BleConnectionState.Connected,
                        height = 36.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // ================================================================
            // LIVE PACKET LOG TERMINAL
            // ================================================================
            VedaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PROTOCOL TERMINAL",
                        style = Typography.labelSmall,
                        color = TextTertiary
                    )
                    Row {
                        TabPill(
                            label = "ALL (${logs.size})",
                            isSelected = selectedLogTab == 0,
                            onClick = { selectedLogTab = 0 }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TabPill(
                            label = "ERRORS (${errorLogs.size})",
                            isSelected = selectedLogTab == 1,
                            onClick = { selectedLogTab = 1 }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.sm))

                val displayedLogs = if (selectedLogTab == 0) logs else errorLogs

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(CanvasMidnightNavy)
                        .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                        .padding(VedaSpacing.sm)
                ) {
                    if (displayedLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No telemetry logs recorded.",
                                style = Typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(displayedLogs.take(100)) { entry ->
                                LogRow(entry)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.xl))
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextTertiary)
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

@Composable
private fun TabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(VedaRadii.pill))
            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceElevated)
            .border(1.dp, if (isSelected) AccentCyan else StrokeSubtle, RoundedCornerShape(VedaRadii.pill))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = if (isSelected) AccentCyan else TextTertiary
        )
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val dirColor = when (entry.direction) {
        LogDirection.OUTBOUND -> AccentCyan
        LogDirection.INBOUND -> StatusSuccess
        LogDirection.ERROR -> StatusError
        LogDirection.SYSTEM -> AccentViolet
    }

    val dirSymbol = when (entry.direction) {
        LogDirection.OUTBOUND -> "▲ OUT"
        LogDirection.INBOUND -> "▼ IN "
        LogDirection.ERROR -> "✖ ERR"
        LogDirection.SYSTEM -> "◆ SYS"
    }

    val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeStr,
                style = Typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = TextTertiary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = dirSymbol,
                style = Typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = dirColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "[${entry.tag}]",
                style = Typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = TextSecondary
            )
        }
        Text(
            text = entry.message,
            style = Typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = TextPrimary
        )
        if (entry.rawPayload != null) {
            Text(
                text = "PAYLOAD: ${entry.rawPayload}",
                style = Typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = dirColor.copy(alpha = 0.8f)
            )
        }
    }
}
