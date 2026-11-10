package com.anish.veda.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.ui.components.CyberHeader
import com.anish.veda.ui.components.FuturisticCard
import com.anish.veda.ui.components.GlowingButton
import com.anish.veda.ui.components.GlowingOutlinedButton
import com.anish.veda.ui.components.TechBadge
import com.anish.veda.ui.theme.AlertRed
import com.anish.veda.ui.theme.BorderNavy
import com.anish.veda.ui.theme.CyberBlue
import com.anish.veda.ui.theme.MatrixGreen
import com.anish.veda.ui.theme.NeonCyan
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy
import com.anish.veda.ui.theme.VedaSurfaceNavy
import com.anish.veda.ui.theme.WarningAmber
import com.anish.veda.utils.Constants
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.viewmodel.GeneralSettingsViewModel

@Composable
fun GeneralSettingsScreen(
    navController: NavController,
    viewModel: GeneralSettingsViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isBatteryExempt = PermissionUtils.isIgnoringBatteryOptimizations(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "General Settings",
            subtitle = "Background Execution & System Preferences",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Auto Connect Card
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Reconnect on Launch",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically establishes GATT link to last paired watch upon app start or phone reboot",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = settings.autoConnect,
                        onCheckedChange = { viewModel.setAutoConnect(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Auto Time Sync on Connect
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Sync Time on Connect",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Immediately synchronizes watch software RTC when BLE handshake completes",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = settings.autoSyncTimeOnConnect,
                        onCheckedChange = { viewModel.setAutoSyncTime(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }
            }

            // Periodic Time Sync
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Periodic Background Time Sync",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Prevents software RTC drift on RTC-free ESP32-C3 hardware",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = settings.periodicTimeSync,
                        onCheckedChange = { viewModel.setPeriodicTimeSync(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VedaBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VedaCardNavy
                        )
                    )
                }

                if (settings.periodicTimeSync) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SYNC INTERVAL",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 6, 12).forEach { hours ->
                            val isSelected = settings.syncIntervalHours == hours
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonCyan else VedaSurfaceNavy)
                                    .clickable { viewModel.setSyncIntervalHours(hours) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${hours}h",
                                    color = if (isSelected) VedaBlack else TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // OPPO ColorOS & Android 15 Background Guide Card
            FuturisticCard(glowBorder = true, borderColor = CyberBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "OPPO COLOROS / ANDROID 15 SETUP",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keep BLE link active when screen is off or other apps are open",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ColorOsStep(
                    step = "1",
                    title = "Battery Optimization",
                    desc = "Set to 'Don't Optimize' to prevent ColorOS from cutting the Bluetooth connection when the phone is locked."
                )

                ColorOsStep(
                    step = "2",
                    title = "Allow Background Activity",
                    desc = "In App Info > Battery Usage, ensure 'Allow background activity' is toggled ON."
                )

                ColorOsStep(
                    step = "3",
                    title = "Auto-Launch (ColorOS)",
                    desc = "In Settings > Apps > Auto-launch, enable VEDA Companion so the watch reconnects automatically after reboot."
                )

                ColorOsStep(
                    step = "4",
                    title = "Lock in Recent Tasks",
                    desc = "Open Recents / Multi-tasking view, tap the 3 dots or lock icon above VEDA Companion to prevent swipe-to-kill."
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlowingButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(PermissionUtils.createBatteryOptimizationIntent(context))
                        }
                    ) {
                        Text(text = "BATTERY EXEMPTION", fontSize = 11.sp)
                    }

                    GlowingOutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(PermissionUtils.createAppDetailsSettingsIntent(context))
                        }
                    ) {
                        Text(text = "APP INFO SETTINGS", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Honest Security Limitations Note
                Text(
                    text = "Android Limitation Note: Android 15 & ColorOS security policy strictly blocks any background service from auto-restarting if you explicitly Force Stop the app in Android Settings, or if Bluetooth permissions are revoked. Under normal use, the persistent foreground service keeps the watch connected.",
                    color = TextMuted.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            // Battery Optimization Card
            FuturisticCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (isBatteryExempt) MatrixGreen else AlertRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Battery Optimization Status",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isBatteryExempt) "Unrestricted background execution active" else "Restricted — ColorOS may kill background link",
                                color = if (isBatteryExempt) MatrixGreen else AlertRed,
                                fontSize = 12.sp
                            )
                        }
                    }

                    TechBadge(
                        text = if (isBatteryExempt) "EXEMPT" else "RESTRICTED",
                        color = if (isBatteryExempt) MatrixGreen else AlertRed
                    )
                }
            }

            // Foreground Service Control
            FuturisticCard {
                Text(
                    text = "FOREGROUND SERVICE CONTROL",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restart the background daemon to reload GATT callbacks and notification channels",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlowingOutlinedButton(
                    onClick = { viewModel.restartForegroundService() }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RESTART VEDA SERVICE")
                }
            }

            // ESP32-C3 Hardware Specification Info
            FuturisticCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FIRMWARE HARDWARE SPECS",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SpecItem("Microcontroller", "ESP32-C3 RISC-V 160MHz")
                SpecItem("Display Driver", "SH1106 128x64 OLED (I2C)")
                SpecItem("Advertised Name", Constants.DEFAULT_WATCH_NAME)
                SpecItem("Service UUID", Constants.SERVICE_UUID_STRING)
                SpecItem("Write Characteristic", Constants.WRITE_CHARACTERISTIC_UUID_STRING)
                SpecItem("GATT Write Property", "PROPERTY_WRITE | PROPERTY_WRITE_NR")
                SpecItem("Time Format", "T|YYYY|MM|DD|HH|MM|SS")
                SpecItem("Notification Format", "N|cat|app|title|msg")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ColorOsStep(step: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VedaSurfaceNavy),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
