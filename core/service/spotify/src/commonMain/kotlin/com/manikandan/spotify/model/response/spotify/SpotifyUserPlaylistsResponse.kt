package com.manikandan.spotify.model.response.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUserPlaylistsResponse(
    @SerialName("items")
    val items: List<SpotifyPlaylistItem?> = emptyList(),
    @SerialName("total")
    val total: Int = 0,
    @SerialName("limit")
    val limit: Int = 50,
    @SerialName("offset")
    val offset: Int = 0,
)

@Serializable
data class SpotifyPlaylistItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String? = null,
    @SerialName("images")
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("tracks")
    val tracks: SpotifyTracksRef? = null,
    @SerialName("owner")
    val owner: SpotifyOwner? = null,
)

@Serializable
data class SpotifyImage(
    @SerialName("url")
    val url: String = "",
    @SerialName("height")
    val height: Int? = null,
    @SerialName("width")
    val width: Int? = null,
)

@Serializable
data class SpotifyTracksRef(
    @SerialName("total")
    val total: Int = 0,
    @SerialName("href")
    val href: String? = null,
)

@Serializable
data class SpotifyOwner(
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("id")
    val id: String? = null,
)

@Serializable
data class SpotifyUserProfileResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("images")
    val images: List<SpotifyImage> = emptyList(),
)

@Serializable
data class SpotifyRecentlyPlayedResponse(
    @SerialName("items")
    val items: List<SpotifyRecentlyPlayedItem?> = emptyList(),
)

@Serializable
data class SpotifyRecentlyPlayedItem(
    @SerialName("track")
    val track: SpotifyTrackItem? = null,
    @SerialName("played_at")
    val playedAt: String? = null,
)

@Serializable
data class SpotifyTopTracksResponse(
    @SerialName("items")
    val items: List<SpotifyTrackItem?> = emptyList(),
)

@Serializable
data class SpotifySavedTracksResponse(
    @SerialName("items")
    val items: List<SpotifySavedTrackItem?> = emptyList(),
)

@Serializable
data class SpotifySavedTrackItem(
    @SerialName("track")
    val track: SpotifyTrackItem? = null,
    @SerialName("added_at")
    val addedAt: String? = null,
)

@Serializable
data class SpotifyTracksSearchResponse(
    @SerialName("tracks")
    val tracks: SpotifyTrackSearchPage = SpotifyTrackSearchPage(),
)

@Serializable
data class SpotifyTrackSearchPage(
    @SerialName("items")
    val items: List<SpotifyTrackItem?> = emptyList(),
)

@Serializable
data class SpotifyPlaylistDetailResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String? = null,
    @SerialName("images")
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("owner")
    val owner: SpotifyOwner? = null,
    @SerialName("tracks")
    val tracks: SpotifyPlaylistTracksPage = SpotifyPlaylistTracksPage(),
)

@Serializable
data class SpotifyPlaylistTracksPage(
    @SerialName("items")
    val items: List<SpotifyPlaylistTrackEntry?> = emptyList(),
    @SerialName("total")
    val total: Int = 0,
)

@Serializable
data class SpotifyPlaylistTrackEntry(
    @SerialName("track")
    val track: SpotifyTrackItem? = null,
)

@Serializable
data class SpotifyTrackItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("duration_ms")
    val durationMs: Long = 0,
    @SerialName("artists")
    val artists: List<SpotifyArtistItem> = emptyList(),
    @SerialName("album")
    val album: SpotifyAlbumItem? = null,
)

@Serializable
data class SpotifyArtistItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
)

@Serializable
data class SpotifyAlbumItem(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("images")
    val images: List<SpotifyImage> = emptyList(),
)

@Serializable
data class SpotifyFeaturedPlaylistsResponse(
    @SerialName("message")
    val message: String? = null,
    @SerialName("playlists")
    val playlists: SpotifyPlaylistsWrapper = SpotifyPlaylistsWrapper(),
)

@Serializable
data class SpotifyPlaylistsWrapper(
    @SerialName("items")
    val items: List<SpotifyPlaylistItem?> = emptyList(),
    @SerialName("total")
    val total: Int = 0,
)
