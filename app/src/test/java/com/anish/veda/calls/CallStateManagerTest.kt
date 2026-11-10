package com.anish.veda.calls

import com.anish.veda.bluetooth.BleProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStateManagerTest {

    @Test
    fun testIncomingCallPacketFormatting() {
        val packet = BleProtocol.buildIncomingCallPacket("Marcus Vance")
        assertEquals("N|1|Phone|Incoming call|Marcus Vance", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testIncomingCallUnknownCallerFormatting() {
        val packet = BleProtocol.buildIncomingCallPacket("UNKNOWN")
        assertEquals("N|1|Phone|Incoming call|UNKNOWN", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testMissedCallPacketFormatting() {
        val packet = BleProtocol.buildMissedCallPacket("Elena Rostova")
        assertEquals("N|2|Phone|Missed call|Elena Rostova", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testMissedCallUnknownCallerFormatting() {
        val packet = BleProtocol.buildMissedCallPacket("UNKNOWN")
        assertEquals("N|2|Phone|Missed call|UNKNOWN", packet)
        assertTrue(BleProtocol.isValidNotificationPacket(packet))
    }

    @Test
    fun testCallPacketSanitizesPipesInCallerName() {
        val unsafeCaller = "John|Dr. Smith|Agent"
        val packet = BleProtocol.buildIncomingCallPacket(unsafeCaller)
        assertEquals("N|1|Phone|Incoming call|John/Dr. Smith/Agent", packet)
        assertEquals(4, packet.count { it == '|' })
    }

    @Test
    fun testDialerPackageKeywordsForOppoAndAndroid15() {
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.coloros.phoneno"))
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.oppo.telephony"))
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.google.android.dialer"))
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.android.server.telecom"))
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.android.incallui"))
        assertTrue(CallNotificationDetector.isDialerOrTelecomPackage("com.oplus.dia"))

        // Unrelated apps should not match
        assertFalse(CallNotificationDetector.isDialerOrTelecomPackage("com.whatsapp"))
        assertFalse(CallNotificationDetector.isDialerOrTelecomPackage("com.spotify.music"))
    }
}
