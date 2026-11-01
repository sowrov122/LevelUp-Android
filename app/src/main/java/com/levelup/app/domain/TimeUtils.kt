package com.levelup.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A task's weekly slot, used for conflict detection. */
data class TaskSlot(val startMinute: Int, val durationMinutes: Int, val repeatDaysMask: Int)

object TimeUtils {
    const val MINUTES_PER_DAY = 1440
    const val WEEKDAYS = 0b0011111      // Mon..Fri
    const val WEEKEND = 0b1100000       // Sat, Sun
    const val ALL_DAYS = 0b1111111

    fun zoneOf(id: String): ZoneId =
        if (id.isBlank()) ZoneId.systemDefault() else runCatching { ZoneId.of(id) }.getOrElse { ZoneId.systemDefault() }

    /** 0 when the task ends the same day, 1 when it ends after midnight (end <= start). */
    fun resolveEndDayOffset(startMinute: Int, endMinute: Int): Int = if (endMinute <= startMinute) 1 else 0

    fun durationMinutes(startMinute: Int, endMinute: Int, endDayOffset: Int): Int =
        endDayOffset * MINUTES_PER_DAY + endMinute - startMinute

    fun format(minuteOfDay: Int): String {
        val m = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return "%02d:%02d".format(m / 60, m % 60)
    }

    /** Parses "H:MM" / "HH:MM" into minutes since midnight, or null when invalid. */
    fun parseHhMm(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun isActiveOn(mask: Int, date: LocalDate): Boolean = (mask and dayBit(date.dayOfWeek)) != 0

    fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    /**
     * Wall-clock minute -> instant for a date. Minutes >= 1440 roll into the next day.
     * Built from local date-time so DST gaps/overlaps are resolved by java.time rules.
     */
    fun toInstant(date: LocalDate, minuteOfDay: Int, zone: ZoneId): Instant {
        val d = date.plusDays((minuteOfDay / MINUTES_PER_DAY).toLong())
        val m = minuteOfDay % MINUTES_PER_DAY
        return d.atTime(m / 60, m % 60).atZone(zone).toInstant()
    }

    /** Next reminder trigger strictly after [now], or null when the task never repeats. */
    fun nextReminderInstant(
        startMinute: Int,
        repeatDaysMask: Int,
        offsetMinutes: Int,
        now: Instant,
        zone: ZoneId,
    ): Instant? {
        if (repeatDaysMask and ALL_DAYS == 0) return null
        val today = LocalDate.ofInstant(now, zone)
        for (i in -1..8) {
            val date = today.plusDays(i.toLong())
            if (!isActiveOn(repeatDaysMask, date)) continue
            val trigger = toInstant(date, startMinute, zone).minusSeconds(offsetMinutes * 60L)
            if (trigger.isAfter(now)) return trigger
        }
        return null
    }

    fun isInQuietHours(nowMinute: Int, startMinute: Int, endMinute: Int): Boolean {
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) nowMinute in startMinute until endMinute
        else nowMinute >= startMinute || nowMinute < endMinute
    }

    /** True when two weekly slots overlap, including overnight tasks spilling into the next day. */
    fun conflicts(a: TaskSlot, b: TaskSlot): Boolean {
        for (da in 0..6) {
            if (a.repeatDaysMask and (1 shl da) == 0) continue
            for (db in 0..6) {
                if (b.repeatDaysMask and (1 shl db) == 0) continue
                val shiftDays = ((db - da) % 7 + 7) % 7
                val shift = when (shiftDays) {
                    0 -> 0
                    1 -> 1
                    6 -> -1
                    else -> continue
                } * MINUTES_PER_DAY
                val bStart = b.startMinute + shift
                val bEnd = bStart + b.durationMinutes
                val aEnd = a.startMinute + a.durationMinutes
                if (a.startMinute < bEnd && bStart < aEnd) return true
            }
        }
        return false
    }

    fun validate(draft: TaskDraft): String? = when {
        draft.title.isBlank() -> "Please enter a title."
        draft.startMinute == draft.endMinute -> "Start and end times must differ."
        draft.repeatDaysMask and ALL_DAYS == 0 -> "Choose at least one day."
        draft.reminderOffsetMinutes != null && draft.reminderOffsetMinutes < 0 -> "Reminder offset cannot be negative."
        draft.reminderOffsetMinutes != null && draft.reminderOffsetMinutes > 24 * 60 -> "Reminder offset is too large."
        else -> null
    }
}
