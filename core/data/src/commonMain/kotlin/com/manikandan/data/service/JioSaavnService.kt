package com.manikandan.data.service

import com.manikandan.data.mediaservice.TrackLanguageFilter
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.browse.playlist.Author
import com.manikandan.domain.data.model.browse.playlist.PlaylistBrowse
import com.manikandan.domain.data.model.searchResult.SearchSuggestions
import com.manikandan.domain.data.model.searchResult.playlists.PlaylistsResult
import com.manikandan.domain.data.model.searchResult.songs.Album
import com.manikandan.domain.data.model.searchResult.songs.Artist
import com.manikandan.domain.data.model.searchResult.songs.SongsResult
import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.data.type.SearchResultType
import com.manikandan.domain.utils.toTrack
import com.manikandan.ktorext.getEngine
import com.manikandan.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * High-performance backend music and playlist provider for JioSaavn.
 * Provides invisible backend search, playlist fetching, high-res (320kbps) audio stream
 * resolution, and endless same-language radio queues without exposing external branding.
 */
object JioSaavnService {
    private const val TAG = "JioSaavnService"
    const val SAAVN_RADIO_PREFIX = "saavn_radio:"
    const val SAAVN_CONT_PREFIX = "saavn_cont:"

    private val client by lazy {
        HttpClient(getEngine()) {
            expectSuccess = false
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Memory caches for stream URLs, Track metadata, normalized languages, and radio stations
    private val streamUrlCache = Collections.synchronizedMap(HashMap<String, String>())
    private val trackCache = Collections.synchronizedMap(HashMap<String, Track>())
    private val trackLanguageCache = Collections.synchronizedMap(HashMap<String, String>())
    private val stationIdCache = Collections.synchronizedMap(HashMap<String, String>())

    fun getCachedTrack(videoId: String): Track? {
        val cleanId = videoId.removePrefix("saavn:")
        return trackCache[cleanId] ?: trackCache[videoId]
    }

    fun getTrackLanguage(videoId: String): String? {
        val cleanId = videoId.removePrefix("saavn:")
        return trackLanguageCache[cleanId] ?: trackLanguageCache[videoId] ?: trackLanguageCache["saavn:$cleanId"]
    }

    private fun recordTrackLanguage(id: String, rawLang: String?) {
        val cleanId = id.removePrefix("saavn:")
        val normalized = TrackLanguageFilter.normalizeLanguageCode(rawLang)
        if (normalized != TrackLanguageFilter.LANG_UNKNOWN) {
            trackLanguageCache[cleanId] = normalized
            trackLanguageCache["saavn:$cleanId"] = normalized
        }
    }

    private fun Track.toSongsResult(): SongsResult = SongsResult(
        album = album,
        artists = artists,
        category = category ?: "SONG",
        duration = duration,
        durationSeconds = durationSeconds,
        feedbackTokens = feedbackTokens,
        isExplicit = isExplicit,
        resultType = resultType ?: "Song",
        thumbnails = thumbnails ?: emptyList(),
        title = title,
        videoId = videoId,
        videoType = videoType ?: "Song",
        year = year ?: "",
    )

    private fun parseDirectSongObj(obj: JsonObject): Track? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val rawTitle = obj["title"]?.jsonPrimitive?.content
            ?: obj["song"]?.jsonPrimitive?.content
            ?: return null
        val title = rawTitle.unescapeHtml()
        if (title.isBlank()) return null

        val moreInfo = obj["more_info"]?.jsonObject
        val albumName = (moreInfo?.get("album")?.jsonPrimitive?.content
            ?: obj["album"]?.jsonPrimitive?.content)?.unescapeHtml()

        val durationSec = moreInfo?.get("duration")?.jsonPrimitive?.content?.toIntOrNull()
            ?: obj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
        val durationStr = if (durationSec != null) {
            "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}"
        } else null

        val artistMapPrimary = moreInfo?.get("artistMap")?.jsonObject?.get("primary_artists")?.jsonArray
        val artistsFromMap = artistMapPrimary?.mapNotNull { art ->
            val name = art.jsonObject["name"]?.jsonPrimitive?.content?.unescapeHtml()
            if (!name.isNullOrBlank()) Artist(name = name, id = null) else null
        }.orEmpty()

        val artists = if (artistsFromMap.isNotEmpty()) {
            artistsFromMap
        } else {
            val rawArtist = obj["subtitle"]?.jsonPrimitive?.content?.unescapeHtml()
                ?: moreInfo?.get("music")?.jsonPrimitive?.content?.unescapeHtml()
                ?: moreInfo?.get("singers")?.jsonPrimitive?.content?.unescapeHtml()
                ?: obj["singers"]?.jsonPrimitive?.content?.unescapeHtml()
            if (!rawArtist.isNullOrBlank()) {
                rawArtist.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != albumName && it != title }
                    .map { Artist(name = it, id = null) }
            } else {
                emptyList()
            }
        }

        val rawImg = obj["image"]?.jsonPrimitive?.content ?: ""
        val highResImg = rawImg.replace("150x150", "500x500").replace("50x50", "500x500")
        val thumbnails = if (highResImg.isNotBlank()) {
            listOf(Thumbnail(url = highResImg, width = 500, height = 500))
        } else emptyList()

        val rawLang = obj["language"]?.jsonPrimitive?.content
            ?: moreInfo?.get("language")?.jsonPrimitive?.content
        recordTrackLanguage(id, rawLang)

        val encryptedUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
            ?: obj["encrypted_media_url"]?.jsonPrimitive?.content
        if (!encryptedUrl.isNullOrBlank()) {
            val decrypted = decryptDes(encryptedUrl)
            if (!decrypted.isNullOrBlank()) {
                val has320 = (moreInfo?.get("320kbps")?.jsonPrimitive?.content
                    ?: obj["320kbps"]?.jsonPrimitive?.content) != "false"
                val streamUrl = if (has320) {
                    decrypted.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
                } else {
                    decrypted.replace("_96.mp4", "_160.mp4")
                }
                streamUrlCache[id] = streamUrl
                streamUrlCache["saavn:$id"] = streamUrl
            }
        }

        val track = Track(
            album = Album(name = albumName ?: "", id = ""),
            artists = artists,
            duration = durationStr,
            durationSeconds = durationSec,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = thumbnails,
            title = title,
            videoId = "saavn:$id",
            videoType = "Song",
            category = "SONG",
            feedbackTokens = null,
            resultType = "Song",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
        )
        trackCache[id] = track
        trackCache["saavn:$id"] = track
        return track
    }

