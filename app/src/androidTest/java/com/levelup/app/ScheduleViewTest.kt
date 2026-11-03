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
import com.levelup.app.domain.CategoryGroup
import com.levelup.app.domain.Priority
import com.levelup.app.domain.TaskStatus
import com.levelup.app.ui.screens.ScheduleView
import com.levelup.app.ui.theme.LevelUpTheme
import com.levelup.app.ui.viewmodel.RoutineDayUi
import com.levelup.app.ui.viewmodel.TaskRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ScheduleViewTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 2) // a Monday

    private fun row(id: String, title: String, cat: Category, start: Int, minutes: Int, status: TaskStatus, active: Boolean, xp: Int) =
        TaskRowUi(
            instanceId = "i$id", taskId = "t$id", title = title, category = cat, timeLabel = "", durationMinutes = minutes,
            status = status, inProgress = active, priority = Priority.NORMAL,
            pointsAwarded = if (status == TaskStatus.COMPLETED) xp else 0, xpReward = xp, startMinute = start,
        )

    private fun day(canToggle: Boolean = true) = RoutineDayUi(
        today = today, selected = today, weekDays = (0L..6L).map { today.plusDays(it) },
        rows = listOf(
            row("1", "Walking & Garden Care", Category.WALKING, 6 * 60, 30, TaskStatus.COMPLETED, false, 20),
            row("2", "AI Coding", Category.AI_CODING, 7 * 60, 60, TaskStatus.PENDING, true, 10),
            row("3", "Workout", Category.FITNESS, 10 * 60 + 30, 60, TaskStatus.PENDING, false, 50),
            row("4", "Breakfast", Category.MEALS, 5 * 60, 30, TaskStatus.MISSED, false, 10),
        ),
        completed = 1, total = 4, canToggle = canToggle, loading = false, points = 20, streak = 3,
    )

    private fun show(
        day: RoutineDayUi = day(),
        filter: CategoryGroup = CategoryGroup.ALL,
        onFilter: (CategoryGroup) -> Unit = {},
        onToggle: (String, Boolean) -> Unit = { _, _ -> },
        onEdit: (String) -> Unit = {},
        onDelete: (String) -> Unit = {},
        onDuplicate: (String) -> Unit = {},
    ) = compose.setContent {
        LevelUpTheme {
            ScheduleView(day, filter, false, onFilter, {}, onToggle, onEdit, onDuplicate, onDelete, {})
        }
    }

    private fun scrollTo(text: String) = compose.onNodeWithTag("schedule_list").performScrollToNode(hasText(text))

    @Test fun showsEveryStateWithTimesDurationAndXp() {
        show()
        scrollTo("Walking & Garden Care")
        compose.onNodeWithText("Completed").assertIsDisplayed()
        compose.onNodeWithText("06:00 AM").assertIsDisplayed()
        compose.onNodeWithText("– 06:30 AM").assertIsDisplayed()
        compose.onNodeWithText("+20 XP").assertIsDisplayed()
        scrollTo("AI Coding")
        compose.onNodeWithText("Active Now").assertIsDisplayed()
        scrollTo("Workout")
        compose.onNodeWithText("Upcoming").assertIsDisplayed()
        compose.onNodeWithText("+50 XP").assertIsDisplayed()
        scrollTo("Breakfast")
        compose.onNodeWithText("Missed").assertIsDisplayed()
    }

    @Test fun summaryShowsRealCounts() {
        show()
        compose.onNodeWithText("1 of 4 tasks completed").assertIsDisplayed()
        compose.onNodeWithText("Today's Routine").assertIsDisplayed()
    }

    @Test fun categoryFilterHidesOtherTasks() {
        show(filter = CategoryGroup.HEALTH)
        scrollTo("Workout")
        compose.onNodeWithText("Workout").assertIsDisplayed()
        compose.onNodeWithText("AI Coding").assertDoesNotExist()
    }

    @Test fun tappingAFilterChipReportsTheGroup() {
        var chosen: CategoryGroup? = null
        show(onFilter = { chosen = it })
        compose.onNodeWithText("Study").performClick()
        assertEquals(CategoryGroup.STUDY, chosen)
    }

    @Test fun markerTogglesCompletionOnlyWhenAllowed() {
        var got: Pair<String, Boolean>? = null
        show(onToggle = { id, v -> got = id to v })
        scrollTo("Workout")
        compose.onNodeWithContentDescription("Mark Workout done").performClick()
        assertEquals("i3" to true, got)
        scrollTo("Walking & Garden Care")
        compose.onNodeWithContentDescription("Mark Walking & Garden Care not done").performClick()
        assertEquals("i1" to false, got)
    }

    @Test fun pastOrFutureDaysCannotBeTicked() {
        show(day = day(canToggle = false))
        scrollTo("Workout")
        compose.onNodeWithContentDescription("Mark Workout done").assertDoesNotExist()
    }

    @Test fun overflowMenuEditsAndDeletesTheRightTask() {
        var edited: String? = null
        var deleted: String? = null
        show(onEdit = { edited = it }, onDelete = { deleted = it })
        scrollTo("Workout")
        compose.onNodeWithContentDescription("More actions for Workout").performClick()
        compose.onNodeWithText("Edit").performClick()
        assertEquals("t3", edited)
        compose.onNodeWithContentDescription("More actions for Workout").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertTrue(deleted == "t3")
    }
}
