package com.levelup.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Kept for existing call sites; both map to the design tokens.
val Success: Color = AppColors.Success
val Danger: Color = AppColors.Danger

/**
 * Material colour roles mapped to the tokens:
 *  - primary (soft blue, or the equipped theme colour) = progress, links, checked controls
 *  - primaryContainer / secondaryContainer (lavender) = selected chips, nav indicator, primary buttons
 *  - secondary (gold) = coins and rewards
 */
@Composable
fun LevelUpTheme(accent: Color = AppColors.ProgressBlue, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = AppColors.Background,
        primaryContainer = AppColors.Lavender,
        onPrimaryContainer = AppColors.TextPrimary,
        secondary = AppColors.RewardGold,
        onSecondary = AppColors.Background,
        secondaryContainer = AppColors.Lavender,
        onSecondaryContainer = AppColors.TextPrimary,
        tertiary = AppColors.Success,
        onTertiary = AppColors.Background,
        background = AppColors.Background,
        onBackground = AppColors.TextPrimary,
        surface = AppColors.Card,
        onSurface = AppColors.TextPrimary,
        surfaceVariant = AppColors.Elevated,
        onSurfaceVariant = AppColors.TextSecondary,
        surfaceContainerLowest = AppColors.Background,
        surfaceContainerLow = AppColors.Card,
        surfaceContainer = AppColors.Header,
        surfaceContainerHigh = AppColors.Elevated,
        surfaceContainerHighest = AppColors.Elevated,
        outline = AppColors.Border,
        outlineVariant = AppColors.Border,
        error = AppColors.Danger,
        onError = AppColors.Background,
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(Radii.chip),
            small = RoundedCornerShape(Radii.chip),
            medium = RoundedCornerShape(Radii.card),
            large = RoundedCornerShape(Radii.dialog),
        ),
        content = content,
    )
}
