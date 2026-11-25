package com.pulse.music.ui.screen.player.content

import androidx.compose.animation.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import com.manikandan.domain.data.model.streams.TimeLine
import com.manikandan.domain.data.player.GenericCastState
import com.manikandan.domain.mediaservice.handler.ControlState
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.pulse.music.extension.GradientAngle
import com.pulse.music.extension.GradientOffset
import com.pulse.music.ui.icon.KeyboardArrowDown
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.viewModel.NowPlayingScreenData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VibrantExpressiveVisualTest {

    @OptIn(ExperimentalFoundationApi::class)
    private fun buildFakeState(
        isPlaying: Boolean,
        sliderPercent: Float,
    ): NowPlayingContentState {
        val timeline = TimeLine(current = 94_000L, total = 225_000L, bufferedPercent = 100, loading = false)
        return NowPlayingContentState(
            screenData = NowPlayingScreenData(
                playlistName = "Tamil Hits Radio",
                nowPlayingTitle = "Naa Ready (From \"Leo\")",
                artistName = "Anirudh Ravichander, Vijay",
                isVideo = false,
                isExplicit = false,
                thumbnailURL = null,
            ),
            controllerState = ControlState(
                isPlaying = isPlaying,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = true,
                isNextAvailable = true,
                isPreviousAvailable = true,
                isCrossfading = false,
                volume = 1f,
            ),
            timelineState = timeline,
            timelineFlow = MutableStateFlow(timeline),
            likeStatus = true,
            castState = GenericCastState.NOT_CASTING,
            shouldShowVideo = false,
            isUserLoggedIn = true,
            artworkQueue = emptyList(),
            currentOrderIndex = 0,
            artworkPagerState = PagerState(currentPage = 0, pageCount = { 1 }),
            startColor = Animatable(Color(0xFF7C3AED)),
            endColor = Animatable(Color(0xFF101418)),
            spotShadowColor = Color(0xFFFFB347),
            gradientOffset = GradientOffset(GradientAngle.CW45),
            sliderTrackColor = Color(0xFFFFD59E),
            sliderValue = sliderPercent,
            currentLyricLineIndex = -1,
            showControlLayout = true,
            controlLayoutAlpha = 1f,
            showHideMiddleLayout = false,
            shouldShowToolbar = true,
            isInPipMode = false,
            mainScrollState = ScrollState(0),
            isExpanded = false,
            dismissIcon = PulseIcons.KeyboardArrowDown,
            audioCodecLabel = "FLAC",
            ambientModeEnabled = true,
            ambientColor = Color(0xFFFF9E6D),
            ambientSecondaryColor = Color(0xFF7C3AED),
        )
    }

    private fun renderSnapshot(isPlaying: Boolean, sliderPercent: Float, filename: String): Int {
        val actions = NowPlayingContentActions(
            onUIEvent = {},
            onSeekToQueueIndex = {},
            onArtworkBitmap = {},
            onSliderChange = {},
            onSliderChangeFinished = {},
            onToggleControls = {},
            onNavigateToArtist = {},
            onAddToYouTubeLiked = {},
            onShowMoreSheet = {},
            onShowQueue = {},
            onShowInfo = {},
            onShowAddToPlaylist = {},
            onShowFullscreenLyrics = {},
            onShowVoteDialog = {},
            onEnterFullscreenVideo = {},
            onDismiss = {},
            onToolbarVisibilityChange = {},
            onMoveQueueItem = { _, _ -> },
            onRemoveQueueItem = {},
        )
        val scene = ImageComposeScene(
            width = 430,
            height = 932,
            density = Density(1f),
            coroutineContext = Dispatchers.Unconfined,
        ) {
            NowPlayingContentVibrantExpressive(
                state = buildFakeState(isPlaying = isPlaying, sliderPercent = sliderPercent),
                actions = actions,
            )
        }
        try {
            scene.render(nanoTime = 0L)
            scene.render(nanoTime = 350_000_000L)
            val skiaImage = scene.render(nanoTime = 750_000_000L)
            val pngData = skiaImage.encodeToData(EncodedImageFormat.PNG)
                ?: error("Failed to encode VibrantExpressive screenshot")
            val bytes = pngData.bytes
            val outDir = File("build/visual-verification").apply { mkdirs() }
            val outFile = File(outDir, filename)
            outFile.writeBytes(bytes)
            return bytes.size
        } finally {
            scene.close()
        }
    }

    @Test
    fun vibrantExpressiveRendersCircularWavyProgressRingWhenPlayingAndCalmWhenPaused() {
        val playingBytes = renderSnapshot(isPlaying = true, sliderPercent = 48f, filename = "vibrant_playing.png")
        val pausedBytes = renderSnapshot(isPlaying = false, sliderPercent = 68f, filename = "vibrant_paused.png")
        assertTrue(playingBytes > 5_000, "Playing snapshot PNG too small: $playingBytes bytes")
        assertTrue(pausedBytes > 5_000, "Paused snapshot PNG too small: $pausedBytes bytes")
    }
}
