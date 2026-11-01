package com.levelup.app

import android.app.Application
import android.content.Context
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.SettingsStore
import com.levelup.app.notifications.ReminderManager
import com.levelup.app.service.EssentialApps
import com.levelup.app.service.GuardController
import com.levelup.app.service.GuardNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Small explicit dependency graph (no DI framework needed for this size). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: LevelUpDatabase = LevelUpDatabase.build(appContext)
    val settings = SettingsStore(appContext)
    val reminders = ReminderManager(appContext, database, settings)
    val repository = LevelUpRepository(
        database, settings, reminders,
        essentialPackages = { EssentialApps.all(appContext) },
    )
    val notifier = GuardNotifier(appContext)
    val guard = GuardController(appContext, repository, settings)
}

class LevelUpApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderManager.createChannel(this)
        container.notifier.createChannels()
        container.guard.observe(appScope)
        appScope.launch { container.repository.bootstrap() }
    }
}
