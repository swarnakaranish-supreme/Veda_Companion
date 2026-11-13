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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val bluetoothManager = app.bleManager
    private val preferencesRepository = app.preferencesRepository
    private val timeSyncManager = app.timeSyncManager
    private val healthRepository = app.healthRepository

    val connectionState: StateFlow<BleConnectionState> = bluetoothManager.connectionState
    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow
    val isSyncingTime: StateFlow<Boolean> = timeSyncManager.isSyncing

    // Real telemetry flows from HealthRepository & BleManager
    private val _batteryPercent = MutableStateFlow<Int?>(null)
    val batteryPercent: StateFlow<Int?> = _batteryPercent.asStateFlow()

    val heartRateBpm: StateFlow<Int?> = healthRepository.liveHeartRate
    val spo2Percent: StateFlow<Int?> = healthRepository.liveSpO2
    val sensorQuality: StateFlow<Int?> = healthRepository.sensorQuality

    private val _isDndActive = MutableStateFlow(false)
    val isDndActive: StateFlow<Boolean> = _isDndActive.asStateFlow()

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
        _batteryPercent.value = null
        healthRepository.stopMeasurement()
    }

    fun syncTime() {
        val context = getApplication<Application>()
        VedaBleForegroundService.syncTime(context)
    }

    /**
     * Find My Watch: Sends high-priority alert vibration to the watch
     */
    fun findMyWatch() {
        viewModelScope.launch {
            if (connectionState.value.isConnected) {
                val packet = BleProtocol.buildNormalNotificationPacket(
                    appName = "VEDA SYSTEM",
                    title = "Find My Watch",
                    message = "Ping alert from phone"
                )
                bluetoothManager.sendPayloadDirect(packet)
            }
        }
    }

    /**
     * Do Not Disturb toggle
     */
    fun toggleDnd() {
        val newState = !_isDndActive.value
        _isDndActive.value = newState
    }

    /**
     * Send test notification
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            val packet = BleProtocol.buildNormalNotificationPacket(
                appName = "VEDA Test",
                title = "Companion Link",
                message = "Watch connection verified successfully!"
            )
            bluetoothManager.sendPayloadDirect(packet)
            preferencesRepository.incrementNotificationCount()
        }
    }
}
