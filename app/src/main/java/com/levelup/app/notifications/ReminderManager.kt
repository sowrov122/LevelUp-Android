package com.levelup.app.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.levelup.app.MainActivity
import com.levelup.app.R
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.RoutineTaskEntity
import com.levelup.app.data.SettingsStore
import com.levelup.app.domain.TimeUtils
import java.time.Instant
import java.time.LocalTime

/**
 * Schedules one pending alarm per task (its *next* occurrence). Re-using the same PendingIntent
 * identity (action + data) replaces the previous alarm, so edits can never create duplicates.
 *
 * Uses setAndAllowWhileIdle (inexact): no exact-alarm permission is requested, so reminders may
 * arrive a few minutes late under Doze/battery optimisation.
 */
class ReminderManager(
    private val context: Context,
    private val db: LevelUpDatabase,
    private val settings: SettingsStore,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() {
        db.routineTaskDao().getAll().forEach { schedule(it) }
    }

    suspend fun cancelAll() {
        db.routineTaskDao().getAll().forEach { cancel(it.id) }
    }

    suspend fun schedule(task: RoutineTaskEntity) {
        val pi = pending(ACTION_REMIND, task.id)
        val offset = task.reminderOffsetMinutes
        if (!task.isEnabled || offset == null) {
            alarmManager.cancel(pi)
            return
        }
        val zone = TimeUtils.zoneOf(settings.current().timezoneId)
        val next = TimeUtils.nextReminderInstant(task.startMinute, task.repeatDaysMask, offset, Instant.now(), zone)
        if (next == null) {
            alarmManager.cancel(pi)
            return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pi)
    }

    fun cancel(taskId: String) {
        alarmManager.cancel(pending(ACTION_REMIND, taskId))
        alarmManager.cancel(pending(ACTION_SNOOZE_FIRE, taskId))
    }

    suspend fun onReminderFired(taskId: String, wasSnoozed: Boolean) {
        val task = db.routineTaskDao().get(taskId) ?: return
        if (task.isEnabled) {
            val s = settings.current()
            val zone = TimeUtils.zoneOf(s.timezoneId)
            val now = LocalTime.now(zone)
            val nowMinute = now.hour * 60 + now.minute
            val quiet = s.quietEnabled && TimeUtils.isInQuietHours(nowMinute, s.quietStartMinute, s.quietEndMinute)
            if (s.notificationsEnabled && !quiet) show(task)
        }
        if (!wasSnoozed) schedule(task) // plan the next occurrence
    }

    fun snooze(taskId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ACTION_SNOOZE_FIRE, taskId))
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    private fun show(task: RoutineTaskEntity) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        createChannel(context)

        val open = PendingIntent.getActivity(
            context,
            notificationId(task.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val offset = task.reminderOffsetMinutes ?: 0
        val text = if (offset > 0) "Starts in $offset min · ${TimeUtils.format(task.startMinute)}"
        else "Starting now · ${TimeUtils.format(task.startMinute)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_levelup)
            .setContentTitle(task.title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Snooze $SNOOZE_MINUTES min", pending(ACTION_SNOOZE, task.id))
            .build()
        nm.notify(notificationId(task.id), notification)
    }

    private fun notificationId(taskId: String): Int = taskId.hashCode()

    private fun pending(action: String, taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("levelup://${action.substringAfterLast('.').lowercase()}/$taskId"))
            .putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "routine_reminders"
        const val ACTION_REMIND = "com.levelup.app.action.REMIND"
        const val ACTION_SNOOZE = "com.levelup.app.action.SNOOZE"
        const val ACTION_SNOOZE_FIRE = "com.levelup.app.action.SNOOZE_FIRE"
        const val EXTRA_TASK_ID = "task_id"
        const val SNOOZE_MINUTES = 10

        fun createChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Routine reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Reminders for tasks in your daily routine"
                    },
                )
            }
        }
    }
}
