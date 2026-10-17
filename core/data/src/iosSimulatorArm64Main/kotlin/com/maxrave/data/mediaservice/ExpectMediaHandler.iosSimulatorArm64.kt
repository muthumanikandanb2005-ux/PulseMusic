package com.maxrave.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: com.manikandan.domain.manager.DataStoreManager,
    songRepository: com.manikandan.domain.repository.SongRepository,
    streamRepository: com.manikandan.domain.repository.StreamRepository,
    localPlaylistRepository: com.manikandan.domain.repository.LocalPlaylistRepository,
    analyticsRepository: com.manikandan.domain.repository.AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.manikandan.domain.mediaservice.handler.MediaPlayerHandler {
    TODO("Not yet implemented")
}