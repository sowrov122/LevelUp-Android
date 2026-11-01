package com.levelup.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.app.AppContainer
import com.levelup.app.data.FocusSessionEntity
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.PurchaseResult
import com.levelup.app.data.RoutineTaskEntity
import com.levelup.app.data.ScoreEventEntity
import com.levelup.app.data.TaskInstanceEntity
import com.levelup.app.data.UserSettings
import com.levelup.app.domain.AchievementDef
import com.levelup.app.domain.Achievements
import com.levelup.app.domain.Category
import com.levelup.app.domain.DaySummary
import com.levelup.app.domain.FocusOutcome
import com.levelup.app.domain.FocusStatus
import com.levelup.app.domain.LevelProgress
import com.levelup.app.domain.Priority
import com.levelup.app.domain.ScoringEngine
import com.levelup.app.domain.Streaks
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TaskSlot
import com.levelup.app.domain.TaskStatus
import com.levelup.app.domain.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

private val EMPTY_SETTINGS = UserSettings()

private fun ticker(intervalMs: Long): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(intervalMs)
    }
}

// =========================================================== Today

data class TaskRowUi(
    val instanceId: String,
    val taskId: String,
    val title: String,
    val category: Category,
    val timeLabel: String,
    val durationMinutes: Int,
    val status: TaskStatus,
    val inProgress: Boolean,
    val priority: Priority,
    val pointsAwarded: Int,
)

