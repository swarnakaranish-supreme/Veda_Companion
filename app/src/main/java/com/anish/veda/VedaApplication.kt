package com.anish.veda

import android.app.Application
import com.anish.veda.bluetooth.BleManager
import com.anish.veda.bluetooth.TimeSyncManager
import com.anish.veda.data.repository.LogRepository
import com.anish.veda.data.repository.PreferencesRepository

class VedaApplication : Application() {

    lateinit var logRepository: LogRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var bleManager: BleManager
        private set

    lateinit var timeSyncManager: TimeSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        logRepository = LogRepository(maxEntries = 400)
        preferencesRepository = PreferencesRepository(this)
        bleManager = BleManager(this, logRepository)
        timeSyncManager = TimeSyncManager(bleManager, preferencesRepository, logRepository)

        logRepository.log("SYSTEM", "VEDA Companion Application initialized.")
    }

    companion object {
        lateinit var instance: VedaApplication
            private set
    }
}
