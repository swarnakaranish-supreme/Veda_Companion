package com.anish.veda.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.ui.theme.AccentCyan
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

/**
 * Premium Commercial Smartwatch Central Dial for VEDA V2.0.
 * Restores the iconic circular hardware visual language from Reference Image 1,
 * bound to real Bluetooth GATT connection state and signal telemetry.
 */
@Composable
fun VedaCentralDial(
    connectionState: BleConnectionState,
    watchName: String,
    modifier: Modifier = Modifier,
    rssi: Int? = null,
    onDialClick: (() -> Unit)? = null
) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isBusy = connectionState.isBusy

    // Subtle, slow, non-distracting animation (8s period for busy, slow calm pulse for connected)
    val transition = rememberInfiniteTransition(label = "veda_dial_anim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dialRotation"
    )

    val pulseGlow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val (dialAccentColor, statusLabel, statusSubtext) = when (connectionState) {
        is BleConnectionState.Connected -> Triple(StatusGreen, "CONNECTED", if (rssi != null && rssi != 0) "$rssi dBm • Tap to Sync" else "Signal Strong • Tap to Sync")
        is BleConnectionState.Connecting -> Triple(AccentCyan, "CONNECTING...", "Establishing GATT link")
        is BleConnectionState.ConnectedGatt -> Triple(AccentCyan, "GATT OPEN", "Negotiating MTU")
        is BleConnectionState.NegotiatingMtu -> Triple(AccentCyan, "MTU CONFIG", "Allocating payload")
        is BleConnectionState.DiscoveringServices -> Triple(AccentCyan, "DISCOVERING", "Mapping primary service")
        is BleConnectionState.Reconnecting -> Triple(StatusWarning, "RECONNECTING", "Attempt ${connectionState.attempt}/${connectionState.maxAttempts}")
        is BleConnectionState.Scanning -> Triple(AccentCyan, "SCANNING...", "Searching for watch")
        is BleConnectionState.Disconnecting -> Triple(StatusWarning, "DISCONNECTING", "Releasing BLE link")
        is BleConnectionState.Disconnected -> Triple(TextTertiary, "STANDBY", "Tap to Connect Watch")
        is BleConnectionState.Error -> Triple(StatusError, "LINK ERROR", "Tap to Retry Connection")
    }

    Box(
        modifier = modifier
            .size(230.dp)
            .clip(CircleShape)
            .then(
                if (onDialClick != null) Modifier.clickable { onDialClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 4.dp.toPx()
            val trackRadius = outerRadius - 12.dp.toPx()
            val innerRadius = trackRadius - 10.dp.toPx()

            // 1. Outer subtle hairline ring
            drawCircle(
                color = StrokeSubtle,
                radius = outerRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Precision Tick Marks (36 ticks = every 10 degrees)
            val totalTicks = 36
            for (i in 0 until totalTicks) {
                val angle = (i * (360f / totalTicks)) + (if (isBusy) rotation else 0f)
                val rad = Math.toRadians(angle.toDouble())
                val isMajor = (i % 9 == 0) // Cardinal ticks at 0, 90, 180, 270
                val tickLen = if (isMajor) 6.dp.toPx() else 3.dp.toPx()
                val tickColor = when {
                    isMajor && isConnected -> StatusGreen.copy(alpha = 0.8f)
                    isMajor && isBusy -> AccentCyan.copy(alpha = 0.8f)
                    isMajor -> StrokeFocused
                    else -> StrokeSubtle
                }

                val startX = (center.x + (outerRadius - tickLen) * Math.cos(rad)).toFloat()
                val startY = (center.y + (outerRadius - tickLen) * Math.sin(rad)).toFloat()
                val endX = (center.x + outerRadius * Math.cos(rad)).toFloat()
                val endY = (center.y + outerRadius * Math.sin(rad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // 3. Middle Telemetry Status Arc
            val sweepAngle = when {
                isConnected -> 360f
                isBusy -> 160f
                else -> 90f
            }
            val startAngle = if (isBusy) rotation else -90f

            if (isConnected) {
                // Complete calm ambient glow ring
                drawCircle(
                    color = StatusGreen.copy(alpha = 0.15f * pulseGlow),
                    radius = trackRadius,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = StatusGreen.copy(alpha = 0.85f),
                    radius = trackRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            dialAccentColor.copy(alpha = 0.1f),
                            dialAccentColor,
                            dialAccentColor.copy(alpha = 0.1f)
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Inner Dark Graphite Core Face
            drawCircle(
                color = CanvasSecondary,
                radius = innerRadius
            )
            drawCircle(
                color = StrokeSubtle,
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Central Dial Information (Pure Typography & Real Hardware State)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            // Micro Hardware Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(VedaRadii.pill))
                    .background(SurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (isConnected) StatusGreen else TextTertiary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ESP32-C3",
                    style = Typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Watch Identifier
            Text(
                text = watchName,
                style = Typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 19.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Connection Status Tag with Pulse Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(VedaRadii.pill))
                    .background(dialAccentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                VedaPulseDot(
                    color = dialAccentColor,
                    size = 6.dp,
                    pulsing = isConnected || isBusy
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    style = Typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = dialAccentColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtext / Signal / Action Hint
            Text(
                text = statusSubtext,
                style = Typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = TextTertiary
            )
        }
    }
}
