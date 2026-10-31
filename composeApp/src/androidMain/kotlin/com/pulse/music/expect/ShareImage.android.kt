package com.pulse.music.expect

import android.content.ContentValues
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

private const val TAG = "ShareImage"
private const val MIME_PNG = "image/png"
private const val ALBUM = "Pulse"

actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()
            val resolver = context.contentResolver
            val isScopedStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            val values =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                    if (isScopedStorage) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                        // Marks the row incomplete so no gallery app indexes a half-written file.
                        // Cleared below once the bytes are on disk.
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

            val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return@runCatching false

            if (isScopedStorage) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse { error ->
            // Below API 29 this insert needs WRITE_EXTERNAL_STORAGE to have been granted at
            // runtime, and nothing in the app asks for it — so a save on Android 8 or 9 lands
            // here rather than succeeding. Reported to the caller, never swallowed.
            Logger.e(TAG, "Could not save $fileName to the gallery: ${error.message}")
            false
        }
    }

actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()

            // cacheDir, which provider_paths.xml already exposes as `cache`. The gallery copy is a
            // separate, explicit action — sharing must not silently write to the user's photos.
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = MIME_PNG
                    putExtra(Intent.EXTRA_STREAM, uri)
                    // Without this the receiving app gets a uri it is not allowed to open.
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share $fileName: ${error.message}")
            false
        }
    }

actual fun isSnapchatAvailable(): Boolean {
    val context: AppCompatActivity = runCatching { getKoin().get<AppCompatActivity>() }.getOrNull() ?: return false
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo("com.snapchat.android", android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo("com.snapchat.android", 0)
        }
        true
    }.getOrDefault(false)
}

actual suspend fun shareToSnapchat(
    bytes: ByteArray,
    fileName: String,
    songTitle: String,
    artistName: String,
    deepLinkUrl: String,
    caption: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()

            // 1. Write the card to the app's cache directory
            val dir = File(context.cacheDir, "snapchat_cards").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)

            // 2. Build Snapchat Creative Kit share intent
            // Snapchat's Creative Kit intent action targets com.snapchat.android
            val snapIntent =
                Intent("com.snapchat.android.CREATIVE_KIT_SDK_SHARE").apply {
                    setPackage("com.snapchat.android")
                    // Photo payload
                    putExtra("com.snapchat.android.sdk.creative.media_uri", uri)
                    // Caption
                    putExtra("com.snapchat.android.sdk.creative.caption_text", caption)
                    // Context card swipe-up attachment link
                    putExtra("com.snapchat.android.sdk.creative.attachment_url", deepLinkUrl)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                }

            if (snapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(snapIntent)
                true
            } else {
                // Fallback: standard ACTION_SEND targeted to Snapchat package
                val fallbackIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = MIME_PNG
                        setPackage("com.snapchat.android")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "$caption\n$deepLinkUrl")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    }
                if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(fallbackIntent)
                    true
                } else {
                    false
                }
            }
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share to Snapchat: ${error.message}")
            false
        }
    }
