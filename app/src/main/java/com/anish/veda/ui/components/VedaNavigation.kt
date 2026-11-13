package com.anish.veda.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasMidnightNavy
import com.anish.veda.ui.theme.CanvasPrimary
import com.anish.veda.ui.theme.StatusGreen
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

/**
 * Top App Bar with VEDA V2.0 Branding and Live Telemetry Chip
 */
@Composable
fun VedaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusColor: Color = AccentCyan,
    pulsing: Boolean = false,
    onStatusClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasPrimary)
            .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VEDA",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VedaRadii.xs))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "V2.0",
                        style = Typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AccentCyan
                    )
                }
            }
            Text(
                text = title,
                style = Typography.labelSmall,
                color = TextTertiary
            )
        }

        if (statusText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(VedaRadii.pill))
                    .then(
                        if (onStatusClick != null) Modifier.clickable { onStatusClick() }
                        else Modifier
                    )
            ) {
                VedaStatusChip(
                    statusText = statusText,
                    statusColor = statusColor,
                    pulsing = pulsing
                )
            }
        }
    }
}

data class VedaNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeColor: Color = AccentCyan
)

// Clean 5-Destination Primary Navigation (Home, Health, VEDA AI, Alerts, Menu)
val VedaPrimaryNavDestinations = listOf(
    VedaNavItem(Screen.Dashboard.route, "Home", Icons.Default.Dashboard, AccentCyan),
    VedaNavItem(Screen.Health.route, "Health", Icons.Default.Favorite, StatusGreen),
    VedaNavItem(Screen.AiAssistant.route, "VEDA AI", Icons.Default.AutoAwesome, AccentViolet),
    VedaNavItem(Screen.NotificationSettings.route, "Alerts", Icons.Default.Notifications, AccentCyan),
    VedaNavItem(Screen.Menu.route, "Menu", Icons.Default.Menu, AccentCyan)
)

/**
 * Premium Ergonomic 5-Tab Bottom Navigation Bar for VEDA V2.1.
 * Designed for comfortable one-handed reach with >= 48dp touch targets,
 * prominent icons, high-contrast active state indicators, and gesture inset handling.
 */
@Composable
fun VedaBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasMidnightNavy)
            .border(width = 1.dp, color = StrokeSubtle)
            .navigationBarsPadding() // Ensures adequate padding above Android system gesture nav bar
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VedaPrimaryNavDestinations.forEach { item ->
                // Check if currentRoute matches the item or one of its child screens
                val isSelected = currentRoute == item.route || when (item.route) {
                    Screen.Menu.route -> currentRoute == Screen.Menu.route ||
                            currentRoute == Screen.Bluetooth.route ||
                            currentRoute == Screen.WatchStudio.route ||
                            currentRoute == Screen.HapticStudio.route ||
                            currentRoute == Screen.Guardian.route ||
                            currentRoute == Screen.Diagnostics.route ||
                            currentRoute == Screen.PersonalizationStudio.route ||
                            currentRoute == Screen.WatchRemoteControl.route ||
                            currentRoute == Screen.GeneralSettings.route ||
                            currentRoute == Screen.CallSettings.route
                    else -> false
                }

                val activeColor = item.activeColor
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else TextTertiary,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "iconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) TextPrimary else TextTertiary,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "labelColor"
                )

                // Large touch target (min 48dp x 48dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(VedaRadii.md))
                        .background(
                            if (isSelected) SurfaceGraphite else Color.Transparent
                        )
                        .then(
                            if (isSelected) Modifier.border(1.dp, activeColor.copy(alpha = 0.25f), RoundedCornerShape(VedaRadii.md))
                            else Modifier
                        )
                        .clickable { onNavigate(item.route) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icon with active highlight
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = iconColor
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.label,
                            style = Typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            ),
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}
