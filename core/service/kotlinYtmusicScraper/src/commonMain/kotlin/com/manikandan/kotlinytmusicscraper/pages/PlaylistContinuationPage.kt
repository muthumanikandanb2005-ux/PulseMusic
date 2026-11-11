package com.manikandan.kotlinytmusicscraper.pages

import com.manikandan.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)