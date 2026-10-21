package com.maxrave.simpmusic.expect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.manikandan.domain.data.model.update.UpdateData
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AppUpdateAndroid"

actual suspend fun downloadAndInstallAppUpdate(
    update: UpdateData,
    onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    onReadyToInstall: () -> Unit,
    onError: (String) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val context: AppCompatActivity = getKoin().get()
        val primaryAbi = android.os.Build.SUPPORTED_ABIS.firstOrNull()
        val asset = update.getAndroidApkAsset(primaryAbi)
        val downloadUrl = asset?.downloadUrl
            ?: "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/${update.tagName}/PulseMusic-${update.tagName}-arm64-v8a.apk"
        val totalSize = asset?.size ?: 0L

        Logger.i(TAG, "Starting ultra-fast Android update download (ABI: $primaryAbi) from: $downloadUrl")
        val baseDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.externalCacheDir
            ?: File(context.cacheDir, "updates")
        val updateDir = File(baseDir, "updates")
        if (!updateDir.exists()) updateDir.mkdirs()

        val apkFile = File(updateDir, "PulseMusic-${update.tagName}.apk")
        val partFile = File(updateDir, "PulseMusic-${update.tagName}.apk.part")
        if (partFile.exists()) partFile.delete()

        downloadWithRedirects(downloadUrl, partFile, totalSize, onProgress)

        if (!partFile.exists() || partFile.length() < 1024 * 1024) {
            onError("Downloaded APK file is incomplete.")
            return@withContext
        }

        if (apkFile.exists()) apkFile.delete()
        if (!partFile.renameTo(apkFile)) {
            partFile.copyTo(apkFile, overwrite = true)
            partFile.delete()
        }
        apkFile.setReadable(true, false)

        onReadyToInstall()

        withContext(Dispatchers.Main) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.FileProvider",
                    apkFile,
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resInfoList = context.packageManager.queryIntentActivities(
                    installIntent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
                )
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to launch package installer: ${e.message}", e)
                onError("Failed to launch package installer: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Logger.e(TAG, "Error downloading Android update: ${e.message}", e)
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
            val contentLength = if (conn.contentLengthLong > 0) conn.contentLengthLong else expectedTotal
            BufferedInputStream(conn.inputStream, 256 * 1024).use { input ->
                BufferedOutputStream(FileOutputStream(destination), 256 * 1024).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var bytesRead: Int
                    var downloaded: Long = 0
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 200 || (contentLength > 0 && downloaded == contentLength)) {
                            lastReportTime = now
                            val progress = if (contentLength > 0) downloaded.toFloat() / contentLength.toFloat() else 0f
                            onProgress(progress, downloaded, contentLength)
                        }
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
