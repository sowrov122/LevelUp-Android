package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationProgressTest {
    @Test fun levelsFollowFocusedMinutes() {
        assertEquals(1, StationProgress.level(0))
        assertEquals(1, StationProgress.level(4))
        assertEquals(2, StationProgress.level(5))
        assertEquals(3, StationProgress.level(10))     // a first 10-minute session reaches level 3
        assertEquals(3, StationProgress.level(39))
        assertEquals(4, StationProgress.level(40))     // next 30 minutes
        assertEquals(5, StationProgress.level(100))    // next 60 minutes
        assertEquals(6, StationProgress.level(160))    // next 60 minutes: last level
        assertEquals(6, StationProgress.level(5000))
        assertEquals(1, StationProgress.level(-3))
    }

    @Test fun progressToNextLevel() {
        assertEquals(40, StationProgress.nextThreshold(10))
        assertEquals(0.5f, StationProgress.fractionToNext(25), 0.001f)   // halfway from 10 to 40
        assertEquals(0f, StationProgress.fractionToNext(0), 0.001f)
        assertNull(StationProgress.nextThreshold(160))
        assertEquals(1f, StationProgress.fractionToNext(500), 0.001f)
    }

    @Test fun percentAddedBySession() {
        assertEquals(33, StationProgress.percentForSession(10, 10))   // 10 of the 30 minutes to level 4
        assertEquals(100, StationProgress.percentForSession(10, 60))  // capped
        assertEquals(0, StationProgress.percentForSession(200, 25))   // top level
    }

    @Test fun nextSessionStartsFromTheEarnedLevel() {
        // brand-new user: starts at level 1 and a 10-minute session builds up to level 3
        assertEquals(0, StationProgress.runningStageIndex(0, 0))
        assertEquals(2, StationProgress.runningStageIndex(0, 10 * 60))
        // after one completed 10-minute session the next one starts at level 3 (stage index 2)
        assertEquals(2, StationProgress.runningStageIndex(10, 0))
        assertEquals(3, StationProgress.runningStageIndex(10, 30 * 60))   // 40 minutes in total
        assertEquals(5, StationProgress.runningStageIndex(100, 60 * 60))  // 160 minutes in total
    }
}
