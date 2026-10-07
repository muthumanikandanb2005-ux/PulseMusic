package com.maxrave.data.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ParsedPlaylist(
    val title: String,
    val tracks: List<ParsedTrack>,
)

data class ParsedTrack(
    val query: String,
    val title: String? = null,
    val artist: String? = null,
    val videoId: String? = null,
)

object PlaylistFormatParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(rawContent: String, fallbackTitle: String = "Imported Playlist"): ParsedPlaylist? {
        val content = rawContent.trim().removePrefix("\uFEFF")
        if (content.isBlank()) return null

        // 1. Generic JSON Parser (non-Pulse or simple track list)
        if (content.startsWith("{") || content.startsWith("[")) {
            val parsedJson = runCatching { parseJson(content, fallbackTitle) }.getOrNull()
            if (parsedJson != null && parsedJson.tracks.isNotEmpty()) {
                return parsedJson
            }
        }

        // 2. M3U / M3U8 Playlist Parser
        if (content.contains("#EXTM3U") || content.contains("#EXTINF") || content.contains("#PLAYLIST:")) {
            val parsedM3u = parseM3u(content, fallbackTitle)
            if (parsedM3u.tracks.isNotEmpty()) {
                return parsedM3u
            }
        }

        // 3. CSV / TSV Playlist Parser (Spotify, Amazon Music, Gaana, etc.)
        val parsedCsv = parseCsv(content, fallbackTitle)
        if (parsedCsv != null && parsedCsv.tracks.isNotEmpty()) {
            return parsedCsv
        }

        // 4. Apple Music / iTunes XML Playlist Parser
        if (content.contains("<plist") || (content.contains("<key>Tracks</key>") && content.contains("<key>Name</key>"))) {
            val parsedXml = parseAppleMusicXml(content, fallbackTitle)
            if (parsedXml.tracks.isNotEmpty()) {
                return parsedXml
            }
        }

        // 5. Plain Text fallback (one song per line)
        val parsedText = parsePlainText(content, fallbackTitle)
        if (parsedText.tracks.isNotEmpty()) {
            return parsedText
        }

