package com.levelup.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopularAppsTest {
    @Test fun knownAppsAreRecognised() {
        assertTrue(PopularApps.isPopular("com.instagram.android"))
        assertFalse(PopularApps.isPopular("com.example.notes"))
    }

    @Test fun essentialPhoneAppsAreNeverOffered() {
        listOf("com.android.dialer", "com.google.android.dialer", "com.android.settings", "com.google.android.apps.maps")
            .forEach { assertFalse(PopularApps.isPopular(it)) }
    }
}
