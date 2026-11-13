package com.anish.veda.notifications

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class AppNotificationItem(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val isPopular: Boolean = false
)

class NotificationSettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_notification_prefs", Context.MODE_PRIVATE)

    // Master switch
    private val _isMasterEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_ENABLED, true))
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    // Allowed Apps
    private val _allowedAppsMap = MutableStateFlow<Map<String, Boolean>>(loadAppPreferences())
    val allowedAppsMap: StateFlow<Map<String, Boolean>> = _allowedAppsMap.asStateFlow()

    // Privacy Mode
    private val _privacyModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_MODE, false))
    val privacyModeEnabled: StateFlow<Boolean> = _privacyModeEnabled.asStateFlow()

    private val _hideSenderInPrivacy = MutableStateFlow(prefs.getBoolean(KEY_HIDE_SENDER, false))
    val hideSenderInPrivacy: StateFlow<Boolean> = _hideSenderInPrivacy.asStateFlow()

    // Quiet Hours / DND Schedule
    private val _quietHoursEnabled = MutableStateFlow(prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false))
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private val _quietHoursStartHour = MutableStateFlow(prefs.getInt(KEY_QUIET_START_H, 22))
    val quietHoursStartHour: StateFlow<Int> = _quietHoursStartHour.asStateFlow()

    private val _quietHoursStartMinute = MutableStateFlow(prefs.getInt(KEY_QUIET_START_M, 0))
    val quietHoursStartMinute: StateFlow<Int> = _quietHoursStartMinute.asStateFlow()

    private val _quietHoursEndHour = MutableStateFlow(prefs.getInt(KEY_QUIET_END_H, 7))
    val quietHoursEndHour: StateFlow<Int> = _quietHoursEndHour.asStateFlow()

    private val _quietHoursEndMinute = MutableStateFlow(prefs.getInt(KEY_QUIET_END_M, 0))
    val quietHoursEndMinute: StateFlow<Int> = _quietHoursEndMinute.asStateFlow()

    private val _quietHoursAllowCalls = MutableStateFlow(prefs.getBoolean(KEY_QUIET_ALLOW_CALLS, true))
    val quietHoursAllowCalls: StateFlow<Boolean> = _quietHoursAllowCalls.asStateFlow()

    // VIP Contacts Filter
    private val _vipContactsOnly = MutableStateFlow(prefs.getBoolean(KEY_VIP_ONLY, false))
    val vipContactsOnly: StateFlow<Boolean> = _vipContactsOnly.asStateFlow()

    private val _vipContacts = MutableStateFlow<Set<String>>(
        prefs.getStringSet(KEY_VIP_CONTACTS, emptySet()) ?: emptySet()
    )
    val vipContacts: StateFlow<Set<String>> = _vipContacts.asStateFlow()

    companion object {
        private const val KEY_MASTER_ENABLED = "master_notifications_enabled"
        private const val PREF_PREFIX_APP = "app_enabled_"
        private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"
        private const val KEY_HIDE_SENDER = "hide_sender_in_privacy"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_H = "quiet_start_hour"
        private const val KEY_QUIET_START_M = "quiet_start_minute"
        private const val KEY_QUIET_END_H = "quiet_end_hour"
        private const val KEY_QUIET_END_M = "quiet_end_minute"
        private const val KEY_QUIET_ALLOW_CALLS = "quiet_allow_calls"
        private const val KEY_VIP_ONLY = "vip_contacts_only"
        private const val KEY_VIP_CONTACTS = "vip_contacts_set"

        // Standard communication apps highlighted by default
        val POPULAR_PACKAGES = listOf(
            "com.whatsapp",                         // WhatsApp
            "com.whatsapp.w4b",                     // WhatsApp Business
            "com.instagram.android",                // Instagram
            "com.facebook.katana",                  // Facebook
            "com.facebook.orca",                    // Facebook Messenger
            "org.telegram.messenger",               // Telegram
            "com.google.android.gm",                // Gmail
            "com.google.android.apps.messaging",    // Google Messages (SMS)
            "com.android.mms",                      // AOSP SMS
            "com.samsung.android.messaging"         // Samsung SMS
        )
    }

    private fun loadAppPreferences(): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        for (pkg in POPULAR_PACKAGES) {
            map[pkg] = prefs.getBoolean(PREF_PREFIX_APP + pkg, true)
        }
        for (key in prefs.all.keys) {
            if (key.startsWith(PREF_PREFIX_APP)) {
                val pkg = key.removePrefix(PREF_PREFIX_APP)
                map[pkg] = prefs.getBoolean(key, true)
            }
        }
        return map
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _isMasterEnabled.value = enabled
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean(PREF_PREFIX_APP + packageName, enabled).apply()
        val updated = _allowedAppsMap.value.toMutableMap()
        updated[packageName] = enabled
        _allowedAppsMap.value = updated
    }

    fun setAllApps(apps: List<AppNotificationItem>, enabled: Boolean) {
        val editor = prefs.edit()
        val updated = _allowedAppsMap.value.toMutableMap()
        for (app in apps) {
            editor.putBoolean(PREF_PREFIX_APP + app.packageName, enabled)
            updated[app.packageName] = enabled
        }
        editor.apply()
        _allowedAppsMap.value = updated
    }

    fun enablePopularAppsOnly(apps: List<AppNotificationItem>) {
        val editor = prefs.edit()
        val updated = _allowedAppsMap.value.toMutableMap()
        for (app in apps) {
            val enable = app.isPopular
            editor.putBoolean(PREF_PREFIX_APP + app.packageName, enable)
            updated[app.packageName] = enable
        }
        editor.apply()
        _allowedAppsMap.value = updated
    }

    fun isAppAllowed(packageName: String): Boolean {
        if (!_isMasterEnabled.value) return false
        val customVal = _allowedAppsMap.value[packageName]
        return customVal ?: true
    }

    // Privacy Mode
    fun setPrivacyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
        _privacyModeEnabled.value = enabled
    }

    fun setHideSenderInPrivacy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_SENDER, enabled).apply()
        _hideSenderInPrivacy.value = enabled
    }

    // Quiet Hours
    fun setQuietHours(
        enabled: Boolean,
        startH: Int,
        startM: Int,
        endH: Int,
        endM: Int,
        allowCalls: Boolean
    ) {
        prefs.edit()
            .putBoolean(KEY_QUIET_HOURS_ENABLED, enabled)
            .putInt(KEY_QUIET_START_H, startH.coerceIn(0, 23))
            .putInt(KEY_QUIET_START_M, startM.coerceIn(0, 59))
            .putInt(KEY_QUIET_END_H, endH.coerceIn(0, 23))
            .putInt(KEY_QUIET_END_M, endM.coerceIn(0, 59))
            .putBoolean(KEY_QUIET_ALLOW_CALLS, allowCalls)
            .apply()

        _quietHoursEnabled.value = enabled
        _quietHoursStartHour.value = startH
        _quietHoursStartMinute.value = startM
        _quietHoursEndHour.value = endH
        _quietHoursEndMinute.value = endM
        _quietHoursAllowCalls.value = allowCalls
    }

    fun isQuietHoursActive(calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!_quietHoursEnabled.value) return false

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = _quietHoursStartHour.value * 60 + _quietHoursStartMinute.value
        val endMinutes = _quietHoursEndHour.value * 60 + _quietHoursEndMinute.value

        return if (startMinutes <= endMinutes) {
            // E.g., 08:00 to 17:00
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight, e.g., 22:00 to 07:00
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun getQuietHoursRangeText(): String {
        return String.format(
            Locale.US,
            "%02d:%02d - %02d:%02d",
            _quietHoursStartHour.value,
            _quietHoursStartMinute.value,
            _quietHoursEndHour.value,
            _quietHoursEndMinute.value
        )
    }

    // VIP Contacts
    fun setVipContactsOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIP_ONLY, enabled).apply()
        _vipContactsOnly.value = enabled
    }

    fun addVipContact(contact: String) {
        val trimmed = contact.trim()
        if (trimmed.isEmpty()) return
        val current = _vipContacts.value.toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_VIP_CONTACTS, current).apply()
        _vipContacts.value = current
    }

    fun removeVipContact(contact: String) {
        val current = _vipContacts.value.toMutableSet()
        current.remove(contact)
        prefs.edit().putStringSet(KEY_VIP_CONTACTS, current).apply()
        _vipContacts.value = current
    }

    fun isContactVip(senderOrTitle: String): Boolean {
        if (senderOrTitle.isBlank()) return false
        val vips = _vipContacts.value
        val lower = senderOrTitle.lowercase()
        return vips.any { vip ->
            val vipLower = vip.lowercase()
            lower.contains(vipLower) || vipLower.contains(lower)
        }
    }

    suspend fun getInstalledApps(): List<AppNotificationItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val currentPrefs = _allowedAppsMap.value

        val items = mutableListOf<AppNotificationItem>()
        for (appInfo in installed) {
            val pkg = appInfo.packageName
            if (pkg == context.packageName) continue

            val isLaunchable = pm.getLaunchIntentForPackage(pkg) != null
            val isPopular = POPULAR_PACKAGES.contains(pkg)

            if (isLaunchable || isPopular) {
                val name = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast('.')
                }
                val isEnabled = currentPrefs[pkg] ?: true

                items.add(
                    AppNotificationItem(
                        packageName = pkg,
                        appName = name,
                        isEnabled = isEnabled,
                        isPopular = isPopular
                    )
                )
            }
        }

        items.sortWith(
            compareByDescending<AppNotificationItem> { it.isPopular }
                .thenBy { it.appName.lowercase() }
        )
        items
    }
}
