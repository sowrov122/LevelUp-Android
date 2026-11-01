package com.levelup.app.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.levelup.app.LevelUpApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs only while distraction protection is enabled, Usage Access is granted and a focus session is
 * RUNNING. Polls package-level foreground events every few seconds (no screen content, no keystrokes).
 */
class FocusGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as LevelUpApp).container
        val type = when {
            Build.VERSION.SDK_INT >= 34 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            Build.VERSION.SDK_INT >= 29 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            else -> 0
        }
        ServiceCompat.startForeground(this, GuardNotifier.GUARD_NOTIFICATION_ID, container.notifier.foregroundNotification(), type)

        if (job?.isActive != true) {
            job = scope.launch {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                var since = System.currentTimeMillis()
                while (isActive) {
                    if (!UsageAccess.isGranted(this@FocusGuardService) || !container.repository.isProtectionActive()) {
                        stopSelf()
                        break
                    }
                    val now = System.currentTimeMillis()
                    // Small overlap so no event is missed; duplicates are removed by the policy + unique keys.
                    val events = usm.queryEvents(since - OVERLAP_MS, now)
                    val e = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(e)
                        if (e.eventType == FOREGROUND_EVENT && e.packageName != packageName && e.timeStamp >= since - OVERLAP_MS) {
                            val label = runCatching {
                                packageManager.getApplicationLabel(packageManager.getApplicationInfo(e.packageName, 0)).toString()
                            }.getOrDefault(e.packageName)
                            container.repository.handleForegroundEvent(e.packageName, e.timeStamp, label)
                                ?.let { container.notifier.showInterruption(it) }
                        }
                    }
                    since = now
                    delay(POLL_MS)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val POLL_MS = 2_000L
        const val OVERLAP_MS = 3_000L
        const val FOREGROUND_EVENT = 1 // UsageEvents.Event.MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
    }
}
