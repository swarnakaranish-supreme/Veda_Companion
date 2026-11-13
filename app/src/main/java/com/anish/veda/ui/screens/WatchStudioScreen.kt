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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anish.veda.data.model.VedaWatchFaces
import com.anish.veda.data.model.WatchFaceMeta
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaPillButton
import com.anish.veda.ui.components.VedaPrimaryButton
import com.anish.veda.ui.components.VedaTopAppBar
import com.anish.veda.ui.components.WatchPreviewCanvas
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusSuccess
import com.anish.veda.ui.theme.StatusWarning
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
import com.anish.veda.viewmodel.WatchStudioViewModel

@Composable
fun WatchStudioScreen(
    navController: NavController,
    viewModel: WatchStudioViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val customFace by viewModel.customFace.collectAsState()
    val notifTimeoutSec by viewModel.notifTimeoutSec.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncNotice by viewModel.syncSuccessNotice.collectAsState()

    val isConnected = connectionState.isConnected
    val selectedMeta = VedaWatchFaces[config.selectedFaceIndex.coerceIn(0, VedaWatchFaces.lastIndex)]

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "Watch Face Studio",
                statusText = if (isConnected) "LINK READY" else "OFFLINE",
                statusColor = if (isConnected) StatusSuccess else StatusError
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.WatchStudio.route,
                onNavigate = { route ->
                    if (route != Screen.WatchStudio.route) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.md)
        ) {
            // HERO PREVIEW CARD: 240x280 ST7789 CANVAS
            VedaCard(
                accentColor = AccentCyan
            ) {
                VedaCardHeader(
                    label = "ST7789 240x280 Hardware Preview",
                    statusText = selectedMeta.tag,
                    statusColor = AccentCyan
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WatchPreviewCanvas(
                        faceIndex = config.selectedFaceIndex,
                        is24Hour = config.is24Hour,
                        dateFormat = config.dateFormat
                    )
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                Text(
                    text = selectedMeta.name.uppercase(),
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedMeta.description,
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // NINE WATCH FACES CAROUSEL SELECTOR
            VedaCard {
                VedaCardHeader(
                    label = "Select Watch Face (${VedaWatchFaces.size} Styles)",
                    statusText = "#${config.selectedFaceIndex + 1} ACTIVE",
                    statusColor = AccentCyan
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VedaWatchFaces.forEach { face ->
                        val isSelected = face.index == config.selectedFaceIndex
                        val borderColor = if (isSelected) AccentCyan else StrokeSubtle
                        val bgColor = if (isSelected) SurfaceElevated else SurfaceGraphite

                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .clip(RoundedCornerShape(VedaRadii.md))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(VedaRadii.md))
                                .clickable { viewModel.selectFace(face.index) }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Watch,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) AccentCyan else TextTertiary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = AccentCyan
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = face.name,
                                    style = Typography.titleSmall.copy(fontSize = 12.sp),
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = face.tag,
                                    style = Typography.labelSmall.copy(fontSize = 8.sp),
                                    color = if (isSelected) AccentCyan else TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // CUSTOM WATCH FACE BUILDER & PERSONALIZER (FACE 9)
            VedaCard(accentColor = AccentCyan) {
                VedaCardHeader(
                    label = "Custom Watch Face Creator",
                    statusText = "PERSONAL STUDIO",
                    statusColor = AccentCyan
                )

                Text(
                    text = "Craft your personalized watch face layout and flash directly to ESP32 firmware in real time.",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Layout Style Selector
                Text(
                    text = "Dial Style",
                    style = Typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val styles = listOf("Bold Dig", "Minimal", "Cyber", "Sport")
                    styles.forEachIndexed { idx, name ->
                        Box(modifier = Modifier.weight(1f)) {
                            VedaPillButton(
                                label = name,
                                isSelected = customFace.style == idx,
                                onClick = { viewModel.updateCustomStyle(idx) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Accent Color Palette (RGB565 colors supported by firmware)
                Text(
                    text = "Accent Color: ${customFace.colorName}",
                    style = Typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val colors = listOf(
                        Triple("Cyan", 0x07FF, AccentCyan),
                        Triple("Green", 0x07E0, StatusSuccess),
                        Triple("Violet", 0x981F, AccentViolet),
                        Triple("Teal", 0x04F7, androidx.compose.ui.graphics.Color(0xFF26C6DA)),
                        Triple("Gold", 0xFEA0, StatusWarning),
                        Triple("Red", 0xF800, StatusError)
                    )
                    colors.forEach { (name, rgb565, composeColor) ->
                        val isSelected = customFace.accentColorRgb565 == rgb565
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(VedaRadii.sm))
                                .background(composeColor.copy(alpha = if (isSelected) 0.9f else 0.3f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TextPrimary else StrokeSubtle,
                                    shape = RoundedCornerShape(VedaRadii.sm)
                                )
                                .clickable { viewModel.updateCustomColor(rgb565, name) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CanvasDeepBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Telemetry Widgets
                Text(
                    text = "Display Widgets",
                    style = Typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Heart Rate (BPM)", style = Typography.bodySmall, color = TextPrimary)
                        Switch(
                            checked = customFace.showHeartRate,
                            onCheckedChange = { viewModel.toggleCustomHeartRate(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CanvasDeepBlack, checkedTrackColor = AccentCyan)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blood Oxygen (SpO2)", style = Typography.bodySmall, color = TextPrimary)
                        Switch(
                            checked = customFace.showSpO2,
                            onCheckedChange = { viewModel.toggleCustomSpO2(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CanvasDeepBlack, checkedTrackColor = AccentCyan)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calendar Date & Day", style = Typography.bodySmall, color = TextPrimary)
                        Switch(
                            checked = customFace.showDate,
                            onCheckedChange = { viewModel.toggleCustomDate(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CanvasDeepBlack, checkedTrackColor = AccentCyan)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Seconds Arc / Readout", style = Typography.bodySmall, color = TextPrimary)
                        Switch(
                            checked = customFace.showSeconds,
                            onCheckedChange = { viewModel.toggleCustomSeconds(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CanvasDeepBlack, checkedTrackColor = AccentCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Custom Brand / Label Moniker
                Text(
                    text = "Dial Brand Name: ${customFace.customLabel}",
                    style = Typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val labels = listOf("ANISH", "VEDA", "CHRONO", "PRO")
                    labels.forEach { lbl ->
                        Box(modifier = Modifier.weight(1f)) {
                            VedaPillButton(
                                label = lbl,
                                isSelected = customFace.customLabel == lbl,
                                onClick = { viewModel.updateCustomLabel(lbl) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Flash Custom Face Action Button
                VedaPrimaryButton(
                    text = "Apply Custom Face to Watch",
                    onClick = { viewModel.applyCustomFaceToWatch() },
                    icon = Icons.Default.Tune,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // HARDWARE DISPLAY & CUSTOM FACE SETTINGS
            VedaCard {
                VedaCardHeader(
                    label = "Hardware Display Controls",
                    statusText = "CUSTOM PREFERENCES",
                    statusColor = AccentCyan
                )

                // 1. Brightness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backlight Brightness",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${config.brightnessPercent}%",
                        style = Typography.labelMedium,
                        color = AccentCyan
                    )
                }

                Slider(
                    value = config.brightnessPercent.toFloat(),
                    onValueChange = { viewModel.setBrightness(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StrokeFocused
                    )
                )

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // 2. Screen Timeout Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Screen Timeout",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (config.timeoutSeconds == 0) "Always On" else "${config.timeoutSeconds}s",
                        style = Typography.labelMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10, 15, 30, 60, 0).forEach { sec ->
                        val isSelected = config.timeoutSeconds == sec
                        val label = if (sec == 0) "∞" else "${sec}s"
                        Box(modifier = Modifier.weight(1f)) {
                            VedaPillButton(
                                label = label,
                                isSelected = isSelected,
                                onClick = { viewModel.setTimeoutSeconds(sec) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.lg))

                // 3. Notification Auto-Dismiss Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Display Timeout",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${notifTimeoutSec}s",
                        style = Typography.labelMedium,
                        color = AccentCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        val isSelected = notifTimeoutSec == sec
                        val label = if (sec == 10) "10s (Opt)" else "${sec}s"
                        Box(modifier = Modifier.weight(1f)) {
                            VedaPillButton(
                                label = label,
                                isSelected = isSelected,
                                onClick = { viewModel.setNotificationTimeout(sec) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(VedaSpacing.lg))

                // 4. 24-Hour Time Format Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "24-Hour Military Format",
                            style = Typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (config.is24Hour) "14:30 (24-Hour)" else "02:30 PM (12-Hour)",
                            style = Typography.bodySmall,
                            color = TextTertiary
                        )
                    }

                    Switch(
                        checked = config.is24Hour,
                        onCheckedChange = { viewModel.set24HourFormat(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CanvasDeepBlack,
                            checkedTrackColor = AccentCyan,
                            uncheckedTrackColor = StrokeFocused
                        )
                    )
                }

                Spacer(modifier = Modifier.height(VedaSpacing.md))

                // 5. Date Format Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date Format",
                        style = Typography.titleSmall,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD").forEach { fmt ->
                        val isSelected = config.dateFormat == fmt
                        Box(modifier = Modifier.weight(1f)) {
                            VedaPillButton(
                                label = fmt,
                                isSelected = isSelected,
                                onClick = { viewModel.setDateFormat(fmt) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VedaSpacing.md))

            // TACTICAL 3-BUTTON HARDWARE CONTROLS NOTICE
            VedaCard(borderColor = StrokeSubtle) {
                VedaCardHeader(
                    label = "Firmware Architecture",
                    statusText = "3-BUTTON INPUT",
                    statusColor = StatusSuccess
                )
                Text(
                    text = "High-efficiency local hardware rendering at 80 MHz CPU clock. Press Button 1 (UP) or Button 2 (DOWN) to cycle watch faces. Press Button 3 (SELECT) to enter menus. Hold Button 3 for 3 seconds to trigger Guardian Emergency SOS.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.lg))

            // SYNC CONFIGURATION ACTION BUTTON
            VedaPrimaryButton(
                text = if (isSyncing) "Syncing to Watch..." else "Sync Configuration to Watch",
                onClick = { viewModel.syncAllConfiguration() },
                icon = Icons.Default.Sync,
                enabled = isConnected && !isSyncing,
                loading = isSyncing
            )

            AnimatedVisibility(visible = syncNotice != null) {
                Spacer(modifier = Modifier.height(VedaSpacing.sm))
                Text(
                    text = syncNotice ?: "",
                    style = Typography.bodySmall,
                    color = StatusSuccess,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(VedaSpacing.xl))
        }
    }
}
