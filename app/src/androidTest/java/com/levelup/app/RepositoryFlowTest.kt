package com.levelup.app

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.SettingsStore
import com.levelup.app.domain.TaskDraft
import com.levelup.app.domain.TaskStatus
import com.levelup.app.domain.TimeUtils
import com.levelup.app.notifications.ReminderManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real repository against an in-memory Room database. */
@RunWith(AndroidJUnit4::class)
class RepositoryFlowTest {
    private lateinit var db: LevelUpDatabase
    private lateinit var repo: LevelUpRepository
    private var wall = 1_700_000_000_000L
    private var elapsed = 1_000_000L

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, LevelUpDatabase::class.java).allowMainThreadQueries().build()
        val settings = SettingsStore(ctx)
        repo = LevelUpRepository(
            db, settings, ReminderManager(ctx, db, settings),
            nowMillis = { wall },
            elapsedRealtime = { elapsed },
        )
    }

    @After fun tearDown() = db.close()

    private fun draft(title: String = "Study", id: String? = null) = TaskDraft(
        id = id, title = title, startMinute = 600, endMinute = 660,
        repeatDaysMask = TimeUtils.ALL_DAYS, reminderOffsetMinutes = null,
    )

    @Test fun togglingCompletionAwardsExactlyOnceAndKeepsAuditTrail() = runBlocking {
        repo.saveTask(draft())
        val today = repo.today().toString()
        val instance = db.taskInstanceDao().getForDate(today).single()

        repo.setCompleted(instance.id, true)
        repo.setCompleted(instance.id, true)   // repeated tap is a no-op
        repo.setCompleted(instance.id, false)  // undo
        repo.setCompleted(instance.id, true)   // redo

        // 60 min task = 10 XP, plus the "every task done" bonus = 30 XP. Never doubled.
        assertEquals(40L, db.scoreEventDao().totalXp())
        // complete(task+bonus) + undo(task+bonus) + redo(task+bonus) = 6 rows, plus the one-time
        // "First Step" achievement coin event = 7 visible ledger rows. Nothing was overwritten.
        assertEquals(7, db.scoreEventDao().getAll().size)
        assertEquals(TaskStatus.COMPLETED, db.taskInstanceDao().get(instance.id)?.status)
    }

    @Test fun editingRoutineKeepsCompletedHistory() = runBlocking {
        repo.saveTask(draft("Study"))
        val taskId = db.routineTaskDao().getAll().single().id
        val today = repo.today().toString()
        val instance = db.taskInstanceDao().getForDate(today).single()
        repo.setCompleted(instance.id, true)

        repo.saveTask(draft("Renamed", taskId))
        repo.ensureDay(repo.today())

        val after = db.taskInstanceDao().get(instance.id)
        assertEquals("Study", after?.title)
        assertEquals(TaskStatus.COMPLETED, after?.status)

        repo.deleteTask(taskId)
        assertNotNull(db.taskInstanceDao().get(instance.id)) // completed history survives deletion
    }

    @Test fun focusRewardIsGrantedOnceAndCancelEarnsNothing() = runBlocking {
        val id = repo.startFocus("Deep work", null, 1500)!!
        assertNull(repo.startFocus("Second", null, 600)) // only one active session

        wall += 1500_000; elapsed += 1500_000
        val first = repo.finishFocus(id)
        assertEquals(25, first?.reward?.xp)
        assertNull(repo.finishFocus(id)) // cannot reward twice
        assertEquals(25L, db.scoreEventDao().totalXp())

        val id2 = repo.startFocus("Abandoned", null, 1500)!!
        wall += 600_000; elapsed += 600_000
        repo.cancelFocus(id2)
        assertEquals(25L, db.scoreEventDao().totalXp()) // unchanged
    }

    private fun timed(title: String, startH: Int, id: String? = null) = TaskDraft(
        id = id, title = title, startMinute = startH * 60, endMinute = startH * 60 + 60,
        repeatDaysMask = TimeUtils.ALL_DAYS, reminderOffsetMinutes = null,
    )

    @Test fun orderingByHandAndByTimeAndChronologicalDay() = runBlocking {
        repo.saveTask(timed("Late", 12))
        repo.saveTask(timed("Early", 8))
        repo.saveTask(timed("Middle", 10))
        suspend fun titles() = db.routineTaskDao().getAll().map { it.title }

        assertEquals(listOf("Late", "Early", "Middle"), titles())          // creation order
        val middle = db.routineTaskDao().getAll().first { it.title == "Middle" }
        repo.moveTask(middle.id, -1)
        assertEquals(listOf("Late", "Middle", "Early"), titles())          // manual reorder
        repo.sortTasksByTime()
        assertEquals(listOf("Early", "Middle", "Late"), titles())          // sorted by time

        // the daily timeline is chronological regardless of the manual order
        val today = repo.today().toString()
        assertEquals(listOf("Early", "Middle", "Late"), db.taskInstanceDao().getForDate(today).map { it.title })
    }

    @Test fun editingATaskMovesItsPendingInstanceAndKeepsRemindersOptional() = runBlocking {
        repo.saveTask(timed("Study", 9))
        val task = db.routineTaskDao().getAll().single()
        repo.saveTask(timed("Study", 14, task.id).copy(reminderOffsetMinutes = 10))
        val today = repo.today().toString()
        val instance = db.taskInstanceDao().getForDate(today).single()
        assertEquals(14 * 60, instance.startMinute)
        assertEquals(10, db.routineTaskDao().get(task.id)?.reminderOffsetMinutes)
    }

    @Test fun deletingATaskRemovesItsPendingInstance() = runBlocking {
        repo.saveTask(timed("Temp", 9))
        val task = db.routineTaskDao().getAll().single()
        repo.deleteTask(task.id)
        assertEquals(0, db.routineTaskDao().count())
        assertEquals(0, db.taskInstanceDao().getForDate(repo.today().toString()).size)
    }
}
