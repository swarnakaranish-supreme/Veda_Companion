package com.anish.veda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StrokeFocused
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pixel-accurate 240x280 ST7789 TFT Watch Face Preview Canvas
 */
@Composable
fun WatchPreviewCanvas(
    faceIndex: Int,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = true,
    dateFormat: String = "DD/MM/YYYY"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(240f / 280f) // 1.69-inch ST7789 Aspect Ratio (240x280)
            .clip(RoundedCornerShape(24.dp))
            .background(CanvasDeepBlack)
            .border(2.dp, StrokeFocused, RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val cal = Calendar.getInstance()
            val rawHour = cal.get(Calendar.HOUR_OF_DAY)
            val hour = if (is24Hour) rawHour else (if (rawHour % 12 == 0) 12 else rawHour % 12)
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.SECOND)

            val timeStr = String.format("%02d:%02d", hour, minute)
            val secStr = String.format("%02d", second)
            val dateStr = if (dateFormat.contains("MM/DD")) "11/09" else "09/11"

            when (faceIndex) {
                0 -> drawMinimalDigital(w, h, timeStr, secStr, dateStr)
                1 -> drawFuturisticHud(w, h, timeStr, secStr, dateStr)
                2 -> drawCircularTechnical(w, h, hour, minute, second, timeStr)
                3 -> drawAnalogInspired(w, h, hour, minute, second)
                4 -> drawHealthDashboard(w, h, timeStr, dateStr)
                5 -> drawTimeDateMinimal(w, h, hour, minute, dateStr)
                6 -> drawDataFocus(w, h, timeStr, secStr, dateStr)
                7 -> drawVedaSignature(w, h, timeStr, dateStr)
                else -> drawMinimalDigital(w, h, timeStr, secStr, dateStr)
            }
        }
    }
}

// 1. MINIMAL DIGITAL
private fun DrawScope.drawMinimalDigital(w: Float, h: Float, timeStr: String, secStr: String, dateStr: String) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.26f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawText(timeStr, w / 2f, h * 0.52f, paint)

        val subPaint = android.graphics.Paint().apply {
            color = 0xFF37D7F2.toInt() // Cyan
            textSize = w * 0.08f
            typeface = android.graphics.Typeface.SANS_SERIF
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("WED, $dateStr", w / 2f, h * 0.32f, subPaint)
        drawText(":$secStr", w * 0.85f, h * 0.52f, subPaint)
    }

    // Battery bar at bottom
    drawRoundRect(
        color = Color(0xFF37D7F2),
        topLeft = Offset(w * 0.3f, h * 0.75f),
        size = Size(w * 0.4f, 6.dp.toPx()),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
}

