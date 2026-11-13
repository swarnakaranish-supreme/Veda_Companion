package com.anish.veda.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.HapticConfig
import com.anish.veda.data.model.LogDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HapticRepository(
    context: Context,
    private val bleManager: BleManager,
    private val logRepository: LogRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_haptic_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _hapticConfigFlow = MutableStateFlow(loadConfig())
    val hapticConfigFlow: StateFlow<HapticConfig> = _hapticConfigFlow.asStateFlow()

    private val _lastTestTriggered = MutableStateFlow<Int?>(null)
    val lastTestTriggered: StateFlow<Int?> = _lastTestTriggered.asStateFlow()

    private fun loadConfig(): HapticConfig {
        return HapticConfig(
            callProfile = prefs.getInt(KEY_CALL_PROFILE, 0),
            msgProfile = prefs.getInt(KEY_MSG_PROFILE, 1),
            alarmProfile = prefs.getInt(KEY_ALARM_PROFILE, 1),
            emergencyProfile = prefs.getInt(KEY_EMERGENCY_PROFILE, 0),
            navFeedback = prefs.getBoolean(KEY_NAV_FEEDBACK, true),
            navEffectId = prefs.getInt(KEY_NAV_EFFECT, 24),
            motorType = prefs.getString(KEY_MOTOR_TYPE, "ERM Coin Motor") ?: "ERM Coin Motor"
        )
    }

    private fun saveConfig(cfg: HapticConfig) {
        prefs.edit()
            .putInt(KEY_CALL_PROFILE, cfg.callProfile)
            .putInt(KEY_MSG_PROFILE, cfg.msgProfile)
            .putInt(KEY_ALARM_PROFILE, cfg.alarmProfile)
            .putInt(KEY_EMERGENCY_PROFILE, cfg.emergencyProfile)
            .putBoolean(KEY_NAV_FEEDBACK, cfg.navFeedback)
            .putInt(KEY_NAV_EFFECT, cfg.navEffectId)
            .putString(KEY_MOTOR_TYPE, cfg.motorType)
            .apply()
        _hapticConfigFlow.value = cfg
    }

    fun updateConfig(update: (HapticConfig) -> HapticConfig) {
        val newCfg = update(_hapticConfigFlow.value)
        saveConfig(newCfg)
        syncConfigToWatch(newCfg)
    }

    fun syncConfigToWatch(config: HapticConfig = _hapticConfigFlow.value) {
        val packet = BleProtocol.buildHapticConfigPacket(
            callProfile = config.callProfile,
            msgProfile = config.msgProfile,
            alarmProfile = config.alarmProfile,
            emergencyProfile = config.emergencyProfile,
            navFeedback = config.navFeedback,
            navEffectId = config.navEffectId
        )

        logRepository.log("HAPTIC_SYNC", "Syncing haptic profile to watch", LogDirection.OUTBOUND, packet)
        bleManager.enqueuePayload(packet)
    }

    fun playEffect(effectId: Int) {
        _lastTestTriggered.value = effectId
        val packet = BleProtocol.buildHapticPlayPacket(effectId)
        logRepository.log("HAPTIC_TEST", "Triggering DRV2605 effect #$effectId", LogDirection.OUTBOUND, packet)
        scope.launch {
            bleManager.sendPayloadDirect(packet)
        }
    }

    fun stopHaptics() {
        val packet = BleProtocol.buildHapticStopPacket()
        logRepository.log("HAPTIC_STOP", "Stopping haptic playback", LogDirection.OUTBOUND, packet)
        scope.launch {
            bleManager.sendPayloadDirect(packet)
        }
    }

    companion object {
        private const val KEY_CALL_PROFILE = "haptic_call_profile"
        private const val KEY_MSG_PROFILE = "haptic_msg_profile"
        private const val KEY_ALARM_PROFILE = "haptic_alarm_profile"
        private const val KEY_EMERGENCY_PROFILE = "haptic_emergency_profile"
        private const val KEY_NAV_FEEDBACK = "haptic_nav_feedback"
        private const val KEY_NAV_EFFECT = "haptic_nav_effect"
        private const val KEY_MOTOR_TYPE = "haptic_motor_type"
    }
}
