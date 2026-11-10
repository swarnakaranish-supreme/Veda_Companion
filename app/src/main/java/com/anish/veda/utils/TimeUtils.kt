package com.anish.veda.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Formats the current time according to ESP32-C3 firmware protocol:
     * "T|YYYY|MM|DD|HH|MM|SS"
     */
    fun createTimeSyncPayload(date: Date = Date()): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val day = calendar.get(Calendar.DAY_OF_MONTH) // 1-31
        val hour = calendar.get(Calendar.HOUR_OF_DAY) // 0-23
        val minute = calendar.get(Calendar.MINUTE) // 0-59
        val second = calendar.get(Calendar.SECOND) // 0-59

        return String.format(
            Locale.US,
            "T|%04d|%02d|%02d|%02d|%02d|%02d",
            year,
            month,
            day,
            hour,
            minute,
            second
        )
    }

    /**
     * Human readable relative or formatted timestamp
     */
    fun formatSyncTime(timestamp: Long): String {
        if (timestamp <= 0) return "Never synced"
        val diffMs = System.currentTimeMillis() - timestamp
        if (diffMs < 0) return "Just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    fun formatDetailedTimestamp(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
