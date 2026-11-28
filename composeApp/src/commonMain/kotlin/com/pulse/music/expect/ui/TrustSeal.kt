package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun WindowsTitleBarTrustSeal(
    isDark: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
)

@Composable
expect fun WindowsTrustSealSettingsRow(
    isDark: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    cardBackground: Color,
    divider: @Composable () -> Unit,
)

@Composable
expect fun WindowsCreditTrustBadge()
