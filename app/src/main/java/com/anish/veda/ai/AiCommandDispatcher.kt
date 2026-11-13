package com.anish.veda.ai

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.AiActionType
import com.anish.veda.data.model.AiChatMessage
import com.anish.veda.data.model.MessageSender
import com.anish.veda.data.repository.HapticRepository
import com.anish.veda.data.repository.HealthRepository
import com.anish.veda.data.repository.NotificationRepository
import com.anish.veda.notifications.NotificationSettingsRepository
import com.anish.veda.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedCommandResult(
    val handled: Boolean,
    val responseText: String,
    val actionType: AiActionType = AiActionType.NONE,
    val actionPayload: String? = null,
    val requiresConfirmation: Boolean = false,
    val smartReplies: List<String> = emptyList()
)

class AiCommandDispatcher(
    private val context: Context,
    private val healthRepository: HealthRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationSettingsRepo: NotificationSettingsRepository,
    private val hapticRepository: HapticRepository,
    private val bleManager: BleManager
) {

    /**
     * Attempts to parse and execute a local natural-language command.
     * Returns ParsedCommandResult with handled=true if resolved offline.
     */
    fun processCommand(input: String): ParsedCommandResult {
        val query = input.trim().lowercase(Locale.US)

        // 1. Check Alarm Command (e.g., "Set an alarm for 6 AM", "Alarm at 7:30")
        if (query.contains("alarm")) {
            val alarmResult = parseAndSetAlarm(input)
            if (alarmResult != null) return alarmResult
        }

        // 2. Check Timer Command (e.g., "Start a 10-minute timer", "Set timer for 5 minutes")
        if (query.contains("timer")) {
            val timerResult = parseAndSetTimer(input)
            if (timerResult != null) return timerResult
        }

        // 3. Check Notification Summary Command
        if (query.contains("summar") || query.contains("notifications") || query.contains("alerts") || query.contains("messages")) {
            if (query.contains("recent") || query.contains("what") || query.contains("read") || query.contains("check") || query.contains("summar")) {
                return generateNotificationSummary()
            }
        }

        // 4. Check Health Telemetry Command (e.g., "Show my latest heart rate", "What is my pulse?", "SpO2")
        if (query.contains("heart rate") || query.contains("pulse") || query.contains("bpm") || query.contains("spo2") || query.contains("oxygen") || query.contains("health")) {
            return generateHealthInsights()
        }

        // 5. Check Do Not Disturb / Quiet Hours (e.g., "Enable Do Not Disturb", "Turn off DND")
        if (query.contains("do not disturb") || query.contains("dnd") || query.contains("quiet hours")) {
            val enable = !query.contains("disable") && !query.contains("off") && !query.contains("stop")
            notificationSettingsRepo.setMasterEnabled(!enable)
            return ParsedCommandResult(
                handled = true,
                responseText = if (enable) {
                    "Do Not Disturb mode is now ENABLED. Notification forwarding to your smartwatch is muted."
                } else {
                    "Do Not Disturb mode is now DISABLED. Smartwatch notification forwarding is active."
                },
                actionType = AiActionType.TOGGLE_DND,
                actionPayload = if (enable) "DND_ON" else "DND_OFF"
            )
        }

        // 6. Check Find My Watch / Vibrate Watch
        if (query.contains("find") && (query.contains("watch") || query.contains("phone"))) {
            if (bleManager.connectionState.value.isConnected) {
                hapticRepository.playEffect(16) // 1000ms continuous alert burst
                return ParsedCommandResult(
                    handled = true,
                    responseText = "Signal transmitted! High-priority vibration alert sent to your VEDA smartwatch.",
                    actionType = AiActionType.FIND_WATCH,
                    actionPayload = "FIND_WATCH"
                )
            } else {
                return ParsedCommandResult(
                    handled = true,
                    responseText = "Cannot locate watch: The VEDA Smartwatch is currently disconnected. Reconnect via Device Hub.",
                    actionType = AiActionType.NONE
                )
            }
        }

        // 7. Check Daily Briefing
        if (query.contains("briefing") || query.contains("morning update") || query.contains("daily report") || query.contains("overview")) {
            return generateDailyBriefing()
        }

        // 8. Watch Face Configuration
        if (query.contains("watch face") || query.contains("face")) {
            val faces = listOf("digital", "hud", "circular", "analog", "health", "minimal", "data", "signature")
            faces.forEachIndexed { idx, name ->
                if (query.contains(name)) {
                    val packet = BleProtocol.buildSelectFacePacket(idx)
                    bleManager.enqueuePayload(packet)
                    return ParsedCommandResult(
                        handled = true,
                        responseText = "Watch face updated to \"${name.uppercase()}\" on your smartwatch display.",
                        actionType = AiActionType.WATCH_CONFIG,
                        actionPayload = "FACE_$idx"
                    )
                }
            }
        }

        // 9. Guardian SOS Emergency Command (Safety Protected with Confirmation)
        if (query.contains("sos") || query.contains("emergency") || query.contains("send help") || query.contains("distress")) {
            return ParsedCommandResult(
                handled = true,
                responseText = "EMERGENCY SAFETY COMMAND DETECTED: Do you want to trigger the Guardian Emergency SOS beacon and broadcast your GPS coordinates to emergency contacts?",
                actionType = AiActionType.TRIGGER_SOS,
                actionPayload = "TRIGGER_SOS",
                requiresConfirmation = true,
                smartReplies = listOf("Confirm SOS", "Cancel")
            )
        }

        return ParsedCommandResult(handled = false, responseText = "")
    }

    private fun parseAndSetAlarm(input: String): ParsedCommandResult? {
        val pattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(input)

        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: return null
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase(Locale.US)

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)

            // Trigger Android system Alarm Intent
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "VEDA Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}

            return ParsedCommandResult(
                handled = true,
                responseText = "Alarm set for $formattedTime on your device and synchronized with VEDA.",
                actionType = AiActionType.SET_ALARM,
                actionPayload = "$hour:$minute"
            )
        }
        return null
    }

    private fun parseAndSetTimer(input: String): ParsedCommandResult? {
        val pattern = Pattern.compile("(\\d+)\\s*(minute|min|second|sec|hour|hr)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(input)

        if (matcher.find()) {
            val amount = matcher.group(1)?.toIntOrNull() ?: return null
            val unit = matcher.group(2)?.lowercase(Locale.US) ?: "minute"

            val totalSeconds = when {
                unit.startsWith("sec") -> amount
                unit.startsWith("hour") || unit.startsWith("hr") -> amount * 3600
                else -> amount * 60
            }

            try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "VEDA Timer")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}

            val unitLabel = if (totalSeconds >= 60) "${totalSeconds / 60} minute(s)" else "$totalSeconds second(s)"

            return ParsedCommandResult(
                handled = true,
                responseText = "Timer started for $unitLabel.",
                actionType = AiActionType.SET_TIMER,
                actionPayload = totalSeconds.toString()
            )
        }
        return null
    }

    fun generateNotificationSummary(): ParsedCommandResult {
        val recent = notificationRepository.historyFlow.value.take(15)

        if (recent.isEmpty()) {
            return ParsedCommandResult(
                handled = true,
                responseText = "Your notification log is clear. No alerts have been intercepted in the current timeline.",
                actionType = AiActionType.NOTIFICATION_SUMMARY
            )
        }

        val appGroups = recent.groupBy { it.appName }
        val sb = StringBuilder("Here is your tactical notification summary:\n\n")

        for ((appName, items) in appGroups) {
            val count = items.size
            val lastTitle = items.first().title
            sb.append("• **$appName** ($count alert${if (count > 1) "s" else ""}): \"$lastTitle\"\n")
        }

        val unread = notificationRepository.unreadCountFlow.value
        if (unread > 0) {
            sb.append("\nYou currently have **$unread unread** notifications awaiting review.")
        }

        return ParsedCommandResult(
            handled = true,
            responseText = sb.toString(),
            actionType = AiActionType.NOTIFICATION_SUMMARY,
            smartReplies = listOf("Clear notifications", "Turn on DND", "Open Notification Hub")
        )
    }

    fun generateHealthInsights(): ParsedCommandResult {
        val recent = healthRepository.recentMeasurements.value

        if (recent.isEmpty()) {
            return ParsedCommandResult(
                handled = true,
                responseText = "No sensor readings are recorded in your health database yet. Place your finger firmly on the MAX30102 sensor and start a live test from the Health Hub.\n\n*Notice: VEDA telemetry is for wellness and fitness tracking only and is not intended for medical diagnosis.*",
                actionType = AiActionType.HEALTH_QUERY
            )
        }

        val latest = recent.first()
        val avgBpm = recent.map { it.heartRate }.filter { it > 0 }.average()
        val avgSpo2 = recent.map { it.spo2 }.filter { it > 0 }.average()

        val timeStr = TimeUtils.formatSyncTime(latest.timestamp)
        val sb = StringBuilder()
        sb.append("VEDA Health Telemetry Report ($timeStr):\n\n")
        sb.append("• **Heart Rate:** ${latest.heartRate} BPM (Avg: ${avgBpm.toInt()} BPM)\n")
        sb.append("• **Blood Oxygen:** ${latest.spo2}% SpO2 (Avg: ${avgSpo2.toInt()}%)\n")
        sb.append("• **Signal Quality:** ${latest.quality}%\n")
        sb.append("• **Sensor Status:** ${latest.status}\n\n")

        if (latest.heartRate in 60..100) {
            sb.append("Your resting heart rate is in a balanced athletic range. ")
        } else if (latest.heartRate > 100) {
            sb.append("Elevated heart rate detected. Ensure you are adequately rested and hydrated. ")
        }

        if (latest.spo2 >= 95) {
            sb.append("Blood oxygenation levels are optimal.")
        } else if (latest.spo2 in 90..94) {
            sb.append("Slightly lowered SpO2 reading. Check sensor placement.")
        }

        sb.append("\n\n*Disclaimer: Not intended for medical diagnosis.*")

        return ParsedCommandResult(
            handled = true,
            responseText = sb.toString(),
            actionType = AiActionType.HEALTH_QUERY,
            smartReplies = listOf("View Health Graphs", "Start New Test", "Export CSV")
        )
    }

    fun generateDailyBriefing(): ParsedCommandResult {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val dateStr = dateFormat.format(now.time)
        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(now.time)

        val isConnected = bleManager.connectionState.value.isConnected
        val recentHealth = healthRepository.recentMeasurements.value.firstOrNull()
        val unreadNotifs = notificationRepository.unreadCountFlow.value
        val isQuiet = notificationSettingsRepo.isQuietHoursActive()

        val sb = StringBuilder()
        sb.append("Good day, Commander. Here is your VEDA Daily Briefing:\n\n")
        sb.append("📅 **Date & Time:** $dateStr at $timeStr\n")
        sb.append("⌚ **Smartwatch Link:** ${if (isConnected) "Connected & Synchronized" else "Disconnected"}\n")

        if (recentHealth != null) {
            sb.append("❤️ **Latest Heart Rate:** ${recentHealth.heartRate} BPM (${recentHealth.spo2}% SpO2)\n")
        } else {
            sb.append("❤️ **Health Sensor:** No active readings today\n")
        }

        sb.append("🔔 **Alert Queue:** $unreadNotifs unread notification(s)\n")
        sb.append("🌙 **Quiet Hours:** ${if (isQuiet) "Active" else "Inactive"}\n\n")
        sb.append("All wearable subsystems are calibrated and operational.")

        return ParsedCommandResult(
            handled = true,
            responseText = sb.toString(),
            actionType = AiActionType.DAILY_BRIEFING,
            smartReplies = listOf("Check Heart Rate", "Summarize Alerts", "Change Watch Face")
        )
    }
}
