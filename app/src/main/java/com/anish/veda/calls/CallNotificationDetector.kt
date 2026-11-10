package com.anish.veda.calls

import android.app.Notification
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.anish.veda.bluetooth.BleProtocol

/**
 * Intelligent detector for phone-call notifications posted by dialer and telecom apps.
 * Optimized for OPPO ColorOS (Android 15), Google Dialer, and standard Android telecom subsystems.
 */
object CallNotificationDetector {

    private val DIALER_PACKAGE_KEYWORDS = listOf(
        "dialer",
        "telecom",
        "telephony",
        "incallui",
        "phone",
        "call",
        "coloros.phoneno",
        "oppo.telephony",
        "oplus"
    )

    /**
     * Determines whether a posted StatusBarNotification represents an active ringing or incoming call.
     */
    fun isIncomingCall(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val pkg = sbn.packageName.lowercase()

        // 1. Standard Android Call Category
        if (notification.category == Notification.CATEGORY_CALL) {
            return true
        }

        // 2. Android Telecom Call Person or Call Style
        val extras = notification.extras
        if (extras != null) {
            if (extras.containsKey(Notification.EXTRA_CALL_PERSON) ||
                extras.containsKey("android.callType") ||
                extras.containsKey(NotificationCompat.EXTRA_CALL_PERSON)
            ) {
                return true
            }

            // Android 12+ CallStyle template check
            val template = extras.getString(Notification.EXTRA_TEMPLATE)
            if (template != null && template.contains("CallStyle", ignoreCase = true)) {
                return true
            }
        }

        // 3. Dialer package heuristics (OPPO A59 5G / ColorOS dialer)
        if (isDialerOrTelecomPackage(pkg)) {
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.lowercase() ?: ""

            if (title.contains("incoming") || title.contains("ringing") ||
                text.contains("incoming call") || text.contains("ringing") ||
                subText.contains("incoming call")
            ) {
                return true
            }

            // Check action buttons for Answer / Decline
            notification.actions?.forEach { action ->
                val actionTitle = action.title?.toString()?.lowercase() ?: ""
                if (actionTitle.contains("answer") || actionTitle.contains("accept") ||
                    actionTitle.contains("decline") || actionTitle.contains("reject")
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Determines whether a posted StatusBarNotification represents an unanswered / missed call.
     */
    fun isMissedCall(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val pkg = sbn.packageName.lowercase()

        // 1. Standard Android Missed Call Category
        if (notification.category == Notification.CATEGORY_MISSED_CALL) {
            return true
        }

        // 2. Dialer package heuristics for missed call notifications
        if (isDialerOrTelecomPackage(pkg)) {
            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""

            if (title.contains("missed call") || text.contains("missed call") ||
                title.contains("missed") || text.contains("missed")
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts the caller name or phone number from the notification extras.
     * Returns null if caller details are unavailable or cannot be determined.
     */
    fun extractCallerName(sbn: StatusBarNotification): String? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        // 1. Check title (primary location for contact name or phone number)
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        if (isValidCallerName(title)) {
            return BleProtocol.sanitizePipes(title!!)
        }

        // 2. Check Call Person extra
        val person = extras.getCharSequence(Notification.EXTRA_CALL_PERSON)?.toString()?.trim()
        if (isValidCallerName(person)) {
            return BleProtocol.sanitizePipes(person!!)
        }

        // 3. Fallback to text if title was generic (e.g. title: "Incoming Call", text: "Mom")
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (isValidCallerName(text)) {
            return BleProtocol.sanitizePipes(text!!)
        }

        return null
    }

    private fun isValidCallerName(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val lower = candidate.lowercase()
        // Reject generic system strings
        if (lower == "incoming call" ||
            lower == "outgoing call" ||
            lower == "missed call" ||
            lower == "call ended" ||
            lower == "dialing" ||
            lower == "phone" ||
            lower == "ringing"
        ) {
            return false
        }
        return true
    }

    fun isDialerOrTelecomPackage(pkg: String): Boolean {
        return DIALER_PACKAGE_KEYWORDS.any { pkg.contains(it) }
    }
}
