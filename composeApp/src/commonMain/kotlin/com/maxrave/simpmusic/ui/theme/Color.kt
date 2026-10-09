package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand =====

/**
 * Brand seed color for Pulse. The whole Material 3 ColorScheme is generated from this
 * color at runtime — see [AppTheme].
 */
val seed = Color(0xFFFA2D48)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF2E63)

/** Currently playing lyric line. */
val lyricActiveColor = Color(0xFF00E5FF)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

// Light-theme counterparts of the shimmer tokens.
val shimmerBackgroundLight = Color(0x7EDCD8D8)
val shimmerLineLight = Color(0xFFCFC8C8)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Desktop shell =====
// Spotify-style layering for the desktop window: the window itself takes the extreme, panels
// step one shade back towards the middle so they read as raised. The light panel is not here —
// it comes from colorScheme.surfaceContainer, which already sits at the right distance.

val desktopWindowDark = Color(0xFF000000)
val desktopWindowLight = Color(0xFFFFFFFF)
val desktopPanelDark = Color(0xFF121212)

// ===== Desktop window controls =====
// The macOS traffic lights. Fixed by convention rather than by theme — users read these by
// colour, so they stay the same in light and dark.

val windowCloseButton = Color(0xFFFF605C)
val windowCloseButtonHover = Color(0xFFE54942)
val windowMinimiseButton = Color(0xFFFFBD44)
val windowMinimiseButtonHover = Color(0xFFE5A93D)
val windowMaximiseButton = Color(0xFF00CA4E)
val windowMaximiseButtonHover = Color(0xFF00B344)

// ===== Legacy — do not add new usages =====

/**
 * Old M3 primary (lavender). Kept only for the SettingScreen storage bar,
 * which stays untouched by owner's decision.
 */
@Deprecated("Legacy storage bar color only — use MaterialTheme.colorScheme.primary in new code")
val md_theme_dark_primary = Color(0xFFB2C5FF)

// ===== Pearl Lavender Theme (Pastel Lavender Palette) =====
// Light mode tokens
val pearlLavenderLightBackground = Color(0xFFF8F4FF)
val pearlLavenderLightSurface = Color(0xFFEADDF8)
val pearlLavenderLightSurfaceSecondary = Color(0xFFC9B4E6)
val pearlLavenderLightPrimary = Color(0xFFA78BFA)
val pearlLavenderLightSelected = Color(0xFF8B6FCD)
val pearlLavenderLightPlayButton = Color(0xFFF2B8C6)
val pearlLavenderLightProgressBar = Color(0xFFE8A6B7)
val pearlLavenderLightTextPrimary = Color(0xFF6B5B8F)
val pearlLavenderLightTextSecondary = Color(0xFF7D6E9A)
val pearlLavenderLightOnPrimary = Color(0xFFFFFFEF)
val pearlLavenderLightDivider = Color(0xFFF5EFFF)

// Dark mode tokens
val pearlLavenderDarkBackground = Color(0xFF1B1230)
val pearlLavenderDarkSurface = Color(0xFF291B40)
val pearlLavenderDarkSurfaceSecondary = Color(0xFF3A2A57)
val pearlLavenderDarkPrimary = Color(0xFFA78BFA)
val pearlLavenderDarkPlayButton = Color(0xFFF2B8C6)
val pearlLavenderDarkProgressBar = Color(0xFFE8A6B7)
val pearlLavenderDarkTextSecondary = Color(0xFFC4B6D8)
val pearlLavenderDarkTextPrimary = Color(0xFFFFFFFF)

