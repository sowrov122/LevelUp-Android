package com.levelup.app.domain

/**
 * Well-known social, video, game-streaming and browser apps, offered as one-tap choices when they are
 * installed. Pure convenience: the user still decides, and nothing is selected by default.
 */
object PopularApps {
    val packages: Set<String> = setOf(
        // social / short video
        "com.google.android.youtube", "com.facebook.katana", "com.facebook.lite", "com.facebook.orca",
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.twitter.android",
        "com.snapchat.android", "com.reddit.frontpage", "com.pinterest",
        // streaming
        "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient", "com.spotify.music",
        // browsers
        "com.android.chrome", "org.mozilla.firefox", "com.brave.browser", "com.microsoft.emmx",
        "com.opera.browser", "com.sec.android.app.sbrowser",
    )

    fun isPopular(packageName: String): Boolean = packageName in packages
}
