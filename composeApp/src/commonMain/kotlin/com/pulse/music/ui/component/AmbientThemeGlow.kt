package com.pulse.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.pulse.music.Platform
import com.pulse.music.extension.angledGradientBackground
import com.pulse.music.extension.artworkScrimBrush
import com.pulse.music.extension.rgbFactor
import com.pulse.music.getPlatform
import com.pulse.music.ui.theme.desktopPanelDark
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url

/**
 * Home's ambient top glow for pages that have no artwork of their own: the theme's primary tone
 * fading into the page across the top 360dp, drawn with the same angled-gradient-plus-bottom-scrim
 * recipe HomeScreen paints under its first shelf (light theme lifts the tone toward white for a
 * pastel; dark keeps it deep via rgbFactor). Static, since the theme colour does not move.
 *
 * Emit it as the FIRST sibling of the screen's content: inside the nav host every destination
 * composes into a Box, so an earlier sibling is simply the layer underneath — no wrapper needed.
 *
 * The gradient tail aims at what is ACTUALLY painted behind the screen. The desktop shell wraps
 * content in its own panel colour rather than colorScheme.background, and a tail aimed at the
 * wrong one ends on a hard seam where the glow stops — see HomeScreen's pageBackground note.
 *
 * Screens that need more than this stay hand-rolled on purpose: Mix for you animates the tone from
 * its first playlist's artwork, and the Listen Together pair paint the glow inside a liquid-glass
 * backdrop source so their floating back button has something to refract.
 */
/** How tall the glow is — callers gate their bar frost on "has this fully scrolled past". */
val AmbientGlowHeight = 360.dp

@Composable
fun AmbientThemeGlow(
    modifier: Modifier = Modifier,
    // The RAW tone to glow with — an artwork colour (see [rememberNowPlayingGlowTint]) or the
    // theme primary, handed in as-is; light/dark shaping happens here. Null means NO GLOW: the
    // gradient collapses into the page colour, so the layer is simply invisible until a tone
    // arrives — and the animation below is what makes it breathe in rather than pop.
    tint: Color? = null,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    // Animated so a track change breathes to the new tone instead of snapping — Home's 500ms.
    val glow by animateColorAsState(
        targetValue =
            when {
                tint == null -> pageBackground
                isLightTheme -> lerp(tint, Color.White, 0.85f)
                // 0.45, not Home's 0.3: Home multiplies a mid-saturated ARTWORK colour, while a
                // pastel primary handed in here at 0.3 read as near-black.
                else -> tint.rgbFactor(0.45f)
            },
        animationSpec = tween(500),
        label = "ambientGlow",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(AmbientGlowHeight)
            .angledGradientBackground(listOf(glow, pageBackground), 25f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(artworkScrimBrush(pageBackground)),
        )
    }
}

/**
 * The dominant colour of the now-playing artwork, for feeding [AmbientThemeGlow]'s tint.
 *
 * The same DominantColorState + network-loader machinery HomeScreen runs on its main thumbnail.
 * Null while nothing is playing AND while the artwork has not resolved yet — deliberately not a
 * theme-colour fallback, so a silent app shows a plain page rather than a stand-in glow.
 * `Color.Unspecified` is the sentinel for "not resolved": the state must start somewhere, and any
 * real colour would flash as a wrong glow for the first frames.
 */
@Composable
fun rememberNowPlayingGlowTint(thumbnailUrl: String?): Color? {
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = Color.Unspecified,
            defaultOnColor = Color.Unspecified,
            loader = networkLoader,
        )
    LaunchedEffect(thumbnailUrl) {
        thumbnailUrl?.let { dominantColorState.updateFrom(Url(it)) }
    }
    if (thumbnailUrl == null) return null
    return dominantColorState.color.takeIf { it.isSpecified }
}

/**
 * Organic moving multi-color ambient lighting background driven by the album artwork palette.
 * Features drifting, breathing ambient light orbs that illuminate lyrics and player backdrops.
 */
@Composable
fun MovingAmbientLightBackground(
    primaryColor: Color,
    secondaryColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    alphaMultiplier: Float = 0.65f,
) {
    val prim = if (primaryColor.isSpecified && primaryColor != Color.Transparent && primaryColor != Color.Black) {
        primaryColor
    } else {
        Color(0xFF818CF8) // Soft periwinkle fallback
    }

    val sec = if (secondaryColor.isSpecified && secondaryColor != Color.Transparent && secondaryColor != Color.Black) {
        secondaryColor
    } else {
        prim.copy(alpha = 0.8f)
    }

    val tertiary = lerp(prim, sec, 0.5f)

    val infiniteTransition = rememberInfiniteTransition(label = "ambientMovement")

    // Slow, organic drifting movements for the multi-colour ambient light centers
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftAngle1",
    )

    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftAngle2",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val animatedPrim by animateColorAsState(prim, tween(200), label = "animPrim")
    val animatedSec by animateColorAsState(sec, tween(200), label = "animSec")
    val animatedTert by animateColorAsState(tertiary, tween(200), label = "animTert")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                if (size.width <= 0f || size.height <= 0f) return@drawBehind

                val w = size.width
                val h = size.height

                // Orb 1: Primary vibrant color moving around top-center
                val x1 = w * 0.45f + cos(t1) * (w * 0.28f)
                val y1 = h * 0.28f + sin(t1) * (h * 0.16f)
                val r1 = (w * 0.85f) * pulseScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedPrim.copy(alpha = 0.48f * alphaMultiplier),
                            animatedPrim.copy(alpha = 0.22f * alphaMultiplier),
                            Color.Transparent,
                        ),
                        center = Offset(x1, y1),
                        radius = r1,
                    ),
                    radius = r1,
                    center = Offset(x1, y1),
                )

                // Orb 2: Secondary color moving diagonally across middle & bottom
                val x2 = w * 0.55f + sin(t2) * (w * 0.30f)
                val y2 = h * 0.62f + cos(t2) * (h * 0.20f)
                val r2 = (w * 0.95f) * (2.02f - pulseScale)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedSec.copy(alpha = 0.42f * alphaMultiplier),
                            animatedSec.copy(alpha = 0.16f * alphaMultiplier),
                            Color.Transparent,
                        ),
                        center = Offset(x2, y2),
                        radius = r2,
                    ),
                    radius = r2,
                    center = Offset(x2, y2),
                )

                // Orb 3: Tertiary soft glow near upper-right / center
                val x3 = w * 0.68f + cos(t2 + 1.5f) * (w * 0.22f)
                val y3 = h * 0.22f + sin(t1 * 1.3f) * (h * 0.14f)
                val r3 = w * 0.75f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedTert.copy(alpha = 0.36f * alphaMultiplier),
                            animatedTert.copy(alpha = 0.12f * alphaMultiplier),
                            Color.Transparent,
                        ),
                        center = Offset(x3, y3),
                        radius = r3,
                    ),
                    radius = r3,
                    center = Offset(x3, y3),
                )
            },
    )
}

