package com.anish.veda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

/**
 * Precision HUD Data Card for VEDA V2.
 * Flat dark graphite elevation, 1dp subtle hairline stroke, optional top status accent.
 */
@Composable
fun VedaCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = VedaRadii.lg,
    containerColor: Color = SurfaceGraphite,
    borderColor: Color = StrokeSubtle,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = clickableModifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional top telemetry accent strip (1.5dp)
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(accentColor)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(VedaSpacing.lg)
            ) {
                content()
            }
        }
    }
}

/**
 * Standardized Telemetry Header inside a VedaCard
 */
@Composable
fun VedaCardHeader(
    label: String,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusColor: Color = TextTertiary,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = VedaSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = Typography.labelSmall,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.weight(1f))
        if (statusText != null) {
            Text(
                text = statusText.uppercase(),
                style = Typography.labelSmall,
                color = statusColor
            )
        }
        trailingContent?.invoke()
    }
}
