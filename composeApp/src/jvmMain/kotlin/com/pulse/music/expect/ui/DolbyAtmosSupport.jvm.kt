package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.manikandan.data.mediaservice.isDesktopDolbyAtmosSupported

@Composable
actual fun rememberIsDolbyAtmosSupported(): Boolean = remember { isDesktopDolbyAtmosSupported() }

actual fun isDolbyAtmosHardwareSupported(): Boolean = isDesktopDolbyAtmosSupported()
