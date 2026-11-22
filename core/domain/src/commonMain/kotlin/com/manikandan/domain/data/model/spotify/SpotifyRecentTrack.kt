package com.manikandan.domain.data.model.spotify

data class SpotifyRecentTrack(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val playedAt: String? = null,
)
