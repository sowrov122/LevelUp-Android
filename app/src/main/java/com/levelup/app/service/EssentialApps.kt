package com.levelup.app.service

import android.content.Context
import android.provider.Telephony
import android.telecom.TelecomManager

/** Apps that can never be restricted: phone, messages, maps, system UI/settings and Level Up itself. */
object EssentialApps {
    private val KNOWN = setOf(
        "com.android.settings", "com.android.systemui", "com.android.phone", "com.android.dialer",
        "com.google.android.dialer", "com.android.mms", "com.google.android.apps.messaging",
        "com.google.android.apps.maps", "com.android.emergency", "com.android.contacts",
        "com.google.android.contacts", "com.android.deskclock", "com.google.android.deskclock",
    )

    fun all(context: Context): Set<String> {
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
        val sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
        return KNOWN + setOfNotNull(dialer, sms, context.packageName)
    }
}