        return null
    }

    private fun parseAppleMusicXml(content: String, fallbackTitle: String): ParsedPlaylist {
        val tracks = mutableListOf<ParsedTrack>()
        val dictBlocks = content.split("<dict>")
        for (block in dictBlocks) {
            val nameMatch = Regex("<key>Name</key>\\s*<string>([^<]+)</string>", RegexOption.IGNORE_CASE).find(block)
            val artistMatch = Regex("<key>Artist</key>\\s*<string>([^<]+)</string>", RegexOption.IGNORE_CASE).find(block)
            if (nameMatch != null) {
                val title = nameMatch.groupValues[1].trim()
                val artist = artistMatch?.groupValues?.getOrNull(1)?.trim()
                val query = if (!artist.isNullOrBlank()) "$artist - $title" else title
                tracks.add(ParsedTrack(query = query, title = title, artist = artist))
            }
        }
        return ParsedPlaylist(title = fallbackTitle, tracks = tracks)
    }

    private fun parseJson(content: String, fallbackTitle: String): ParsedPlaylist? {
        val element = json.parseToJsonElement(content)
        var title = fallbackTitle
        val tracks = mutableListOf<ParsedTrack>()

        fun extractTrack(item: JsonObject): ParsedTrack? {
            val songTitle = item["title"]?.jsonPrimitive?.content
                ?: item["name"]?.jsonPrimitive?.content
                ?: item["trackName"]?.jsonPrimitive?.content
                ?: item["track"]?.jsonPrimitive?.content
                ?: return null

            val artist = item["artist"]?.jsonPrimitive?.content
                ?: item["artistName"]?.jsonPrimitive?.content
                ?: item["artists"]?.let { artistElem ->
                    if (artistElem is JsonArray) {
                        artistElem.mapNotNull { it.jsonPrimitive.content }.joinToString(", ")
                    } else {
                        artistElem.jsonPrimitive.content
                    }
                }

            val videoId = item["videoId"]?.jsonPrimitive?.content
                ?: item["id"]?.jsonPrimitive?.content
                ?: item["youtubeId"]?.jsonPrimitive?.content

            val query = if (!artist.isNullOrBlank()) "$artist - $songTitle" else songTitle
            return ParsedTrack(query = query, title = songTitle, artist = artist, videoId = videoId)
        }

        if (element is JsonObject) {
            title = element["name"]?.jsonPrimitive?.content
                ?: element["title"]?.jsonPrimitive?.content
                ?: element["playlist"]?.jsonPrimitive?.content
                ?: fallbackTitle

            val trackArray = element["tracks"] as? JsonArray
                ?: element["songs"] as? JsonArray
                ?: element["items"] as? JsonArray
                ?: element["data"] as? JsonArray

            trackArray?.forEach { trackElem ->
                if (trackElem is JsonObject) {
                    extractTrack(trackElem)?.let { tracks.add(it) }
                }
            }
        } else if (element is JsonArray) {
            element.forEach { trackElem ->
                if (trackElem is JsonObject) {
                    extractTrack(trackElem)?.let { tracks.add(it) }
                }
            }
        }

        return if (tracks.isNotEmpty()) ParsedPlaylist(title, tracks) else null
    }

    private fun parseM3u(content: String, fallbackTitle: String): ParsedPlaylist {
        var playlistTitle = fallbackTitle
        val tracks = mutableListOf<ParsedTrack>()
        val lines = content.lines()

        var pendingTitle: String? = null
        var pendingArtist: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            if (line.startsWith("#PLAYLIST:", ignoreCase = true)) {
                playlistTitle = line.substringAfter(":").trim()
            } else if (line.startsWith("#EXT-X-NAME:", ignoreCase = true)) {
                playlistTitle = line.substringAfter(":").trim()
            } else if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val info = line.substringAfter(":").substringAfter(",").trim()
                if (info.isNotBlank()) {
                    if (info.contains(" - ")) {
                        val parts = info.split(" - ", limit = 2)
                        pendingArtist = parts[0].trim()
                        pendingTitle = parts[1].trim()
                    } else {
                        pendingTitle = info
                        pendingArtist = null
                    }
                }
            } else if (!line.startsWith("#")) {
                // File path, URL, or song title line
                val title: String
                val artist: String?

                if (pendingTitle != null) {
                    title = pendingTitle
                    artist = pendingArtist
                } else {
                    // Extract from filename
                    val filename = line.substringAfterLast('/').substringAfterLast('\\')
                    val cleaned = filename
                        .replace(Regex("\\.(mp3|flac|wav|m4a|aac|ogg|opus|wma)$", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("^\\d+[\\.\\-_\\s]+"), "")
                        .trim()

                    if (cleaned.contains(" - ")) {
                        val parts = cleaned.split(" - ", limit = 2)
                        artist = parts[0].trim()
                        title = parts[1].trim()
                    } else {
                        title = cleaned
                        artist = null
                    }
                }

                if (title.isNotBlank()) {
                    val query = if (!artist.isNullOrBlank()) "$artist - $title" else title
                    tracks.add(ParsedTrack(query = query, title = title, artist = artist))
                }

                pendingTitle = null
                pendingArtist = null
            }
        }

        return ParsedPlaylist(title = playlistTitle, tracks = tracks)
    }

    private fun parseCsv(content: String, fallbackTitle: String): ParsedPlaylist? {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return null

        val firstLine = lines.first()
        val delimiter = when {
            firstLine.count { it == '\t' } >= 1 -> '\t'
            firstLine.count { it == ';' } >= 1 -> ';'
            firstLine.count { it == ',' } >= 1 -> ','
            else -> return null
        }

        val headers = splitCsvRow(firstLine, delimiter).map { it.trim().lowercase() }
        val titleIdx = headers.indexOfFirst {
            it.matches(Regex(".*(track[\\s_]?name|song[\\s_]?name|title|name).*"))
        }
        val artistIdx = headers.indexOfFirst {
            it.matches(Regex(".*(artist[\\s_]?name(\\([s\\)]+)?|artist|artists|performer).*"))
        }

        if (titleIdx == -1) return null

        val tracks = mutableListOf<ParsedTrack>()
        for (i in 1 until lines.size) {
            val row = splitCsvRow(lines[i], delimiter)
            val title = row.getOrNull(titleIdx)?.trim() ?: continue
            if (title.isBlank()) continue
            val artist = if (artistIdx != -1) row.getOrNull(artistIdx)?.trim() else null

            val query = if (!artist.isNullOrBlank()) "$artist - $title" else title
            tracks.add(ParsedTrack(query = query, title = title, artist = artist))
        }

        return if (tracks.isNotEmpty()) ParsedPlaylist(title = fallbackTitle, tracks = tracks) else null
    }

    private fun splitCsvRow(row: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in row) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    private fun parsePlainText(content: String, fallbackTitle: String): ParsedPlaylist {
        val tracks = mutableListOf<ParsedTrack>()
        for (rawLine in content.lines()) {
            var line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) continue

            // Strip track numbers e.g. "01. ", "1 - ", "[1] "
            line = line.replace(Regex("^(\\[?\\d+\\]?[\\.\\-_\\s]+)"), "").trim()
            if (line.isBlank()) continue

            val title: String
            val artist: String?
            when {
                line.contains(" - ") -> {
                    val parts = line.split(" - ", limit = 2)
                    artist = parts[0].trim()
                    title = parts[1].trim()
                }
                line.contains(" – ") -> {
                    val parts = line.split(" – ", limit = 2)
                    artist = parts[0].trim()
                    title = parts[1].trim()
                }
                line.contains(" by ", ignoreCase = true) -> {
                    val parts = line.split(Regex(" by ", RegexOption.IGNORE_CASE), limit = 2)
                    title = parts[0].trim()
                    artist = parts[1].trim()
                }
                else -> {
                    title = line
                    artist = null
                }
            }

            if (title.isNotBlank()) {
                val query = if (!artist.isNullOrBlank()) "$artist - $title" else title
                tracks.add(ParsedTrack(query = query, title = title, artist = artist))
            }
        }
        return ParsedPlaylist(title = fallbackTitle, tracks = tracks)
    }
}
