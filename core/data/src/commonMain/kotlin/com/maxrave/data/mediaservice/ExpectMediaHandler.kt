package com.maxrave.data.mediaservice

import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.mediaservice.handler.MediaPlayerHandler
import com.manikandan.domain.repository.AnalyticsRepository
import com.manikandan.domain.repository.LocalPlaylistRepository
import com.manikandan.domain.repository.SongRepository
import com.manikandan.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler