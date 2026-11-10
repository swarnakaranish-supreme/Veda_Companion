package com.anish.veda.bluetooth

sealed class BleConnectionState {
    data class Disconnected(val reason: String? = null) : BleConnectionState()
    data object Scanning : BleConnectionState()
    data class Connecting(val deviceAddress: String, val deviceName: String?) : BleConnectionState()
    data class ConnectedGatt(val deviceAddress: String, val deviceName: String?) : BleConnectionState()
    data class NegotiatingMtu(val deviceAddress: String, val deviceName: String?) : BleConnectionState()
    data class DiscoveringServices(val deviceAddress: String, val deviceName: String?) : BleConnectionState()
    data class Connected(
        val deviceAddress: String,
        val deviceName: String,
        val mtu: Int = BleConstants.DEFAULT_MTU,
        val rssi: Int = 0
    ) : BleConnectionState()
    data class Disconnecting(val deviceAddress: String) : BleConnectionState()
    data class Reconnecting(
        val deviceAddress: String,
        val deviceName: String?,
        val attempt: Int,
        val maxAttempts: Int
    ) : BleConnectionState()
    data class Error(val message: String, val isFatal: Boolean = false) : BleConnectionState()

    val isConnected: Boolean
        get() = this is Connected

    val isBusy: Boolean
        get() = this is Scanning ||
                this is Connecting ||
                this is ConnectedGatt ||
                this is NegotiatingMtu ||
                this is DiscoveringServices ||
                this is Disconnecting ||
                this is Reconnecting
}
