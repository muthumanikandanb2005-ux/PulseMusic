package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.manikandan.domain.data.model.ui.ScreenSizeInfo
import com.maxrave.logger.Logger

fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}

@Suppress("DEPRECATION")
@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val context = LocalContext.current
    val activity = context.getActivityOrNull()
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current

    return remember(configuration) {
        val confWdp = configuration.screenWidthDp
        val confHdp = configuration.screenHeightDp
        val confWpx = with(localDensity) { confWdp.dp.roundToPx() }
        val confHpx = with(localDensity) { confHdp.dp.roundToPx() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            val mHeight = windowMetrics?.bounds?.height() ?: 0
            val mWidth = windowMetrics?.bounds?.width() ?: 0
            Logger.w("getScreenSizeInfo", "WindowMetrics: $mHeight x $mWidth, config: $confHdp x $confWdp")

            if (mHeight > 0 && mWidth > 0) {
                ScreenSizeInfo(
                    hDP = with(localDensity) { mHeight.toDp().value.toInt() },
                    wDP = with(localDensity) { mWidth.toDp().value.toInt() },
                    hPX = mHeight,
                    wPX = mWidth,
                )
            } else {
                ScreenSizeInfo(
                    hDP = confHdp,
                    wDP = confWdp,
                    hPX = confHpx,
                    wPX = confWpx,
                )
            }
        } else {
            val point = Point()
            activity?.windowManager?.defaultDisplay?.getRealSize(point)
            Logger.w("getScreenSizeInfo", "Point: ${point.y} x ${point.x}, config: $confHdp x $confWdp")

            if (point.y > 0 && point.x > 0) {
                ScreenSizeInfo(
                    hDP = with(localDensity) { point.y.toDp().value.toInt() },
                    wDP = with(localDensity) { point.x.toDp().value.toInt() },
                    hPX = point.y,
                    wPX = point.x,
                )
            } else {
                ScreenSizeInfo(
                    hDP = confHdp,
                    wDP = confWdp,
                    hPX = confHpx,
                    wPX = confWpx,
                )
            }
        }
    }
}

@Composable
actual fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return (getActivityOrNull() as? ComponentActivity) ?: throw IllegalStateException("Activity not found in context")
}

@Composable
actual fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity()
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer =
            Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
        activity.addOnPictureInPictureModeChangedListener(
            observer,
        )
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}