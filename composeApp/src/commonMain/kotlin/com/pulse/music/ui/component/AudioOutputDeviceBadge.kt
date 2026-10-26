package com.pulse.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.music.expect.ui.rememberCurrentAudioOutputDevice
import com.pulse.music.ui.icon.Bluetooth
import com.pulse.music.ui.icon.Headphones
import com.pulse.music.ui.icon.SimpIcons
import com.pulse.music.ui.icon.VolumeUp

/**
 * Audio output device badge indicator showing active device routing (Bluetooth, Wired/USB, Phone Speaker).
 * Strictly styled with soft pastel colours (no harsh white or neon tones).
 */
@Composable
fun AudioOutputDeviceBadge(
    modifier: Modifier = Modifier,
    softAccent: Color = Color(0xFFA788FA), // Soft pastel lavender default
    containerColor: Color? = null,
    borderColor: Color? = null,
    contentColor: Color? = null,
    fontSize: TextUnit = 10.sp,
    iconSize: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(7.dp),
    onClick: (() -> Unit)? = null,
) {
    val device = rememberCurrentAudioOutputDevice()

    val effectiveContainerColor = containerColor ?: softAccent.copy(alpha = 0.12f)
    val effectiveBorderColor = borderColor ?: softAccent.copy(alpha = 0.24f)
    val effectiveContentColor = contentColor ?: softAccent.copy(alpha = 0.90f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(effectiveContainerColor)
            .border(width = 0.5.dp, color = effectiveBorderColor, shape = shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = device,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AudioOutputDeviceAnimation",
        ) { currentDevice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val icon = when {
                    currentDevice.isBluetooth -> SimpIcons.Bluetooth
                    currentDevice.isHeadphones -> SimpIcons.Headphones
                    else -> SimpIcons.VolumeUp
                }

                Icon(
                    imageVector = icon,
                    contentDescription = currentDevice.name,
                    tint = effectiveContentColor,
                    modifier = Modifier.size(iconSize),
                )

                Text(
                    text = currentDevice.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = effectiveContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
