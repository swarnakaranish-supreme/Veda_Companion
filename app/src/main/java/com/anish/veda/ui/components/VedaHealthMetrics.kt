package com.anish.veda.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

/**
 * Health Metric HUD Card for Dashboard and Health Screens.
 * Adheres strictly to the requirement: "Use actual application state. Do not show fake readings. Show unavailable states correctly."
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VedaMetricCard(
    label: String,
    value: String?,
    unit: String?,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    statusNote: String? = null,
    onClick: (() -> Unit)? = null
) {
    VedaCard(
        modifier = modifier,
        accentColor = if (value != null) accentColor else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(VedaSpacing.sm))
                Text(
                    text = label.uppercase(),
                    style = Typography.labelSmall,
                    color = TextTertiary
                )
            }

            if (statusNote != null) {
                Text(
                    text = statusNote.uppercase(),
                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (value != null) StatusSuccess else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(VedaSpacing.md))

        // Numerical reading with smooth vertical slide transitions for real updates
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedContent(
                targetState = value ?: "--",
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith
                            slideOutVertically { height -> -height }
                },
                label = "MetricNumericUpdate"
            ) { targetValue ->
                Text(
                    text = targetValue,
                    style = Typography.displayMedium,
                    color = if (value != null) TextPrimary else TextTertiary
                )
            }

            if (unit != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = Typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(VedaSpacing.xs))
        Text(
            text = if (value != null) "Real-time reading" else "Unavailable / Not measured yet",
            style = Typography.bodySmall,
            color = TextTertiary
        )
    }
}

/**
 * Sensor Quality 4-Segment HUD Meter
 */
@Composable
fun VedaSensorQualityMeter(
    qualityScore: Int?, // 0 to 100, or null if disconnected
    modifier: Modifier = Modifier
) {
    val activeSegments = when {
        qualityScore == null -> 0
        qualityScore >= 75 -> 4
        qualityScore >= 50 -> 3
        qualityScore >= 25 -> 2
        qualityScore > 0 -> 1
        else -> 0
    }

    val stateText = when {
        qualityScore == null -> "Sensor Idle / Disconnected"
        qualityScore >= 75 -> "Optimal Contact"
        qualityScore >= 50 -> "Good Contact"
        qualityScore >= 25 -> "Fair (Motion Detected)"
        else -> "Poor Contact / Readjust"
    }

    val segmentColor = when {
        qualityScore == null -> TextTertiary
        qualityScore >= 75 -> StatusSuccess
        qualityScore >= 50 -> AccentCyan
        qualityScore >= 25 -> StatusWarning
        else -> StatusError
    }

    VedaCard(
        modifier = modifier,
        accentColor = if (qualityScore != null) segmentColor else null
    ) {
        VedaCardHeader(
            label = "Sensor Quality (MAX30102)",
            statusText = if (qualityScore != null) "$qualityScore%" else "INACTIVE",
            statusColor = segmentColor
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = VedaSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..4) {
                val isFilled = i <= activeSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(VedaRadii.xs))
                        .background(if (isFilled) segmentColor else StrokeSubtle)
                )
            }
        }

        Spacer(modifier = Modifier.height(VedaSpacing.xs))
        Text(
            text = stateText,
            style = Typography.bodySmall,
            color = if (qualityScore != null) segmentColor else TextTertiary
        )
    }
}
