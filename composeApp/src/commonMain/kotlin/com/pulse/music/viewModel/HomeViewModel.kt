package com.pulse.music.viewModel

import androidx.lifecycle.viewModelScope
import com.manikandan.common.SELECTED_LANGUAGE
import com.manikandan.common.SUPPORTED_LANGUAGE
import com.manikandan.common.SUPPORTED_MUSIC_LANGUAGE
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.model.home.HomeDataCombine
import com.manikandan.domain.data.model.home.HomeItem
import com.manikandan.domain.data.model.home.chart.Chart
import com.manikandan.domain.data.model.mood.Mood
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.manager.DataStoreManager.Values.TRUE
import com.manikandan.domain.repository.HomeRepository
import com.manikandan.domain.utils.Resource
import com.manikandan.logger.Logger
import com.pulse.music.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pulsemusic.composeapp.generated.resources.Res
import pulsemusic.composeapp.generated.resources.music_video
import pulsemusic.composeapp.generated.resources.new_release
import pulsemusic.composeapp.generated.resources.song
import pulsemusic.composeapp.generated.resources.view_count
import com.manikandan.common.Config
import com.manikandan.domain.data.entities.LocalPlaylistEntity
import com.manikandan.domain.data.model.searchResult.playlists.PlaylistsResult
import com.manikandan.domain.data.model.spotify.SpotifyRecentTrack
import com.manikandan.domain.mediaservice.handler.PlaylistType
import com.manikandan.domain.mediaservice.handler.QueueData
import com.manikandan.domain.repository.LocalPlaylistRepository
import com.manikandan.domain.repository.PlaylistRepository
import com.manikandan.domain.repository.SearchRepository
import com.manikandan.domain.utils.toTrack
import com.manikandan.domain.extension.upgradeArtworkUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
    private val searchRepository: SearchRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) : BaseViewModel() {
    private val _spotifyRecentlyPlayed: MutableStateFlow<List<SpotifyRecentTrack>> = MutableStateFlow(emptyList())
    val spotifyRecentlyPlayed: StateFlow<List<SpotifyRecentTrack>> = _spotifyRecentlyPlayed.asStateFlow()

    private val _spotifyPlaylists: MutableStateFlow<List<PlaylistsResult>> = MutableStateFlow(emptyList())
    val spotifyPlaylists: StateFlow<List<PlaylistsResult>> = _spotifyPlaylists.asStateFlow()

    private val _spotifyTrending: MutableStateFlow<List<PlaylistsResult>> = MutableStateFlow(emptyList())
    val spotifyTrending: StateFlow<List<PlaylistsResult>> = _spotifyTrending.asStateFlow()

    private val _spotifyTrendingTracks: MutableStateFlow<List<SpotifyRecentTrack>> = MutableStateFlow(emptyList())
    val spotifyTrendingTracks: StateFlow<List<SpotifyRecentTrack>> = _spotifyTrendingTracks.asStateFlow()

    val isSpotifyLoggedIn: StateFlow<Boolean> =
        dataStoreManager
            .spdc
            .map { it.isNotBlank() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""
    private var musicStreamingLanguage: String = "auto"

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    init {
        homeJob = Job()
        viewModelScope.launch {
            regionCodeChart.value = dataStoreManager.chartKey.first()
            val chartRegion = regionCodeChart.value.let { if (it == null || it == "VN") "ZZ" else it }
            exploreChart(chartRegion)
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            // Single coordinated startup + settings collector: prevents 5x duplicate getHomeItemList()
            // calls and forced 1s cookie shimmer stalls on startup.
            var lastMusicLang: String? = null
            var lastCookie: String? = null
            val jobHome =
                launch {
                    combine(
                        dataStoreManager.location.distinctUntilChanged(),
                        dataStoreManager.language.distinctUntilChanged(),
                        dataStoreManager.musicLanguage.distinctUntilChanged(),
                        dataStoreManager.cookie.distinctUntilChanged(),
                        params,
                    ) { loc, lang, musicLang, cookieVal, paramVal ->
                        HomeTriggerState(loc, lang, musicLang, cookieVal, paramVal)
                    }.distinctUntilChanged().collectLatest { trigger ->
                        regionCode = trigger.location
                        language = trigger.language
                        musicStreamingLanguage = trigger.musicLanguage
                        if (trigger.musicLanguage != lastMusicLang) {
                            lastMusicLang = trigger.musicLanguage
                            val langRegion = SUPPORTED_MUSIC_LANGUAGE.getDefaultRegionForCode(trigger.musicLanguage)
                            if (langRegion != null) {
                                exploreChart(langRegion)
                            }
                        }
                        if (trigger.cookie != lastCookie) {
                            lastCookie = trigger.cookie
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                        getHomeItemList(trigger.params)
                    }
                }
            val job6 =
                launch {
                    homeItemList.collectLatest { list ->
                        _mainHomeThumbnail.value =
                            upgradeArtworkUrl(
                                list
                                    .firstOrNull()
                                    ?.contents
                                    ?.firstOrNull()
                                    ?.thumbnails
                                    ?.maxByOrNull { it.width.toLong() * it.height.toLong() }
                                    ?.url
                                    ?: list.firstOrNull()?.contents?.firstOrNull()?.thumbnails?.lastOrNull()?.url,
                            )
                    }
                }
            jobHome.join()
            job6.join()
        }
        viewModelScope.launch {
            dataStoreManager.spdc.distinctUntilChanged().collectLatest { spdc ->
                if (spdc.isNotBlank()) {
                    loadSpotifyHomeData()
                } else {
                    _spotifyRecentlyPlayed.value = emptyList()
                    _spotifyPlaylists.value = emptyList()
                    _spotifyTrending.value = emptyList()
                    _spotifyTrendingTracks.value = emptyList()
                }
            }
        }
    }

    fun loadSpotifyHomeData() {
        viewModelScope.launch {
            val spdc = dataStoreManager.spdc.first()
            if (spdc.isBlank()) {
                _spotifyRecentlyPlayed.value = emptyList()
                _spotifyPlaylists.value = emptyList()
                _spotifyTrending.value = emptyList()
                _spotifyTrendingTracks.value = emptyList()
                return@launch
            }
            // Fetch all Spotify shelves in parallel so a slow/empty endpoint never blocks the others
            launch {
                playlistRepository.getSpotifyPlaylists().collect { list ->
                    if (list != null) {
                        _spotifyPlaylists.value = list
                    }
                }
            }
            launch {
                playlistRepository.getSpotifyRecentlyPlayed().collect { list ->
                    if (list != null) {
                        _spotifyRecentlyPlayed.value = list
                    }
                }
            }
            launch {
                playlistRepository.getSpotifyTrendingPlaylists().collect { list ->
                    if (list != null) {
                        _spotifyTrending.value = list
                    }
                }
            }
            launch {
                playlistRepository.getSpotifyTrendingTracks().collect { list ->
                    if (list != null) {
                        _spotifyTrendingTracks.value = list
                    }
                }
            }
        }
    }

    private data class HomeTriggerState(
        val location: String,
        val language: String,
        val musicLanguage: String,
        val cookie: String,
        val params: String?,
    )

    private fun SpotifyRecentTrack.toPlayableTrack(): com.manikandan.domain.data.model.browse.album.Track {
        val cleanId = id.removePrefix("sp_track:")
        val crispThumb = upgradeArtworkUrl(thumbnail)
        val parsedArtists = artist
            .split(",")
            .mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) com.manikandan.domain.data.model.searchResult.songs.Artist(id = "", name = trimmed) else null
            }
            .ifEmpty { listOf(com.manikandan.domain.data.model.searchResult.songs.Artist(id = "", name = "Spotify")) }
        return com.manikandan.domain.data.model.browse.album.Track(
            album = com.manikandan.domain.data.model.searchResult.songs.Album(id = "", name = ""),
            artists = parsedArtists,
            duration = "",
            durationSeconds = 0,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = crispThumb?.let { listOf(com.manikandan.domain.data.model.searchResult.songs.Thumbnail(height = 640, url = it, width = 640)) } ?: emptyList(),
            title = title,
            videoId = "sp_track:$cleanId",
            videoType = "Song",
            category = "SONG",
            feedbackTokens = null,
            resultType = "Song",
            year = "",
        )
    }

    fun playSpotifyTrack(track: SpotifyRecentTrack) {
        viewModelScope.launch {
            val sourceList =
                when {
                    spotifyTrendingTracks.value.any { it.id == track.id && it.title == track.title } ->
                        spotifyTrendingTracks.value
                    spotifyRecentlyPlayed.value.any { it.id == track.id && it.title == track.title } ->
                        spotifyRecentlyPlayed.value
                    else -> listOf(track)
                }
            val queueTracks = ArrayList(sourceList.map { it.toPlayableTrack() })
            val targetTrack =
                queueTracks.find { it.videoId == "sp_track:${track.id.removePrefix("sp_track:")}" }
                    ?: track.toPlayableTrack().also { queueTracks.add(0, it) }
            val startIndex = queueTracks.indexOf(targetTrack).coerceAtLeast(0)
            setQueueData(
                QueueData.Data(
                    listTracks = queueTracks,
                    firstPlayedTrack = targetTrack,
                    playlistId = "spotify:home",
                    playlistName = "Spotify",
                    playlistType = PlaylistType.RADIO,
                    continuation = null,
                ),
            )
            loadMediaItem(
                targetTrack,
                type = Config.SONG_CLICK,
                index = startIndex,
            )
        }
    }

    fun openSpotifyPlaylist(
        playlist: PlaylistsResult,
        onNavigateLocal: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val spotifyId = playlist.browseId
            val allLocal = localPlaylistRepository.getAllLocalPlaylists().firstOrNull()
            val existing = allLocal?.find { it.youtubePlaylistId == spotifyId }
            if (existing != null) {
                onNavigateLocal(existing.id)
            } else {
                localPlaylistRepository.insertLocalPlaylist(
                    LocalPlaylistEntity(
                        title = playlist.title,
                        thumbnail = upgradeArtworkUrl(
                            playlist.thumbnails?.maxByOrNull { it.width.toLong() * it.height.toLong() }?.url
                                ?: playlist.thumbnails?.firstOrNull()?.url,
                        ),
                        youtubePlaylistId = spotifyId,
                    ),
                    "Created playlist",
                ).collect {
                    val updated = localPlaylistRepository.getAllLocalPlaylists().firstOrNull()?.find {
                        it.youtubePlaylistId == spotifyId
                    }
                    if (updated != null) {
                        onNavigateLocal(updated.id)
                    }
                }
            }
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun getHomeItemList(params: String? = null) {
        if (_homeItemList.value.isEmpty()) {
            loading.value = true
        }
        _homeListState.value = ListState.LOADING
        loadSpotifyHomeData()
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                language =
                    dataStoreManager.getString(SELECTED_LANGUAGE).first()
                        ?: SUPPORTED_LANGUAGE.codes.first()
                musicStreamingLanguage =
                    runCatching { dataStoreManager.musicLanguage.first() }.getOrDefault("auto")
                regionCode = dataStoreManager.location.first()
                combine(
                    homeRepository.getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ),
                    homeRepository.getMoodAndMomentsData(),
                    homeRepository.getChartData(dataStoreManager.chartKey.first()),
                    homeRepository.getNewRelease(
                        getString(Res.string.new_release),
                        getString(Res.string.music_video),
                    ),
                ) { home, exploreMood, exploreChart, newRelease ->
                    HomeDataCombine(home, exploreMood, exploreChart, newRelease)
                }.catch { e ->
                    Logger.e("HomeViewModel", "getHomeItemList failed: ${e.message}")
                    loading.value = false
                    _homeListState.value = ListState.IDLE
                    showSnackBarErrorState.emit(e.message ?: "Network error")
                }.collect { result ->
                    val home = result.home
                    Logger.d("home size", "${home.data?.second?.size}")
                    val exploreMoodItem = result.mood
                    val chart = result.chart
                    val newRelease = result.newRelease
                    when (home) {
                        is Resource.Success -> {
                            _continuation.value = home.data?.first
                            _homeItemList.value = filterCleanHomeItems(home.data?.second ?: listOf())
                        }

                        else -> {
                            _continuation.value = null
                            _homeItemList.value = listOf()
                        }
                    }
                    if (continuation.value.isNullOrEmpty()) {
                        _homeListState.value = ListState.PAGINATION_EXHAUST
                    } else {
                        _homeListState.value = ListState.IDLE
                    }
                    when (chart) {
                        is Resource.Success -> {
                            _chart.value = filterCleanChart(chart.data)
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    when (newRelease) {
                        is Resource.Success -> {
                            _newRelease.value = filterCleanHomeItems(newRelease.data ?: arrayListOf())
                        }

                        else -> {
                            _newRelease.value = arrayListOf()
                        }
                    }
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                        }

                        else -> {
                            _exploreMoodItem.value = null
                        }
                    }
                    regionCodeChart.value = dataStoreManager.chartKey.first()
                    Logger.d("HomeViewModel", "getHomeItemList: $result")
                    dataStoreManager.cookie.first().let {
                        if (it != "") {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                    }
                    when {
                        home is Resource.Error -> home.message
                        exploreMoodItem is Resource.Error -> exploreMoodItem.message
                        chart is Resource.Error -> chart.message
                        else -> null
                    }?.let {
                        showSnackBarErrorState.emit(it)
                        Logger.w("Error", "getHomeItemList: ${home.message}")
                        Logger.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                        Logger.w("Error", "getHomeItemList: ${chart.message}")
                    }
                    loading.value = false
                }
            }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val newItems = filterCleanHomeItems(home.data?.second ?: listOf())
                                _homeItemList.update { it + newItems }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).catch { e ->
                    Logger.e("HomeViewModel", "exploreChart failed: ${e.message}")
                    loadingChart.value = false
                }.collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = filterCleanChart(values.data)
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    private fun isBollywoodOrVideo(content: com.manikandan.domain.data.model.home.Content?, musicLang: String = musicStreamingLanguage): Boolean {
        if (content == null) return false
        val title = content.title.lowercase()
        val desc = content.description?.lowercase().orEmpty()
        val album = content.album?.name?.lowercase().orEmpty()
        val videoType = content.videoType?.lowercase().orEmpty()

        // Filter Bollywood content only when user hasn't explicitly selected Hindi
        if (musicLang != "hi") {
            if (title.contains("bollywood") || desc.contains("bollywood") || album.contains("bollywood")) {
                return true
            }
            if (content.artists?.any { it.name.lowercase().contains("bollywood") } == true) {
                return true
            }
        }

        // Filter video songs (YouTube Music classifies standard audio tracks as ATV - Audio Track Video)
        if (videoType.isNotEmpty() && !videoType.contains("atv") && (videoType.contains("omv") || videoType.contains("ugc") || videoType.contains("video"))) {
            return true
        }

        // Filter video songs by title patterns
        val videoPatterns = listOf(
            "official video",
            "official music video",
            "music video",
            "video song",
            "full video",
            "visualizer",
            "lyric video",
            "performance video",
            "(video)",
            "[video]",
            "(mv)",
            "[mv]"
        )
        return videoPatterns.any { title.contains(it) }
    }

    private fun isBollywoodOrVideoShelf(shelfTitle: String, musicLang: String = musicStreamingLanguage): Boolean {
        val lower = shelfTitle.lowercase()
        val hasBollywood = if (musicLang != "hi") lower.contains("bollywood") else false
        return hasBollywood || lower.contains("video") || lower.contains("music video")
    }

    private fun filterCleanHomeItems(items: List<HomeItem>): List<HomeItem> {
        return items.mapNotNull { item ->
            if (isBollywoodOrVideoShelf(item.title)) {
                null
            } else {
                val filteredContents = item.contents.filterNotNull().filter { content ->
                    !isBollywoodOrVideo(content)
                }
                if (filteredContents.isEmpty()) {
                    null
                } else {
                    item.copy(contents = filteredContents)
                }
            }
        }
    }

    private fun filterCleanChart(chart: Chart?): Chart? {
        if (chart == null) return null
        val filteredPlaylists = chart.listChartItem.mapNotNull { item ->
            if (isBollywoodOrVideoShelf(item.title)) {
                null
            } else {
                val cleanPlaylists = item.playlists.filter { playlist ->
                    val pTitle = playlist.title.lowercase()
                    !pTitle.contains("bollywood") && !pTitle.contains("video")
                }
                if (cleanPlaylists.isEmpty()) null else item.copy(playlists = cleanPlaylists)
            }
        }
        return chart.copy(listChartItem = filteredPlaylists)
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}