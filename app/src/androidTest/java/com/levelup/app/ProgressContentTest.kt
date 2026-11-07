package com.levelup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.domain.DaySummary
import com.levelup.app.domain.FocusDay
import com.levelup.app.domain.LifetimeStats
import com.levelup.app.ui.screens.ProgressContent
import com.levelup.app.ui.theme.LevelUpTheme
import com.levelup.app.ui.viewmodel.ProgressUiState
import com.levelup.app.ui.viewmodel.buildProgressState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProgressContentTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 4)

    private fun state(empty: Boolean = false, range: Int = 7): ProgressUiState =
        if (empty) {
            buildProgressState(range, 0, today, emptyList(), emptyMap(), emptyMap(), LifetimeStats(), emptyMap())
        } else {
            buildProgressState(
                range, 0, today,
                summaries = listOf(
                    DaySummary(today, 8, 6), DaySummary(today.minusDays(1), 8, 8),
                    DaySummary(today.minusDays(12), 6, 6), DaySummary(today.minusDays(11), 6, 5),
                    DaySummary(today.minusDays(10), 6, 6), DaySummary(today.minusDays(9), 6, 6), DaySummary(today.minusDays(8), 6, 6),
                ),
                pointsByDay = mapOf(today to 35, today.minusDays(1) to 120),
                focusByDay = mapOf(today to FocusDay(25, 1), today.minusDays(1) to FocusDay(65, 2)),
                stats = LifetimeStats(completedTasks = 12, perfectDays = 2, focusSessions = 3, totalFocusMinutes = 90, longestStreak = 5, level = 3, totalXp = 3420),
                unlockedAt = emptyMap(),
            )
        }

    private fun show(s: ProgressUiState, onRange: (Int) -> Unit = {}, onOffset: (Int) -> Unit = {}) =
        compose.setContent { LevelUpTheme { ProgressContent(s, onRange, onOffset) } }

    @Test fun showsSelectorsTilesAndChartsFromStoredData() {
        show(state())
        compose.onNodeWithText("Progress").assertIsDisplayed()
        compose.onNodeWithText("7 Days").assertIsDisplayed()
        compose.onNodeWithText("30 Days").assertIsDisplayed()
        compose.onNodeWithText("2 Days").assertIsDisplayed()          // current streak: today (75%) and yesterday
        compose.onNodeWithText("5 Days").assertIsDisplayed()          // best streak
        compose.onNodeWithText("1 h 30 m").assertIsDisplayed()        // 25 + 65 focus minutes in the window
        compose.onNodeWithTag("tile_sessions").assertIsDisplayed()
        compose.onNodeWithTag("completion_chart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("points_chart").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("heatmap").performScrollTo().assertIsDisplayed()
    }

    @Test fun rangeAndPeriodSelectorsReportTheChoice() {
        var range: Int? = null
        var offset: Int? = null
        show(state(), onRange = { range = it }, onOffset = { offset = it })
        compose.onNodeWithTag("range_30").performClick()
        assertEquals(30, range)
        compose.onNodeWithTag("period_pill").performClick()
        compose.onNodeWithText("22 Oct – 28 Oct 2026").performClick()
        assertEquals(1, offset)
    }

    @Test fun thirtyDayStateHasThirtyBars() {
        assertEquals(30, state(range = 30).completionBars.size)
        assertEquals(30, state(range = 30).pointsBars.size)
        show(state(range = 30))
        compose.onNodeWithTag("completion_chart").performScrollTo().assertIsDisplayed()
    }

    @Test fun emptyHistoryShowsAHelpfulMessageInsteadOfSampleData() {
        show(state(empty = true))
        compose.onNodeWithTag("progress_empty").assertIsDisplayed()
        compose.onNodeWithText("No history yet").assertIsDisplayed()
    }

    @Test fun achievementsShowRealProgressAndViewAllRevealsTheRest() {
        show(state())
        // closest-to-done milestones come first: XP collector (3,420 of 5,000) leads
        compose.onNodeWithTag("achievement_xp_collector").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3,420 / 5,000").assertIsDisplayed()
        compose.onNodeWithTag("achievement_task_master").assertDoesNotExist()      // not in the top four yet
        compose.onNodeWithText("View All").performScrollTo().performClick()
        compose.onNodeWithTag("achievement_task_master").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("12 / 50").assertIsDisplayed()                       // 12 completed tasks of 50
        compose.onNodeWithTag("achievement_streak_5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5 / 5").assertIsDisplayed()                         // 5-day streak reached, shown at its target
        compose.onNodeWithText("Show less").performScrollTo().assertIsDisplayed()
    }
}
