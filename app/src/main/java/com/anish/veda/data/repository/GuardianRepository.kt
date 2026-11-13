package com.anish.veda.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.anish.veda.bluetooth.BleConnectionState
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.EmergencyContact
import com.anish.veda.data.model.GpsCoordinates
import com.anish.veda.data.model.GuardianConfig
import com.anish.veda.data.model.LogDirection
import com.anish.veda.data.model.SosStatus
import com.anish.veda.guardian.EmergencyDispatcher
import com.anish.veda.guardian.GuardianLocationProvider
import com.anish.veda.guardian.SmsDispatchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class GuardianRepository(
    private val context: Context,
    private val bleManager: BleManager,
    val locationProvider: GuardianLocationProvider,
    val emergencyDispatcher: EmergencyDispatcher,
    private val logRepository: LogRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("veda_guardian_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // SOS State
    private val _sosStatus = MutableStateFlow(SosStatus.IDLE)
    val sosStatus: StateFlow<SosStatus> = _sosStatus.asStateFlow()

    private val _remainingCountdown = MutableStateFlow(10)
    val remainingCountdown: StateFlow<Int> = _remainingCountdown.asStateFlow()

    private val _lastDispatchMessage = MutableStateFlow<String?>(null)
    val lastDispatchMessage: StateFlow<String?> = _lastDispatchMessage.asStateFlow()

    private val _contactsFlow = MutableStateFlow(loadContacts())
    val contactsFlow: StateFlow<List<EmergencyContact>> = _contactsFlow.asStateFlow()

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<GuardianConfig> = _configFlow.asStateFlow()

    private val _disconnectionAlertEvent = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val disconnectionAlertEvent: SharedFlow<String> = _disconnectionAlertEvent.asSharedFlow()

    private var countdownJob: Job? = null
    private var lastWasConnected = false

    init {
        // Listen to BLE incoming telemetry notifications
        scope.launch {
            bleManager.incomingNotificationFlow.collect { rawPayload ->
                handleIncomingBlePayload(rawPayload)
            }
        }

        // Listen to connection state for phone disconnection alerts
        scope.launch {
            bleManager.connectionState.collect { state ->
                val isConnected = state is BleConnectionState.Connected
                if (lastWasConnected && !isConnected && state !is BleConnectionState.Disconnecting) {
                    // Unexpected disconnection
                    if (_configFlow.value.disconnectionAlertEnabled) {
                        val alertMsg = "Smartwatch disconnected unexpectedly! Check device proximity."
                        logRepository.log("GUARDIAN", alertMsg)
                        _disconnectionAlertEvent.tryEmit(alertMsg)
                    }
                }
                lastWasConnected = isConnected
            }
        }
    }

    private fun handleIncomingBlePayload(raw: String) {
        val sosAction = BleProtocol.parseEmergencyPacket(raw) ?: return
        logRepository.log("GUARDIAN_BLE", "Received Guardian Event from watch: $sosAction", LogDirection.INBOUND)
        when (sosAction) {
            "TRIGGER" -> {
                startCountdown(fromWatch = true)
            }
            "CANCEL" -> {
                cancelCountdown(fromWatch = true)
            }
            "DISPATCH" -> {
                dispatchSos(fromWatch = true)
            }
        }
    }

    // ========================================================================
    // SOS EXECUTION LIFECYCLE
    // ========================================================================

    fun startCountdown(fromWatch: Boolean = false) {
        if (_sosStatus.value == SosStatus.COUNTDOWN || _sosStatus.value == SosStatus.DISPATCHING_SMS) {
            return
        }

        val totalSecs = _configFlow.value.countdownSeconds
        _remainingCountdown.value = totalSecs
        _sosStatus.value = SosStatus.COUNTDOWN
        _lastDispatchMessage.value = null
        logRepository.log("GUARDIAN", "SOS Countdown started (${totalSecs}s) [Source: ${if (fromWatch) "Watch" else "Phone"}].")

        if (_configFlow.value.vibratePhoneOnSos) {
            emergencyDispatcher.startCountdownVibration()
        }

        // Forward to watch if triggered from phone
        if (!fromWatch) {
            scope.launch {
                bleManager.sendPayloadDirect(BleProtocol.buildEmergencyTriggerPacket())
            }
        }

        // Start pre-fetching location in background during countdown
        scope.launch {
            locationProvider.getFreshLocation()
        }

        countdownJob?.cancel()
        countdownJob = scope.launch {
            var remaining = totalSecs
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _remainingCountdown.value = remaining
            }
            // Countdown expired without cancellation -> Dispatch SOS
            dispatchSos(fromWatch = false)
        }
    }

    fun cancelCountdown(fromWatch: Boolean = false) {
        countdownJob?.cancel()
        countdownJob = null
        emergencyDispatcher.stopVibration()

        _sosStatus.value = SosStatus.CANCELLED
        _lastDispatchMessage.value = "SOS was cancelled by user."
        logRepository.log("GUARDIAN", "SOS Cancelled [Source: ${if (fromWatch) "Watch" else "Phone"}].")

        // Forward to watch if cancelled on phone
        if (!fromWatch) {
            scope.launch {
                bleManager.sendPayloadDirect(BleProtocol.buildEmergencyCancelPacket())
            }
        }

        // Auto-rearm to IDLE after 3.5 seconds
        scope.launch {
            delay(3500L)
            if (_sosStatus.value == SosStatus.CANCELLED) {
                _sosStatus.value = SosStatus.IDLE
                _lastDispatchMessage.value = null
            }
        }
    }

    fun dispatchSos(fromWatch: Boolean = false) {
        countdownJob?.cancel()
        countdownJob = null
        emergencyDispatcher.stopVibration()

        scope.launch {
            _sosStatus.value = SosStatus.FETCHING_LOCATION
            logRepository.log("GUARDIAN", "Acquiring precision GPS fix for emergency dispatch...")

            // Try to get fresh location with fallback to current
            val location = locationProvider.getFreshLocation() ?: locationProvider.currentLocation.value

            _sosStatus.value = SosStatus.DISPATCHING_SMS
            logRepository.log("GUARDIAN", "Dispatching emergency SMS broadcast...")

            val contacts = _contactsFlow.value
            val template = _configFlow.value.messageTemplate
            val result = emergencyDispatcher.dispatchEmergencyBroadcast(contacts, template, location)

            when (result) {
                is SmsDispatchResult.Success -> {
                    _sosStatus.value = SosStatus.SENT_CONFIRMED
                    val msg = "Emergency broadcast confirmed sent to ${result.recipientCount} contact(s)."
                    _lastDispatchMessage.value = msg
                    logRepository.log("GUARDIAN", msg)
                }
                is SmsDispatchResult.ManualActionRequired -> {
                    _sosStatus.value = SosStatus.FAILED
                    _lastDispatchMessage.value = "SMS permission not granted. Launching SMS app..."
                    logRepository.log("GUARDIAN", "SMS permission required - redirected to SMS app.")
                    try {
                        context.startActivity(result.intent)
                    } catch (e: Exception) {
                        _lastDispatchMessage.value = "Failed to launch SMS intent: ${e.message}"
                    }
                }
                is SmsDispatchResult.Failure -> {
                    _sosStatus.value = SosStatus.FAILED
                    _lastDispatchMessage.value = "Emergency dispatch failed: ${result.reason}"
                    logRepository.log("GUARDIAN", "Dispatch failed: ${result.reason}")
                }
            }
        }
    }

    fun resetStatus() {
        countdownJob?.cancel()
        countdownJob = null
        emergencyDispatcher.stopVibration()
        _sosStatus.value = SosStatus.IDLE
        _remainingCountdown.value = _configFlow.value.countdownSeconds
        _lastDispatchMessage.value = null
    }

    // ========================================================================
    // EMERGENCY CONTACTS PERSISTENCE
    // ========================================================================

    private fun loadContacts(): List<EmergencyContact> {
        val raw = prefs.getString(KEY_CONTACTS_JSON, null) ?: return defaultContacts()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<EmergencyContact>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    EmergencyContact(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        phoneNumber = obj.getString("phoneNumber"),
                        relationship = obj.optString("relationship", "Emergency Contact"),
                        isPrimary = obj.optBoolean("isPrimary", false)
                    )
                )
            }
            if (list.isEmpty()) defaultContacts() else list
        } catch (e: Exception) {
            defaultContacts()
        }
    }

    private fun defaultContacts(): List<EmergencyContact> {
        return listOf(
            EmergencyContact(
                name = "Primary Emergency Contact",
                phoneNumber = "112",
                relationship = "Emergency Services",
                isPrimary = true
            )
        )
    }

    private fun saveContacts(contacts: List<EmergencyContact>) {
        val array = JSONArray()
        for (c in contacts) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phoneNumber", c.phoneNumber)
                put("relationship", c.relationship)
                put("isPrimary", c.isPrimary)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONTACTS_JSON, array.toString()).apply()
        _contactsFlow.value = contacts
    }

    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        val current = _contactsFlow.value.toMutableList()
        if (isPrimary) {
            // Demote other primary contacts
            for (i in current.indices) {
                if (current[i].isPrimary) current[i] = current[i].copy(isPrimary = false)
            }
        }
        current.add(
            EmergencyContact(
                name = name.trim(),
                phoneNumber = phone.trim(),
                relationship = relationship.trim().ifEmpty { "Emergency Contact" },
                isPrimary = isPrimary || current.isEmpty()
            )
        )
        saveContacts(current)
    }

    fun removeContact(id: String) {
        val current = _contactsFlow.value.filter { it.id != id }
        saveContacts(current)
    }

    fun setPrimaryContact(id: String) {
        val current = _contactsFlow.value.map { contact ->
            contact.copy(isPrimary = contact.id == id)
        }
        saveContacts(current)
    }

    // ========================================================================
    // CONFIGURATION MANAGEMENT
    // ========================================================================

    private fun loadConfig(): GuardianConfig {
        return GuardianConfig(
            gestureMode = prefs.getInt(KEY_GESTURE_MODE, GuardianConfig.GESTURE_HOLD_3S),
            countdownSeconds = prefs.getInt(KEY_COUNTDOWN_SECONDS, 10),
            messageTemplate = prefs.getString(KEY_MSG_TEMPLATE, null)
                ?: "EMERGENCY: I need immediate assistance! My current GPS location: {LOCATION}",
            disconnectionAlertEnabled = prefs.getBoolean(KEY_DISCONNECT_ALERT, true),
            vibratePhoneOnSos = prefs.getBoolean(KEY_VIBRATE_PHONE, true)
        )
    }

    fun updateConfig(update: (GuardianConfig) -> GuardianConfig) {
        val newCfg = update(_configFlow.value)
        prefs.edit()
            .putInt(KEY_GESTURE_MODE, newCfg.gestureMode)
            .putInt(KEY_COUNTDOWN_SECONDS, newCfg.countdownSeconds)
            .putString(KEY_MSG_TEMPLATE, newCfg.messageTemplate)
            .putBoolean(KEY_DISCONNECT_ALERT, newCfg.disconnectionAlertEnabled)
            .putBoolean(KEY_VIBRATE_PHONE, newCfg.vibratePhoneOnSos)
            .apply()
        _configFlow.value = newCfg

        // Automatically sync gesture & countdown config to watch
        syncConfigToWatch(newCfg)
    }

    fun syncConfigToWatch(config: GuardianConfig = _configFlow.value) {
        val packet = BleProtocol.buildGuardianConfigPacket(config.gestureMode, config.countdownSeconds)
        scope.launch {
            bleManager.sendPayloadDirect(packet)
            logRepository.log("GUARDIAN", "Synced Guardian config to watch: Gesture=${if (config.gestureMode == 0) "HOLD 3s" else "TRIPLE CLICK"}, Countdown=${config.countdownSeconds}s")
        }
    }

    companion object {
        private const val KEY_CONTACTS_JSON = "guardian_contacts_json"
        private const val KEY_GESTURE_MODE = "guardian_gesture_mode"
        private const val KEY_COUNTDOWN_SECONDS = "guardian_countdown_seconds"
        private const val KEY_MSG_TEMPLATE = "guardian_msg_template"
        private const val KEY_DISCONNECT_ALERT = "guardian_disconnect_alert"
        private const val KEY_VIBRATE_PHONE = "guardian_vibrate_phone"
    }
}
