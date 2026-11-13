package com.anish.veda.data.model

data class WatchConfig(
    val selectedFaceIndex: Int = 0,
    val brightnessPercent: Int = 75,
    val timeoutSeconds: Int = 15,
    val is24Hour: Boolean = true,
    val dateFormat: String = "DD/MM/YYYY"
)

data class WatchFaceMeta(
    val index: Int,
    val name: String,
    val tag: String,
    val description: String
)

val VedaWatchFaces = listOf(
    WatchFaceMeta(
        index = 0,
        name = "Minimal Digital",
        tag = "ULTRA CLEAN",
        description = "Large high-contrast numerical typography with date and battery gauge."
    ),
    WatchFaceMeta(
        index = 1,
        name = "Futuristic HUD",
        tag = "TACTICAL",
        description = "Aerospace HUD corner telemetry brackets with heart rate and link status."
    ),
    WatchFaceMeta(
        index = 2,
        name = "Circular Technical",
        tag = "PRECISION",
        description = "Radial 60-second index bezel with center digital readout and live metrics."
    ),
    WatchFaceMeta(
        index = 3,
        name = "Analog Inspired",
        tag = "HERITAGE",
        description = "Classic mechanical dial with smooth swept hands and 3 o'clock date window."
    ),
    WatchFaceMeta(
        index = 4,
        name = "Health Dashboard",
        tag = "BIOMETRICS",
        description = "Real-time MAX30102 PPG pulse wave, live SpO2 arc, and activity stats."
    ),
    WatchFaceMeta(
        index = 5,
        name = "Time & Date Minimal",
        tag = "ESSENTIAL",
        description = "Vertical stacked hour-over-minute layout with full weekday spelling."
    ),
    WatchFaceMeta(
        index = 6,
        name = "Data Focus",
        tag = "QUADRANT",
        description = "Four discrete telemetry cells for battery, time, pulse, and connectivity."
    ),
    WatchFaceMeta(
        index = 7,
        name = "VEDA Signature",
        tag = "BRAND FLAGSHIP",
        description = "Signature angular geometry, electric cyan chevrons, and dual-time readouts."
    ),
    WatchFaceMeta(
        index = 8,
        name = "Custom Studio",
        tag = "USER DESIGNED",
        description = "Personalized custom watch face crafted directly in VEDA Companion Studio."
    )
)

data class CustomWatchFaceState(
    val style: Int = 0, // 0: Bold Digital, 1: Minimalist, 2: Cyberpunk, 3: Sport
    val accentColorRgb565: Int = 0x07FF, // COLOR_CYAN (0x07FF)
    val colorName: String = "Neon Cyan",
    val showHeartRate: Boolean = true,
    val showSpO2: Boolean = true,
    val showDate: Boolean = true,
    val showSeconds: Boolean = true,
    val showStatus: Boolean = true,
    val customLabel: String = "VEDA"
) {
    fun calculateWidgetsMask(): Int {
        var mask = 0
        if (showHeartRate) mask = mask or (1 shl 0)
        if (showSpO2) mask = mask or (1 shl 1)
        if (showDate) mask = mask or (1 shl 2)
        if (showSeconds) mask = mask or (1 shl 3)
        if (showStatus) mask = mask or (1 shl 4)
        return mask
    }
}
