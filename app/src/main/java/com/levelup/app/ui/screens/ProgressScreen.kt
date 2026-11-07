package com.levelup.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.domain.AchievementUnit
import com.levelup.app.ui.components.AppCard
import com.levelup.app.ui.components.ArtImage
import com.levelup.app.ui.components.AxisBarChart
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.FlameIcon
import com.levelup.app.ui.components.HeatmapGrid
import com.levelup.app.ui.components.IconTile
import com.levelup.app.ui.components.LinkButton
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.theme.AppColors
import com.levelup.app.ui.theme.Art
import com.levelup.app.ui.viewmodel.AchievementUi
import com.levelup.app.ui.viewmodel.ProgressUiState
import com.levelup.app.ui.viewmodel.ProgressViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat

@Composable
fun ProgressScreen(vm: ProgressViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    ProgressContent(s, onRange = vm::setRange, onOffset = vm::setOffset)
}

/** Stateless Progress screen. Every number comes from [s], which is built from stored history only. */
@Composable
fun ProgressContent(s: ProgressUiState, onRange: (Int) -> Unit, onOffset: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var achievementsY by remember { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 12.dp).testTag("progress_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.BarChart, null, tint = AppColors.TextPrimary, modifier = Modifier.size(38.dp))
            Column(Modifier.weight(1f)) {
                Text("Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Text("Your growth, in numbers", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
            IconTile(Icons.Filled.EmojiEvents, "Go to achievements", { scope.launch { scroll.animateScrollTo(achievementsY) } })
        }

        // 7 / 30 days and the period picker
        val shape = RoundedCornerShape(14.dp)
        Row(Modifier.fillMaxWidth().clip(shape).background(AppColors.Card).border(1.dp, AppColors.Border, shape).padding(3.dp)) {
            listOf("7 Days" to 7, "30 Days" to 30).forEach { (label, days) ->
                val selected = s.rangeDays == days
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) AppColors.Lavender else Color.Transparent)
                        .clickable(role = Role.Tab) { onRange(days) }
                        .padding(vertical = 10.dp)
                        .testTag("range_$days"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary)
                }
            }
        }
        var periodMenu by remember { mutableStateOf(false) }
        Box {
            Row(
                Modifier.fillMaxWidth().clip(shape).background(AppColors.Card).border(1.dp, AppColors.Border, shape)
                    .clickable(onClickLabel = "Choose a period") { periodMenu = true }.padding(horizontal = 12.dp, vertical = 11.dp)
                    .testTag("period_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                Text(s.rangeLabel, Modifier.weight(1f), color = AppColors.TextPrimary)
                Icon(Icons.Filled.ArrowDropDown, null, tint = AppColors.TextSecondary)
            }
            DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                s.periods.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(if (p.offset == 0) "${p.label} (current)" else p.label) },
                        onClick = { periodMenu = false; onOffset(p.offset) },
                    )
                }
            }
        }

        if (!s.loading && !s.hasAnyData) {
            AppCard(Modifier.fillMaxWidth().testTag("progress_empty")) {
                EmptyState(
                    "No history yet",
                    "Complete a task or a focus session and your charts will appear here. Nothing on this screen is sample data.",
                )
            }
        }

        // summary tiles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(Modifier.weight(1f), { FlameIcon(Modifier.size(30.dp)) }, "Current Streak", dayCount(s.currentStreak))
            SummaryTile(Modifier.weight(1f), { ArtImage(Art.Trophy, Modifier.size(30.dp)) }, "Best Streak", dayCount(s.bestStreak))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                Modifier.weight(1f).testTag("tile_focus_time"),
                { Icon(Icons.Filled.Schedule, null, tint = AppColors.TextPrimary, modifier = Modifier.size(28.dp)) },
                "Focus time · ${s.rangeDays} days", hoursMinutes(s.focusMinutes),
            )
            SummaryTile(
                Modifier.weight(1f).testTag("tile_sessions"),
                { Icon(Icons.Filled.TrackChanges, null, tint = AppColors.TextPrimary, modifier = Modifier.size(28.dp)) },
                "Sessions · ${s.rangeDays} days", "${s.focusSessions}",
            )
        }

        // charts
        ChartCard("Daily Completion (%)", { Icon(Icons.Filled.CheckCircle, null, tint = AppColors.ProgressBlue) }, "completion_chart", {
            info = "Tasks completed divided by tasks scheduled on that day. A day with nothing scheduled shows 0%."
        }) {
            AxisBarChart(s.completionBars, axisMax = 100, barColor = AppColors.ProgressBlue, valueSuffix = "%")
            if (s.hasAnyData && !s.periodHasData) NoDataNote()
        }
        ChartCard("Daily Points", { Icon(Icons.Filled.Star, null, tint = AppColors.RewardGold) }, "points_chart", {
            info = "Points from your reward ledger for that day: task rewards, bonuses and focus sessions, minus any distraction penalties. A day never shows below 0."
        }) {
            AxisBarChart(s.pointsBars, axisMax = s.pointsAxisMax, barColor = AppColors.RewardGold)
            if (s.hasAnyData && !s.periodHasData) NoDataNote()
        }

        ChartCard("Activity Heatmap (Last 5 Weeks)", { Icon(Icons.Filled.CalendarMonth, null, tint = AppColors.TextSecondary) }, "heatmap", {
            info = "Each square is one day. Colour shows the share of that day's tasks you completed; a focus session on a day with no completed tasks counts as low activity."
        }) { HeatmapGrid(s.heatWeeks) }

        // achievements
        Row(
            Modifier.fillMaxWidth().onGloballyPositioned { achievementsY = it.positionInParent().y.toInt() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.EmojiEvents, null, tint = AppColors.RewardGold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("Achievements", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            LinkButton(if (expanded) "Show less" else "View All", { expanded = !expanded })
        }
        (if (expanded) s.achievements else s.achievements.take(4)).forEach { AchievementCard(it) }
        Spacer(Modifier.height(8.dp))
    }

    info?.let { text ->
        AlertDialog(
            onDismissRequest = { info = null },
            containerColor = AppColors.Elevated,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text("How this is calculated") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Got it", color = AppColors.ProgressBlue) } },
        )
    }
}

