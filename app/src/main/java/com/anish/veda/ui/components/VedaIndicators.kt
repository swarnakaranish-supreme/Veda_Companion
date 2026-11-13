package com.anish.veda.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii

/**
 * Real-time HUD pulse dot with subtle expanding concentric ripple ring.
 */
@Composable
fun VedaPulseDot(
    modifier: Modifier = Modifier,
    color: Color = StatusSuccess,
    size: Dp = 8.dp,
    pulsing: Boolean = true
) {
    if (!pulsing) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "PulseTransition")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(size * 2.5f),
        contentAlignment = Alignment.Center
    ) {
        // Expanding ripple
        Canvas(modifier = Modifier.size(size * scale)) {
            drawCircle(
                color = color.copy(alpha = alpha),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        // Center solid core
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * 4-Bar Stepped RSSI Signal Strength Indicator
 */
@Composable
fun VedaRssiBars(
    rssi: Int?,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentCyan,
    inactiveColor: Color = StrokeSubtle
) {
    val activeBars = when {
        rssi == null -> 0
        rssi >= -60 -> 4
        rssi >= -75 -> 3
        rssi >= -85 -> 2
        rssi >= -95 -> 1
        else -> 0
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            val barHeight = (4 + (i * 3)).dp
            val barColor = if (i <= activeBars) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
            if (i < 4) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

/**
 * Geometric Battery Micro-Gauge
 */
@Composable
fun VedaBatteryIndicator(
    batteryPercent: Int?,
    modifier: Modifier = Modifier,
    isCharging: Boolean = false
) {
    val effectivePercent = batteryPercent?.coerceIn(0, 100)
    val color = when {
        effectivePercent == null -> TextTertiary
        effectivePercent <= 15 -> StatusError
        effectivePercent <= 30 -> StatusWarning
        else -> AccentCyan
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCharging) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                modifier = Modifier.size(14.dp),
                tint = StatusSuccess
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        // Battery outer shell
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .padding(1.5.dp)
            ) {
                if (effectivePercent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(effectivePercent / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color)
                    )
                }
            }
            // Battery positive terminal nipple
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(5.dp)
                    .background(color.copy(alpha = 0.7f))
            )
        }

        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (effectivePercent != null) "$effectivePercent%" else "--%",
            style = Typography.labelMedium.copy(fontSize = 11.sp),
            color = if (effectivePercent != null) TextPrimary else TextTertiary
        )
    }
}

/**
 * Tactical Connection Status Chip (Connected / Disconnected / Syncing)
 */
@Composable
fun VedaStatusChip(
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(VedaRadii.pill))
            .background(statusColor.copy(alpha = 0.12f))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(VedaRadii.pill))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VedaPulseDot(
            color = statusColor,
            size = 6.dp,
            pulsing = pulsing
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText.uppercase(),
            style = Typography.labelSmall.copy(fontSize = 9.sp),
            color = statusColor
        )
    }
}
