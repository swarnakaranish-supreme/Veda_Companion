package com.anish.veda.bluetooth

import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Locale

object BleProtocol {

    // Firmware capacity limits from ble_phone.h & ble_phone.cpp
    // BlePhoneNotification struct: char app[49], char title[73], char message[181]
    const val MAX_APP_LENGTH = 48
    const val MAX_TITLE_LENGTH = 72
    const val MAX_MESSAGE_LENGTH = 180

    // Categories defined in firmware
    const val CATEGORY_NORMAL = 0
    const val CATEGORY_INCOMING_CALL = 1
    const val CATEGORY_MISSED_CALL = 2
    const val CATEGORY_MESSAGING = 4
    const val CATEGORY_CALENDAR = 5
    const val CATEGORY_EMAIL = 6

    /**
     * Sanitizes pipe delimiters, newlines and carriage returns without length truncation.
     */
    fun sanitizePipes(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input
            .replace('|', '/')
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
    }

    /**
     * Sanitizes a string for watch protocol by removing reserved pipe '|' characters,
     * newlines, carriage returns and limiting max length.
     */
    fun sanitizeField(input: String?, maxLength: Int): String {
        if (input.isNullOrEmpty()) return ""
        val cleaned = input
            .replace('|', '/')
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
        return if (cleaned.length > maxLength) {
            cleaned.substring(0, maxLength)
        } else {
            cleaned
        }
    }

    /**
     * Formats time synchronization payload: "T|YEAR|MONTH|DAY|HOUR|MINUTE|SECOND"
     * Example: "T|2026|11|09|14|30|00"
     * Validates year between 2024 and 2099 per firmware sscanf constraints.
     */
    fun buildTimePacket(calendar: Calendar = Calendar.getInstance()): String {
        val y = calendar.get(Calendar.YEAR).coerceIn(2024, 2099)
        val mo = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val mi = calendar.get(Calendar.MINUTE)
        val s = calendar.get(Calendar.SECOND)

        return String.format(Locale.US, "T|%04d|%02d|%02d|%02d|%02d|%02d", y, mo, d, h, mi, s)
    }

    /**
     * Formats normal notification payload: "N|0|APP_NAME|TITLE|MESSAGE"
     */
    fun buildNormalNotificationPacket(
        appName: String,
        title: String,
        message: String,
        maxMessageLen: Int = MAX_MESSAGE_LENGTH
    ): String {
        return buildNotificationPacket(
            category = CATEGORY_NORMAL,
            app = appName,
            title = title,
            message = message,
            customMaxMsgLength = maxMessageLen
        )
    }

    /**
     * Formats incoming call payload: "N|1|Phone|Incoming call|CALLER_NAME"
     */
    fun buildIncomingCallPacket(callerNameOrNumber: String): String {
        return buildNotificationPacket(
            category = CATEGORY_INCOMING_CALL,
            app = "Phone",
            title = "Incoming call",
            message = callerNameOrNumber
        )
    }

    /**
     * Formats missed call payload: "N|2|Phone|Missed call|CALLER_NAME"
     */
    fun buildMissedCallPacket(callerNameOrNumber: String): String {
        return buildNotificationPacket(
            category = CATEGORY_MISSED_CALL,
            app = "Phone",
            title = "Missed call",
            message = callerNameOrNumber
        )
    }

    /**
     * Generic notification builder: "N|category|app|title|message"
     */
    fun buildNotificationPacket(
        category: Int,
        app: String,
        title: String,
        message: String,
        customMaxMsgLength: Int = MAX_MESSAGE_LENGTH
    ): String {
        val safeCategory = category.coerceIn(0, 255)
        val safeApp = sanitizeField(app, MAX_APP_LENGTH)
        val safeTitle = sanitizeField(title, MAX_TITLE_LENGTH)
        val effectiveMaxMsg = customMaxMsgLength.coerceIn(10, MAX_MESSAGE_LENGTH)
        val safeMessage = sanitizeField(message, effectiveMaxMsg)

        return "N|$safeCategory|$safeApp|$safeTitle|$safeMessage"
    }

