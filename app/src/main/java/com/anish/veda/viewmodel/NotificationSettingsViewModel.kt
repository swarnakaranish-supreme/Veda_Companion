package com.anish.veda.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.veda.VedaApplication
import com.anish.veda.bluetooth.BleProtocol
import com.anish.veda.data.model.NotificationHistoryItem
import com.anish.veda.data.model.VedaSettings
import com.anish.veda.notifications.AppNotificationItem
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
    val notificationSettingsRepo = app.notificationSettingsRepository
    val notificationRepo = app.notificationRepository

    val settings: StateFlow<VedaSettings> = preferencesRepository.settingsFlow
    val isMasterEnabled: StateFlow<Boolean> = notificationSettingsRepo.isMasterEnabled

    // History state
    val historyList: StateFlow<List<NotificationHistoryItem>> = notificationRepo.historyFlow
    val unreadCount: StateFlow<Int> = notificationRepo.unreadCountFlow

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery

    private val _selectedCategoryFilter = MutableStateFlow<Int?>(null)
    val selectedCategoryFilter: StateFlow<Int?> = _selectedCategoryFilter

    // Installed apps state
    private val _installedApps = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val installedApps: StateFlow<List<AppNotificationItem>> = _installedApps

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps

    val filteredApps: StateFlow<List<AppNotificationItem>> = combine(
        _installedApps,
        _appSearchQuery
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

    // Policy & Privacy state
    val privacyModeEnabled: StateFlow<Boolean> = notificationSettingsRepo.privacyModeEnabled
    val hideSenderInPrivacy: StateFlow<Boolean> = notificationSettingsRepo.hideSenderInPrivacy
    val quietHoursEnabled: StateFlow<Boolean> = notificationSettingsRepo.quietHoursEnabled
    val quietHoursStartHour: StateFlow<Int> = notificationSettingsRepo.quietHoursStartHour
    val quietHoursStartMinute: StateFlow<Int> = notificationSettingsRepo.quietHoursStartMinute
    val quietHoursEndHour: StateFlow<Int> = notificationSettingsRepo.quietHoursEndHour
    val quietHoursEndMinute: StateFlow<Int> = notificationSettingsRepo.quietHoursEndMinute
    val quietHoursAllowCalls: StateFlow<Boolean> = notificationSettingsRepo.quietHoursAllowCalls

    val vipContactsOnly: StateFlow<Boolean> = notificationSettingsRepo.vipContactsOnly
    val vipContactsList: StateFlow<Set<String>> = notificationSettingsRepo.vipContacts

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

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
        notificationRepo.refreshHistory(query, _selectedCategoryFilter.value)
    }

    fun setSelectedCategoryFilter(category: Int?) {
        _selectedCategoryFilter.value = category
        notificationRepo.refreshHistory(_historySearchQuery.value, category)
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
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

    fun setAllAppsEnabled(enabled: Boolean) {
        notificationSettingsRepo.setAllApps(_installedApps.value, enabled)
        val updated = _installedApps.value.map { it.copy(isEnabled = enabled) }
        _installedApps.value = updated
    }

    fun enablePopularAppsOnly() {
        notificationSettingsRepo.enablePopularAppsOnly(_installedApps.value)
        val updated = _installedApps.value.map { it.copy(isEnabled = it.isPopular) }
        _installedApps.value = updated
    }

    fun setPrivacyMode(enabled: Boolean) {
        notificationSettingsRepo.setPrivacyMode(enabled)
    }

    fun setHideSenderInPrivacy(enabled: Boolean) {
        notificationSettingsRepo.setHideSenderInPrivacy(enabled)
    }

    fun setQuietHours(
        enabled: Boolean,
        startH: Int,
        startM: Int,
        endH: Int,
        endM: Int,
        allowCalls: Boolean
    ) {
        notificationSettingsRepo.setQuietHours(enabled, startH, startM, endH, endM, allowCalls)
    }

    fun setVipContactsOnly(enabled: Boolean) {
        notificationSettingsRepo.setVipContactsOnly(enabled)
    }

    fun addVipContact(contact: String) {
        notificationSettingsRepo.addVipContact(contact)
    }

    fun removeVipContact(contact: String) {
        notificationSettingsRepo.removeVipContact(contact)
    }

    fun setMaxMessageLength(length: Int) {
        preferencesRepository.updateSettings { it.copy(maxMessageLength = length) }
    }

    fun markAllAsRead() {
        notificationRepo.markAllAsRead()
    }

    fun markAsRead(id: Long) {
        notificationRepo.markAsRead(id)
    }

    fun deleteNotification(id: Long) {
        notificationRepo.deleteNotification(id)
    }

    fun clearHistory() {
        notificationRepo.clearHistory()
    }

    fun sendTestNotification(
        category: Int = BleProtocol.CATEGORY_NORMAL,
        appName: String = "Test App",
        title: String = "VEDA Test Notification",
        text: String = "This is a test notification from VEDA V2 Companion."
    ) {
        viewModelScope.launch {
            val maxLen = settings.value.maxMessageLength
            val isPrivacy = privacyModeEnabled.value
            val hideSender = hideSenderInPrivacy.value

            val effectiveTitle = if (isPrivacy && hideSender) "VEDA Alert" else title
            val effectiveText = if (isPrivacy) "[Content Hidden for Privacy]" else text

            val packet = BleProtocol.buildNotificationPacket(
                category = category,
                app = appName,
                title = effectiveTitle,
                message = effectiveText,
                customMaxMsgLength = maxLen
            )
            bleManager.sendPayloadDirect(packet)
            preferencesRepository.incrementNotificationCount()

            // Record test alert in database
            notificationRepo.recordNotification(
                NotificationHistoryItem(
                    timestamp = System.currentTimeMillis(),
                    packageName = "com.anish.veda.test",
                    appName = appName,
                    title = title,
                    message = text,
                    category = category,
                    categoryName = com.anish.veda.notifications.NotificationCategory.getCategoryName(category),
                    wasForwarded = true,
                    rejectionReason = null,
                    isRead = false,
                    isVip = false
                )
            )
        }
    }
}
