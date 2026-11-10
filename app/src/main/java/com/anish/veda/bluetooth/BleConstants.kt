package com.anish.veda.bluetooth

import java.util.UUID

object BleConstants {
    // Exact ESP32-C3 firmware parameters from ble_phone.cpp & README_ARDUINO_IDE.txt
    const val SERVICE_UUID_STRING = "6c9d0001-2c9f-4e4e-9a9b-1b0c3f25a001"
    const val WRITE_CHARACTERISTIC_UUID_STRING = "6c9d0002-2c9f-4e4e-9a9b-1b0c3f25a001"
    const val DEFAULT_DEVICE_NAME = "ANISH-WATCH"

    val SERVICE_UUID: UUID = UUID.fromString(SERVICE_UUID_STRING)
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString(WRITE_CHARACTERISTIC_UUID_STRING)

    // BLE MTU configurations
    const val DEFAULT_MTU = 23
    const val TARGET_MTU = 512

    // Scan & Connection timeouts
    const val DEFAULT_SCAN_DURATION_MS = 15000L
    const val RECONNECT_DELAY_MS = 3000L
    const val MAX_RECONNECT_ATTEMPTS = 6
    const val RSSI_POLL_INTERVAL_MS = 5000L

    // Progressive controlled reconnection backoff (ms)
    // Rapid phase (attempts 1-3) -> Progressive phase (attempts 4-6)
    val RECONNECT_BACKOFF_STEPS_MS = longArrayOf(2000L, 4000L, 8000L, 15000L, 30000L, 60000L)

    // Standby Watchdog parameters (low-duty cycle for powered-off or out-of-range watch)
    const val STANDBY_SCAN_INTERVAL_MS = 120000L // 2 minutes periodic wake-up
    const val STANDBY_SCAN_DURATION_MS = 6000L   // 6 seconds targeted scan

    // Inter-packet pacing delay to prevent ESP32 BLE buffer congestion
    const val PACKET_PACING_DELAY_MS = 40L
}
