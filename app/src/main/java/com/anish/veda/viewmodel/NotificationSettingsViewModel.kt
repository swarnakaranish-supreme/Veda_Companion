package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.VedaSettings
import com.anish.veda.notifications.AppNotificationItem
import com.anish.veda.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VedaApplication
    private val preferencesRepository = app.preferencesRepository
    private val bleManager = app.bleManager
    val notificationSettingsRepo = NotificationSettingsRepository(application)

    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow
    val isMasterEnabled: StateFlow<Boolean> = notificationSettingsRepo.isMasterEnabled

    private val _installedApps = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val installedApps: StateFlow<List<AppNotificationItem>> = _installedApps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps

    val filteredApps: StateFlow<List<AppNotificationItem>> = combine(
        _installedApps,
        _searchQuery
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = notificationSettingsRepo.getInstalledApps()
            _isLoadingApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMasterNotifications(enabled: Boolean) {
        notificationSettingsRepo.setMasterEnabled(enabled)
        preferencesRepository.updateSettings { it.copy(forwardNotifications = enabled) }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        notificationSettingsRepo.setAppEnabled(packageName, enabled)
        val updated = _installedApps.value.map {
            if (it.packageName == packageName) it.copy(isEnabled = enabled) else it
        }
        _installedApps.value = updated
    }

    fun setMaxMessageLength(length: Int) {
        preferencesRepository.updateSettings { it.copy(maxMessageLength = length) }
    }

    fun sendTestNotification(appName: String, title: String, text: String) {
        viewModelScope.launch {
            val packet = BleProtocol.buildNormalNotificationPacket(
                appName = appName,
                title = title,
                message = text,
                maxMessageLen = settings.value.maxMessageLength
            )
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementNotificationCount()
        }
    }
}
