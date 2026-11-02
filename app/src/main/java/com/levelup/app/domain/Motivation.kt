package com.levelup.app.domain

/** Picks the message for the Today card from the real state of the day. Pure and deterministic. */
object Motivation {
    fun message(completed: Int, total: Int, streakDays: Int, focusMinutes: Int): String = when {
        total <= 0 -> "Nothing is scheduled today. Add a task, or enjoy the rest."
        completed >= total ->
            if (streakDays >= 2) "Every task done. Day $streakDays of your streak is secured."
            else "Every task done. This is what a streak is made of."
        completed == 0 ->
            if (focusMinutes > 0) "You already focused $focusMinutes min. Now pick the first task."
            else "A fresh day. Start with the next task, not the whole day."
        completed * 2 < total ->
            "$completed of $total done. Keep the momentum with the next one."
        else -> {
            val left = total - completed
            val tail = if (streakDays >= 2) " Protect your $streakDays-day streak." else ""
            "Over half done, $left to go.$tail"
        }
    }
}
