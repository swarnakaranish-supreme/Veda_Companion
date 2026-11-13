package com.anish.veda.data.model

data class NotificationHistoryItem(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val category: Int,
    val categoryName: String,
    val wasForwarded: Boolean,
    val rejectionReason: String? = null,
    val isRead: Boolean = false,
    val isVip: Boolean = false
)
