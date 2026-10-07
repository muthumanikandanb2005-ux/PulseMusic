package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable
import org.simpmusic.lyrics.domain.Lyrics

@Serializable
data class SpotifyLyricsApiResponse(
    val error: Boolean = false,
    val message: String? = null,
    val syncType: String? = "LINE_SYNCED",
    val lines: List<SpotifyLyricsApiLine>? = null,
) {
    @Serializable
    data class SpotifyLyricsApiLine(
        val startTimeMs: String? = null,
        val endTimeMs: String? = null,
        val words: String? = null,
        val timeTag: String? = null,
        val syllables: List<String>? = null,
    )
}

/**
 * Parses [mm:ss.xx] or [mm:ss] time tags to milliseconds.
 */
private fun parseTimeTagToMs(timeTag: String?): Long {
    if (timeTag.isNullOrBlank()) return 0L
    val cleaned = timeTag.trim().removePrefix("[").removeSuffix("]")
    val parts = cleaned.split(":")
    if (parts.size >= 2) {
        val minutes = parts[0].toLongOrNull() ?: 0L
        val secondsParts = parts[1].split(".")
        val seconds = secondsParts[0].toLongOrNull() ?: 0L
        val ms = if (secondsParts.size > 1) {
            val frac = secondsParts[1]
            if (frac.length == 2) (frac.toLongOrNull() ?: 0L) * 10L
            else frac.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        } else 0L
        return minutes * 60_000L + seconds * 1000L + ms
    }
    return 0L
}

fun SpotifyLyricsApiResponse.toLibraryLyrics(): Lyrics {
    val srcLines = this.lines ?: emptyList()
    val parsedLines = ArrayList<Lyrics.LyricsX.Line>()

    for (i in srcLines.indices) {
        val cur = srcLines[i]
        val startMs = if (!cur.startTimeMs.isNullOrBlank()) {
            cur.startTimeMs.toLongOrNull() ?: 0L
        } else {
            parseTimeTagToMs(cur.timeTag)
        }

        val nextStartMs: Long? = if (i + 1 < srcLines.size) {
            val next = srcLines[i + 1]
            if (!next.startTimeMs.isNullOrBlank()) {
                next.startTimeMs.toLongOrNull()
            } else {
                parseTimeTagToMs(next.timeTag)
            }
        } else null

        val endMs = if (!cur.endTimeMs.isNullOrBlank() && cur.endTimeMs != "0") {
            cur.endTimeMs
        } else if (nextStartMs != null && nextStartMs > startMs) {
            nextStartMs.toString()
        } else {
            (startMs + 4000L).toString()
        }

        parsedLines.add(
            Lyrics.LyricsX.Line(
                startTimeMs = startMs.toString(),
                endTimeMs = endMs,
                words = cur.words.orEmpty(),
                syllables = cur.syllables ?: emptyList(),
            ),
        )
    }

    return Lyrics(
        lyrics = Lyrics.LyricsX(
            lines = parsedLines,
            syncType = this.syncType ?: if (parsedLines.isNotEmpty()) "LINE_SYNCED" else "UNSYNCED",
        ),
    )
}
