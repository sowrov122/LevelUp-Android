package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.InterruptionAction
import com.levelup.app.service.UsageAccess
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.NotificationPermissionCard
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.viewmodel.BlockerViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HISTORY_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm")

@Composable
fun BlockerScreen(vm: BlockerViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explain by remember { mutableStateOf(false) }
    var confirmPause by remember { mutableStateOf<Int?>(null) }
    var penaltyText by remember(s.settings.blockerPenaltyPoints) { mutableStateOf(s.settings.blockerPenaltyPoints.toString()) }
    var cooldownText by remember(s.settings.blockerCooldownSeconds) { mutableStateOf(s.settings.blockerCooldownSeconds.toString()) }
    var coinsText by remember(s.settings.blockerPenaltyCoins) { mutableStateOf(s.settings.blockerPenaltyCoins.toString()) }
    var xpText by remember(s.settings.blockerPenaltyXp) { mutableStateOf(s.settings.blockerPenaltyXp.toString()) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshAccess() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val pausedActive = s.settings.blockerPausedUntilMs > s.nowMs
    val q = s.query.trim().lowercase()
    val selectedPackages = s.selected.map { it.packageName }.toSet()
    val picker = s.installed
        .filter { q.isBlank() || it.label.lowercase().contains(q) }
        .sortedWith(compareByDescending<com.levelup.app.ui.viewmodel.InstalledApp> { it.packageName in selectedPackages }.thenBy { it.label.lowercase() })

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "During a focus session, Level Up checks which app comes to the front. If it is one you selected below, " +
                            "you get a reminder and lose a few points for the day (XP and level are never reduced). " +
                            "It runs only while a focus session is running, only for apps you chose, and nothing leaves your phone.",
                    )
                    Text(
                        "Limits: it cannot see what you do inside an app, cannot detect adult content or reels, and cannot stop an app from " +
                            "opening. Some phones stop background checks to save battery, so detection can be late or missing. " +
                            "You can switch it off at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Distraction protection", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = s.settings.blockerEnabled,
                    onCheckedChange = { on -> if (on && !s.usageAccess) explain = true else vm.setEnabled(on) },
                )
            }
            Text(
                when {
                    !s.settings.blockerEnabled -> "Off. Nothing is being observed."
                    !s.usageAccess -> "On, but waiting for Usage Access. Nothing is observed until you allow it."
                    pausedActive -> "Paused. No penalties until protection resumes."
                    else -> "On. Active during focus sessions."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Usage Access", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (s.usageAccess) "Allowed in Android settings" else "Not allowed",
                            color = if (s.usageAccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedButton(onClick = { context.startActivity(UsageAccess.settingsIntent()) }) { Text("Open settings") }
                }
            }
        }

        item { NotificationPermissionCard() }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strict mode", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = s.settings.blockerStrict, onCheckedChange = { vm.setStrict(it) })
                    }
                    Text(
                        "On: opening ANY app other than Level Up, the home screen, phone, messages, maps, clock and system settings " +
                            "during a focus session costs you. Off: only the apps you pick below count. " +
                            "Android does not let one app lock other apps, so this is a penalty, not a wall. " +
                            "For a harder lock use \"Lock phone to Level Up\" on the Focus screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionTitle("Rules")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    penaltyText, { penaltyText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Penalty (points)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    cooldownText, { cooldownText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Cooldown (sec)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    coinsText, { coinsText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Also −coins") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    xpText, { xpText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Also −XP") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Coins and XP never go below zero. Deducting XP can lower your level, so it is off (0) by default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "One launch costs at most one penalty, then the same app is only logged until the cooldown ends (30–3600 s). " +
                    "Daily points never drop below zero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Button(onClick = { vm.saveRules(penaltyText.toIntOrNull() ?: 0, cooldownText.toIntOrNull() ?: 300, coinsText.toIntOrNull() ?: 0, xpText.toIntOrNull() ?: 0) }) { Text("Save rules") }
        }

        item {
            SectionTitle("Pause / emergency")
            if (pausedActive) {
                Text("Paused until ${HISTORY_FORMAT.format(Instant.ofEpochMilli(s.settings.blockerPausedUntilMs).atZone(ZoneId.systemDefault()))}")
                OutlinedButton(onClick = { vm.resume() }) { Text("Resume protection now") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { m ->
                        FilterChip(selected = false, onClick = { confirmPause = m }, label = { Text("Pause $m min") })
                    }
                }
                Text(
                    "Phone, messages, maps, clock and system settings are never restricted, so you can always reach them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            SectionTitle("Apps to restrict (${s.selected.size})")
            OutlinedTextField(
                s.query, { vm.setQuery(it) }, label = { Text("Search apps") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            if (s.loadingApps) Text("Loading installed apps…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!s.loadingApps && picker.isEmpty()) Text("No matching apps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(picker, key = { it.packageName }) { app ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = app.packageName in selectedPackages, onCheckedChange = { vm.toggleApp(app, it) })
                Text(app.label, Modifier.weight(1f))
            }
        }

        item { SectionTitle("Interruption history") }
        if (s.interruptions.isEmpty()) {
            item { Text("No interruptions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(s.interruptions, key = { it.id }) { e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(e.displayName)
                    Text(
                        HISTORY_FORMAT.format(Instant.ofEpochMilli(e.detectedAt).atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    when (e.actionTaken) {
                        InterruptionAction.PENALIZED -> "−${e.pointsPenalty} pts"
                        InterruptionAction.LOGGED_ONLY -> "logged"
                        InterruptionAction.OVERRIDE -> "override"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text("Allow Usage Access?") },
            text = {
                Text(
                    "To notice when a selected app opens, Android requires you to allow \"Usage access\" for Level Up in its settings. " +
                        "Level Up reads only which app came to the front, only during a focus session, and only for apps you picked. " +
                        "It does not read screen content, keystrokes or messages, and nothing is uploaded. " +
                        "You can revoke it anytime in Android settings.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    vm.setEnabled(true)
                    context.startActivity(UsageAccess.settingsIntent())
                }) { Text("Continue to settings") }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Not now") } },
        )
    }

    confirmPause?.let { minutes ->
        ConfirmDialog(
            "Pause protection for $minutes minutes?",
            "No points will be deducted while paused. The pause is recorded in your interruption history.",
            "Pause",
            onConfirm = { vm.pause(minutes); confirmPause = null },
            onDismiss = { confirmPause = null },
        )
    }
}
