package com.anish.veda.data.model

import java.util.UUID

enum class SyncStatus(val label: String) {
    SYNCED("SYNCHRONIZED"),
    SYNCING("SYNCHRONIZING..."),
    FAILED("SYNC FAILED"),
    NEVER_SYNCED("NOT SYNCHRONIZED")
}

data class SensorHealth(
    val name: String,
    val bus: String,
    val description: String,
    val isHealthy: Boolean = true,
    val statusText: String = "OPERATIONAL"
)

data class BatteryInfo(
    val isAvailable: Boolean = false,
    val percentage: Int? = null,
    val voltageMv: Int? = null,
    val statusMessage: String = "HARDWARE UNAVAILABLE (No dedicated MAX17048 fuel gauge or ADC voltage divider on VEDA V2)"
)

data class DeviceMetadata(
    val watchName: String = "ANISH-WATCH",
    val macAddress: String = "--",
    val firmwareVersion: String = "v2.0.0 (VEDA-C3)",
    val protocolVersion: String = "v1.2.0",
    val hardwareArchitecture: String = "ESP32-C3 SuperMini (RISC-V 32-bit @ 160MHz)",
    val displaySpecs: String = "1.69\" ST7789 TFT 240x280 (8-pin SPI)",
    val sensors: List<SensorHealth> = defaultSensors(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val lastSyncTimestamp: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.NEVER_SYNCED,
    val negotiatedMtu: Int = 23,
    val rssi: Int? = null,
    val pingLatencyMs: Long? = null
) {
    companion object {
        fun defaultSensors(): List<SensorHealth> = listOf(
            SensorHealth(
                name = "MAX30102 PPG Optical Sensor",
                bus = "I2C 0x57 (SDA:0, SCL:1)",
                description = "Red + IR Photoplethysmography (Heart Rate & SpO2)",
                isHealthy = true,
                statusText = "OPERATIONAL"
            ),
            SensorHealth(
                name = "TI DRV2605L Haptic Driver",
                bus = "I2C 0x5A (SDA:0, SCL:1)",
                description = "LRA/ERM Advanced Haptic Waveform Synthesizer",
                isHealthy = true,
                statusText = "OPERATIONAL"
            ),
            SensorHealth(
                name = "ST7789 IPS 1.69\" Display",
                bus = "8-pin SPI (240x280 High Contrast)",
                description = "Dedicated High-Frame-Rate Screen Engine",
                isHealthy = true,
                statusText = "OPERATIONAL"
            ),
            SensorHealth(
                name = "Rotary Encoder + Push Button",
                bus = "GPIO 8 (A), 10 (B), 9 (SW)",
                description = "Tactile Navigation Dial & Emergency Panic Trigger",
                isHealthy = true,
                statusText = "OPERATIONAL"
            )
        )
    }
}

enum class ConnectionHistoryEventType(val label: String) {
    CONNECTED("LINK ESTABLISHED"),
    DISCONNECTED("MANUAL DISCONNECT"),
    CONNECTION_LOST("UNEXPECTED LINK LOSS"),
    RECONNECTING("AUTO-RECONNECT CYCLE"),
    FAILED("HANDSHAKE FAILED")
}

data class ConnectionHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: ConnectionHistoryEventType,
    val deviceName: String,
    val deviceAddress: String,
    val durationSeconds: Long? = null,
    val notes: String? = null
)

data class TroubleshootingGuide(
    val title: String,
    val category: String,
    val symptoms: String,
    val steps: List<String>
)
