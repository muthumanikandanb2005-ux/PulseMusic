package com.maxrave.data.listentogether

import com.maxrave.common.MERGING_DATA_TYPE
import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.domain.data.model.listentogether.RoomTrack
import com.manikandan.domain.data.player.GenericMediaItem
import com.manikandan.domain.data.player.GenericMediaMetadata
import com.manikandan.domain.mediaservice.handler.MediaPlayerHandler
import com.manikandan.domain.mediaservice.handler.QueueData
import com.manikandan.domain.mediaservice.handler.SimpleMediaState
import com.manikandan.domain.repository.ListenTogetherRepository
import com.maxrave.logger.Logger
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.simpmusic.listentogether.ListenTogetherSession
import org.simpmusic.listentogether.PlaybackActions
import org.simpmusic.listentogether.TrackInfo

private const val PLAY_SETTLE_TIMEOUT_MS = 2_000L

/** Poll step for the settle wait; playWhenReady has no flow to collect. */
private const val PLAY_SETTLE_POLL_MS = 50L
private const val TAG = "ListenTogetherBridge"

/** What the guest reacts to. A data class so `distinctUntilChanged` compares every field. */
private data class RoomSnapshot(
    val track: RoomTrack?,
    val isPlaying: Boolean,
    val position: Long,
    val queueIds: List<String>,
)

/**
 * Joins a Listen Together room to the local player.
 *
 * Lives in `commonMain` and talks only to [MediaPlayerHandler], which is the one interface both
 * platforms implement — Android's `MediaServiceHandlerImpl` and Desktop's
 * `JvmMediaPlayerHandlerImpl` share nothing else, so anything written against a concrete player
 * would have to be written twice and would drift.
 *
 * Direction of travel is decided entirely by who hosts:
 * - **Host** watches the local player and publishes what it does.
 * - **Guest** watches the room and applies what the host did, and publishes nothing.
 *
 * The [applyingRemote] guard is what stops those two from feeding each other: applying a remote
 * pause makes the local player report "paused", which would otherwise be published straight back
 * to the server as a fresh command.
 */
