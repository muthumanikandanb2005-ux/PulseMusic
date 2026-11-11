package com.manikandan.domain.data.model.searchResult

import com.manikandan.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)