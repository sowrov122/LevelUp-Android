package com.levelup.app.ui.theme

import androidx.annotation.DrawableRes
import com.levelup.app.R

/** Illustration assets (WebP in res/drawable-nodpi). Screens reference artwork only through this object. */
object Art {
    @DrawableRes val SplashBackground = R.drawable.splash_bg
    @DrawableRes val HeroObservatory = R.drawable.hero_observatory
    @DrawableRes val LogoRocket = R.drawable.logo_rocket
    @DrawableRes val AvatarAstronaut = R.drawable.avatar_astronaut

    @DrawableRes val Coin = R.drawable.coin
    @DrawableRes val Trophy = R.drawable.trophy
    @DrawableRes val BadgeMedal = R.drawable.badge_medal
    @DrawableRes val LevelUpCrest = R.drawable.levelup_crest
    @DrawableRes val Crown = R.drawable.crown
    @DrawableRes val ShieldLock = R.drawable.shield_lock
    @DrawableRes val AlertTriangle = R.drawable.alert_triangle

    /** The space station in six build steps; index 0 is the empty platform. */
    @DrawableRes val StationStages: List<Int> = listOf(
        R.drawable.station_stage_1, R.drawable.station_stage_2, R.drawable.station_stage_3,
        R.drawable.station_stage_4, R.drawable.station_stage_5, R.drawable.station_stage_6,
    )

    /** Picture for a shop item id (see RewardCatalog), or null for items without artwork such as colour themes. */
    @DrawableRes
    fun shopItem(id: String): Int? = when (id) {
        "obs_satellite" -> R.drawable.item_satellite
        "obs_garden" -> R.drawable.item_garden
        "obs_antenna" -> R.drawable.item_antenna
        "obs_solar" -> R.drawable.item_solar
        "obs_shuttle" -> R.drawable.item_shuttle
        "obs_starship" -> R.drawable.item_starship
        "obs_comet" -> R.drawable.item_comet
        "obs_nebula" -> R.drawable.item_nebula
        "obs_ringed_planet" -> R.drawable.item_ringed_planet
        "obs_moon" -> R.drawable.item_moon
        "obs_new_planet" -> R.drawable.item_new_planet
        else -> null
    }

    /** Which station picture to show for a session that is [progress] (0..1) complete. */
    fun stationStageIndex(progress: Float): Int =
        (progress.coerceIn(0f, 1f) * StationStages.size).toInt().coerceAtMost(StationStages.size - 1)
}
