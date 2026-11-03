package com.levelup.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.levelup.app.notifications.ReminderManager
import com.levelup.app.ui.AppRoot

class MainActivity : ComponentActivity() {
    private var openTaskId by mutableStateOf<String?>(null)
    private var openDest by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openTaskId = intent?.getStringExtra(ReminderManager.EXTRA_TASK_ID)
        openDest = intent?.getStringExtra(EXTRA_DEST)
        val container = (application as LevelUpApp).container
        setContent { AppRoot(container, openTaskId, { openTaskId = null }, openDest) { openDest = null } }
    }

    override fun onStart() {
        super.onStart()
        // A focus session may have reached its planned end while the app was closed: collect it now, once.
        val repo = (application as LevelUpApp).container.repository
        lifecycleScope.launch { repo.finishDueFocusSession() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID)
        openDest = intent.getStringExtra(EXTRA_DEST)
    }

    companion object {
        const val EXTRA_DEST = "dest"
    }
}
