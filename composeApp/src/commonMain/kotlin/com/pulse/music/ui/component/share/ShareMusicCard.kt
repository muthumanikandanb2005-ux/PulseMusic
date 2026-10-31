package com.pulse.music.ui.component.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pulse.music.ui.component.lyrics.shareCardContentColor
import com.pulse.music.ui.component.rememberHolderPainter

/**
 * Spotify-style Music Card for Pulse Music.
 *
 * Renders in a 9:16 vertical ratio suitable for Snapchat Stories, Instagram Stories,
 * and direct social sharing.
 *
 * Features:
 * - Rich ambient gradient background driven by the track's dominant palette color
 * - Large, rounded album cover with soft ambient drop-shadow
 * - High-contrast bold song title & artist typography
 * - Signature Pulse Music Soundwave Barcode Bar at the bottom — inspired by Spotify Code
 *   but designed with Pulse Music's brand aesthetics (custom waveform frequencies + Pulse badge)
 */
@Composable
fun ShareMusicCard(
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap? = null,
    artworkUrl: String? = null,
    background: Color,
    modifier: Modifier = Modifier,
    albumName: String? = null,
) {
    val contentColor = background.shareCardContentColor()
    val secondaryColor = contentColor.copy(alpha = 0.72f)
    val accentTint = if (background == Color.Black || background == Color(0xFF141414)) Color(0xFFE5E5EA) else background

    // Background gradient: subtle darkening towards the bottom for depth
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            background,
            lerp(background, Color(0xFF09090B), 0.55f),
            Color(0xFF050507),
        ),
    )

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Top Brand Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Pulse Logo Dot Indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentTint),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "PULSE MUSIC",
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Center: Album Cover Art with Glow ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = accentTint.copy(alpha = 0.65f),
                        ambientColor = Color.Black,
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1A1A1E))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork,
                        contentDescription = songTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(artworkUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(artworkUrl)
                            .crossfade(false)
                            .build(),
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentDescription = songTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 44.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Track Title & Artist Metadata ─────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = songTitle.ifBlank { "Unknown Title" },
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 23.sp,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = artistName.ifBlank { "Unknown Artist" },
                    color = secondaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!albumName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = albumName,
                        color = secondaryColor.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Bottom: Pulse Music Waveform Bar (Spotify Code counterpart) ───
            PulseWaveCodeBar(
                accentColor = accentTint,
                contentColor = contentColor,
            )
        }
    }
}

/**
 * Signature Pulse Music Waveform Code Bar.
 *
 * Spotify cards feature the iconic "Spotify Code" barcode soundwave.
 * Pulse Music has its own distinctive audio waveform frequency signature:
 * - Pulse icon capsule on the left
 * - 24 dynamic soundwave frequency bars reflecting rhythm
 * - "Listen on Pulse Music" subtitle pill
 */
@Composable
private fun PulseWaveCodeBar(
    accentColor: Color,
    contentColor: Color,
) {
    val barHeights = listOf(
        0.35f, 0.65f, 0.45f, 0.85f, 0.55f, 1.0f,
        0.75f, 0.40f, 0.90f, 0.60f, 0.30f, 0.80f,
        0.95f, 0.50f, 0.70f, 0.40f, 0.85f, 0.60f,
        1.0f, 0.55f, 0.75f, 0.45f, 0.65f, 0.35f,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141416).copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Pulse Brand Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "▶",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PULSE",
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
            }

            // Right: Soundwave bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
            ) {
                barHeights.forEach { scale ->
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height((20 * scale).dp)
                            .clip(RoundedCornerShape(50))
                            .background(contentColor.copy(alpha = 0.85f)),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle
        Text(
            text = "LISTEN ON PULSE MUSIC",
            color = contentColor.copy(alpha = 0.55f),
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
    }
}
