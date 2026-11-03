package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineDomainTest {
    @Test fun twelveHourDisplay() {
        assertEquals("12:00 AM", TimeUtils.formatForDisplay(0, false))
        assertEquals("06:00 AM", TimeUtils.formatForDisplay(6 * 60, false))
        assertEquals("12:00 PM", TimeUtils.formatForDisplay(12 * 60, false))
        assertEquals("01:05 PM", TimeUtils.formatForDisplay(13 * 60 + 5, false))
        assertEquals("11:59 PM", TimeUtils.formatForDisplay(23 * 60 + 59, false))
    }

    @Test fun twentyFourHourDisplayAndWrapAroundMidnight() {
        assertEquals("13:05", TimeUtils.formatForDisplay(13 * 60 + 5, true))
        assertEquals("00:00", TimeUtils.formatForDisplay(24 * 60, true))      // a 23:00 task ending at midnight
        assertEquals("12:30 AM", TimeUtils.formatForDisplay(24 * 60 + 30, false))
    }

    @Test fun everyCategoryBelongsToOneFilterGroup() {
        Category.entries.forEach { assertTrue(it.group != CategoryGroup.ALL) }
        assertEquals(CategoryGroup.WORK, Category.AI_CODING.group)
        assertEquals(CategoryGroup.WORK, Category.TRADING.group)
        assertEquals(CategoryGroup.STUDY, Category.STUDY.group)
        assertEquals(CategoryGroup.HEALTH, Category.FITNESS.group)
        assertEquals(CategoryGroup.PERSONAL, Category.WALKING.group)
        assertEquals(CategoryGroup.MEAL, Category.MEALS.group)
    }

    @Test fun filterLabelsAreUnique() {
        assertEquals(CategoryGroup.entries.size, CategoryGroup.entries.map { it.label }.toSet().size)
    }
}
