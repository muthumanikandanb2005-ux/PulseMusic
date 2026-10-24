package org.simpmusic.listentogether

import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext

private const val TAG = "ListenTogetherSession"

/** Where the socket is, independent of whether a room has been joined. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState

    data object Connecting : ConnectionState

    data class Connected(val serverVersion: String) : ConnectionState

    /** Retries are exhausted or the server refused us; the user has to act. */
    data class Failed(val reason: String) : ConnectionState
}

/** One person in the room, as the UI needs them. */
data class RoomMember(
    val userId: String,
    val username: String,
    val isHost: Boolean,
    val isConnected: Boolean,
    /** True while this member has not answered `buffer_ready` for the current track. */
    val isBuffering: Boolean = false,
)

data class PendingJoin(
    val userId: String,
    val username: String,
)

data class PendingSuggestion(
    val suggestionId: String,
    val fromUsername: String,
    val track: TrackInfo,
)

data class ListenTogetherState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    /** Null until a room is created or joined. */
    val roomCode: String? = null,
    val selfUserId: String = "",
    val isHost: Boolean = false,
    val members: List<RoomMember> = emptyList(),
    val joinRequests: List<PendingJoin> = emptyList(),
    val suggestions: List<PendingSuggestion> = emptyList(),
    val currentTrack: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    /**
     * The room's queue, in the host's order.
     *
     * Without this a guest only ever knows the *current* track, so the moment it ends they carry
     * on with whatever their own player had queued — which is everyone listening to something
     * different one track later.
     */
    val queue: List<TrackInfo> = emptyList(),
    /** Non-empty while the room is held at the buffer barrier. */
    val waitingFor: List<String> = emptyList(),
    /** Server time the last transport command was captured at; 0 when unknown. */
    val lastActionServerTime: Long = 0L,
    /**
     * The code we asked to join and have not heard back about.
     *
     * Joining is not immediate — the host has to approve it — and without this the UI has nothing
     * to show between sending `join_room` and `join_approved` arriving, so a wrong code and a host
     * who simply has not looked at their phone are indistinguishable: both look like nothing
     * happened.
     */
    val pendingJoinCode: String? = null,
    /** Set once and cleared by the UI, so a transient failure cannot wedge the screen. */
    val error: String? = null,
) {
    val inRoom: Boolean get() = roomCode != null
    val isConnected: Boolean get() = connection is ConnectionState.Connected

    /** Names, not ids — "Waiting for Long" is the only useful phrasing of the barrier. */
    val waitingForNames: List<String>
        get() = waitingFor.mapNotNull { id -> members.firstOrNull { it.userId == id }?.username }
}

/**
 * The room state machine.
 *
 * Consumes [ListenTogetherClient.events] and never touches a frame, which is what keeps it
 * testable without a socket and identical on Android and Desktop — the two platforms run entirely
 * separate player handlers, so anything that reached into playback from here would have to be
 * written twice.
 */
