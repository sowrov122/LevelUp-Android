package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    @Test fun nothingIsUnlockedWithoutProgress() {
        assertTrue(Achievements.unlocked(LifetimeStats()).isEmpty())
    }

    @Test fun eachMilestoneUnlocksExactlyAtItsTarget() {
        fun ids(s: LifetimeStats) = Achievements.unlocked(s)
        assertFalse("task_master" in ids(LifetimeStats(completedTasks = 49)))
        assertTrue("task_master" in ids(LifetimeStats(completedTasks = 50)))
        assertFalse("focus_hero" in ids(LifetimeStats(totalFocusMinutes = 1199)))
        assertTrue("focus_hero" in ids(LifetimeStats(totalFocusMinutes = 1200)))
        assertFalse("consistency_king" in ids(LifetimeStats(longestStreak = 13)))
        assertTrue("consistency_king" in ids(LifetimeStats(longestStreak = 14)))
        assertFalse("xp_collector" in ids(LifetimeStats(totalXp = 4999)))
        assertTrue("xp_collector" in ids(LifetimeStats(totalXp = 5000)))
    }

    @Test fun progressValuesComeFromTheStats() {
        val s = LifetimeStats(completedTasks = 12, totalFocusMinutes = 90, longestStreak = 4, totalXp = 3420, level = 3)
        assertEquals(12L, Achievements.byId("task_master")!!.progress(s))
        assertEquals(90L, Achievements.byId("focus_hero")!!.progress(s))
        assertEquals(4L, Achievements.byId("consistency_king")!!.progress(s))
        assertEquals(3420L, Achievements.byId("xp_collector")!!.progress(s))
    }

    @Test fun definitionsAreSane() {
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
        Achievements.all.forEach {
            assertTrue(it.target > 0 && it.xp > 0 && it.coins > 0)
        }
    }
}
