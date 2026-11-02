package com.levelup.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import com.levelup.app.ui.components.AppCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.LevelTitles
import com.levelup.app.domain.TaskStatus
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.ObservatoryScene
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.ProgressRing
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.components.StatCard
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.theme.Danger
import com.levelup.app.ui.theme.Success
import com.levelup.app.ui.viewmodel.TaskRowUi
import com.levelup.app.ui.viewmodel.TodayViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val MESSAGES = listOf(
    "Small steps, done daily, add up.",
    "Start with the next task, not the whole day.",
    "A missed block is just a reset point.",
    "Progress beats perfection.",
    "Protect your focus, and the rest follows.",
    "One finished task changes the mood of the day.",
    "Consistency is a skill. You are training it.",
    "Rest is part of the routine too.",
    "Begin now; adjust as you go.",
    "Show up for the next hour.",
)

@Composable
fun TodayScreen(
    vm: TodayViewModel,
    highlightTaskId: String?,
    onStartFocus: () -> Unit,
    onAddTask: () -> Unit,
    onEditRoutine: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val date = state.date

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val hour = LocalTime.now().hour
            val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
            Box(Modifier.fillMaxWidth()) {
                ObservatoryScene(state.heroSessions, state.heroMinutes, state.heroOwned, state.reducedMotion, Modifier.clip(RoundedCornerShape(24.dp)))
                Column(Modifier.padding(18.dp)) {
                    Text(greeting + if (state.name.isBlank()) "" else ",", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
                    if (state.name.isNotBlank()) {
                        Text(state.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (date != null) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }
        }

        // Level card
        item {
            AppCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${state.level.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Lv. ${state.level.level}  ${LevelTitles.titleFor(state.level.level)}", fontWeight = FontWeight.Bold)
                        ProgressBar(state.level.fraction, color = MaterialTheme.colorScheme.primary, height = 10.dp)
                        Text(
                            "${state.level.xpIntoLevel} / ${state.level.xpForNextLevel} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Progress ring + stat tiles
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val pct = if (state.total == 0) 0 else state.completed * 100 / state.total
                        ProgressRing(
                            fraction = if (state.total == 0) 0f else state.completed.toFloat() / state.total,
                            centerText = "${state.completed}/${state.total}",
                            subText = "Tasks done",
                            modifier = Modifier.size(150.dp),
                        )
                        Text("$pct%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("🔥", "${state.streaks.current}", "Day streak")
                    MiniStat("⭐", "${state.pointsToday}", "Points today")
                    MiniStat("🪙", "${state.coins}", "Coins")
                }
            }
        }

        // Motivation
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("❝", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Column {
                        if (date != null) {
                            Text(MESSAGES[date.toEpochDay().mod(MESSAGES.size.toLong()).toInt()], style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "Focus today: ${formatMinutes(state.focusMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Continue Your Routine  →", onEditRoutine, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onStartFocus, modifier = Modifier.weight(1f)) { Text("Start Focus") }
                    OutlinedButton(onClick = onAddTask, modifier = Modifier.weight(1f)) { Text("Add Task") }
                }
            }
        }

        state.nextTask?.let { next ->
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (next.inProgress) "Happening now" else "Up next",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(next.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${next.timeLabel} · ${formatMinutes(next.durationMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionTitle("Today's tasks") }

        if (!state.loading && state.tasks.isEmpty()) {
            item {
                EmptyState(
                    "Nothing scheduled today",
                    "Add a task or enable days in your routine to see it here.",
                )
            }
        }

        items(state.tasks, key = { it.instanceId }) { row ->
            TaskRow(row, highlighted = row.taskId == highlightTaskId, onToggle = { vm.setCompleted(row.instanceId, it) })
        }
    }
}

@Composable
private fun TaskRow(row: TaskRowUi, highlighted: Boolean, onToggle: (Boolean) -> Unit) {
    AppCard(
        Modifier.fillMaxWidth(),
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = row.status == TaskStatus.COMPLETED, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${row.timeLabel} · ${formatMinutes(row.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(row.category.colorHex)))
                when {
                    row.status == TaskStatus.COMPLETED -> Text("+${row.pointsAwarded}", style = MaterialTheme.typography.labelMedium, color = Success)
                    row.inProgress -> Text("Now", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    row.status == TaskStatus.MISSED -> Text("Missed", style = MaterialTheme.typography.labelMedium, color = Danger)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: String, value: String, label: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
