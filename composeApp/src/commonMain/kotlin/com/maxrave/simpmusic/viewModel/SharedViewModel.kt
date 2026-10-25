package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config.ALBUM_CLICK
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.PLAYLIST_CLICK
import com.maxrave.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.common.Config.SHARE
import com.maxrave.common.Config.SONG_CLICK
import com.maxrave.common.Config.VIDEO_CLICK
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.common.MERGING_DATA_TYPE
import com.manikandan.domain.data.entities.AlbumEntity
import com.manikandan.domain.data.entities.DownloadState
import com.manikandan.domain.data.entities.LocalPlaylistEntity
import com.manikandan.domain.data.entities.LyricsEntity
import com.manikandan.domain.data.entities.NewFormatEntity
import com.manikandan.domain.data.entities.PlaylistEntity
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.entities.SongInfoEntity
import com.manikandan.domain.data.entities.TranslatedLyricsEntity
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.canvas.CanvasResult
import com.manikandan.domain.data.model.download.DownloadProgress
import com.manikandan.domain.data.model.intent.GenericIntent
import com.manikandan.domain.data.model.metadata.Lyrics
import com.manikandan.domain.data.model.streams.TimeLine
import com.manikandan.domain.data.model.update.UpdateData
import com.manikandan.domain.data.player.GenericCastState
import com.manikandan.domain.extension.decodeHtmlEntities
import com.manikandan.domain.extension.isSong
import com.manikandan.domain.extension.isVideo
import com.manikandan.domain.extension.toGenericMediaItem
import com.manikandan.domain.extension.toSongEntity
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.data.model.lyrics.RomanizationLanguage
import com.manikandan.domain.manager.DataStoreManager.Values.FALSE
import com.manikandan.domain.manager.DataStoreManager.Values.TRUE
import com.manikandan.domain.mediaservice.handler.ControlState
import com.manikandan.domain.mediaservice.handler.DownloadHandler
import com.manikandan.domain.mediaservice.handler.NowPlayingTrackState
import com.manikandan.domain.mediaservice.handler.PlayerEvent
import com.manikandan.domain.mediaservice.handler.PlaylistType
import com.manikandan.domain.mediaservice.handler.QueueData
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.manikandan.domain.mediaservice.handler.SimpleMediaState
import com.manikandan.domain.mediaservice.handler.SleepTimerState
import com.manikandan.domain.repository.AlbumRepository
import com.manikandan.domain.repository.CacheRepository
import com.manikandan.domain.repository.LocalPlaylistRepository
import com.manikandan.domain.repository.LyricsCanvasRepository
import com.manikandan.domain.repository.PlaylistRepository
import com.manikandan.domain.repository.SongRepository
import com.manikandan.domain.repository.StreamRepository
import com.manikandan.domain.repository.UpdateRepository
import com.manikandan.domain.utils.Resource
import com.manikandan.domain.utils.toListName
import com.manikandan.domain.utils.toLyrics
import com.manikandan.domain.utils.toLyricsEntity
import com.manikandan.domain.utils.toSongEntity
import com.manikandan.domain.utils.toSyncedLyrics
import com.manikandan.domain.utils.toTrack
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import org.koin.core.component.inject
import com.maxrave.simpmusic.expect.downloadAndInstallAppUpdate
import com.maxrave.simpmusic.expect.getDownloadFolderPath
import com.maxrave.simpmusic.expect.ui.toByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.simpmusic.lastfm.completeLogin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.added_to_youtube_liked
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.lastfm_login_failed
import simpmusic.composeapp.generated.resources.login_success
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_youtube_liked
import simpmusic.composeapp.generated.resources.shared
import simpmusic.composeapp.generated.resources.updated
import simpmusic.composeapp.generated.resources.vote_submitted
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.reflect.KClass

