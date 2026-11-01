package com.levelup.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.levelup.app.domain.Category
import com.levelup.app.domain.FocusStatus
import com.levelup.app.domain.Priority
import com.levelup.app.domain.ScoreEventType
import com.levelup.app.domain.TaskStatus

/** A recurring routine task. repeatDaysMask: bit 0 = Monday ... bit 6 = Sunday. */
@Entity(tableName = "routine_tasks")
data class RoutineTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val category: Category,
    val startMinute: Int,
    val endMinute: Int,
    val endDayOffset: Int,
    val durationMinutes: Int,
    val priority: Priority,
    val reminderOffsetMinutes: Int?,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val repeatDaysMask: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A dated occurrence of a routine task. Title/time fields are snapshots, so history survives
 * later edits or deletion of the routine task (intentionally no foreign key).
 */
@Entity(
    tableName = "task_instances",
    indices = [Index(value = ["taskId", "localDate"], unique = true), Index("localDate")],
)
data class TaskInstanceEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val localDate: String,
    val title: String,
    val category: Category,
    val priority: Priority,
    val startMinute: Int,
    val durationMinutes: Int,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val status: TaskStatus,
    val completedAt: Long?,
    val pointsAwarded: Int,
    val xpAwarded: Int,
)

/** Append-only audit ledger. Rows are never updated or deleted (except by Clear Data). */
@Entity(
    tableName = "score_events",
    indices = [Index(value = ["idempotencyKey"], unique = true), Index("localDate"), Index("referenceId")],
)
data class ScoreEventEntity(
    @PrimaryKey val id: String,
    val eventType: ScoreEventType,
    val referenceId: String,
    val localDate: String,
    val pointsDelta: Int,
    val xpDelta: Int,
    val coinsDelta: Int,
    val occurredAt: Long,
    val idempotencyKey: String,
    val note: String?,
)

@Entity(tableName = "focus_sessions", indices = [Index("localDate"), Index("status")])
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: Category?,
    val plannedSeconds: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val accumulatedSeconds: Long,
    val status: FocusStatus,
    /** SystemClock.elapsedRealtime() at last resume; -1 while paused/finished. */
    val resumedAtElapsed: Long,
    /** Wall clock at last resume, used only to detect reboots. */
    val resumedAtWall: Long,
    val rewardedMinutes: Int,
    val pointsAwarded: Int,
    val xpAwarded: Int,
    val coinsAwarded: Int,
    val interruptionCount: Int,
    val localDate: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

@Entity(tableName = "owned_rewards")
data class OwnedRewardEntity(
    @PrimaryKey val rewardItemId: String,
    val acquiredAt: Long,
    val source: String,
)

// ---- Query result holders ----
data class KeyValue(val k: String, val v: Int)
data class DaySummaryRow(val localDate: String, val total: Int, val completed: Int)
data class RefNet(val referenceId: String, val points: Int, val xp: Int, val coins: Int, val events: Int)

/** Apps the user explicitly chose to restrict. Opt-in only: nothing is ever blocked by default. */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val isEnabled: Boolean,
    val addedAt: Long,
)

/** Transparent history of every detected interruption (and every pause/override). */
@Entity(
    tableName = "interruption_events",
    indices = [Index(value = ["idempotencyKey"], unique = true), Index("detectedAt")],
)
data class InterruptionEventEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val displayName: String,
    val detectedAt: Long,
    val focusSessionId: String?,
    val actionTaken: com.levelup.app.domain.InterruptionAction,
    val pointsPenalty: Int,
    val idempotencyKey: String,
)
