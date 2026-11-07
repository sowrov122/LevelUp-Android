package com.levelup.app.domain

enum class AchievementUnit { COUNT, MINUTES, DAYS, XP }

data class LifetimeStats(
    val completedTasks: Int = 0,
    val perfectDays: Int = 0,
    val focusSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val maxFocusSessionsInDay: Int = 0,
    val longestStreak: Int = 0,
    val level: Int = 1,
    val studyMinutes: Int = 0,
    val totalXp: Long = 0L,
)

/** A milestone with a visible target. [progress] reads only persisted totals. */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int,
    val coins: Int,
    val target: Long,
    val unit: AchievementUnit,
    val progress: (LifetimeStats) -> Long,
)

object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_task", "First Step", "Complete your first task.", 10, 10, 1, AchievementUnit.COUNT) { it.completedTasks.toLong() },
        AchievementDef("task_master", "Task Master", "Complete 50 tasks.", 200, 20, 50, AchievementUnit.COUNT) { it.completedTasks.toLong() },
        AchievementDef("first_focus", "First Focus Session", "Complete one focus session of 10+ minutes.", 15, 15, 1, AchievementUnit.COUNT) { it.focusSessions.toLong() },
        AchievementDef("focus_x3_day", "Triple Focus", "Complete three focus sessions in one day.", 50, 25, 3, AchievementUnit.COUNT) { it.maxFocusSessionsInDay.toLong() },
        AchievementDef("focus_7h", "Seven Hours of Focus", "Accumulate 7 hours of focus time.", 150, 50, 420, AchievementUnit.MINUTES) { it.totalFocusMinutes.toLong() },
        AchievementDef("focus_hero", "Focus Hero", "Accumulate 20 hours of focus time.", 300, 30, 1200, AchievementUnit.MINUTES) { it.totalFocusMinutes.toLong() },
        AchievementDef("streak_5", "Five-Day Routine Streak", "Keep a 5-day streak (70%+ tasks done each day).", 100, 40, 5, AchievementUnit.DAYS) { it.longestStreak.toLong() },
        AchievementDef("consistency_king", "Consistency King", "Maintain a 14-day streak.", 500, 50, 14, AchievementUnit.DAYS) { it.longestStreak.toLong() },
        AchievementDef("routine_champion", "Routine Completion Champion", "Finish every task on 3 different days.", 150, 40, 3, AchievementUnit.DAYS) { it.perfectDays.toLong() },
        AchievementDef("study_20h", "Academic Study Milestone", "Complete 20 hours of study blocks.", 300, 50, 1200, AchievementUnit.MINUTES) { it.studyMinutes.toLong() },
        AchievementDef("level_5", "Level 5", "Reach level 5.", 100, 30, 5, AchievementUnit.COUNT) { it.level.toLong() },
        AchievementDef("xp_collector", "XP Collector", "Earn 5,000 total XP.", 1000, 100, 5000, AchievementUnit.XP) { it.totalXp },
    )

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }

    fun unlocked(s: LifetimeStats): Set<String> = all.filter { it.progress(s) >= it.target }.map { it.id }.toSet()
}
