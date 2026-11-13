package com.anish.veda.notifications

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.data.repository.PreferencesRepository

class NotificationProcessor(
    private val context: Context,
    private val bleManager: BleManager,
    private val preferencesRepository: PreferencesRepository,
    private val settingsRepository: NotificationSettingsRepository,
    private val logRepository: LogRepository
) {
    /**
     * Extracts title and body text from a StatusBarNotification, handling
     * standard styles, BigTextStyle, and MessagingStyle.
     */
    fun extractContent(sbn: StatusBarNotification): Pair<String, String> {
        val notification = sbn.notification ?: return "" to ""
        val extras: Bundle = notification.extras ?: return "" to ""

        // 1. Extract Title
        var title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        if (title.isNullOrBlank()) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        }
        if (title.isNullOrBlank()) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        }

        // 2. Extract Message Text
        var text: String? = null

        // Check for MessagingStyle (Android N+, API 24+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messages.isNullOrEmpty()) {
                val lastMsg = messages.last()
                if (lastMsg is Bundle) {
                    val msgText = lastMsg.getCharSequence("text")?.toString()
                    val sender = lastMsg.getCharSequence("sender")?.toString()
                    if (!msgText.isNullOrBlank()) {
                        text = if (!sender.isNullOrBlank() && title.isNullOrBlank()) {
                            title = sender
                            msgText
                        } else if (!sender.isNullOrBlank()) {
                            "$sender: $msgText"
                        } else {
                            msgText
                        }
                    }
                }
            }
        }

        // Check for BigTextStyle
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        }

        // Check for standard text
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        }

        // Check for subText or infoText
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        }
        if (text.isNullOrBlank()) {
            text = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        }

        return (title ?: "").trim() to (text ?: "").trim()
    }

    /**
     * Resolves human-friendly application label.
     */
    fun resolveAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            when (packageName) {
                "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
                "com.instagram.android" -> "Instagram"
                "com.facebook.katana" -> "Facebook"
                "com.facebook.orca" -> "Messenger"
                "org.telegram.messenger" -> "Telegram"
                "com.google.android.gm" -> "Gmail"
                "com.google.android.apps.messaging", "com.android.mms" -> "Messages"
                else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }

    /**
     * Formats and enqueues notification for ESP32 transmission:
     * Format: N|category|APP_NAME|TITLE|MESSAGE
     */
    fun processAndForward(
        packageName: String,
        rawTitle: String,
        rawText: String,
        category: Int
    ) {
        val appName = resolveAppName(packageName)
        val maxMsgLen = preferencesRepository.settingsFlow.value.maxMessageLength

        // Apply Privacy Mode if enabled by user
        val isPrivacy = settingsRepository.privacyModeEnabled.value
        val hideSender = settingsRepository.hideSenderInPrivacy.value

        val effectiveTitle = if (isPrivacy && hideSender) {
            "VEDA Notification"
        } else {
            rawTitle
        }

        val effectiveMessage = if (isPrivacy) {
            "[Content Hidden for Privacy]"
        } else {
            rawText
        }

        // Build standard N|category|APP_NAME|TITLE|MESSAGE packet with pipe sanitization and truncation
        val packet = BleProtocol.buildNotificationPacket(
            category = category,
            app = appName,
            title = effectiveTitle,
            message = effectiveMessage,
            customMaxMsgLength = maxMsgLen
        )

        logRepository.log(
            "NOTIF_PROC",
            "Forwarding cat=$category [$appName]: \"$effectiveTitle\"",
            LogDirection.OUTBOUND,
            packet
        )

        // Queue packet into serialized write queue
        bleManager.enqueuePayload(packet)
        preferencesRepository.incrementNotificationCount()
        lastForwardedNotification = "[$appName] $effectiveTitle"
        lastForwardedTimestamp = System.currentTimeMillis()
    }

    companion object {
        var lastForwardedNotification: String? = null
            private set
        var lastForwardedTimestamp: Long = 0L
            private set
    }
}
