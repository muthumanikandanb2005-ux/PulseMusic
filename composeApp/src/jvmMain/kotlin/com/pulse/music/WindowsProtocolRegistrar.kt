package com.pulse.music

import com.manikandan.logger.Logger
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.io.File

/**
 * Registers the "pulsemusic://" custom URI protocol handler and Windows AUMID metadata under
 * HKEY_CURRENT_USER using direct in-process JNA Win32 Registry APIs (zero child processes).
 */
object WindowsProtocolRegistrar {
    private const val TAG = "WindowsProtocolRegistrar"
    private const val SCHEME = "pulsemusic"
    private const val LASTFM_SCHEME = "wordbyword"

    private fun hkcuSubKeyOf(scheme: String) = "Software\\Classes\\$scheme"

    fun register() {
        if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return

        val exePath = resolveExePath() ?: run {
            Logger.e(TAG, "Could not resolve executable path, skipping protocol registration")
            return
        }

        register(SCHEME, "URL:Pulse Music Protocol", exePath)
        register(LASTFM_SCHEME, "URL:Pulse Music Last.fm Callback", exePath)
        registerApplicationIdentity(exePath)
    }

    private fun registerApplicationIdentity(exePath: String) {
        try {
            val exeFile = File(exePath)
            val appDir = exeFile.parentFile

            val userHome = System.getProperty("user.home") ?: System.getenv("USERPROFILE") ?: ""
            val pulseAppDataDir = File(System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming", "PulseMusic")
            pulseAppDataDir.mkdirs()
            val appDataIco = File(pulseAppDataDir, "PulseMusic.ico")

            val icoCandidates = listOfNotNull(
                appDir?.let { File(it, "PulseMusic.ico") },
                appDir?.let { File(it, "Pulse.ico") },
                appDir?.let { File(it, "icon.ico") },
            )
            val foundIco = icoCandidates.firstOrNull { it.exists() }
            if (foundIco != null && (!appDataIco.exists() || appDataIco.length() == 0L)) {
                runCatching { foundIco.copyTo(appDataIco, overwrite = true) }
            } else if (!appDataIco.exists() || appDataIco.length() == 0L) {
                runCatching {
                    val stream =
                        WindowsProtocolRegistrar::class.java.getResourceAsStream("/circle_app_icon.ico")
                            ?: WindowsProtocolRegistrar::class.java.getResourceAsStream("/icon.ico")
                    stream?.use { input ->
                        appDataIco.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }

            val iconPath =
                when {
                    appDataIco.exists() && appDataIco.length() > 0L -> appDataIco.absolutePath
                    foundIco != null -> foundIco.absolutePath
                    else -> exePath
                }

            val appName = exeFile.name
            val appNames = listOf(appName, "Pulse.exe", "PulseMusic.exe").distinct()
            for (name in appNames) {
                val regAppKey = "Software\\Classes\\Applications\\$name"
                regSet(regAppKey, null, "Pulse Music")
                regSet(regAppKey, "FriendlyAppName", "Pulse Music")
                regSet(regAppKey, "ApplicationCompany", "Manikandan")
                regSet("$regAppKey\\DefaultIcon", null, iconPath)
            }

            val aumids = listOf("com.pulse.music", "Pulse", "Pulse.exe", "PulseMusic")
            for (aumid in aumids) {
                val aumidKey = "Software\\Classes\\AppUserModelId\\$aumid"
                regSet(aumidKey, "DisplayName", "Pulse Music")
                regSet(aumidKey, "IconUri", iconPath)
                regSet(aumidKey, "IconBackgroundColor", "0")
            }

            if (appDir != null) {
                val appPathsKey = "Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\$appName"
                regSet(appPathsKey, null, exePath)
                regSet(appPathsKey, "Path", appDir.absolutePath)
            }
            Logger.d(TAG, "Windows Application Identity registered in-process for $appName and SMTC")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register Windows Application Identity: ${e.message}")
        }
    }

    private fun register(
        scheme: String,
        description: String,
        exePath: String,
    ) {
        val subKey = hkcuSubKeyOf(scheme)
        try {
            if (isAlreadyRegistered(scheme, exePath)) {
                Logger.d(TAG, "$scheme:// already registered with correct path")
                return
            }

            Logger.d(TAG, "Registering $scheme:// protocol handler -> $exePath")
            regSet(subKey, null, description)
            regSet(subKey, "URL Protocol", "")
            regSet("$subKey\\DefaultIcon", null, "\"$exePath\",0")
            regSet("$subKey\\shell\\open\\command", null, "\"$exePath\" \"%1\"")
            Logger.d(TAG, "$scheme:// registered successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register $scheme:// handler: ${e.message}")
        }
    }

    private fun isAlreadyRegistered(
        scheme: String,
        currentExePath: String,
    ): Boolean =
        runCatching {
            val result = regQuery("${hkcuSubKeyOf(scheme)}\\shell\\open\\command", null)
            val normalizedExe = currentExePath.replace("\\", "/").lowercase()
            result?.replace("\\", "/")?.lowercase()?.contains(normalizedExe) == true
        }.getOrDefault(false)

    fun resolveExePath(): String? {
        val javaHome = System.getProperty("java.home") ?: return null
        val javaHomeDir = File(javaHome)

        val appDir =
            if (javaHomeDir.name == "runtime") {
                javaHomeDir.parentFile
            } else {
                generateSequence(javaHomeDir) { it.parentFile }
                    .firstOrNull { it.name == "runtime" }
                    ?.parentFile
            }

        if (appDir != null) {
            val candidateNames = listOf("Pulse.exe", "PulseMusic.exe", "Pulse Music.exe")
            for (name in candidateNames) {
                val candidate = File(appDir, name)
                if (candidate.exists()) {
                    return candidate.absolutePath
                }
            }
        }

        return ProcessHandle.current().info().command().orElse(null)
    }

    private fun regSet(
        hkcuSubKey: String,
        valueName: String?,
        data: String,
    ) {
        Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, hkcuSubKey)
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, hkcuSubKey, valueName, data)
    }

    private fun regQuery(
        hkcuSubKey: String,
        valueName: String?,
    ): String? {
        if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, hkcuSubKey)) return null
        return if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, hkcuSubKey, valueName)) {
            Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, hkcuSubKey, valueName)
        } else {
            null
        }
    }
}
