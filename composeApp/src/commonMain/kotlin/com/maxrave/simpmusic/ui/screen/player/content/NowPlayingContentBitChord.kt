package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.maxrave.simpmusic.ui.component.AudioOutputDeviceBadge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.viewModel.UIEvent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Shadow
import com.maxrave.simpmusic.ui.component.AIBadge
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.viewModel.LyricsProvider

import androidx.compose.foundation.isSystemInDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalIsPearlLavenderTheme

import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.screen.player.content.expressive.WavySeekBar

internal val BitChordNeonAccent = Color(0xFFA788FA)
private val BitChordOledBlack = Color(0xFF000000)
private val BitChordSurface = Color(0xFF0D0D10)

/**
 * BitChord-inspired sleek, ultra-smooth player UI for Pulse Music.
 * Engineered for 60/120fps performance on low-end and flagship devices alike.
 * Uses isolated timeline sub-recomposition, spring-animated artwork breathing,
 * hairline capsule scrubbers, and AMOLED pure-black presentation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentBitChord(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    NowPlayingExpressiveTheme(state = state) {
        val isPlaying = state.controllerState.isPlaying
        val songTitle = state.screenData.nowPlayingTitle.ifEmpty { "Pulse Music" }
        val artistName = state.screenData.artistName.ifEmpty { "Pulse Studio" }

        val isLavender = LocalIsPearlLavenderTheme.current
        val isDark = isSystemInDarkTheme()
        var showShareLyricsSheet by rememberSaveable { mutableStateOf(false) }

        val albumThemeColor = remember(state.spotShadowColor, state.startColor.value) {
            if (state.spotShadowColor != Color.Unspecified &&
                state.spotShadowColor != Color.Black &&
                state.spotShadowColor != Color.Transparent
            ) {
                state.spotShadowColor
            } else if (state.startColor.value != Color.Black && state.startColor.value != Color.Transparent) {
                state.startColor.value
            } else {
                BitChordNeonAccent
            }
        }
        val accentColor by animateColorAsState(
            targetValue = albumThemeColor,
            animationSpec = tween(600),
            label = "BitChordAccentColor",
        )

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (isLavender) {
                        Modifier.background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    BitChordOledBlack.copy(alpha = 0.82f),
                                    BitChordOledBlack,
                                ),
                            ),
                        )
                    },
                ),
        ) {
        val primaryGlow = if (state.ambientColor.isSpecified && state.ambientColor != Color.Transparent && state.ambientColor != Color.Black) {
            state.ambientColor
        } else if (state.spotShadowColor.isSpecified && state.spotShadowColor != Color.Transparent && state.spotShadowColor != Color.White) {
            state.spotShadowColor
        } else {
            state.startColor.value
        }
        val secondaryGlow = if (state.ambientSecondaryColor.isSpecified && state.ambientSecondaryColor != Color.Transparent && state.ambientSecondaryColor != Color.Black) {
            state.ambientSecondaryColor
        } else {
            primaryGlow
        }

        val animatedAmbientColor by animateColorAsState(
            targetValue = primaryGlow,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "BitChordAmbientColor",
        )
        val animatedSecondaryColor by animateColorAsState(
            targetValue = secondaryGlow,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "BitChordAmbientSecondaryColor",
        )

        // Ambient radial and vertical bloom behind artwork with protective bottom scrim for time/controls
        if (state.ambientModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (size.width <= 0f || size.height <= 0f) return@drawBehind
                        val centerOffset = Offset(size.width / 2f, size.height * 0.35f)
                        val radius = maxOf(1f, size.width * 0.85f)
                        // 1. Soft radial artwork halo derived dynamically from album image
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedAmbientColor.copy(alpha = 0.45f),
                                    animatedSecondaryColor.copy(alpha = 0.20f),
                                    Color.Transparent,
                                ),
                                center = centerOffset,
                                radius = radius,
                            ),
                            radius = radius,
                            center = centerOffset,
                        )
                        // 2. Soft top-down ambient tint derived from album image
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    animatedAmbientColor.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = size.height * 0.55f,
                            ),
                        )
                        // 3. Contrast gradient scrim across bottom half: ensures song time & controls are ALWAYS 100% visible
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.75f),
                                ),
                                startY = size.height * 0.48f,
                                endY = size.height,
                            ),
                        )
                    },
            )
        }

        // Soft breathing ambient glow behind artwork matching album image
        val breatheTransition = rememberInfiniteTransition(label = "ArtworkAmbientBreathe")
        val breatheScale by breatheTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "BreatheScale",
        )
        val breatheAlpha by breatheTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "BreatheAlpha",
        )
        if (state.ambientModeEnabled || isLavender) {
            val fallbackA = Color(0xFFA788FA)
            val fallbackB = if (isDark) Color(0xFFF288C6) else Color(0xFFF2B8C6)
            val glowStop1 = if (animatedAmbientColor != Color.Black && animatedAmbientColor.isSpecified) animatedAmbientColor else fallbackA
            val glowStop2 = if (animatedSecondaryColor != Color.Black && animatedSecondaryColor.isSpecified) animatedSecondaryColor else fallbackB

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(breatheScale)
                    .graphicsLayer { alpha = if (state.controllerState.isPlaying) breatheAlpha else 0.25f }
                    .drawBehind {
                        if (size.width <= 0f || size.height <= 0f) return@drawBehind
                        val centerOffset = Offset(size.width / 2f, size.height * 0.38f)
                        val radius = maxOf(1f, size.width * 0.75f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowStop1.copy(alpha = 0.55f),
                                    glowStop2.copy(alpha = 0.28f),
                                    Color.Transparent,
                                ),
                                center = centerOffset,
                                radius = radius,
                            ),
                            radius = radius,
                            center = centerOffset,
                        )
                    },
            )
        }

        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Column: Top Bar dismiss + Breathing Artwork Poster
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Mini Top Bar for left column
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { actions.onDismiss() },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = state.dismissIcon,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BitChordMiniVisualizer(isPlaying = isPlaying, color = accentColor)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.6.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = accentColor,
                        )
                    }

                    // Centered Breathing Artwork Poster (fits height in landscape)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BitChordArtworkHero(
                            state = state,
                            actions = actions,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f, matchHeightConstraintsFirst = true),
                        )
                    }

                    // Instagram-style Glowing Lyrics line under the album image in landscape
                    val landscapeLyricLine = remember(state.screenData.lyricsData, state.currentLyricLineIndex) {
                        state.screenData.lyricsData?.lyrics?.lines
                            ?.getOrNull(state.currentLyricLineIndex)
                            ?.words
                            ?.stripRichSyncTimestamps()
                    }
                    if (!landscapeLyricLine.isNullOrBlank()) {
                        BitChordInstagramGlowingLyrics(
                            line = landscapeLyricLine,
                            translatedLine = null,
                            accentColor = accentColor,
                            onClick = { actions.onShowFullscreenLyrics() },
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }

                // Right Column: More Options + Track Info + Scrubber + Transport Controls + Action Dock
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top Bar for right column with track/playlist title and overflow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val queueTrack = state.artworkQueue.getOrNull(state.currentOrderIndex)
                        Text(
                            text = queueTrack?.title ?: "Pulse Music",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                        )
                        IconButton(
                            onClick = { actions.onShowMoreSheet() },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = SimpIcons.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    // Track Info (Title, Clickable Artist, Codec badge & Heart toggle)
                    BitChordTrackInfoRow(
                        title = songTitle,
                        artist = artistName,
                        codec = state.audioCodecLabel ?: "HQ AUDIO",
                        isLiked = state.likeStatus,
                        onArtistClick = { actions.onNavigateToArtist() },
                        onLikeClick = { actions.onAddToYouTubeLiked() },
                        accentColor = accentColor,
                    )

                    // Isolated Hairline Scrubber (ThinSlider)
                    BitChordIsolatedScrubber(
                        state = state,
                        actions = actions,
                        accentColor = accentColor,
                    )

                    // Primary Transport Controls (Shuffle, Prev, Big Pulse Play/Pause, Next, Repeat)
                    BitChordTransportRow(
                        state = state,
                        actions = actions,
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                    )

                    // Bottom Action Dock (Lyrics, Pipeline/Info, Add to Playlist, Queue)
                    BitChordActionDock(
                        hasLyrics = state.screenData.lyricsData != null,
                        actions = actions,
                        accentColor = accentColor,
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 520.dp)
                    .verticalScroll(scrollState)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. Top Bar: Dismiss, Header title, More options
                BitChordTopBar(
                    state = state,
                    actions = actions,
                    accentColor = accentColor,
                )

                Spacer(Modifier.height(8.dp))

                // 2. Centered Breathing Artwork Hero with swipe support
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BitChordArtworkHero(
                        state = state,
                        actions = actions,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Instagram-style Glowing Lyrics line under the album image
                val activeLyricLine = remember(state.screenData.lyricsData, state.currentLyricLineIndex) {
                    state.screenData.lyricsData?.lyrics?.lines
                        ?.getOrNull(state.currentLyricLineIndex)
                        ?.words
                        ?.stripRichSyncTimestamps()
                }
                val translatedLyricLine = remember(state.screenData.lyricsData, state.currentLyricLineIndex) {
                    state.screenData.lyricsData?.translatedLyrics?.first?.lines
                        ?.getOrNull(state.currentLyricLineIndex)
                        ?.words
                        ?.stripRichSyncTimestamps()
                }

                AnimatedVisibility(
                    visible = !activeLyricLine.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    if (!activeLyricLine.isNullOrBlank()) {
                        BitChordInstagramGlowingLyrics(
                            line = activeLyricLine,
                            translatedLine = translatedLyricLine,
                            accentColor = accentColor,
                            onClick = { actions.onShowFullscreenLyrics() },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 3. Track Info (Title, Clickable Artist, Codec badge & Heart toggle)
                BitChordTrackInfoRow(
                    title = songTitle,
                    artist = artistName,
                    codec = state.audioCodecLabel ?: "HQ AUDIO",
                    isLiked = state.likeStatus,
                    onArtistClick = { actions.onNavigateToArtist() },
                    onLikeClick = { actions.onAddToYouTubeLiked() },
                    accentColor = accentColor,
                )

                Spacer(Modifier.height(16.dp))

                // 4. Isolated Hairline Scrubber (ThinSlider) — zero parent recomposition!
                BitChordIsolatedScrubber(
                    state = state,
                    actions = actions,
                    accentColor = accentColor,
                )

                Spacer(Modifier.height(16.dp))

                // 5. Primary Transport Controls (Shuffle, Prev, Big Pulse Play/Pause, Next, Repeat)
                BitChordTransportRow(
                    state = state,
                    actions = actions,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                )

                Spacer(Modifier.height(18.dp))

                // 6. Bottom Action Dock (Lyrics, Pipeline/Info, Add to Playlist, Queue)
                BitChordActionDock(
                    hasLyrics = state.screenData.lyricsData != null,
                    actions = actions,
                    accentColor = accentColor,
                )

                // 7. Spotify-style Pulse Neon Lyrics Card below transport
                if (state.screenData.lyricsData?.lyrics?.lines?.any { !it.words.isNullOrBlank() } == true) {
                    Spacer(Modifier.height(20.dp))
                    BitChordSpotifyLyricsCard(
                        state = state,
                        actions = actions,
                        accentColor = accentColor,
                        onShareLyrics = { showShareLyricsSheet = true },
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }

        if (showShareLyricsSheet) {
            state.screenData.lyricsData?.let { lData ->
                ShareLyricsSheet(
                    lines = lData.toShareLyricsLines(),
                    songTitle = state.screenData.nowPlayingTitle,
                    artistName = state.screenData.artistName,
                    artwork = state.screenData.bitmap,
                    seedColor = state.startColor.value,
                    initialLineIndex = state.currentLyricLineIndex,
                    onDismiss = { showShareLyricsSheet = false },
                )
            }
        }
    }
}

/** Top bar with chevron dismiss, centered "NOW PLAYING", and overflow menu. */
@Composable
private fun BitChordTopBar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    accentColor: Color,
) {
    val isLavender = LocalIsPearlLavenderTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { actions.onDismiss() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = state.dismissIcon,
                contentDescription = "Dismiss",
                tint = if (isLavender) MaterialTheme.colorScheme.onBackground else Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BitChordMiniVisualizer(isPlaying = state.controllerState.isPlaying, color = accentColor)
                Text(
                    text = "PLAYING FROM PULSE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = accentColor,
                )
                BitChordMiniVisualizer(isPlaying = state.controllerState.isPlaying, color = accentColor)
            }
            val queueTrack = state.artworkQueue.getOrNull(state.currentOrderIndex)
            Text(
                text = queueTrack?.title ?: "Pulse Music",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = if (isLavender) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp),
                textAlign = TextAlign.Center,
            )
        }

        IconButton(
            onClick = { actions.onShowMoreSheet() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SimpIcons.MoreVert,
                contentDescription = "More Options",
                tint = if (isLavender) MaterialTheme.colorScheme.onBackground else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Upgrades YouTube and GoogleUserContent thumbnail URLs to maximum uncompressed resolution. */
private fun getHighResArtworkUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    var res = url
    if (res.contains("googleusercontent.com") || res.contains("ggpht.com")) {
        res = res.replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*"), "=s1200")
    }
    return res
}

/** Artwork / Video Hero with breathing scale animation (1.0f play / 0.94f pause) and rounded corners. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BitChordArtworkHero(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    isPlaying: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1f),
) {
    var isVideoMode by rememberSaveable(state.screenData.thumbnailURL) {
        mutableStateOf(state.shouldShowVideo)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ArtworkBreathingScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = state.spotShadowColor.copy(alpha = 0.45f),
                ambientColor = Color.Black,
            )
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
            )
            .background(BitChordSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (isVideoMode || state.shouldShowVideo) {
            Box(modifier = Modifier.fillMaxSize()) {
                MediaPlayerViewWithSubtitle(
                    playerName = MAIN_PLAYER,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    shouldShowSubtitle = true,
                    shouldPip = false,
                    shouldScaleDownSubtitle = true,
                    timelineState = state.timelineState,
                    lyricsData = state.screenData.lyricsData?.lyrics,
                    translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                    isInPipMode = state.isInPipMode,
                    mainTextStyle = MaterialTheme.typography.bodyLarge,
                    translatedTextStyle = MaterialTheme.typography.bodyMedium,
                )
                IconButton(
                    onClick = { actions.onEnterFullscreenVideo() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                ) {
                    Icon(
                        imageVector = SimpIcons.Fullscreen,
                        contentDescription = "Fullscreen Video",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else if (state.artworkQueue.isNotEmpty()) {
            HorizontalPager(
                state = state.artworkPagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val track: Track? = state.artworkQueue.getOrNull(page)
                val rawUrl = track?.thumbnails?.lastOrNull()?.url
                    ?: track?.thumbnails?.firstOrNull()?.url
                    ?: (if (page == state.currentOrderIndex || page == state.artworkPagerState.currentPage) state.screenData.thumbnailURL else null)
                var artworkUrl by remember(rawUrl) {
                    mutableStateOf(getHighResArtworkUrl(rawUrl))
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(artworkUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(300)
                        .build(),
                    contentDescription = track?.title ?: "Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        if (page == state.currentOrderIndex || page == state.artworkPagerState.currentPage) {
                            actions.onArtworkBitmap(it.result.image.toImageBitmap())
                        }
                    },
                    onError = {
                        val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                        if (fallback != null && fallback != artworkUrl) {
                            artworkUrl = fallback
                        }
                    },
                    error = rememberHolderPainter(),
                    placeholder = rememberHolderPainter(),
                )
            }
        } else {
            val rawUrl = state.screenData.thumbnailURL
            var artworkUrl by remember(rawUrl) {
                mutableStateOf(getHighResArtworkUrl(rawUrl))
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(artworkUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(300)
                    .build(),
                contentDescription = "Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = {
                    actions.onArtworkBitmap(it.result.image.toImageBitmap())
                },
                onError = {
                    val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != artworkUrl) {
                        artworkUrl = fallback
                    }
                },
                error = rememberHolderPainter(),
                placeholder = rememberHolderPainter(),
            )
        }
    }
}

/** Title, Artist, Codec Badge, and Heart/Like Button. */
@Composable
internal fun BitChordTrackInfoRow(
    title: String,
    artist: String,
    codec: String,
    isLiked: Boolean,
    onArtistClick: () -> Unit,
    onLikeClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isLavender = LocalIsPearlLavenderTheme.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                ),
                color = if (isLavender) MaterialTheme.colorScheme.onBackground else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                    ),
                    color = if (isLavender) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).clickable { onArtistClick() },
                )

                // Lossless/Hi-Res/Codec badge pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = codec,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = accentColor,
                    )
                }

                // Audio Output Device Indicator pill (Soft pastel colours)
                AudioOutputDeviceBadge(
                    softAccent = accentColor,
                    fontSize = 9.sp,
                    iconSize = 11.dp,
                )
            }
        }

        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isLiked) accentColor.copy(alpha = 0.15f) else Color.Transparent),
        ) {
            Icon(
                imageVector = if (isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) accentColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Isolated Scrubber collecting `state.timelineFlow` and `state.timelineState` locally.
 * Prevents full NowPlaying screen recomposition during high-frequency audio position updates!
 */
@Composable
private fun BitChordIsolatedScrubber(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    accentColor: Color,
) {
    val isLavender = LocalIsPearlLavenderTheme.current
    val timeline by state.timelineFlow.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val rawTotal = timeline.total.takeIf { it > 1L }
        ?: state.timelineState.total.takeIf { it > 1L }
        ?: (state.artworkQueue.getOrNull(state.currentOrderIndex)?.durationSeconds?.toLong()?.let { it * 1000L })
        ?: 1L

    val totalMs = rawTotal.coerceAtLeast(1L)
    val currentMs = when {
        isDragging -> (dragProgress * totalMs).toLong()
        timeline.current > 0L -> timeline.current
        state.timelineState.current > 0L -> state.timelineState.current
        state.sliderValue > 0f -> (totalMs * (state.sliderValue / 100f)).toLong()
        else -> 0L
    }.coerceIn(0L, totalMs)

    val currentFraction = if (isDragging) {
        dragProgress
    } else if (totalMs > 1L) {
        (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        (state.sliderValue / 100f).coerceIn(0f, 1f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        WavySeekBar(
            progressFraction = currentFraction,
            isPlaying = state.controllerState.isPlaying,
            activeColor = accentColor,
            trackColor = if (isLavender) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.2f),
            thumbColor = if (isLavender) MaterialTheme.colorScheme.primary else Color.White,
            onSliderChange = { newPercent ->
                isDragging = true
                dragProgress = newPercent / 100f
                actions.onSliderChange(newPercent)
            },
            onSliderChangeFinished = {
                isDragging = false
                actions.onSliderChangeFinished()
            },
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val timeShadow = Shadow(
                color = Color.Black.copy(alpha = 0.75f),
                blurRadius = 4f,
                offset = Offset(0f, 1f),
            )
            Text(
                text = formatDuration(currentMs),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    shadow = timeShadow,
                ),
                color = if (isLavender) MaterialTheme.colorScheme.primary else Color.White,
            )

            val remainingMs = (totalMs - currentMs).coerceAtLeast(0L)
            Text(
                text = "-${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = timeShadow,
                ),
                color = if (isLavender) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/** BitChord's iconic Hairline Capsule scrubber that broadens smoothly under user contact. */
@Composable
private fun BitChordThinSlider(
    fraction: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    idleHeight: Dp = 5.dp,
    activeHeight: Dp = 11.dp,
    activeColor: Color = BitChordNeonAccent,
    inactiveColor: Color = Color.White.copy(alpha = 0.2f),
) {
    var isTouching by remember { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isTouching) activeHeight else idleHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "ThinSliderHeight",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(activeHeight + 18.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouching = true
                    onValueChange((down.position.x / size.width).coerceIn(0f, 1f))

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) {
                            pointer.consume()
                            break
                        }
                        onValueChange((pointer.position.x / size.width).coerceIn(0f, 1f))
                        pointer.consume()
                    }
                    isTouching = false
                    onValueChangeFinished()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight),
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

            // Inactive unplayed background bar
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius,
            )

            // Active played highlight bar
            val playedWidth = barWidth * fraction.coerceIn(0f, 1f)
            if (playedWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(playedWidth, barHeight),
                    cornerRadius = cornerRadius,
                )
                // Visible playhead thumb for clear progressive timing indication
                val thumbRadius = if (isTouching) 7.dp.toPx() else 4.5.dp.toPx()
                val thumbCenter = Offset(
                    x = playedWidth.coerceIn(thumbRadius, barWidth - thumbRadius),
                    y = barHeight / 2f,
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = thumbCenter,
                )
            }
        }
    }
}

