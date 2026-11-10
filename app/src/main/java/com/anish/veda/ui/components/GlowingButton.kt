package com.anish.veda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy

@Composable
fun GlowingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    containerColor: Color = NeonCyan,
    contentColor: Color = VedaBlack,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun GlowingOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    borderColor: Color = NeonCyan,
    contentColor: Color = NeonCyan,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = VedaCardNavy.copy(alpha = 0.5f),
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun DestructiveOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    content: @Composable RowScope.() -> Unit
) {
    GlowingOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        borderColor = AlertRed,
        contentColor = AlertRed,
        content = content
    )
}