private fun dayCount(n: Int) = "$n ${if (n == 1) "Day" else "Days"}"
private fun hoursMinutes(minutes: Int) = if (minutes < 60) "$minutes m" else "${minutes / 60} h ${minutes % 60} m"

@Composable
private fun NoDataNote() {
    Text("No activity in this period. Try another range.", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SummaryTile(modifier: Modifier, icon: @Composable () -> Unit, label: String, value: String) {
    AppCard(modifier.height(72.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.Elevated), contentAlignment = Alignment.Center) { icon() }
            Column {
                Text(label, fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, icon: @Composable () -> Unit, tag: String, onInfo: () -> Unit, content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Icon(
                    Icons.Filled.Info, contentDescription = "About $title", tint = AppColors.TextSecondary,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onInfo).padding(8.dp),
                )
            }
            content()
        }
    }
}

// ============================================================ achievements

@Composable
private fun AchievementCard(a: AchievementUi) {
    AppCard(Modifier.fillMaxWidth().testTag("achievement_${a.def.id}")) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AchievementBadge(a.def.id, a.completed)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(a.def.title, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text(a.def.description, fontSize = 12.sp, color = AppColors.TextSecondary, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressBar(a.fraction, Modifier.weight(1f), color = if (a.completed) AppColors.Success else AppColors.ProgressBlue, height = 7.dp)
                    Text(progressText(a), fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1, softWrap = false, modifier = Modifier.testTag("progress_${a.def.id}"))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatePill(a.completed)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Star, null, tint = AppColors.RewardGold, modifier = Modifier.size(13.dp))
                    Text("+${a.def.xp} XP", fontSize = 11.sp, color = AppColors.TextPrimary, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ArtImage(Art.Coin, Modifier.size(14.dp))
                    Text("+${a.def.coins} Coins", fontSize = 11.sp, color = AppColors.TextPrimary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StatePill(completed: Boolean) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (completed) Color.Transparent else AppColors.Lavender)
            .border(1.dp, if (completed) AppColors.Success.copy(alpha = 0.8f) else AppColors.Lavender, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (completed) Icon(Icons.Filled.Check, null, tint = AppColors.Success, modifier = Modifier.size(11.dp))
        Text(if (completed) "Completed" else "In Progress", fontSize = 10.sp, color = if (completed) AppColors.Success else AppColors.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun AchievementBadge(id: String, completed: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(46.dp).clip(shape).background(AppColors.Elevated).border(1.5.dp, if (completed) AppColors.RewardGold else AppColors.Border, shape),
        contentAlignment = Alignment.Center,
    ) {
        val m = Modifier.size(32.dp).alpha(if (completed) 1f else 0.85f)
        when (id) {
            "task_master" -> ArtImage(Art.Trophy, m)
            "first_task", "routine_champion" -> ArtImage(Art.BadgeMedal, m)
            "streak_5", "consistency_king" -> FlameIcon(m)
            "level_5" -> ArtImage(Art.LevelUpCrest, m)
            "xp_collector" -> ArtImage(Art.Crown, m)
            "study_20h" -> Icon(Icons.Filled.MenuBook, null, tint = AppColors.TextPrimary, modifier = Modifier.size(26.dp))
            else -> Icon(Icons.Filled.Timer, null, tint = AppColors.TextPrimary, modifier = Modifier.size(26.dp))
        }
    }
}

private fun progressText(a: AchievementUi): String {
    val n = NumberFormat.getIntegerInstance()
    return when (a.def.unit) {
        AchievementUnit.MINUTES -> "${formatMinutes(a.shownProgress.toInt())} / ${formatMinutes(a.def.target.toInt())}"
        else -> "${n.format(a.shownProgress)} / ${n.format(a.def.target)}"
    }
}
