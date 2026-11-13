package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.WatchConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val bleManager = app.bleManager
    private val logRepository = app.logRepository

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState

    private val _config = MutableStateFlow(WatchConfig())
    val config: StateFlow<WatchConfig> = _config.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSuccessNotice = MutableStateFlow<String?>(null)
    val syncSuccessNotice: StateFlow<String?> = _syncSuccessNotice.asStateFlow()

    fun selectFace(index: Int) {
        _config.value = _config.value.copy(selectedFaceIndex = index.coerceIn(0, 7))
        // Auto-send face index change immediately if connected
        viewModelScope.launch {
            if (connectionState.value.isConnected) {
                bleManager.sendPayloadDirect(BleProtocol.buildSelectFacePacket(index))
            }
        }
    }

    fun setBrightness(percent: Int) {
        _config.value = _config.value.copy(brightnessPercent = percent.coerceIn(0, 100))
    }

    fun setTimeoutSeconds(seconds: Int) {
        _config.value = _config.value.copy(timeoutSeconds = seconds)
    }

    fun set24HourFormat(is24Hour: Boolean) {
        _config.value = _config.value.copy(is24Hour = is24Hour)
    }

    fun setDateFormat(format: String) {
        _config.value = _config.value.copy(dateFormat = format)
    }

    fun syncAllConfiguration() {
        viewModelScope.launch {
            if (!connectionState.value.isConnected) {
                _syncSuccessNotice.value = "Watch disconnected. Connect first."
                return@launch
            }

            _isSyncing.value = true
            val current = _config.value

            // 1. Face index
            bleManager.sendPayloadDirect(BleProtocol.buildSelectFacePacket(current.selectedFaceIndex))
            delay(50)

            // 2. Brightness
            bleManager.sendPayloadDirect(BleProtocol.buildBrightnessPacket(current.brightnessPercent))
            delay(50)

            // 3. Screen timeout
            bleManager.sendPayloadDirect(BleProtocol.buildTimeoutPacket(current.timeoutSeconds))
            delay(50)

            // 4. Time and Date Format
            bleManager.sendPayloadDirect(BleProtocol.buildFormatPacket(current.is24Hour, current.dateFormat))

            logRepository.log("WATCH_STUDIO", "Synchronized configuration to watch successfully.")
            _isSyncing.value = false
            _syncSuccessNotice.value = "Watch face & settings synchronized!"

            delay(3000)
            _syncSuccessNotice.value = null
        }
    }
}
