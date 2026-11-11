package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable

/**
 * Returns true if the current hardware / platform natively supports Dolby Atmos or Spatial Audio.
 */
@Composable
expect fun rememberIsDolbyAtmosSupported(): Boolean

expect fun isDolbyAtmosHardwareSupported(): Boolean
