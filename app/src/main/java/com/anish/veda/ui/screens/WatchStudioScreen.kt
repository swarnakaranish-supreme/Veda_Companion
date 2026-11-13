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
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncNotice by viewModel.syncSuccessNotice.collectAsState()

    val isConnected = connectionState.isConnected
    val selectedMeta = VedaWatchFaces[config.selectedFaceIndex.coerceIn(0, 7)]

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

            // EIGHT WATCH FACES CAROUSEL SELECTOR
            VedaCard {
                VedaCardHeader(
                    label = "Select Watch Face (8 Styles)",
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

                // 3. 24-Hour Time Format Toggle
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

                // 4. Date Format Selection
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

            // LOCAL RENDERING & ROTARY ENCODER NOTICE
            VedaCard(borderColor = StrokeSubtle) {
                VedaCardHeader(
                    label = "Firmware Architecture",
                    statusText = "HARDWARE RENDERING",
                    statusColor = StatusSuccess
                )
                Text(
                    text = "Graphics are generated entirely in ESP32-C3 firmware. No heavy bitmaps are streamed over BLE. Use the rotary encoder on your watch to cycle faces, and press Enter to select.",
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
