package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.parser.PlaylistFormatParser
import com.maxrave.data.parser.search.parseSearchSong
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.importdata.ImportData
import com.maxrave.domain.data.model.importdata.ImportPlaylist
import com.maxrave.domain.data.model.importdata.ImportResult
import com.maxrave.domain.data.model.importdata.ImportSong
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.domain.repository.ImportRepository
import com.maxrave.domain.utils.MusicVideoType
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json

private const val TAG = "ImportRepositoryImpl"

/**
 * How many songs go into one transaction.
 */
private const val SONG_BATCH_SIZE = 500

internal class ImportRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : ImportRepository {
    private val jsonFormat =
        Json {
            ignoreUnknownKeys = true
        }

    override fun import(
        data: String,
        invalidFileMessage: String,
        defaultPlaylistName: String,
    ): Flow<ImportProgress> =
        flow {
            emit(ImportProgress.Preparing)

            val cleanContent = data.trim().removePrefix("\uFEFF")

            // Fast path: native Pulse / SimpMusic JSON export format
            val pulseData = if (cleanContent.startsWith("{")) {
                runCatching { jsonFormat.decodeFromString<ImportData>(cleanContent) }.getOrNull()
            } else null

            if (pulseData != null && (pulseData.songs.isNotEmpty() || pulseData.playlists.isNotEmpty())) {
                runCatching {
                    val songsById = pulseData.songs.associateBy { it.videoId }
                    val total = pulseData.songs.size

                    var written = 0
                    pulseData.songs.chunked(SONG_BATCH_SIZE).forEach { chunk ->
                        localDataSource.insertSongs(chunk.map { it.toSongEntity() })
                        written += chunk.size
                        emit(ImportProgress.Importing(processed = written, total = total))
                    }

                    var playlistsCreated = 0
                    var skippedEntries = 0
                    pulseData.playlists.forEach { playlist ->
                        val videoIds = playlist.videoIds.filter { songsById.containsKey(it) }
                        skippedEntries += playlist.videoIds.size - videoIds.size
                        val playlistId =
                            localDataSource.insertLocalPlaylistWithTracks(
                                localPlaylist = playlist.toLocalPlaylistEntity(videoIds),
                                videoIds = videoIds,
                            )
                        if (playlistId != -1L) playlistsCreated++
                    }

                    ImportResult(
                        playlistsCreated = playlistsCreated,
                        songsImported = written,
                        skippedEntries = skippedEntries,
                    )
                }.onSuccess { result ->
                    Logger.i(TAG, "import (native): $result")
                    emit(ImportProgress.Success(result))
                }.onFailure { throwable ->
                    Logger.e(TAG, "import (native): failed while writing - ${throwable.message}")
                    emit(ImportProgress.Error(throwable.message ?: invalidFileMessage))
                }
                return@flow
            }

            // Universal playlist parsing (M3U, M3U8, CSV, TSV, TXT, generic JSON)
            val parsed = PlaylistFormatParser.parse(cleanContent, defaultPlaylistName)
            if (parsed == null || parsed.tracks.isEmpty()) {
                Logger.e(TAG, "import: cannot parse file with any supported format")
                emit(ImportProgress.Error(invalidFileMessage))
                return@flow
            }

            runCatching {
                val total = parsed.tracks.size
                var processed = 0
                val videoIds = mutableListOf<String>()
                var skippedEntries = 0

                emit(ImportProgress.Importing(processed = 0, total = total))

                for (track in parsed.tracks) {
                    if (!track.videoId.isNullOrBlank()) {
                        videoIds.add(track.videoId)
                    } else {
                        val query = track.query.ifBlank {
                            "${track.artist ?: ""} ${track.title ?: ""}".trim()
                        }
                        if (query.isNotBlank()) {
                            val resolvedSong = runCatching {
                                val searchRes = youTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                searchRes?.let { parseSearchSong(it) }?.firstOrNull()
                            }.getOrNull()

                            if (resolvedSong != null) {
                                val songEntity = resolvedSong.toTrack().toSongEntity()
                                localDataSource.insertSong(songEntity)
                                videoIds.add(songEntity.videoId)
                            } else {
                                skippedEntries++
                            }
                        } else {
                            skippedEntries++
                        }
                    }
                    processed++
                    emit(ImportProgress.Importing(processed = processed, total = total))
                }

                if (videoIds.isEmpty()) {
                    emit(ImportProgress.Error("No tracks could be found or matched for this playlist."))
                    return@flow
                }

                val firstThumb = localDataSource.getSong(videoIds.first())?.thumbnails
                val playlistTitle = parsed.title.ifBlank { defaultPlaylistName }
                val playlistEntity = LocalPlaylistEntity(
                    title = playlistTitle,
                    thumbnail = firstThumb,
                    tracks = videoIds,
                )
                val playlistId = localDataSource.insertLocalPlaylistWithTracks(playlistEntity, videoIds)
                val playlistsCreated = if (playlistId != -1L) 1 else 0

                ImportResult(
                    playlistsCreated = playlistsCreated,
                    songsImported = videoIds.size,
                    skippedEntries = skippedEntries,
                )
            }.onSuccess { result ->
                Logger.i(TAG, "import (universal): $result")
                emit(ImportProgress.Success(result))
            }.onFailure { throwable ->
                Logger.e(TAG, "import (universal): failed - ${throwable.message}")
                emit(ImportProgress.Error(throwable.message ?: invalidFileMessage))
            }
        }.flowOn(Dispatchers.IO)

