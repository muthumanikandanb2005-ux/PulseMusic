package com.pulse.music.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.pulse.music.extension.formatDuration
import com.pulse.music.extension.getScreenSizeInfo
import com.pulse.music.ui.component.AudioOutputDeviceBadge
import com.pulse.music.ui.component.MovingAmbientLightBackground
import com.pulse.music.ui.component.heartBurst
import com.pulse.music.ui.component.rememberHeartBurstState
import com.pulse.music.ui.icon.Favorite
import com.pulse.music.ui.icon.FavoriteBorder
import com.pulse.music.ui.icon.MoreVert
import com.pulse.music.ui.icon.PlaylistAdd
import com.pulse.music.ui.icon.QueueMusic
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.screen.player.content.expressive.ExpressiveTransportRow
import com.pulse.music.ui.screen.player.content.expressive.WavySeekBar
import com.pulse.music.ui.theme.typo
import com.pulse.music.viewModel.UIEvent
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource
import pulsemusic.composeapp.generated.resources.Res
import pulsemusic.composeapp.generated.resources.now_playing_upper

/**
 * Vibrant Expressive Now Playing style:
 * Inspired by the Behance modern music player UI design with Material 3 Expressive theme,
 * dynamic multi-colour moving ambient light background in the backdrop and lyrics,
 * elevated squircle artwork, bouncy expressive controls, and lyrics depth blur.
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
    val screenInfo = getScreenSizeInfo()
    val colorScheme = MaterialTheme.colorScheme

    val primaryAmbient = if (state.ambientColor.isSpecified && state.ambientColor != Color.Transparent && state.ambientColor != Color.Black) {
        state.ambientColor
    } else if (state.startColor.value.isSpecified && state.startColor.value != Color.Black) {
        state.startColor.value
    } else {
        colorScheme.primary
    }

    val secondaryAmbient = if (state.ambientSecondaryColor.isSpecified && state.ambientSecondaryColor != Color.Transparent && state.ambientSecondaryColor != Color.Black) {
        state.ambientSecondaryColor
    } else {
        colorScheme.secondary
    }

    var topAppBarHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var middleLayoutHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var infoLayoutHeightDp by rememberSaveable { mutableIntStateOf(0) }
    var middleLayoutPaddingDp by rememberSaveable { mutableIntStateOf(20) }

    LaunchedEffect(topAppBarHeightDp, middleLayoutHeightDp, infoLayoutHeightDp, screenInfo) {
        if (topAppBarHeightDp > 0 && middleLayoutHeightDp > 0 && infoLayoutHeightDp > 0 && screenInfo.hDP > 0) {
            val result = (screenInfo.hDP - topAppBarHeightDp - middleLayoutHeightDp - infoLayoutHeightDp - 20) / 2
            middleLayoutPaddingDp = if (result > 20) result else 20
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Multi-color moving ambient light in the background according to album image
        if (state.ambientModeEnabled) {
            MovingAmbientLightBackground(
                primaryColor = primaryAmbient,
                secondaryColor = secondaryAmbient,
                modifier = Modifier.fillMaxSize(),
                alphaMultiplier = 0.70f,
            )
        }

        // Protective bottom contrast scrim ensuring controls remain perfectly readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (size.width <= 0f || size.height <= 0f) return@drawBehind
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f),
                            ),
                            startY = size.height * 0.45f,
                            endY = size.height,
                        ),
                    )
                },
        )

        Column(
            Modifier
                .verticalScroll(state.mainScrollState, enabled = state.isExpanded)
                .fillMaxSize(),
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onDismiss) {
                    Icon(
                        imageVector = state.dismissIcon,
                        contentDescription = "Dismiss",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.now_playing_upper),
                        style = typo().labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colorScheme.primary,
                    )
                    val subtitle = state.artworkQueue.getOrNull(state.currentOrderIndex)?.album?.name
                        ?: state.screenData.playlistName.takeIf { it.isNotBlank() }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = typo().bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = actions.onShowMoreSheet) {
                        Icon(
                            imageVector = PulseIcons.MoreVert,
                            contentDescription = "More",
                            tint = colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(middleLayoutPaddingDp.dp))

            // Center Elevated Circular Artwork Card with Progressive Around It & Queue Swipe Pager
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val outerSize = maxWidth.coerceAtMost(330.dp)
                val innerArtworkSize = outerSize - 28.dp

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
                            val r = size.width * 0.72f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryAmbient.copy(alpha = 0.60f),
                                        secondaryAmbient.copy(alpha = 0.30f),
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

                // Progressive circular arc drawn around the round album image
                val progressFraction = (state.sliderValue / 100f).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(150),
                    label = "circularProgressArc",
                )

                val ringTrackColor = colorScheme.surfaceVariant.copy(alpha = 0.35f)
                val primaryColor = colorScheme.primary
                val tertiaryColor = colorScheme.tertiary

                Canvas(
                    modifier = Modifier.size(outerSize),
                ) {
                    val strokeWidthPx = 6.dp.toPx()
                    val arcPadding = strokeWidthPx / 2f + 2.dp.toPx()
                    val arcSize = Size(
                        size.width - arcPadding * 2,
                        size.height - arcPadding * 2,
                    )
                    val topLeft = Offset(arcPadding, arcPadding)

                    // Background Track Ring
                    drawArc(
                        color = ringTrackColor,
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
                                    primaryColor,
                                    primaryAmbient,
                                    secondaryAmbient,
                                    tertiaryColor,
                                    primaryColor,
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

                        // Glowing head indicator knob at current angle
                        val angleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                        val r = arcSize.width / 2f
                        val knobX = center.x + r * cos(angleRad)
                        val knobY = center.y + r * sin(angleRad)

                        drawCircle(
                            color = Color.White,
                            radius = strokeWidthPx * 0.9f,
                            center = Offset(knobX, knobY),
                        )
                    }
                }

                // Round Circle Album Image Theme
                Surface(
                    modifier = Modifier
                        .size(innerArtworkSize)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            spotColor = primaryAmbient,
                            ambientColor = secondaryAmbient,
                        ),
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHigh,
                ) {
                    HorizontalPager(
                        state = state.artworkPagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val track = state.artworkQueue.getOrNull(page)
                        val thumbUrl = track?.thumbnails?.lastOrNull()?.url
                            ?: state.screenData.thumbnailURL

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
            }

            Spacer(modifier = Modifier.height(middleLayoutPaddingDp.dp))

            // Track Details & Actions Row
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.screenData.nowPlayingTitle,
                            style = typo().headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.screenData.artistName,
                            style = typo().bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { actions.onNavigateToArtist() },
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Heart Like Button with pulse burst animation
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
                                tint = if (state.likeStatus) Color(0xFFFF4081) else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        // Add to Playlist Button
                        IconButton(onClick = actions.onShowAddToPlaylist) {
                            Icon(
                                imageVector = PulseIcons.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expressive Seek Bar & Timestamps
                WavySeekBar(
                    progressFraction = state.sliderValue / 100f,
                    isPlaying = state.controllerState.isPlaying,
                    activeColor = colorScheme.primary,
                    trackColor = colorScheme.secondaryContainer,
                    thumbColor = colorScheme.primary,
                    onSliderChange = actions.onSliderChange,
                    onSliderChangeFinished = actions.onSliderChangeFinished,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().offset(y = (-8).dp).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(
                            if (state.sliderValue > 0f && state.timelineState.total > 0L) {
                                ((state.sliderValue / 100f) * state.timelineState.total).roundToLong()
                            } else {
                                state.timelineState.current
                            },
                        ),
                        style = typo().labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(state.timelineState.total),
                        style = typo().labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expressive Transport Controls Row
                ExpressiveTransportRow(
                    controllerState = state.controllerState,
                    loading = state.timelineState.loading,
                    onUIEvent = actions.onUIEvent,
                    modifier = Modifier.fillMaxWidth(),
                    showShuffleAndRepeat = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Quick Action Badges
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AudioOutputDeviceBadge()

                    state.audioCodecLabel?.let { codec ->
                        val isFlac = codec.contains("flac", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFlac) Color(0xFFFFD700).copy(alpha = 0.20f) else colorScheme.surfaceContainerHighest,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = codec,
                                style = typo().labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isFlac) Color(0xFFFFD700) else colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    IconButton(onClick = actions.onShowQueue) {
                        Icon(
                            imageVector = PulseIcons.QueueMusic,
                            contentDescription = "Queue",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
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