class ListenTogetherSession(
    private val client: ListenTogetherClient,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : CoroutineScope {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logger.e(TAG, "Unhandled exception in ListenTogetherSession: ${throwable.message}", throwable)
        _state.update { it.copy(error = throwable.message ?: "Unexpected error") }
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher + exceptionHandler

    private val _state = MutableStateFlow(ListenTogetherState())
    val state: StateFlow<ListenTogetherState> = _state.asStateFlow()

    private var pump: Job? = null

    /** Host-side conveniences from settings; auto-approving joins defaults to true for seamless code joining. */
    var autoApproveJoins: Boolean = true
    var autoApproveSuggestions: Boolean = false

    /** Opens the socket. When [force] is true, cancels any waiting reconnect backoff to connect immediately. */
    fun connect(force: Boolean = true) {
        if (pump == null || pump?.isActive != true) {
            pump?.cancel()
            pump = launch {
                runCatching {
                    client.events.collect(::onEvent)
                }.onFailure {
                    Logger.e(TAG, "Error collecting client events: ${it.message}", it)
                }
            }
        }
        _state.update { it.copy(connection = ConnectionState.Connecting, error = null) }
        client.connect(force = force)
    }

    suspend fun awaitConnected(timeoutMs: Long = 8_000L): Boolean {
        if (client.isConnected && state.value.isConnected) return true
        connect(force = true)
        return withTimeoutOrNull(timeoutMs) {
            while (!client.isConnected || !state.value.isConnected) {
                delay(50L)
            }
            true
        } ?: false
    }

    fun disconnect() {
        client.disconnect()
        pump?.cancel()
        pump = null
        _state.value = ListenTogetherState()
    }

    fun createRoom(username: String) =
        launch {
            runCatching {
                val name = username.trim().ifBlank { "Pulse User" }
                pendingUsername = name
                client.clearSessionToken()
                _state.update { it.copy(connection = ConnectionState.Connecting, error = null) }
                val connected = awaitConnected(8_000L)
                if (!connected) {
                    _state.update {
                        it.copy(
                            connection = ConnectionState.Failed("Connection timed out. Please check your internet connection."),
                            error = "Failed to connect to server. Please check internet connection.",
                        )
                    }
                    return@launch
                }
                val sent = client.send(MessageTypes.CREATE_ROOM, CreateRoomPayload(username = pendingUsername))
                if (!sent) {
                    _state.update { it.copy(error = "Could not send room creation request. Please try again.") }
                    client.connect(force = true)
                }
            }.onFailure { e ->
                Logger.e(TAG, "Error in createRoom: ${e.message}", e)
                _state.update {
                    it.copy(
                        connection = ConnectionState.Failed("Error creating room: ${e.message}"),
                        error = "Error creating room: ${e.message}",
                    )
                }
            }
        }

    fun joinRoom(
        roomCode: String,
        username: String,
    ) = launch {
        runCatching {
            val name = username.trim().ifBlank { "Pulse User" }
            pendingUsername = name
            val code = roomCode.trim().uppercase()
            if (code.isBlank()) return@launch

            client.clearSessionToken()
            _state.update { it.copy(pendingJoinCode = code, error = null, connection = ConnectionState.Connecting) }

            val connected = awaitConnected(8_000L)
            if (!connected) {
                _state.update {
                    it.copy(
                        pendingJoinCode = null,
                        connection = ConnectionState.Failed("Connection timed out. Please check your internet connection."),
                        error = "Failed to connect to server. Please check internet connection.",
                    )
                }
                return@launch
            }

            val sent = client.send(MessageTypes.JOIN_ROOM, JoinRoomPayload(roomCode = code, username = pendingUsername))
            if (!sent) {
                _state.update { it.copy(pendingJoinCode = null, error = "Could not send join request. Please try again.") }
                client.connect(force = true)
            }
        }.onFailure { e ->
            Logger.e(TAG, "Error in joinRoom: ${e.message}", e)
            _state.update {
                it.copy(
                    pendingJoinCode = null,
                    connection = ConnectionState.Failed("Error joining room: ${e.message}"),
                    error = "Error joining room: ${e.message}",
                )
            }
        }
    }

    /** Gives up on a join that has not been answered. Local only — the server needs no message. */
    fun cancelJoin() = _state.update { it.copy(pendingJoinCode = null) }

    fun leaveRoom() =
        launch {
            runCatching {
                client.send(MessageTypes.LEAVE_ROOM, LeaveRoomPayload())
                client.clearSessionToken()
                _state.update {
                    it.copy(
                        selfUserId = "",
                        pendingJoinCode = null,
                        roomCode = null,
                        isHost = false,
                        members = emptyList(),
                        joinRequests = emptyList(),
                        suggestions = emptyList(),
                        currentTrack = null,
                        waitingFor = emptyList(),
                        queue = emptyList(),
                        isPlaying = false,
                        position = 0L,
                    )
                }
            }.onFailure { Logger.e(TAG, "Error leaving room: ${it.message}", it) }
        }

    fun approveJoin(userId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId = userId))
                _state.update { s -> s.copy(joinRequests = s.joinRequests.filterNot { it.userId == userId }) }
            }.onFailure { Logger.e(TAG, "Error approving join: ${it.message}", it) }
        }

    fun rejectJoin(userId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId = userId))
                _state.update { s -> s.copy(joinRequests = s.joinRequests.filterNot { it.userId == userId }) }
            }.onFailure { Logger.e(TAG, "Error rejecting join: ${it.message}", it) }
        }

    fun approveSuggestion(suggestionId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(suggestionId = suggestionId))
                dropSuggestion(suggestionId)
            }.onFailure { Logger.e(TAG, "Error approving suggestion: ${it.message}", it) }
        }

    fun rejectSuggestion(suggestionId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(suggestionId = suggestionId))
                dropSuggestion(suggestionId)
            }.onFailure { Logger.e(TAG, "Error rejecting suggestion: ${it.message}", it) }
        }

    fun kickUser(userId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.KICK_USER, KickUserPayload(userId = userId))
            }.onFailure { Logger.e(TAG, "Error kicking user: ${it.message}", it) }
        }

    fun transferHost(userId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.TRANSFER_HOST, TransferHostPayload(newHostId = userId))
            }.onFailure { Logger.e(TAG, "Error transferring host: ${it.message}", it) }
        }

    fun suggestTrack(track: TrackInfo) =
        launch {
            runCatching {
                client.send(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(trackInfo = track))
            }.onFailure { Logger.e(TAG, "Error suggesting track: ${it.message}", it) }
        }

    /**
     * Publishes one transport command to the room. Host only — the server ignores it from a guest.
     *
     * `capturedAtServerTime` is what lets a late-arriving PLAY still land on the right position:
     * the receiver advances [position] by however long the frame spent in flight, measured on the
     * SERVER's clock rather than its own. Sending 0 (an uncalibrated clock) is safe — the receiver
     * treats it as "unknown" and uses the raw position.
     */
    fun sendPlaybackAction(
        action: String,
        trackId: String,
        position: Long,
        trackInfo: TrackInfo?,
        queue: List<TrackInfo> = emptyList(),
        queueTitle: String = "",
    ) = launch {
        runCatching {
            client.send(
                MessageTypes.PLAYBACK_ACTION,
                PlaybackActionPayload(
                    action = action,
                    trackId = trackId,
                    position = position,
                    trackInfo = trackInfo,
                    queue = queue,
                    queueTitle = queueTitle,
                    capturedAtServerTime = client.serverNow() ?: 0L,
                ),
            )
        }.onFailure { Logger.e(TAG, "Error sending playback action: ${it.message}", it) }
    }

    /** Publishes the whole queue. Host only; guests take the host's order verbatim. */
    fun sendQueue(
        tracks: List<TrackInfo>,
        queueTitle: String,
    ) = launch {
        runCatching {
            client.send(
                MessageTypes.PLAYBACK_ACTION,
                PlaybackActionPayload(
                    action = PlaybackActions.SYNC_QUEUE,
                    queue = tracks,
                    queueTitle = queueTitle,
                    capturedAtServerTime = client.serverNow() ?: 0L,
                ),
            )
        }.onFailure { Logger.e(TAG, "Error sending queue: ${it.message}", it) }
    }

    /** See `ServerClock.positionAt` — corrects a room position for time spent in flight. */
    fun positionAt(
        position: Long,
        isPlaying: Boolean,
    ): Long = client.positionAt(position, _state.value.lastActionServerTime, isPlaying)

    fun requestSync() =
        launch {
            runCatching {
                client.send(MessageTypes.REQUEST_SYNC, null)
            }.onFailure { Logger.e(TAG, "Error requesting sync: ${it.message}", it) }
        }

    fun reportBufferReady(trackId: String) =
        launch {
            runCatching {
                client.send(MessageTypes.BUFFER_READY, BufferReadyPayload(trackId = trackId))
            }.onFailure { Logger.e(TAG, "Error reporting buffer ready: ${it.message}", it) }
        }

    fun clearError() = _state.update { it.copy(error = null) }

    fun release() {
        client.release()
        pump = null
    }

    private fun dropSuggestion(id: String) =
        _state.update { s -> s.copy(suggestions = s.suggestions.filterNot { it.suggestionId == id }) }

    private fun onEvent(event: ListenTogetherEvent) {
        when (event) {
            is ListenTogetherEvent.Connected ->
                _state.update { it.copy(connection = ConnectionState.Connected(event.serverVersion)) }

            is ListenTogetherEvent.ClockReady -> Unit

            is ListenTogetherEvent.Disconnected ->
                _state.update {
                    if (event.willRetry) {
                        it.copy(connection = ConnectionState.Connecting)
                    } else {
                        it.copy(
                            connection = ConnectionState.Failed(event.reason ?: "Disconnected"),
                            error = event.reason,
                        )
                    }
                }

            is ListenTogetherEvent.Message -> onMessage(event.type, event.payload)
        }
    }

    private fun onMessage(
        type: String,
        payload: Any?,
    ) {
        when (type) {
            MessageTypes.ROOM_CREATED -> {
                val p = payload as? RoomCreatedPayload ?: return
                _state.update {
                    it.copy(
                        roomCode = p.roomCode,
                        selfUserId = p.userId,
                        isHost = true,
                        // The server sends no member list for a brand-new room: the host is alone
                        // in it, and USER_JOINED carries everyone who arrives afterwards.
                        members = listOf(RoomMember(p.userId, pendingUsername, isHost = true, isConnected = true)),
                    )
                }
            }

            MessageTypes.JOIN_APPROVED -> {
                val p = payload as? JoinApprovedPayload ?: return
                // Ask for the current state explicitly: JoinApproved carries whatever the server
                // last heard, which is nothing at all if the host has not issued a command yet.
                launch { client.send(MessageTypes.REQUEST_SYNC, null) }
                val initialMembers = p.state?.users.orEmpty().map { u -> u.toMember() }.toMutableList()
                if (initialMembers.none { it.userId == p.userId }) {
                    initialMembers.add(RoomMember(p.userId, pendingUsername, isHost = false, isConnected = true))
                }
                _state.update {
                    it.copy(
                        roomCode = p.roomCode,
                        selfUserId = p.userId,
                        isHost = false,
                        pendingJoinCode = null,
                        members = initialMembers,
                        queue = p.state?.queue.orEmpty(),
                        currentTrack = p.state?.currentTrack,
                        isPlaying = p.state?.isPlaying ?: false,
                        position = p.state?.position ?: 0L,
                    )
                }
            }

            MessageTypes.JOIN_REJECTED ->
                _state.update {
                    it.copy(
                        pendingJoinCode = null,
                        error = (payload as? JoinRejectedPayload)?.reason?.ifBlank { null } ?: "The host declined",
                    )
                }

            MessageTypes.JOIN_REQUEST -> {
                val p = payload as? JoinRequestPayload ?: return
                if (autoApproveJoins) {
                    approveJoin(p.userId)
                    return
                }
                _state.update { s ->
                    if (s.joinRequests.any { it.userId == p.userId }) {
                        s
                    } else {
                        s.copy(joinRequests = s.joinRequests + PendingJoin(p.userId, p.username))
                    }
                }
            }

            MessageTypes.USER_JOINED -> {
                val p = payload as? UserJoinedPayload ?: return
                _state.update { s ->
                    if (s.members.any { it.userId == p.userId }) {
                        s
                    } else {
                        s.copy(members = s.members + RoomMember(p.userId, p.username, isHost = false, isConnected = true))
                    }
                }
            }

            MessageTypes.USER_LEFT -> {
                val p = payload as? UserLeftPayload ?: return
                _state.update { s -> s.copy(members = s.members.filterNot { it.userId == p.userId }) }
            }

            MessageTypes.USER_DISCONNECTED -> {
                val p = payload as? UserDisconnectedPayload ?: return
                _state.update { s -> s.copy(members = s.members.map { if (it.userId == p.userId) it.copy(isConnected = false) else it }) }
            }

            MessageTypes.USER_RECONNECTED -> {
                val p = payload as? UserReconnectedPayload ?: return
                _state.update { s -> s.copy(members = s.members.map { if (it.userId == p.userId) it.copy(isConnected = true) else it }) }
            }

            MessageTypes.HOST_CHANGED -> {
                val p = payload as? HostChangedPayload ?: return
                _state.update { s ->
                    s.copy(
                        isHost = p.newHostId == s.selfUserId,
                        members = s.members.map { it.copy(isHost = it.userId == p.newHostId) },
                    )
                }
            }

            MessageTypes.KICKED ->
                _state.value =
                    ListenTogetherState(
                        connection = _state.value.connection,
                        error = (payload as? KickedPayload)?.reason ?: "Removed from the room",
                    )

            MessageTypes.BUFFER_WAIT -> {
                val p = payload as? BufferWaitPayload ?: return
                _state.update { s ->
                    s.copy(
                        waitingFor = p.waitingFor,
                        members = s.members.map { it.copy(isBuffering = it.userId in p.waitingFor) },
                    )
                }
            }

            MessageTypes.BUFFER_COMPLETE ->
                _state.update { s -> s.copy(waitingFor = emptyList(), members = s.members.map { it.copy(isBuffering = false) }) }

            MessageTypes.SYNC_STATE -> {
                val p = payload as? SyncStatePayload ?: return
                _state.update {
                    it.copy(
                        currentTrack = p.currentTrack,
                        isPlaying = p.isPlaying,
                        position = p.position,
                        // Only replace the queue when the server actually sent one — sync_state
                        // with an empty queue means "nothing to say", not "the queue is empty".
                        queue = p.queue.ifEmpty { it.queue },
                    )
                }
            }

            MessageTypes.SYNC_PLAYBACK, MessageTypes.PLAYBACK_ACTION -> {
                val p = payload as? PlaybackActionPayload ?: return
                _state.update {
                    it.copy(
                        currentTrack = p.trackInfo ?: it.currentTrack,
                        isPlaying =
                            when (p.action) {
                                PlaybackActions.PAUSE -> false
                                PlaybackActions.PLAY -> true
                                else -> it.isPlaying
                            },
                        position = if (p.action == PlaybackActions.SYNC_QUEUE) it.position else p.position,
                        queue = p.queue.ifEmpty { it.queue },
                        lastActionServerTime = p.capturedAtServerTime,
                    )
                }
            }

            MessageTypes.SUGGESTION_RECEIVED -> {
                val p = payload as? SuggestionReceivedPayload ?: return
                val track = p.trackInfo ?: return
                if (autoApproveSuggestions) {
                    approveSuggestion(p.suggestionId)
                    return
                }
                _state.update { s ->
                    if (s.suggestions.any { it.suggestionId == p.suggestionId }) {
                        s
                    } else {
                        s.copy(suggestions = s.suggestions + PendingSuggestion(p.suggestionId, p.fromUsername, track))
                    }
                }
            }

            MessageTypes.ERROR -> {
                val p = payload as? ErrorPayload ?: return
                // `rate_limited` is about one message, not the session, and showing it would put an
                // alarming banner up for something the user cannot act on.
                if (p.code != "rate_limited") {
                    Logger.w(TAG, "Server error ${p.code}: ${p.message}")
                    // An error while waiting to be let in ends that wait — most often a bad code.
                    _state.update {
                        it.copy(pendingJoinCode = null, error = p.message.ifBlank { p.code })
                    }
                }
            }
        }
    }

    /**
     * The name the user typed, kept so ROOM_CREATED can name the host.
     *
     * The server echoes the room code and user id back but not the username it was given, and the
     * host has to appear in their own member list like everyone else.
     */
    private var pendingUsername: String = ""
}

private fun UserInfo.toMember() =
    RoomMember(
        userId = userId,
        username = username,
        isHost = isHost,
        isConnected = isConnected,
    )
