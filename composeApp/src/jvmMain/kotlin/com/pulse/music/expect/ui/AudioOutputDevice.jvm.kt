package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCurrentAudioOutputDevice(): AudioOutputDevice =
    AudioOutputDevice(name = "Speakers", isSpeaker = true)
