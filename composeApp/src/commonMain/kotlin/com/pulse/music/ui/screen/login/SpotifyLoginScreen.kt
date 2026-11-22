package com.pulse.music.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.manikandan.common.Config
import com.pulse.music.expect.ui.PlatformWebView
import com.pulse.music.expect.ui.createWebViewCookieManager
import com.pulse.music.expect.ui.rememberWebViewState
import com.pulse.music.extension.getStringBlocking
import com.pulse.music.ui.component.DevCookieLogInBottomSheet
import com.pulse.music.ui.component.DevLogInBottomSheet
import com.pulse.music.ui.component.DevLogInType
import com.pulse.music.ui.component.RippleIconButton
import com.pulse.music.ui.icon.ArrowBackIosNew
import com.pulse.music.ui.icon.Cookie
import com.pulse.music.ui.icon.LogoDev
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.theme.typo
import com.pulse.music.viewModel.LogInViewModel
import com.pulse.music.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pulsemusic.composeapp.generated.resources.Res
import pulsemusic.composeapp.generated.resources.log_in_to_spotify
import pulsemusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun SpotifyLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val spotifyStatus by viewModel.spotifyStatus.collectAsStateWithLifecycle()

    val fullSpotifyCookies by viewModel.fullSpotifyCookies.collectAsStateWithLifecycle()

    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showCookiesBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    // Handle login success
    LaunchedEffect(spotifyStatus) {
        if (spotifyStatus) {
            settingsViewModel.setSpotifyLogIn(true)
            viewModel.makeToast(getString(Res.string.login_success))
            navController.navigateUp()
        }
    }

    val state = rememberWebViewState()
    val cookieManager = createWebViewCookieManager()

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            // WebView for Spotify login
            PlatformWebView(
                state,
                Config.SPOTIFY_LOG_IN_URL,
                aboveContent = {
                    if (devLoginSheet) {
                        DevLogInBottomSheet(
                            onDismiss = {
                                devLoginSheet = false
                            },
                            onDone = { spdc ->
                                devLoginSheet = false
                                val spdcText = "sp_dc=$spdc"
                                viewModel.saveSpotifySpdc(spdcText)
                            },
                            type = DevLogInType.Spotify,
                        )
                    }

                    if (showCookiesBottomSheet) {
                        DevCookieLogInBottomSheet(
                            onDismiss = {
                                showCookiesBottomSheet = false
                            },
                            type = DevLogInType.Spotify,
                            cookies = fullSpotifyCookies,
                        )
                    }
                },
            ) { url ->
                val cookie = cookieManager.getCookie(url)
                if (cookie.isNotEmpty()) {
                    val cookies =
                        cookie.split("; ").mapNotNull {
                            val idx = it.indexOf('=')
                            if (idx > 0) it.substring(0, idx).trim() to it.substring(idx + 1).trim() else null
                        }
                    viewModel.setFullSpotifyCookies(cookies)
                }

                val spdcCookie = cookie.takeIf { it.contains("sp_dc=") }
                    ?: cookieManager.getCookie("https://open.spotify.com").takeIf { it.contains("sp_dc=") }
                    ?: cookieManager.getCookie("https://accounts.spotify.com").takeIf { it.contains("sp_dc=") }

                val isCompletionUrl = url.startsWith("https://open.spotify.com") ||
                    url.contains("status") ||
                    url.contains("overview") ||
                    (!url.contains("/login") && !url.contains("/authorize"))

                if (spdcCookie != null && isCompletionUrl) {
                    viewModel.saveSpotifySpdc(spdcCookie)
                }
            }
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = stringResource(Res.string.log_in_to_spotify),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        PulseIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        devLoginSheet = true
                    },
                ) {
                    Icon(
                        PulseIcons.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}