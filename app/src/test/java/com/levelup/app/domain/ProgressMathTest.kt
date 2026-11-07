package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgressMathTest {
    private val today = LocalDate.of(2026, 11, 4) // a Wednesday

    @Test fun completionPercentRoundsDownAndHandlesEmptyDays() {
        assertEquals(0, ProgressMath.completionPercent(0, 0))
        assertEquals(57, ProgressMath.completionPercent(7, 4))
        assertEquals(100, ProgressMath.completionPercent(8, 8))
    }

    @Test fun heatLevelsMatchTheLegend() {
        assertEquals(HeatLevel.NONE, ProgressMath.heatLevel(0, 0, 0))
        assertEquals(HeatLevel.NONE, ProgressMath.heatLevel(8, 0, 0))
        assertEquals(HeatLevel.LOW, ProgressMath.heatLevel(8, 2, 0))      // 25%
        assertEquals(HeatLevel.MEDIUM, ProgressMath.heatLevel(8, 3, 0))   // 37%
        assertEquals(HeatLevel.MEDIUM, ProgressMath.heatLevel(4, 3, 0))   // 75%
        assertEquals(HeatLevel.HIGH, ProgressMath.heatLevel(8, 7, 0))     // 87%
        assertEquals(HeatLevel.HIGH, ProgressMath.heatLevel(3, 3, 0))
        assertEquals(HeatLevel.LOW, ProgressMath.heatLevel(0, 0, 25))     // focus only
        assertEquals(HeatLevel.MEDIUM, ProgressMath.heatLevel(8, 3, 25))  // tasks decide when present
    }

    @Test fun windowsAreConsecutiveAndShiftByWholePeriods() {
        val w7 = ProgressMath.window(today, 7, 0)
        assertEquals(LocalDate.of(2026, 10, 29), w7.start)
        assertEquals(today, w7.end)
        assertEquals(7, w7.days.size)
        val previous = ProgressMath.window(today, 7, 1)
        assertEquals(LocalDate.of(2026, 10, 28), previous.end)
        assertEquals(7, previous.days.size)
        assertEquals(30, ProgressMath.window(today, 30, 0).days.size)
        assertEquals(ProgressMath.window(today, 7, 0).days, ProgressMath.window(today, 7, -5).days)
    }

    @Test fun heatmapCoversFiveMondayStartedWeeks() {
        val mondays = ProgressMath.heatMondays(today)
        assertEquals(5, mondays.size)
        assertTrue(mondays.all { it.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(LocalDate.of(2026, 11, 2), mondays.last())
        assertEquals(LocalDate.of(2026, 10, 5), mondays.first())
    }

    @Test fun axisMaxIsRoundAndTicksAreWholeNumbers() {
        assertEquals(100, ProgressMath.axisMax(0))
        assertEquals(100, ProgressMath.axisMax(100))
        assertEquals(200, ProgressMath.axisMax(101))
        assertEquals(500, ProgressMath.axisMax(420))
        assertEquals(1_000, ProgressMath.axisMax(501))
        assertEquals(listOf(0, 125, 250, 375, 500), ProgressMath.ticks(500))
        assertEquals(listOf(0, 25, 50, 75, 100), ProgressMath.ticks(100))
    }
}
