package com.levelup.app.domain

enum class ShopCategory(val label: String) { BASE("Base"), SHIP("Ship"), PLANET("Planet"), THEME("Theme") }

data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val priceCoins: Int,
    val category: ShopCategory,
    val colorHex: Long? = null,
)

/** Cosmetic-only items. Nothing here changes scoring. Ids starting with "obs_" are drawn on the station. */
object RewardCatalog {
    const val DEFAULT_THEME_ID = "default"
    const val DEFAULT_THEME_COLOR = 0xFFF4A34B

    val items: List<ShopItem> = listOf(
        // Base
        ShopItem("obs_satellite", "Orbital Satellite", "A small satellite circling above the dome.", 100, ShopCategory.BASE),
        ShopItem("obs_garden", "Garden Module", "A glass greenhouse on your station.", 500, ShopCategory.BASE),
        ShopItem("obs_antenna", "Comm Antenna", "A tall antenna with a signal dish.", 600, ShopCategory.BASE),
        ShopItem("obs_solar", "Solar Panel Array", "Panels that catch the starlight.", 800, ShopCategory.BASE),
        // Ship
        ShopItem("obs_shuttle", "Shuttle", "A small shuttle parked in orbit.", 1_500, ShopCategory.SHIP),
        ShopItem("obs_starship", "Deep-Space Ship", "A large ship for long journeys.", 5_000, ShopCategory.SHIP),
        // Planet / sky
        ShopItem("obs_comet", "Comet Trail", "A comet streaks across your sky.", 50, ShopCategory.PLANET),
        ShopItem("obs_nebula", "Violet Nebula", "A glowing nebula behind your stars.", 80, ShopCategory.PLANET),
        ShopItem("obs_ringed_planet", "Ringed Planet", "Adds rings to your home planet.", 120, ShopCategory.PLANET),
        ShopItem("obs_moon", "Moon", "A quiet moon in your sky.", 300, ShopCategory.PLANET),
        ShopItem("obs_new_planet", "New Planet", "A second, warm-coloured world.", 10_000, ShopCategory.PLANET),
        // Themes
        ShopItem("theme_moonlit", "Moonlit theme", "Cool silver-blue accent colour.", 60, ShopCategory.THEME, 0xFF8FB8F0),
        ShopItem("theme_forest", "Forest theme", "Soft moss-green accent colour.", 60, ShopCategory.THEME, 0xFF7BC98F),
    )

    fun find(id: String): ShopItem? = items.firstOrNull { it.id == id }

    fun inCategory(category: ShopCategory): List<ShopItem> = items.filter { it.category == category }.sortedBy { it.priceCoins }

    fun themeColor(themeId: String): Long =
        items.firstOrNull { it.id == themeId && it.category == ShopCategory.THEME }?.colorHex ?: DEFAULT_THEME_COLOR
}
