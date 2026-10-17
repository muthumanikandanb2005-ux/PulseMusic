package com.manikandan.domain.data.model.home.chart

import com.manikandan.domain.data.model.browse.artist.ResultPlaylist

data class ChartItemPlaylist(
    val title: String,
    val playlists: List<ResultPlaylist>,
)