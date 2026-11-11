package com.pulse.music.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.pulse.music.ui.navigation.destination.home.CreditDestination
import com.pulse.music.ui.navigation.destination.home.ListenTogetherDestination
import com.pulse.music.ui.navigation.destination.home.ListenTogetherSettingsDestination
import com.pulse.music.ui.navigation.destination.home.MoodDestination
import com.pulse.music.ui.navigation.destination.home.NotificationDestination
import com.pulse.music.ui.navigation.destination.home.RecentlySongsDestination
import com.pulse.music.ui.navigation.destination.home.SettingsDestination
import com.pulse.music.ui.screen.home.ListenTogetherScreen
import com.pulse.music.ui.screen.home.ListenTogetherSettingsScreen
import com.pulse.music.ui.screen.home.MoodScreen
import com.pulse.music.ui.screen.home.NotificationScreen
import com.pulse.music.ui.screen.home.RecentlySongsScreen
import com.pulse.music.ui.screen.home.SettingScreen
import com.pulse.music.ui.screen.other.CreditScreen

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        MoodScreen(
            navController = navController,
            params = params,
        )
    }
    composable<ListenTogetherDestination> {
        ListenTogetherScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<ListenTogetherSettingsDestination> {
        ListenTogetherSettingsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsDestination> {
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
}