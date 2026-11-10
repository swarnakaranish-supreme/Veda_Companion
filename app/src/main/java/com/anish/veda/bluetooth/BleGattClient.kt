package com.anish.veda.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BleGattClient(
    private val context: Context,
    private val logRepository: LogRepository,
    private val onConnectionStateChanged: (BleConnectionState) -> Unit,
    private val onDisconnectedUnexpectedly: (deviceAddress: String, deviceName: String?) -> Unit
) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var targetAddress: String? = null
    private var targetName: String? = null
    private var currentMtu = BleConstants.DEFAULT_MTU
    private var isManualDisconnect = false

    // Integrated serialized GATT write queue with write-with-response tracking and retry logic
    val writeQueue = BleWriteQueue(
        logRepository = logRepository,
        gattProvider = { bluetoothGatt },
        characteristicProvider = { writeCharacteristic },
        mtuProvider = { currentMtu }
    )

    val queueSize: StateFlow<Int> = writeQueue.queueSize
    val isProcessingQueue: StateFlow<Boolean> = writeQueue.isProcessing
    val queueStats: StateFlow<QueueStats> = writeQueue.stats

    val isServiceDiscovered: Boolean get() = writeCharacteristic != null
    val isCharacteristicDiscovered: Boolean get() = writeCharacteristic != null
    val negotiatedMtu: Int get() = currentMtu
    val lastSuccessfulPayload: String? get() = writeQueue.lastSuccessfulPayload
    val lastSuccessfulTimestamp: Long get() = writeQueue.lastSuccessfulTimestamp

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val address = targetAddress ?: gatt?.device?.address ?: "Unknown"
            val name = targetName ?: gatt?.device?.name ?: BleConstants.DEFAULT_DEVICE_NAME

            val statusMessage = when (status) {
                BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
                133 -> "GATT_ERROR_133 (Link loss / Timeout)"
                8 -> "GATT_CONN_TIMEOUT"
                19 -> "GATT_CONN_TERMINATE_PEER_USER"
                22 -> "GATT_CONN_TERMINATE_LOCAL_HOST"
                else -> "Status code $status"
            }

            val stateMessage = when (newState) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "State $newState"
            }

            logRepository.log("BLE_GATT", "Connection state change: $stateMessage ($statusMessage) on $name [$address]")

            if (status != BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                val wasManual = isManualDisconnect
                cleanupResources()
                onConnectionStateChanged(BleConnectionState.Disconnected(statusMessage))
                if (!wasManual) {
                    onDisconnectedUnexpectedly(address, name)
                }
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    bluetoothGatt = gatt
                    isManualDisconnect = false
                    logRepository.log("BLE_GATT", "GATT link established. Requesting MTU negotiation (${BleConstants.TARGET_MTU})...")
                    onConnectionStateChanged(BleConnectionState.ConnectedGatt(address, name))

                    // Step 1: Request maximum MTU for high-throughput packet streaming
                    onConnectionStateChanged(BleConnectionState.NegotiatingMtu(address, name))
                    val mtuRequested = gatt?.requestMtu(BleConstants.TARGET_MTU) ?: false
                    if (!mtuRequested) {
                        logRepository.log("BLE_GATT", "requestMtu returned false, proceeding immediately to service discovery...")
                        onConnectionStateChanged(BleConnectionState.DiscoveringServices(address, name))
                        gatt?.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasManual = isManualDisconnect
                    cleanupResources()
                    onConnectionStateChanged(BleConnectionState.Disconnected("Disconnected from watch"))
                    if (!wasManual) {
                        onDisconnectedUnexpectedly(address, name)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            currentMtu = mtu
            val address = targetAddress ?: ""
            val name = targetName ?: BleConstants.DEFAULT_DEVICE_NAME

            logRepository.log("BLE_GATT", "MTU negotiated: $mtu bytes (status: $status). Initiating service discovery...")
            onConnectionStateChanged(BleConnectionState.DiscoveringServices(address, name))
            gatt?.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val address = targetAddress ?: ""
            val name = targetName ?: BleConstants.DEFAULT_DEVICE_NAME

            if (status != BluetoothGatt.GATT_SUCCESS) {
                logRepository.log("BLE_GATT", "Service discovery failed with status $status", LogDirection.ERROR)
                cleanupResources()
                onConnectionStateChanged(BleConnectionState.Error("Service discovery failed (status $status)"))
                return
            }

            logRepository.log("BLE_GATT", "Services discovered. Verifying VEDA Service UUID (${BleConstants.SERVICE_UUID_STRING})...")

            val service = gatt?.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                logRepository.log(
                    "BLE_GATT",
                    "Service UUID ${BleConstants.SERVICE_UUID} not found on peripheral!",
                    LogDirection.ERROR
                )
                cleanupResources()
                onConnectionStateChanged(BleConnectionState.Error("VEDA service missing on device"))
                return
            }

            logRepository.log("BLE_GATT", "Service verified! Locating write characteristic (${BleConstants.WRITE_CHARACTERISTIC_UUID_STRING})...")

            val characteristic = service.getCharacteristic(BleConstants.WRITE_CHARACTERISTIC_UUID)
            if (characteristic == null) {
                logRepository.log(
                    "BLE_GATT",
                    "Characteristic ${BleConstants.WRITE_CHARACTERISTIC_UUID} not found!",
                    LogDirection.ERROR
                )
                cleanupResources()
                onConnectionStateChanged(BleConnectionState.Error("VEDA characteristic missing"))
                return
            }

            // Verify writable characteristic properties: must support WRITE or WRITE_NO_RESPONSE
            val props = characteristic.properties
            val hasWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
            val hasWriteNoResp = (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

            logRepository.log("BLE_GATT", "Characteristic found! Properties: WRITE=$hasWrite, WRITE_NR=$hasWriteNoResp")

            if (!hasWrite && !hasWriteNoResp) {
                logRepository.log("BLE_GATT", "Characteristic exists but lacks write properties!", LogDirection.ERROR)
                cleanupResources()
                onConnectionStateChanged(BleConnectionState.Error("Characteristic does not support write"))
                return
            }

            writeCharacteristic = characteristic

            // Only mark connected when GATT, MTU, Service and Writable Characteristic are all verified!
            logRepository.log("BLE_GATT", "All GATT stages verified! VEDA Smartwatch is ONLINE.")
            onConnectionStateChanged(
                BleConnectionState.Connected(
                    deviceAddress = address,
                    deviceName = name,
                    mtu = currentMtu
                )
            )

            // Read initial RSSI
            gatt.readRemoteRssi()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val ok = status == BluetoothGatt.GATT_SUCCESS
            if (!ok) {
                logRepository.log("BLE_GATT", "Characteristic write failure: status=$status", LogDirection.ERROR)
            }
            // Signal serialized write queue that the write operation finished
            writeQueue.onCharacteristicWriteCompleted(status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val address = targetAddress ?: return
                val name = targetName ?: BleConstants.DEFAULT_DEVICE_NAME
                onConnectionStateChanged(
                    BleConnectionState.Connected(
                        deviceAddress = address,
                        deviceName = name,
                        mtu = currentMtu,
                        rssi = rssi
                    )
                )
            }
        }
    }

    fun connect(device: BluetoothDevice): Boolean {
        cleanupResources()
        isManualDisconnect = false
        targetAddress = device.address
        targetName = device.name ?: BleConstants.DEFAULT_DEVICE_NAME

        logRepository.log("BLE_GATT", "Initiating LE GATT connection to ${targetName} [${targetAddress}]...")
        onConnectionStateChanged(BleConnectionState.Connecting(device.address, targetName))

        return try {
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            bluetoothGatt != null
        } catch (e: Exception) {
            logRepository.log("BLE_GATT", "connectGatt failed: ${e.message}", LogDirection.ERROR)
            onConnectionStateChanged(BleConnectionState.Error("Failed to initiate connection: ${e.message}"))
            false
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        val gatt = bluetoothGatt
        val addr = targetAddress ?: ""
        if (gatt != null) {
            logRepository.log("BLE_GATT", "Manually disconnecting from $addr...")
            onConnectionStateChanged(BleConnectionState.Disconnecting(addr))
            try {
                gatt.disconnect()
            } catch (e: Exception) {
                logRepository.log("BLE_GATT", "Error during disconnect: ${e.message}", LogDirection.ERROR)
            }
            cleanupResources()
            onConnectionStateChanged(BleConnectionState.Disconnected("Manual disconnect"))
        } else {
            onConnectionStateChanged(BleConnectionState.Disconnected("Already disconnected"))
        }
    }

    fun cleanupResources() {
        writeQueue.clear()
        writeCharacteristic = null
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            // Ignore close exceptions
        }
        bluetoothGatt = null
    }

    fun enqueuePayload(payload: String) {
        writeQueue.enqueue(payload)
    }

    suspend fun sendPayloadDirect(payload: String): Boolean {
        return writeQueue.sendDirect(payload)
    }

    fun readRssi() {
        bluetoothGatt?.readRemoteRssi()
    }
}
