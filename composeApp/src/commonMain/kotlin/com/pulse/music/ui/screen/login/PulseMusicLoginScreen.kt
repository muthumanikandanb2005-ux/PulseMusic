package com.pulse.music.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.SupabaseAuthRepository
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.pulse.music.ui.screen.profile.GENDER_OPTIONS
import com.pulse.music.ui.screen.profile.INDIAN_LANGUAGES
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val PulseNeonGreen = Color(0xFF00E676)
private val PulseOledBlack = Color(0xFF000000)
private val PulseFieldSurface = Color(0xFF101014)
private val PulseCardSurface = Color(0xFF0C130E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PulseMusicLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit = {},
    showBottomNavigation: () -> Unit = {},
    supabaseAuthRepository: SupabaseAuthRepository = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
    songRepository: SongRepository = koinInject(),
    localPlaylistRepository: LocalPlaylistRepository = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val isLoggedIn by supabaseAuthRepository.isLoggedIn.collectAsStateWithLifecycle(false)
    val userEmail by supabaseAuthRepository.currentUserEmail.collectAsStateWithLifecycle(null)

    val savedProfileName by dataStoreManager.profileName.collectAsStateWithLifecycle("")
    val savedProfileAge by dataStoreManager.profileAge.collectAsStateWithLifecycle("")
    val savedProfileGender by dataStoreManager.profileGender.collectAsStateWithLifecycle("")
    val savedProfileLanguage by dataStoreManager.profileLanguagePreference.collectAsStateWithLifecycle("")

    // User library flows
    val likedSongs by songRepository.getLikedSongs().collectAsStateWithLifecycle(emptyList())
    val userPlaylists by localPlaylistRepository.getAllLocalPlaylists().collectAsStateWithLifecycle(emptyList())
    var recentSongs by remember { mutableStateOf<List<SongEntity>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }

    // State determining whether to show ONLY Details vs Login/Signup form
    var hasJustSignedUpOrIn by remember { mutableStateOf(false) }
    val isUserActive = (isLoggedIn && !userEmail.isNullOrBlank()) || savedProfileName.isNotBlank() || !userEmail.isNullOrBlank() || hasJustSignedUpOrIn

    var isSignUpMode by remember { mutableStateOf(false) }

    // User Profile Form State
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var languagePreference by remember { mutableStateOf("Tamil (தமிழ்)") }

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }

    // Auth Form State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Populate profile inputs once loaded
    LaunchedEffect(savedProfileName, savedProfileAge, savedProfileGender, savedProfileLanguage) {
        if (name.isEmpty() && savedProfileName.isNotEmpty()) name = savedProfileName
        if (age.isEmpty() && savedProfileAge.isNotEmpty()) age = savedProfileAge
        if (savedProfileGender.isNotEmpty()) gender = savedProfileGender
        if (savedProfileLanguage.isNotEmpty()) languagePreference = savedProfileLanguage
    }

    // Load recent songs
    LaunchedEffect(isLoggedIn, userEmail, savedProfileName) {
        try {
            recentSongs = songRepository.getRecentSong(limit = 10, offset = 0)
        } catch (_: Exception) {
            // ignore
        }
    }

    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isUserActive) "Pulse Music Profile & Library" else "Account & Profile Setup",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            SimpIcons.ArrowBackIosNew,
                            Modifier.size(32.dp),
                            true,
                        ) {
                            coroutineScope.launch {
                                dataStoreManager.setHasSeenLoginPrompt(true)
                            }
                            navController.navigateUp()
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PulseOledBlack,
                ),
            )
        },
        containerColor = PulseOledBlack,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                if (isUserActive) {
                    // ==========================================
                    // 1. INTEGRATED LOGGED-IN PROFILE & LIBRARY DETAILS ONLY
                    // ==========================================

                    val displayName = when {
                        name.isNotBlank() -> name
                        savedProfileName.isNotBlank() -> savedProfileName
                        !userEmail.isNullOrBlank() -> userEmail!!.substringBefore("@")
                        else -> "Pulse Listener"
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .border(2.dp, PulseNeonGreen, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseNeonGreen,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName,
                        style = typo().titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    val displayEmail = userEmail ?: "pulse.listener@local"
                    Text(
                        text = displayEmail,
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Language Preference & Age Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF14241B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PulseNeonGreen.copy(alpha = 0.6f)),
                        ) {
                            Text(
                                text = if (languagePreference.isNotBlank()) languagePreference else "Tamil (தமிழ்)",
                                style = typo().labelSmall,
                                color = PulseNeonGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }

                        if (age.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PulseFieldSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A36)),
                            ) {
                                Text(
                                    text = "$age yrs • $gender",
                                    style = typo().labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CLOUD SYNC & DATA PERSISTENCE CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, PulseNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(PulseNeonGreen),
                                )
                                Text(
                                    text = "Cloud Sync & Backup",
                                    style = typo().titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        statusMessage = "Syncing local library to cloud..."
                                        val res = supabaseAuthRepository.syncAll()
                                        isSyncing = false
                                        res.fold(
                                            onSuccess = {
                                                statusMessage = "Sync completed! All library data backed up."
                                                isError = false
                                            },
                                            onFailure = { err ->
                                                statusMessage = err.message ?: "Sync failed"
                                                isError = true
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PulseNeonGreen,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                enabled = !isSyncing,
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Sync,
                                            contentDescription = "Sync",
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Sync Now",
                                            style = typo().labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your liked songs, custom playlists, and listening history are persistently stored and automatically backed up to your account.",
                            style = typo().bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LIBRARY STATISTICS OVERVIEW (3 CARDS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate(RecentlySongsDestination)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = PulseCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E24)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.History,
                                    contentDescription = "History",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${recentSongs.size}",
                                    style = typo().titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Recently Played",
                                    style = typo().labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate(LibraryDynamicPlaylistDestination(type = "favorite"))
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = PulseCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E24)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Favorite,
                                    contentDescription = "Liked",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${likedSongs.size}",
                                    style = typo().titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Liked Songs",
                                    style = typo().labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = PulseCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E24)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.QueueMusic,
                                    contentDescription = "Playlists",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${userPlaylists.size}",
                                    style = typo().titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Playlists",
                                    style = typo().labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // RECENTLY LISTENED SECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, Color(0xFF1E2822), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = SimpIcons.History,
                                    contentDescription = "Recent",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Recently Listened",
                                    style = typo().titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            if (recentSongs.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        navController.navigate(RecentlySongsDestination)
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text(
                                        text = "View All",
                                        style = typo().labelSmall,
                                        color = PulseNeonGreen,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (recentSongs.isEmpty()) {
                            Text(
                                text = "No recent tracks yet. Start listening to any music and your last played songs will appear here automatically!",
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                recentSongs.take(5).forEach { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                sharedViewModel.loadMediaItemFromTrack(song.toTrack(), Config.SONG_CLICK)
                                            }
                                            .background(PulseFieldSurface)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnails,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = song.title,
                                                style = typo().bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = song.artistName?.joinToString(", ") ?: "Unknown Artist",
                                                style = typo().bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                sharedViewModel.loadMediaItemFromTrack(song.toTrack(), Config.SONG_CLICK)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.PlayArrow,
                                                contentDescription = "Play",
                                                tint = PulseNeonGreen,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // LIKED SONGS SECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, Color(0xFF1E2822), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Favorite,
                                    contentDescription = "Liked",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Liked Songs (${likedSongs.size})",
                                    style = typo().titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            if (likedSongs.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        navController.navigate(LibraryDynamicPlaylistDestination(type = "favorite"))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text(
                                        text = "Open Playlist",
                                        style = typo().labelSmall,
                                        color = PulseNeonGreen,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (likedSongs.isEmpty()) {
                            Text(
                                text = "You haven't liked any songs yet. Tap the heart icon on any player to add songs to your favorites!",
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                likedSongs.take(4).forEach { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                sharedViewModel.loadMediaItemFromTrack(song.toTrack(), Config.SONG_CLICK)
                                            }
                                            .background(PulseFieldSurface)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnails,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = song.title,
                                                style = typo().bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = song.artistName?.joinToString(", ") ?: "Pulse Music",
                                                style = typo().bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                sharedViewModel.loadMediaItemFromTrack(song.toTrack(), Config.SONG_CLICK)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.PlayArrow,
                                                contentDescription = "Play",
                                                tint = PulseNeonGreen,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // USER PLAYLISTS SECTION
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, Color(0xFF1E2822), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = SimpIcons.QueueMusic,
                                    contentDescription = "Playlists",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "My Playlists (${userPlaylists.size})",
                                    style = typo().titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (userPlaylists.isEmpty()) {
                            Text(
                                text = "No local playlists yet. Create playlists from the Library tab to keep your music organized!",
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                userPlaylists.forEach { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                navController.navigate(LocalPlaylistDestination(id = playlist.id))
                                            }
                                            .background(PulseFieldSurface)
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF192A1F)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.QueueMusic,
                                                contentDescription = null,
                                                tint = PulseNeonGreen,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = playlist.title,
                                                style = typo().bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${playlist.tracks?.size ?: 0} tracks",
                                                style = typo().bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // EDITABLE PROFILE SETTINGS CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, PulseNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Edit Profile & Language",
                            style = typo().titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseNeonGreen,
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Profile Name", color = PulseNeonGreen) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseNeonGreen,
                                unfocusedBorderColor = Color(0xFF2E2E36),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = PulseNeonGreen,
                                focusedContainerColor = PulseFieldSurface,
                                unfocusedContainerColor = PulseFieldSurface,
                            ),
                        )

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { char -> char.isDigit() } },
                            label = { Text("Age", color = PulseNeonGreen) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseNeonGreen,
                                unfocusedBorderColor = Color(0xFF2E2E36),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = PulseNeonGreen,
                                focusedContainerColor = PulseFieldSurface,
                                unfocusedContainerColor = PulseFieldSurface,
                            ),
                        )

                        // Gender Selector Chips
                        Column {
                            Text(
                                text = "Gender",
                                style = typo().labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GENDER_OPTIONS.forEach { opt ->
                                    val isSelected = gender == opt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) PulseNeonGreen else PulseFieldSurface)
                                            .border(
                                                1.dp,
                                                if (isSelected) PulseNeonGreen else Color(0xFF2E2E36),
                                                RoundedCornerShape(20.dp),
                                            )
                                            .clickable { gender = opt }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = opt,
                                            style = typo().labelMedium,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }

                        // Indian Language Preference Selector Button
                        Column {
                            Text(
                                text = "Language Preference",
                                style = typo().labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PulseFieldSurface)
                                    .border(1.dp, PulseNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { showLanguagePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Subtitles,
                                            contentDescription = "Language",
                                            tint = PulseNeonGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = if (languagePreference.isNotBlank()) languagePreference else "Select Indian Language",
                                            style = typo().bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Icon(
                                        imageVector = SimpIcons.KeyboardArrowDown,
                                        contentDescription = "Select",
                                        tint = PulseNeonGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        // Save Profile Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                    statusMessage = "Profile updated successfully!"
                                    isError = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseNeonGreen,
                                contentColor = Color.Black,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = if (isError) MaterialTheme.colorScheme.error else PulseNeonGreen,
                            style = typo().bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // DONE / RETURN BUTTON
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                dataStoreManager.setHasSeenLoginPrompt(true)
                            }
                            navController.navigateUp()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseNeonGreen,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Done • Return to Music", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SIGN OUT / SWITCH ACCOUNT BUTTON
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                supabaseAuthRepository.signOut()
                                dataStoreManager.setProfile("", "", "", "")
                                name = ""
                                age = ""
                                hasJustSignedUpOrIn = false
                                statusMessage = "Signed out. You can now sign in or create a new account."
                                isError = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252),
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Sign Out / Switch Account", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                } else {
                    // ==========================================
                    // 2. INTEGRATED AUTH & SIGN UP PROFILE FLOW
                    // ==========================================

                    // Segmented Mode Toggle: [ Sign In ] vs [ Sign Up ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PulseCardSurface)
                            .border(1.dp, Color(0xFF26332A), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpMode) PulseNeonGreen else Color.Transparent)
                                .clickable {
                                    isSignUpMode = false
                                    statusMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sign In",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUpMode) Color.Black else Color.White,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpMode) PulseNeonGreen else Color.Transparent)
                                .clickable {
                                    isSignUpMode = true
                                    statusMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sign Up / Register",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUpMode) Color.Black else Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isSignUpMode) "Create Pulse Music Account & Profile" else "Welcome to Pulse Music",
                        style = typo().titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isSignUpMode) {
                            "Fill your name, age, and Indian language preference below to set up your profile and cloud backup simultaneously."
                        } else {
                            "Sign in with your email or Google account to access your synchronized library, liked songs, and playlists."
                        },
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // PROFILE FIELDS REQUIRED ON SIGN UP
                    if (isSignUpMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PulseCardSurface)
                                .border(1.dp, PulseNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Profile Details",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PulseNeonGreen,
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name *", color = if (name.isNotEmpty()) PulseNeonGreen else Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PulseNeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E2E36),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = PulseNeonGreen,
                                    focusedContainerColor = PulseFieldSurface,
                                    unfocusedContainerColor = PulseFieldSurface,
                                ),
                            )

                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter { char -> char.isDigit() } },
                                label = { Text("Age *", color = if (age.isNotEmpty()) PulseNeonGreen else Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PulseNeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E2E36),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = PulseNeonGreen,
                                    focusedContainerColor = PulseFieldSurface,
                                    unfocusedContainerColor = PulseFieldSurface,
                                ),
                            )

                            // Gender selector chips
                            Column {
                                Text(
                                    text = "Gender",
                                    style = typo().labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    GENDER_OPTIONS.forEach { opt ->
                                        val isSelected = gender == opt
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) PulseNeonGreen else PulseFieldSurface)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) PulseNeonGreen else Color(0xFF2E2E36),
                                                    RoundedCornerShape(20.dp),
                                                )
                                                .clickable { gender = opt }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                text = opt,
                                                style = typo().labelSmall,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                    }
                                }
                            }

                            // Indian Language Picker Field
                            Column {
                                Text(
                                    text = "Language Preference (Indian Languages)",
                                    style = typo().labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PulseFieldSurface)
                                        .border(1.dp, PulseNeonGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                        .clickable { showLanguagePicker = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Subtitles,
                                                contentDescription = "Language",
                                                tint = PulseNeonGreen,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = if (languagePreference.isNotBlank()) languagePreference else "Select Indian Language",
                                                style = typo().bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        Icon(
                                            imageVector = SimpIcons.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = PulseNeonGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // CREDENTIALS INPUT
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = if (email.isNotEmpty()) PulseNeonGreen else Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PulseNeonGreen,
                            focusedContainerColor = PulseFieldSurface,
                            unfocusedContainerColor = PulseFieldSurface,
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = if (password.isNotEmpty()) PulseNeonGreen else Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PulseNeonGreen,
                            focusedContainerColor = PulseFieldSurface,
                            unfocusedContainerColor = PulseFieldSurface,
                        ),
                    )

                    AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = if (isError) MaterialTheme.colorScheme.error else PulseNeonGreen,
                            style = typo().bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // PRIMARY SUBMIT BUTTON
                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                if (name.isBlank()) {
                                    statusMessage = "Please enter your name"
                                    isError = true
                                    return@Button
                                }
                                if (age.isBlank()) {
                                    statusMessage = "Please enter your age"
                                    isError = true
                                    return@Button
                                }
                            }
                            if (email.isBlank() || password.isBlank()) {
                                statusMessage = "Please enter email and password"
                                isError = true
                                return@Button
                            }

                            coroutineScope.launch {
                                isLoading = true
                                statusMessage = null

                                if (isSignUpMode) {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                }

                                val result = if (isSignUpMode) {
                                    supabaseAuthRepository.signUp(email.trim(), password)
                                } else {
                                    supabaseAuthRepository.signIn(email.trim(), password)
                                }

                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        dataStoreManager.setHasSeenLoginPrompt(true)
                                        hasJustSignedUpOrIn = true
                                        statusMessage = if (isSignUpMode) "Profile and account created successfully!" else "Signed in successfully!"
                                        isError = false
                                        coroutineScope.launch {
                                            supabaseAuthRepository.syncAll()
                                        }
                                    },
                                    onFailure = { err ->
                                        statusMessage = err.message ?: "Authentication failed"
                                        isError = true
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseNeonGreen,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Sign Up & Save Profile" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // "OR" DIVIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF26262E),
                        )
                        Text(
                            text = "  OR  ",
                            style = typo().labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF26262E),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GOOGLE SIGN IN BUTTON
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                googleEmailInput = if (email.isNotBlank()) email else ""
                                googleNameInput = if (name.isNotBlank()) name else ""
                                showGoogleSignInDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2024),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33363F)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GUEST CONTINUE / LOCAL PROFILE OPTION
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (name.isNotBlank() || age.isNotBlank() || languagePreference.isNotBlank()) {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                }
                                dataStoreManager.setHasSeenLoginPrompt(true)
                                hasJustSignedUpOrIn = true
                            }
                        }
                    ) {
                        Text(
                            text = "Continue as Guest / View Details",
                            color = Color.White.copy(alpha = 0.75f),
                            style = typo().labelLarge,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // ==========================================
    // 3. GOOGLE SIGN IN DIALOG
    // ==========================================
    if (showGoogleSignInDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSignInDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "G",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4),
                        )
                    }
                    Text(
                        text = "Sign in with Google",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Sign in with your Google account to automatically store and sync your liked songs and playlists across devices.",
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Email *", color = PulseNeonGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PulseNeonGreen,
                            focusedContainerColor = PulseFieldSurface,
                            unfocusedContainerColor = PulseFieldSurface,
                        ),
                    )

                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Display Name (Optional)", color = PulseNeonGreen) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PulseNeonGreen,
                            focusedContainerColor = PulseFieldSurface,
                            unfocusedContainerColor = PulseFieldSurface,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleEmailInput.isBlank()) return@Button
                        showGoogleSignInDialog = false
                        coroutineScope.launch {
                            isLoading = true
                            val res = supabaseAuthRepository.signInWithGoogle(
                                email = googleEmailInput.trim(),
                                name = googleNameInput.trim().ifBlank { null },
                            )
                            isLoading = false
                            res.fold(
                                onSuccess = {
                                    if (googleNameInput.isNotBlank()) {
                                        dataStoreManager.setProfile(
                                            name = googleNameInput.trim(),
                                            age = age.ifBlank { "22" },
                                            gender = gender,
                                            languagePreference = languagePreference,
                                        )
                                    }
                                    dataStoreManager.setHasSeenLoginPrompt(true)
                                    hasJustSignedUpOrIn = true
                                    statusMessage = "Welcome, ${googleNameInput.ifBlank { googleEmailInput }}! Google account connected."
                                    isError = false
                                    coroutineScope.launch {
                                        supabaseAuthRepository.syncAll()
                                    }
                                },
                                onFailure = { err ->
                                    statusMessage = err.message ?: "Google sign in failed"
                                    isError = true
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseNeonGreen,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Sign In with Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoogleSignInDialog = false },
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0D140F),
            shape = RoundedCornerShape(20.dp),
        )
    }

    // ==========================================
    // 4. INDIAN LANGUAGES SELECTION DIALOG
    // ==========================================
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Select Indian Language",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(
                        onClick = { showLanguagePicker = false },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(INDIAN_LANGUAGES) { lang ->
                        val formattedLang = "${lang.englishName} (${lang.nativeScript})"
                        val isSelected = languagePreference.startsWith(lang.englishName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else PulseFieldSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) PulseNeonGreen else Color(0xFF26262E),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    languagePreference = formattedLang
                                    showLanguagePicker = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = lang.englishName,
                                    style = typo().bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PulseNeonGreen else Color.White,
                                )
                                Text(
                                    text = lang.nativeScript,
                                    style = typo().bodySmall,
                                    color = if (isSelected) PulseNeonGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = SimpIcons.Check,
                                    contentDescription = "Selected",
                                    tint = PulseNeonGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF0D140F),
            shape = RoundedCornerShape(20.dp),
        )
    }
}
