package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelTitlesTest {
    @Test fun titlesFollowLevelRanges() {
        assertEquals("Cadet", LevelTitles.titleFor(1))
        assertEquals("Cadet", LevelTitles.titleFor(2))
        assertEquals("Navigator", LevelTitles.titleFor(3))
        assertEquals("Focus Builder", LevelTitles.titleFor(7))
        assertEquals("Discipline Builder", LevelTitles.titleFor(12))
        assertEquals("Galaxy Architect", LevelTitles.titleFor(500))
        assertEquals("Cadet", LevelTitles.titleFor(0))
    }
}
