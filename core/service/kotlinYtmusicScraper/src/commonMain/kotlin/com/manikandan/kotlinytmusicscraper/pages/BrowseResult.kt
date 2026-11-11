package com.manikandan.kotlinytmusicscraper.pages

import com.manikandan.kotlinytmusicscraper.models.YTItem

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )
}