    override fun importFromUrl(
        url: String,
        invalidUrlMessage: String,
        defaultPlaylistName: String?,
    ): Flow<ImportProgress> =
        flow {
            emit(ImportProgress.Preparing)

            val parsed = com.maxrave.data.parser.OnlinePlaylistFetcher.fetch(url, youTube, defaultPlaylistName)
            if (parsed == null || parsed.tracks.isEmpty()) {
                Logger.e(TAG, "importFromUrl: cannot fetch or parse playlist from URL: $url")
                emit(ImportProgress.Error(invalidUrlMessage))
                return@flow
            }

            runCatching {
                val total = parsed.tracks.size
                var processed = 0
                val videoIds = mutableListOf<String>()
                var skippedEntries = 0

                emit(ImportProgress.Importing(processed = 0, total = total))

                for (track in parsed.tracks) {
                    if (!track.videoId.isNullOrBlank()) {
                        videoIds.add(track.videoId)
                        val existingSong = localDataSource.getSong(track.videoId)
                        if (existingSong == null && !track.title.isNullOrBlank()) {
                            val minimalEntity = SongEntity(
                                videoId = track.videoId,
                                title = track.title,
                                artistName = track.artist?.split(", ")?.toList() ?: emptyList(),
                                artistId = emptyList(),
                                isAvailable = true,
                                duration = "",
                                durationSeconds = 0,
                                isExplicit = false,
                                likeStatus = "",
                                thumbnails = null,
                                videoType = "",
                                category = null,
                                resultType = null,
                                liked = false,
                                totalPlayTime = 0,
                                downloadState = 0,
                            )
                            localDataSource.insertSong(minimalEntity)
                        }
                    } else {
                        val query = track.query.ifBlank {
                            "${track.artist ?: ""} ${track.title ?: ""}".trim()
                        }
                        if (query.isNotBlank()) {
                            val resolvedSong = runCatching {
                                val searchRes = youTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                searchRes?.let { parseSearchSong(it) }?.firstOrNull()
                            }.getOrNull()

                            if (resolvedSong != null) {
                                val songEntity = resolvedSong.toTrack().toSongEntity()
                                localDataSource.insertSong(songEntity)
                                videoIds.add(songEntity.videoId)
                            } else {
                                skippedEntries++
                            }
                        } else {
                            skippedEntries++
                        }
                    }
                    processed++
                    emit(ImportProgress.Importing(processed = processed, total = total))
                }

                if (videoIds.isEmpty()) {
                    emit(ImportProgress.Error("No tracks could be found or matched for this playlist."))
                    return@flow
                }

                val firstThumb = localDataSource.getSong(videoIds.first())?.thumbnails
                val playlistTitle = parsed.title.ifBlank { defaultPlaylistName ?: "Imported Playlist" }
                val playlistEntity = LocalPlaylistEntity(
                    title = playlistTitle,
                    thumbnail = firstThumb,
                    tracks = videoIds,
                )
                val playlistId = localDataSource.insertLocalPlaylistWithTracks(playlistEntity, videoIds)
                val playlistsCreated = if (playlistId != -1L) 1 else 0

                ImportResult(
                    playlistsCreated = playlistsCreated,
                    songsImported = videoIds.size,
                    skippedEntries = skippedEntries,
                )
            }.onSuccess { result ->
                Logger.i(TAG, "importFromUrl: successfully imported $result")
                emit(ImportProgress.Success(result))
            }.onFailure { throwable ->
                Logger.e(TAG, "importFromUrl: failed - ${throwable.message}")
                emit(ImportProgress.Error(throwable.message ?: invalidUrlMessage))
            }
        }.flowOn(Dispatchers.IO)
}


private fun ImportSong.toSongEntity(): SongEntity =
    SongEntity(
        videoId = videoId,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId?.takeIf { it.size == (artistName?.size ?: 0) },
        artistName = artistName,
        duration = duration,
        durationSeconds = durationSeconds,
        isAvailable = true,
        isExplicit = isExplicit,
        likeStatus = "",
        thumbnails = thumbnails,
        title = title,
        videoType = MusicVideoType.normalize(videoType) ?: "",
        category = null,
        resultType = null,
        liked = false,
        totalPlayTime = 0,
        downloadState = 0,
    )

private fun ImportPlaylist.toLocalPlaylistEntity(videoIds: List<String>): LocalPlaylistEntity =
    LocalPlaylistEntity(
        title = title,
        thumbnail = thumbnail,
        tracks = videoIds,
    )
