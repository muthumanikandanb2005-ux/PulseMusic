package com.manikandan.kotlinytmusicscraper.pages

import com.manikandan.kotlinytmusicscraper.models.AlbumItem
import com.manikandan.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<AlbumItem>,
    val musicVideo: List<VideoItem>,
)