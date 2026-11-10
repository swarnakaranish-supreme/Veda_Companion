package com.anish.veda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anish.veda.ui.theme.BorderGlow
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CardGradientEnd
import com.anish.veda.ui.theme.CardGradientStart

@Composable
fun FuturisticCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glowBorder: Boolean = false,
    borderColor: Color = if (glowBorder) BorderGlow else BorderNavy,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CardGradientStart, CardGradientEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}
