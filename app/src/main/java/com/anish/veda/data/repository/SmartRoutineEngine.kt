package com.anish.veda.data.repository

import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.RoutineAction
import com.anish.veda.data.model.RoutineActionType
import com.anish.veda.data.model.SmartRoutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RoutineExecutionResult {
    data object Idle : RoutineExecutionResult()
    data class Running(val routineName: String, val step: Int, val totalSteps: Int) : RoutineExecutionResult()
    data class Success(val routineName: String, val message: String) : RoutineExecutionResult()
    data class Error(val routineName: String, val error: String) : RoutineExecutionResult()
}

class SmartRoutineEngine(
    private val bleManager: BleManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val defaultRoutines = listOf(
        SmartRoutine(
            id = "sleep_routine",
            title = "Sleep Routine",
            description = "Engage DND, set display contrast to 20%, reduce screen timeout to 5s",
            iconCode = "SLEEP",
            actions = listOf(
                RoutineAction(RoutineActionType.SET_DND, parameterBool = true),
                RoutineAction(RoutineActionType.SET_BRIGHTNESS, parameterInt = 20),
                RoutineAction(RoutineActionType.SET_TIMEOUT, parameterInt = 5),
                RoutineAction(RoutineActionType.PLAY_HAPTIC, parameterInt = 14)
            )
        ),
        SmartRoutine(
            id = "focus_routine",
            title = "Focus Mode",
            description = "Engage DND, switch to Minimal Digital face, set timeout to 10s",
            iconCode = "FOCUS",
            actions = listOf(
                RoutineAction(RoutineActionType.SET_DND, parameterBool = true),
                RoutineAction(RoutineActionType.SET_WATCH_FACE, parameterInt = 0),
                RoutineAction(RoutineActionType.SET_TIMEOUT, parameterInt = 10)
            )
        ),
        SmartRoutine(
            id = "health_check",
            title = "Health Check",
            description = "Start MAX30102 live PPG biometric measurement and trigger haptic pulse",
            iconCode = "HEALTH",
            actions = listOf(
                RoutineAction(RoutineActionType.PLAY_HAPTIC, parameterInt = 1),
                RoutineAction(RoutineActionType.START_HEALTH_MEASUREMENT, parameterBool = true)
            )
        ),
        SmartRoutine(
            id = "normal_routine",
            title = "Normal Routine",
            description = "Disable DND, restore brightness to 70%, restore Futuristic HUD face",
            iconCode = "NORMAL",
            actions = listOf(
                RoutineAction(RoutineActionType.SET_DND, parameterBool = false),
                RoutineAction(RoutineActionType.SET_BRIGHTNESS, parameterInt = 70),
                RoutineAction(RoutineActionType.SET_WATCH_FACE, parameterInt = 1),
                RoutineAction(RoutineActionType.SET_TIMEOUT, parameterInt = 15),
                RoutineAction(RoutineActionType.SYNC_TIME)
            )
        )
    )

    private val _routines = MutableStateFlow(defaultRoutines)
    val routines: StateFlow<List<SmartRoutine>> = _routines.asStateFlow()

    private val _executionState = MutableStateFlow<RoutineExecutionResult>(RoutineExecutionResult.Idle)
    val executionState: StateFlow<RoutineExecutionResult> = _executionState.asStateFlow()

    fun toggleRoutine(id: String) {
        _routines.value = _routines.value.map { r ->
            if (r.id == id) r.copy(enabled = !r.enabled) else r
        }
    }

    fun executeRoutine(routine: SmartRoutine) {
        if (!bleManager.connectionState.value.isConnected) {
            _executionState.value = RoutineExecutionResult.Error(routine.title, "Watch disconnected. Connect before running routine.")
            return
        }

        scope.launch {
            try {
                val total = routine.actions.size
                routine.actions.forEachIndexed { index, action ->
                    _executionState.value = RoutineExecutionResult.Running(routine.title, index + 1, total)

                    when (action.type) {
                        RoutineActionType.SET_DND -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildDndPacket(action.parameterBool))
                        }
                        RoutineActionType.SET_BRIGHTNESS -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildBrightnessPacket(action.parameterInt))
                        }
                        RoutineActionType.SET_TIMEOUT -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildTimeoutPacket(action.parameterInt))
                        }
                        RoutineActionType.SET_WATCH_FACE -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildSelectFacePacket(action.parameterInt))
                        }
                        RoutineActionType.START_HEALTH_MEASUREMENT -> {
                            val cmd = if (action.parameterBool) BleProtocol.CMD_START_MEASUREMENT else BleProtocol.CMD_STOP_MEASUREMENT
                            bleManager.sendPayloadDirect(cmd)
                        }
                        RoutineActionType.PLAY_HAPTIC -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildHapticPlayPacket(action.parameterInt))
                        }
                        RoutineActionType.SYNC_TIME -> {
                            bleManager.sendPayloadDirect(BleProtocol.buildTimePacket())
                        }
                    }

                    // Throttle sequentially so watch BLE onWrite processes each without buffer overrun
                    delay(220)
                }

                _executionState.value = RoutineExecutionResult.Success(routine.title, "Routine executed successfully ($total actions dispatched).")
            } catch (e: Exception) {
                _executionState.value = RoutineExecutionResult.Error(routine.title, e.message ?: "Failed during execution.")
            }
        }
    }
}
