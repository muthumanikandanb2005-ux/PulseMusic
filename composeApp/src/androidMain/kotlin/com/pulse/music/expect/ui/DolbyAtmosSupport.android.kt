package com.pulse.music.expect.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.manikandan.data.mediaservice.isDolbyAtmosHardwareSupported
import org.koin.mp.KoinPlatform

@Composable
actual fun rememberIsDolbyAtmosSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) { isDolbyAtmosHardwareSupported(context) }
}

actual fun isDolbyAtmosHardwareSupported(): Boolean {
    val context =
        runCatching { KoinPlatform.getKoin().getOrNull<Context>() }.getOrNull()
            ?: return false
    return isDolbyAtmosHardwareSupported(context)
}
