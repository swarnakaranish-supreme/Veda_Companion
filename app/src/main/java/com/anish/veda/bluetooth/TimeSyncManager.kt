package com.anish.veda.bluetooth

import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class TimeSyncManager(
    private val bleManager: BleManager,
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var periodicSyncJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(preferencesRepository.settingsFlow.value.lastSyncTimestamp)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    init {
        // Observe connection state: sync immediately after successful connection if enabled
        scope.launch {
            bleManager.connectionState.collect { state ->
                if (state is BleConnectionState.Connected) {
                    val settings = preferencesRepository.settingsFlow.value
                    if (settings.autoSyncTimeOnConnect) {
                        delay(600) // Brief delay to let the ESP32 settle after GATT discovery
                        syncTimeNow()
                    }
                }
            }
        }

        startPeriodicScheduler()
    }

    /**
     * Executes time synchronization immediately.
     * Formats packet according to firmware protocol: T|YEAR|MONTH|DAY|HOUR|MINUTE|SECOND
     */
    suspend fun syncTimeNow(): Boolean {
        if (!bleManager.connectionState.value.isConnected) {
            logRepository.log("TIME_SYNC", "Cannot sync time: Watch is not connected", LogDirection.ERROR)
            return false
        }

        _isSyncing.value = true
        val timePacket = BleProtocol.buildTimePacket(Calendar.getInstance())
        logRepository.log("TIME_SYNC", "Transmitting RTC time sync packet: $timePacket", LogDirection.OUTBOUND, timePacket)

        val success = bleManager.sendPayloadDirect(timePacket)
        if (success) {
            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            preferencesRepository.recordSyncTime(now)
            logRepository.log("TIME_SYNC", "Watch software RTC successfully synchronized to $timePacket")
        } else {
            logRepository.log("TIME_SYNC", "Watch time synchronization failed or timed out", LogDirection.ERROR)
        }
        _isSyncing.value = false
        return success
    }

    /**
     * Non-suspending trigger for time synchronization.
     */
    fun requestTimeSync() {
        scope.launch {
            syncTimeNow()
        }
    }

    private fun startPeriodicScheduler() {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (isActive) {
                val settings = preferencesRepository.settingsFlow.value
                val intervalHours = settings.syncIntervalHours.coerceIn(1, 24)
                delay(intervalHours * 3600 * 1000L)

                if (settings.periodicTimeSync && bleManager.connectionState.value.isConnected) {
                    syncTimeNow()
                }
            }
        }
    }
}
