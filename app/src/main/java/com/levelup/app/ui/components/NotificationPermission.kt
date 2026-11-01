package com.levelup.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/**
 * Shown only while notifications are blocked. The permission dialog is never triggered before the
 * user has read why reminders need it.
 */
@Composable
fun NotificationPermissionCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var showWhy by remember { mutableStateOf(false) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        // If Android will not show the dialog again, send the user to the system settings screen.
        if (!enabled) openAppNotificationSettings(context)
    }

    if (enabled) return

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Reminders are turned off", style = MaterialTheme.typography.titleSmall)
            Text(
                "Without notification permission your tasks will not remind you. Everything else still works offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(onClick = { showWhy = true }) { Text("Turn on reminders") }
        }
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("Allow notifications?") },
            text = {
                Text(
                    "Level Up uses notifications only to remind you before a routine task starts. " +
                        "Reminders are created on your device and nothing is sent anywhere. " +
                        "Delivery can be a few minutes late when battery saving is active.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWhy = false
                    if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else openAppNotificationSettings(context)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showWhy = false }) { Text("Not now") } },
        )
    }
}
