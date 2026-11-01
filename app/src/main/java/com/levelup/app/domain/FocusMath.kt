package com.levelup.app.domain

/**
 * Elapsed-time maths for the focus timer. The UI countdown is never the source of truth;
 * these functions derive elapsed time from persisted timestamps.
 */
object FocusMath {
    private const val CLOCK_DRIFT_TOLERANCE_MS = 120_000L

    /**
     * SystemClock.elapsedRealtime() restarts at boot. If it went backwards, or the wall clock advanced
     * far more than uptime did, the device was off: do not invent focused time.
     */
    fun rebootSuspected(resumedAtElapsed: Long, resumedAtWall: Long, nowElapsed: Long, nowWall: Long): Boolean {
        if (resumedAtElapsed < 0 || nowElapsed < resumedAtElapsed) return true
        val monotonicMs = nowElapsed - resumedAtElapsed
        val wallMs = nowWall - resumedAtWall
        return wallMs - monotonicMs > CLOCK_DRIFT_TOLERANCE_MS
    }

    fun elapsedSeconds(
        status: FocusStatus,
        accumulatedSeconds: Long,
        resumedAtElapsed: Long,
        resumedAtWall: Long,
        nowElapsed: Long,
        nowWall: Long,
    ): Long {
        if (status != FocusStatus.RUNNING) return accumulatedSeconds
        if (rebootSuspected(resumedAtElapsed, resumedAtWall, nowElapsed, nowWall)) return accumulatedSeconds
        return accumulatedSeconds + (nowElapsed - resumedAtElapsed) / 1000
    }
}
