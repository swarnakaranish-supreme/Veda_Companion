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
    val accentColorIndex: Int = 0
)
