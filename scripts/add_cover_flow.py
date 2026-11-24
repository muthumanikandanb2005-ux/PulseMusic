path = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic\composeApp\src\commonMain\kotlin\com\pulse\music\ui\screen\player\content\NowPlayingExpressiveCards.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Add imports
imports_to_add = """import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
"""

if "import androidx.compose.ui.text.font.FontWeight" not in text:
    text = text.replace("import androidx.compose.material3.Text\n", "import androidx.compose.material3.Text\n" + imports_to_add, 1)

# Update the Card Box with Cover Flow 3D graphicsLayer
target_card_box = """            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .alpha(if (pageHasCanvas) 0f else 1f)
                        .aspectRatio(cardAspectRatio),
            ) {"""

replacement_card_box = """            val pageOffset = ((state.artworkPagerState.currentPage - page) + state.artworkPagerState.currentPageOffsetFraction)
            val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            cameraDistance = 12 * density
                            // 3D Cover Flow perspective rotation as shown in reference image
                            rotationY = -pageOffset.coerceIn(-1f, 1f) * 26f
                            scaleX = 1f - (absOffset * 0.16f)
                            scaleY = 1f - (absOffset * 0.16f)
                            alpha = if (pageHasCanvas) 0f else (1f - (absOffset * 0.35f))
                            translationX = pageOffset * 26.dp.toPx()
                            shadowElevation = if (absOffset < 0.1f) 18.dp.toPx() else 4.dp.toPx()
                        }
                        .aspectRatio(cardAspectRatio),
            ) {"""

text = text.replace(target_card_box, replacement_card_box, 1)

# Add Cover Flow bottom track banner before the card box closes
target_close = """                        contentScale = ContentScale.Crop,
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}"""

replacement_close = """                        contentScale = ContentScale.Crop,
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Cover Flow track banner at the bottom of the card matching the reference image
                val bannerTitle = if (isCurrentArtworkPage) state.screenData.nowPlayingTitle else (pageTrack?.title ?: "")
                val bannerArtist = if (isCurrentArtworkPage) state.screenData.artistName else (pageTrack?.artists?.firstOrNull()?.name ?: "")
                if (bannerTitle.isNotBlank() && !pageHasCanvas) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.50f),
                                        Color.Black.copy(alpha = 0.88f),
                                    ),
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = bannerTitle,
                                style = typo().titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                            if (bannerArtist.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = bannerArtist,
                                    style = typo().bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 12.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}"""

text_norm = text.replace("\r\n", "\n")
target_close_norm = target_close.replace("\r\n", "\n")
replacement_close_norm = replacement_close.replace("\r\n", "\n")

if target_close_norm in text_norm:
    text_norm = text_norm.replace(target_close_norm, replacement_close_norm, 1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text_norm)
    print("NowPlayingExpressiveCards.kt updated successfully with Cover Flow 3D rotation and banner!")
else:
    print("Target close not found!")
