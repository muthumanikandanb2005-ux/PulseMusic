package com.manikandan.spotify

import com.manikandan.spotify.model.response.spotify.SpotifyFeaturedPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistDetailResponse
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedResponse
import com.manikandan.spotify.model.response.spotify.SpotifySavedTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTopTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTracksSearchResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserPlaylistsResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotifyVerificationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun nullableSlotsInSpotifyResponsesDeserializeSafely() {
        val playlistsRaw = """
            {
              "items": [
                null,
                {
                  "id": "pl_1",
                  "name": "Liked Vibes",
                  "images": [{"url": "https://i.scdn.co/image/1", "height": 300, "width": 300}],
                  "tracks": {"total": 25},
                  "owner": {"id": "u1", "display_name": "Tester"}
                },
                null
              ],
              "total": 3
            }
        """.trimIndent()
        val plDecoded = json.decodeFromString<SpotifyUserPlaylistsResponse>(playlistsRaw)
        assertEquals(1, plDecoded.items.filterNotNull().size)
        assertEquals("pl_1", plDecoded.items.filterNotNull().first().id)

        val recentRaw = """
            {
              "items": [
                null,
                {"track": {"id": "t1", "name": "Track One", "artists": [{"id": "a1", "name": "Artist 1"}]}, "played_at": "2026-11-25T10:00:00Z"},
                {"track": null}
              ]
            }
        """.trimIndent()
        val recentDecoded = json.decodeFromString<SpotifyRecentlyPlayedResponse>(recentRaw)
        assertEquals(1, recentDecoded.items.mapNotNull { it?.track }.size)

        val topRaw = """{"items": [null, {"id": "top1", "name": "Top Hit"}]}"""
        assertEquals(1, json.decodeFromString<SpotifyTopTracksResponse>(topRaw).items.filterNotNull().size)

        val savedRaw = """{"items": [null, {"track": null}, {"track": {"id": "s1", "name": "Saved Track"}}]}"""
        assertEquals(1, json.decodeFromString<SpotifySavedTracksResponse>(savedRaw).items.mapNotNull { it?.track }.size)

        val searchTrackRaw = """{"tracks": {"items": [null, {"id": "st1", "name": "Search Hit"}]}}"""
        assertEquals(1, json.decodeFromString<SpotifyTracksSearchResponse>(searchTrackRaw).tracks.items.filterNotNull().size)

        val featuredRaw = """{"playlists": {"items": [null, {"id": "fp1", "name": "Viral 50"}]}}"""
        assertEquals(1, json.decodeFromString<SpotifyFeaturedPlaylistsResponse>(featuredRaw).playlists.items.filterNotNull().size)

        val detailRaw = """
            {
              "id": "dp1",
              "name": "Trending Playlist",
              "tracks": {
                "items": [
                  null,
                  {"track": null},
                  {"track": {"id": "dt1", "name": "Detail Song", "duration_ms": 200000}}
                ],
                "total": 3
              }
            }
        """.trimIndent()
        val detailDecoded = json.decodeFromString<SpotifyPlaylistDetailResponse>(detailRaw)
        assertEquals(1, detailDecoded.tracks.items.mapNotNull { it?.track }.size)
    }

    @Test
    fun spotifyClientPublicEndpointsRespondCleanly() = runBlocking {
        val client = SpotifyClient()
        val openHomeStatus = runCatching { client.getSpotifyOpenHomeHtml("").status.value }.getOrDefault(-1)
        assertTrue(openHomeStatus in 200..499, "Expected valid HTTP response from open.spotify.com, got $openHomeStatus")

        val clientTokenStatus = runCatching { client.getSpotifyClientToken().status.value }.getOrDefault(-1)
        assertTrue(clientTokenStatus == -1 || clientTokenStatus in 200..599)
    }
}
