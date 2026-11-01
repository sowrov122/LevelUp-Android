package com.levelup.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.data.LevelUpDatabase
import com.levelup.app.data.ScoreEventEntity
import com.levelup.app.domain.ScoreEventType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerDatabaseTest {
    private lateinit var db: LevelUpDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, LevelUpDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun event(id: String, key: String, xp: Int) = ScoreEventEntity(
        id, ScoreEventType.TASK_COMPLETED, "task:1", "2026-01-01", xp, xp, 0, 0L, key, null,
    )

    @Test fun duplicateIdempotencyKeyIsIgnored() = runBlocking {
        val dao = db.scoreEventDao()
        assertNotEquals(-1L, dao.insert(event("a", "task:1#0", 5)))
        assertEquals(-1L, dao.insert(event("b", "task:1#0", 5)))
        assertEquals(5L, dao.totalXp())
    }

    @Test fun reversalKeepsHistoryAndNetsToZero() = runBlocking {
        val dao = db.scoreEventDao()
        dao.insert(event("a", "task:1#0", 5))
        dao.insert(event("b", "task:1#1", -5))
        val net = dao.netFor(listOf("task:1")).single()
        assertEquals(0, net.xp)
        assertEquals(2, net.events)
        assertEquals(2, dao.getAll().size)
    }
}
