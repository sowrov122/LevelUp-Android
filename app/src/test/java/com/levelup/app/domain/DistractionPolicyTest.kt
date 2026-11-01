package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DistractionPolicyTest {
    private fun decide(
        eventMs: Long = 100_000,
        lastHandled: Long? = null,
        lastPenalty: Long? = null,
        penalty: Int = 5,
        cooldown: Int = 300,
        enabled: Boolean = true,
        focus: Boolean = true,
        selected: Boolean = true,
        essential: Boolean = false,
        pausedUntil: Long = 0,
    ) = DistractionPolicy.calculate(eventMs, lastHandled, lastPenalty, penalty, cooldown, enabled, focus, selected, essential, pausedUntil)

    @Test fun penalizesFirstLaunch() {
        assertEquals(PenaltyDecision(InterruptionDecision.PENALIZE, 5), decide())
    }

    @Test fun ignoresWhenNotProtected() {
        assertEquals(InterruptionDecision.IGNORE, decide(enabled = false).decision)
        assertEquals(InterruptionDecision.IGNORE, decide(focus = false).decision)
        assertEquals(InterruptionDecision.IGNORE, decide(selected = false).decision)
        assertEquals(InterruptionDecision.IGNORE, decide(essential = true).decision)
    }

    @Test fun duplicateEventIsNotPenalizedTwice() {
        assertEquals(InterruptionDecision.DUPLICATE, decide(eventMs = 100_000, lastHandled = 100_000).decision)
        assertEquals(InterruptionDecision.DUPLICATE, decide(eventMs = 99_000, lastHandled = 100_000).decision)
    }

    @Test fun cooldownOnlyLogs() {
        assertEquals(InterruptionDecision.LOG_ONLY, decide(eventMs = 200_000, lastHandled = 100_000, lastPenalty = 100_000).decision)
        // after the cooldown a new launch is penalised again
        assertEquals(
            InterruptionDecision.PENALIZE,
            decide(eventMs = 100_000 + 300_000, lastHandled = 100_000, lastPenalty = 100_000).decision,
        )
    }

    @Test fun pausedProtectionNeverPenalizes() {
        assertEquals(InterruptionDecision.IGNORE, decide(eventMs = 100_000, pausedUntil = 150_000).decision)
        assertEquals(InterruptionDecision.PENALIZE, decide(eventMs = 150_000, pausedUntil = 150_000).decision)
    }

    @Test fun zeroPenaltyJustLogs() {
        assertEquals(PenaltyDecision(InterruptionDecision.LOG_ONLY, 0), decide(penalty = 0))
    }
}
