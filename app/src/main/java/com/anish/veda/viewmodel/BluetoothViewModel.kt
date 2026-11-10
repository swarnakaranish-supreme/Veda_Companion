package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleDeviceModel
import com.anish.veda.services.VedaBleForegroundService
import kotlinx.coroutines.flow.StateFlow

import com.anish.veda.utils.PermissionUtils

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val bleManager = app.bleManager
    private val preferencesRepository = app.preferencesRepository

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val isBluetoothEnabled: StateFlow<Boolean> = bleManager.isBluetoothEnabled

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

    fun disconnect() {
        val context = getApplication<Application>()
        VedaBleForegroundService.disconnect(context)
    }
}
