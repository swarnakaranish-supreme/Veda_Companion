package com.anish.veda.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.WarningAmber

@Composable
fun CircularStatusGauge(
    connectionState: BleConnectionState,
    watchName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gauge_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (statusColor, statusText) = when (connectionState) {
        is BleConnectionState.Connected -> MatrixGreen to "ONLINE"
        is BleConnectionState.Connecting -> WarningAmber to "LINKING..."
        is BleConnectionState.ConnectedGatt -> WarningAmber to "GATT OPEN"
        is BleConnectionState.NegotiatingMtu -> WarningAmber to "MTU CONFIG"
        is BleConnectionState.DiscoveringServices -> WarningAmber to "GATT DISCOVERY"
        is BleConnectionState.Reconnecting -> WarningAmber to "RECONNECTING"
        is BleConnectionState.Scanning -> NeonCyan to "SEARCHING..."
        is BleConnectionState.Disconnecting -> WarningAmber to "DISCONNECTING"
        is BleConnectionState.Disconnected -> AlertRed to "OFFLINE"
        is BleConnectionState.Error -> AlertRed to "ERROR"
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 8.dp.toPx()
            val middleRadius = outerRadius - 16.dp.toPx()
            val innerRadius = middleRadius - 14.dp.toPx()

            // Outer subtle track
            drawCircle(
                color = BorderNavy,
                radius = outerRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer decorative tick marks
            val totalTicks = 24
            for (i in 0 until totalTicks) {
                val angle = (i * (360f / totalTicks)) + (if (connectionState.isBusy) rotation else 0f)
                val rad = Math.toRadians(angle.toDouble())
                val startX = (center.x + (outerRadius - 4.dp.toPx()) * Math.cos(rad)).toFloat()
                val startY = (center.y + (outerRadius - 4.dp.toPx()) * Math.sin(rad)).toFloat()
                val endX = (center.x + outerRadius * Math.cos(rad)).toFloat()
                val endY = (center.y + outerRadius * Math.sin(rad)).toFloat()

                drawLine(
                    color = if (i % 6 == 0) NeonCyan.copy(alpha = 0.8f) else BorderNavy,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 6 == 0) 2.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // Middle gradient arc
            val sweepAngle = if (connectionState.isConnected) 300f else if (connectionState.isBusy) 180f else 60f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(statusColor.copy(alpha = 0.2f), statusColor, statusColor.copy(alpha = 0.2f))
                ),
                startAngle = if (connectionState.isBusy) rotation else -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner glowing ring
            drawCircle(
                color = if (connectionState.isConnected) MatrixGreen.copy(alpha = pulseAlpha * 0.15f)
                else NeonCyan.copy(alpha = 0.05f),
                radius = innerRadius
            )
            drawCircle(
                color = statusColor.copy(alpha = 0.4f),
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ESP32-C3",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = watchName,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Pill
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            if (connectionState is BleConnectionState.Connected && connectionState.rssi != 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${connectionState.rssi} dBm",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
