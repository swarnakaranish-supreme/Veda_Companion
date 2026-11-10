package com.anish.veda.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Calendar

class BleProtocolTest {

    @Test
    fun testExactUserAuditPayloads() {
        // 1. Time Synchronization: T|2026|11|09|14|30|00
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val timePacket = BleProtocol.buildTimePacket(calendar)
        assertEquals("T|2026|11|09|14|30|00", timePacket)
        assertTrue(BleProtocol.isValidTimePacket(timePacket))

        // 2. Normal Notification: N|0|WhatsApp|New Message|Hello
        val notifPacket = BleProtocol.buildNormalNotificationPacket(
            appName = "WhatsApp",
            title = "New Message",
            message = "Hello"
        )
        assertEquals("N|0|WhatsApp|New Message|Hello", notifPacket)
        assertTrue(BleProtocol.isValidNotificationPacket(notifPacket))

        // 3. Incoming Call: N|1|Phone|Incoming call|Anish
        val incomingCallPacket = BleProtocol.buildIncomingCallPacket("Anish")
        assertEquals("N|1|Phone|Incoming call|Anish", incomingCallPacket)
        assertTrue(BleProtocol.isValidNotificationPacket(incomingCallPacket))

        // 4. Missed Call: N|2|Phone|Missed call|Anish
        val missedCallPacket = BleProtocol.buildMissedCallPacket("Anish")
        assertEquals("N|2|Phone|Missed call|Anish", missedCallPacket)
        assertTrue(BleProtocol.isValidNotificationPacket(missedCallPacket))
    }

    @Test
    fun testTimePacketFormat() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER) // 10 -> 11
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }

        val packet = BleProtocol.buildTimePacket(calendar)
        assertEquals("T|2026|11|09|14|30|00", packet)
        assertTrue(BleProtocol.isValidTimePacket(packet))
    }

    @Test
    fun testTimePacketValidation() {
        assertTrue(BleProtocol.isValidTimePacket("T|2026|11|09|14|30|00"))
        assertTrue(BleProtocol.isValidTimePacket("T|2024|01|01|00|00|00"))
        assertTrue(BleProtocol.isValidTimePacket("T|2099|12|31|23|59|59"))

        // Invalid cases
        assertFalse(BleProtocol.isValidTimePacket("T|2023|11|09|14|30|00")) // Year < 2024
        assertFalse(BleProtocol.isValidTimePacket("T|2026|13|09|14|30|00")) // Month > 12
        assertFalse(BleProtocol.isValidTimePacket("T|2026|11|32|14|30|00")) // Day > 31
        assertFalse(BleProtocol.isValidTimePacket("T|2026|11|09|24|30|00")) // Hour > 23
        assertFalse(BleProtocol.isValidTimePacket("T|2026|11|09|14|60|00")) // Min > 59
        assertFalse(BleProtocol.isValidTimePacket("T|2026|11|09|14|30|60")) // Sec > 59
        assertFalse(BleProtocol.isValidTimePacket("N|0|App|Title|Msg"))     // Not a T packet
    }

    @Test
    fun testNormalNotificationPacket() {
        val packet = BleProtocol.buildNormalNotificationPacket(
            appName = "WhatsApp",
            title = "Alice",
            message = "Hello from Android!"
        )
        assertEquals("N|0|WhatsApp|Alice|Hello from Android!", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testIncomingCallPacket() {
        val packet = BleProtocol.buildIncomingCallPacket("Sarah Connor (+1 555-0199)")
        assertEquals("N|1|Phone|Incoming call|Sarah Connor (+1 555-0199)", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testMissedCallPacket() {
        val packet = BleProtocol.buildMissedCallPacket("John Doe (+1 555-0142)")
        assertEquals("N|2|Phone|Missed call|John Doe (+1 555-0142)", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testPipeDelimiterSanitization() {
        val unsafeApp = "My|App|Name"
        val unsafeTitle = "Alert|Urgent|Notice"
        val unsafeMessage = "Line 1|Line 2|Line 3"

        val packet = BleProtocol.buildNormalNotificationPacket(
            appName = unsafeApp,
            title = unsafeTitle,
            message = unsafeMessage
        )

        // All internal pipes must be replaced with '/' to protect firmware parsing
        assertEquals("N|0|My/App/Name|Alert/Urgent/Notice|Line 1/Line 2/Line 3", packet)

        // The only pipes present must be the exact 4 delimiters
        val pipeCount = packet.count { it == '|' }
        assertEquals(4, pipeCount)
    }

    @Test
    fun testNewlineAndCarriageReturnSanitization() {
        val multilineMessage = "First line\nSecond line\rThird line"
        val packet = BleProtocol.buildNormalNotificationPacket(
            appName = "Slack",
            title = "Channel",
            message = multilineMessage
        )

        assertFalse(packet.contains('\n'))
        assertFalse(packet.contains('\r'))
        assertEquals("N|0|Slack|Channel|First line Second line Third line", packet)
    }

    @Test
    fun testFieldCapacityTruncation() {
        val hugeApp = "A".repeat(100)
        val hugeTitle = "T".repeat(150)
        val hugeMessage = "M".repeat(300)

        val packet = BleProtocol.buildNormalNotificationPacket(
            appName = hugeApp,
            title = hugeTitle,
            message = hugeMessage
        )

        val parts = packet.split('|')
        assertEquals(5, parts.size)

        // app must be truncated to max 48
        assertEquals(BleProtocol.MAX_APP_LENGTH, parts[2].length)
        // title must be truncated to max 72
        assertEquals(BleProtocol.MAX_TITLE_LENGTH, parts[3].length)
        // message must be truncated to max 180
        assertEquals(BleProtocol.MAX_MESSAGE_LENGTH, parts[4].length)
    }

    @Test
    fun testMtuAwarePacketFitting() {
        val longMsg = "This is a very long notification that exceeds standard default 23-byte MTU payload size."
        val packet = BleProtocol.buildNormalNotificationPacket("App", "Title", longMsg)

        // Default MTU 23 -> Max payload = 20 bytes
        val smallMtuPacket = BleProtocol.fitPacketToMtu(packet, mtu = 23)
        val smallMtuBytes = smallMtuPacket.toByteArray(StandardCharsets.UTF_8).size
        assertTrue("Packet bytes ($smallMtuBytes) must be <= 20", smallMtuBytes <= 20)
        assertTrue(BleProtocol.isValidNotificationPacket(smallMtuPacket))

        // MTU 64 -> Max payload = 61 bytes
        val midMtuPacket = BleProtocol.fitPacketToMtu(packet, mtu = 64)
        val midMtuBytes = midMtuPacket.toByteArray(StandardCharsets.UTF_8).size
        assertTrue("Packet bytes ($midMtuBytes) must be <= 61", midMtuBytes <= 61)
        assertTrue(BleProtocol.isValidNotificationPacket(midMtuPacket))

        // Large MTU 512 -> Preserves full packet
        val largeMtuPacket = BleProtocol.fitPacketToMtu(packet, mtu = 512)
        assertEquals(packet, largeMtuPacket)
    }

    @Test
    fun testUtf8MultiByteTruncation() {
        val emojiText = "Hello 😊🚀🌍 Watch!"
        val truncated = BleProtocol.truncateUtf8ToBytes(emojiText, maxBytes = 10)
        val truncatedBytes = truncated.toByteArray(StandardCharsets.UTF_8).size
        assertTrue(truncatedBytes <= 10)
        // Ensure valid string without broken surrogate pairs
        assertTrue(truncated.startsWith("Hello"))
    }
}
