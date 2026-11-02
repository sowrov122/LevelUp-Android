package com.levelup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.domain.Category
import com.levelup.app.domain.Priority
import com.levelup.app.domain.ScoringEngine
import com.levelup.app.domain.Streaks
import com.levelup.app.domain.TaskStatus
import com.levelup.app.ui.screens.TodayContent
import com.levelup.app.ui.theme.LevelUpTheme
import com.levelup.app.ui.viewmodel.TaskRowUi
import com.levelup.app.ui.viewmodel.TodayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TodayContentTest {
    @get:Rule val compose = createComposeRule()

    private fun row(id: String, title: String, status: TaskStatus, xp: Int, inProgress: Boolean = false) = TaskRowUi(
        instanceId = "d:$id", taskId = id, title = title, category = Category.STUDY,
        timeLabel = "10:00–12:00", durationMinutes = 120, status = status, inProgress = inProgress,
        priority = Priority.NORMAL, pointsAwarded = if (status == TaskStatus.COMPLETED) xp else 0, xpReward = xp,
    )

    private val state = TodayUiState(
        loading = false,
        date = LocalDate.of(2026, 11, 2),
        name = "Sourav",
        tasks = listOf(
            row("a", "Academic Study", TaskStatus.PENDING, 20, inProgress = true),
            row("b", "Workout", TaskStatus.COMPLETED, 10),
        ),
        completed = 1,
        total = 2,
        pointsToday = 10,
        level = ScoringEngine.calculateLevelFromXp(200),   // level 2, 75 / 150 XP
        coins = 855,
        streaks = Streaks(3, 5),
        focusMinutes = 25,
    )

    private fun show(onToggle: (String, Boolean) -> Unit = { _, _ -> }, onAdd: () -> Unit = {}) {
        compose.setContent {
            LevelUpTheme { TodayContent(state, null, onStartFocus = {}, onAddTask = onAdd, onEditRoutine = {}, onToggle = onToggle, hour = 9) }
        }
    }

    @Test fun showsGreetingLevelAndSummaryFromState() {
        show()
        compose.onNodeWithText("Good morning,").assertIsDisplayed()
        compose.onNodeWithText("Sourav").assertIsDisplayed()
        compose.onNodeWithText("Level 2").assertIsDisplayed()
        compose.onNodeWithText("Cadet").assertIsDisplayed()
        compose.onNodeWithText("75 / 150 XP").assertIsDisplayed()
        compose.onNodeWithText("75 XP to Lv. 3").assertIsDisplayed()
        compose.onNodeWithText("1/2").assertIsDisplayed()
        compose.onNodeWithText("855").assertIsDisplayed()
    }

    @Test fun listsTasksWithTimeDurationAndXp() {
        show()
        compose.onNodeWithTag("today_list").performScrollToNode(hasText("Academic Study"))
        compose.onNodeWithText("Academic Study").assertIsDisplayed()
        compose.onNodeWithText("+20 XP").assertIsDisplayed()
        compose.onNodeWithText("In progress").assertIsDisplayed()
        compose.onNodeWithTag("today_list").performScrollToNode(hasText("Workout"))
        compose.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test fun tickingATaskReportsItsInstanceId() {
        var got: Pair<String, Boolean>? = null
        show(onToggle = { id, v -> got = id to v })
        compose.onNodeWithTag("today_list").performScrollToNode(hasText("Academic Study"))
        compose.onNodeWithContentDescription("Mark Academic Study done").performClick()
        assertEquals("d:a" to true, got)
    }

    @Test fun quickActionsAreWired() {
        var added = false
        show(onAdd = { added = true })
        compose.onNodeWithText("Add Task").performClick()
        assertTrue(added)
    }
}
