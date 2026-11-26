package com.manikandan.domain.extension

import com.manikandan.common.MERGING_DATA_TYPE
import com.manikandan.domain.data.entities.SongEntity
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.cookie.CookieItem
import com.manikandan.domain.data.model.searchResult.songs.Thumbnail
import com.manikandan.domain.data.player.GenericMediaItem
import com.manikandan.domain.data.player.GenericMediaMetadata
import com.manikandan.domain.utils.connectArtists
import com.manikandan.domain.utils.toListName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

@OptIn(ExperimentalTime::class)
fun epochMillisToLocalDateTime(epochMillis: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())

fun LocalDateTime.isBefore(other: LocalDateTime): Boolean = this < other

fun LocalDateTime.isAfter(other: LocalDateTime): Boolean = this > other

@OptIn(ExperimentalTime::class)
fun LocalDateTime.plusSeconds(seconds: Long): LocalDateTime =
    this
        .toInstant(TimeZone.currentSystemDefault())
        .plus(seconds, DateTimeUnit.SECOND, TimeZone.currentSystemDefault())
        .toLocalDateTime(TimeZone.currentSystemDefault())

@OptIn(ExperimentalTime::class)
fun LocalDateTime.plusMinutes(minutes: Long): LocalDateTime = plusSeconds(minutes * 60)

fun startTimestampOfThisYear(): LocalDateTime = LocalDateTime(now().year, 1, 1, 0, 0, 0, 0)

fun LocalDateTime.beforeXDays(x: Int): LocalDateTime = this.date.minus(x, DateTimeUnit.DAY).atTime(this.time)

fun GenericMediaItem.isSong(): Boolean = this.metadata.description?.contains(MERGING_DATA_TYPE.SONG) == true

fun GenericMediaItem.isVideo(): Boolean = this.metadata.description?.contains(MERGING_DATA_TYPE.VIDEO) == true

fun GenericMediaItem.toSongEntity(): SongEntity =
    SongEntity(
        videoId = this.mediaId,
        albumId = null,
        albumName = this.metadata.albumTitle.toString(),
        artistId = null,
        artistName = listOf(this.metadata.artist.toString()),
        duration = "",
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = this.metadata.artworkUri.toString(),
        title = this.metadata.title.toString(),
        videoType = "",
        category = "",
        resultType = "",
        liked = false,
        totalPlayTime = 0,
        downloadState = 0,
    )

fun SongEntity.toGenericMediaItem(): GenericMediaItem {
    val upgradedThumb = upgradeArtworkUrl(this.thumbnails)
    val isSong = isSquareMusicArtwork(upgradedThumb) ||
        (!this.videoId.isNullOrBlank() && this.videoType == "Song")
    return GenericMediaItem(
        mediaId = this.videoId,
        uri = this.videoId,
        metadata =
            GenericMediaMetadata(
                title = this.title,
                artist = this.artistName?.connectArtists(),
                albumTitle = this.albumName,
                artworkUri = upgradedThumb,
                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
            ),
        customCacheKey = this.videoId,
    )
}

fun Track.toGenericMediaItem(): GenericMediaItem {
    val thumbUrl =
        this.thumbnails.bestThumbnailUrl()
            ?: "http://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg"
    val artistName: String = this.artists.toListName().connectArtists()
    val bestThumb = this.thumbnails?.maxByOrNull { it.width * it.height }
    val isSong =
        (
            bestThumb?.height != 0 &&
                bestThumb?.height == bestThumb?.width &&
                bestThumb?.height != null
        ) &&
            (!thumbUrl.contains("hq720") && !thumbUrl.contains("maxresdefault"))
    return GenericMediaItem(
        mediaId = this.videoId,
        uri = this.videoId,
        metadata =
            GenericMediaMetadata(
                title = this.title,
                artist = artistName,
                albumTitle = this.album?.name,
                artworkUri = thumbUrl,
                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
            ),
        customCacheKey = this.videoId,
    )
}

private val gUserContentSizeWRegex = Regex("=w\\d+-h\\d+[^?&#]*")
private val gUserContentSizeSRegex = Regex("=s\\d+[^?&#]*")

