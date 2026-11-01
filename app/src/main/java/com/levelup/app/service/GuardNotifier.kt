package com.levelup.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.levelup.app.MainActivity
import com.levelup.app.R
import com.levelup.app.data.InterruptionResult
import com.levelup.app.domain.InterruptionDecision

class GuardNotifier(private val context: Context) {
    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(GUARD_CHANNEL, "Focus protection status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while Level Up watches for your selected apps during a focus session"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, "Focus interruptions", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when a selected app is opened during a focus session"
            },
        )
    }

    fun foregroundNotification(): Notification {
        createChannels()
        return NotificationCompat.Builder(context, GUARD_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_levelup)
            .setContentTitle("Focus protection is on")
            .setContentText("Only your selected apps are checked, and only during this focus session.")
            .setOngoing(true)
            .setContentIntent(openFocusIntent())
            .build()
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun showInterruption(r: InterruptionResult) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        createChannels()
        val text = if (r.decision == InterruptionDecision.PENALIZE) {
            "−${r.penalty} points today. Head back to your focus session."
        } else {
            "Logged with no extra penalty (cooldown). Head back to your focus session."
        }
        val emergency = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, GuardActionReceiver::class.java).setAction(ACTION_EMERGENCY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_levelup)
            .setContentTitle("${r.displayName} opened during focus")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openFocusIntent())
            .addAction(0, "Emergency pause 10 min", emergency)
            .build()
        nm.notify(ALERT_NOTIFICATION_ID, n)
    }

    private fun openFocusIntent(): PendingIntent = PendingIntent.getActivity(
        context, 2,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DEST, "focus")
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val GUARD_CHANNEL = "focus_protection"
        const val ALERT_CHANNEL = "focus_alerts"
        const val GUARD_NOTIFICATION_ID = 9001
        const val ALERT_NOTIFICATION_ID = 9002
        const val ACTION_EMERGENCY = "com.levelup.app.action.EMERGENCY_OVERRIDE"
    }
}
