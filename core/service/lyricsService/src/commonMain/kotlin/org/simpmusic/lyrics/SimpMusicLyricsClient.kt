package org.simpmusic.lyrics

import com.maxrave.ktorext.crypto.Hmac
import com.maxrave.ktorext.crypto.HmacUri
import com.maxrave.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.simpmusic.lyrics.am.AMAlbumResource
import org.simpmusic.lyrics.am.AMSongWithAlbum
import org.simpmusic.lyrics.am.parseAMHlsVariants
import org.simpmusic.lyrics.am.pickAMRendition
import org.simpmusic.lyrics.am.AMArtistResource
import org.simpmusic.lyrics.am.AMSearchResponse
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import org.simpmusic.lyrics.models.response.BaseResponse
import org.simpmusic.lyrics.models.response.BetterLyricsResponse
import org.simpmusic.lyrics.models.response.BiniLyricsSearchResponse
import org.simpmusic.lyrics.models.response.LrclibObject
import org.simpmusic.lyrics.models.response.LyricsResponse
import org.simpmusic.lyrics.models.response.SpotifyLyricsApiResponse
import org.simpmusic.lyrics.models.response.toLibraryLyrics
import org.simpmusic.lyrics.models.response.TranslatedLyricsResponse
import org.simpmusic.lyrics.parser.parseSyncedLyrics
import org.simpmusic.lyrics.parser.parseUnsyncedLyrics
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.abs

private const val TAG = "SimpMusicLyricsClient"

class SimpMusicLyricsClient {
    private val algorithm = ""

    private val hmacService = Hmac("HmacSHA256", "simpmusic-lyrics")
    private val lyricsService = SimpMusicLyrics()
    private var insertingLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingLyrics: Boolean
        get() = insertingLyrics.second

    private var tooManyRequest: Boolean = false

    private var insertingTranslatedLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingTranslatedLyrics: Boolean
        get() = insertingTranslatedLyrics.second

    suspend fun getLyrics(videoId: String): Result<List<LyricsResponse>> =
        runCatching {
            lyricsService.findLyricsByVideoId(videoId).bodyOrThrow<List<LyricsResponse>>()
        }

    suspend fun getTranslatedLyrics(
        videoId: String,
        language: String,
    ): Result<TranslatedLyricsResponse> =
        runCatching {
            if (language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            lyricsService.findTranslatedLyrics(videoId, language).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun insertLyrics(lyricsBody: LyricsBody): Result<LyricsResponse> =
        runCatching {
            if (tooManyRequest) {
                throw IllegalStateException("Too many requests, please wait before trying again.")
            }
            if (isInsertingLyrics && insertingLyrics.first == lyricsBody.videoId) {
                throw IllegalStateException("Already inserting lyrics, please wait until the current operation is complete.")
            }
            insertingLyrics = lyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.BASE_HMAC_URI,
                )
            lyricsService.insertLyrics(lyricsBody, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun insertTranslatedLyrics(translatedLyricsBody: TranslatedLyricsBody): Result<TranslatedLyricsResponse> =
        runCatching {
            if (translatedLyricsBody.language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            if (isInsertingTranslatedLyrics && insertingTranslatedLyrics.first == translatedLyricsBody.videoId) {
                throw IllegalStateException("Already inserting translated lyrics, please wait until the current operation is complete.")
            }
            insertingTranslatedLyrics = translatedLyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.TRANSLATED_HMAC_URI,
                )
            lyricsService.insertTranslatedLyrics(translatedLyricsBody, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun voteLyrics(
        lyricsId: String,
        upvote: Boolean,
    ): Result<LyricsResponse> =
        runCatching {
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_HMAC_URI,
                )
            lyricsService.voteLyrics(lyricsId, upvote, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun voteTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Result<TranslatedLyricsResponse> =
        runCatching {
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_TRANSLATED_HMAC_URI,
                )
            lyricsService.voteTranslatedLyrics(translatedLyricsId, upvote, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
        duration: Int?,
        clientId: String = "Q577SzaQFl01aTiReKY9NKgkcENPwiaHVceaOk4p8JQ",
    ) = runCatching {
        // 1. Try exact lookup with duration
        var exactObj: LrclibObject? = try {
            val resp =
                lyricsService.getLrclibLyricsExact(
                    trackName = q_track,
                    artistName = q_artist,
                    duration = duration,
                    clientId = clientId,
                )
            if (resp.status.value in 200..299) {
                resp.body<LrclibObject>()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        // 2. If exact with duration failed, try exact WITHOUT duration (handles YouTube vs Album length drifts)
        if (exactObj == null && duration != null && duration > 0) {
            exactObj = try {
                val resp =
                    lyricsService.getLrclibLyricsExact(
                        trackName = q_track,
                        artistName = q_artist,
                        duration = null,
                        clientId = clientId,
                    )
                if (resp.status.value in 200..299) {
                    resp.body<LrclibObject>()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        // 3. Fallback to search endpoint if exact lookup wasn't found
        val lrclibObject: LrclibObject? =
            if (exactObj != null) {
                exactObj
            } else {
                val rs =
                    try {
                        lyricsService
                            .searchLrclibLyrics(
                                q_track = q_track,
                                q_artist = q_artist,
                                clientId = clientId,
                            ).body<List<LrclibObject>>()
                    } catch (_: Exception) {
                        emptyList()
                    }
                val candidates = rs.filter { !it.instrumental && (!it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()) }
                val matched = if (duration != null && duration > 0) {
                    candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        val aName = it.artistName.orEmpty()
                        isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName) && abs(it.duration.toInt() - duration) <= 15
                    } ?: candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        val aName = it.artistName.orEmpty()
                        isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName) && abs(it.duration.toInt() - duration) <= 30
                    } ?: candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        val aName = it.artistName.orEmpty()
                        isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName)
                    } ?: candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        isTrackExactMatch(q_track, tName) && abs(it.duration.toInt() - duration) <= 15
                    }
                } else {
                    candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        val aName = it.artistName.orEmpty()
                        isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName)
                    } ?: candidates.find {
                        val tName = (it.trackName ?: it.name).orEmpty()
                        isTrackExactMatch(q_track, tName)
                    }
                }
                matched
            }
        if (lrclibObject != null) {
            val syncedLyrics = lrclibObject.syncedLyrics
            val plainLyrics = lrclibObject.plainLyrics
            if (!syncedLyrics.isNullOrEmpty()) {
                parseSyncedLyrics(syncedLyrics)
            } else if (!plainLyrics.isNullOrEmpty()) {
                parseUnsyncedLyrics(plainLyrics)
            } else {
                null
            }
        } else {
            null
        }
    }


    suspend fun searchBetterLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int?,
    ) = runCatching {
        val rs =
            lyricsService
                .searchBetterLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                    durationSeconds = durationSeconds,
                ).body<BetterLyricsResponse>()
        rs.ttml
    }

