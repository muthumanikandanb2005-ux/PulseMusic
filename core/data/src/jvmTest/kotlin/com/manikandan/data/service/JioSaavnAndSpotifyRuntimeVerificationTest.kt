package com.manikandan.data.service

import com.manikandan.data.mediaservice.TrackLanguageFilter
import com.manikandan.data.parser.OnlinePlaylistFetcher
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.searchResult.songs.Artist
import com.manikandan.domain.utils.toTrack
import com.manikandan.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JioSaavnAndSpotifyRuntimeVerificationTest {

    private fun fakeTrack(id: String, title: String, artist: String): Track = Track(
        album = null,
        artists = listOf(Artist(id = null, name = artist)),
        duration = "3:30",
        durationSeconds = 210,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = emptyList(),
        title = title,
        videoId = id,
        videoType = "Song",
        category = "SONG",
        feedbackTokens = null,
        resultType = "Song",
        year = null,
    )

    @Test
    fun languageFilterNormalizesExtendedIndicCodesAndBlocksCrossLanguageEvenWithSharedArtist() {
        assertEquals("ta", TrackLanguageFilter.normalizeLanguageCode("Tamil"))
        assertEquals("hi", TrackLanguageFilter.normalizeLanguageCode("Hindi"))
        assertEquals("mr", TrackLanguageFilter.normalizeLanguageCode("Marathi"))
        assertEquals("gu", TrackLanguageFilter.normalizeLanguageCode("Gujarati"))
        assertEquals("bho", TrackLanguageFilter.normalizeLanguageCode("Bhojpuri"))
        assertEquals("tamil", TrackLanguageFilter.toLanguageQueryName("ta"))

        val tamilSeed = fakeTrack("seed1", "Munbe Vaa (From \"Sillunu Oru Kaadhal\") (Tamil)", "A.R. Rahman")
        val sameArtistHindi = fakeTrack("cand1", "Kun Faya Kun (Hindi)", "A.R. Rahman")
        val sameLangTamil = fakeTrack("cand2", "New York Nagaram (Tamil)", "A.R. Rahman")

        assertFalse(
            TrackLanguageFilter.isCompatibleLanguage(sameArtistHindi, listOf(tamilSeed)),
            "Shared multilingual artist must NOT allow Hindi track into Tamil radio",
        )
        assertTrue(
            TrackLanguageFilter.isCompatibleLanguage(sameLangTamil, listOf(tamilSeed)),
            "Same Tamil track should match Tamil seed",
        )
    }

    @Test
    fun liveJioSaavnSameLanguageEndlessQueueProducesUniqueContinuousSameLanguageBatches() = runBlocking {
        val searchResults = JioSaavnService.searchSongs("Naa Ready Leo Tamil")
        assertTrue(searchResults.isNotEmpty(), "Expected live JioSaavn search results for Naa Ready")

        val seedTrack = searchResults.first().toTrack()
        assertTrue(seedTrack.videoId.startsWith("saavn:"), "Expected saavn: prefix, got ${seedTrack.videoId}")

        val playlistId = "${JioSaavnService.SAAVN_RADIO_PREFIX}${seedTrack.videoId}"
        val (batch1, cont1) = JioSaavnService.getRelatedRadio(seedTrack.videoId)
        assertTrue(batch1.isNotEmpty(), "First endless radio batch must not be empty")
        assertNotNull(cont1, "First batch must return continuation cursor")
        assertTrue(cont1.startsWith(JioSaavnService.SAAVN_CONT_PREFIX), "Unexpected continuation: $cont1")

        val seedRef = listOf(seedTrack)
        batch1.forEach { track ->
            assertTrue(track.videoId.startsWith("saavn:"))
            assertTrue(
                TrackLanguageFilter.isCompatibleLanguage(track, seedRef),
                "Batch 1 track '${track.title}' (lang=${JioSaavnService.getTrackLanguage(track.videoId)}) leaked outside seed language",
            )
        }

        val exclude1 = batch1.map { it.videoId }.toMutableSet().apply { add(seedTrack.videoId) }
        val (batch2, cont2) = JioSaavnService.getContinueRadioTracks(playlistId, cont1, excludeIds = exclude1)
        assertTrue(batch2.isNotEmpty(), "Second endless radio batch must not be empty")
        assertNotNull(cont2, "Second batch must return next continuation cursor")

        val overlap12 = batch2.map { it.videoId }.intersect(exclude1)
        assertTrue(overlap12.isEmpty(), "Batch 2 repeated tracks already in queue: $overlap12")
        batch2.forEach { track ->
            assertTrue(
                TrackLanguageFilter.isCompatibleLanguage(track, seedRef),
                "Batch 2 track '${track.title}' (lang=${JioSaavnService.getTrackLanguage(track.videoId)}) leaked outside seed language",
            )
        }

        val exclude2 = exclude1 + batch2.map { it.videoId }
        val (batch3, cont3) = JioSaavnService.getContinueRadioTracks(playlistId, cont2, excludeIds = exclude2)
        assertTrue(batch3.isNotEmpty(), "Third endless radio batch must not be empty")
        assertNotNull(cont3, "Third batch must return next continuation cursor")
        assertTrue(batch3.map { it.videoId }.intersect(exclude2).isEmpty(), "Batch 3 repeated prior tracks")

        println(
            "JioSaavn live endless radio verified: seed='${seedTrack.title}' (lang=${JioSaavnService.getTrackLanguage(seedTrack.videoId)}), " +
                "batch1=${batch1.size}, batch2=${batch2.size}, batch3=${batch3.size}",
        )
    }

    @Test
    fun liveSpotifyEmbedPlaylistFallbackReturnsTrendingTracks() = runBlocking {
        val parsed = OnlinePlaylistFetcher.fetch(
            url = "https://open.spotify.com/playlist/37i9dQZEVXbLZ52XmnySJg",
            youTube = YouTube(),
        )
        assertNotNull(parsed, "Spotify Top 50 India embed fetch should succeed")
        assertTrue(parsed.tracks.isNotEmpty(), "Expected trending tracks from Spotify embed")
        println("Spotify live embed verified: title='${parsed.title}', tracks=${parsed.tracks.size}")
    }
}
