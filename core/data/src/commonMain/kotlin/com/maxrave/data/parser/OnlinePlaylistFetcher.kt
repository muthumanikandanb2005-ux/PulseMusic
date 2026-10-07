package com.maxrave.data.parser

import com.maxrave.ktorext.getEngine
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "OnlinePlaylistFetcher"
private const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

object OnlinePlaylistFetcher {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        HttpClient(getEngine()) {
            followRedirects = true
            expectSuccess = false
        }
    }

    suspend fun fetch(url: String, youTube: YouTube, defaultTitle: String? = null): ParsedPlaylist? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        Logger.i(TAG, "Fetching online playlist from URL: $trimmed")

        return try {
            when {
                isYouTubeUrl(trimmed) -> fetchYouTube(trimmed, youTube, defaultTitle)
                isSpotifyUrl(trimmed) -> fetchSpotify(trimmed, defaultTitle)
                isAppleMusicUrl(trimmed) -> fetchAppleMusic(trimmed, defaultTitle)
                isGaanaUrl(trimmed) -> fetchGaana(trimmed, defaultTitle)
                isJioSaavnUrl(trimmed) -> fetchJioSaavn(trimmed, defaultTitle)
                isDeezerUrl(trimmed) -> fetchDeezer(trimmed, defaultTitle)
                isAmazonMusicUrl(trimmed) -> fetchAmazonMusic(trimmed, defaultTitle)
                else -> fetchGenericWebPage(trimmed, defaultTitle)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to fetch playlist from $trimmed: ${e.message}")
            null
        }
    }

    private fun isYouTubeUrl(url: String): Boolean =
        url.contains("youtube.com") || url.contains("youtu.be") ||
            url.startsWith("PL") || url.startsWith("RDCLAK") || url.startsWith("OLAK")

    private fun isSpotifyUrl(url: String): Boolean =
        url.contains("spotify.com") || url.startsWith("spotify:") || url.contains("spotify.link")

    private fun isAppleMusicUrl(url: String): Boolean =
        url.contains("music.apple.com")

    private fun isJioSaavnUrl(url: String): Boolean =
        url.contains("jiosaavn.com")

    private fun isDeezerUrl(url: String): Boolean =
        url.contains("deezer.com") || url.contains("deezer.page.link")

    private fun isAmazonMusicUrl(url: String): Boolean =
        url.contains("music.amazon.")

    private fun isGaanaUrl(url: String): Boolean =
        url.contains("gaana.com")

    /**
     * 1. YouTube & YouTube Music
     */
    private suspend fun fetchYouTube(url: String, youTube: YouTube, defaultTitle: String?): ParsedPlaylist? {
        val playlistId = when {
            url.contains("list=") -> url.substringAfter("list=").substringBefore("&")
            url.startsWith("PL") || url.startsWith("RDCLAK") || url.startsWith("OLAK") -> url
            else -> null
        } ?: return null

        val plInfo = runCatching { youTube.playlist(playlistId).getOrNull() }.getOrNull()
        val title = plInfo?.playlist?.title?.takeIf { it.isNotBlank() } ?: defaultTitle ?: "YouTube Playlist"
        val fullTracks = youTube.getPlaylistFullTracks(playlistId).getOrNull() ?: emptyList()

        if (fullTracks.isEmpty()) return null

        val tracks = fullTracks.map { song ->
            val artistName = song.artists.joinToString(", ") { it.name }
            ParsedTrack(
                query = "$artistName ${song.title}".trim(),
                title = song.title,
                artist = artistName,
                videoId = song.id,
            )
        }

        return ParsedPlaylist(title = title, tracks = tracks)
    }

    /**
     * 2. Spotify (Embed Scraper - Zero Auth required)
     */
    private suspend fun fetchSpotify(url: String, defaultTitle: String?): ParsedPlaylist? {
        var resolvedUrl = url
        if (url.contains("spotify.link")) {
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, BROWSER_UA)
            }
            resolvedUrl = response.headers[HttpHeaders.Location] ?: url
        }

        val isAlbum = resolvedUrl.contains("/album/") || resolvedUrl.startsWith("spotify:album:")
        val type = if (isAlbum) "album" else "playlist"
        val id = when {
            resolvedUrl.contains("/$type/") ->
                resolvedUrl.substringAfter("/$type/").substringBefore("?").substringBefore("/")
            resolvedUrl.startsWith("spotify:$type:") ->
                resolvedUrl.substringAfter("spotify:$type:").substringBefore("?")
            else -> null
        } ?: return null

        val embedUrl = "https://open.spotify.com/embed/$type/$id"
        val html = httpClient.get(embedUrl) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }.bodyAsText()

        // 1. Extract from __NEXT_DATA__
        val nextDataMatch = Regex("""<script id="__NEXT_DATA__"[^>]*>([\s\S]*?)</script>""").find(html)
        if (nextDataMatch != null) {
            val jsonStr = nextDataMatch.groupValues[1]
            val root = runCatching { json.parseToJsonElement(jsonStr).jsonObject }.getOrNull()
            val entity = root?.get("props")?.jsonObject
                ?.get("pageProps")?.jsonObject
                ?.get("state")?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("entity")?.jsonObject

            if (entity != null) {
                val name = entity["name"]?.jsonPrimitive?.content ?: defaultTitle ?: "Spotify Playlist"
                val trackList = entity["trackList"]?.jsonArray ?: JsonArray(emptyList())

                val tracks = trackList.mapNotNull { element ->
                    val obj = element.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val artist = obj["subtitle"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: ""
                    ParsedTrack(
                        query = "$artist $title".trim(),
                        title = title,
                        artist = artist,
                    )
                }

                if (tracks.isNotEmpty()) {
                    return ParsedPlaylist(title = name, tracks = tracks)
                }
            }
        }

        // Fallback: Regex search for tracks in Spotify Embed HTML
        val titleMatch = Regex("""<title>([^<]+)</title>""").find(html)
        val title = titleMatch?.groupValues?.get(1)?.substringBefore(" | ")?.trim() ?: defaultTitle ?: "Spotify Playlist"
        val trackRegex = Regex(""""title":"([^"]+)".*?"subtitle":"([^"]+)"""")
        val tracks = trackRegex.findAll(html).map { match ->
            val trackTitle = match.groupValues[1]
            val trackArtist = match.groupValues[2]
            ParsedTrack(
                query = "$trackArtist $trackTitle".trim(),
                title = trackTitle,
                artist = trackArtist,
            )
        }.distinctBy { "${it.title}_${it.artist}" }.toList()

        return if (tracks.isNotEmpty()) ParsedPlaylist(title = title, tracks = tracks) else null
    }

    /**
     * 3. Apple Music (serialized-server-data & schema.org JSON-LD)
     */
    private suspend fun fetchAppleMusic(url: String, defaultTitle: String?): ParsedPlaylist? {
        val html = httpClient.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }.bodyAsText()

        // 1. Try serialized-server-data script
        val serverDataMatch = Regex("""<script type="application/json" id="serialized-server-data">([\s\S]*?)</script>""").find(html)
        if (serverDataMatch != null) {
            val root = runCatching { json.parseToJsonElement(serverDataMatch.groupValues[1]).jsonObject }.getOrNull()
            val dataArr = root?.get("data")?.jsonArray
            val firstSec = dataArr?.firstOrNull()?.jsonObject
            val secData = firstSec?.get("data")?.jsonObject
            val sections = secData?.get("sections")?.jsonArray ?: JsonArray(emptyList())

            var plTitle: String? = null
            val tracks = mutableListOf<ParsedTrack>()

            for (sec in sections) {
                val secObj = sec.jsonObject
                val items = secObj["items"]?.jsonArray ?: continue
                if (plTitle == null) {
                    plTitle = items.firstOrNull()?.jsonObject?.get("title")?.jsonPrimitive?.content
                }
                for (item in items) {
                    val itemObj = item.jsonObject
                    val title = itemObj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: continue
                    val artist = itemObj["artistName"]?.jsonPrimitive?.content ?: ""
                    if (artist.isNotBlank()) {
                        tracks.add(
                            ParsedTrack(
                                query = "$artist $title".trim(),
                                title = title,
                                artist = artist,
                            )
                        )
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                val finalTitle = plTitle ?: defaultTitle ?: "Apple Music Playlist"
                return ParsedPlaylist(title = finalTitle, tracks = tracks)
            }
        }

        // 2. Fallback: schema.org JSON-LD
        return parseSchemaOrgMusicPlaylist(html, defaultTitle ?: "Apple Music Playlist")
    }

    /**
     * 4. JioSaavn (Web API & JSON-LD)
     */
    private suspend fun fetchJioSaavn(url: String, defaultTitle: String?): ParsedPlaylist? {
        val token = url.trimEnd('/').substringAfterLast('/')
        val apiUrl = "https://www.jiosaavn.com/api.php?__call=webapi.get&token=$token&type=playlist&_format=json&p=1&n=200"

        val res = runCatching {
            httpClient.get(apiUrl) {
                header(HttpHeaders.UserAgent, BROWSER_UA)
            }.bodyAsText()
        }.getOrNull()

        if (!res.isNullOrBlank() && res.startsWith("{")) {
            val root = runCatching { json.parseToJsonElement(res).jsonObject }.getOrNull()
            val name = root?.get("title")?.jsonPrimitive?.content
                ?: root?.get("listname")?.jsonPrimitive?.content
                ?: defaultTitle ?: "JioSaavn Playlist"
            val songs = root?.get("songs")?.jsonArray ?: JsonArray(emptyList())

            val tracks = songs.mapNotNull { s ->
                val obj = s.jsonObject
                val title = obj["song"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artist = obj["primary_artists"]?.jsonPrimitive?.content
                    ?: obj["singers"]?.jsonPrimitive?.content
                    ?: ""
                ParsedTrack(
                    query = "$artist $title".trim(),
                    title = title,
                    artist = artist,
                )
            }

            if (tracks.isNotEmpty()) {
                return ParsedPlaylist(title = name, tracks = tracks)
            }
        }

        return fetchGenericWebPage(url, defaultTitle ?: "JioSaavn Playlist")
    }

    /**
     * 5. Amazon Music
     */
    private suspend fun fetchAmazonMusic(url: String, defaultTitle: String?): ParsedPlaylist? {
        return fetchGenericWebPage(url, defaultTitle ?: "Amazon Music Playlist")
    }

    /**
     * 6. Gaana (schema.org MusicPlaylist / MusicAlbum & fallback)
     */
    private suspend fun fetchGaana(url: String, defaultTitle: String?): ParsedPlaylist? {
        val html = httpClient.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }.bodyAsText()

        // 1. Try schema.org JSON-LD (Gaana outputs @type: MusicPlaylist or MusicAlbum with full track list)
        val schemaPlaylist = parseSchemaOrgMusicPlaylist(html, defaultTitle ?: "Gaana Playlist")
        if (schemaPlaylist != null && schemaPlaylist.tracks.isNotEmpty()) {
            return schemaPlaylist
        }

        // 2. Fallback: regex search for Gaana track links
        val titleMatch = Regex("""<title>([^<]+)</title>""").find(html)
        val title = titleMatch?.groupValues?.get(1)?.substringBefore(" | ")?.substringBefore(" - ")?.trim()
            ?: defaultTitle ?: "Gaana Playlist"

        val songRegex = Regex("""href="/song/([^"]+)"""")
        val tracks = songRegex.findAll(html).map { match ->
            val slug = match.groupValues[1].replace('-', ' ')
            ParsedTrack(
                query = slug,
                title = slug,
                artist = "",
            )
        }.distinctBy { it.title }.toList()

        return if (tracks.isNotEmpty()) ParsedPlaylist(title = title, tracks = tracks) else null
    }

    /**
     * 7. Deezer (Official Public API - Zero Auth required)
     */
    private suspend fun fetchDeezer(url: String, defaultTitle: String?): ParsedPlaylist? {
        var resolvedUrl = url
        if (url.contains("deezer.page.link")) {
            val res = httpClient.get(url) { header(HttpHeaders.UserAgent, BROWSER_UA) }
            resolvedUrl = res.headers[HttpHeaders.Location] ?: url
        }
        val isAlbum = resolvedUrl.contains("/album/")
        val type = if (isAlbum) "album" else "playlist"
        val id = Regex("""/(?:playlist|album)/(\d+)""").find(resolvedUrl)?.groupValues?.get(1) ?: return null
        val apiUrl = "https://api.deezer.com/$type/$id"
        val jsonStr = httpClient.get(apiUrl) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
        }.bodyAsText()

        val root = runCatching { json.parseToJsonElement(jsonStr).jsonObject }.getOrNull() ?: return null
        val title = root["title"]?.jsonPrimitive?.content ?: defaultTitle ?: "Deezer Playlist"
        val tracksObj = root["tracks"]?.jsonObject
        val trackArray = tracksObj?.get("data")?.jsonArray ?: root["data"]?.jsonArray ?: JsonArray(emptyList())

        val tracks = trackArray.mapNotNull { item ->
            val obj = item.jsonObject
            val trackTitle = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val artist = obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
            ParsedTrack(
                query = "$artist $trackTitle".trim(),
                title = trackTitle,
                artist = artist,
            )
        }

        return if (tracks.isNotEmpty()) ParsedPlaylist(title = title, tracks = tracks) else null
    }

    /**
     * 8. Generic Web Page / Fallback
     */
    private suspend fun fetchGenericWebPage(url: String, defaultTitle: String?): ParsedPlaylist? {
        val html = httpClient.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }.bodyAsText()

        val parsedSchema = parseSchemaOrgMusicPlaylist(html, defaultTitle)
        if (parsedSchema != null && parsedSchema.tracks.isNotEmpty()) {
            return parsedSchema
        }

        // Title from og:title or <title>
        val titleMatch = Regex("""<meta property="og:title" content="([^"]+)"""").find(html)
            ?: Regex("""<title>([^<]+)</title>""").find(html)
        val title = titleMatch?.groupValues?.get(1)?.trim() ?: defaultTitle ?: "Imported Playlist"

        // Search for music:song tags
        val songTags = Regex("""<meta (?:property|name)="music:song" content="([^"]+)"""").findAll(html).toList()
        if (songTags.isNotEmpty()) {
            val tracks = songTags.map { ParsedTrack(query = it.groupValues[1]) }
            return ParsedPlaylist(title = title, tracks = tracks)
        }

        return null
    }

    /**
     * Extracts track list from schema.org JSON-LD
     */
    private fun parseSchemaOrgMusicPlaylist(html: String, defaultTitle: String?): ParsedPlaylist? {
        val scriptMatches = Regex("""<script[^>]*type="application/ld\+json"[^>]*>([\s\S]*?)</script>""").findAll(html)

        for (match in scriptMatches) {
            val content = match.groupValues[1].trim()
            val parsedElement = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: continue

            val objectsToScan = mutableListOf<JsonObject>()
            when (parsedElement) {
                is JsonObject -> {
                    if (parsedElement.containsKey("@graph") && parsedElement["@graph"] is JsonArray) {
                        parsedElement["@graph"]!!.jsonArray.forEach { if (it is JsonObject) objectsToScan.add(it) }
                    } else {
                        objectsToScan.add(parsedElement)
                    }
                }
                is JsonArray -> {
                    parsedElement.forEach { if (it is JsonObject) objectsToScan.add(it) }
                }
                else -> {}
            }

            for (root in objectsToScan) {
                val type = root["@type"]?.jsonPrimitive?.content ?: ""

                if (type == "MusicPlaylist" || type == "MusicAlbum" || type == "ItemList") {
                    val title = root["name"]?.jsonPrimitive?.content ?: defaultTitle ?: "Imported Playlist"
                    val trackElements = root["track"]?.jsonArray
                        ?: root["itemListElement"]?.jsonArray
                        ?: root["tracks"]?.jsonArray
                        ?: continue

                    val tracks = trackElements.mapNotNull { elem ->
                        if (elem !is JsonObject) return@mapNotNull null
                        val itemObj = if (elem.containsKey("item") && elem["item"] is JsonObject) {
                            elem["item"]!!.jsonObject
                        } else {
                            elem
                        }
                        val trackName = itemObj["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                        val artistName = when (val byArtist = itemObj["byArtist"]) {
                            is JsonObject -> byArtist["name"]?.jsonPrimitive?.content
                            is JsonArray -> byArtist.firstOrNull()?.let {
                                if (it is JsonObject) it["name"]?.jsonPrimitive?.content
                                else runCatching { it.jsonPrimitive.content }.getOrNull()
                            }
                            is JsonPrimitive -> byArtist.content
                            else -> null
                        } ?: ""

                        ParsedTrack(
                            query = "$artistName $trackName".trim(),
                            title = trackName,
                            artist = artistName,
                        )
                    }

                    if (tracks.isNotEmpty()) {
                        return ParsedPlaylist(title = title, tracks = tracks)
                    }
                }
            }
        }

        return null
    }
}
