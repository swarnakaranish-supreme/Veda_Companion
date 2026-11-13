package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.data.model.HapticCatalog
import com.anish.veda.data.model.HapticConfig
import com.anish.veda.data.model.HapticProfilePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HapticStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val hapticRepo = app.hapticRepository
    private val bleManager = app.bleManager

    val hapticConfig: StateFlow<HapticConfig> = hapticRepo.hapticConfigFlow
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val lastTestTriggered: StateFlow<Int?> = hapticRepo.lastTestTriggered

    private val _selectedPresetId = MutableStateFlow<String?>("tactical_feedback")
    val selectedPresetId: StateFlow<String?> = _selectedPresetId.asStateFlow()

    fun selectPreset(preset: HapticProfilePreset) {
        _selectedPresetId.value = preset.id
        hapticRepo.updateConfig {
            preset.config
        }
    }

    fun setCallProfile(profile: Int) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(callProfile = profile) }
    }

    fun setMsgProfile(profile: Int) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(msgProfile = profile) }
    }

    fun setAlarmProfile(profile: Int) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(alarmProfile = profile) }
    }

    fun setEmergencyProfile(profile: Int) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(emergencyProfile = profile) }
    }

    fun setNavFeedback(enabled: Boolean) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(navFeedback = enabled) }
    }

    fun setNavEffectId(effectId: Int) {
        _selectedPresetId.value = null
        hapticRepo.updateConfig { it.copy(navEffectId = effectId) }
    }

    fun playEffect(effectId: Int) {
        viewModelScope.launch {
            hapticRepo.playEffect(effectId)
        }
    }

    fun stopHaptics() {
        viewModelScope.launch {
            hapticRepo.stopHaptics()
        }
    }

    fun syncToWatch() {
        viewModelScope.launch {
            hapticRepo.syncConfigToWatch()
        }
    }
}
