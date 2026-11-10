package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleConstants
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.calls.CallStateManager
import com.anish.veda.calls.CallTelemetryState
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.LogEntry
import com.anish.veda.notifications.NotificationProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiagnosticsTelemetry(
    val bluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val connectedDeviceName: String = "None",
    val connectedDeviceAddress: String = "--",
    val gattState: String = "DISCONNECTED",
    val isServiceDiscovered: Boolean = false,
    val isCharacteristicDiscovered: Boolean = false,
    val negotiatedMtu: Int = BleConstants.DEFAULT_MTU,
    val lastSuccessfulWrite: String = "None yet",
    val lastSuccessfulWriteTime: Long = 0L,
    val lastNotification: String = "None yet",
    val lastNotificationTime: Long = 0L,
    val lastCallEvent: String = "No call events detected",
    val lastCallEventTime: Long = 0L
)

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val logRepository = app.logRepository
    private val bleManager = app.bleManager
    private val preferencesRepository = app.preferencesRepository

    val logs: StateFlow<List<LogEntry>> = logRepository.logsFlow
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val callTelemetry: StateFlow<CallTelemetryState> = CallStateManager.telemetryState

    val errorLogs: StateFlow<List<LogEntry>> = logRepository.logsFlow
        .map { list -> list.filter { it.direction == LogDirection.ERROR } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customPayload = MutableStateFlow("N|0|VEDA|Diagnostics|Hello ESP32-C3!")
    val customPayload: StateFlow<String> = _customPayload.asStateFlow()

    private val _isSendingTest = MutableStateFlow(false)
    val isSendingTest: StateFlow<Boolean> = _isSendingTest.asStateFlow()

    fun getTelemetrySnapshot(): DiagnosticsTelemetry {
        val state = connectionState.value
        val (devName, devAddr) = when (state) {
            is BleConnectionState.Connected -> Pair(state.deviceName, state.deviceAddress)
            is BleConnectionState.Connecting -> Pair(state.deviceName ?: "ANISH-WATCH", state.deviceAddress)
            is BleConnectionState.ConnectedGatt -> Pair(state.deviceName ?: "ANISH-WATCH", state.deviceAddress)
            is BleConnectionState.NegotiatingMtu -> Pair(state.deviceName ?: "ANISH-WATCH", state.deviceAddress)
            is BleConnectionState.DiscoveringServices -> Pair(state.deviceName ?: "ANISH-WATCH", state.deviceAddress)
            is BleConnectionState.Reconnecting -> Pair(state.deviceName ?: "ANISH-WATCH", state.deviceAddress)
            is BleConnectionState.Disconnecting -> Pair("ANISH-WATCH", state.deviceAddress)
            else -> Pair("None", "--")
        }

        val gattStateDesc = when (state) {
            is BleConnectionState.Connected -> "STATE_CONNECTED (Active)"
            is BleConnectionState.Connecting -> "CONNECTING_LE"
            is BleConnectionState.ConnectedGatt -> "GATT_OPEN"
            is BleConnectionState.NegotiatingMtu -> "NEGOTIATING_MTU"
            is BleConnectionState.DiscoveringServices -> "DISCOVERING_SERVICES"
            is BleConnectionState.Reconnecting -> "RECONNECTING (Attempt ${state.attempt}/${state.maxAttempts})"
            is BleConnectionState.Scanning -> "BLE_SCANNING"
            is BleConnectionState.Disconnecting -> "DISCONNECTING"
            is BleConnectionState.Disconnected -> state.reason?.let { "DISCONNECTED ($it)" } ?: "DISCONNECTED"
            is BleConnectionState.Error -> "GATT_ERROR: ${state.message}"
        }

        val lastWritePayload = bleManager.lastSuccessfulPayload ?: "None yet"
        val lastWriteTime = bleManager.lastSuccessfulTimestamp

        val lastNotif = NotificationProcessor.lastForwardedNotification ?: "No notifications forwarded"
        val lastNotifTime = NotificationProcessor.lastForwardedTimestamp

        val callTel = callTelemetry.value

        return DiagnosticsTelemetry(
            bluetoothEnabled = bleManager.isBluetoothEnabled.value,
            isScanning = bleManager.isScanning.value,
            connectedDeviceName = devName,
            connectedDeviceAddress = devAddr,
            gattState = gattStateDesc,
            isServiceDiscovered = bleManager.isServiceDiscovered,
            isCharacteristicDiscovered = bleManager.isCharacteristicDiscovered,
            negotiatedMtu = bleManager.negotiatedMtu,
            lastSuccessfulWrite = lastWritePayload,
            lastSuccessfulWriteTime = lastWriteTime,
            lastNotification = lastNotif,
            lastNotificationTime = lastNotifTime,
            lastCallEvent = callTel.lastEvent,
            lastCallEventTime = callTel.lastTimestamp
        )
    }

    fun updateCustomPayload(text: String) {
        _customPayload.value = text
    }

    fun sendPayload() {
        val payload = _customPayload.value.trim()
        if (payload.isNotEmpty()) {
            viewModelScope.launch {
                bleManager.sendPayloadDirect(payload)
            }
        }
    }

    /**
     * Test Button 1: Send Test Notification
     * Sends real protocol message: N|0|VEDA Test|Diagnostics Alert|Watch packet pipeline verified!
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            _isSendingTest.value = true
            val packet = BleProtocol.buildNormalNotificationPacket(
                appName = "VEDA Test",
                title = "Diagnostics Alert",
                message = "Watch packet pipeline verified!"
            )
            logRepository.log("DIAG_TEST", "Sending Real Test Notification: $packet", LogDirection.OUTBOUND, packet)
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementNotificationCount()
            _isSendingTest.value = false
        }
    }

    /**
     * Test Button 2: Send Test Incoming Call
     * Sends real protocol message: N|1|Phone|Incoming call|Marcus Vance
     */
    fun sendTestIncomingCall() {
        viewModelScope.launch {
            _isSendingTest.value = true
            val packet = BleProtocol.buildIncomingCallPacket("Marcus Vance")
            logRepository.log("DIAG_TEST", "Sending Real Test Incoming Call: $packet", LogDirection.OUTBOUND, packet)
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementCallCount()
            _isSendingTest.value = false
        }
    }

    /**
     * Test Button 3: Send Test Missed Call
     * Sends real protocol message: N|2|Phone|Missed call|Elena Rostova
     */
    fun sendTestMissedCall() {
        viewModelScope.launch {
            _isSendingTest.value = true
            val packet = BleProtocol.buildMissedCallPacket("Elena Rostova")
            logRepository.log("DIAG_TEST", "Sending Real Test Missed Call: $packet", LogDirection.OUTBOUND, packet)
            bleManager.sendPayloadDirect(packet)
            _isSendingTest.value = false
        }
    }

    /**
     * Test Button 4: Sync Time
     * Sends real protocol message: T|YEAR|MONTH|DAY|HOUR|MINUTE|SECOND
     */
    fun sendTimeSync() {
        viewModelScope.launch {
            _isSendingTest.value = true
            val packet = BleProtocol.buildTimePacket()
            logRepository.log("DIAG_TEST", "Sending Real Time Sync: $packet", LogDirection.OUTBOUND, packet)
            val success = bleManager.sendPayloadDirect(packet)
            if (success) {
                preferencesRepository.recordSyncTime()
            }
            _isSendingTest.value = false
        }
    }

    fun clearLogs() {
        logRepository.clear()
    }

    fun getExportText(): String {
        return logRepository.exportAsText()
    }
}
