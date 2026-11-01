package com.levelup.app.domain

/** Features that could become premium later. Core daily-use features are deliberately NOT in this list. */
enum class PremiumFeature(val title: String, val description: String, val gatedInThisVersion: Boolean) {
    MULTIPLE_ROUTINES("Multiple routine templates", "Keep several routines and switch between them.", false),
    ADVANCED_ANALYTICS("Advanced analytics", "Longer history ranges and deeper breakdowns.", false),
    EXTRA_ENVIRONMENTS("More observatory environments", "Extra scenes and cosmetic collections.", false),
    CUSTOM_BLOCK_SCHEDULES("Custom protection schedules", "Schedule distraction protection outside focus sessions.", false),
    ENCRYPTED_BACKUP("Backup and encrypted export", "Opt-in backup of your data.", false),
    WIDGETS("Home-screen widgets", "Glanceable progress and next task.", false),
}

/** Always free: these must keep working with no purchase. */
val CORE_FREE_FEATURES = listOf(
    "Routine scheduling and editing",
    "Task reminders",
    "Points, XP, levels and streaks",
    "Focus timer and Cosmic Observatory",
    "Progress history, achievements and coin rewards",
    "Selected-app distraction protection",
    "Data export and Clear Data",
)

enum class EntitlementState { FREE, PREMIUM }

data class Entitlement(val state: EntitlementState, val billingAvailable: Boolean)

object PremiumCatalog {
    /** True when the user may use the feature. Nothing is locked while it is not purchasable. */
    fun isAccessible(feature: PremiumFeature, entitlement: Entitlement): Boolean =
        !feature.gatedInThisVersion || entitlement.state == EntitlementState.PREMIUM
}