class ListenTogetherPlaybackBridge(
    /**
     * Read through the repository (domain types), written through the session (protocol types).
     *
     * The split is deliberate: reacting to a room is app logic and belongs on our own model, while
     * publishing a command is protocol detail that must not leak upward — putting
     * `sendPlaybackAction` on the domain interface would drag Metrolist's schema into domain.
     */
    private val repository: ListenTogetherRepository,
    private val session: ListenTogetherSession,
    private val handler: MediaPlayerHandler,
    private val scope: CoroutineScope,
) {
    private var started = false
    private var applyingRemote = false
    private var lastPublishedTrackId: String? = null
    private var lastAppliedTrackId: String? = null
    private var lastAppliedQueueIds: List<String> = emptyList()

    /**
     * Whether the room was playing before the command currently being applied.
     *
     * Needed because change_track always says "not playing" — see [watchRoomForGuests].
     */
    private var lastRoomPlaying = false

    /** Idempotent: callers cannot know whether something else already started it. */
    fun start() {
        if (started) return
        started = true
        Logger.i(TAG, "Playback bridge started")
        scope.launch { runCatching { suppressCrossfadeWhileInRoom() }.onFailure { Logger.e(TAG, "suppressCrossfade failed", it) } }
        scope.launch { runCatching { watchRoomForGuests() }.onFailure { Logger.e(TAG, "watchRoomForGuests failed", it) } }
        scope.launch { runCatching { resyncGuestOnResume() }.onFailure { Logger.e(TAG, "resyncGuestOnResume failed", it) } }
        scope.launch { runCatching { publishCurrentStateOnJoin() }.onFailure { Logger.e(TAG, "publishCurrentStateOnJoin failed", it) } }
        scope.launch { runCatching { publishStateWhenSomeoneArrives() }.onFailure { Logger.e(TAG, "publishStateWhenSomeoneArrives failed", it) } }
        scope.launch { runCatching { publishQueueAsHost() }.onFailure { Logger.e(TAG, "publishQueueAsHost failed", it) } }
        scope.launch { runCatching { publishTrackChangesAsHost() }.onFailure { Logger.e(TAG, "publishTrackChangesAsHost failed", it) } }
        scope.launch { runCatching { publishPlayPauseAsHost() }.onFailure { Logger.e(TAG, "publishPlayPauseAsHost failed", it) } }
        scope.launch { runCatching { publishSeeksAsHost() }.onFailure { Logger.e(TAG, "publishSeeksAsHost failed", it) } }
        scope.launch { runCatching { answerBufferBarrier() }.onFailure { Logger.e(TAG, "answerBufferBarrier failed", it) } }
    }

    /**
     * Crossfade overlaps two tracks for seconds, which drifts a room apart at every transition.
     *
     * The user's own setting is left alone — this is a separate override on the player, the same
     * shape as the sleep-timer fade, so a process death mid-room cannot lose their preference.
     */
    private suspend fun suppressCrossfadeWhileInRoom() {
        repository.room
            .map { it.inRoom }
            .distinctUntilChanged()
            .collect { inRoom ->
                runCatching {
                    withContext(Dispatchers.Main) {
                        runCatching { handler.player.crossfadeSuppressed = inRoom }
                    }
                    if (!inRoom) {
                        lastRoomPlaying = false
                    } else if (!repository.room.value.isHost) {
                        // Ask for the live position the moment we are in.
                        repository.requestSync()
                    }
                    Logger.i(TAG, if (inRoom) "Crossfade suppressed for the room" else "Crossfade restored")
                }.onFailure { e ->
                    Logger.e(TAG, "Error in suppressCrossfadeWhileInRoom: ${e.message}")
                }
            }
    }

    /**
     * A guest may pause, and stays paused.
     */
    private suspend fun resyncGuestOnResume() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { locallyPlaying ->
                runCatching {
                    val room = repository.room.value
                    if (!room.inRoom || room.isHost || applyingRemote) return@collect
                    if (!locallyPlaying) {
                        Logger.i(TAG, "Guest paused locally — leaving the room running")
                        return@collect
                    }
                    Logger.i(TAG, "Guest resumed — asking the server where the room is")
                    repository.requestSync()
                }.onFailure { e ->
                    Logger.e(TAG, "Error in resyncGuestOnResume: ${e.message}")
                }
            }
    }

    // ─────────────────────────── guest: follow the host ───────────────────────────

    private suspend fun watchRoomForGuests() {
        repository.room
            .map {
                RoomSnapshot(
                    track = it.currentTrack,
                    isPlaying = it.isPlaying,
                    position = it.position,
                    queueIds = it.queue.map { t -> t.id },
                ) to (it.inRoom && !it.isHost)
            }
            .distinctUntilChanged()
            .collect { (snapshot, shouldFollow) ->
                if (!shouldFollow) return@collect
                val (track, isPlaying, position, queueIds) = snapshot
                Logger.i(TAG, "Room says: track=${track?.id} playing=$isPlaying pos=$position queue=${queueIds.size}")
                applyingRemote = true
                try {
                    val queueChanged = queueIds != lastAppliedQueueIds
                    val trackChanged =
                        track != null && track.id.isNotBlank() && (track.id != lastAppliedTrackId || queueChanged)
                    val playing = if (trackChanged && !isPlaying) lastRoomPlaying else isPlaying
                    lastRoomPlaying = playing
                    if (trackChanged && track != null) {
                        val sameTrack = track.id == lastAppliedTrackId
                        lastAppliedTrackId = track.id
                        lastAppliedQueueIds = queueIds
                        val startAt =
                            when {
                                sameTrack -> withContext(Dispatchers.Main) { runCatching { handler.player.currentPosition }.getOrDefault(0L) }
                                position <= 0L -> 0L
                                else -> session.positionAt(position, playing)
                            }
                        playTrack(track, keepPosition = startAt, playWhenReady = playing)
                    }
                    applyTransport(playing, position)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to apply remote state: ${e.message}")
                } finally {
                    applyingRemote = false
                }
            }
    }

    private suspend fun applyTransport(
        isPlaying: Boolean,
        position: Long,
    ) = withContext(Dispatchers.Main) {
        runCatching {
            val corrected = session.positionAt(position, isPlaying)
            val currentPos = runCatching { handler.player.currentPosition }.getOrDefault(0L)
            if (abs(currentPos - corrected) > SEEK_TOLERANCE_MS) {
                runCatching { handler.player.seekTo(corrected) }
            }
            val playWhenReady = runCatching { handler.player.playWhenReady }.getOrDefault(false)
            if (isPlaying && !playWhenReady) {
                runCatching { handler.player.play() }
            } else if (!isPlaying && playWhenReady) {
                runCatching { handler.player.pause() }
            }
        }.onFailure { e ->
            Logger.e(TAG, "applyTransport failed: ${e.message}")
        }
    }

    /**
     * Loads the host's track and queue.
     */
    private suspend fun playTrack(
        info: RoomTrack,
        keepPosition: Long = 0L,
        playWhenReady: Boolean,
    ) {
        val roomQueue = repository.room.value.queue
        val ordered =
            (listOf(info) + roomQueue.filter { it.id != info.id })
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
        if (ordered.isEmpty()) {
            Logger.w(TAG, "Guest playTrack: ordered queue is empty, skipping")
            return
        }
        Logger.i(TAG, "Guest loading ${info.id} (${info.title}) + ${ordered.size - 1} upcoming")

        withContext(Dispatchers.Main) {
            runCatching {
                handler.clearMediaItems()
                handler.addMediaItem(ordered.first().toRoomMediaItem(), playWhenReady = playWhenReady)
                val rest = ordered.drop(1)
                if (rest.isNotEmpty()) handler.addMediaItemList(rest.map { it.toRoomMediaItem() })
                if (keepPosition > 0L) handler.player.seekTo(keepPosition)
            }.onFailure { e ->
                Logger.e(TAG, "Error in playTrack: ${e.message}")
            }
        }
    }

    private fun RoomTrack.toRoomMediaItem(): GenericMediaItem =
        GenericMediaItem(
            mediaId = id,
            uri = id,
            metadata =
                GenericMediaMetadata(
                    title = title,
                    artist = artist.ifBlank { null },
                    albumTitle = album.ifBlank { null },
                    artworkUri = thumbnail.ifBlank { null },
                    description = MERGING_DATA_TYPE.SONG,
                ),
            customCacheKey = id,
        )

    // ─────────────────────────── host: publish what we do ───────────────────────────

    private suspend fun publishCurrentStateOnJoin() {
        repository.room
            .map { it.inRoom && it.isHost }
            .distinctUntilChanged()
            .collect { isHosting ->
                runCatching {
                    if (isHosting) publishSnapshot()
                }.onFailure { Logger.e(TAG, "Error publishing state on join: ${it.message}") }
            }
    }

    private suspend fun publishStateWhenSomeoneArrives() {
        repository.room
            .map { it.members.size }
            .distinctUntilChanged()
            .collect { count ->
                runCatching {
                    val state = repository.room.value
                    if (state.inRoom && state.isHost && count > 1) publishSnapshot()
                }.onFailure { Logger.e(TAG, "Error publishing state when someone arrives: ${it.message}") }
            }
    }

    private suspend fun publishQueueAsHost() {
        handler.queueData
            .map { it?.data?.listTracks?.map { t -> t.videoId }.orEmpty() }
            .distinctUntilChanged()
            .collect { ids ->
                runCatching {
                    val state = repository.room.value
                    if (!state.inRoom || !state.isHost || applyingRemote || ids.isEmpty()) return@collect
                    publishQueue()
                }.onFailure { Logger.e(TAG, "Error publishing queue: ${it.message}") }
            }
    }

    private fun publishQueue() {
        runCatching {
            val data = handler.queueData.value?.data ?: return
            val tracks = data.listTracks.toList().map { it.toTrackInfo() }
            if (tracks.isEmpty()) return
            session.sendQueue(tracks, data.playlistName.orEmpty())
            Logger.i(TAG, "Published queue of ${tracks.size} track(s)")
        }.onFailure { e ->
            Logger.e(TAG, "Error in publishQueue: ${e.message}")
        }
    }

    private fun publishSnapshot() {
        runCatching {
            val item = handler.nowPlaying.value ?: return
            if (item.mediaId.isBlank()) return
            lastPublishedTrackId = item.mediaId
            val data = handler.queueData.value?.data
            val currentPos = runCatching { handler.player.currentPosition }.getOrDefault(0L)
            val isReady = runCatching { handler.player.playWhenReady }.getOrDefault(false)
            session.sendPlaybackAction(
                action = PlaybackActions.CHANGE_TRACK,
                trackId = item.mediaId,
                position = currentPos,
                trackInfo = item.toTrackInfo(),
                queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
                queueTitle = data?.playlistName.orEmpty(),
            )
            session.sendPlaybackAction(
                action = if (isReady) PlaybackActions.PLAY else PlaybackActions.PAUSE,
                trackId = "",
                position = currentPos,
                trackInfo = null,
            )
            Logger.i(TAG, "Published current state to the room: ${item.mediaId}")
        }.onFailure { e ->
            Logger.e(TAG, "Error publishing snapshot: ${e.message}")
        }
    }

    private suspend fun publishTrackChangesAsHost() {
        handler.nowPlaying
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.mediaId == new.mediaId }
            .collect { item ->
                runCatching {
                    val state = repository.room.value
                    if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                    if (item.mediaId == lastPublishedTrackId) return@collect
                    lastPublishedTrackId = item.mediaId
                    Logger.i(TAG, "Host publishing track change: ${item.mediaId}")
                    val data = handler.queueData.value?.data
                    session.sendPlaybackAction(
                        action = PlaybackActions.CHANGE_TRACK,
                        trackId = item.mediaId,
                        position = 0L,
                        trackInfo = item.toTrackInfo(),
                        queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
                        queueTitle = data?.playlistName.orEmpty(),
                    )
                    val started =
                        withTimeoutOrNull(PLAY_SETTLE_TIMEOUT_MS) {
                            while (!runCatching { handler.player.playWhenReady }.getOrDefault(false) &&
                                !runCatching { handler.player.isPlaying }.getOrDefault(false)
                            ) {
                                delay(PLAY_SETTLE_POLL_MS)
                            }
                        } != null
                    if (started) {
                        session.sendPlaybackAction(
                            action = PlaybackActions.PLAY,
                            trackId = "",
                            position = runCatching { handler.player.currentPosition }.getOrDefault(0L),
                            trackInfo = null,
                        )
                    }
                }.onFailure { e ->
                    Logger.e(TAG, "Error publishing track change: ${e.message}")
                }
            }
    }

    private suspend fun publishPlayPauseAsHost() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { isPlaying ->
                runCatching {
                    val state = repository.room.value
                    if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                    val intent = runCatching { handler.player.playWhenReady }.getOrDefault(isPlaying)
                    if (isPlaying != intent) return@collect
                    Logger.i(TAG, "Host publishing ${if (intent) "PLAY" else "PAUSE"}")
                    session.sendPlaybackAction(
                        action = if (intent) PlaybackActions.PLAY else PlaybackActions.PAUSE,
                        trackId = "",
                        position = runCatching { handler.player.currentPosition }.getOrDefault(0L),
                        trackInfo = null,
                    )
                }.onFailure { e ->
                    Logger.e(TAG, "Error publishing play/pause: ${e.message}")
                }
            }
    }

    private suspend fun publishSeeksAsHost() {
        var lastProgress = 0L
        var lastAt = 0L
        handler.simpleMediaState.collect { mediaState ->
            runCatching {
                val progress = (mediaState as? SimpleMediaState.Progress)?.progress ?: return@collect
                val now = PROCESS_START.elapsedNow().inWholeMilliseconds
                val previous = lastProgress
                val previousAt = lastAt
                lastProgress = progress
                lastAt = now

                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                if (previousAt == 0L) return@collect

                val elapsed = now - previousAt
                val isPlaying = runCatching { handler.player.isPlaying }.getOrDefault(false)
                val expected = previous + if (isPlaying) elapsed else 0L
                if (kotlin.math.abs(progress - expected) < SEEK_DETECT_MS) return@collect

                Logger.i(TAG, "Host publishing SEEK to $progress (expected ~$expected)")
                session.sendPlaybackAction(
                    action = PlaybackActions.SEEK,
                    trackId = "",
                    position = progress,
                    trackInfo = null,
                )
            }.onFailure { e ->
                Logger.e(TAG, "Error publishing seek: ${e.message}")
            }
        }
    }

    // ─────────────────────────── the buffer barrier ───────────────────────────

    private suspend fun answerBufferBarrier() {
        repository.room
            .map { it.waitingFor to it.currentTrack?.id }
            .distinctUntilChanged()
            .collect { (waitingFor, trackId) ->
                runCatching {
                    val state = repository.room.value
                    if (trackId.isNullOrBlank() || !state.inRoom) return@collect
                    if (state.selfUserId !in waitingFor) return@collect
                    withTimeoutOrNull(BUFFER_READY_TIMEOUT_MS) {
                        while (runCatching { handler.player.bufferedPercentage }.getOrDefault(0) < READY_BUFFER_PERCENT &&
                            runCatching { handler.player.bufferedPosition }.getOrDefault(0L) <= 0L &&
                            !runCatching { handler.player.isPlaying }.getOrDefault(false)
                        ) {
                            delay(100L)
                        }
                    }
                    val pct = runCatching { handler.player.bufferedPercentage }.getOrDefault(0)
                    Logger.i(TAG, "Buffer ready for track $trackId (buffered $pct%)")
                    session.reportBufferReady(trackId)
                }.onFailure { e ->
                    Logger.e(TAG, "Error answering buffer barrier: ${e.message}")
                }
            }
    }

    private fun RoomTrack.toProtocol(): TrackInfo =
        TrackInfo(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = durationMs,
            thumbnail = thumbnail,
        )

    private fun Track.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = videoId,
            title = title,
            artist = artists?.joinToString(", ") { it.name }.orEmpty(),
            album = album?.name.orEmpty(),
            duration = (durationSeconds?.toLong() ?: 0L) * 1000L,
            thumbnail = thumbnails?.lastOrNull()?.url.orEmpty(),
        )

    private fun GenericMediaItem.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = mediaId,
            title = metadata.title.orEmpty(),
            artist = metadata.artist.orEmpty(),
            album = metadata.albumTitle.orEmpty(),
            duration = runCatching { handler.player.duration.coerceAtLeast(0L) }.getOrDefault(0L),
            thumbnail = metadata.artworkUri.orEmpty(),
        )

    private companion object {
        val PROCESS_START = TimeSource.Monotonic.markNow()
        const val SEEK_TOLERANCE_MS = 750L
        const val SEEK_DETECT_MS = 2_500L
        const val READY_BUFFER_PERCENT = 5
        const val BUFFER_READY_TIMEOUT_MS = 4_000L
    }
}