// DataStore key for the kotlin-footguns star prompt. Cleared on every version bump,
// so the ask comes back after an update - same policy as OPEN_APP_TIME.
const val FOOTGUNS_STAR_KEY = "footguns_starred"

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val updateRepository: UpdateRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
) : BaseViewModel() {
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    /**
     * Which extractor and cipher decoder produced the current track's URLs, shown in the info sheet.
     *
     * Read alongside the format rather than stored with it: the format row is cached and reused,
     * while this describes the extraction that happened in this run of the app.
     */
    private val _extractSource: MutableStateFlow<String?> = MutableStateFlow(null)
    val extractSource: StateFlow<String?> = _extractSource.asStateFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private val _showNotificationPermissionDialog = MutableStateFlow(false)
    val showNotificationPermissionDialog: StateFlow<Boolean> = _showNotificationPermissionDialog

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData

    val castState: StateFlow<GenericCastState> get() = mediaPlayerHandler.castState

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var lyricsJob: Job? = null
    private var currentLyricsVideoId: String? = null
    private var lyricsQueriedAccurateDurationForVideoId: String? = null

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow<Boolean>(false)
    val likeStatus: StateFlow<Boolean> = _likeStatus

    /**
     * Which body of the Apple Music player was open last — held by ENUM NAME so this class stays
     * ignorant of the UI enum, which is internal to the player package.
     *
     * It cannot live in the composable: that player is inside a ModalBottomSheet, so dismissing
     * the sheet disposes the whole tree and takes any rememberSaveable with it — the tab snapped
     * back to the artwork every single time it was reopened. This class is a Koin `single`, so it
     * outlives the sheet while still resetting on app restart, which is the right lifetime for
     * "where I was a moment ago".
     */
    private val _lastPlayerViewTab = MutableStateFlow<String?>(null)
    val lastPlayerViewTab: StateFlow<String?> = _lastPlayerViewTab

    fun setLastPlayerViewTab(tabName: String) {
        _lastPlayerViewTab.value = tabName
    }

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    private var _downloadState: MutableStateFlow<DownloadHandler.Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<DownloadHandler.Download?> = _downloadState.asStateFlow()

    private var songInfoJob: Job? = null

    private var _updateResponse = MutableStateFlow<UpdateData?>(null)
    val updateResponse: StateFlow<UpdateData?> = _updateResponse

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    sealed class UpdateDownloadState {
        data object Idle : UpdateDownloadState()
        data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState()
        data object ReadyToInstall : UpdateDownloadState()
        data class Error(val message: String) : UpdateDownloadState()
    }

    private val _availableUpdate = MutableStateFlow<UpdateData?>(null)
    val availableUpdate: StateFlow<UpdateData?> = _availableUpdate.asStateFlow()

    private val _updateDownloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateDownloadState: StateFlow<UpdateDownloadState> = _updateDownloadState.asStateFlow()

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    // Vote state for translated lyrics
    private val _translatedVoteState = MutableStateFlow<VoteData?>(null)
    val translatedVoteState: StateFlow<VoteData?> = _translatedVoteState.asStateFlow()

    // Vote state for original lyrics
    private val _lyricsVoteState = MutableStateFlow<VoteData?>(null)
    val lyricsVoteState: StateFlow<VoteData?> = _lyricsVoteState.asStateFlow()

    init {
        viewModelScope.launch {
            log("SharedViewModel init")
            VersionManager.initialize()
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.putString(FOOTGUNS_STAR_KEY, "false")
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            checkForAppUpdate(silent = true)
            val timeLineJob =
                launch {
                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                                (new.first.total.toString() + new.second.songEntity?.videoId).hashCode()
                        }.collectLatest {
                            log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                            val nowPlaying = it.second
                            val timeline = it.first
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                    Logger.w(tag, "Duration is ${timeline.total}")
                                    Logger.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    val vid = song.videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                                    if (nowPlayingScreenData.value.lyricsData == null &&
                                        lyricsQueriedAccurateDurationForVideoId != vid &&
                                        (lyricsJob == null || lyricsJob?.isActive == false)
                                    ) {
                                        lyricsQueriedAccurateDurationForVideoId = vid
                                        Logger.w(tag, "Get lyrics from format with accurate duration ${(timeline.total / 1000).toInt()}")
                                        currentLyricsVideoId = vid
                                        lyricsJob = launch {
                                            getLyricsFromFormat(nowPlaying.mediaItem.isVideo(), song, (timeline.total / 1000).toInt())
                                        }
                                    }
                                }
                            }
                        }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        Logger.w(tag, "GetVideo is $it")
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
//            val controllerStateJob =
//                launch {
//                    controllerState.map { it.isLiked }.distinctUntilChanged().collectLatest {
//                        if (dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE) {
//                            nowPlayingState.value?.mediaItem?.mediaId?.let {
//                                getLikeStatus(it)
//                            }
//                        }
//                    }
//                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
//            controllerStateJob.join()
        }

        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                }.collectLatest { state ->
                    val videoId = state.mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    if (videoId.isEmpty()) return@collectLatest
                    Logger.w(tag, "NowPlayingState track changed to $videoId")
                    lyricsJob?.cancel()
                    canvasJob?.cancel()
                    lyricsQueriedAccurateDurationForVideoId = null
                    _nowPlayingState.value = state

                    val metadataDurationMs = (state.songEntity?.durationSeconds ?: state.track?.durationSeconds ?: 0).toLong() * 1000L
                    val seededTotal =
                        metadataDurationMs.takeIf { it > 0L }
                            ?: mediaPlayerHandler.getPlayerDuration().takeIf { it > 0L }
                            ?: -1L
                    _timeline.update { it.copy(total = seededTotal) }

                    val title = state.songEntity?.title
                        ?: state.track?.title
                        ?: state.mediaItem.metadata.title
                        ?: ""
                    val artist = state.songEntity?.artistName?.joinToString(", ")
                        ?: state.track?.artists?.toListName()?.joinToString(", ")
                        ?: state.mediaItem.metadata.artist
                        ?: ""

                    _nowPlayingScreenData.value =
                        NowPlayingScreenData(
                            nowPlayingTitle = title,
                            artistName = artist,
                            isVideo = state.mediaItem.isVideo(),
                            thumbnailURL = state.mediaItem.metadata.artworkUri,
                            canvasData = null,
                            lyricsData = null, // ALWAYS wipe old lyrics immediately on track change!
                            songInfoData = null,
                            playlistName =
                                mediaPlayerHandler.queueData.value
                                    ?.data
                                    ?.playlistName ?: "",
                        )

                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                isExplicit = song.isExplicit,
                            )
                        }
                    }

                    // Directly trigger LRCLIB lyrics for the new song!
                    val songForLyrics = state.songEntity
                        ?: state.track?.toSongEntity()
                        ?: state.mediaItem.toSongEntity()
                    val durationSec = (seededTotal / 1000).toInt().coerceAtLeast(0)
                    currentLyricsVideoId = videoId
                    if (durationSec > 0) {
                        lyricsQueriedAccurateDurationForVideoId = videoId
                    }
                    lyricsJob = launch {
                        getLyricsFromFormat(state.mediaItem.isVideo(), songForLyrics, durationSec)
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Ended -> {
                                // Park at the end of the track rather than at -1. The only formatter
                                // for these fields renders any negative as "NA:NA", and nothing here
                                // is guaranteed to follow: at the end of the queue the player simply
                                // stays ended, so a -1 written here stays on screen. Worse, the
                                // Progress branch below ignores negative values and the Loading
                                // branch restores `total` without touching `current`, which is how
                                // the player ends up showing a correct duration next to "NA:NA".
                                _timeline.update {
                                    it.copy(
                                        current = it.total.coerceAtLeast(0L),
                                        bufferedPercent = 0,
                                        loading = false,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    if (_timeline.value.total > 0L) {
                                        _timeline.update {
                                            it.copy(
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = mediaPlayerHandler.getPlayerDuration().takeIf { d -> d > 0L } ?: it.total,
                                            )
                                        }
                                    }
                                }
                                // When progress hasn't changed (same value polled again) or is negative,
                                // don't modify loading state. The loading flag is already managed by
                                // Buffering/Ready/Loading state events. Setting loading=true here would
                                // cause rapid flickering whenever the same value arrives twice,
                                // which it can: the handler's ticker and the adapter's position
                                // poll both run at 50ms and are not in step, so a poll is sometimes
                                // read twice. A repeat is not evidence of a stall, and the loading
                                // flag belongs to the Buffering/Ready events rather than to a
                                // guess made here.
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = mediaPlayerHandler.getProgress(),
                                        loading = false,
                                        // The player's own duration wins, but ONLY when it has one.
                                        // ExoPlayer answers C.TIME_UNSET (a large negative, not
                                        // null) until it has parsed the container, and Ready is
                                        // also published from onIsLoadingChanged — which fires
                                        // before STATE_READY. Writing that would throw away the
                                        // metadata duration seeded on the track change above.
                                        total = mediaState.duration.takeIf { d -> d > 0L } ?: it.total,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.w(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.w(tag, "ControlState is $it")
                        _controllerState.value = it
                        // Propagate crossfade state to timeline so UI can react
                        _timeline.update { timeline ->
                            timeline.copy(isCrossfading = it.isCrossfading)
                        }
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
        }
        // Reset downloading songs & playlists to not downloaded
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }

    /**
     * Finishes a Last.fm login from the `wordbyword://lastfm-auth` callback.
     *
     * It lands here, and not on the login screen, because the callback arrives at the app rather
     * than at any one screen: the user left for their browser, and the screen they left from may
     * not even exist any more if the process was killed. Routing the token onwards through
     * navigation would push a second copy of the login screen on top of the one already open.
     *
     * The screen learns it succeeded by watching the stored session key, not by being told.
     */
    fun completeLastfmLogin(token: String) {
        if (token.isEmpty()) return
        viewModelScope.launch {
            val session = completeLogin(token)
            if (session != null) {
                dataStoreManager.setLastfmSession(
                    sessionKey = session.sessionKey,
                    username = session.username,
                )
                makeToast(getString(Res.string.login_success))
            } else {
                makeToast(getString(Res.string.lastfm_login_failed))
            }
        }
    }

    fun showNotificationPermissionDialog() {
        _showNotificationPermissionDialog.value = true
    }

    fun dismissNotificationPermissionDialog(doNotShowAgain: Boolean) {
        _showNotificationPermissionDialog.value = false
        if (doNotShowAgain) {
            putString("notification_permission_do_not_ask", "true")
        }
    }

    private fun getLikeStatus(videoId: String?) {
        viewModelScope.launch {
            if (videoId != null) {
                _likeStatus.value = false
                songRepository.getLikeStatus(videoId).collectLatest { status ->
                    _likeStatus.value = status
                }
            }
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.w(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            // Both sources fill the same slot, so they are tried in order rather than raced:
            // animated artwork first, and a Spotify canvas only if that found nothing AND the user
            // has that switch on too. Neither switch touches the other — a track with no animated
            // artwork still gets its canvas, and turning Spotify off still means no canvas at all.
            val sources =
                buildList {
                    if (dataStoreManager.amAnimatedArtwork.first() == TRUE) {
                        add(lyricsCanvasRepository.getAMAnimatedArtwork(videoId))
                    }
                    if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                        add(lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, duration))
                    }
                }
            if (sources.isEmpty()) return@launch

            var resolved = false
            for (source in sources) {
                if (resolved) break
                source.cancellable().collect { response ->
                    val data = response.data
                    if (response is Resource.Success && data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        resolved = true
                        _canvas.value = data
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    NowPlayingScreenData.CanvasData(
                                        isVideo = data.isVideo,
                                        url = data.canvasUrl,
                                    ),
                            )
                        }
                        // Save canvas video url
                        if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                        // Save canvas thumb url
                        data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                    } else {
                        log("Get canvas miss from a source: ${response.message}", LogLevel.WARN)
                    }
                }
            }

            if (!resolved) {
                nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                    _nowPlayingScreenData.update {
                        it.copy(
                            canvasData =
                                NowPlayingScreenData.CanvasData(
                                    isVideo = url.isCanvasVideoUrl(),
                                    url = url,
                                ),
                        )
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    fun putString(
        key: String,
        value: String,
    ) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }


    fun checkIsRestoring() {
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCacheKeys.contains(song.videoId)) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            playlistRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                albumRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                playlistRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                localPlaylistRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            lyricsCanvasRepository.insertLyrics(lyrics)
        }
    }

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            lyricsCanvasRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Logger.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )
                    getAITranslationLyrics(
                        track.videoId,
                        lyricsData,
                    )
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            val localSong = songRepository.getSongById(videoId).firstOrNull()
            if (localSong != null) {
                val track = localSong.toTrack()
                mediaPlayerHandler.setQueueData(
                    QueueData.Data(
                        listTracks = arrayListOf(track),
                        firstPlayedTrack = track,
                        playlistId = "RDAMVM$videoId",
                        playlistName = getString(Res.string.shared),
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItemFromTrack(track, SONG_CLICK)
            } else {
                streamRepository.getFullMetadata(videoId).collectLatest { response ->
                    val track = response.data
                    when (response) {
                        is Resource.Success if (track != null) -> {
                            mediaPlayerHandler.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(track),
                                    firstPlayedTrack = track,
                                    playlistId = "RDAMVM$videoId",
                                    playlistName = getString(Res.string.shared),
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            loadMediaItemFromTrack(track, SONG_CLICK)
                        }

                        else -> {
                            log("Load shared media item error: ${response.message}", LogLevel.WARN)
                            makeToast("${getString(Res.string.error)}: ${response.message}")
                        }
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.clearMediaItems()
            withContext(Dispatchers.Main) {
                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            launch(Dispatchers.IO) {
                try {
                    songRepository.insertSong(track.toSongEntity()).firstOrNull()
                    val songEntity = songRepository.getSongById(track.videoId).firstOrNull()
                    if (songEntity != null) {
                        Logger.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                        _liked.value = songEntity.liked
                    }
                    track.durationSeconds?.let {
                        songRepository.updateDurationSeconds(it, track.videoId)
                    }
                } catch (e: Exception) {
                    Logger.e("SharedViewModel", "Error saving track to database: ${e.message}")
                }
            }

            when (type) {
                SONG_CLICK, VIDEO_CLICK, SHARE -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK, ALBUM_CLICK -> {
                    loadPlaylistOrAlbum(index = index ?: 0)
                }
            }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )
                }

                UIEvent.Forward -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                }

                UIEvent.PlayPause -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )
                }

                UIEvent.Next -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                }

                UIEvent.Previous -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )
                }

                UIEvent.SkipToPrevious -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.SkipToPrevious,
                    )
                }

                UIEvent.Stop -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                }

                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }

                UIEvent.Shuffle -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                }

                UIEvent.ToggleLike -> {
                    Logger.w(tag, "ToggleLike")
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    // Apply to the player first: persisting to DataStore is a suspending disk write
                    // and must not sit between the user's gesture and the audible change.
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                    dataStoreManager.setPlayerVolume(newVolume)
                }
            }
        }

    override fun onCleared() {
        Logger.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllDownloadingLocalPlaylists().collectLatest { playlists ->
                playlists.forEach { playlist ->
                    localPlaylistRepository.updateDownloadState(playlist.id, 0, successMessage = getString(Res.string.updated)).lastOrNull()
                }
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadingPlaylist().collectLatest { list ->
                list.forEach { data ->
                    when (data) {
                        is AlbumEntity -> {
                            albumRepository.updateAlbumDownloadState(data.browseId, 0)
                        }

                        is PlaylistEntity -> {
                            playlistRepository.updatePlaylistDownloadState(data.id, 0)
                        }

                        else -> {
                            // Skip
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            songRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
            songRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            _extractSource.value = streamRepository.getExtractSource(mediaId)
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.w(tag, "Get format for $mediaId: $f")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                        // Re-read on every emission: the first one usually lands before the
                        // extractor has finished, so the source is only known on a later pass.
                        _extractSource.value = streamRepository.getExtractSource(mediaId)
                    }
                }
        }
    }


    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }


    fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = dataStoreManager.autoCheckForUpdates.first() == TRUE
            if (enabled) {
                checkForAppUpdate(silent = true)
            }
        }
    }

    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        mediaPlayerHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        inputLyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.LRCLIB,
    ) {
        val cleanVideoId = videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        val currentVideoId = nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
            ?: nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
        if (currentVideoId != null && currentVideoId.isNotEmpty() && cleanVideoId != currentVideoId) {
            Logger.w(tag, "updateLyrics: discarding lyrics for $cleanVideoId as current song is $currentVideoId")
            return
        }

        val hasNonBlankLines = inputLyrics?.lines?.any { !it.words.isNullOrBlank() } == true
        if (inputLyrics == null || (!isTranslatedLyrics && !hasNonBlankLines)) {
            if (!isTranslatedLyrics) {
                _nowPlayingScreenData.update {
                    it.copy(
                        lyricsData = null,
                    )
                }
            }
            return
        }

        val lyrics =
            inputLyrics.copy(
                lines =
                    inputLyrics.lines?.map { line ->
                        line.copy(
                            words = decodeHtmlEntities(line.words),
                        )
                    },
            )

        if (isTranslatedLyrics && lyricsProvider != LyricsProvider.AI) {
            // Skip sync validation for AI translations — timestamps are copied programmatically
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            val originalLines = originalLyrics?.lines
            val lyricsLines = lyrics.lines
            if (originalLyrics != null && originalLines != null && lyricsLines != null) {
                var timeSyncErrorCount = 0
                val totalLines = originalLines.size

                if (originalLines.size == lyricsLines.size) {
                    // Line counts match: compare by index (1:1 mapping)
                    originalLines.forEachIndexed { index, originalLine ->
                        val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                        val translatedLine = lyricsLines[index]
                        val translatedTime = translatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) {
                            timeSyncErrorCount++
                        }
                    }
                } else {
                    // Line count mismatch: use timestamp-based matching with used-line tracking
                    val usedIndices = mutableSetOf<Int>()
                    originalLines.forEach { originalLine ->
                        val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                        var bestIndex = -1
                        var bestDiff = Long.MAX_VALUE
                        lyricsLines.forEachIndexed { index, line ->
                            if (index !in usedIndices) {
                                val diff = abs((line.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                                if (diff < bestDiff) {
                                    bestDiff = diff
                                    bestIndex = index
                                }
                            }
                        }
                        if (bestIndex >= 0) {
                            usedIndices.add(bestIndex)
                            if (bestDiff > 1000L) {
                                timeSyncErrorCount++
                            }
                        } else {
                            timeSyncErrorCount++
                        }
                    }
                }

                // Use percentage-based threshold: reject if >25% of lines are out of sync
                val syncErrorRatio = if (totalLines > 0) timeSyncErrorCount.toFloat() / totalLines else 0f
                if (syncErrorRatio > 0.25f || (totalLines > 0 && timeSyncErrorCount > totalLines / 2)) {
                    Logger.w(
                        tag,
                        "Translated lyrics out of sync: $timeSyncErrorCount/$totalLines lines with time diff > 1s (${(syncErrorRatio * 100).toInt()}%)",
                    )

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        lyricsCanvasRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        log("Removed out-of-sync translated lyrics for $videoId")
                        val simpMusicLyricsId = lyrics.simpMusicLyrics?.id
                        if (lyricsProvider == LyricsProvider.SIMPMUSIC && !simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(
                                        translatedLyricsId = simpMusicLyricsId,
                                        false,
                                    ).collectLatest {
                                        when (it) {
                                            is Resource.Error -> {
                                                Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                        nowPlayingScreenData.value.lyricsData?.lyrics?.let {
                            getAITranslationLyrics(
                                videoId,
                                it,
                            )
                        }
                    }
                    return
                }
            }
        }

        val currentSongId = _nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
            ?: _nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
        if (currentSongId == null || cleanVideoId == currentSongId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _translatedVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics to lyricsProvider,
                                ),
                        )
                    }
                    if (track != null) {
                        viewModelScope.launch {
                            val shouldSendLyricsToSimpMusic =
                                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE &&
                                    lyricsProvider != LyricsProvider.SIMPMUSIC
                            if (shouldSendLyricsToSimpMusic) {
                                lyricsCanvasRepository
                                    .insertSimpMusicTranslatedLyrics(
                                        dataStoreManager,
                                        track,
                                        lyrics,
                                        dataStoreManager.translationLanguage.first(),
                                    ).collect {
                                        when (it) {
                                            is Resource.Error -> {
                                                log("Insert SimpMusic Translated Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                log("Insert SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }

                false -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _lyricsVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                        )
                    }
                    // Save lyrics to database
                    viewModelScope.launch {
                        lyricsCanvasRepository.insertLyrics(
                            LyricsEntity(
                                videoId = cleanVideoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                    if (track != null) {
                        viewModelScope.launch {
                            val shouldSendLyricsToSimpMusic =
                                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE &&
                                    lyricsProvider != LyricsProvider.SIMPMUSIC
                            if (shouldSendLyricsToSimpMusic) {
                                lyricsCanvasRepository
                                    .insertSimpMusicLyrics(
                                        dataStoreManager,
                                        track,
                                        duration,
                                        lyrics,
                                    ).collect {
                                        when (it) {
                                            is Resource.Error -> {
                                                Logger.w(tag, "Insert SimpMusic Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                Logger.d(tag, "Insert SimpMusic Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getLyricsFromFormat(
        isVideo: Boolean,
        song: SongEntity,
        duration: Int,
    ) {
        val cleanVideoId = song.videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        log("Get Lyrics From Format for $cleanVideoId", LogLevel.WARN)
        val artistName = song.artistName
        val artist =
            if (artistName?.firstOrNull() != null &&
                artistName
                    .firstOrNull()
                    ?.contains("Various Artists") == false
            ) {
                artistName.firstOrNull()
            } else {
                mediaPlayerHandler.nowPlaying.value
                    ?.metadata
                    ?.artist
                    ?: ""
            }
        resetLyricsVoteState()
        // 1. Immediately check local Room database for saved lyrics so lyrics display instantly on song transition!
        viewModelScope.launch(Dispatchers.IO) {
            val localSaved = lyricsCanvasRepository.getSavedLyrics(cleanVideoId).firstOrNull()
            if (localSaved != null && nowPlayingScreenData.value.lyricsData == null) {
                val currentSongId = nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    ?: nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                if (currentSongId == cleanVideoId) {
                    val lyricsData = localSaved.toLyrics()
                    updateLyrics(
                        cleanVideoId,
                        duration,
                        lyricsData,
                        false,
                        LyricsProvider.LRCLIB,
                    )
                }
            }
        }
        // 2. Fetch from preferred lyrics provider (BiniLyrics or LRCLIB)
        viewModelScope.launch {
            val preferredProvider = dataStoreManager.lyricsProvider.first()
            if (preferredProvider == DataStoreManager.BINI_LYRICS) {
                getBiniLyrics(
                    song = song.copy(videoId = cleanVideoId),
                    artist = (artist ?: ""),
                    duration = duration,
                    fallbackToLrclib = true,
                )
            } else {
                getLrclibLyrics(
                    song = song.copy(videoId = cleanVideoId),
                    artist = (artist ?: ""),
                    duration = duration,
                )
            }
        }
    }

    private suspend fun getSimpMusicLyrics(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository.getSimpMusicLyrics(videoId).collectLatest {
            Logger.w(tag, "Get SimpMusic Lyrics for $videoId: $it")
            val data = it.data
            if (it is Resource.Success && data != null) {
                Logger.d(tag, "Get SimpMusic Lyrics Success")
                updateLyrics(
                    videoId,
                    duration,
                    data,
                    false,
                    LyricsProvider.SIMPMUSIC,
                )
                insertLyrics(
                    data.toLyricsEntity(videoId),
                )
                getSimpMusicTranslatedLyrics(
                    videoId,
                    data,
                )
            } else if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                getSpotifyLyrics(
                    song.toTrack().copy(durationSeconds = duration),
                    "${song.title} $artist",
                    duration,
                )
            } else {
                getLrclibLyrics(
                    song,
                    (artist ?: ""),
                    duration,
                )
            }
        }
    }

    private suspend fun getYouTubeCaption(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
        fallbackToSaved: Boolean = false,
    ) {
        lyricsCanvasRepository
            .getYouTubeCaption(dataStoreManager.youtubeSubtitleLanguage.first(), videoId)
            .cancellable()
            .collect { response ->
                val data = response.data
                when (response) {
                    is Resource.Success if (data != null) -> {
                        val lyrics = data.first
                        val translatedLyrics = data.second
                        insertLyrics(lyrics.toLyricsEntity(videoId))
                        updateLyrics(
                            videoId,
                            duration,
                            lyrics,
                            false,
                            LyricsProvider.YOUTUBE,
                        )
                        if (translatedLyrics != null) {
                            updateLyrics(
                                videoId,
                                duration,
                                translatedLyrics,
                                true,
                                LyricsProvider.YOUTUBE,
                            )
                        } else {
                            getAITranslationLyrics(
                                videoId,
                                lyrics,
                            )
                        }
                    }

                    else -> {
                        getSimpMusicLyrics(
                            videoId,
                            song,
                            (artist ?: ""),
                            duration,
                        )
                    }
                }
            }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        val targetVideoId = song.videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            lyricsCanvasRepository
                .getLrclibLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val currentVideoId = nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                        ?: nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    if (currentVideoId != null && currentVideoId.isNotEmpty() && targetVideoId != currentVideoId) {
                        Logger.w(tag, "LRCLIB lyrics arrived for $targetVideoId but current song is $currentVideoId, ignoring")
                        return@collectLatest
                    }
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null && !data.lines.isNullOrEmpty()) -> {
                            Logger.d(tag, "Get LRCLIB Lyrics Success for ${song.title} ($targetVideoId)")
                            updateLyrics(
                                targetVideoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                            insertLyrics(
                                data.toLyricsEntity(
                                    targetVideoId,
                                ),
                            )
                            getAITranslationLyrics(
                                targetVideoId,
                                data,
                            )
                        }

                        else -> {
                            Logger.d(tag, "LRCLIB yielded no lyrics for ${song.title}, falling back to BiniLyrics")
                            getBiniLyrics(
                                song = song,
                                artist = artist,
                                duration = duration,
                                fallbackToLrclib = false,
                            )
                        }
                    }
                }
        }
    }

    private fun getMusixMatchLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
        fallbackToLrclib: Boolean = true,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getMusixMatchLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null && !data.lines.isNullOrEmpty()) -> {
                            Logger.d(tag, "Get MusixMatch Lyrics Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.MUSIXMATCH,
                            )
                            insertLyrics(
                                res.data?.toLyricsEntity(
                                    song.videoId,
                                ) ?: return@collectLatest,
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                data,
                            )
                        }

                        else -> {
                            if (fallbackToLrclib) {
                                Logger.d(tag, "MusixMatch yielded no lyrics, falling back to LRCLIB online")
                                getLrclibLyrics(
                                    song,
                                    artist,
                                    duration,
                                )
                            } else {
                                Logger.d(tag, "MusixMatch and LRCLIB completed, falling back to YouTube captions")
                                getYouTubeCaption(
                                    song.videoId,
                                    song,
                                    artist,
                                    duration,
                                    fallbackToSaved = false,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun getBiniLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
        fallbackToLrclib: Boolean = true,
    ) {
        val targetVideoId = song.videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        viewModelScope.launch {
            lyricsCanvasRepository
                .getBiniLyrics(
                    artist = artist,
                    track = song.title,
                    duration = duration,
                    isrc = null,
                ).collectLatest { res ->
                    val currentVideoId = nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                        ?: nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    if (currentVideoId != null && currentVideoId.isNotEmpty() && targetVideoId != currentVideoId) {
                        Logger.w(tag, "BiniLyrics arrived for $targetVideoId but current song is $currentVideoId, ignoring")
                        return@collectLatest
                    }
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null && !data.lines.isNullOrEmpty()) -> {
                            Logger.d(tag, "Get BiniLyrics Success for ${song.title} ($targetVideoId)")
                            updateLyrics(
                                targetVideoId,
                                duration,
                                data,
                                false,
                                LyricsProvider.BINI_LYRICS,
                            )
                            insertLyrics(
                                data.toLyricsEntity(
                                    targetVideoId,
                                ),
                            )
                            getAITranslationLyrics(
                                targetVideoId,
                                data,
                            )
                        }

                        else -> {
                            Logger.d(tag, "BiniLyrics yielded no lyrics for ${song.title}")
                            if (fallbackToLrclib) {
                                getLrclibLyrics(
                                    song = song,
                                    artist = artist,
                                    duration = duration,
                                )
                            } else {
                                // Check local Room database as offline / network error fallback
                                val localSaved = lyricsCanvasRepository.getSavedLyrics(targetVideoId).firstOrNull()
                                if (localSaved != null) {
                                    val curId = nowPlayingState.value?.mediaItem?.mediaId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                                        ?: nowPlayingState.value?.songEntity?.videoId?.removePrefix(MERGING_DATA_TYPE.VIDEO)
                                    if (curId == targetVideoId) {
                                        val lyricsData = localSaved.toLyrics()
                                        updateLyrics(
                                            targetVideoId,
                                            duration,
                                            lyricsData,
                                            false,
                                            LyricsProvider.LRCLIB,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun getBetterLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getBetterLyrics(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Logger.d(tag, "Get BetterLyrics Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                data,
                                false,
                                LyricsProvider.BETTER_LYRICS,
                            )
                            insertLyrics(
                                data.toLyricsEntity(
                                    song.videoId,
                                ),
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                data,
                            )
                        }

                        else -> {
                            log("Get BetterLyrics Error: ${res.message}")
                            getSimpMusicLyrics(
                                song.videoId,
                                song,
                                artist,
                                duration,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getSimpMusicTranslatedLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        val translationLanguage =
            dataStoreManager.translationLanguage.first()
        lyricsCanvasRepository.getSimpMusicTranslatedLyrics(videoId, translationLanguage).collectLatest { response ->
            val data = response.data
            when (response) {
                is Resource.Success if (data != null) -> {
                    // If SimpMusic translated lyrics are RICH_SYNCED (word-by-word),
                    // convert to LINE_SYNCED, downvote, and fallback to AI translation
                    if (data.syncType == "RICH_SYNCED") {
                        Logger.w(tag, "SimpMusic translated lyrics are RICH_SYNCED, downvoting and falling back to AI")
                        val simpMusicLyricsId = data.simpMusicLyrics?.id
                        if (!simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(simpMusicLyricsId, false)
                                    .collectLatest { voteResult ->
                                        when (voteResult) {
                                            is Resource.Error -> Logger.w(tag, "Downvote RICH_SYNCED translated lyrics error: ${voteResult.message}")
                                            is Resource.Success -> Logger.d(tag, "Downvote RICH_SYNCED translated lyrics success")
                                        }
                                    }
                            }
                        }
                        // Fallback to AI translation
                        getAITranslationLyrics(videoId, lyrics)
                    } else {
                        Logger.d(tag, "Get SimpMusic Translated Lyrics Success")
                        updateLyrics(
                            videoId,
                            0,
                            data,
                            true,
                            LyricsProvider.SIMPMUSIC,
                        )
                    }
                }

                else -> {
                    Logger.w(tag, "Get SimpMusic Translated Lyrics Error: ${response.message}")
                    getAITranslationLyrics(
                        videoId,
                        lyrics,
                    )
                }
            }
        }
    }

    private suspend fun getAITranslationLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        Logger.d(tag, "Get AI Translation Lyrics for $videoId")
        if (dataStoreManager.useAITranslation.first() == TRUE &&
            dataStoreManager.aiApiKey.first().isNotEmpty() &&
            dataStoreManager.enableTranslateLyric.first() == FALSE
        ) {
            val savedTranslatedLyrics =
                lyricsCanvasRepository
                    .getSavedTranslatedLyrics(
                        videoId,
                        dataStoreManager.translationLanguage.first(),
                    ).firstOrNull()
            if (savedTranslatedLyrics != null) {
                Logger.d(tag, "Get Saved Translated Lyrics")
                updateLyrics(
                    videoId,
                    0,
                    savedTranslatedLyrics.toLyrics(),
                    true,
                    LyricsProvider.AI,
                )
            } else {
                // Convert RICH_SYNCED to LINE_SYNCED before sending to AI
                // AI should only work with line-level or plain lyrics
                val lyricsForAi =
                    if (lyrics.syncType == "RICH_SYNCED") {
                        lyrics.toSyncedLyrics()
                    } else {
                        lyrics
                    }
                lyricsCanvasRepository
                    .getAITranslationLyrics(
                        lyricsForAi,
                        dataStoreManager.translationLanguage.first(),
                    ).cancellable()
                    .collectLatest {
                        val data = it.data
                        when (it) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Get AI Translate Lyrics Success")
                                lyricsCanvasRepository.insertTranslatedLyrics(
                                    TranslatedLyricsEntity(
                                        videoId = videoId,
                                        language = dataStoreManager.translationLanguage.first(),
                                        error = false,
                                        lines = data.lines,
                                        syncType = data.syncType,
                                    ),
                                )
                                updateLyrics(
                                    videoId,
                                    0,
                                    data,
                                    true,
                                    LyricsProvider.AI,
                                )
                            }

                            else -> {
                                Logger.w(tag, "Get AI Translate Lyrics Error: ${it.message}")
                            }
                        }
                    }
            }
        }
    }

    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Logger.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            lyricsCanvasRepository.getSpotifyLyrics(dataStoreManager, query, duration).cancellable().collect { response ->
                Logger.d("Check SpotifyLyrics", response.toString())
                val data = response.data
                when (response) {
                    is Resource.Success -> {
                        if (data != null && !data.lines.isNullOrEmpty()) {
                            insertLyrics(
                                data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )
                            getAITranslationLyrics(
                                track.videoId,
                                data,
                            )
                        } else {
                            getLrclibLyrics(
                                track.toSongEntity(),
                                track.artists.toListName().firstOrNull() ?: "",
                                duration ?: 0,
                            )
                        }
                    }

                    else -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    private fun setLyricsProvider() {
        viewModelScope.launch {
            val songEntity = nowPlayingState.value?.songEntity ?: return@launch
            val isVideo = nowPlayingState.value?.mediaItem?.isVideo() ?: false
            getLyricsFromFormat(isVideo, songEntity, timeline.value.total.toInt() / 1000)
        }
    }


    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            if (listTrack.size == 1 && dataStoreManager.endlessQueue.first() == TRUE) {
                mediaPlayerHandler.playNext(listTrack.first())
                makeToast(getString(Res.string.play_next))
            } else {
                mediaPlayerHandler.loadMoreCatalog(listTrack)
                makeToast(getString(Res.string.added_to_queue))
            }
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = mediaPlayerHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value
                if (!like) {
                    songRepository
                        .addToYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                makeToast(getString(Res.string.added_to_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                } else {
                    songRepository
                        .removeFromYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                makeToast(getString(Res.string.removed_from_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                }
            }
        }
    }

    fun getTranslucentBottomBar() = dataStoreManager.translucentBottomBar

    fun getEnableLiquidGlass() = dataStoreManager.enableLiquidGlass

    fun getLocalTrackingEnabled() = dataStoreManager.localTrackingEnabled

    // Drives the Mix for you tab: YouTube hands an anonymous session no mixes at all.
    fun getYouTubeLoggedIn() = dataStoreManager.loggedIn

    fun getThemeMode() = dataStoreManager.themeMode

    fun getThemeColorSource() = dataStoreManager.themeColorSource

    fun getCustomThemeColor() = dataStoreManager.customThemeColor

    fun getNowPlayingStyle() = dataStoreManager.nowPlayingStyle

    fun getAmbientMode() = dataStoreManager.ambientMode

    fun getPearlLavenderTheme() = dataStoreManager.pearlLavenderTheme

    fun setPearlLavenderTheme(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setPearlLavenderTheme(enabled)
        }
    }

    fun setAmbientMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAmbientMode(enabled)
        }
    }

    fun getLyricsStyle() = dataStoreManager.lyricsStyle

    fun getLyricsOffsetMs() = dataStoreManager.lyricsOffsetMs

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    fun setThemeColorSource(source: String) {
        viewModelScope.launch {
            dataStoreManager.setThemeColorSource(source)
        }
    }

    fun setCustomThemeColor(argbHex: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomThemeColor(argbHex)
        }
    }

    fun setNowPlayingStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setNowPlayingStyle(style)
        }
    }

    fun setLyricsStyle(style: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsStyle(style)
        }
    }

    fun getRomanizationLanguages() = dataStoreManager.romanizationLanguages

    fun setRomanizationLanguages(languages: Set<RomanizationLanguage>) {
        viewModelScope.launch {
            // Sorted by name so the stored string is stable: an unsorted Set writes a different
            // value for the same selection depending on iteration order, which makes the DataStore
            // flow emit on a change that did not happen.
            dataStoreManager.setRomanizationLanguages(languages.map { it.name }.sorted().joinToString(","))
        }
    }


    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }


    fun dismissUpdate() {
        _availableUpdate.value = null
        _updateDownloadState.value = UpdateDownloadState.Idle
    }

    fun resetUpdateDownloadState() {
        _updateDownloadState.value = UpdateDownloadState.Idle
    }

    fun downloadAndInstallUpdate(update: UpdateData) {
        viewModelScope.launch {
            _updateDownloadState.value = UpdateDownloadState.Downloading(0f, 0L, 0L)
            downloadAndInstallAppUpdate(
                update = update,
                onProgress = { progress, downloaded, total ->
                    _updateDownloadState.value = UpdateDownloadState.Downloading(progress, downloaded, total)
                },
                onReadyToInstall = {
                    _updateDownloadState.value = UpdateDownloadState.ReadyToInstall
                },
                onError = { err ->
                    _updateDownloadState.value = UpdateDownloadState.Error(err)
                },
            )
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (current.isBlank()) return true
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun checkForAppUpdate(silent: Boolean = true) {
        viewModelScope.launch {
            try {
                VersionManager.initialize()
                updateRepository.checkForGithubReleaseUpdate().collect { res ->
                    when (res) {
                        is Resource.Success -> {
                            val update = res.data
                            if (update != null && update.tagName.isNotBlank()) {
                                val current = VersionManager.getVersionName().trim().removePrefix("v").removePrefix("V")
                                val remote = update.tagName.trim().removePrefix("v").removePrefix("V")
                                if (remote.isNotEmpty() && isNewerVersion(remote, current)) {
                                    _availableUpdate.value = update
                                    if (!silent) {
                                        makeToast("Update available: v$remote")
                                    }
                                } else if (!silent) {
                                    makeToast("Pulse Music is up to date (v$current)")
                                }
                            }
                        }
                        is Resource.Error -> {
                            if (!silent) {
                                makeToast("Unable to check for updates. Please try again later.")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(tag, "checkForAppUpdate error: ${e.message}")
                if (!silent) {
                    makeToast("Unable to check for updates. Please try again later.")
                }
            }
        }
    }

    fun shouldCheckForUpdate(): Boolean = true


    fun downloadFile(bitmap: ImageBitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${getDownloadFolderPath()}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                val bytesArray = bitmap.toByteArray()
                try {
                    val fileOutputStream = FileOutputStream("$path.jpg")
                    fileOutputStream.write(bytesArray)
                    fileOutputStream.close()
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                songRepository
                    .downloadToFile(
                        track = track,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }



    /**
     * Vote for SimpMusic original lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteLyrics(upvote: Boolean) {
        val lyricsData = _nowPlayingScreenData.value.lyricsData
        val lyricsProvider = lyricsData?.lyricsProvider
        val simpMusicLyricsId = lyricsData?.lyrics?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _lyricsVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicLyrics(
                    lyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Lyrics Error ${result.message}")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Lyrics Success")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    private fun resetLyricsVoteState() {
        runCatching {
            _lyricsVoteState.value = null
            _translatedVoteState.value = null
        }.onFailure {
            Logger.w(tag, "Failed to reset lyrics vote state: ${it.message}")
        }
    }

    /**
     * Vote for SimpMusic translated lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteTranslatedLyrics(upvote: Boolean) {
        val translatedLyrics = _nowPlayingScreenData.value.lyricsData?.translatedLyrics
        val lyricsProvider = translatedLyrics?.second
        val simpMusicLyricsId = translatedLyrics?.first?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic translated lyrics or missing ID")
            return
        }

        viewModelScope.launch {
            _translatedVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
            lyricsCanvasRepository
                .voteSimpMusicTranslatedLyrics(
                    translatedLyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${result.message}")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Success(upvote),
                                    vote = it.vote + if (upvote) 1 else -1,
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    fun isUserLoggedIn(): Boolean = runBlocking { dataStoreManager.cookie.first().isNotEmpty() }

    // Flow-based variant of [isUserLoggedIn] so composables can collect login state once
    // instead of calling runBlocking inside composition (used by NowPlayingScreenContent).
    fun isUserLoggedInFlow(): Flow<Boolean> = dataStoreManager.cookie.map { it.isNotEmpty() }

    fun isCombineFavoriteAndYTLiked(): Boolean = runBlocking { dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    /**
     * Always advances to the previous track — bypasses the 3-second
     * "seek to start of current track" rule used by [Previous]. Used by the
     * NowPlaying artwork pager swipe.
     */
    data object SkipToPrevious : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    BETTER_LYRICS,
    BINI_LYRICS,
    MUSIXMATCH,
    AI,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

data class VoteData(
    val id: String,
    val vote: Int,
    val state: VoteState,
)

sealed class VoteState {
    data object Idle : VoteState()

    data object Loading : VoteState()

    data class Success(
        val upvote: Boolean,
    ) : VoteState()

    data class Error(
        val message: String,
    ) : VoteState()
}

/**
 * Whether a stored canvas url points at something a player should open rather than an image.
 *
 * The column holds whatever the active source wrote: a Spotify canvas is an `.mp4`, while AM
 * animated artwork is an HLS `.m3u8` master playlist. Testing only for `.mp4` — as this did before
 * AM existed — sends every AM artwork down the still-image branch, and because the branch that
 * reads this is the one that restores a *cached* url, the failure only appears from the second play
 * of a track onwards.
 */
private fun String.isCanvasVideoUrl(): Boolean = contains(".mp4") || contains(".m3u8")
