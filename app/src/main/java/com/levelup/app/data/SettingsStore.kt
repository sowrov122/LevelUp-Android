package com.levelup.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "level_up_settings")

data class UserSettings(
    val displayName: String = "",
    val onboardingDone: Boolean = false,
    val seeded: Boolean = false,
    /** Blank = follow the device time zone. */
    val timezoneId: String = "",
    val quietEnabled: Boolean = false,
    val quietStartMinute: Int = 22 * 60 + 30,
    val quietEndMinute: Int = 6 * 60 + 30,
    val reducedMotion: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val accentThemeId: String = "default",
    val focusDailyCapMinutes: Int = 120,
    /** Master switch for distraction protection. Off by default; nothing is observed while off. */
    val blockerEnabled: Boolean = false,
    val blockerPenaltyPoints: Int = 5,
    val blockerCooldownSeconds: Int = 300,
    /** Epoch millis until which protection is paused (user pause / emergency override). */
    val blockerPausedUntilMs: Long = 0L,
)

class SettingsStore(private val context: Context) {
    private object K {
        val NAME = stringPreferencesKey("display_name")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val SEEDED = booleanPreferencesKey("seeded")
        val TZ = stringPreferencesKey("timezone_id")
        val QUIET_ON = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val REDUCED = booleanPreferencesKey("reduced_motion")
        val NOTIF = booleanPreferencesKey("notifications_enabled")
        val ACCENT = stringPreferencesKey("accent_theme")
        val FOCUS_CAP = intPreferencesKey("focus_daily_cap")
        val BLOCK_ON = booleanPreferencesKey("blocker_enabled")
        val BLOCK_PENALTY = intPreferencesKey("blocker_penalty")
        val BLOCK_COOLDOWN = intPreferencesKey("blocker_cooldown")
        val BLOCK_PAUSED = longPreferencesKey("blocker_paused_until")
    }

    val flow: Flow<UserSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun current(): UserSettings = flow.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsDataStore.edit { p ->
            val n = transform(p.toSettings())
            p[K.NAME] = n.displayName
            p[K.ONBOARDED] = n.onboardingDone
            p[K.SEEDED] = n.seeded
            p[K.TZ] = n.timezoneId
            p[K.QUIET_ON] = n.quietEnabled
            p[K.QUIET_START] = n.quietStartMinute
            p[K.QUIET_END] = n.quietEndMinute
            p[K.REDUCED] = n.reducedMotion
            p[K.NOTIF] = n.notificationsEnabled
            p[K.ACCENT] = n.accentThemeId
            p[K.FOCUS_CAP] = n.focusDailyCapMinutes
            p[K.BLOCK_ON] = n.blockerEnabled
            p[K.BLOCK_PENALTY] = n.blockerPenaltyPoints
            p[K.BLOCK_COOLDOWN] = n.blockerCooldownSeconds
            p[K.BLOCK_PAUSED] = n.blockerPausedUntilMs
        }
    }

    suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            displayName = this[K.NAME] ?: d.displayName,
            onboardingDone = this[K.ONBOARDED] ?: d.onboardingDone,
            seeded = this[K.SEEDED] ?: d.seeded,
            timezoneId = this[K.TZ] ?: d.timezoneId,
            quietEnabled = this[K.QUIET_ON] ?: d.quietEnabled,
            quietStartMinute = this[K.QUIET_START] ?: d.quietStartMinute,
            quietEndMinute = this[K.QUIET_END] ?: d.quietEndMinute,
            reducedMotion = this[K.REDUCED] ?: d.reducedMotion,
            notificationsEnabled = this[K.NOTIF] ?: d.notificationsEnabled,
            accentThemeId = this[K.ACCENT] ?: d.accentThemeId,
            focusDailyCapMinutes = this[K.FOCUS_CAP] ?: d.focusDailyCapMinutes,
            blockerEnabled = this[K.BLOCK_ON] ?: d.blockerEnabled,
            blockerPenaltyPoints = this[K.BLOCK_PENALTY] ?: d.blockerPenaltyPoints,
            blockerCooldownSeconds = this[K.BLOCK_COOLDOWN] ?: d.blockerCooldownSeconds,
            blockerPausedUntilMs = this[K.BLOCK_PAUSED] ?: d.blockerPausedUntilMs,
        )
    }
}
