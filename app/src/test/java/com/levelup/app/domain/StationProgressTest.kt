package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationProgressTest {
    @Test fun levelsFollowSessionThresholds() {
        assertEquals(1, StationProgress.level(0))
        assertEquals(2, StationProgress.level(1))
        assertEquals(2, StationProgress.level(2))
        assertEquals(3, StationProgress.level(3))
        assertEquals(4, StationProgress.level(6))
        assertEquals(5, StationProgress.level(10))
        assertEquals(6, StationProgress.level(15))
        assertEquals(6, StationProgress.level(500))
        assertEquals(1, StationProgress.level(-4))
    }

    @Test fun progressToNextLevel() {
        assertEquals(10, StationProgress.nextThreshold(6))
        assertEquals(0.5f, StationProgress.fractionToNext(8), 0.001f)   // 8 of 6..10
        assertEquals(0f, StationProgress.fractionToNext(0), 0.001f)
        assertNull(StationProgress.nextThreshold(15))
        assertEquals(1f, StationProgress.fractionToNext(40), 0.001f)
    }

    @Test fun percentPerSessionAndStage() {
        assertEquals(25, StationProgress.percentPerSession(6))   // 4 sessions between Lv.4 and Lv.5
        assertEquals(100, StationProgress.percentPerSession(0))  // 1 session to reach Lv.2
        assertEquals(0, StationProgress.percentPerSession(15))
        assertEquals(0, StationProgress.stageIndex(0))
        assertEquals(5, StationProgress.stageIndex(99))
    }
}
