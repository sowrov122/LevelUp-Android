package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.Category
import com.levelup.app.domain.Observatory
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.ObservatoryScene
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.components.formatClock
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.viewmodel.FocusViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(vm: FocusViewModel, onOpenBlocker: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var preset by rememberSaveable { mutableIntStateOf(25) }
    var custom by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ObservatoryScene(state.sessions, state.minutes, state.owned, state.reducedMotion)
        Text(
            "${state.sessions} sessions · ${formatMinutes(state.minutes)} focused · ${Observatory.starCount(state.sessions, state.minutes)} stars",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val active = state.active
        if (active != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(active.title, style = MaterialTheme.typography.titleMedium)
                    Text(formatClock(active.remainingSeconds), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (active.running) "Focusing…" else "Paused",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (active.interrupted) {
                        Text(
                            "The device restarted, so the timer was paused to avoid counting time it cannot verify.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (active.running) Button(onClick = { vm.pause(active.id) }) { Text("Pause") }
                        else Button(onClick = { vm.resume(active.id) }) { Text("Resume") }
                        OutlinedButton(onClick = { confirmFinish = true }) { Text("Finish") }
                        TextButton(onClick = { confirmCancel = true }) { Text("Cancel") }
                    }
                }
            }
            if (confirmFinish) {
                ConfirmDialog(
                    "Finish early?",
                    "You will earn rewards only for the minutes already focused (at least 5 minutes are needed).",
                    "Finish",
                    onConfirm = { confirmFinish = false; vm.finish(active.id) },
                    onDismiss = { confirmFinish = false },
                )
            }
            if (confirmCancel) {
                ConfirmDialog(
                    "Cancel this session?",
                    "Cancelled sessions earn no points, XP or coins.",
                    "Cancel session",
                    onConfirm = { confirmCancel = false; vm.cancel(active.id) },
                    onDismiss = { confirmCancel = false },
                )
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New focus session", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(title, { title = it }, label = { Text("What will you focus on?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 25, 45, 60).forEach { m ->
                            FilterChip(
                                selected = custom.isBlank() && preset == m,
                                onClick = { preset = m; custom = "" },
                                label = { Text("$m min") },
                            )
                        }
                    }
                    OutlinedTextField(
                        custom, { custom = it.filter(Char::isDigit).take(3) },
                        label = { Text("Custom minutes (1–600)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Category (optional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Category.entries.forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c.label) })
                        }
                    }
                    val minutes = custom.toIntOrNull() ?: preset
                    Button(
                        onClick = { vm.start(title, category, minutes) },
                        enabled = minutes in 1..600,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start focus") }
                    Text(
                        "Rewards: 1 XP per focused minute, 1 point per 5 minutes, daily cap applies. Sessions under 5 minutes earn nothing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.lastOutcome?.let { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (o.completed) {
                        Text("Session complete", style = MaterialTheme.typography.titleMedium)
                        Text("${o.focusedMinutes} min focused · +${o.reward.points} points · +${o.reward.xp} XP · +${o.reward.coins} coins")
                        if (o.cappedByDailyLimit) {
                            Text("Daily focus reward cap reached: only ${o.rewardedMinutes} min were rewarded.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("Session too short", style = MaterialTheme.typography.titleMedium)
                        Text("Sessions under 5 minutes are not rewarded.")
                    }
                    TextButton(onClick = { vm.dismissOutcome() }) { Text("Dismiss") }
                }
            }
        }

        TextButton(onClick = onOpenBlocker) { Text("Distraction protection settings") }

        SectionTitle("Observatory milestones")
        Observatory.milestones.forEach { m ->
            val done = Observatory.isUnlocked(m, state.sessions, state.minutes)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(m.title, fontWeight = FontWeight.SemiBold)
                    Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (done) "Unlocked" else "Locked", color = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
