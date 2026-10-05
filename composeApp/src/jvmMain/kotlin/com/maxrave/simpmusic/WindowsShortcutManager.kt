package com.maxrave.simpmusic

import com.maxrave.logger.Logger
import java.io.File

/**
 * Manages Windows Desktop and Start Menu shortcuts for Pulse Music.
 * Emulates the standard Spotify Windows desktop integration:
 * - Creates a desktop shortcut named "Pulse Music.lnk" with Pulse Music branding and logo.
 * - Creates a Start Menu Programs shortcut so users can search "Pulse Music" from the Windows taskbar.
 * - Automatically purges any legacy "SimpMusic.lnk" references to maintain clean Pulse Music branding.
 */
object WindowsShortcutManager {
    private const val TAG = "WindowsShortcutManager"
    private const val SHORTCUT_NAME = "Pulse Music.lnk"
    private const val APP_DESCRIPTION = "Pulse Music - High Fidelity Music Player"

    fun ensureShortcutsCreated() {
        if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return

        try {
            val exePath = WindowsProtocolRegistrar.resolveExePath()
            if (exePath == null || !File(exePath).exists()) {
                Logger.w(TAG, "Cannot resolve executable path for desktop shortcut")
                return
            }

            val exeFile = File(exePath)
            val appDir = exeFile.parentFile ?: return

            // Locate icon file: check app directory first, then fallback to exe
            val icoCandidate = File(appDir, "PulseMusic.ico")
            val iconPath = if (icoCandidate.exists()) icoCandidate.absolutePath else exeFile.absolutePath

            val userHome = System.getProperty("user.home") ?: System.getenv("USERPROFILE") ?: return
            val desktopDir = File(userHome, "Desktop")
            val startMenuDir = File(System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming", "Microsoft\\Windows\\Start Menu\\Programs")

            // 1. Purge legacy SimpMusic shortcuts from Desktop and Start Menu
            removeLegacyShortcuts(desktopDir, startMenuDir)

            // 2. Create Desktop Shortcut if missing
            if (desktopDir.exists()) {
                val desktopShortcut = File(desktopDir, SHORTCUT_NAME)
                if (!desktopShortcut.exists()) {
                    createShortcut(
                        shortcutFile = desktopShortcut,
                        targetPath = exeFile.absolutePath,
                        workingDir = appDir.absolutePath,
                        iconPath = iconPath,
                        description = APP_DESCRIPTION,
                    )
                    Logger.d(TAG, "Created desktop shortcut at: ${desktopShortcut.absolutePath}")
                }
            }

            // 3. Create Start Menu Shortcut if missing
            if (startMenuDir.exists()) {
                val startMenuShortcut = File(startMenuDir, SHORTCUT_NAME)
                if (!startMenuShortcut.exists()) {
                    createShortcut(
                        shortcutFile = startMenuShortcut,
                        targetPath = exeFile.absolutePath,
                        workingDir = appDir.absolutePath,
                        iconPath = iconPath,
                        description = APP_DESCRIPTION,
                    )
                    Logger.d(TAG, "Created start menu shortcut at: ${startMenuShortcut.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to ensure shortcuts: ${e.message}", e)
        }
    }

    private fun removeLegacyShortcuts(desktopDir: File, startMenuDir: File) {
        val legacyNames = listOf("SimpMusic.lnk", "Simp Music.lnk")
        for (legacy in legacyNames) {
            val desktopLegacy = File(desktopDir, legacy)
            if (desktopLegacy.exists()) {
                runCatching { desktopLegacy.delete() }
            }
            val startMenuLegacy = File(startMenuDir, legacy)
            if (startMenuLegacy.exists()) {
                runCatching { startMenuLegacy.delete() }
            }
        }
    }

    private fun createShortcut(
        shortcutFile: File,
        targetPath: String,
        workingDir: String,
        iconPath: String,
        description: String,
    ) {
        // Use PowerShell to create Windows Shell Link (.lnk)
        val script = """
            ${'$'}ws = New-Object -ComObject WScript.Shell
            ${'$'}s = ${'$'}ws.CreateShortcut('${shortcutFile.absolutePath.replace("'", "''")}')
            ${'$'}s.TargetPath = '${targetPath.replace("'", "''")}'
            ${'$'}s.WorkingDirectory = '${workingDir.replace("'", "''")}'
            ${'$'}s.IconLocation = '${iconPath.replace("'", "''")},0'
            ${'$'}s.Description = '${description.replace("'", "''")}'
            ${'$'}s.Save()
        """.trimIndent()

        val process = ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val err = process.inputStream.bufferedReader().readText()
            Logger.w(TAG, "PowerShell shortcut creation exit $exitCode: $err")
        }
    }
}
