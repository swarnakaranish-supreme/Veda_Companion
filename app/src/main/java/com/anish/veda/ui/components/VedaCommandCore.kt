package com.anish.veda.ui.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasPrimary
import com.anish.veda.ui.theme.CanvasSecondary
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusGreen
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
import com.anish.veda.utils.TimeUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 6 Interactive Dial Modes for VEDA Command Core
 */
enum class CommandCoreMode(val label: String, val code: String) {
    NEURAL("NEURAL CONNECTION", "LNK"),
    HEALTH("HEALTH PULSE", "MAX"),
    ENERGY("ENERGY HUB", "PWR"),
    CHRONOS("CHRONOS RTC", "CLK"),
    AI("NEURAL ENGINE", "AI"),
    COMMAND("COMMAND RADIAL", "CMD")
}

/**
 * VEDA V2.1 Signature Feature: Multi-Layered Interactive Command Core.
 * Combines high-precision technical telemetry with direct wearable control.
 */
@Composable
fun VedaCommandCore(
    connectionState: BleConnectionState,
    watchName: String,
    modifier: Modifier = Modifier,
    initialMode: Int = 0,
    heartRate: Int? = null,
    spo2: Int? = null,
    sensorQuality: Int = 0,
    isMeasuringHealth: Boolean = false,
    rssi: Int? = null,
    lastSyncTimestamp: Long = 0L,
    isAiProcessing: Boolean = false,
    reducedMotion: Boolean = false,
    onModeChanged: ((CommandCoreMode) -> Unit)? = null,
    onCenterClick: (CommandCoreMode) -> Unit,
    onDoubleTap: () -> Unit,
    onRadialAction: (String) -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember {
        mutableStateOf(CommandCoreMode.entries.getOrElse(initialMode) { CommandCoreMode.NEURAL })
    }
    var radialExpanded by remember { mutableStateOf(false) }

    // Read phone battery via Android BatteryManager
    var phoneBatteryPercent by remember { mutableIntStateOf(100) }
    var isPhoneCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, iFilter)
        batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            phoneBatteryPercent = if (level >= 0 && scale > 0) ((level.toFloat() / scale) * 100).toInt() else 100
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isPhoneCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    // Chronos local time state
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFmt.format(now)
            currentDateString = dateFmt.format(now)
            delay(1000)
        }
    }

    // Animation transition
    val transition = rememberInfiniteTransition(label = "core_anim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 24000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_rotation"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reducedMotion) 3000 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Dynamic accent color based on mode and connection
    val modeAccent = when (currentMode) {
        CommandCoreMode.NEURAL -> when (connectionState) {
            is BleConnectionState.Connected -> StatusGreen
            is BleConnectionState.Error -> StatusError
            is BleConnectionState.Disconnected -> TextTertiary
            else -> AccentCyan
        }
        CommandCoreMode.HEALTH -> if (isMeasuringHealth) StatusGreen else AccentCyan
        CommandCoreMode.ENERGY -> StatusWarning
        CommandCoreMode.CHRONOS -> AccentCyan
        CommandCoreMode.AI -> AccentViolet
        CommandCoreMode.COMMAND -> AccentCyan
    }

    // Gesture tracking for horizontal swipe
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(280.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (totalDragX < -60f) {
                            // Swipe Left -> Next Mode
                            val nextIdx = (currentMode.ordinal + 1) % CommandCoreMode.entries.size
                            currentMode = CommandCoreMode.entries[nextIdx]
                            onModeChanged?.invoke(currentMode)
                        } else if (totalDragX > 60f) {
                            // Swipe Right -> Prev Mode
                            val prevIdx = if (currentMode.ordinal - 1 < 0) CommandCoreMode.entries.size - 1 else currentMode.ordinal - 1
                            currentMode = CommandCoreMode.entries[prevIdx]
                            onModeChanged?.invoke(currentMode)
                        }
                        totalDragX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onCenterClick(currentMode)
                    },
                    onDoubleTap = {
                        onDoubleTap()
                    },
                    onLongPress = {
                        radialExpanded = !radialExpanded
                        if (radialExpanded) {
                            currentMode = CommandCoreMode.COMMAND
                            onModeChanged?.invoke(currentMode)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Multi-Layer Outer Telemetry Ring Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 14.dp.toPx()
            val middleRadius = outerRadius - 16.dp.toPx()
            val innerRadius = middleRadius - 8.dp.toPx()

            // Outer dark bezel backdrop
            drawCircle(
                color = CanvasSecondary,
                radius = outerRadius + 8.dp.toPx(),
                center = center
            )
            drawCircle(
                color = StrokeSubtle,
                radius = outerRadius + 8.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 36-tick outer precision ring
            val tickCount = 36
            val tickLength = 5.dp.toPx()
            for (i in 0 until tickCount) {
                val angleDeg = (i * (360f / tickCount))
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 6 == 0
                val length = if (isMajor) tickLength * 1.6f else tickLength
                val tickColor = if (isMajor) modeAccent.copy(alpha = 0.8f) else StrokeFocused.copy(alpha = 0.5f)

                val startX = center.x + (outerRadius - length) * cos(angleRad).toFloat()
                val startY = center.y + (outerRadius - length) * sin(angleRad).toFloat()
                val endX = center.x + outerRadius * cos(angleRad).toFloat()
                val endY = center.y + outerRadius * sin(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // Mode arc indicator (shows current active quadrant)
            val sweepPerMode = 360f / CommandCoreMode.entries.size
            val startAngle = currentMode.ordinal * sweepPerMode - 90f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(modeAccent.copy(alpha = 0.2f), modeAccent, modeAccent.copy(alpha = 0.2f))
                ),
                startAngle = if (reducedMotion) startAngle else startAngle + (rotation * 0.15f) % 360f,
                sweepAngle = sweepPerMode - 10f,
                useCenter = false,
                topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                size = androidx.compose.ui.geometry.Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner dark graphite core
            drawCircle(
                color = SurfaceGraphite,
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = StrokeFocused,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Mode Indicator Pips at the top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CommandCoreMode.entries.forEach { mode ->
                val isSelected = mode == currentMode
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) modeAccent else StrokeFocused)
                        .clickable {
                            currentMode = mode
                            onModeChanged?.invoke(mode)
                        }
                )
            }
        }

        // Center Display Content per Mode
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .padding(8.dp)
        ) {
            when (currentMode) {
                CommandCoreMode.NEURAL -> {
                    // MODE 1: NEURAL CONNECTION
                    val isConnected = connectionState is BleConnectionState.Connected
                    val statusText = when (connectionState) {
                        is BleConnectionState.Connected -> "CONNECTED"
                        is BleConnectionState.Connecting -> "CONNECTING"
                        is BleConnectionState.Reconnecting -> "RECONNECT"
                        is BleConnectionState.Error -> "LINK FAULT"
                        else -> "STANDBY"
                    }
                    Text(
                        text = watchName,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(modeAccent)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = modeAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConnected && rssi != null && rssi != 0) "$rssi dBm" else "GATT STABLE",
                        style = Typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lastSyncTimestamp > 0) "Synced: ${TimeUtils.formatSyncTime(lastSyncTimestamp)}" else "Tap to Sync",
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary
                    )
                }

                CommandCoreMode.HEALTH -> {
                    // MODE 2: HEALTH PULSE
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (isMeasuringHealth && !reducedMotion) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (heartRate != null && heartRate > 0) "$heartRate" else "--",
                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "BPM • SpO2: ${if (spo2 != null && spo2 > 0) "$spo2%" else "--"}",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMeasuringHealth) "SAMPLING ($sensorQuality%)" else "TAP TO MEASURE",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, color = if (isMeasuringHealth) StatusGreen else TextTertiary)
                    )
                }

                CommandCoreMode.ENERGY -> {
                    // MODE 3: ENERGY
                    Icon(
                        imageVector = if (isPhoneCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$phoneBatteryPercent%",
                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isPhoneCharging) "PHONE CHARGING" else "PHONE BATTERY",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "WATCH: HARDWARE N/A",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = TextTertiary
                    )
                }

                CommandCoreMode.CHRONOS -> {
                    // MODE 4: CHRONOS RTC
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentTimeString.isNotEmpty()) currentTimeString else "12:00:00",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentDateString.isNotEmpty()) currentDateString else "VEDA RTC",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TAP: SYNC CLOCK",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, color = AccentCyan)
                    )
                }

                CommandCoreMode.AI -> {
                    // MODE 5: VEDA NEURAL AI
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentViolet,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (isAiProcessing && !reducedMotion) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "VEDA AI",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isAiProcessing) "PROCESSING..." else "GEMINI ACTIVE",
                        style = Typography.labelSmall.copy(fontSize = 10.sp, color = AccentViolet)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TAP FOR BRIEFING",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, color = TextTertiary)
                    )
                }

                CommandCoreMode.COMMAND -> {
                    // MODE 6: COMMAND RADIAL
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "COMMAND",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (radialExpanded) "EXPANDED" else "HOLD TO EXPAND",
                        style = Typography.labelSmall.copy(fontSize = 10.sp, color = AccentCyan)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "8 RADIAL ACTIONS",
                        style = Typography.labelSmall.copy(fontSize = 9.sp, color = TextTertiary)
                    )
                }
            }
        }

        // Expandable 8-Radial Action Overlay
        AnimatedVisibility(
            visible = radialExpanded,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            RadialActionMenu(
                onActionSelected = { action ->
                    radialExpanded = false
                    onRadialAction(action)
                },
                onDismiss = { radialExpanded = false }
            )
        }
    }
}

