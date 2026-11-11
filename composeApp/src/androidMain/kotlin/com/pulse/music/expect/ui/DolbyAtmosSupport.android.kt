package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.manikandan.data.mediaservice.isDolbyAtmosHardwareSupported

@Composable
actual fun rememberIsDolbyAtmosSupported(): Boolean {
    return true
}

actual fun isDolbyAtmosHardwareSupported(): Boolean = true