/**
 * Rewrites thumbnail URLs from Spotify, Google/YouTube, and JioSaavn CDNs to full-resolution variants
 * so album art stays razor sharp across Home, Playlists, MiniPlayer, and Now Playing.
 */
fun upgradeArtworkUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    var res = url
    if (res.contains("scdn.co") || res.contains("spotifycdn.com")) {
        res = res
            .replace("ab67616d00004851", "ab67616d0000b273")
            .replace("ab67616d00001e02", "ab67616d0000b273")
    }
    if (res.contains("googleusercontent.com") || res.contains("ggpht.com")) {
        res = gUserContentSizeWRegex.replace(res, "=w1080-h1080-l90-rj")
        res = gUserContentSizeSRegex.replace(res, "=s1080")
    }
    if (res.contains("saavncdn.com")) {
        res = res
            .replace("-50x50.", "-500x500.")
            .replace("-150x150.", "-500x500.")
            .replace("50x50.jpg", "500x500.jpg")
            .replace("150x150.jpg", "500x500.jpg")
    }
    if (res.contains("i.ytimg.com") && res.contains("/sddefault.")) {
        res = res.replace("/sddefault.", "/maxresdefault.")
    }
    return res
}

/**
 * Selects the highest-resolution thumbnail URL and upgrades CDN size parameters when available.
 */
fun List<Thumbnail>?.bestThumbnailUrl(): String? {
    if (this.isNullOrEmpty()) return null
    val best = this.maxByOrNull { it.width.toLong() * it.height.toLong() } ?: this.lastOrNull()
    return upgradeArtworkUrl(best?.url)
}

private fun isSquareMusicArtwork(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return (url.contains("w544") && url.contains("h544")) ||
        (url.contains("w1080") && url.contains("h1080")) ||
        (url.contains("w1200") && url.contains("h1200")) ||
        url.contains("ab67616d") ||
        url.contains("500x500")
}

fun CookieItem.Content.toNetscapeFormat(includeSubdomains: Boolean = true): String {
    with(this) {
        val stringList =
            listOf(
                domain,
                includeSubdomains.toString().uppercase(),
                path,
                isSecure.toString().uppercase(),
                expiresUtc.toString(),
                name,
                value,
            )

        val builder = StringBuilder(stringList.first())

        for (s in stringList.subList(1, stringList.size)) {
            if (s.isNotEmpty()) {
                if (builder.isNotEmpty()) {
                    builder.append("\u0009")
                }
                builder.append(s)
            }
        }
        return builder.toString()
    }
}

fun CookieItem.toNetScapeString(): String =
    "# Netscape HTTP Cookie File\n" +
        "# WebView Generated by the Pulse app\n" +
        "# This is a generated file! Do not edit.\n\n" +
        this.content
            .fold(StringBuilder("")) { acc, cookie ->
                acc.append(cookie.toNetscapeFormat()).append("\n")
            }.toString()

/**
 * Decodes HTML entities in a string to their corresponding characters.
 * Handles both named entities (e.g. &amp;) and numeric entities (e.g. &#39; &#x27;).
 */
fun decodeHtmlEntities(text: String): String {
    // Named HTML entities
    val namedEntities =
        mapOf(
            "&apos;" to "'",
            "&quot;" to "\"",
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&#x27;" to "'",
            "&#x22;" to "\"",
            "&#x26;" to "&",
            "&#x3C;" to "<",
            "&#x3E;" to ">",
        )

    var result = text

    // Replace named entities
    for ((entity, char) in namedEntities) {
        result = result.replace(entity, char, ignoreCase = true)
    }

    // Replace hex numeric entities: &#xHH; or &#xHH (with or without semicolon)
    result =
        Regex("""&#x([0-9a-fA-F]+);?""").replace(result) { match ->
            val codePoint = match.groupValues[1].toIntOrNull(16)
            if (codePoint != null) {
                Char(codePoint).toString()
            } else {
                match.value
            }
        }

    // Replace decimal numeric entities: &#DD; or &#DD (with or without semicolon)
    result =
        Regex("""&#(\d+);?""").replace(result) { match ->
            val codePoint = match.groupValues[1].toIntOrNull()
            if (codePoint != null) {
                Char(codePoint).toString()
            } else {
                match.value
            }
        }

    return result
}