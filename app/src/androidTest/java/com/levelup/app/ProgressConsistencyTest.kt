package com.levelup.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.SettingsStore
import com.levelup.app.domain.Achievements
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TimeUtils
import com.levelup.app.notifications.ReminderManager
import com.levelup.app.ui.viewmodel.buildProgressState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Progress numbers must equal what the task history, focus sessions and reward ledger actually contain. */
@RunWith(AndroidJUnit4::class)
class ProgressConsistencyTest {
    private lateinit var db: LevelUpDatabase
    private lateinit var repo: LevelUpRepository
    private var wall = 1_700_000_000_000L
    private var elapsed = 1_000_000L

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, LevelUpDatabase::class.java).allowMainThreadQueries().build()
        val settings = SettingsStore(ctx)
        repo = LevelUpRepository(db, settings, ReminderManager(ctx, db, settings), nowMillis = { wall }, elapsedRealtime = { elapsed })
    }

    @After fun tearDown() = db.close()

    private fun task(title: String, startH: Int) = TaskDraft(
        title = title, startMinute = startH * 60, endMinute = startH * 60 + 60,
        repeatDaysMask = TimeUtils.ALL_DAYS, reminderOffsetMinutes = null,
    )

    private suspend fun screen() = repo.today().let { today ->
        buildProgressState(
            rangeDays = 7, offset = 0, today = today,
            summaries = repo.observeSummaries().first(),
            pointsByDay = repo.observePointsByDay(today.minusDays(40)).first(),
            focusByDay = repo.observeFocusByDay(today.minusDays(40)).first(),
            stats = repo.observeLifetimeStats().first(),
            unlockedAt = repo.observeAchievements().first().associate { it.id to it.unlockedAt },
        )
    }

    @Test fun chartsAndTotalsMatchHistoryAndLedger() = runBlocking {
        repo.saveTask(task("Study", 9))
        repo.saveTask(task("Workout", 11))
        val today = repo.today()
        val first = db.taskInstanceDao().getForDate(today.toString()).first()
        repo.setCompleted(first.id, true)                                   // 1 of 2 tasks, 60 min = 10 points / 10 XP

        val id = repo.startFocus("Deep work", null, 1500)!!
        wall += 1500_000; elapsed += 1500_000
        repo.finishFocus(id)                                                // 25 min = 5 points / 25 XP

        val s = screen()
        val ledgerToday = db.scoreEventDao().getAll().filter { it.localDate == today.toString() }

        // completion chart = task history
        assertEquals(50, s.completionBars.last().value)
        // points chart = ledger (never below zero), including task, focus and no hidden extras
        assertEquals(ledgerToday.sumOf { it.pointsDelta }.coerceAtLeast(0), s.pointsBars.last().value)
        assertEquals(15, s.pointsBars.last().value)
        // focus tiles = focus sessions
        assertEquals(25, s.focusMinutes)
        assertEquals(1, s.focusSessions)
        // XP: 10 + 25 earned, plus the XP of the achievements that unlocked (first task, first focus)
        val achievementXp = Achievements.byId("first_task")!!.xp + Achievements.byId("first_focus")!!.xp
        assertEquals(35L + achievementXp, db.scoreEventDao().totalXp())
        assertEquals(db.scoreEventDao().totalXp(), repo.observeLifetimeStats().first().totalXp)
        // achievements shown as completed are exactly the ones recorded in the database
        val shownCompleted = s.achievements.filter { it.completed }.map { it.def.id }.toSet()
        assertEquals(db.achievementDao().ids().toSet(), shownCompleted)
        // heat map: today is 50% = medium activity
        assertEquals(com.levelup.app.domain.HeatLevel.MEDIUM, s.heatWeeks.last().days.first { it.isToday }.level)
    }

    @Test fun undoingATaskUpdatesTheChartsAndTheLedgerStaysAuditable() = runBlocking {
        repo.saveTask(task("Study", 9))
        val today = repo.today()
        val inst = db.taskInstanceDao().getForDate(today.toString()).single()
        repo.setCompleted(inst.id, true)
        assertEquals(100, screen().completionBars.last().value)
        repo.setCompleted(inst.id, false)
        val after = screen()
        assertEquals(0, after.completionBars.last().value)
        assertEquals(0, after.pointsBars.last().value)                      // reversal removes the points again
        assertEquals(0, after.focusMinutes)
        assertEquals(true, db.scoreEventDao().getAll().any { it.pointsDelta < 0 })   // the reversal is visible in the ledger
    }
}
