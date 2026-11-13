package com.anish.veda.guardian

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.anish.veda.data.model.EmergencyContact
import com.anish.veda.data.model.GpsCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

sealed class SmsDispatchResult {
    data class Success(val recipientCount: Int) : SmsDispatchResult()
    data class Failure(val reason: String) : SmsDispatchResult()
    data class ManualActionRequired(val intent: Intent) : SmsDispatchResult()
}

class EmergencyDispatcher(private val context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun startCountdownVibration() {
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 300, 200, 300, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Repeat
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    fun stopVibration() {
        vibrator.cancel()
    }

    suspend fun dispatchEmergencyBroadcast(
        contacts: List<EmergencyContact>,
        messageTemplate: String,
        location: GpsCoordinates?
    ): SmsDispatchResult = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) {
            return@withContext SmsDispatchResult.Failure("No emergency contacts configured.")
        }

        val locationText = if (location != null) {
            "${location.mapsUrl} (${location.displayCoords})"
        } else {
            "Location unavailable (GPS fix pending)"
        }

        val finalMessage = messageTemplate.replace("{LOCATION}", locationText)

        // Check if SMS permission is granted
        if (!hasSmsPermission()) {
            // Create fallback intent for SMS app
            val numbers = contacts.joinToString(";") { it.phoneNumber }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$numbers")
                putExtra("sms_body", finalMessage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return@withContext SmsDispatchResult.ManualActionRequired(intent)
        }

        // Send via SmsManager with broadcast confirmation
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var sentSuccessCount = 0
        val errors = mutableListOf<String>()

        for (contact in contacts) {
            val phone = contact.phoneNumber.trim()
            if (phone.isEmpty()) continue

            val result = sendSingleSmsWithConfirmation(smsManager, phone, finalMessage)
            if (result) {
                sentSuccessCount++
            } else {
                errors.add("Failed to send to ${contact.name} ($phone)")
            }
        }

        if (sentSuccessCount > 0) {
            SmsDispatchResult.Success(sentSuccessCount)
        } else {
            SmsDispatchResult.Failure(errors.joinToString("; ").ifEmpty { "Carrier dispatch error." })
        }
    }

    private suspend fun sendSingleSmsWithConfirmation(
        smsManager: SmsManager,
        phoneNumber: String,
        message: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val actionId = "VEDA_SMS_SENT_${System.currentTimeMillis()}_${phoneNumber.hashCode()}"
        val sentIntent = PendingIntent.getBroadcast(
            context,
            phoneNumber.hashCode(),
            Intent(actionId),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                context.unregisterReceiver(this)
                val isSuccess = resultCode == Activity.RESULT_OK
                if (continuation.isActive) {
                    continuation.resume(isSuccess)
                }
            }
        }

        val filter = IntentFilter(actionId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }

        try {
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>().apply {
                    add(sentIntent)
                    for (i in 1 until parts.size) add(PendingIntent.getBroadcast(context, 0, Intent("DUMMY"), PendingIntent.FLAG_IMMUTABLE))
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
            }
        } catch (e: Exception) {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }
}
