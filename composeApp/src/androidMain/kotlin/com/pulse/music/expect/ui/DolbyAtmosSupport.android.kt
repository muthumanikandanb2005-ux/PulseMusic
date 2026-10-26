package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.maxrave.data.mediaservice.isDolbyAtmosHardwareSupported

@Composable
actual fun rememberIsDolbyAtmosSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) { isDolbyAtmosHardwareSupported(context) }
}

actual fun isDolbyAtmosHardwareSupported(): Boolean = false
