package com.anish.veda.notifications

import android.app.Notification
import com.anish.veda.utils.Constants

object NotificationCategory {

    /**
     * Maps an Android Notification category and package name into the firmware category byte
     */
    fun resolveCategory(androidCategory: String?, packageName: String): Int {
        val lowerPkg = packageName.lowercase()

        // 1. Check android-native category string
        when (androidCategory) {
            Notification.CATEGORY_CALL -> return Constants.CATEGORY_INCOMING_CALL
            Notification.CATEGORY_MISSED_CALL -> return Constants.CATEGORY_MISSED_CALL
            Notification.CATEGORY_MESSAGE -> return Constants.CATEGORY_MESSAGING
            Notification.CATEGORY_EMAIL -> return Constants.CATEGORY_EMAIL
            Notification.CATEGORY_EVENT, Notification.CATEGORY_REMINDER -> return Constants.CATEGORY_CALENDAR
        }

        // 2. Fallback to package heuristics
        return when {
            lowerPkg.contains("dialer") || lowerPkg.contains("telecom") || lowerPkg.contains("phone") ->
                Constants.CATEGORY_INCOMING_CALL

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("signal") ||
                    lowerPkg.contains("mms") || lowerPkg.contains("messaging") || lowerPkg.contains("discord") ||
                    lowerPkg.contains("slack") || lowerPkg.contains("viber") || lowerPkg.contains("wechat") ||
                    lowerPkg.contains("zalo") || lowerPkg.contains("messenger") ->
                Constants.CATEGORY_MESSAGING

            lowerPkg.contains("mail") || lowerPkg.contains("gmail") || lowerPkg.contains("outlook") ||
                    lowerPkg.contains("yahoo") || lowerPkg.contains("thunderbird") ->
                Constants.CATEGORY_EMAIL

            lowerPkg.contains("calendar") || lowerPkg.contains("clock") || lowerPkg.contains("alarm") ||
                    lowerPkg.contains("tasks") || lowerPkg.contains("reminder") ->
                Constants.CATEGORY_CALENDAR

            else -> Constants.CATEGORY_GENERIC
        }
    }

    fun getCategoryName(category: Int): String {
        return when (category) {
            Constants.CATEGORY_GENERIC -> "Generic App"
            Constants.CATEGORY_INCOMING_CALL -> "Incoming Call"
            Constants.CATEGORY_MISSED_CALL -> "Missed Call"
            Constants.CATEGORY_MESSAGING -> "Instant Message"
            Constants.CATEGORY_CALENDAR -> "Calendar / Reminder"
            Constants.CATEGORY_EMAIL -> "Email"
            else -> "App ($category)"
        }
    }
}
