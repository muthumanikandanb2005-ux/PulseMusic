package com.pulse.music.service.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manikandan.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.pulse.music.service.test.notification.NotificationHandler
import com.pulse.music.utils.VersionManager
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
                VersionManager.initialize()
                Logger.i(TAG, "Starting Pulse App Update check...")

                val isAutoCheckEnabled = dataStoreManager.autoCheckForUpdates.first()
                if (isAutoCheckEnabled != DataStoreManager.TRUE) {
                    Logger.i(TAG, "Auto check for updates is disabled, skipping...")
                    return@withContext Result.success()
                }

                val rawEndpoint = dataStoreManager.customUpdateEndpoint.first().trim()
                val endpoints = mutableListOf<String>()
                val cacheBuster = System.currentTimeMillis()

                // Primary freshest: raw GitHub with cache buster
                endpoints.add("https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json?t=$cacheBuster")
                // Secondary fallback: GitHub latest release API
                endpoints.add("https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest")
                // Fast CDN endpoint
                endpoints.add("https://cdn.jsdelivr.net/gh/muthumanikandanb2005-ux/PulseMusic@main/version.json")

                if (rawEndpoint.isNotBlank() &&
                    !rawEndpoint.contains("maxrave-dev", ignoreCase = true) &&
                    !rawEndpoint.contains("simpmusic", ignoreCase = true)
                ) {
                    val formatted = if (!rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://") && rawEndpoint.contains("/")) {
                        "https://api.github.com/repos/$rawEndpoint/releases/latest"
                    } else {
                        rawEndpoint
                    }
                    endpoints.add(0, formatted)
                }

                // Record check timestamp
                val nowMillis = System.currentTimeMillis()
                dataStoreManager.putString("CheckForUpdateAt", nowMillis.toString())

                val currentVersion = VersionManager.getVersionName()
                var remoteVersion = ""
                var downloadUrl = ""

                for (endpoint in endpoints.distinct()) {
                    try {
                        Logger.i(TAG, "Fetching update metadata from: $endpoint")
                        val str = fetchString(endpoint)
                        if (str.isNotBlank()) {
                            val json = JSONObject(str)
                            var curVer = ""
                            var curUrl = ""

                            if (json.has("tag_name")) {
                                curVer = json.getString("tag_name").trim()
                                if (json.has("assets")) {
                                    val assets = json.getJSONArray("assets")
                                    val apkList = mutableListOf<Pair<String, String>>()
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.getJSONObject(i)
                                        val name = asset.optString("name", "")
                                        val assetUrl = asset.optString("browser_download_url", "")
                                        if (name.endsWith(".apk", ignoreCase = true) || assetUrl.endsWith(".apk", ignoreCase = true)) {
                                            apkList.add(name to assetUrl)
                                        }
                                    }
                                    val primaryAbi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
                                    curUrl = apkList.firstOrNull { it.first.contains(primaryAbi, ignoreCase = true) }?.second
                                        ?: apkList.firstOrNull { it.first.contains("arm64-v8a", ignoreCase = true) }?.second
                                        ?: apkList.firstOrNull { it.first.contains("armeabi-v7a", ignoreCase = true) }?.second
                                        ?: apkList.firstOrNull { it.first.contains("universal", ignoreCase = true) }?.second
                                        ?: apkList.firstOrNull()?.second ?: ""
                                }
                            } else if (json.has("version")) {
                                curVer = json.getString("version").trim()
                                curUrl = json.optString("downloadUrl", "")
                                if (curUrl.isEmpty()) {
                                    curUrl = json.optString("download_url", "")
                                }
                            }

                            if (curVer.isNotBlank() && curUrl.isNotBlank()) {
                                if (remoteVersion.isBlank() || isNewerVersion(curVer, remoteVersion)) {
                                    remoteVersion = curVer
                                    downloadUrl = curUrl
                                    Logger.i(TAG, "Discovered newest release candidate $remoteVersion from $endpoint")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to fetch from $endpoint: ${e.message}")
                    }
                }

                Logger.i(TAG, "Current version: $currentVersion, Remote version: $remoteVersion, URL: $downloadUrl")

                if (remoteVersion.isNotEmpty() && downloadUrl.isNotEmpty() && isNewerVersion(remoteVersion, currentVersion)) {
                    Logger.i(TAG, "New version $remoteVersion detected! Posting update notification immediately...")
                    NotificationHandler.createAppUpdateNotificationChannel(applicationContext)
                    NotificationHandler.postUpdateAvailableNotification(
                        context = applicationContext,
                        versionName = remoteVersion.trim().removePrefix("v").removePrefix("V"),
                        downloadUrl = downloadUrl,
                    )

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
        conn.setRequestProperty("User-Agent", "PulseMusicFastDownloader")
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Accept", "application/json, text/plain, */*")
        conn.connect()

        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw IOException("HTTP error $code from $urlStr")
        }

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
            conn.setRequestProperty("User-Agent", "PulseMusicFastDownloader")
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.setRequestProperty("Connection", "Keep-Alive")
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
                java.io.BufferedInputStream(conn.inputStream, 256 * 1024).use { input ->
                    java.io.BufferedOutputStream(destination.outputStream(), 256 * 1024).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
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
    }
}
