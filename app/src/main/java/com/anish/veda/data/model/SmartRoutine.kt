package com.anish.veda.data.model

enum class RoutineActionType {
    SET_DND,
    SET_BRIGHTNESS,
    SET_TIMEOUT,
    SET_WATCH_FACE,
    START_HEALTH_MEASUREMENT,
    PLAY_HAPTIC,
    SYNC_TIME
}

data class RoutineAction(
    val type: RoutineActionType,
    val parameterInt: Int = 0,
    val parameterBool: Boolean = false
)

data class SmartRoutine(
    val id: String,
    val title: String,
    val description: String,
    val iconCode: String,
    val enabled: Boolean = true,
    val actions: List<RoutineAction>
)
