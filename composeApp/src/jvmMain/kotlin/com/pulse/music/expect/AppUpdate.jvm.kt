package com.pulse.music.expect

import com.manikandan.domain.data.model.update.UpdateData
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

private const val TAG = "AppUpdateJvm"

private fun queryLatestWindowsSetupAsset(): Pair<String, Long>? {
    return try {
        val url = URL("https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "PulseMusicApp/3.2.3")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.connect()
        if (conn.responseCode in 200..299) {
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val regex = """"browser_download_url":\s*"([^"]+?\.(?:exe|msi))"""".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(jsonText)
            if (match != null) {
                val dlUrl = match.groupValues[1]
                dlUrl to 0L
            } else null
        } else null
    } catch (e: Exception) {
        Logger.w(TAG, "Failed to query latest releases: ${e.message}")
        null
    }
}

actual suspend fun downloadAndInstallAppUpdate(
    update: UpdateData,
    onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    onReadyToInstall: () -> Unit,
    onError: (String) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val asset = update.getWindowsSetupAsset()
        var downloadUrl = asset?.downloadUrl
        var totalSize = asset?.size ?: 0L

        if (downloadUrl == null) {
            Logger.i(TAG, "No direct Windows asset in update data, querying GitHub releases API...")
            val latestWindowsAsset = queryLatestWindowsSetupAsset()
            if (latestWindowsAsset != null) {
                downloadUrl = latestWindowsAsset.first
                totalSize = latestWindowsAsset.second
                Logger.i(TAG, "Discovered latest Windows release installer: $downloadUrl")
            }
        }

        if (downloadUrl == null) {
            downloadUrl = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.1.8/PulseMusic-Windows-x64-Setup.exe"
            Logger.i(TAG, "Falling back to verified Windows installer: $downloadUrl")
        }

        Logger.i(TAG, "Starting desktop update download from: $downloadUrl")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "PulseMusicUpdates")
        if (!tempDir.exists()) tempDir.mkdirs()

        val setupFile = File(tempDir, "PulseMusic-Windows-x64-Setup-${update.tagName}.exe")
        val partFile = File(tempDir, "PulseMusic-Windows-x64-Setup-${update.tagName}.exe.part")
        if (partFile.exists()) partFile.delete()

        try {
            downloadWithRedirects(downloadUrl, partFile, totalSize, onProgress)
        } catch (downloadEx: Exception) {
            Logger.w(TAG, "Initial download failed: ${downloadEx.message}, attempting fallback...")
            val fallbackUrl = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.1.8/PulseMusic-Windows-x64-Setup.exe"
            if (downloadUrl != fallbackUrl) {
                downloadWithRedirects(fallbackUrl, partFile, 0L, onProgress)
            } else {
                throw downloadEx
            }
        }

        if (!partFile.exists() || partFile.length() < 1024 * 1024) {
            onError("Downloaded update installer file is incomplete.")
            return@withContext
        }

        if (setupFile.exists()) setupFile.delete()
        if (!partFile.renameTo(setupFile)) {
            partFile.copyTo(setupFile, overwrite = true)
            partFile.delete()
        }

        onReadyToInstall()
        delay(1000)

        val isWindows = System.getProperty("os.name", "").contains("Windows", ignoreCase = true)
        if (isWindows) {
            Logger.i(TAG, "Preparing detached update runner for Windows installer: ${setupFile.absolutePath}")
            val runnerFile = File(tempDir, "pulse_update_runner.bat")
            val batchScript = buildString {
                appendLine("@echo off")
                appendLine("timeout /t 2 /nobreak >nul")
                appendLine("taskkill /F /IM Pulse.exe >nul 2>&1")
                appendLine("timeout /t 1 /nobreak >nul")
                appendLine("\"${setupFile.absolutePath}\" /SP- /SILENT /FORCECLOSEAPPLICATIONS")
                appendLine("timeout /t 2 /nobreak >nul")
                appendLine("set \"PULSE_EXE=%LOCALAPPDATA%\\Programs\\Pulse Music\\Pulse.exe\"")
                appendLine("if exist \"%PULSE_EXE%\" (")
                appendLine("    start \"\" \"%PULSE_EXE%\"")
                appendLine(")")
                appendLine("exit")
            }
            runnerFile.writeText(batchScript)

            Logger.i(TAG, "Launching Windows update runner detached: ${runnerFile.absolutePath}")
            val pb = ProcessBuilder(
                "cmd.exe",
                "/c",
                "start",
                "\"PulseMusicUpdater\"",
                "/min",
                runnerFile.absolutePath
            )
            pb.directory(tempDir)
            pb.start()
            delay(500)
            exitProcess(0)
        } else {
            openUrl(downloadUrl)
        }
    } catch (e: Exception) {
        Logger.e(TAG, "Error downloading update: ${e.message}", e)
        runCatching { openUrl("https://github.com/muthumanikandanb2005-ux/PulseMusic/releases") }
        onError(e.message ?: "Failed to download update. Opened releases page in browser.")
    }
}

private fun downloadWithRedirects(
    urlStr: String,
    destination: File,
    expectedTotal: Long,
    onProgress: (Float, Long, Long) -> Unit,
    maxRedirects: Int = 10,
) {
    var currentUrl = urlStr
    var redirects = 0
    while (redirects < maxRedirects) {
        val url = URL(currentUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "PulseMusicApp/3.2.3")
        conn.connect()

        val status = conn.responseCode
        if (status in 300..399) {
            val location = conn.getHeaderField("Location")
                ?: throw IOException("Redirect without Location header")
            currentUrl = location
            redirects++
            conn.disconnect()
            continue
        } else if (status in 200..299) {
            val contentLength = if (conn.contentLengthLong > 0) conn.contentLengthLong else expectedTotal
            conn.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var downloaded: Long = 0
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 100 || (contentLength > 0 && downloaded == contentLength)) {
                            lastReportTime = now
                            val progress = if (contentLength > 0) downloaded.toFloat() / contentLength.toFloat() else 0f
                            onProgress(progress, downloaded, contentLength)
                        }
                    }
                }
            }
            conn.disconnect()
            return
        } else {
            conn.disconnect()
            throw IOException("HTTP error $status while downloading update")
        }
    }
    throw IOException("Too many redirects ($redirects)")
}
