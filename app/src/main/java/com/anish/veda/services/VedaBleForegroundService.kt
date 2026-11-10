package com.anish.veda.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anish.veda.MainActivity
import com.anish.veda.R
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.calls.CallStateManager
import com.anish.veda.data.model.LogDirection
import com.anish.veda.utils.Constants
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VedaBleForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val app by lazy { VedaApplication.instance }
    private val bleManager by lazy { app.bleManager }
    private val preferencesRepository by lazy { app.preferencesRepository }
    private val logRepository by lazy { app.logRepository }

    private var timeSyncJob: Job? = null

    // Screen State Receiver to trigger fast reconnect when phone is picked up or unlocked
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    bleManager.checkStandbyReconnect()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (!PermissionUtils.canStartConnectedDeviceFgs(this)) {
            logRepository.log(
                "SVC",
                "Bluetooth runtime permissions missing for connectedDevice FGS. Stopping service.",
                LogDirection.ERROR
            )
            stopSelf()
            return
        }

        val promoted = startForegroundWithNotification("Initializing VEDA Background Service...")
        if (!promoted) {
            logRepository.log(
                "SVC",
                "Foreground promotion failed. Stopping service immediately without further initialization.",
                LogDirection.ERROR
            )
            stopSelf()
            return
        }

        // Observe connection state to dynamically update persistent notification & handle auto-sync
        serviceScope.launch {
            bleManager.connectionState.collect { state ->
                val (title, text) = when (state) {
                    is BleConnectionState.Connected -> {
                        val rssiStr = state.rssi?.let { " • $it dBm" } ?: ""
                        Pair(
                            "VEDA Smartwatch • Connected",
                            "${state.deviceName} active (MTU: ${state.mtu}$rssiStr)"
                        )
                    }
                    is BleConnectionState.Connecting -> {
                        Pair(
                            "VEDA Smartwatch • Connecting",
                            "Connecting to ${state.deviceName ?: "ANISH-WATCH"}..."
                        )
                    }
                    is BleConnectionState.ConnectedGatt -> {
                        Pair(
                            "VEDA Smartwatch • Negotiating",
                            "GATT channel open with ${state.deviceName ?: "watch"}"
                        )
                    }
                    is BleConnectionState.NegotiatingMtu -> {
                        Pair(
                            "VEDA Smartwatch • Configuring",
                            "Negotiating high-throughput MTU (512 bytes)..."
                        )
                    }
                    is BleConnectionState.DiscoveringServices -> {
                        Pair(
                            "VEDA Smartwatch • Verifying",
                            "Discovering VEDA GATT services & characteristics..."
                        )
                    }
                    is BleConnectionState.Reconnecting -> {
                        Pair(
                            "VEDA Smartwatch • Reconnecting",
                            "Auto-reconnecting (Attempt ${state.attempt}/${state.maxAttempts})..."
                        )
                    }
                    is BleConnectionState.Scanning -> {
                        Pair(
                            "VEDA Smartwatch • Scanning",
                            "Locating ANISH-WATCH over Bluetooth LE..."
                        )
                    }
                    is BleConnectionState.Disconnecting -> {
                        Pair(
                            "VEDA Smartwatch • Disconnecting",
                            "Closing GATT connection safely..."
                        )
                    }
                    is BleConnectionState.Disconnected -> {
                        val reason = state.reason?.let { " ($it)" } ?: ""
                        Pair(
                            "VEDA Smartwatch • Disconnected",
                            "Watch offline$reason • Tap to connect"
                        )
                    }
                    is BleConnectionState.Error -> {
                        Pair(
                            "VEDA Smartwatch • Alert",
                            "Link notice: ${state.message}"
                        )
                    }
                }

                updateForegroundNotification(title, text, state is BleConnectionState.Connected)

                if (state is BleConnectionState.Connected) {
                    val settings = preferencesRepository.settingsFlow.value
                    if (settings.autoSyncTimeOnConnect) {
                        launch {
                            delay(1000) // Small delay after connection to let watch settle
                            syncTimeNow()
                        }
                    }
                }
            }
        }

        // Register screen wake receiver
        try {
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, screenFilter)
        } catch (e: Exception) {
            logRepository.log("SVC", "Failed to register screen receiver: ${e.message}", LogDirection.ERROR)
        }

        startTimeSyncScheduler()
        CallStateManager.startListening(this)
        logRepository.log("SVC", "VedaBleForegroundService started (connectedDevice type)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionUtils.canStartConnectedDeviceFgs(this)) {
            logRepository.log(
                "SVC",
                "onStartCommand: Missing Bluetooth permissions for connectedDevice FGS. Stopping service.",
                LogDirection.ERROR
            )
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            Constants.ACTION_CONNECT -> {
                val address = intent.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS)
                val name = intent.getStringExtra(Constants.EXTRA_DEVICE_NAME)
                if (!address.isNullOrBlank()) {
                    bleManager.connect(address, name)
                } else {
                    val lastAddress = preferencesRepository.settingsFlow.value.lastDeviceAddress
                    if (!lastAddress.isNullOrBlank()) {
                        bleManager.connect(lastAddress, preferencesRepository.settingsFlow.value.lastDeviceName)
                    } else {
                        bleManager.startScan()
                    }
                }
            }
            Constants.ACTION_DISCONNECT -> {
                bleManager.disconnect()
                stopSelf()
            }
            Constants.ACTION_START_SCAN -> {
                bleManager.startScan()
            }
            Constants.ACTION_STOP_SCAN -> {
                bleManager.stopScan()
            }
            Constants.ACTION_SYNC_TIME -> {
                serviceScope.launch {
                    syncTimeNow()
                }
            }
        }

        // START_NOT_STICKY ensures Android OS does not restart this service with null intents
        // or attempt invalid background service restorations.
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logRepository.log("SVC", "User swiped app from Recents. Maintaining persistent BLE background connection.")
    }

    private suspend fun syncTimeNow() {
        val payload = BleProtocol.buildTimePacket()
        logRepository.log("TIME_SYNC", "Pushing clock sync to watch: $payload", LogDirection.OUTBOUND)
        val success = bleManager.sendPayloadDirect(payload)
        if (success) {
            preferencesRepository.recordSyncTime()
            logRepository.log("TIME_SYNC", "Watch clock synchronized successfully")
        } else {
            logRepository.log("TIME_SYNC", "Watch clock sync failed or device disconnected", LogDirection.ERROR)
        }
    }

    private fun startTimeSyncScheduler() {
        timeSyncJob?.cancel()
        timeSyncJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val settings = preferencesRepository.settingsFlow.value
                val intervalHours = settings.syncIntervalHours.coerceIn(1, 24)
                delay(intervalHours * 60 * 60 * 1000L)

                if (settings.periodicTimeSync && bleManager.connectionState.value.isConnected) {
                    syncTimeNow()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.FOREGROUND_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundWithNotification(contentText: String): Boolean {
        val notification = buildNotification("VEDA Companion", contentText, isConnected = false)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification)
            }
            true
        } catch (e: SecurityException) {
            logRepository.log("SVC", "SecurityException during startForeground: ${e.message}", LogDirection.ERROR)
            false
        } catch (e: IllegalStateException) {
            logRepository.log("SVC", "ForegroundServiceStartNotAllowedException / IllegalStateException: ${e.message}", LogDirection.ERROR)
            false
        } catch (e: Exception) {
            logRepository.log("SVC", "Exception during startForeground: ${e.message}", LogDirection.ERROR)
            false
        }
    }

    private fun updateForegroundNotification(title: String, contentText: String, isConnected: Boolean) {
        val notification = buildNotification(title, contentText, isConnected)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, contentText: String, isConnected: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, Constants.FOREGROUND_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingLaunch)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (isConnected) {
            // Action 1: Manual Time Sync (only active while service is running and connected)
            val syncIntent = Intent(this, VedaBleForegroundService::class.java).apply {
                action = Constants.ACTION_SYNC_TIME
            }
            val pendingSync = PendingIntent.getService(
                this,
                1,
                syncIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Sync Time", pendingSync)

            // Action 2: Disconnect (safely stops service from notification)
            val disconnectIntent = Intent(this, VedaBleForegroundService::class.java).apply {
                action = Constants.ACTION_DISCONNECT
            }
            val pendingDisconnect = PendingIntent.getService(
                this,
                2,
                disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Disconnect", pendingDisconnect)
        }
        // Notice: When disconnected, no PendingIntent.getService() action is added.
        // Tapping the notification body (pendingLaunch) opens MainActivity safely,
        // allowing the user to initiate connection from a visible Activity context.

        return builder.build()
    }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignored on teardown
        }
        CallStateManager.stopListening(this)
        serviceScope.cancel()
        logRepository.log("SVC", "VedaBleForegroundService destroyed")
    }

    companion object {
        fun start(context: Context) {
            if (!PermissionUtils.canStartConnectedDeviceFgs(context)) {
                android.util.Log.w(
                    "VedaBleForegroundService",
                    "Cannot start connectedDevice service: Bluetooth permissions not granted."
                )
                return
            }
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to start service", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, VedaBleForegroundService::class.java))
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to stop service", e)
            }
        }

        fun connect(context: Context, address: String?, name: String?) {
            if (!PermissionUtils.canStartConnectedDeviceFgs(context)) {
                android.util.Log.w(
                    "VedaBleForegroundService",
                    "Cannot start connectedDevice service for connect: Bluetooth permissions not granted."
                )
                return
            }
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java).apply {
                    action = Constants.ACTION_CONNECT
                    putExtra(Constants.EXTRA_DEVICE_ADDRESS, address)
                    putExtra(Constants.EXTRA_DEVICE_NAME, name)
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to start service for connect", e)
            }
        }

        fun disconnect(context: Context) {
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java).apply {
                    action = Constants.ACTION_DISCONNECT
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to send disconnect to service", e)
            }
        }

        fun startScan(context: Context) {
            if (!PermissionUtils.canStartConnectedDeviceFgs(context)) {
                android.util.Log.w(
                    "VedaBleForegroundService",
                    "Cannot start scan via service: Bluetooth permissions not granted."
                )
                return
            }
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java).apply {
                    action = Constants.ACTION_START_SCAN
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to start scan via service", e)
            }
        }

        fun stopScan(context: Context) {
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java).apply {
                    action = Constants.ACTION_STOP_SCAN
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to stop scan via service", e)
            }
        }

        fun syncTime(context: Context) {
            try {
                val intent = Intent(context, VedaBleForegroundService::class.java).apply {
                    action = Constants.ACTION_SYNC_TIME
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("VedaBleForegroundService", "Failed to start service for syncTime", e)
            }
        }
    }
}
