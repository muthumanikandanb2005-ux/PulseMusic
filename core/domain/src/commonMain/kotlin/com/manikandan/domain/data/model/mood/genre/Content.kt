package com.manikandan.domain.data.model.mood.genre

import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val thumbnail: List<Thumbnail>?,
    val title: Title,
) : HomeContentType