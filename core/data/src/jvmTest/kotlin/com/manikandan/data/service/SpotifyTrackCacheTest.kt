package com.manikandan.data.service

import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.searchResult.songs.Album
import com.manikandan.domain.data.model.searchResult.songs.Artist
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SpotifyTrackCacheTest {
    @Test
    fun cacheStoresAndResolvesBothPrefixedAndBareIds() = runBlocking {
        val sample = Track(
            album = Album(id = "alb1", name = "Test Album"),
            artists = listOf(Artist(id = "art1", name = "Test Artist")),
            duration = "3:30",
            durationSeconds = 210,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = emptyList(),
            title = "Midnight City",
            videoId = "sp_track:4uLU6hMCjMI75M1A2tKUQC",
            videoType = "Song",
            category = "SONG",
            feedbackTokens = null,
            resultType = "Song",
            year = "",
        )
        SpotifyTrackCache.putAll(listOf(sample))

        val prefixed = SpotifyTrackCache.get("sp_track:4uLU6hMCjMI75M1A2tKUQC")
        assertNotNull(prefixed)
        assertEquals("Midnight City", prefixed.title)

        val bare = SpotifyTrackCache.get("4uLU6hMCjMI75M1A2tKUQC")
        assertNotNull(bare)
        assertEquals("Test Artist", bare.artists?.firstOrNull()?.name)
    }
}
