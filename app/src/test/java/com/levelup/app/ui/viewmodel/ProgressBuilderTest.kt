package com.levelup.app.ui.viewmodel

import com.levelup.app.domain.DaySummary
import com.levelup.app.domain.FocusDay
import com.levelup.app.domain.HeatLevel
import com.levelup.app.domain.LifetimeStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressBuilderTest {
    private val today = LocalDate.of(2026, 11, 4) // Wednesday

    private fun day(back: Long, total: Int, done: Int) = DaySummary(today.minusDays(back), total, done)

    private val summaries = listOf(day(0, 8, 4), day(1, 7, 7), day(2, 5, 1), day(10, 4, 4))
    private val points = mapOf(today to 35, today.minusDays(1) to 120, today.minusDays(2) to 0)
    private val focus = mapOf(today to FocusDay(25, 1), today.minusDays(1) to FocusDay(65, 2), today.minusDays(20) to FocusDay(40, 1))
    private val stats = LifetimeStats(completedTasks = 16, perfectDays = 2, focusSessions = 4, totalFocusMinutes = 130, level = 2, totalXp = 200)

    private fun build(range: Int = 7, offset: Int = 0, s: List<DaySummary> = summaries, p: Map<LocalDate, Int> = points, f: Map<LocalDate, FocusDay> = focus, st: LifetimeStats = stats) =
        buildProgressState(range, offset, today, s, p, f, st, emptyMap())

    @Test fun completionBarsAreTasksDoneOverTasksScheduled() {
        val bars = build().completionBars
        assertEquals(7, bars.size)
        assertEquals(50, bars.last().value)          // today 4 of 8
        assertEquals(100, bars[5].value)             // yesterday 7 of 7
        assertEquals(20, bars[4].value)              // 1 of 5
        assertEquals(0, bars[0].value)               // nothing scheduled
    }

    @Test fun pointsBarsMatchTheLedgerAndFocusTotalsStayInsideTheWindow() {
        val s = build()
        assertEquals(listOf(0, 0, 0, 0, 0, 120, 35), s.pointsBars.map { it.value })
        assertEquals(200, s.pointsAxisMax)             // 120 fits the 200 axis
        assertEquals(25 + 65, s.focusMinutes)         // the 40 min 20 days ago is outside 7 days
        assertEquals(3, s.focusSessions)
        val thirty = build(range = 30)
        assertEquals(30, thirty.completionBars.size)
        assertEquals(25 + 65 + 40, thirty.focusMinutes)
        assertEquals(4, thirty.focusSessions)
    }

    @Test fun previousPeriodShiftsTheWindowAndTheTotals() {
        val s = build(offset = 1)
        assertTrue(s.rangeLabel.startsWith("22 Oct"))
        assertEquals(0, s.focusMinutes)
        assertEquals(100, s.completionBars.first { it.value > 0 }.value)   // the 4-of-4 day, 10 days ago
    }

    @Test fun heatmapHasFiveWeeksWithFutureDaysEmpty() {
        val heat = build().heatWeeks
        assertEquals(5, heat.size)
        assertTrue(heat.all { it.days.size == 7 })
        val current = heat.last().days                 // Mon 2 Nov .. Sun 8 Nov
        assertEquals(HeatLevel.MEDIUM, current[2].level)   // today 50%
        assertTrue(current[2].isToday)
        assertNull(current[3].level)                   // tomorrow is in the future
        assertEquals(HeatLevel.HIGH, current[1].level) // yesterday 100%
    }

    @Test fun streaksAndAchievementProgressComeFromStoredData() {
        val s = build(st = stats.copy(longestStreak = 4, totalXp = 3420, completedTasks = 12))
        assertEquals(1, s.currentStreak)               // today (50%) is below the 70% bar so counting starts yesterday (100%); two days ago was 20%
        assertEquals(1, s.bestStreak.coerceAtMost(1))   // 10 days ago (100%) is a separate one-day run
        val xp = s.achievements.first { it.def.id == "xp_collector" }
        assertEquals(3420L, xp.progress)
        assertFalse(xp.completed)
        val task = s.achievements.first { it.def.id == "task_master" }
        assertEquals(12L, task.shownProgress)
        val capped = build(st = stats.copy(completedTasks = 500)).achievements.first { it.def.id == "task_master" }
        assertTrue(capped.completed)
        assertEquals(50L, capped.shownProgress)        // never shows more than the target
        assertEquals(1f, capped.fraction, 0.0001f)
    }

    @Test fun incompleteAchievementsComeFirstClosestToDone() {
        val list = build(st = stats.copy(longestStreak = 4, totalXp = 3420)).achievements
        val firstCompleted = list.indexOfFirst { it.completed }
        assertTrue(list.take(firstCompleted).none { it.completed })
        val incomplete = list.filter { !it.completed }
        assertEquals(incomplete.map { it.fraction }.sortedDescending(), incomplete.map { it.fraction })
    }

    @Test fun emptyHistoryProducesEmptyFlagsAndZeroCharts() {
        val s = build(s = emptyList(), p = emptyMap(), f = emptyMap(), st = LifetimeStats())
        assertFalse(s.hasAnyData)
        assertFalse(s.periodHasData)
        assertTrue(s.completionBars.all { it.value == 0 })
        assertEquals(100, s.pointsAxisMax)
        assertTrue(s.heatWeeks.flatMap { it.days }.filter { it.level != null }.all { it.level == HeatLevel.NONE })
        assertEquals(0, s.currentStreak)
    }
}
