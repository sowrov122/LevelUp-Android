package com.levelup.app.domain

/**
 * Station levels, driven only by total completed focus minutes (not by how many sessions it took).
 * Reaching a level needs: Lv.2 5 min, Lv.3 10 min, Lv.4 40 min (next 30), Lv.5 100 min (next 60), Lv.6 160 min (next 60).
 * Pure and deterministic.
 */
object StationProgress {
    val thresholdsMinutes: List<Int> = listOf(0, 5, 10, 40, 100, 160)
    val maxLevel: Int get() = thresholdsMinutes.size

    fun level(totalMinutes: Int): Int = thresholdsMinutes.indexOfLast { totalMinutes >= it }.coerceAtLeast(0) + 1

    /** Focus minutes needed for the next level, or null at the top level. */
    fun nextThreshold(totalMinutes: Int): Int? = thresholdsMinutes.firstOrNull { it > totalMinutes }

    /** 0..1 progress from the current level's threshold to the next; 1 at the top level. */
    fun fractionToNext(totalMinutes: Int): Float {
        val next = nextThreshold(totalMinutes) ?: return 1f
        val current = thresholdsMinutes[level(totalMinutes) - 1]
        return ((totalMinutes - current).toFloat() / (next - current)).coerceIn(0f, 1f)
    }

    /** Whole percent of the current level's gap that a session of [sessionMinutes] would add, capped at 100. */
    fun percentForSession(totalMinutes: Int, sessionMinutes: Int): Int {
        val next = nextThreshold(totalMinutes) ?: return 0
        val current = thresholdsMinutes[level(totalMinutes) - 1]
        return (sessionMinutes * 100 / (next - current)).coerceIn(0, 100)
    }

    /** Index (0..5) into the six station pictures. */
    fun stageIndex(totalMinutes: Int): Int = level(totalMinutes) - 1

    /**
     * While a session runs the station is drawn for "everything focused before + the minutes of this session so far",
     * so a new session starts at the level the user has already earned and keeps growing from there.
     */
    fun runningStageIndex(completedMinutes: Int, elapsedSeconds: Long): Int =
        stageIndex(completedMinutes + (elapsedSeconds / 60).toInt())
}
