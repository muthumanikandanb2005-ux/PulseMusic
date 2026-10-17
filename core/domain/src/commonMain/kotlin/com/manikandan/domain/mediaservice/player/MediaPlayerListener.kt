package com.manikandan.domain.mediaservice.player

import com.manikandan.domain.data.player.GenericCastState
import com.manikandan.domain.data.player.GenericMediaItem
import com.manikandan.domain.data.player.GenericTracks
import com.manikandan.domain.data.player.PlayerError

/**
 * Listener interface for media player events
 */
interface MediaPlayerListener {
    fun onPlaybackStateChanged(playbackState: Int) {}

    fun onIsPlayingChanged(isPlaying: Boolean) {}

    // Default no-op so non-emitting implementors (e.g. the JVM adapter) don't have to override it.
    fun onSeeked(positionMs: Long) {}

    fun onMediaItemTransition(
        mediaItem: GenericMediaItem?,
        reason: Int,
    ) {}

    fun onTimelineChanged(
        list: List<GenericMediaItem>, reason: String
    ) {}

    fun onTracksChanged(tracks: GenericTracks) {}

    fun onPlayerError(error: PlayerError) {}

    fun shouldOpenOrCloseEqualizerIntent(shouldOpen: Boolean) {}

    fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean, list: List<GenericMediaItem>) {}

    fun onRepeatModeChanged(repeatMode: Int) {}

    fun onIsLoadingChanged(isLoading: Boolean) {}

    fun onCrossfadeStateChanged(isCrossfading: Boolean) {}

    fun onVolumeChanged(volume: Float) {}

    fun onCastStateChanged(castState: GenericCastState) {}
}