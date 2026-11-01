package com.levelup.app.domain

enum class InterruptionDecision { IGNORE, DUPLICATE, PENALIZE, LOG_ONLY }

data class PenaltyDecision(val decision: InterruptionDecision, val penaltyPoints: Int = 0)

/**
 * Pure rule for what happens when a user-selected app reaches the foreground.
 * XP is never touched: penalties only reduce *daily points*, so one slip cannot erase long-term progress.
 */
object DistractionPolicy {
    fun calculate(
        eventTimeMs: Long,
        lastHandledEventMs: Long?,
        lastPenaltyMs: Long?,
        penaltyPoints: Int,
        cooldownSeconds: Int,
        protectionEnabled: Boolean,
        focusRunning: Boolean,
        isSelectedApp: Boolean,
        isEssential: Boolean,
        pausedUntilMs: Long,
    ): PenaltyDecision {
        if (!protectionEnabled || !focusRunning || !isSelectedApp || isEssential) return PenaltyDecision(InterruptionDecision.IGNORE)
        // The same foreground event can be returned by overlapping polling windows.
        if (lastHandledEventMs != null && eventTimeMs <= lastHandledEventMs) return PenaltyDecision(InterruptionDecision.DUPLICATE)
        if (eventTimeMs < pausedUntilMs) return PenaltyDecision(InterruptionDecision.IGNORE)
        val cooldownMs = cooldownSeconds.coerceAtLeast(0) * 1000L
        if (lastPenaltyMs != null && eventTimeMs - lastPenaltyMs < cooldownMs) return PenaltyDecision(InterruptionDecision.LOG_ONLY)
        val points = penaltyPoints.coerceAtLeast(0)
        return if (points == 0) PenaltyDecision(InterruptionDecision.LOG_ONLY) else PenaltyDecision(InterruptionDecision.PENALIZE, points)
    }
}
