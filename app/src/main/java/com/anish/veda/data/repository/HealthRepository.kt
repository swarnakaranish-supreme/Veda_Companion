package com.anish.veda.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.database.HealthDatabaseHelper
import com.anish.veda.data.model.DailyHealthSummary
import com.anish.veda.data.model.HealthMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRepository(
    private val context: Context,
    private val bleManager: BleManager,
    private val logRepository: LogRepository
) {
    private val repoScope = CoroutineScope(Dispatchers.IO + Job())
    private val dbHelper = HealthDatabaseHelper(context)

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _liveHeartRate = MutableStateFlow<Int?>(null)
    val liveHeartRate: StateFlow<Int?> = _liveHeartRate.asStateFlow()

    private val _liveSpO2 = MutableStateFlow<Int?>(null)
    val liveSpO2: StateFlow<Int?> = _liveSpO2.asStateFlow()

    private val _sensorQuality = MutableStateFlow<Int?>(null)
    val sensorQuality: StateFlow<Int?> = _sensorQuality.asStateFlow()

    private val _recentMeasurements = MutableStateFlow<List<HealthMeasurement>>(emptyList())
    val recentMeasurements: StateFlow<List<HealthMeasurement>> = _recentMeasurements.asStateFlow()

    private val _dailySummaries = MutableStateFlow<List<DailyHealthSummary>>(emptyList())
    val dailySummaries: StateFlow<List<DailyHealthSummary>> = _dailySummaries.asStateFlow()

    init {
        // Load initial persisted measurements
        refreshHistory()

        // 1. Listen for incoming BLE notifications from the watch
        repoScope.launch {
            bleManager.incomingNotificationFlow.collectLatest { payload ->
                processIncomingPacket(payload)
            }
        }

        // 2. Reset telemetry automatically when peripheral disconnects
        repoScope.launch {
            bleManager.connectionState.collectLatest { state ->
                if (state !is BleConnectionState.Connected) {
                    _isMeasuring.value = false
                    _liveHeartRate.value = null
                    _liveSpO2.value = null
                    _sensorQuality.value = null
                }
            }
        }
    }

    fun startMeasurement() {
        if (!bleManager.connectionState.value.isConnected) {
            logRepository.log("HEALTH", "Cannot start measurement: Watch is disconnected", com.anish.veda.data.model.LogDirection.ERROR)
            return
        }
        _isMeasuring.value = true
        bleManager.startHealthMeasurement()
    }

    fun stopMeasurement() {
        _isMeasuring.value = false
        _liveHeartRate.value = null
        _liveSpO2.value = null
        _sensorQuality.value = null
        bleManager.stopHealthMeasurement()
    }

    /**
     * Parses and validates raw incoming sensor packets from the watch.
     * Guaranteed never to invent readings or save noise.
     */
    private fun processIncomingPacket(payload: String) {
        val packet = BleProtocol.parseHealthPacket(payload) ?: return

        if (!packet.hasFinger) {
            // Finger removed / sensor idle: show actual state without inventing data
            _liveHeartRate.value = null
            _liveSpO2.value = null
            _sensorQuality.value = 0
            return
        }

        // Validation bounds: physiological human limits
        val validBpm = if (packet.bpm in 40..220) packet.bpm else null
        val validSpo2 = if (packet.spo2 in 70..100) packet.spo2 else null
        val validQuality = packet.quality.coerceIn(0, 100)

        _liveHeartRate.value = validBpm
        _liveSpO2.value = validSpo2
        _sensorQuality.value = validQuality

        // Only persist if we have a valid heart rate reading
        if (validBpm != null && validSpo2 != null) {
            val record = HealthMeasurement(
                timestamp = System.currentTimeMillis(),
                heartRate = validBpm,
                spo2 = validSpo2,
                quality = validQuality
            )
            dbHelper.insertMeasurement(record)
            refreshHistory()
        }
    }

    fun refreshHistory() {
        repoScope.launch {
            val list = dbHelper.getRecentMeasurements(limit = 60)
            val summaries = dbHelper.getDailySummaries(days = 7)
            _recentMeasurements.value = list
            _dailySummaries.value = summaries
        }
    }

    fun clearAllHistory() {
        repoScope.launch {
            dbHelper.clearHistory()
            refreshHistory()
        }
    }

    /**
     * Generates standard RFC 4180 CSV export of all recorded health measurements.
     */
    suspend fun exportCsv(context: Context): Uri? = withContext(Dispatchers.IO) {
        val list = dbHelper.getRecentMeasurements(limit = 1000)
        if (list.isEmpty()) return@withContext null

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(exportDir, "VEDA_Health_Report_$timeStamp.csv")

        FileWriter(csvFile).use { writer ->
            writer.append("Record_ID,Timestamp_Epoch_Ms,Date,Time,HeartRate_BPM,SpO2_Percent,Signal_Quality_Percent,Status\n")
            for (m in list) {
                writer.append("${m.id},")
                writer.append("${m.timestamp},")
                writer.append("${m.formattedDateOnly},")
                writer.append("${m.formattedHourOnly},")
                writer.append("${m.heartRate},")
                writer.append("${m.spo2},")
                writer.append("${m.quality},")
                writer.append("${m.status}\n")
            }
        }

        try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
        } catch (e: Exception) {
            Uri.fromFile(csvFile)
        }
    }
}
