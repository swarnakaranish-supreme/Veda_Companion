package com.anish.veda.notifications

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AppNotificationItem(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val isPopular: Boolean = false
)

class NotificationSettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_notification_prefs", Context.MODE_PRIVATE)

    private val _isMasterEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_ENABLED, true))
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _allowedAppsMap = MutableStateFlow<Map<String, Boolean>>(loadAppPreferences())
    val allowedAppsMap: StateFlow<Map<String, Boolean>> = _allowedAppsMap.asStateFlow()

    companion object {
        private const val KEY_MASTER_ENABLED = "master_notifications_enabled"
        private const val PREF_PREFIX_APP = "app_enabled_"

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
        // By default, all popular apps are enabled
        for (pkg in POPULAR_PACKAGES) {
            map[pkg] = prefs.getBoolean(PREF_PREFIX_APP + pkg, true)
        }
        // Load any other custom-configured app preferences
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

    fun isAppAllowed(packageName: String): Boolean {
        if (!_isMasterEnabled.value) return false
        val customVal = _allowedAppsMap.value[packageName]
        return if (customVal != null) {
            customVal
        } else {
            // Default: popular apps enabled by default, others enabled unless user disabled
            true
        }
    }

    suspend fun getInstalledApps(): List<AppNotificationItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val currentPrefs = _allowedAppsMap.value

        val items = mutableListOf<AppNotificationItem>()
        for (appInfo in installed) {
            val pkg = appInfo.packageName
            // Skip self and basic system framework non-launcher packages
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

        // Sort: Popular apps first, then alphabetical
        items.sortWith(
            compareByDescending<AppNotificationItem> { it.isPopular }
                .thenBy { it.appName.lowercase() }
        )
        items
    }
}
