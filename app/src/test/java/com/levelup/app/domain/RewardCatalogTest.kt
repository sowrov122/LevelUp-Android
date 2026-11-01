package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCatalogTest {
    @Test fun idsAreUniqueAndPricesPositive() {
        assertEquals(RewardCatalog.items.size, RewardCatalog.items.map { it.id }.toSet().size)
        assertTrue(RewardCatalog.items.all { it.priceCoins > 0 })
    }

    @Test fun everyTabHasItemsSortedByPrice() {
        ShopCategory.entries.forEach { c ->
            val list = RewardCatalog.inCategory(c)
            assertTrue("$c empty", list.isNotEmpty())
            assertEquals(list.map { it.priceCoins }.sorted(), list.map { it.priceCoins })
        }
    }

    @Test fun themesHaveColoursAndOthersDoNot() {
        RewardCatalog.items.filter { it.category == ShopCategory.THEME }.forEach { assertTrue(it.colorHex != null) }
        assertEquals(RewardCatalog.DEFAULT_THEME_COLOR, RewardCatalog.themeColor("missing"))
    }
}
