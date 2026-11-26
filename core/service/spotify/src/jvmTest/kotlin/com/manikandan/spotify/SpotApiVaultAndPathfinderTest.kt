package com.manikandan.spotify

import com.manikandan.spotify.auth.SpotApiVault
import com.manikandan.spotify.auth.SpotifyTotp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpotApiVaultAndPathfinderTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun vaultDecodesClientConfigAndQueryHashesCleanly() {
        val hex32 = Regex("^[0-9a-f]{32}$")
        val hex64 = Regex("^[0-9a-f]{64}$")

        val clientId = SpotApiVault.defaultClientId()
        assertTrue(hex32.matches(clientId), "Client ID must decode to 32-char hex")

        val appVer = SpotApiVault.defaultAppVersion()
        assertTrue(appVer.contains(".") && appVer.length >= 6, "App version must be valid")

        val deviceId = SpotApiVault.defaultDeviceId()
        assertEquals(36, deviceId.length, "Device ID must decode to UUID length")

        val (totpVersion, cipher) = SpotApiVault.fallbackTotpSecret()
        assertEquals(61, totpVersion)
        assertTrue(cipher.size >= 16)

        val expectedOps = listOf(
            "searchTracks",
            "searchDesktop",
            "getAlbum",
            "getTrack",
            "queryArtistOverview",
            "queryArtistRelated",
            "fetchExtractedColors",
            "similarAlbumsBasedOnThisTrack",
        )
        for (op in expectedOps) {
            val hash = SpotApiVault.fallbackQueryHash(op)
            assertNotNull(hash, "Expected fallback hash for $op")
            assertTrue(hex64.matches(hash), "Expected 64-char sha256 for $op")
        }
    }

    @Test
    fun totpDecodesCipherAndGeneratesSixDigitCode() {
        val raw = "A1b2C3"
        val decoded = SpotifyTotp.decodeRawSecretString(raw)
        assertEquals(listOf(65, 49, 98, 50, 67, 51), decoded)

        val (version, cipher) = SpotApiVault.fallbackTotpSecret()
        assertEquals(61, version)
        val otp = SpotifyTotp.at(1_700_000_000_000L, version to cipher)
        assertEquals(6, otp.length)
        assertTrue(otp.all { ch -> ch.isDigit() }, "Generated OTP must contain 6 digits")
    }

    @Test
    fun bundleSessionExtractsLiveQueryHashesTotpAndWebpackChunks() = kotlinx.coroutines.runBlocking {
        val session = SpotApiBundleSession(SpotifyClient())
        val fakeHash = "a".repeat(64)
        val sampleJs = """
            "use strict";
            var l={secret:"X!9",version:62};
            e.exports=n.p+"fetchPlaylist","query","$fakeHash";
            __webpack_require__.u=e=>""+({12:"xpui-routes-playlist",99:"vendor"}[e]||e)+"."+{12:"abcd1234",99:"ef567890"}[e]+".js";
        """.trimIndent()

        session.ingestBundleSource("https://open.spotifycdn.com/cdn/build/web-player/web-player.1234.js", sampleJs)
        assertEquals(fakeHash, session.resolveQueryHash("fetchPlaylist"))

        val chunks = session.extractWebpackChunkLinks(sampleJs)
        assertEquals(2, chunks.size)
        assertTrue(chunks.any { it.endsWith("xpui-routes-playlist.abcd1234.js") })
    }

    @Test
    fun pathfinderParsersExtractPlaylistMetadataAndTracks() {
        val spotify = Spotify()
        val payload = """
            {
              "uri": "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
              "name": "Today's Top Hits",
              "description": "Global chart",
              "images": {
                "items": [
                  {
                    "sources": [
                      {"url": "https://i.scdn.co/image/cover300", "width": 300, "height": 300},
                      {"url": "https://i.scdn.co/image/cover640", "width": 640, "height": 640}
                    ]
                  }
                ]
              },
              "ownerV2": {
                "data": {
                  "name": "Spotify",
                  "username": "spotify"
                }
              },
              "content": {
                "totalCount": 50,
                "items": [
                  {
                    "itemV2": {
                      "data": {
                        "uri": "spotify:track:4uLU6hMCjMI75M1A2tKUQC",
                        "name": "Starlight",
                        "trackDuration": {"totalMilliseconds": 213000},
                        "artists": {
                          "items": [
                            {"uri": "spotify:artist:12Chz98pHFMPJEknJQMWvI", "profile": {"name": "Muse"}}
                          ]
                        },
                        "albumOfTrack": {
                          "uri": "spotify:album:0lw68yx3MhKflWFqCsGkIs",
                          "name": "Black Holes and Revelations",
                          "coverArt": {
                            "sources": [
                              {"url": "https://i.scdn.co/image/album640", "width": 640, "height": 640}
                            ]
                          }
                        }
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val root = json.parseToJsonElement(payload).jsonObject
        val summary = spotify.parsePathfinderPlaylistSummary("37i9dQZF1DXcBWIGoYBM5M", "Today's Top Hits", root)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", summary.id)
        assertEquals("Spotify", summary.owner?.displayName)
        assertEquals(50, summary.tracks?.total)
        assertEquals("https://i.scdn.co/image/cover640", summary.images.firstOrNull()?.url)
        assertEquals("https://i.scdn.co/image/cover640", summary.images.lastOrNull()?.url)

        val entries = spotify.parsePathfinderPlaylistEntries(root["content"]!!.jsonObject["items"]!!.jsonArray)
        assertEquals(1, entries.size)
        val track = entries.first().track
        assertNotNull(track)
        assertEquals("4uLU6hMCjMI75M1A2tKUQC", track.id)
        assertEquals("Starlight", track.name)
        assertEquals(213000L, track.durationMs)
        assertEquals("Muse", track.artists.firstOrNull()?.name)
        assertEquals("Black Holes and Revelations", track.album?.name)
    }
}
