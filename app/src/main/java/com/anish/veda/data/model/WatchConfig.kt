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
    )
)
