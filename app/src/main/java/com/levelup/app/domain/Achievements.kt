package com.levelup.app.domain

data class AchievementDef(val id: String, val title: String, val description: String, val coins: Int)

data class LifetimeStats(
    val completedTasks: Int = 0,
    val perfectDays: Int = 0,
    val focusSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val maxFocusSessionsInDay: Int = 0,
    val longestStreak: Int = 0,
    val level: Int = 1,
    val studyMinutes: Int = 0,
)

/** Explicit, visible unlock rules. Each rule only reads persisted data. */
object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_task", "First Step", "Complete your first task.", 10),
        AchievementDef("first_focus", "First Focus Session", "Complete one focus session of 5+ minutes.", 15),
        AchievementDef("focus_x3_day", "Triple Focus", "Complete three focus sessions in one day.", 25),
        AchievementDef("streak_5", "Five-Day Routine Streak", "Keep a 5-day streak (70%+ tasks done each day).", 40),
        AchievementDef("focus_7h", "Seven Hours of Focus", "Reach 420 total focus minutes.", 50),
        AchievementDef("study_20h", "Academic Study Milestone", "Complete 20 hours of study blocks.", 50),
        AchievementDef("routine_champion", "Routine Completion Champion", "Finish every task on 3 different days.", 40),
        AchievementDef("level_5", "Level 5", "Reach level 5.", 30),
    )

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }

    fun unlocked(s: LifetimeStats): Set<String> = buildSet {
        if (s.completedTasks >= 1) add("first_task")
        if (s.focusSessions >= 1) add("first_focus")
        if (s.maxFocusSessionsInDay >= 3) add("focus_x3_day")
        if (s.longestStreak >= 5) add("streak_5")
        if (s.totalFocusMinutes >= 420) add("focus_7h")
        if (s.studyMinutes >= 20 * 60) add("study_20h")
        if (s.perfectDays >= 3) add("routine_champion")
        if (s.level >= 5) add("level_5")
    }
}
