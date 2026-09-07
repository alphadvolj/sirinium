package com.dlab.sirinium

import android.app.Application
import com.dlab.sirinium.di.appModule
import com.dlab.sirinium.notification.ScheduleNotificationManager
import com.dlab.sirinium.sync.SyncScheduler
import com.dlab.sirinium.widget.WidgetRefreshWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SiriniumApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Dependency Injection with Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SiriniumApp)
            modules(appModule)
        }

        // 2. Initialize Notification Channels
        ScheduleNotificationManager(this)

        // 3. Schedule Background Sync based on user settings
        val settingsPrefs = getSharedPreferences("sirinium_settings", MODE_PRIVATE)
        val autoSync = settingsPrefs.getBoolean("auto_sync_enabled", true)
        val syncInterval = settingsPrefs.getLong("auto_sync_interval_mins", 120L)
        if (autoSync) {
            SyncScheduler.schedulePeriodicSync(this, syncInterval)
        } else {
            SyncScheduler.cancelPeriodicSync(this)
        }

        // 4. Schedule periodic widget refresh (every 15 min) to keep widgets
        //    up-to-date without needing to open the app
        WidgetRefreshWorker.schedule(this)
    }
}
