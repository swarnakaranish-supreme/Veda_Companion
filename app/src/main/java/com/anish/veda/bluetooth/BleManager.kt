package com.anish.veda.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context,
    private val logRepository: LogRepository
) {
    private val managerScope = CoroutineScope(Dispatchers.Main + Job())

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected())
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    // Scanner component
    private val scanner = BleDeviceScanner(
        context = context,
        bluetoothAdapterProvider = { bluetoothAdapter },
        logRepository = logRepository
    )
    val scannedDevices: StateFlow<List<BleDeviceModel>> = scanner.scannedDevices
    val isScanning: StateFlow<Boolean> = scanner.isScanning

    // GATT Client component
    private val gattClient = BleGattClient(
        context = context,
        logRepository = logRepository,
        onConnectionStateChanged = { newState ->
            _connectionState.value = newState
            if (newState is BleConnectionState.Connected) {
                cancelActiveReconnection()
                cancelStandbyWatchdog()
            }
        },
        onDisconnectedUnexpectedly = { address, name ->
            handleUnexpectedDisconnection(address, name)
        }
    )

    private var reconnectJob: Job? = null
    private var standbyWatchdogJob: Job? = null
    private var rssiPollingJob: Job? = null
    private var lastConnectedAddress: String? = null
    private var lastConnectedName: String? = null
    private var isAutoReconnectEnabled = true

    // Receiver to monitor Bluetooth hardware ON/OFF changes in real-time
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        _isBluetoothEnabled.value = false
                        logRepository.log("BLE_ADAPTER", "Bluetooth adapter turned OFF by user. Pausing reconnect cycles.")
                        cancelActiveReconnection()
                        cancelStandbyWatchdog()
                        stopScan()
                        gattClient.cleanupResources()
                        _connectionState.value = BleConnectionState.Disconnected("Bluetooth disabled")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        _isBluetoothEnabled.value = true
                        logRepository.log("BLE_ADAPTER", "Bluetooth adapter turned ON. Resuming connection if applicable.")
                        val targetAddr = lastConnectedAddress
                        if (isAutoReconnectEnabled && !targetAddr.isNullOrBlank()) {
                            connect(targetAddr, lastConnectedName)
                        }
                    }
                }
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bluetoothStateReceiver, filter)
        } catch (e: Exception) {
            logRepository.log("BLE_INIT", "Error registering bluetooth state receiver: ${e.message}", LogDirection.ERROR)
        }

        // Start RSSI polling loop when connected
        managerScope.launch {
            connectionState.collect { state ->
                if (state is BleConnectionState.Connected) {
                    startRssiPolling()
                } else {
                    stopRssiPolling()
                }
            }
        }
    }

    fun isAdapterAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    fun isAdapterEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun startScan(durationMs: Long = BleConstants.DEFAULT_SCAN_DURATION_MS): Boolean {
        cancelActiveReconnection()
        cancelStandbyWatchdog()
        _connectionState.value = BleConnectionState.Scanning
        val started = scanner.startScan(durationMs)
        if (!started) {
            _connectionState.value = BleConnectionState.Disconnected("Failed to start scan")
        }
        return started
    }

    fun stopScan() {
        scanner.stopScan()
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected()
        }
    }

    fun connect(address: String, name: String? = null, autoReconnect: Boolean = true): Boolean {
        stopScan()
        cancelActiveReconnection()
        cancelStandbyWatchdog()

        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            logRepository.log("BLE_CONNECT", "Bluetooth permissions missing", LogDirection.ERROR)
            _connectionState.value = BleConnectionState.Error("Missing Bluetooth permissions")
            return false
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            logRepository.log("BLE_CONNECT", "Bluetooth is disabled", LogDirection.ERROR)
            _connectionState.value = BleConnectionState.Error("Bluetooth is disabled")
            return false
        }

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            logRepository.log("BLE_CONNECT", "Invalid MAC address: $address", LogDirection.ERROR)
            _connectionState.value = BleConnectionState.Error("Invalid MAC address")
            return false
        }

        if (device == null) {
            _connectionState.value = BleConnectionState.Error("Device not found")
            return false
        }

        lastConnectedAddress = address
        lastConnectedName = name ?: device.name ?: BleConstants.DEFAULT_DEVICE_NAME
        isAutoReconnectEnabled = autoReconnect

        return gattClient.connect(device)
    }

    fun disconnect() {
        cancelActiveReconnection()
        cancelStandbyWatchdog()
        stopScan()
        gattClient.disconnect()
    }

    /**
     * Handles unexpected disconnection using controlled progressive backoff and standby watchdog.
     */
    private fun handleUnexpectedDisconnection(address: String, name: String?) {
        if (!isAutoReconnectEnabled) {
            logRepository.log("BLE_RECONNECT", "Auto-reconnect disabled by configuration; staying disconnected.")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            logRepository.log("BLE_RECONNECT", "Bluetooth adapter is disabled; awaiting adapter state change.")
            _connectionState.value = BleConnectionState.Disconnected("Bluetooth adapter disabled")
            return
        }

        cancelActiveReconnection()
        cancelStandbyWatchdog()

        reconnectJob = managerScope.launch {
            val backoffSteps = BleConstants.RECONNECT_BACKOFF_STEPS_MS
            val maxAttempts = backoffSteps.size

            for (i in 0 until maxAttempts) {
                val attemptNumber = i + 1
                val delayMs = backoffSteps[i]

                logRepository.log(
                    "BLE_RECONNECT",
                    "Scheduling progressive reconnect attempt $attemptNumber of $maxAttempts to $name in ${delayMs}ms..."
                )

                _connectionState.value = BleConnectionState.Reconnecting(
                    deviceAddress = address,
                    deviceName = name,
                    attempt = attemptNumber,
                    maxAttempts = maxAttempts
                )

                delay(delayMs)

                // Verify adapter state before initiating connect
                val currentAdapter = bluetoothAdapter
                if (currentAdapter == null || !currentAdapter.isEnabled) {
                    logRepository.log("BLE_RECONNECT", "Bluetooth turned off during backoff. Pausing.", LogDirection.ERROR)
                    _connectionState.value = BleConnectionState.Disconnected("Bluetooth disabled")
                    return@launch
                }

                val device = try {
                    currentAdapter.getRemoteDevice(address)
                } catch (e: Exception) {
                    null
                }

                if (device != null) {
                    val initiated = gattClient.connect(device)
                    if (initiated) {
                        return@launch
                    }
                }
            }

            // Phase 2: If all progressive attempts fail, watch is likely powered off or out of range.
            // Switch to low-power Standby Watchdog to preserve battery while maintaining auto-recovery!
            logRepository.log(
                "BLE_RECONNECT",
                "Watch appears powered-off or out of range. Entering energy-efficient standby watchdog."
            )
            _connectionState.value = BleConnectionState.Disconnected("Watch offline (Standby watchdog active)")
            startStandbyWatchdog(address, name)
        }
    }

    /**
     * Low-duty standby watchdog: periodically scans for ANISH-WATCH without battery drain.
     */
    private fun startStandbyWatchdog(address: String, name: String?) {
        cancelStandbyWatchdog()
        standbyWatchdogJob = managerScope.launch {
            while (isActive && isAutoReconnectEnabled) {
                delay(BleConstants.STANDBY_SCAN_INTERVAL_MS)

                val adapter = bluetoothAdapter
                if (adapter != null && adapter.isEnabled && !_connectionState.value.isConnected) {
                    logRepository.log("BLE_WATCHDOG", "Standby cycle: checking if watch powered back on...")
                    val device = try {
                        adapter.getRemoteDevice(address)
                    } catch (e: Exception) {
                        null
                    }

                    if (device != null) {
                        gattClient.connect(device)
                    }
                }
            }
        }
    }

    /**
     * Triggered when the phone screen is turned ON or unlocked.
     * If the watch was in standby due to power-off, checks immediately!
     */
    fun checkStandbyReconnect() {
        val targetAddr = lastConnectedAddress
        if (isAutoReconnectEnabled &&
            !targetAddr.isNullOrBlank() &&
            !_connectionState.value.isConnected &&
            _connectionState.value !is BleConnectionState.Connecting &&
            _connectionState.value !is BleConnectionState.Reconnecting
        ) {
            val adapter = bluetoothAdapter
            if (adapter != null && adapter.isEnabled) {
                logRepository.log("BLE_RESUME", "Screen active: triggering immediate watch connection check...")
                connect(targetAddr, lastConnectedName)
            }
        }
    }

    private fun cancelActiveReconnection() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun cancelStandbyWatchdog() {
        standbyWatchdogJob?.cancel()
        standbyWatchdogJob = null
    }

    private fun startRssiPolling() {
        stopRssiPolling()
        rssiPollingJob = managerScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(BleConstants.RSSI_POLL_INTERVAL_MS)
                if (connectionState.value.isConnected) {
                    gattClient.readRssi()
                }
            }
        }
    }

    private fun stopRssiPolling() {
        rssiPollingJob?.cancel()
        rssiPollingJob = null
    }

    fun enqueuePayload(payload: String) {
        gattClient.enqueuePayload(payload)
    }

    suspend fun sendPayloadDirect(payload: String): Boolean {
        return gattClient.sendPayloadDirect(payload)
    }

    fun readRssi() {
        gattClient.readRssi()
    }

    val isServiceDiscovered: Boolean get() = gattClient.isServiceDiscovered
    val isCharacteristicDiscovered: Boolean get() = gattClient.isCharacteristicDiscovered
    val negotiatedMtu: Int get() = gattClient.negotiatedMtu
    val lastSuccessfulPayload: String? get() = gattClient.lastSuccessfulPayload
    val lastSuccessfulTimestamp: Long get() = gattClient.lastSuccessfulTimestamp
}