data class TodayUiState(
    val loading: Boolean = true,
    val date: LocalDate? = null,
    val name: String = "",
    val tasks: List<TaskRowUi> = emptyList(),
    val completed: Int = 0,
    val total: Int = 0,
    val pointsToday: Int = 0,
    val level: LevelProgress = ScoringEngine.calculateLevelFromXp(0),
    val coins: Int = 0,
    val streaks: Streaks = Streaks(0, 0),
    val nextTask: TaskRowUi? = null,
    val focusMinutes: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val repo: LevelUpRepository) : ViewModel() {
    private data class Extras(val focusMinutes: Int, val coins: Int, val name: String, val now: Long)

    val state: StateFlow<TodayUiState> = repo.observeDate().flatMapLatest { date ->
        flow { repo.ensureDay(date); emit(Unit) }.flatMapLatest {
            val extras = combine(
                repo.observeFocusMinutesOn(date), repo.observeCoins(), repo.settingsFlow, ticker(30_000),
            ) { focus, coins, settings, now -> Extras(focus, coins, settings.displayName, now) }
            combine(
                repo.observeInstances(date), repo.observePointsOn(date), repo.observeTotalXp(),
                repo.observeSummaries(), extras,
            ) { instances, points, xp, summaries, ex -> build(date, instances, points, xp, summaries, ex) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun build(
        date: LocalDate,
        instances: List<TaskInstanceEntity>,
        points: Int,
        xp: Long,
        summaries: List<DaySummary>,
        ex: Extras,
    ): TodayUiState {
        val rows = instances.map { i ->
            val done = i.status == TaskStatus.COMPLETED
            TaskRowUi(
                instanceId = i.id,
                taskId = i.taskId,
                title = i.title,
                category = i.category,
                timeLabel = "${TimeUtils.format(i.startMinute)}–${TimeUtils.format(i.startMinute + i.durationMinutes)}",
                durationMinutes = i.durationMinutes,
                status = when {
                    done -> TaskStatus.COMPLETED
                    ex.now >= i.scheduledEnd -> TaskStatus.MISSED
                    else -> TaskStatus.PENDING
                },
                inProgress = !done && ex.now >= i.scheduledStart && ex.now < i.scheduledEnd,
                priority = i.priority,
                pointsAwarded = i.pointsAwarded,
            )
        }
        return TodayUiState(
            loading = false,
            date = date,
            name = ex.name,
            tasks = rows,
            completed = rows.count { it.status == TaskStatus.COMPLETED },
            total = rows.size,
            pointsToday = points,
            level = ScoringEngine.calculateLevelFromXp(xp),
            coins = ex.coins,
            streaks = ScoringEngine.calculateStreaks(summaries, date),
            nextTask = rows.firstOrNull { it.status == TaskStatus.PENDING },
            focusMinutes = ex.focusMinutes,
        )
    }

    fun setCompleted(instanceId: String, completed: Boolean) {
        viewModelScope.launch { repo.setCompleted(instanceId, completed) }
    }
}

// ========================================================== Routine

data class RoutineItemUi(val task: RoutineTaskEntity, val conflictsWith: List<String>)

data class RoutineUiState(
    val loading: Boolean = true,
    val items: List<RoutineItemUi> = emptyList(),
    val anyReminderConfigured: Boolean = false,
)

class RoutineViewModel(private val repo: LevelUpRepository) : ViewModel() {
    val state: StateFlow<RoutineUiState> = repo.observeTasks().map { tasks ->
        val slots = tasks.filter { it.isEnabled }.associate { it.id to TaskSlot(it.startMinute, it.durationMinutes, it.repeatDaysMask) }
        RoutineUiState(
            loading = false,
            items = tasks.map { t ->
                val mine = slots[t.id]
                val clashes = if (mine == null) emptyList() else tasks
                    .filter { it.id != t.id && it.isEnabled && TimeUtils.conflicts(mine, slots.getValue(it.id)) }
                    .map { it.title }
                RoutineItemUi(t, clashes)
            },
            anyReminderConfigured = tasks.any { it.isEnabled && it.reminderOffsetMinutes != null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineUiState())

    fun save(draft: TaskDraft) { viewModelScope.launch { repo.saveTask(draft) } }
    fun delete(taskId: String) { viewModelScope.launch { repo.deleteTask(taskId) } }
    fun setEnabled(taskId: String, enabled: Boolean) { viewModelScope.launch { repo.setTaskEnabled(taskId, enabled) } }
    fun move(taskId: String, delta: Int) { viewModelScope.launch { repo.moveTask(taskId, delta) } }
    fun duplicate(taskId: String) { viewModelScope.launch { repo.duplicateTask(taskId) } }
}

// ============================================================= Focus

data class ActiveFocusUi(
    val id: String,
    val title: String,
    val plannedSeconds: Long,
    val elapsedSeconds: Long,
    val running: Boolean,
    val interrupted: Boolean,
) {
    val remainingSeconds: Long get() = (plannedSeconds - elapsedSeconds).coerceAtLeast(0)
}

data class FocusUiState(
    val active: ActiveFocusUi? = null,
    val lastOutcome: FocusOutcome? = null,
    val sessions: Int = 0,
    val minutes: Int = 0,
    val owned: Set<String> = emptySet(),
    val reducedMotion: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(private val repo: LevelUpRepository) : ViewModel() {
    private val lastOutcome = MutableStateFlow<FocusOutcome?>(null)
    private val finishing = AtomicBoolean(false)

    private val activeUi: Flow<ActiveFocusUi?> = repo.observeActiveFocus().flatMapLatest<FocusSessionEntity?, ActiveFocusUi?> { s ->
        when {
            s == null -> flowOf<ActiveFocusUi?>(null)
            s.status == FocusStatus.RUNNING -> flow<ActiveFocusUi?> {
                while (true) {
                    emit(toUi(s))
                    delay(500)
                }
            }
            else -> flowOf(toUi(s))
        }
    }

    val state: StateFlow<FocusUiState> = combine(
        activeUi, repo.observeFocusStats(), repo.observeOwnedIds(), repo.settingsFlow, lastOutcome,
    ) { active, stats, owned, settings, outcome ->
        FocusUiState(active, outcome, stats.first, stats.second, owned, settings.reducedMotion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    init {
        // The persisted timestamps are the source of truth; auto-finish when the plan is complete.
        viewModelScope.launch {
            activeUi.collect { a ->
                if (a != null && a.running && a.elapsedSeconds >= a.plannedSeconds && finishing.compareAndSet(false, true)) {
                    try {
                        repo.finishFocus(a.id)?.let { lastOutcome.value = it }
                    } finally {
                        finishing.set(false)
                    }
                }
            }
        }
    }

    private fun toUi(s: FocusSessionEntity) = ActiveFocusUi(
        id = s.id,
        title = s.title,
        plannedSeconds = s.plannedSeconds,
        elapsedSeconds = minOf(repo.elapsedSeconds(s), s.plannedSeconds),
        running = s.status == FocusStatus.RUNNING,
        interrupted = s.interruptionCount > 0,
    )

    fun start(title: String, category: Category?, minutes: Int) {
        viewModelScope.launch {
            lastOutcome.value = null
            repo.startFocus(title, category, minutes.coerceIn(1, 600) * 60L)
        }
    }

    fun pause(id: String) { viewModelScope.launch { repo.pauseFocus(id) } }
    fun resume(id: String) { viewModelScope.launch { repo.resumeFocus(id) } }
    fun finish(id: String) { viewModelScope.launch { repo.finishFocus(id)?.let { lastOutcome.value = it } } }
    fun cancel(id: String) { viewModelScope.launch { repo.cancelFocus(id) } }
    fun dismissOutcome() { lastOutcome.value = null }
}

// ========================================================== Progress

data class HeatCell(val date: LocalDate, val percent: Int?)
data class AchievementUi(val def: AchievementDef, val unlockedAt: Long?)

data class ProgressUiState(
    val rangeDays: Int = 7,
    val completionBars: List<Pair<String, Int>> = emptyList(),
    val pointsBars: List<Pair<String, Int>> = emptyList(),
    val heat: List<HeatCell> = emptyList(),
    val categoryMinutes: List<Pair<Category, Int>> = emptyList(),
    val streaks: Streaks = Streaks(0, 0),
    val focusSessions: Int = 0,
    val focusMinutes: Int = 0,
    val achievements: List<AchievementUi> = emptyList(),
    val hasAnyData: Boolean = false,
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val repo: LevelUpRepository) : ViewModel() {
    private val range = MutableStateFlow(7)

    val state: StateFlow<ProgressUiState> =
        combine(range, repo.observeDate()) { r, d -> r to d }.flatMapLatest { (r, today) ->
            val from = today.minusDays(r - 1L)
            combine(
                repo.observeSummaries(), repo.observePointsByDay(from), repo.observeCategoryMinutes(from),
                repo.observeFocusStats(), repo.observeAchievements(),
            ) { summaries, points, cats, focus, ach ->
                val byDate = summaries.associateBy { it.date }
                val days = (0 until r).map { from.plusDays(it.toLong()) }
                fun label(i: Int, d: LocalDate) = if (r <= 7 || i % 5 == 0) d.dayOfMonth.toString() else ""
                val unlocked = ach.associate { it.id to it.unlockedAt }
                ProgressUiState(
                    rangeDays = r,
                    completionBars = days.mapIndexed { i, d ->
                        val s = byDate[d]
                        label(i, d) to (if (s == null || s.total == 0) 0 else s.completed * 100 / s.total)
                    },
                    pointsBars = days.mapIndexed { i, d -> label(i, d) to (points[d] ?: 0) },
                    heat = (34 downTo 0).map { back ->
                        val d = today.minusDays(back.toLong())
                        val s = byDate[d]
                        HeatCell(d, if (s == null || s.total == 0) null else s.completed * 100 / s.total)
                    },
                    categoryMinutes = cats.entries.sortedByDescending { it.value }.map { it.key to it.value },
                    streaks = ScoringEngine.calculateStreaks(summaries, today),
                    focusSessions = focus.first,
                    focusMinutes = focus.second,
                    achievements = Achievements.all.map { AchievementUi(it, unlocked[it.id]) },
                    hasAnyData = summaries.any { it.completed > 0 } || focus.first > 0,
                    loading = false,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setRange(days: Int) { range.value = days }
}

// =========================================================== Rewards

data class RewardsUiState(
    val level: LevelProgress = ScoringEngine.calculateLevelFromXp(0),
    val coins: Int = 0,
    val owned: Set<String> = emptySet(),
    val equippedTheme: String = "default",
    val recent: List<ScoreEventEntity> = emptyList(),
)

class RewardsViewModel(private val repo: LevelUpRepository) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = message

    val state: StateFlow<RewardsUiState> = combine(
        repo.observeTotalXp(), repo.observeCoins(), repo.observeOwnedIds(), repo.settingsFlow, repo.observeRecentEvents(25),
    ) { xp, coins, owned, settings, recent ->
        RewardsUiState(ScoringEngine.calculateLevelFromXp(xp), coins, owned, settings.accentThemeId, recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsUiState())

    fun buy(itemId: String) {
        viewModelScope.launch {
            message.value = when (repo.purchase(itemId)) {
                PurchaseResult.Success -> "Purchased!"
                PurchaseResult.AlreadyOwned -> "You already own this."
                PurchaseResult.InsufficientCoins -> "Not enough coins yet."
                PurchaseResult.NotFound -> "Item not found."
            }
        }
    }

    fun equip(themeId: String) { viewModelScope.launch { repo.equipTheme(themeId) } }
    fun consumeToast() { message.value = null }
}

// =========================================================== Settings

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    val settings: StateFlow<UserSettings> =
        repo.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY_SETTINGS)

    private val status = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = status

    fun update(transform: (UserSettings) -> UserSettings) { viewModelScope.launch { repo.updateSettings(transform) } }

    fun export(uri: Uri) {
        viewModelScope.launch {
            status.value = try {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    container.appContext.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("Could not open the selected file.")
                }
                "Export saved."
            } catch (e: Exception) {
                "Export failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            status.value = try {
                repo.clearAll()
                "All data cleared. The sample routine was restored."
            } catch (e: Exception) {
                "Could not clear data: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun consumeStatus() { status.value = null }
}

class OnboardingViewModel(private val repo: LevelUpRepository) : ViewModel() {
    fun finish(name: String, quietEnabled: Boolean, bedMinute: Int, wakeMinute: Int) {
        viewModelScope.launch {
            repo.updateSettings {
                it.copy(
                    displayName = name.trim(),
                    onboardingDone = true,
                    quietEnabled = quietEnabled,
                    quietStartMinute = bedMinute,
                    quietEndMinute = wakeMinute,
                )
            }
        }
    }
}
