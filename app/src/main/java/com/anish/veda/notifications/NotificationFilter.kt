package com.anish.veda.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.calls.CallNotificationDetector
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import java.util.concurrent.ConcurrentHashMap

sealed class FilterResult {
    data object Allow : FilterResult()
    data class Reject(val reason: String) : FilterResult()
}

class NotificationFilter(
    private val ownPackageName: String,
    private val settingsRepository: NotificationSettingsRepository,
    private val bleManager: BleManager,
    private val logRepository: LogRepository
) {
    companion object {
        private const val DEDUPLICATION_WINDOW_MS = 6000L
        private const val MAX_CACHE_ENTRIES = 100
    }

    // Cache of recent notification fingerprints to prevent duplicate alerts
    // Key: hash string, Value: timestamp millis
    private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()

    fun shouldProcess(
        sbn: StatusBarNotification,
        extractedTitle: String,
        extractedText: String
    ): FilterResult {
        val pkg = sbn.packageName

        // 1. Check self
        if (pkg == ownPackageName) {
            return FilterResult.Reject("Skipping own companion app notification")
        }

        // 2. Reject call telemetry from regular N|0 notification pipeline
        if (CallNotificationDetector.isIncomingCall(sbn) || CallNotificationDetector.isMissedCall(sbn)) {
            return FilterResult.Reject("Handled as call telemetry ($pkg)")
        }

        // 3. Check master switch
        if (!settingsRepository.isMasterEnabled.value) {
            return FilterResult.Reject("Master notification forwarding is OFF")
        }

        // 4. Check individual app permission
        if (!settingsRepository.isAppAllowed(pkg)) {
            return FilterResult.Reject("Application $pkg is disabled by user filter")
        }

        // 5. Check watch connection state: don't flood queue if watch is offline
        if (!bleManager.connectionState.value.isConnected) {
            return FilterResult.Reject("Watch is currently disconnected")
        }

        val notification = sbn.notification ?: return FilterResult.Reject("Null notification object")

        // 6. Check ongoing notifications (music player, background download, persistent timer)
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) {
            return FilterResult.Reject("Skipping ongoing background event ($pkg)")
        }

        // 7. Check empty content
        if (extractedTitle.isBlank() && extractedText.isBlank()) {
            return FilterResult.Reject("Skipping notification with empty title and body ($pkg)")
        }

        // 8. Check group summary without distinct body
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary && extractedText.isBlank()) {
            return FilterResult.Reject("Skipping empty group summary header ($pkg)")
        }

        // 9. Duplicate detection
        val fingerprint = "$pkg|$extractedTitle|$extractedText"
        val now = System.currentTimeMillis()
        val lastSeen = recentNotificationTimestamps[fingerprint]

        if (lastSeen != null && (now - lastSeen) < DEDUPLICATION_WINDOW_MS) {
            return FilterResult.Reject("Duplicate alert detected within ${DEDUPLICATION_WINDOW_MS}ms window ($pkg)")
        }

        // Register in cache
        cleanOldFingerprints(now)
        recentNotificationTimestamps[fingerprint] = now

        return FilterResult.Allow
    }

    private fun cleanOldFingerprints(now: Long) {
        if (recentNotificationTimestamps.size > MAX_CACHE_ENTRIES) {
            val iterator = recentNotificationTimestamps.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > DEDUPLICATION_WINDOW_MS) {
                    iterator.remove()
                }
            }
        }
    }
}
