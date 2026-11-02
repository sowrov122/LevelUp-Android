package com.levelup.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.LevelTitles
import com.levelup.app.domain.Motivation
import com.levelup.app.domain.TaskStatus
import com.levelup.app.ui.components.AppCard
import com.levelup.app.ui.components.ArtImage
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.FlameIcon
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.ProgressRing
import com.levelup.app.ui.components.SecondaryButton
import com.levelup.app.ui.components.SectionHeader
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.theme.Art
import com.levelup.app.ui.theme.Radii
import com.levelup.app.ui.viewmodel.TaskRowUi
import com.levelup.app.ui.viewmodel.TodayUiState
import com.levelup.app.ui.viewmodel.TodayViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    vm: TodayViewModel,
    highlightTaskId: String?,
    onStartFocus: () -> Unit,
    onAddTask: () -> Unit,
    onEditRoutine: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    TodayContent(state, highlightTaskId, onStartFocus, onAddTask, onEditRoutine, onToggle = vm::setCompleted)
}

/** Stateless Today screen: everything it shows comes from [state], which is built from saved data. */
@Composable
fun TodayContent(
    state: TodayUiState,
    highlightTaskId: String?,
    onStartFocus: () -> Unit,
    onAddTask: () -> Unit,
    onEditRoutine: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hour: Int = LocalTime.now().hour,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("today_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { GreetingCard(state, hour) }
        item { LevelCard(state) }
        item { SummaryRow(state) }
        item { MotivationCard(state) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Start Focus", onStartFocus, Modifier.weight(1f))
                SecondaryButton("Add Task", onAddTask, Modifier.weight(1f))
                SecondaryButton("Routine", onEditRoutine, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Today's tasks", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                if (state.total > 0) {
                    Text("${state.completed}/${state.total} done", style = MaterialTheme.typography.labelLarge, color = AppColors.TextSecondary)
                }
            }
        }
        if (!state.loading && state.tasks.isEmpty()) {
            item { EmptyState("Nothing scheduled today", "Add a task or enable days in your routine to see it here.") }
        }
        items(state.tasks, key = { it.instanceId }) { row ->
            TaskCard(row, highlighted = row.taskId == highlightTaskId, onToggle = { onToggle(row.instanceId, it) })
        }
    }
}

@Composable
private fun GreetingCard(state: TodayUiState, hour: Int) {
    val shape = RoundedCornerShape(Radii.card)
    val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    Box(Modifier.fillMaxWidth().height(124.dp).clip(shape).border(1.dp, AppColors.Border, shape)) {
        Image(
            painterResource(Art.HeroObservatory),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
        )
        // Plain left-to-right scrim so the text stays readable over the artwork.
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE6140E0A), Color(0x99140E0A), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp)) {
            Text(
                if (state.name.isBlank()) greeting else "$greeting,",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextSecondary,
            )
            if (state.name.isNotBlank()) {
                Text(state.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            }
            state.date?.let {
                Text(it.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun LevelCard(state: TodayUiState) {
    val level = state.level
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtImage(Art.LevelUpCrest, Modifier.size(60.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Level ${level.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text(LevelTitles.titleFor(level.level), style = MaterialTheme.typography.titleSmall, color = AppColors.TextSecondary)
                }
                ProgressBar(level.fraction, height = 10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${level.xpIntoLevel} / ${level.xpForNextLevel} XP", style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
                    Text(
                        "${level.xpForNextLevel - level.xpIntoLevel} XP to Lv. ${level.level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(state: TodayUiState) {
    val pct = if (state.total == 0) 0 else state.completed * 100 / state.total
    Row(Modifier.fillMaxWidth().height(172.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ProgressRing(
                    fraction = if (state.total == 0) 0f else state.completed.toFloat() / state.total,
                    centerText = "${state.completed}/${state.total}",
                    subText = "tasks done",
                    modifier = Modifier.size(124.dp),
                )
                Text("$pct% complete", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow(Modifier.weight(1f), { FlameIcon(Modifier.size(24.dp)) }, "${state.streaks.current} ${if (state.streaks.current == 1) "day" else "days"}", "streak · best ${state.streaks.longest}")
            StatRow(Modifier.weight(1f), { Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.RewardGold, modifier = Modifier.size(24.dp)) }, "${state.pointsToday}", "points today")
            StatRow(Modifier.weight(1f), { ArtImage(Art.Coin, Modifier.size(26.dp)) }, "${state.coins}", "coins")
        }
    }
}

@Composable
private fun StatRow(modifier: Modifier, icon: @Composable () -> Unit, value: String, label: String) {
    AppCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MotivationCard(state: TodayUiState) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                Motivation.message(state.completed, state.total, state.streaks.current, state.focusMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextPrimary,
            )
            Text("Focus today: ${formatMinutes(state.focusMinutes)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun TaskCard(row: TaskRowUi, highlighted: Boolean, onToggle: (Boolean) -> Unit) {
    val done = row.status == TaskStatus.COMPLETED
    val border = when {
        highlighted -> BorderStroke(2.dp, AppColors.ProgressBlue)
        row.inProgress -> BorderStroke(1.5.dp, AppColors.Lavender)
        else -> BorderStroke(1.dp, AppColors.Border)
    }
    AppCard(Modifier.fillMaxWidth(), border = border) {
        Row(Modifier.height(IntrinsicSize.Min).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(Color(row.category.colorHex)))
            Checkbox(
                checked = done,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = "Mark ${row.title} done" },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.Lavender,
                    checkmarkColor = AppColors.TextPrimary,
                    uncheckedColor = AppColors.TextSecondary,
                ),
            )
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text(
                    "${row.timeLabel} · ${formatMinutes(row.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            Column(Modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.End) {
                Text(
                    "+${if (done) row.pointsAwarded else row.xpReward} XP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.RewardGold,
                )
                when {
                    done -> Text("Done", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
                    row.inProgress -> Text("In progress", style = MaterialTheme.typography.labelSmall, color = AppColors.ProgressBlue)
                    row.status == TaskStatus.MISSED -> Text("Missed", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
                    else -> {}
                }
            }
        }
    }
}
