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

    private val _customFace = MutableStateFlow(com.anish.veda.data.model.CustomWatchFaceState())
    val customFace: StateFlow<com.anish.veda.data.model.CustomWatchFaceState> = _customFace.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSuccessNotice = MutableStateFlow<String?>(null)
    val syncSuccessNotice: StateFlow<String?> = _syncSuccessNotice.asStateFlow()

    private val _notifTimeoutSec = MutableStateFlow(10)
    val notifTimeoutSec: StateFlow<Int> = _notifTimeoutSec.asStateFlow()

    fun selectFace(index: Int) {
        _config.value = _config.value.copy(selectedFaceIndex = index.coerceIn(0, 8))
        // Auto-send face index change immediately if connected
        viewModelScope.launch {
            if (connectionState.value.isConnected) {
                bleManager.sendPayloadDirect(BleProtocol.buildSelectFacePacket(index))
            }
        }
    }

    fun updateCustomStyle(style: Int) {
        _customFace.value = _customFace.value.copy(style = style.coerceIn(0, 3))
    }

    fun updateCustomColor(rgb565: Int, name: String) {
        _customFace.value = _customFace.value.copy(accentColorRgb565 = rgb565, colorName = name)
    }

    fun toggleCustomHeartRate(show: Boolean) {
        _customFace.value = _customFace.value.copy(showHeartRate = show)
    }

    fun toggleCustomSpO2(show: Boolean) {
        _customFace.value = _customFace.value.copy(showSpO2 = show)
    }

    fun toggleCustomDate(show: Boolean) {
        _customFace.value = _customFace.value.copy(showDate = show)
    }

    fun toggleCustomSeconds(show: Boolean) {
        _customFace.value = _customFace.value.copy(showSeconds = show)
    }

    fun toggleCustomStatus(show: Boolean) {
        _customFace.value = _customFace.value.copy(showStatus = show)
    }

    fun updateCustomLabel(label: String) {
        _customFace.value = _customFace.value.copy(customLabel = label.take(15))
    }

    fun setNotificationTimeout(seconds: Int) {
        val safeSec = seconds.coerceIn(3, 60)
        _notifTimeoutSec.value = safeSec
        viewModelScope.launch {
            if (connectionState.value.isConnected) {
                bleManager.sendPayloadDirect(BleProtocol.buildNotificationTimeoutPacket(safeSec))
            }
        }
    }

    fun applyCustomFaceToWatch() {
        viewModelScope.launch {
            if (!connectionState.value.isConnected) {
                _syncSuccessNotice.value = "Watch disconnected. Connect first."
                return@launch
            }
            val cf = _customFace.value
            val packet = BleProtocol.buildCustomFacePacket(
                style = cf.style,
                accentColorRgb565 = cf.accentColorRgb565,
                widgetsMask = cf.calculateWidgetsMask(),
                customLabel = cf.customLabel
            )
            bleManager.sendPayloadDirect(packet)
            _config.value = _config.value.copy(selectedFaceIndex = 8)
            logRepository.log("WATCH_STUDIO", "Custom watch face '${cf.customLabel}' applied to physical watch.")
            _syncSuccessNotice.value = "Custom Watch Face Applied to Watch!"
            delay(3000)
            _syncSuccessNotice.value = null
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
