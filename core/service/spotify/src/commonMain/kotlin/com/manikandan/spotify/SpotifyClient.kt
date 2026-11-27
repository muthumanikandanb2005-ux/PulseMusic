package com.manikandan.spotify

import com.manikandan.ktorext.encoding.brotli
import com.manikandan.ktorext.getEngine
import com.manikandan.spotify.auth.SpotApiVault
import com.manikandan.spotify.model.body.CanvasBody
import com.manikandan.spotify.model.body.SpotifyClientBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random

class SpotifyClient {
    private var spotifyClient = createSpotifyClient()
    private var jsonClient =
        createSpotifyClient(
            onlyJson = true,
        )

    var proxy: ProxyConfig? = null
        set(value) {
            if (field == value) return
            field = value
            spotifyClient.close()
            spotifyClient = createSpotifyClient()
            jsonClient.close()
            jsonClient = createSpotifyClient(onlyJson = true)
        }

    // Security: Do NOT attach HTTP header/body loggers or cURL loggers so Spotify cookies (sp_dc),
    // Bearer tokens, Client-Tokens, TOTP params, and persisted query hashes remain hidden from logs.
    @OptIn(ExperimentalSerializationApi::class)
    private fun createSpotifyClient(onlyJson: Boolean = false) =
        HttpClient(getEngine()) {
            followRedirects = true
            expectSuccess = false
            install(HttpCache)
            install(HttpSend) {
                maxSendCount = 100
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                if (!onlyJson) {
                    register(
                        ContentType.Text.Plain,
                        KotlinxSerializationConverter(
                            Json {
                                prettyPrint = true
                                isLenient = true
                                ignoreUnknownKeys = true
                                explicitNulls = false
                                encodeDefaults = true
                            },
                        ),
                    )
                    protobuf(
                        ProtoBuf {
                            encodeDefaults = true
                        },
                    )
                }
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
            install(ContentEncoding) {
                brotli(1.0F)
                gzip(0.9F)
                deflate(0.8F)
            }
            defaultRequest {
                url("https://api.spotify.com")
            }
            if (proxy != null) {
                engine {
                    proxy = this@SpotifyClient.proxy
                }
            }
        }

    suspend fun getPublicText(url: String): String =
        spotifyClient.get(url) {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Accept, "*/*")
        }.bodyAsText()

    suspend fun getSpotifyLastestTotpSecret() =
        spotifyClient.get("https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretDict.json") {
            userAgent(USER_AGENT)
            contentType(ContentType.Application.Json)
        }

    suspend fun getSpotifyServerTime(spdc: String) =
        spotifyClient.get("https://open.spotify.com/api/server-time") {
            userAgent(USER_AGENT)
            if (spdc.isNotBlank()) {
                header("Cookie", "sp_dc=$spdc")
            }
            header("App-platform", "WebPlayer")
            header("Spotify-App-Version", SpotApiVault.defaultAppVersion())
            header("Accept", "application/json")
            header("Origin", "https://open.spotify.com")
            header("Referer", "https://open.spotify.com/")
        }

    suspend fun getSpotifyAccessToken(
        spdc: String,
        otpValue: String,
        reason: String = "init",
        sTime: String,
        cTime: String,
        totpVersion: Int,
        productType: String = "web-player",
    ) = spotifyClient.get("https://open.spotify.com/api/token") {
        userAgent(USER_AGENT)
        contentType(ContentType.Application.Json)
        if (spdc.isNotBlank()) {
            header("Cookie", "sp_dc=$spdc")
        }
        parameter("reason", reason)
        parameter("productType", productType)
        parameter("totp", otpValue)
        parameter("totpServer", otpValue)
        parameter("totpVer", totpVersion)
        header("Accept", "application/json")
        header("Origin", "https://open.spotify.com")
        header("Referer", "https://open.spotify.com/")
    }

    suspend fun getSpotifyLyricsToken(spdc: String) =
        spotifyClient.get("https://open.spotify.com/get_access_token?reason=transport&productType=web_player") {
            userAgent(USER_AGENT)
            contentType(ContentType.Application.Json)
            if (spdc.isNotBlank()) {
                header("Cookie", "sp_dc=$spdc")
            }
        }

    suspend fun getSpotifyLyrics(
        token: String,
        clientToken: String,
        trackId: String,
    ) = spotifyClient.get("https://spclient.wg.spotify.com/color-lyrics/v2/track/$trackId?format=json&vocalRemoval=false&market=from_token") {
        userAgent(USER_AGENT)
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $token")
        header("Client-Token", clientToken)
        header("App-platform", "WebPlayer")
    }

    suspend fun searchSpotifyTrack(
        q: String,
        authToken: String,
        clientToken: String,
        shaHash: String? = null,
    ) = spotifyClient.get("https://api-partner.spotify.com/pathfinder/v1/query?operationName=searchTracks") {
        userAgent(USER_AGENT)
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $authToken")
        header("Client-Token", clientToken)
        header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
        val variable =
            buildJsonObject {
                put("searchTerm", q)
                put("offset", 0)
                put("limit", 10)
                put("numberOfTopResults", 5)
                put("includeAudiobooks", false)
                put("includePreReleases", false)
            }.toString()
        val sha = shaHash ?: SpotApiVault.fallbackQueryHash("searchTracks").orEmpty()
        parameter("variables", variable)
        parameter(
            "extensions",
            "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"$sha\"}}",
        )
    }

    suspend fun getSpotifyTrackById(token: String, trackId: String) =
        spotifyClient.get("https://api.spotify.com/v1/tracks/$trackId") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

