package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalIsPearlLavenderTheme
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.math.roundToInt

@Immutable
data class SettingsThemeColors(
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val searchBackground: Color,
    val searchBorder: Color,
    val titleText: Color,
    val subtitleText: Color,
    val iconTint: Color,
    val divider: Color,
    val headerText: Color,
    val chevronTint: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val switchTrackChecked: Color,
    val switchThumbChecked: Color,
    val switchTrackUnchecked: Color,
    val switchThumbUnchecked: Color,
)

@Composable
fun getSettingsThemeColors(): SettingsThemeColors {
    val isDark = LocalIsDarkTheme.current
    val isLavender = LocalIsPearlLavenderTheme.current

    return when {
        isLavender -> {
            if (isDark) {
                SettingsThemeColors(
                    screenBackground = Color(0xFF1B1130),
                    cardBackground = Color(0xFF271A42),
                    cardBorder = Color(0xFF3B2A5F),
                    searchBackground = Color(0xFF2E204E),
                    searchBorder = Color(0xFF4C3870),
                    titleText = Color(0xFFF3EDFD),
                    subtitleText = Color(0xFFB8A7D4),
                    iconTint = Color(0xFFA78BFA),
                    divider = Color(0xFF3B2A5F),
                    headerText = Color(0xFFA78BFA),
                    chevronTint = Color(0xFF8B77AA),
                    badgeBg = Color(0xFF3B2A5F),
                    badgeText = Color(0xFFE9D8FD),
                    switchTrackChecked = Color(0xFFA78BFA),
                    switchThumbChecked = Color(0xFF1B1130),
                    switchTrackUnchecked = Color(0xFF3B2A5F),
                    switchThumbUnchecked = Color(0xFF6B588E),
                )
            } else {
                SettingsThemeColors(
                    screenBackground = Color(0xFFFAF7FD),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFE8DEF8),
                    searchBackground = Color(0xFFF3EDF9),
                    searchBorder = Color(0xFFE2D2F2),
                    titleText = Color(0xFF26193E),
                    subtitleText = Color(0xFF746591),
                    iconTint = Color(0xFF7C3AED),
                    divider = Color(0xFFF0E6F8),
                    headerText = Color(0xFF7C3AED),
                    chevronTint = Color(0xFF9E8FB8),
                    badgeBg = Color(0xFFEADDFF),
                    badgeText = Color(0xFF4F378B),
                    switchTrackChecked = Color(0xFF7C3AED),
                    switchThumbChecked = Color(0xFFFFFFFF),
                    switchTrackUnchecked = Color(0xFFE2D2F2),
                    switchThumbUnchecked = Color(0xFF9E8FB8),
                )
            }
        }
        !isDark -> {
            SettingsThemeColors(
                screenBackground = Color(0xFFF2F2F7),
                cardBackground = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE5E5EA),
                searchBackground = Color(0xFFE5E5EA),
                searchBorder = Color(0xFFD1D1D6),
                titleText = Color(0xFF000000),
                subtitleText = Color(0xFF636366),
                iconTint = Color(0xFF3C3C43),
                divider = Color(0xFFE5E5EA),
                headerText = Color(0xFF6E6E73),
                chevronTint = Color(0xFF8E8E93),
                badgeBg = Color(0xFFE5E5EA),
                badgeText = Color(0xFF3C3C43),
                switchTrackChecked = Color(0xFF000000),
                switchThumbChecked = Color(0xFFFFFFFF),
                switchTrackUnchecked = Color(0xFFE5E5EA),
                switchThumbUnchecked = Color(0xFF8E8E93),
            )
        }
        else -> {
            SettingsThemeColors(
                screenBackground = Color(0xFF000000),
                cardBackground = Color(0xFF161618),
                cardBorder = Color(0xFF222226),
                searchBackground = Color(0xFF1C1C1E),
                searchBorder = Color(0xFF26262A),
                titleText = Color(0xFFFFFFFF),
                subtitleText = Color(0xFF8E8E93),
                iconTint = Color(0xFFD1D1D6),
                divider = Color(0xFF222226),
                headerText = Color(0xFF8E8E93),
                chevronTint = Color(0xFF636366),
                badgeBg = Color(0xFF2C2C2E),
                badgeText = Color(0xFFCCCCCC),
                switchTrackChecked = Color(0xFFFFFFFF),
                switchThumbChecked = Color(0xFF000000),
                switchTrackUnchecked = Color(0xFF2C2C2E),
                switchThumbUnchecked = Color(0xFF48484A),
            )
        }
    }
}

/**
 * Settings Card Container supporting Dark, Light and Lavender themes.
 */
@Composable
fun PulseSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = getSettingsThemeColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
        content = content,
    )
}

