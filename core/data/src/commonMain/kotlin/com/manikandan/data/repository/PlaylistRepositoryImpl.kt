package com.manikandan.data.repository

import com.manikandan.data.db.datasource.LocalDataSource
import com.manikandan.data.parser.OnlinePlaylistFetcher
import com.manikandan.data.service.JioSaavnService
import com.manikandan.data.service.SpotifyTrackCache
import com.manikandan.domain.extension.upgradeArtworkUrl
import com.manikandan.domain.utils.toSongEntity
import com.manikandan.spotify.model.response.spotify.SpotifyImage
import com.manikandan.spotify.model.response.spotify.SpotifyTrackItem
import com.manikandan.data.extension.getFullDataFromDB
import com.manikandan.data.mapping.toListTrack
import com.manikandan.data.mapping.toTrack
import com.manikandan.data.mapping.toYouTubeWatchEndpoint
import com.manikandan.data.parser.parseLibraryPlaylist
import com.manikandan.data.parser.parseNextLibraryPlaylist
import com.manikandan.data.parser.parsePlaylistData
import com.manikandan.domain.data.entities.ArtistEntity
import com.manikandan.domain.data.entities.LocalPlaylistEntity
import com.manikandan.domain.data.entities.PlaylistEntity
import com.manikandan.domain.data.entities.SetVideoIdEntity
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.entities.YourYouTubePlaylistList
import com.manikandan.spotify.Spotify
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.browse.playlist.Author
import com.manikandan.domain.data.model.browse.playlist.PlaylistBrowse
import com.manikandan.domain.data.model.searchResult.playlists.PlaylistsResult
import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.data.type.ChartItem
import com.manikandan.domain.data.type.PlaylistType
import com.manikandan.domain.extension.now
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.repository.PlaylistRepository
import com.manikandan.domain.utils.Resource
import com.manikandan.domain.utils.isRadioMix
import com.manikandan.domain.utils.toTrack
import com.manikandan.kotlinytmusicscraper.YouTube
import com.manikandan.kotlinytmusicscraper.models.MusicShelfRenderer
import com.manikandan.kotlinytmusicscraper.models.SongItem
import com.manikandan.kotlinytmusicscraper.models.WatchEndpoint
import com.manikandan.kotlinytmusicscraper.pages.NextPage
import com.manikandan.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.manikandan.kotlinytmusicscraper.parser.getPlaylistRadioEndpoint
import com.manikandan.kotlinytmusicscraper.parser.getPlaylistShuffleEndpoint
import com.manikandan.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

internal class PlaylistRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : PlaylistRepository {
    private val spotify = Spotify()
    override fun getAllPlaylists(limit: Int): Flow<List<PlaylistEntity>> =
        flow {
            emit(localDataSource.getAllPlaylists(limit))
        }.flowOn(Dispatchers.IO)

    override fun getPlaylist(id: String): Flow<PlaylistEntity?> =
        flow {
            emit(localDataSource.getPlaylist(id))
        }.flowOn(Dispatchers.IO)

    override fun getLikedPlaylists(): Flow<List<PlaylistEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getLikedPlaylists(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun insertPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }

