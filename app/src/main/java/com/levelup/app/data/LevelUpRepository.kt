package com.levelup.app.data

import android.os.SystemClock
import androidx.room.withTransaction
import com.levelup.app.domain.Achievements
import com.levelup.app.domain.FocusDay
import com.levelup.app.domain.Category
import com.levelup.app.domain.DaySummary
import com.levelup.app.domain.DayTaskFlag
import com.levelup.app.domain.DistractionPolicy
import com.levelup.app.domain.InterruptionAction
import com.levelup.app.domain.InterruptionDecision
import com.levelup.app.domain.FocusMath
import com.levelup.app.domain.FocusOutcome
import com.levelup.app.domain.FocusStatus
import com.levelup.app.domain.LifetimeStats
import com.levelup.app.domain.Reward
import com.levelup.app.domain.RewardCatalog
import com.levelup.app.domain.ScoreEventType
import com.levelup.app.domain.ScoringEngine
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TaskStatus
import com.levelup.app.domain.TimeUtils
import com.levelup.app.notifications.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class InterruptionResult(
    val displayName: String,
    val decision: InterruptionDecision,
    val penalty: Int,
    val xpPenalty: Int = 0,
    val coinPenalty: Int = 0,
)

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object InsufficientCoins : PurchaseResult
    data object NotFound : PurchaseResult
}

/**
 * The only layer that coordinates DAOs. All reward-affecting writes run inside one Room transaction
 * and go through the append-only ledger with unique idempotency keys.
 */
