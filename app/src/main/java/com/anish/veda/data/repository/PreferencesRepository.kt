package com.anish.veda.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.anish.veda.data.model.VedaSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_companion_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<VedaSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): VedaSettings {
        return VedaSettings(
            lastDeviceAddress = prefs.getString(KEY_LAST_ADDRESS, null),
            lastDeviceName = prefs.getString(KEY_LAST_NAME, "ANISH-WATCH"),
            autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, true),
            forwardNotifications = prefs.getBoolean(KEY_FORWARD_NOTIFICATIONS, true),
            forwardCalls = prefs.getBoolean(KEY_FORWARD_CALLS, true),
            forwardMissedCalls = prefs.getBoolean(KEY_FORWARD_MISSED_CALLS, true),
            showCallerName = prefs.getBoolean(KEY_SHOW_CALLER_NAME, true),
            resolveCallerNames = prefs.getBoolean(KEY_RESOLVE_CALLER_NAMES, true),
            autoSyncTimeOnConnect = prefs.getBoolean(KEY_AUTO_SYNC_TIME, true),
            periodicTimeSync = prefs.getBoolean(KEY_PERIODIC_TIME_SYNC, true),
            syncIntervalHours = prefs.getInt(KEY_SYNC_INTERVAL, 2),
            maxMessageLength = prefs.getInt(KEY_MAX_MSG_LEN, 120),
            allowedApps = prefs.getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet(),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TS, 0L),
            notificationForwardedCount = prefs.getInt(KEY_NOTIF_COUNT, 0),
            callsForwardedCount = prefs.getInt(KEY_CALL_COUNT, 0),
            accentColorIndex = prefs.getInt(KEY_ACCENT_COLOR, 0),
            themePreset = prefs.getString(KEY_THEME_PRESET, "DEFAULT") ?: "DEFAULT",
            fontScaleIndex = prefs.getInt(KEY_FONT_SCALE, 1),
            fontFamilyChoice = prefs.getString(KEY_FONT_FAMILY, "DEFAULT") ?: "DEFAULT",
            interfaceStyle = prefs.getString(KEY_INTERFACE_STYLE, "FUTURISTIC_HUD") ?: "FUTURISTIC_HUD",
            reducedMotion = prefs.getBoolean(KEY_REDUCED_MOTION, false),
            startingDialMode = prefs.getInt(KEY_STARTING_DIAL_MODE, 0),
            dashboardWidgetHealth = prefs.getBoolean(KEY_WIDGET_HEALTH, true),
            dashboardWidgetAi = prefs.getBoolean(KEY_WIDGET_AI, true),
            dashboardWidgetQuickActions = prefs.getBoolean(KEY_WIDGET_QUICK_ACTIONS, true),
            quickAction1 = prefs.getString(KEY_QUICK_ACTION_1, "FIND_WATCH") ?: "FIND_WATCH",
            quickAction2 = prefs.getString(KEY_QUICK_ACTION_2, "WATCH_STUDIO") ?: "WATCH_STUDIO",
            quickAction3 = prefs.getString(KEY_QUICK_ACTION_3, "DND") ?: "DND"
        )
    }

    @Synchronized
    fun updateSettings(transform: (VedaSettings) -> VedaSettings) {
        val current = _settingsFlow.value
        val updated = transform(current)
        prefs.edit().apply {
            putString(KEY_LAST_ADDRESS, updated.lastDeviceAddress)
            putString(KEY_LAST_NAME, updated.lastDeviceName)
            putBoolean(KEY_AUTO_CONNECT, updated.autoConnect)
            putBoolean(KEY_FORWARD_NOTIFICATIONS, updated.forwardNotifications)
            putBoolean(KEY_FORWARD_CALLS, updated.forwardCalls)
            putBoolean(KEY_FORWARD_MISSED_CALLS, updated.forwardMissedCalls)
            putBoolean(KEY_SHOW_CALLER_NAME, updated.showCallerName)
            putBoolean(KEY_RESOLVE_CALLER_NAMES, updated.resolveCallerNames)
            putBoolean(KEY_AUTO_SYNC_TIME, updated.autoSyncTimeOnConnect)
            putBoolean(KEY_PERIODIC_TIME_SYNC, updated.periodicTimeSync)
            putInt(KEY_SYNC_INTERVAL, updated.syncIntervalHours)
            putInt(KEY_MAX_MSG_LEN, updated.maxMessageLength)
            putStringSet(KEY_ALLOWED_APPS, updated.allowedApps)
            putLong(KEY_LAST_SYNC_TS, updated.lastSyncTimestamp)
            putInt(KEY_NOTIF_COUNT, updated.notificationForwardedCount)
            putInt(KEY_CALL_COUNT, updated.callsForwardedCount)
            putInt(KEY_ACCENT_COLOR, updated.accentColorIndex)
            putString(KEY_THEME_PRESET, updated.themePreset)
            putInt(KEY_FONT_SCALE, updated.fontScaleIndex)
            putString(KEY_FONT_FAMILY, updated.fontFamilyChoice)
            putString(KEY_INTERFACE_STYLE, updated.interfaceStyle)
            putBoolean(KEY_REDUCED_MOTION, updated.reducedMotion)
            putInt(KEY_STARTING_DIAL_MODE, updated.startingDialMode)
            putBoolean(KEY_WIDGET_HEALTH, updated.dashboardWidgetHealth)
            putBoolean(KEY_WIDGET_AI, updated.dashboardWidgetAi)
            putBoolean(KEY_WIDGET_QUICK_ACTIONS, updated.dashboardWidgetQuickActions)
            putString(KEY_QUICK_ACTION_1, updated.quickAction1)
            putString(KEY_QUICK_ACTION_2, updated.quickAction2)
            putString(KEY_QUICK_ACTION_3, updated.quickAction3)
        }.apply()
        _settingsFlow.value = updated
    }

    fun recordSyncTime(timestamp: Long = System.currentTimeMillis()) {
        updateSettings { it.copy(lastSyncTimestamp = timestamp) }
    }

    fun incrementNotificationCount() {
        updateSettings { it.copy(notificationForwardedCount = it.notificationForwardedCount + 1) }
    }

    fun incrementCallCount() {
        updateSettings { it.copy(callsForwardedCount = it.callsForwardedCount + 1) }
    }

    fun setLastConnectedDevice(address: String, name: String?) {
        updateSettings {
            it.copy(
                lastDeviceAddress = address,
                lastDeviceName = name ?: it.lastDeviceName ?: "ANISH-WATCH"
            )
        }
    }

    companion object {
        private const val KEY_LAST_ADDRESS = "last_device_address"
        private const val KEY_LAST_NAME = "last_device_name"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_FORWARD_NOTIFICATIONS = "forward_notifications"
        private const val KEY_FORWARD_CALLS = "forward_calls"
        private const val KEY_FORWARD_MISSED_CALLS = "forward_missed_calls"
        private const val KEY_SHOW_CALLER_NAME = "show_caller_name"
        private const val KEY_RESOLVE_CALLER_NAMES = "resolve_caller_names"
        private const val KEY_AUTO_SYNC_TIME = "auto_sync_time"
        private const val KEY_PERIODIC_TIME_SYNC = "periodic_time_sync"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_MAX_MSG_LEN = "max_msg_len"
        private const val KEY_ALLOWED_APPS = "allowed_apps"
        private const val KEY_LAST_SYNC_TS = "last_sync_timestamp"
        private const val KEY_NOTIF_COUNT = "notification_count"
        private const val KEY_CALL_COUNT = "call_count"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_INTERFACE_STYLE = "interface_style"
        private const val KEY_REDUCED_MOTION = "reduced_motion"
        private const val KEY_STARTING_DIAL_MODE = "starting_dial_mode"
        private const val KEY_WIDGET_HEALTH = "widget_health"
        private const val KEY_WIDGET_AI = "widget_ai"
        private const val KEY_WIDGET_QUICK_ACTIONS = "widget_quick_actions"
        private const val KEY_QUICK_ACTION_1 = "quick_action_1"
        private const val KEY_QUICK_ACTION_2 = "quick_action_2"
        private const val KEY_QUICK_ACTION_3 = "quick_action_3"
    }
}
