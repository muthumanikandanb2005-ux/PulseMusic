package com.maxrave.simpmusic.expect.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (isDark) {
        // Keep the OLED look: pin background/surface to pure black like the seed scheme.
        dynamicDarkColorScheme(context).copy(
            background = Color.Black,
            surface = Color.Black,
        )
    } else {
        dynamicLightColorScheme(context)
    }
}

actual fun isWallpaperDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
