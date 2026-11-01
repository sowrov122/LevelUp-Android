package com.levelup.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTaskDao {
    @Query("SELECT * FROM routine_tasks ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE id = :id")
    suspend fun get(id: String): RoutineTaskEntity?

    @Query("SELECT COUNT(*) FROM routine_tasks")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM routine_tasks")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: RoutineTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<RoutineTaskEntity>)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TaskInstanceDao {
    @Query("SELECT * FROM task_instances WHERE localDate = :date ORDER BY startMinute")
    fun observeForDate(date: String): Flow<List<TaskInstanceEntity>>

    @Query("SELECT * FROM task_instances WHERE localDate = :date ORDER BY startMinute")
    suspend fun getForDate(date: String): List<TaskInstanceEntity>

    @Query("SELECT * FROM task_instances WHERE id = :id")
    suspend fun get(id: String): TaskInstanceEntity?

    @Query("SELECT * FROM task_instances ORDER BY localDate, startMinute")
    suspend fun getAll(): List<TaskInstanceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(instance: TaskInstanceEntity): Long

    @Update
    suspend fun update(instance: TaskInstanceEntity)

    /** Only ever removes pending rows: completed history is never deleted. */
    @Query("DELETE FROM task_instances WHERE id = :id AND status != 'COMPLETED'")
    suspend fun deletePending(id: String)

    @Query("DELETE FROM task_instances WHERE taskId = :taskId AND localDate >= :fromDate AND status != 'COMPLETED'")
    suspend fun deletePendingForTaskFrom(taskId: String, fromDate: String)

    @Query(
        "SELECT localDate, COUNT(*) AS total, " +
            "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed " +
            "FROM task_instances GROUP BY localDate ORDER BY localDate"
    )
    fun observeDaySummaries(): Flow<List<DaySummaryRow>>

    @Query(
        "SELECT localDate, COUNT(*) AS total, " +
            "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed " +
            "FROM task_instances GROUP BY localDate ORDER BY localDate"
    )
    suspend fun getDaySummaries(): List<DaySummaryRow>

    @Query(
        "SELECT category AS k, COALESCE(SUM(durationMinutes), 0) AS v FROM task_instances " +
            "WHERE status = 'COMPLETED' AND localDate >= :fromDate GROUP BY category"
    )
    fun observeCategoryMinutes(fromDate: String): Flow<List<KeyValue>>

    @Query("SELECT COUNT(*) FROM task_instances WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM task_instances WHERE status = 'COMPLETED' AND category = 'STUDY'")
    suspend fun studyMinutes(): Int
}

@Dao
interface ScoreEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: ScoreEventEntity): Long

    @Query("SELECT COALESCE(SUM(xpDelta), 0) FROM score_events")
    fun observeTotalXp(): Flow<Long>

    @Query("SELECT COALESCE(SUM(xpDelta), 0) FROM score_events")
    suspend fun totalXp(): Long

    @Query("SELECT COALESCE(SUM(coinsDelta), 0) FROM score_events")
    fun observeCoins(): Flow<Int>

    @Query("SELECT COALESCE(SUM(coinsDelta), 0) FROM score_events")
    suspend fun coins(): Int

    @Query("SELECT COALESCE(SUM(pointsDelta), 0) FROM score_events WHERE localDate = :date")
    fun observePointsOn(date: String): Flow<Int>

    @Query(
        "SELECT localDate AS k, COALESCE(SUM(pointsDelta), 0) AS v FROM score_events " +
            "WHERE localDate >= :fromDate GROUP BY localDate"
    )
    fun observePointsByDay(fromDate: String): Flow<List<KeyValue>>

    @Query(
        "SELECT referenceId, COALESCE(SUM(pointsDelta), 0) AS points, COALESCE(SUM(xpDelta), 0) AS xp, " +
            "COALESCE(SUM(coinsDelta), 0) AS coins, COUNT(*) AS events " +
            "FROM score_events WHERE referenceId IN (:refs) GROUP BY referenceId"
    )
    suspend fun netFor(refs: List<String>): List<RefNet>

    @Query("SELECT * FROM score_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScoreEventEntity>>

    @Query("SELECT * FROM score_events ORDER BY occurredAt")
    suspend fun getAll(): List<ScoreEventEntity>
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity)

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun get(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActive(): FocusSessionEntity?

    @Query("SELECT COALESCE(SUM(rewardedMinutes), 0) FROM focus_sessions WHERE localDate = :date AND status = 'COMPLETED'")
    suspend fun rewardedMinutesOn(date: String): Int

    @Query("SELECT COALESCE(SUM(accumulatedSeconds), 0) / 60 FROM focus_sessions WHERE localDate = :date AND status = 'COMPLETED'")
    fun observeFocusMinutesOn(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Query("SELECT COALESCE(SUM(accumulatedSeconds), 0) / 60 FROM focus_sessions WHERE status = 'COMPLETED'")
    fun observeTotalMinutes(): Flow<Int>

    @Query("SELECT COALESCE(SUM(accumulatedSeconds), 0) / 60 FROM focus_sessions WHERE status = 'COMPLETED'")
    suspend fun totalMinutes(): Int

    @Query(
        "SELECT COALESCE(MAX(c), 0) FROM " +
            "(SELECT COUNT(*) AS c FROM focus_sessions WHERE status = 'COMPLETED' GROUP BY localDate)"
    )
    suspend fun maxCompletedInOneDay(): Int

    @Query("SELECT * FROM focus_sessions ORDER BY createdAt")
    suspend fun getAll(): List<FocusSessionEntity>
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT id FROM achievements")
    suspend fun ids(): List<String>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(a: AchievementEntity): Long
}

@Dao
interface OwnedRewardDao {
    @Query("SELECT * FROM owned_rewards")
    fun observeAll(): Flow<List<OwnedRewardEntity>>

    @Query("SELECT * FROM owned_rewards")
    suspend fun getAll(): List<OwnedRewardEntity>

    @Query("SELECT COUNT(*) FROM owned_rewards WHERE rewardItemId = :id")
    suspend fun countOwned(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(o: OwnedRewardEntity): Long
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps ORDER BY displayName COLLATE NOCASE")
    suspend fun getAll(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkg")
    suspend fun get(pkg: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("UPDATE blocked_apps SET isEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)
}

@Dao
interface InterruptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: InterruptionEventEntity): Long

    @Query("SELECT * FROM interruption_events ORDER BY detectedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<InterruptionEventEntity>>

    @Query("SELECT * FROM interruption_events ORDER BY detectedAt")
    suspend fun getAll(): List<InterruptionEventEntity>

    @Query("SELECT MAX(detectedAt) FROM interruption_events WHERE packageName = :pkg")
    suspend fun lastEventTime(pkg: String): Long?

    @Query("SELECT MAX(detectedAt) FROM interruption_events WHERE packageName = :pkg AND actionTaken = 'PENALIZED'")
    suspend fun lastPenaltyTime(pkg: String): Long?
}
