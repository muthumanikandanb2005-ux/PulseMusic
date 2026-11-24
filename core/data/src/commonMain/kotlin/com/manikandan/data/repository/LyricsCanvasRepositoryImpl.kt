@file:OptIn(ExperimentalTime::class)

package com.manikandan.data.repository

import com.manikandan.data.db.datasource.LocalDataSource
import com.manikandan.data.mapping.toCanvasResult
import com.manikandan.data.mapping.toLyrics
import com.manikandan.domain.data.entities.LyricsEntity
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.entities.TranslatedLyricsEntity
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.browse.artist.ArtistLogo
import com.manikandan.domain.data.model.canvas.CanvasResult
import com.manikandan.domain.data.model.metadata.Lyrics
import com.manikandan.domain.data.model.metadata.PulseMusicLyrics
import com.manikandan.domain.extension.now
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.repository.LyricsCanvasRepository
import com.manikandan.domain.utils.Resource
import com.manikandan.domain.utils.connectArtists
import com.manikandan.domain.utils.toListName
import com.manikandan.domain.utils.toPlainLrcString
import com.manikandan.domain.utils.toRichSyncLrcString
import com.manikandan.domain.utils.toSyncedLrcString
import com.manikandan.domain.utils.toSyncedLyrics
import com.manikandan.kotlinytmusicscraper.YouTube
import com.manikandan.logger.Logger
import com.manikandan.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.pulsemusic.aiservice.AiClient
import org.pulsemusic.lyrics.PulseMusicLyricsClient
import org.pulsemusic.lyrics.am.AMAlbumResource
import org.pulsemusic.lyrics.am.AMEditorialVideo
import org.pulsemusic.lyrics.am.AMMotionVideo
import org.pulsemusic.lyrics.am.AMSongWithAlbum
import org.pulsemusic.lyrics.am.toImageUrl
import org.pulsemusic.lyrics.models.request.LyricsBody
import org.pulsemusic.lyrics.models.request.TranslatedLyricsBody
import org.pulsemusic.lyrics.parser.parseTtmlLyrics
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class LyricsCanvasRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val spotify: Spotify,
    private val pulseMusicLyrics: PulseMusicLyricsClient,
    private val aiClient: AiClient,
    private val dataStoreManager: DataStoreManager,
) : LyricsCanvasRepository {
    /**
     * Video ids whose animated artwork is being searched right now, so the same track is never sent
     * to AM twice at once. Not a cancellation: the first attempt is the one that fills the cache.
     */
    private val amArtworkInFlight = mutableSetOf<String>()
    private val amArtworkLock = Mutex()

    override fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> = flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    override suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertLyrics(lyricsEntity)
        }

    override suspend fun insertTranslatedLyrics(translatedLyrics: TranslatedLyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertTranslatedLyrics(translatedLyrics)
        }

    override fun getSavedTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<TranslatedLyricsEntity?> = flow { emit(localDataSource.getTranslatedLyrics(videoId, language)) }.flowOn(Dispatchers.IO)

    override suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.removeTranslatedLyrics(videoId, language)
    }

    override fun getYouTubeCaption(
        preferLang: String,
        videoId: String,
    ): Flow<Resource<Pair<Lyrics, Lyrics?>>> =
        flow {
            runCatching {
                youTube
                    .getYouTubeCaption(videoId, preferLang)
                    .onSuccess { lyrics ->
                        emit(
                            Resource.Success<Pair<Lyrics, Lyrics?>>(
                                Pair(lyrics.first.toLyrics(), lyrics.second?.toLyrics()),
                            ),
                        )
                    }.onFailure { e ->
                        Logger.d("Lyrics", "Error: ${e.message}")
                        emit(Resource.Error<Pair<Lyrics, Lyrics?>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getCanvas(
        dataStoreManager: DataStoreManager,
        videoId: String,
        duration: Int,
        fallbackSong: SongEntity?,
    ): Flow<Resource<CanvasResult>> =
        flow {
            runCatching {
                var song = localDataSource.getSong(videoId) ?: fallbackSong
                if (song == null) {
                    kotlinx.coroutines.delay(250)
                    song = localDataSource.getSong(videoId) ?: fallbackSong
                }
                val songTitle = song?.title?.ifBlank { null } ?: fallbackSong?.title ?: ""
                val artistName = song?.artistName?.firstOrNull()?.ifBlank { null } ?: fallbackSong?.artistName?.firstOrNull() ?: ""
                val q =
                    "$songTitle $artistName"
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                        .replace("  ", " ")
                        .trim()
                if (q.isBlank()) {
                    emit(Resource.Error<CanvasResult>("Empty song title"))
                    return@runCatching
                }
                var spotifyPersonalToken = ""
                var spotifyClientToken = ""
                val spdc = dataStoreManager.spdc.first().removePrefix("sp_dc=").trim()

                if (dataStoreManager.spotifyPersonalToken.first().isNotEmpty() &&
                    dataStoreManager.spotifyClientToken.first().isNotEmpty() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                    dataStoreManager.spotifyClientTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                    dataStoreManager.spotifyClientTokenExpires.first() != 0L
                ) {
                    spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                    spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                } else {
                    // Fetch Client Token
                    spotify.getClientToken().onSuccess {
                        dataStoreManager.setSpotifyClientTokenExpires(
                            (it.grantedToken.expiresAfterSeconds * 1000L) + Clock.System.now().toEpochMilliseconds(),
                        )
                        dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                        spotifyClientToken = it.grantedToken.token
                    }
                    // Fetch Personal/Anonymous Token using valid access token helper (standard or TOTP)
                    val personalToken = spotify.getValidAccessToken(spdc)
                    if (personalToken != null) {
                        spotifyPersonalToken = personalToken
                        dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                        dataStoreManager.setSpotifyPersonalTokenExpires(Clock.System.now().toEpochMilliseconds() + 3600_000L)
                    }
                }

                if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                    val authToken = spotifyPersonalToken
                    spotify.searchSpotifyTrack(q, authToken, spotifyClientToken)
                        .onSuccess { searchResponse ->
                            val items = searchResponse.data?.searchV2?.tracksV2?.items.orEmpty()
                            val candidateTracks = if (duration > 0 && items.isNotEmpty()) {
                                items.sortedBy { item ->
                                    val trackSec = (item.item?.data?.duration?.totalMilliseconds ?: 0) / 1000
                                    abs(trackSec - duration)
                                }
                            } else {
                                items
                            }

                            var canvasResult: CanvasResult? = null
                            for (track in candidateTracks) {
                                val trackId = track.item?.data?.id
                                if (!trackId.isNullOrEmpty()) {
                                    val canvasResponse = spotify.getSpotifyCanvas(trackId, authToken, spotifyClientToken).getOrNull()
                                    val res = canvasResponse?.toCanvasResult()
                                    if (res != null && res.canvasUrl.isNotBlank()) {
                                        canvasResult = res
                                        break
                                    }
                                }
                            }

                            if (canvasResult != null) {
                                emit(Resource.Success(canvasResult))
                            } else {
                                emit(Resource.Error<CanvasResult>("No canvas found"))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Resource.Error<CanvasResult>(throwable.message ?: "Search failed"))
                        }
                } else {
                    emit(Resource.Error<CanvasResult>("Not authenticated with Spotify"))
                }
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Animated album artwork from the hidden AM catalog. It fills exactly the slot a Spotify canvas
     * fills — same [CanvasResult], same two columns on `song` — so everything downstream is unaware
     * of which source produced it. Unlike the canvas path it needs no account of any kind.
     */
    override fun getAMAnimatedArtwork(
        videoId: String,
        fallbackSong: SongEntity?,
    ): Flow<Resource<CanvasResult>> =
        flow {
            var song = localDataSource.getSong(videoId) ?: fallbackSong
            if (song == null) {
                kotlinx.coroutines.delay(200)
                song = localDataSource.getSong(videoId) ?: fallbackSong
            }
            if (song == null) {
                Logger.e(AM_ARTWORK_TAG, "Not found $videoId")
                emit(Resource.Error<CanvasResult>("Song not found"))
                return@flow
            }
            // Reuse what an earlier play already resolved. The column is shared with the Spotify
            // canvas, so only an HLS url is taken back — an `.mp4` left there by that path would
            // pin the track to it and this source would never be asked. Without this the number of
            // requests grows with plays rather than with tracks, against an API that is not ours.
            val cached = song.canvasUrl?.takeIf { it.contains(".m3u8") }
            if (cached != null) {
                // Logged because a cache hit returns without touching the network: without this
                // line a stale url looks identical to a fresh mismatch in the log.
                Logger.d(AM_ARTWORK_TAG, "cache hit for $videoId (${song.title}) --> $cached")
                emit(
                    Resource.Success(
                        CanvasResult(
                            isVideo = true,
                            canvasUrl = cached,
                            canvasThumbUrl = song.canvasThumbUrl,
                        ),
                    ),
                )
                return@flow
            }

            // The caller launches this outside the collector that triggered it, so a second
            // timeline emission for the same track — the duration changes once buffering finishes —
            // fires an identical second search while the first is still in the air. Claiming the id
            // drops the duplicate without cancelling the attempt that is already running.
            if (!claimAmArtwork(videoId)) {
                emit(Resource.Error<CanvasResult>("Already searching"))
                return@flow
            }

            val rawArtist = song.artistName?.firstOrNull().orEmpty()
            val cleanArtist = cleanArtistForLyrics(rawArtist)
            val cleanTitle = cleanTrackTitleForLyrics(song.title)
            // YouTube Music leaves the album name null on plenty of rows, and rows written by older
            // builds carry the literal placeholder "Album"; neither can be searched for.
            val albumName = song.albumName?.takeIf { it.isNotBlank() && it != PLACEHOLDER_ALBUM_NAME }
            Logger.d(AM_ARTWORK_TAG, "resolving $videoId — album=$albumName title=$cleanTitle artist=$cleanArtist")

            try {
                // Tier 1 — the album name, which is what AM indexes one animated artwork against.
                val byAlbum =
                    albumName?.let { name ->
                        pulseMusicLyrics
                            .searchAMAlbum("$name $cleanArtist", limit = AM_SEARCH_LIMIT)
                            .onFailure { Logger.e(AM_ARTWORK_TAG, "album search failed: ${it.message}") }
                            .getOrNull()
                            ?.pickAlbumMatch(name, cleanArtist)
                    }

                // Tier 2 — the track itself, which carries the album it belongs to.
                val album =
                    byAlbum ?: pulseMusicLyrics
                        .searchAMSongsWithAlbums("$cleanTitle $cleanArtist", limit = AM_SEARCH_LIMIT)
                        .onFailure { Logger.e(AM_ARTWORK_TAG, "song search failed: ${it.message}") }
                        .getOrNull()
                        ?.pickSongMatch(
                            title = cleanTitle,
                            artist = cleanArtist,
                            durationSeconds = song.durationSeconds,
                            albumHint = albumName,
                        )
                        ?.album

                val master =
                    album
                        ?.attributes
                        ?.editorialVideo
                        ?.preferredRendition()
                        ?.video
                Logger.d(AM_ARTWORK_TAG, "Matched ${album?.attributes?.name} for $videoId --> $master")
                if (master == null) {
                    emit(Resource.Error<CanvasResult>("No animated artwork"))
                    return@flow
                }
                // Resolve the ladder down to one rendition here, so neither player has to choose:
                // mpv would take the top of it by default (10-bit HEVC at 2048x2732, ~52 MB for a
                // 22-second loop) and ExoPlayer would pick by bandwidth estimate, giving the two
                // platforms different results for the same track. Falls back to the master url,
                // which still plays — just not as cheaply.
                val url =
                    pulseMusicLyrics
                        .selectAMRendition(master, AM_MIN_RENDITION_WIDTH)
                        .getOrNull()
                        ?: master
                Logger.d(AM_ARTWORK_TAG, "Rendition for $videoId --> $url")
                val previewFrame =
                    album.attributes
                        ?.editorialVideo
                        ?.preferredRendition()
                        ?.previewFrame
                emit(
                    Resource.Success(
                        CanvasResult(
                            isVideo = true,
                            canvasUrl = url,
                            // The url is a {w}x{h} template and the frame carries its own
                            // dimensions — the tall rendition is 2048x2732, not square — so asking
                            // for a square crop of it would squash the image.
                            canvasThumbUrl =
                                previewFrame?.toImageUrl(
                                    width = previewFrame.width ?: AM_PREVIEW_FRAME_SIZE,
                                    height = previewFrame.height ?: AM_PREVIEW_FRAME_SIZE,
                                ),
                        ),
                    ),
                )
            } finally {
                amArtworkLock.withLock { amArtworkInFlight -= videoId }
            }
        }.flowOn(Dispatchers.IO)

    /** Returns true when this call took ownership of [videoId]; false when a search is already out. */
    private suspend fun claimAmArtwork(videoId: String): Boolean =
        amArtworkLock.withLock {
            if (videoId in amArtworkInFlight) false else amArtworkInFlight.add(videoId)
        }

    /**
     * Fallback high-resolution video canvas directly from the track's YouTube video/stream,
     * ensuring 100% compulsory full-screen video loop playback across all songs without requiring any login.
     */
    override fun getYouTubeVideoCanvas(videoId: String): Flow<Resource<CanvasResult>> =
        flow {
            runCatching {
                Logger.d(AM_ARTWORK_TAG, "Querying YouTube video canvas fallback for $videoId")
                val playerResult = youTube.player(videoId).getOrNull()
                val streamingData = playerResult?.second?.streamingData
                val hlsUrl = streamingData?.hlsManifestUrl
                val adaptiveFormats = streamingData?.adaptiveFormats.orEmpty()
                val formats = streamingData?.formats.orEmpty()

                // Find highest resolution MP4 video stream with valid URL
                val bestMp4Format =
                    adaptiveFormats
                        .filter {
                            it.mimeType.contains("video/mp4", ignoreCase = true) &&
                                !it.url.isNullOrBlank()
                        }.maxByOrNull { it.height ?: 0 }
                        ?: formats.filter { !it.url.isNullOrBlank() }.maxByOrNull { it.height ?: 0 }

                val canvasVideoUrl = bestMp4Format?.url ?: hlsUrl
                if (!canvasVideoUrl.isNullOrBlank()) {
                    val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    Logger.d(AM_ARTWORK_TAG, "Resolved YouTube canvas fallback for $videoId -> $canvasVideoUrl")
                    emit(
                        Resource.Success(
                            CanvasResult(
                                isVideo = true,
                                canvasUrl = canvasVideoUrl,
                                canvasThumbUrl = thumbUrl,
                            ),
                        ),
                    )
                } else {
                    emit(Resource.Error<CanvasResult>("No video stream available for $videoId"))
                }
            }.onFailure { throwable ->
                Logger.e(AM_ARTWORK_TAG, "YouTube canvas fallback failed for $videoId: ${throwable.message}")
                emit(Resource.Error<CanvasResult>(throwable.message ?: "Failed to resolve stream"))
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasUrl(videoId, canvasUrl)
    }

    override suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasThumbUrl(videoId, canvasThumbUrl)
    }

    override fun getSpotifyLyrics(
        dataStoreManager: DataStoreManager,
        query: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            var emittedSuccess = false
            val q =
                query
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
                    .replace("  ", " ")
            Logger.d("Lyrics", "getSpotifyLyrics query: $q")

            val apiUrl = dataStoreManager.spotifyLyricsApiUrl.first().trim()

            // 1. Try Spotify Web Player with user's sp_dc session (if available)
            var spotifyPersonalToken = ""
            var spotifyClientToken = ""
            if (dataStoreManager.spotifyPersonalToken.first().isNotEmpty() &&
                dataStoreManager.spotifyPersonalTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                dataStoreManager.spotifyClientTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                dataStoreManager.spotifyClientTokenExpires.first() != 0L
            ) {
                spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                spotifyClientToken = dataStoreManager.spotifyClientToken.first()
            } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                val clientRes = spotify.getClientToken()
                clientRes.onSuccess {
                    dataStoreManager.setSpotifyClientTokenExpires(
                        (it.grantedToken.expiresAfterSeconds * 1000L) + Clock.System.now().toEpochMilliseconds(),
                    )
                    dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                    spotifyClientToken = it.grantedToken.token
                }
                val tokenRes = spotify.getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                tokenRes.onSuccess {
                    spotifyPersonalToken = it.accessToken
                    dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                    dataStoreManager.setSpotifyPersonalTokenExpires(it.accessTokenExpirationTimestampMs)
                }
            }

            var resolvedTrackId: String? = null
            if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                val searchRes = spotify.searchSpotifyTrack(q, spotifyPersonalToken, spotifyClientToken)
                searchRes.onSuccess { searchResponse ->
                    val track =
                        if (duration != 0 && duration != null) {
                            searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                abs(
                                    ((it.item?.data?.duration?.totalMilliseconds ?: (0 / 1000)) - duration)
                                ) < 1
                            } ?: searchResponse.data?.searchV2?.tracksV2?.items?.firstOrNull()
                        } else {
                            searchResponse.data?.searchV2?.tracksV2?.items?.firstOrNull()
                        }
                    resolvedTrackId = track?.item?.data?.id
                    if (!resolvedTrackId.isNullOrEmpty()) {
                        spotify
                            .getSpotifyLyrics(resolvedTrackId!!, spotifyPersonalToken, spotifyClientToken)
                            .onSuccess {
                                val lyrics = it.toLyrics()
                                if (!lyrics.lines.isNullOrEmpty()) {
                                    emit(Resource.Success(lyrics))
                                    emittedSuccess = true
                                }
                            }
                    }
                }
            }

            // 2. If direct Spotify lyrics not resolved yet and custom Spotify Lyrics API endpoint configured
            if (!emittedSuccess && apiUrl.isNotEmpty()) {
                Logger.d("Lyrics", "Trying custom Spotify Lyrics API ($apiUrl) for query: $q (trackId: $resolvedTrackId)")
                val apiRes = pulseMusicLyrics.getSpotifyLyricsFromApi(
                    endpoint = apiUrl,
                    trackId = resolvedTrackId,
                    query = if (resolvedTrackId == null) q else null,
                )
                apiRes.onSuccess { libLyrics ->
                    val lyrics = libLyrics.toLyrics()
                    if (!lyrics.lines.isNullOrEmpty()) {
                        emit(Resource.Success(lyrics))
                        emittedSuccess = true
                    }
                }.onFailure {
                    Logger.e("Lyrics", "Spotify Lyrics API failed: ${it.message}")
                }
            }

            // 3. If neither produced lyrics, emit error so waterfall falls back cleanly
            if (!emittedSuccess) {
                emit(Resource.Error("Not found"))
            }
        }.flowOn(Dispatchers.IO)

    private fun cleanTrackTitleForLyrics(title: String): String {
        val cleaned = title
            .replace(Regex("(?i)\\[[^\\]]*(?:official|audio|video|lyric|visualizer|remaster|hd|4k|mv|music video|full song)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\([^\\)]*(?:official|audio|video|lyric|visualizer|remaster|hd|4k|mv|music video|full song)[^\\)]*\\)"), "")
            .replace(Regex("(?i)\\|.*$"), "")
            .replace(Regex("(?i)-\\s*(?:official|audio|video|lyric|visualizer|remaster|hd|4k|mv).*$"), "")
            .replace(Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: )[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: )[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { title.trim() }
    }

    private fun cleanArtistForLyrics(artist: String): String {
        val cleaned = artist
            .replace(Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: )[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: )[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex(" - Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { artist.trim() }
    }

    override fun getLrclibLyricsData(
        sartist: String,
        strack: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getLrclibLyricsData: $sartist $strack $duration")
            val clientId =
                try {
                    dataStoreManager.lrclibClientId.first().trim().ifBlank {
                        DataStoreManager.DEFAULT_LRCLIB_CLIENT_ID
                    }
                } catch (_: Exception) {
                    DataStoreManager.DEFAULT_LRCLIB_CLIENT_ID
                }
            val qartist = cleanArtistForLyrics(sartist)
            val qtrack = cleanTrackTitleForLyrics(strack)
            var result = pulseMusicLyrics.searchLrclibLyrics(
                q_track = qtrack,
                q_artist = qartist,
                duration = duration,
                clientId = clientId,
            ).getOrNull()

            // If cleaned search didn't match, try raw title/artist
            if (result == null && (qtrack != strack.trim() || qartist != sartist.trim())) {
                result = pulseMusicLyrics.searchLrclibLyrics(
                    q_track = strack.trim(),
                    q_artist = sartist.trim(),
                    duration = duration,
                    clientId = clientId,
                ).getOrNull()
            }

            // Also try with primary artist if artist contains multiple names separated by comma or &
            if (result == null && (sartist.contains(",") || sartist.contains("&"))) {
                val primaryArtist = cleanArtistForLyrics(sartist.split(",", "&").first().trim())
                if (primaryArtist.isNotEmpty() && primaryArtist != qartist) {
                    result = pulseMusicLyrics.searchLrclibLyrics(
                        q_track = qtrack,
                        q_artist = primaryArtist,
                        duration = duration,
                        clientId = clientId,
                    ).getOrNull()
                }
            }

            if (result != null) {
                emit(Resource.Success<Lyrics>(result.toLyrics()))
            } else {
                emit(Resource.Error<Lyrics>("Not found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getMusixMatchLyricsData(
        sartist: String,
        strack: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getMusixMatchLyricsData: $sartist $strack $duration")
            val qartist = cleanArtistForLyrics(sartist)
            val qtrack = cleanTrackTitleForLyrics(strack)
            var result = pulseMusicLyrics.searchMusixMatchLyrics(
                q_track = qtrack,
                q_artist = qartist,
                durationSeconds = duration,
            ).getOrNull()

            // If cleaned search didn't match, try raw title/artist
            if (result == null && (qtrack != strack.trim() || qartist != sartist.trim())) {
                result = pulseMusicLyrics.searchMusixMatchLyrics(
                    q_track = strack.trim(),
                    q_artist = sartist.trim(),
                    durationSeconds = duration,
                ).getOrNull()
            }

            if (result != null) {
                emit(Resource.Success<Lyrics>(result.toLyrics()))
            } else {
                emit(Resource.Error<Lyrics>("Not found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getBetterLyrics(
        artist: String,
        track: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getBetterLyrics: $artist $track")
            val qartist = cleanArtistForLyrics(artist)
            val qtrack = cleanTrackTitleForLyrics(track)
            pulseMusicLyrics
                .searchBetterLyrics(qtrack, qartist, duration)
                .onSuccess { ttml ->
                    if (ttml.isNullOrEmpty()) {
                        emit(Resource.Error<Lyrics>("No BetterLyrics found"))
                        return@onSuccess
                    }
                    val lyrics = parseTtmlLyrics(ttml).toLyrics()
                    emit(Resource.Success(lyrics))
                }.onFailure {
                    it.printStackTrace()
                    emit(Resource.Error<Lyrics>("BetterLyrics search failed"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getBiniLyrics(
        artist: String,
        track: String,
        duration: Int?,
        isrc: String?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getBiniLyrics: $artist $track $duration $isrc")
            val qartist = cleanArtistForLyrics(artist)
            val qtrack = cleanTrackTitleForLyrics(track)
            var result = pulseMusicLyrics.searchBiniLyrics(
                q_track = qtrack,
                q_artist = qartist,
                durationSeconds = duration,
                isrc = isrc,
            ).getOrNull()

            // If cleaned search didn't match, try raw title/artist
            if (result == null && (qtrack != track.trim() || qartist != artist.trim())) {
                result = pulseMusicLyrics.searchBiniLyrics(
                    q_track = track.trim(),
                    q_artist = artist.trim(),
                    durationSeconds = duration,
                    isrc = isrc,
                ).getOrNull()
            }

            // Also try with primary artist if artist contains multiple names separated by comma or &
            if (result == null && (artist.contains(",") || artist.contains("&"))) {
                val primaryArtist = cleanArtistForLyrics(artist.split(",", "&").first().trim())
                if (primaryArtist.isNotEmpty() && primaryArtist != qartist) {
                    result = pulseMusicLyrics.searchBiniLyrics(
                        q_track = qtrack,
                        q_artist = primaryArtist,
                        durationSeconds = duration,
                        isrc = isrc,
                    ).getOrNull()
                }
            }

            if (result != null) {
                emit(Resource.Success<Lyrics>(result.toLyrics()))
            } else {
                emit(Resource.Error<Lyrics>("No BiniLyrics found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getLyricaLyrics(
        artist: String,
        track: String,
        duration: Int?,
        translateLanguage: String?,
        romanize: Boolean,
        customBaseUrl: String?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getLyricaLyrics: $artist $track $duration $translateLanguage")
            val qartist = cleanArtistForLyrics(artist)
            val qtrack = cleanTrackTitleForLyrics(track)
            var result = pulseMusicLyrics.searchLyricaLyrics(
                q_track = qtrack,
                q_artist = qartist,
                durationSeconds = duration,
                translateLanguage = translateLanguage,
                romanize = romanize,
                customBaseUrl = customBaseUrl,
            ).getOrNull()

            if (result == null && (qtrack != track.trim() || qartist != artist.trim())) {
                result = pulseMusicLyrics.searchLyricaLyrics(
                    q_track = track.trim(),
                    q_artist = artist.trim(),
                    durationSeconds = duration,
                    translateLanguage = translateLanguage,
                    romanize = romanize,
                    customBaseUrl = customBaseUrl,
                ).getOrNull()
            }

            if (result != null) {
                emit(Resource.Success<Lyrics>(result.toLyrics()))
            } else {
                emit(Resource.Error<Lyrics>("No Lyrica lyrics found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getLyricaTranslatedLyrics(
        artist: String,
        track: String,
        targetLanguage: String,
        customBaseUrl: String?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getLyricaTranslatedLyrics: $artist $track $targetLanguage")
            val qartist = cleanArtistForLyrics(artist)
            val qtrack = cleanTrackTitleForLyrics(track)
            var result = pulseMusicLyrics.getLyricaTranslatedLyrics(
                q_track = qtrack,
                q_artist = qartist,
                targetLanguage = targetLanguage,
                customBaseUrl = customBaseUrl,
            ).getOrNull()

            if (result == null && (qtrack != track.trim() || qartist != artist.trim())) {
                result = pulseMusicLyrics.getLyricaTranslatedLyrics(
                    q_track = track.trim(),
                    q_artist = artist.trim(),
                    targetLanguage = targetLanguage,
                    customBaseUrl = customBaseUrl,
                ).getOrNull()
            }

            if (result != null) {
                emit(Resource.Success<Lyrics>(result.toLyrics()))
            } else {
                emit(Resource.Error<Lyrics>("No Lyrica translated lyrics found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getArtistLogo(artistName: String): Flow<Resource<ArtistLogo>> =
        flow {
            pulseMusicLyrics
                .searchAMArtist(artistName, limit = 1)
                .onSuccess { artists ->
                    val id = artists.firstOrNull()?.id
                    if (id == null) {
                        emit(Resource.Error<ArtistLogo>("Artist not found"))
                        return@onSuccess
                    }
                    pulseMusicLyrics
                        .getAMArtist(id)
                        .onSuccess { artist ->
                            val logo = artist?.attributes?.editorialArtwork?.musicContentColorLogoTrimmed
                            val srcW = logo?.width ?: 0
                            val srcH = logo?.height ?: 0
                            // Scale down to ~1000px wide, keeping the logo aspect ratio.
                            val targetW = 1000
                            val targetH = if (srcW > 0) (srcH.toLong() * targetW / srcW).toInt() else srcH
                            val url = logo?.toImageUrl(targetW, targetH)
                            if (logo == null || url == null) {
                                emit(Resource.Error<ArtistLogo>("No artist logo"))
                                return@onSuccess
                            }
                            emit(
                                Resource.Success(
                                    ArtistLogo(
                                        logoUrl = url,
                                        bgColorHex = logo.bgColor,
                                        width = srcW,
                                        height = srcH,
                                    ),
                                ),
                            )
                        }.onFailure {
                            emit(Resource.Error<ArtistLogo>(it.message ?: "Failed to fetch artist"))
                        }
                }.onFailure {
                    emit(Resource.Error<ArtistLogo>(it.message ?: "Artist search failed"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getAITranslationLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
    ): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                Logger.w("AI Translation", "targetLanguage: $targetLanguage")
                aiClient
                    .translateLyrics(lyrics, targetLanguage)
                    .onSuccess { translatedLyrics ->
                        Logger.w("AI Translation", "translatedLyrics: $translatedLyrics")
                        emit(Resource.Success(translatedLyrics))
                    }.onFailure { throwable ->
                        Logger.e("AI Translation", "Error: ${throwable.message}")
                        emit(Resource.Error<Lyrics>("Translation failed"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    // PulseMusic Lyrics
    private val pulseMusicLyricsTag = "PulseMusicLyricsRepository"

    override fun getPulseMusicLyrics(videoId: String): Flow<Resource<Lyrics>> =
        flow {
            pulseMusicLyrics
                .getLyrics(videoId)
                .onSuccess { lyrics ->
                    Logger.d(pulseMusicLyricsTag, "Lyrics found: $lyrics")
                    val result = lyrics.firstOrNull()
                    if (result == null) {
                        Logger.w(pulseMusicLyricsTag, "No lyrics found for videoId: $videoId")
                        emit(Resource.Error<Lyrics>("No lyrics found"))
                        return@onSuccess
                    }
                    val appLyrics =
                        result.toLyrics()?.copy(
                            pulseMusicLyrics =
                                PulseMusicLyrics(
                                    id = result.id,
                                    vote = result.vote,
                                ),
                        )
                    if (appLyrics == null) {
                        Logger.w(pulseMusicLyricsTag, "Failed to convert lyrics for videoId: $videoId")
                        emit(Resource.Error<Lyrics>("Failed to convert lyrics"))
                        return@onSuccess
                    }
                    emit(
                        Resource.Success<Lyrics>(
                            appLyrics,
                        ),
                    )
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Get Lyrics Error: ${it.message}")
                    emit(Resource.Error<Lyrics>(it.message ?: "Failed to get lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getPulseMusicTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<Resource<Lyrics>> =
        flow {
            pulseMusicLyrics
                .getTranslatedLyrics(videoId, language)
                .onSuccess { lyrics ->
                    Logger.d(pulseMusicLyricsTag, "Translated Lyrics found: ${lyrics.toLyrics()}")
                    emit(
                        Resource.Success<Lyrics>(
                            lyrics
                                .toLyrics()
                                .copy(
                                    pulseMusicLyrics =
                                        PulseMusicLyrics(
                                            id = lyrics.id,
                                            vote = lyrics.vote,
                                        ),
                                ),
                        ),
                    )
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Get Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<Lyrics>(it.message ?: "Failed to get translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun votePulseMusicLyrics(
        lyricsId: String,
        upvote: Boolean,
    ): Flow<Resource<String>> =
        flow {
            pulseMusicLyrics
                .voteLyrics(lyricsId, upvote)
                .onSuccess {
                    Logger.d(pulseMusicLyricsTag, "Vote Lyrics Success: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Vote Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to vote lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun votePulseMusicTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Flow<Resource<String>> =
        flow {
            pulseMusicLyrics
                .voteTranslatedLyrics(translatedLyricsId, upvote)
                .onSuccess {
                    Logger.d(pulseMusicLyricsTag, "Vote Translated Lyrics Success: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Vote Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to vote translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun insertPulseMusicLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        duration: Int,
        lyrics: Lyrics,
    ): Flow<Resource<String>> =
        flow {
            if (lyrics.lines.isNullOrEmpty()) {
                emit(
                    Resource.Error<String>("Lyrics are empty"),
                )
                return@flow
            }
            val syncedLyric =
                if (lyrics.syncType == "LINE_SYNCED") {
                    lyrics.toSyncedLrcString()
                } else if (lyrics.syncType == "RICH_SYNCED") {
                    lyrics.toSyncedLyrics().toSyncedLrcString()
                } else {
                    null
                }
            val richSyncedLyric =
                if (lyrics.syncType == "RICH_SYNCED") {
                    lyrics.toRichSyncLrcString()
                } else {
                    null
                }
            val (contributorName, contributorEmail) = dataStoreManager.contributorName.first() to dataStoreManager.contributorEmail.first()
            pulseMusicLyrics
                .insertLyrics(
                    LyricsBody(
                        videoId = track.videoId,
                        songTitle = track.title,
                        artistName = track.artists?.toListName()?.connectArtists() ?: "",
                        albumName = track.album?.name ?: "",
                        durationSeconds = duration,
                        plainLyric = lyrics.toPlainLrcString() ?: "",
                        syncedLyrics = syncedLyric,
                        richSyncLyrics = richSyncedLyric,
                        contributor = contributorName,
                        contributorEmail = contributorEmail,
                        trackType = if (track.thumbnails?.firstOrNull()?.let { it.width == it.height && it.width > 0 } == true) "SONG" else "VIDEO",
                    ),
                ).onSuccess {
                    Logger.d(pulseMusicLyricsTag, "Inserted Lyrics: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Insert Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to insert lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun insertPulseMusicTranslatedLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        translatedLyrics: Lyrics,
        language: String,
    ): Flow<Resource<String>> =
        flow {
            val syncedLyrics = translatedLyrics.toSyncedLrcString()
            if (translatedLyrics.lines.isNullOrEmpty() || syncedLyrics == null || language.length != 2) {
                emit(
                    Resource.Error<String>("Lyrics are empty"),
                )
                return@flow
            }
            val (contributorName, contributorEmail) = dataStoreManager.contributorName.first() to dataStoreManager.contributorEmail.first()
            pulseMusicLyrics
                .insertTranslatedLyrics(
                    TranslatedLyricsBody(
                        videoId = track.videoId,
                        translatedLyric = syncedLyrics,
                        language = language,
                        contributor = contributorName,
                        contributorEmail = contributorEmail,
                    ),
                ).onSuccess {
                    Logger.d(pulseMusicLyricsTag, "Inserted Translated Lyrics: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(pulseMusicLyricsTag, "Insert Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to insert translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)
}

private const val AM_ARTWORK_TAG = "AMArtwork"
private const val AM_SEARCH_LIMIT = 10

// Narrowest animated-artwork rendition considered sharp enough. On the measured ladders this lands
// on 830x1106 for the tall cut and 768x768 for the square one, both H.264 at roughly 2-3 Mbps.
private const val AM_MIN_RENDITION_WIDTH = 720

/** [matchScore] tier for an exact name match. */
private const val EXACT_NAME_TIER = 0

/**
 * How far two running times may differ and still be the same recording. Wide enough to absorb the
 * encoding differences between YouTube and Apple, video intro/outro differences, and edits.
 */
private const val AM_DURATION_TOLERANCE_SECONDS = 18
// Only used when AM omits the preview frame's own dimensions, which it normally supplies.
private const val AM_PREVIEW_FRAME_SIZE = 1080

// The album name YouTube Music rows fall back to when the real one is unknown. Declared here for
// the same reason the DAO and the local data source each declare their own: it is a value this
// file has to recognise, not one it shares.
private const val PLACEHOLDER_ALBUM_NAME = "Album"

/**
 * The query cleaning the Spotify canvas search applies before it hits the network — featured-artist
 * markers, the conjunctions that join two artists, and the punctuation that survives them. Kept
 * character-for-character identical to that chain so both sources search for the same thing.
 */
private fun String.cleanForSearch(): String =
    this
        .replace(
            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
            " ",
        ).replace(
            Regex("( và | & | и | e | und |, |和| dan)"),
            " ",
        ).replace("  ", " ")
        .replace(Regex("([()])"), "")
        .replace(".", " ")
        .replace("  ", " ")

/**
 * Lowercase, and reduce anything that is not a letter or a digit to a single space. Deliberately
 * built on [Char.isLetterOrDigit] rather than an `[^a-z0-9]` character class: that class treats
 * every accented letter as punctuation, which would grind "Hoàng Thùy Linh" down to "ho ng th y
 * linh" and make the comparison below meaningless for most of the languages this app serves.
 */
private fun String.normalizeForMatch(): String =
    lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

/**
 * Comparison key. Falls back to the raw lowercased text when normalising leaves nothing behind,
 * which is exactly what happens to an album titled with a symbol: Ed Sheeran's "÷" and "=" contain
 * no letters or digits at all, reduce to an empty string, and would otherwise match nothing.
 */
private fun String.matchKey(): String = normalizeForMatch().ifEmpty { trim().lowercase() }

private fun String.matchesLoosely(other: String): Boolean {
    val a = matchKey()
    val b = other.matchKey()
    if (a.isEmpty() || b.isEmpty()) return false
    return a.contains(b) || b.contains(a)
}

/**
 * How closely an album name answers the name that was searched for — lower is better, null means
 * it is not that album at all.
 *
 * AM's search cannot be told to sort, and its own ranking puts reissues above the plain edition:
 * "Hybrid Theory (Deluxe Edition)" ranks above "Hybrid Theory", "1989 (Taylor's Version) [Deluxe]"
 * above "1989". Taking the first loose match therefore returns the artwork of a *different edition*
 * of the right album. Ranking by how much text the candidate adds picks the plain one back out.
 */
private fun matchScore(
    candidate: String,
    subject: String,
): Pair<Int, Int>? {
    val c = candidate.matchKey()
    val s = subject.matchKey()
    if (c.isEmpty() || s.isEmpty()) return null
    return when {
        c == s -> 0 to 0
        // The subject plus a suffix — "<album> (Deluxe Edition)". The shorter the addition, the
        // closer the edition is to the one being played.
        c.startsWith(s) -> 1 to (c.length - s.length)
        c.contains(s) -> 2 to (c.length - s.length)
        s.contains(c) -> 3 to (s.length - c.length)
        else -> null
    }
}

private data class ScoredMatch(
    val tier: Int,
    val extraLength: Int,
    val demoted: Int,
    val rank: Int,
)

/**
 * Rank by how closely a name answers [subject] and return the best, or null when nothing is that
 * thing at all.
 *
 * [demote] is consulted only AFTER name closeness, never before it — lower sorts first. Ordering
 * those the other way round is what made a track pick up the wrong recording: filtering to "has
 * artwork" first threw away the studio version and left the live one, which then won by default.
 */
private fun <T> List<T>.bestNameMatch(
    subject: String,
    demote: (T) -> Int = { 0 },
    nameOf: (T) -> String?,
): T? =
    mapIndexedNotNull { rank, item ->
        matchScore(nameOf(item).orEmpty(), subject)?.let { (tier, extra) ->
            ScoredMatch(tier, extra, demote(item), rank) to item
        }
    }.minWithOrNull(
        compareBy(
            { it.first.tier },
            { it.first.extraLength },
            { it.first.demoted },
            { it.first.rank },
        ),
    )?.second

/**
 * Pick the album by name, and only when the name matches EXACTLY.
 *
 * AM's album index is not complete enough to trust a near-miss. Searching "Mắt Nhắm Mắt Mở
 * HIEUTHUHAI" returns exactly one album — *Mắt Nhắm Mắt Mở (Studio Live Session) - EP* — and not
 * the album itself, so the best available match was a different release of a different recording,
 * accepted purely because nothing else was on offer. The track search below finds the real album
 * for the same track, so anything short of an exact name here defers to it rather than guessing.
 */
private fun List<AMAlbumResource>.pickAlbumMatch(
    subject: String,
    artist: String,
): AMAlbumResource? =
    filter { it.hasAnimatedArtwork() }
        .filter { it.attributes?.artistName.agreesWith(artist) }
        .bestNameMatch(subject) { it.attributes?.name }
        ?.takeIf { (matchScore(it.attributes?.name.orEmpty(), subject)?.first ?: 99) <= 2 }

/**
 * Pick the album for a track reached by title.
 *
 * The album must be the one the matched TRACK belongs to, which is why the search asks for songs
 * and carries [AMSongWithAlbum.album] along. Reading an album straight out of the response instead
 * — "the first one that has artwork" — attaches an unrelated release to the track: for an artist
 * with exactly one animated release, every song of theirs ends up wearing it. Observed with
 * HIEUTHUHAI, where a single carrying artwork was served for unrelated tracks.
 *
 * The track title is scored too, for the same reason the album name is on the other path: AM's
 * order is a ranking, not an answer.
 */
private fun List<AMSongWithAlbum>.pickSongMatch(
    title: String,
    artist: String,
    durationSeconds: Int,
    albumHint: String?,
): AMSongWithAlbum? =
    filter { it.artistName.agreesWith(artist) }
        .bestNameMatch(
            subject = title,
            demote = { candidate ->
                val durationMisses = !candidate.matchesDuration(durationSeconds)
                val albumName = candidate.album.attributes?.name.orEmpty()
                val hintMisses = albumHint != null && (matchScore(albumName, albumHint)?.first ?: 99) > 2
                val hasNoArtwork = !candidate.album.hasAnimatedArtwork()
                (if (durationMisses) 2 else 0) + (if (hintMisses) 1 else 0) + (if (hasNoArtwork) 4 else 0)
            },
        ) { it.songName }
        ?.takeIf { it.album.hasAnimatedArtwork() }

/**
 * Whether this is the same recording, by length. Unknown on either side counts as agreement — an
 * absent number is not evidence of a different take.
 */
private fun AMSongWithAlbum.matchesDuration(durationSeconds: Int): Boolean {
    val theirs = durationInMillis ?: return true
    if (durationSeconds <= 0) return true
    return abs(theirs / 1000L - durationSeconds) <= AM_DURATION_TOLERANCE_SECONDS
}

/**
 * Whether a credited name is the artist being played. Blank means the row carried no artist at all,
 * which cannot disagree with anything.
 */
private fun String?.agreesWith(artist: String): Boolean =
    artist.isBlank() || (this != null && this.matchesLoosely(artist))

private fun AMAlbumResource.hasAnimatedArtwork(): Boolean =
    attributes
        ?.editorialVideo
        ?.preferredRendition()
        ?.video != null

/**
 * Every album that has animated artwork ships all four renditions, but they are read defensively
 * anyway. The tall cut is preferred because the canvas slot it lands in is a full-height backdrop,
 * which is what the Spotify canvas it replaces was shaped for.
 */
private fun AMEditorialVideo.preferredRendition(): AMMotionVideo? =
    listOfNotNull(motionDetailTall, motionTallVideo3x4, motionDetailSquare, motionSquareVideo1x1)
        .firstOrNull { it.video != null }