    override suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) {
            val oldPlaylist = getPlaylist(playlistEntity.id).firstOrNull()
            if (oldPlaylist != null) {
                localDataSource.insertAndReplacePlaylist(
                    playlistEntity.copy(
                        downloadState = oldPlaylist.downloadState,
                        liked = oldPlaylist.liked,
                    ),
                )
            } else {
                localDataSource.insertAndReplacePlaylist(playlistEntity)
            }
        }

    override suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertRadioPlaylist(playlistEntity) }

    override suspend fun updatePlaylistLiked(
        playlistId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistLiked(
            likeStatus,
            playlistId,
        )
    }

    override suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistInLibrary(
            inLibrary,
            playlistId,
        )
    }

    override suspend fun updatePlaylistDownloadState(
        playlistId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistDownloadState(
            downloadState,
            playlistId,
        )
    }

    override fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    override fun getAllDownloadingPlaylist(): Flow<List<PlaylistType>> =
        flow { emit(localDataSource.getAllDownloadingPlaylist()) }.flowOn(Dispatchers.IO)

    private suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) = withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    override fun getRadio(
        radioId: String,
        defaultDescription: String,
        radioString: String,
        viewString: String,
        originalTrack: SongEntity?,
        artist: ArtistEntity?,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        if (radioId.isRadioMix()) {
            getRDATRadioData(radioId, viewString)
        } else {
            flow {
                runCatching {
                    youTube
                        .next(endpoint = WatchEndpoint(playlistId = radioId))
                        .onSuccess { next ->
                            Logger.w("Radio", "Title: ${next.title}")
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            var continuation = next.continuation
                            Logger.w("Radio", "data: ${data.size}")
                            var count = 0
                            while (continuation != null && count < 3) {
                                youTube
                                    .next(
                                        endpoint = WatchEndpoint(playlistId = radioId),
                                        continuation = continuation,
                                    ).onSuccess { nextContinue ->
                                        data.addAll(nextContinue.items)
                                        continuation = nextContinue.continuation
                                        if (data.size >= 50) {
                                            count = 3
                                        }
                                        Logger.w("Radio", "data: ${data.size}")
                                        count++
                                    }.onFailure {
                                        count = 3
                                    }
                            }
                            val listTrackResult = data.toListTrack()
                            if (originalTrack != null) {
                                listTrackResult.add(0, originalTrack.toTrack())
                            }
                            Logger.w("Repository", "data: ${data.size}")
                            val playlistBrowse =
                                PlaylistBrowse(
                                    author = Author(id = "", name = "YouTube Music"),
                                    description =
                                    defaultDescription,
                                    duration = "",
                                    durationSeconds = 0,
                                    id = radioId,
                                    privacy = "PRIVATE",
                                    thumbnails =
                                        listOf(
                                            Thumbnail(
                                                544,
                                                originalTrack?.thumbnails ?: artist?.thumbnails ?: "",
                                                544,
                                            ),
                                        ),
                                    title = "${originalTrack?.title ?: artist?.name} $radioString",
                                    trackCount = listTrackResult.size,
                                    tracks = listTrackResult,
                                    year = now().year.toString(),
                                )
                            Logger.w("Repository", "playlistBrowse: $playlistBrowse")
                            emit(Resource.Success<Pair<PlaylistBrowse, String?>>(Pair(playlistBrowse, continuation)))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(Resource.Error<Pair<PlaylistBrowse, String?>>(exception.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)
        }

    override fun getRDATRadioData(
        radioId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        flow<Resource<Pair<PlaylistBrowse, String?>>> {
            runCatching {
                val id =
                    if (radioId.startsWith("VL")) {
                        radioId
                    } else {
                        "VL$radioId"
                    }
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Logger.d("Data", "data: $data")
                            Logger.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Logger.d("Header", "header: $header")
                        val finalContinueParam =
                            result.getPlaylistContinuation()
                        Logger.d("Repository", "playlist data: ${listContent.size}")
                        Logger.d("Repository", "continueParam: $finalContinueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        Logger.d("Repository", "playlist final data: ${listContent.size}")
                        if (finalContinueParam != null) {
                            parsePlaylistData(header, listContent, radioId, viewString)?.let { playlist ->
                                emit(
                                    Resource.Success(
                                        Pair(
                                            playlist.copy(
                                                author = Author("", "YouTube Music"),
                                            ),
                                            finalContinueParam,
                                        ),
                                    ),
                                )
                            } ?: emit(Resource.Error("Can't parse data"))
                        } else {
                            emit(Resource.Error("Continue param is null"))
                        }
                    }.onFailure { e ->
                        Logger.e("Playlist Data", e.message ?: "Error")
                        emit(Resource.Error(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getFullPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<PlaylistBrowse>> =
        flow {
            if (playlistId.startsWith("saavn_pl:")) {
                val jioBrowse = JioSaavnService.getPlaylistBrowse(playlistId)
                if (jioBrowse != null) {
                    emit(Resource.Success(jioBrowse))
                    return@flow
                }
            }
            if (playlistId.startsWith("spotify:")) {
                val spotifyBrowse = fetchSpotifyPlaylistBrowse(playlistId)
                if (spotifyBrowse != null) {
                    emit(Resource.Success(spotifyBrowse))
                } else {
                    emit(Resource.Error("Unable to load Spotify playlist"))
                }
                return@flow
            }
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Logger.d("getPlaylistData", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<Track> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Logger.d("getPlaylistData", "data: $data")
                            Logger.d("getPlaylistData", "data size: ${data.size}")
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Logger.d("getPlaylistData", "header: $header")
                        var continueParam =
                            result.getPlaylistContinuation()
                        var count = 0
                        Logger.d("getPlaylistData", "playlist data: ${listContent.size}")
                        Logger.d("getPlaylistData", "continueParam: $continueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = null,
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    Logger.d("getPlaylistData", "continue: $continueParam")
                                    Logger.d(
                                        "getPlaylistData",
                                        "values: ${values.onResponseReceivedActions}",
                                    )
                                    val dataMore: List<SongItem> =
                                        values.onResponseReceivedActions
                                            ?.firstOrNull()
                                            ?.appendContinuationItemsAction
                                            ?.continuationItems
                                            ?.apply {
                                                Logger.w("getPlaylistData", "dataMore: ${this.size}")
                                            }?.mapNotNull {
                                                NextPage.fromMusicResponsiveListItemRenderer(
                                                    it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                                )
                                            } ?: emptyList()
                                    listContent.addAll(dataMore.map { it.toTrack() })
                                    continueParam =
                                        values.getPlaylistContinuation()
                                    count++
                                }.onFailure {
                                    Logger.e("getPlaylistData", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Logger.d("getPlaylistData", "playlist final data: ${listContent.size}")
                        parsePlaylistData(header, data ?: emptyList(), playlistId, viewString)?.let { playlist ->
                            emit(
                                Resource.Success<PlaylistBrowse>(
                                    playlist.copy(
                                        tracks =
                                            playlist.tracks.toMutableList().apply {
                                                addAll(listContent)
                                            },
                                        trackCount = (playlist.trackCount + listContent.size),
                                    ),
                                ),
                            )
                        } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
                    }.onFailure { e ->
                        Logger.e("getPlaylistData", e.message ?: "Error")
                        emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        flow {
            if (playlistId.startsWith("saavn_pl:")) {
                val jioBrowse = JioSaavnService.getPlaylistBrowse(playlistId)
                if (jioBrowse != null) {
                    emit(Resource.Success<Pair<PlaylistBrowse, String?>>(Pair(jioBrowse, null)))
                    return@flow
                }
            }
            if (playlistId.startsWith("spotify:")) {
                val spotifyBrowse = fetchSpotifyPlaylistBrowse(playlistId)
                if (spotifyBrowse != null) {
                    emit(Resource.Success<Pair<PlaylistBrowse, String?>>(Pair(spotifyBrowse, null)))
                } else {
                    emit(Resource.Error("Unable to load Spotify playlist"))
                }
                return@flow
            }
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Logger.d("getPlaylistData", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<Track> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Logger.d("getPlaylistData", "data: $data")
                            Logger.d("getPlaylistData", "data size: ${data.size}")
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Logger.d("getPlaylistData", "header: $header")
                        val continueParam =
                            result.getPlaylistContinuation()
                        val radioEndpoint =
                            result.getPlaylistRadioEndpoint()
                        val shuffleEndpoint =
                            result.getPlaylistShuffleEndpoint()
                        Logger.d("getPlaylistData", "Endpoint: $radioEndpoint $shuffleEndpoint")
                        try {
                            parsePlaylistData(header, data ?: emptyList(), playlistId, viewString)?.let { playlist ->
                                emit(
                                    Resource.Success<Pair<PlaylistBrowse, String?>>(
                                        Pair(
                                            playlist.copy(
                                                tracks =
                                                    playlist.tracks.toMutableList().apply {
                                                        addAll(listContent)
                                                    },
                                                trackCount = (playlist.trackCount + listContent.size),
                                                shuffleEndpoint = shuffleEndpoint?.toYouTubeWatchEndpoint(),
                                                radioEndpoint = radioEndpoint?.toYouTubeWatchEndpoint(),
                                            ),
                                            continueParam,
                                        ),
                                    ),
                                )
                            } ?: emit(
                                Resource.Error<
                                    Pair<PlaylistBrowse, String?>,
                                >("Error"),
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emit(
                                Resource.Error<
                                    Pair<PlaylistBrowse, String?>,
                                >(e.message.toString()),
                            )
                        }
                    }.onFailure { e ->
                        Logger.e("getPlaylistData", e.message ?: "Error")
                        emit(
                            Resource.Error<
                                Pair<PlaylistBrowse, String?>,
                            >(e.message.toString()),
                        )
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getLibraryPlaylist(): Flow<List<PlaylistsResult>?> =
        flow {
            youTube
                .getLibraryPlaylists()
                .onSuccess { data ->
                    val input =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.get(
                                0,
                            )?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.get(
                                0,
                            )?.gridRenderer
                            ?.items
                    val listItem = mutableListOf<PlaylistsResult>()
                    if (input.isNullOrEmpty()) {
                        Logger.w("Library", "No playlists found")
                        emit(null)
                        return@onSuccess
                    }
                    listItem.addAll(
                        parseLibraryPlaylist(input),
                    )
                    var continuation =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.gridRenderer
                            ?.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation
                    while (continuation != null) {
                        youTube
                            .nextYouTubePlaylists(continuation)
                            .onSuccess { nextData ->
                                continuation = nextData.second
                                Logger.w("Library", "continuation: $continuation")
                                val nextInput = nextData.first
                                listItem.addAll(
                                    parseNextLibraryPlaylist(nextInput),
                                )
                            }.onFailure { exception ->
                                exception.printStackTrace()
                                Logger.e("Library", "Error: ${exception.message}")
                                continuation = null
                            }
                    }
                    if (listItem.isNotEmpty()) {
                        emit(listItem)
                        val account = localDataSource.getUsedGoogleAccount()
                        val isNeeded =
                            dataStoreManager.keepYouTubePlaylistOffline.first() == DataStoreManager.TRUE &&
                                dataStoreManager.loggedIn.first() == DataStoreManager.TRUE && account != null
                        if (isNeeded) {
                            insertYourYouTubePlaylist(
                                YourYouTubePlaylistList(
                                    emailPageId = "${account.email}_${account.pageId ?: ""}",
                                    listBrowseIds = listItem.map { it.browseId },
                                ),
                            )
                        }
                    } else {
                        emit(null)
                    }
                }.onFailure { e ->
                    Logger.e("Library", "Error: ${e.message}")
                    e.printStackTrace()
                    val account = localDataSource.getUsedGoogleAccount()
                    val isNeeded =
                        dataStoreManager.keepYouTubePlaylistOffline.first() == DataStoreManager.TRUE &&
                            dataStoreManager.loggedIn.first() == DataStoreManager.TRUE && account != null
                    if (isNeeded) {
                        val list =
                            getYourYouTubePlaylistList("${account.email}_${account.pageId ?: ""}")
                                .lastOrNull()
                        if (list != null) {
                            emit(
                                list.listBrowseIds.mapNotNull { id ->
                                    getPlaylist(id).lastOrNull()?.let {
                                        PlaylistsResult(
                                            author = it.author ?: "",
                                            browseId = it.id,
                                            category = "",
                                            itemCount = "${ it.trackCount }",
                                            resultType = "",
                                            thumbnails =
                                                listOf(
                                                    Thumbnail(
                                                        width = 544,
                                                        url = it.thumbnails,
                                                        height = 544,
                                                    ),
                                                ),
                                            title = it.title,
                                        )
                                    }
                                },
                            )
                        } else {
                            emit(null)
                        }
                    } else {
                        emit(null)
                    }
                }
        }.flowOn(Dispatchers.IO)

    override fun getMixedForYou(): Flow<List<PlaylistsResult>?> =
        flow {
            youTube
                .getMixedForYou()
                .onSuccess { data ->
                    val input =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.get(
                                0,
                            )?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.get(
                                0,
                            )?.gridRenderer
                            ?.items
                    val listItem = mutableListOf<PlaylistsResult>()
                    if (input.isNullOrEmpty()) {
                        Logger.w("Mixed For You", "No playlists found")
                        emit(null)
                        return@onSuccess
                    }
                    listItem.addAll(
                        parseLibraryPlaylist(input),
                    )
                    var continuation =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.gridRenderer
                            ?.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation
                    while (continuation != null) {
                        youTube
                            .nextYouTubePlaylists(continuation)
                            .onSuccess { nextData ->
                                continuation = nextData.second
                                Logger.w("Mixed For You", "continuation: $continuation")
                                val nextInput = nextData.first
                                listItem.addAll(
                                    parseNextLibraryPlaylist(nextInput),
                                )
                            }.onFailure { exception ->
                                exception.printStackTrace()
                                Logger.e("Mixed For You", "Error: ${exception.message}")
                                continuation = null
                            }
                    }
                    if (listItem.isNotEmpty()) {
                        emit(listItem)
                    } else {
                        emit(null)
                    }
                }
        }.flowOn(Dispatchers.IO)

    override fun updateYourYouTubePlaylistTitle(
        playlistId: String,
        newTitle: String,
    ): Flow<Resource<String>> =
        flow {
            youTube
                .editPlaylist(playlistId, newTitle)
                .onSuccess {
                    emit(Resource.Success(it.toString()))
                }.onFailure {
                    emit(Resource.Error<String>(it.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    override suspend fun insertYourYouTubePlaylist(yourYouTubePlaylist: YourYouTubePlaylistList) =
        withContext(Dispatchers.IO) {
            localDataSource.insertYourYouTubePlaylist(yourYouTubePlaylist)
        }

    override suspend fun deleteAllYourYouTubePlaylist() =
        withContext(Dispatchers.IO) {
            localDataSource.deleteAllYourYouTubePlaylist()
        }

    override fun getYourYouTubePlaylistList(emailPageId: String): Flow<YourYouTubePlaylistList?> =
        flow {
            emit(localDataSource.getYourYouTubePlaylistList(emailPageId))
        }.flowOn(Dispatchers.IO)

    override fun getChartPlaylist(): Flow<Resource<List<ChartItem>>> =
        flow {
            youTube
                .getPulseMusicChart()
                .onSuccess { response ->
                    val data = response.data?.filterNotNull() ?: emptyList()
                    val result =
                        data.mapNotNull {
                            ChartItem(
                                name = it.name ?: return@mapNotNull null,
                                ytPlaylistId = it.youtubePlaylistId ?: return@mapNotNull null,
                            )
                        }
                    emit(Resource.Success(result))
                }.onFailure { exception ->
                    exception.printStackTrace()
                    emit(Resource.Error<List<ChartItem>>(exception.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    private fun bestSpotifyImagesToThumbnails(images: List<SpotifyImage>?): List<Thumbnail> {
        if (images.isNullOrEmpty()) return emptyList()
        val best = images.maxByOrNull { (it.width ?: 0).toLong() * (it.height ?: 0).toLong() } ?: images.firstOrNull()
        val upgradedUrl = upgradeArtworkUrl(best?.url) ?: return emptyList()
        val w = (best?.width ?: 640).coerceAtLeast(640)
        val h = (best?.height ?: 640).coerceAtLeast(640)
        return listOf(Thumbnail(height = h, width = w, url = upgradedUrl))
    }

    private fun bestSpotifyImageUrl(images: List<SpotifyImage>?): String? =
        bestSpotifyImagesToThumbnails(images).lastOrNull()?.url

    private fun isLowResThumbnail(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return url.contains("ab67616d00004851") ||
            url.contains("ab67616d00001e02") ||
            url.contains("w60-") ||
            url.contains("w120-") ||
            url.contains("-50x50.") ||
            url.contains("-150x150.")
    }

    private suspend fun mapSpotifyTracksToBrowseTracks(items: List<SpotifyTrackItem>): List<Track> {
        val tracks = items.filter { it.name.isNotBlank() }.mapIndexed { index, spTrack ->
            val stableId = spTrack.id.takeIf { it.isNotBlank() }
                ?: "gen_${index}_${spTrack.name.hashCode().toUInt()}"
            val seconds = (spTrack.durationMs / 1000).toInt()
            Track(
                album = com.manikandan.domain.data.model.searchResult.songs.Album(
                    id = spTrack.album?.id.orEmpty(),
                    name = spTrack.album?.name.orEmpty(),
                ),
                artists = spTrack.artists.map {
                    com.manikandan.domain.data.model.searchResult.songs.Artist(
                        id = it.id,
                        name = it.name,
                    )
                },
                duration = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                durationSeconds = seconds,
                isAvailable = true,
                isExplicit = false,
                likeStatus = null,
                thumbnails = bestSpotifyImagesToThumbnails(spTrack.album?.images),
                title = spTrack.name,
                videoId = "sp_track:$stableId",
                videoType = "Song",
                category = "SONG",
                feedbackTokens = null,
                resultType = "Song",
                year = "",
            )
        }
        SpotifyTrackCache.putAll(tracks)
        runCatching { localDataSource.insertSongs(tracks.map { it.toSongEntity() }) }
        return tracks
    }

    private suspend fun fetchSpotifyPlaylistBrowse(playlistId: String): PlaylistBrowse? {
        val cleanSpotifyId = playlistId
            .removePrefix("spotify:playlist:")
            .removePrefix("spotify:")
            .substringBefore("?")
            .trim()
        val spdc = dataStoreManager.spdc.first()

        if (cleanSpotifyId.equals("liked", ignoreCase = true)) {
            if (spdc.isBlank()) return null
            val likedItems = spotify.fetchLikedTracksWithSpdc(spdc, limit = 100).getOrNull().orEmpty()
            val tracks = mapSpotifyTracksToBrowseTracks(likedItems)
            if (tracks.isEmpty()) return null
            return PlaylistBrowse(
                author = Author(id = "spotify", name = "Spotify"),
                description = "Your Liked Songs on Spotify",
                duration = "",
                durationSeconds = tracks.sumOf { it.durationSeconds ?: 0 },
                id = "spotify:liked",
                privacy = "PRIVATE",
                thumbnails = listOf(
                    Thumbnail(
                        height = 640,
                        width = 640,
                        url = SPOTIFY_LIKED_THUMB_URL,
                    ),
                ),
                title = "Spotify Liked Songs",
                trackCount = tracks.size,
                tracks = tracks,
                year = "",
            )
        }

        val detail = spotify.fetchPlaylistByIdWithSpdc(spdc, cleanSpotifyId, trackLimit = 100).getOrNull()
        val rawItems = detail?.tracks?.items?.mapNotNull { it?.track }?.filter { it.name.isNotBlank() }.orEmpty()
        if (detail != null && rawItems.isNotEmpty()) {
            val resolvedTracks = mapSpotifyTracksToBrowseTracks(rawItems)
            return PlaylistBrowse(
                author = Author(
                    id = detail.owner?.id ?: "",
                    name = detail.owner?.displayName ?: "Spotify",
                ),
                description = detail.description,
                duration = "",
                durationSeconds = resolvedTracks.sumOf { it.durationSeconds ?: 0 },
                id = playlistId,
                privacy = "PUBLIC",
                thumbnails = bestSpotifyImagesToThumbnails(detail.images),
                title = detail.name.ifBlank { "Spotify Playlist" },
                trackCount = detail.tracks.total.takeIf { it > 0 } ?: resolvedTracks.size,
                tracks = resolvedTracks,
                year = "",
            )
        }

        val parsed = runCatching {
            OnlinePlaylistFetcher.fetch("https://open.spotify.com/playlist/$cleanSpotifyId", youTube)
        }.getOrNull()
        if (parsed != null && parsed.tracks.isNotEmpty()) {
            val resolvedTracks = coroutineScope {
                parsed.tracks.take(40).chunked(8).flatMap { batch ->
                    batch.mapIndexed { idx, pt ->
                        async {
                            val rawTitle = pt.title ?: pt.query.substringBefore(" - ").ifBlank { pt.query }
                            val rawArtist = pt.artist ?: pt.query.substringAfter(" - ", "")
                            JioSaavnService.findBestSongMatch(rawTitle, rawArtist.ifBlank { null }) ?: Track(
                                album = com.manikandan.domain.data.model.searchResult.songs.Album(id = "", name = ""),
                                artists = listOf(
                                    com.manikandan.domain.data.model.searchResult.songs.Artist(
                                        id = "",
                                        name = rawArtist.ifBlank { "Spotify" },
                                    ),
                                ),
                                duration = "",
                                durationSeconds = 0,
                                isAvailable = true,
                                isExplicit = false,
                                likeStatus = null,
                                thumbnails = emptyList(),
                                title = rawTitle,
                                videoId = "sp_track:raw_${idx}_${pt.query.hashCode().toUInt()}",
                                videoType = "Song",
                                category = "SONG",
                                feedbackTokens = null,
                                resultType = "Song",
                                year = "",
                            )
                        }
                    }.awaitAll()
                }
            }
            SpotifyTrackCache.putAll(resolvedTracks)
            runCatching { localDataSource.insertSongs(resolvedTracks.map { it.toSongEntity() }) }
            return PlaylistBrowse(
                author = Author(id = "", name = "Spotify"),
                description = null,
                duration = "",
                durationSeconds = resolvedTracks.sumOf { it.durationSeconds ?: 0 },
                id = playlistId,
                privacy = "PUBLIC",
                thumbnails = emptyList(),
                title = parsed.title.ifBlank { "Spotify Playlist" },
                trackCount = resolvedTracks.size,
                tracks = resolvedTracks,
                year = "",
            )
        }
        return null
    }

    override fun getSpotifyPlaylists(): Flow<List<PlaylistsResult>?> =
        flow {
            val spdc = dataStoreManager.spdc.first()
            if (spdc.isBlank()) {
                emit(emptyList())
                return@flow
            }
            val (playlistsRes, likedRes) = coroutineScope {
                val plDeferred = async { spotify.fetchPlaylistsWithSpdc(spdc) }
                val likedDeferred = async { runCatching { spotify.fetchLikedTracksWithSpdc(spdc, limit = 1) }.getOrNull() }
                plDeferred.await() to likedDeferred.await()
            }
            val results = mutableListOf<PlaylistsResult>()
            val likedItems = likedRes?.getOrNull().orEmpty()
            if (likedItems.isNotEmpty() || likedRes?.isSuccess == true) {
                results.add(
                    PlaylistsResult(
                        author = "Spotify",
                        browseId = "spotify:liked",
                        category = "Spotify Playlist",
                        itemCount = if (likedItems.isNotEmpty()) "${likedItems.size}+" else "0",
                        resultType = "Playlist",
                        thumbnails = listOf(
                            Thumbnail(
                                height = 640,
                                width = 640,
                                url = SPOTIFY_LIKED_THUMB_URL,
                            ),
                        ),
                        title = "Spotify Liked Songs",
                    ),
                )
            }
            if (playlistsRes.isSuccess) {
                val items = playlistsRes.getOrNull() ?: emptyList()
                items.forEach { item ->
                    results.add(
                        PlaylistsResult(
                            author = item.owner?.displayName ?: "Spotify",
                            browseId = "spotify:${item.id}",
                            category = "Spotify Playlist",
                            itemCount = "${item.tracks?.total ?: 0}",
                            resultType = "Playlist",
                            thumbnails = bestSpotifyImagesToThumbnails(item.images),
                            title = item.name,
                        ),
                    )
                }
            }
            emit(results)
        }.flowOn(Dispatchers.IO)

    override fun getSpotifyTrendingPlaylists(): Flow<List<PlaylistsResult>?> =
        flow {
            val spdc = dataStoreManager.spdc.first()
            val trendingRes = spotify.fetchFeaturedPlaylistsWithSpdc(spdc, limit = 20)
            if (trendingRes.isSuccess) {
                val items = trendingRes.getOrNull() ?: emptyList()
                val results = items.map { item ->
                    PlaylistsResult(
                        author = item.owner?.displayName ?: "Spotify",
                        browseId = "spotify:${item.id}",
                        category = "Trending on Spotify",
                        itemCount = "${item.tracks?.total ?: 0}",
                        resultType = "Playlist",
                        thumbnails = bestSpotifyImagesToThumbnails(item.images),
                        title = item.name,
                    )
                }
                emit(results)
            } else {
                Logger.w("Spotify", "getSpotifyTrendingPlaylists failed: ${trendingRes.exceptionOrNull()?.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)

    override fun getSpotifyTrendingTracks(): Flow<List<com.manikandan.domain.data.model.spotify.SpotifyRecentTrack>?> =
        flow {
            val spdc = dataStoreManager.spdc.first()
            val tracksRes = spotify.fetchTrendingTracksWithSpdc(spdc, limit = 20)
            if (tracksRes.isSuccess) {
                val items = tracksRes.getOrNull().orEmpty()
                val validItems = items.filter { it.name.isNotBlank() }
                if (validItems.isNotEmpty()) {
                    mapSpotifyTracksToBrowseTracks(validItems)
                }
                val mapped = validItems.map { track ->
                    com.manikandan.domain.data.model.spotify.SpotifyRecentTrack(
                        id = track.id,
                        title = track.name,
                        artist = track.artists.joinToString(", ") { it.name },
                        thumbnail = bestSpotifyImageUrl(track.album?.images),
                        playedAt = null,
                    )
                }
                if (mapped.isNotEmpty()) {
                    emit(mapped)
                    return@flow
                }
            } else {
                Logger.w("Spotify", "getSpotifyTrendingTracks failed: ${tracksRes.exceptionOrNull()?.message}")
            }

            // Fallback via Spotify public Top 50 embed page
            val fallbackParsed = runCatching {
                OnlinePlaylistFetcher.fetch("https://open.spotify.com/playlist/37i9dQZEVXbLZ52XmnySJg", youTube)
                    ?: OnlinePlaylistFetcher.fetch("https://open.spotify.com/playlist/37i9dQZEVXbMDoHDwVN2tF", youTube)
            }.getOrNull()
            val embedTracks = fallbackParsed?.tracks?.take(20)?.mapIndexed { idx, t ->
                val resolvedTitle = t.title ?: t.query
                com.manikandan.domain.data.model.spotify.SpotifyRecentTrack(
                    id = "trending_${idx}_${resolvedTitle.hashCode()}",
                    title = resolvedTitle,
                    artist = t.artist ?: "",
                    thumbnail = null,
                    playedAt = null,
                )
            } ?: emptyList()
            emit(embedTracks)
        }.flowOn(Dispatchers.IO)

    override fun getSpotifyRecentlyPlayed(): Flow<List<com.manikandan.domain.data.model.spotify.SpotifyRecentTrack>?> =
        flow {
            val spdc = dataStoreManager.spdc.first()
            if (spdc.isBlank()) {
                emit(emptyList())
                return@flow
            }
            val recentlyRes = spotify.fetchRecentlyPlayedWithSpdc(spdc, limit = 20)
            if (recentlyRes.isSuccess) {
                val items = recentlyRes.getOrNull() ?: emptyList()
                val validTracks = items.mapNotNull { it.track }.filter { it.name.isNotBlank() }
                if (validTracks.isNotEmpty()) {
                    mapSpotifyTracksToBrowseTracks(validTracks)
                }
                val results = items.mapNotNull { item ->
                    val track = item.track?.takeIf { it.name.isNotBlank() } ?: return@mapNotNull null
                    com.manikandan.domain.data.model.spotify.SpotifyRecentTrack(
                        id = track.id,
                        title = track.name,
                        artist = track.artists.joinToString(", ") { it.name },
                        thumbnail = bestSpotifyImageUrl(track.album?.images),
                        playedAt = item.playedAt,
                    )
                }
                emit(results)
            } else {
                Logger.w("Spotify", "getSpotifyRecentlyPlayed failed: ${recentlyRes.exceptionOrNull()?.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun syncSpotifyPlaylists(spdc: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (spdc.isBlank()) return@runCatching 0

                // 1. Sync User Profile (display name & avatar)
                val profileRes = spotify.fetchUserProfileWithSpdc(spdc)
                if (profileRes.isSuccess) {
                    val profile = profileRes.getOrNull()
                    if (profile != null) {
                        profile.displayName?.let { name ->
                            dataStoreManager.setSpotifyAccountName(name)
                        }
                        bestSpotifyImageUrl(profile.images)?.let { thumb ->
                            dataStoreManager.setSpotifyAccountThumbUrl(thumb)
                        }
                    }
                }

                // 2. Fetch User Playlists
                val playlistsRes = spotify.fetchPlaylistsWithSpdc(spdc)
                val playlists = playlistsRes.getOrThrow()
                var insertedCount = 0

                for (pl in playlists) {
                    val spotifyId = "spotify:${pl.id}"
                    val existing = localDataSource.getLocalPlaylistByYoutubePlaylistId(spotifyId)
                    val thumbUrl = bestSpotifyImageUrl(pl.images)
                    if (existing == null) {
                        localDataSource.insertLocalPlaylist(
                            LocalPlaylistEntity(
                                title = pl.name,
                                thumbnail = thumbUrl,
                                youtubePlaylistId = spotifyId,
                            ),
                        )
                        insertedCount++
                    } else if (thumbUrl != null && isLowResThumbnail(existing.thumbnail)) {
                        localDataSource.updateLocalPlaylistThumbnail(thumbUrl, existing.id)
                    }
                }
                insertedCount
            }
        }

    companion object {
        private const val SPOTIFY_LIKED_THUMB_URL = "https://misc.scdn.co/liked-songs/liked-songs-640.png"
    }
}