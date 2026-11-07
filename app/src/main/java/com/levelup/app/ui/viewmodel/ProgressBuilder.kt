package com.levelup.app.ui.viewmodel

import com.levelup.app.domain.AchievementDef
import com.levelup.app.domain.Achievements
import com.levelup.app.domain.DaySummary
import com.levelup.app.domain.FocusDay
import com.levelup.app.domain.HeatLevel
import com.levelup.app.domain.LifetimeStats
import com.levelup.app.domain.ProgressMath
import com.levelup.app.domain.ScoringEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class ChartBar(val label: String, val sublabel: String, val value: Int)
data class HeatCell(val date: LocalDate, val level: HeatLevel?, val isToday: Boolean)
data class HeatWeek(val label: String, val days: List<HeatCell>)
data class PeriodOption(val offset: Int, val label: String)

data class AchievementUi(val def: AchievementDef, val progress: Long, val unlockedAt: Long?) {
    val completed: Boolean get() = progress >= def.target
    val fraction: Float get() = (progress.toFloat() / def.target).coerceIn(0f, 1f)
    val shownProgress: Long get() = progress.coerceAtMost(def.target)
}

data class ProgressUiState(
    val rangeDays: Int = 7,
    val offset: Int = 0,
    val rangeLabel: String = "",
    val periods: List<PeriodOption> = emptyList(),
    val completionBars: List<ChartBar> = emptyList(),
    val pointsBars: List<ChartBar> = emptyList(),
    val pointsAxisMax: Int = 100,
    val heatWeeks: List<HeatWeek> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val focusMinutes: Int = 0,
    val focusSessions: Int = 0,
    val achievements: List<AchievementUi> = emptyList(),
    val hasAnyData: Boolean = false,
    val periodHasData: Boolean = false,
    val loading: Boolean = true,
)

private val RANGE_FORMAT = DateTimeFormatter.ofPattern("dd MMM")
private val RANGE_FORMAT_YEAR = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val SUB_LABEL = DateTimeFormatter.ofPattern("dd MMM")

private fun rangeLabel(start: LocalDate, end: LocalDate): String =
    "${start.format(RANGE_FORMAT)} – ${end.format(RANGE_FORMAT_YEAR)}"

/**
 * Builds the whole Progress screen from stored data only. Pure: the same inputs always give the same screen,
 * and every number can be traced to task history (summaries), the reward ledger (points, XP) or focus sessions.
 */
fun buildProgressState(
    rangeDays: Int,
    offset: Int,
    today: LocalDate,
    summaries: List<DaySummary>,
    pointsByDay: Map<LocalDate, Int>,
    focusByDay: Map<LocalDate, FocusDay>,
    stats: LifetimeStats,
    unlockedAt: Map<String, Long>,
): ProgressUiState {
    val window = ProgressMath.window(today, rangeDays, offset)
    val days = window.days
    val byDate = summaries.associateBy { it.date }
    val compact = rangeDays <= 7

    fun labelFor(index: Int, date: LocalDate) = when {
        compact -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        index % 5 == 0 -> date.dayOfMonth.toString()
        else -> ""
    }

    val completionBars = days.mapIndexed { i, d ->
        val s = byDate[d]
        ChartBar(labelFor(i, d), if (compact) d.format(SUB_LABEL) else "", ProgressMath.completionPercent(s?.total ?: 0, s?.completed ?: 0))
    }
    val pointsBars = days.mapIndexed { i, d ->
        ChartBar(labelFor(i, d), if (compact) d.format(SUB_LABEL) else "", pointsByDay[d] ?: 0)
    }

    val heatWeeks = ProgressMath.heatMondays(today).map { monday ->
        val sunday = monday.plusDays(6)
        val label = if (monday.month == sunday.month) "${monday.dayOfMonth} – ${sunday.format(RANGE_FORMAT)}"
        else "${monday.format(RANGE_FORMAT)} – ${sunday.format(RANGE_FORMAT)}"
        HeatWeek(
            label,
            (0L..6L).map { plus ->
                val d = monday.plusDays(plus)
                val s = byDate[d]
                HeatCell(
                    d,
                    if (d.isAfter(today)) null else ProgressMath.heatLevel(s?.total ?: 0, s?.completed ?: 0, focusByDay[d]?.minutes ?: 0),
                    d == today,
                )
            },
        )
    }

    val streaks = ScoringEngine.calculateStreaks(summaries, today)
    val achievements = Achievements.all
        .map { AchievementUi(it, it.progress(stats), unlockedAt[it.id]) }
        .sortedWith(compareBy<AchievementUi> { it.completed }.thenByDescending { it.fraction })

    return ProgressUiState(
        rangeDays = rangeDays,
        offset = offset,
        rangeLabel = rangeLabel(window.start, window.end),
        periods = (0..3).map { o -> ProgressMath.window(today, rangeDays, o).let { PeriodOption(o, rangeLabel(it.start, it.end)) } },
        completionBars = completionBars,
        pointsBars = pointsBars,
        pointsAxisMax = ProgressMath.axisMax(pointsBars.maxOfOrNull { it.value } ?: 0),
        heatWeeks = heatWeeks,
        currentStreak = streaks.current,
        bestStreak = streaks.longest,
        focusMinutes = days.sumOf { focusByDay[it]?.minutes ?: 0 },
        focusSessions = days.sumOf { focusByDay[it]?.sessions ?: 0 },
        achievements = achievements,
        hasAnyData = stats.completedTasks > 0 || stats.focusSessions > 0,
        periodHasData = completionBars.any { it.value > 0 } || pointsBars.any { it.value > 0 } || days.any { (focusByDay[it]?.sessions ?: 0) > 0 },
        loading = false,
    )
}
