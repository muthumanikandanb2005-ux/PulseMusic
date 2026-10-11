package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPlaybackBridge
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Screen state for Listen Together.
 *
 * The session owns everything the server says; this holds only what the user has typed but not yet
 * sent, so a half-filled room code survives a recomposition without being confused for room state.
 */
class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    init {
        // The bridge is a singleton that outlives this screen; starting it here is simply the
        // first moment anything asks for it. Koin's `createdAtStart` does not fire for modules
        // added with loadKoinModules, so without an explicit injection it is never constructed
        // at all — and then a room syncs membership while no audio follows anyone.
        bridge.start()

        // Host conveniences live in settings but are applied by the session, so they are mirrored
        // onto it for as long as this screen exists. Default autoApproveJoins to true unless disabled.
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_JOINS).collect {
                repository.autoApproveJoins = it != ListenTogetherPrefs.FALSE
            }
        }
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS).collect {
                repository.autoApproveSuggestions = it == ListenTogetherPrefs.TRUE
            }
        }
        viewModelScope.launch {
            val savedName = dataStore.getString(ListenTogetherPrefs.DISPLAY_NAME).first()
            if (!savedName.isNullOrBlank()) {
                _displayName.value = savedName
            } else {
                _displayName.value = "Pulse User"
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListenTogetherRoom())

    private val _displayName = MutableStateFlow("Pulse User")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _roomCodeInput = MutableStateFlow("")
    val roomCodeInput: StateFlow<String> = _roomCodeInput.asStateFlow()

    fun onDisplayNameChange(value: String) {
        val trimmed = value.take(MAX_USERNAME_LENGTH)
        _displayName.value = trimmed
        viewModelScope.launch {
            dataStore.putString(ListenTogetherPrefs.DISPLAY_NAME, trimmed)
        }
    }

    fun onRoomCodeChange(value: String) {
        val raw = value.trim()
        val codeToUse = if (raw.contains(" ") || raw.contains("/") || raw.contains(":")) {
            val regex = Regex("[A-Za-z0-9]{4,8}")
            val matches = regex.findAll(raw).toList()
            matches.lastOrNull()?.value ?: raw
        } else {
            raw
        }
        _roomCodeInput.value =
            codeToUse.uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_LENGTH)
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()

    fun createRoom() {
        val name = _displayName.value.ifBlank { "Pulse User" }
        repository.createRoom(name)
    }

    fun joinRoom() {
        val name = _displayName.value.ifBlank { "Pulse User" }
        val code = _roomCodeInput.value.trim()
        if (code.isBlank()) return
        repository.joinRoom(code, name)
    }

    fun leaveRoom() {
        repository.leaveRoom()
        _roomCodeInput.value = ""
    }

    fun approveJoin(userId: String) {
        repository.approveJoin(userId)
    }

    fun rejectJoin(userId: String) {
        repository.rejectJoin(userId)
    }

    fun approveSuggestion(id: String) {
        repository.approveSuggestion(id)
    }

    fun rejectSuggestion(id: String) {
        repository.rejectSuggestion(id)
    }

    fun kickUser(userId: String) {
        repository.kickUser(userId)
    }

    /**
     * Blocks by NAME and then kicks.
     *
     * The protocol has no ban and the server mints a new user id per connection, so the id cannot
     * be blocked on — the name is the only thing that survives a reconnect, and it is changeable.
     * A convenience, not a security control.
     */
    fun blockAndKick(
        userId: String,
        username: String,
    ) {
        viewModelScope.launch {
            val current =
                dataStore
                    .getString(ListenTogetherPrefs.BLOCKLIST)
                    .first()
                    .orEmpty()
                    .split(ListenTogetherPrefs.BLOCKLIST_SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (current.none { it.equals(username, ignoreCase = true) }) {
                dataStore.putString(
                    ListenTogetherPrefs.BLOCKLIST,
                    (current + username).joinToString(ListenTogetherPrefs.BLOCKLIST_SEPARATOR),
                )
            }
            repository.kickUser(userId)
        }
    }

    fun transferHost(userId: String) {
        repository.transferHost(userId)
    }

    fun cancelJoin() = repository.cancelJoin()

    fun clearError() = repository.clearError()

    companion object {
        const val ROOM_CODE_LENGTH = 8
        private const val MAX_USERNAME_LENGTH = 50
    }
}
