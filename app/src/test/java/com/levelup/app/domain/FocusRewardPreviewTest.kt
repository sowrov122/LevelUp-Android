package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** The reward preview on the Focus screen uses the same function that grants the real reward. */
class FocusRewardPreviewTest {
    @Test fun previewMatchesRealRewardForPresets() {
        listOf(15 to Reward(3, 15, 1), 25 to Reward(5, 25, 2), 45 to Reward(9, 45, 4), 60 to Reward(12, 60, 6)).forEach { (m, expected) ->
            assertEquals(expected, ScoringEngine.calculateFocusReward(m * 60L, 120, 0).reward)
        }
    }

    @Test fun previewRespectsTheDailyCap() {
        val p = ScoringEngine.calculateFocusReward(60 * 60L, 120, 90)
        assertEquals(30, p.rewardedMinutes)
        assertEquals(Reward(6, 30, 3), p.reward)
        assertEquals(Reward.ZERO, ScoringEngine.calculateFocusReward(25 * 60L, 120, 120).reward)
    }
}
