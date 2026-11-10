package com.anish.veda.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets

data class QueueStats(
    val totalEnqueued: Long = 0,
    val totalSent: Long = 0,
    val totalRetried: Long = 0,
    val totalFailed: Long = 0
)

class BleWriteQueue(
    private val logRepository: LogRepository,
    private val gattProvider: () -> BluetoothGatt?,
    private val characteristicProvider: () -> BluetoothGattCharacteristic?,
    private val mtuProvider: () -> Int
) {
    private val queueScope = CoroutineScope(Dispatchers.IO + Job())
    private val queueMutex = Mutex()

    private val writeChannel = Channel<WriteTask>(capacity = Channel.BUFFERED)
    private var pendingWriteDeferred: CompletableDeferred<Int>? = null

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _stats = MutableStateFlow(QueueStats())
    val stats: StateFlow<QueueStats> = _stats.asStateFlow()

    var lastSuccessfulPayload: String? = null
        private set
    var lastSuccessfulTimestamp: Long = 0L
        private set

    data class WriteTask(
        val payload: String,
        val resultDeferred: CompletableDeferred<Boolean>? = null,
        var retryCount: Int = 0
    )

    init {
        // Start serial queue processor
        queueScope.launch {
            while (isActive) {
                val task = writeChannel.receive()
                _queueSize.value = (_queueSize.value - 1).coerceAtLeast(0)
                _isProcessing.value = true

                val success = processTaskWithRetries(task)
                task.resultDeferred?.complete(success)

                // Enforce mandatory inter-packet pacing delay for ESP32
                delay(BleConstants.PACKET_PACING_DELAY_MS)
                _isProcessing.value = false
            }
        }
    }

    /**
     * Enqueue a text payload for asynchronous serialized delivery to the watch.
     * Preserves strict FIFO order and does not silently drop messages.
     */
    fun enqueue(payload: String) {
        _stats.value = _stats.value.copy(totalEnqueued = _stats.value.totalEnqueued + 1)
        _queueSize.value = _queueSize.value + 1
        writeChannel.trySend(WriteTask(payload))
    }

    /**
     * Send payload and suspend until the write (and any required retries) finishes.
     */
    suspend fun sendDirect(payload: String): Boolean {
        _stats.value = _stats.value.copy(totalEnqueued = _stats.value.totalEnqueued + 1)
        val deferred = CompletableDeferred<Boolean>()
        _queueSize.value = _queueSize.value + 1
        writeChannel.send(WriteTask(payload, deferred))
        return deferred.await()
    }

    /**
     * Callback invoked by BluetoothGattCallback.onCharacteristicWrite.
     * Unblocks the serialized write pipeline.
     */
    fun onCharacteristicWriteCompleted(status: Int) {
        val currentDeferred = pendingWriteDeferred
        if (currentDeferred != null && currentDeferred.isActive) {
            currentDeferred.complete(status)
        }
    }

    private suspend fun processTaskWithRetries(task: WriteTask): Boolean {
        val maxRetries = 2
        var attempt = 0
        var succeeded = false

        while (attempt <= maxRetries && !succeeded) {
            attempt++
            succeeded = executeSingleGattWrite(task.payload)
            if (!succeeded) {
                if (attempt <= maxRetries) {
                    _stats.value = _stats.value.copy(totalRetried = _stats.value.totalRetried + 1)
                    logRepository.log(
                        "BLE_QUEUE",
                        "Write failed. Retrying (attempt $attempt of $maxRetries)...",
                        LogDirection.SYSTEM
                    )
                    delay(80) // Backoff delay before retry
                } else {
                    _stats.value = _stats.value.copy(totalFailed = _stats.value.totalFailed + 1)
                    logRepository.log(
                        "BLE_QUEUE",
                        "Write permanently failed after $maxRetries retries: ${task.payload}",
                        LogDirection.ERROR
                    )
                }
            } else {
                _stats.value = _stats.value.copy(totalSent = _stats.value.totalSent + 1)
            }
        }
        return succeeded
    }

    private suspend fun executeSingleGattWrite(rawPayload: String): Boolean = queueMutex.withLock {
        val gatt = gattProvider()
        val characteristic = characteristicProvider()
        val currentMtu = mtuProvider()

        if (gatt == null || characteristic == null) {
            logRepository.log("BLE_QUEUE", "Cannot write: GATT or Characteristic is null", LogDirection.ERROR)
            return false
        }

        // 1. Truncate payload to fit current negotiated MTU
        val safePayload = BleProtocol.fitPacketToMtu(rawPayload, currentMtu)
        val bytes = safePayload.toByteArray(StandardCharsets.UTF_8)

        // 2. Determine write type: check characteristic properties
        val props = characteristic.properties
        val supportsNoResponse = (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        val supportsWriteWithResponse = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0

        // Prefer WRITE_WITH_RESPONSE if supported to ensure reliable delivery,
        // otherwise WRITE_NO_RESPONSE.
        val writeType = if (supportsWriteWithResponse) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        val deferred = if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
            CompletableDeferred<Int>().also { pendingWriteDeferred = it }
        } else {
            null
        }

        logRepository.log(
            "BLE_TX",
            "Writing (${bytes.size} bytes, MTU: $currentMtu, WriteType: ${if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) "WITH_RESP" else "NO_RESP"}): $safePayload",
            LogDirection.OUTBOUND,
            safePayload
        )

        val writeInitiated = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val res = gatt.writeCharacteristic(characteristic, bytes, writeType)
                res == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = bytes
                @Suppress("DEPRECATION")
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        } catch (e: Exception) {
            logRepository.log("BLE_QUEUE", "Exception initiating GATT write: ${e.message}", LogDirection.ERROR)
            false
        }

        if (!writeInitiated) {
            logRepository.log("BLE_QUEUE", "GATT writeCharacteristic returned false", LogDirection.ERROR)
            pendingWriteDeferred = null
            return false
        }

        // 3. For WRITE_WITH_RESPONSE: await onCharacteristicWrite completion callback
        if (deferred != null) {
            val status = withTimeoutOrNull(2500L) {
                deferred.await()
            }
            pendingWriteDeferred = null

            if (status == null) {
                logRepository.log("BLE_QUEUE", "Write operation timed out waiting for GATT acknowledgement", LogDirection.ERROR)
                return false
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logRepository.log("BLE_QUEUE", "Write operation failed with GATT status $status", LogDirection.ERROR)
                return false
            }
        }

        lastSuccessfulPayload = safePayload
        lastSuccessfulTimestamp = System.currentTimeMillis()
        return true
    }

    fun clear() {
        pendingWriteDeferred?.cancel()
        pendingWriteDeferred = null
        _queueSize.value = 0
    }
}
