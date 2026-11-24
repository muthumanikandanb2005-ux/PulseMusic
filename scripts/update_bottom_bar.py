path = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic\composeApp\src\commonMain\kotlin\com\pulse\music\ui\component\AppBottomNavigationBar.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Add imports
imports_needed = """import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
"""

if "import androidx.compose.animation.core.Spring" not in text:
    text = text.replace("import androidx.compose.animation.core.animateDpAsState\n", "import androidx.compose.animation.core.animateDpAsState\n" + imports_needed, 1)

target_row = """    Row(
        verticalAlignment = Alignment.CenterVertically,
        // One centred cluster — capsule, gap, FAB — exactly like the glass bar: fill = false keeps
        // the capsule at its measured width, so the leftover goes around the cluster instead of
        // wedging itself between the capsule and the search button.
        horizontalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 8.dp),
    ) {
        BoxWithConstraints(Modifier.weight(1f, fill = false)) {
            // Every tab the same width, capped so two tabs on a wide screen do not stretch into
            // slabs — the same budget rule as the glass tab bar.
            val tabWidth = ((maxWidth - CapsuleInset * 2) / barTabs.size).coerceAtMost(FlatTabWidth)
            val selectedPosition = barTabs.indexOfFirst { it.ordinal == selectedIndex }
            val indicatorOffset by animateDpAsState(tabWidth * selectedPosition.coerceAtLeast(0), label = "flatBarIndicator")
            Box(
                modifier =
                    Modifier
                        .height(FlatBarHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(capsuleColor)
                        .padding(horizontal = CapsuleInset),
                contentAlignment = Alignment.CenterStart,
            ) {
                // The sliding indicator — the flat stand-in for the glass bar's frosted blob. Hidden
                // while Search (a non-capsule tab) is the selection, so nothing sits half-lit.
                if (selectedPosition >= 0) {
                    Box(
                        modifier =
                            Modifier
                                .offset(x = indicatorOffset)
                                .size(width = tabWidth, height = FlatIndicatorHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(indicatorColor),
                    )
                }
                Row {
                    barTabs.forEach { screen ->
                        val selected = selectedIndex == screen.ordinal
                        val contentColor =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        Column(
                            modifier =
                                Modifier
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectTab(screen) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CompositionLocalProvider(LocalContentColor provides contentColor) {
                                screen.icon()
                            }
                            Text(
                                stringResource(screen.title),
                                style = typo().bodySmall,
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        val searchSelected = selectedIndex == BottomNavScreen.Search.ordinal
        Box(
            modifier =
                Modifier
                    .size(FlatIndicatorHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (searchSelected) indicatorColor else capsuleColor)
                    .clickable { selectTab(BottomNavScreen.Search) },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (searchSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ) {
                BottomNavScreen.Search.icon()
            }
        }
    }"""

replacement_row = """    val searchSelected = selectedIndex == BottomNavScreen.Search.ordinal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 8.dp),
    ) {
        // Unified Pinterest-Style Capsule Dock matching reference design
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = capsuleColor,
            shadowElevation = 10.dp,
            modifier = Modifier.height(FlatBarHeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                // Far-Left: Circular Brand Emblem / Search Button
                Surface(
                    onClick = { selectTab(BottomNavScreen.Search) },
                    shape = CircleShape,
                    color = if (searchSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = if (searchSelected) 6.dp else 2.dp,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (searchSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        ) {
                            BottomNavScreen.Search.icon()
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Navigation Tabs with Sliding Squircle Chip & Glowing Dot Indicator
                BoxWithConstraints {
                    val tabWidth = ((maxWidth.coerceAtMost(360.dp)) / barTabs.size).coerceIn(60.dp, 82.dp)
                    val selectedPosition = barTabs.indexOfFirst { it.ordinal == selectedIndex }
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * selectedPosition.coerceAtLeast(0),
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                        label = "flatBarIndicator",
                    )

                    // Sliding Squircle Indicator Chip
                    if (selectedPosition >= 0) {
                        Box(
                            modifier =
                                Modifier
                                    .offset(x = indicatorOffset)
                                    .size(width = tabWidth, height = FlatIndicatorHeight)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(indicatorColor),
                        )
                    }

                    Row {
                        barTabs.forEach { screen ->
                            val selected = selectedIndex == screen.ordinal
                            val contentColor =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                }
                            Column(
                                modifier =
                                    Modifier
                                        .width(tabWidth)
                                        .height(FlatIndicatorHeight)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectTab(screen) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CompositionLocalProvider(LocalContentColor provides contentColor) {
                                    screen.icon()
                                }
                                Text(
                                    stringResource(screen.title),
                                    style = typo().labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = contentColor,
                                    maxLines = 1,
                                )
                                // Active Tab Dot Indicator (as seen in reference image)
                                if (selected) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(contentColor),
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
target_row_norm = target_row.replace("\r\n", "\n")
replacement_row_norm = replacement_row.replace("\r\n", "\n")

if target_row_norm in text_norm:
    text_norm = text_norm.replace(target_row_norm, replacement_row_norm, 1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text_norm)
    print("AppBottomNavigationBar.kt updated successfully with Pinterest-style unified capsule dock!")
else:
    print("Target row not found!")
