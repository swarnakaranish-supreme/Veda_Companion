package com.anish.veda.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.data.model.DailyHealthSummary
import com.anish.veda.data.model.HealthMeasurement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val healthRepository = app.healthRepository
    private val bleManager = app.bleManager

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val isMeasuring: StateFlow<Boolean> = healthRepository.isMeasuring
    val liveHeartRate: StateFlow<Int?> = healthRepository.liveHeartRate
    val liveSpO2: StateFlow<Int?> = healthRepository.liveSpO2
    val sensorQuality: StateFlow<Int?> = healthRepository.sensorQuality
    val recentMeasurements: StateFlow<List<HealthMeasurement>> = healthRepository.recentMeasurements
    val dailySummaries: StateFlow<List<DailyHealthSummary>> = healthRepository.dailySummaries

    fun startMeasurement() {
        healthRepository.startMeasurement()
    }

    fun stopMeasurement() {
        healthRepository.stopMeasurement()
    }

    fun toggleMeasurement() {
        if (isMeasuring.value) {
            stopMeasurement()
        } else {
            startMeasurement()
        }
    }

    fun clearHistory() {
        healthRepository.clearAllHistory()
    }

    fun exportAndShareCsv(context: Context) {
        viewModelScope.launch {
            val uri = healthRepository.exportCsv(context) ?: return@launch
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VEDA Smartwatch Health Telemetry Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share VEDA Health CSV Export"))
        }
    }
}
