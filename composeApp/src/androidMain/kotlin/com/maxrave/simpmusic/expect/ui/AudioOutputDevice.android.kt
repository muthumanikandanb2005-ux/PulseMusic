package com.maxrave.simpmusic.expect.ui

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private fun resolveCurrentOutputDevice(context: Context): AudioOutputDevice {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        ?: return AudioOutputDevice("Phone Speaker", isSpeaker = true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // 1. Bluetooth Audio Priority: TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_SCO, TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER
        for (device in devices) {
            val type = device.type
            val isBluetooth = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                ))
            if (isBluetooth) {
                val productName = device.productName?.toString()?.trim()
                val name = if (!productName.isNullOrBlank()) productName else "Bluetooth Audio"
                return AudioOutputDevice(name = name, isBluetooth = true, isHeadphones = false, isSpeaker = false)
            }
        }

        // 2. Wired / USB Priority: TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET, TYPE_USB_DEVICE
        for (device in devices) {
            val type = device.type
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                return AudioOutputDevice(name = "Headphones", isBluetooth = false, isHeadphones = true, isSpeaker = false)
            }
            if (type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                return AudioOutputDevice(name = "USB Audio", isBluetooth = false, isHeadphones = true, isSpeaker = false)
            }
        }

        // 3. Internal: TYPE_BUILTIN_SPEAKER
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                return AudioOutputDevice(name = "Phone Speaker", isBluetooth = false, isHeadphones = false, isSpeaker = true)
            }
        }
    }

    // Pre-M / Fallback using AudioManager flags
    @Suppress("DEPRECATION")
    return when {
        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn ->
            AudioOutputDevice(name = "Bluetooth Audio", isBluetooth = true, isHeadphones = false, isSpeaker = false)
        audioManager.isWiredHeadsetOn ->
            AudioOutputDevice(name = "Headphones", isBluetooth = false, isHeadphones = true, isSpeaker = false)
        else ->
            AudioOutputDevice(name = "Phone Speaker", isBluetooth = false, isHeadphones = false, isSpeaker = true)
    }
}

@Composable
actual fun rememberCurrentAudioOutputDevice(): AudioOutputDevice {
    val context = LocalContext.current
    var currentDevice by remember(context) {
        mutableStateOf(resolveCurrentOutputDevice(context))
    }

    DisposableEffect(context) {
        val mainHandler = Handler(Looper.getMainLooper())
        val updateDevice = {
            currentDevice = resolveCurrentOutputDevice(context)
        }

        // 1. Dynamic AudioDeviceCallback for real-time plug/unplug events
        val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val cb = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateDevice()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateDevice()
                }
            }
            audioManager?.registerAudioDeviceCallback(cb, mainHandler)
            cb
        } else {
            null
        }

        // 2. BroadcastReceiver for Bluetooth state and headset plug broadcasts
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                updateDevice()
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return currentDevice
}
