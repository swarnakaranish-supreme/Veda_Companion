package com.anish.veda.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.LogDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class TelephonyState {
    IDLE,
    RINGING,
    OFFHOOK
}

data class CallTelemetryState(
    val state: TelephonyState = TelephonyState.IDLE,
    val lastCaller: String = "None",
    val lastEvent: String = "Telephony engine ready (Android 15)",
    val lastTimestamp: Long = 0L,
    val isIncomingActive: Boolean = false,
    val isCallActive: Boolean = false,
    val isOutgoing: Boolean = false
)

object CallStateManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _telemetryState = MutableStateFlow(CallTelemetryState())
    val telemetryState: StateFlow<CallTelemetryState> = _telemetryState.asStateFlow()

    // Internal state tracking
    private var previousTelephonyState: Int = TelephonyManager.CALL_STATE_IDLE
    private var isIncomingRinging = false
    private var wasAnswered = false
    private var isOutgoing = false
    private var incomingAlertSent = false
    private var missedAlertSentForCurrentSession = false
    private var pendingCallerIdentity: String? = null
    private var incomingPendingJob: Job? = null
    private var missedPendingJob: Job? = null

    // Deduplication across listeners
    private var lastIncomingAlertTimestamp = 0L
    private var lastMissedAlertTimestamp = 0L
    private var lastMissedCaller: String? = null
    private val isListenerRegistered = AtomicBoolean(false)

    // Hardware Telephony Listeners
    private var telephonyCallback: Any? = null
    private var legacyListener: Any? = null

    /**
     * Registers TelephonyManager listener.
     * Uses TelephonyCallback on Android 12+ (API 31-35, e.g. OPPO A59 5G on Android 15),
     * and PhoneStateListener on older versions.
     */
    fun startListening(context: Context) {
        if (isListenerRegistered.get()) return

        val app = VedaApplication.instance
        val logRepo = app.logRepository

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logRepo.log("CALL_TELEPHONY", "READ_PHONE_STATE permission not granted. Telephony listener postponed.")
            return
        }

        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager == null) {
                logRepo.log("CALL_TELEPHONY", "TelephonyManager service unavailable.", LogDirection.ERROR)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerModernTelephonyCallback(context, telephonyManager)
            } else {
                registerLegacyPhoneStateListener(context, telephonyManager)
            }

            isListenerRegistered.set(true)
            logRepo.log("CALL_TELEPHONY", "Telephony state listener active (API ${Build.VERSION.SDK_INT})")
        } catch (e: SecurityException) {
            logRepo.log("CALL_TELEPHONY", "SecurityException registering TelephonyCallback: ${e.message}", LogDirection.ERROR)
        } catch (e: Exception) {
            logRepo.log("CALL_TELEPHONY", "Error registering TelephonyCallback: ${e.message}", LogDirection.ERROR)
        }
    }

    /**
     * Unregisters TelephonyManager listener to prevent leaks.
     */
    fun stopListening(context: Context) {
        if (!isListenerRegistered.get()) return

        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val cb = telephonyCallback as? TelephonyCallback
                    if (cb != null) {
                        telephonyManager.unregisterTelephonyCallback(cb)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val pl = legacyListener as? PhoneStateListener
                    if (pl != null) {
                        telephonyManager.listen(pl, PhoneStateListener.LISTEN_NONE)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored on teardown
        } finally {
            telephonyCallback = null
            legacyListener = null
            isListenerRegistered.set(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModernTelephonyCallback(context: Context, tm: TelephonyManager) {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleTelephonyState(context, state, null)
            }
        }
        tm.registerTelephonyCallback(context.mainExecutor, callback)
        telephonyCallback = callback
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyPhoneStateListener(context: Context, tm: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleTelephonyState(context, state, phoneNumber)
            }
        }
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        legacyListener = listener
    }

    /**
     * Centralized telephony state transition evaluator.
     */
    @Synchronized
    fun handleTelephonyState(context: Context, state: Int, rawNumber: String?) {
        val app = VedaApplication.instance
        val logRepo = app.logRepository

        if (!rawNumber.isNullOrBlank()) {
            pendingCallerIdentity = rawNumber.trim()
        }

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Prevent duplicate/repeated incoming-call events while continuously ringing
                if (isIncomingRinging) {
                    return
                }

                isIncomingRinging = true
                wasAnswered = false
                isOutgoing = false
                incomingAlertSent = false
                missedAlertSentForCurrentSession = false

                logRepo.log("CALL_TELEPHONY", "Incoming call ringing detected (TelephonyCallback)")
                updateTelemetry(
                    state = TelephonyState.RINGING,
                    caller = pendingCallerIdentity ?: "Detecting...",
                    event = "Incoming call ringing",
                    isIncoming = true,
                    isCall = false,
                    isOut = false
                )

                // Debounce 350ms to allow NotificationListenerService to supply full caller name from heads-up notification
                incomingPendingJob?.cancel()
                incomingPendingJob = scope.launch {
                    delay(350)
                    if (isIncomingRinging && !incomingAlertSent) {
                        dispatchIncomingCallAlert(context, pendingCallerIdentity)
                    }
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                incomingPendingJob?.cancel()

                if (isIncomingRinging) {
                    // Call was answered!
                    wasAnswered = true
                    isIncomingRinging = false
                    logRepo.log("CALL_TELEPHONY", "Call answered by user (OFFHOOK)")
                    updateTelemetry(
                        state = TelephonyState.OFFHOOK,
                        caller = pendingCallerIdentity ?: "Active Call",
                        event = "Call answered",
                        isIncoming = false,
                        isCall = true,
                        isOut = false
                    )
                } else if (previousTelephonyState == TelephonyManager.CALL_STATE_IDLE) {
                    // Direct transition from IDLE to OFFHOOK -> Outgoing call!
                    isOutgoing = true
                    wasAnswered = false
                    isIncomingRinging = false
                    incomingAlertSent = false
                    logRepo.log("CALL_TELEPHONY", "Outgoing call placed by user (Alerts suppressed)")
                    updateTelemetry(
                        state = TelephonyState.OFFHOOK,
                        caller = "Outgoing Call",
                        event = "Outgoing call active (Alerts suppressed)",
                        isIncoming = false,
                        isCall = true,
                        isOut = true
                    )
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                incomingPendingJob?.cancel()

                // Check for missed call
                val isMissedCandidate = isIncomingRinging && !wasAnswered && !isOutgoing

                if (isMissedCandidate) {
                    logRepo.log("CALL_TELEPHONY", "Unanswered incoming call ended -> evaluating missed call")
                    // Allow NotificationListenerService (which receives CATEGORY_MISSED_CALL from dialer) to process
                    missedPendingJob?.cancel()
                    missedPendingJob = scope.launch {
                        delay(1000)
                        if (!missedAlertSentForCurrentSession) {
                            dispatchMissedCallAlert(context, pendingCallerIdentity)
                        }
                    }
                }

                // Reset state
                isIncomingRinging = false
                wasAnswered = false
                isOutgoing = false
                incomingAlertSent = false
                pendingCallerIdentity = null

                updateTelemetry(
                    state = TelephonyState.IDLE,
                    caller = "None",
                    event = if (isMissedCandidate) "Missed call ended" else "Call ended (Phone Idle)",
                    isIncoming = false,
                    isCall = false,
                    isOut = false
                )
            }
        }

        previousTelephonyState = state
    }

    /**
     * Invoked by VedaNotificationListener when an incoming call notification is posted.
     */
    @Synchronized
    fun onIncomingCallNotification(context: Context, extractedCaller: String?, packageName: String) {
        val app = VedaApplication.instance
        val logRepo = app.logRepository

        // Suppress if outgoing call or alert already sent
        if (isOutgoing || incomingAlertSent) {
            return
        }

        if (!extractedCaller.isNullOrBlank()) {
            pendingCallerIdentity = extractedCaller
        }

        isIncomingRinging = true
        wasAnswered = false
        missedAlertSentForCurrentSession = false

        incomingPendingJob?.cancel()
        dispatchIncomingCallAlert(context, pendingCallerIdentity)
        logRepo.log("CALL_NOTIF", "Incoming call captured from dialer ($packageName): ${pendingCallerIdentity ?: "UNKNOWN"}")
    }

    /**
     * Invoked by VedaNotificationListener when a missed call notification is posted.
     */
    @Synchronized
    fun onMissedCallNotification(context: Context, extractedCaller: String?, packageName: String) {
        val app = VedaApplication.instance
        val logRepo = app.logRepository

        missedPendingJob?.cancel()

        val caller = extractedCaller ?: pendingCallerIdentity
        dispatchMissedCallAlert(context, caller)
        logRepo.log("CALL_NOTIF", "Missed call captured from dialer ($packageName): ${caller ?: "UNKNOWN"}")
    }

    /**
     * Builds and enqueues the incoming call packet: N|1|Phone|Incoming call|CALLER_NAME
     */
    private fun dispatchIncomingCallAlert(context: Context, rawCaller: String?) {
        val app = VedaApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value
        val bleManager = app.bleManager
        val logRepo = app.logRepository

        if (!settings.forwardCalls) {
            logRepo.log("CALL_FWD", "Incoming call alerts disabled in settings.")
            return
        }

        if (!bleManager.connectionState.value.isConnected) {
            logRepo.log("CALL_FWD", "Watch disconnected. Dropping incoming call alert.")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastIncomingAlertTimestamp < 3000L) {
            return // Prevent duplicate alert
        }

        val callerDisplay = resolveCallerDisplay(context, rawCaller, settings.showCallerName, settings.resolveCallerNames)
        val packet = BleProtocol.buildIncomingCallPacket(callerDisplay)

        bleManager.enqueuePayload(packet)
        incomingAlertSent = true
        lastIncomingAlertTimestamp = now
        app.preferencesRepository.incrementCallCount()

        logRepo.log("CALL_FWD", "Forwarded Incoming Call to Watch: $callerDisplay", LogDirection.OUTBOUND, packet)
        updateTelemetry(
            state = TelephonyState.RINGING,
            caller = callerDisplay,
            event = "Incoming Call forwarded: $callerDisplay",
            isIncoming = true,
            isCall = false,
            isOut = false
        )
    }

    /**
     * Builds and enqueues the missed call packet: N|2|Phone|Missed call|CALLER_NAME
     */
    private fun dispatchMissedCallAlert(context: Context, rawCaller: String?) {
        val app = VedaApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value
        val bleManager = app.bleManager
        val logRepo = app.logRepository

        if (!settings.forwardMissedCalls) {
            logRepo.log("CALL_FWD", "Missed call alerts disabled in settings.")
            return
        }

        if (!bleManager.connectionState.value.isConnected) {
            logRepo.log("CALL_FWD", "Watch disconnected. Dropping missed call alert.")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastMissedAlertTimestamp < 5000L && lastMissedCaller == rawCaller) {
            return // Prevent duplicate missed call alerts
        }

        val callerDisplay = resolveCallerDisplay(context, rawCaller, settings.showCallerName, settings.resolveCallerNames)
        val packet = BleProtocol.buildMissedCallPacket(callerDisplay)

        bleManager.enqueuePayload(packet)
        missedAlertSentForCurrentSession = true
        lastMissedAlertTimestamp = now
        lastMissedCaller = rawCaller

        logRepo.log("CALL_FWD", "Forwarded Missed Call to Watch: $callerDisplay", LogDirection.OUTBOUND, packet)
        updateTelemetry(
            state = TelephonyState.IDLE,
            caller = callerDisplay,
            event = "Missed Call forwarded: $callerDisplay",
            isIncoming = false,
            isCall = false,
            isOut = false
        )
    }

    /**
     * Resolves caller identity according to privacy settings and contact book lookup.
     * If caller information is unavailable or showCallerName is disabled, outputs UNKNOWN.
     */
    fun resolveCallerDisplay(
        context: Context,
        rawCaller: String?,
        showCallerName: Boolean,
        resolveContactNames: Boolean
    ): String {
        if (!showCallerName) {
            return "UNKNOWN"
        }

        if (rawCaller.isNullOrBlank()) {
            return "UNKNOWN"
        }

        val clean = BleProtocol.sanitizePipes(rawCaller.trim())
        if (clean.isBlank() || clean.equals("unknown", ignoreCase = true)) {
            return "UNKNOWN"
        }

        // Attempt contact name resolution if it looks like a phone number
        if (resolveContactNames && looksLikePhoneNumber(clean)) {
            val contactName = lookupContactName(context, clean)
            if (!contactName.isNullOrBlank()) {
                return BleProtocol.sanitizePipes(contactName.trim())
            }
        }

        return clean
    }

    private fun looksLikePhoneNumber(text: String): Boolean {
        val digitsAndSymbols = text.count { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
        return digitsAndSymbols >= 5 && text.any { it.isDigit() }
    }

    private fun lookupContactName(context: Context, phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            val uri: Uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        it.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun updateTelemetry(
        state: TelephonyState,
        caller: String,
        event: String,
        isIncoming: Boolean,
        isCall: Boolean,
        isOut: Boolean
    ) {
        _telemetryState.value = CallTelemetryState(
            state = state,
            lastCaller = caller,
            lastEvent = event,
            lastTimestamp = System.currentTimeMillis(),
            isIncomingActive = isIncoming,
            isCallActive = isCall,
            isOutgoing = isOut
        )
    }
}
