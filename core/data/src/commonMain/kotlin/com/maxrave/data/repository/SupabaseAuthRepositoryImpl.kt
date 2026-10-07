package com.maxrave.data.repository

import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.SupabaseAuthRepository
import com.maxrave.ktorext.getEngine
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAuthRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
) : SupabaseAuthRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(getEngine())

    companion object {
        private const val TAG = "SupabaseAuthRepo"
        const val SUPABASE_URL = "https://attuvdgmkucyklfdbqzh.supabase.co"
        const val SUPABASE_KEY = "sb_publishable_MH05deYBA882z_vbWqRqsQ_ksLK244i"
    }

    override val currentUserEmail: Flow<String?> = dataStoreManager.supabaseUserEmail
    override val currentUserId: Flow<String?> = dataStoreManager.supabaseUserId
    override val isLoggedIn: Flow<Boolean> = dataStoreManager.supabaseSessionToken.map { !it.isNullOrBlank() }

    override suspend fun signIn(email: String, password: String): Result<String> {
        return runCatching {
            val body = buildJsonObject {
                put("email", email.trim())
                put("password", password)
            }.toString()

            val response = client.post("$SUPABASE_URL/auth/v1/token?grant_type=password") {
                header("apikey", SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val text = response.bodyAsText()
            Logger.d(TAG, "signIn response status: ${response.status}")

            if (response.status.isSuccess()) {
                val jsonObject = json.parseToJsonElement(text).jsonObject
                val token = jsonObject["access_token"]?.jsonPrimitive?.content
                val refreshToken = jsonObject["refresh_token"]?.jsonPrimitive?.content
                val userObj = jsonObject["user"]?.jsonObject
                val userId = userObj?.get("id")?.jsonPrimitive?.content
                val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: email.trim()

                dataStoreManager.setSupabaseAuth(token, userEmail, userId, refreshToken)
                userEmail
            } else {
                val errorMsg = runCatching {
                    val errObj = json.parseToJsonElement(text).jsonObject
                    errObj["msg"]?.jsonPrimitive?.content
                        ?: errObj["error_description"]?.jsonPrimitive?.content
                        ?: "Login failed"
                }.getOrDefault("Login failed")
                error(errorMsg)
            }
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        return runCatching {
            val body = buildJsonObject {
                put("email", email.trim())
                put("password", password)
            }.toString()

            val response = client.post("$SUPABASE_URL/auth/v1/signup") {
                header("apikey", SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val text = response.bodyAsText()
            Logger.d(TAG, "signUp response status: ${response.status}")

            if (response.status.isSuccess()) {
                val jsonObject = json.parseToJsonElement(text).jsonObject
                val userObj = jsonObject["user"]?.jsonObject ?: jsonObject
                val userId = userObj["id"]?.jsonPrimitive?.content ?: "user_${email.trim().hashCode()}"
                val userEmail = userObj["email"]?.jsonPrimitive?.content ?: email.trim()
                val token = jsonObject["access_token"]?.jsonPrimitive?.content
                    ?: jsonObject["session"]?.jsonObject?.get("access_token")?.jsonPrimitive?.content
                    ?: "pulse_session_${userId}"
                val refreshToken = jsonObject["refresh_token"]?.jsonPrimitive?.content
                    ?: jsonObject["session"]?.jsonObject?.get("refresh_token")?.jsonPrimitive?.content

                dataStoreManager.setSupabaseAuth(token, userEmail, userId, refreshToken)
                userEmail
            } else {
                val errorMsg = runCatching {
                    val errObj = json.parseToJsonElement(text).jsonObject
                    errObj["msg"]?.jsonPrimitive?.content
                        ?: errObj["error_description"]?.jsonPrimitive?.content
                        ?: "Sign up failed"
                }.getOrDefault("Sign up failed")
                error(errorMsg)
            }
        }
    }

    override suspend fun signInWithGoogle(email: String, name: String?): Result<String> {
        return runCatching {
            val cleanEmail = email.trim()
            val userId = "google_${cleanEmail.hashCode()}"
            val sessionToken = "pulse_google_session_${userId}"

            dataStoreManager.setSupabaseAuth(
                token = sessionToken,
                email = cleanEmail,
                userId = userId,
                refreshToken = null,
            )
            if (!name.isNullOrBlank()) {
                dataStoreManager.setProfile(
                    name = name.trim(),
                    age = dataStoreManager.profileAge.firstOrNull() ?: "",
                    gender = dataStoreManager.profileGender.firstOrNull() ?: "Male",
                    languagePreference = dataStoreManager.profileLanguagePreference.firstOrNull() ?: "Tamil (தமிழ்)",
                )
            }
            cleanEmail
        }
    }

    override suspend fun signOut() {
        dataStoreManager.setSupabaseAuth(null, null, null, null)
    }

    override suspend fun refreshSession(): Result<Boolean> {
        return runCatching {
            val refreshToken = dataStoreManager.supabaseRefreshToken.firstOrNull()
            if (refreshToken.isNullOrBlank()) {
                error("No refresh token available")
            }
            val body = buildJsonObject {
                put("refresh_token", refreshToken)
            }.toString()

            val response = client.post("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token") {
                header("apikey", SUPABASE_KEY)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val text = response.bodyAsText()
            Logger.d(TAG, "refreshSession response status: ${response.status}")

            if (response.status.isSuccess()) {
                val jsonObject = json.parseToJsonElement(text).jsonObject
                val newToken = jsonObject["access_token"]?.jsonPrimitive?.content
                val newRefreshToken = jsonObject["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
                val userObj = jsonObject["user"]?.jsonObject
                val userId = userObj?.get("id")?.jsonPrimitive?.content ?: dataStoreManager.supabaseUserId.firstOrNull()
                val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: dataStoreManager.supabaseUserEmail.firstOrNull()

                dataStoreManager.setSupabaseAuth(newToken, userEmail, userId, newRefreshToken)
                true
            } else {
                Logger.w(TAG, "Refresh token rejected (${response.status}). Invaliding expired session.")
                signOut()
                error("Session expired, signed out")
            }
        }
    }

    private suspend fun performAuthenticatedRequest(
        actionName: String,
        block: suspend (userId: String, token: String) -> HttpResponse,
    ) {
        val userId = dataStoreManager.supabaseUserId.firstOrNull()
        var token = dataStoreManager.supabaseSessionToken.firstOrNull()

        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            Logger.d(TAG, "$actionName skipped: user is not authenticated")
            return
        }

        runCatching {
            var response = block(userId, token)
            if (response.status.value == 401) {
                Logger.d(TAG, "$actionName: 401 received, attempting token refresh...")
                val refreshResult = refreshSession()
                if (refreshResult.isSuccess) {
                    val freshToken = dataStoreManager.supabaseSessionToken.firstOrNull()
                    if (!freshToken.isNullOrBlank()) {
                        response = block(userId, freshToken)
                    }
                }
            }

            if (!response.status.isSuccess()) {
                Logger.w(TAG, "$actionName failed with status: ${response.status}")
            } else {
                Logger.d(TAG, "$actionName succeeded with status: ${response.status}")
            }
        }.onFailure {
            Logger.w(TAG, "$actionName encountered error: ${it.message}")
        }
    }

    override suspend fun syncLikedSong(song: SongEntity, liked: Boolean) {
        scope.launch {
            performAuthenticatedRequest("syncLikedSong") { userId, token ->
                if (liked) {
                    val payload = buildJsonObject {
                        put("user_id", userId)
                        put("song_id", song.videoId)
                        put("song_data", buildJsonObject {
                            put("id", song.videoId)
                            put("title", song.title)
                            put("artist", song.artistName?.joinToString(", ") ?: "")
                            put("coverUrl", song.thumbnails ?: "")
                            put("duration", song.durationSeconds)
                        })
                    }.toString()

                    client.post("$SUPABASE_URL/rest/v1/user_liked_songs") {
                        header("apikey", SUPABASE_KEY)
                        header("Authorization", "Bearer $token")
                        header("Prefer", "resolution=merge-duplicates")
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                } else {
                    client.delete("$SUPABASE_URL/rest/v1/user_liked_songs?user_id=eq.$userId&song_id=eq.${song.videoId}") {
                        header("apikey", SUPABASE_KEY)
                        header("Authorization", "Bearer $token")
                    }
                }
            }
        }
    }

    override suspend fun syncPlaylist(playlist: LocalPlaylistEntity, tracks: List<String>) {
        scope.launch {
            performAuthenticatedRequest("syncPlaylist") { userId, token ->
                val payload = buildJsonObject {
                    put("id", "pl_${playlist.id}")
                    put("user_id", userId)
                    put("name", playlist.title)
                    put("description", "")
                    put("songs", buildJsonArray {
                        tracks.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                    })
                }.toString()

                client.post("$SUPABASE_URL/rest/v1/user_playlists") {
                    header("apikey", SUPABASE_KEY)
                    header("Authorization", "Bearer $token")
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
        }
    }

    override suspend fun syncListenCount(videoId: String, song: SongEntity?) {
        scope.launch {
            performAuthenticatedRequest("syncListenCount") { userId, token ->
                val payload = buildJsonObject {
                    put("id", "${videoId}_$userId")
                    put("user_id", userId)
                    put("song_id", videoId)
                    put("song_data", buildJsonObject {
                        put("id", videoId)
                        put("title", song?.title ?: videoId)
                        put("artist", song?.artistName?.joinToString(", ") ?: "")
                        put("coverUrl", song?.thumbnails ?: "")
                    })
                    put("play_count", 1)
                }.toString()

                client.post("$SUPABASE_URL/rest/v1/user_listen_history") {
                    header("apikey", SUPABASE_KEY)
                    header("Authorization", "Bearer $token")
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
        }
    }

    override suspend fun syncAnalyticsEvent(eventType: String, eventData: Map<String, String>) {
        scope.launch {
            performAuthenticatedRequest("syncAnalyticsEvent") { userId, token ->
                val payload = buildJsonObject {
                    put("user_id", userId)
                    put("event_type", eventType)
                    put("metadata", buildJsonObject {
                        eventData.forEach { (k, v) -> put(k, v) }
                    })
                }.toString()

                client.post("$SUPABASE_URL/rest/v1/user_analytics") {
                    header("apikey", SUPABASE_KEY)
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
        }
    }

    override suspend fun syncAll(): Result<Unit> {
        return runCatching {
            performAuthenticatedRequest("syncAll") { userId, token ->
                val payload = buildJsonObject {
                    put("user_id", userId)
                    put("event_type", "full_background_sync")
                    put("metadata", buildJsonObject {
                        put("source", "pulse_music_sync_service")
                    })
                }.toString()

                client.post("$SUPABASE_URL/rest/v1/user_analytics") {
                    header("apikey", SUPABASE_KEY)
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
            Unit
        }
    }
}
