package com.maxrave.simpmusic.service.test.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.utils.ComposeResUtils
import kotlinx.coroutines.runBlocking

object NotificationHandler {
    private const val CHANNEL_ID = "transactions_reminder_channel"

    suspend fun createReminderNotification(
        context: Context,
        noti: NotificationModel,
    ) {
        //  No back-stack when launched
        val action = Intent(context, MainActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action.data = "simpmusic://notification".toUri()
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val bitmap =
            runBlocking {
                val loader = ImageLoader(context)
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(
                            noti.single
                                .firstOrNull()
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                                ?: noti.album
                                    .firstOrNull()
                                    ?.thumbnails
                                    ?.lastOrNull()
                                    ?.url,
                        ).allowHardware(false) // Disable hardware bitmaps.
                        .build()

                return@runBlocking when (val result = loader.execute(request)) {
                    is SuccessResult -> {
                        result.image.toBitmap()
                    }

                    else -> {
                        AppCompatResources
                            .getDrawable(context, R.drawable.holder)
                            ?.toBitmap(128, 128)
                    }
                }
            }
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(noti.name)
                .setContentText(
                    if (noti.single.isNotEmpty()) {
                        "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_SINGLES)}: ${noti.single.joinToString { it.title }}"
                    } else {
                        "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_ALBUMS)}: ${noti.album.joinToString { it.title }}"
                    },
                ).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent) // For launching the MainActivity
                .setAutoCancel(true) // Remove notification when tapped
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
        with(NotificationManagerCompat.from(context)) {
            if (!canPostNotification(context)) {
                return
            }
            notify(noti.hashCode(), builder.build())
        }
    }

    fun canPostNotification(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Required on Android O+
     */
    fun createNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "Update Followed Artists"
            val descriptionText =
                "This channel sends notification when followed artists release new music"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system

            notificationManager.createNotificationChannel(channel)
        }
    }

    private const val BLOG_CHANNEL_ID = "blog_updates_channel"

    /**
     * Separate channel so users can silence blog updates without affecting artist-release
     * notifications.
     */
    fun createBlogNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(BLOG_CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    BLOG_CHANNEL_ID,
                    "Blog updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifies when a new blog post is published"
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a local notification for a new blog post. Tapping opens [url] in the browser
     * (ACTION_VIEW). No image is loaded — the feed carries no per-item thumbnail.
     * The notification id is derived from [url] so the same post never stacks duplicates.
     */
    fun createBlogNotification(
        context: Context,
        title: String,
        text: String?,
        url: String,
    ) {
        val action =
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                url.hashCode(),
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            NotificationCompat
                .Builder(context, BLOG_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        with(NotificationManagerCompat.from(context)) {
            if (!canPostNotification(context)) {
                return
            }
            notify(url.hashCode(), builder.build())
        }
    }

    private const val APP_UPDATE_CHANNEL_ID = "pulse_app_update_channel"

    fun createAppUpdateNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(APP_UPDATE_CHANNEL_ID) == null) {
                val channel =
                    NotificationChannel(
                        APP_UPDATE_CHANNEL_ID,
                        "Pulse App Updates",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Notifies when a new version of Pulse is ready to install"
                        enableLights(true)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        setShowBadge(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun createAppUpdateNotification(
        context: Context,
        versionName: String,
        apkFile: java.io.File,
    ) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder =
            NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle("Pulse Update Ready ($versionName)")
                .setContentText("A new version of Pulse has been downloaded. Tap to install.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            if (!canPostNotification(context)) {
                return
            }
            notify(1001, builder.build())
        }
    }

    private const val TRENDING_CHANNEL_ID = "pulse_trending_music_channel"

    fun createTrendingNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(TRENDING_CHANNEL_ID) == null) {
                val channel =
                    NotificationChannel(
                        TRENDING_CHANNEL_ID,
                        "Trending & New Music",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Notifies when trending songs and new releases are available"
                        enableLights(true)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        setShowBadge(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun postTrendingSongNotification(
        context: Context,
        title: String,
        artist: String,
        videoId: String? = null,
    ) {
        createTrendingNotificationChannel(context)
        if (!canPostNotification(context)) {
            com.maxrave.logger.Logger.w("NotificationHandler", "Cannot post trending notification: permission not granted or disabled")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!videoId.isNullOrEmpty()) {
                putExtra("EXTRA_VIDEO_ID", videoId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (title + artist).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder =
            NotificationCompat.Builder(context, TRENDING_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle("🔥 Trending Now: $title")
                .setContentText("By $artist • Tap to stream on Pulse Music")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("🔥 \"$title\" by $artist is trending on charts! Tap to stream in crystal-clear audio on Pulse Music.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            notify((title + artist).hashCode(), builder.build())
        }
    }

    fun postUpdateAvailableNotification(
        context: Context,
        versionName: String,
        downloadUrl: String? = null,
    ) {
        createAppUpdateNotificationChannel(context)
        if (!canPostNotification(context)) {
            com.maxrave.logger.Logger.w("NotificationHandler", "Cannot post update notification: permission not granted or disabled")
            return
        }

        val intent = if (!downloadUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW, downloadUrl.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder =
            NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle("⚡ Pulse Music v$versionName Available")
                .setContentText("A new update with improved playback and features is ready!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Pulse Music v$versionName is now available. Tap to update for the latest performance improvements and features.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            notify(1002, builder.build())
        }
    }

    fun postTestNotification(context: Context): Boolean {
        createTrendingNotificationChannel(context)
        if (!canPostNotification(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder =
            NotificationCompat.Builder(context, TRENDING_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle("⚡ Pulse Music Connected")
                .setContentText("Mobile notification panel alerts are working perfectly!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("⚡ Your mobile notification panel is successfully configured! You will receive instant notifications for trending songs, new releases, and app updates.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            notify(9999, builder.build())
        }
        return true
    }
}