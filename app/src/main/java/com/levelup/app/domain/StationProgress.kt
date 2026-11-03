package com.levelup.app.domain

/** Station levels, driven only by the number of completed focus sessions. Pure and deterministic. */
object StationProgress {
    /** Completed sessions needed for Lv. 1 .. Lv. 6. */
    val thresholds: List<Int> = listOf(0, 1, 3, 6, 10, 15)
    val maxLevel: Int get() = thresholds.size

    fun level(sessions: Int): Int = thresholds.indexOfLast { sessions >= it.coerceAtLeast(0) }.coerceAtLeast(0) + 1

    /** Sessions needed for the next level, or null at the top level. */
    fun nextThreshold(sessions: Int): Int? = thresholds.firstOrNull { it > sessions }

    /** 0..1 progress from the current level's threshold to the next; 1 at the top level. */
    fun fractionToNext(sessions: Int): Float {
        val next = nextThreshold(sessions) ?: return 1f
        val current = thresholds[level(sessions) - 1]
        return ((sessions - current).toFloat() / (next - current)).coerceIn(0f, 1f)
    }

    /** How much one more session moves the bar, in whole percent (0 at the top level). */
    fun percentPerSession(sessions: Int): Int {
        val next = nextThreshold(sessions) ?: return 0
        val current = thresholds[level(sessions) - 1]
        return 100 / (next - current)
    }

    /** Index into the six station pictures for the idle scene. */
    fun stageIndex(sessions: Int): Int = level(sessions) - 1
}
