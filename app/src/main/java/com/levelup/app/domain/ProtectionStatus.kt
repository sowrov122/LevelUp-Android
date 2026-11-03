package com.levelup.app.domain

enum class ProtectionState { OFF, NEEDS_USAGE_ACCESS, ACTIVE, ACTIVE_STRICT }

/**
 * Honest description of distraction protection. The app only warns and deducts points when a selected
 * app opens (it cannot block apps), and only after the user has allowed Usage Access.
 */
object ProtectionStatus {
    fun state(enabled: Boolean, usageAccess: Boolean, strict: Boolean): ProtectionState = when {
        !enabled -> ProtectionState.OFF
        !usageAccess -> ProtectionState.NEEDS_USAGE_ACCESS
        strict -> ProtectionState.ACTIVE_STRICT
        else -> ProtectionState.ACTIVE
    }

    fun subtitle(state: ProtectionState): String = when (state) {
        ProtectionState.OFF -> "Off. Turn on to lose points when you open distracting apps."
        ProtectionState.NEEDS_USAGE_ACCESS -> "On, but Usage Access is not allowed yet, so nothing is being checked."
        ProtectionState.ACTIVE -> "On. Opening your selected apps costs points during focus."
        ProtectionState.ACTIVE_STRICT -> "On, strict. Opening any other app costs points during focus."
    }
}
