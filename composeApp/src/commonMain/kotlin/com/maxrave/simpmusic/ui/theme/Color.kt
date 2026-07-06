package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand =====

/**
 * Brand seed color. The whole Material 3 ColorScheme is generated from this
 * color at runtime — see [AppTheme].
 */
val seed = Color(0xFF8ECAE6)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF4081)

/** Currently playing lyric line. */
val lyricActiveColor = Color(0xFFFFFF00)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Deprecated tokens — remove call sites during the color sweep phase =====

@Deprecated("Use MaterialTheme.colorScheme.background instead")
val md_theme_dark_background = Color(0xFF000000)

@Deprecated("Use MaterialTheme.colorScheme.primary instead")
val md_theme_dark_primary = Color(0xFFB2C5FF)

@Deprecated("Use MaterialTheme.colorScheme.primary instead")
val bottomBarSeedDark = Color(0xff53a7d0)

@Deprecated("Use Color.White instead")
val white = Color(0xFFFFFFFF)

@Deprecated("Use Color.Transparent instead")
val transparent = Color(0x00000000)
