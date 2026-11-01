package com.levelup.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.SettingsStore
import com.levelup.app.domain.FocusStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Starts/stops the watcher service. Nothing runs unless the user turned protection on AND granted Usage Access. */
class GuardController(
    private val context: Context,
    private val repo: LevelUpRepository,
    private val settings: SettingsStore,
) {
    fun observe(scope: CoroutineScope) {
        scope.launch {
            combine(
                settings.flow.map { it.blockerEnabled }.distinctUntilChanged(),
                repo.observeActiveFocus().map { it?.status == FocusStatus.RUNNING }.distinctUntilChanged(),
            ) { enabled, running -> enabled && running && UsageAccess.isGranted(context) }
                .distinctUntilChanged()
                .collect { shouldRun -> if (shouldRun) start() else stop() }
        }
    }

    private fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, FocusGuardService::class.java))
        } catch (_: Exception) {
            // Android 12+ may refuse a background start; protection simply stays off until the app is opened.
        }
    }

    private fun stop() {
        context.stopService(Intent(context, FocusGuardService::class.java))
    }
}
