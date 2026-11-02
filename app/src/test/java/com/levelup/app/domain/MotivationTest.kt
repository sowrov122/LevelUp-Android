package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotivationTest {
    @Test fun noTasksScheduled() {
        assertTrue(Motivation.message(0, 0, 5, 30).startsWith("Nothing is scheduled"))
    }

    @Test fun allDoneMentionsStreakOnlyWhenThereIsOne() {
        assertEquals("Every task done. Day 4 of your streak is secured.", Motivation.message(6, 6, 4, 0))
        assertEquals("Every task done. This is what a streak is made of.", Motivation.message(6, 6, 1, 0))
    }

    @Test fun noneDoneUsesFocusWhenAvailable() {
        assertEquals("You already focused 25 min. Now pick the first task.", Motivation.message(0, 5, 0, 25))
        assertTrue(Motivation.message(0, 5, 0, 0).startsWith("A fresh day"))
    }

    @Test fun belowAndAboveHalfwayBoundary() {
        assertTrue(Motivation.message(2, 5, 0, 0).startsWith("2 of 5 done"))       // 40%
        assertEquals("Over half done, 2 to go.", Motivation.message(3, 5, 0, 0))     // 60%
        assertTrue(Motivation.message(3, 6, 0, 0).startsWith("Over half done"))      // exactly 50%
        assertEquals("Over half done, 3 to go. Protect your 7-day streak.", Motivation.message(3, 6, 7, 0))
    }
}
