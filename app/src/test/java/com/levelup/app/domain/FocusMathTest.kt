package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusMathTest {
    @Test fun pausedSessionIgnoresClocks() {
        assertEquals(90L, FocusMath.elapsedSeconds(FocusStatus.PAUSED, 90, -1, 0, 5_000_000, 9_000_000))
    }

    @Test fun runningSessionAddsMonotonicTime() {
        // resumed at uptime 10s, now uptime 70s -> 60s more
        assertEquals(
            160L,
            FocusMath.elapsedSeconds(FocusStatus.RUNNING, 100, 10_000, 1_000_000, 70_000, 1_060_000),
        )
    }

    @Test fun rebootIsDetectedAndNotCounted() {
        // uptime went backwards
        assertTrue(FocusMath.rebootSuspected(500_000, 1_000_000, 20_000, 2_000_000))
        assertEquals(100L, FocusMath.elapsedSeconds(FocusStatus.RUNNING, 100, 500_000, 1_000_000, 20_000, 2_000_000))
        // wall clock advanced an hour while uptime advanced one minute: device was off
        assertTrue(FocusMath.rebootSuspected(10_000, 0, 70_000, 3_600_000))
    }

    @Test fun smallClockDriftIsTolerated() {
        assertFalse(FocusMath.rebootSuspected(10_000, 0, 70_000, 61_000))
    }
}
