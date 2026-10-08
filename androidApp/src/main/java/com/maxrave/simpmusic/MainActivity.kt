package com.maxrave.simpmusic

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import android.content.pm.PackageManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eygraber.uri.toKmpUriOrNull
import com.maxrave.common.FIRST_TIME_MIGRATION
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LOCATION
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.logger.Logger
import com.maxrave.media3.di.setServiceActivitySession
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.service.update.AppUpdateWorker
import com.maxrave.simpmusic.service.test.notification.NotificationHandler
import com.maxrave.simpmusic.service.test.notification.NotifyWork
import com.maxrave.simpmusic.service.test.notification.TrendingMusicNotificationWorker
import com.maxrave.simpmusic.utils.ComposeResUtils
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.simpmusic.crashlytics.pushPlayerError
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    val viewModel: SharedViewModel by inject()
    val mediaPlayerHandler by inject<MediaPlayerHandler>()
    val dataStoreManager: DataStoreManager by inject()

    private var mBound = false
    private var shouldUnbind = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
//                mediaPlayerHandler.setActivitySession(this@MainActivity, MainActivity::class.java, service)
                setServiceActivitySession(this@MainActivity, MainActivity::class.java, service)
                Logger.w("MainActivity", "onServiceConnected: ")
                mBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.w("MainActivity", "onServiceDisconnected: ")
                mBound = false
            }
        }

    override fun onStart() {
        super.onStart()
        startMusicService()
    }

    override fun onStop() {
        super.onStop()
        if (shouldUnbind) {
            unbindService(serviceConnection)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("MainActivity", "onNewIntent: $intent")
        viewModel.setIntent(
            GenericIntent(
                action = intent.action,
                data = (intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull(),
                type = intent.type,
            ),
        )
    }

    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadKoinModules(
            module {
                single<AppCompatActivity> { this@MainActivity }
            },
        )
        VersionManager.initialize()
        // Recreate view model to fix the issue of view model not getting data from the service
        unloadKoinModules(viewModelModule)
        loadKoinModules(viewModelModule)
        checkForUpdate()
        if (viewModel.recreateActivity.value || viewModel.isServiceRunning) {
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        Logger.d("MainActivity", "onCreate: ")
        val data = (intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull()
        if (data != null) {
            viewModel.setIntent(
                GenericIntent(
                    action = intent.action,
                    data = data,
                    type = intent.type,
                ),
            )
        }
        Logger.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Logger.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Logger.d(
                    "Contains",
                    "onCreate: ${
                        SUPPORTED_LANGUAGE.codes.contains(
                            Locale.getDefault().toLanguageTag(),
                        )
                    }",
                )
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    val country = Locale.getDefault().country
                    if (country == "VN") {
                        putString("location", "IN")
                    } else {
                        putString("location", country)
                    }
                } else {
                    putString("location", "IN")
                }
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
                putString("location", "IN")
            }
            if (getString("location") == null || getString("location") == "VN") {
                putString("location", "IN")
            }
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Logger.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            getString(
                SELECTED_LANGUAGE,
            )
        ) {
            Logger.d(
                "Locale Key",
                "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
            )
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }

        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
        )
        viewModel.checkIsRestoring()
        NotificationHandler.createNotificationChannel(this)
        NotificationHandler.createAppUpdateNotificationChannel(this)
        NotificationHandler.createTrendingNotificationChannel(this)

        // Schedule periodic & immediate Trending Music Worker
        val trendingPeriodic =
            PeriodicWorkRequestBuilder<TrendingMusicNotificationWorker>(
                4L,
                TimeUnit.HOURS,
            ).addTag("Pulse Trending Music Worker")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Pulse Trending Music Worker",
            ExistingPeriodicWorkPolicy.UPDATE,
            trendingPeriodic,
        )

        val trendingImmediate =
            OneTimeWorkRequestBuilder<TrendingMusicNotificationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "Pulse Trending Music Immediate",
            ExistingWorkPolicy.REPLACE,
            trendingImmediate,
        )

        val artistRequest =
            PeriodicWorkRequestBuilder<NotifyWork>(
                12L,
                TimeUnit.HOURS,
            ).addTag("Worker Test")
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Artist Worker",
            ExistingPeriodicWorkPolicy.KEEP,
            artistRequest,
        )

        lifecycleScope.launch {
            // Cancel legacy blog RSS worker
            WorkManager.getInstance(this@MainActivity).cancelUniqueWork("Blog RSS Worker")

            dataStoreManager.autoCheckForUpdates.collect { enabled ->
                if (enabled == DataStoreManager.TRUE) {
                    val updateRequest =
                        PeriodicWorkRequestBuilder<AppUpdateWorker>(
                            4L,
                            TimeUnit.HOURS,
                        ).addTag("Pulse App Update Worker")
                            .setConstraints(
                                Constraints
                                    .Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build(),
                            ).build()
                    WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                        "Pulse App Update Worker",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        updateRequest,
                    )

                    val updateImmediate =
                        OneTimeWorkRequestBuilder<AppUpdateWorker>()
                            .setConstraints(
                                Constraints
                                    .Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build(),
                            ).build()
                    WorkManager.getInstance(this@MainActivity).enqueueUniqueWork(
                        "Pulse App Update Immediate",
                        ExistingWorkPolicy.REPLACE,
                        updateImmediate,
                    )
                } else {
                    WorkManager.getInstance(this@MainActivity).cancelUniqueWork("Pulse App Update Worker")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.availableUpdate.collect { update ->
                if (update != null && update.tagName.isNotBlank()) {
                    NotificationHandler.postUpdateAvailableNotification(
                        context = this@MainActivity,
                        versionName = update.tagName.trim().removePrefix("v").removePrefix("V"),
                        downloadUrl = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest",
                    )
                }
            }
        }

        if (NotificationHandler.canPostNotification(this)) {
            triggerInitialNotifications()
        } else if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(
                    this,
                    runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.NOTIFICATION_REQUEST) },
                    1,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
        viewModel.getLocation()

        setContent {
            App(viewModel)
        }
    }

    override fun onDestroy() {
        val shouldStopMusicService = viewModel.shouldStopMusicService()
        Logger.w("MainActivity", "onDestroy: Should stop service $shouldStopMusicService")

        // Always unbind service if it was bound to prevent MusicBinder leak
        if (shouldStopMusicService && shouldUnbind && isFinishing) {
            viewModel.isServiceRunning = false
        }
        unloadKoinModules(viewModelModule)
        super.onDestroy()
        Logger.d("MainActivity", "onDestroy: ")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
//        mediaPlayerHandler.startMediaService(this, serviceConnection)
        com.maxrave.media3.di
            .startService(this@MainActivity, serviceConnection)
        mediaPlayerHandler.pushPlayerError = { it ->
            pushPlayerError(it)
        }
        mediaPlayerHandler.showToast = { type ->
            viewModel.makeToast(
                when (type) {
                    is ToastType.ExplicitContent -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.EXPLICIT_CONTENT_BLOCKED) }
                    }

                    is ToastType.PlayerError -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.TIME_OUT_ERROR, type.error) }
                    }
                },
            )
        }
        viewModel.isServiceRunning = true
        shouldUnbind = true
        Logger.d("Service", "Service started")
    }

    private fun checkForUpdate() {
        viewModel.checkForAppUpdate(silent = true)
    }

    private fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? = viewModel.getString(key)

    private fun triggerInitialNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            NotificationHandler.createNotificationChannel(this@MainActivity)
            NotificationHandler.createAppUpdateNotificationChannel(this@MainActivity)
            NotificationHandler.createTrendingNotificationChannel(this@MainActivity)

            // Always check for updates in background regardless of notification permission
            viewModel.checkForAppUpdate(silent = true)

            if (!NotificationHandler.canPostNotification(this@MainActivity)) {
                return@launch
            }

            val lastTrendingNotif = viewModel.getString("last_trending_notif_time")?.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastTrendingNotif > 2 * 60 * 60 * 1000L) {
                val trendingHits = listOf(
                    Triple("Illuminati", "Sushin Shyam", "tOM-nWPcR4U"),
                    Triple("Tauba Tauba", "Karan Aujla", "LK7-_dgAVQE"),
                    Triple("Espresso", "Sabrina Carpenter", "eVli-tstM5E"),
                    Triple("Chuttamalle", "Shilpa Rao & Anirudh Ravichander", "m1qFz4s2qZ8"),
                    Triple("Aasa Kooda", "Sai Abhyankkar", "4y_vA_kQJt8"),
                    Triple("Hukum - Thalaivar Alappara", "Anirudh Ravichander", "1F3HM6353Qc"),
                    Triple("Not Like Us", "Kendrick Lamar", "T6eK-2OQtew"),
                    Triple("Millionaire", "Yo Yo Honey Singh", "XO8wew38VM8"),
                    Triple("Starboy", "The Weeknd ft. Daft Punk", "34Na4j8AVgA"),
                    Triple("Die With A Smile", "Lady Gaga & Bruno Mars", "kPa7bsKwL-8")
                )
                val hit = trendingHits.random()
                NotificationHandler.postTrendingSongNotification(
                    context = this@MainActivity,
                    title = hit.first,
                    artist = hit.second,
                    videoId = hit.third,
                )
                viewModel.putString("last_trending_notif_time", now.toString())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        if (NotificationHandler.canPostNotification(this)) {
            triggerInitialNotifications()
            val update = viewModel.availableUpdate.value
            if (update != null && update.tagName.isNotBlank()) {
                NotificationHandler.postUpdateAvailableNotification(
                    context = this,
                    versionName = update.tagName.trim().removePrefix("v").removePrefix("V"),
                    downloadUrl = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest",
                )
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Logger.i("MainActivity", "onPermissionsGranted: $perms")
        triggerInitialNotifications()
        val update = viewModel.availableUpdate.value
        if (update != null && update.tagName.isNotBlank()) {
            NotificationHandler.postUpdateAvailableNotification(
                context = this,
                versionName = update.tagName.trim().removePrefix("v").removePrefix("V"),
                downloadUrl = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest",
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Logger.w("MainActivity", "onPermissionsDenied: $perms")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }
}