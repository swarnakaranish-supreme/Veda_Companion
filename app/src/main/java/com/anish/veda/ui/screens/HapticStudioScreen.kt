package com.anish.veda.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.data.model.HapticCatalog
import com.anish.veda.data.model.HapticCategory
import com.anish.veda.data.model.HapticConfig
import com.anish.veda.data.model.HapticEffect
import com.anish.veda.data.model.HapticProfileOption
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaDangerButton
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaSecondaryButton
import com.anish.veda.ui.components.VedaStatusChip
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.CanvasMidnightNavy
import com.anish.veda.ui.theme.StatusGreen
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing
import com.anish.veda.ui.theme.WarningAmber
import com.anish.veda.viewmodel.HapticStudioViewModel

@Composable
fun HapticStudioScreen(
    navController: NavController,
    viewModel: HapticStudioViewModel
) {
    val hapticConfig by viewModel.hapticConfig.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedPresetId by viewModel.selectedPresetId.collectAsState()
    val lastTestTriggered by viewModel.lastTestTriggered.collectAsState()

    var activeCategoryFilter by remember { mutableStateOf<HapticCategory?>(null) }
    var syncSuccessBanner by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.HapticStudio.route,
                onNavigate = { route ->
                    if (route != Screen.HapticStudio.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                .background(CanvasDeepBlack)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasDeepBlack)
                    .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VEDA",
                                style = Typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VedaRadii.xs))
                                    .background(AccentCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DRV2605L",
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = AccentCyan
                                )
                            }
                        }
                        Text(
                            text = "HAPTIC STUDIO",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                VedaStatusChip(
                    statusText = if (connectionState.isConnected) "ONLINE" else "OFFLINE",
                    statusColor = if (connectionState.isConnected) StatusGreen else AlertRed,
                    pulsing = connectionState.isConnected
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hardware Architecture Telemetry Card
                item {
                    VedaCard {
                        VedaCardHeader(
                            label = "Hardware Driver Architecture",
                            statusText = "I2C 0x5A",
                            statusColor = AccentCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("DRIVER IC", style = Typography.labelSmall, color = TextTertiary)
                                Text("TI DRV2605L", style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Column {
                                Text("ACTUATOR", style = Typography.labelSmall, color = TextTertiary)
                                Text("ERM Coin Motor", style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Column {
                                Text("ROM LIBRARY", style = Typography.labelSmall, color = TextTertiary)
                                Text("TS2200 Lib 1", style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                        }
                    }
                }

                // 2. Profile Presets Carousel
                item {
                    Column {
                        Text(
                            text = "PRESET HAPTIC PROFILES",
                            style = Typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HapticCatalog.PRESETS.forEach { preset ->
                                val isSelected = selectedPresetId == preset.id
                                val bg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else CanvasMidnightNavy
                                val stroke = if (isSelected) AccentCyan else StrokeSubtle

                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(VedaRadii.sm))
                                        .background(bg)
                                        .border(1.dp, stroke, RoundedCornerShape(VedaRadii.sm))
                                        .clickable { viewModel.selectPreset(preset) }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = preset.name,
                                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) AccentCyan else TextPrimary
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(VedaRadii.xs))
                                                .background(SurfaceElevated)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = preset.tag,
                                                style = Typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                color = if (isSelected) AccentCyan else TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = preset.description,
                                            style = Typography.bodySmall.copy(fontSize = 10.sp),
                                            color = TextSecondary,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Call Vibration Profile
                item {
                    HapticSectionCard(
                        title = "Incoming Call Profile",
                        icon = Icons.Default.Phone,
                        iconTint = WarningAmber,
                        options = HapticConfig.CALL_PROFILES,
                        selectedId = hapticConfig.callProfile,
                        onSelect = { viewModel.setCallProfile(it) },
                        onTest = { option -> viewModel.playEffect(option.previewEffectId) }
                    )
                }

                // 4. Message Vibration Profile
                item {
                    HapticSectionCard(
                        title = "Message Notification Profile",
                        icon = Icons.Default.Message,
                        iconTint = AccentCyan,
                        options = HapticConfig.MSG_PROFILES,
                        selectedId = hapticConfig.msgProfile,
                        onSelect = { viewModel.setMsgProfile(it) },
                        onTest = { option -> viewModel.playEffect(option.previewEffectId) }
                    )
                }

                // 5. Alarm Vibration Profile
                item {
                    HapticSectionCard(
                        title = "Alarm Wakeup Profile",
                        icon = Icons.Default.Alarm,
                        iconTint = AccentViolet,
                        options = HapticConfig.ALARM_PROFILES,
                        selectedId = hapticConfig.alarmProfile,
                        onSelect = { viewModel.setAlarmProfile(it) },
                        onTest = { option -> viewModel.playEffect(option.previewEffectId) }
                    )
                }

                // 6. Emergency SOS Profile
                item {
                    HapticSectionCard(
                        title = "Emergency SOS Profile",
                        icon = Icons.Default.Security,
                        iconTint = AlertRed,
                        options = HapticConfig.EMERGENCY_PROFILES,
                        selectedId = hapticConfig.emergencyProfile,
                        onSelect = { viewModel.setEmergencyProfile(it) },
                        onTest = { option -> viewModel.playEffect(option.previewEffectId) }
                    )
                }

                // 7. Navigation Rotary Encoder Detents
                item {
                    VedaCard {
                        VedaCardHeader(
                            label = "Navigation Detent Feedback",
                            statusText = if (hapticConfig.navFeedback) "ENABLED" else "MUTED",
                            statusColor = if (hapticConfig.navFeedback) StatusGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rotary Encoder Haptic Clicks",
                                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Instant mechanical click upon scrolling physical encoder",
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = hapticConfig.navFeedback,
                                onCheckedChange = { viewModel.setNavFeedback(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StatusGreen,
                                    checkedTrackColor = StatusGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceElevated
                                )
                            )
                        }

                        if (hapticConfig.navFeedback) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "DETENT FEEL / WAVEFORM",
                                style = Typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticConfig.NAV_EFFECTS.forEach { effect ->
                                    val isSelected = hapticConfig.navEffectId == effect.id
                                    val bg = if (isSelected) AccentCyan.copy(alpha = 0.2f) else CanvasDeepBlack
                                    val stroke = if (isSelected) AccentCyan else StrokeSubtle

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(VedaRadii.sm))
                                            .background(bg)
                                            .border(1.dp, stroke, RoundedCornerShape(VedaRadii.sm))
                                            .clickable {
                                                viewModel.setNavEffectId(effect.id)
                                                viewModel.playEffect(effect.id)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = effect.name,
                                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) AccentCyan else TextPrimary
                                            )
                                            Text(
                                                text = effect.intensity,
                                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Haptic Test Centre (Interactive Catalog)
                item {
                    VedaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Haptic Waveform Test Centre",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Trigger raw DRV2605 ROM waveforms on your watch",
                                        style = Typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.stopHaptics() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf(
                                null to "ALL",
                                HapticCategory.CLICK to "CLICKS",
                                HapticCategory.BUZZ to "BUZZES",
                                HapticCategory.ALERT to "ALERTS",
                                HapticCategory.PULSE to "PULSES",
                                HapticCategory.RAMP to "RAMPS"
                            )

                            categories.forEach { (cat, label) ->
                                val isSelected = activeCategoryFilter == cat
                                val bg = if (isSelected) AccentCyan.copy(alpha = 0.2f) else CanvasDeepBlack
                                val stroke = if (isSelected) AccentCyan else StrokeSubtle
                                val textCol = if (isSelected) AccentCyan else TextSecondary

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(VedaRadii.pill))
                                        .background(bg)
                                        .border(1.dp, stroke, RoundedCornerShape(VedaRadii.pill))
                                        .clickable { activeCategoryFilter = cat }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = Typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = textCol
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filtered Effects
                        val displayedEffects = HapticCatalog.EFFECTS.filter {
                            activeCategoryFilter == null || it.category == activeCategoryFilter
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedEffects.forEach { effect ->
                                val isTesting = lastTestTriggered == effect.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(VedaRadii.xs))
                                        .background(if (isTesting) AccentCyan.copy(alpha = 0.1f) else CanvasDeepBlack)
                                        .border(
                                            1.dp,
                                            if (isTesting) AccentCyan else StrokeSubtle,
                                            RoundedCornerShape(VedaRadii.xs)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "#${effect.id}",
                                                style = Typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = AccentCyan
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = effect.name,
                                                style = Typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "${effect.description} • ${effect.intensity}",
                                            style = Typography.bodySmall.copy(fontSize = 10.sp),
                                            color = TextSecondary
                                        )
                                    }

                                    VedaSecondaryButton(
                                        text = "PLAY",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = { viewModel.playEffect(effect.id) },
                                        modifier = Modifier.width(80.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Sync Haptics Button
                item {
                    VedaPrimaryButton(
                        text = "SYNC HAPTIC PROFILES TO WATCH",
                        icon = Icons.Default.Sync,
                        onClick = {
                            viewModel.syncToWatch()
                            syncSuccessBanner = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    AnimatedVisibility(visible = syncSuccessBanner) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(StatusGreen.copy(alpha = 0.15f))
                                .border(1.dp, StatusGreen, RoundedCornerShape(VedaRadii.sm))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Haptic profile synchronized to DRV2605L on watch!",
                                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HapticSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    options: List<HapticProfileOption>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onTest: (HapticProfileOption) -> Unit
) {
    val activeOption = options.find { it.id == selectedId } ?: options[0]

    VedaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = activeOption.title,
                        style = Typography.labelSmall,
                        color = iconTint
                    )
                }
            }

            VedaSecondaryButton(
                text = "TEST",
                icon = Icons.Default.PlayArrow,
                onClick = { onTest(activeOption) },
                modifier = Modifier.width(84.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Options Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option.id == selectedId
                val bg = if (isSelected) iconTint.copy(alpha = 0.15f) else CanvasDeepBlack
                val stroke = if (isSelected) iconTint else StrokeSubtle

                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(bg)
                        .border(1.dp, stroke, RoundedCornerShape(VedaRadii.sm))
                        .clickable { onSelect(option.id) }
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.title,
                                style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) iconTint else TextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.description,
                            style = Typography.bodySmall.copy(fontSize = 9.sp),
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
