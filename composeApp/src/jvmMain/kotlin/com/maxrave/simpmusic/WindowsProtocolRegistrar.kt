package com.maxrave.simpmusic

import com.maxrave.logger.Logger

/**
 * Registers the "simpmusic://" custom URI protocol handler in Windows Registry
 * under HKEY_CURRENT_USER (no admin rights required).
 *
 * Registry structure:
 * ```
 * HKCU\Software\Classes\simpmusic
 *     (Default) = "URL:SimpMusic Protocol"
 *     URL Protocol = ""
 *     \DefaultIcon
 *         (Default) = "\"<exe_path>\",0"
 *     \shell\open\command
 *         (Default) = "\"<exe_path>\" \"%1\""
 * ```
 */
object WindowsProtocolRegistrar {
    private const val TAG = "WindowsProtocolRegistrar"
    private const val SCHEME = "simpmusic"

    /**
     * The Last.fm auth callback. Its scheme is fixed by the callback URL registered on the Last.fm
     * API account, so it cannot be folded into "simpmusic".
     */
    private const val LASTFM_SCHEME = "wordbyword"

    private fun regKeyOf(scheme: String) = "HKCU\\Software\\Classes\\$scheme"

    fun register() {
        if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return

        val exePath = resolveExePath() ?: run {
            Logger.e(TAG, "Could not resolve executable path, skipping protocol registration")
            return
        }

        register("pulsemusic", "URL:Pulse Music Protocol", exePath)
        register(SCHEME, "URL:Pulse Music Protocol", exePath)
        register(LASTFM_SCHEME, "URL:Pulse Music Last.fm Callback", exePath)
        registerApplicationIdentity(exePath)
    }

