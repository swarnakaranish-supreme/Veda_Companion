package com.anish.veda.bluetooth

data class BleDeviceModel(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isVedaWatch: Boolean,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = name ?: "Unknown Device"
}
