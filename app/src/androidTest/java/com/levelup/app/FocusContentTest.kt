package com.levelup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.domain.Category
import com.levelup.app.ui.screens.FocusIdleContent
import com.levelup.app.ui.screens.FocusRunningContent
import com.levelup.app.ui.theme.LevelUpTheme
import com.levelup.app.ui.viewmodel.ActiveFocusUi
import com.levelup.app.ui.viewmodel.FocusUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusContentTest {
    @get:Rule val compose = createComposeRule()

    private val idleState = FocusUiState(sessions = 6, minutes = 150, protectionOn = false, reducedMotion = true, rewardedMinutesToday = 0, dailyCapMinutes = 120)

    private fun showIdle(
        state: FocusUiState = idleState,
        minutes: Int = 25,
        usageAccess: Boolean = true,
        onMinutes: (Int) -> Unit = {},
        onStart: () -> Unit = {},
        onProtection: (Boolean) -> Unit = {},
        onBlocker: () -> Unit = {},
    ) = compose.setContent {
        LevelUpTheme {
            FocusIdleContent(
                state, "", {}, minutes, onMinutes, null, {}, usageAccess, false, {}, onStart, onProtection, onBlocker,
                {}, {}, {}, {}, {},
            )
        }
    }

    @Test fun idleShowsHeaderTimerAndPresets() {
        showIdle()
        compose.onNodeWithText("Focus Mode").assertIsDisplayed()
        compose.onNodeWithTag("focus_time").assertIsDisplayed()
        compose.onNodeWithText("25:00").assertIsDisplayed()
        listOf("15 min", "25 min", "45 min", "60 min").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test fun presetsAndStartAreWired() {
        var picked: Int? = null
        var started = false
        showIdle(onMinutes = { picked = it }, onStart = { started = true })
        compose.onNodeWithText("45 min").performClick()
        assertEquals(45, picked)
        compose.onNodeWithTag("start_focus").performClick()
        assertTrue(started)
    }

    @Test fun stationCardReflectsRealFocusedMinutesAndRewardPreview() {
        showIdle()
        compose.onNodeWithTag("station_level").performScrollTo()
        compose.onNodeWithTag("station_level").assertIsDisplayed()
        compose.onNodeWithText("Lv. 5").assertIsDisplayed()                 // 150 focused minutes = level 5
        compose.onNodeWithText("150 / 160 min").assertIsDisplayed()
        compose.onNodeWithText("+25 XP").assertIsDisplayed()
        compose.onNodeWithText("+25 min").assertIsDisplayed()
        compose.onNodeWithText("+41%").assertIsDisplayed()                  // 25 of the 60 minutes between level 5 and 6
    }

    @Test fun rewardNoteWarnsWhenTheDailyCapIsReached() {
        showIdle(state = idleState.copy(rewardedMinutesToday = 120))
        compose.onNodeWithTag("reward_note").performScrollTo()
        compose.onNodeWithTag("reward_note").assertIsDisplayed()
    }

    @Test fun protectionToggleAndHonestStatusText() {
        var toggled: Boolean? = null
        showIdle(onProtection = { toggled = it })
        compose.onNodeWithTag("protection_switch").performScrollTo()
        compose.onNodeWithText("Off. Turn on to lose points when you open distracting apps.").assertIsDisplayed()
        compose.onNodeWithTag("protection_switch").performClick()
        assertEquals(true, toggled)
    }

    @Test fun protectionWithoutUsageAccessSaysNothingIsChecked() {
        showIdle(state = idleState.copy(protectionOn = true), usageAccess = false)
        compose.onNodeWithTag("protection_switch").performScrollTo()
        compose.onNodeWithText("On, but Usage Access is not allowed yet, so nothing is being checked.").assertIsDisplayed()
    }

    @Test fun manageAppsOpensTheBlockerSettings() {
        var opened = false
        showIdle(onBlocker = { opened = true })
        compose.onNodeWithText("Manage Apps").performScrollTo()
        compose.onNodeWithText("Manage Apps").performClick()
        assertTrue(opened)
    }

    @Test fun runningShowsCountdownAndControls() {
        var paused = false
        val active = ActiveFocusUi("s1", "German Study", 1500, 300, running = true, interrupted = false)
        compose.setContent {
            LevelUpTheme {
                FocusRunningContent(
                    FocusUiState(active = active, reducedMotion = true), active, Category.STUDY, true,
                    onPause = { paused = true }, onResume = {}, onFinish = {}, onCancel = {},
                )
            }
        }
        compose.onNodeWithText("German Study").assertIsDisplayed()
        compose.onNodeWithText("20:00").assertIsDisplayed()            // 1500 - 300 seconds
        compose.onNodeWithContentDescription("Pause").performClick()
        assertTrue(paused)
    }

    @Test fun pausedSessionOffersResume() {
        var resumed = false
        val active = ActiveFocusUi("s1", "Deep work", 1500, 300, running = false, interrupted = true)
        compose.setContent {
            LevelUpTheme {
                FocusRunningContent(
                    FocusUiState(active = active, reducedMotion = true), active, null, true,
                    onPause = {}, onResume = { resumed = true }, onFinish = {}, onCancel = {},
                )
            }
        }
        compose.onNodeWithText("Paused").assertIsDisplayed()
        compose.onNodeWithText("The phone restarted, so the timer was paused instead of guessing the time.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Resume").performClick()
        assertTrue(resumed)
    }
}
