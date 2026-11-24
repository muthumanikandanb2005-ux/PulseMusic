package com.pulse.music.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.pulse.music.expect.ui.MediaPlayerView
import com.pulse.music.expect.ui.rememberCurrentAudioOutputDevice
import com.pulse.music.extension.formatDuration
import com.pulse.music.ui.component.MovingAmbientLightBackground
import com.pulse.music.ui.component.heartBurst
import com.pulse.music.ui.component.rememberHeartBurstState
import com.pulse.music.ui.icon.Bluetooth
import com.pulse.music.ui.icon.Favorite
import com.pulse.music.ui.icon.FavoriteBorder
import com.pulse.music.ui.icon.Headphones
import com.pulse.music.ui.icon.KeyboardArrowDown
import com.pulse.music.ui.icon.MoreVert
import com.pulse.music.ui.icon.Pause
import com.pulse.music.ui.icon.PlayArrow
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.icon.QueueMusic
import com.pulse.music.ui.icon.RemoveCircleOutline
import com.pulse.music.ui.icon.Repeat
import com.pulse.music.ui.icon.RepeatOne
import com.pulse.music.ui.icon.Shuffle
import com.pulse.music.ui.icon.SkipNext
import com.pulse.music.ui.icon.SkipPrevious
import com.pulse.music.ui.icon.VolumeUp
import com.pulse.music.ui.theme.typo
import com.pulse.music.viewModel.UIEvent
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import pulsemusic.composeapp.generated.resources.Res
import pulsemusic.composeapp.generated.resources.now_playing_upper

