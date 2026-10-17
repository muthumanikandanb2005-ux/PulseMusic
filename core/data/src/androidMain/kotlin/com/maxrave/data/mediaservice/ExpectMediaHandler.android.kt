package com.maxrave.data.mediaservice

import com.manikandan.domain.repository.AnalyticsRepository

actual fun createMediaServiceHandler(
    dataStoreManager: com.manikandan.domain.manager.DataStoreManager,
    songRepository: com.manikandan.domain.repository.SongRepository,
    streamRepository: com.manikandan.domain.repository.StreamRepository,
    localPlaylistRepository: com.manikandan.domain.repository.LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.manikandan.domain.mediaservice.handler.MediaPlayerHandler =
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        analyticsRepository,
        coroutineScope,
    )