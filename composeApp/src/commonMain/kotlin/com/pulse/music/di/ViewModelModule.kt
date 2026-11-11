package com.pulse.music.di

import com.pulse.music.viewModel.AlbumViewModel
import com.pulse.music.utils.VersionManager
import com.pulse.music.viewModel.AnalyticsViewModel
import com.pulse.music.viewModel.ListenTogetherSettingsViewModel
import com.pulse.music.viewModel.ListenTogetherViewModel
import com.pulse.music.viewModel.ArtistViewModel
import com.pulse.music.viewModel.HomeViewModel
import com.pulse.music.viewModel.ImportViewModel
import com.pulse.music.viewModel.LibraryDynamicPlaylistViewModel
import com.pulse.music.viewModel.LibraryViewModel
import com.pulse.music.viewModel.LocalPlaylistViewModel
import com.pulse.music.viewModel.LogInViewModel
import com.pulse.music.viewModel.MoodViewModel
import com.pulse.music.viewModel.MoreAlbumsViewModel
import com.pulse.music.viewModel.NotificationViewModel
import com.pulse.music.viewModel.NowPlayingBottomSheetViewModel
import com.pulse.music.viewModel.PlaylistViewModel
import com.pulse.music.viewModel.PodcastViewModel
import com.pulse.music.viewModel.RecentlySongsViewModel
import com.pulse.music.viewModel.SearchViewModel
import com.pulse.music.viewModel.AutoEqViewModel
import com.pulse.music.viewModel.SettingsViewModel
import com.pulse.music.viewModel.SharedViewModel
import com.pulse.music.viewModel.SongSelectionViewModel
import com.pulse.music.viewModel.WrappedViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            SongSelectionViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ImportViewModel(
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AutoEqViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AnalyticsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            WrappedViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ListenTogetherSettingsViewModel(get())
        }
        viewModel {
            ListenTogetherViewModel(
                repository = get(),
                dataStore = get(),
                bridge = get(),
            )
        }

    }