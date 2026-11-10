package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.calls.CallStateManager
import com.anish.veda.calls.CallTelemetryState
import com.anish.veda.data.model.VedaSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val preferencesRepository = app.preferencesRepository
    private val bleManager = app.bleManager

    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow
    val telemetryState: StateFlow<CallTelemetryState> = CallStateManager.telemetryState

    fun setForwardCalls(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(forwardCalls = enabled) }
    }

    fun setForwardMissedCalls(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(forwardMissedCalls = enabled) }
    }

    fun setShowCallerName(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(showCallerName = enabled) }
    }

    fun setResolveCallerNames(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(resolveCallerNames = enabled) }
    }

    fun refreshTelephonyListener() {
        CallStateManager.startListening(app.applicationContext)
    }

    fun sendTestIncomingCall(callerName: String = "Marcus Vance (+1 555-0199)") {
        viewModelScope.launch {
            val settingsVal = settings.value
            val callerDisplay = CallStateManager.resolveCallerDisplay(
                context = app.applicationContext,
                rawCaller = callerName,
                showCallerName = settingsVal.showCallerName,
                resolveContactNames = settingsVal.resolveCallerNames
            )
            val packet = BleProtocol.buildIncomingCallPacket(callerDisplay)
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementCallCount()
        }
    }

    fun sendTestMissedCall(callerName: String = "Elena Rostova (+1 555-0142)") {
        viewModelScope.launch {
            val settingsVal = settings.value
            val callerDisplay = CallStateManager.resolveCallerDisplay(
                context = app.applicationContext,
                rawCaller = callerName,
                showCallerName = settingsVal.showCallerName,
                resolveContactNames = settingsVal.resolveCallerNames
            )
            val packet = BleProtocol.buildMissedCallPacket(callerDisplay)
            bleManager.sendPayloadDirect(packet)
        }
    }

    fun sendTestUnknownCall() {
        viewModelScope.launch {
            val packet = BleProtocol.buildIncomingCallPacket("UNKNOWN")
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementCallCount()
        }
    }
}
