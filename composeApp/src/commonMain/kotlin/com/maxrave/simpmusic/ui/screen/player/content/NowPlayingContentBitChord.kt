package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.RepeatState
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

private val BitChordPulseRed = Color(0xFFFA2D48)
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
    val isPlaying = state.controllerState.isPlaying
    val songTitle = state.screenData.nowPlayingTitle.ifEmpty { "Pulse Music" }
    val artistName = state.screenData.artistName.ifEmpty { "Pulse Studio" }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BitChordOledBlack),
    ) {
        val ambientColor = state.spotShadowColor

        // Ambient radial glow behind artwork (hardware-accelerated drawBehind)
        if (state.ambientModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.28f }
                    .drawBehind {
                        val centerOffset = Offset(size.width / 2f, size.height * 0.38f)
                        val radius = size.width * 0.75f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ambientColor,
                                    ambientColor.copy(alpha = 0.4f),
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Top Bar: Dismiss, Header title, More options
            BitChordTopBar(
                state = state,
                actions = actions,
            )

            Spacer(Modifier.height(8.dp))

            // 2. Centered Breathing Artwork Hero with swipe support
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BitChordArtworkHero(
                    state = state,
                    actions = actions,
                    isPlaying = isPlaying,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. Track Info (Title, Clickable Artist, Codec badge & Heart toggle)
            BitChordTrackInfoRow(
                title = songTitle,
                artist = artistName,
                codec = state.audioCodecLabel ?: "HQ AUDIO",
                isLiked = state.likeStatus,
                onArtistClick = { actions.onNavigateToArtist() },
                onLikeClick = { actions.onAddToYouTubeLiked() },
            )

            Spacer(Modifier.height(18.dp))

            // 4. Isolated Hairline Scrubber (ThinSlider) — zero parent recomposition!
            BitChordIsolatedScrubber(
                state = state,
                actions = actions,
            )

            Spacer(Modifier.height(16.dp))

            // 5. Primary Transport Controls (Shuffle, Prev, Big Pulse Play/Pause, Next, Repeat)
            BitChordTransportRow(
                state = state,
                actions = actions,
                isPlaying = isPlaying,
            )

            Spacer(Modifier.height(20.dp))

            // 6. Bottom Action Dock (Lyrics, Pipeline/Info, Add to Playlist, Queue)
            BitChordActionDock(
                hasLyrics = state.screenData.lyricsData != null,
                actions = actions,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Top bar with chevron dismiss, centered "NOW PLAYING", and overflow menu. */
@Composable
private fun BitChordTopBar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
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
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PLAYING FROM PULSE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = BitChordPulseRed,
            )
            val queueTrack = state.artworkQueue.getOrNull(state.currentOrderIndex)
            Text(
                text = queueTrack?.title ?: "Pulse Music",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = 0.7f),
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
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Artwork Hero with breathing scale animation (1.0f play / 0.94f pause) and rounded corners. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BitChordArtworkHero(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    isPlaying: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ArtworkBreathingScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
        if (state.artworkQueue.isNotEmpty()) {
            HorizontalPager(
                state = state.artworkPagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val track: Track? = state.artworkQueue.getOrNull(page)
                val artworkUrl = track?.thumbnails?.lastOrNull()?.url
                    ?: state.screenData.thumbnailURL

                AsyncImage(
                    model = artworkUrl,
                    contentDescription = track?.title ?: "Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = rememberHolderPainter(),
                    placeholder = rememberHolderPainter(),
                )
            }
        } else {
            AsyncImage(
                model = state.screenData.thumbnailURL,
                contentDescription = "Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = rememberHolderPainter(),
                placeholder = rememberHolderPainter(),
            )
        }
    }
}

/** Title, Artist, Codec Badge, and Heart/Like Button. */
@Composable
private fun BitChordTrackInfoRow(
    title: String,
    artist: String,
    codec: String,
    isLiked: Boolean,
    onArtistClick: () -> Unit,
    onLikeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                color = Color.White,
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
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onArtistClick() },
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
                        color = BitChordPulseRed,
                    )
                }
            }
        }

        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isLiked) BitChordPulseRed.copy(alpha = 0.15f) else Color.Transparent),
        ) {
            Icon(
                imageVector = if (isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) BitChordPulseRed else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Isolated Scrubber collecting `state.timelineFlow` locally.
 * Prevents full NowPlaying screen recomposition during high-frequency audio position updates!
 */
@Composable
private fun BitChordIsolatedScrubber(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val timeline by state.timelineFlow.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val totalMs = timeline.total.coerceAtLeast(1L)
    val currentMs = if (isDragging) (dragProgress * totalMs).toLong() else timeline.current

    val currentFraction = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        BitChordThinSlider(
            fraction = currentFraction,
            onValueChange = { newFraction ->
                isDragging = true
                dragProgress = newFraction
                actions.onSliderChange(newFraction * 100f)
            },
            onValueChangeFinished = {
                isDragging = false
                actions.onSliderChangeFinished()
            },
            idleHeight = 5.dp,
            activeHeight = 11.dp,
            activeColor = BitChordPulseRed,
            inactiveColor = Color.White.copy(alpha = 0.2f),
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentMs / 1000),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                ),
                color = Color.White.copy(alpha = 0.55f),
            )

            val remainingSec = ((totalMs - currentMs).coerceAtLeast(0L)) / 1000
            Text(
                text = "-${formatDuration(remainingSec)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                ),
                color = Color.White.copy(alpha = 0.55f),
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
    activeColor: Color = BitChordPulseRed,
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
            }
        }
    }
}

/** Transport Row: Shuffle, SkipPrevious, Large Pulse Play/Pause, SkipNext, Repeat. */
@Composable
private fun BitChordTransportRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    isPlaying: Boolean,
) {
    val isShuffle = state.controllerState.isShuffle
    val repeatState = state.controllerState.repeatState

    Row(
        modifier = Modifier.fillMaxWidth(),
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
                tint = if (isShuffle) BitChordPulseRed else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Previous
        IconButton(
            onClick = { actions.onUIEvent(UIEvent.Previous) },
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                imageVector = SimpIcons.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }

        // Large Circular Pulse Play/Pause Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = BitChordPulseRed.copy(alpha = 0.6f),
                )
                .clip(CircleShape)
                .background(BitChordPulseRed)
                .clickable { actions.onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(38.dp),
            )
        }

        // Next
        IconButton(
            onClick = { actions.onUIEvent(UIEvent.Next) },
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                imageVector = SimpIcons.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
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
                else -> BitChordPulseRed
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
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
                tint = if (hasLyrics) BitChordPulseRed else Color.White.copy(alpha = 0.6f),
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
