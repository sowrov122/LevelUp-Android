package com.levelup.app.data

import com.levelup.app.domain.Category
import com.levelup.app.domain.Priority
import com.levelup.app.domain.TimeUtils

/** Editable sample routine, inserted once on first launch. Not hard-coded schedule logic. */
object SeedData {
    fun sampleTasks(now: Long): List<RoutineTaskEntity> {
        var order = 0
        fun t(
            id: String, title: String, start: String, end: String,
            category: Category, priority: Priority, mask: Int,
        ): RoutineTaskEntity {
            val s = TimeUtils.parseHhMm(start)!!
            val e = TimeUtils.parseHhMm(end)!!
            val off = TimeUtils.resolveEndDayOffset(s, e)
            return RoutineTaskEntity(
                id = id, title = title, description = null, category = category,
                startMinute = s, endMinute = e, endDayOffset = off,
                durationMinutes = TimeUtils.durationMinutes(s, e, off),
                priority = priority,
                reminderOffsetMinutes = if (priority == Priority.LOW) null else 5,
                isEnabled = true, sortOrder = order++, repeatDaysMask = mask,
                createdAt = now, updatedAt = now,
            )
        }

        val wd = TimeUtils.WEEKDAYS
        val we = TimeUtils.WEEKEND
        return listOf(
            t("seed-wd-0730", "AI Coding", "07:30", "08:00", Category.AI_CODING, Priority.NORMAL, wd),
            t("seed-wd-0800", "Walking + Garden Care", "08:00", "09:00", Category.WALKING, Priority.NORMAL, wd),
            t("seed-wd-0900", "Breakfast + Trading", "09:00", "10:00", Category.TRADING, Priority.LOW, wd),
            t("seed-wd-1000", "Academic Study", "10:00", "12:00", Category.STUDY, Priority.HIGH, wd),
            t("seed-wd-1200", "Trading + AI Coding", "12:00", "14:00", Category.TRADING, Priority.NORMAL, wd),
            t("seed-wd-1400", "Lunch + Trading", "14:00", "15:00", Category.TRADING, Priority.LOW, wd),
            t("seed-wd-1500", "Academic Study", "15:00", "17:00", Category.STUDY, Priority.HIGH, wd),
            t("seed-wd-1700", "AI Coding + Trading", "17:00", "18:00", Category.AI_CODING, Priority.NORMAL, wd),
            t("seed-wd-1800", "Workout", "18:00", "19:00", Category.FITNESS, Priority.HIGH, wd),
            t("seed-wd-1900", "Academic Study", "19:00", "22:00", Category.STUDY, Priority.HIGH, wd),
            t("seed-wd-2300", "Trading + AI Coding", "23:00", "00:00", Category.TRADING, Priority.LOW, wd),

            t("seed-we-0830", "Walking + Garden Care", "08:30", "09:30", Category.WALKING, Priority.NORMAL, we),
            t("seed-we-0930", "Slow Breakfast", "09:30", "10:30", Category.MEALS, Priority.LOW, we),
            t("seed-we-1030", "Academic Study", "10:30", "12:30", Category.STUDY, Priority.HIGH, we),
            t("seed-we-1400", "AI Side Project", "14:00", "15:30", Category.AI_CODING, Priority.NORMAL, we),
            t("seed-we-1700", "Workout", "17:00", "18:00", Category.FITNESS, Priority.HIGH, we),
            t("seed-we-1900", "Academic Study", "19:00", "20:30", Category.STUDY, Priority.NORMAL, we),
        )
    }
}
