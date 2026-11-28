package com.manikandan.data.di

import com.manikandan.common.Config.SERVICE_SCOPE
import com.manikandan.data.io.fileDir
import com.manikandan.data.repository.AccountRepositoryImpl
import com.manikandan.data.repository.AlbumRepositoryImpl
import com.manikandan.data.repository.AnalyticsRepositoryImpl
import com.manikandan.data.repository.ArtistRepositoryImpl
import com.manikandan.data.repository.AutoEqRepositoryImpl
import com.manikandan.data.lyrics.LyricsRomanizerRepositoryImpl
import com.manikandan.data.repository.CommonRepositoryImpl
import com.manikandan.data.repository.HomeRepositoryImpl
import com.manikandan.data.repository.ImportRepositoryImpl
import com.manikandan.data.repository.LocalPlaylistRepositoryImpl
import com.manikandan.data.repository.LyricsCanvasRepositoryImpl
import com.manikandan.data.repository.PlaylistRepositoryImpl
import com.manikandan.data.repository.PodcastRepositoryImpl
import com.manikandan.data.repository.SearchRepositoryImpl
import com.manikandan.data.repository.SongRepositoryImpl
import com.manikandan.data.repository.StreamRepositoryImpl
import com.manikandan.data.repository.UpdateRepositoryImpl
import com.manikandan.domain.repository.AccountRepository
import com.manikandan.domain.repository.AlbumRepository
import com.manikandan.domain.repository.AnalyticsRepository
import com.manikandan.domain.repository.ArtistRepository
import com.manikandan.domain.repository.AutoEqRepository
import com.manikandan.domain.repository.LyricsRomanizerRepository
import com.manikandan.domain.repository.CommonRepository
import com.manikandan.domain.repository.HomeRepository
import com.manikandan.domain.repository.ImportRepository
import com.manikandan.domain.repository.LocalPlaylistRepository
import com.manikandan.domain.repository.LyricsCanvasRepository
import com.manikandan.domain.repository.PlaylistRepository
import com.manikandan.domain.repository.PodcastRepository
import com.manikandan.domain.repository.SearchRepository
import com.manikandan.domain.repository.SongRepository
import com.manikandan.domain.repository.StreamRepository
import com.manikandan.domain.repository.UpdateRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository> {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository> {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository> {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository> {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get())
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        // Lazy: constructing it costs a few File.length() calls, but the kuromoji dictionary
        // behind it is loaded on first Japanese line and never before — so this must NOT be
        // createdAtStart, or every launch pays for a feature most listeners leave off. The path
        // is where Android keeps the downloaded ipadic pack (the APK no longer bundles it);
        // Desktop and iOS ignore it.
        single<LyricsRomanizerRepository> {
            LyricsRomanizerRepositoryImpl("${fileDir()}/kuromoji-ipadic")
        }

        single<HomeRepository> {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository> {
            ImportRepositoryImpl(get(), get())
        }

        single<LocalPlaylistRepository> {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository> {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get(), get())
        }

        single<PlaylistRepository> {
            PlaylistRepositoryImpl(get(), get(), get(), get())
        }

        single<PodcastRepository> {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository> {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository> {
            SongRepositoryImpl(get(), get(), get(), get())
        }

        single<StreamRepository> {
            StreamRepositoryImpl(get(), get(), get())
        }

        single<UpdateRepository> {
            UpdateRepositoryImpl(get(), get())
        }

        single<AnalyticsRepository> {
            AnalyticsRepositoryImpl(get())
        }

    }