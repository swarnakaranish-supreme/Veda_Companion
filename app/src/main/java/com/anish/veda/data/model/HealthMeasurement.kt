package com.anish.veda.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Validated timestamped biometric measurement record.
 * "Never invent readings. Store timestamped measurements. Show measurement quality."
 */
data class HealthMeasurement(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int,      // BPM (40..220)
    val spo2: Int,           // % (70..100)
    val quality: Int,        // % (0..100)
    val status: String = determineStatus(quality)
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDateOnly: String
        get() {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedHourOnly: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    companion object {
        fun determineStatus(quality: Int): String {
            return when {
                quality >= 75 -> "OPTIMAL"
                quality >= 50 -> "GOOD"
                quality >= 25 -> "FAIR"
                else -> "LOW_SIGNAL"
            }
        }
    }
}

data class DailyHealthSummary(
    val dateLabel: String,
    val avgHeartRate: Int,
    val avgSpo2: Int,
    val sampleCount: Int
)
