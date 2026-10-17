package com.manikandan.domain.data.model.browse.artist

import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.data.type.HomeContentType

data class ResultPlaylist(
    val id: String,
    val author: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
) : HomeContentType