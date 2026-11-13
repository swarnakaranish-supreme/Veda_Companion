package com.anish.veda

import android.app.Application
import com.anish.veda.ai.AiCommandDispatcher
import com.anish.veda.ai.GeminiApiClient
import com.anish.veda.ai.VoiceInputManager
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.TimeSyncManager
import com.anish.veda.data.database.AiChatDatabaseHelper
import com.anish.veda.data.repository.AiSettingsRepository
import com.anish.veda.data.repository.DeviceRepository
import com.anish.veda.data.repository.GuardianRepository
import com.anish.veda.data.repository.HapticRepository
import com.anish.veda.data.repository.HealthRepository
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.data.repository.NotificationRepository
import com.anish.veda.data.repository.PreferencesRepository
import com.anish.veda.guardian.EmergencyDispatcher
import com.anish.veda.guardian.GuardianLocationProvider
import com.anish.veda.notifications.NotificationSettingsRepository

class VedaApplication : Application() {

    lateinit var logRepository: LogRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var bleManager: BleManager
        private set

    lateinit var timeSyncManager: TimeSyncManager
        private set

    lateinit var healthRepository: HealthRepository
        private set

    lateinit var notificationSettingsRepository: NotificationSettingsRepository
        private set

    lateinit var notificationRepository: NotificationRepository
        private set

    lateinit var hapticRepository: HapticRepository
        private set

    lateinit var aiSettingsRepository: AiSettingsRepository
        private set

    lateinit var aiChatDatabaseHelper: AiChatDatabaseHelper
        private set

    lateinit var aiCommandDispatcher: AiCommandDispatcher
        private set

    lateinit var geminiApiClient: GeminiApiClient
        private set

    lateinit var voiceInputManager: VoiceInputManager
        private set

    lateinit var guardianLocationProvider: GuardianLocationProvider
        private set

    lateinit var emergencyDispatcher: EmergencyDispatcher
        private set

    lateinit var guardianRepository: GuardianRepository
        private set

    lateinit var deviceRepository: DeviceRepository
        private set

    lateinit var smartRoutineEngine: com.anish.veda.data.repository.SmartRoutineEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        logRepository = LogRepository(maxEntries = 400)
        preferencesRepository = PreferencesRepository(this)
        bleManager = BleManager(this, logRepository)
        timeSyncManager = TimeSyncManager(bleManager, preferencesRepository, logRepository)
        healthRepository = HealthRepository(this, bleManager, logRepository)

        notificationSettingsRepository = NotificationSettingsRepository(this)
        notificationRepository = NotificationRepository(this, notificationSettingsRepository)
        hapticRepository = HapticRepository(this, bleManager, logRepository)

        // Initialize VEDA AI Intelligence
        aiSettingsRepository = AiSettingsRepository(this)
        aiChatDatabaseHelper = AiChatDatabaseHelper(this)
        aiCommandDispatcher = AiCommandDispatcher(
            context = this,
            healthRepository = healthRepository,
            notificationRepository = notificationRepository,
            notificationSettingsRepo = notificationSettingsRepository,
            hapticRepository = hapticRepository,
            bleManager = bleManager
        )
        geminiApiClient = GeminiApiClient(aiSettingsRepository)
        voiceInputManager = VoiceInputManager(this)

        // Initialize Guardian Safety Subsystem
        guardianLocationProvider = GuardianLocationProvider(this)
        emergencyDispatcher = EmergencyDispatcher(this)
        guardianRepository = GuardianRepository(
            context = this,
            bleManager = bleManager,
            locationProvider = guardianLocationProvider,
            emergencyDispatcher = emergencyDispatcher,
            logRepository = logRepository
        )
        deviceRepository = DeviceRepository(
            context = this,
            bleManager = bleManager,
            timeSyncManager = timeSyncManager,
            preferencesRepository = preferencesRepository,
            logRepository = logRepository
        )
        smartRoutineEngine = com.anish.veda.data.repository.SmartRoutineEngine(
            bleManager = bleManager
        )

        logRepository.log("SYSTEM", "VEDA Companion Application initialized with VEDA AI, Guardian & Device Management.")
    }

    companion object {
        lateinit var instance: VedaApplication
            private set
    }
}
