package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import kotlinx.coroutines.flow.Flow

interface SupabaseAuthRepository {
    val currentUserEmail: Flow<String?>
    val currentUserId: Flow<String?>
    val isLoggedIn: Flow<Boolean>

    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(email: String, name: String? = null): Result<String>
    suspend fun signOut()
    suspend fun refreshSession(): Result<Boolean>

    suspend fun syncLikedSong(song: SongEntity, liked: Boolean)
    suspend fun syncPlaylist(playlist: LocalPlaylistEntity, tracks: List<String>)
    suspend fun syncListenCount(videoId: String, song: SongEntity?)
    suspend fun syncAnalyticsEvent(eventType: String, eventData: Map<String, String>)
    suspend fun syncAll(): Result<Unit>
}