    /**
     * Truncates and adjusts the packet to ensure its UTF-8 byte length does not
     * exceed the maximum allowable ATT payload for the given MTU: (mtu - 3) bytes.
     *
     * In the watch firmware, onWrite receives the entire packet in one GATT write.
     * Never assume a large payload fits into one GATT write without MTU bounds.
     */
    fun fitPacketToMtu(packet: String, mtu: Int): String {
        val maxPayloadBytes = (mtu - 3).coerceAtLeast(20)
        val packetBytes = packet.toByteArray(StandardCharsets.UTF_8)
        if (packetBytes.size <= maxPayloadBytes) {
            return packet
        }

        // If it's a Time packet, it's 21 bytes; if MTU is tiny (e.g. 23 bytes = 20 payload),
        // we return as is or compact format
        if (packet.startsWith("T|")) {
            return packet
        }

        // If it's a Notification packet: N|category|app|title|message
        if (packet.startsWith("N|")) {
            val parts = packet.split('|')
            if (parts.size >= 5) {
                val cat = parts[1]
                var app = parts[2]
                var title = parts[3]
                var msg = parts.subList(4, parts.size).joinToString("|")

                val prefix = "N|$cat|$app|$title|"
                val prefixBytes = prefix.toByteArray(StandardCharsets.UTF_8).size

                if (prefixBytes < maxPayloadBytes) {
                    val remainingBytes = maxPayloadBytes - prefixBytes
                    val truncatedMsg = truncateUtf8ToBytes(msg, remainingBytes)
                    return "$prefix$truncatedMsg"
                } else {
                    // Header itself exceeds MTU; compress app & title
                    app = sanitizeField(app, 8)
                    title = sanitizeField(title, 12)
                    val shortPrefix = "N|$cat|$app|$title|"
                    val shortPrefixBytes = shortPrefix.toByteArray(StandardCharsets.UTF_8).size
                    val remainingBytes = (maxPayloadBytes - shortPrefixBytes).coerceAtLeast(0)
                    val truncatedMsg = truncateUtf8ToBytes(msg, remainingBytes)
                    return "$shortPrefix$truncatedMsg"
                }
            }
        }

        return truncateUtf8ToBytes(packet, maxPayloadBytes)
    }

    /**
     * Safely truncates a UTF-8 string to not exceed [maxBytes] without splitting
     * multi-byte code points or UTF-16 surrogate pairs (such as emojis).
     */
    fun truncateUtf8ToBytes(str: String, maxBytes: Int): String {
        if (str.isEmpty() || maxBytes <= 0) return ""
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size <= maxBytes) return str