    private fun parseSaavnDevSongObj(obj: JsonObject): Track? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val title = (obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "").unescapeHtml()
        if (title.isBlank()) return null

        val albumName = obj["album"]?.jsonObject?.get("name")?.jsonPrimitive?.content?.unescapeHtml()
        val durationSec = obj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
        val durationStr = if (durationSec != null) {
            "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}"
        } else null

        val artistsObj = obj["artists"]?.jsonObject?.get("primary")?.jsonArray
        val artists = artistsObj?.mapNotNull { art ->
            val artName = art.jsonObject["name"]?.jsonPrimitive?.content?.unescapeHtml()
            if (!artName.isNullOrBlank()) Artist(name = artName, id = null) else null
        } ?: emptyList()

        val images = obj["image"]?.jsonArray
        val bestImg = images?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: images?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
        val thumbnails = if (bestImg.isNotBlank()) listOf(Thumbnail(url = bestImg, width = 500, height = 500)) else emptyList()

        val rawLang = obj["language"]?.jsonPrimitive?.content
        recordTrackLanguage(id, rawLang)

        val downloadUrls = obj["downloadUrl"]?.jsonArray
        val bestAudio = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            ?: downloadUrls?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
        if (!bestAudio.isNullOrBlank()) {
            streamUrlCache[id] = bestAudio
            streamUrlCache["saavn:$id"] = bestAudio
        }

