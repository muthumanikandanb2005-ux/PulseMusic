package com.maxrave.kotlinytmusicscraper.models.simpmusic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reactions(
    @SerialName("confused")
    val confused: Int? = null,
    @SerialName("eyes")
    val eyes: Int? = null,
    @SerialName("heart")
    val heart: Int? = null,
    @SerialName("hooray")
    val hooray: Int? = null,
    @SerialName("laugh")
    val laugh: Int? = null,
    @SerialName("rocket")
    val rocket: Int? = null,
    @SerialName("total_count")
    val totalCount: Long? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("+1")
    val x1: Int? = null,
    @SerialName("-1")
    val x2: Int? = null,
)