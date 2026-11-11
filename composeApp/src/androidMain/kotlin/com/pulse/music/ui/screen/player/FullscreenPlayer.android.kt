package com.pulse.music.ui.screen.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pulse.music.extension.findActivity

@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
    val resources = LocalResources.current
    val context = LocalContext.current

    val originalOrientation by rememberSaveable {
        mutableIntStateOf(resources.configuration.orientation)
    }

    DisposableEffect(true) {
        onLaunch.invoke()
        val activity = context.findActivity()
        runCatching {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        onDispose {
            runCatching { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

            runCatching {
                insetsController.apply {
                    show(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            // restore original orientation when view disappears
            runCatching {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                }
            }
            runCatching { onDispose.invoke() }
        }
    }

    LaunchedEffect(true) {
        val activity = context.findActivity()
        val window = activity.window

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}