    private fun registerApplicationIdentity(exePath: String) {
        try {
            val exeFile = java.io.File(exePath)
            val appDir = exeFile.parentFile

            // Ensure a standalone .ico file exists in AppData for reliable Windows SMTC icon resolution
            val userHome = System.getProperty("user.home") ?: System.getenv("USERPROFILE") ?: ""
            val pulseAppDataDir = java.io.File(System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming", "PulseMusic")
            pulseAppDataDir.mkdirs()
            val appDataIco = java.io.File(pulseAppDataDir, "PulseMusic.ico")

            val icoCandidates = listOfNotNull(
                appDir?.let { java.io.File(it, "PulseMusic.ico") },
                appDir?.let { java.io.File(it, "Pulse.ico") },
                appDir?.let { java.io.File(it, "icon.ico") },
            )
            val foundIco = icoCandidates.firstOrNull { it.exists() }
            if (foundIco != null && (!appDataIco.exists() || appDataIco.length() == 0L)) {
                runCatching { foundIco.copyTo(appDataIco, overwrite = true) }
            } else if (!appDataIco.exists() || appDataIco.length() == 0L) {
                runCatching {
                    val stream = WindowsProtocolRegistrar::class.java.getResourceAsStream("/circle_app_icon.ico")
                        ?: WindowsProtocolRegistrar::class.java.getResourceAsStream("/icon.ico")
                    stream?.use { input ->
                        appDataIco.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }

            val iconPath = when {
                appDataIco.exists() && appDataIco.length() > 0L -> appDataIco.absolutePath
                foundIco != null -> foundIco.absolutePath
                else -> exePath
            }

            val appName = exeFile.name
            val appNames = listOf(appName, "Pulse.exe", "PulseMusic.exe")
            for (name in appNames) {
                val regAppKey = "HKCU\\Software\\Classes\\Applications\\$name"
                regAdd(regAppKey, null, "Pulse Music")
                regAdd(regAppKey, "FriendlyAppName", "Pulse Music")
                regAdd(regAppKey, "ApplicationCompany", "Pulse Music Studio (Muthumanikandan B)")
                regAdd("$regAppKey\\DefaultIcon", null, iconPath)
            }

            val aumids = listOf("com.pulse.music", "Pulse", "Pulse.exe", "PulseMusic")
            for (aumid in aumids) {
                val aumidKey = "HKCU\\Software\\Classes\\AppUserModelId\\$aumid"
                regAdd(aumidKey, "DisplayName", "Pulse Music")
                regAdd(aumidKey, "IconUri", iconPath)
                regAdd(aumidKey, "IconBackgroundColor", "0")
            }

            if (appDir != null) {
                val appPathsKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\$appName"
                regAdd(appPathsKey, null, exePath)
                regAdd(appPathsKey, "Path", appDir.absolutePath)
            }
            Logger.d(TAG, "Windows Application Identity registered for $appName and SMTC")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register Windows Application Identity: ${e.message}")
        }
    }

    private fun register(
        scheme: String,
        description: String,
        exePath: String,
    ) {
        val regKey = regKeyOf(scheme)
        try {
            if (isAlreadyRegistered(scheme, exePath)) {
                Logger.d(TAG, "$scheme:// already registered with correct path")
                return
            }

            Logger.d(TAG, "Registering $scheme:// protocol handler -> $exePath")

            // Main key with protocol description
            regAdd(regKey, null, description)
            regAdd(regKey, "URL Protocol", "")

            // DefaultIcon
            regAdd("$regKey\\DefaultIcon", null, "\"$exePath\",0")

            // shell\open\command
            regAdd("$regKey\\shell\\open\\command", null, "\"$exePath\" \"%1\"")

            Logger.d(TAG, "$scheme:// registered successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register $scheme:// handler: ${e.message}")
        }
    }

    private fun isAlreadyRegistered(
        scheme: String,
        currentExePath: String,
    ): Boolean {
        return try {
            val result = regQuery("${regKeyOf(scheme)}\\shell\\open\\command", null)
            // Registry stores path with quotes: "C:\path\to\Pulse.exe" "%1"
            // Normalize both for comparison
            val normalizedExe = currentExePath.replace("\\", "/").lowercase()
            result?.replace("\\", "/")?.lowercase()?.contains(normalizedExe) == true
        } catch (_: Exception) {
            false
        }
    }

    fun resolveExePath(): String? {
        // JPackage directory structure:
        //   <app>/runtime/...  (java.home points here)
        //   <app>/Pulse.exe (or PulseMusic.exe)
        // So we go: java.home → parent (runtime) → parent (app) → Pulse.exe
        val javaHome = System.getProperty("java.home") ?: return null
        val javaHomeDir = java.io.File(javaHome)

        // Try JPackage layout: java.home is <app>/runtime/... or <app>/runtime
        val appDir = if (javaHomeDir.name == "runtime") {
            javaHomeDir.parentFile
        } else {
            // java.home might be deeper, e.g., <app>/runtime/conf/...
            generateSequence(javaHomeDir) { it.parentFile }
                .firstOrNull { it.name == "runtime" }
                ?.parentFile
        }

        if (appDir != null) {
            val candidateNames = listOf("Pulse.exe", "PulseMusic.exe", "Pulse Music.exe", "SimpMusic.exe")
            for (name in candidateNames) {
                val candidate = java.io.File(appDir, name)
                if (candidate.exists()) {
                    return candidate.absolutePath
                }
            }
        }

        // Fallback: running from IDE/dev environment, use current process
        return ProcessHandle.current().info().command().orElse(null)
    }

    private fun regAdd(key: String, valueName: String?, data: String) {
        // Build command as a single string for cmd.exe to avoid
        // ProcessBuilder double-escaping embedded quotes in data
        val valueFlag = if (valueName != null) "/v \"$valueName\"" else "/ve"
        val escapedData = data.replace("\"", "\\\"")
        val cmdString = "reg add \"$key\" /f $valueFlag /t REG_SZ /d \"$escapedData\""

        val process = ProcessBuilder("cmd.exe", "/c", cmdString)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            Logger.e(TAG, "reg add failed (exit=$exitCode): $output")
        }
    }

    private fun regQuery(key: String, valueName: String?): String? {
        val command = mutableListOf("reg", "query", key)
        if (valueName != null) {
            command.addAll(listOf("/v", valueName))
        } else {
            command.add("/ve")
        }

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return if (exitCode == 0) output else null
    }
}
