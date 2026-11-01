package com.levelup.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.theme.LevelUpTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmDialogShowsMessageAndConfirms() {
        var confirmed = false
        compose.setContent {
            LevelUpTheme {
                ConfirmDialog("Finish early?", "Only focused minutes are rewarded.", "Finish", { confirmed = true }, {})
            }
        }
        compose.onNodeWithText("Finish early?").assertIsDisplayed()
        compose.onNodeWithText("Finish").performClick()
        assertTrue(confirmed)
    }

    @Test fun cancelDismissesWithoutConfirming() {
        var confirmed = false
        var dismissed = false
        compose.setContent {
            LevelUpTheme { ConfirmDialog("Clear all data?", "This is permanent.", "Delete", { confirmed = true }, { dismissed = true }) }
        }
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed && !confirmed)
    }
}
