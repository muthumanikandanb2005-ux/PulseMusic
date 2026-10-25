package com.maxrave.simpmusic.ui.screen.library

import com.manikandan.domain.data.entities.AlbumEntity
import com.manikandan.domain.data.entities.SongEntity

enum class UnifiedShelfFilter {
    ALL,
    STREAMING,
    ALBUMS,
    PERSONAL_AUDIO,
}

sealed class UnifiedShelfItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val thumbnailUrl: String?
    abstract val filterType: UnifiedShelfFilter

    data class StreamingSong(
        val song: SongEntity,
    ) : UnifiedShelfItem() {
        override val id: String get() = song.videoId
        override val title: String get() = song.title
        override val subtitle: String get() = (song.artistName?.joinToString(", ") ?: "Artist") + " • " + song.duration
        override val thumbnailUrl: String? get() = song.thumbnails
        override val filterType: UnifiedShelfFilter get() = UnifiedShelfFilter.STREAMING
    }

    data class Album(
        val album: AlbumEntity,
    ) : UnifiedShelfItem() {
        override val id: String get() = album.browseId
        override val title: String get() = album.title
        override val subtitle: String get() = (album.artistName?.joinToString(", ") ?: "Artist") + " • " + album.trackCount + " tracks" + (album.year?.let { " • $it" } ?: "")
        override val thumbnailUrl: String? get() = album.thumbnails
        override val filterType: UnifiedShelfFilter get() = UnifiedShelfFilter.ALBUMS
    }

    data class PersonalAudio(
        val song: SongEntity,
    ) : UnifiedShelfItem() {
        override val id: String get() = song.videoId
        override val title: String get() = song.title
        override val subtitle: String get() = (song.artistName?.joinToString(", ") ?: "Personal Audio") + " • " + song.duration + " • Offline"
        override val thumbnailUrl: String? get() = song.thumbnails
        override val filterType: UnifiedShelfFilter get() = UnifiedShelfFilter.PERSONAL_AUDIO
    }
}
