package com.manikandan.domain.data.update

import com.manikandan.domain.data.model.update.UpdateAsset
import com.manikandan.domain.data.model.update.UpdateData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateVersionTest {

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    @Test
    fun testVersionComparison() {
        // Standard semantic version bumps
        assertTrue(isNewerVersion("3.1.8", "3.1.7"))
        assertTrue(isNewerVersion("v3.1.8", "3.1.7"))
        assertTrue(isNewerVersion("3.2.0", "3.1.8"))
        assertTrue(isNewerVersion("4.0.0", "3.1.8"))
        assertTrue(isNewerVersion("3.1.10", "3.1.9"))

        // Same version
        assertFalse(isNewerVersion("3.1.7", "3.1.7"))
        assertFalse(isNewerVersion("v3.1.7", "3.1.7"))
        assertFalse(isNewerVersion("3.1.7", "v3.1.7"))

        // Older version
        assertFalse(isNewerVersion("3.1.6", "3.1.7"))
        assertFalse(isNewerVersion("2.9.9", "3.0.0"))

        // Pre-release or suffix handling
        assertTrue(isNewerVersion("v3.1.8-beta", "3.1.7"))
        assertFalse(isNewerVersion("v3.1.7-beta", "3.1.7"))
    }

    @Test
    fun testGetAndroidApkAsset() {
        val assets = listOf(
            UpdateAsset(name = "PulseMusic-arm64-v8a.apk", downloadUrl = "https://example.com/arm64.apk", size = 1000L),
            UpdateAsset(name = "PulseMusic-Universal.apk", downloadUrl = "https://example.com/universal.apk", size = 2000L),
            UpdateAsset(name = "PulseMusic-Setup.exe", downloadUrl = "https://example.com/setup.exe", size = 3000L),
        )
        val update = UpdateData(
            tagName = "v3.1.8",
            releaseTime = "2026-10-20T13:00:00Z",
            body = "Changelog",
            assets = assets,
        )

        // Native ABI (arm64-v8a) prioritized by default for 2x faster download
        val androidAsset = update.getAndroidApkAsset()
        assertNotNull(androidAsset)
        assertEquals("PulseMusic-arm64-v8a.apk", androidAsset.name)
        assertEquals("https://example.com/arm64.apk", androidAsset.downloadUrl)

        // Explicit preferred ABI
        val universalAsset = update.getAndroidApkAsset("universal")
        assertNotNull(universalAsset)
        assertEquals("PulseMusic-Universal.apk", universalAsset.name)

        val windowsAsset = update.getWindowsSetupAsset()
        assertNotNull(windowsAsset)
        assertEquals("PulseMusic-Setup.exe", windowsAsset.name)
    }

    @Test
    fun testGetAndroidApkAssetFallback() {
        val assets = listOf(
            UpdateAsset(name = "PulseMusic-Universal.apk", downloadUrl = "https://example.com/universal.apk", size = 2000L),
        )
        val update = UpdateData(
            tagName = "v3.1.8",
            releaseTime = "2026-10-20T13:00:00Z",
            body = "Changelog",
            assets = assets,
        )

        val androidAsset = update.getAndroidApkAsset()
        assertNotNull(androidAsset)
        assertEquals("PulseMusic-Universal.apk", androidAsset.name)
    }
}
