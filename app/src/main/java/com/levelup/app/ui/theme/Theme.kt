package com.levelup.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF140E0A)
val AppSurface = Color(0xFF211710)
val AppSurface2 = Color(0xFF2D2118)
val Accent = Color(0xFFF4A34B)
val Gold = Color(0xFFFFCE00)
val Success = Color(0xFF53D69A)
val TextPrimary = Color(0xFFFFF4E6)
val TextSecondary = Color(0xFFB6A393)
val Danger = Color(0xFFF2735A)

@Composable
fun LevelUpTheme(accent: Color = Accent, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = AppBackground,
        secondary = Gold,
        onSecondary = AppBackground,
        tertiary = Success,
        onTertiary = AppBackground,
        background = AppBackground,
        onBackground = TextPrimary,
        surface = AppSurface,
        onSurface = TextPrimary,
        surfaceVariant = AppSurface2,
        onSurfaceVariant = TextSecondary,
        surfaceContainer = AppSurface,
        surfaceContainerLow = AppSurface,
        surfaceContainerHigh = AppSurface2,
        surfaceContainerHighest = AppSurface2,
        outline = Color(0xFF5A4A3C),
        error = Danger,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
