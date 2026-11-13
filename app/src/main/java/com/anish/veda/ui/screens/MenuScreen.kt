package com.anish.veda.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.anish.veda.ui.components.VedaBottomNavBar
import com.anish.veda.ui.components.VedaCard
import com.anish.veda.ui.components.VedaCardHeader
import com.anish.veda.ui.components.VedaTopAppBar
import com.anish.veda.ui.navigation.Screen
import com.anish.veda.ui.theme.AccentCyan
import com.anish.veda.ui.theme.AccentViolet
import com.anish.veda.ui.theme.CanvasDeepBlack
import com.anish.veda.ui.theme.CanvasPrimary
import com.anish.veda.ui.theme.CanvasSecondary
import com.anish.veda.ui.theme.StatusError
import com.anish.veda.ui.theme.StatusGreen
import com.anish.veda.ui.theme.StatusWarning
import com.anish.veda.ui.theme.StrokeSubtle
import com.anish.veda.ui.theme.SurfaceElevated
import com.anish.veda.ui.theme.SurfaceGraphite
import com.anish.veda.ui.theme.TextPrimary
import com.anish.veda.ui.theme.TextSecondary
import com.anish.veda.ui.theme.TextTertiary
import com.anish.veda.ui.theme.Typography
import com.anish.veda.ui.theme.VedaRadii
import com.anish.veda.ui.theme.VedaSpacing

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val targetRoute: String
)

data class MenuCategory(
    val categoryName: String,
    val categoryCode: String,
    val items: List<MenuItem>
)

/**
 * 5th Navigation Tab: VEDA Menu.
 * Centralized, categorized, and searchable directory of all smartwatch capabilities.
 */
@Composable
fun MenuScreen(
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }

    val allCategories = remember {
        listOf(
            MenuCategory(
                categoryName = "MY WATCH",
                categoryCode = "A",
                items = listOf(
                    MenuItem("Device Hub & Bluetooth", "GATT connection status, pairing, RSSI, auto-reconnect", Icons.Default.Bluetooth, AccentCyan, Screen.Bluetooth.route),
                    MenuItem("Hardware Diagnostics", "Sensor self-tests, protocol check, battery status", Icons.Default.Security, StatusGreen, Screen.Diagnostics.route)
                )
            ),
            MenuCategory(
                categoryName = "WATCH CONTROLS & REMOTE",
                categoryCode = "B",
                items = listOf(
                    MenuItem("Watch Remote Control", "Remote backlight, DND, timeout, alarm & timer commands", Icons.Default.Build, AccentCyan, Screen.WatchRemoteControl.route),
                    MenuItem("Watch Face Studio", "Select and preview 8 local watch faces on ST7789", Icons.Default.Watch, AccentCyan, Screen.WatchStudio.route)
                )
            ),
            MenuCategory(
                categoryName = "BIOMETRIC HEALTH",
                categoryCode = "C",
                items = listOf(
                    MenuItem("Heart Rate & Blood O2", "Live MAX30102 PPG pulse monitor, history & trends", Icons.Default.Favorite, StatusError, Screen.Health.route)
                )
            ),
            MenuCategory(
                categoryName = "NOTIFICATIONS & CALLS",
                categoryCode = "D",
                items = listOf(
                    MenuItem("Alerts & Notification Hub", "App package filtering, quiet hours, privacy mode, timeline", Icons.Default.Notifications, AccentCyan, Screen.NotificationSettings.route),
                    MenuItem("Call Forwarding Settings", "Telephony state detection, caller name resolution", Icons.Default.Notifications, AccentViolet, Screen.CallSettings.route)
                )
            ),
            MenuCategory(
                categoryName = "VEDA NEURAL AI",
                categoryCode = "E",
                items = listOf(
                    MenuItem("AI Assistant & Briefings", "Gemini conversational AI, voice command mic, smart replies", Icons.Default.AutoAwesome, AccentViolet, Screen.AiAssistant.route)
                )
            ),
            MenuCategory(
                categoryName = "SAFETY & GUARDIAN",
                categoryCode = "F",
                items = listOf(
                    MenuItem("Emergency Guardian SOS", "Watch hardware button triggers, phone GPS, contact SMS alerts", Icons.Default.Security, StatusError, Screen.Guardian.route)
                )
            ),
            MenuCategory(
                categoryName = "HAPTIC STUDIO",
                categoryCode = "G",
                items = listOf(
                    MenuItem("DRV2605L Haptics", "Custom vibration patterns for calls, alarms & navigation", Icons.Default.Vibration, AccentCyan, Screen.HapticStudio.route)
                )
            ),
            MenuCategory(
                categoryName = "PERSONALIZATION STUDIO",
                categoryCode = "H",
                items = listOf(
                    MenuItem("Personalization Studio", "Theme presets, accent colours, fonts, dial starting mode", Icons.Default.Palette, AccentCyan, Screen.PersonalizationStudio.route)
                )
            ),
            MenuCategory(
                categoryName = "SYSTEM & PERMISSIONS",
                categoryCode = "I",
                items = listOf(
                    MenuItem("System Permissions", "Manage Android 14/15 BLE, notifications, location permissions", Icons.Default.Settings, TextSecondary, Screen.PermissionsOnboarding.route),
                    MenuItem("General Settings", "Synchronization intervals, message length limits, backup", Icons.Default.Settings, TextSecondary, Screen.GeneralSettings.route)
                )
            )
        )
    }

    // Filter items based on query
    val filteredCategories = if (searchQuery.isBlank()) {
        allCategories
    } else {
        allCategories.mapNotNull { cat ->
            val matchingItems = cat.items.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.subtitle.contains(searchQuery, ignoreCase = true) ||
                        cat.categoryName.contains(searchQuery, ignoreCase = true)
            }
            if (matchingItems.isNotEmpty()) cat.copy(items = matchingItems) else null
        }
    }

    Scaffold(
        topBar = {
            VedaTopAppBar(
                title = "VEDA Directory",
                statusText = "INDEX",
                statusColor = AccentCyan
            )
        },
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.Menu.route,
                onNavigate = { route ->
                    if (route != Screen.Menu.route) {
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
                .padding(horizontal = VedaSpacing.lg, vertical = VedaSpacing.sm)
        ) {
            // Live Directory Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VedaSpacing.md),
                placeholder = {
                    Text(
                        text = "Search settings, features or tools...",
                        style = Typography.bodyMedium,
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = StrokeSubtle,
                    focusedContainerColor = SurfaceGraphite,
                    unfocusedContainerColor = SurfaceGraphite,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(VedaRadii.md),
                singleLine = true
            )

            // Categorized List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(VedaSpacing.md)
            ) {
                filteredCategories.forEach { category ->
                    item {
                        Text(
                            text = category.categoryName,
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = AccentCyan,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                    }

                    items(category.items) { item ->
                        MenuRowCard(
                            item = item,
                            onClick = {
                                navController.navigate(item.targetRoute)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(VedaSpacing.lg))
                }
            }
        }
    }
}

@Composable
private fun MenuRowCard(
    item: MenuItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VedaRadii.md))
            .background(SurfaceGraphite)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(VedaRadii.sm))
                .background(item.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = Typography.titleMedium.copy(fontSize = 15.sp),
                color = TextPrimary
            )
            Text(
                text = item.subtitle,
                style = Typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}
