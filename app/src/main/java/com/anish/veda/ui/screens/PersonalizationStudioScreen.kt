package com.anish.veda.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.VedaApplication
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentColorNames
import com.anish.veda.ui.theme.AccentColors
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusGreen
import com.anish.veda.ui.theme.StrokeFocused
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
 * VEDA V2.1 Personalization Studio.
 * Live theme configuration, accent color picker, font sizing, dial modes, and widget layout.
 */
@Composable
fun PersonalizationStudioScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val app = context.applicationContext as VedaApplication
    val preferencesRepository = app.preferencesRepository
    val settings by preferencesRepository.settingsFlow.collectAsState()

    val themes = listOf(
        Pair("DEFAULT", "Default VEDA"),
        Pair("GRAPHITE", "Matte Graphite"),
        Pair("MIDNIGHT", "Midnight Deep"),
        Pair("LIGHT", "Clinical Light")
    )

    val fontScales = listOf("Compact", "Standard", "Prominent")
    val dialModes = listOf("Neural Link", "Health Pulse", "Energy Hub", "Chronos RTC", "Neural AI", "Command")
    val quickActionOptions = listOf(
        Pair("FIND_WATCH", "Find Watch"),
        Pair("WATCH_STUDIO", "Watch Faces"),
        Pair("DND", "Do Not Disturb"),
        Pair("EMERGENCY", "Emergency SOS"),
        Pair("HAPTIC", "Haptic Studio"),
        Pair("HEALTH", "Health Pulse")
    )

    Scaffold(
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Menu.route,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = CanvasDeepBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VedaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Personalization Studio",
                        style = Typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Live theme styling, accents & dashboard layouts",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // SECTION 1: THEME PRESET
            VedaCard {
                VedaCardHeader(
                    label = "System Theme Preset",
                    statusText = settings.themePreset,
                    statusColor = AccentCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (code, title) ->
                        val isSelected = settings.themePreset.equals(code, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentCyan else StrokeSubtle,
                                    shape = RoundedCornerShape(VedaRadii.sm)
                                )
                                .clickable {
                                    preferencesRepository.updateSettings { it.copy(themePreset = code) }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) AccentCyan else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 2: ACCENT COLOUR PALETTE
            VedaCard {
                VedaCardHeader(
                    label = "Accent Colour",
                    statusText = AccentColorNames.getOrElse(settings.accentColorIndex) { "Cyan" },
                    statusColor = AccentColors.getOrElse(settings.accentColorIndex) { AccentCyan }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AccentColors.forEachIndexed { index, color ->
                        val isSelected = settings.accentColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    preferencesRepository.updateSettings { it.copy(accentColorIndex = index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CanvasDeepBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 3: FONT SCALE & MOTION
            VedaCard {
                VedaCardHeader(
                    label = "Typography & Motion",
                    statusText = fontScales.getOrElse(settings.fontScaleIndex) { "Standard" },
                    statusColor = AccentCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontScales.forEachIndexed { index, scaleName ->
                        val isSelected = settings.fontScaleIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentCyan else StrokeSubtle,
                                    shape = RoundedCornerShape(VedaRadii.sm)
                                )
                                .clickable {
                                    preferencesRepository.updateSettings { it.copy(fontScaleIndex = index) }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scaleName,
                                style = Typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) AccentCyan else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // Reduced Motion Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reduced Motion",
                            style = Typography.titleMedium.copy(fontSize = 14.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "Minimize rotation and particle effects to conserve battery",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = { checked ->
                            preferencesRepository.updateSettings { it.copy(reducedMotion = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 4: COMMAND CORE STARTING MODE
            VedaCard {
                VedaCardHeader(
                    label = "Command Core Starting Mode",
                    statusText = dialModes.getOrElse(settings.startingDialMode) { "Neural Link" },
                    statusColor = AccentCyan
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dialModes.forEachIndexed { index, modeName ->
                        val isSelected = settings.startingDialMode == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else SurfaceElevated)
                                .clickable {
                                    preferencesRepository.updateSettings { it.copy(startingDialMode = index) }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mode ${index + 1}: $modeName",
                                style = Typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) AccentCyan else TextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // SECTION 5: DASHBOARD WIDGET TOGGLES
            VedaCard {
                VedaCardHeader(
                    label = "Dashboard Widgets",
                    statusText = "CUSTOMIZE",
                    statusColor = AccentCyan
                )

                // Health Metric Widget Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Biometric Telemetry", style = Typography.bodyMedium, color = TextPrimary)
                        Text(text = "Show Heart Rate and Blood O2 cards on Home", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = settings.dashboardWidgetHealth,
                        onCheckedChange = { checked ->
                            preferencesRepository.updateSettings { it.copy(dashboardWidgetHealth = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AI Widget Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "VEDA AI Briefing Card", style = Typography.bodyMedium, color = TextPrimary)
                        Text(text = "Show smart summary and daily briefing card on Home", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = settings.dashboardWidgetAi,
                        onCheckedChange = { checked ->
                            preferencesRepository.updateSettings { it.copy(dashboardWidgetAi = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Actions Widget Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tactical Quick Actions", style = Typography.bodyMedium, color = TextPrimary)
                        Text(text = "Show 3 customizable shortcut action pills on Home", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = settings.dashboardWidgetQuickActions,
                        onCheckedChange = { checked ->
                            preferencesRepository.updateSettings { it.copy(dashboardWidgetQuickActions = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // Reset Button
            VedaSecondaryButton(
                text = "Reset All Layouts to Default",
                onClick = {
                    preferencesRepository.updateSettings {
                        it.copy(
                            themePreset = "DEFAULT",
                            accentColorIndex = 0,
                            fontScaleIndex = 1,
                            reducedMotion = false,
                            startingDialMode = 0,
                            dashboardWidgetHealth = true,
                            dashboardWidgetAi = true,
                            dashboardWidgetQuickActions = true,
                            quickAction1 = "FIND_WATCH",
                            quickAction2 = "WATCH_STUDIO",
                            quickAction3 = "DND"
                        )
                    }
                },
                icon = Icons.Default.Refresh
            )

            Spacer(modifier = Modifier.height(VedaSpacing.lg))
        }
    }
}
