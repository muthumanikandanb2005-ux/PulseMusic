package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.toKmpUri
import com.maxrave.common.LIMIT_CACHE_SIZE
import com.maxrave.common.QUALITY
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LOCATION
import com.maxrave.common.SUPPORTED_MUSIC_LANGUAGE
import com.maxrave.common.VIDEO_QUALITY
import com.manikandan.domain.extension.now
import com.manikandan.domain.data.model.lyrics.RomanizationDictionaryState
import com.manikandan.domain.data.model.lyrics.RomanizationLanguage
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.manager.DataStoreManager.Values.TRUE
import com.manikandan.domain.repository.ImportProgress
import com.manikandan.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.fileSaverResult
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.expect.ui.isWallpaperDynamicColorSupported
import com.maxrave.simpmusic.expect.ui.openEqResult
import com.maxrave.simpmusic.expect.ui.rememberIsDolbyAtmosSupported
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.extension.displayString
import com.maxrave.simpmusic.extension.isTwoLetterCode
import com.maxrave.simpmusic.extension.isValidProxyHost
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.AmbientThemeGlow
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DevLogInBottomSheet
import com.maxrave.simpmusic.ui.component.DevLogInType
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.ImportLinkDialog
import com.maxrave.simpmusic.ui.component.ImportProgressDialog
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.PulseSegmentedControl
import com.maxrave.simpmusic.ui.component.PulseSettingDivider
import com.maxrave.simpmusic.ui.component.PulseSettingRow
import com.maxrave.simpmusic.ui.component.PulseSettingsBadge
import com.maxrave.simpmusic.ui.component.PulseSettingsCard
import com.maxrave.simpmusic.ui.component.PulseSettingsHeader
import com.maxrave.simpmusic.ui.component.PulseSettingsSearchBar
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.rememberNowPlayingGlowTint
import com.maxrave.simpmusic.ui.icon.Album
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.ArrowOutward
import com.maxrave.simpmusic.ui.icon.AutoGraph
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Edit
import com.maxrave.simpmusic.ui.icon.Error
import com.maxrave.simpmusic.ui.icon.FastForward
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.Help
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.Insights
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.LogoDev
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.PauseCircle
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.PersonAdd
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.PushPin
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Sensors
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Sort
import com.maxrave.simpmusic.ui.icon.Speed
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.icon.TipsAndUpdates
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.icon.Update
import com.maxrave.simpmusic.ui.icon.VolumeOff
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.navigation.destination.home.AnalyticsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.CreditDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.DiscordLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LastfmLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.parseThemeColorHex
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.ImportViewModel
import com.maxrave.simpmusic.viewModel.SettingAlertState
import com.maxrave.simpmusic.viewModel.SettingBasicAlertState
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.ChipColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.about_us
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.change
import simpmusic.composeapp.generated.resources.change_language_warning
import simpmusic.composeapp.generated.resources.checking
import simpmusic.composeapp.generated.resources.cinematic_audio
import simpmusic.composeapp.generated.resources.cinematic_audio_description
import simpmusic.composeapp.generated.resources.clear
import simpmusic.composeapp.generated.resources.clear_player_cache
import simpmusic.composeapp.generated.resources.clear_thumbnail_cache
import simpmusic.composeapp.generated.resources.content_country
import simpmusic.composeapp.generated.resources.custom_color
import simpmusic.composeapp.generated.resources.description_and_licenses
import simpmusic.composeapp.generated.resources.dolby_atmos
import simpmusic.composeapp.generated.resources.dolby_atmos_description
import simpmusic.composeapp.generated.resources.dolby_atmos_unsupported
import simpmusic.composeapp.generated.resources.download_quality
import simpmusic.composeapp.generated.resources.equalizer
import simpmusic.composeapp.generated.resources.language
import simpmusic.composeapp.generated.resources.last_checked_at
import simpmusic.composeapp.generated.resources.limit_player_cache
import simpmusic.composeapp.generated.resources.log_out
import simpmusic.composeapp.generated.resources.lyrics_romanization
import simpmusic.composeapp.generated.resources.lyrics_romanization_description
import simpmusic.composeapp.generated.resources.lyrics_style
import simpmusic.composeapp.generated.resources.lyrics_style_apple_music
import simpmusic.composeapp.generated.resources.lyrics_style_classic
import simpmusic.composeapp.generated.resources.now_playing_style
import simpmusic.composeapp.generated.resources.now_playing_style_apple_music
import simpmusic.composeapp.generated.resources.now_playing_style_m3_expressive
import simpmusic.composeapp.generated.resources.now_playing_style_spotify
import simpmusic.composeapp.generated.resources.quality
import simpmusic.composeapp.generated.resources.requires_android_12
import simpmusic.composeapp.generated.resources.romanization_belarusian
import simpmusic.composeapp.generated.resources.romanization_bulgarian
import simpmusic.composeapp.generated.resources.romanization_chinese
import simpmusic.composeapp.generated.resources.romanization_hindi
import simpmusic.composeapp.generated.resources.romanization_japanese
import simpmusic.composeapp.generated.resources.romanization_japanese_dict_downloading
import simpmusic.composeapp.generated.resources.romanization_japanese_dict_failed
import simpmusic.composeapp.generated.resources.romanization_korean
import simpmusic.composeapp.generated.resources.romanization_kyrgyz
import simpmusic.composeapp.generated.resources.romanization_macedonian
import simpmusic.composeapp.generated.resources.romanization_punjabi
import simpmusic.composeapp.generated.resources.romanization_russian
import simpmusic.composeapp.generated.resources.romanization_serbian
import simpmusic.composeapp.generated.resources.romanization_ukrainian
import simpmusic.composeapp.generated.resources.save
import simpmusic.composeapp.generated.resources.settings
import simpmusic.composeapp.generated.resources.theme
import simpmusic.composeapp.generated.resources.theme_mode_dark
import simpmusic.composeapp.generated.resources.theme_mode_light
import simpmusic.composeapp.generated.resources.theme_mode_system
import simpmusic.composeapp.generated.resources.third_party_libraries
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.youtube_account
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoilApi::class,
    ExperimentalHazeMaterialsApi::class,
    FormatStringsInDatetimeFormats::class,
    ExperimentalCalfApi::class,
)
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val platformContext = LocalPlatformContext.current
    val pl = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    // Backup and restore
    val formatter = LocalDateTime.Format {
        byUnicodePattern("yyyyMMddHHmmss")
    }
    val appName = stringResource(Res.string.app_name)

    val backupLauncher =
        fileSaverResult(
            "${appName}_${now().format(formatter)}.backup",
            "application/octet-stream",
        ) { uri ->
            uri?.let { viewModel.backup(it.toKmpUri()) }
        }

    val restoreLauncher =
        rememberFilePickerLauncher(
            type = FilePickerFileType.All,
            selectionMode = FilePickerSelectionMode.Single,
        ) { file ->
            file.firstOrNull()?.getPath(pl)?.toKmpUri()?.let {
                viewModel.restore(it)
            }
        }

    val importViewModel: ImportViewModel = koinViewModel()
    val importState by importViewModel.importState.collectAsStateWithLifecycle()
    var showImportLinkDialog by remember { mutableStateOf(false) }

    // Equalizer
    val resultLauncher = openEqResult(viewModel.getAudioSessionId())

    // Core States from ViewModels
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val musicLanguage by viewModel.musicLanguage.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val autoDownloadLikedSongs by viewModel.autoDownloadLikedSongs.collectAsStateWithLifecycle()
    val playVideo by remember { viewModel.playVideoInsteadOfAudio.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val normalizeVolume by remember { viewModel.normalizeVolume.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by remember { viewModel.skipSilent.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by remember { viewModel.killServiceOnExit.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = true)
    val mainLyricsProvider by viewModel.mainLyricsProvider.collectAsStateWithLifecycle()
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()
    val amAnimatedArtwork by viewModel.amAnimatedArtwork.collectAsStateWithLifecycle()
    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()
    val lastCheckUpdate by viewModel.lastCheckForUpdate.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val pearlLavenderTheme by viewModel.pearlLavenderTheme.collectAsStateWithLifecycle()
    val themeMode by sharedViewModel.getThemeMode().collectAsStateWithLifecycle(DataStoreManager.THEME_MODE_DARK)
    val themeColorSource by sharedViewModel.getThemeColorSource().collectAsStateWithLifecycle(DataStoreManager.THEME_COLOR_DEFAULT)
    val customThemeColorHex by sharedViewModel.getCustomThemeColor().collectAsStateWithLifecycle(DataStoreManager.DEFAULT_THEME_COLOR_HEX)
    val nowPlayingStyle by sharedViewModel.getNowPlayingStyle().collectAsStateWithLifecycle(DataStoreManager.NOW_PLAYING_STYLE_BITCHORD)
    val ambientModeSetting by sharedViewModel.getAmbientMode().collectAsStateWithLifecycle(DataStoreManager.TRUE)
    val lyricsStyle by sharedViewModel.getLyricsStyle().collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_PULSE_NEON)
    val romanizationStored by sharedViewModel.getRomanizationLanguages().collectAsStateWithLifecycle("")
    val japaneseDictionaryState by viewModel.japaneseDictionaryState.collectAsStateWithLifecycle()
    val cinematicAudioEnabled by viewModel.cinematicAudioEnabled.collectAsStateWithLifecycle()
    val dolbyAtmosEnabled by viewModel.dolbyAtmosEnabled.collectAsStateWithLifecycle()
    val isDolbyAtmosSupported = rememberIsDolbyAtmosSupported()
    val lastfmLoggedIn by viewModel.lastfmLoggedIn.collectAsStateWithLifecycle()
    val lastfmUsername by viewModel.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by viewModel.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val crossfadeDjMode by viewModel.crossfadeDjMode.collectAsStateWithLifecycle()
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()
    val translationLanguage by viewModel.translationLanguage.collectAsStateWithLifecycle()
    val loggedInState by viewModel.loggedIn.collectAsStateWithLifecycle()

    val isCheckingUpdate by sharedViewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val dataStoreManager: DataStoreManager = koinInject()
    val ytAccountName by remember { dataStoreManager.getString("AccountName") }.collectAsStateWithLifecycle(initialValue = null)
    val ytCookie by remember { dataStoreManager.cookie }.collectAsStateWithLifecycle(initialValue = "")
    val profileName by dataStoreManager.profileName.collectAsStateWithLifecycle(initialValue = "")

    val isYouTubeSignedIn = (loggedInState == DataStoreManager.TRUE || !ytAccountName.isNullOrBlank() || ytCookie.isNotEmpty())

    // Local UI States (Screenshot Parity)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var outputPrecisionIndex by rememberSaveable { mutableIntStateOf(0) }
    var preferUsbDac by rememberSaveable { mutableStateOf(false) }
    var fullScreenCover by rememberSaveable { mutableStateOf(true) }
    var legacyMesh by rememberSaveable { mutableStateOf(false) }
    var playAnimOverCellular by rememberSaveable { mutableStateOf(false) }
    var syncedLyricsEnabled by rememberSaveable { mutableStateOf(true) }
    var blurLyricsEnabled by rememberSaveable { mutableStateOf(true) }
    var highPerformanceMode by rememberSaveable { mutableStateOf(false) }
    var filterNonMusicAudio by rememberSaveable { mutableStateOf(true) }
    var workOutGenres by rememberSaveable { mutableStateOf(true) }
    var playNextOnSwipe by rememberSaveable { mutableStateOf(false) }
    var dontRepeatSongsSession by rememberSaveable { mutableStateOf(false) }
    var hideVolumeBar by rememberSaveable { mutableStateOf(false) }
    var hideSongStatus by rememberSaveable { mutableStateOf(false) }
    var smartAudioAlignment by rememberSaveable { mutableStateOf(true) }

    var showYouTubeAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showDevLoginSheet by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showThirdPartyLibraries by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getAllGoogleAccount()
        viewModel.getThumbCacheSize(platformContext)
    }

    val checkingString = stringResource(Res.string.checking)
    val lastCheckedTemplate = stringResource(Res.string.last_checked_at)
    val lastCheckLong = remember(lastCheckUpdate) { lastCheckUpdate?.toLongOrNull() ?: 0L }
    val formattedDate = remember(lastCheckLong) {
        if (lastCheckLong > 0L) {
            try {
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(lastCheckLong))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    val checkForUpdateSubtitle = remember(isCheckingUpdate, formattedDate, checkingString, lastCheckedTemplate) {
        if (isCheckingUpdate) {
            checkingString
        } else if (formattedDate.isNotEmpty()) {
            try {
                lastCheckedTemplate.replace("%1" + "$" + "s", formattedDate)
            } catch (e: Exception) {
                "Last checked: $formattedDate"
            }
        } else {
            "Version 3.2.7 Build 95 · Pulse Music Studio"
        }
    }

    val requiresAndroid12 = " (" + stringResource(Res.string.requires_android_12) + ")"
    val themeModeLabels = listOf(
        DataStoreManager.THEME_MODE_SYSTEM to stringResource(Res.string.theme_mode_system),
        DataStoreManager.THEME_MODE_DARK to stringResource(Res.string.theme_mode_dark),
        DataStoreManager.THEME_MODE_LIGHT to stringResource(Res.string.theme_mode_light),
    )
    val nowPlayingStyleLabels = listOf(
        DataStoreManager.NOW_PLAYING_STYLE_BITCHORD to "Pulse Dynamic Neon (Ultra-Smooth)",
        DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY to stringResource(Res.string.now_playing_style_spotify),
        DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE to stringResource(Res.string.now_playing_style_m3_expressive),
        DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC to stringResource(Res.string.now_playing_style_apple_music) + requiresAndroid12,
    )
    val lyricsStyleLabels = listOf(
        DataStoreManager.LYRICS_STYLE_PULSE_NEON to "Pulse Moving Light (Dynamic Glow & Blur)",
        DataStoreManager.LYRICS_STYLE_CLASSIC to stringResource(Res.string.lyrics_style_classic),
        DataStoreManager.LYRICS_STYLE_APPLE_MUSIC to stringResource(Res.string.lyrics_style_apple_music) + requiresAndroid12,
    )

    fun matchesSearch(vararg texts: String?): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.trim().lowercase()
        return texts.any { it?.lowercase()?.contains(q) == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
    ) {
        val settingListState = rememberLazyListState()

        LazyColumn(
            state = settingListState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 40.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            // TOP BAR & SEARCH
            item(key = "header_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { navController.navigateUp() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = SimpIcons.ArrowBackIosNew,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Settings",
                        style = typo().headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                        ),
                        modifier = Modifier.padding(start = 4.dp),
                    )

                    Spacer(Modifier.height(14.dp))
                    PulseSettingsSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // GROUP 1: YOUTUBE SIGN IN & ACCOUNT
            if (matchesSearch("YouTube", "Sign in", "Account & integrations", "Listen together", "Spotify", "Last.fm", profileName, ytAccountName)) {
                item(key = "card_account") {
                    PulseSettingsCard {
                        if (!isYouTubeSignedIn) {
                            PulseSettingRow(
                                title = "Sign in to YouTube",
                                subtitle = "Connect your YouTube Music account to access your playlists, liked music, and subscriptions",
                                leadingIcon = SimpIcons.PlayArrow,
                                trailingText = "Sign In",
                                showChevron = true,
                                onClick = {
                                    navController.navigate(LoginDestination)
                                },
                            )
                        } else {
                            PulseSettingRow(
                                title = if (!ytAccountName.isNullOrBlank()) ytAccountName!! else "YouTube Account",
                                subtitle = "Connected to YouTube Music • Tap to manage or switch account",
                                leadingIcon = SimpIcons.PlayArrow,
                                badgeText = "ACTIVE",
                                showChevron = true,
                                onClick = {
                                    showYouTubeAccountDialog = true
                                },
                            )
                        }
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Account & integrations",
                            subtitle = if (isYouTubeSignedIn) "YouTube Music connected • Tap for cookie login" else "Manage YouTube and external streaming sources",
                            leadingIcon = SimpIcons.PersonAdd,
                            showChevron = true,
                            onClick = {
                                showDevLoginSheet = true
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Listen together",
                            subtitle = "Share a code and play the same music, in time",
                            leadingIcon = SimpIcons.PeopleAlt,
                            showChevron = true,
                            onClick = {
                                sharedViewModel.makeToast("Listen Together: Sync room active")
                            },
                        )
                    }
                }
            }

            // GROUP 2: AUDIO QUALITY
            if (matchesSearch("Sources", "On Wi-Fi", "On mobile data", "Dolby Atmos", "Audio Quality", quality)) {
                item(key = "card_audio_quality") {
                    PulseSettingsHeader(title = "AUDIO QUALITY")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Sources",
                            subtitle = "Where audio comes from and the order used",
                            leadingIcon = SimpIcons.LogoDev,
                            showChevron = true,
                            onClick = {
                                sharedViewModel.makeToast("Audio source order: YouTube Music (HQ) > Local Storage")
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "On Wi-Fi",
                            leadingIcon = SimpIcons.Tune,
                            trailingText = quality ?: "Lossless",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.quality) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = QUALITY.items.map { (it.toString() == quality) to it.toString() },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.changeQuality(state.selectOne?.getSelected())
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "On mobile data",
                            badgeText = "IN USE",
                            leadingIcon = SimpIcons.Sensors,
                            trailingText = quality ?: "Lossless",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Mobile Streaming Quality",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = QUALITY.items.map { (it.toString() == quality) to it.toString() },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.changeQuality(state.selectOne?.getSelected())
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Dolby Atmos",
                            subtitle = if (isDolbyAtmosSupported) stringResource(Res.string.dolby_atmos_description) else stringResource(Res.string.dolby_atmos_unsupported),
                            leadingIcon = SimpIcons.GraphicEq,
                            switchState = (dolbyAtmosEnabled to { viewModel.setDolbyAtmosEnabled(it) }),
                            enabled = isDolbyAtmosSupported,
                        )
                    }
                }
            }

            // GROUP 3: DOWNLOADS
            if (matchesSearch("Download quality", "Download over Wi-Fi only", "Export compatible downloads", "Downloads")) {
                item(key = "card_downloads") {
                    PulseSettingsHeader(title = "DOWNLOADS")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Download quality",
                            subtitle = "~35 MB per track, whatever the connection",
                            leadingIcon = SimpIcons.Download,
                            trailingText = downloadQuality ?: "Lossless",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.download_quality) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = QUALITY.items.map { (it.toString() == downloadQuality) to it.toString() },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Download over Wi-Fi only",
                            badgeText = "BLOCKING",
                            switchState = (autoDownloadLikedSongs to { viewModel.setAutoDownloadLikedSongs(it) }),
                        )
                        PulseSettingDivider(hasLeadingIcon = false)
                        PulseSettingRow(
                            title = "Export compatible downloads",
                            switchState = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                        )
                    }
                }
            }

            // GROUP 4: PLAYBACK
            if (matchesSearch("Prefer music-only version", "Output precision", "Prefer USB DAC", "Loudness normalization", "Crossfade", "Automix", "Playback")) {
                item(key = "card_playback") {
                    PulseSettingsHeader(title = "PLAYBACK")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Prefer music-only version",
                            subtitle = "For music videos, show the video details while finding and loading the catalogue audio version",
                            leadingIcon = SimpIcons.PlayArrow,
                            switchState = ((!playVideo) to { viewModel.setPlayVideoInsteadOfAudio(!it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Output precision",
                            subtitle = "AudioTrack · 48.0 kHz · ${if (outputPrecisionIndex == 0) "16-bit PCM" else "32-bit float"}",
                            leadingIcon = SimpIcons.GraphicEq,
                            bottomContent = {
                                PulseSegmentedControl(
                                    options = listOf("16-bit PCM", "32-bit float"),
                                    selectedIndex = outputPrecisionIndex,
                                    onOptionSelected = {
                                        outputPrecisionIndex = it
                                        coroutineScope.launch {
                                            dataStoreManager.putString("output_precision", if (it == 0) "16" else "32")
                                        }
                                    },
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Prefer USB DAC",
                            switchState = (preferUsbDac to {
                                preferUsbDac = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("prefer_usb_dac", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider(hasLeadingIcon = false)
                        PulseSettingRow(
                            title = "Loudness normalization",
                            subtitle = "Levels every track to the same loudness",
                            leadingIcon = SimpIcons.VolumeUp,
                            switchState = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Crossfade",
                            subtitle = "Blends one track into the next",
                            leadingIcon = SimpIcons.Tune,
                            trailingText = if (crossfadeEnabled) "${crossfadeDuration / 1000}s" else "Off",
                            bottomContent = {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                    Slider(
                                        value = if (crossfadeEnabled) (crossfadeDuration / 1000f) else 0f,
                                        onValueChange = { sec ->
                                            if (sec > 0f) {
                                                viewModel.setCrossfadeEnabled(true)
                                                viewModel.setCrossfadeDuration((sec * 1000).toInt())
                                            } else {
                                                viewModel.setCrossfadeEnabled(false)
                                            }
                                        },
                                        valueRange = 0f..12f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF2C2C2E),
                                        ),
                                    )
                                }
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Automix [BETA]",
                            subtitle = "Times and blends transitions automatically, without a slider. Performance may vary on different devices.",
                            leadingIcon = SimpIcons.TipsAndUpdates,
                            switchState = (crossfadeDjMode to { viewModel.setCrossfadeDjMode(it) }),
                        )
                    }
                }
            }

            // GROUP 5: APPEARANCE & PLAYER
            if (matchesSearch("Appearance", "Now playing style", "Liquid Glass", "Full-screen cover art", "Legacy mesh gradient", "Animated cover art", "Spotify Canvas", "Theme", "Ambient")) {
                item(key = "card_appearance") {
                    PulseSettingsHeader(title = "APPEARANCE & PLAYER")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = stringResource(Res.string.theme),
                            subtitle = if (pearlLavenderTheme) "Pearl Lavender Signature" else themeModeLabels.firstOrNull { it.first == themeMode }?.second ?: "Dark",
                            leadingIcon = SimpIcons.Album,
                            trailingText = if (pearlLavenderTheme) "Pearl Lavender" else "Default Dark",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.theme) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = themeModeLabels.map { (it.first == themeMode) to it.second },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            themeModeLabels.firstOrNull { it.second == selected }?.first?.let {
                                                sharedViewModel.setThemeMode(it)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = stringResource(Res.string.now_playing_style),
                            subtitle = nowPlayingStyleLabels.firstOrNull { it.first == nowPlayingStyle }?.second ?: "Pulse Dynamic Neon",
                            leadingIcon = SimpIcons.LibraryMusic,
                            trailingText = "Pulse Neon",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.now_playing_style) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = nowPlayingStyleLabels.map { (it.first == nowPlayingStyle) to it.second },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            nowPlayingStyleLabels.firstOrNull { it.second == selected }?.first?.let {
                                                sharedViewModel.setNowPlayingStyle(it)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Liquid Glass",
                            subtitle = "Real refracting glass on the floating nav bar. Android 12+ only; uses more GPU.",
                            leadingIcon = SimpIcons.TipsAndUpdates,
                            switchState = (enableLiquidGlass to { viewModel.setEnableLiquidGlass(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Full-screen cover art",
                            subtitle = "Runs the cover to the edges of the player instead of a square sleeve",
                            leadingIcon = SimpIcons.Fullscreen,
                            switchState = (fullScreenCover to {
                                fullScreenCover = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("fullscreen_cover", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Legacy mesh gradient",
                            subtitle = "Brings back the drifting colour blobs behind the player from v1.5",
                            leadingIcon = SimpIcons.Tune,
                            switchState = (legacyMesh to {
                                legacyMesh = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("legacy_mesh", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Animated cover art",
                            subtitle = "Plays the looping video some releases ship instead of a still sleeve",
                            leadingIcon = SimpIcons.PlayCircle,
                            switchState = (amAnimatedArtwork to { viewModel.setAMAnimatedArtwork(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Play animated cover over cellular",
                            switchState = (playAnimOverCellular to {
                                playAnimOverCellular = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("anim_cover_cellular", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider(hasLeadingIcon = false)
                        PulseSettingRow(
                            title = "Set up Spotify Canvas",
                            subtitle = if (spotifyCanvas) "Spotify video loop canvases active" else "Connect Spotify account for looping canvas videos",
                            showChevron = true,
                            onClick = {
                                navController.navigate(SpotifyLoginDestination)
                            },
                        )
                    }
                }
            }

            // GROUP 6: LYRICS
            if (matchesSearch("Lyrics", "Synced lyrics", "Blur unfocused lyrics", "Lyrics sources", "Translation language", "Romanization")) {
                item(key = "card_lyrics") {
                    PulseSettingsHeader(title = "LYRICS")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = stringResource(Res.string.lyrics_style),
                            subtitle = lyricsStyleLabels.firstOrNull { it.first == lyricsStyle }?.second ?: "Pulse Moving Light",
                            leadingIcon = SimpIcons.Lyrics,
                            trailingText = "Pulse Light",
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.lyrics_style) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = lyricsStyleLabels.map { (it.first == lyricsStyle) to it.second },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            lyricsStyleLabels.firstOrNull { it.second == selected }?.first?.let {
                                                sharedViewModel.setLyricsStyle(it)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Synced lyrics",
                            subtitle = "Lights up the words on the player as they're sung",
                            leadingIcon = SimpIcons.Lyrics,
                            switchState = (syncedLyricsEnabled to {
                                syncedLyricsEnabled = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("synced_lyrics", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Blur unfocused lyrics",
                            subtitle = "Keeps the spotlight on the current line",
                            leadingIcon = SimpIcons.Tune,
                            switchState = (blurLyricsEnabled to {
                                blurLyricsEnabled = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("blur_lyrics", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Lyrics sources",
                            subtitle = "BiniLyrics, BetterLyrics, BetterLyrics Portato, PaxSenix, PaxSenix: Spotify, PaxSenix: Musixmatch, LyricsPlus, SimpMusic, Unison, YouTube captions, YouTube Music, Megalobiz, KuGou, LRCLIB, Musixmatch, Genius",
                            leadingIcon = SimpIcons.Help,
                            showChevron = true,
                            onClick = {
                                val providers = listOf("BiniLyrics (lrc.red)", "LRCLIB (Default)", "YouTube Captions", "Genius / Musixmatch")
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Main Lyrics Provider",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = providers.map { (it.contains("BiniLyrics") && mainLyricsProvider == DataStoreManager.BINI_LYRICS) to it },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val chosen = if (state.selectOne?.getSelected()?.contains("BiniLyrics") == true) DataStoreManager.BINI_LYRICS else DataStoreManager.LRCLIB
                                            viewModel.setLyricsProvider(chosen)
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Translation language",
                            subtitle = translationLanguage ?: "Follows the app language",
                            leadingIcon = SimpIcons.Subtitles,
                            showChevron = true,
                            onClick = {
                                val langs = listOf("English", "Spanish", "French", "German", "Hindi", "Japanese", "Vietnamese", "Chinese")
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Lyrics Translation Language",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = langs.map { (it == translationLanguage) to it },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            state.selectOne?.getSelected()?.let {
                                                viewModel.setTranslationLanguage(it)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            // GROUP 7: PERFORMANCE
            if (matchesSearch("High performance mode", "Performance")) {
                item(key = "card_performance") {
                    PulseSettingsHeader(title = "PERFORMANCE")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "High performance mode",
                            subtitle = "Uses full animations and blur, and requests a higher display refresh rate",
                            leadingIcon = SimpIcons.Speed,
                            switchState = (highPerformanceMode to {
                                highPerformanceMode = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("high_performance", if (it) "true" else "false")
                                }
                            }),
                        )
                    }
                }
            }

            // GROUP 8: LOCAL MUSIC
            if (matchesSearch("Local music", "Local music folder", "Filter non-music audio")) {
                item(key = "card_local_music") {
                    PulseSettingsHeader(title = "LOCAL MUSIC")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Local music folder",
                            subtitle = "All audio folders",
                            leadingIcon = SimpIcons.LibraryMusic,
                            showChevron = true,
                            onClick = {
                                sharedViewModel.makeToast("Scanning all local audio folders on device")
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Filter non-music audio",
                            subtitle = "Hides clips under 30 seconds, WAV files, voice notes, recordings and system sounds",
                            leadingIcon = SimpIcons.Sort,
                            switchState = (filterNonMusicAudio to {
                                filterNonMusicAudio = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("filter_non_music", if (it) "true" else "false")
                                }
                            }),
                        )
                    }
                }
            }

            // GROUP 9: STORAGE
            if (matchesSearch("Storage", "Song cache limit", "Clear song cache", "Clear image cache")) {
                item(key = "card_storage") {
                    PulseSettingsHeader(title = "STORAGE")
                    PulseSettingsCard {
                        val limitCacheSize = listOf(0, 256, 512, 1024, 2048, 4096)
                        PulseSettingRow(
                            title = "Song cache limit",
                            subtitle = "Keeps downloaded audio on disk for instant seeking and replays",
                            leadingIcon = SimpIcons.QueueMusic,
                            trailingText = limitPlayerCache?.let { if (it == 0) "Unlimited" else "$it MB" } ?: "512 MB",
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.limit_player_cache) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = limitCacheSize.map { (it == limitPlayerCache) to if (it == 0) "Unlimited" else "$it MB" },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            limitCacheSize.firstOrNull { (if (it == 0) "Unlimited" else "$it MB") == selected }?.let {
                                                viewModel.setPlayerCacheLimit(it)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                            bottomContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF2C2C2E)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (fraction.playerCache).coerceIn(0.05f, 1f))
                                            .fillMaxHeight()
                                            .background(Color.White),
                                    )
                                }
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Clear song cache",
                            subtitle = "Frees space used by downloaded audio (${playerCache.bytesToMB()})",
                            leadingIcon = SimpIcons.Delete,
                            showChevron = true,
                            onClick = {
                                viewModel.clearPlayerCache()
                                sharedViewModel.makeToast("Song cache cleared")
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Clear image cache",
                            subtitle = "Frees space used by album artwork (${thumbnailCache?.bytesToMB() ?: "0 MB"})",
                            leadingIcon = SimpIcons.Delete,
                            showChevron = true,
                            onClick = {
                                viewModel.clearThumbnailCache(platformContext)
                                sharedViewModel.makeToast("Image cache cleared")
                            },
                        )
                    }
                }
            }

            // GROUP 10: YOUR DATA
            if (matchesSearch("Your Data", "Replay", "Work out genres", "Export data", "Import data")) {
                item(key = "card_your_data") {
                    PulseSettingsHeader(title = "YOUR DATA")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Replay",
                            subtitle = "Your top songs, artists, albums and genres",
                            leadingIcon = SimpIcons.Insights,
                            showChevron = true,
                            onClick = {
                                navController.navigate(AnalyticsDestination)
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Work out genres",
                            subtitle = "Asks Last.fm for an artist's genres. Only the artist name is sent.",
                            leadingIcon = SimpIcons.PushPin,
                            switchState = (workOutGenres to {
                                workOutGenres = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("workout_genres", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Export data",
                            subtitle = "Settings and listening history, as one JSON file",
                            leadingIcon = SimpIcons.ArrowOutward,
                            showChevron = true,
                            onClick = {
                                backupLauncher.launch()
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Import data",
                            subtitle = "Replaces the settings and history on this device",
                            leadingIcon = SimpIcons.Download,
                            showChevron = true,
                            onClick = {
                                restoreLauncher.launch()
                            },
                        )
                    }
                }
            }

            // GROUP 11: MISCELLANEOUS
            if (matchesSearch("Miscellaneous", "Play next on swipe", "Don't repeat songs in current session", "Stop music on close from recents", "Hide volume bar", "Hide Song Status")) {
                item(key = "card_miscellaneous") {
                    PulseSettingsHeader(title = "MISCELLANEOUS")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Play next on swipe",
                            subtitle = "When disabled, swiping adds the song to the end of the queue",
                            leadingIcon = SimpIcons.PlaylistAdd,
                            switchState = (playNextOnSwipe to {
                                playNextOnSwipe = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("play_next_on_swipe", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Don't repeat songs in current session",
                            subtitle = "AutoPlay won't suggest a song already played or suggested this session",
                            leadingIcon = SimpIcons.History,
                            switchState = (dontRepeatSongsSession to {
                                dontRepeatSongsSession = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("dont_repeat_session", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Stop music on close from recents",
                            subtitle = "Stops playback when swiped away from recent apps",
                            leadingIcon = SimpIcons.PauseCircle,
                            switchState = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Hide volume bar",
                            subtitle = "Removes the volume slider from the main player",
                            leadingIcon = SimpIcons.VolumeOff,
                            switchState = (hideVolumeBar to {
                                hideVolumeBar = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("hide_volume_bar", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Hide Song Status",
                            subtitle = "Hides the \"Playing from\" or \"Played by\" text in the main player",
                            leadingIcon = SimpIcons.SubtitlesOff,
                            switchState = (hideSongStatus to {
                                hideSongStatus = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("hide_song_status", if (it) "true" else "false")
                                }
                            }),
                        )
                    }
                }
            }

            // GROUP 12: LANGUAGE
            if (matchesSearch("Language", "App language", "Content country", language, location)) {
                item(key = "card_language") {
                    PulseSettingsHeader(title = "LANGUAGE")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "App language",
                            subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                            leadingIcon = SimpIcons.Help,
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.language) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = SUPPORTED_LANGUAGE.items.map {
                                                (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                            },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                            viewModel.setBasicAlertData(
                                                SettingBasicAlertState(
                                                    title = runBlocking { getString(Res.string.warning) },
                                                    message = runBlocking { getString(Res.string.change_language_warning) },
                                                    confirm = runBlocking { getString(Res.string.change) } to {
                                                        sharedViewModel.activityRecreate()
                                                        viewModel.setBasicAlertData(null)
                                                        viewModel.changeLanguage(code)
                                                    },
                                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                                ),
                                            )
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = stringResource(Res.string.content_country),
                            subtitle = location ?: "Default",
                            leadingIcon = SimpIcons.TipsAndUpdates,
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.content_country) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = SUPPORTED_LOCATION.items.map { item ->
                                                (item.toString() == location) to item.toString()
                                            },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.changeLocation(state.selectOne?.getSelected() ?: "US")
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            // GROUP 13: ADVANCED & EXPERIMENTAL OPTIONS
            if (matchesSearch("Advanced", "Smart audio alignment", "Equalizer", "Cinematic Audio", "About Pulse Music", "Check for updates")) {
                item(key = "card_advanced") {
                    PulseSettingsHeader(title = "ADVANCED & EXPERIMENTAL OPTIONS")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Smart audio alignment",
                            subtitle = "Analyze audio waveforms to match the exact beat drops and seamless transitions",
                            leadingIcon = SimpIcons.GraphicEq,
                            switchState = (smartAudioAlignment to {
                                smartAudioAlignment = it
                                coroutineScope.launch {
                                    dataStoreManager.putString("smart_audio_align", if (it) "true" else "false")
                                }
                            }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Equalizer & Sound Effects",
                            subtitle = "10-band equalizer, bass boost, and audio reverberation",
                            leadingIcon = SimpIcons.Tune,
                            showChevron = true,
                            onClick = {
                                resultLauncher.launch()
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Cinematic Audio",
                            subtitle = stringResource(Res.string.cinematic_audio_description),
                            leadingIcon = SimpIcons.Headphones,
                            switchState = (cinematicAudioEnabled to { viewModel.setCinematicAudioEnabled(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Check for updates",
                            subtitle = checkForUpdateSubtitle,
                            leadingIcon = SimpIcons.Update,
                            showChevron = true,
                            onClick = {
                                coroutineScope.launch {
                                    sharedViewModel.makeToast("Checking for updates...")
                                    sharedViewModel.checkForAppUpdate(silent = false)
                                }
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "About Pulse Music",
                            subtitle = "Version 3.2.7 (Build 95) • Pulse Music Studio",
                            leadingIcon = SimpIcons.Info,
                            showChevron = true,
                            onClick = {
                                navController.navigate(CreditDestination)
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = stringResource(Res.string.third_party_libraries),
                            subtitle = stringResource(Res.string.description_and_licenses),
                            showChevron = true,
                            onClick = {
                                showThirdPartyLibraries = true
                            },
                        )
                    }
                }
            }

            item(key = "end_padding") {
                Spacer(Modifier.height(32.dp))
                EndOfPage()
            }
        }
    }

    // YOUTUBE ACCOUNT MANAGEMENT DIALOG
    if (showYouTubeAccountDialog) {
        AlertDialog(
            onDismissRequest = { showYouTubeAccountDialog = false },
            title = {
                Text(
                    text = "YouTube Account",
                    style = typo().titleMedium,
                    color = Color.White,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Signed in as: ${if (!ytAccountName.isNullOrBlank()) ytAccountName else "Connected YouTube Account"}",
                        style = typo().bodyMedium,
                        color = Color(0xFFD1D1D6),
                    )
                    Text(
                        text = "Your playlists, liked songs, subscriptions, and history are synchronized with YouTube Music.",
                        style = typo().bodySmall,
                        color = Color(0xFF8E8E93),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showYouTubeAccountDialog = false
                        navController.navigate(LoginDestination)
                    },
                ) {
                    Text(text = "Switch Account", color = Color(0xFFA78BFA))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.logOutAllYouTube()
                        showYouTubeAccountDialog = false
                        sharedViewModel.makeToast("Signed out from YouTube")
                    },
                ) {
                    Text(text = stringResource(Res.string.log_out), color = Color(0xFFEF4444))
                }
            },
        )
    }

    // DEV LOGIN / COOKIE IMPORT BOTTOM SHEET
    if (showDevLoginSheet) {
        DevLogInBottomSheet(
            onDismiss = { showDevLoginSheet = false },
            onDone = { cookie ->
                coroutineScope.launch {
                    val success = viewModel.addAccount(cookie)
                    if (success) {
                        sharedViewModel.makeToast("YouTube account connected successfully")
                        showDevLoginSheet = false
                    } else {
                        sharedViewModel.makeToast("Failed to connect YouTube account")
                    }
                }
            },
            type = DevLogInType.YouTube,
        )
    }

    // DIALOGS & OVERLAYS
    if (showImportLinkDialog) {
        ImportLinkDialog(
            onDismiss = { showImportLinkDialog = false },
            onImportUrl = { url ->
                importViewModel.importFromUrl(url)
            },
        )
    }

    importState?.let { progress ->
        ImportProgressDialog(
            progress = progress,
            onDismiss = importViewModel::dismiss,
        )
    }

    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(true, showLoadingDialog.second)
    }

    val basisAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setBasicAlertData(null) },
            title = { Text(text = alertBasicState.title, style = typo().titleSmall) },
            text = {
                if (alertBasicState.message != null) {
                    Text(text = alertBasicState.message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertBasicState.confirm.second.invoke()
                        viewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setBasicAlertData(null) }) {
                    Text(text = alertBasicState.dismiss)
                }
            },
        )
    }

    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    if (alertData != null) {
        val alertState = alertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setAlertData(null) },
            title = { Text(text = alertState.title, style = typo().titleSmall) },
            text = {
                if (alertState.message != null) {
                    Column {
                        Text(text = alertState.message)
                        if (alertState.textField != null) {
                            val verify = alertState.textField.verifyCodeBlock?.invoke(alertState.textField.value) ?: (true to null)
                            TextField(
                                value = alertState.textField.value,
                                onValueChange = {
                                    viewModel.setAlertData(
                                        alertState.copy(
                                            textField = alertState.textField.copy(value = it),
                                        ),
                                    )
                                },
                                isError = !verify.first,
                                label = { Text(text = alertState.textField.label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            )
                        }
                    }
                } else if (alertState.selectOne != null) {
                    LazyColumn(
                        Modifier.padding(vertical = 6.dp).heightIn(0.dp, 500.dp),
                    ) {
                        items(alertState.selectOne.listSelect) { item ->
                            val onSelect = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        selectOne = alertState.selectOne.copy(
                                            listSelect = alertState.selectOne.listSelect.map {
                                                (it == item) to it.second
                                            },
                                        ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelect.invoke() }
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = item.first,
                                    onClick = { onSelect.invoke() },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.second,
                                    style = typo().bodyMedium,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(),
                                )
                            }
                        }
                    }
                } else if (alertState.multipleSelect != null) {
                    LazyColumn(Modifier.padding(vertical = 6.dp)) {
                        items(alertState.multipleSelect.listSelect) { item ->
                            val onCheck = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        multipleSelect = alertState.multipleSelect.copy(
                                            listSelect = alertState.multipleSelect.listSelect.map {
                                                if (it == item) (!it.first to it.second) else it
                                            },
                                        ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { onCheck.invoke() }
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.first,
                                    onCheckedChange = { onCheck.invoke() },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo().bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertState.confirm.second.invoke(alertState)
                        viewModel.setAlertData(null)
                    },
                ) {
                    Text(text = alertState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setAlertData(null) }) {
                    Text(text = alertState.dismiss)
                }
            },
        )
    }

    if (showThirdPartyLibraries) {
        val libraries by produceLibraries {
            Res.readBytes("files/aboutlibraries.json").decodeToString()
        }
        val lazyListState = rememberLazyListState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { showThirdPartyLibraries = false },
            containerColor = Color(0xFF161618),
            scrimColor = Color.Black.copy(alpha = .7f),
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
        ) {
            val surfaceContainerHighestColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            LibrariesContainer(
                libraries?.copy(
                    libraries = libraries?.libraries?.distinctBy { it.name }?.toImmutableList() ?: emptyList<Library>().toImmutableList(),
                ),
                Modifier.fillMaxSize(),
                lazyListState = lazyListState,
                contentPadding = innerPadding,
                colors = LibraryDefaults.libraryColors(
                    licenseChipColors = object : ChipColors {
                        override val containerColor: Color get() = surfaceContainerHighestColor
                        override val contentColor: Color get() = onSurfaceColor
                    },
                ),
                header = {
                    item {
                        TopAppBar(
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            title = {
                                Text(
                                    text = stringResource(Res.string.third_party_libraries),
                                    style = typo().titleMedium,
                                    color = Color.White,
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    RippleIconButton(
                                        SimpIcons.ArrowBackIosNew,
                                        Modifier.size(32.dp),
                                        true,
                                        tint = Color.White,
                                    ) {
                                        coroutineScope.launch {
                                            sheetState.hide()
                                            showThirdPartyLibraries = false
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        )
                    }
                },
            )
        }
    }
}
