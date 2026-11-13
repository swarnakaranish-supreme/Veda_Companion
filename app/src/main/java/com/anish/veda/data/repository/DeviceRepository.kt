package com.anish.veda.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.bluetooth.TimeSyncManager
import com.anish.veda.data.model.BatteryInfo
import com.anish.veda.data.model.ConnectionHistoryEntry
import com.anish.veda.data.model.ConnectionHistoryEventType
import com.anish.veda.data.model.DeviceMetadata
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.SensorHealth
import com.anish.veda.data.model.SyncStatus
import com.anish.veda.data.model.TroubleshootingGuide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DeviceRepository(
    private val context: Context,
    private val bleManager: BleManager,
    private val timeSyncManager: TimeSyncManager,
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_device_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _deviceMetadata = MutableStateFlow(loadInitialMetadata())
    val deviceMetadata: StateFlow<DeviceMetadata> = _deviceMetadata.asStateFlow()

    private val _connectionHistory = MutableStateFlow(loadConnectionHistory())
    val connectionHistory: StateFlow<List<ConnectionHistoryEntry>> = _connectionHistory.asStateFlow()

    private val _isAutoReconnectEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_RECONNECT, true)
    )
    val isAutoReconnectEnabled: StateFlow<Boolean> = _isAutoReconnectEnabled.asStateFlow()

    private val _isPingInProgress = MutableStateFlow(false)
    val isPingInProgress: StateFlow<Boolean> = _isPingInProgress.asStateFlow()

    private val _pingLatency = MutableStateFlow<Long?>(null)
    val pingLatency: StateFlow<Long?> = _pingLatency.asStateFlow()

    private var pingSentTimestamp: Long = 0L
    private var connectedSessionStartTime: Long = 0L
    private var previousState: BleConnectionState? = null

    init {
        // Sync auto-reconnect setting to BleManager
        bleManager.setAutoReconnectEnabled(_isAutoReconnectEnabled.value)

        // Monitor BLE Connection State changes
        scope.launch {
            bleManager.connectionState.collect { state ->
                handleConnectionStateChange(state)
            }
        }

        // Monitor incoming BLE notifications for D|INFO and D|PONG
        scope.launch {
            bleManager.incomingNotificationFlow.collect { raw ->
                handleIncomingTelemetry(raw)
            }
        }

        // Monitor Time Sync updates
        scope.launch {
            timeSyncManager.lastSyncTimestamp.collect { syncTime ->
                val current = _deviceMetadata.value
                val newStatus = if (syncTime > 0) SyncStatus.SYNCED else SyncStatus.NEVER_SYNCED
                _deviceMetadata.value = current.copy(
                    lastSyncTimestamp = syncTime,
                    syncStatus = newStatus
                )
            }
        }

        scope.launch {
            timeSyncManager.isSyncing.collect { isSyncing ->
                if (isSyncing) {
                    val current = _deviceMetadata.value
                    _deviceMetadata.value = current.copy(syncStatus = SyncStatus.SYNCING)
                }
            }
        }
    }

    private fun loadInitialMetadata(): DeviceMetadata {
        val lastAddr = preferencesRepository.settingsFlow.value.lastDeviceAddress ?: "--"
        val lastName = preferencesRepository.settingsFlow.value.lastDeviceName ?: "ANISH-WATCH"
        val lastSync = preferencesRepository.settingsFlow.value.lastSyncTimestamp
        return DeviceMetadata(
            watchName = lastName,
            macAddress = lastAddr,
            firmwareVersion = "v2.0.0 (VEDA-C3)",
            protocolVersion = "v1.2.0",
            lastSyncTimestamp = lastSync,
            syncStatus = if (lastSync > 0) SyncStatus.SYNCED else SyncStatus.NEVER_SYNCED,
            batteryInfo = BatteryInfo(
                isAvailable = false,
                percentage = null,
                statusMessage = "HARDWARE UNAVAILABLE — VEDA V2 has no dedicated MAX17048 fuel gauge or ADC voltage divider. Battery readings are omitted to avoid reporting fabricated percentages."
            )
        )
    }

    private fun handleConnectionStateChange(newState: BleConnectionState) {
        val oldState = previousState
        previousState = newState

        when (newState) {
            is BleConnectionState.Connected -> {
                connectedSessionStartTime = System.currentTimeMillis()
                val current = _deviceMetadata.value
                _deviceMetadata.value = current.copy(
                    watchName = newState.deviceName,
                    macAddress = newState.deviceAddress,
                    negotiatedMtu = newState.mtu,
                    rssi = newState.rssi,
                    batteryInfo = BatteryInfo(
                        isAvailable = false,
                        percentage = null,
                        statusMessage = "HARDWARE UNAVAILABLE (No fuel gauge circuit)"
                    )
                )

                recordHistoryEvent(
                    eventType = ConnectionHistoryEventType.CONNECTED,
                    deviceName = newState.deviceName,
                    deviceAddress = newState.deviceAddress,
                    notes = "MTU=${newState.mtu}, Initial RSSI=${newState.rssi} dBm"
                )

                // Query watch device info and ping
                scope.launch {
                    delay(500)
                    queryDeviceInfo()
                }
            }

            is BleConnectionState.Disconnected -> {
                if (oldState is BleConnectionState.Connected) {
                    val durationSec = if (connectedSessionStartTime > 0) {
                        (System.currentTimeMillis() - connectedSessionStartTime) / 1000
                    } else null

                    val eventType = if (newState.reason == "Manual disconnect") {
                        ConnectionHistoryEventType.DISCONNECTED
                    } else {
                        ConnectionHistoryEventType.CONNECTION_LOST
                    }

                    recordHistoryEvent(
                        eventType = eventType,
                        deviceName = oldState.deviceName,
                        deviceAddress = oldState.deviceAddress,
                        durationSeconds = durationSec,
                        notes = newState.reason ?: "Link dropped"
                    )
                }
                connectedSessionStartTime = 0L
                _pingLatency.value = null
            }

            is BleConnectionState.Reconnecting -> {
                if (oldState !is BleConnectionState.Reconnecting) {
                    recordHistoryEvent(
                        eventType = ConnectionHistoryEventType.RECONNECTING,
                        deviceName = newState.deviceName ?: "ANISH-WATCH",
                        deviceAddress = newState.deviceAddress,
                        notes = "Auto-reconnect attempt ${newState.attempt} of ${newState.maxAttempts}"
                    )
                }
            }

            is BleConnectionState.Error -> {
                recordHistoryEvent(
                    eventType = ConnectionHistoryEventType.FAILED,
                    deviceName = _deviceMetadata.value.watchName,
                    deviceAddress = _deviceMetadata.value.macAddress,
                    notes = newState.message
                )
            }

            else -> {}
        }
    }

    private fun handleIncomingTelemetry(raw: String) {
        // 1. Device Info Packet: D|INFO|<name>|<fwVersion>|<protoVersion>|<sensors>|<battery>
        val info = BleProtocol.parseDeviceInfoPacket(raw)
        if (info != null) {
            logRepository.log("DEVICE_MGMT", "Received watch hardware info: FW=${info.firmwareVersion}, Proto=${info.protocolVersion}", LogDirection.INBOUND)
            val current = _deviceMetadata.value
            _deviceMetadata.value = current.copy(
                watchName = info.watchName,
                firmwareVersion = "v${info.firmwareVersion}",
                protocolVersion = "v${info.protocolVersion}",
                batteryInfo = BatteryInfo(
                    isAvailable = false,
                    percentage = null,
                    statusMessage = "HARDWARE UNAVAILABLE — Firmware reports ${info.batteryStatus}. Battery percentage omitted for physical accuracy."
                )
            )
            return
        }

        // 2. Pong Packet: D|PONG
        if (BleProtocol.isPongPacket(raw)) {
            val now = System.currentTimeMillis()
            if (pingSentTimestamp > 0) {
                val roundTrip = (now - pingSentTimestamp).coerceAtLeast(1L)
                _pingLatency.value = roundTrip
                _isPingInProgress.value = false
                val current = _deviceMetadata.value
                _deviceMetadata.value = current.copy(pingLatencyMs = roundTrip)
                logRepository.log("DEVICE_MGMT", "Watch Pong received! Round-trip latency: ${roundTrip}ms", LogDirection.INBOUND)
            }
        }
    }

    // ========================================================================
    // DEVICE ACTIONS & DIAGNOSTICS
    // ========================================================================

    fun queryDeviceInfo() {
        if (!bleManager.connectionState.value.isConnected) return
        scope.launch {
            val req = BleProtocol.buildDeviceInfoRequestPacket()
            bleManager.sendPayloadDirect(req)
        }
    }

    fun pingWatch() {
        if (!bleManager.connectionState.value.isConnected) return
        scope.launch {
            _isPingInProgress.value = true
            pingSentTimestamp = System.currentTimeMillis()
            val ping = BleProtocol.buildPingPacket()
            val sent = bleManager.sendPayloadDirect(ping)
            if (!sent) {
                _isPingInProgress.value = false
                _pingLatency.value = null
            }
        }
    }

    fun triggerSyncNow() {
        scope.launch {
            val success = timeSyncManager.syncTimeNow()
            if (success) {
                queryDeviceInfo()
            }
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        _isAutoReconnectEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply()
        bleManager.setAutoReconnectEnabled(enabled)
    }

    // ========================================================================
    // CONNECTION HISTORY PERSISTENCE
    // ========================================================================

    private fun recordHistoryEvent(
        eventType: ConnectionHistoryEventType,
        deviceName: String,
        deviceAddress: String,
        durationSeconds: Long? = null,
        notes: String? = null
    ) {
        val entry = ConnectionHistoryEntry(
            eventType = eventType,
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            durationSeconds = durationSeconds,
            notes = notes
        )
        val currentList = _connectionHistory.value.toMutableList()
        currentList.add(0, entry) // prepend newest
        if (currentList.size > 50) {
            currentList.subList(50, currentList.size).clear()
        }
        _connectionHistory.value = currentList
        saveConnectionHistory(currentList)
    }

    private fun loadConnectionHistory(): List<ConnectionHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ConnectionHistoryEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ConnectionHistoryEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.getLong("timestamp"),
                        eventType = ConnectionHistoryEventType.valueOf(obj.getString("eventType")),
                        deviceName = obj.getString("deviceName"),
                        deviceAddress = obj.getString("deviceAddress"),
                        durationSeconds = if (obj.has("durationSeconds")) obj.getLong("durationSeconds") else null,
                        notes = if (obj.has("notes")) obj.getString("notes") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveConnectionHistory(list: List<ConnectionHistoryEntry>) {
        try {
            val array = JSONArray()
            for (entry in list) {
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("timestamp", entry.timestamp)
                    put("eventType", entry.eventType.name)
                    put("deviceName", entry.deviceName)
                    put("deviceAddress", entry.deviceAddress)
                    if (entry.durationSeconds != null) put("durationSeconds", entry.durationSeconds)
                    if (entry.notes != null) put("notes", entry.notes)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearConnectionHistory() {
        _connectionHistory.value = emptyList()
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
    }

    // ========================================================================
    // TROUBLESHOOTING GUIDANCE
    // ========================================================================

    fun getTroubleshootingGuides(): List<TroubleshootingGuide> = listOf(
        TroubleshootingGuide(
            title = "Smartwatch Not Discovered During Scan",
            category = "BLE Discovery",
            symptoms = "Radar scanner displays 0 devices or 'ANISH-WATCH' does not appear in the scan list.",
            steps = listOf(
                "Ensure the ESP32-C3 SuperMini is powered ON (LED indicator active).",
                "Verify watch is in ADVERTISING state (not connected to another phone).",
                "Ensure Android Location (GPS) and Bluetooth are toggled ON in system quick settings.",
                "Verify Nearby Devices permission is granted to VEDA Companion.",
                "Press the Enter button on the watch to ensure the microcontroller is awake."
            )
        ),
        TroubleshootingGuide(
            title = "GATT 133 / Connection Handshake Timeouts",
            category = "GATT Handshake",
            symptoms = "App attempts to connect but drops with GATT_ERROR_133 or link timeout.",
            steps = listOf(
                "Cycle phone Bluetooth OFF for 5 seconds and toggle back ON.",
                "Move watch closer (within 1 meter) to eliminate RF interference.",
                "Toggle Auto-Reconnection OFF, wait 5 seconds, and tap CONNECT manually.",
                "If bonds are corrupted, reboot the ESP32-C3 SuperMini power rail."
            )
        ),
        TroubleshootingGuide(
            title = "Battery Display Shows 'Hardware Unavailable'",
            category = "Battery & Power",
            symptoms = "Battery card shows 'HARDWARE UNAVAILABLE' instead of a percentage.",
            steps = listOf(
                "This is expected normal behavior: VEDA V2 hardware does not include a dedicated MAX17048 I2C fuel gauge chip or an analog resistor divider on an ADC pin.",
                "VEDA V2 avoids fabricating estimated percentages to maintain absolute telemetry accuracy.",
                "Charge the 450mAh 3.7V LiPo battery via standard USB charger until charging LED indicator indicates full."
            )
        ),
        TroubleshootingGuide(
            title = "Time & Settings Out of Sync",
            category = "Synchronization",
            symptoms = "Watch time or watch face settings do not reflect changes made in app.",
            steps = listOf(
                "Verify BLE connection state shows 'LINK ESTABLISHED' in green.",
                "Tap 'SYNC NOW' on the Synchronization Status card.",
                "Check diagnostic log in the Diagnostics tab to confirm packet transmission: T|YYYY|MM|DD...",
                "Ensure screen timeout on watch hasn't put the display to sleep during write."
            )
        )
    )

    fun exportDiagnosticsSummary(): String {
        val meta = _deviceMetadata.value
        val history = _connectionHistory.value
        val connState = bleManager.connectionState.value

        return buildString {
            appendLine("=== VEDA V2 DEVICE DIAGNOSTICS REPORT ===")
            appendLine("Generated At: ${java.util.Date()}")
            appendLine("Device Name: ${meta.watchName}")
            appendLine("MAC Address: ${meta.macAddress}")
            appendLine("GATT Connection State: $connState")
            appendLine("Firmware Version: ${meta.firmwareVersion}")
            appendLine("BLE Protocol Version: ${meta.protocolVersion}")
            appendLine("Hardware Platform: ${meta.hardwareArchitecture}")
            appendLine("Display Engine: ${meta.displaySpecs}")
            appendLine("Negotiated MTU: ${meta.negotiatedMtu} bytes")
            appendLine("Signal Strength (RSSI): ${meta.rssi?.let { "$it dBm" } ?: "N/A"}")
            appendLine("Ping Round-Trip Latency: ${meta.pingLatencyMs?.let { "${it}ms" } ?: "N/A"}")
            appendLine("Last Successful Sync: ${if (meta.lastSyncTimestamp > 0) java.util.Date(meta.lastSyncTimestamp) else "Never"}")
            appendLine("Auto-Reconnect: ${_isAutoReconnectEnabled.value}")
            appendLine("Battery Status: ${meta.batteryInfo.statusMessage}")
            appendLine("\n--- Sensor Health Status ---")
            meta.sensors.forEach { s ->
                appendLine("• ${s.name} [${s.bus}]: ${s.statusText} (${s.description})")
            }
            appendLine("\n--- Connection History (Recent ${history.size}) ---")
            history.take(15).forEach { h ->
                val dur = h.durationSeconds?.let { " (${it}s duration)" } ?: ""
                appendLine("${java.util.Date(h.timestamp)} | ${h.eventType.label} | ${h.notes ?: ""}$dur")
            }
            appendLine("=========================================")
        }
    }

    companion object {
        private const val KEY_HISTORY_JSON = "device_connection_history_json"
        private const val KEY_AUTO_RECONNECT = "device_auto_reconnect_enabled"
    }
}
