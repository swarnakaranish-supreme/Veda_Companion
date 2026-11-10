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
            accentColorIndex = prefs.getInt(KEY_ACCENT_COLOR, 0)
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
    }
}
