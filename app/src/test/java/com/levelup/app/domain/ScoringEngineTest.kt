package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScoringEngineTest {
    @Test fun taskRewardBoundaries() {
        assertEquals(Reward.ZERO, ScoringEngine.calculateTaskReward(0))
        assertEquals(5, ScoringEngine.calculateTaskReward(15).xp)
        assertEquals(5, ScoringEngine.calculateTaskReward(30).xp)
        assertEquals(10, ScoringEngine.calculateTaskReward(31).xp)
        assertEquals(10, ScoringEngine.calculateTaskReward(60).xp)
        assertEquals(20, ScoringEngine.calculateTaskReward(61).xp)
        assertEquals(20, ScoringEngine.calculateTaskReward(120).xp)
        assertEquals(25, ScoringEngine.calculateTaskReward(121).xp)
        assertEquals(25, ScoringEngine.calculateTaskReward(600).points)
    }

    @Test fun levelBoundaries() {
        assertEquals(1, ScoringEngine.calculateLevelFromXp(0).level)
        assertEquals(1, ScoringEngine.calculateLevelFromXp(124).level)   // level 1 needs 125
        val l2 = ScoringEngine.calculateLevelFromXp(125)
        assertEquals(2, l2.level)
        assertEquals(0L, l2.xpIntoLevel)
        assertEquals(150L, l2.xpForNextLevel)
        assertEquals(2, ScoringEngine.calculateLevelFromXp(274).level)
        assertEquals(3, ScoringEngine.calculateLevelFromXp(275).level)
        assertEquals(1, ScoringEngine.calculateLevelFromXp(-50).level)
    }

    @Test fun levelHandlesHugeXpWithoutOverflow() {
        val p = ScoringEngine.calculateLevelFromXp(Long.MAX_VALUE)
        assertTrue(p.level in 2..ScoringEngine.MAX_LEVEL)
        assertTrue(p.xpIntoLevel >= 0)
    }

    @Test fun reconcileGivesExactlyOneAwardWhenToggling() {
        val reward = Reward(5, 5, 0)
        val desiredDone = mapOf("task:a" to reward)
        // complete
        var deltas = ScoringEngine.reconcile(desiredDone, emptyMap())
        assertEquals(listOf("task:a" to reward), deltas)
        // already awarded -> nothing more
        assertTrue(ScoringEngine.reconcile(desiredDone, mapOf("task:a" to reward)).isEmpty())
        // undo
        deltas = ScoringEngine.reconcile(emptyMap(), mapOf("task:a" to reward))
        assertEquals(Reward(-5, -5, 0), deltas.single().second)
        // redo after undo: net is zero again, so award the full reward once more
        deltas = ScoringEngine.reconcile(desiredDone, mapOf("task:a" to Reward.ZERO))
        assertEquals(reward, deltas.single().second)
    }

    @Test fun dailyBonuses() {
        val tasks = listOf(DayTaskFlag(true, true), DayTaskFlag(false, false))
        assertEquals(setOf("priority:2026-01-01"), ScoringEngine.calculateDailyBonuses("2026-01-01", tasks).keys)
        val all = listOf(DayTaskFlag(true, true), DayTaskFlag(false, true))
        assertEquals(setOf("priority:d", "perfect:d"), ScoringEngine.calculateDailyBonuses("d", all).keys)
        assertTrue(ScoringEngine.calculateDailyBonuses("d", emptyList()).isEmpty())
    }

    @Test fun focusRewardCapsAndMinimums() {
        assertEquals(Reward.ZERO, ScoringEngine.calculateFocusReward(599, 120, 0).reward)      // under 10 minutes
        assertEquals(Reward(2, 10, 1), ScoringEngine.calculateFocusReward(600, 120, 0).reward)   // exactly 10 minutes
        val r = ScoringEngine.calculateFocusReward(25 * 60L, 120, 0)
        assertEquals(Reward(points = 5, xp = 25, coins = 2), r.reward)
        val capped = ScoringEngine.calculateFocusReward(60 * 60L, 120, 100)
        assertEquals(20, capped.rewardedMinutes)
        assertEquals(Reward.ZERO, ScoringEngine.calculateFocusReward(30 * 60L, 120, 120).reward)
    }

    @Test fun dailyPointsFloorAtZero() {
        assertEquals(0, ScoringEngine.dailyPointsBalance(-12))
        assertEquals(7, ScoringEngine.dailyPointsBalance(7))
    }

    @Test fun streaks() {
        val d = LocalDate.of(2026, 3, 10)
        fun day(offset: Long, done: Int, total: Int = 10) = DaySummary(d.minusDays(offset), total, done)
        val days = listOf(day(0, 7), day(1, 8), day(2, 10), day(3, 6), day(4, 9), day(5, 9))
        val s = ScoringEngine.calculateStreaks(days, d)
        assertEquals(3, s.current)   // today, -1, -2 qualify; -3 is 60%
        assertEquals(3, s.longest)
        // today not yet qualifying: streak counts from yesterday
        val s2 = ScoringEngine.calculateStreaks(listOf(day(0, 1), day(1, 8), day(2, 8)), d)
        assertEquals(2, s2.current)
        assertEquals(Streaks(0, 0), ScoringEngine.calculateStreaks(emptyList(), d))
    }

    @Test fun achievementsRules() {
        assertTrue(Achievements.unlocked(LifetimeStats()).isEmpty())
        val ids = Achievements.unlocked(LifetimeStats(completedTasks = 1, focusSessions = 1, longestStreak = 5, level = 5))
        assertTrue(ids.containsAll(setOf("first_task", "first_focus", "streak_5", "level_5")))
        assertTrue("focus_7h" !in ids)
    }

    @Test fun observatoryMilestonesFollowSavedTotals() {
        assertTrue(Observatory.unlockedIds(0, 0).isEmpty())
        assertEquals(setOf("dome"), Observatory.unlockedIds(1, 10))
        assertTrue("planet" in Observatory.unlockedIds(0, 120))
        assertEquals(80, Observatory.starCount(1000, 100000))
    }
}