        var byteCount = 0
        val sb = StringBuilder()
        var offset = 0
        while (offset < str.length) {
            val codePoint = str.codePointAt(offset)
            val charCount = Character.charCount(codePoint)
            val cpString = str.substring(offset, offset + charCount)
            val cpBytes = cpString.toByteArray(StandardCharsets.UTF_8).size

            if (byteCount + cpBytes > maxBytes) break
            sb.append(cpString)
            byteCount += cpBytes
            offset += charCount
        }
        return sb.toString()
    }

    /**
     * Validates whether a payload string conforms to the firmware's time packet format.
     */
    fun isValidTimePacket(packet: String): Boolean {
        if (!packet.startsWith("T|")) return false
        val parts = packet.split('|')
        if (parts.size != 7) return false
        return try {
            val y = parts[1].toInt()
            val mo = parts[2].toInt()
            val d = parts[3].toInt()
            val h = parts[4].toInt()
            val mi = parts[5].toInt()
            val s = parts[6].toInt()
            y in 2024..2099 && mo in 1..12 && d in 1..31 && h in 0..23 && mi in 0..59 && s in 0..59
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates whether a payload conforms to the firmware's notification packet format:
     * N|category|app|title|message (must contain at least 4 pipe separators).
     */
    fun isValidNotificationPacket(packet: String): Boolean {
        if (!packet.startsWith("N|")) return false
        val p1 = packet.indexOf('|', 2)
        if (p1 < 0) return false
        val p2 = packet.indexOf('|', p1 + 1)
        if (p2 < 0) return false
        val p3 = packet.indexOf('|', p2 + 1)
        if (p3 < 0) return false
        return true
    }

    // ========================================================================
    // VEDA V2 HEALTH TELEMETRY PROTOCOL
    // Format from Watch: "H|BPM|SPO2|QUALITY"
    // Commands to Watch: "M|START" / "M|STOP"
    // ========================================================================
    const val CMD_START_MEASUREMENT = "M|START"
    const val CMD_STOP_MEASUREMENT = "M|STOP"

    // ========================================================================
    // VEDA V2 CONFIGURATION & WATCH STUDIO PROTOCOL
    // Packets: "C|FACE|<idx>", "C|BRI|<0..255>", "C|TO|<sec>", "C|FMT|<12|24>|<fmt>"
    // ========================================================================
    fun buildSelectFacePacket(faceIndex: Int): String {
        return "C|FACE|${faceIndex.coerceIn(0, 7)}"
    }

    fun buildBrightnessPacket(brightnessPercent: Int): String {
        val pwm = (brightnessPercent.coerceIn(0, 100) * 2.55f).toInt()
        return "C|BRI|$pwm"
    }

    fun buildTimeoutPacket(timeoutSeconds: Int): String {
        return "C|TO|${timeoutSeconds.coerceIn(0, 3600)}"
    }

    fun buildFormatPacket(is24Hour: Boolean, dateFormat: String): String {
        val hourCode = if (is24Hour) 24 else 12
        val safeDateFmt = sanitizeField(dateFormat, 15)
        return "C|FMT|$hourCode|$safeDateFmt"
    }

    fun buildDndPacket(enabled: Boolean): String {
        return "C|DND|${if (enabled) 1 else 0}"
    }

    fun buildSetAlarmPacket(hour: Int, minute: Int, active: Boolean): String {
        return "A|SET|${hour.coerceIn(0, 23)}|${minute.coerceIn(0, 59)}|${if (active) 1 else 0}"
    }

    fun buildDeleteAlarmPacket(): String {
        return "A|DEL"
    }

    data class HealthPacket(
        val bpm: Int,
        val spo2: Int,
        val quality: Int,
        val hasFinger: Boolean
    )

    fun parseHealthPacket(payload: String): HealthPacket? {
        if (!payload.startsWith("H|")) return null
        val parts = payload.trim().split('|')
        if (parts.size < 4) return null
        return try {
            val bpm = parts[1].toInt()
            val spo2 = parts[2].toInt()
            val quality = parts[3].toInt()
            val hasFinger = bpm > 0 || spo2 > 0 || quality > 0
            HealthPacket(
                bpm = bpm.coerceIn(0, 250),
                spo2 = spo2.coerceIn(0, 100),
                quality = quality.coerceIn(0, 100),
                hasFinger = hasFinger
            )
        } catch (e: Exception) {
            null
        }
    }

    // ========================================================================
    // VEDA V2 HAPTIC PROTOCOL (DRV2605L)
    // Commands: "V|PLAY|<id>", "V|CFG|<call>|<msg>|<alarm>|<emergency>|<nav>|<nav_eff>", "V|STOP"
    // ========================================================================
    fun buildHapticPlayPacket(effectId: Int): String {
        return "V|PLAY|${effectId.coerceIn(1, 123)}"
    }

    fun buildHapticConfigPacket(
        callProfile: Int,
        msgProfile: Int,
        alarmProfile: Int,
        emergencyProfile: Int,
        navFeedback: Boolean,
        navEffectId: Int
    ): String {
        val navVal = if (navFeedback) 1 else 0
        return "V|CFG|${callProfile.coerceIn(0, 3)}|${msgProfile.coerceIn(0, 4)}|${alarmProfile.coerceIn(0, 3)}|${emergencyProfile.coerceIn(0, 2)}|$navVal|${navEffectId.coerceIn(1, 123)}"
    }

    fun buildHapticStopPacket(): String {
        return "V|STOP"
    }

    // ========================================================================
    // VEDA V2 GUARDIAN SAFETY PROTOCOL
    // Packets: "E|SOS|TRIGGER", "E|SOS|CANCEL", "E|SOS|DISPATCH", "E|CFG|<gesture>|<countdown>"
    // ========================================================================
    fun buildEmergencyTriggerPacket(): String = "E|SOS|TRIGGER"
    fun buildEmergencyCancelPacket(): String = "E|SOS|CANCEL"
    fun buildGuardianConfigPacket(gestureMode: Int, countdownSeconds: Int): String {
        return "E|CFG|${gestureMode.coerceIn(0, 1)}|${countdownSeconds.coerceIn(3, 60)}"
    }

    fun parseEmergencyPacket(payload: String): String? {
        if (!payload.startsWith("E|SOS|")) return null
        return payload.removePrefix("E|SOS|").trim()
    }

    // ========================================================================
    // VEDA V2 DEVICE MANAGEMENT & DIAGNOSTICS PROTOCOL
    // Packets: "D|REQ", "D|PING", "D|PONG", "D|INFO|<name>|<fw>|<proto>|<sensors>|<battery>"
    // ========================================================================
    fun buildDeviceInfoRequestPacket(): String = "D|REQ"
    fun buildPingPacket(): String = "D|PING"

    data class DeviceInfoPacket(
        val watchName: String,
        val firmwareVersion: String,
        val protocolVersion: String,
        val sensorStatus: String,
        val batteryStatus: String
    )

    fun parseDeviceInfoPacket(payload: String): DeviceInfoPacket? {
        if (!payload.startsWith("D|INFO|")) return null
        val parts = payload.split('|')
        if (parts.size < 7) return null
        return DeviceInfoPacket(
            watchName = parts[2],
            firmwareVersion = parts[3],
            protocolVersion = parts[4],
            sensorStatus = parts[5],
            batteryStatus = parts[6]
        )
    }

    fun isPongPacket(payload: String): Boolean = payload.startsWith("D|PONG")
}
