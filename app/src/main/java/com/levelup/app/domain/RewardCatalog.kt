package com.levelup.app.domain

enum class ShopCategory { THEME, OBSERVATORY }

data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val priceCoins: Int,
    val category: ShopCategory,
    val colorHex: Long? = null,
)

/** Cosmetic-only items. Nothing here changes scoring. */
object RewardCatalog {
    const val DEFAULT_THEME_ID = "default"
    const val DEFAULT_THEME_COLOR = 0xFFF4A34B

    val items: List<ShopItem> = listOf(
        ShopItem("theme_moonlit", "Moonlit theme", "Cool silver-blue accent colour.", 60, ShopCategory.THEME, 0xFF8FB8F0),
        ShopItem("theme_forest", "Forest theme", "Soft moss-green accent colour.", 60, ShopCategory.THEME, 0xFF7BC98F),
        ShopItem("obs_comet", "Comet Trail", "A comet streaks across your observatory sky.", 50, ShopCategory.OBSERVATORY),
        ShopItem("obs_nebula", "Violet Nebula", "A glowing nebula behind your stars.", 80, ShopCategory.OBSERVATORY),
        ShopItem("obs_satellite", "Orbital Satellite", "A small satellite circling above the dome.", 100, ShopCategory.OBSERVATORY),
        ShopItem("obs_ringed_planet", "Ringed Planet", "Adds rings to your home planet.", 120, ShopCategory.OBSERVATORY),
    )

    fun find(id: String): ShopItem? = items.firstOrNull { it.id == id }

    fun themeColor(themeId: String): Long =
        items.firstOrNull { it.id == themeId && it.category == ShopCategory.THEME }?.colorHex ?: DEFAULT_THEME_COLOR
}
