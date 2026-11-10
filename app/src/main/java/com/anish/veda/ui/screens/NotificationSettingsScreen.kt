package com.anish.veda.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.anish.veda.notifications.AppNotificationItem
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
import com.anish.veda.ui.theme.TextDim
import com.anish.veda.ui.theme.TextMuted
import com.anish.veda.ui.theme.TextWhite
import com.anish.veda.ui.theme.VedaBlack
import com.anish.veda.ui.theme.VedaCardNavy
import com.anish.veda.ui.theme.VedaSurfaceNavy
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.viewmodel.NotificationSettingsViewModel

@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsState()
    val isMasterEnabled by viewModel.isMasterEnabled.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()

    var isListenerGranted by remember {
        mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context))
    }

    // Refresh listener status when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerGranted = PermissionUtils.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VedaBlack)
    ) {
        CyberHeader(
            title = "Notification Hub",
            subtitle = "Android Notification Forwarding Service",
            onBackClick = { navController.popBackStack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Access Status Card
            item {
                FuturisticCard(
                    glowBorder = !isListenerGranted,
                    borderColor = if (isListenerGranted) BorderNavy else AlertRed
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isListenerGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isListenerGranted) MatrixGreen else AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Notification Access Status",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isListenerGranted) "Active and listening for notifications" else "Permission required to stream notifications",
                                    color = if (isListenerGranted) MatrixGreen else AlertRed,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TechBadge(
                            text = if (isListenerGranted) "ACTIVE" else "DISABLED",
                            color = if (isListenerGranted) MatrixGreen else AlertRed
                        )
                    }

                    if (!isListenerGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GlowingButton(
                            onClick = {
                                context.startActivity(PermissionUtils.createNotificationListenerSettingsIntent())
                            },
                            containerColor = AlertRed,
                            contentColor = TextWhite
                        ) {
                            Text(
                                text = "ENABLE NOTIFICATION ACCESS",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Master Notification Switch
            item {
                FuturisticCard(glowBorder = isMasterEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Master Notification Stream",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Send formatted N|0|APP|TITLE|MSG packets over BLE to watch",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isMasterEnabled,
                            onCheckedChange = { viewModel.setMasterNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VedaBlack,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = VedaCardNavy
                            )
                        )
                    }
                }
            }

            // Large Notification Truncation Slider
            item {
                FuturisticCard {
                    Text(
                        text = "Message Payload Truncation",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Truncates message body to fit ESP32 OLED buffer (Current: ${settings.maxMessageLength} chars)",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = settings.maxMessageLength.toFloat(),
                        onValueChange = { viewModel.setMaxMessageLength(it.toInt()) },
                        valueRange = 40f..180f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = BorderNavy
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "40 chars", color = TextMuted, fontSize = 11.sp)
                        Text(text = "180 chars (Firmware Max)", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Test Notification Triggers
            item {
                FuturisticCard {
                    Text(
                        text = "TRIGGER TEST NOTIFICATIONS",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Pushes actual N|0|... payloads to watch via GATT queue",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlowingOutlinedButton(
                            onClick = {
                                viewModel.sendTestNotification(
                                    appName = "WhatsApp",
                                    title = "John Miller",
                                    text = "See you at the robotics workshop at 5PM!"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "WhatsApp", fontSize = 12.sp)
                        }

                        GlowingOutlinedButton(
                            onClick = {
                                viewModel.sendTestNotification(
                                    appName = "Instagram",
                                    title = "Sophie",
                                    text = "Sent you a photo."
                                )
                            },
                            modifier = Modifier.weight(1f),
                            borderColor = CyberBlue,
                            contentColor = CyberBlue
                        ) {
                            Text(text = "Instagram", fontSize = 12.sp)
                        }

                        GlowingOutlinedButton(
                            onClick = {
                                viewModel.sendTestNotification(
                                    appName = "Gmail",
                                    title = "Security Alert",
                                    text = "New login from ESP32-C3 companion device."
                                )
                            },
                            modifier = Modifier.weight(1f),
                            borderColor = MatrixGreen,
                            contentColor = MatrixGreen
                        ) {
                            Text(text = "Gmail", fontSize = 12.sp)
                        }
                    }
                }
            }

            // App Filter Section Header & Search Field
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "APP-SPECIFIC FORWARDING FILTERS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (isLoadingApps) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = "Search installed applications...", color = TextDim, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // App List
            items(filteredApps, key = { it.packageName }) { appItem ->
                AppFilterRow(
                    item = appItem,
                    onToggle = { isEnabled ->
                        viewModel.setAppEnabled(appItem.packageName, isEnabled)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppFilterRow(
    item: AppNotificationItem,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(item.packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationIcon(item.packageName)
        } catch (e: Exception) {
            null
        }
    }

    FuturisticCard(
        glowBorder = item.isPopular && item.isEnabled,
        borderColor = if (item.isEnabled) BorderNavy else BorderNavy.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (appIcon != null) {
                    val bitmap = remember(appIcon) {
                        appIcon.toBitmap(width = 80, height = 80).asImageBitmap()
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = item.appName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VedaSurfaceNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.appName,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.isPopular) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TechBadge(text = "RECOMMENDED", color = NeonCyan)
                        }
                    }
                    Text(
                        text = item.packageName,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = item.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VedaBlack,
                    checkedTrackColor = NeonCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = VedaCardNavy
                )
            )
        }
    }
}
