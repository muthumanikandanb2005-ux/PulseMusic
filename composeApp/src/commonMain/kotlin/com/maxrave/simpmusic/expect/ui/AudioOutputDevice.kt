package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

data class AudioOutputDevice(
    val name: String,
    val isBluetooth: Boolean = false,
    val isHeadphones: Boolean = false,
    val isSpeaker: Boolean = true,
)

/**
 * Automatically observes and evaluates the currently active audio output routing device:
 * - Bluetooth Audio (TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_SCO, TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER) -> queries productName (e.g. "Sony WH-1000XM4")
 * - Wired / USB (TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET, TYPE_USB_DEVICE) -> "Headphones" or "USB Audio"
 * - Internal (TYPE_BUILTIN_SPEAKER) -> "Phone Speaker"
 * Automatically re-evaluates whenever a device connects/disconnects or headphones are plugged/unplugged.
 */
@Composable
expect fun rememberCurrentAudioOutputDevice(): AudioOutputDevice
