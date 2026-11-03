package com.levelup.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.levelup.app.LevelUpApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles reminder alarms and the notification's Snooze action. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID) ?: return
        val container = (context.applicationContext as LevelUpApp).container
        val reminders = container.reminders
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ReminderManager.ACTION_FOCUS_END ->
                        container.repository.finishDueFocusSession()?.let { reminders.showFocusComplete(it) }
                    ReminderManager.ACTION_REMIND -> reminders.onReminderFired(taskId, wasSnoozed = false)
                    ReminderManager.ACTION_SNOOZE_FIRE -> reminders.onReminderFired(taskId, wasSnoozed = true)
                    ReminderManager.ACTION_SNOOZE -> reminders.snooze(taskId)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Boot, app update, time-zone and clock changes: rebuild every reminder alarm. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminders = (context.applicationContext as LevelUpApp).container.reminders
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                reminders.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Emergency pause" action on the interruption notification. */
class GuardActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != com.levelup.app.service.GuardNotifier.ACTION_EMERGENCY) return
        val container = (context.applicationContext as LevelUpApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.repository.pauseProtection(10, emergency = true)
                androidx.core.app.NotificationManagerCompat.from(context)
                    .cancel(com.levelup.app.service.GuardNotifier.ALERT_NOTIFICATION_ID)
            } finally {
                pending.finish()
            }
        }
    }
}
