package org.simpmusic.lyrics.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiniLyricsSearchResponse(
    @SerialName("results")
    val results: List<BiniLyricsItem> = emptyList(),
    @SerialName("total")
    val total: Int = 0,
    @SerialName("source")
    val source: String? = null,
)

@Serializable
data class BiniLyricsItem(
    @SerialName("id")
    val id: String? = null,
    @SerialName("track_name")
    val trackName: String? = null,
    @SerialName("artist_name")
    val artistName: String? = null,
    @SerialName("album_name")
    val albumName: String? = null,
    @SerialName("duration")
    val duration: Int? = null,
    @SerialName("isrc")
    val isrc: String? = null,
    @SerialName("timing_type")
    val timingType: String? = null,
    @SerialName("lyricsUrl")
    val lyricsUrl: String? = null,
)
