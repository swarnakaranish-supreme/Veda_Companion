package com.anish.veda.data.model

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: LogDirection = LogDirection.SYSTEM,
    val tag: String,
    val message: String,
    val rawPayload: String? = null
)

enum class LogDirection {
    OUTBOUND,
    INBOUND,
    SYSTEM,
    ERROR
}
