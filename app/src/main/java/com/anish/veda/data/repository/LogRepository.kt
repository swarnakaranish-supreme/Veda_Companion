package com.anish.veda.data.repository

import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

class LogRepository(private val maxEntries: Int = 300) {

    private val deque = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    fun log(tag: String, message: String, direction: LogDirection = LogDirection.SYSTEM, rawPayload: String? = null) {
        val entry = LogEntry(
            tag = tag,
            message = message,
            direction = direction,
            rawPayload = rawPayload
        )
        deque.addFirst(entry)
        while (deque.size > maxEntries) {
            deque.pollLast()
        }
        _logsFlow.value = deque.toList()
    }

    fun clear() {
        deque.clear()
        _logsFlow.value = emptyList()
    }

    fun exportAsText(): String {
        return deque.reversed().joinToString(separator = "\n") { entry ->
            "[${entry.timestamp}] [${entry.direction}] [${entry.tag}] ${entry.message} ${entry.rawPayload?.let { "-> $it" } ?: ""}"
        }
    }
}
