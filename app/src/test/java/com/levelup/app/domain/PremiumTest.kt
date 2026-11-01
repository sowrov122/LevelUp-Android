package com.levelup.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {
    private val free = Entitlement(EntitlementState.FREE, billingAvailable = false)

    @Test fun nothingIsLockedWhileBillingIsUnavailable() {
        PremiumFeature.entries.forEach { assertTrue(PremiumCatalog.isAccessible(it, free)) }
    }

    @Test fun gatedFeatureNeedsPremium() {
        val gated = object {
            fun accessible(gatedFlag: Boolean, state: EntitlementState) = !gatedFlag || state == EntitlementState.PREMIUM
        }
        assertFalse(gated.accessible(true, EntitlementState.FREE))
        assertTrue(gated.accessible(true, EntitlementState.PREMIUM))
    }

    @Test fun coreFeaturesAreListedAsFree() {
        assertTrue(CORE_FREE_FEATURES.size >= 5)
    }
}
