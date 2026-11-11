package com.manikandan.kotlinytmusicscraper.models.body

import com.manikandan.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)