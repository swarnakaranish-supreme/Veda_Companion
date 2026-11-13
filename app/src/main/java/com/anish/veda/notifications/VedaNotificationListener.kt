package com.anish.veda.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.anish.veda.VedaApplication
import com.anish.veda.calls.CallNotificationDetector
import com.anish.veda.calls.CallStateManager
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.NotificationHistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VedaNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val app by lazy { VedaApplication.instance }
    private val logRepository by lazy { app.logRepository }
    private val bleManager by lazy { app.bleManager }
    private val preferencesRepository by lazy { app.preferencesRepository }
    private val notificationSettingsRepo by lazy { app.notificationSettingsRepository }
    private val notificationRepo by lazy { app.notificationRepository }

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
            settingsRepository = notificationSettingsRepo,
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
            if (CallNotificationDetector.isIncomingCall(sbn)) {
                val caller = CallNotificationDetector.extractCallerName(sbn)
                CallStateManager.onIncomingCallNotification(applicationContext, caller, sbn.packageName)
                return
            }

            if (CallNotificationDetector.isMissedCall(sbn)) {
                val caller = CallNotificationDetector.extractCallerName(sbn)
                CallStateManager.onMissedCallNotification(applicationContext, caller, sbn.packageName)
                return
            }

            // 2. Normal Notification Pipeline
            val (extractedTitle, extractedText) = notificationProcessor.extractContent(sbn)
            val category = NotificationCategory.resolveCategory(sbn.notification?.category, sbn.packageName)
            val categoryName = NotificationCategory.getCategoryName(category)
            val appName = notificationProcessor.resolveAppName(sbn.packageName)
            val isVip = notificationSettingsRepo.isContactVip(extractedTitle) ||
                    notificationSettingsRepo.isContactVip(extractedText)

            // Evaluate filter rules (master toggle, app filter, deduplication, quiet hours, vip, etc.)
            val decision = notificationFilter.shouldProcess(sbn, extractedTitle, extractedText, category)

            when (decision) {
                is FilterResult.Allow -> {
                    // Forward using format: N|category|APP_NAME|TITLE|MESSAGE
                    notificationProcessor.processAndForward(
                        packageName = sbn.packageName,
                        rawTitle = extractedTitle,
                        rawText = extractedText,
                        category = category
                    )

                    // Record forwarded event in database history
                    serviceScope.launch {
                        notificationRepo.recordNotification(
                            NotificationHistoryItem(
                                timestamp = System.currentTimeMillis(),
                                packageName = sbn.packageName,
                                appName = appName,
                                title = extractedTitle,
                                message = extractedText,
                                category = category,
                                categoryName = categoryName,
                                wasForwarded = true,
                                rejectionReason = null,
                                isRead = false,
                                isVip = isVip
                            )
                        )
                    }
                }
                is FilterResult.Reject -> {
                    // Don't log own package or empty notifications to user timeline
                    if (sbn.packageName != packageName && (extractedTitle.isNotBlank() || extractedText.isNotBlank())) {
                        serviceScope.launch {
                            notificationRepo.recordNotification(
                                NotificationHistoryItem(
                                    timestamp = System.currentTimeMillis(),
                                    packageName = sbn.packageName,
                                    appName = appName,
                                    title = extractedTitle,
                                    message = extractedText,
                                    category = category,
                                    categoryName = categoryName,
                                    wasForwarded = false,
                                    rejectionReason = decision.reason,
                                    isRead = false,
                                    isVip = isVip
                                )
                            )
                        }
                    }
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
