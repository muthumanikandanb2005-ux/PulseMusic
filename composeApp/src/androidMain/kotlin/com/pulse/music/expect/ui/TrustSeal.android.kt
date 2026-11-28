package com.pulse.music.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun WindowsTitleBarTrustSeal(
    isDark: Boolean,
    tint: Color,
    modifier: Modifier,
) = Unit

@Composable
actual fun WindowsTrustSealSettingsRow(
    isDark: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    cardBackground: Color,
    divider: @Composable () -> Unit,
) = Unit

@Composable
actual fun WindowsCreditTrustBadge() = Unit
