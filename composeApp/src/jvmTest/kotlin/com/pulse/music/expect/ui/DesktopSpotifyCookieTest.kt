package com.pulse.music.expect.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSpotifyCookieTest {
    @Test
    fun `sanitizeSpotifyCookie extracts sp_dc from raw token, header, cURL, and Netscape formats`() {
        val token = "AQB_example_spotify_session_token_1234567890"

        val fromBare = sanitizeSpotifyCookie(token)
        assertEquals("sp_dc=$token", fromBare)
        assertTrue(hasSpotifyAuthTokens(fromBare))

        val fromHeader = sanitizeSpotifyCookie("cookie: sp_t=abc123; sp_dc=$token; sp_landing=xyz")
        assertEquals("sp_dc=$token", fromHeader)
        assertTrue(hasSpotifyAuthTokens(fromHeader))

        val fromCurlHeader = sanitizeSpotifyCookie(
            "curl 'https://open.spotify.com/' -H 'accept: */*' -H 'cookie: sp_t=abc; sp_dc=$token; sp_key=k'",
        )
        assertEquals("sp_dc=$token", fromCurlHeader)
        assertTrue(hasSpotifyAuthTokens(fromCurlHeader))

        val fromCurlB = sanitizeSpotifyCookie(
            "curl 'https://api-partner.spotify.com/' -b 'sp_t=abc; sp_dc=$token'",
        )
        assertEquals("sp_dc=$token", fromCurlB)
        assertTrue(hasSpotifyAuthTokens(fromCurlB))

        val fromNetscape = """
            # Netscape HTTP Cookie File
            .spotify.com	TRUE	/	TRUE	1893456000	sp_t	abc
            .spotify.com	TRUE	/	TRUE	1893456000	sp_dc	$token
        """.trimIndent()
        val netscapeResult = sanitizeSpotifyCookie(fromNetscape)
        assertEquals("sp_dc=$token", netscapeResult)
        assertTrue(hasSpotifyAuthTokens(netscapeResult))
    }

    @Test
    fun `hasSpotifyAuthTokens rejects blank or non-Spotify cookies while YouTube tokens stay valid`() {
        assertFalse(hasSpotifyAuthTokens(""))
        assertFalse(hasSpotifyAuthTokens("SAPISID=yt123; __Secure-3PAPISID=yt456"))
        assertTrue(hasYouTubeAuthTokens("SAPISID=yt123; __Secure-3PAPISID=yt456"))
    }
}
