package com.anish.veda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.data.model.DailyHealthSummary
import com.anish.veda.data.model.HealthMeasurement
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

/**
 * Continuous Smooth Bézier Spline Chart for Heart Rate Trends
 */
@Composable
fun VedaSplineTrendChart(
    measurements: List<HealthMeasurement>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    lineColor: Color = StatusError
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(VedaRadii.md))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO HISTORICAL TELEMETRY RECORDED",
                style = Typography.labelSmall,
                color = TextTertiary
            )
        }
        return
    }

    val reversedData = measurements.take(30).reversed()
    val values = reversedData.map { it.heartRate.toFloat() }
    val minVal = (values.minOrNull() ?: 50f).coerceAtLeast(40f)
    val maxVal = (values.maxOrNull() ?: 120f).coerceAtMost(200f)
    val range = if (maxVal - minVal > 5f) maxVal - minVal else 10f

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(VedaRadii.md))
                .background(SurfaceElevated)
                .padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw hairline gridlines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = StrokeSubtle,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (values.size < 2) {
                    // Draw single point if only 1 sample
                    val x = w / 2f
                    val y = h - ((values[0] - minVal) / range) * h
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    return@Canvas
                }

                val stepX = w / (values.size - 1)
                val path = Path()
                val fillPath = Path()

                val firstX = 0f
                val firstY = h - ((values[0] - minVal) / range) * h
                path.moveTo(firstX, firstY)
                fillPath.moveTo(firstX, h)
                fillPath.lineTo(firstX, firstY)

                for (i in 1 until values.size) {
                    val prevX = (i - 1) * stepX
                    val prevY = h - ((values[i - 1] - minVal) / range) * h
                    val currentX = i * stepX
                    val currentY = h - ((values[i] - minVal) / range) * h

                    val controlX1 = (prevX + currentX) / 2f
                    val controlY1 = prevY
                    val controlX2 = (prevX + currentX) / 2f
                    val controlY2 = currentY

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
                }

                val lastX = (values.size - 1) * stepX
                fillPath.lineTo(lastX, h)
                fillPath.close()

                // Draw gradient under-fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // Draw smooth stroke line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MIN: ${minVal.toInt()} BPM",
                style = Typography.labelSmall.copy(fontSize = 9.sp),
                color = TextTertiary
            )
            Text(
                text = "PEAK: ${maxVal.toInt()} BPM",
                style = Typography.labelSmall.copy(fontSize = 9.sp),
                color = TextTertiary
            )
        }
    }
}

/**
 * 7-Day Stepped Bar Chart for Daily Averages
 */
@Composable
fun VedaDailyBarChart(
    summaries: List<DailyHealthSummary>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    if (summaries.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(VedaRadii.md))
            .background(SurfaceElevated)
            .padding(VedaSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEach { item ->
                val barHeightFraction = if (item.avgHeartRate > 0) {
                    (item.avgHeartRate.toFloat() / 160f).coerceIn(0.15f, 1f)
                } else 0.05f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.avgHeartRate > 0) {
                        Text(
                            text = "${item.avgHeartRate}",
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height((height.value * 0.5f * barHeightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (item.avgHeartRate > 0) AccentCyan else StrokeSubtle)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.dateLabel,
                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (item.avgHeartRate > 0) TextSecondary else TextTertiary
                    )
                }
            }
        }
    }
}
