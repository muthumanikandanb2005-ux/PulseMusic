package com.manikandan.data.mediaservice

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.io.File

internal data class DolbyRegistrySignals(
    val osName: String,
    val osVersion: String,
    val hasActiveDolbySoftwareEntry: Boolean,
    val hasActiveDolbyService: Boolean,
    val hasDolbyApoEntry: Boolean,
    val hasDolbySystemDriverFile: Boolean = false,
)

internal object DolbyDesktopDetector {
    private val DOLBY_SOFTWARE_KEYS = listOf(
        "SOFTWARE\\Dolby",
        "SOFTWARE\\WOW6432Node\\Dolby",
        "SOFTWARE\\Dolby Laboratories",
    )

    private val DOLBY_SERVICE_NAMES = listOf(
        "DolbyDAXAPI",
        "DolbyDAX2API",
        "DolbyDAX3API",
        "DolbyAtmos",
        "DolbyAudioService",
        "DolbyAPO",
        "DAX3API",
    )

    private const val APO_REGISTRY_PATH =
        "SOFTWARE\\Classes\\AudioEngine\\AudioProcessingObjects"

    private const val OVERRIDE_PROPERTY = "pulse.dolby.supported.override"

    private val cachedSupport: Boolean by lazy {
        val override = System.getProperty(OVERRIDE_PROPERTY)?.trim()?.lowercase()
        when (override) {
            "true" -> true
            "false" -> false
            else -> evaluate(probeHostSignals())
        }
    }

    fun isSupported(): Boolean = cachedSupport

    internal fun evaluate(signals: DolbyRegistrySignals): Boolean {
        val os = signals.osName.lowercase()
        return when {
            os.contains("win") -> {
                signals.hasActiveDolbyService ||
                    signals.hasDolbyApoEntry ||
                    signals.hasActiveDolbySoftwareEntry ||
                    signals.hasDolbySystemDriverFile
            }
            os.contains("mac") -> isMacOsSpatialSupported(signals.osVersion)
            else -> false
        }
    }

    internal fun isMacOsSpatialSupported(osVersion: String): Boolean {
        val major = osVersion.substringBefore('.').toIntOrNull() ?: return false
        // macOS Big Sur (11) / Monterey (12)+ provides native CoreAudio Spatial Audio / Dolby Atmos rendering.
        return major >= 12
    }

    private fun probeHostSignals(): DolbyRegistrySignals {
        val osName = System.getProperty("os.name", "")
        val osVersion = System.getProperty("os.version", "")
        if (!osName.contains("Win", ignoreCase = true)) {
            return DolbyRegistrySignals(
                osName = osName,
                osVersion = osVersion,
                hasActiveDolbySoftwareEntry = false,
                hasActiveDolbyService = false,
                hasDolbyApoEntry = false,
                hasDolbySystemDriverFile = false,
            )
        }

        val hasSoftware = DOLBY_SOFTWARE_KEYS.any { keyPath ->
            hasMeaningfulDolbySoftwareKey(WinReg.HKEY_LOCAL_MACHINE, keyPath)
        } || hasMeaningfulDolbySoftwareKey(WinReg.HKEY_CURRENT_USER, "Software\\Dolby")

        val hasService = DOLBY_SERVICE_NAMES.any { serviceName ->
            isEnabledWindowsService("SYSTEM\\CurrentControlSet\\Services\\$serviceName")
        }

        val hasApo = hasDolbyApoRegistered()
        val hasDriver = hasInstalledDolbyDriverBinary()

        return DolbyRegistrySignals(
            osName = osName,
            osVersion = osVersion,
            hasActiveDolbySoftwareEntry = hasSoftware,
            hasActiveDolbyService = hasService,
            hasDolbyApoEntry = hasApo,
            hasDolbySystemDriverFile = hasDriver,
        )
    }

    /**
     * Requires at least one real product subkey or value so empty leftover registry shells
     * do not falsely advertise Dolby support.
     */
    private fun hasMeaningfulDolbySoftwareKey(root: WinReg.HKEY, keyPath: String): Boolean =
        runCatching {
            if (!Advapi32Util.registryKeyExists(root, keyPath)) return@runCatching false
            val subKeys = Advapi32Util.registryGetKeys(root, keyPath)
            val relevantSubKey = subKeys.any { sub ->
                val lower = sub.lowercase()
                lower.contains("dax") ||
                    lower.contains("atmos") ||
                    lower.contains("dolby") ||
                    lower.contains("audio") ||
                    lower.contains("access")
            }
            if (relevantSubKey) return@runCatching true
            val values = Advapi32Util.registryGetValues(root, keyPath)
            values.keys.any { it.isNotBlank() }
        }.getOrDefault(false)

    private fun isEnabledWindowsService(serviceKey: String): Boolean =
        runCatching {
            if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, serviceKey)) {
                return@runCatching false
            }
            // Start == 4 means SERVICE_DISABLED
            val startType =
                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, serviceKey, "Start")) {
                    Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, serviceKey, "Start")
                } else {
                    2
                }
            startType != 4
        }.getOrDefault(false)

    private fun hasDolbyApoRegistered(): Boolean =
        runCatching {
            if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, APO_REGISTRY_PATH)) {
                return@runCatching false
            }
            val clsids = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, APO_REGISTRY_PATH)
            clsids.any { clsid ->
                val entryKey = "$APO_REGISTRY_PATH\\$clsid"
                val values = runCatching {
                    Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, entryKey)
                }.getOrDefault(emptyMap())
                values.values.any { value ->
                    val text = value?.toString()?.lowercase() ?: ""
                    text.contains("dolby") || text.contains("dax3") || text.contains("atmos")
                }
            }
        }.getOrDefault(false)

    private fun hasInstalledDolbyDriverBinary(): Boolean =
        runCatching {
            val windir = System.getenv("SystemRoot") ?: System.getenv("WINDIR") ?: return@runCatching false
            val system32 = File(windir, "System32")
            val daxApi = File(system32, "DolbyDAX2APOProp.dll")
            val dax3Api = File(system32, "DolbyDAX3APO.dll")
            val dolbyDec = File(system32, "DolbyDecMFT.dll")
            daxApi.exists() || dax3Api.exists() || dolbyDec.exists()
        }.getOrDefault(false)
}

fun isDesktopDolbyAtmosSupported(): Boolean = DolbyDesktopDetector.isSupported()
