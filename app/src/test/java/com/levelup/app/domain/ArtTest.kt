package com.levelup.app.domain

import com.levelup.app.ui.theme.Art
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArtTest {
    @Test fun everyArtworkShopItemHasAPicture() {
        RewardCatalog.items.filter { it.category != ShopCategory.THEME }.forEach { assertNotNull("no art for ${it.id}", Art.shopItem(it.id)) }
        RewardCatalog.items.filter { it.category == ShopCategory.THEME }.forEach { assertNull(Art.shopItem(it.id)) }
    }

    @Test fun stationStageFollowsProgress() {
        assertEquals(0, Art.stationStageIndex(0f))
        assertEquals(2, Art.stationStageIndex(0.4f))
        assertEquals(5, Art.stationStageIndex(1f))
        assertEquals(0, Art.stationStageIndex(-3f))
        assertEquals(6, Art.StationStages.size)
    }
}
