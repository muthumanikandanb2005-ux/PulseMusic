package com.maxrave.data.di

import com.maxrave.common.Config.SERVICE_SCOPE
import com.maxrave.data.io.fileDir
import com.maxrave.data.repository.AccountRepositoryImpl
import com.maxrave.data.repository.AlbumRepositoryImpl
import com.maxrave.data.repository.AnalyticsRepositoryImpl
import com.maxrave.data.repository.ArtistRepositoryImpl
import com.maxrave.data.repository.AutoEqRepositoryImpl
import com.maxrave.data.lyrics.LyricsRomanizerRepositoryImpl
import com.maxrave.data.repository.CommonRepositoryImpl
import com.maxrave.data.repository.HomeRepositoryImpl
import com.maxrave.data.repository.ImportRepositoryImpl
import com.maxrave.data.repository.LocalPlaylistRepositoryImpl
import com.maxrave.data.repository.LyricsCanvasRepositoryImpl
import com.maxrave.data.repository.PlaylistRepositoryImpl
import com.maxrave.data.repository.PodcastRepositoryImpl
import com.maxrave.data.repository.SearchRepositoryImpl
import com.maxrave.data.repository.SongRepositoryImpl
import com.maxrave.data.repository.StreamRepositoryImpl
import com.maxrave.data.repository.UpdateRepositoryImpl
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
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
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

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get(), get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get(), get())
        }

        single<AnalyticsRepository>(createdAtStart = true) {
            AnalyticsRepositoryImpl(get())
        }

        single<com.manikandan.domain.repository.SupabaseAuthRepository>(createdAtStart = true) {
            com.maxrave.data.repository.SupabaseAuthRepositoryImpl(
                dataStoreManager = get(),
                songRepository = get(),
                localPlaylistRepository = get(),
            )
        }
    }