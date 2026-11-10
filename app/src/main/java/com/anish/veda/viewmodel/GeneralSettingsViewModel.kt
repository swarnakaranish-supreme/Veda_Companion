package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anish.veda.VedaApplication
import com.anish.veda.data.model.VedaSettings
import com.anish.veda.services.VedaBleForegroundService
import com.anish.veda.utils.PermissionUtils
import kotlinx.coroutines.flow.StateFlow

class GeneralSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val preferencesRepository = app.preferencesRepository

    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow

    fun setAutoConnect(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(autoConnect = enabled) }
    }

    fun setAutoSyncTime(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(autoSyncTimeOnConnect = enabled) }
    }

    fun setPeriodicTimeSync(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(periodicTimeSync = enabled) }
    }

    fun setSyncIntervalHours(hours: Int) {
        preferencesRepository.updateSettings { it.copy(syncIntervalHours = hours) }
    }

    fun setAccentColorIndex(index: Int) {
        preferencesRepository.updateSettings { it.copy(accentColorIndex = index) }
    }

    fun restartForegroundService() {
        val context = getApplication<Application>()
        if (PermissionUtils.hasBluetoothPermissions(context)) {
            VedaBleForegroundService.stop(context)
            VedaBleForegroundService.start(context)
        }
    }
}
