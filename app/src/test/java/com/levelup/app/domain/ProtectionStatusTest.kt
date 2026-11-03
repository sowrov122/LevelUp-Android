package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProtectionStatusTest {
    @Test fun statesFollowTheRealConditions() {
        assertEquals(ProtectionState.OFF, ProtectionStatus.state(enabled = false, usageAccess = true, strict = true))
        assertEquals(ProtectionState.NEEDS_USAGE_ACCESS, ProtectionStatus.state(true, false, false))
        assertEquals(ProtectionState.ACTIVE, ProtectionStatus.state(true, true, false))
        assertEquals(ProtectionState.ACTIVE_STRICT, ProtectionStatus.state(true, true, true))
    }

    @Test fun messagesNeverClaimThatAppsAreBlocked() {
        ProtectionState.entries.forEach {
            assertFalse(ProtectionStatus.subtitle(it).contains("block", ignoreCase = true))
        }
        assertFalse(ProtectionStatus.subtitle(ProtectionState.NEEDS_USAGE_ACCESS).contains("Opening"))
    }
}
