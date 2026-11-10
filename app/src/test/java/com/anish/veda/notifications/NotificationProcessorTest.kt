package com.anish.veda.notifications

import com.anish.veda.bluetooth.BleProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationProcessorTest {

    @Test
    fun testNormalNotificationFormatting() {
        val app = "WhatsApp"
        val title = "Sarah"
        val msg = "Hey, are you coming to the workshop?"

        val packet = BleProtocol.buildNormalNotificationPacket(app, title, msg)
        assertEquals("N|0|WhatsApp|Sarah|Hey, are you coming to the workshop?", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testPipeSanitizationInNotifications() {
        val app = "System|Alert"
        val title = "CPU|Temp"
        val msg = "Temp: 75C | Status: OK"

        val packet = BleProtocol.buildNormalNotificationPacket(app, title, msg)
        assertEquals("N|0|System/Alert|CPU/Temp|Temp: 75C / Status: OK", packet)
        assertEquals(4, packet.count { it == '|' })
    }

    @Test
    fun testMaxMessageLengthEnforcement() {
        val longMsg = "A".repeat(300)
        val packet = BleProtocol.buildNormalNotificationPacket(
            appName = "Telegram",
            title = "Channel",
            message = longMsg,
            maxMessageLen = 120
        )

        val parts = packet.split('|')
        assertEquals(5, parts.size)
        assertEquals(120, parts[4].length)
    }

    @Test
    fun testPopularAppResolution() {
        val popularApps = NotificationSettingsRepository.POPULAR_PACKAGES
        assertTrue(popularApps.contains("com.whatsapp"))
        assertTrue(popularApps.contains("com.instagram.android"))
        assertTrue(popularApps.contains("com.facebook.katana"))
        assertTrue(popularApps.contains("org.telegram.messenger"))
        assertTrue(popularApps.contains("com.google.android.gm"))
        assertTrue(popularApps.contains("com.google.android.apps.messaging"))
    }
}
