package com.anish.veda.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anish.veda.VedaApplication

class VedaBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Under Android 12/14/15 background service restrictions, starting a connectedDevice
        // foreground service from BOOT_COMPLETED or MY_PACKAGE_REPLACED broadcast receivers
        // is strictly prohibited and throws ForegroundServiceStartNotAllowedException.
        // Auto-connect will be initiated safely once the user launches the app into the foreground.
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = VedaApplication.instance
            app.logRepository.log("BOOT", "Broadcast received: $action. Deferring connection until app is opened by user.")
        }
    }
}
