package com.maxrave.simpmusic.expect

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

actual suspend fun downloadAndInstallAppUpdate(
    update: UpdateData,
    onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    onReadyToInstall: () -> Unit,
    onError: (String) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val asset = update.getWindowsSetupAsset()
        val downloadUrl = asset?.downloadUrl
            ?: "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/${update.tagName}/PulseMusic-Windows-x64-Setup.exe"
        val totalSize = asset?.size ?: 0L

        Logger.i(TAG, "Starting desktop update download from: $downloadUrl")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "PulseMusicUpdates")
        if (!tempDir.exists()) tempDir.mkdirs()

        val setupFile = File(tempDir, "PulseMusic-Windows-x64-Setup-${update.tagName}.exe")
        val partFile = File(tempDir, "PulseMusic-Windows-x64-Setup-${update.tagName}.exe.part")
        if (partFile.exists()) partFile.delete()

        downloadWithRedirects(downloadUrl, partFile, totalSize, onProgress)

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
            Logger.i(TAG, "Launching Windows installer: ${setupFile.absolutePath}")
            val pb = ProcessBuilder(
                setupFile.absolutePath,
                "/SILENT",
                "/CLOSEAPPLICATIONS",
                "/RESTARTAPPLICATIONS",
            )
            pb.directory(tempDir)
            pb.start()
            delay(1000)
            exitProcess(0)
        } else {
            openUrl(downloadUrl)
        }
    } catch (e: Exception) {
        Logger.e(TAG, "Error downloading update: ${e.message}", e)
        onError(e.message ?: "Failed to download update.")
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
        conn.setRequestProperty("User-Agent", "PulseMusicApp")
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
