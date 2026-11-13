package com.anish.veda.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.NotificationHistoryItem
import com.anish.veda.notifications.AppNotificationItem
import com.anish.veda.notifications.NotificationCategory
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
import com.anish.veda.ui.theme.StrokeHighlight
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
import com.anish.veda.utils.PermissionUtils
import com.anish.veda.utils.TimeUtils
import com.anish.veda.viewmodel.NotificationSettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State collections
    val settings by viewModel.settings.collectAsState()
    val isMasterEnabled by viewModel.isMasterEnabled.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val historySearchQuery by viewModel.historySearchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()

    val filteredApps by viewModel.filteredApps.collectAsState()
    val appSearchQuery by viewModel.appSearchQuery.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()

    val privacyMode by viewModel.privacyModeEnabled.collectAsState()
    val hideSender by viewModel.hideSenderInPrivacy.collectAsState()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsState()
    val quietStartH by viewModel.quietHoursStartHour.collectAsState()
    val quietStartM by viewModel.quietHoursStartMinute.collectAsState()
    val quietEndH by viewModel.quietHoursEndHour.collectAsState()
    val quietEndM by viewModel.quietHoursEndMinute.collectAsState()
    val quietAllowCalls by viewModel.quietHoursAllowCalls.collectAsState()

    val vipOnly by viewModel.vipContactsOnly.collectAsState()
    val vipList by viewModel.vipContactsList.collectAsState()

    var isListenerGranted by remember {
        mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context))
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Timeline, 1: Apps, 2: Rules & Privacy

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

    Scaffold(
        bottomBar = {
            VedaBottomNavBar(
                currentRoute = Screen.NotificationSettings.route,
                onNavigate = { route ->
                    if (route != Screen.NotificationSettings.route) {
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
            // Top App Bar
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
                                    text = "ALERT HUB",
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = AccentCyan
                                )
                            }
                        }
                        Text(
                            text = "NOTIFICATION CENTRE",
                            style = Typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                VedaStatusChip(
                    statusText = if (isMasterEnabled) "FORWARDING" else "MUTED",
                    statusColor = if (isMasterEnabled) StatusGreen else AlertRed,
                    pulsing = isMasterEnabled
                )
            }

            // Tab Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasMidnightNavy)
                    .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("TIMELINE", "APP FILTER", "POLICIES & PRIVACY")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val activeBg = if (isSelected) AccentCyan.copy(alpha = 0.15f) else Color.Transparent
                    val activeStroke = if (isSelected) AccentCyan else StrokeSubtle
                    val activeText = if (isSelected) AccentCyan else TextSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VedaRadii.pill))
                            .background(activeBg)
                            .border(1.dp, activeStroke, RoundedCornerShape(VedaRadii.pill))
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = Typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = activeText
                        )
                    }
                }
            }

            // Content Area based on Tab
            when (selectedTab) {
                0 -> TimelineTabContent(
                    historyList = historyList,
                    searchQuery = historySearchQuery,
                    onSearchQueryChange = { viewModel.setHistorySearchQuery(it) },
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelect = { viewModel.setSelectedCategoryFilter(it) },
                    unreadCount = unreadCount,
                    onMarkAllAsRead = { viewModel.markAllAsRead() },
                    onClearHistory = { viewModel.clearHistory() },
                    onDeleteNotification = { viewModel.deleteNotification(it) },
                    isListenerGranted = isListenerGranted,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
                1 -> AppFilterTabContent(
                    installedApps = filteredApps,
                    searchQuery = appSearchQuery,
                    onSearchQueryChange = { viewModel.setAppSearchQuery(it) },
                    isLoading = isLoadingApps,
                    onToggleApp = { pkg, enabled -> viewModel.setAppEnabled(pkg, enabled) },
                    onEnableAll = { viewModel.setAllAppsEnabled(true) },
                    onDisableAll = { viewModel.setAllAppsEnabled(false) },
                    onEnablePopular = { viewModel.enablePopularAppsOnly() }
                )
                2 -> PoliciesTabContent(
                    isListenerGranted = isListenerGranted,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    isMasterEnabled = isMasterEnabled,
                    onToggleMaster = { viewModel.setMasterNotifications(it) },
                    privacyMode = privacyMode,
                    onTogglePrivacy = { viewModel.setPrivacyMode(it) },
                    hideSender = hideSender,
                    onToggleHideSender = { viewModel.setHideSenderInPrivacy(it) },
                    quietHoursEnabled = quietHoursEnabled,
                    quietStartH = quietStartH,
                    quietStartM = quietStartM,
                    quietEndH = quietEndH,
                    quietEndM = quietEndM,
                    quietAllowCalls = quietAllowCalls,
                    onUpdateQuietHours = { enabled, sh, sm, eh, em, allowCalls ->
                        viewModel.setQuietHours(enabled, sh, sm, eh, em, allowCalls)
                    },
                    vipOnly = vipOnly,
                    onToggleVipOnly = { viewModel.setVipContactsOnly(it) },
                    vipList = vipList,
                    onAddVip = { viewModel.addVipContact(it) },
                    onRemoveVip = { viewModel.removeVipContact(it) },
                    maxMessageLength = settings.maxMessageLength,
                    onUpdateMaxLength = { viewModel.setMaxMessageLength(it) },
                    onSendTest = { cat, app, title, text ->
                        viewModel.sendTestNotification(cat, app, title, text)
                    }
                )
            }
        }
    }
}