        val track = Track(
            album = Album(name = albumName ?: "", id = ""),
            artists = artists,
            duration = durationStr,
            durationSeconds = durationSec,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = thumbnails,
            title = title,
            videoId = "saavn:$id",
            videoType = "Song",
            category = "SONG",
            feedbackTokens = null,
            resultType = "Song",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
        )
        trackCache[id] = track
        trackCache["saavn:$id"] = track
        return track
    }

    private fun String.unescapeHtml(): String = this
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .trim()

    /**
     * Retrieves autocomplete search suggestions and recommended song items from JioSaavn.
     */
    suspend fun getSearchSuggestions(query: String): SearchSuggestions = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext SearchSuggestions(emptyList(), emptyList())
        val cleanQuery = query.trim()
        val queries = mutableListOf<String>()
        val songs = mutableListOf<SongsResult>()

        try {
            val encoded = cleanQuery.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&ctx=web6dot0"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject

                // 1. Top query suggestions
                val topData = root["topquery"]?.jsonObject?.get("data")?.jsonArray
                topData?.forEach { elem ->
                    val title = elem.jsonObject["title"]?.jsonPrimitive?.content?.unescapeHtml()
                    if (!title.isNullOrBlank() && !queries.contains(title)) {
                        queries.add(title)
                    }
                }

                // 2. Song suggestions
                val songData = root["songs"]?.jsonObject?.get("data")?.jsonArray
                songData?.forEach { elem ->
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@forEach
                    val title = obj["title"]?.jsonPrimitive?.content?.unescapeHtml() ?: return@forEach
                    val album = obj["album"]?.jsonPrimitive?.content?.unescapeHtml() ?: ""
                    val rawImg = obj["image"]?.jsonPrimitive?.content ?: ""
                    val highResImg = rawImg.replace("50x50", "500x500").replace("150x150", "500x500")
                    val moreInfo = obj["more_info"]?.jsonObject
                    val rawArtist = moreInfo?.get("primary_artists")?.jsonPrimitive?.content?.unescapeHtml()
                        ?: obj["description"]?.jsonPrimitive?.content?.unescapeHtml()
                    val artists = if (!rawArtist.isNullOrBlank()) {
                        rawArtist.split(",", "·").mapNotNull {
                            val trimmed = it.trim()
                            if (trimmed.isNotBlank() && trimmed != title && trimmed != album) Artist(name = trimmed, id = null) else null
                        }
                    } else emptyList()

                    recordTrackLanguage(
                        id,
                        obj["language"]?.jsonPrimitive?.content ?: moreInfo?.get("language")?.jsonPrimitive?.content,
                    )

                    val thumbnails = if (highResImg.isNotBlank()) {
                        listOf(Thumbnail(url = highResImg, width = 500, height = 500))
                    } else emptyList()

                    val track = Track(
                        album = Album(name = album, id = ""),
                        artists = artists,
                        duration = null,
                        durationSeconds = null,
                        isAvailable = true,
                        isExplicit = false,
                        likeStatus = null,
                        thumbnails = thumbnails,
                        title = title,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        category = "SONG",
                        feedbackTokens = null,
                        resultType = "Song",
                        year = "",
                    )
                    trackCache[id] = track
                    trackCache["saavn:$id"] = track
                    songs.add(track.toSongsResult())
                }

                // 3. Album suggestions (as query keywords)
                val albumData = root["albums"]?.jsonObject?.get("data")?.jsonArray
                albumData?.forEach { elem ->
                    val title = elem.jsonObject["title"]?.jsonPrimitive?.content?.unescapeHtml()
                    if (!title.isNullOrBlank() && !queries.contains(title)) {
                        queries.add(title)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.d(TAG, "getSearchSuggestions error: ${e.message}")
        }

        SearchSuggestions(queries = queries, recommendedItems = songs)
    }

    /**
     * Searches songs from JioSaavn and maps them to domain [SongsResult].
     */
    suspend fun searchSongs(query: String): List<SongsResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim()
        val songs = searchSongsDirect(cleanQuery)
        if (songs.isNotEmpty()) return@withContext songs
        searchSongsSaavnDev(cleanQuery)
    }

    private suspend fun searchSongsPageDirect(query: String, page: Int = 1, count: Int = 20): List<Track> {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&n=$count&p=$page&q=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: return emptyList()
                results.mapNotNull { parseDirectSongObj(it.jsonObject) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Logger.d(TAG, "searchSongsPageDirect error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchSongsDirect(query: String): List<SongsResult> =
        searchSongsPageDirect(query, page = 1, count = 20).map { it.toSongsResult() }

    private suspend fun searchSongsPageSaavnDev(query: String, page: Int = 1, limit: Int = 20): List<Track> {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://saavn.dev/api/search/songs?query=$encoded&page=$page&limit=$limit"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonObject
                val results = data?.get("results")?.jsonArray ?: return emptyList()
                results.mapNotNull { parseSaavnDevSongObj(it.jsonObject) }
            } else emptyList()
        } catch (e: Exception) {
            Logger.d(TAG, "searchSongsSaavnDev error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchSongsSaavnDev(query: String): List<SongsResult> =
        searchSongsPageSaavnDev(query, page = 1, limit = 20).map { it.toSongsResult() }

    /**
     * Searches playlists from JioSaavn and maps them to domain [PlaylistsResult].
     */
    suspend fun searchPlaylists(query: String): List<PlaylistsResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim()
        val direct = searchPlaylistsDirect(cleanQuery)
        if (direct.isNotEmpty()) return@withContext direct
        searchPlaylistsSaavnDev(cleanQuery)
    }

    private suspend fun searchPlaylistsDirect(query: String): List<PlaylistsResult> {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val playlistsObj = root["playlists"]?.jsonObject
                val data = playlistsObj?.get("data")?.jsonArray ?: return emptyList()

                data.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = (obj["title"]?.jsonPrimitive?.content ?: "").unescapeHtml()
                    if (title.isBlank()) return@mapNotNull null

                    val rawImg = obj["image"]?.jsonPrimitive?.content ?: ""
                    val highResImg = rawImg.replace("50x50", "500x500").replace("150x150", "500x500")
                    val thumbnails = if (highResImg.isNotBlank()) listOf(Thumbnail(url = highResImg, width = 500, height = 500)) else emptyList()

                    PlaylistsResult(
                        author = "Playlist",
                        browseId = "saavn_pl:$id",
                        category = "PLAYLIST",
                        itemCount = "",
                        resultType = "Playlist",
                        thumbnails = thumbnails,
                        title = title,
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            Logger.d(TAG, "searchPlaylistsDirect error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchPlaylistsSaavnDev(query: String): List<PlaylistsResult> {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://saavn.dev/api/search/playlists?query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonObject
                val results = data?.get("results")?.jsonArray ?: return emptyList()

                results.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = (obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "").unescapeHtml()
                    val images = obj["image"]?.jsonArray
                    val bestImg = images?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                        ?: images?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                    val thumbnails = if (bestImg.isNotBlank()) listOf(Thumbnail(url = bestImg, width = 500, height = 500)) else emptyList()

                    PlaylistsResult(
                        author = "Playlist",
                        browseId = "saavn_pl:$id",
                        category = "PLAYLIST",
                        itemCount = obj["songCount"]?.jsonPrimitive?.content ?: "",
                        resultType = "Playlist",
                        thumbnails = thumbnails,
                        title = title,
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            Logger.d(TAG, "searchPlaylistsSaavnDev error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Retrieves playlist details and tracklist for a JioSaavn playlist.
     */
    suspend fun getPlaylistBrowse(playlistId: String): PlaylistBrowse? = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("saavn_pl:")
        getPlaylistBrowseDirect(cleanId) ?: getPlaylistBrowseSaavnDev(cleanId)
    }

    private suspend fun getPlaylistBrowseDirect(listId: String): PlaylistBrowse? {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=playlist.getDetails&_format=json&_marker=0&api_version=4&listid=$listId"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val title = (root["title"]?.jsonPrimitive?.content ?: "").unescapeHtml()
                val rawImg = root["image"]?.jsonPrimitive?.content ?: ""
                val highResImg = rawImg.replace("150x150", "500x500").replace("50x50", "500x500")
                val thumbnails = if (highResImg.isNotBlank()) listOf(Thumbnail(url = highResImg, width = 500, height = 500)) else emptyList()

                val list = root["list"]?.jsonArray ?: root["songs"]?.jsonArray ?: return null
                val tracks = list.mapNotNull { parseDirectSongObj(it.jsonObject) }

                PlaylistBrowse(
                    author = Author(id = "author_$listId", name = "Playlist"),
                    description = root["header_desc"]?.jsonPrimitive?.content?.unescapeHtml(),
                    duration = "",
                    durationSeconds = tracks.sumOf { it.durationSeconds ?: 0 },
                    id = "saavn_pl:$listId",
                    privacy = "PUBLIC",
                    thumbnails = thumbnails,
                    title = title,
                    trackCount = tracks.size,
                    tracks = tracks,
                    year = root["year"]?.jsonPrimitive?.content ?: "",
                )
            } else null
        } catch (e: Exception) {
            Logger.d(TAG, "getPlaylistBrowseDirect error: ${e.message}")
            null
        }
    }

    private suspend fun getPlaylistBrowseSaavnDev(listId: String): PlaylistBrowse? {
        return try {
            val url = "https://saavn.dev/api/playlists?id=$listId"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonObject ?: return null
                val title = (data["name"]?.jsonPrimitive?.content ?: "").unescapeHtml()
                val images = data["image"]?.jsonArray
                val bestImg = images?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?: images?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                val thumbnails = if (bestImg.isNotBlank()) listOf(Thumbnail(url = bestImg, width = 500, height = 500)) else emptyList()

                val songs = data["songs"]?.jsonArray ?: return null
                val tracks = songs.mapNotNull { parseSaavnDevSongObj(it.jsonObject) }

                PlaylistBrowse(
                    author = Author(id = "author_$listId", name = "Playlist"),
                    description = data["description"]?.jsonPrimitive?.content?.unescapeHtml(),
                    duration = "",
                    durationSeconds = tracks.sumOf { it.durationSeconds ?: 0 },
                    id = "saavn_pl:$listId",
                    privacy = "PUBLIC",
                    thumbnails = thumbnails,
                    title = title,
                    trackCount = tracks.size,
                    tracks = tracks,
                    year = data["year"]?.jsonPrimitive?.content ?: "",
                )
            } else null
        } catch (e: Exception) {
            Logger.d(TAG, "getPlaylistBrowseSaavnDev error: ${e.message}")
            null
        }
    }

    /**
     * Fetches full song metadata (including language, artists, and decrypted stream URL) by ID.
     */
    suspend fun fetchSongDetails(songId: String): Track? = withContext(Dispatchers.IO) {
        val cleanId = songId.removePrefix(SAAVN_RADIO_PREFIX).removePrefix("saavn:")
        if (cleanId.isBlank()) return@withContext null

        try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$cleanId&_format=json&_marker=0&api_version=4"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val songObj = root[cleanId]?.jsonObject
                    ?: root["songs"]?.jsonArray?.firstOrNull()?.jsonObject
                if (songObj != null) {
                    parseDirectSongObj(songObj)?.let { return@withContext it }
                }
            }
        } catch (e: Exception) {
            Logger.d(TAG, "fetchSongDetails direct error: ${e.message}")
        }

        try {
            val url = "https://saavn.dev/api/songs/$cleanId"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val songObj = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
                if (songObj != null) {
                    parseSaavnDevSongObj(songObj)?.let { return@withContext it }
                }
            }
        } catch (e: Exception) {
            Logger.d(TAG, "fetchSongDetails saavn.dev error: ${e.message}")
        }

        getCachedTrack(cleanId)
    }

    private suspend fun getOrCreateStationId(cleanSeedId: String): String? {
        stationIdCache[cleanSeedId]?.let { return it }
        return try {
            val encodedEntity = """["$cleanSeedId"]""".encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=webradio.createEntityStation&api_version=4&_format=json&_marker=0&ctx=android&entity_id=$encodedEntity&entity_type=queue"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val stationId = root["stationid"]?.jsonPrimitive?.content
                if (!stationId.isNullOrBlank()) {
                    stationIdCache[cleanSeedId] = stationId
                    stationId
                } else null
            } else null
        } catch (e: Exception) {
            Logger.d(TAG, "getOrCreateStationId error: ${e.message}")
            null
        }
    }

    private suspend fun fetchStationSongs(stationId: String, count: Int = 25): List<Track> {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=webradio.getSong&api_version=4&_format=json&_marker=0&ctx=android&stationid=${stationId.encodeURLParameter()}&k=$count&next=1"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value !in 200..299) return emptyList()
            val elem = json.parseToJsonElement(response.bodyAsText())
            when (elem) {
                is JsonArray -> elem.mapNotNull { item ->
                    val obj = item.jsonObject
                    parseDirectSongObj(obj["song"]?.jsonObject ?: obj)
                }
                is JsonObject -> {
                    val tracks = mutableListOf<Track>()
                    elem["songs"]?.jsonArray?.forEach { item ->
                        val obj = item.jsonObject
                        parseDirectSongObj(obj["song"]?.jsonObject ?: obj)?.let { tracks.add(it) }
                    }
                    elem["song"]?.jsonObject?.let { single ->
                        parseDirectSongObj(single)?.let { tracks.add(it) }
                    }
                    for ((key, value) in elem) {
                        if (key == "error" || key == "stationid" || key == "songs" || key == "song") continue
                        val entryObj = value as? JsonObject ?: continue
                        val songObj = entryObj["song"] as? JsonObject ?: entryObj
                        parseDirectSongObj(songObj)?.let { tracks.add(it) }
                    }
                    tracks
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Logger.d(TAG, "fetchStationSongs error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchRecoSongs(cleanSeedId: String, langName: String?): List<Track> {
        return try {
            val langParam = if (!langName.isNullOrBlank()) "&language=${langName.encodeURLParameter()}" else ""
            val url = "https://www.jiosaavn.com/api.php?__call=reco.getreco&api_version=4&_format=json&_marker=0&pid=$cleanSeedId$langParam"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value !in 200..299) return emptyList()
            val elem = json.parseToJsonElement(response.bodyAsText())
            when (elem) {
                is JsonArray -> elem.mapNotNull { parseDirectSongObj(it.jsonObject) }
                is JsonObject -> {
                    val arr = elem["results"]?.jsonArray ?: elem["list"]?.jsonArray ?: elem["songs"]?.jsonArray
                    if (arr != null) {
                        arr.mapNotNull { parseDirectSongObj(it.jsonObject) }
                    } else {
                        elem.values.mapNotNull { v ->
                            (v as? JsonObject)?.let { parseDirectSongObj(it) }
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Logger.d(TAG, "fetchRecoSongs error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchSaavnDevSuggestions(cleanSeedId: String, limit: Int = 25): List<Track> {
        return try {
            val url = "https://saavn.dev/api/songs/$cleanSeedId/suggestions?limit=$limit"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value !in 200..299) return emptyList()
            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val dataArr = root["data"]?.jsonArray ?: return emptyList()
            dataArr.mapNotNull { parseSaavnDevSongObj(it.jsonObject) }
        } catch (e: Exception) {
            Logger.d(TAG, "fetchSaavnDevSuggestions error: ${e.message}")
            emptyList()
        }
    }

    private fun normalizeTitleKey(rawTitle: String?): String {
        if (rawTitle.isNullOrBlank()) return ""
        return rawTitle
            .lowercase()
            .replace(Regex("""\([^)]*\)|\[[^\]]*]"""), " ")
            .replace(Regex("""(?i)\b(from|feat|ft|lofi|slowed|reverb|remix|unplugged|version|acoustic|sped up)\b.*"""), " ")
            .replace(Regex("""[^a-z0-9\u0900-\u0DFF\uAC00-\uD7AF\u3040-\u30FF]"""), "")
            .trim()
    }

    private fun isSameLanguageRadioMatch(
        candidate: Track,
        seedTrack: Track?,
        targetLang: String,
    ): Boolean {
        if (targetLang == TrackLanguageFilter.LANG_UNKNOWN) {
            return seedTrack == null || TrackLanguageFilter.isCompatibleLanguage(candidate, listOf(seedTrack))
        }
        val candLang = getTrackLanguage(candidate.videoId)
            ?: TrackLanguageFilter.detectLanguage(candidate)
        if (candLang != TrackLanguageFilter.LANG_UNKNOWN) {
            return candLang == targetLang
        }
        return if (targetLang == TrackLanguageFilter.LANG_ENGLISH) {
            true
        } else {
            seedTrack != null && TrackLanguageFilter.isCompatibleLanguage(candidate, listOf(seedTrack))
        }
    }

    private fun buildContinuation(seedId: String, page: Int, langCode: String): String =
        "$SAAVN_CONT_PREFIX$seedId:$page:$langCode"

    /**
     * Generates an endless, same-language radio batch for a JioSaavn track, returning both the
     * tracks and a non-null continuation cursor for continuous infinite queue expansion.
     */
    suspend fun getRelatedRadio(
        seedVideoId: String,
        page: Int = 1,
        langOverride: String? = null,
        excludeIds: Set<String> = emptySet(),
    ): Pair<List<Track>, String?> = withContext(Dispatchers.IO) {
        val cleanSeed = seedVideoId.removePrefix(SAAVN_RADIO_PREFIX).removePrefix("saavn:")
        if (cleanSeed.isBlank()) return@withContext Pair(emptyList(), null)

        var seedTrack = getCachedTrack(cleanSeed)
        var resolvedLang = langOverride
            ?.let { TrackLanguageFilter.normalizeLanguageCode(it) }
            ?.takeIf { it != TrackLanguageFilter.LANG_UNKNOWN }
            ?: getTrackLanguage(cleanSeed)?.takeIf { it != TrackLanguageFilter.LANG_UNKNOWN }

        if (seedTrack == null || resolvedLang == null || seedTrack.artists.isNullOrEmpty()) {
            fetchSongDetails(cleanSeed)?.let { detailed ->
                seedTrack = detailed
            }
            if (resolvedLang == null) {
                resolvedLang = getTrackLanguage(cleanSeed)?.takeIf { it != TrackLanguageFilter.LANG_UNKNOWN }
            }
        }

        val targetLang = resolvedLang
            ?: seedTrack?.let { TrackLanguageFilter.detectLanguage(it) }
            ?: TrackLanguageFilter.LANG_UNKNOWN
        val langQuery = TrackLanguageFilter.toLanguageQueryName(targetLang)
        val seedArtist = seedTrack?.artists?.firstOrNull()?.name?.trim().orEmpty()
        val secondArtist = seedTrack?.artists?.getOrNull(1)?.name?.trim().orEmpty()
        val seedAlbum = seedTrack?.album?.name?.trim().orEmpty()
        val seedTitleKey = normalizeTitleKey(seedTrack?.title)

        val candidates = mutableListOf<Track>()

        // 1. Official JioSaavn Entity Station (fresh radio tracks each request)
        val stationId = getOrCreateStationId(cleanSeed)
        if (stationId != null) {
            candidates.addAll(fetchStationSongs(stationId, count = 25))
        }

        // 2. Song recommendations & suggestions (richest on page 1 or when station returns few songs)
        if (page == 1 || candidates.size < 10) {
            candidates.addAll(fetchRecoSongs(cleanSeed, langQuery))
            candidates.addAll(fetchSaavnDevSuggestions(cleanSeed, limit = 25))
        }

        // 3. Paginated same-language discovery search to ensure infinite variety on subsequent batches
        val discoveryQueries = buildList {
            if (seedArtist.isNotBlank()) {
                add(if (langQuery != null) "$seedArtist $langQuery songs" else "$seedArtist hits")
            }
            if (secondArtist.isNotBlank() && secondArtist != seedArtist) {
                add(if (langQuery != null) "$secondArtist $langQuery songs" else "$secondArtist songs")
            }
            if (seedAlbum.isNotBlank() && page == 1) {
                add(if (langQuery != null) "$seedAlbum $langQuery" else seedAlbum)
            }
            if (langQuery != null) {
                add("trending $langQuery songs")
                add("top $langQuery hits")
                add("best $langQuery songs")
                add("latest $langQuery songs")
                add("evergreen $langQuery hits")
            } else if (!seedTrack?.title.isNullOrBlank()) {
                add(seedTrack!!.title)
            }
        }

        fun collectMatching(rawList: List<Track>, out: MutableList<Track>, seenIds: MutableSet<String>, seenTitleArtist: MutableSet<String>) {
            for (cand in rawList) {
                if (cand.videoId in seenIds) continue
                val titleKey = normalizeTitleKey(cand.title)
                if (titleKey.isBlank()) continue
                // Skip clones/slowed/lofi variants of the seed song itself so queue always progresses
                if (seedTitleKey.isNotBlank() && titleKey == seedTitleKey) continue
                val artistKey = cand.artists?.firstOrNull()?.name?.trim()?.lowercase().orEmpty()
                val dedupeKey = "$titleKey|$artistKey"
                if (dedupeKey in seenTitleArtist) continue
                if (!isSameLanguageRadioMatch(cand, seedTrack, targetLang)) continue
                seenIds.add(cand.videoId)
                seenTitleArtist.add(dedupeKey)
                out.add(cand)
            }
        }

        val accepted = mutableListOf<Track>()
        val seenIds = excludeIds.toMutableSet().apply { add("saavn:$cleanSeed") }
        val seenTitleArtist = mutableSetOf<String>()
        collectMatching(candidates, accepted, seenIds, seenTitleArtist)

        if (accepted.size < 14 && discoveryQueries.isNotEmpty()) {
            val startQueryIdx = (page - 1) % discoveryQueries.size
            for (offset in 0 until discoveryQueries.size.coerceAtMost(3)) {
                val queryIdx = (startQueryIdx + offset) % discoveryQueries.size
                val query = discoveryQueries[queryIdx]
                val searchPage = ((page - 1) / discoveryQueries.size) + 1 + offset
                val directPage = searchSongsPageDirect(query, page = searchPage, count = 25)
                val fallbackPage = if (directPage.isEmpty()) {
                    searchSongsPageSaavnDev(query, page = searchPage, limit = 25)
                } else emptyList()
                collectMatching(directPage + fallbackPage, accepted, seenIds, seenTitleArtist)
                if (accepted.size >= 18) break
            }
        }

        val nextCont = buildContinuation(cleanSeed, page + 1, targetLang)
        Logger.d(TAG, "getRelatedRadio(seed=$cleanSeed, page=$page, lang=$targetLang) -> ${accepted.size} tracks")
        Pair(accepted, nextCont)
    }

    /**
     * Continues an active JioSaavn radio queue using the encoded [continuation] cursor.
     */
    suspend fun getContinueRadioTracks(
        playlistId: String,
        continuation: String,
        excludeIds: Set<String> = emptySet(),
    ): Pair<List<Track>, String?> {
        var seedId = playlistId
            .removePrefix(SAAVN_RADIO_PREFIX)
            .removePrefix("saavn_pl:")
            .removePrefix("RDAMVM")
            .removePrefix("saavn:")
        var page = 2
        var langOverride: String? = null

        if (continuation.startsWith(SAAVN_CONT_PREFIX)) {
            val parts = continuation.removePrefix(SAAVN_CONT_PREFIX).split(":")
            if (parts.isNotEmpty() && parts[0].isNotBlank()) seedId = parts[0]
            if (parts.size >= 2) page = (parts[1].toIntOrNull() ?: 2).coerceAtLeast(1)
            if (parts.size >= 3) langOverride = parts[2]
        }
        return getRelatedRadio(
            seedVideoId = seedId,
            page = page,
            langOverride = langOverride,
            excludeIds = excludeIds,
        )
    }

    /**
     * Resolves a direct high-bitrate audio stream URL for a given JioSaavn song ID.
     */
    suspend fun resolveStreamById(songId: String): String? = withContext(Dispatchers.IO) {
        val cleanId = songId.removePrefix("saavn:")
        streamUrlCache[cleanId]?.let { return@withContext it }
        streamUrlCache[songId]?.let { return@withContext it }
        streamUrlCache["saavn:$cleanId"]?.let { return@withContext it }

        fetchSongDetails(cleanId)
        streamUrlCache[cleanId] ?: streamUrlCache[songId] ?: streamUrlCache["saavn:$cleanId"]
    }

    /**
     * Resolves a direct playable audio stream URL for a given song title and artist as fallback.
     */
    /**
     * Finds the best matching JioSaavn track for [title] + [artist], rejecting unrelated fuzzy hits
     * whose normalized title or artist tokens do not match.
     */
    suspend fun findBestSongMatch(
        title: String,
        artist: String? = null,
        expectedDurationSec: Int? = null,
    ): Track? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val cleanTitle = TrackMatcher.normalizeTitle(title).ifBlank { title.trim() }
        val cleanArtist = artist?.replace(Regex("""(?i)\b(topic|vevo)\b"""), "")?.trim()
        val query = if (!cleanArtist.isNullOrBlank()) "$cleanTitle $cleanArtist" else cleanTitle

        val candidates = ArrayList<SongsResult>()
        val primaryResults = runCatching { searchSongs(query) }.getOrDefault(emptyList())
        candidates.addAll(primaryResults)
        if (candidates.isEmpty() && query != cleanTitle) {
            candidates.addAll(runCatching { searchSongs(cleanTitle) }.getOrDefault(emptyList()))
        }

        var bestCandidate: SongsResult? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (cand in candidates.take(10)) {
            val candArtistStr = cand.artists?.joinToString(", ") { it.name }
            val score = TrackMatcher.scoreCandidate(
                expectedTitle = title,
                expectedArtist = cleanArtist,
                candidateTitle = cand.title.orEmpty(),
                candidateArtists = candArtistStr,
                expectedDurationSec = expectedDurationSec,
                candidateDurationSec = cand.durationSeconds,
            )
            if (score > bestScore) {
                bestScore = score
                bestCandidate = cand
            }
        }

        if (bestCandidate != null && TrackMatcher.isAcceptableMatch(bestScore)) {
            val rawId = bestCandidate.videoId.removePrefix("saavn:")
            trackCache[rawId] ?: trackCache[bestCandidate.videoId] ?: bestCandidate.toTrack()
        } else {
            null
        }
    }

    suspend fun resolveStream(
        title: String,
        artist: String? = null,
        expectedDurationSec: Int? = null,
    ): String? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val matched = findBestSongMatch(title, artist, expectedDurationSec)
        if (matched != null) {
            val saavnId = matched.videoId.removePrefix("saavn:")
            val stream = resolveStreamById(saavnId)
            if (!stream.isNullOrBlank()) {
                Logger.d(TAG, "Validated JioSaavn match for '$title' -> '${matched.title}' ($saavnId)")
                return@withContext stream
            }
        }
        Logger.d(TAG, "No verified JioSaavn match for '$title' (${artist ?: "unknown"}); deferring to YouTube")
        null
    }

    private suspend fun tryFromSaavnDirect(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&n=5&p=1&q=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject

                val moreInfo = firstSong?.get("more_info")?.jsonObject
                val encrypted = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
                    ?: firstSong?.get("encrypted_media_url")?.jsonPrimitive?.content
                if (!encrypted.isNullOrBlank()) {
                    val decrypted = decryptDes(encrypted)
                    if (!decrypted.isNullOrBlank()) {
                        val streamUrl = decrypted
                            .replace("_96.mp4", "_320.mp4")
                            .replace("_160.mp4", "_320.mp4")
                        Logger.w(TAG, "Resolved stream via JioSaavn DES decrypt: $streamUrl")
                        return streamUrl
                    }
                }

                val previewUrl = moreInfo?.get("media_preview_url")?.jsonPrimitive?.content
                    ?: firstSong?.get("media_preview_url")?.jsonPrimitive?.content
                if (!previewUrl.isNullOrBlank()) {
                    val streamUrl = previewUrl
                        .replace("preview.saavncdn.com", "aac.saavncdn.com")
                        .replace("_96_p.mp4", "_320.mp4")
                    Logger.w(TAG, "Resolved stream via direct JioSaavn preview synthesis: $streamUrl")
                    return streamUrl
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "jiosaavn.com direct resolution skipped: ${e.message}")
            null
        }
    }

    private suspend fun tryFromSaavnDev(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://saavn.dev/api/search/songs?query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonObject
                val results = data?.get("results")?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject
                val downloadUrls = firstSong?.get("downloadUrl")?.jsonArray
                val bestUrl = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?: downloadUrls?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                if (!bestUrl.isNullOrBlank()) {
                    Logger.w(TAG, "Resolved stream via saavn.dev: $bestUrl")
                    return bestUrl
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "saavn.dev resolution skipped: ${e.message}")
            null
        }
    }

    private suspend fun tryFromVercelApi(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://jiosaavn-api-privatetesting.vercel.app/search?query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: root["data"]?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject
                val mediaUrl = firstSong?.get("media_url")?.jsonPrimitive?.content
                    ?: firstSong?.get("download_url")?.jsonPrimitive?.content
                if (!mediaUrl.isNullOrBlank()) {
                    val highRes = mediaUrl.replace("_96.mp4", "_320.mp4")
                    Logger.w(TAG, "Resolved stream via vercel JioSaavn API: $highRes")
                    return highRes
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "vercel jiosaavn resolution skipped: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decryptDes(encryptedBase64: String): String? {
        return try {
            val encryptedBytes = Base64.decode(encryptedBase64.trim())
            val cipherClass = Class.forName("javax.crypto.Cipher")
            val secretKeySpecClass = Class.forName("javax.crypto.spec.SecretKeySpec")

            val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
            val keySpec = secretKeySpecClass.getConstructor(ByteArray::class.java, String::class.java)
                .newInstance(keyBytes, "DES")

            val getInstanceMethod = cipherClass.getMethod("getInstance", String::class.java)
            val cipher = getInstanceMethod.invoke(null, "DES/ECB/PKCS5Padding")

            val initMethod = cipher.javaClass.getMethod("init", Int::class.javaPrimitiveType, Class.forName("java.security.Key"))
            initMethod.invoke(cipher, 2 /* DECRYPT_MODE */, keySpec)

            val doFinalMethod = cipher.javaClass.getMethod("doFinal", ByteArray::class.java)
            val decryptedBytes = doFinalMethod.invoke(cipher, encryptedBytes) as ByteArray

            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            Logger.e(TAG, "DES decrypt failed: ${e.message}")
            null
        }
    }
}
