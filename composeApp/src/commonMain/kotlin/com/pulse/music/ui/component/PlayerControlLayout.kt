package com.pulse.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manikandan.domain.mediaservice.handler.ControlState
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.pulse.music.ui.icon.Pause
import com.pulse.music.ui.icon.PauseCircle
import com.pulse.music.ui.icon.PlayArrow
import com.pulse.music.ui.icon.PlayCircle
import com.pulse.music.ui.icon.Repeat
import com.pulse.music.ui.icon.RepeatOne
import com.pulse.music.ui.icon.Shuffle
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.icon.SkipNext
import com.pulse.music.ui.icon.SkipPrevious
import com.pulse.music.ui.theme.seed
import com.pulse.music.viewModel.UIEvent

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    isSmallSize: Boolean = false,
    // Bare ▶ / ⏸ glyphs instead of the disc-enclosed PlayCircle/PauseCircle pair.
    // The desktop capsule asks for these; Now Playing keeps the discs.
    plainPlayPause: Boolean = false,
    // The capsule already pads its own edges; stacking this 20dp on top of that
    // read as a hole at both ends of the transport cluster.
    horizontalPadding: Dp = 20.dp,
    // Tint for the ACTIVE shuffle/repeat state. The default keeps the raw seed (#8ECAE6) every
    // existing call site had; the capsule passes a theme-aware colour because pastel seed on a
    // light glass surface is nearly invisible.
    activeColor: Color = seed,
    contentColor: Color = Color.White,
    onUIEvent: (UIEvent) -> Unit,
) {
    val height = if (isSmallSize) 48.dp else 96.dp
    val smallIcon = if (isSmallSize) 20.dp to 28.dp else 32.dp to 42.dp
    val mediumIcon = if (isSmallSize) 28.dp to 38.dp else 42.dp to 52.dp
    val bigIcon = if (isSmallSize) 38.dp to 48.dp else 72.dp to 96.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = horizontalPadding),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val shuffleTint by animateColorAsState(
                targetValue = if (controllerState.isShuffle) activeColor else contentColor,
                animationSpec = tween(220),
                label = "shuffleTint",
            )
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .pixelClickable(pressedScale = 0.82f, hoveredScale = 1.08f, role = Role.Button) {
                            onUIEvent(UIEvent.Shuffle)
                        }
                        .pixelPopOnChange(controllerState.isShuffle, peakScale = 1.2f, onlyWhen = controllerState.isShuffle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PulseIcons.Shuffle,
                    tint = shuffleTint,
                    contentDescription = "",
                    modifier = Modifier.size(smallIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .pixelClickable(
                            enabled = controllerState.isPreviousAvailable,
                            pressedScale = 0.84f,
                            hoveredScale = 1.08f,
                            role = Role.Button,
                        ) {
                            if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PulseIcons.SkipPrevious,
                    tint = if (controllerState.isPreviousAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = "",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(bigIcon.second)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .pixelClickable(pressedScale = 0.85f, hoveredScale = 1.07f, role = Role.Button) {
                            onUIEvent(UIEvent.PlayPause)
                        }
                        .pixelPopOnChange(controllerState.isPlaying, peakScale = 1.12f),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = controllerState.isPlaying,
                    transitionSpec = { PixelMotion.iconMorphTransition() },
                    contentAlignment = Alignment.Center,
                    label = "playPauseTransportMorph",
                ) { isPlaying ->
                    Icon(
                        imageVector = if (!isPlaying) {
                            if (plainPlayPause) PulseIcons.PlayArrow else PulseIcons.PlayCircle
                        } else {
                            if (plainPlayPause) PulseIcons.Pause else PulseIcons.PauseCircle
                        },
                        tint = contentColor,
                        contentDescription = "",
                        modifier = Modifier.size(bigIcon.first),
                    )
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .pixelClickable(
                            enabled = controllerState.isNextAvailable,
                            pressedScale = 0.84f,
                            hoveredScale = 1.08f,
                            role = Role.Button,
                        ) {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PulseIcons.SkipNext,
                    tint = if (controllerState.isNextAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = "",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val repeatTint by animateColorAsState(
                targetValue = if (controllerState.repeatState != RepeatState.None) activeColor else contentColor,
                animationSpec = tween(220),
                label = "repeatTint",
            )
            Box(
                modifier =
                    Modifier
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .pixelClickable(pressedScale = 0.82f, hoveredScale = 1.08f, role = Role.Button) {
                            onUIEvent(UIEvent.Repeat)
                        }
                        .pixelPopOnChange(
                            controllerState.repeatState,
                            peakScale = 1.2f,
                            onlyWhen = controllerState.repeatState != RepeatState.None,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = controllerState.repeatState,
                    transitionSpec = { PixelMotion.iconMorphTransition() },
                    contentAlignment = Alignment.Center,
                    label = "repeatMorph",
                ) { rs ->
                    Icon(
                        imageVector = if (rs == RepeatState.One) PulseIcons.RepeatOne else PulseIcons.Repeat,
                        tint = repeatTint,
                        contentDescription = "",
                        modifier = Modifier.size(smallIcon.first),
                    )
                }
            }
        }
    }
}