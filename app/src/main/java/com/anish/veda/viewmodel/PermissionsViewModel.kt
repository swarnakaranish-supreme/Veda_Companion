package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionCheckItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isCritical: Boolean = true
)

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _permissionStates = MutableStateFlow<List<PermissionCheckItem>>(emptyList())
    val permissionStates: StateFlow<List<PermissionCheckItem>> = _permissionStates.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val context = getApplication<Application>()
        val hasBt = PermissionUtils.hasBluetoothPermissions(context)
        val hasNotifListener = PermissionUtils.isNotificationListenerEnabled(context)
        val hasCall = PermissionUtils.hasCallPermissions(context)
        val hasBattery = PermissionUtils.isIgnoringBatteryOptimizations(context)
        val hasPostNotif = PermissionUtils.hasPostNotificationPermission(context)

        _permissionStates.value = listOf(
            PermissionCheckItem(
                id = "bluetooth",
                title = "Bluetooth & Nearby Scanning",
                description = "Required to discover and exchange GATT commands with your ESP32-C3 watch.",
                isGranted = hasBt,
                isCritical = true
            ),
            PermissionCheckItem(
                id = "notification_listener",
                title = "Notification Interception Access",
                description = "Enables forwarding messages, WhatsApp, and app alerts directly to the watch screen.",
                isGranted = hasNotifListener,
                isCritical = true
            ),
            PermissionCheckItem(
                id = "calls",
                title = "Phone & Call State",
                description = "Enables real-time incoming call and missed call alerts on the watch display.",
                isGranted = hasCall,
                isCritical = false
            ),
            PermissionCheckItem(
                id = "battery",
                title = "Background Power Optimization Exemption",
                description = "Prevents Android from killing the BLE companion service when your phone is in standby.",
                isGranted = hasBattery,
                isCritical = false
            ),
            PermissionCheckItem(
                id = "post_notif",
                title = "Companion Service Notifications",
                description = "Displays the persistent foreground status notification in your status bar.",
                isGranted = hasPostNotif,
                isCritical = false
            )
        )
    }

    val allCriticalGranted: Boolean
        get() = _permissionStates.value.filter { it.isCritical }.all { it.isGranted }
}
