package com.levelup.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.app.ui.components.BarChart
import com.levelup.app.ui.components.EmptyState
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.SectionTitle
import com.levelup.app.ui.components.StatCard
import com.levelup.app.ui.components.formatMinutes
import com.levelup.app.ui.viewmodel.ProgressViewModel
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(vm: ProgressViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = s.rangeDays == 7, onClick = { vm.setRange(7) }, label = { Text("7 days") })
            FilterChip(selected = s.rangeDays == 30, onClick = { vm.setRange(30) }, label = { Text("30 days") })
        }

        if (!s.loading && !s.hasAnyData) {
            EmptyState(
                "No history yet",
                "Complete a task or a focus session and your charts will appear here. Nothing on this screen is sample data.",
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Streak", "${s.streaks.current} d", Modifier.weight(1f), sub = "best ${s.streaks.longest}")
            StatCard("Focus", formatMinutes(s.focusMinutes), Modifier.weight(1f), sub = "${s.focusSessions} sessions")
        }

        SectionTitle("Daily completion (%)")
        Card(Modifier.fillMaxWidth()) { Box(Modifier.padding(12.dp)) { BarChart(s.completionBars, maxValue = 100) } }

        SectionTitle("Daily points")
        Card(Modifier.fillMaxWidth()) {
            Box(Modifier.padding(12.dp)) { BarChart(s.pointsBars, barColor = MaterialTheme.colorScheme.secondary) }
        }

        SectionTitle("Last 5 weeks")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                s.heat.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { cell ->
                            val p = cell.percent
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (p == null) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f + 0.8f * p / 100f),
                                    ),
                            )
                        }
                    }
                }
                Text(
                    "${s.heat.firstOrNull()?.date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""} → today · darker = more tasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Time by category (completed)")
        if (s.categoryMinutes.isEmpty()) {
            Text("No completed tasks in this range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val max = s.categoryMinutes.maxOf { it.second }.coerceAtLeast(1)
            s.categoryMinutes.forEach { (cat, mins) ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat.label)
                        Text(formatMinutes(mins))
                    }
                    ProgressBar(mins.toFloat() / max, color = Color(cat.colorHex))
                }
            }
        }

        SectionTitle("Achievements")
        s.achievements.forEach { a ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(a.def.title, fontWeight = FontWeight.SemiBold)
                        Text(a.def.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (a.unlockedAt != null) "Unlocked" else "+${a.def.coins} coins",
                        color = if (a.unlockedAt != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