/**
 * Vibrant Expressive Now Playing style:
 * Inspired by the Behance modern music player UI design with Material 3 Expressive theme,
 * concentric glowing progress ring around a circular disc artwork,
 * centered title/artist/duration timestamps, dynamic multi-colour moving ambient light background,
 * full-screen/aperture video canvas playback support, and 5-button main control row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentVibrantExpressive(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    NowPlayingVibrantExpressiveTheme(state = state) {
        NowPlayingVibrantExpressiveLayout(state = state, actions = actions)
    }
}

@Composable
internal fun NowPlayingVibrantExpressiveTheme(
    state: NowPlayingContentState,
    content: @Composable () -> Unit,
) {
    // Immediate color syncing derived directly from album image
    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor.isSpecified && paletteColor != Color.Black && paletteColor != Color.Transparent) {
        paletteColor
    } else if (state.ambientColor.isSpecified && state.ambientColor != Color.Transparent && state.ambientColor != Color.Black) {
        state.ambientColor
    } else {
        Color(0xFF818CF8) // Soft periwinkle fallback
    }

    val animatedSeedColor by animateColorAsState(
        targetValue = seedColor,
        animationSpec = tween(durationMillis = 150),
        label = "vibrantExpressiveSeedColor",
    )

    val derivedScheme = rememberDynamicColorScheme(
        seedColor = animatedSeedColor,
        isDark = true,
        isAmoled = false,
        style = PaletteStyle.Vibrant,
    )

    MaterialExpressiveTheme(colorScheme = derivedScheme) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingVibrantExpressiveLayout(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme

    val primaryAmbient = if (state.ambientColor.isSpecified && state.ambientColor != Color.Transparent && state.ambientColor != Color.Black) {
        state.ambientColor
    } else if (state.startColor.value.isSpecified && state.startColor.value != Color.Black && state.startColor.value != Color.Transparent) {
        state.startColor.value
    } else {
        colorScheme.primary
    }

    val secondaryAmbient = if (state.ambientSecondaryColor.isSpecified && state.ambientSecondaryColor != Color.Transparent && state.ambientSecondaryColor != Color.Black) {
        state.ambientSecondaryColor
    } else {
        colorScheme.secondary
    }

    val hasCanvas = state.screenData.canvasData != null
    val isCanvasVideo = state.screenData.canvasData?.isVideo == true
    val canvasUrl = state.screenData.canvasData?.url

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101418))) {
        // Multi-colour moving ambient background / Fullscreen canvas video backdrop
        if (hasCanvas && canvasUrl != null) {
            Crossfade(targetState = isCanvasVideo, label = "vibrantCanvasBackdrop") { isVid ->
                if (isVid) {
                    MediaPlayerView(
                        url = canvasUrl,
                        cropToBounds = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(canvasUrl)
                            .crossfade(500)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(canvasUrl)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            // Multi-colour ambient glow overlay over video canvas
            MovingAmbientLightBackground(
                primaryColor = primaryAmbient,
                secondaryColor = secondaryAmbient,
                modifier = Modifier.fillMaxSize(),
                alphaMultiplier = 0.35f,
            )
        } else {
            // Multi-color dynamic moving ambient light in the background according to album image
            MovingAmbientLightBackground(
                primaryColor = primaryAmbient,
                secondaryColor = secondaryAmbient,
                modifier = Modifier.fillMaxSize(),
                alphaMultiplier = 0.85f,
            )
        }

        // Protective contrast scrim ensuring controls remain perfectly readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (size.width <= 0f || size.height <= 0f) return@drawBehind
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.85f),
                            ),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                },
        )

        Column(
            modifier = Modifier
                .verticalScroll(state.mainScrollState, enabled = state.isExpanded)
                .fillMaxSize(),
        ) {
            // Header Top Bar: Downward chevron left, context/playlist name center, three dots right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onDismiss) {
                    Icon(
                        imageVector = PulseIcons.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp),
                    )
                }

                val contextTitle = state.screenData.playlistName.takeIf { it.isNotBlank() }
                    ?: state.artworkQueue.getOrNull(state.currentOrderIndex)?.album?.name
                    ?: stringResource(Res.string.now_playing_upper)
                Text(
                    text = contextTitle,
                    style = typo().titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )

                IconButton(onClick = actions.onShowMoreSheet) {
                    Icon(
                        imageVector = PulseIcons.MoreVert,
                        contentDescription = "More",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upper Metadata: Centered Track Title, Artist Name, and Timestamps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.screenData.nowPlayingTitle,
                    style = typo().headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        color = Color.White,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.screenData.artistName,
                    style = typo().bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.65f),
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { actions.onNavigateToArtist() },
                )

                Spacer(modifier = Modifier.height(8.dp))

                val currentDurationFormatted = formatDuration(
                    if (state.sliderValue > 0f && state.timelineState.total > 0L) {
                        ((state.sliderValue / 100f) * state.timelineState.total).roundToLong()
                    } else {
                        state.timelineState.current
                    },
                )
                val totalDurationFormatted = formatDuration(state.timelineState.total)
                Text(
                    text = "$currentDurationFormatted  |  $totalDurationFormatted",
                    style = typo().labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.50f),
                        letterSpacing = 0.5.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Circular Artwork Disc with Concentric Glowing Circular Scrubber Ring
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val outerSize = (maxWidth * 0.74f).coerceIn(240.dp, 300.dp)
                val innerArtworkSize = outerSize - 32.dp

                // Dynamic breathing ambient glow behind circular album image
                val breatheTransition = rememberInfiniteTransition(label = "cardAmbientGlow")
                val breatheScale by breatheTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "breatheScale",
                )

                Box(
                    modifier = Modifier
                        .size(outerSize)
                        .scale(breatheScale)
                        .drawBehind {
                            val r = size.width * 0.70f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryAmbient.copy(alpha = 0.55f),
                                        secondaryAmbient.copy(alpha = 0.25f),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = r,
                                ),
                                center = center,
                                radius = r,
                            )
                        },
                )

                // Progressive circular arc drawn around the round album image with interactive scrubbing
                val progressFraction = (state.sliderValue / 100f).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(120),
                    label = "circularProgressArc",
                )

                val strokeWidthPx = with(LocalDensity.current) { 3.dp.toPx() }
                val arcPadding = strokeWidthPx / 2f + with(LocalDensity.current) { 2.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .size(outerSize)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                fun calcProgress(pos: Offset): Float {
                                    val angle = Math.toDegrees(
                                        kotlin.math.atan2((pos.y - centerY).toDouble(), (pos.x - centerX).toDouble()),
                                    ).toFloat()
                                    val normalizedAngle = (angle + 90f).let { if (it < 0f) it + 360f else it }
                                    return (normalizedAngle / 360f).coerceIn(0f, 1f)
                                }
                                actions.onSliderChange(calcProgress(down.position) * 100f)
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.pressed) {
                                        change.consume()
                                        actions.onSliderChange(calcProgress(change.position) * 100f)
                                    } else {
                                        break
                                    }
                                }
                                actions.onSliderChangeFinished()
                            }
                        },
                ) {
                    val arcSize = Size(
                        size.width - arcPadding * 2,
                        size.height - arcPadding * 2,
                    )
                    val topLeft = Offset(arcPadding, arcPadding)

                    // Background Track Ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.18f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                        ),
                    )

                    // Active Progressive Arc
                    val sweepAngle = animatedProgress * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFFD59E),
                                    Color(0xFFFFB347),
                                    Color.White,
                                    Color(0xFFFFD59E),
                                ),
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round,
                            ),
                        )

                        // Glowing knob indicator at current progress position
                        val angleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                        val r = arcSize.width / 2f
                        val knobX = center.x + r * cos(angleRad)
                        val knobY = center.y + r * sin(angleRad)

                        // Outer halo glow
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = 7.dp.toPx(),
                            center = Offset(knobX, knobY),
                        )
                        // Inner solid knob
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(knobX, knobY),
                        )
                    }
                }

                // Smooth rotating vinyl disc animation like Material Expressive theme
                val isPlaying = state.controllerState.isPlaying
                val discRotation = remember { Animatable(0f) }
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (isActive) {
                            discRotation.animateTo(
                                targetValue = discRotation.value + 360f,
                                animationSpec = tween(durationMillis = 20000, easing = LinearEasing),
                            )
                        }
                    }
                }

                // Inner Artwork Disc / Canvas Video Container with tactile swipe gesture for song skipping
                // Live swipe offset drives the cover flow overlay
                val swipeOffsetAnim = remember { Animatable(0f) }
                val scope = rememberCoroutineScope()
                var accumulatedSwipeX by remember { mutableFloatStateOf(0f) }
                var isSwiping by remember { mutableStateOf(false) }
                val showCoverFlow by remember { derivedStateOf { abs(swipeOffsetAnim.value) > 8f } }
                Surface(
                    modifier = Modifier
                        .size(innerArtworkSize)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            spotColor = primaryAmbient,
                            ambientColor = secondaryAmbient,
                        )
                        .pointerInput(state.artworkQueue.size) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    accumulatedSwipeX = 0f
                                    isSwiping = true
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedSwipeX += dragAmount
                                    scope.launch {
                                        swipeOffsetAnim.snapTo(accumulatedSwipeX.coerceIn(-size.width.toFloat(), size.width.toFloat()))
                                    }
                                },
                                onDragEnd = {
                                    isSwiping = false
                                    if (accumulatedSwipeX < -40f) {
                                        actions.onUIEvent(UIEvent.Next)
                                    } else if (accumulatedSwipeX > 40f) {
                                        actions.onUIEvent(UIEvent.Previous)
                                    }
                                    scope.launch {
                                        swipeOffsetAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                    accumulatedSwipeX = 0f
                                },
                                onDragCancel = {
                                    isSwiping = false
                                    accumulatedSwipeX = 0f
                                    scope.launch {
                                        swipeOffsetAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                },
                            )
                        }
                        .graphicsLayer {
                            if (!(hasCanvas && isCanvasVideo)) {
                                rotationZ = discRotation.value % 360f
                            }
                            // Live 3D tilt while swiping - disc tilts with swipe direction
                            if (isSwiping) {
                                val tiltFraction = (swipeOffsetAnim.value / (size.width * 0.5f)).coerceIn(-1f, 1f)
                                rotationY = -tiltFraction * 25f
                                cameraDistance = 10f * density
                            }
                        },
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHigh,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (hasCanvas && isCanvasVideo && canvasUrl != null) {
                            MediaPlayerView(
                                url = canvasUrl,
                                cropToBounds = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (state.artworkQueue.isNotEmpty()) {
                            HorizontalPager(
                                state = state.artworkPagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                val pageOffset = ((state.artworkPagerState.currentPage - page) + state.artworkPagerState.currentPageOffsetFraction)
                                val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                                val track = state.artworkQueue.getOrNull(page)
                                val isCurrentArtworkPage = (page == state.currentOrderIndex || page == state.artworkPagerState.currentPage)
                                val thumbUrl = track?.thumbnails?.lastOrNull()?.url
                                    ?: (if (isCurrentArtworkPage) state.screenData.thumbnailURL else null)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            cameraDistance = 12 * density
                                            rotationY = -pageOffset.coerceIn(-1f, 1f) * 24f
                                            scaleX = 1f - (absOffset * 0.14f)
                                            scaleY = 1f - (absOffset * 0.14f)
                                            alpha = 1f - (absOffset * 0.28f)
                                        },
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalPlatformContext.current)
                                            .data(thumbUrl)
                                            .crossfade(200)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        contentDescription = track?.title ?: state.screenData.nowPlayingTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(state.screenData.thumbnailURL)
                                    .crossfade(200)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = state.screenData.nowPlayingTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Subtle vinyl turntable center spindle & grooved ring overlay
                        if (!(hasCanvas && isCanvasVideo)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val c = center
                                val maxR = size.width / 2f
                                for (i in 1..3) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.05f),
                                        radius = maxR * (0.35f + i * 0.18f),
                                        center = c,
                                        style = Stroke(width = 1.dp.toPx()),
                                    )
                                }
                                drawCircle(
                                    color = Color(0xFF101418),
                                    radius = 11.dp.toPx(),
                                    center = c,
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.40f),
                                    radius = 11.dp.toPx(),
                                    center = c,
                                    style = Stroke(width = 1.5.dp.toPx()),
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.90f),
                                    radius = 3.5.dp.toPx(),
                                    center = c,
                                )
                            }
                        }
                    }
                }

                // Apple Music-style Cover Flow Overlay â€” appears while swiping the disc
                // Shows neighbouring album cards fanning out in 3D perspective
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCoverFlow,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier
                        .size(maxWidth * 1.2f, outerSize * 1.6f)
                        .align(Alignment.Center),
                ) {
                    CoverFlowOverlay(
                        queue = state.artworkQueue,
                        currentIndex = state.currentOrderIndex,
                        swipeOffset = swipeOffsetAnim.value,
                        primaryAmbient = primaryAmbient,
                        cardWidth = innerArtworkSize * 0.72f,
                        cardHeight = outerSize * 1.1f,
                        currentThumbnailUrl = state.screenData.thumbnailURL,
                    )
                }

            } // end BoxWithConstraints

            Spacer(modifier = Modifier.height(18.dp))

            // Material Expressive 3 Progress Bar & Audio Quality Badge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val currentDurationFormatted = formatDuration(
                        if (state.sliderValue > 0f && state.timelineState.total > 0L) {
                            ((state.sliderValue / 100f) * state.timelineState.total).roundToLong()
                        } else {
                            state.timelineState.current
                        },
                    )
                    val totalDurationFormatted = formatDuration(state.timelineState.total)

                    Text(
                        text = currentDurationFormatted,
                        style = typo().labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f),
                        ),
                    )

                    // Audio Quality Badge: which quality is playing
                    val audioQualityText = when {
                        state.audioCodecLabel?.contains("flac", ignoreCase = true) == true -> "FLAC 24-BIT"
                        state.audioCodecLabel?.contains("lossless", ignoreCase = true) == true -> "LOSSLESS HD"
                        state.audioCodecLabel?.contains("opus", ignoreCase = true) == true -> "256 KBPS OPUS"
                        state.audioCodecLabel?.contains("aac", ignoreCase = true) == true -> "320 KBPS AAC"
                        !state.audioCodecLabel.isNullOrBlank() -> "${state.audioCodecLabel.uppercase()} HD"
                        else -> "320 KBPS AAC"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        ) {
                            Icon(
                                imageVector = PulseIcons.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFFFFD59E),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = audioQualityText,
                                style = typo().labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFD59E),
                                    letterSpacing = 0.8.sp,
                                ),
                            )
                        }
                    }

                    Text(
                        text = totalDurationFormatted,
                        style = typo().labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f),
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Material Expressive 3 squiggly/wavy progress slider bar
                MaterialExpressive3ProgressBar(
                    progress = (state.sliderValue / 100f).coerceIn(0f, 1f),
                    isPlaying = state.controllerState.isPlaying,
                    primaryColor = primaryAmbient,
                    secondaryColor = secondaryAmbient,
                    onSeek = { actions.onSliderChange(it * 100f) },
                    onSeekFinished = actions.onSliderChangeFinished,
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary 5-Button Transport Row: âŠ– |â—€ â–¶| â–¶| â™¥
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Remove / Skip / Dislike
                IconButton(
                    onClick = {
                        if (state.currentOrderIndex in state.artworkQueue.indices) {
                            actions.onRemoveQueueItem(state.currentOrderIndex)
                        }
                        actions.onUIEvent(UIEvent.Next)
                    },
                ) {
                    Icon(
                        imageVector = PulseIcons.RemoveCircleOutline,
                        contentDescription = "Hide or Skip",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // 2. Previous
                IconButton(
                    onClick = { actions.onUIEvent(UIEvent.Previous) },
                ) {
                    Icon(
                        imageVector = PulseIcons.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 3. Big Solid White Circular Play/Pause Button
                Surface(
                    onClick = { actions.onUIEvent(UIEvent.PlayPause) },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(66.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = if (state.controllerState.isPlaying) PulseIcons.Pause else PulseIcons.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                // 4. Next
                IconButton(
                    onClick = { actions.onUIEvent(UIEvent.Next) },
                ) {
                    Icon(
                        imageVector = PulseIcons.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 5. Heart / Like Button
                val heartBurstState = rememberHeartBurstState()
                IconButton(
                    onClick = {
                        if (!state.likeStatus) heartBurstState.fire()
                        actions.onUIEvent(UIEvent.ToggleLike)
                    },
                    modifier = Modifier.heartBurst(heartBurstState),
                ) {
                    Icon(
                        imageVector = if (state.likeStatus) PulseIcons.Favorite else PulseIcons.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (state.likeStatus) Color(0xFFFF5252) else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary 4-Button Actions Row: â¤® â‰¡ ðŸ”Š ðŸ”
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Shuffle
                IconButton(
                    onClick = { actions.onUIEvent(UIEvent.Shuffle) },
                ) {
                    Icon(
                        imageVector = PulseIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.controllerState.isShuffle) Color.White else Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 2. Queue
                IconButton(
                    onClick = actions.onShowQueue,
                ) {
                    Icon(
                        imageVector = PulseIcons.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 3. Audio Output / Volume
                val device = rememberCurrentAudioOutputDevice()
                val outputIcon = when {
                    device.isBluetooth -> PulseIcons.Bluetooth
                    device.isHeadphones -> PulseIcons.Headphones
                    else -> PulseIcons.VolumeUp
                }
                IconButton(
                    onClick = { /* audio output */ },
                ) {
                    Icon(
                        imageVector = outputIcon,
                        contentDescription = device.name,
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 4. Repeat
                val isRepeatOne = state.controllerState.repeatState is RepeatState.One
                val isRepeatActive = state.controllerState.repeatState !is RepeatState.None
                IconButton(
                    onClick = { actions.onUIEvent(UIEvent.Repeat) },
                ) {
                    Icon(
                        imageVector = if (isRepeatOne) PulseIcons.RepeatOne else PulseIcons.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) Color.White else Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Below-the-fold Expressive Cards: Lyrics with moving ambient glow and depth blur, Queue, Info
            ExpressiveBelowTheFold(
                state = state,
                actions = actions,
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Apple Music / iOS-style Cover Flow carousel that appears as an animated overlay while the user
 * swipes the album disc. Shows up to 5 cards (prev-prev, prev, current, next, next-next) in a
 * perspective-fan layout with 3D Y-axis rotation and depth scaling. The card layout mirrors the
 * reference image: large centre card, smaller angled side cards peaking from behind.
 */
@Composable
private fun CoverFlowOverlay(
    queue: List<com.manikandan.domain.data.model.browse.album.Track>,
    currentIndex: Int,
    swipeOffset: Float,
    primaryAmbient: Color,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    currentThumbnailUrl: String?,
) {
    // Normalise offset to [-1 .. 1] range where 1 = full width of card
    val density = LocalDensity.current
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val normOffset = (swipeOffset / (cardWidthPx * 1.4f)).coerceIn(-1f, 1f)

    // Dark blurred backdrop so the cover flow pops against the player UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * abs(normOffset))),
        contentAlignment = Alignment.Center,
    ) {
        // Render cards from -2 to +2 relative to current, back to front
        (-2..2).forEach { relIdx ->
            val track = queue.getOrNull(currentIndex + relIdx)
            val thumbUrl = track?.thumbnails?.lastOrNull()?.url
                ?: if (relIdx == 0) currentThumbnailUrl else null

            // Position: each card is shifted by its relative index Ã— card width Ã— spacing factor
            // The swipe offset shifts all cards so the dragging feels like the carousel moving
            val baseTranslateX = (relIdx.toFloat() - normOffset) * cardWidthPx * 0.78f
            // 3D rotation: neighbouring cards are angled inward; more distant = more tilted
            val rotY = when (relIdx) {
                0  -> -normOffset * 20f                 // current card tilts with drag
                1, -1 -> (relIdx * 38f) + (-normOffset * 10f)
                else   -> relIdx * 55f
            }
            // Scale: front card is 100%, each step back is smaller
            val scale = 1f - (abs(relIdx.toFloat() - normOffset).coerceIn(0f, 2f) * 0.17f)
            // Alpha: fade out distant cards
            val alpha = (1f - abs(relIdx.toFloat() - normOffset).coerceIn(0f, 2f) * 0.35f).coerceIn(0f, 1f)
            // Elevation (z-order): current card is on top
            val zOffset = (2f - abs(relIdx.toFloat() - normOffset)).coerceIn(0f, 2f)

            if (thumbUrl != null || relIdx == 0) {
                Box(
                    modifier = Modifier
                        .size(cardWidth, cardHeight)
                        .graphicsLayer {
                            translationX = baseTranslateX
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            cameraDistance = 10f * density.density
                            rotationY = rotY
                            shadowElevation = zOffset * 20f
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .shadow(
                            elevation = (zOffset * 12f).dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = primaryAmbient.copy(alpha = 0.4f),
                            spotColor = primaryAmbient.copy(alpha = 0.6f),
                        ),
                ) {
                    if (thumbUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(thumbUrl)
                                .crossfade(200)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = track?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(primaryAmbient.copy(alpha = 0.3f)),
                        )
                    }
                    // Track name label at bottom of each card
                    if (track != null && scale > 0.65f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = cardHeight.value * 0.55f,
                                    ),
                                ),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                            ) {
                                Text(
                                    text = track.title ?: "",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                if (relIdx == 0) {
                                    Text(
                                        text = track.artists?.firstOrNull()?.name ?: "",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 10.sp,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive squiggly wave progress bar with interactive scrubbing,
 * dynamic undulating wave when playing, smooth straightening when paused,
 * and glowing scrub knob.
 */
@Composable
private fun MaterialExpressive3ProgressBar(
    progress: Float,
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "expressiveProgress",
    )

    val waveTransition = rememberInfiniteTransition(label = "expressiveWaveTransition")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "expressiveWavePhase",
    )

    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "expressiveWaveAmplitude",
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun update(x: Float) {
                    val p = (x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(p)
                }
                update(down.position.x)
                down.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.pressed) {
                        change.consume()
                        update(change.position.x)
                    } else {
                        break
                    }
                }
                onSeekFinished()
            }
        },
    ) {
        val trackHeight = 5.dp.toPx()
        val centerY = size.height / 2f
        val w = size.width
        val activeW = (w * animatedProgress).coerceIn(0f, w)

        // Inactive background track
        drawLine(
            color = Color.White.copy(alpha = 0.20f),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round,
        )

        // Active track with expressive squiggly wave when playing
        if (activeW > 0f) {
            val path = Path()
            val ampPx = 3.5.dp.toPx() * waveAmplitude
            val wavelengthPx = 28.dp.toPx()
            path.moveTo(0f, centerY)

            var x = 0f
            val step = 3f
            while (x <= activeW) {
                val waveY = centerY + sin((x / wavelengthPx) * 2f * kotlin.math.PI.toFloat() - wavePhase) * ampPx
                path.lineTo(x, waveY)
                x += step
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFD59E),
                        Color(0xFFFFB347),
                        Color.White,
                    ),
                    startX = 0f,
                    endX = activeW.coerceAtLeast(1f),
                ),
                style = Stroke(
                    width = trackHeight,
                    cap = StrokeCap.Round,
                ),
            )

            // Expressive thumb knob at current position
            val thumbY = centerY + sin((activeW / wavelengthPx) * 2f * kotlin.math.PI.toFloat() - wavePhase) * ampPx
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = Offset(activeW, thumbY),
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(activeW, thumbY),
            )
        }
    }
}
