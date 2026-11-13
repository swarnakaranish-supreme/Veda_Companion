package com.anish.veda.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleProtocol
import kotlinx.coroutines.launch
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusGreen
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

/**
 * VEDA V2.1 Watch Remote Control Centre.
 * Live, verified GATT command dispatcher for physical ST7789, DRV2605L, and RTC features.
 */
@Composable
fun WatchRemoteControlScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as VedaApplication
    val bleManager = app.bleManager
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState is BleConnectionState.Connected

    // Local state for interactive controls
    var brightnessSlider by remember { mutableFloatStateOf(60f) }
    var selectedTimeoutSec by remember { mutableIntStateOf(10) }
    var dndActive by remember { mutableStateOf(false) }
    var alarmHour by remember { mutableIntStateOf(7) }
    var alarmMinute by remember { mutableIntStateOf(0) }
    var alarmActive by remember { mutableStateOf(false) }
    var isMeasuringHealth by remember { mutableStateOf(false) }
    var selectedFaceIndex by remember { mutableIntStateOf(0) }
    var lastCommandAck by remember { mutableStateOf("READY") }

    val watchFaceNames = listOf(
        "Minimal Digital",
        "Futuristic HUD",
        "Circular Technical",
        "Analog Inspired",
        "Health Dashboard",
        "Time & Date Minimal",
        "Data Focus",
        "VEDA Signature"
    )

    val timeoutOptions = listOf(5, 10, 20, 30, 60)

    Scaffold(
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Menu.route,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = CanvasDeepBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VedaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Watch Remote Control",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isConnected) "GATT Active • Ready for Dispatch" else "Offline • Connect watch to send commands",
                        style = Typography.bodySmall,
                        color = if (isConnected) StatusGreen else StatusWarning
                    )
                }
            }

            // SECTION 1: WATCH FACE SELECTOR
            VedaCard {
                VedaCardHeader(
                    label = "Active Watch Face (Local ST7789)",
                    statusText = watchFaceNames[selectedFaceIndex],
                    statusColor = AccentCyan
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    watchFaceNames.forEachIndexed { index, name ->
                        val isSelected = index == selectedFaceIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                                .clickable {
                                    selectedFaceIndex = index
                                    if (isConnected) {
                                        coroutineScope.launch {
                                            bleManager.sendPayloadDirect(BleProtocol.buildSelectFacePacket(index))
                                        }
                                        lastCommandAck = "FACE $index DISPATCHED"
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. $name",
                                style = Typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) AccentCyan else TextPrimary
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 2: BRIGHTNESS & DISPLAY TIMEOUT
            VedaCard {
                VedaCardHeader(
                    label = "Display & Backlight Control",
                    statusText = "${brightnessSlider.toInt()}%",
                    statusColor = AccentCyan
                )

                Text(
                    text = "Display Backlight PWM",
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = brightnessSlider,
                    onValueChange = { brightnessSlider = it },
                    onValueChangeFinished = {
                        if (isConnected) {
                            coroutineScope.launch {
                                bleManager.sendPayloadDirect(BleProtocol.buildBrightnessPacket(brightnessSlider.toInt()))
                            }
                            lastCommandAck = "BRIGHTNESS ${brightnessSlider.toInt()}% SENT"
                        }
                    },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = SurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Screen Timeout (Seconds)",
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeoutOptions.forEach { sec ->
                        val isSelected = sec == selectedTimeoutSec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentCyan else StrokeSubtle,
                                    shape = RoundedCornerShape(VedaRadii.sm)
                                )
                                .clickable {
                                    selectedTimeoutSec = sec
                                    if (isConnected) {
                                        coroutineScope.launch {
                                            bleManager.sendPayloadDirect(BleProtocol.buildTimeoutPacket(sec))
                                        }
                                        lastCommandAck = "TIMEOUT ${sec}s SENT"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s",
                                style = Typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) AccentCyan else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 3: DO NOT DISTURB & POWER SAVING
            VedaCard {
                VedaCardHeader(
                    label = "System Modes",
                    statusText = if (dndActive) "DND ENGAGED" else "NORMAL",
                    statusColor = if (dndActive) StatusWarning else StatusGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Do Not Disturb (DND)", style = Typography.titleMedium.copy(fontSize = 14.sp), color = TextPrimary)
                        Text(text = "Silence vibration and prevent notification wakeups on watch", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = dndActive,
                        onCheckedChange = { checked ->
                            dndActive = checked
                            if (isConnected) {
                                coroutineScope.launch {
                                    bleManager.sendPayloadDirect(BleProtocol.buildDndPacket(checked))
                                }
                                lastCommandAck = if (checked) "DND ACTIVATED" else "DND MUTED"
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = StatusWarning, checkedTrackColor = StatusWarning.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 4: HARDWARE RTC ALARM CONTROL
            VedaCard {
                VedaCardHeader(
                    label = "Watch Hardware RTC Alarm",
                    statusText = if (alarmActive) String.format("%02d:%02d", alarmHour, alarmMinute) else "DISABLED",
                    statusColor = if (alarmActive) AccentCyan else TextTertiary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format("%02d:%02d", alarmHour, alarmMinute),
                            style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Hardware alarm wakes ESP32 from sleep and triggers DRV2605L",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = alarmActive,
                        onCheckedChange = { checked ->
                            alarmActive = checked
                            if (isConnected) {
                                coroutineScope.launch {
                                    if (checked) {
                                        bleManager.sendPayloadDirect(BleProtocol.buildSetAlarmPacket(alarmHour, alarmMinute, true))
                                    } else {
                                        bleManager.sendPayloadDirect(BleProtocol.buildDeleteAlarmPacket())
                                    }
                                }
                                lastCommandAck = if (checked) "ALARM SET FOR $alarmHour:$alarmMinute" else "ALARM DELETED"
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        VedaSecondaryButton(
                            text = "+1 Hour",
                            onClick = {
                                alarmHour = (alarmHour + 1) % 24
                                if (alarmActive && isConnected) {
                                    coroutineScope.launch {
                                        bleManager.sendPayloadDirect(BleProtocol.buildSetAlarmPacket(alarmHour, alarmMinute, true))
                                    }
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VedaSecondaryButton(
                            text = "+10 Min",
                            onClick = {
                                alarmMinute = (alarmMinute + 10) % 60
                                if (alarmActive && isConnected) {
                                    coroutineScope.launch {
                                        bleManager.sendPayloadDirect(BleProtocol.buildSetAlarmPacket(alarmHour, alarmMinute, true))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 5: HEALTH & HAPTIC TESTING
            VedaCard {
                VedaCardHeader(
                    label = "Sensors & Haptics Dispatch",
                    statusText = lastCommandAck,
                    statusColor = StatusGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        VedaPrimaryButton(
                            text = if (isMeasuringHealth) "Stop Health" else "Start Health",
                            onClick = {
                                isMeasuringHealth = !isMeasuringHealth
                                val cmd = if (isMeasuringHealth) BleProtocol.CMD_START_MEASUREMENT else BleProtocol.CMD_STOP_MEASUREMENT
                                coroutineScope.launch {
                                    bleManager.sendPayloadDirect(cmd)
                                }
                                lastCommandAck = if (isMeasuringHealth) "MAX30102 STARTED" else "MAX30102 STOPPED"
                            },
                            icon = Icons.Default.Favorite
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VedaSecondaryButton(
                            text = "Test Haptic",
                            onClick = {
                                coroutineScope.launch {
                                    bleManager.sendPayloadDirect(BleProtocol.buildHapticPlayPacket(14)) // Strong click
                                }
                                lastCommandAck = "HAPTIC #14 SENT"
                            },
                            icon = Icons.Default.Vibration
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                VedaSecondaryButton(
                    text = "Sync Clock & Date to RTC",
                    onClick = {
                        coroutineScope.launch {
                            bleManager.sendPayloadDirect(BleProtocol.buildTimePacket())
                        }
                        lastCommandAck = "RTC TIME SYNCED"
                    },
                    icon = Icons.Default.Sync
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.lg))
        }
    }
}
