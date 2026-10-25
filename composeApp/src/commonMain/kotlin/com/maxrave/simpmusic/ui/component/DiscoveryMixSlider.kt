package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.discovery_mix
import simpmusic.composeapp.generated.resources.discovery_mix_description
import simpmusic.composeapp.generated.resources.familiar_songs
import simpmusic.composeapp.generated.resources.new_discovery

/**
 * Discovery Mix Slider:
 * Allows user to choose a clear ratio between familiar tracks (from library/history)
 * and new recommendations (from streaming discovery) and adjust it dynamically while listening.
 */
@Composable
fun DiscoveryMixSlider(
    ratio: Float,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
) {
    val familiarPercent = ((1f - ratio) * 100).toInt().coerceIn(0, 100)
    val discoveryPercent = (ratio * 100).toInt().coerceIn(0, 100)

    val lavenderPrimary = Color(0xFFA78BFA)
    val lavenderSoft = Color(0xFFC4B5FD)
    val lavenderTrack = Color(0xFF3B354D)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Header Row: Icon + Title + Live Ratio Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(lavenderPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.Tune,
                            contentDescription = null,
                            tint = lavenderSoft,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.discovery_mix),
                        style = typo().titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Dynamic Ratio Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = lavenderPrimary.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = "$familiarPercent% Familiar • $discoveryPercent% New",
                        style = typo().labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = lavenderSoft,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.discovery_mix_description),
                style = typo().bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The Smooth Ratio Slider
            Slider(
                value = ratio,
                onValueChange = { onRatioChange(it) },
                valueRange = 0f..1f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = lavenderSoft,
                    activeTrackColor = lavenderPrimary,
                    inactiveTrackColor = lavenderTrack,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Labels under slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "100% " + stringResource(Res.string.familiar_songs),
                    style = typo().labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                Text(
                    text = "100% " + stringResource(Res.string.new_discovery),
                    style = typo().labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets row: 80/20 (Default), 50/50, 20/80
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DiscoveryPresetChip(
                    text = "80/20 Familiar",
                    isSelected = (ratio in 0.18f..0.22f),
                    onClick = { onRatioChange(0.20f) },
                    modifier = Modifier.weight(1f),
                )
                DiscoveryPresetChip(
                    text = "50/50 Balanced",
                    isSelected = (ratio in 0.48f..0.52f),
                    onClick = { onRatioChange(0.50f) },
                    modifier = Modifier.weight(1f),
                )
                DiscoveryPresetChip(
                    text = "20/80 Discovery",
                    isSelected = (ratio in 0.78f..0.82f),
                    onClick = { onRatioChange(0.80f) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DiscoveryPresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lavenderPrimary = Color(0xFFA78BFA)
    val bgColor by animateColorAsState(
        if (isSelected) lavenderPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
    )
    val textColor by animateColorAsState(
        if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typo().labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
    }
}
