package com.pulse.music.expect

import android.app.PendingIntent
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

            // 2. Try official SnapCreative SDK via reflection if initialized
            try {
                val snapCreativeClass = Class.forName("com.snapchat.kit.sdk.SnapCreative")
                val getApiMethod = snapCreativeClass.getMethod("getApi", android.content.Context::class.java)
                val snapApi = getApiMethod.invoke(null, context)

                val photoContentClass = Class.forName("com.snapchat.kit.sdk.creative.models.SnapPhotoContent")
                val getMediaFactoryMethod = snapCreativeClass.getMethod("getMediaFactory", android.content.Context::class.java)
                val mediaFactory = getMediaFactoryMethod.invoke(null, context)

                val getSnapPhotoFromFileMethod = mediaFactory.javaClass.getMethod("getSnapPhotoFromFile", File::class.java)
                val snapPhotoFile = getSnapPhotoFromFileMethod.invoke(mediaFactory, file)

                val snapPhotoContent = photoContentClass.getConstructor(snapPhotoFile.javaClass).newInstance(snapPhotoFile)
                if (caption.isNotBlank()) {
                    photoContentClass.getMethod("setCaptionText", String::class.java).invoke(snapPhotoContent, caption)
                }
                if (deepLinkUrl.isNotBlank()) {
                    photoContentClass.getMethod("setAttachmentUrl", String::class.java).invoke(snapPhotoContent, deepLinkUrl)
                }

                val sendMethod = snapApi.javaClass.getMethod("send", Class.forName("com.snapchat.kit.sdk.creative.models.SnapContent"))
                sendMethod.invoke(snapApi, snapPhotoContent)
                Logger.d(TAG, "Shared to Snapchat Snap editor via SnapCreative SDK")
                return@runCatching true
            } catch (sdkError: Throwable) {
                Logger.d(TAG, "SnapCreative SDK reflection skipped: ${sdkError.message}")
            }

            // 3. Official Snapchat Creative Kit Lite "Share to Preview" Intent
            // Targeting snapchat://creativekit/preview opens Snapchat directly into the
            // Snap preview/editor screen where the media is rendered as a Snap, never as a chat message.
            val previewUri = Uri.parse("snapchat://creativekit/preview")
            val snapPreviewIntent =
                Intent(Intent.ACTION_SEND).apply {
                    setPackage("com.snapchat.android")
                    setDataAndType(previewUri, MIME_PNG)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra("CLIENT_ID", "com.pulse.music")
                    putExtra("CLIENT_APP_NAME", "Pulse Music")
                    if (caption.isNotBlank()) {
                        putExtra("captionText", caption)
                        putExtra("caption", caption)
                    }
                    if (deepLinkUrl.isNotBlank()) {
                        putExtra("attachmentUrl", deepLinkUrl)
                        putExtra("attachment_url", deepLinkUrl)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(
                        "RESULT_INTENT_EXTRA",
                        PendingIntent.getActivity(
                            context,
                            1001,
                            Intent(),
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                PendingIntent.FLAG_IMMUTABLE
                            } else {
                                PendingIntent.FLAG_ONE_SHOT
                            },
                        ),
                    )
                }

            if (snapPreviewIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(snapPreviewIntent)
                Logger.d(TAG, "Opened Snapchat Snap Preview screen")
                return@runCatching true
            }

            // 4. Fallback to Creative Kit Lite "Share to Camera" with sticker/overlay
            val cameraUri = Uri.parse("snapchat://creativekit/camera")
            val snapCameraIntent =
                Intent(Intent.ACTION_SEND).apply {
                    setPackage("com.snapchat.android")
                    setDataAndType(cameraUri, MIME_PNG)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra("CLIENT_ID", "com.pulse.music")
                    putExtra("CLIENT_APP_NAME", "Pulse Music")
                    if (caption.isNotBlank()) {
                        putExtra("captionText", caption)
                        putExtra("caption", caption)
                    }
                    if (deepLinkUrl.isNotBlank()) {
                        putExtra("attachmentUrl", deepLinkUrl)
                        putExtra("attachment_url", deepLinkUrl)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

            if (snapCameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(snapCameraIntent)
                Logger.d(TAG, "Opened Snapchat Camera Snap screen")
                return@runCatching true
            }

            // 5. Direct launch of preview intent (in case queries block resolveActivity)
            try {
                context.startActivity(snapPreviewIntent)
                Logger.d(TAG, "Direct launch of snapPreviewIntent succeeded")
                true
            } catch (e: Exception) {
                Logger.e(TAG, "Could not start Snapchat as a Snap: ${e.message}")
                false
            }
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share to Snapchat: ${error.message}")
            false
        }
    }
