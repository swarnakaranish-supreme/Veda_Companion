package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleDeviceModel
import com.anish.veda.data.model.ConnectionHistoryEntry
import com.anish.veda.data.model.DeviceMetadata
import com.anish.veda.data.model.TroubleshootingGuide
import com.anish.veda.services.VedaBleForegroundService
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.flow.StateFlow

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val bleManager = app.bleManager
    private val preferencesRepository = app.preferencesRepository
    private val deviceRepository = app.deviceRepository

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isBluetoothEnabled: StateFlow<Boolean> = bleManager.isBluetoothEnabled

    // VEDA V2 Device Management Flows
    val deviceMetadata: StateFlow<DeviceMetadata> = deviceRepository.deviceMetadata
    val connectionHistory: StateFlow<List<ConnectionHistoryEntry>> = deviceRepository.connectionHistory
    val isAutoReconnectEnabled: StateFlow<Boolean> = deviceRepository.isAutoReconnectEnabled
    val pingLatency: StateFlow<Long?> = deviceRepository.pingLatency
    val isPingInProgress: StateFlow<Boolean> = deviceRepository.isPingInProgress
    val troubleshootingGuides: List<TroubleshootingGuide> = deviceRepository.getTroubleshootingGuides()

    fun startScan() {
        val context = getApplication<Application>()
        if (PermissionUtils.hasBluetoothPermissions(context)) {
            VedaBleForegroundService.startScan(context)
        }
    }

    fun stopScan() {
        val context = getApplication<Application>()
        VedaBleForegroundService.stopScan(context)
    }

    fun connectDevice(device: BleDeviceModel) {
        val context = getApplication<Application>()
        if (PermissionUtils.hasBluetoothPermissions(context)) {
            preferencesRepository.setLastConnectedDevice(device.address, device.name)
            VedaBleForegroundService.connect(context, device.address, device.name)
        }
    }

    fun connectAddress(address: String, name: String? = null) {
        val context = getApplication<Application>()
        if (PermissionUtils.hasBluetoothPermissions(context)) {
            preferencesRepository.setLastConnectedDevice(address, name)
            VedaBleForegroundService.connect(context, address, name)
        }
    }

    fun disconnect() {
        val context = getApplication<Application>()
        VedaBleForegroundService.disconnect(context)
    }

    fun setAutoReconnect(enabled: Boolean) {
        deviceRepository.setAutoReconnect(enabled)
    }

    fun triggerSyncNow() {
        deviceRepository.triggerSyncNow()
    }

    fun pingWatch() {
        deviceRepository.pingWatch()
    }

    fun clearConnectionHistory() {
        deviceRepository.clearConnectionHistory()
    }

    fun exportDiagnostics(): String {
        return deviceRepository.exportDiagnosticsSummary()
    }
}
