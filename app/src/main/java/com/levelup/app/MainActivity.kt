package com.levelup.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.levelup.app.notifications.ReminderManager
import com.levelup.app.ui.AppRoot

class MainActivity : ComponentActivity() {
    private var openTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openTaskId = intent?.getStringExtra(ReminderManager.EXTRA_TASK_ID)
        val container = (application as LevelUpApp).container
        setContent { AppRoot(container, openTaskId) { openTaskId = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID)
    }
}
