package com.levelup.app.domain

import java.time.LocalDate

enum class Category(val label: String, val colorHex: Long) {
    AI_CODING("AI Coding", 0xFFF4A34B),
    STUDY("Academic Study", 0xFFFFCE00),
    TRADING("Trading", 0xFF53D69A),
    FITNESS("Fitness", 0xFFF2735A),
    WALKING("Walking & Garden", 0xFFA3C76D),
    MEALS("Meals & Care", 0xFFC9A27E),
}

enum class Priority(val label: String) { LOW("Low"), NORMAL("Normal"), HIGH("High") }

enum class TaskStatus { PENDING, COMPLETED, SKIPPED, MISSED }

enum class FocusStatus { RUNNING, PAUSED, COMPLETED, CANCELLED }

enum class ScoreEventType {
    TASK_COMPLETED, TASK_REVERSED, FOCUS_COMPLETED, DAILY_BONUS, ACHIEVEMENT, REWARD_PURCHASE, DISTRACTION_PENALTY, TEST_GRANT
}

enum class InterruptionAction { PENALIZED, LOGGED_ONLY, OVERRIDE }

/** Integer-only reward bundle. Points = today's behaviour score, XP = long-term, coins = spendable. */
data class Reward(val points: Int = 0, val xp: Int = 0, val coins: Int = 0) {
    operator fun plus(o: Reward) = Reward(points + o.points, xp + o.xp, coins + o.coins)
    operator fun minus(o: Reward) = Reward(points - o.points, xp - o.xp, coins - o.coins)
    val isZero: Boolean get() = points == 0 && xp == 0 && coins == 0

    companion object {
        val ZERO = Reward()
    }
}

data class LevelProgress(
    val level: Int,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
    val totalXp: Long,
) {
    /** Display-only fraction (never used for scoring). */
    val fraction: Float
        get() = if (xpForNextLevel <= 0L) 0f else (xpIntoLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

data class DaySummary(val date: LocalDate, val total: Int, val completed: Int)

data class Streaks(val current: Int, val longest: Int)

data class FocusReward(val rewardedMinutes: Int, val reward: Reward)

data class FocusOutcome(
    val completed: Boolean,
    val focusedMinutes: Long,
    val reward: Reward,
    val rewardedMinutes: Int,
) {
    val cappedByDailyLimit: Boolean get() = completed && rewardedMinutes < focusedMinutes
}

data class DayTaskFlag(val isHighPriority: Boolean, val isCompleted: Boolean)

/** Everything the editor can change on a routine task. */
data class TaskDraft(
    val id: String? = null,
    val title: String = "",
    val description: String? = null,
    val category: Category = Category.STUDY,
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 10 * 60,
    val priority: Priority = Priority.NORMAL,
    val reminderOffsetMinutes: Int? = 5,
    val isEnabled: Boolean = true,
    val repeatDaysMask: Int = TimeUtils.ALL_DAYS,
)

/** Rank names shown next to the level number. Display only; they never affect scoring. */
object LevelTitles {
    private val TITLES = listOf(
        1 to "Cadet", 3 to "Navigator", 5 to "Focus Builder", 8 to "Station Engineer",
        12 to "Discipline Builder", 18 to "Commander", 25 to "Star Captain", 35 to "Galaxy Architect",
    )

    fun titleFor(level: Int): String = TITLES.lastOrNull { level >= it.first }?.second ?: "Cadet"
}

/** Filter groups shown on the Routine screen. Every [Category] belongs to exactly one group. */
enum class CategoryGroup(val label: String) {
    ALL("All"), STUDY("Study"), WORK("Work"), HEALTH("Health"), PERSONAL("Personal"), MEAL("Meal"),
}

val Category.group: CategoryGroup
    get() = when (this) {
        Category.STUDY -> CategoryGroup.STUDY
        Category.AI_CODING, Category.TRADING -> CategoryGroup.WORK
        Category.FITNESS -> CategoryGroup.HEALTH
        Category.WALKING -> CategoryGroup.PERSONAL
        Category.MEALS -> CategoryGroup.MEAL
    }

/** Completed focus minutes and sessions on one local date. */
data class FocusDay(val minutes: Int, val sessions: Int)
