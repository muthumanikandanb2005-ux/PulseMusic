package com.pulse.music

import com.manikandan.logger.Logger
import java.io.File

/**
 * Cleans up legacy shortcut filenames from Desktop and Start Menu.
 *
 * Current Windows Desktop and Start Menu shortcuts ("Pulse Music.lnk") are created cleanly by
 * the Inno Setup installer (`[Icons]`) with the matching `AppUserModelID`, avoiding hidden
 * runtime `powershell.exe` / `WScript.Shell` subprocess invocations that trip Defender/EDR
 * behavioral heuristics.
 */
object WindowsShortcutManager {
    private const val TAG = "WindowsShortcutManager"

    fun ensureShortcutsCreated() {
        if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return

        try {
            val userHome = System.getProperty("user.home") ?: System.getenv("USERPROFILE") ?: return
            val desktopDir = File(userHome, "Desktop")
            val startMenuDir =
                File(
                    System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming",
                    "Microsoft\\Windows\\Start Menu\\Programs",
                )
            removeLegacyShortcuts(desktopDir, startMenuDir)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to inspect shortcuts: ${e.message}", e)
        }
    }

    private fun removeLegacyShortcuts(
        desktopDir: File,
        startMenuDir: File,
    ) {
        val legacyNames = listOf("PulseMusic.lnk", "Simp Music.lnk")
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
}
