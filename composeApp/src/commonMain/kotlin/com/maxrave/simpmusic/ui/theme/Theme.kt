package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Semantic colors that sit outside the Material 3 ColorScheme.
 * Read them via [LocalAppColors] inside composables.
 */
@Immutable
data class AppColors(
    val favorite: Color,
    val lyricActive: Color,
    val shimmerBackground: Color,
    val shimmerLine: Color,
    val overlay: Color,
    val overlayHeavy: Color,
)

private val DefaultAppColors =
    AppColors(
        favorite = favoriteColor,
        lyricActive = lyricActiveColor,
        shimmerBackground = shimmerBackground,
        shimmerLine = shimmerLine,
        overlay = overlay,
        overlayHeavy = blackMoreOverlay,
    )

val LocalAppColors = staticCompositionLocalOf { DefaultAppColors }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    content:
        @Composable()
        () -> Unit,
) {
    // isAmoled pins background/surface to pure black to keep the OLED look.
    val colorScheme =
        rememberDynamicColorScheme(
            seedColor = seed,
            isDark = true,
            isAmoled = true,
            style = PaletteStyle.TonalSpot,
        )
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onSurfaceVariant,
                LocalAppColors provides DefaultAppColors,
                content = content,
            )
        },
        typography = typo(colorScheme),
    )
}