/** Transport Row: Shuffle, SkipPrevious, Large Pulse Play/Pause, SkipNext, Repeat. */
@Composable
internal fun BitChordTransportRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isLavender = LocalIsPearlLavenderTheme.current
    val isShuffle = state.controllerState.isShuffle
    val repeatState = state.controllerState.repeatState

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle Toggle
        IconButton(
            onClick = { actions.onUIEvent(UIEvent.Shuffle) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = SimpIcons.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) accentColor else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Previous — Material Expressive 3 Pill Button
        val prevInteraction = remember { MutableInteractionSource() }
        val isPrevPressed by prevInteraction.collectIsPressedAsState()
        val prevScale by animateFloatAsState(
            targetValue = if (isPrevPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "M3EPrevPressScale",
        )
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 52.dp)
                .scale(prevScale)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isLavender) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }
                )
                .clickable(
                    interactionSource = prevInteraction,
                    indication = null,
                    onClick = { actions.onUIEvent(UIEvent.Previous) }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.SkipPrevious,
                contentDescription = "Previous",
                tint = if (isLavender) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                modifier = Modifier.size(30.dp),
            )
        }

        // Material Expressive 3 Play/Pause Button (Morphing corner, clean tactile surface, zero glow)
        val playCornerRadius by animateDpAsState(
            targetValue = if (isPlaying) 20.dp else 36.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "M3EPlayCorner",
        )
        val playWidth by animateDpAsState(
            targetValue = if (isPlaying) 76.dp else 72.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "M3EPlayWidth",
        )
        val playInteraction = remember { MutableInteractionSource() }
        val isPlayPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "M3EPlayPressScale",
        )

        Box(
            modifier = Modifier
                .size(width = playWidth, height = 72.dp)
                .scale(playScale)
                .clip(RoundedCornerShape(playCornerRadius))
                .background(
                    if (isLavender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                )
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }

        // Next — Material Expressive 3 Pill Button
        val nextInteraction = remember { MutableInteractionSource() }
        val isNextPressed by nextInteraction.collectIsPressedAsState()
        val nextScale by animateFloatAsState(
            targetValue = if (isNextPressed) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "M3ENextPressScale",
        )
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 52.dp)
                .scale(nextScale)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isLavender) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }
                )
                .clickable(
                    interactionSource = nextInteraction,
                    indication = null,
                    onClick = { actions.onUIEvent(UIEvent.Next) }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.SkipNext,
                contentDescription = "Next",
                tint = if (isLavender) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                modifier = Modifier.size(30.dp),
            )
        }

        // Repeat Toggle
        IconButton(
            onClick = { actions.onUIEvent(UIEvent.Repeat) },
            modifier = Modifier.size(44.dp),
        ) {
            val repeatIcon = when (repeatState) {
                RepeatState.One -> SimpIcons.RepeatOne
                else -> SimpIcons.Repeat
            }
            val repeatTint = when (repeatState) {
                RepeatState.None -> Color.White.copy(alpha = 0.45f)
                else -> accentColor
            }
            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = repeatTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Bottom Action Dock: Lyrics, Audio Info, Add to Playlist, Queue Sheet. */
@Composable
private fun BitChordActionDock(
    hasLyrics: Boolean,
    actions: NowPlayingContentActions,
    accentColor: Color,
) {
    val isLavender = LocalIsPearlLavenderTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLavender) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lyrics button
        IconButton(
            onClick = { actions.onShowFullscreenLyrics() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SimpIcons.Subtitles,
                contentDescription = "Lyrics",
                tint = if (hasLyrics) accentColor else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }

        // Pipeline / Equalizer / Audio Info
        IconButton(
            onClick = { actions.onShowInfo() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SimpIcons.Info,
                contentDescription = "Audio Pipeline Info",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }

        // Add to Playlist
        IconButton(
            onClick = { actions.onShowAddToPlaylist() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SimpIcons.PlaylistAdd,
                contentDescription = "Add to Playlist",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Queue
        IconButton(
            onClick = { actions.onShowQueue() },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SimpIcons.QueueMusic,
                contentDescription = "Up Next Queue",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Animated mini visualizer with 3 jumping bars indicating active playback. */
@Composable
fun BitChordMiniVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "MiniVisualizer")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = if (isPlaying) 14f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Bar1",
    )
    val h2 by transition.animateFloat(
        initialValue = 12f,
        targetValue = if (isPlaying) 5f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Bar2",
    )
    val h3 by transition.animateFloat(
        initialValue = 6f,
        targetValue = if (isPlaying) 15f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Bar3",
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

/** Instagram-style dynamic glowing lyrics subtitle directly under the artwork. */
@Composable
private fun BitChordInstagramGlowingLyrics(
    line: String,
    translatedLine: String?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "InstagramLyricsGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GlowAlpha",
    )
    val glowShift by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GlowShift",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    letterSpacing = (-0.1).sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.65f),
                        blurRadius = 4f,
                    ),
                ),
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (!translatedLine.isNullOrBlank()) {
                Text(
                    text = translatedLine,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.5.sp,
                    ),
                    color = accentColor.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Spotify-style Pulse Neon Lyrics Card rendered below the player controls. */
@Composable
private fun BitChordSpotifyLyricsCard(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    accentColor: Color,
    onShareLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsData = state.screenData.lyricsData ?: return
    val hasLines = lyricsData.lyrics?.lines?.any { !it.words.isNullOrBlank() } == true
    if (!hasLines) return

    val isLavender = LocalIsPearlLavenderTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "SpotifyCardNeon")
    val cardGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "CardGlowAlpha",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isLavender) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color(0xFF101015),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header: "LYRICS" + AI Badge + Rate + Share + Fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            shadow = Shadow(
                                color = accentColor.copy(alpha = 0.6f),
                                blurRadius = 12f,
                            ),
                        ),
                        color = accentColor,
                    )
                    if (lyricsData.translatedLyrics?.second == LyricsProvider.AI) {
                        AIBadge()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (lyricsData.canVote()) {
                        IconButton(
                            onClick = { actions.onShowVoteDialog() },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = SimpIcons.ThumbsUpDown,
                                contentDescription = "Rate Lyrics",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onShareLyrics,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Share,
                            contentDescription = "Share Lyrics",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "MORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = accentColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .clickable { actions.onShowFullscreenLyrics() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Body: Synchronized LyricsView with neon glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                LyricsView(
                    lyricsData = lyricsData,
                    timeLine = state.timelineFlow,
                    onLineClick = { fraction ->
                        actions.onUIEvent(UIEvent.UpdateProgress(fraction))
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Footer: Sync type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val syncTypeStr = when (lyricsData.lyrics?.syncType) {
                    "LINE_SYNCED" -> "LINE SYNCED"
                    "RICH_SYNCED" -> "RICH SYNCED"
                    else -> "UNSYNCED"
                }
                Text(
                    text = syncTypeStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}