class LevelUpRepository(
    private val db: LevelUpDatabase,
    private val settings: SettingsStore,
    private val reminders: ReminderManager,
    private val essentialPackages: () -> Set<String> = { emptySet() },
    private val allowTestGrants: Boolean = false,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private val taskDao = db.routineTaskDao()
    private val instanceDao = db.taskInstanceDao()
    private val scoreDao = db.scoreEventDao()
    private val focusDao = db.focusSessionDao()
    private val achievementDao = db.achievementDao()
    private val ownedDao = db.ownedRewardDao()
    private val blockedDao = db.blockedAppDao()
    private val interruptionDao = db.interruptionDao()

    val settingsFlow: Flow<UserSettings> = settings.flow

    // ---------------------------------------------------------------- time

    suspend fun zone(): ZoneId = TimeUtils.zoneOf(settings.current().timezoneId)

    suspend fun today(): LocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis()), zone())

    /** Emits the current local date (user's time zone) and again whenever it changes. */
    fun observeDate(): Flow<LocalDate> =
        flow {
            while (true) {
                emit(nowMillis())
                delay(15_000)
            }
        }.combine(settings.flow) { now, s ->
            LocalDate.ofInstant(Instant.ofEpochMilli(now), TimeUtils.zoneOf(s.timezoneId))
        }.distinctUntilChanged()

    // ---------------------------------------------------------- bootstrap

    suspend fun bootstrap() {
        ensureSeeded()
        recoverFocusSession()
        finishDueFocusSession()
        ensureDay(today())
        reminders.rescheduleAll()
    }

    private suspend fun ensureSeeded() {
        if (settings.current().seeded) return
        if (taskDao.count() == 0) taskDao.upsertAll(SeedData.sampleTasks(nowMillis()))
        settings.update { it.copy(seeded = true) }
    }

    // ----------------------------------------------------- routine & days

    fun observeTasks(): Flow<List<RoutineTaskEntity>> = taskDao.observeAll()

    /**
     * Brings a date's instances in line with the routine. Completed instances are history and are
     * never rewritten; pending ones follow routine edits.
     */
    suspend fun ensureDay(date: LocalDate) {
        val zone = zone()
        db.withTransaction {
            val key = date.toString()
            val tasks = taskDao.getAll().filter { it.isEnabled && TimeUtils.isActiveOn(it.repeatDaysMask, date) }
            val existing = instanceDao.getForDate(key).associateBy { it.id }
            val wanted = HashSet<String>()
            for (t in tasks) {
                val id = "$key:${t.id}"
                wanted += id
                val old = existing[id]
                if (old != null && old.status == TaskStatus.COMPLETED) continue
                val fresh = TaskInstanceEntity(
                    id = id,
                    taskId = t.id,
                    localDate = key,
                    title = t.title,
                    category = t.category,
                    priority = t.priority,
                    startMinute = t.startMinute,
                    durationMinutes = t.durationMinutes,
                    scheduledStart = TimeUtils.toInstant(date, t.startMinute, zone).toEpochMilli(),
                    scheduledEnd = TimeUtils.toInstant(date, t.startMinute + t.durationMinutes, zone).toEpochMilli(),
                    status = TaskStatus.PENDING,
                    completedAt = null,
                    pointsAwarded = 0,
                    xpAwarded = 0,
                )
                if (old == null) instanceDao.insertIgnore(fresh) else if (old != fresh) instanceDao.update(fresh)
            }
            for (old in existing.values) {
                if (old.id !in wanted && old.status != TaskStatus.COMPLETED) instanceDao.deletePending(old.id)
            }
        }
    }

    fun observeInstances(date: LocalDate): Flow<List<TaskInstanceEntity>> = instanceDao.observeForDate(date.toString())

    suspend fun saveTask(draft: TaskDraft) {
        val now = nowMillis()
        val offsetDay = TimeUtils.resolveEndDayOffset(draft.startMinute, draft.endMinute)
        val duration = TimeUtils.durationMinutes(draft.startMinute, draft.endMinute, offsetDay)
        val entity = db.withTransaction<RoutineTaskEntity> {
            val existing = draft.id?.let { taskDao.get(it) }
            val e = RoutineTaskEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = draft.title.trim(),
                description = draft.description?.trim()?.ifBlank { null },
                category = draft.category,
                startMinute = draft.startMinute,
                endMinute = draft.endMinute,
                endDayOffset = offsetDay,
                durationMinutes = duration,
                priority = draft.priority,
                reminderOffsetMinutes = draft.reminderOffsetMinutes,
                isEnabled = draft.isEnabled,
                sortOrder = existing?.sortOrder ?: (taskDao.maxSortOrder() + 1),
                repeatDaysMask = draft.repeatDaysMask,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            taskDao.upsert(e)
            e
        }
        reminders.schedule(entity)
        ensureDay(today())
    }

    suspend fun deleteTask(taskId: String) {
        val today = today()
        db.withTransaction {
            taskDao.delete(taskId)
            instanceDao.deletePendingForTaskFrom(taskId, today.toString())
        }
        reminders.cancel(taskId)
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) {
        val task = taskDao.get(taskId) ?: return
        val updated = task.copy(isEnabled = enabled, updatedAt = nowMillis())
        taskDao.upsert(updated)
        reminders.schedule(updated)
        ensureDay(today())
    }

    suspend fun moveTask(taskId: String, delta: Int) {
        db.withTransaction {
            val list = taskDao.getAll().mapIndexed { i, t -> t.copy(sortOrder = i) }.toMutableList()
            val from = list.indexOfFirst { it.id == taskId }
            val to = from + delta
            if (from < 0 || to !in list.indices) return@withTransaction
            val a = list[from]
            val b = list[to]
            list[from] = b.copy(sortOrder = from)
            list[to] = a.copy(sortOrder = to)
            taskDao.upsertAll(list)
        }
    }

    /** Rewrites the manual order so tasks run from earliest to latest start time (ties keep their order). */
    suspend fun sortTasksByTime() {
        db.withTransaction {
            val sorted = taskDao.getAll().sortedWith(compareBy({ it.startMinute }, { it.sortOrder }))
            taskDao.upsertAll(sorted.mapIndexed { i, t -> t.copy(sortOrder = i) })
        }
    }

    suspend fun duplicateTask(taskId: String) {
        val t = taskDao.get(taskId) ?: return
        val now = nowMillis()
        val copy = t.copy(
            id = UUID.randomUUID().toString(),
            title = t.title + " (copy)",
            sortOrder = taskDao.maxSortOrder() + 1,
            createdAt = now,
            updatedAt = now,
        )
        taskDao.upsert(copy)
        reminders.schedule(copy)
        ensureDay(today())
    }

    // ------------------------------------------------- completion & ledger

    /** Idempotent: calling with the current state is a no-op; toggling cannot farm XP. */
    suspend fun setCompleted(instanceId: String, completed: Boolean) {
        val today = today()
        db.withTransaction {
            val inst = instanceDao.get(instanceId) ?: return@withTransaction
            if ((inst.status == TaskStatus.COMPLETED) == completed) return@withTransaction
            val now = nowMillis()
            val reward = ScoringEngine.calculateTaskReward(inst.durationMinutes)
            instanceDao.update(
                inst.copy(
                    status = if (completed) TaskStatus.COMPLETED else TaskStatus.PENDING,
                    completedAt = if (completed) now else null,
                    pointsAwarded = if (completed) reward.points else 0,
                    xpAwarded = if (completed) reward.xp else 0,
                ),
            )
            reconcileDay(inst.localDate, now)
            evaluateAchievements(today, now)
        }
    }

    /** Appends correcting ledger entries so the net per reference key is exactly one award or zero. */
    private suspend fun reconcileDay(dateKey: String, now: Long) {
        val instances = instanceDao.getForDate(dateKey)
        val desired = LinkedHashMap<String, Reward>()
        val titles = HashMap<String, String>()
        for (i in instances) {
            val ref = "task:${i.id}"
            titles[ref] = i.title
            if (i.status == TaskStatus.COMPLETED) desired[ref] = ScoringEngine.calculateTaskReward(i.durationMinutes)
        }
        desired.putAll(
            ScoringEngine.calculateDailyBonuses(
                dateKey,
                instances.map { DayTaskFlag(it.priority == com.levelup.app.domain.Priority.HIGH, it.status == TaskStatus.COMPLETED) },
            ),
        )
        titles["priority:$dateKey"] = "All high-priority tasks done"
        titles["perfect:$dateKey"] = "Every task done"

        val net = scoreDao.netFor(titles.keys.toList()).associateBy { it.referenceId }
        val current = net.mapValues { Reward(it.value.points, it.value.xp, it.value.coins) }

        for ((ref, delta) in ScoringEngine.reconcile(desired, current)) {
            val positive = delta.points + delta.xp + delta.coins > 0
            val isBonus = !ref.startsWith("task:")
            val type = when {
                !positive -> ScoreEventType.TASK_REVERSED
                isBonus -> ScoreEventType.DAILY_BONUS
                else -> ScoreEventType.TASK_COMPLETED
            }
            val label = titles[ref] ?: ref
            scoreDao.insert(
                ScoreEventEntity(
                    id = UUID.randomUUID().toString(),
                    eventType = type,
                    referenceId = ref,
                    localDate = dateKey,
                    pointsDelta = delta.points,
                    xpDelta = delta.xp,
                    coinsDelta = delta.coins,
                    occurredAt = now,
                    idempotencyKey = "$ref#${net[ref]?.events ?: 0}",
                    note = if (positive) label else "Reversed: $label",
                ),
            )
        }
    }

    private suspend fun evaluateAchievements(today: LocalDate, now: Long) {
        val summaries = instanceDao.getDaySummaries().map { DaySummary(LocalDate.parse(it.localDate), it.total, it.completed) }
        val stats = LifetimeStats(
            completedTasks = instanceDao.completedCount(),
            perfectDays = summaries.count { it.total > 0 && it.completed == it.total },
            focusSessions = focusDao.completedCount(),
            totalFocusMinutes = focusDao.totalMinutes(),
            maxFocusSessionsInDay = focusDao.maxCompletedInOneDay(),
            longestStreak = ScoringEngine.calculateStreaks(summaries, today).longest,
            level = ScoringEngine.calculateLevelFromXp(scoreDao.totalXp()).level,
            studyMinutes = instanceDao.studyMinutes(),
            totalXp = scoreDao.totalXp(),
        )
        val already = achievementDao.ids().toSet()
        for (id in Achievements.unlocked(stats) - already) {
            val def = Achievements.byId(id) ?: continue
            if (achievementDao.insertIgnore(AchievementEntity(id, now)) == -1L) continue
            scoreDao.insert(
                ScoreEventEntity(
                    id = UUID.randomUUID().toString(),
                    eventType = ScoreEventType.ACHIEVEMENT,
                    referenceId = "ach:$id",
                    localDate = today.toString(),
                    pointsDelta = 0,
                    xpDelta = def.xp,
                    coinsDelta = def.coins,
                    occurredAt = now,
                    idempotencyKey = "ach:$id",
                    note = "Achievement: ${def.title}",
                ),
            )
        }
    }

    // ------------------------------------------------------------ read flows

    fun observePointsOn(date: LocalDate): Flow<Int> =
        scoreDao.observePointsOn(date.toString()).map { ScoringEngine.dailyPointsBalance(it) }

    fun observeTotalXp(): Flow<Long> = scoreDao.observeTotalXp()
    fun observeCoins(): Flow<Int> = scoreDao.observeCoins()
    fun observeRecentEvents(limit: Int): Flow<List<ScoreEventEntity>> = scoreDao.observeRecent(limit)

    fun observeSummaries(): Flow<List<DaySummary>> =
        instanceDao.observeDaySummaries().map { rows -> rows.map { DaySummary(LocalDate.parse(it.localDate), it.total, it.completed) } }

    fun observePointsByDay(from: LocalDate): Flow<Map<LocalDate, Int>> =
        scoreDao.observePointsByDay(from.toString()).map { rows ->
            rows.associate { LocalDate.parse(it.k) to ScoringEngine.dailyPointsBalance(it.v) }
        }

    fun observeCategoryMinutes(from: LocalDate): Flow<Map<Category, Int>> =
        instanceDao.observeCategoryMinutes(from.toString()).map { rows ->
            rows.mapNotNull { r -> runCatching { Category.valueOf(r.k) }.getOrNull()?.let { it to r.v } }.toMap()
        }

    fun observeFocusMinutesOn(date: LocalDate): Flow<Int> = focusDao.observeFocusMinutesOn(date.toString())

    /** (completed sessions, total focused minutes) */
    fun observeFocusStats(): Flow<Pair<Int, Int>> =
        combine(focusDao.observeCompletedCount(), focusDao.observeTotalMinutes()) { s, m -> s to m }

    fun observeAchievements(): Flow<List<AchievementEntity>> = achievementDao.observeAll()

    /** Completed-session minutes and counts per local date, from [from] onwards. */
    fun observeFocusByDay(from: LocalDate): Flow<Map<LocalDate, FocusDay>> =
        combine(focusDao.observeFocusMinutesByDay(from.toString()), focusDao.observeFocusSessionsByDay(from.toString())) { minutes, sessions ->
            val m = minutes.associate { LocalDate.parse(it.k) to it.v }
            val s = sessions.associate { LocalDate.parse(it.k) to it.v }
            (m.keys + s.keys).associateWith { FocusDay(m[it] ?: 0, s[it] ?: 0) }
        }

    /** Everything the achievement cards need, recomputed whenever tasks, focus sessions or the ledger change. */
    fun observeLifetimeStats(): Flow<LifetimeStats> {
        val tasks = combine(instanceDao.observeCompletedCount(), instanceDao.observeStudyMinutes(), observeSummaries()) { done, study, days ->
            Triple(done, study, days)
        }
        val focus = combine(focusDao.observeCompletedCount(), focusDao.observeTotalMinutes(), focusDao.observeMaxCompletedInOneDay()) { n, m, perDay ->
            Triple(n, m, perDay)
        }
        return combine(tasks, focus, scoreDao.observeTotalXp()) { (done, study, days), (sessions, minutes, perDay), xp ->
            LifetimeStats(
                completedTasks = done,
                perfectDays = days.count { it.total > 0 && it.completed == it.total },
                focusSessions = sessions,
                totalFocusMinutes = minutes,
                maxFocusSessionsInDay = perDay,
                longestStreak = ScoringEngine.calculateStreaks(days, LocalDate.ofEpochDay(0)).longest,
                level = ScoringEngine.calculateLevelFromXp(xp).level,
                studyMinutes = study,
                totalXp = xp,
            )
        }
    }
    fun observeOwnedIds(): Flow<Set<String>> = ownedDao.observeAll().map { list -> list.map { it.rewardItemId }.toSet() }

    // ------------------------------------------------------------------ focus

    fun observeActiveFocus(): Flow<FocusSessionEntity?> = focusDao.observeActive()
    fun observeRewardedMinutesOn(date: LocalDate): Flow<Int> = focusDao.observeRewardedMinutesOn(date.toString())
    fun observeRecentFocus(limit: Int): Flow<List<FocusSessionEntity>> = focusDao.observeRecentCompleted(limit)

    /**
     * Finishes a RUNNING session whose planned time has passed while the app was closed or in the background.
     * Safe to call any time: rewards are still granted exactly once.
     */
    suspend fun finishDueFocusSession(): FocusOutcome? {
        val s = focusDao.getActive() ?: return null
        if (s.status != FocusStatus.RUNNING || elapsedSeconds(s) < s.plannedSeconds) return null
        return finishFocus(s.id)
    }

    fun elapsedSeconds(s: FocusSessionEntity): Long =
        FocusMath.elapsedSeconds(s.status, s.accumulatedSeconds, s.resumedAtElapsed, s.resumedAtWall, elapsedRealtime(), nowMillis())

    /** Returns the new session id, or null if a session is already active. */
    suspend fun startFocus(title: String, category: Category?, plannedSeconds: Long): String? {
        val zone = zone()
        return db.withTransaction<String?> {
            if (focusDao.getActive() != null) return@withTransaction null
            val now = nowMillis()
            val id = UUID.randomUUID().toString()
            focusDao.insert(
                FocusSessionEntity(
                    id = id,
                    title = title.trim().ifBlank { "Focus session" },
                    category = category,
                    plannedSeconds = plannedSeconds.coerceAtLeast(ScoringEngine.MIN_FOCUS_SECONDS),
                    startedAt = now,
                    endedAt = null,
                    accumulatedSeconds = 0,
                    status = FocusStatus.RUNNING,
                    resumedAtElapsed = elapsedRealtime(),
                    resumedAtWall = now,
                    rewardedMinutes = 0,
                    pointsAwarded = 0,
                    xpAwarded = 0,
                    coinsAwarded = 0,
                    interruptionCount = 0,
                    localDate = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone).toString(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            id
        }.also { id -> if (id != null) reminders.scheduleFocusEnd(nowMillis() + plannedSeconds.coerceAtLeast(ScoringEngine.MIN_FOCUS_SECONDS) * 1000) }
    }

    suspend fun pauseFocus(id: String) {
        db.withTransaction {
            val s = focusDao.get(id) ?: return@withTransaction
            if (s.status != FocusStatus.RUNNING) return@withTransaction
            focusDao.update(
                s.copy(
                    accumulatedSeconds = minOf(elapsedSeconds(s), s.plannedSeconds),
                    status = FocusStatus.PAUSED,
                    resumedAtElapsed = -1,
                    updatedAt = nowMillis(),
                ),
            )
        }
        reminders.cancelFocusEnd()
    }

    suspend fun resumeFocus(id: String) {
        var remainingSeconds = -1L
        db.withTransaction {
            val s = focusDao.get(id) ?: return@withTransaction
            if (s.status != FocusStatus.PAUSED) return@withTransaction
            val now = nowMillis()
            focusDao.update(s.copy(status = FocusStatus.RUNNING, resumedAtElapsed = elapsedRealtime(), resumedAtWall = now, updatedAt = now))
            remainingSeconds = s.plannedSeconds - s.accumulatedSeconds
        }
        if (remainingSeconds > 0) reminders.scheduleFocusEnd(nowMillis() + remainingSeconds * 1000)
    }

    /** Sessions shorter than 10 minutes are discarded without reward. Rewards are granted exactly once. */
    suspend fun finishFocus(id: String): FocusOutcome? {
        val outcome = finishFocusInternal(id)
        if (outcome != null) reminders.cancelFocusEnd()
        return outcome
    }

    private suspend fun finishFocusInternal(id: String): FocusOutcome? {
        val zone = zone()
        val cap = settings.current().focusDailyCapMinutes
        return db.withTransaction<FocusOutcome?> {
            val s = focusDao.get(id) ?: return@withTransaction null
            if (s.status != FocusStatus.RUNNING && s.status != FocusStatus.PAUSED) return@withTransaction null
            val now = nowMillis()
            val elapsed = minOf(elapsedSeconds(s), s.plannedSeconds)
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
            if (elapsed < ScoringEngine.MIN_FOCUS_SECONDS) {
                focusDao.update(
                    s.copy(status = FocusStatus.CANCELLED, accumulatedSeconds = elapsed, endedAt = now, resumedAtElapsed = -1, localDate = date.toString(), updatedAt = now),
                )
                return@withTransaction FocusOutcome(false, elapsed / 60, Reward.ZERO, 0)
            }
            val fr = ScoringEngine.calculateFocusReward(elapsed, cap, focusDao.rewardedMinutesOn(date.toString()))
            focusDao.update(
                s.copy(
                    status = FocusStatus.COMPLETED, accumulatedSeconds = elapsed, endedAt = now, resumedAtElapsed = -1,
                    rewardedMinutes = fr.rewardedMinutes, pointsAwarded = fr.reward.points, xpAwarded = fr.reward.xp,
                    coinsAwarded = fr.reward.coins, localDate = date.toString(), updatedAt = now,
                ),
            )
            if (!fr.reward.isZero) {
                scoreDao.insert(
                    ScoreEventEntity(
                        id = UUID.randomUUID().toString(),
                        eventType = ScoreEventType.FOCUS_COMPLETED,
                        referenceId = "focus:${s.id}",
                        localDate = date.toString(),
                        pointsDelta = fr.reward.points,
                        xpDelta = fr.reward.xp,
                        coinsDelta = fr.reward.coins,
                        occurredAt = now,
                        idempotencyKey = "focus:${s.id}",
                        note = "Focus: ${s.title} (${fr.rewardedMinutes} min)",
                    ),
                )
            }
            evaluateAchievements(date, now)
            FocusOutcome(true, elapsed / 60, fr.reward, fr.rewardedMinutes)
        }
    }

    /** Cancelled sessions never receive rewards. */
    suspend fun cancelFocus(id: String) {
        db.withTransaction {
            val s = focusDao.get(id) ?: return@withTransaction
            if (s.status != FocusStatus.RUNNING && s.status != FocusStatus.PAUSED) return@withTransaction
            val now = nowMillis()
            focusDao.update(
                s.copy(status = FocusStatus.CANCELLED, accumulatedSeconds = minOf(elapsedSeconds(s), s.plannedSeconds), endedAt = now, resumedAtElapsed = -1, updatedAt = now),
            )
        }
        reminders.cancelFocusEnd()
    }

    /** After a reboot a RUNNING session is paused with only the time we can prove. */
    private suspend fun recoverFocusSession() {
        db.withTransaction {
            val s = focusDao.getActive() ?: return@withTransaction
            if (s.status == FocusStatus.RUNNING &&
                FocusMath.rebootSuspected(s.resumedAtElapsed, s.resumedAtWall, elapsedRealtime(), nowMillis())
            ) {
                focusDao.update(
                    s.copy(status = FocusStatus.PAUSED, resumedAtElapsed = -1, interruptionCount = s.interruptionCount + 1, updatedAt = nowMillis()),
                )
            }
        }
    }

    // ---------------------------------------------------------------- rewards

    suspend fun purchase(itemId: String): PurchaseResult {
        val item = RewardCatalog.find(itemId) ?: return PurchaseResult.NotFound
        val zone = zone()
        return db.withTransaction<PurchaseResult> {
            if (ownedDao.countOwned(itemId) > 0) return@withTransaction PurchaseResult.AlreadyOwned
            if (scoreDao.coins() < item.priceCoins) return@withTransaction PurchaseResult.InsufficientCoins
            val now = nowMillis()
            ownedDao.insertIgnore(OwnedRewardEntity(itemId, now, "COINS"))
            scoreDao.insert(
                ScoreEventEntity(
                    id = UUID.randomUUID().toString(),
                    eventType = ScoreEventType.REWARD_PURCHASE,
                    referenceId = "buy:$itemId",
                    localDate = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone).toString(),
                    pointsDelta = 0,
                    xpDelta = 0,
                    coinsDelta = -item.priceCoins,
                    occurredAt = now,
                    idempotencyKey = "buy:$itemId",
                    note = "Purchased: ${item.title}",
                ),
            )
            PurchaseResult.Success
        }
    }

    /** Debug builds only: lets testers try the shop. Recorded in the ledger like everything else. */
    suspend fun grantTestCoins(amount: Int): Boolean {
        if (!allowTestGrants) return false
        val now = nowMillis()
        val zone = zone()
        scoreDao.insert(
            ScoreEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = ScoreEventType.TEST_GRANT,
                referenceId = "test:$now",
                localDate = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone).toString(),
                pointsDelta = 0,
                xpDelta = 0,
                coinsDelta = amount.coerceIn(1, 100_000),
                occurredAt = now,
                idempotencyKey = "testgrant:$now",
                note = "Test coins (debug build only)",
            ),
        )
        return true
    }

    suspend fun equipTheme(themeId: String) {
        if (themeId != RewardCatalog.DEFAULT_THEME_ID && ownedDao.countOwned(themeId) == 0) return
        settings.update { it.copy(accentThemeId = themeId) }
    }

    // ------------------------------------------------- distraction protection

    fun observeBlockedApps(): Flow<List<BlockedAppEntity>> = blockedDao.observeAll()
    fun observeInterruptions(limit: Int): Flow<List<InterruptionEventEntity>> = interruptionDao.observeRecent(limit)

    suspend fun setAppSelected(packageName: String, label: String, selected: Boolean) {
        if (selected) {
            if (packageName in essentialPackages()) return // essential apps can never be restricted
            blockedDao.upsert(BlockedAppEntity(packageName, label, true, nowMillis()))
        } else {
            blockedDao.delete(packageName)
        }
    }

    suspend fun setAppRuleEnabled(packageName: String, enabled: Boolean) = blockedDao.setEnabled(packageName, enabled)

    /** Settings changes that must not trigger a reminder reschedule. */
    suspend fun updateSettingsQuiet(transform: (UserSettings) -> UserSettings) = settings.update(transform)

    suspend fun isProtectionActive(): Boolean {
        val s = settings.current()
        return s.blockerEnabled && focusDao.getActive()?.status == FocusStatus.RUNNING
    }

    /**
     * Called when a package reaches the foreground. Only user-selected, non-essential apps during a
     * RUNNING focus session can ever produce an event; repeated events are deduplicated and a per-app
     * cooldown prevents repeated deductions for one launch.
     */
    suspend fun handleForegroundEvent(packageName: String, eventTimeMs: Long, fallbackName: String = packageName): InterruptionResult? {
        val s = settings.current()
        val app = blockedDao.get(packageName)?.takeIf { it.isEnabled }
        if (app == null && !s.blockerStrict) return null
        val displayName = app?.displayName ?: fallbackName
        val active = focusDao.getActive()
        val verdict = DistractionPolicy.calculate(
            eventTimeMs = eventTimeMs,
            lastHandledEventMs = interruptionDao.lastEventTime(packageName),
            lastPenaltyMs = interruptionDao.lastPenaltyTime(packageName),
            penaltyPoints = s.blockerPenaltyPoints,
            cooldownSeconds = s.blockerCooldownSeconds,
            protectionEnabled = s.blockerEnabled,
            focusRunning = active?.status == FocusStatus.RUNNING,
            isSelectedApp = true,
            isEssential = packageName in essentialPackages(),
            pausedUntilMs = s.blockerPausedUntilMs,
            otherPenaltyConfigured = s.blockerPenaltyCoins > 0 || s.blockerPenaltyXp > 0,
        )
        if (verdict.decision == InterruptionDecision.IGNORE || verdict.decision == InterruptionDecision.DUPLICATE) return null

        val date = LocalDate.ofInstant(Instant.ofEpochMilli(eventTimeMs), TimeUtils.zoneOf(s.timezoneId)).toString()
        val penalize = verdict.decision == InterruptionDecision.PENALIZE
        var xpCut = 0
        var coinCut = 0
        db.withTransaction {
            val inserted = interruptionDao.insert(
                InterruptionEventEntity(
                    id = UUID.randomUUID().toString(),
                    packageName = packageName,
                    displayName = displayName,
                    detectedAt = eventTimeMs,
                    focusSessionId = active?.id,
                    actionTaken = if (penalize) InterruptionAction.PENALIZED else InterruptionAction.LOGGED_ONLY,
                    pointsPenalty = verdict.penaltyPoints,
                    idempotencyKey = "intr:$packageName:$eventTimeMs",
                ),
            )
            if (inserted != -1L && penalize) {
                // Never push XP or coins below zero, whatever the configured penalty is.
                xpCut = minOf(s.blockerPenaltyXp, scoreDao.totalXp().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(0)
                coinCut = minOf(s.blockerPenaltyCoins, scoreDao.coins()).coerceAtLeast(0)
                scoreDao.insert(
                    ScoreEventEntity(
                        id = UUID.randomUUID().toString(),
                        eventType = ScoreEventType.DISTRACTION_PENALTY,
                        referenceId = "intr:$packageName:$eventTimeMs",
                        localDate = date,
                        pointsDelta = -verdict.penaltyPoints,
                        xpDelta = -xpCut,
                        coinsDelta = -coinCut,
                        occurredAt = eventTimeMs,
                        idempotencyKey = "penalty:$packageName:$eventTimeMs",
                        note = "Opened $displayName during focus",
                    ),
                )
            }
        }
        return InterruptionResult(displayName, verdict.decision, verdict.penaltyPoints, xpCut, coinCut)
    }

    /** User-initiated pause or emergency override. Always logged; never penalised. */
    suspend fun pauseProtection(minutes: Int, emergency: Boolean) {
        val now = nowMillis()
        val mins = minutes.coerceIn(1, 240)
        settings.update { it.copy(blockerPausedUntilMs = now + mins * 60_000L) }
        interruptionDao.insert(
            InterruptionEventEntity(
                id = UUID.randomUUID().toString(),
                packageName = "(pause)",
                displayName = if (emergency) "Emergency override ($mins min)" else "Protection paused ($mins min)",
                detectedAt = now,
                focusSessionId = focusDao.getActive()?.id,
                actionTaken = InterruptionAction.OVERRIDE,
                pointsPenalty = 0,
                idempotencyKey = "pause:$now",
            ),
        )
    }

    suspend fun resumeProtection() = settings.update { it.copy(blockerPausedUntilMs = 0L) }

    // ------------------------------------------------------- settings & data

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        settings.update(transform)
        reminders.rescheduleAll()
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("exportedAt", Instant.ofEpochMilli(nowMillis()).toString())
        root.put("app", "Level Up")
        root.put("schemaVersion", 1)
        root.put("routineTasks", JSONArray().apply {
            taskDao.getAll().forEach { t ->
                put(JSONObject().put("id", t.id).put("title", t.title).put("category", t.category.name)
                    .put("start", TimeUtils.format(t.startMinute)).put("end", TimeUtils.format(t.endMinute))
                    .put("durationMinutes", t.durationMinutes).put("priority", t.priority.name)
                    .put("reminderOffsetMinutes", t.reminderOffsetMinutes ?: JSONObject.NULL)
                    .put("enabled", t.isEnabled).put("repeatDaysMask", t.repeatDaysMask))
            }
        })
        root.put("taskInstances", JSONArray().apply {
            instanceDao.getAll().forEach { i ->
                put(JSONObject().put("id", i.id).put("date", i.localDate).put("title", i.title)
                    .put("category", i.category.name).put("durationMinutes", i.durationMinutes)
                    .put("status", i.status.name).put("completedAt", i.completedAt ?: JSONObject.NULL)
                    .put("pointsAwarded", i.pointsAwarded).put("xpAwarded", i.xpAwarded))
            }
        })
        root.put("focusSessions", JSONArray().apply {
            focusDao.getAll().forEach { f ->
                put(JSONObject().put("id", f.id).put("title", f.title).put("date", f.localDate)
                    .put("status", f.status.name).put("plannedSeconds", f.plannedSeconds)
                    .put("focusedSeconds", f.accumulatedSeconds).put("pointsAwarded", f.pointsAwarded)
                    .put("xpAwarded", f.xpAwarded).put("coinsAwarded", f.coinsAwarded))
            }
        })
        root.put("scoreLedger", JSONArray().apply {
            scoreDao.getAll().forEach { e ->
                put(JSONObject().put("id", e.id).put("type", e.eventType.name).put("reference", e.referenceId)
                    .put("date", e.localDate).put("points", e.pointsDelta).put("xp", e.xpDelta)
                    .put("coins", e.coinsDelta).put("occurredAt", e.occurredAt).put("note", e.note ?: JSONObject.NULL))
            }
        })
        root.put("selectedApps", JSONArray().apply {
            blockedDao.getAll().forEach { put(JSONObject().put("package", it.packageName).put("name", it.displayName).put("enabled", it.isEnabled)) }
        })
        root.put("interruptions", JSONArray().apply {
            interruptionDao.getAll().forEach {
                put(JSONObject().put("package", it.packageName).put("name", it.displayName).put("detectedAt", it.detectedAt)
                    .put("action", it.actionTaken.name).put("pointsPenalty", it.pointsPenalty))
            }
        })
        root.put("achievements", JSONArray().apply {
            achievementDao.getAll().forEach { put(JSONObject().put("id", it.id).put("unlockedAt", it.unlockedAt)) }
        })
        root.put("ownedRewards", JSONArray().apply {
            ownedDao.getAll().forEach { put(JSONObject().put("id", it.rewardItemId).put("acquiredAt", it.acquiredAt)) }
        })
        return root.toString(2)
    }

    /** Wipes every table and preference, then restores the editable sample routine. */
    suspend fun clearAll() {
        reminders.cancelAll()
        withContext(Dispatchers.IO) { db.clearAllTables() }
        settings.clear()
        ensureSeeded()
        ensureDay(today())
        reminders.rescheduleAll()
    }
}
