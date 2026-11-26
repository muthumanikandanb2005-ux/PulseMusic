package com.manikandan.data.service

import com.manikandan.domain.data.model.searchResult.songs.Artist
import com.manikandan.domain.data.model.searchResult.songs.SongsResult
import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.extension.bestThumbnailUrl
import com.manikandan.domain.extension.upgradeArtworkUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackMatcherAndArtworkTest {
    @Test
    fun rejectsUnrelatedFallbackTracksAndAcceptsExactMatch() {
        // Simulates two distinct Spotify songs where JioSaavn previously returned the same unrelated hit.
        val unrelatedHit = TrackMatcher.scoreCandidate(
            expectedTitle = "Blinding Lights",
            expectedArtist = "The Weeknd",
            candidateTitle = "Kesariya",
            candidateArtists = "Arijit Singh, Pritam",
            expectedDurationSec = 200,
            candidateDurationSec = 268,
        )
        assertFalse(TrackMatcher.isAcceptableMatch(unrelatedHit), "Unrelated hit must be rejected")

        val wrongArtistSameShortWord = TrackMatcher.scoreCandidate(
            expectedTitle = "Stay",
            expectedArtist = "The Kid LAROI, Justin Bieber",
            candidateTitle = "Stay",
            candidateArtists = "Rihanna, Mikky Ekko",
            expectedDurationSec = 141,
            candidateDurationSec = 240,
        )
        assertFalse(TrackMatcher.isAcceptableMatch(wrongArtistSameShortWord), "Conflicting artist short title must be rejected")

        val validMatch = TrackMatcher.scoreCandidate(
            expectedTitle = "Blinding Lights",
            expectedArtist = "The Weeknd",
            candidateTitle = "Blinding Lights (Official Audio)",
            candidateArtists = "The Weeknd",
            expectedDurationSec = 200,
            candidateDurationSec = 201,
        )
        assertTrue(TrackMatcher.isAcceptableMatch(validMatch), "Matching track should be accepted: $validMatch")
    }

    @Test
    fun penalizesUnrequestedRemixOverStudioCut() {
        val remixScore = TrackMatcher.scoreCandidate(
            expectedTitle = "Starboy",
            expectedArtist = "The Weeknd",
            candidateTitle = "Starboy (Slowed + Reverb Remix)",
            candidateArtists = "The Weeknd",
        )
        val originalScore = TrackMatcher.scoreCandidate(
            expectedTitle = "Starboy",
            expectedArtist = "The Weeknd",
            candidateTitle = "Starboy (feat. Daft Punk)",
            candidateArtists = "The Weeknd, Daft Punk",
        )
        assertTrue(originalScore > remixScore, "Original version ($originalScore) must outrank unrequested remix ($remixScore)")
    }

    @Test
    fun rankSearchResultsMovesExactTrackAboveUnrelatedFirstItem() {
        val wrongFirst = SongsResult(
            album = null,
            artists = listOf(Artist(id = "", name = "Random Artist")),
            category = "Song",
            duration = "4:00",
            durationSeconds = 240,
            feedbackTokens = null,
            isExplicit = false,
            resultType = "Song",
            thumbnails = emptyList(),
            title = "Completely Different Song",
            videoId = "saavn:wrong1",
            videoType = "Song",
            year = "2024",
        )
        val rightSecond = SongsResult(
            album = null,
            artists = listOf(Artist(id = "", name = "The Weeknd")),
            category = "Song",
            duration = "3:20",
            durationSeconds = 200,
            feedbackTokens = null,
            isExplicit = false,
            resultType = "Song",
            thumbnails = emptyList(),
            title = "Blinding Lights",
            videoId = "yt_exact_1",
            videoType = "Song",
            year = "2020",
        )
        val ranked = TrackMatcher.rankSearchResults("Blinding Lights The Weeknd", listOf(wrongFirst, rightSecond))
        assertEquals("yt_exact_1", ranked.first().videoId)
    }

    @Test
    fun upgradeArtworkUrlProducesFullQualityUrlsAndBestThumbnailPicksMaxWidth() {
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273abcdef",
            upgradeArtworkUrl("https://i.scdn.co/image/ab67616d00004851abcdef"),
        )
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273abcdef",
            upgradeArtworkUrl("https://i.scdn.co/image/ab67616d00001e02abcdef"),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/xyz=w1080-h1080-l90-rj",
            upgradeArtworkUrl("https://lh3.googleusercontent.com/xyz=w60-h60-l90-rj"),
        )
        assertEquals(
            "https://c.saavncdn.com/123/art-500x500.jpg",
            upgradeArtworkUrl("https://c.saavncdn.com/123/art-50x50.jpg"),
        )

        val spotifyOrderedDescending = listOf(
            Thumbnail(height = 640, width = 640, url = "https://i.scdn.co/image/ab67616d0000b273big"),
            Thumbnail(height = 300, width = 300, url = "https://i.scdn.co/image/ab67616d00001e02med"),
            Thumbnail(height = 64, width = 64, url = "https://i.scdn.co/image/ab67616d00004851tiny"),
        )
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273big",
            spotifyOrderedDescending.bestThumbnailUrl(),
        )
    }
}
