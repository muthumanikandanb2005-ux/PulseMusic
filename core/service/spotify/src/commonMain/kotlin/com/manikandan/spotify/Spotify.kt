package com.manikandan.spotify

import com.manikandan.spotify.auth.SpotApiVault
import com.manikandan.spotify.auth.SpotifyAuth
import com.manikandan.spotify.model.response.spotify.CanvasResponse
import com.manikandan.spotify.model.response.spotify.ClientTokenResponse
import com.manikandan.spotify.model.response.spotify.PersonalTokenResponse
import com.manikandan.spotify.model.response.spotify.SpotifyAlbumItem
import com.manikandan.spotify.model.response.spotify.SpotifyArtistItem
import com.manikandan.spotify.model.response.spotify.SpotifyFeaturedPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyImage
import com.manikandan.spotify.model.response.spotify.SpotifyLyricsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyOwner
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistDetailResponse
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistItem
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistTrackEntry
import com.manikandan.spotify.model.response.spotify.SpotifyPlaylistTracksPage
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedItem
import com.manikandan.spotify.model.response.spotify.SpotifyRecentlyPlayedResponse
import com.manikandan.spotify.model.response.spotify.SpotifySavedTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTopTracksResponse
import com.manikandan.spotify.model.response.spotify.SpotifyTrackItem
import com.manikandan.spotify.model.response.spotify.SpotifyTracksRef
import com.manikandan.spotify.model.response.spotify.SpotifyTracksSearchResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserPlaylistsResponse
import com.manikandan.spotify.model.response.spotify.SpotifyUserProfileResponse
import com.manikandan.spotify.model.response.spotify.search.SpotifySearchResponse
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.bodyAsText
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Spotify {
    private val spotifyClient = SpotifyClient()
    private val bundleSession = SpotApiBundleSession(spotifyClient)
    private val spotifyAuth = SpotifyAuth(spotifyClient, bundleSession)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private data class CachedSpotSession(
        val spdc: String,
        val accessToken: String,
        val clientToken: String,
        val clientId: String,
        val clientVersion: String,
        val ttlMs: Long,
        val mark: TimeMark,
    ) {
        fun isFresh(): Boolean = mark.elapsedNow().inWholeMilliseconds < (ttlMs - 60_000L)
    }

    private val tokenMutex = Mutex()
    private var authSession: CachedSpotSession? = null
    private var anonSession: CachedSpotSession? = null

    fun removeProxy() {
        spotifyClient.proxy = null
    }

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
        }
    }

    suspend fun getPersonalToken(spdc: String) =
        runCatching {
            spotifyClient.getSpotifyLyricsToken(spdc.removePrefix("sp_dc=").trim()).body<PersonalTokenResponse>()
        }

    suspend fun getPersonalTokenWithTotp(spdc: String) = spotifyAuth.refreshToken(spdc)

    suspend fun getClientToken() =
        runCatching {
            spotifyClient
                .getSpotifyClientToken(
                    clientId = SpotApiVault.defaultClientId(),
                    clientVersion = bundleSession.currentClientVersion(),
                )
                .body<ClientTokenResponse>()
        }

    suspend fun searchSpotifyTrack(
        query: String,
        authToken: String,
        clientToken: String,
    ) = runCatching {
        val dynamicHash = bundleSession.resolveQueryHash("searchTracks")
        spotifyClient
            .searchSpotifyTrack(query, authToken, clientToken, shaHash = dynamicHash)
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
                trackId = trackId,
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

    private suspend fun getOrCreateSession(
        spdc: String,
        forceRefresh: Boolean = false,
        allowAnonymousFallback: Boolean = false,
    ): CachedSpotSession? = tokenMutex.withLock {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        if (cleanSpdc.isBlank() && !allowAnonymousFallback) return@withLock null

        val cached = if (cleanSpdc.isNotBlank()) authSession else anonSession
        if (!forceRefresh &&
            cached != null &&
            cached.spdc == cleanSpdc &&
            cached.accessToken.isNotBlank() &&
            cached.clientToken.isNotBlank() &&
            cached.isFresh()
        ) {
            return@withLock cached
        }

        // Ensure bundle session is warm (live clientVersion, live TOTP secret, dynamic query hashes)
        val warm = runCatching { bundleSession.warmIfNeeded(cleanSpdc, force = forceRefresh) }.getOrNull()
        val clientVer = warm?.clientVersion ?: bundleSession.currentClientVersion()

        // 1. Obtain Personal / Web-Player Access Token (TOTP first -> transport -> SSR HTML fallback)
        var tokenResp = getPersonalTokenWithTotp(cleanSpdc).getOrNull()
        if (tokenResp == null || tokenResp.accessToken.isBlank()) {
            tokenResp = getPersonalToken(cleanSpdc).getOrNull()
        }
        val accessToken = tokenResp?.accessToken?.takeIf { it.isNotBlank() }
            ?: warm?.embeddedAccessToken?.takeIf { it.isNotBlank() }
            ?: return@withLock null

        val clientId = tokenResp?.clientId?.takeIf { it.isNotBlank() } ?: SpotApiVault.defaultClientId()
        val expMs = tokenResp?.accessTokenExpirationTimestampMs ?: 0L
        val ttlMs = if (expMs > 0L) {
            (expMs - getTimeMillis()).coerceIn(120_000L, 3_600_000L)
        } else {
            1_800_000L
        }

        // 2. Obtain companion Client-Token tied to clientId + clientVersion (required by SpotAPI Pathfinder v2)
        val existingClientToken = cached?.clientToken?.takeIf { !forceRefresh && it.isNotBlank() }
        val clientToken = existingClientToken
            ?: runCatching {
                spotifyClient.getSpotifyClientToken(
                    clientId = clientId,
                    clientVersion = clientVer,
                ).body<ClientTokenResponse>().grantedToken.token
            }.getOrNull()
            ?: ""

        val nextSession = CachedSpotSession(
            spdc = cleanSpdc,
            accessToken = accessToken,
            clientToken = clientToken,
            clientId = clientId,
            clientVersion = clientVer,
            ttlMs = ttlMs,
            mark = TimeSource.Monotonic.markNow(),
        )
        if (cleanSpdc.isNotBlank()) {
            authSession = nextSession
        } else {
            anonSession = nextSession
        }
        nextSession
    }

    suspend fun getValidAccessToken(spdc: String, forceRefresh: Boolean = false): String? =
        getOrCreateSession(spdc, forceRefresh = forceRefresh, allowAnonymousFallback = false)?.accessToken

    private suspend fun executePathfinder(
        spdc: String,
        operationName: String,
        variables: JsonObject,
        allowAnonymous: Boolean = true,
    ): JsonObject? {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val sha = bundleSession.resolveQueryHash(operationName, cleanSpdc) ?: return null
        val session = getOrCreateSession(
            spdc = cleanSpdc,
            forceRefresh = false,
            allowAnonymousFallback = allowAnonymous,
        ) ?: return null

        var response = runCatching {
            spotifyClient.pathfinderQuery(
                operationName = operationName,
                sha256Hash = sha,
                variables = variables,
                authToken = session.accessToken,
                clientToken = session.clientToken,
                clientVersion = session.clientVersion,
            )
        }.getOrNull() ?: return null

        if (response.status.value == 401) {
            val fresh = getOrCreateSession(
                spdc = cleanSpdc,
                forceRefresh = true,
                allowAnonymousFallback = allowAnonymous,
            ) ?: return null
            response = runCatching {
                spotifyClient.pathfinderQuery(
                    operationName = operationName,
                    sha256Hash = sha,
                    variables = variables,
                    authToken = fresh.accessToken,
                    clientToken = fresh.clientToken,
                    clientVersion = fresh.clientVersion,
                )
            }.getOrNull() ?: return null
        }

        if (response.status.value !in 200..299) return null
        val root = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull()
        return root?.get("data") as? JsonObject
    }

    // ========== User Playlists (SpotAPI libraryV3 + REST /v1/me/playlists) ==========

    suspend fun fetchPlaylistsWithSpdc(spdc: String): Result<List<SpotifyPlaylistItem>> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val collected = LinkedHashMap<String, SpotifyPlaylistItem>()

        // 1. Primary Web API: paginated /v1/me/playlists
        val session = getOrCreateSession(cleanSpdc, allowAnonymousFallback = false)
            ?: throw Exception("Failed to obtain Spotify session")

        var offset = 0
        var activeToken = session.accessToken
        while (offset < 200) {
            var response = runCatching {
                spotifyClient.getSpotifyUserPlaylists(activeToken, limit = 50, offset = offset)
            }.getOrNull()
            if (response?.status?.value == 401) {
                activeToken = getOrCreateSession(cleanSpdc, forceRefresh = true)?.accessToken ?: activeToken
                response = runCatching {
                    spotifyClient.getSpotifyUserPlaylists(activeToken, limit = 50, offset = offset)
                }.getOrNull()
            }
            val parsed = runCatching { response?.body<SpotifyUserPlaylistsResponse>() }.getOrNull() ?: break
            val items = parsed.items.filterNotNull()
            items.forEach { pl ->
                if (pl.id.isNotBlank() && pl.name.isNotBlank()) {
                    collected.putIfAbsent(pl.id, pl)
                }
            }
            if (items.size < 50 || collected.size >= parsed.total) break
            offset += 50
        }

        // 2. SpotAPI Pathfinder libraryV3 (captures followed playlists & folders even when /v1/me/playlists is restricted)
        var libOffset = 0
        while (libOffset < 200) {
            val libData = executePathfinder(
                spdc = cleanSpdc,
                operationName = "libraryV3",
                variables = buildJsonObject {
                    put("limit", 100)
                    put("offset", libOffset)
                    putJsonArray("filters") { add(JsonPrimitive("Playlists")) }
                    put("order", null as String?)
                    put("textFilter", "")
                    putJsonArray("features") {}
                    put("folderUri", null as String?)
                    put("flatten", true)
                    putJsonArray("expandedFolders") {}
                    put("includeFoldersWhenFlattening", false)
                },
                allowAnonymous = false,
            ) ?: break

            val libraryV3 = libData.obj("me")?.obj("libraryV3") ?: break
            val libItems = libraryV3.arr("items") ?: break
            if (libItems.isEmpty()) break

            for (el in libItems) {
                val wrapper = (el as? JsonObject)?.obj("item") ?: continue
                val data = wrapper.obj("data") ?: continue
                val uri = wrapper.str("_uri") ?: data.str("uri") ?: continue
                if (!uri.startsWith("spotify:playlist:")) continue
                val plId = uri.removePrefix("spotify:playlist:")
                val name = data.str("name")?.takeIf { it.isNotBlank() } ?: continue
                val parsedItem = parsePathfinderPlaylistSummary(plId, name, data)
                val existing = collected[plId]
                if (existing == null) {
                    collected[plId] = parsedItem
                } else if (existing.images.isEmpty() && parsedItem.images.isNotEmpty()) {
                    collected[plId] = existing.copy(images = parsedItem.images)
                }
            }

            val totalCount = libraryV3.int("totalCount") ?: 0
            if (libItems.size < 100 || (totalCount > 0 && libOffset + libItems.size >= totalCount)) break
            libOffset += 100
        }

        collected.values.toList()
    }

    // ========== User Liked Songs (SpotAPI fetchLibraryTracks + REST /v1/me/tracks) ==========

    suspend fun fetchLikedTracksWithSpdc(spdc: String, limit: Int = 100): Result<List<SpotifyTrackItem>> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val collected = LinkedHashMap<String, SpotifyTrackItem>()

        // 1. SpotAPI Pathfinder fetchLibraryTracks
        var offset = 0
        while (collected.size < limit) {
            val pageData = executePathfinder(
                spdc = cleanSpdc,
                operationName = "fetchLibraryTracks",
                variables = buildJsonObject {
                    put("offset", offset)
                    put("limit", minOf(100, limit - collected.size))
                },
                allowAnonymous = false,
            ) ?: break

            val tracksPage = pageData.obj("me")?.obj("library")?.obj("tracks") ?: break
            val items = tracksPage.arr("items") ?: break
            if (items.isEmpty()) break

            for (el in items) {
                val trackWrapper = (el as? JsonObject)?.obj("track") ?: continue
                val data = trackWrapper.obj("data") ?: continue
                val uri = trackWrapper.str("_uri") ?: trackWrapper.str("uri") ?: data.str("uri")
                val parsed = parsePathfinderTrack(data, uri) ?: continue
                collected.putIfAbsent(parsed.id.ifBlank { parsed.name }, parsed)
            }
            val total = tracksPage.int("totalCount") ?: 0
            if (items.size < 50 || (total > 0 && offset + items.size >= total)) break
            offset += items.size
        }

        // 2. Fallback: REST /v1/me/tracks
        if (collected.isEmpty()) {
            val token = getValidAccessToken(cleanSpdc)
            if (!token.isNullOrBlank()) {
                var restOffset = 0
                while (collected.size < limit) {
                    val page = runCatching {
                        spotifyClient.getSpotifySavedTracks(token, limit = minOf(50, limit - collected.size), offset = restOffset)
                            .body<SpotifySavedTracksResponse>()
                    }.getOrNull() ?: break
                    val pageTracks = page.items.mapNotNull { it?.track }.filter { it.name.isNotBlank() }
                    if (pageTracks.isEmpty()) break
                    pageTracks.forEach { t -> collected.putIfAbsent(t.id.ifBlank { t.name }, t) }
                    if (page.items.size < 50) break
                    restOffset += 50
                }
            }
        }

        collected.values.take(limit)
    }

    // ========== User Listening History (SpotAPI userTopContent/recents + REST fallbacks) ==========

    suspend fun fetchRecentlyPlayedWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyRecentlyPlayedItem>> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val token = getValidAccessToken(cleanSpdc)

        if (!token.isNullOrBlank()) {
            val recentItems = runCatching {
                spotifyClient.getSpotifyRecentlyPlayed(token, limit).body<SpotifyRecentlyPlayedResponse>().items.filterNotNull()
            }.getOrDefault(emptyList())
            if (recentItems.isNotEmpty()) return@runCatching recentItems

            val topTracks = runCatching {
                spotifyClient.getSpotifyTopTracks(token, limit).body<SpotifyTopTracksResponse>().items.filterNotNull()
            }.getOrDefault(emptyList())
            if (topTracks.isNotEmpty()) {
                return@runCatching topTracks.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
            }
        }

        // SpotAPI Pathfinder userTopContent (SHORT_TERM top tracks)
        val topData = executePathfinder(
            spdc = cleanSpdc,
            operationName = "userTopContent",
            variables = buildJsonObject {
                put("includeTopArtists", false)
                put("includeTopTracks", true)
                putJsonObject("topArtistsInput") {
                    put("offset", 0)
                    put("limit", 10)
                    put("sortBy", "AFFINITY")
                    put("timeRange", "SHORT_TERM")
                }
                putJsonObject("topTracksInput") {
                    put("offset", 0)
                    put("limit", limit)
                    put("sortBy", "AFFINITY")
                    put("timeRange", "SHORT_TERM")
                }
            },
            allowAnonymous = false,
        )
        val pathfinderTop = topData?.obj("me")?.obj("profile")?.obj("topTracks")?.arr("items")
            ?.mapNotNull { el ->
                val data = (el as? JsonObject)?.obj("data") ?: return@mapNotNull null
                parsePathfinderTrack(data, data.str("uri"))
            }.orEmpty()
        if (pathfinderTop.isNotEmpty()) {
            return@runCatching pathfinderTop.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
        }

        // SpotAPI Pathfinder recents + library saved tracks fallback
        val liked = fetchLikedTracksWithSpdc(cleanSpdc, limit = limit).getOrDefault(emptyList())
        if (liked.isNotEmpty()) {
            return@runCatching liked.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
        }

        val recentsData = executePathfinder(
            spdc = cleanSpdc,
            operationName = "recents",
            variables = buildJsonObject {
                putJsonArray("uris") { add(JsonPrimitive("spotify:list:recents:page")) }
                put("limit", limit)
                put("offset", 0)
            },
            allowAnonymous = false,
        )
        val recentsTracks = recentsData?.arr("lists")?.firstOrNull()?.let { it as? JsonObject }
            ?.obj("items")?.arr("items")
            ?.mapNotNull { el ->
                val entity = (el as? JsonObject)?.obj("entity")?.obj("data") ?: return@mapNotNull null
                val uri = entity.str("uri") ?: return@mapNotNull null
                if (!uri.startsWith("spotify:track:")) return@mapNotNull null
                val identity = entity.obj("identityTrait")
                val name = identity?.str("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artists = identity.obj("contributors")?.arr("items")?.mapNotNull { c ->
                    (c as? JsonObject)?.str("name")?.let { SpotifyArtistItem(name = it) }
                }.orEmpty()
                val coverUrl = parseCoverSources(
                    entity.obj("visualIdentityTrait")?.obj("squareCoverImage")
                        ?.obj("image")?.obj("data")?.arr("sources"),
                ).lastOrNull()?.url
                SpotifyTrackItem(
                    id = uri.removePrefix("spotify:track:"),
                    name = name,
                    artists = artists,
                    album = coverUrl?.let { SpotifyAlbumItem(images = listOf(SpotifyImage(url = it, width = 640, height = 640))) },
                )
            }.orEmpty()
        recentsTracks.map { SpotifyRecentlyPlayedItem(track = it, playedAt = null) }
    }

    // ========== User Profile ==========

    suspend fun fetchUserProfileWithSpdc(spdc: String): Result<SpotifyUserProfileResponse> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val token = getValidAccessToken(cleanSpdc)
        val restProfile = token?.let { runCatching { getUserProfile(it).getOrNull() }.getOrNull() }
        if (restProfile != null && (!restProfile.displayName.isNullOrBlank() || restProfile.id.isNotBlank())) {
            return@runCatching restProfile
        }

        // SpotAPI Pathfinder profileAttributes fallback
        val profileData = executePathfinder(
            spdc = cleanSpdc,
            operationName = "profileAttributes",
            variables = buildJsonObject {},
            allowAnonymous = false,
        )?.obj("me")?.obj("profile")
            ?: throw Exception("Failed to fetch Spotify profile")

        val avatars = profileData.obj("avatar")?.arr("sources")?.mapNotNull { el ->
            val src = el as? JsonObject ?: return@mapNotNull null
            val u = src.str("url") ?: return@mapNotNull null
            SpotifyImage(url = u, width = src.int("width"), height = src.int("height"))
        }.orEmpty()

        SpotifyUserProfileResponse(
            id = profileData.str("username").orEmpty(),
            displayName = profileData.str("name") ?: profileData.str("username"),
            images = avatars,
        )
    }

    // ========== Featured / Trending Playlists (SpotAPI Pathfinder + Charts + REST fallback) ==========

    suspend fun fetchFeaturedPlaylistsWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyPlaylistItem>> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val collected = LinkedHashMap<String, SpotifyPlaylistItem>()

        // 1. SpotAPI Pathfinder searchDesktop for viral & trending charts playlists
        for (query in listOf("Top 50 Viral Hits", "Trending Hits Charts", "Hot Hits")) {
            if (collected.size >= limit) break
            val data = executePathfinder(
                spdc = cleanSpdc,
                operationName = "searchDesktop",
                variables = searchDesktopVariables(query = query, limit = limit),
                allowAnonymous = true,
            ) ?: continue
            val searchRoot = data.obj("searchV2") ?: data.obj("search") ?: continue
            val playlistsSection = searchRoot.obj("playlists") ?: continue
            val items = playlistsSection.arr("items") ?: continue
            for (el in items) {
                val pData = (el as? JsonObject)?.obj("data") ?: continue
                val uri = pData.str("uri") ?: continue
                if (!uri.startsWith("spotify:playlist:")) continue
                val plId = uri.removePrefix("spotify:playlist:")
                val name = pData.str("name")?.takeIf { it.isNotBlank() } ?: continue
                collected.putIfAbsent(plId, parsePathfinderPlaylistSummary(plId, name, pData))
            }
        }

        // 2. Editorial Chart Playlists (SpotAPI Charts + India charts via Pathfinder fetchPlaylist / fetchPlaylistMetadata)
        val chartIds = listOf(
            "37i9dQZEVXbLZ52XmnySJg", // Top 50 - India
            "37i9dQZEVXbMDoHDwVN2tF", // Top 50 - Global
            "37i9dQZEVXbNG2KDcFcKOF", // Top Songs - Global
            "37i9dQZEVXbLiRSasKsNU9", // Viral 50 - Global
            "37i9dQZF1DXcBWIGoYBM5M", // Today's Top Hits
            "37i9dQZF1DX0XUfTFmNBRM", // Hot Hits Hindi
        )
        if (collected.size < limit) {
            for (chartId in chartIds) {
                if (collected.containsKey(chartId)) continue
                val detail = fetchPathfinderPlaylist(cleanSpdc, chartId, trackLimit = 5)
                if (detail != null && detail.name.isNotBlank()) {
                    collected.putIfAbsent(
                        chartId,
                        SpotifyPlaylistItem(
                            id = chartId,
                            name = detail.name,
                            description = detail.description,
                            images = detail.images,
                            tracks = SpotifyTracksRef(total = detail.tracks.total),
                            owner = detail.owner,
                        ),
                    )
                }
            }
        }

        // 3. REST fallbacks if token permits
        val token = getValidAccessToken(cleanSpdc)
        if (collected.isEmpty() && !token.isNullOrBlank()) {
            for (query in listOf("Top 50 Viral Hits", "Trending Songs Charts")) {
                if (collected.size >= limit) break
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

        if (collected.isEmpty() && !token.isNullOrBlank()) {
            runCatching {
                val legacy = spotifyClient.getSpotifyFeaturedPlaylists(token, limit).body<SpotifyFeaturedPlaylistsResponse>()
                legacy.playlists.items.filterNotNull().forEach { item ->
                    if (item.id.isNotBlank()) collected.putIfAbsent(item.id, item)
                }
            }
        }

        collected.values.take(limit)
    }

    // ========== Trending Songs (SpotAPI Pathfinder Charts & Search + REST fallback) ==========

    suspend fun fetchTrendingTracksWithSpdc(spdc: String, limit: Int = 20): Result<List<SpotifyTrackItem>> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val tracks = LinkedHashMap<String, SpotifyTrackItem>()

        // 1. Editorial charts via SpotAPI Pathfinder fetchPlaylistContents / fetchPlaylist
        for (chartId in listOf(
            "37i9dQZEVXbLZ52XmnySJg", // Top 50 - India
            "37i9dQZEVXbMDoHDwVN2tF", // Top 50 - Global
            "37i9dQZEVXbNG2KDcFcKOF", // Top Songs - Global
            "37i9dQZF1DXcBWIGoYBM5M", // Today's Top Hits
        )) {
            val detail = fetchPathfinderPlaylist(cleanSpdc, chartId, trackLimit = limit)
            val list = detail?.tracks?.items?.mapNotNull { it?.track }?.filter { it.name.isNotBlank() }.orEmpty()
            if (list.isNotEmpty()) {
                list.forEach { t -> tracks.putIfAbsent(t.id.ifBlank { t.name }, t) }
                if (tracks.size >= limit) break
            }
        }

        // 2. SpotAPI Pathfinder searchDesktop for trending hits
        if (tracks.isEmpty()) {
            val searchData = executePathfinder(
                spdc = cleanSpdc,
                operationName = "searchDesktop",
                variables = searchDesktopVariables(query = "Top 50 Trending Hits", limit = limit),
                allowAnonymous = true,
            )
            val trackItems = (searchData?.obj("searchV2") ?: searchData?.obj("search"))
                ?.obj("tracksV2")?.arr("items")
            trackItems?.forEach { el ->
                val tData = (el as? JsonObject)?.obj("item")?.obj("data") ?: return@forEach
                val parsed = parsePathfinderTrack(tData, tData.str("uri")) ?: return@forEach
                tracks.putIfAbsent(parsed.id.ifBlank { parsed.name }, parsed)
            }
        }

        // 3. REST /v1 fallback
        val token = getValidAccessToken(cleanSpdc)
        if (tracks.isEmpty() && !token.isNullOrBlank()) {
            for (chartId in listOf("37i9dQZEVXbLZ52XmnySJg", "37i9dQZEVXbMDoHDwVN2tF", "37i9dQZF1DXcBWIGoYBM5M")) {
                val res = runCatching {
                    spotifyClient.getSpotifyPlaylistById(token, chartId).body<SpotifyPlaylistDetailResponse>()
                }.getOrNull()
                val list = res?.tracks?.items?.mapNotNull { it?.track }?.filter { it.name.isNotBlank() }.orEmpty()
                if (list.isNotEmpty()) {
                    list.forEach { t -> tracks.putIfAbsent(t.id.ifBlank { t.name }, t) }
                    break
                }
            }
            if (tracks.isEmpty()) {
                val searched = runCatching {
                    spotifyClient.searchSpotifyTracksApi(token, "Trending Hits Top 50", limit).body<SpotifyTracksSearchResponse>()
                }.getOrNull()
                searched?.tracks?.items?.filterNotNull()?.filter { it.name.isNotBlank() }?.forEach { t ->
                    tracks.putIfAbsent(t.id.ifBlank { t.name }, t)
                }
            }
        }

        tracks.values.take(limit)
    }

    // ========== Full Playlist Detail (SpotAPI Pathfinder + REST /v1 fallback) ==========

    suspend fun fetchPlaylistByIdWithSpdc(
        spdc: String,
        playlistId: String,
        trackLimit: Int = 100,
    ): Result<SpotifyPlaylistDetailResponse> = runCatching {
        val cleanSpdc = spdc.removePrefix("sp_dc=").trim()
        val cleanId = playlistId
            .removePrefix("spotify:playlist:")
            .removePrefix("spotify:")
            .substringAfter("playlist/")
            .substringBefore("?")
            .substringBefore("/")
            .trim()

        // 1. Prefer SpotAPI Pathfinder fetchPlaylist / fetchPlaylistContents (works on private playlists with sp_dc AND editorial charts 37i9dQ...)
        val pfDetail = fetchPathfinderPlaylist(cleanSpdc, cleanId, trackLimit = trackLimit)
        if (pfDetail != null && pfDetail.tracks.items.isNotEmpty()) {
            return@runCatching pfDetail
        }

        // 2. Fallback to REST /v1/playlists/{id}
        val token = getOrCreateSession(cleanSpdc, allowAnonymousFallback = true)?.accessToken
            ?: return@runCatching (pfDetail ?: throw Exception("Failed to obtain Spotify access token"))
        val restDetail = spotifyClient.getSpotifyPlaylistById(token, cleanId).body<SpotifyPlaylistDetailResponse>()
        if (restDetail.tracks.items.isNotEmpty() || pfDetail == null) {
            restDetail
        } else {
            pfDetail
        }
    }

    private suspend fun fetchPathfinderPlaylist(
        cleanSpdc: String,
        playlistId: String,
        trackLimit: Int = 100,
    ): SpotifyPlaylistDetailResponse? {
        val playlistUri = "spotify:playlist:$playlistId"
        val allowAnon = cleanSpdc.isBlank()

        // Try fetchPlaylist first, else fetchPlaylistMetadata + fetchPlaylistContents
        val fullData = executePathfinder(
            spdc = cleanSpdc,
            operationName = "fetchPlaylist",
            variables = buildJsonObject {
                put("uri", playlistUri)
                put("offset", 0)
                put("limit", minOf(100, trackLimit))
                put("enableWatchFeedEntrypoint", false)
            },
            allowAnonymous = allowAnon || true,
        )
        val playlistV2 = fullData?.obj("playlistV2")

        val metaNode = playlistV2 ?: executePathfinder(
            spdc = cleanSpdc,
            operationName = "fetchPlaylistMetadata",
            variables = buildJsonObject {
                put("uri", playlistUri)
                put("enableWatchFeedEntrypoint", false)
            },
            allowAnonymous = true,
        )?.obj("playlistV2") ?: return null

        val name = metaNode.str("name") ?: ""
        val description = metaNode.str("description")
        val images = parsePathfinderImages(metaNode.obj("images"))
        val ownerData = metaNode.obj("ownerV2")?.obj("data")
        val owner = SpotifyOwner(
            displayName = ownerData?.str("name") ?: ownerData?.str("username"),
            id = ownerData?.str("username") ?: ownerData?.str("uri")?.substringAfterLast(":"),
        )

        val entries = mutableListOf<SpotifyPlaylistTrackEntry>()
        var totalCount = metaNode.obj("content")?.int("totalCount") ?: 0

        val initialItems = metaNode.obj("content")?.arr("items")
        if (!initialItems.isNullOrEmpty()) {
            entries.addAll(parsePathfinderPlaylistEntries(initialItems))
        }

        // Paginate further tracks using fetchPlaylistContents if initial page lacked items or stopped before trackLimit
        var offset = entries.size
        while (offset < trackLimit && (totalCount == 0 || offset < minOf(totalCount, trackLimit))) {
            if (initialItems != null && initialItems.isEmpty()) break
            val contentData = executePathfinder(
                spdc = cleanSpdc,
                operationName = "fetchPlaylistContents",
                variables = buildJsonObject {
                    put("uri", playlistUri)
                    put("offset", offset)
                    put("limit", minOf(100, trackLimit - offset))
                    put("enableWatchFeedEntrypoint", false)
                    put("includeEpisodeContentRatingsV2", true)
                },
                allowAnonymous = true,
            )?.obj("playlistV2")?.obj("content") ?: break

            totalCount = contentData.int("totalCount") ?: totalCount
            val pageItems = contentData.arr("items") ?: break
            if (pageItems.isEmpty()) break
            entries.addAll(parsePathfinderPlaylistEntries(pageItems))
            offset += pageItems.size
            if (pageItems.size < 50) break
        }

        return SpotifyPlaylistDetailResponse(
            id = playlistId,
            name = name,
            description = description,
            images = images,
            owner = owner,
            tracks = SpotifyPlaylistTracksPage(
                items = entries,
                total = maxOf(totalCount, entries.size),
            ),
        )
    }

    // ========== Pathfinder JSON Parsers ==========

    private fun searchDesktopVariables(query: String, limit: Int): JsonObject = buildJsonObject {
        put("searchTerm", query)
        put("offset", 0)
        put("limit", limit)
        put("numberOfTopResults", minOf(limit, 5))
        put("includeAudiobooks", false)
        put("includeArtistHasConcertsField", false)
        put("includePreReleases", false)
        put("includeLocalConcertsField", false)
        put("includeAuthors", false)
        put("includeEpisodeContentRatingsV2", false)
        put("isPrefix", null as String?)
        putJsonArray("sectionFilters") { add(JsonPrimitive("GENERIC")) }
    }

    internal fun parsePathfinderPlaylistSummary(id: String, name: String, data: JsonObject): SpotifyPlaylistItem {
        val images = parsePathfinderImages(data.obj("images"))
        val ownerData = data.obj("ownerV2")?.obj("data")
        val owner = SpotifyOwner(
            displayName = ownerData?.str("name") ?: ownerData?.str("username") ?: "Spotify",
            id = ownerData?.str("username") ?: ownerData?.str("uri")?.substringAfterLast(":"),
        )
        val total = data.obj("content")?.int("totalCount") ?: 0
        return SpotifyPlaylistItem(
            id = id,
            name = name,
            description = data.str("description"),
            images = images,
            tracks = SpotifyTracksRef(total = total),
            owner = owner,
        )
    }

    internal fun parsePathfinderPlaylistEntries(items: JsonArray): List<SpotifyPlaylistTrackEntry> =
        items.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val wrapper = obj.obj("itemV2") ?: obj.obj("item")
            val data = wrapper?.obj("data") ?: return@mapNotNull null
            val uri = data.str("uri") ?: wrapper.str("_uri") ?: return@mapNotNull null
            if (!uri.startsWith("spotify:track:")) return@mapNotNull null
            val track = parsePathfinderTrack(data, uri) ?: return@mapNotNull null
            SpotifyPlaylistTrackEntry(track = track)
        }

    internal fun parsePathfinderTrack(data: JsonObject, fallbackUri: String?): SpotifyTrackItem? {
        val name = data.str("name")?.takeIf { it.isNotBlank() } ?: return null
        val uri = data.str("uri") ?: fallbackUri ?: ""
        val id = data.str("id")?.takeIf { it.isNotBlank() } ?: uri.substringAfterLast(":").takeIf { it.isNotBlank() } ?: ""

        val artists = (data.obj("artists")?.arr("items") ?: data.obj("firstArtist")?.arr("items"))
            ?.mapNotNull { aEl ->
                val aObj = aEl as? JsonObject ?: return@mapNotNull null
                val aName = aObj.obj("profile")?.str("name") ?: aObj.str("name") ?: return@mapNotNull null
                val aId = aObj.str("uri")?.substringAfterLast(":").orEmpty()
                SpotifyArtistItem(id = aId, name = aName)
            }.orEmpty()

        val albumObj = data.obj("albumOfTrack")
        val albumImages = parseCoverSources(albumObj?.obj("coverArt")?.arr("sources"))
        val album = albumObj?.let {
            SpotifyAlbumItem(
                id = it.str("id") ?: it.str("uri")?.substringAfterLast(":").orEmpty(),
                name = it.str("name").orEmpty(),
                images = albumImages,
            )
        }

        val durationMs = (data.obj("trackDuration") ?: data.obj("duration"))?.long("totalMilliseconds") ?: 0L

        return SpotifyTrackItem(
            id = id,
            name = name,
            durationMs = durationMs,
            artists = artists,
            album = album,
        )
    }

    private fun parsePathfinderImages(imagesObj: JsonObject?): List<SpotifyImage> {
        val items = imagesObj?.arr("items") ?: return emptyList()
        val result = mutableListOf<SpotifyImage>()
        for (itemEl in items) {
            val sources = (itemEl as? JsonObject)?.arr("sources") ?: continue
            result.addAll(parseCoverSources(sources))
        }
        return result
    }

    private fun upgradeSpotifyCdnUrl(url: String): String =
        url
            .replace("ab67616d00004851", "ab67616d0000b273")
            .replace("ab67616d00001e02", "ab67616d0000b273")

    private fun parseCoverSources(sources: JsonArray?): List<SpotifyImage> {
        if (sources == null) return emptyList()
        val parsed = sources.mapNotNull { s ->
            val obj = s as? JsonObject ?: return@mapNotNull null
            val rawUrl = obj.str("url")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = upgradeSpotifyCdnUrl(rawUrl)
            val wasUpgraded = url != rawUrl
            SpotifyImage(
                url = url,
                width = if (wasUpgraded) 640 else obj.int("width"),
                height = if (wasUpgraded) 640 else obj.int("height"),
            )
        }
        val best = parsed.maxByOrNull { (it.width ?: 0).toLong() * (it.height ?: 0).toLong() }
        return listOfNotNull(best)
    }

    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun JsonObject.arr(key: String): JsonArray? = get(key) as? JsonArray
    private fun JsonObject.str(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (get(key) as? JsonPrimitive)?.longOrNull
}