    /**
     * Execute a SpotAPI Pathfinder GraphQL v2 query with full WebPlayer headers.
     */
    suspend fun pathfinderQuery(
        operationName: String,
        sha256Hash: String,
        variables: JsonObject,
        authToken: String,
        clientToken: String,
        clientVersion: String = SpotApiVault.defaultAppVersion(),
    ) = jsonClient.post("https://api-partner.spotify.com/pathfinder/v2/query") {
        userAgent(MODERN_CHROME_UA)
        header(HttpHeaders.Accept, "application/json")
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $authToken")
        header("Client-Token", clientToken)
        header("Spotify-App-Version", clientVersion)
        header("App-platform", "WebPlayer")
        header("Origin", "https://open.spotify.com")
        header("Referer", "https://open.spotify.com/")
        header("Accept-Language", "en")
        setBody(
            buildJsonObject {
                put("variables", variables)
                put("operationName", operationName)
                putJsonObject("extensions") {
                    putJsonObject("persistedQuery") {
                        put("version", JsonPrimitive(1))
                        put("sha256Hash", JsonPrimitive(sha256Hash))
                    }
                }
            },
        )
    }

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
        clientToken: String,
    ) = spotifyClient.post("https://spclient.wg.spotify.com/canvaz-cache/v0/canvases") {
        headers {
            append(HttpHeaders.Accept, "application/protobuf")
            append(HttpHeaders.ContentType, "application/protobuf")
            append(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            append(HttpHeaders.Authorization, "Bearer $token")
            append("Client-Token", clientToken)
            append(HttpHeaders.UserAgent, "Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)")
        }
        setBody(
            CanvasBody(
                tracks =
                    listOf(
                        CanvasBody.Track(
                            track_uri = "spotify:track:$trackId",
                        ),
                    ),
            ),
        )
    }

    suspend fun getSpotifyOpenHomeHtml(spdc: String) =
        spotifyClient.get("https://open.spotify.com/") {
            userAgent(MODERN_CHROME_UA)
            if (spdc.isNotBlank()) {
                header("Cookie", "sp_dc=$spdc")
            }
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "en-US,en;q=0.9")
        }

    suspend fun getSpotifyUserProfile(token: String) =
        spotifyClient.get("https://api.spotify.com/v1/me") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

    suspend fun getSpotifyUserPlaylists(token: String, limit: Int = 50, offset: Int = 0) =
        spotifyClient.get("https://api.spotify.com/v1/me/playlists") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("limit", limit)
            parameter("offset", offset)
        }

    suspend fun getSpotifyRecentlyPlayed(token: String, limit: Int = 25) =
        spotifyClient.get("https://api.spotify.com/v1/me/player/recently-played") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("limit", limit)
        }

    suspend fun getSpotifyTopTracks(token: String, limit: Int = 20) =
        spotifyClient.get("https://api.spotify.com/v1/me/top/tracks") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("time_range", "short_term")
            parameter("limit", limit)
        }

    suspend fun getSpotifySavedTracks(token: String, limit: Int = 50, offset: Int = 0) =
        spotifyClient.get("https://api.spotify.com/v1/me/tracks") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("limit", limit)
            parameter("offset", offset)
        }

    suspend fun searchSpotifyPlaylists(token: String, query: String, limit: Int = 20) =
        spotifyClient.get("https://api.spotify.com/v1/search") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("q", query)
            parameter("type", "playlist")
            parameter("limit", limit)
        }

    suspend fun searchSpotifyTracksApi(token: String, query: String, limit: Int = 20) =
        spotifyClient.get("https://api.spotify.com/v1/search") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("q", query)
            parameter("type", "track")
            parameter("limit", limit)
        }

    suspend fun getSpotifyPlaylistById(token: String, playlistId: String) =
        spotifyClient.get("https://api.spotify.com/v1/playlists/$playlistId") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

    suspend fun getSpotifyFeaturedPlaylists(token: String, limit: Int = 20) =
        spotifyClient.get("https://api.spotify.com/v1/browse/featured-playlists") {
            userAgent(MODERN_CHROME_UA)
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("limit", limit)
        }

    suspend fun getSpotifyClientToken(
        clientId: String? = null,
        clientVersion: String? = null,
    ) = jsonClient.post("https://clienttoken.spotify.com/v1/clienttoken") {
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.ContentType, "application/json")
            append(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            append(HttpHeaders.UserAgent, MODERN_CHROME_UA)
        }
        setBody(
            SpotifyClientBody(
                client_data = SpotifyClientBody.ClientData(
                    client_version = clientVersion?.takeIf { it.isNotBlank() } ?: SpotApiVault.defaultAppVersion(),
                    client_id = clientId?.takeIf { it.isNotBlank() } ?: SpotApiVault.defaultClientId(),
                ),
            ),
        )
    }

    fun getRandomUserAgent(): String {
        val macOSVersion = "${Random.nextInt(11, 15)}_${Random.nextInt(4, 9)}"
        val webKitVersion = "${Random.nextInt(530, 537)}.${Random.nextInt(30, 37)}"
        val chromeVersion = "${Random.nextInt(80, 105)}.0.${Random.nextInt(3000, 4500)}.${Random.nextInt(60, 125)}"
        val safariVersion = "${Random.nextInt(530, 537)}.${Random.nextInt(30, 36)}"

        return "Mozilla/5.0 (Macintosh; Intel Mac OS X $macOSVersion) AppleWebKit/$webKitVersion (KHTML, like Gecko) Chrome/$chromeVersion Safari/$safariVersion"
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
        const val MODERN_CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
    }
}
