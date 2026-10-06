package com.maxrave.simpmusic.service.test.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

class TrendingMusicNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            try {
                Logger.i(TAG, "Starting Pulse Trending Music notification check...")

                if (!NotificationHandler.canPostNotification(context)) {
                    Logger.w(TAG, "Notification permission is not granted or disabled; skipping")
                    return@withContext Result.success()
                }

                val nowMillis = System.currentTimeMillis()
                val lastCheckStr = dataStoreManager.getString("last_trending_notif_time").first()
                val lastCheck = lastCheckStr?.toLongOrNull() ?: 0L

                // Don't post more frequently than once every 3 hours in background
                if (nowMillis - lastCheck < 3 * 60 * 60 * 1000L) {
                    Logger.i(TAG, "Trending notification was posted recently, skipping.")
                    return@withContext Result.success()
                }

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
                    Triple("Die With A Smile", "Lady Gaga & Bruno Mars", "kPa7bsKwL-8"),
                    Triple("Birds of a Feather", "Billie Eilish", "d5j-Nvd7bVk"),
                    Triple("Ordinary", "Alex Warren", "Vl80g-eA70c")
                )

                val selected = trendingHits[Random.nextInt(trendingHits.size)]
                NotificationHandler.postTrendingSongNotification(
                    context = context,
                    title = selected.first,
                    artist = selected.second,
                    videoId = selected.third,
                )

                dataStoreManager.putString("last_trending_notif_time", nowMillis.toString())
                Logger.i(TAG, "Posted trending notification for: ${selected.first}")

                Result.success()
            } catch (e: Exception) {
                Logger.e(TAG, "Error in TrendingMusicNotificationWorker: ${e.message}", e)
                Result.success()
            }
        }

    companion object {
        const val TAG = "PulseTrendingMusicWorker"
    }
}
