package com.manikandan.kotlinytmusicscraper.models.body

import com.manikandan.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)