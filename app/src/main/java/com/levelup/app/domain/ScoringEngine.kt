package com.levelup.app.domain

import java.time.LocalDate

/**
 * Pure, deterministic scoring rules. No Android, no clock, no floating point for points or coins.
 */
object ScoringEngine {
    const val MIN_FOCUS_SECONDS = 300L          // sessions shorter than 5 minutes earn nothing
    const val MAX_LEVEL = 500_000_000           // keeps cumulative XP inside Long
    const val STREAK_THRESHOLD_PERCENT = 70

    val PRIORITY_BONUS = Reward(points = 20, xp = 20, coins = 5)
    val PERFECT_DAY_BONUS = Reward(points = 30, xp = 30, coins = 10)

    /** 1-30 min: 5, 31-60: 10, 61-120: 20, longer: 25. Points == XP; coins come from milestones. */
    fun calculateTaskReward(durationMinutes: Int): Reward {
        if (durationMinutes <= 0) return Reward.ZERO
        val v = when {
            durationMinutes <= 30 -> 5
            durationMinutes <= 60 -> 10
            durationMinutes <= 120 -> 20
            else -> 25
        }
        return Reward(points = v, xp = v)
    }

    /** One reward bundle per refKey that should exist for a given day. */
    fun calculateDailyBonuses(dateKey: String, tasks: List<DayTaskFlag>): Map<String, Reward> {
        val out = LinkedHashMap<String, Reward>()
        val high = tasks.filter { it.isHighPriority }
        if (high.isNotEmpty() && high.all { it.isCompleted }) out["priority:$dateKey"] = PRIORITY_BONUS
        if (tasks.isNotEmpty() && tasks.all { it.isCompleted }) out["perfect:$dateKey"] = PERFECT_DAY_BONUS
        return out
    }

    /**
     * Differences between what should have been awarded and what the ledger already nets to.
     * Appending these deltas (never editing history) gives exactly one award per key, so
     * complete -> undo -> complete can never farm XP.
     */
    fun reconcile(desired: Map<String, Reward>, current: Map<String, Reward>): List<Pair<String, Reward>> {
        val keys = LinkedHashSet<String>().apply { addAll(desired.keys); addAll(current.keys) }
        return keys.mapNotNull { k ->
            val delta = (desired[k] ?: Reward.ZERO) - (current[k] ?: Reward.ZERO)
            if (delta.isZero) null else k to delta
        }
    }

    /** 1 point per 5 focused minutes, 1 XP per minute, 1 coin per 10 minutes, with a daily cap. */
    fun calculateFocusReward(focusedSeconds: Long, dailyCapMinutes: Int, alreadyRewardedMinutes: Int): FocusReward {
        if (focusedSeconds < MIN_FOCUS_SECONDS) return FocusReward(0, Reward.ZERO)
        val minutes = (focusedSeconds / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val remaining = (dailyCapMinutes - alreadyRewardedMinutes).coerceAtLeast(0)
        val eligible = minOf(minutes, remaining)
        return FocusReward(eligible, Reward(points = eligible / 5, xp = eligible, coins = eligible / 10))
    }

    // ---- Levels: XP to advance from level L = 100 + L * 25 ----

    fun xpRequiredForNextLevel(level: Int): Long = 100L + level.toLong() * 25L

    /** Total XP needed to *reach* [level] (level 1 = 0 XP). */
    fun cumulativeXpForLevel(level: Int): Long {
        val n = (level.coerceAtLeast(1) - 1).toLong()
        return 100L * n + 25L * n * (n + 1) / 2
    }

    fun calculateLevelFromXp(totalXp: Long): LevelProgress {
        val xp = totalXp.coerceAtLeast(0L)
        var lo = 1
        var hi = MAX_LEVEL
        while (lo < hi) {
            val mid = lo + (hi - lo + 1) / 2
            if (cumulativeXpForLevel(mid) <= xp) lo = mid else hi = mid - 1
        }
        return LevelProgress(lo, xp - cumulativeXpForLevel(lo), xpRequiredForNextLevel(lo), xp)
    }

    /** Daily points are floored at zero; the full ledger keeps the real signed history. */
    fun dailyPointsBalance(sumOfEvents: Int): Int = sumOfEvents.coerceAtLeast(0)

    // ---- Streaks ----

    fun dayQualifies(s: DaySummary, thresholdPercent: Int = STREAK_THRESHOLD_PERCENT): Boolean =
        s.total > 0 && s.completed * 100 >= thresholdPercent * s.total

    fun calculateStreaks(days: List<DaySummary>, today: LocalDate, thresholdPercent: Int = STREAK_THRESHOLD_PERCENT): Streaks {
        val qualifying = days.filter { dayQualifies(it, thresholdPercent) }.map { it.date }.toSortedSet()
        if (qualifying.isEmpty()) return Streaks(0, 0)
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in qualifying) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }
        var cursor = if (today in qualifying) today else today.minusDays(1)
        var current = 0
        while (cursor in qualifying) {
            current++
            cursor = cursor.minusDays(1)
        }
        return Streaks(current, longest)
    }
}
