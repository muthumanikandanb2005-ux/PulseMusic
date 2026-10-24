package com.maxrave.data.mediaservice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.audiofx.AudioEffect
import android.os.Build

/**
 * Checks if the current Android device natively supports Dolby Atmos or Spatial Audio.
 */
fun isDolbyAtmosHardwareSupported(context: Context): Boolean {
    // 1. Query Android system AudioEffects for Dolby Audio Processing (DAP) or Dolby Atmos
    try {
        val effects = AudioEffect.queryEffects()
        if (effects != null) {
            for (effect in effects) {
                val name = effect.name?.lowercase() ?: ""
                val implementor = effect.implementor?.lowercase() ?: ""
                val uuid = effect.uuid?.toString()?.lowercase() ?: ""
                val type = effect.type?.toString()?.lowercase() ?: ""
                if (name.contains("dolby") || name.contains("atmos") ||
                    implementor.contains("dolby") ||
                    uuid.contains("46d279d9") || type.contains("46d279d9") ||
                    uuid.contains("efccdae0") || type.contains("efccdae0")
                ) {
                    return true
                }
            }
        }
    } catch (_: Throwable) {}

    // 2. Query Android 12L / 13+ (API 32+) Spatializer
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val spatializer = audioManager?.spatializer
            if (spatializer != null && spatializer.isAvailable) {
                return true
            }
        } catch (_: Throwable) {}
    }

    // 3. Query MediaCodecList for Dolby Atmos (E-AC-3 JOC or AC-4) hardware decoders
    try {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (codec in codecList.codecInfos) {
            if (codec.isEncoder) continue
            for (type in codec.supportedTypes) {
                val tl = type.lowercase()
                if (tl.contains("eac3-joc") || tl.contains("ac4") ||
                    (tl.contains("dolby") && !tl.contains("vision"))
                ) {
                    return true
                }
            }
        }
    } catch (_: Throwable) {}

    // 4. Query AudioDeviceInfo encodings on Android 11+ (API 30+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            for (device in devices) {
                for (enc in device.encodings) {
                    if (enc == AudioFormat.ENCODING_E_AC3_JOC ||
                        enc == AudioFormat.ENCODING_DOLBY_TRUEHD ||
                        enc == AudioFormat.ENCODING_DOLBY_MAT
                    ) {
                        return true
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    // 5. Query system properties for OEM Dolby integration (Samsung, Xiaomi, OnePlus, Motorola)
    try {
        val sysPropClass = Class.forName("android.os.SystemProperties")
        val getMethod = sysPropClass.getMethod("get", String::class.java)
        val dolbyProps = listOf(
            "ro.odm.audio.dolby",
            "ro.audio.dolby",
            "vendor.audio.dolby.ds2.enabled",
            "ro.vendor.audio.dolby.surround"
        )
        for (prop in dolbyProps) {
            val value = getMethod.invoke(null, prop) as? String
            if (!value.isNullOrBlank() && (value.equals("true", ignoreCase = true) || value == "1")) {
                return true
            }
        }
    } catch (_: Throwable) {}

    return false
}
