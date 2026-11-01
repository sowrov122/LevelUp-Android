package com.levelup.app.domain

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val minSessions: Int,
    val minMinutes: Int,
)

/** Cosmic Observatory progression, derived only from saved focus totals. */
object Observatory {
    val milestones: List<Milestone> = listOf(
        Milestone("dome", "Observatory Dome", "Complete your first focus session.", 1, 0),
        Milestone("telescope", "Main Telescope", "Complete 3 focus sessions.", 3, 0),
        Milestone("planet", "Home Planet", "Focus for 2 hours in total.", 0, 120),
        Milestone("constellation", "First Constellation", "Complete 10 focus sessions.", 10, 0),
        Milestone("module", "Engine Module", "Focus for 10 hours in total.", 0, 600),
        Milestone("starship", "Starship Hull", "Complete 25 sessions and 25 hours of focus.", 25, 1500),
    )

    fun isUnlocked(m: Milestone, sessions: Int, minutes: Int): Boolean =
        sessions >= m.minSessions && minutes >= m.minMinutes && (m.minSessions > 0 || m.minMinutes > 0)

    fun unlockedIds(sessions: Int, minutes: Int): Set<String> =
        milestones.filter { isUnlocked(it, sessions, minutes) }.map { it.id }.toSet()

    /** Each session adds a couple of stars; every 15 focused minutes adds one more (max 80). */
    fun starCount(sessions: Int, minutes: Int): Int = (sessions * 2 + minutes / 15).coerceIn(0, 80)
}
