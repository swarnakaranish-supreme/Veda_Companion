package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.data.model.EmergencyContact
import com.anish.veda.data.model.GpsCoordinates
import com.anish.veda.data.model.GuardianConfig
import com.anish.veda.data.model.SosStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val guardianRepo = app.guardianRepository
    private val locationProvider = app.guardianLocationProvider
    private val emergencyDispatcher = app.emergencyDispatcher
    private val bleManager = app.bleManager

    val sosStatus: StateFlow<SosStatus> = guardianRepo.sosStatus
    val remainingCountdown: StateFlow<Int> = guardianRepo.remainingCountdown
    val lastDispatchMessage: StateFlow<String?> = guardianRepo.lastDispatchMessage
    val contacts: StateFlow<List<EmergencyContact>> = guardianRepo.contactsFlow
    val config: StateFlow<GuardianConfig> = guardianRepo.configFlow
    val currentLocation: StateFlow<GpsCoordinates?> = locationProvider.currentLocation
    val disconnectionAlertEvent: SharedFlow<String> = guardianRepo.disconnectionAlertEvent

    val isWatchConnected: StateFlow<Boolean> = bleManager.connectionState
        .map { it is BleConnectionState.Connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLocating = MutableStateFlow(false)

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()
    fun isLocationEnabled(): Boolean = locationProvider.isLocationEnabled()
    fun hasSmsPermission(): Boolean = emergencyDispatcher.hasSmsPermission()

    fun triggerSos() {
        guardianRepo.startCountdown(fromWatch = false)
    }

    fun cancelSos() {
        guardianRepo.cancelCountdown(fromWatch = false)
    }

    fun resetSos() {
        guardianRepo.resetStatus()
    }

    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean = false) {
        guardianRepo.addContact(name, phone, relationship, isPrimary)
    }

    fun removeContact(id: String) {
        guardianRepo.removeContact(id)
    }

    fun setPrimaryContact(id: String) {
        guardianRepo.setPrimaryContact(id)
    }

    fun setGestureMode(mode: Int) {
        guardianRepo.updateConfig { it.copy(gestureMode = mode) }
    }

    fun setCountdownSeconds(seconds: Int) {
        guardianRepo.updateConfig { it.copy(countdownSeconds = seconds) }
    }

    fun setMessageTemplate(template: String) {
        guardianRepo.updateConfig { it.copy(messageTemplate = template) }
    }

    fun setDisconnectionAlert(enabled: Boolean) {
        guardianRepo.updateConfig { it.copy(disconnectionAlertEnabled = enabled) }
    }

    fun setVibratePhone(enabled: Boolean) {
        guardianRepo.updateConfig { it.copy(vibratePhoneOnSos = enabled) }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            isLocating.value = true
            try {
                locationProvider.getFreshLocation()
            } finally {
                isLocating.value = false
            }
        }
    }

    fun syncConfigToWatch() {
        guardianRepo.syncConfigToWatch()
    }
}
