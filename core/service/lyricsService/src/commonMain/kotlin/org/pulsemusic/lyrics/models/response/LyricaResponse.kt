package org.pulsemusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class LyricaTimedLine(
    val id: String? = null,
    val text: String? = null,
    val start_time: Long = 0L,
    val end_time: Long = 0L,
    val translated: String? = null,
    val romanized: String? = null,
)

@Serializable
data class LyricaData(
    val source: String? = null,
    val artist: String? = null,
    val title: String? = null,
    val hasTimestamps: Boolean = false,
    val lyrics: String? = null,
    val timed_lyrics: List<LyricaTimedLine>? = null,
    val translated_lyrics: String? = null,
    val romanized_lyrics: String? = null,
)

@Serializable
data class LyricaResponse(
    val status: String? = null,
    val data: LyricaData? = null,
)