// 2. FUTURISTIC HUD
private fun DrawScope.drawFuturisticHud(w: Float, h: Float, timeStr: String, secStr: String, dateStr: String) {
    val cyan = Color(0xFF37D7F2)
    val stroke = Stroke(width = 2.dp.toPx())

    // Corner HUD Brackets
    val bracketLen = w * 0.12f
    drawPath(Path().apply {
        moveTo(w * 0.08f, h * 0.08f + bracketLen); lineTo(w * 0.08f, h * 0.08f); lineTo(w * 0.08f + bracketLen, h * 0.08f)
    }, cyan, style = stroke)
    drawPath(Path().apply {
        moveTo(w * 0.92f - bracketLen, h * 0.08f); lineTo(w * 0.92f, h * 0.08f); lineTo(w * 0.92f, h * 0.08f + bracketLen)
    }, cyan, style = stroke)
    drawPath(Path().apply {
        moveTo(w * 0.08f, h * 0.92f - bracketLen); lineTo(w * 0.08f, h * 0.92f); lineTo(w * 0.08f + bracketLen, h * 0.92f)
    }, cyan, style = stroke)
    drawPath(Path().apply {
        moveTo(w * 0.92f - bracketLen, h * 0.92f); lineTo(w * 0.92f, h * 0.92f); lineTo(w * 0.92f, h * 0.92f - bracketLen)
    }, cyan, style = stroke)

    drawContext.canvas.nativeCanvas.apply {
        val timePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.22f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(timeStr, w / 2f, h * 0.52f, timePaint)

        val hudPaint = android.graphics.Paint().apply {
            color = 0xFF91A2B5.toInt()
            textSize = w * 0.055f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
        drawText("BAT 84%", w * 0.14f, h * 0.20f, hudPaint)
        drawText("BLE ONLINE", w * 0.56f, h * 0.20f, hudPaint)
        drawText("HR 72 BPM", w * 0.14f, h * 0.82f, hudPaint)
        drawText("O2 98%", w * 0.65f, h * 0.82f, hudPaint)
    }
}

// 3. CIRCULAR TECHNICAL
private fun DrawScope.drawCircularTechnical(w: Float, h: Float, hour: Int, minute: Int, second: Int, timeStr: String) {
    val cx = w / 2f
    val cy = h / 2f
    val r = (w.coerceAtMost(h) / 2f) * 0.82f

    // Outer Dial
    drawCircle(Color(0xFF1E2F43), r, Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))

    // 12 Ticks
    for (i in 0 until 12) {
        val angle = (i * 30.0) * (Math.PI / 180.0) - Math.PI / 2.0
        val x1 = (cx + cos(angle) * (r - 12.dp.toPx())).toFloat()
        val y1 = (cy + sin(angle) * (r - 12.dp.toPx())).toFloat()
        val x2 = (cx + cos(angle) * r).toFloat()
        val y2 = (cy + sin(angle) * r).toFloat()
        drawLine(Color(0xFF37D7F2), Offset(x1, y1), Offset(x2, y2), strokeWidth = 2.dp.toPx())
    }

    // Radial seconds indicator line
    val secAngle = (second * 6.0) * (Math.PI / 180.0) - Math.PI / 2.0
    drawLine(
        Color(0xFFFF637A),
        Offset(cx, cy),
        Offset((cx + cos(secAngle) * (r - 18.dp.toPx())).toFloat(), (cy + sin(secAngle) * (r - 18.dp.toPx())).toFloat()),
        strokeWidth = 2.dp.toPx()
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.16f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(timeStr, cx, cy + 10.dp.toPx(), paint)
    }
}

