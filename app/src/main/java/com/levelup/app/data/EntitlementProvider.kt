package com.levelup.app.data

import com.levelup.app.domain.Entitlement
import com.levelup.app.domain.EntitlementState
import com.levelup.app.domain.PremiumCatalog
import com.levelup.app.domain.PremiumFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single place the app asks "is this unlocked?". A Google Play Billing implementation can replace
 * [NoBillingEntitlementProvider] later without touching any screen. A local boolean must never be
 * treated as proof of purchase once billing exists.
 */
interface EntitlementProvider {
    val entitlement: Flow<Entitlement>
    fun isUnlocked(feature: PremiumFeature): Boolean
}

/** Billing is not implemented: everyone is on the free plan, nothing can be bought, nothing is locked. */
class NoBillingEntitlementProvider : EntitlementProvider {
    private val state = MutableStateFlow(Entitlement(EntitlementState.FREE, billingAvailable = false))
    override val entitlement: Flow<Entitlement> = state.asStateFlow()
    override fun isUnlocked(feature: PremiumFeature): Boolean = PremiumCatalog.isAccessible(feature, state.value)
}
