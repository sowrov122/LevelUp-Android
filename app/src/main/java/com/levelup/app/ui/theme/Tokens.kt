package com.levelup.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Single source of truth for the LEVEL UP visual language. Screens use these or MaterialTheme roles, never raw hex. */
object AppColors {
    val Background = Color(0xFF140E0A)     // main screen background
    val Header = Color(0xFF21140F)         // top bar and bottom navigation
    val Card = Color(0xFF302117)           // cards
    val Elevated = Color(0xFF3A2A20)       // dialogs, sheets, chips, raised surfaces
    val Border = Color(0xFF4A372A)         // subtle card/outline colour
    val Lavender = Color(0xFF62567D)       // primary accent: selected states, primary buttons
    val LavenderSoft = Color(0xFF3B3350)   // Card tinted with lavender: the active task
    val ProgressBlue = Color(0xFF91B8F0)   // progress accent: bars, rings, links
    val RewardGold = Color(0xFFFFD000)     // coins and rewards only
    val TextPrimary = Color(0xFFF5EBDD)    // main text
    val TextSecondary = Color(0xFFB5A89B)  // secondary text
    val Success = Color(0xFF8FD4A8)        // muted mint
    val Danger = Color(0xFFE5756A)         // muted red, used sparingly for errors and penalties
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object Radii {
    val chip = 12.dp
    val button = 14.dp
    val card = 16.dp
    val dialog = 20.dp
}

object Sizes {
    val minTouchTarget = 48.dp
    val buttonHeight = 52.dp
    val badge = 40.dp
}
