package com.anish.veda.data.model

import java.util.UUID

enum class SosStatus(val label: String) {
    IDLE("ARMED & STANDBY"),
    COUNTDOWN("SOS COUNTDOWN ACTIVE"),
    FETCHING_LOCATION("ACQUIRING GPS FIX"),
    DISPATCHING_SMS("DISPATCHING EMERGENCY SMS"),
    SENT_CONFIRMED("DISPATCH CONFIRMED"),
    FAILED("DISPATCH FAILED"),
    CANCELLED("SOS CANCELLED")
}

data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val relationship: String = "Emergency Contact",
    val isPrimary: Boolean = false
)

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    val mapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"

    val displayCoords: String
        get() = String.format(java.util.Locale.US, "%.5f, %.5f (±%.1fm)", latitude, longitude, accuracyMeters)
}

data class GuardianConfig(
    val gestureMode: Int = 0, // 0: Hold 3s, 1: Triple-Click
    val countdownSeconds: Int = 10, // 5, 10, 15, 30
    val messageTemplate: String = "EMERGENCY: I need immediate assistance! My current GPS location: {LOCATION}",
    val disconnectionAlertEnabled: Boolean = true,
    val vibratePhoneOnSos: Boolean = true
) {
    companion object {
        const val GESTURE_HOLD_3S = 0
        const val GESTURE_TRIPLE_CLICK = 1
    }
}