// 4. ANALOG INSPIRED
private fun DrawScope.drawAnalogInspired(w: Float, h: Float, hour: Int, minute: Int, second: Int) {
    val cx = w / 2f
    val cy = h / 2f
    val r = (w.coerceAtMost(h) / 2f) * 0.84f

    drawCircle(Color(0xFF121E2B), r, Offset(cx, cy))
    drawCircle(Color(0xFF2C4360), r, Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))

    // Hour Hand
    val hAngle = ((hour % 12 + minute / 60f) * 30.0) * (Math.PI / 180.0) - Math.PI / 2.0
    drawLine(
        Color.White,
        Offset(cx, cy),
        Offset((cx + cos(hAngle) * (r * 0.5f)).toFloat(), (cy + sin(hAngle) * (r * 0.5f)).toFloat()),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Minute Hand
    val mAngle = (minute * 6.0) * (Math.PI / 180.0) - Math.PI / 2.0
    drawLine(
        Color(0xFF37D7F2),
        Offset(cx, cy),
        Offset((cx + cos(mAngle) * (r * 0.72f)).toFloat(), (cy + sin(mAngle) * (r * 0.72f)).toFloat()),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Second Hand
    val sAngle = (second * 6.0) * (Math.PI / 180.0) - Math.PI / 2.0
    drawLine(
        Color(0xFFFF637A),
        Offset(cx, cy),
        Offset((cx + cos(sAngle) * (r * 0.85f)).toFloat(), (cy + sin(sAngle) * (r * 0.85f)).toFloat()),
        strokeWidth = 1.5.dp.toPx()
    )

    // Center pivot
    drawCircle(Color.White, 4.dp.toPx(), Offset(cx, cy))
}

// 5. HEALTH DASHBOARD
private fun DrawScope.drawHealthDashboard(w: Float, h: Float, timeStr: String, dateStr: String) {
    drawContext.canvas.nativeCanvas.apply {
        val timePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.15f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(timeStr, w * 0.1f, h * 0.25f, timePaint)

        val hrPaint = android.graphics.Paint().apply {
            color = 0xFFFF637A.toInt() // Red
            textSize = w * 0.18f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("72", w * 0.1f, h * 0.55f, hrPaint)

        val labelPaint = android.graphics.Paint().apply {
            color = 0xFF91A2B5.toInt()
            textSize = w * 0.06f
            typeface = android.graphics.Typeface.SANS_SERIF
            isAntiAlias = true
        }
        drawText("BPM", w * 0.45f, h * 0.55f, labelPaint)
        drawText("SpO2 98%", w * 0.1f, h * 0.80f, labelPaint)
    }

    // Stylized ECG Waveform
    val path = Path().apply {
        moveTo(w * 0.1f, h * 0.65f)
        lineTo(w * 0.3f, h * 0.65f)
        lineTo(w * 0.35f, h * 0.60f)
        lineTo(w * 0.42f, h * 0.72f)
        lineTo(w * 0.48f, h * 0.58f)
        lineTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.85f, h * 0.65f)
    }
    drawPath(path, Color(0xFFFF637A), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

// 6. TIME & DATE MINIMAL
private fun DrawScope.drawTimeDateMinimal(w: Float, h: Float, hour: Int, minute: Int, dateStr: String) {
    drawContext.canvas.nativeCanvas.apply {
        val hPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.35f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(String.format("%02d", hour), w / 2f, h * 0.40f, hPaint)

        val mPaint = android.graphics.Paint().apply {
            color = 0xFF37D7F2.toInt()
            textSize = w * 0.35f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(String.format("%02d", minute), w / 2f, h * 0.72f, mPaint)

        val dPaint = android.graphics.Paint().apply {
            color = 0xFF91A2B5.toInt()
            textSize = w * 0.065f
            typeface = android.graphics.Typeface.SANS_SERIF
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("WEDNESDAY • $dateStr", w / 2f, h * 0.88f, dPaint)
    }
}

// 7. DATA FOCUS
private fun DrawScope.drawDataFocus(w: Float, h: Float, timeStr: String, secStr: String, dateStr: String) {
    val stroke = Stroke(width = 1.dp.toPx())
    drawLine(Color(0xFF1E2F43), Offset(w * 0.08f, h / 2f), Offset(w * 0.92f, h / 2f), strokeWidth = 1.dp.toPx())
    drawLine(Color(0xFF1E2F43), Offset(w / 2f, h * 0.08f), Offset(w / 2f, h * 0.92f), strokeWidth = 1.dp.toPx())

    drawContext.canvas.nativeCanvas.apply {
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.08f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
            isFakeBoldText = true
        }
        val sub = android.graphics.Paint().apply {
            color = 0xFF37D7F2.toInt()
            textSize = w * 0.045f
            typeface = android.graphics.Typeface.SANS_SERIF
            isAntiAlias = true
        }
        // Q1: Time
        drawText(timeStr, w * 0.12f, h * 0.28f, p)
        drawText("SEC :$secStr", w * 0.12f, h * 0.36f, sub)

        // Q2: Battery
        drawText("84%", w * 0.58f, h * 0.28f, p)
        drawText("BATTERY", w * 0.58f, h * 0.36f, sub)

        // Q3: Biometrics
        drawText("74", w * 0.12f, h * 0.72f, p)
        drawText("BPM / 98% O2", w * 0.12f, h * 0.80f, sub)

        // Q4: Status
        drawText("ONLINE", w * 0.58f, h * 0.72f, p)
        drawText("VEDA LINK", w * 0.58f, h * 0.80f, sub)
    }
}

// 8. VEDA SIGNATURE
private fun DrawScope.drawVedaSignature(w: Float, h: Float, timeStr: String, dateStr: String) {
    // Angular accent chevrons
    val path = Path().apply {
        moveTo(w * 0.1f, h * 0.15f)
        lineTo(w * 0.4f, h * 0.15f)
        lineTo(w * 0.55f, h * 0.22f)
        lineTo(w * 0.9f, h * 0.22f)
    }
    drawPath(path, Color(0xFF37D7F2), style = Stroke(width = 2.dp.toPx()))

    drawContext.canvas.nativeCanvas.apply {
        val vedaPaint = android.graphics.Paint().apply {
            color = 0xFF37D7F2.toInt()
            textSize = w * 0.08f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("VEDA V2.0", w * 0.12f, h * 0.12f, vedaPaint)

        val timePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.24f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText(timeStr, w / 2f, h * 0.58f, timePaint)

        val subPaint = android.graphics.Paint().apply {
            color = 0xFF91A2B5.toInt()
            textSize = w * 0.06f
            typeface = android.graphics.Typeface.SANS_SERIF
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText("AEROSPACE CHRONO", w / 2f, h * 0.78f, subPaint)
    }
}
