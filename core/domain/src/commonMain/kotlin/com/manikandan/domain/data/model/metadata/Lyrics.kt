package com.manikandan.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val error: Boolean = false,
    val lines: List<Line>?,
    val syncType: String?,
    val pulseMusicLyrics: PulseMusicLyrics? = null,
)

@Serializable
data class PulseMusicLyrics(
    val id: String,
    val vote: Int,
)