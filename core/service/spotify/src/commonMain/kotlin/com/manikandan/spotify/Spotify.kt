package com.manikandan.spotify

import com.manikandan.spotify.auth.SpotifyAuth
import com.manikandan.spotify.model.response.spotify.CanvasResponse
import com.manikandan.spotify.model.response.spotify.ClientTokenResponse
import com.manikandan.spotify.model.response.spotify.PersonalTokenResponse
import com.manikandan.spotify.model.response.spotify.SpotifyFeaturedPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyLyricsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistDetailResponse
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistItem
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedItem
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedResponse
import com.manikandan.spotify.model.response.spotify.SpotifySavedTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTopTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTrackItem
import com.manikandan.spotify.model.response.spotify.SpotifyTracksSearchResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserProfileResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.search.SpotifySearchResponse
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Spotify {
    private val spotifyClient = SpotifyClient()
    private val spotifyAuth = SpotifyAuth(spotifyClient)
    private val tokenMutex = Mutex()
    private var cachedSpdc: String = ""
    private var cachedAccessToken: String? = null
    private var tokenMark: TimeMark? = null
    private var cachedTtlMs: Long = 1_800_000L

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

    private suspend fun extractAccessTokenFromWebSession(spdc: String): String? =
        runCatching {
            val html = spotifyClient.getSpotifyOpenHomeHtml(spdc).bodyAsText()
            val sessionMatch = Regex("""<script id="session"[^>]*>([\s\S]*?)</script>""").find(html)
                ?: Regex("""<script id="appServerConfig"[^>]*>([\s\S]*?)</script>""").find(html)
            val jsonText = sessionMatch?.groupValues?.getOrNull(1)?.trim()
            val token = jsonText?.let {
                Json { ignoreUnknownKeys = true }.parseToJsonElement(it).jsonObject["accessToken"]?.jsonPrimitive?.content
            } ?: Regex(""""accessToken"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
            token?.takeIf { it.isNotBlank() && it.length > 20 }
        }.getOrNull()

    suspend fun getValidAccessToken(spdc: String, forceRefresh: Boolean = false): String? = tokenMutex.withLock {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        if (cleanSpdc.isBlank()) return@withLock null

        val mark = tokenMark
        if (!forceRefresh &&
            cachedSpdc == cleanSpdc &&
            !cachedAccessToken.isNullOrBlank() &&
            mark != null &&
            mark.elapsedNow().inWholeMilliseconds < (cachedTtlMs - 60_000L)
        ) {
            return@withLock cachedAccessToken
        }

        // 1. Prefer TOTP token (most reliable current Spotify Web API flow)
        val totp = getPersonalTokenWithTotp(cleanSpdc).getOrNull()?.accessToken?.takeIf { it.isNotEmpty() }
        if (totp != null) {
            cachedSpdc = cleanSpdc
            cachedAccessToken = totp
            tokenMark = TimeSource.Monotonic.markNow()
            return@withLock totp
        }

        // 2. Fallback: standard web-player transport token
        val standard = getPersonalToken(cleanSpdc).getOrNull()?.accessToken?.takeIf { it.isNotEmpty() }
        if (standard != null) {
            cachedSpdc = cleanSpdc
            cachedAccessToken = standard
            tokenMark = TimeSource.Monotonic.markNow()
            return@withLock standard
        }

        // 3. Fallback: scrape SSR session script directly from open.spotify.com with sp_dc cookie
        val htmlToken = extractAccessTokenFromWebSession(cleanSpdc)
        if (htmlToken != null) {
            cachedSpdc = cleanSpdc
            cachedAccessToken = htmlToken
            tokenMark = TimeSource.Monotonic.markNow()
            return@withLock htmlToken
        }

        null
    }

    suspend fun fetchPlaylistsWithSpdc(spdc: String): Result<List<SpotifyPlaylistItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val response = spotifyClient.getSpotifyUserPlaylists(token, limit = 50)
        val body = if (response.status.value == 401) {
            val fresh = getValidAccessToken(spdc, forceRefresh = true) ?: throw Exception("Unauthorized")
            spotifyClient.getSpotifyUserPlaylists(fresh, limit = 50).body<SpotifyUserPlaylistsResponse>()
        } else {
            response.body<SpotifyUserPlaylistsResponse>()
        }
        body.items.filterNotNull()
    }

    suspend fun fetchRecentlyPlayedWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyRecentlyPlayedItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val recentItems = runCatching {
            spotifyClient.getSpotifyRecentlyPlayed(token, limit).body<SpotifyRecentlyPlayedResponse>().items.filterNotNull()
        }.getOrDefault(emptyList())
        if (recentItems.isNotEmpty()) return@runCatching recentItems

        // Fallback 1: User's short-term top tracks (Spotify frequently returns empty on /recently-played when listening on mobile/connect)
        val topTracks = runCatching {
            spotifyClient.getSpotifyTopTracks(token, limit).body<SpotifyTopTracksResponse>().items.filterNotNull()
        }.getOrDefault(emptyList())
        if (topTracks.isNotEmpty()) {
            return@runCatching topTracks.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
        }

        // Fallback 2: User's Saved / Liked Tracks library
        val savedTracks = runCatching {
            spotifyClient.getSpotifySavedTracks(token, limit).body<SpotifySavedTracksResponse>().items.mapNotNull { it?.track }
        }.getOrDefault(emptyList())
        savedTracks.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
    }

    suspend fun fetchUserProfileWithSpdc(spdc: String): Result<SpotifyUserProfileResponse> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        getUserProfile(token).getOrThrow()
    }

    suspend fun fetchFeaturedPlaylistsWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyPlaylistItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val collected = LinkedHashMap<String, SpotifyPlaylistItem>()

        // 1. Live search for Viral & Top Charts playlists (replaces deprecated /v1/browse/featured-playlists)
        listOf("Top 50 Viral Hits", "Trending Songs Charts").forEach { query ->
            if (collected.size < limit) {
                runCatching {
                    val searchRes = spotifyClient.searchSpotifyPlaylists(token, query, limit).body<SpotifyFeaturedPlaylistsResponse>()
                    searchRes.playlists.items.filterNotNull().forEach { item ->
                        if (item.id.isNotBlank() && item.name.isNotBlank()) {
                            collected.putIfAbsent(item.id, item)
                        }
                    }
                }
            }
        }

        // 2. Fallback editorial chart IDs + legacy featured-playlists if needed
        if (collected.isEmpty()) {
            val chartIds = listOf(
                "37i9dQZEVXbMDoHDwVN2tF", // Top 50 - Global
                "37i9dQZEVXbLZ52XmnySJg", // Top 50 - India
                "37i9dQZEVXbLiRSasKsNU9", // Viral 50 - Global
                "37i9dQZF1DXcBWIGoYBM5M", // Today's Top Hits
            )
            chartIds.forEach { chartId ->
                runCatching {
                    val pl = spotifyClient.getSpotifyPlaylistById(token, chartId).body<SpotifyPlaylistDetailResponse>()
                    if (pl.id.isNotBlank() && pl.name.isNotBlank()) {
                        collected.putIfAbsent(
                            pl.id,
                            SpotifyPlaylistItem(
                                id = pl.id,
                                name = pl.name,
                                description = pl.description,
                                images = pl.images,
                                owner = pl.owner,
                            ),
                        )
                    }
                }
            }
        }

        if (collected.isEmpty()) {
            runCatching {
                val legacy = spotifyClient.getSpotifyFeaturedPlaylists(token, limit).body<SpotifyFeaturedPlaylistsResponse>()
                legacy.playlists.items.filterNotNull().forEach { item ->
                    if (item.id.isNotBlank()) collected.putIfAbsent(item.id, item)
                }
            }
        }

        collected.values.take(limit)
    }

    suspend fun fetchTrendingTracksWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyTrackItem>> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val tracks = mutableListOf<SpotifyTrackItem>()
        // Try Top 50 India / Global playlists first
        for (chartId in listOf("37i9dQZEVXbLZ52XmnySJg", "37i9dQZEVXbMDoHDwVN2tF", "37i9dQZF1DXcBWIGoYBM5M")) {
            val res = runCatching {
                spotifyClient.getSpotifyPlaylistById(token, chartId).body<SpotifyPlaylistDetailResponse>()
            }.getOrNull()
            val list = res?.tracks?.items?.mapNotNull { it?.track }?.filter { it.name.isNotBlank() }.orEmpty()
            if (list.isNotEmpty()) {
                tracks.addAll(list)
                break
            }
        }
        // Fallback: Search trending tracks
        if (tracks.isEmpty()) {
            val searched = runCatching {
                spotifyClient.searchSpotifyTracksApi(token, "Trending Hits Top 50", limit).body<SpotifyTracksSearchResponse>()
            }.getOrNull()
            tracks.addAll(searched?.tracks?.items?.mapNotNull { it }?.filter { it.name.isNotBlank() }.orEmpty())
        }
        tracks.distinctBy { it.id.ifBlank { it.name } }.take(limit)
    }

    suspend fun fetchPlaylistByIdWithSpdc(spdc: String, playlistId: String): Result<SpotifyPlaylistDetailResponse> = runCatching {
        val token = getValidAccessToken(spdc) ?: throw Exception("Failed to obtain Spotify access token")
        val cleanId = playlistId.removePrefix("spotify:").substringBefore("?")
        spotifyClient.getSpotifyPlaylistById(token, cleanId).body<SpotifyPlaylistDetailResponse>()
    }
}