/**
 * Category header with small uppercase bold typography and tracked letter spacing.
 */
@Composable
fun PulseSettingsHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = getSettingsThemeColors()
    Text(
        text = title.uppercase(),
        style = typo().labelSmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = colors.headerText,
        ),
        modifier = modifier.padding(start = 14.dp, top = 22.dp, bottom = 8.dp),
    )
}

/**
 * Small pill badge next to setting titles (e.g. "IN USE", "BLOCKING", "BETA").
 */
@Composable
fun PulseSettingsBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color? = null,
) {
    val colors = getSettingsThemeColors()
    val bg = backgroundColor ?: colors.badgeBg
    val fg = textColor ?: colors.badgeText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = typo().labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

/**
 * Modern High-Contrast Switch matching user screenshots:
 * - When Checked: Solid white track pill with solid black thumb circle on the right.
 * - When Unchecked: Dark charcoal track pill with muted grey thumb circle on the left.
 */
@Composable
fun PulseSettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = getSettingsThemeColors()
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.switchTrackChecked else colors.switchTrackUnchecked,
        animationSpec = tween(durationMillis = 200),
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.switchThumbChecked else colors.switchThumbUnchecked,
        animationSpec = tween(durationMillis = 200),
    )
    val thumbOffsetPercent by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
    )

    val maxOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (trackWidth - trackHeight).toPx()
    }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) {
                onCheckedChange(!checked)
            }
            .padding(padding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumbOffsetPercent * maxOffsetPx).roundToInt(),
                        y = 0,
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

/**
 * Segmented Pill Button Control (e.g. [16-bit PCM] | [32-bit float]).
 */
@Composable
fun PulseSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = getSettingsThemeColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBorder)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) colors.switchTrackChecked else Color.Transparent)
                    .clickable { onOptionSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = typo().labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.switchThumbChecked else colors.subtitleText,
                    ),
                )
            }
        }
    }
}

/**
 * Settings Row Item matching the screenshot hierarchy:
 * - Leading icon (22dp)
 * - Title + optional Badge
 * - Subtitle with clear line height
 * - Bottom extra content (optional segmented buttons / slider)
 * - Trailing widget (chevron with right text, custom switch, or custom control)
 */
@Composable
fun PulseSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeBg: Color? = null,
    badgeTextColor: Color? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    showChevron: Boolean = false,
    switchState: Pair<Boolean, (Boolean) -> Unit>? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val colors = getSettingsThemeColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) colors.iconTint else colors.chevronTint.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 0.dp),
                )
                Spacer(Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = typo().titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (enabled) colors.titleText else colors.subtitleText.copy(alpha = 0.6f),
                        ),
                    )
                    if (badgeText != null) {
                        PulseSettingsBadge(
                            text = badgeText,
                            backgroundColor = badgeBg,
                            textColor = badgeTextColor,
                        )
                    }
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = typo().bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 16.5.sp,
                            color = if (enabled) colors.subtitleText else colors.subtitleText.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            if (switchState != null) {
                Spacer(Modifier.width(12.dp))
                PulseSettingSwitch(
                    checked = switchState.first,
                    onCheckedChange = switchState.second,
                    enabled = enabled,
                )
            } else if (showChevron || trailingText != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (trailingText != null) {
                        Text(
                            text = trailingText,
                            style = typo().bodyMedium.copy(
                                fontSize = 14.sp,
                                color = colors.subtitleText,
                            ),
                        )
                    }
                    if (showChevron) {
                        Icon(
                            imageVector = SimpIcons.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.chevronTint,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        if (bottomContent != null) {
            Spacer(Modifier.height(10.dp))
            bottomContent()
        }
    }
}

/**
 * Divider separating items inside the card, indented nicely to align with text.
 */
@Composable
fun PulseSettingDivider(
    hasLeadingIcon: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = getSettingsThemeColors()
    HorizontalDivider(
        modifier = modifier.padding(
            start = if (hasLeadingIcon) 52.dp else 16.dp,
            end = 16.dp,
        ),
        thickness = 0.7.dp,
        color = colors.divider,
    )
}

/**
 * Top Search Box with rounded pill shape and clean icon/clear button.
 */
@Composable
fun PulseSettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search settings",
) {
    val colors = getSettingsThemeColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.searchBackground)
            .border(1.dp, colors.searchBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = SimpIcons.Search,
                contentDescription = null,
                tint = colors.subtitleText,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typo().bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = colors.subtitleText,
                        ),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = colors.titleText,
                        fontSize = 14.5.sp,
                    ),
                    cursorBrush = SolidColor(colors.titleText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = SimpIcons.Close,
                    contentDescription = "Clear search",
                    tint = colors.subtitleText,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}
