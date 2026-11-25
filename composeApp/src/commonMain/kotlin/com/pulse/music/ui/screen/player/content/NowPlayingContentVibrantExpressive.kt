package com.pulse.music.ui.screen.player.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.pulse.music.ui.component.PixelMotion
import com.pulse.music.ui.component.pixelClickable
import com.pulse.music.ui.component.pixelPopOnChange
import com.pulse.music.ui.component.pixelTapScale
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
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
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
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
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
                AnimatedContent(
                    targetState = state.screenData.nowPlayingTitle to state.screenData.artistName,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    contentAlignment = Alignment.Center,
                    label = "vibrantTrackMeta",
                ) { (trackTitle, artistTitle) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trackTitle,
                            style = typo().headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = Color.White,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = artistTitle,
                            style = typo().bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.65f),
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .pixelClickable(pressedScale = 0.95f) { actions.onNavigateToArtist() }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }

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
                val outerSize = (maxWidth * 0.78f).coerceIn(250.dp, 316.dp)
                val innerArtworkSize = outerSize - 44.dp

                // Dynamic breathing ambient glow behind circular album image
                val breatheTransition = rememberInfiniteTransition(label = "cardAmbientGlow")
                val breatheScale by breatheTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.07f,
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

                val isPlaying = state.controllerState.isPlaying

                // Stationary circular album artwork / Canvas video (no rotation or horizontal pager scroll)
                Surface(
                    modifier = Modifier
                        .size(innerArtworkSize)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            spotColor = primaryAmbient,
                            ambientColor = secondaryAmbient,
                        ),
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
                        } else {
                            Crossfade(
                                targetState = state.screenData.thumbnailURL,
                                animationSpec = tween(260),
                                label = "vibrantArtworkFade",
                            ) { artworkUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(artworkUrl)
                                        .crossfade(200)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = state.screenData.nowPlayingTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }

                // Material 3 Expressive Wavy Circular Progress Ring around circular album image
                val externalProgress = (state.sliderValue / 100f).coerceIn(0f, 1f)
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubProgress by remember { mutableFloatStateOf(externalProgress) }
                val ringHaptic = LocalHapticFeedback.current

                val animatedProgress by animateFloatAsState(
                    targetValue = if (isScrubbing) scrubProgress else externalProgress,
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    label = "circularExpressiveProgress",
                )
                val renderProgress = if (isScrubbing) scrubProgress else animatedProgress

                val wavePhaseTransition = rememberInfiniteTransition(label = "vibrantCircularWave")
                val wavePhase by wavePhaseTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2.0 * PI).toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "vibrantCircularWavePhase",
                )
                val waveAmpFactor by animateFloatAsState(
                    targetValue = if (isPlaying && !isScrubbing) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "vibrantCircularWaveAmp",
                )
                val knobRadiusDp by animateDpAsState(
                    targetValue = if (isScrubbing) 6.5.dp else 4.8.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "vibrantCircularKnobRadius",
                )

                val density = LocalDensity.current
                val strokeWidthPx = with(density) { 4.dp.toPx() }
                val inactiveStrokePx = with(density) { 3.dp.toPx() }
                val waveMaxAmpPx = with(density) { 3.2.dp.toPx() }
                val outerInsetPx = with(density) { 11.dp.toPx() }
                val minTouchRadiusPx = with(density) { (innerArtworkSize / 2f - 8.dp).toPx() }

                Canvas(
                    modifier = Modifier
                        .size(outerSize)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val distFromCenter = hypot(
                                    (down.position.x - centerX).toDouble(),
                                    (down.position.y - centerY).toDouble(),
                                ).toFloat()
                                // Ignore inner center taps so scrolling or pressing artwork doesn't accidentally seek
                                if (distFromCenter < minTouchRadiusPx) return@awaitEachGesture

                                fun calcProgress(pos: Offset): Float {
                                    val angleDeg = (atan2(
                                        (pos.y - centerY).toDouble(),
                                        (pos.x - centerX).toDouble(),
                                    ) * 180.0 / PI).toFloat()
                                    val normalized = ((angleDeg + 90f) % 360f).let { if (it < 0f) it + 360f else it }
                                    return (normalized / 360f).coerceIn(0f, 1f)
                                }

                                isScrubbing = true
                                var lastHapticBucket = -1
                                try {
                                    val startP = calcProgress(down.position)
                                    scrubProgress = startP
                                    lastHapticBucket = (startP * 50f).toInt()
                                    ringHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    actions.onSliderChange(startP * 100f)
                                    down.consume()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.pressed) {
                                            change.consume()
                                            val newP = calcProgress(change.position)
                                            scrubProgress = newP
                                            val bucket = (newP * 50f).toInt()
                                            if (bucket != lastHapticBucket) {
                                                lastHapticBucket = bucket
                                                ringHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            actions.onSliderChange(newP * 100f)
                                        } else {
                                            break
                                        }
                                    }
                                } finally {
                                    isScrubbing = false
                                    actions.onSliderChangeFinished()
                                }
                            }
                        },
                ) {
                    val baseRadius = (size.minDimension / 2f) - outerInsetPx
                    val sweepDeg = (renderProgress * 360f).coerceIn(0f, 360f)

                    // 1. Remaining inactive circular track with expressive cap gaps around active arc
                    val knobGapDeg = if (sweepDeg > 0.5f && sweepDeg < 353f) 5.5f else 0f
                    val startGapDeg = if (sweepDeg > 0.5f && sweepDeg < 353f) 3.5f else 0f
                    val remainingStartDeg = -90f + sweepDeg + knobGapDeg
                    val remainingSweepDeg = (360f - sweepDeg - knobGapDeg - startGapDeg).coerceAtLeast(0f)

                    if (sweepDeg <= 0.5f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = baseRadius,
                            center = center,
                            style = Stroke(width = inactiveStrokePx),
                        )
                    } else if (remainingSweepDeg > 0.5f) {
                        val arcTopLeft = Offset(center.x - baseRadius, center.y - baseRadius)
                        val arcSize = Size(baseRadius * 2f, baseRadius * 2f)
                        drawArc(
                            color = Color.White.copy(alpha = 0.18f),
                            startAngle = remainingStartDeg,
                            sweepAngle = remainingSweepDeg,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = inactiveStrokePx, cap = StrokeCap.Round),
                        )
                    }

                    // 2. Material 3 Expressive Wavy Active Ring
                    if (sweepDeg > 0.5f) {
                        val wavePath = Path()
                        val waveCycles = 16
                        val stepDeg = 1.25f
                        val steps = ceil((sweepDeg / stepDeg).toDouble()).toInt().coerceAtLeast(2)
                        val taperWindowDeg = 10f

                        for (i in 0..steps) {
                            val frac = i.toFloat() / steps.toFloat()
                            val currentDeg = -90f + sweepDeg * frac
                            val angleRad = currentDeg * PI / 180.0

                            val distFromStart = sweepDeg * frac
                            val distFromEnd = sweepDeg * (1f - frac)
                            val edgeTaper = minOf(
                                (distFromStart / taperWindowDeg),
                                (distFromEnd / taperWindowDeg),
                                1f,
                            ).coerceIn(0f, 1f)
                            val smoothTaper = edgeTaper * edgeTaper * (3f - 2f * edgeTaper)

                            val waveOffset = sin(angleRad * waveCycles - wavePhase).toFloat() *
                                waveMaxAmpPx * waveAmpFactor * smoothTaper
                            val r = baseRadius + waveOffset
                            val px = center.x + r * cos(angleRad).toFloat()
                            val py = center.y + r * sin(angleRad).toFloat()
                            if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                        }

                        val activeRingBrush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD59E),
                                Color(0xFFFFB347),
                                Color.White,
                                Color(0xFFFFB347),
                                Color(0xFFFFD59E),
                            ),
                        )

                        // Soft outer glow behind wavy active arc
                        drawPath(
                            path = wavePath,
                            color = Color(0xFFFFB347).copy(alpha = 0.24f),
                            style = Stroke(
                                width = strokeWidthPx * 2.1f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )

                        // Main luminous Material Expressive wavy path
                        drawPath(
                            path = wavePath,
                            brush = activeRingBrush,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }

                    // 3. Morphing glowing scrub thumb at current angle
                    val knobAngleRad = (-90f + sweepDeg) * PI / 180.0
                    val knobX = center.x + baseRadius * cos(knobAngleRad).toFloat()
                    val knobY = center.y + baseRadius * sin(knobAngleRad).toFloat()
                    val knobRadiusPx = knobRadiusDp.toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = if (isScrubbing) 0.42f else 0.28f),
                        radius = knobRadiusPx * 1.85f,
                        center = Offset(knobX, knobY),
                    )
                    drawCircle(
                        color = Color.White,
                        radius = knobRadiusPx,
                        center = Offset(knobX, knobY),
                    )
                }
            } // end BoxWithConstraints

            Spacer(modifier = Modifier.height(14.dp))

            // Compact centered Audio Quality Badge (progress ring sits around circular album art)
            val audioQualityText = when {
                state.audioCodecLabel?.contains("flac", ignoreCase = true) == true -> "FLAC 24-BIT"
                state.audioCodecLabel?.contains("lossless", ignoreCase = true) == true -> "LOSSLESS HD"
                state.audioCodecLabel?.contains("opus", ignoreCase = true) == true -> "256 KBPS OPUS"
                state.audioCodecLabel?.contains("aac", ignoreCase = true) == true -> "320 KBPS AAC"
                !state.audioCodecLabel.isNullOrBlank() -> "${state.audioCodecLabel.uppercase()} HD"
                else -> "320 KBPS AAC"
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary 5-Button Transport Row: âŠ– |â—€ â–¶| â–¶| â™¥
            val controlHaptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Remove / Skip / Dislike
                val skipInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (state.currentOrderIndex in state.artworkQueue.indices) {
                            actions.onRemoveQueueItem(state.currentOrderIndex)
                        }
                        actions.onUIEvent(UIEvent.Next)
                    },
                    interactionSource = skipInteraction,
                    modifier = Modifier.pixelTapScale(skipInteraction, pressedScale = 0.84f, hoveredScale = 1.08f),
                ) {
                    Icon(
                        imageVector = PulseIcons.RemoveCircleOutline,
                        contentDescription = "Hide or Skip",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // 2. Previous
                val prevInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onUIEvent(UIEvent.Previous)
                    },
                    interactionSource = prevInteraction,
                    modifier = Modifier.pixelTapScale(prevInteraction, pressedScale = 0.84f, hoveredScale = 1.08f),
                ) {
                    Icon(
                        imageVector = PulseIcons.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 3. Big Solid White Pixel Morphing Play/Pause Button
                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playCorner by animateDpAsState(
                    targetValue = when {
                        playPressed -> 16.dp
                        state.controllerState.isPlaying -> 22.dp
                        else -> 33.dp
                    },
                    animationSpec = PixelMotion.SpringDpBouncy,
                    label = "vibrantPlayCorner",
                )
                Surface(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onUIEvent(UIEvent.PlayPause)
                    },
                    interactionSource = playInteraction,
                    shape = RoundedCornerShape(playCorner),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(66.dp)
                        .pixelTapScale(playInteraction, pressedScale = 0.90f, hoveredScale = 1.05f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AnimatedContent(
                            targetState = state.controllerState.isPlaying,
                            transitionSpec = { PixelMotion.iconMorphTransition() },
                            contentAlignment = Alignment.Center,
                            label = "vibrantPlayMorph",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) PulseIcons.Pause else PulseIcons.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                // 4. Next
                val nextInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onUIEvent(UIEvent.Next)
                    },
                    interactionSource = nextInteraction,
                    modifier = Modifier.pixelTapScale(nextInteraction, pressedScale = 0.84f, hoveredScale = 1.08f),
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
                val likeInteraction = remember { MutableInteractionSource() }
                val likeTint by animateColorAsState(
                    targetValue = if (state.likeStatus) Color(0xFFFF5252) else Color.White.copy(alpha = 0.70f),
                    animationSpec = tween(220),
                    label = "vibrantLikeTint",
                )
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (!state.likeStatus) heartBurstState.fire()
                        actions.onUIEvent(UIEvent.ToggleLike)
                    },
                    interactionSource = likeInteraction,
                    modifier = Modifier
                        .heartBurst(heartBurstState)
                        .pixelTapScale(likeInteraction, pressedScale = 0.80f, hoveredScale = 1.10f)
                        .pixelPopOnChange(state.likeStatus, peakScale = 1.24f, onlyWhen = state.likeStatus),
                ) {
                    AnimatedContent(
                        targetState = state.likeStatus,
                        transitionSpec = { PixelMotion.iconMorphTransition() },
                        contentAlignment = Alignment.Center,
                        label = "vibrantLikeMorph",
                    ) { liked ->
                        Icon(
                            imageVector = if (liked) PulseIcons.Favorite else PulseIcons.FavoriteBorder,
                            contentDescription = "Like",
                            tint = likeTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
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
                val shuffleInteraction = remember { MutableInteractionSource() }
                val shuffleTint by animateColorAsState(
                    targetValue = if (state.controllerState.isShuffle) Color.White else Color.White.copy(alpha = 0.40f),
                    animationSpec = tween(220),
                    label = "vibrantShuffleTint",
                )
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onUIEvent(UIEvent.Shuffle)
                    },
                    interactionSource = shuffleInteraction,
                    modifier = Modifier
                        .pixelTapScale(shuffleInteraction, pressedScale = 0.84f, hoveredScale = 1.08f)
                        .pixelPopOnChange(state.controllerState.isShuffle, peakScale = 1.2f, onlyWhen = state.controllerState.isShuffle),
                ) {
                    Icon(
                        imageVector = PulseIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = shuffleTint,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 2. Queue
                val queueInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onShowQueue()
                    },
                    interactionSource = queueInteraction,
                    modifier = Modifier.pixelTapScale(queueInteraction, pressedScale = 0.84f, hoveredScale = 1.08f),
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
                val outputInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    interactionSource = outputInteraction,
                    modifier = Modifier.pixelTapScale(outputInteraction, pressedScale = 0.84f, hoveredScale = 1.08f),
                ) {
                    Icon(
                        imageVector = outputIcon,
                        contentDescription = device.name,
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 4. Repeat
                val isRepeatActive = state.controllerState.repeatState !is RepeatState.None
                val repeatInteraction = remember { MutableInteractionSource() }
                val repeatTint by animateColorAsState(
                    targetValue = if (isRepeatActive) Color.White else Color.White.copy(alpha = 0.40f),
                    animationSpec = tween(220),
                    label = "vibrantRepeatTint",
                )
                IconButton(
                    onClick = {
                        controlHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onUIEvent(UIEvent.Repeat)
                    },
                    interactionSource = repeatInteraction,
                    modifier = Modifier
                        .pixelTapScale(repeatInteraction, pressedScale = 0.84f, hoveredScale = 1.08f)
                        .pixelPopOnChange(state.controllerState.repeatState, peakScale = 1.2f, onlyWhen = isRepeatActive),
                ) {
                    AnimatedContent(
                        targetState = state.controllerState.repeatState,
                        transitionSpec = { PixelMotion.iconMorphTransition() },
                        contentAlignment = Alignment.Center,
                        label = "vibrantRepeatMorph",
                    ) { rs ->
                        Icon(
                            imageVector = if (rs is RepeatState.One) PulseIcons.RepeatOne else PulseIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = repeatTint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
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
