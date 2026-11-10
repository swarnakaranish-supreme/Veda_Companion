package com.anish.veda.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.anish.veda.VedaApplication
import com.anish.veda.calls.CallNotificationDetector
import com.anish.veda.calls.CallStateManager
import com.anish.veda.data.model.LogDirection

class VedaNotificationListener : NotificationListenerService() {

    private val app by lazy { VedaApplication.instance }
    private val logRepository by lazy { app.logRepository }
    private val bleManager by lazy { app.bleManager }
    private val preferencesRepository by lazy { app.preferencesRepository }

    private val notificationSettingsRepo by lazy {
        NotificationSettingsRepository(applicationContext)
    }

    private val notificationFilter by lazy {
        NotificationFilter(
            ownPackageName = packageName,
            settingsRepository = notificationSettingsRepo,
            bleManager = bleManager,
            logRepository = logRepository
        )
    }

    private val notificationProcessor by lazy {
        NotificationProcessor(
            context = applicationContext,
            bleManager = bleManager,
            preferencesRepository = preferencesRepository,
            logRepository = logRepository
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logRepository.log("NOTIF_SVC", "NotificationListenerService connected to Android OS.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logRepository.log("NOTIF_SVC", "NotificationListenerService disconnected from Android OS.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            // 1. Phone Call Telemetry Interception
            // Check for Incoming Call Notification (CATEGORY_CALL, CallStyle, or dialer ringing heads-up)
            if (CallNotificationDetector.isIncomingCall(sbn)) {
                val caller = CallNotificationDetector.extractCallerName(sbn)
                CallStateManager.onIncomingCallNotification(applicationContext, caller, sbn.packageName)
                return
            }

            // Check for Missed Call Notification (CATEGORY_MISSED_CALL or dialer missed call)
            if (CallNotificationDetector.isMissedCall(sbn)) {
                val caller = CallNotificationDetector.extractCallerName(sbn)
                CallStateManager.onMissedCallNotification(applicationContext, caller, sbn.packageName)
                return
            }

            // 2. Normal Notification Pipeline
            val (extractedTitle, extractedText) = notificationProcessor.extractContent(sbn)

            // Evaluate filter rules (master toggle, app filter, deduplication, group summary, empty check)
            val decision = notificationFilter.shouldProcess(sbn, extractedTitle, extractedText)

            when (decision) {
                is FilterResult.Allow -> {
                    // Forward using format: N|0|APP_NAME|TITLE|MESSAGE
                    notificationProcessor.processAndForward(
                        packageName = sbn.packageName,
                        rawTitle = extractedTitle,
                        rawText = extractedText
                    )
                }
                is FilterResult.Reject -> {
                    // Filtered out safely
                }
            }
        } catch (e: Exception) {
            logRepository.log("NOTIF_ERR", "Exception processing notification: ${e.message}", LogDirection.ERROR)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
