package com.levelup.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class HeatLevel { NONE, LOW, MEDIUM, HIGH }

data class PeriodWindow(val start: LocalDate, val end: LocalDate) {
    val days: List<LocalDate> get() = generateSequence(start) { d -> d.plusDays(1).takeIf { !it.isAfter(end) } }.toList()
}

/** Pure helpers behind the Progress screen so charts can be checked against the stored history. */
object ProgressMath {
    private val NICE_AXIS_MAX = listOf(100, 200, 400, 500, 1_000, 2_000, 4_000, 5_000, 10_000, 20_000, 40_000, 50_000, 100_000)

    fun completionPercent(total: Int, completed: Int): Int = if (total <= 0) 0 else (completed * 100 / total).coerceIn(0, 100)

    /** None = nothing scheduled/done, Low 1-25%, Medium 26-75%, High 76-100%. A focus session lifts an empty day to Low. */
    fun heatLevel(total: Int, completed: Int, focusMinutes: Int): HeatLevel {
        val pct = completionPercent(total, completed)
        val fromTasks = when {
            total <= 0 || pct == 0 -> HeatLevel.NONE
            pct <= 25 -> HeatLevel.LOW
            pct <= 75 -> HeatLevel.MEDIUM
            else -> HeatLevel.HIGH
        }
        return if (fromTasks == HeatLevel.NONE && focusMinutes > 0) HeatLevel.LOW else fromTasks
    }

    /** [days] consecutive days ending today (offset 0) or [offset] whole periods earlier. */
    fun window(today: LocalDate, days: Int, offset: Int): PeriodWindow {
        val end = today.minusDays(days.toLong() * offset.coerceAtLeast(0))
        return PeriodWindow(end.minusDays(days - 1L), end)
    }

    fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /** Mondays of the last five calendar weeks, oldest first; the last one is the current week. */
    fun heatMondays(today: LocalDate): List<LocalDate> = (4 downTo 0).map { mondayOf(today).minusWeeks(it.toLong()) }

    /** Smallest "round" axis maximum that fits [maxValue] (divisible by 4 so every tick is a whole number). */
    fun axisMax(maxValue: Int): Int = NICE_AXIS_MAX.firstOrNull { it >= maxValue } ?: ((maxValue / 100_000) + 1) * 100_000

    fun ticks(axisMax: Int): List<Int> = listOf(0, axisMax / 4, axisMax / 2, axisMax * 3 / 4, axisMax)
}
