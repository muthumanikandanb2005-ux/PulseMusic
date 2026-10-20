package com.maxrave.kotlinytmusicscraper.models.simpmusic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("download_count")
    val downloadCount: Long? = null,
    @SerialName("id")
    val id: Long? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("size")
    val size: Long? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("uploader")
    val uploader: Uploader? = null,
    @SerialName("url")
    val url: String? = null,
)
