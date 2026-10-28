package com.pulse.music.ui.screen.home
import androidx.compose.foundation.verticalScroll
import com.pulse.music.ui.navigation.destination.home.ListenTogetherDestination
import androidx.compose.foundation.rememberScrollState

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
import com.manikandan.domain.mediaservice.handler.HearingSafetyStatus
import com.manikandan.domain.repository.ImportProgress
import com.manikandan.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.pulse.music.Platform
import com.pulse.music.expect.ui.fileSaverResult
import com.pulse.music.expect.ui.isLyricsBlurSupported
import com.pulse.music.expect.ui.isWallpaperDynamicColorSupported
import com.pulse.music.expect.ui.openEqResult
import com.pulse.music.expect.ui.rememberIsDolbyAtmosSupported
import com.pulse.music.extension.bytesToMB
import com.pulse.music.extension.displayString
import com.pulse.music.extension.isTwoLetterCode
import com.pulse.music.extension.isValidProxyHost
import com.pulse.music.getPlatform
import com.pulse.music.ui.component.ActionButton
import com.pulse.music.ui.component.AmbientThemeGlow
import com.pulse.music.ui.component.CenterLoadingBox
import com.pulse.music.ui.component.DevLogInBottomSheet
import com.pulse.music.ui.component.DevLogInType
import com.pulse.music.ui.component.DiscoveryMixSlider
import com.pulse.music.ui.component.EndOfPage
import com.pulse.music.ui.component.ImportLinkDialog
import com.pulse.music.ui.component.ImportProgressDialog
import com.pulse.music.ui.component.LoadingDialog
import com.pulse.music.ui.component.PulseSegmentedControl
import com.pulse.music.ui.component.PulseSettingDivider
import com.pulse.music.ui.component.PulseSettingRow
import com.pulse.music.ui.component.PulseSettingsBadge
import com.pulse.music.ui.component.PulseSettingsCard
import com.pulse.music.ui.component.PulseSettingsHeader
import com.pulse.music.ui.component.PulseSettingsSearchBar
import com.pulse.music.ui.component.RippleIconButton
import com.pulse.music.ui.component.getSettingsThemeColors
import com.pulse.music.ui.component.rememberNowPlayingGlowTint
import com.pulse.music.ui.icon.Album
import com.pulse.music.ui.icon.ArrowBackIosNew
import com.pulse.music.ui.icon.ArrowForwardIos
import com.pulse.music.ui.icon.ArrowOutward
import com.pulse.music.ui.icon.AutoGraph
import com.pulse.music.ui.icon.Close
import com.pulse.music.ui.icon.Delete
import com.pulse.music.ui.icon.Download
import com.pulse.music.ui.icon.Edit
import com.pulse.music.ui.icon.Error
import com.pulse.music.ui.icon.FastForward
import com.pulse.music.ui.icon.Fullscreen
import com.pulse.music.ui.icon.GraphicEq
import com.pulse.music.ui.icon.Headphones
import com.pulse.music.ui.icon.Help
import com.pulse.music.ui.icon.History
import com.pulse.music.ui.icon.Info
import com.pulse.music.ui.icon.Insights
import com.pulse.music.ui.icon.LibraryMusic
import com.pulse.music.ui.icon.LogoDev
import com.pulse.music.ui.icon.Lyrics
import com.pulse.music.ui.icon.PauseCircle
import com.pulse.music.ui.icon.PeopleAlt
import com.pulse.music.ui.icon.PersonAdd
import com.pulse.music.ui.icon.PlayArrow
import com.pulse.music.ui.icon.PlayCircle
import com.pulse.music.ui.icon.PlaylistAdd
import com.pulse.music.ui.icon.PushPin
import com.pulse.music.ui.icon.QueueMusic
import com.pulse.music.ui.icon.Search
import com.pulse.music.ui.icon.Sensors
import com.pulse.music.ui.icon.Settings
import com.pulse.music.ui.icon.SimpIcons
import com.pulse.music.ui.icon.Sort
import com.pulse.music.ui.icon.Speed
import com.pulse.music.ui.icon.Subtitles
import com.pulse.music.ui.icon.SubtitlesOff
import com.pulse.music.ui.icon.Sync
import com.pulse.music.ui.icon.TipsAndUpdates
import com.pulse.music.ui.icon.Tune
import com.pulse.music.ui.icon.Update
import com.pulse.music.ui.icon.VolumeOff
import com.pulse.music.ui.icon.VolumeUp
import com.pulse.music.ui.navigation.destination.home.AnalyticsDestination
import com.pulse.music.ui.navigation.destination.home.CreditDestination
import com.pulse.music.ui.navigation.destination.login.DiscordLoginDestination
import com.pulse.music.ui.navigation.destination.login.LastfmLoginDestination
import com.pulse.music.ui.navigation.destination.login.LoginDestination
import com.pulse.music.ui.navigation.destination.login.SpotifyLoginDestination
import com.pulse.music.ui.theme.md_theme_dark_primary
import com.pulse.music.ui.theme.parseThemeColorHex
import com.pulse.music.ui.theme.typo
import com.pulse.music.utils.VersionManager
import com.pulse.music.viewModel.ImportViewModel
import com.pulse.music.viewModel.SettingAlertState
import com.pulse.music.viewModel.SettingBasicAlertState
import com.pulse.music.viewModel.SettingsViewModel
import com.pulse.music.viewModel.SharedViewModel
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

    val listeningHealthEnabled by viewModel.listeningHealthEnabled.collectAsStateWithLifecycle()
    val saferVolumeLimitEnabled by viewModel.saferVolumeLimitEnabled.collectAsStateWithLifecycle()
    val breakReminderMinutes by viewModel.breakReminderMinutes.collectAsStateWithLifecycle()
    val healthState by viewModel.listeningHealthState.collectAsStateWithLifecycle()
    val discoveryRatio by viewModel.discoveryRatio.collectAsStateWithLifecycle()

    val isCheckingUpdate by sharedViewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val dataStoreManager: DataStoreManager = koinInject()
    val ytAccountName by remember { dataStoreManager.getString("AccountName") }.collectAsStateWithLifecycle(initialValue = null)
    val ytCookie by remember { dataStoreManager.cookie }.collectAsStateWithLifecycle(initialValue = "")
    val profileName by dataStoreManager.profileName.collectAsStateWithLifecycle(initialValue = "")

    val isYouTubeSignedIn = (loggedInState == DataStoreManager.TRUE || !ytAccountName.isNullOrBlank() || ytCookie.isNotEmpty())

    // Local UI States (Screenshot Parity)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val outputPrecision by viewModel.outputPrecision.collectAsStateWithLifecycle()
    val preferUsbDac by viewModel.preferUsbDac.collectAsStateWithLifecycle()
    var fullScreenCover by rememberSaveable { mutableStateOf(true) }
    val legacyMesh by viewModel.legacyMesh.collectAsStateWithLifecycle()
    val playAnimOverCellular by viewModel.playAnimOverCellular.collectAsStateWithLifecycle()
    var syncedLyricsEnabled by rememberSaveable { mutableStateOf(true) }
    var blurLyricsEnabled by rememberSaveable { mutableStateOf(true) }
    val highPerformanceMode by viewModel.highPerformanceMode.collectAsStateWithLifecycle()
    var filterNonMusicAudio by rememberSaveable { mutableStateOf(true) }
    var workOutGenres by rememberSaveable { mutableStateOf(true) }
    val playNextOnSwipe by viewModel.playNextOnSwipe.collectAsStateWithLifecycle()
    val dontRepeatSongsSession by viewModel.dontRepeatSongsSession.collectAsStateWithLifecycle()
    val stopOnCloseRecents by viewModel.stopOnCloseRecents.collectAsStateWithLifecycle()
    val hideVolumeBar by viewModel.hideVolumeBar.collectAsStateWithLifecycle()
    val hideSongStatus by viewModel.hideSongStatus.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val dailyAnalyticsEnabled by viewModel.dailyAnalyticsEnabled.collectAsStateWithLifecycle()
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var smartAudioAlignment by rememberSaveable { mutableStateOf(true) }

    var showYouTubeAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showDevLoginSheet by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showThirdPartyLibraries by rememberSaveable { mutableStateOf(false) }
    var showAudioSourcesDialog by rememberSaveable { mutableStateOf(false) }
    var showLocalFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showSoundEffectsDialog by rememberSaveable { mutableStateOf(false) }

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
            "Version ${VersionManager.getVersionName()} Build ${VersionManager.getVersionCode()} · Pulse Music Studio"
        }
    }

    val requiresAndroid12 = " (" + stringResource(Res.string.requires_android_12) + ")"
    val themeModeLabels = listOf(
        DataStoreManager.THEME_MODE_SYSTEM to stringResource(Res.string.theme_mode_system),
        DataStoreManager.THEME_MODE_DARK to stringResource(Res.string.theme_mode_dark),
        DataStoreManager.THEME_MODE_LIGHT to stringResource(Res.string.theme_mode_light),
        "PEARL_LAVENDER" to "Pearl Lavender Signature",
    )
    val currentThemeKey = if (pearlLavenderTheme) "PEARL_LAVENDER" else themeMode
    val currentThemeSubtitle = when (currentThemeKey) {
        "PEARL_LAVENDER" -> "Pearl Lavender Signature"
        DataStoreManager.THEME_MODE_LIGHT -> stringResource(Res.string.theme_mode_light)
        DataStoreManager.THEME_MODE_SYSTEM -> stringResource(Res.string.theme_mode_system)
        else -> stringResource(Res.string.theme_mode_dark)
    }
    val currentThemeTrailing = when (currentThemeKey) {
        "PEARL_LAVENDER" -> "Pearl Lavender"
        DataStoreManager.THEME_MODE_LIGHT -> "Light"
        DataStoreManager.THEME_MODE_SYSTEM -> "System"
        else -> "Dark (AMOLED)"
    }
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

    val settingsColors = getSettingsThemeColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsColors.screenBackground),
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
                                tint = settingsColors.titleText,
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
                            color = settingsColors.titleText,
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
                                navController.navigate(ListenTogetherDestination)
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
                                showAudioSourcesDialog = true
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
                            subtitle = "AudioTrack · 48.0 kHz · ${if (outputPrecision == "32") "32-bit float" else "16-bit PCM"}",
                            leadingIcon = SimpIcons.GraphicEq,
                            bottomContent = {
                                PulseSegmentedControl(
                                    options = listOf("16-bit PCM", "32-bit float"),
                                    selectedIndex = if (outputPrecision == "32") 1 else 0,
                                    onOptionSelected = {
                                        viewModel.setOutputPrecision(if (it == 0) "16" else "32")
                                    },
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Prefer USB DAC",
                            subtitle = "Prioritize external USB DAC / audio interface when connected",
                            leadingIcon = SimpIcons.Headphones,
                            switchState = (preferUsbDac to { viewModel.setPreferUsbDac(it) }),
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
                        PulseSettingDivider()
                        val equalizerEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
                        PulseSettingRow(
                            title = "10-Band Equalizer",
                            subtitle = if (equalizerEnabled) "Hardware & DSP peaking biquad EQ enabled" else "Adjust 10 frequency bands and preamp",
                            leadingIcon = SimpIcons.GraphicEq,
                            switchState = (equalizerEnabled to { viewModel.setEqualizerEnabled(it) }),
                        )
                        AnimatedVisibility(visible = equalizerEnabled) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                EqualizerSection(viewModel)
                            }
                        }
                    }
                }
            }

            // GROUP: LISTEN HEALTHY & HEARING SAFETY
            if (matchesSearch("Listen Healthy", "Hearing Safety", "Ear Care", "Volume Limit", "Break Reminder", "Safe", "Health")) {
                item(key = "card_listen_healthy") {
                    PulseSettingsHeader(title = "LISTEN HEALTHY & EAR CARE")
                    PulseSettingsCard {
                        val safetyBadgeText = when (healthState.hearingSafetyStatus) {
                            HearingSafetyStatus.SAFE -> "SAFE"
                            HearingSafetyStatus.MODERATE -> "MODERATE"
                            HearingSafetyStatus.CAUTION -> "BREAK NEEDED"
                        }
                        val safetyBadgeColor = when (healthState.hearingSafetyStatus) {
                            HearingSafetyStatus.SAFE -> Color(0xFF10B981)
                            HearingSafetyStatus.MODERATE -> Color(0xFFF59E0B)
                            HearingSafetyStatus.CAUTION -> Color(0xFFEF4444)
                        }
                        PulseSettingRow(
                            title = "Listen Healthy Monitor",
                            subtitle = if (listeningHealthEnabled) "Actively monitoring continuous headphone exposure" else "Track headphone duration and prevent ear fatigue",
                            leadingIcon = SimpIcons.Headphones,
                            badgeText = if (listeningHealthEnabled) safetyBadgeText else null,
                            badgeBg = safetyBadgeColor,
                            switchState = (listeningHealthEnabled to { viewModel.setListeningHealthEnabled(it) }),
                        )
                        if (listeningHealthEnabled) {
                            PulseSettingDivider()
                            PulseSettingRow(
                                title = "Safe Volume Limiter (85dB Cap)",
                                subtitle = "Automatically caps headphone volume at 75% according to WHO hearing safety standards",
                                leadingIcon = SimpIcons.VolumeUp,
                                badgeText = if (saferVolumeLimitEnabled) "ACTIVE" else null,
                                switchState = (saferVolumeLimitEnabled to { viewModel.setSaferVolumeLimitEnabled(it) }),
                            )
                            PulseSettingDivider()
                            val reminderLabels = listOf(
                                0 to "Off",
                                30 to "Every 30 mins",
                                45 to "Every 45 mins",
                                60 to "Every 60 mins (Recommended)",
                                90 to "Every 90 mins",
                                120 to "Every 2 hours",
                            )
                            PulseSettingRow(
                                title = "Ear Fatigue Break Reminder",
                                subtitle = reminderLabels.firstOrNull { it.first == breakReminderMinutes }?.second ?: "${breakReminderMinutes} mins",
                                leadingIcon = SimpIcons.TipsAndUpdates,
                                trailingText = if (breakReminderMinutes > 0) "${breakReminderMinutes}m" else "Off",
                                showChevron = true,
                                onClick = {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = "Ear Fatigue Break Reminder",
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = reminderLabels.map { (it.first == breakReminderMinutes) to it.second },
                                            ),
                                            confirm = runBlocking { getString(Res.string.change) } to { state ->
                                                val selected = state.selectOne?.getSelected()
                                                val selectedMin = reminderLabels.firstOrNull { it.second == selected }?.first ?: 60
                                                viewModel.setBreakReminderMinutes(selectedMin)
                                            },
                                            dismiss = runBlocking { getString(Res.string.cancel) },
                                        ),
                                    )
                                },
                            )
                            PulseSettingDivider()
                            PulseSettingRow(
                                title = "Session Listening Duration",
                                subtitle = if (healthState.isHeadphonesActive) {
                                    "Headphones active • Session: ${healthState.sessionMinutes} mins • Volume: ${(healthState.currentVolumeRatio * 100).toInt()}%"
                                } else {
                                    "Speaker output • Session: ${healthState.sessionMinutes} mins"
                                },
                                leadingIcon = SimpIcons.AutoGraph,
                                trailingText = "${healthState.sessionMinutes}m",
                            )
                        }
                    }
                }
            }

            // GROUP: SONG DISCOVERY & SMART RADIO
            if (matchesSearch("Discovery", "Song Discovery", "Explore", "Familiar", "Radio", "Smart Mix", "Discover")) {
                item(key = "card_discovery") {
                    PulseSettingsHeader(title = "SONG DISCOVERY & SMART RADIO")
                    PulseSettingsCard {
                        val discoveryPercentage = (discoveryRatio * 100).toInt()
                        PulseSettingRow(
                            title = "Smart Mix Ratio",
                            subtitle = "Balance familiar favorites vs. brand-new music discoveries when playing radio or autoplay",
                            leadingIcon = SimpIcons.Insights,
                            trailingText = "$discoveryPercentage% Discover",
                            bottomContent = {
                                DiscoveryMixSlider(
                                    ratio = discoveryRatio,
                                    onRatioChange = { viewModel.setDiscoveryRatio(it) },
                                    containerColor = Color.Transparent,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            },
                        )
                    }
                }
            }

            // GROUP 5: APPEARANCE & PLAYER
            if (matchesSearch("Appearance", "Now playing style", "Liquid Glass", "Full-screen cover art", "Legacy mesh gradient", "Animated cover art", "Spotify Canvas", "Theme", "Ambient", "Lavender", "Accent")) {
                item(key = "card_appearance") {
                    PulseSettingsHeader(title = "APPEARANCE & PLAYER")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = stringResource(Res.string.theme),
                            subtitle = currentThemeSubtitle,
                            leadingIcon = SimpIcons.Album,
                            trailingText = currentThemeTrailing,
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.theme) },
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = themeModeLabels.map { (it.first == currentThemeKey) to it.second },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            val selectedKey = themeModeLabels.firstOrNull { it.second == selected }?.first
                                            if (selectedKey == "PEARL_LAVENDER") {
                                                viewModel.setPearlLavenderTheme(true)
                                                sharedViewModel.setPearlLavenderTheme(true)
                                            } else if (selectedKey != null) {
                                                viewModel.setPearlLavenderTheme(false)
                                                sharedViewModel.setPearlLavenderTheme(false)
                                                sharedViewModel.setThemeMode(selectedKey)
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Pearl Lavender Theme",
                            subtitle = "Soft pastel lavender palette with coral accents (Light & Dark)",
                            leadingIcon = SimpIcons.Tune,
                            switchState = (pearlLavenderTheme to { enabled ->
                                viewModel.setPearlLavenderTheme(enabled)
                                sharedViewModel.setPearlLavenderTheme(enabled)
                            }),
                        )
                        PulseSettingDivider()
                        val themeColorSourceLabels = listOf(
                            DataStoreManager.THEME_COLOR_DEFAULT to "Pulse Violet (Default)",
                            DataStoreManager.THEME_COLOR_WALLPAPER to "Dynamic Wallpaper (Material You)",
                            DataStoreManager.THEME_COLOR_CUSTOM to "Custom Accent Color",
                        )
                        PulseSettingRow(
                            title = "Accent Color",
                            subtitle = themeColorSourceLabels.firstOrNull { it.first == themeColorSource }?.second ?: "Pulse Violet",
                            leadingIcon = SimpIcons.Tune,
                            trailingText = if (themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM) "#${customThemeColorHex.takeLast(6).uppercase()}" else null,
                            showChevron = true,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Accent Color",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = themeColorSourceLabels.map { (it.first == themeColorSource) to it.second },
                                        ),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            themeColorSourceLabels.firstOrNull { it.second == selected }?.first?.let { source ->
                                                sharedViewModel.setThemeColorSource(source)
                                                if (source == DataStoreManager.THEME_COLOR_CUSTOM) {
                                                    showColorPickerDialog = true
                                                }
                                            }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        if (themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM) {
                            PulseSettingDivider()
                            PulseSettingRow(
                                title = "Customize Accent Color",
                                subtitle = "Current: #${customThemeColorHex.takeLast(6).uppercase()}",
                                leadingIcon = SimpIcons.Tune,
                                trailingText = "Pick Color",
                                showChevron = true,
                                onClick = {
                                    showColorPickerDialog = true
                                },
                            )
                        }
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
                            switchState = (legacyMesh to { viewModel.setLegacyMesh(it) }),
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
                            subtitle = "Stream canvas video loops when connected to mobile data",
                            leadingIcon = SimpIcons.Sensors,
                            switchState = (playAnimOverCellular to { viewModel.setPlayAnimOverCellular(it) }),
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
                            subtitle = "Disables heavy background blurs and GPU load for maximum smoothness and battery life",
                            leadingIcon = SimpIcons.Speed,
                            switchState = (highPerformanceMode to { viewModel.setHighPerformanceMode(it) }),
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
                                showLocalFolderDialog = true
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
            if (matchesSearch("Your Data", "Replay", "Daily Analytics", "Import playlist", "Work out genres", "Export data", "Import data")) {
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
                            title = "Daily Analytics",
                            subtitle = "Track daily listening habits, stats, and personalized daily insights",
                            leadingIcon = SimpIcons.GraphicEq,
                            switchState = (dailyAnalyticsEnabled to { viewModel.setDailyAnalyticsEnabled(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Import playlist from other streaming apps",
                            subtitle = "Import playlists from Spotify, Apple Music, YouTube Music, or M3U",
                            leadingIcon = SimpIcons.Download,
                            showChevron = true,
                            onClick = {
                                showImportLinkDialog = true
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
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Anonymous analytics & crash reports",
                            subtitle = "Share anonymous usage statistics and diagnostics to help improve app reliability",
                            leadingIcon = SimpIcons.Insights,
                            switchState = (analyticsEnabled to { viewModel.setAnalyticsEnabled(it) }),
                        )
                    }
                }
            }

            // GROUP 11: MISCELLANEOUS
            if (matchesSearch("Miscellaneous", "Play next on swipe", "Don't repeat songs in current session", "Stop music on close from recents", "Hide volume bar", "Hide Song Status", "Analytics")) {
                item(key = "card_miscellaneous") {
                    PulseSettingsHeader(title = "MISCELLANEOUS")
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Play next on swipe",
                            subtitle = "When disabled, swiping adds the song to the end of the queue",
                            leadingIcon = SimpIcons.PlaylistAdd,
                            switchState = (playNextOnSwipe to { viewModel.setPlayNextOnSwipe(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Don't repeat songs in current session",
                            subtitle = "AutoPlay won't suggest a song already played or suggested this session",
                            leadingIcon = SimpIcons.History,
                            switchState = (dontRepeatSongsSession to { viewModel.setDontRepeatSongsSession(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Stop music on close from recents",
                            subtitle = "Stops playback when swiped away from recent apps",
                            leadingIcon = SimpIcons.PauseCircle,
                            switchState = (stopOnCloseRecents to { viewModel.setStopOnCloseRecents(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Hide volume bar",
                            subtitle = "Removes the volume slider from the main player",
                            leadingIcon = SimpIcons.VolumeOff,
                            switchState = (hideVolumeBar to { viewModel.setHideVolumeBar(it) }),
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Hide Song Status",
                            subtitle = "Hides the \"Playing from\" or \"Played by\" text in the main player",
                            leadingIcon = SimpIcons.SubtitlesOff,
                            switchState = (hideSongStatus to { viewModel.setHideSongStatus(it) }),
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
                                showSoundEffectsDialog = true
                            },
                        )
                        PulseSettingDivider()
                        PulseSettingRow(
                            title = "Cinematic Audio (8D Audio)",
                            subtitle = stringResource(Res.string.cinematic_audio_description),
                            leadingIcon = SimpIcons.Headphones,
                            switchState = (cinematicAudioEnabled to { viewModel.setCinematicAudioEnabled(it) }),
                        )
                        AnimatedVisibility(visible = cinematicAudioEnabled) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                                CinematicAudioSection(viewModel)
                            }
                        }
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
                            subtitle = "Version ${VersionManager.getVersionName()} (Build ${VersionManager.getVersionCode()}) • Pulse Music Studio",
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
                Spacer(Modifier.height(36.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Designed & Developed by Manikandan",
                        style = typo().bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = settingsColors.iconTint,
                            letterSpacing = 0.2.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pulse Music Studio • Version ${VersionManager.getVersionName()} (Build ${VersionManager.getVersionCode()})",
                        style = typo().bodySmall.copy(
                            color = settingsColors.subtitleText,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
                EndOfPage(withoutCredit = true)
            }
        }
    }

    // YOUTUBE ACCOUNT MANAGEMENT DIALOG
    if (showYouTubeAccountDialog) {
        AlertDialog(
            onDismissRequest = { showYouTubeAccountDialog = false },
            containerColor = settingsColors.cardBackground,
            titleContentColor = settingsColors.titleText,
            textContentColor = settingsColors.subtitleText,
            title = {
                Text(
                    text = "YouTube Account",
                    style = typo().titleMedium,
                    color = settingsColors.titleText,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Signed in as: ${if (!ytAccountName.isNullOrBlank()) ytAccountName else "Connected YouTube Account"}",
                        style = typo().bodyMedium,
                        color = settingsColors.titleText,
                    )
                    Text(
                        text = "Your playlists, liked songs, subscriptions, and history are synchronized with YouTube Music.",
                        style = typo().bodySmall,
                        color = settingsColors.subtitleText,
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
                    Text(text = "Switch Account", color = Color.White)
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

    if (showColorPickerDialog) {
        val presetColors =
            listOf(
                "FF8ECAE6",
                "FF4C82EF",
                "FF9B72CF",
                "FFEF6C9B",
                "FFEF5350",
                "FFF4A340",
                "FFFFCA28",
                "FF66BB6A",
                "FF26A69A",
                "FFBDBDBD",
            )
        var pendingHex by rememberSaveable { mutableStateOf(customThemeColorHex.takeLast(6)) }
        val parsedColor = parseThemeColorHex(pendingHex)
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text(text = "Accent Color", style = typo().titleSmall) },
            text = {
                Column {
                    presetColors.chunked(5).forEach { rowColors ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            rowColors.forEach { hex ->
                                val color = parseThemeColorHex(hex) ?: Color.Gray
                                val isSelected = pendingHex.equals(hex.takeLast(6), ignoreCase = true)
                                Box(
                                    modifier =
                                        Modifier
                                            .padding(4.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape,
                                            ).clickable { pendingHex = hex.takeLast(6) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = pendingHex,
                        onValueChange = { pendingHex = it.take(8) },
                        label = { Text("Hex Color Code") },
                        singleLine = true,
                        isError = parsedColor == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsedColor != null,
                    onClick = {
                        parsedColor?.let {
                            val argb = "FF${pendingHex.takeLast(6).uppercase()}"
                            sharedViewModel.setCustomThemeColor(argb)
                            sharedViewModel.setThemeColorSource(DataStoreManager.THEME_COLOR_CUSTOM)
                        }
                        showColorPickerDialog = false
                    },
                ) { Text(text = stringResource(Res.string.change)) }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
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

    // AUDIO SOURCES DIALOG
    if (showAudioSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showAudioSourcesDialog = false },
            containerColor = settingsColors.cardBackground,
            titleContentColor = settingsColors.titleText,
            textContentColor = settingsColors.subtitleText,
            title = {
                Text(
                    text = "Audio Sources & Priority",
                    style = typo().titleMedium,
                    color = settingsColors.titleText,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pulse Music resolves audio streams in the following priority order for optimal fidelity and zero buffering:",
                        style = typo().bodySmall,
                        color = settingsColors.subtitleText,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(SimpIcons.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("1. YouTube Music HQ (Primary)", style = typo().bodyMedium, color = settingsColors.titleText, fontWeight = FontWeight.SemiBold)
                            Text("256kbps OPUS / AAC lossless stream cache", style = typo().bodySmall, color = settingsColors.subtitleText)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(SimpIcons.LibraryMusic, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("2. Local Device Storage (Fallback)", style = typo().bodyMedium, color = settingsColors.titleText, fontWeight = FontWeight.SemiBold)
                            Text("Device MP3 / FLAC / M4A offline library", style = typo().bodySmall, color = settingsColors.subtitleText)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(SimpIcons.Album, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("3. Spotify Integration", style = typo().bodyMedium, color = settingsColors.titleText, fontWeight = FontWeight.SemiBold)
                            Text("Canvas looping videos & synched lyrics", style = typo().bodySmall, color = settingsColors.subtitleText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioSourcesDialog = false }) {
                    Text("Done", color = Color.White)
                }
            },
        )
    }

    // LOCAL FOLDER SELECTION DIALOG
    if (showLocalFolderDialog) {
        AlertDialog(
            onDismissRequest = { showLocalFolderDialog = false },
            containerColor = settingsColors.cardBackground,
            titleContentColor = settingsColors.titleText,
            textContentColor = settingsColors.subtitleText,
            title = {
                Text(
                    text = "Local Music Folders",
                    style = typo().titleMedium,
                    color = settingsColors.titleText,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pulse Music automatically indexes all supported audio formats (.flac, .mp3, .m4a, .wav, .opus) across your storage folders:",
                        style = typo().bodySmall,
                        color = settingsColors.subtitleText,
                    )
                    Text("• /Music", style = typo().bodyMedium, color = settingsColors.titleText)
                    Text("• /Download", style = typo().bodyMedium, color = settingsColors.titleText)
                    Text("• /Audio & Podcasts", style = typo().bodyMedium, color = settingsColors.titleText)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocalFolderDialog = false
                        sharedViewModel.makeToast("Scanning device storage for local tracks...")
                    },
                ) {
                    Text("Rescan Storage", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalFolderDialog = false }) {
                    Text("Close", color = settingsColors.subtitleText)
                }
            },
        )
    }

    // EQUALIZER & SOUND EFFECTS DIALOG
    if (showSoundEffectsDialog) {
        val eqEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showSoundEffectsDialog = false },
            containerColor = settingsColors.cardBackground,
            titleContentColor = settingsColors.titleText,
            textContentColor = settingsColors.subtitleText,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Equalizer & Sound Effects",
                        style = typo().titleMedium,
                        color = settingsColors.titleText,
                    )
                    IconButton(onClick = { showSoundEffectsDialog = false }) {
                        Icon(SimpIcons.Close, contentDescription = "Close", tint = settingsColors.titleText)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "10-Band Equalizer",
                            subtitle = if (eqEnabled) "Peaking biquad EQ enabled (31Hz - 16kHz)" else "Tap switch to enable EQ curve",
                            leadingIcon = SimpIcons.GraphicEq,
                            switchState = (eqEnabled to { viewModel.setEqualizerEnabled(it) }),
                        )
                        if (eqEnabled) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                                EqualizerSection(viewModel)
                            }
                        }
                    }

                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Open System Equalizer",
                            subtitle = "Launch device vendor DSP (Samsung SoundAlive, Dolby, etc.)",
                            leadingIcon = SimpIcons.Tune,
                            showChevron = true,
                            onClick = {
                                resultLauncher.launch()
                            },
                        )
                    }

                    PulseSettingsCard {
                        PulseSettingRow(
                            title = "Cinematic Audio (8D Audio)",
                            subtitle = stringResource(Res.string.cinematic_audio_description),
                            leadingIcon = SimpIcons.Headphones,
                            switchState = (cinematicAudioEnabled to { viewModel.setCinematicAudioEnabled(it) }),
                        )
                        AnimatedVisibility(visible = cinematicAudioEnabled) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                                CinematicAudioSection(viewModel)
                            }
                        }
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
            },
            confirmButton = {
                TextButton(onClick = { showSoundEffectsDialog = false }) {
                    Text("Done", color = Color.White)
                }
            },
        )
    }

}
