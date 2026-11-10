package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.VedaSettings
import com.anish.veda.services.VedaBleForegroundService
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val bleManager = app.bleManager
    private val preferencesRepository = app.preferencesRepository
    private val timeSyncManager = app.timeSyncManager

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow
    val isSyncingTime: StateFlow<Boolean> = timeSyncManager.isSyncing

    fun connect() {
        val context = getApplication<Application>()
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            return
        }
        val currentSettings = settings.value
        val addr = currentSettings.lastDeviceAddress
        val name = currentSettings.lastDeviceName
        if (!addr.isNullOrBlank()) {
            VedaBleForegroundService.connect(context, addr, name)
        } else {
            VedaBleForegroundService.startScan(context)
        }
    }

    fun disconnect() {
        val context = getApplication<Application>()
        VedaBleForegroundService.disconnect(context)
    }

    fun syncTime() {
        val context = getApplication<Application>()
        VedaBleForegroundService.syncTime(context)
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val packet = BleProtocol.buildNormalNotificationPacket(
                appName = "VEDA Test",
                title = "Companion Link",
                message = "Watch connection verified successfully!"
            )
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementNotificationCount()
        }
    }
}