    suspend fun searchBiniLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int? = null,
        isrc: String? = null,
    ): Result<org.simpmusic.lyrics.domain.Lyrics?> =
        runCatching {
            val response = lyricsService.searchBiniLyrics(
                track = q_track,
                artist = q_artist,
                duration = durationSeconds,
                isrc = isrc,
            )
            if (response.status.value !in 200..299) {
                throw Exception("BiniLyrics API error: ${response.status.value}")
            }
            val searchRes = response.body<BiniLyricsSearchResponse>()
            val candidates = searchRes.results.filter { !it.lyricsUrl.isNullOrBlank() }
            if (candidates.isEmpty()) return@runCatching null

            val matched = if (!isrc.isNullOrBlank()) {
                candidates.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) } ?: candidates.firstOrNull()
            } else if (durationSeconds != null && durationSeconds > 0) {
                candidates.find {
                    val tName = it.trackName.orEmpty()
                    val aName = it.artistName.orEmpty()
                    val d = it.duration ?: 0
                    isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName) && abs(d - durationSeconds) <= 15
                } ?: candidates.find {
                    val tName = it.trackName.orEmpty()
                    val aName = it.artistName.orEmpty()
                    val d = it.duration ?: 0
                    isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName) && abs(d - durationSeconds) <= 30
                } ?: candidates.find {
                    val tName = it.trackName.orEmpty()
                    val aName = it.artistName.orEmpty()
                    isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName)
                } ?: candidates.firstOrNull()
            } else {
                candidates.find {
                    val tName = it.trackName.orEmpty()
                    val aName = it.artistName.orEmpty()
                    isTrackMatch(q_track, tName) && isArtistMatch(q_artist, aName)
                } ?: candidates.firstOrNull()
            }

            val lyricsUrl = matched?.lyricsUrl ?: return@runCatching null
            val rawResp = lyricsService.fetchRawLyricsFromUrl(lyricsUrl)
            if (rawResp.status.value !in 200..299) return@runCatching null
            val rawText = rawResp.bodyAsText()
            if (rawText.isBlank()) return@runCatching null

            if (lyricsUrl.endsWith(".ttml", ignoreCase = true) || rawText.contains("<tt") || rawText.contains("<p")) {
                org.simpmusic.lyrics.parser.parseTtmlLyrics(rawText)
            } else {
                parseSyncedLyrics(rawText)
            }
        }

    suspend fun getSpotifyLyricsFromApi(
        endpoint: String,
        trackId: String? = null,
        spotifyUrl: String? = null,
        query: String? = null,
    ): Result<org.simpmusic.lyrics.domain.Lyrics> =
        runCatching {
            if (endpoint.isBlank()) throw IllegalArgumentException("Spotify Lyrics API endpoint is blank")
            val response =
                lyricsService.getSpotifyLyricsFromApi(
                    endpoint = endpoint,
                    trackId = trackId,
                    spotifyUrl = spotifyUrl,
                    query = query,
                )
            if (response.status.value !in 200..299) {
                throw Exception("Spotify lyrics API error: ${response.status.value}")
            }
            val body = response.body<SpotifyLyricsApiResponse>()
            if (body.error || body.lines.isNullOrEmpty()) {
                throw Exception(body.message ?: "No lyrics returned from Spotify Lyrics API")
            }
            body.toLibraryLyrics()
        }

    suspend fun searchAMArtist(
        name: String,
        limit: Int = 5,
    ): Result<List<AMArtistResource>> =
        runCatching {
            val response = lyricsService.searchAMArtist(name, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val resources = parsed.resources?.artists.orEmpty()
            // Keep the search ranking from results.data; fall back to the resources map order.
            parsed.results
                ?.artists
                ?.data
                ?.mapNotNull { resources[it.id] }
                ?: resources.values.toList()
        }

    /**
     * Fetch a single artist by id, including [AMEditorialArtwork] (name-logo PNG) and keyColor.
     * Returns null when the id is not present in the response.
     */
    suspend fun getAMArtist(id: String): Result<AMArtistResource?> =
        runCatching {
            val response = lyricsService.getAMArtist(id)
            if (response.status.value !in 200..299) {
                throw Exception("AM artist fetch failed: ${response.status.value}")
            }
            response
                .body<AMSearchResponse>()
                .resources
                ?.artists
                ?.get(id)
        }

    /**
     * Search the AM catalog for albums, ranked the way AM ranked them. Each result already carries
     * its animated artwork when it has one, so the caller needs no follow-up request.
     */
    suspend fun searchAMAlbum(
        query: String,
        limit: Int = 5,
    ): Result<List<AMAlbumResource>> =
        runCatching {
            val response = lyricsService.searchAMAlbum(query, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM album search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val resources = parsed.resources?.albums.orEmpty()
            // Keep the search ranking from results.data; fall back to the resources map order.
            parsed.results
                ?.albums
                ?.data
                ?.mapNotNull { resources[it.id] }
                ?: resources.values.toList()
        }

    /**
     * Tracks reached by title, each paired with the album it appears on.
     *
     * The pairing comes from `relationships.albums`, never from the flat `resources.albums` map:
     * that map holds every album any hit belongs to, so reading an album out of it directly
     * attaches whichever release happens to carry artwork to whatever track was searched for.
     * Order follows the search ranking; the caller still decides which hit is the right track.
     */
    suspend fun searchAMSongsWithAlbums(
        query: String,
        limit: Int = 5,
    ): Result<List<AMSongWithAlbum>> =
        runCatching {
            val response = lyricsService.searchAMSongs(query, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM song search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val songs = parsed.resources?.songs.orEmpty()
            val albums = parsed.resources?.albums.orEmpty()
            val ranked =
                parsed.results
                    ?.songs
                    ?.data
                    ?.mapNotNull { songs[it.id] }
                    ?: songs.values.toList()
            ranked.mapNotNull { song ->
                val albumId =
                    song.relationships
                        ?.albums
                        ?.data
                        ?.firstOrNull()
                        ?.id
                val album = albumId?.let { albums[it] } ?: return@mapNotNull null
                AMSongWithAlbum(
                    songName = song.attributes?.name,
                    artistName = song.attributes?.artistName,
                    durationInMillis = song.attributes?.durationInMillis,
                    album = album,
                )
            }
        }

    /**
     * Resolve an animated-artwork master playlist down to the single rendition worth playing, and
     * return its media-playlist url.
     *
     * Doing this here rather than leaving it to the player is what makes the two platforms agree:
     * mpv defaults to `--hls-bitrate=max` and would take the top of the ladder — on a measured
     * artwork that is 2048x2732 10-bit HEVC at 20 Mbps, 52 MB for a 22-second loop — while
     * ExoPlayer would pick by its own bandwidth estimate. Handing both a chosen rendition removes
     * the question. Returns null when the url is not a master playlist.
     */
    suspend fun selectAMRendition(
        masterUrl: String,
        minWidth: Int,
    ): Result<String?> =
        runCatching {
            val response = lyricsService.fetchAMHlsPlaylist(masterUrl)
            if (response.status.value !in 200..299) {
                throw Exception("AM playlist fetch failed: ${response.status.value}")
            }
            parseAMHlsVariants(response.bodyAsText(), masterUrl)
                .pickAMRendition(minWidth)
                ?.uri
        }

    suspend fun searchMusixMatchLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int?,
    ): Result<org.simpmusic.lyrics.domain.Lyrics?> =
        runCatching {
            val raw = lyricsService.searchMusixMatchLyrics(q_track, q_artist, durationSeconds) ?: return@runCatching null
            if (raw.trim().startsWith("[")) {
                try {
                    val jsonElem = Json.parseToJsonElement(raw)
                    if (jsonElem is JsonArray) {
                        val sb = StringBuilder()
                        for (item in jsonElem) {
                            val timeObj = item.jsonObject["time"]?.jsonObject
                            val min = timeObj?.get("minutes")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                            val sec = timeObj?.get("seconds")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                            val hundredths = timeObj?.get("hundredths")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                            val text = item.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.ifBlank { "♪" } ?: "♪"
                            val minStr = min.toString().padStart(2, '0')
                            val secStr = sec.toString().padStart(2, '0')
                            val hunStr = hundredths.toString().padStart(2, '0')
                            sb.append("[$minStr:$secStr.$hunStr]$text\n")
                        }
                        val lrcStr = sb.toString()
                        if (lrcStr.isNotBlank()) {
                            return@runCatching parseSyncedLyrics(lrcStr)
                        }
                    }
                } catch (_: Exception) {}
            }
            if (raw.contains(Regex("""\[\d+:\d+"""))) {
                parseSyncedLyrics(raw)
            } else {
                parseUnsyncedLyrics(raw)
            }
        }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (this.status.value == 429) {
            tooManyRequest = true
            Logger.e(TAG, "Too many requests: ${this.status.value}")
        } else {
            tooManyRequest = false
        }
        try {
            val data = body<BaseResponse<T>>()
            if (data.error != null) {
                val error = data.error
                Logger.e(TAG, "Error response: ${error.reason} (code: ${error.code})")
                throw Exception("Error response: ${error.reason} (code: ${error.code})")
            }
            return data.data ?: throw Exception("Response data is null")
        } catch (e: Exception) {
            throw e
        }
    }

    internal fun cleanTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\s*[\(\[][^\)\]]*[\)\]]"""), " ")
            .replace(Regex("""\s*-\s*(official|audio|video|lyric|lyrics|remaster|remastered|visualizer|live|acoustic|instrumental).*$""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    internal fun isTrackExactMatch(queryTrack: String, candidateTrack: String): Boolean {
        val qClean = cleanTitle(queryTrack)
        val cClean = cleanTitle(candidateTrack)
        if (qClean.isBlank() || cClean.isBlank()) return false
        return qClean == cClean
    }

    internal fun isTrackMatch(queryTrack: String, candidateTrack: String): Boolean {
        val qClean = cleanTitle(queryTrack)
        val cClean = cleanTitle(candidateTrack)
        if (qClean.isBlank() || cClean.isBlank()) return false
        if (qClean == cClean) return true

        val qTokens = qClean.split(" ").filter { it.isNotBlank() }
        val cTokens = cClean.split(" ").filter { it.isNotBlank() }
        if (qTokens.isEmpty() || cTokens.isEmpty()) return false

        // For single-word titles (e.g. "Stay", "Run", "One"), require exact match to prevent
        // matching unrelated songs that share a single common word prefix (e.g. "Runaway", "Someone").
        if (qTokens.size == 1 || cTokens.size == 1) {
            return qClean == cClean
        }

        // For multi-word titles, allow prefix matching if the title overlap is high (>= 70%)
        // and starts at a word boundary.
        val shorter = if (qClean.length <= cClean.length) qClean else cClean
        val longer = if (qClean.length > cClean.length) qClean else cClean
        val ratio = shorter.length.toDouble() / longer.length.toDouble()
        if (ratio >= 0.70 && longer.startsWith(shorter)) {
            return true
        }

        return false
    }

    internal fun isArtistMatch(queryArtist: String, candidateArtist: String): Boolean {
        val q = queryArtist.lowercase().replace(Regex("""[^\p{L}\p{N}\s]"""), " ").replace(Regex("""\s+"""), " ").trim()
        val c = candidateArtist.lowercase().replace(Regex("""[^\p{L}\p{N}\s]"""), " ").replace(Regex("""\s+"""), " ").trim()
        if (q.isBlank() || c.isBlank()) return true
        if (q == c) return true
        if (q.contains(c) || c.contains(q)) return true
        val qWords = q.split(" ").filter { it.length >= 3 }
        val cWords = c.split(" ").filter { it.length >= 3 }
        return qWords.any { it in cWords }
    }
}