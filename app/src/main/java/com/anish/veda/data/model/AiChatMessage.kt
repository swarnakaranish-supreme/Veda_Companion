package com.anish.veda.data.model

enum class MessageSender {
    USER,
    VEDA
}

enum class AiActionType {
    NONE,
    SET_ALARM,
    SET_TIMER,
    TOGGLE_DND,
    HEALTH_QUERY,
    NOTIFICATION_SUMMARY,
    FIND_WATCH,
    WATCH_CONFIG,
    DAILY_BRIEFING,
    SMART_REPLY,
    TRIGGER_SOS
}

data class AiChatMessage(
    val id: Long = 0,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: AiActionType = AiActionType.NONE,
    val actionPayload: String? = null,
    val actionExecuted: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val smartReplies: List<String> = emptyList(),
    val isStreaming: Boolean = false
)
