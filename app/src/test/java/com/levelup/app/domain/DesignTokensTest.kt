package com.levelup.app.domain

import androidx.compose.ui.graphics.Color
import com.levelup.app.ui.theme.AppColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Guards readability: every text/background pairing used by the design system meets WCAG AA (4.5:1). */
class DesignTokensTest {
    private fun lin(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color): Double = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertAa(fg: Color, bg: Color, name: String) =
        assertTrue("$name contrast ${"%.2f".format(contrast(fg, bg))} < 4.5", contrast(fg, bg) >= 4.5)

    @Test fun primaryTextIsReadableOnEverySurface() {
        listOf(AppColors.Background, AppColors.Header, AppColors.Card, AppColors.Elevated, AppColors.Lavender)
            .forEach { assertAa(AppColors.TextPrimary, it, "TextPrimary on $it") }
    }

    @Test fun secondaryTextAndAccentsAreReadableOnCards() {
        listOf(AppColors.Background, AppColors.Card, AppColors.Elevated).forEach {
            assertAa(AppColors.TextSecondary, it, "TextSecondary on $it")
            assertAa(AppColors.ProgressBlue, it, "ProgressBlue on $it")
            assertAa(AppColors.RewardGold, it, "RewardGold on $it")
            assertAa(AppColors.Success, it, "Success on $it")
            assertAa(AppColors.Danger, it, "Danger on $it")
        }
    }
}
