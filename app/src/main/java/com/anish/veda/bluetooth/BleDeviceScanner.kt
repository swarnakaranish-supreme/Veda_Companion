package com.anish.veda.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class BleDeviceScanner(
    private val context: Context,
    private val bluetoothAdapterProvider: () -> BluetoothAdapter?,
    private val logRepository: LogRepository
) {
    private val handler = Handler(Looper.getMainLooper())
    private var scanTimeoutRunnable: Runnable? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceModel>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceModel>> = _scannedDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                processScanResult(it.device, it.rssi, it.scanRecord?.serviceUuids)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach {
                processScanResult(it.device, it.rssi, it.scanRecord?.serviceUuids)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val errorText = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal Bluetooth stack error"
                else -> "Scan error ($errorCode)"
            }
            logRepository.log("BLE_SCAN", "Scan failed: $errorText", LogDirection.ERROR)
            _isScanning.value = false
        }
    }

    private fun processScanResult(
        device: BluetoothDevice,
        rssi: Int,
        serviceUuids: List<ParcelUuid>?
    ) {
        val name = device.name
        val address = device.address

        // Check if device advertises the exact VEDA Service UUID
        val hasVedaService = serviceUuids?.any {
            it.uuid == BleConstants.SERVICE_UUID
        } == true

        // Check if device advertises the exact firmware name "ANISH-WATCH" or variant
        val matchesName = name?.contains("ANISH", ignoreCase = true) == true ||
                name?.contains("VEDA", ignoreCase = true) == true ||
                name?.contains("WATCH", ignoreCase = true) == true

        val isVedaWatch = hasVedaService || matchesName

        val currentList = _scannedDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.address == address }

        val model = BleDeviceModel(
            name = name ?: if (isVedaWatch) BleConstants.DEFAULT_DEVICE_NAME else null,
            address = address,
            rssi = rssi,
            isVedaWatch = isVedaWatch,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        if (index >= 0) {
            currentList[index] = model
        } else {
            currentList.add(model)
        }

        // Prioritize VEDA smartwatch, then sort by strongest RSSI
        currentList.sortWith(
            compareByDescending<BleDeviceModel> { it.isVedaWatch }
                .thenByDescending { it.rssi }
        )

        _scannedDevices.value = currentList
    }

    fun startScan(durationMs: Long = BleConstants.DEFAULT_SCAN_DURATION_MS): Boolean {
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            logRepository.log("BLE_SCAN", "Cannot scan: Missing Bluetooth permissions", LogDirection.ERROR)
            return false
        }

        val adapter = bluetoothAdapterProvider()
        if (adapter == null || !adapter.isEnabled) {
            logRepository.log("BLE_SCAN", "Bluetooth adapter disabled or unavailable", LogDirection.ERROR)
            return false
        }

        val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
        if (scanner == null) {
            logRepository.log("BLE_SCAN", "BluetoothLeScanner is null", LogDirection.ERROR)
            return false
        }

        stopScan()

        val initialList = mutableListOf<BleDeviceModel>()

        // 1. Check devices already paired/bonded in Android system settings
        try {
            adapter.bondedDevices?.forEach { device ->
                val name = device.name
                val address = device.address
                val isVeda = name?.contains("ANISH", ignoreCase = true) == true ||
                        name?.contains("VEDA", ignoreCase = true) == true ||
                        name?.contains("WATCH", ignoreCase = true) == true
                if (isVeda) {
                    initialList.add(
                        BleDeviceModel(
                            name = name ?: BleConstants.DEFAULT_DEVICE_NAME,
                            address = address,
                            rssi = -50,
                            isVedaWatch = true,
                            lastSeenTimestamp = System.currentTimeMillis()
                        )
                    )
                    logRepository.log("BLE_SCAN", "Found bonded VEDA watch: $name [$address]")
                }
            }
        } catch (e: Exception) {
            logRepository.log("BLE_SCAN", "Error reading bonded devices: ${e.message}")
        }

        // 2. Check devices currently connected via GATT to the phone OS
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.getConnectedDevices(BluetoothProfile.GATT)?.forEach { device ->
                val name = device.name
                val address = device.address
                val isVeda = name?.contains("ANISH", ignoreCase = true) == true ||
                        name?.contains("VEDA", ignoreCase = true) == true ||
                        name?.contains("WATCH", ignoreCase = true) == true
                if (isVeda && initialList.none { it.address == address }) {
                    initialList.add(
                        BleDeviceModel(
                            name = name ?: BleConstants.DEFAULT_DEVICE_NAME,
                            address = address,
                            rssi = -40,
                            isVedaWatch = true,
                            lastSeenTimestamp = System.currentTimeMillis()
                        )
                    )
                    logRepository.log("BLE_SCAN", "Found system-connected GATT VEDA watch: $name [$address]")
                }
            }
        } catch (e: Exception) {
            logRepository.log("BLE_SCAN", "Error reading connected GATT devices: ${e.message}")
        }

        _scannedDevices.value = initialList
        _isScanning.value = true

        logRepository.log("BLE_SCAN", "Starting BLE discovery (filters: UUID ${BleConstants.SERVICE_UUID_STRING}, name ${BleConstants.DEFAULT_DEVICE_NAME})...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        // Create filters for targeted discovery
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .build(),
            ScanFilter.Builder()
                .setDeviceName(BleConstants.DEFAULT_DEVICE_NAME)
                .build()
        )

        return try {
            // Some ESP32 advertising packets don't fit 128-bit UUIDs in the primary PDU,
            // so we pass emptyList() to catch all and filter in processScanResult,
            // while prioritizing devices matching UUID/Name.
            scanner.startScan(emptyList(), settings, scanCallback)

            scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
            scanTimeoutRunnable = Runnable {
                logRepository.log("BLE_SCAN", "Scan timed out after ${durationMs}ms")
                stopScan()
            }
            handler.postDelayed(scanTimeoutRunnable!!, durationMs)
            true
        } catch (e: Exception) {
            logRepository.log("BLE_SCAN", "Failed to start BLE scan: ${e.message}", LogDirection.ERROR)
            _isScanning.value = false
            false
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        scanTimeoutRunnable = null

        val scanner = bluetoothAdapterProvider()?.bluetoothLeScanner
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            logRepository.log("BLE_SCAN", "Error while stopping scan: ${e.message}", LogDirection.ERROR)
        }
        _isScanning.value = false
        logRepository.log("BLE_SCAN", "Scan stopped. Total devices found: ${_scannedDevices.value.size}")
    }

    fun clearScannedDevices() {
        _scannedDevices.value = emptyList()
    }
}
