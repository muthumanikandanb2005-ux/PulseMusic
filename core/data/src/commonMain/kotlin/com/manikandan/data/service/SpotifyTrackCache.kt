package com.manikandan.data.service

import com.manikandan.domain.data.model.browse.album.Track
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory metadata cache for Spotify-backed tracks (`sp_track:...`) so playback,
 * queue building, and radio resolution work immediately without waiting for DB round-trips.
 */
internal object SpotifyTrackCache {
    private val mutex = Mutex()
    private val tracksById = LinkedHashMap<String, Track>(256)

    suspend fun putAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        mutex.withLock {
            for (track in tracks) {
                val vid = track.videoId
                if (vid.isBlank()) continue
                tracksById[vid] = track
                if (vid.startsWith("sp_track:")) {
                    tracksById[vid.removePrefix("sp_track:")] = track
                }
            }
            if (tracksById.size > 1200) {
                val keys = tracksById.keys.take(tracksById.size - 800)
                keys.forEach { tracksById.remove(it) }
            }
        }
    }

    suspend fun get(videoId: String): Track? =
        mutex.withLock {
            tracksById[videoId] ?: tracksById[videoId.removePrefix("sp_track:")]
        }
}