// ============================================================================
// TAB 1: TIMELINE TAB
// ============================================================================
@Composable
private fun TimelineTabContent(
    historyList: List<NotificationHistoryItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: Int?,
    onCategorySelect: (Int?) -> Unit,
    unreadCount: Int,
    onMarkAllAsRead: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteNotification: (Long) -> Unit,
    isListenerGranted: Boolean,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Notification Access Warning Banner if not granted
        if (!isListenerGranted) {
            item {
                VedaCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AlertRed
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PERMISSION REQUIRED",
                                    style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AlertRed
                                )
                                Text(
                                    text = "Android Notification Access is disabled. Tap to enable.",
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        VedaSecondaryButton(
                            text = "GRANT",
                            onClick = onOpenSettings,
                            modifier = Modifier.width(90.dp)
                        )
                    }
                }
            }
        }

        // Search and Category Filter Row
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VedaRadii.sm)),
                placeholder = {
                    Text(
                        text = "Search notification timeline...",
                        style = Typography.bodyMedium,
                        color = TextMuted
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
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = StrokeSubtle,
                    focusedContainerColor = CanvasMidnightNavy,
                    unfocusedContainerColor = CanvasMidnightNavy,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Category Filter Chips
        item {
            val categories = listOf(
                null to "ALL",
                BleProtocol.CATEGORY_MESSAGING to "MESSAGES",
                BleProtocol.CATEGORY_INCOMING_CALL to "CALLS",
                BleProtocol.CATEGORY_EMAIL to "EMAIL",
                BleProtocol.CATEGORY_CALENDAR to "CALENDAR",
                BleProtocol.CATEGORY_NORMAL to "APPS"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (catId, label) ->
                    val isSelected = selectedCategory == catId
                    val bg = if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceElevated
                    val border = if (isSelected) AccentCyan else StrokeSubtle
                    val textCol = if (isSelected) AccentCyan else TextSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VedaRadii.pill))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(VedaRadii.pill))
                            .clickable { onCategorySelect(catId) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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
        }

        // Timeline Actions Bar (Unread count, Mark Read, Clear)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${historyList.size} ALERTS LOGGED",
                        style = Typography.labelSmall,
                        color = TextTertiary
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(VedaRadii.pill))
                                .background(AlertRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$unreadCount UNREAD",
                                style = Typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unreadCount > 0) {
                        Text(
                            text = "MARK READ",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan,
                            modifier = Modifier
                                .clickable { onMarkAllAsRead() }
                                .padding(4.dp)
                        )
                    }
                    if (historyList.isNotEmpty()) {
                        Text(
                            text = "CLEAR ALL",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { onClearHistory() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // Timeline List Items or Empty State
        if (historyList.isEmpty()) {
            item {
                VedaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO ALERTS RECORDED",
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Incoming phone notifications will be tracked here in real-time.",
                            style = Typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(historyList, key = { it.id }) { item ->
                NotificationTimelineCard(
                    item = item,
                    onDelete = { onDeleteNotification(item.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationTimelineCard(
    item: NotificationHistoryItem,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val categoryColor = when (item.category) {
        BleProtocol.CATEGORY_MESSAGING -> AccentCyan
        BleProtocol.CATEGORY_INCOMING_CALL, BleProtocol.CATEGORY_MISSED_CALL -> WarningAmber
        BleProtocol.CATEGORY_EMAIL -> AccentViolet
        BleProtocol.CATEGORY_CALENDAR -> StatusGreen
        else -> TextSecondary
    }

    val categoryIcon = when (item.category) {
        BleProtocol.CATEGORY_MESSAGING -> Icons.Default.Message
        BleProtocol.CATEGORY_INCOMING_CALL, BleProtocol.CATEGORY_MISSED_CALL -> Icons.Default.Phone
        BleProtocol.CATEGORY_EMAIL -> Icons.Default.Email
        BleProtocol.CATEGORY_CALENDAR -> Icons.Default.DateRange
        else -> Icons.Default.Notifications
    }

    VedaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        borderColor = if (item.wasForwarded) StrokeSubtle else AlertRed.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: App Icon/Category + App Name + Time + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.appName,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (item.isVip) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "VIP",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = item.categoryName.uppercase(),
                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                            color = categoryColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeUtils.formatSyncTime(item.timestamp),
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VedaRadii.pill))
                            .background(
                                if (item.wasForwarded) StatusGreen.copy(alpha = 0.15f)
                                else AlertRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.wasForwarded) "SENT" else "MUTED",
                            style = Typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (item.wasForwarded) StatusGreen else AlertRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    style = Typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }

            // Message Body (truncated or expanded)
            if (item.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.message,
                    style = Typography.bodySmall,
                    color = TextSecondary,
                    maxLines = if (expanded) 10 else 2
                )
            }

            // Expanded detail: Rejection Reason & Delete Action
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    if (!item.wasForwarded && !item.rejectionReason.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VedaRadii.xs))
                                .background(CanvasDeepBlack)
                                .border(1.dp, AlertRed.copy(alpha = 0.3f), RoundedCornerShape(VedaRadii.xs))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "FILTER REASON: ${item.rejectionReason}",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = AlertRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: APP FILTER TAB
// ============================================================================
@Composable
private fun AppFilterTabContent(
    installedApps: List<AppNotificationItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    onToggleApp: (String, Boolean) -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onEnablePopular: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VedaRadii.sm)),
                placeholder = {
                    Text(
                        text = "Search installed applications...",
                        style = Typography.bodyMedium,
                        color = TextMuted
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
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = StrokeSubtle,
                    focusedContainerColor = CanvasMidnightNavy,
                    unfocusedContainerColor = CanvasMidnightNavy,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Quick Batch Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                        .clickable { onEnablePopular() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "POPULAR ONLY",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = AccentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                        .clickable { onEnableAll() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ENABLE ALL",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = StatusGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(VedaRadii.sm))
                        .background(SurfaceElevated)
                        .border(1.dp, StrokeSubtle, RoundedCornerShape(VedaRadii.sm))
                        .clickable { onDisableAll() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DISABLE ALL",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = TextMuted
                    )
                }
            }
        }

        // Stats strip
        item {
            val enabledCount = installedApps.count { it.isEnabled }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$enabledCount OF ${installedApps.size} APPS FORWARDING",
                    style = Typography.labelSmall,
                    color = TextTertiary
                )
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(32.dp))
                }
            }
        } else if (installedApps.isEmpty()) {
            item {
                VedaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Text(
                        text = "No applications matched \"$searchQuery\"",
                        style = Typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(installedApps, key = { it.packageName }) { appItem ->
                AppFilterRow(
                    appItem = appItem,
                    onToggle = { onToggleApp(appItem.packageName, it) }
                )
            }
        }
    }
}

@Composable
private fun AppFilterRow(
    appItem: AppNotificationItem,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(appItem.packageName) {
        try {
            val iconDrawable = context.packageManager.getApplicationIcon(appItem.packageName)
            iconDrawable.toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    VedaCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (appItem.isEnabled) AccentCyan.copy(alpha = 0.3f) else StrokeSubtle
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
                    Image(
                        bitmap = appIcon,
                        contentDescription = appItem.appName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appItem.appName.take(1).uppercase(),
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appItem.appName,
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        if (appItem.isPopular) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VedaRadii.xs))
                                    .background(AccentCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "POPULAR",
                                    style = Typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                    Text(
                        text = appItem.packageName,
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            Switch(
                checked = appItem.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentCyan,
                    checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceElevated
                )
            )
        }
    }
}

// ============================================================================
// TAB 3: POLICIES & PRIVACY TAB
// ============================================================================
@Composable
private fun PoliciesTabContent(
    isListenerGranted: Boolean,
    onOpenSettings: () -> Unit,
    isMasterEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    privacyMode: Boolean,
    onTogglePrivacy: (Boolean) -> Unit,
    hideSender: Boolean,
    onToggleHideSender: (Boolean) -> Unit,
    quietHoursEnabled: Boolean,
    quietStartH: Int,
    quietStartM: Int,
    quietEndH: Int,
    quietEndM: Int,
    quietAllowCalls: Boolean,
    onUpdateQuietHours: (Boolean, Int, Int, Int, Int, Boolean) -> Unit,
    vipOnly: Boolean,
    onToggleVipOnly: (Boolean) -> Unit,
    vipList: Set<String>,
    onAddVip: (String) -> Unit,
    onRemoveVip: (String) -> Unit,
    maxMessageLength: Int,
    onUpdateMaxLength: (Int) -> Unit,
    onSendTest: (Int, String, String, String) -> Unit
) {
    var newVipName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VedaSpacing.md, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notification Access Status Card
        item {
            VedaCard(
                borderColor = if (isListenerGranted) StatusGreen.copy(alpha = 0.4f) else AlertRed
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isListenerGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isListenerGranted) StatusGreen else AlertRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Notification Listener Service",
                                style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (isListenerGranted) "Active and receiving OS events" else "Access required for alert sync",
                                style = Typography.bodySmall,
                                color = if (isListenerGranted) StatusGreen else AlertRed
                            )
                        }
                    }

                    VedaSecondaryButton(
                        text = if (isListenerGranted) "RECHECK" else "ENABLE",
                        onClick = onOpenSettings,
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
        }

        // Master Switch
        item {
            VedaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Master Alert Forwarding",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Enable or pause all notification transmissions to the watch",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }
            }
        }

        // Privacy Mode Card
        item {
            VedaCard {
                VedaCardHeader(
                    label = "Privacy & Previews",
                    statusText = if (privacyMode) "PROTECTED" else "STANDARD",
                    statusColor = if (privacyMode) AccentCyan else TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smartwatch Privacy Mode",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Replaces message body with \"[Hidden for Privacy]\" on watch screen",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = privacyMode,
                        onCheckedChange = onTogglePrivacy,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }

                if (privacyMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mask Sender / Contact Name",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Displays generic \"VEDA Notification\" header on watch",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = hideSender,
                            onCheckedChange = onToggleHideSender,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Quiet Hours / DND Schedule Card
        item {
            VedaCard {
                VedaCardHeader(
                    label = "Quiet Hours / DND Schedule",
                    statusText = if (quietHoursEnabled) "ACTIVE" else "DISABLED",
                    statusColor = if (quietHoursEnabled) WarningAmber else TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Scheduled Quiet Hours",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Silences non-emergency notifications during specified interval",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = quietHoursEnabled,
                        onCheckedChange = {
                            onUpdateQuietHours(it, quietStartH, quietStartM, quietEndH, quietEndM, quietAllowCalls)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WarningAmber,
                            checkedTrackColor = WarningAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }

                if (quietHoursEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(CanvasDeepBlack)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("START TIME", style = Typography.labelSmall, color = TextTertiary)
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", quietStartH, quietStartM),
                                    style = Typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = WarningAmber
                                )
                            }
                            Text("—", style = Typography.titleLarge, color = TextMuted)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("END TIME", style = Typography.labelSmall, color = TextTertiary)
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", quietEndH, quietEndM),
                                    style = Typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = WarningAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Phone Calls Through",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Emergency phone rings will bypass quiet hours",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = quietAllowCalls,
                            onCheckedChange = {
                                onUpdateQuietHours(quietHoursEnabled, quietStartH, quietStartM, quietEndH, quietEndM, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusGreen,
                                checkedTrackColor = StatusGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // VIP Contacts Whitelist Card
        item {
            VedaCard {
                VedaCardHeader(
                    label = "VIP Contact Whitelist",
                    statusText = if (vipOnly) "VIP ONLY" else "ALL CONTACTS",
                    statusColor = if (vipOnly) WarningAmber else AccentCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VIP Contacts Only Mode",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Forward alerts ONLY from contacts in your VIP list",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = vipOnly,
                        onCheckedChange = onToggleVipOnly,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WarningAmber,
                            checkedTrackColor = WarningAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add VIP Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newVipName,
                        onValueChange = { newVipName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Enter contact name or number...", style = Typography.bodySmall, color = TextMuted)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = StrokeSubtle,
                            focusedContainerColor = CanvasDeepBlack,
                            unfocusedContainerColor = CanvasDeepBlack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newVipName.isNotBlank()) {
                                onAddVip(newVipName)
                                newVipName = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(VedaRadii.sm))
                            .background(AccentCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = CanvasDeepBlack
                        )
                    }
                }

                // VIP List Chips
                if (vipList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        vipList.forEach { vip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(VedaRadii.pill))
                                    .background(SurfaceElevated)
                                    .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(VedaRadii.pill))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = vip,
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveVip(vip) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Payload Max Length Slider
        item {
            VedaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Max Message Length",
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "$maxMessageLength chars",
                        style = Typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls payload truncation per firmware MTU buffer limit (180 chars max)",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
                Slider(
                    value = maxMessageLength.toFloat(),
                    onValueChange = { onUpdateMaxLength(it.toInt()) },
                    valueRange = 20f..180f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StrokeSubtle
                    )
                )
            }
        }

        // BLE Test Notification Transmitter
        item {
            VedaCard {
                VedaCardHeader(
                    label = "Watch Link Test Transmitter",
                    statusText = "SIMULATOR",
                    statusColor = AccentCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Send test notification packets directly to your smartwatch over BLE:",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VedaSecondaryButton(
                        text = "Message",
                        icon = Icons.Default.Message,
                        onClick = {
                            onSendTest(
                                BleProtocol.CATEGORY_MESSAGING,
                                "WhatsApp",
                                "Anish Veda",
                                "Hey! Checking in on the VEDA V2 project status."
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VedaSecondaryButton(
                        text = "Call",
                        icon = Icons.Default.Phone,
                        onClick = {
                            onSendTest(
                                BleProtocol.CATEGORY_INCOMING_CALL,
                                "Phone",
                                "Incoming Call",
                                "Dr. Vikram Sarabhai"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VedaSecondaryButton(
                        text = "Email",
                        icon = Icons.Default.Email,
                        onClick = {
                            onSendTest(
                                BleProtocol.CATEGORY_EMAIL,
                                "Gmail",
                                "Security Alert",
                                "New sign-in detected on VEDA Core terminal."
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VedaSecondaryButton(
                        text = "Calendar",
                        icon = Icons.Default.DateRange,
                        onClick = {
                            onSendTest(
                                BleProtocol.CATEGORY_CALENDAR,
                                "Calendar",
                                "Meeting Reminder",
                                "Embedded Systems Demo starts in 15 minutes."
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
