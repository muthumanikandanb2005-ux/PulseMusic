package com.pulse.music.expect

import android.app.PendingIntent
import android.content.ClipData
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

            // Explicitly grant read URI permission to Snapchat process
            context.grantUriPermission(
                "com.snapchat.android",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )

            // 2. Snapchat Direct Snap Sharing (Must go as a Snap/Story, NEVER as a 1-on-1 Chat)
            // Snapchat's Creative Kit intent contract uses custom URI schemes:
            // - "snapchat://creativekit/preview": opens directly into Snap Preview/Editor canvas
            // - "snapchat://creativekit/camera": opens directly into Snap Camera with sticker overlay
            // Plain ACTION_SEND without these URIs gets routed by Snapchat to its SendTo/Chat screen.
            val previewUri = Uri.parse("snapchat://creativekit/preview")
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    9834,
                    Intent(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_ONE_SHOT
                    },
                )

            // Primary method: Official Snap Creative Kit Lite - Share to Preview (Opens directly in Snap Editor as a Snap)
            val snapPreviewIntent =
                Intent(Intent.ACTION_SEND).apply {
                    setPackage("com.snapchat.android")
                    setDataAndType(previewUri, MIME_PNG)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, "Snap", uri)
                    putExtra("CLIENT_ID", "01a6f1e4-5b9c-46fa-a9ca-f7fc9f09f112")
                    putExtra("CLIENT_APP_NAME", "Pulse Music")
                    if (caption.isNotBlank()) {
                        putExtra("captionText", caption)
                    }
                    if (deepLinkUrl.isNotBlank()) {
                        putExtra("attachmentUrl", deepLinkUrl)
                    }
                    putExtra("RESULT_INTENT", pendingIntent)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

            val pm = context.packageManager
            val canResolvePreview =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(snapPreviewIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0)).isNotEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(snapPreviewIntent, 0).isNotEmpty()
                }

            if (canResolvePreview) {
                context.startActivity(snapPreviewIntent)
                Logger.d(TAG, "Opened Snapchat via snapchat://creativekit/preview as Snap")
                return@runCatching true
            }

            // Secondary method: Official Snap Creative Kit Lite - Share to Camera with Sticker
            val cameraUri = Uri.parse("snapchat://creativekit/camera")
            val snapCameraIntent =
                Intent(Intent.ACTION_SEND).apply {
                    setPackage("com.snapchat.android")
                    setDataAndType(cameraUri, "*/*")
                    clipData = ClipData.newUri(context.contentResolver, "Snap", uri)
                    putExtra("CLIENT_ID", "01a6f1e4-5b9c-46fa-a9ca-f7fc9f09f112")
                    putExtra("CLIENT_APP_NAME", "Pulse Music")
                    if (caption.isNotBlank()) {
                        putExtra("captionText", caption)
                    }
                    try {
                        val stickerJson =
                            JSONObject().apply {
                                put("uri", uri)
                                put("posX", 0.5)
                                put("posY", 0.5)
                                put("rotation", 0)
                                put("widthDp", 280)
                                put("heightDp", 450)
                            }
                        putExtra("sticker", stickerJson.toString())
                    } catch (_: Exception) {}
                    putExtra("RESULT_INTENT", pendingIntent)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

            val canResolveCamera =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(snapCameraIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0)).isNotEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(snapCameraIntent, 0).isNotEmpty()
                }

            if (canResolveCamera) {
                context.startActivity(snapCameraIntent)
                Logger.d(TAG, "Opened Snapchat via snapchat://creativekit/camera as Snap")
                return@runCatching true
            }

            // Tertiary method: Explicitly target Snapchat LandingPageActivity (main Snap / Camera screen)
            try {
                val landingIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        component = ComponentName("com.snapchat.android", "com.snapchat.android.LandingPageActivity")
                        type = MIME_PNG
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, "Snap", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                context.startActivity(landingIntent)
                Logger.d(TAG, "Opened Snapchat LandingPageActivity as Snap")
                return@runCatching true
            } catch (e: Exception) {
                Logger.w(TAG, "LandingPageActivity launch failed: ${e.message}")
            }

            // Final direct attempt with snapPreviewIntent (bypassing queryIntentActivities restrictions)
            context.startActivity(snapPreviewIntent)
            Logger.d(TAG, "Direct launch of snapPreviewIntent succeeded")
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share to Snapchat: ${error.message}")
            false
        }
    }
