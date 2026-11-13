package com.anish.veda.data.repository

import android.content.Context
import com.anish.veda.data.database.NotificationDatabaseHelper
import com.anish.veda.data.model.NotificationHistoryItem
import com.anish.veda.notifications.NotificationSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationRepository(
    context: Context,
    val settingsRepo: NotificationSettingsRepository
) {
    private val dbHelper = NotificationDatabaseHelper(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _historyFlow = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<NotificationHistoryItem>> = _historyFlow.asStateFlow()

    private val _unreadCountFlow = MutableStateFlow(0)
    val unreadCountFlow: StateFlow<Int> = _unreadCountFlow.asStateFlow()

    private var currentSearchQuery = ""
    private var currentCategoryFilter: Int? = null

    init {
        refreshHistory()
    }

    fun refreshHistory(
        searchQuery: String = currentSearchQuery,
        categoryFilter: Int? = currentCategoryFilter
    ) {
        currentSearchQuery = searchQuery
        currentCategoryFilter = categoryFilter
        scope.launch {
            val list = dbHelper.getHistory(
                searchQuery = searchQuery,
                categoryFilter = categoryFilter
            )
            val unread = dbHelper.getUnreadCount()
            _historyFlow.value = list
            _unreadCountFlow.value = unread
        }
    }

    suspend fun recordNotification(item: NotificationHistoryItem): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertNotification(item)
        val list = dbHelper.getHistory(
            searchQuery = currentSearchQuery,
            categoryFilter = currentCategoryFilter
        )
        val unread = dbHelper.getUnreadCount()
        _historyFlow.value = list
        _unreadCountFlow.value = unread
        id
    }

    fun markAllAsRead() {
        scope.launch {
            dbHelper.markAllAsRead()
            refreshHistory()
        }
    }

    fun markAsRead(id: Long) {
        scope.launch {
            dbHelper.markAsRead(id)
            refreshHistory()
        }
    }

    fun deleteNotification(id: Long) {
        scope.launch {
            dbHelper.deleteNotification(id)
            refreshHistory()
        }
    }

    fun clearHistory() {
        scope.launch {
            dbHelper.clearHistory()
            refreshHistory()
        }
    }
}
