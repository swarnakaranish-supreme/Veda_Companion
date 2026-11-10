package com.anish.veda.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleReconnectionBackoffTest {

    @Test
    fun testReconnectionBackoffIntervals() {
        val steps = BleConstants.RECONNECT_BACKOFF_STEPS_MS
        assertEquals(BleConstants.MAX_RECONNECT_ATTEMPTS, steps.size)

        // Ensure backoff steps are monotonically increasing to avoid tight retry loops
        for (i in 0 until steps.size - 1) {
            assertTrue(
                "Backoff step ${i + 1} (${steps[i + 1]}ms) must be >= step $i (${steps[i]}ms)",
                steps[i + 1] >= steps[i]
            )
        }

        // Rapid phase
        assertEquals(2000L, steps[0])
        assertEquals(4000L, steps[1])
        assertEquals(8000L, steps[2])

        // Progressive phase
        assertEquals(15000L, steps[3])
        assertEquals(30000L, steps[4])
        assertEquals(60000L, steps[5])
    }

    @Test
    fun testStandbyWatchdogParameters() {
        // Watchdog periodic cycle must be >= 1 minute to preserve battery
        assertTrue(BleConstants.STANDBY_SCAN_INTERVAL_MS >= 60000L)
        // Scan duration must be short (e.g. <= 10 seconds)
        assertTrue(BleConstants.STANDBY_SCAN_DURATION_MS <= 10000L)
    }

    @Test
    fun testBleConnectionStateProperties() {
        val connected = BleConnectionState.Connected("AA:BB:CC:DD:EE:FF", "ANISH-WATCH", 512, -65)
        assertTrue(connected.isConnected)
        assertFalse(connected.isBusy)

        val disconnected = BleConnectionState.Disconnected("Manual disconnect")
        assertFalse(disconnected.isConnected)
        assertFalse(disconnected.isBusy)

        val reconnecting = BleConnectionState.Reconnecting("AA:BB:CC:DD:EE:FF", "ANISH-WATCH", 2, 6)
        assertFalse(reconnecting.isConnected)
        assertTrue(reconnecting.isBusy)

        val scanning = BleConnectionState.Scanning
        assertFalse(scanning.isConnected)
        assertTrue(scanning.isBusy)
    }
}
