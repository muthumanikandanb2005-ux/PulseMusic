package com.maxrave.simpmusic.service.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.service.test.notification.NotificationHandler
import com.maxrave.simpmusic.utils.VersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            try {
                Logger.i(TAG, "Starting Pulse App Update check...")

                val isAutoCheckEnabled = dataStoreManager.autoCheckForUpdates.first()
                if (isAutoCheckEnabled != DataStoreManager.TRUE) {
                    Logger.i(TAG, "Auto check for updates is disabled, skipping...")
                    return@withContext Result.success()
                }

                val rawEndpoint = dataStoreManager.customUpdateEndpoint.first().trim()
                val endpoint = when {
                    rawEndpoint.isBlank() -> "https://api.github.com/repos/manikandan-dev/PulseMusic/releases/latest"
                    !rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://") && rawEndpoint.contains("/") ->
                        "https://api.github.com/repos/$rawEndpoint/releases/latest"
                    else -> rawEndpoint
                }

                // Record check timestamp
                val nowMillis = System.currentTimeMillis()
                dataStoreManager.putString("CheckForUpdateAt", nowMillis.toString())

                Logger.i(TAG, "Fetching update metadata from: $endpoint")
                val responseJsonStr = fetchString(endpoint)
                if (responseJsonStr.isBlank()) {
                    Logger.w(TAG, "Empty response from update endpoint")
                    return@withContext Result.success()
                }

                val json = JSONObject(responseJsonStr)
                var remoteVersion = ""
                var downloadUrl = ""

                if (json.has("tag_name")) {
                    // GitHub Release format
                    remoteVersion = json.getString("tag_name").trim()
                    if (json.has("assets")) {
                        val assets = json.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            val assetUrl = asset.optString("browser_download_url", "")
                            if (name.endsWith(".apk", ignoreCase = true) || assetUrl.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = assetUrl
                                break
                            }
                        }
                    }
                } else if (json.has("version")) {
                    // Custom JSON format: {"version": "1.0.1", "downloadUrl": "..."}
                    remoteVersion = json.getString("version").trim()
                    downloadUrl = json.optString("downloadUrl", "")
                    if (downloadUrl.isEmpty()) {
                        downloadUrl = json.optString("download_url", "")
                    }
                }

                val currentVersion = VersionManager.getVersionName()
                Logger.i(TAG, "Current version: $currentVersion, Remote version: $remoteVersion, URL: $downloadUrl")

                if (remoteVersion.isNotEmpty() && downloadUrl.isNotEmpty() && isNewerVersion(remoteVersion, currentVersion)) {
                    Logger.i(TAG, "New version $remoteVersion detected! Starting background download...")
                    val updateDir = File(context.cacheDir, "updates")
                    if (!updateDir.exists()) {
                        updateDir.mkdirs()
                    }

                    val apkFile = File(updateDir, "PulseMusic-$remoteVersion.apk")
                    val tempFile = File(updateDir, "PulseMusic-$remoteVersion.apk.tmp")

                    downloadFileWithRedirects(downloadUrl, tempFile)

                    if (tempFile.exists() && tempFile.length() > 1024 * 1024) { // at least 1MB
                        if (apkFile.exists()) {
                            apkFile.delete()
                        }
                        tempFile.renameTo(apkFile)
                        Logger.i(TAG, "Update downloaded successfully to ${apkFile.absolutePath} (${apkFile.length()} bytes)")

                        NotificationHandler.createAppUpdateNotificationChannel(applicationContext)
                        NotificationHandler.createAppUpdateNotification(applicationContext, remoteVersion, apkFile)
                    } else {
                        Logger.w(TAG, "Downloaded update APK is missing or too small (${tempFile.length()} bytes)")
                        if (tempFile.exists()) tempFile.delete()
                    }
                } else {
                    Logger.i(TAG, "App is up to date.")
                }

                Result.success()
            } catch (e: Exception) {
                Logger.e(TAG, "Error checking or downloading app update: ${e.message}", e)
                Result.retry()
            }
        }

    private fun fetchString(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "PulseMusicApp")
        conn.connect()

        return conn.inputStream.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                reader.readText()
            }
        }
    }

    private fun downloadFileWithRedirects(urlStr: String, destination: File, maxRedirects: Int = 5) {
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
                conn.inputStream.use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
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

    companion object {
        private const val TAG = "PulseAppUpdateWorker"

        fun isNewerVersion(remote: String, current: String): Boolean {
            val cleanRemote = remote.trim().removePrefix("v").substringBefore("-")
            val cleanCurrent = current.trim().removePrefix("v").substringBefore("-")
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
    }
}
