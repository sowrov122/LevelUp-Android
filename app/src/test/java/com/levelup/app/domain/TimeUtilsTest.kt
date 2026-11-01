package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class TimeUtilsTest {
    private val dhaka = ZoneId.of("Asia/Dhaka")

    @Test fun durationCrossesMidnight() {
        val start = 23 * 60
        val end = 0
        val off = TimeUtils.resolveEndDayOffset(start, end)
        assertEquals(1, off)
        assertEquals(60, TimeUtils.durationMinutes(start, end, off))
        assertEquals(30, TimeUtils.durationMinutes(7 * 60 + 30, 8 * 60, TimeUtils.resolveEndDayOffset(450, 480)))
        assertEquals(120, TimeUtils.durationMinutes(23 * 60, 60, 1))
    }

    @Test fun parseAndFormat() {
        assertEquals(450, TimeUtils.parseHhMm("07:30"))
        assertEquals(450, TimeUtils.parseHhMm("7:30"))
        assertNull(TimeUtils.parseHhMm("24:00"))
        assertNull(TimeUtils.parseHhMm("12:60"))
        assertNull(TimeUtils.parseHhMm("abc"))
        assertEquals("00:00", TimeUtils.format(1440))
        assertEquals("07:30", TimeUtils.format(450))
    }

    @Test fun weekdayMask() {
        val monday = LocalDate.of(2026, 3, 2)
        assertTrue(TimeUtils.isActiveOn(TimeUtils.WEEKDAYS, monday))
        assertFalse(TimeUtils.isActiveOn(TimeUtils.WEEKEND, monday))
        assertTrue(TimeUtils.isActiveOn(TimeUtils.WEEKEND, monday.plusDays(5)))
    }

    @Test fun overnightEndInstantIsNextDay() {
        val d = LocalDate.of(2026, 3, 2)
        val end = TimeUtils.toInstant(d, 23 * 60 + 60, dhaka)
        assertEquals(TimeUtils.toInstant(d.plusDays(1), 0, dhaka), end)
    }

    @Test fun nextReminder() {
        val now = Instant.parse("2026-03-02T00:00:00Z") // 06:00 in Dhaka (UTC+6), a Monday
        val next = TimeUtils.nextReminderInstant(7 * 60 + 30, TimeUtils.WEEKDAYS, 10, now, dhaka)
        assertNotNull(next)
        // 07:20 Dhaka == 01:20Z
        assertEquals(Instant.parse("2026-03-02T01:20:00Z"), next)
        // After the trigger the next one is the following weekday
        val after = TimeUtils.nextReminderInstant(7 * 60 + 30, TimeUtils.WEEKDAYS, 10, next!!, dhaka)
        assertEquals(Instant.parse("2026-03-03T01:20:00Z"), after)
        // Friday evening -> Monday
        val friday = Instant.parse("2026-03-06T10:00:00Z")
        assertEquals(Instant.parse("2026-03-09T01:20:00Z"), TimeUtils.nextReminderInstant(450, TimeUtils.WEEKDAYS, 10, friday, dhaka))
        assertNull(TimeUtils.nextReminderInstant(450, 0, 10, now, ZoneOffset.UTC))
    }

    @Test fun quietHours() {
        assertTrue(TimeUtils.isInQuietHours(23 * 60, 22 * 60, 6 * 60))
        assertTrue(TimeUtils.isInQuietHours(5 * 60, 22 * 60, 6 * 60))
        assertFalse(TimeUtils.isInQuietHours(12 * 60, 22 * 60, 6 * 60))
        assertTrue(TimeUtils.isInQuietHours(13 * 60, 12 * 60, 14 * 60))
        assertFalse(TimeUtils.isInQuietHours(10, 600, 600))
    }

    @Test fun conflicts() {
        val a = TaskSlot(10 * 60, 120, TimeUtils.WEEKDAYS)
        val b = TaskSlot(11 * 60, 60, TimeUtils.WEEKDAYS)
        val c = TaskSlot(12 * 60, 60, TimeUtils.WEEKDAYS)
        val weekendB = TaskSlot(11 * 60, 60, TimeUtils.WEEKEND)
        assertTrue(TimeUtils.conflicts(a, b))
        assertFalse(TimeUtils.conflicts(a, c))        // touching ends do not overlap
        assertFalse(TimeUtils.conflicts(a, weekendB)) // different days
        // overnight task Monday 23:00-01:00 overlaps Tuesday 00:30
        val night = TaskSlot(23 * 60, 120, 0b0000001)
        val early = TaskSlot(30, 30, 0b0000010)
        assertTrue(TimeUtils.conflicts(night, early))
        assertTrue(TimeUtils.conflicts(early, night))
    }

    @Test fun validation() {
        assertNull(TimeUtils.validate(TaskDraft(title = "x")))
        assertEquals("Please enter a title.", TimeUtils.validate(TaskDraft(title = " ")))
        assertEquals("Start and end times must differ.", TimeUtils.validate(TaskDraft(title = "x", startMinute = 60, endMinute = 60)))
        assertEquals("Choose at least one day.", TimeUtils.validate(TaskDraft(title = "x", repeatDaysMask = 0)))
    }
}

class TimeUtilsDstTest {
    private val newYork = ZoneId.of("America/New_York")

    @Test fun springForwardGapIsResolvedForwards() {
        // 02:30 does not exist on 2026-03-08 in New York; java.time moves it to 03:30 EDT (07:30Z).
        assertEquals(
            Instant.parse("2026-03-08T07:30:00Z"),
            TimeUtils.toInstant(LocalDate.of(2026, 3, 8), 2 * 60 + 30, newYork),
        )
    }

    @Test fun fallBackOverlapUsesEarlierOffset() {
        // 01:30 happens twice on 2026-11-01; the first occurrence is EDT (05:30Z).
        assertEquals(
            Instant.parse("2026-11-01T05:30:00Z"),
            TimeUtils.toInstant(LocalDate.of(2026, 11, 1), 90, newYork),
        )
    }

    @Test fun midnightBoundaryBelongsToTheNextLocalDate() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val justBefore = Instant.parse("2026-03-02T17:59:59Z") // 23:59:59 on 2 March in Dhaka
        val atMidnight = Instant.parse("2026-03-02T18:00:00Z") // 00:00 on 3 March
        assertEquals(LocalDate.of(2026, 3, 2), LocalDate.ofInstant(justBefore, dhaka))
        assertEquals(LocalDate.of(2026, 3, 3), LocalDate.ofInstant(atMidnight, dhaka))
        assertEquals(atMidnight, TimeUtils.toInstant(LocalDate.of(2026, 3, 2), 24 * 60, dhaka))
    }
}
