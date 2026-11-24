package com.pulse.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pulse.music.extension.greyScale
import com.pulse.music.ui.navigation.destination.home.AnalyticsDestination
import com.pulse.music.ui.navigation.destination.home.HomeDestination
import com.pulse.music.ui.navigation.destination.library.LibraryDestination
import com.pulse.music.ui.navigation.destination.library.MixForYouDestination
import com.pulse.music.ui.navigation.destination.search.SearchDestination
import com.pulse.music.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pulsemusic.composeapp.generated.resources.*
import kotlin.reflect.KClass

/**
 * The phone bottom bar with liquid glass OFF: the same floating capsule-and-FAB form as the glass
 * bar, drawn flat. One capsule of tabs (Search stays out — it is the round button beside it, the
 * same split the glass bar makes), a sliding rounded indicator instead of the frosted blob, solid
 * theme surfaces instead of refraction. Geometry mirrors the glass bar so switching the setting
 * changes the material, not the layout: 96dp tab cap, 64dp bar, 56dp indicator and FAB.
 */
@Composable
fun AppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    isTranslucentBackground: Boolean = false,
    showAnalyticsTab: Boolean = false,
    showMixForYouTab: Boolean = false,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // `ordinal` identifies a tab, it is NOT the position — Mix for you and Analytics sit before
    // Library here while keeping the ordinal they were declared with, so that the numbering stays
    // stable whether or not those tabs are present.
    val bottomNavScreens =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
            BottomNavScreen.Search,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                is AnalyticsDestination -> BottomNavScreen.Analytics.ordinal
                is MixForYouDestination -> BottomNavScreen.MixForYou.ordinal
                else -> BottomNavScreen.Home.ordinal // Default to Home if not recognized
            },
        )
    }
    // A tab can disappear from the list under the user: tracking gets turned off while Analytics is
    // selected, or the YouTube session ends while Mix for you is. Fall back to Home in both cases so
    // nothing is left highlighted.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }
    val selectTab: (BottomNavScreen) -> Unit = { screen ->
        if (selectedIndex == screen.ordinal) {
            if (currentBackStackEntry?.destination?.hierarchy?.any {
                    it.hasRoute(screen.destination::class)
                } == true
            ) {
                reloadDestinationIfNeeded(screen.destination::class)
            } else {
                navController.navigate(screen.destination)
            }
        } else {
            selectedIndex = screen.ordinal
            navController.navigate(screen.destination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Search rides in its own circular button, so the capsule holds everything else.
    val barTabs = bottomNavScreens.filter { it != BottomNavScreen.Search }

    // The translucent switch tints the CAPSULE ITSELF, never a strip behind it — the area around
    // the floating cluster always shows the page. ON reads the content through the pill; OFF is a
    // solid surface. The indicator stays nearer opaque so the selection survives busy artwork.
    val capsuleColor =
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isTranslucentBackground) 0.72f else 1f)
    val indicatorColor =
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isTranslucentBackground) 0.85f else 1f)

    val searchSelected = selectedIndex == BottomNavScreen.Search.ordinal

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
    }
}

// Mirrors the glass tab bar's geometry (TabWidth/BarHeight/BlobHeight/BarInset in
// LiquidGlassTabBar.android.kt) so the two bars are one form in two materials.
private val FlatTabWidth = 96.dp
private val FlatBarHeight = 64.dp
private val FlatIndicatorHeight = 56.dp
private val CapsuleInset = 6.dp

@Composable
fun AppNavigationRail(
    startDestination: Any = HomeDestination,
    navController: NavController,
    showAnalyticsTab: Boolean = false,
    showMixForYouTab: Boolean = false,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // See the note in AppBottomNavigationBar: `ordinal` is the tab's identity, not its position.
    val bottomNavScreens =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Library,
            BottomNavScreen.Search,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                is AnalyticsDestination -> BottomNavScreen.Analytics.ordinal
                is MixForYouDestination -> BottomNavScreen.MixForYou.ordinal
                else -> BottomNavScreen.Home.ordinal // Default to Home if not recognized
            },
        )
    }
    // A tab can disappear from the list under the user: tracking gets turned off while Analytics is
    // selected, or the YouTube session ends while Mix for you is. Fall back to Home in both cases so
    // nothing is left highlighted.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }
    NavigationRail {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.padding(horizontal = 16.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.mono),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(32.dp)
                            .clip(CircleShape),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        bottomNavScreens.forEach { screen ->
            NavigationRailItem(
                icon = screen.icon,
                label = {
                    Text(
                        stringResource(screen.title),
                        style =
                            if (selectedIndex == screen.ordinal) {
                                typo().bodySmall
                            } else {
                                typo().bodySmall.greyScale()
                            },
                    )
                },
                selected = selectedIndex == screen.ordinal,
                onClick = {
                    if (selectedIndex == screen.ordinal) {
                        if (currentBackStackEntry?.destination?.hierarchy?.any {
                                it.hasRoute(screen.destination::class)
                            } == true
                        ) {
                            reloadDestinationIfNeeded(
                                screen.destination::class,
                            )
                        } else {
                            navController.navigate(screen.destination)
                        }
                    } else {
                        selectedIndex = screen.ordinal
                        navController.navigate(screen.destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}