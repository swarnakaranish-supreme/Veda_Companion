package com.anish.veda.data.model

data class VedaSettings(
    val lastDeviceAddress: String? = null,
    val lastDeviceName: String? = "ANISH-WATCH",
    val autoConnect: Boolean = true,
    val forwardNotifications: Boolean = true,
    val forwardCalls: Boolean = true,
    val forwardMissedCalls: Boolean = true,
    val showCallerName: Boolean = true,
    val resolveCallerNames: Boolean = true,
    val autoSyncTimeOnConnect: Boolean = true,
    val periodicTimeSync: Boolean = true,
    val syncIntervalHours: Int = 2,
    val maxMessageLength: Int = 120,
    val allowedApps: Set<String> = emptySet(), // empty means allow all non-filtered
    val lastSyncTimestamp: Long = 0L,
    val notificationForwardedCount: Int = 0,
    val callsForwardedCount: Int = 0,
    val accentColorIndex: Int = 0,
    // V2.1 Personalization & Customization fields
    val themePreset: String = "DEFAULT", // "DEFAULT", "GRAPHITE", "MIDNIGHT", "LIGHT"
    val fontScaleIndex: Int = 1, // 0: Small, 1: Normal, 2: Large
    val fontFamilyChoice: String = "DEFAULT", // "DEFAULT", "MONOSPACE", "SERIF"
    val interfaceStyle: String = "FUTURISTIC_HUD", // "MINIMAL", "FUTURISTIC_HUD", "TECHNICAL", "CLASSIC", "SOFT"
    val reducedMotion: Boolean = false,
    val startingDialMode: Int = 0, // 0..5 (Neural, Health, Energy, Chronos, AI, Command)
    val dashboardWidgetHealth: Boolean = true,
    val dashboardWidgetAi: Boolean = true,
    val dashboardWidgetQuickActions: Boolean = true,
    val quickAction1: String = "FIND_WATCH",
    val quickAction2: String = "WATCH_STUDIO",
    val quickAction3: String = "DND"
)
