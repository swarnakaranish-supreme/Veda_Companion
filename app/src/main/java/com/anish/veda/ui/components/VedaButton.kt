package com.anish.veda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusErrorDim
import com.anish.veda.ui.theme.StrokeFocused
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

@Composable
fun VedaDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    VedaEmergencyButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        height = height
    )
}

/**
 * Primary Tactical Action Button (Solid Electric Cyan)
 */
@Composable
fun VedaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 48.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(VedaRadii.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCyan,
            contentColor = CanvasDeepBlack,
            disabledContainerColor = AccentCyan.copy(alpha = 0.25f),
            disabledContentColor = CanvasDeepBlack.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = VedaSpacing.lg, vertical = VedaSpacing.sm)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CanvasDeepBlack,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(VedaSpacing.sm))
                }
                Text(
                    text = text.uppercase(),
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                )
            }
        }
    }
}

/**
 * Outlined Secondary Action Button (Hairline Slate)
 */
@Composable
fun VedaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(VedaRadii.md),
        border = BorderStroke(1.dp, if (enabled) StrokeFocused else StrokeFocused.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceGraphite,
            contentColor = TextPrimary,
            disabledContentColor = TextSecondary.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = VedaSpacing.lg, vertical = VedaSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(VedaSpacing.sm))
            }
            Text(
                text = text,
                style = Typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}

/**
 * Emergency Destructive Button (SOS / Abort)
 */
@Composable
fun VedaEmergencyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(VedaRadii.md),
        border = BorderStroke(1.dp, StatusError),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = StatusErrorDim,
            contentColor = StatusError,
            disabledContentColor = StatusError.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = VedaSpacing.lg, vertical = VedaSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = StatusError
                )
                Spacer(modifier = Modifier.width(VedaSpacing.sm))
            }
            Text(
                text = text.uppercase(),
                style = Typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

/**
 * Compact Pill Button for quick actions or filters
 */
@Composable
fun VedaPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    accentColor: Color = AccentCyan
) {
    val containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else SurfaceElevated
    val contentColor = if (isSelected) accentColor else TextSecondary
    val borderColor = if (isSelected) accentColor.copy(alpha = 0.5f) else StrokeFocused

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(VedaRadii.pill),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = Typography.labelMedium.copy(color = contentColor)
            )
        }
    }
}
