package com.manikandan.spotify

import com.manikandan.spotify.auth.SpotifyAuth
import com.manikandan.spotify.model.response.spotify.CanvasResponse
import com.manikandan.spotify.model.response.spotify.ClientTokenResponse
import com.manikandan.spotify.model.response.spotify.PersonalTokenResponse
import com.manikandan.spotify.model.response.spotify.SpotifyLyricsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistItem
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedItem
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserProfileResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.search.SpotifySearchResponse
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http

class Spotify {
    private val spotifyClient = SpotifyClient()
    private val spotifyAuth = SpotifyAuth(spotifyClient)

    /**
     * Remove proxy for client
     */
    fun removeProxy() {
        spotifyClient.proxy = null
    }

    /**
     * Set the proxy for client
     */
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        val verifiedHost =
            if (!host.contains("http")) {
                "http://$host"
            } else {
                host
            }
        runCatching {
            if (isHttp) ProxyBuilder.http("$verifiedHost:$port") else ProxyBuilder.socks(verifiedHost, port)
        }.onSuccess {
            spotifyClient.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

    /**
     * Get personal token using the standard method
     */
    suspend fun getPersonalToken(spdc: String) =
        runCatching {
            spotifyClient.getSpotifyLyricsToken(spdc).body<PersonalTokenResponse>()
        }

    /**
     * Get personal token using the more reliable TOTP-based method
     * This should be used when the standard method fails
     */
    suspend fun getPersonalTokenWithTotp(spdc: String) = spotifyAuth.refreshToken(spdc)

    suspend fun getClientToken() =
        runCatching {
            spotifyClient
                .getSpotifyClientToken()
                .body<ClientTokenResponse>()
        }

    suspend fun searchSpotifyTrack(
        query: String,
        authToken: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient
            .searchSpotifyTrack(query, authToken, clientToken)
            .body<SpotifySearchResponse>()
    }

    suspend fun getSpotifyLyrics(
        trackId: String,
        token: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient
            .getSpotifyLyrics(
                token = token,
                clientToken = clientToken,
                trackId,
            ).body<SpotifyLyricsResponse>()
    }

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient.getSpotifyCanvas(trackId, token, clientToken).body<CanvasResponse>()
    }

    suspend fun getUserProfile(token: String) = runCatching {
        spotifyClient.getSpotifyUserProfile(token).body<SpotifyUserProfileResponse>()
    }

    suspend fun getUserPlaylists(token: String, limit: Int = 50) = runCatching {
        spotifyClient.getSpotifyUserPlaylists(token, limit).body<SpotifyUserPlaylistsResponse>()
    }

    suspend fun getRecentlyPlayed(token: String, limit: Int = 20) = runCatching {
        spotifyClient.getSpotifyRecentlyPlayed(token, limit).body<SpotifyRecentlyPlayedResponse>()
    }

    suspend fun getValidAccessToken(spdc: String): String? {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val standard = getPersonalToken(cleanSpdc).getOrNull()?.accessToken?.takeIf { it.isNotEmpty() }
        if (standard != null) return standard
        val totp = getPersonalTokenWithTotp(cleanSpdc).getOrNull()?.accessToken?.takeIf { it.isNotEmpty() }
        return totp
    }

    suspend fun fetchPlaylistsWithSpdc(spdc: String): Result<List<SpotifyPlaylistItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val res = getUserPlaylists(token).getOrThrow()
        res.items
    }

    suspend fun fetchRecentlyPlayedWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyRecentlyPlayedItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val res = getRecentlyPlayed(token, limit).getOrThrow()
        res.items
    }

    suspend fun fetchUserProfileWithSpdc(spdc: String): Result<SpotifyUserProfileResponse> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        getUserProfile(token).getOrThrow()
    }

    suspend fun fetchFeaturedPlaylistsWithSpdc(spdc: String, limit: Int = 20): Result<List<com.manikandan.spotify.model.response.spotify.SpotifyPlaylistItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val res = spotifyClient.getSpotifyFeaturedPlaylists(token, limit).body<com.manikandan.spotify.model.response.spotify.SpotifyFeaturedPlaylistsResponse>()
        res.playlists.items
    }
}