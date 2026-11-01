package com.pulse.music.ui.component.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.music.Platform
import com.pulse.music.expect.copyToClipboard
import com.pulse.music.expect.isSnapchatAvailable
import com.pulse.music.expect.saveImageToDevice
import com.pulse.music.expect.shareImage
import com.pulse.music.expect.shareToSnapchat
import com.pulse.music.expect.ui.rememberSaveImagePermission
import com.pulse.music.expect.ui.toPngByteArray
import com.pulse.music.getPlatform
import com.pulse.music.ui.component.capture.capturable
import com.pulse.music.ui.component.capture.rememberCaptureController
import com.pulse.music.ui.icon.ContentCopy
import com.pulse.music.ui.icon.Download
import com.pulse.music.ui.icon.KeyboardArrowDown
import com.pulse.music.ui.icon.Share
import com.pulse.music.ui.icon.SimpIcons
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import kotlin.random.Random

// Snapchat brand color
private val SnapYellow = Color(0xFFFFFC00)

/**
 * Spotify-style Music Card Sharing Bottom Sheet.
 *
 * Offers an interactive preview of the 9:16 music card with selectable color themes,
 * paired with direct sharing destinations:
 *
 * 1. Snapchat (via Snapchat Creative Kit) — sends the card image + swipe-up context card link
 * 2. Share Card (System share sheet with generated high-res PNG image)
 * 3. Copy Link (Copies track URL to clipboard)
 * 4. Save to Device (Saves the card PNG directly to user's gallery)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMusicCardSheet(
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap? = null,
    artworkUrl: String? = null,
    seedColor: Color,
    videoId: String,
    albumName: String? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()

    var cardBackground by remember(seedColor) { mutableStateOf(seedColor) }
    var busy by remember { mutableStateOf(false) }

    val trackUrl = if (videoId.isNotBlank()) "https://music.youtube.com/watch?v=$videoId" else "https://github.com/muthumanikandanb2005-ux/PulseMusic"
    val deepLinkUrl = trackUrl

    val fileName = remember(songTitle) {
        val safeName = songTitle.ifBlank { "song" }.take(30).map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
        "PulseMusic_${safeName}_${Random.nextInt(100_000, 999_999)}.png"
    }

    val savePermission = rememberSaveImagePermission { granted ->
        if (!granted) {
            showToast("Storage permission required to save card", ToastGravity.Bottom)
            busy = false
            return@rememberSaveImagePermission
        }
        scope.launch {
            val bytes = captureController.captureAsync().await().toPngByteArray()
            val ok = bytes != null && saveImageToDevice(bytes, fileName)
            showToast(if (ok) "Music card saved to Photos" else "Failed to save card", ToastGravity.Bottom)
            busy = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top Header Row ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Share Song",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Spotify-style Music Card & Snapchat",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                    )
                }

                Spacer(modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Interactive Music Card Preview (Capturable) ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .capturable(captureController),
                contentAlignment = Alignment.Center,
            ) {
                ShareMusicCard(
                    songTitle = songTitle,
                    artistName = artistName,
                    artwork = artwork,
                    artworkUrl = artworkUrl,
                    background = cardBackground,
                    albumName = albumName,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Card Color Palette Swatches ───────────────────────────────────
            CardColorPalette(
                seedColor = seedColor,
                selected = cardBackground,
                onSelect = { cardBackground = it },
            )

            Spacer(modifier = Modifier.height(22.dp))

            // ── Share Actions / Destinations ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1. Snapchat Creative Kit Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SnapYellow)
                        .clickable(enabled = !busy) {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    runCatching {
                                        val bytes = captureController.captureAsync().await().toPngByteArray()
                                        if (bytes != null) {
                                            val ok = shareToSnapchat(
                                                bytes = bytes,
                                                fileName = fileName,
                                                songTitle = songTitle,
                                                artistName = artistName,
                                                deepLinkUrl = deepLinkUrl,
                                                caption = "🎵 $songTitle — $artistName | Pulse Music",
                                            )
                                            if (!ok) {
                                                showToast("Snapchat not installed or unavailable", ToastGravity.Bottom)
                                            } else {
                                                onDismiss()
                                            }
                                        } else {
                                            showToast("Failed to render music card", ToastGravity.Bottom)
                                        }
                                    }.onFailure {
                                        showToast("Could not share to Snapchat", ToastGravity.Bottom)
                                    }
                                    busy = false
                                }
                            }
                        }
                        .padding(vertical = 14.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "👻", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (busy) "Preparing Snap…" else "Share as a Snap to Snapchat",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 2. Share Music Card Image (System Share Sheet)
                ShareActionRow(
                    icon = SimpIcons.Share,
                    title = "Share Music Card (Image)",
                    subtitle = "Send 9:16 card to Instagram, WhatsApp, Stories & more",
                    onClick = {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                runCatching {
                                    val bytes = captureController.captureAsync().await().toPngByteArray()
                                    if (bytes != null) {
                                        shareImage(bytes, fileName, "Share Music Card")
                                        onDismiss()
                                    }
                                }
                                busy = false
                            }
                        }
                    },
                )

                // 3. Copy Track Link
                ShareActionRow(
                    icon = SimpIcons.ContentCopy,
                    title = "Copy Song Link",
                    subtitle = trackUrl,
                    onClick = {
                        copyToClipboard("Pulse Music Song", trackUrl)
                        showToast("Song link copied to clipboard", ToastGravity.Bottom)
                    },
                )

                // 4. Save to Device
                ShareActionRow(
                    icon = SimpIcons.Download,
                    title = "Save Card to Gallery",
                    subtitle = "Save 9:16 vertical card to your photos",
                    onClick = {
                        if (!busy) {
                            busy = true
                            savePermission.requestIfNeeded()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ShareActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF202024))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C30)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CardColorPalette(
    seedColor: Color,
    selected: Color,
    onSelect: (Color) -> Unit,
) {
    val swatches = remember(seedColor) {
        listOf(
            seedColor,
            Color(0xFF141416), // AMOLED Midnight
            Color(0xFF1E3A5F), // Deep Ocean Indigo
            Color(0xFF4A154B), // Velvet Berry
            Color(0xFF1F4E38), // Forest Emerald
            Color(0xFF7A2020), // Crimson Fire
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatches.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (color == selected) 2.5.dp else 1.dp,
                        color = if (color == selected) Color.White else Color.White.copy(alpha = 0.15f),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}