/**
 * 8-Action Radial Menu for Command Mode
 */
@Composable
private fun RadialActionMenu(
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = listOf(
        Triple("WATCH_FACE", Icons.Default.Watch, "Face"),
        Triple("BRIGHTNESS", Icons.Default.BrightnessMedium, "Bright"),
        Triple("DND", Icons.Default.DoNotDisturbOn, "DND"),
        Triple("HEALTH", Icons.Default.Favorite, "Measure"),
        Triple("HAPTIC", Icons.Default.Vibration, "Haptic"),
        Triple("SYNC", Icons.Default.Sync, "Sync"),
        Triple("POWER", Icons.Default.PowerSettingsNew, "Power"),
        Triple("DIAGNOSTICS", Icons.Default.Security, "Diag")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasPrimary.copy(alpha = 0.88f), CircleShape)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        val radius = 104.dp
        actions.forEachIndexed { index, (key, icon, label) ->
            val angleDeg = (index * (360.0 / actions.size)) - 90.0
            val angleRad = angleDeg * (PI / 180.0)
            val offsetX = (radius.value * cos(angleRad)).roundToInt()
            val offsetY = (radius.value * sin(angleRad)).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.dp.roundToPx(), offsetY.dp.roundToPx()) }
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .clickable { onActionSelected(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center Dismiss pill
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onDismiss() }
        ) {
            Text(
                text = "CLOSE",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
        }
    }
}
