package com.maxrave.kotlinytmusicscraper.models.simpmusic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubResponse(
    @SerialName("assets")
    val assets: List<Asset?>? = null,
    @SerialName("assets_url")
    val assetsUrl: String? = null,
    @SerialName("author")
    val author: Author? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("draft")
    val draft: Boolean? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("node_id")
    val nodeId: String? = null,
    @SerialName("prerelease")
    val prerelease: Boolean? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("reactions")
    val reactions: Reactions? = null,
    @SerialName("tag_name")
    val tagName: String? = null,
    @SerialName("tarball_url")
    val tarballUrl: String? = null,
    @SerialName("target_commitish")
    val targetCommitish: String? = null,
    @SerialName("upload_url")
    val uploadUrl: String? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("zipball_url")
    val zipballUrl: String? = null,
)
