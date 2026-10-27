package com.pulse.music.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.manikandan.domain.data.entities.AlbumEntity
import com.manikandan.domain.data.entities.LocalPlaylistEntity
import com.manikandan.domain.data.entities.PlaylistEntity
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.model.searchResult.playlists.PlaylistsResult
import com.manikandan.domain.data.type.ChartItem
import com.manikandan.domain.data.type.MonthlyRecapItem
import com.manikandan.domain.data.type.PlaylistType
import com.manikandan.domain.data.type.RecentlyType
import com.manikandan.domain.extension.now
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.repository.AlbumRepository
import com.manikandan.domain.repository.AnalyticsRepository
import com.manikandan.domain.repository.CommonRepository
import com.manikandan.domain.repository.LocalPlaylistRepository
import com.manikandan.domain.repository.PlaylistRepository
import com.manikandan.domain.repository.PodcastRepository
import com.manikandan.domain.repository.SongRepository
import com.manikandan.domain.utils.LocalResource
import com.manikandan.domain.utils.Resource
import com.manikandan.domain.utils.isRadioPlaylistId
import com.pulse.music.ui.screen.home.analytics.monthFullNameResource
import com.pulse.music.ui.screen.library.LibraryDynamicPlaylistType
import com.pulse.music.viewModel.base.BaseViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_local_playlist
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year
import simpmusic.composeapp.generated.resources.youtube_liked_music
import com.manikandan.domain.data.entities.DownloadState
import com.pulse.music.ui.screen.library.UnifiedShelfItem
import com.pulse.music.ui.screen.library.UnifiedShelfFilter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val podcastRepository: PodcastRepository,
) : BaseViewModel() {
    private val _currentScreen: MutableStateFlow<LibraryChipType> = MutableStateFlow(LibraryChipType.YOUR_LIBRARY)
    val currentScreen: StateFlow<LibraryChipType> get() = _currentScreen.asStateFlow()
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _chartPlaylists: MutableStateFlow<LocalResource<List<ChartItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val chartPlaylists: StateFlow<LocalResource<List<ChartItem>>> get() = _chartPlaylists.asStateFlow()

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong.asStateFlow()

    /**
     * The months the Wrapped tab offers a recap for, newest first.
     *
     * A [MonthlyRecapItem] rather than the destination's own
     * [LibraryDynamicPlaylistType.MonthlyRecap]: the tab draws these through the shared
     * `GridLibraryPlaylist`, which renders only [PlaylistType]s, and a tile needs a title and a
     * cover on top of the year and month the destination carries. The destination is rebuilt from
     * the year and month when a tile is tapped.
     */
    private val _monthlyRecaps: MutableStateFlow<LocalResource<List<MonthlyRecapItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val monthlyRecaps: StateFlow<LocalResource<List<MonthlyRecapItem>>> get() = _monthlyRecaps.asStateFlow()

    private val _accountThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    /**
     * Whether the Wrapped chip has anything behind it.
     *
     * The same setting the Analytics tab follows, read the same way — Wrapped and the recaps are
     * built entirely from `playback_event`, which local tracking is what fills.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val localTrackingEnabled = dataStoreManager.localTrackingEnabled.mapLatest { it == DataStoreManager.TRUE }

    init {
        viewModelScope.launch {
            val currentScreenJob =
                launch {
                    dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                        LibraryChipType.fromStringValue(chipType)?.let {
                            _currentScreen.value = it
                        }
                    }
                }
            val cookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collect { cookie ->
                        _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
                        if (cookie.isNotEmpty()) {
                            getRecentlyAdded()
                            getYouTubePlaylist()
                            getYouTubeMixedForYou()
                        }
                    }
                }
            currentScreenJob.join()
            cookieJob.join()
        }
    }

    fun setCurrentScreen(chipType: LibraryChipType) {
        _currentScreen.value = chipType
        viewModelScope.launch {
            dataStoreManager.putString("library_current_screen", chipType.toStringValue())
        }
    }

    fun getRecentlyAdded() {
        viewModelScope.launch {
            commonRepository.getAllRecentData().collectLatest { data ->
                val temp: MutableList<RecentlyType> = mutableListOf()
                temp.addAll(data)
                temp
                    .find {
                        it is PlaylistEntity && it.id.isRadioPlaylistId()
                    }.let {
                        temp.remove(it)
                    }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
                    temp.removeIf { it is PlaylistEntity && it.id == "LM" }
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.youtube_liked_music),
                            author = "YouTube Music",
                            id = "LM",
                            description = "PIN",
                            thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png",
                        ),
                    )
                }
                temp.reverse()
                _recentlyAdded.value = LocalResource.Success(temp.toImmutableList())
            }
        }
    }

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { data ->
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getMixedForYou().collect { data ->
                _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            albumRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                playlistRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                        is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                        is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                            },
                        )
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    fun getCanvasSong() {
        _listCanvasSong.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    /**
     * Which of the last twelve months the user actually listened in, and what each tile shows.
     *
     * A month with no plays is left out rather than shown empty: a "Recap March" that opens onto
     * nothing is worse than no row at all. Twelve is a cap, not a quota — a new install shows one
     * row, or none.
     *
     * The count comes first and gates everything after it: twelve `COUNT`s over an indexed
     * timestamp range are cheap, so the months with nothing in them are dropped before anything
     * asks them for a ranking. Only the survivors pay for a cover.
     *
     * Title and cover are resolved here rather than in the tile, which cannot suspend: the title
     * needs a month name out of a string resource with a format argument, and the cover needs a
     * ranking query followed by a song lookup.
     */
    fun getMonthlyRecaps() {
        _monthlyRecaps.value = LocalResource.Loading()
        viewModelScope.launch {
            val today = now().date
            val thisMonth = LocalDate(today.year, today.month, 1)
            val months =
                (0 until MONTHS_OF_RECAP)
                    .map { thisMonth.minus(it, DateTimeUnit.MONTH) }
                    .mapNotNull { firstDay ->
                        val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                        val start = firstDay.atTime(0, 0)
                        val end = lastDay.atTime(23, 59, 59)
                        val plays =
                            analyticsRepository
                                .getPlaybackEventCountInRange(
                                    startTimestamp = start,
                                    endTimestamp = end,
                                ).firstOrNull() ?: 0L
                        if (plays <= 0L) return@mapNotNull null
                        MonthlyRecapItem(
                            year = firstDay.year,
                            month = firstDay.month.number,
                            title = recapTitle(firstDay.year, firstDay.month, today.year),
                        )
                    }
            _monthlyRecaps.value = LocalResource.Success(months)
        }
    }

    /**
     * "Recap January", or "Recap January 2025" once the year stops being obvious.
     *
     * The same rule and the same two format strings as the header the tile opens — see
     * [LibraryDynamicPlaylistType.title]. Fully qualified because [BaseViewModel] has a `getString`
     * of its own that takes no format argument and wraps `runBlocking`, which has no business
     * running inside a coroutine that is already suspended here.
     */
    private suspend fun recapTitle(
        year: Int,
        month: Month,
        currentYear: Int,
    ): String {
        val monthName =
            org.jetbrains.compose.resources
                .getString(monthFullNameResource(month))
        return if (year == currentYear) {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month, monthName)
        } else {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month_year, monthName, year.toString())
        }
    }

    fun getChartPlaylists() {
        _chartPlaylists.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getChartPlaylist().collectLatest {
                when (it) {
                    is Resource.Success -> _chartPlaylists.value = LocalResource.Success(it.data ?: emptyList())
                    is Resource.Error -> _chartPlaylists.value = LocalResource.Error(it.message ?: "Unknown error")
                }
            }
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            localPlaylistRepository
                .insertLocalPlaylist(
                    localPlaylistEntity,
                    getString(Res.string.added_local_playlist),
                ).lastOrNull()
                ?.let {
                    log("Created playlist with id: $it")
                }
            getLocalPlaylist()
        }
    }

    fun deleteSong(videoId: String) {
        _recentlyAdded.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            delay(500) // Wait for the database to update
            getRecentlyAdded()
        }
    }

    private val _unifiedItems: MutableStateFlow<List<UnifiedShelfItem>> = MutableStateFlow(emptyList())
    private val _unifiedQuery: MutableStateFlow<String> = MutableStateFlow("")
    val unifiedQuery: StateFlow<String> = _unifiedQuery.asStateFlow()
    private val _unifiedFilter: MutableStateFlow<UnifiedShelfFilter> = MutableStateFlow(UnifiedShelfFilter.ALL)
    val unifiedFilter: StateFlow<UnifiedShelfFilter> = _unifiedFilter.asStateFlow()

    val filteredUnifiedItems: StateFlow<List<UnifiedShelfItem>> =
        combine(_unifiedItems, _unifiedQuery, _unifiedFilter) { items, query, filter ->
            items.filter { item ->
                val matchesFilter = when (filter) {
                    UnifiedShelfFilter.ALL -> true
                    UnifiedShelfFilter.STREAMING -> item.filterType == UnifiedShelfFilter.STREAMING
                    UnifiedShelfFilter.ALBUMS -> item.filterType == UnifiedShelfFilter.ALBUMS
                    UnifiedShelfFilter.PERSONAL_AUDIO -> item.filterType == UnifiedShelfFilter.PERSONAL_AUDIO
                }
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    item.title.contains(query, ignoreCase = true) || item.subtitle.contains(query, ignoreCase = true)
                }
                matchesFilter && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUnifiedQuery(query: String) {
        _unifiedQuery.value = query
    }

    fun setUnifiedFilter(filter: UnifiedShelfFilter) {
        _unifiedFilter.value = filter
    }

    fun getUnifiedShelfData() {
        viewModelScope.launch {
            combine(
                songRepository.getAllSongs(1000),
                albumRepository.getAllAlbums(1000),
            ) { allSongs, allAlbums ->
                val list = mutableListOf<UnifiedShelfItem>()
                allSongs.forEach { song ->
                    if (song.inLibrary != Config.REMOVED_SONG_DATE_TIME) {
                        if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                            list.add(UnifiedShelfItem.PersonalAudio(song))
                        } else {
                            list.add(UnifiedShelfItem.StreamingSong(song))
                        }
                    }
                }
                allAlbums.forEach { album ->
                    list.add(UnifiedShelfItem.Album(album))
                }
                list.sortBy { it.title.lowercase() }
                list
            }.collectLatest {
                _unifiedItems.value = it
            }
        }
    }

    companion object {
        /** How far back the Wrapped tab offers recaps, counting the current month as the first. */
        private const val MONTHS_OF_RECAP = 12
    }
}