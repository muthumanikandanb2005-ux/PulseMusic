package com.manikandan.data.service

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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * High-performance backend music and playlist provider for JioSaavn.
 * Provides invisible backend search, playlist fetching, and high-res (320kbps) audio stream
 * resolution without exposing any external branding in the UI.
 */
object JioSaavnService {
    private const val TAG = "JioSaavnService"

    private val client by lazy {
        HttpClient(getEngine()) {
            expectSuccess = false
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Memory cache for stream URLs and Track metadata to allow instant playback
    private val streamUrlCache = mutableMapOf<String, String>()
    private val trackCache = mutableMapOf<String, Track>()

    fun getCachedTrack(videoId: String): Track? {
        val cleanId = videoId.removePrefix("saavn:")
        return trackCache[cleanId] ?: trackCache[videoId]
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

                    val songResult = SongsResult(
                        album = Album(name = album, id = ""),
                        artists = artists,
                        category = "SONG",
                        duration = null,
                        durationSeconds = null,
                        feedbackTokens = null,
                        isExplicit = false,
                        resultType = "Song",
                        thumbnails = thumbnails,
                        title = title,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        year = "",
                    )
                    songs.add(songResult)
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

    private suspend fun searchSongsDirect(query: String): List<SongsResult> {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&n=20&p=1&q=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: return emptyList()

                results.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val rawTitle = obj["title"]?.jsonPrimitive?.content
                        ?: obj["song"]?.jsonPrimitive?.content
                        ?: return@mapNotNull null
                    val title = rawTitle.unescapeHtml()

                    val moreInfo = obj["more_info"]?.jsonObject
                    val albumName = (moreInfo?.get("album")?.jsonPrimitive?.content
                        ?: obj["album"]?.jsonPrimitive?.content)?.unescapeHtml()

                    val durationSec = moreInfo?.get("duration")?.jsonPrimitive?.content?.toIntOrNull()
                        ?: obj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
                    val durationStr = if (durationSec != null) {
                        "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}"
                    } else null

                    val rawArtist = obj["subtitle"]?.jsonPrimitive?.content?.unescapeHtml()
                        ?: moreInfo?.get("music")?.jsonPrimitive?.content?.unescapeHtml()
                        ?: obj["singers"]?.jsonPrimitive?.content?.unescapeHtml()

                    val artists = if (!rawArtist.isNullOrBlank()) {
                        rawArtist.split(",").map { Artist(name = it.trim(), id = null) }
                    } else emptyList()

                    val rawImg = obj["image"]?.jsonPrimitive?.content ?: ""
                    val highResImg = rawImg.replace("150x150", "500x500").replace("50x50", "500x500")
                    val thumbnails = if (highResImg.isNotBlank()) {
                        listOf(Thumbnail(url = highResImg, width = 500, height = 500))
                    } else emptyList()

                    val encryptedUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
                        ?: obj["encrypted_media_url"]?.jsonPrimitive?.content
                    if (!encryptedUrl.isNullOrBlank()) {
                        val decrypted = decryptDes(encryptedUrl)
                        if (!decrypted.isNullOrBlank()) {
                            val stream320 = decrypted.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
                            streamUrlCache[id] = stream320
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

                    SongsResult(
                        album = Album(name = albumName ?: "", id = ""),
                        artists = artists,
                        category = "SONG",
                        duration = durationStr,
                        durationSeconds = durationSec,
                        feedbackTokens = null,
                        isExplicit = false,
                        resultType = "Song",
                        thumbnails = thumbnails,
                        title = title,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        year = obj["year"]?.jsonPrimitive?.content ?: "",
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Logger.d(TAG, "searchSongsDirect error: ${e.message}")
            emptyList()
        }
    }

    private suspend fun searchSongsSaavnDev(query: String): List<SongsResult> {
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
                val results = data?.get("results")?.jsonArray ?: return emptyList()

                results.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = (obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "").unescapeHtml()
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

                    val downloadUrls = obj["downloadUrl"]?.jsonArray
                    val bestAudio = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    if (!bestAudio.isNullOrBlank()) {
                        streamUrlCache[id] = bestAudio
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

                    SongsResult(
                        album = Album(name = albumName ?: "", id = ""),
                        artists = artists,
                        category = "SONG",
                        duration = durationStr,
                        durationSeconds = durationSec,
                        feedbackTokens = null,
                        isExplicit = false,
                        resultType = "Song",
                        thumbnails = thumbnails,
                        title = title,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        year = obj["year"]?.jsonPrimitive?.content ?: "",
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            Logger.d(TAG, "searchSongsSaavnDev error: ${e.message}")
            emptyList()
        }
    }

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
                val tracks = list.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val songTitle = (obj["title"]?.jsonPrimitive?.content ?: obj["song"]?.jsonPrimitive?.content ?: "").unescapeHtml()

                    val moreInfo = obj["more_info"]?.jsonObject
                    val albumName = (moreInfo?.get("album")?.jsonPrimitive?.content ?: obj["album"]?.jsonPrimitive?.content)?.unescapeHtml()
                    val durationSec = moreInfo?.get("duration")?.jsonPrimitive?.content?.toIntOrNull()
                        ?: obj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
                    val durationStr = if (durationSec != null) {
                        "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}"
                    } else null

                    val rawArtist = obj["subtitle"]?.jsonPrimitive?.content?.unescapeHtml()
                        ?: moreInfo?.get("music")?.jsonPrimitive?.content?.unescapeHtml()
                        ?: obj["singers"]?.jsonPrimitive?.content?.unescapeHtml()

                    val artists = if (!rawArtist.isNullOrBlank()) {
                        rawArtist.split(",").map { Artist(name = it.trim(), id = null) }
                    } else emptyList()

                    val songImg = obj["image"]?.jsonPrimitive?.content ?: ""
                    val songHighResImg = songImg.replace("150x150", "500x500").replace("50x50", "500x500")
                    val songThumbs = if (songHighResImg.isNotBlank()) listOf(Thumbnail(url = songHighResImg, width = 500, height = 500)) else emptyList()

                    val encUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
                        ?: obj["encrypted_media_url"]?.jsonPrimitive?.content
                    if (!encUrl.isNullOrBlank()) {
                        val dec = decryptDes(encUrl)
                        if (!dec.isNullOrBlank()) {
                            streamUrlCache[id] = dec.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
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
                        thumbnails = songThumbs,
                        title = songTitle,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        category = "SONG",
                        feedbackTokens = null,
                        resultType = "Song",
                        year = obj["year"]?.jsonPrimitive?.content ?: "",
                    )
                    trackCache[id] = track
                    trackCache["saavn:$id"] = track
                    track
                }

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
                val tracks = songs.mapNotNull { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val songTitle = (obj["name"]?.jsonPrimitive?.content ?: "").unescapeHtml()
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

                    val sImages = obj["image"]?.jsonArray
                    val sBestImg = sImages?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                        ?: sImages?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                    val songThumbs = if (sBestImg.isNotBlank()) listOf(Thumbnail(url = sBestImg, width = 500, height = 500)) else emptyList()

                    val downloadUrls = obj["downloadUrl"]?.jsonArray
                    val bestAudio = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    if (!bestAudio.isNullOrBlank()) {
                        streamUrlCache[id] = bestAudio
                    }

                    val track = Track(
                        album = Album(name = albumName ?: "", id = ""),
                        artists = artists,
                        duration = durationStr,
                        durationSeconds = durationSec,
                        isAvailable = true,
                        isExplicit = false,
                        likeStatus = null,
                        thumbnails = songThumbs,
                        title = songTitle,
                        videoId = "saavn:$id",
                        videoType = "Song",
                        category = "SONG",
                        feedbackTokens = null,
                        resultType = "Song",
                        year = obj["year"]?.jsonPrimitive?.content ?: "",
                    )
                    trackCache[id] = track
                    trackCache["saavn:$id"] = track
                    track
                }

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
     * Resolves a direct high-bitrate audio stream URL for a given JioSaavn song ID.
     */
    suspend fun resolveStreamById(songId: String): String? = withContext(Dispatchers.IO) {
        val cleanId = songId.removePrefix("saavn:")
        streamUrlCache[cleanId]?.let { return@withContext it }
        streamUrlCache[songId]?.let { return@withContext it }
        streamUrlCache["saavn:$cleanId"]?.let { return@withContext it }

        try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$cleanId&_format=json&_marker=0&api_version=4"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val song = root[cleanId]?.jsonObject
                val moreInfo = song?.get("more_info")?.jsonObject
                val encrypted = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
                    ?: song?.get("encrypted_media_url")?.jsonPrimitive?.content
                if (!encrypted.isNullOrBlank()) {
                    val decrypted = decryptDes(encrypted)
                    if (!decrypted.isNullOrBlank()) {
                        val stream320 = decrypted.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
                        streamUrlCache[cleanId] = stream320
                        streamUrlCache[songId] = stream320
                        streamUrlCache["saavn:$cleanId"] = stream320
                        return@withContext stream320
                    }
                }
            }
        } catch (e: Exception) {
            Logger.d(TAG, "resolveStreamById error: ${e.message}")
        }
        null
    }

    /**
     * Resolves a direct playable audio stream URL for a given song title and artist as fallback.
     */
    suspend fun resolveStream(title: String, artist: String? = null): String? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val cleanTitle = title
            .replace(Regex("""(?i)\(?(feat\.|ft\.|official|video|audio|lyric|remix|version)\)?.*"""), "")
            .trim()
        val cleanArtist = artist?.replace(Regex("""(?i)\b(topic|vevo)\b"""), "")?.trim()
        val query = if (!cleanArtist.isNullOrBlank()) "$cleanTitle $cleanArtist" else cleanTitle

        Logger.d(TAG, "Resolving fallback audio stream from JioSaavn for: $query")

        tryFromSaavnDirect(query)
            ?: tryFromSaavnDev(query)
            ?: tryFromVercelApi(query)
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
