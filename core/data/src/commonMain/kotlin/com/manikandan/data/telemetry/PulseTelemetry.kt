package com.manikandan.data.telemetry

import com.manikandan.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * PulseTelemetry — Lightweight, Zero-PII In-App Telemetry Client
 *
 * Transmits strictly non-personally-identifiable usage metrics to the owner-only
 * dashboard (https://pulsemusic-landing.vercel.app/owner-telemetry.html, PIN: 2005).
 *
 * Data sent: song plays, search queries, stream requests, session start/end.
 * Data NOT sent: real names, emails, phone numbers, real IPs, passwords, tokens.
 *
 * All transmissions are:
 *   - Asynchronous (fire-and-forget, never blocks UI)
 *   - Fail-safe (exception swallowed silently)
 *   - Minimal (no polling, no continuous streaming)
 *
 * Version: 4.1.7 — Live data only, persisted to private GitHub Gist store.
 */
object PulseTelemetry {

    const val APP_VERSION = "4.1.7"

    private const val ENDPOINT = "https://pulsemusic-landing.vercel.app/api/track"

    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient by lazy {
        HttpClient(getEngine()) {
            followRedirects = true
            expectSuccess  = false
        }
    }

    // Anonymous per-session hex ID — regenerated each app launch. Zero-PII.
    private var sessionUserId: String? = null

    fun getOrGenerateUserId(): String {
        sessionUserId?.let { return it }
        val chars  = "0123456789abcdef"
        val suffix = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val id     = "usr_$suffix"
        sessionUserId = id
        return id
    }

    private fun escapeJson(str: String): String = str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun post(jsonBody: String) {
        scope.launch {
            runCatching {
                httpClient.post(ENDPOINT) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }
            }
            // Fire-and-forget: errors are intentionally swallowed
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Track a song play event.
     * Called when user listens to ≥ 30 s of a track.
     */
    fun trackSongPlay(
        title:           String,
        artist:          String,
        durationSeconds: Long,
        listenedSeconds: Long,
        isFlac:          Boolean,
        device:          String = "Android Device",
        os:              String = "Android",
        appVersion:      String = APP_VERSION,
        userId:          String? = null,
    ) {
        val uid  = userId ?: getOrGenerateUserId()
        val json = """{"type":"song_play","userId":"$uid","title":"${escapeJson(title.take(80))}","artist":"${escapeJson(artist.take(60))}","durationSeconds":$durationSeconds,"listenedSeconds":$listenedSeconds,"isFlac":$isFlac,"device":"${escapeJson(device.take(50))}","os":"${escapeJson(os.take(30))}","appVersion":"$appVersion"}"""
        post(json)
    }

    /**
     * Track a search query.
     * Called when the user submits a search term.
     */
    fun trackSearch(
        query:        String,
        category:     String = "SONG",
        resultsCount: Int    = 0,
        appVersion:   String = APP_VERSION,
        userId:       String? = null,
    ) {
        if (query.isBlank()) return
        val uid  = userId ?: getOrGenerateUserId()
        val json = """{"type":"search_query","userId":"$uid","query":"${escapeJson(query.take(100))}","category":"${escapeJson(category.take(30))}","resultsCount":$resultsCount,"appVersion":"$appVersion"}"""
        post(json)
    }

    /**
     * Track a stream/audio request.
     * Called when the app fetches an audio stream URL.
     */
    fun trackStreamRequest(
        endpoint:   String,
        format:     String = "AUDIO",
        latencyMs:  Long   = 0L,
        status:     Int    = 200,
        appVersion: String = APP_VERSION,
        userId:     String? = null,
    ) {
        val uid  = userId ?: getOrGenerateUserId()
        val json = """{"type":"stream_request","userId":"$uid","endpoint":"${escapeJson(endpoint.take(80))}","format":"${escapeJson(format.take(40))}","latencyMs":$latencyMs,"status":$status,"appVersion":"$appVersion"}"""
        post(json)
    }

    /**
     * Track an app session.
     * Call at session start (sessionMinutes=0) and again at session end with real duration.
     */
    fun trackSession(
        device:         String = "Android Device",
        os:             String = "Android",
        arch:           String = "arm64-v8a",
        sessionMinutes: Int    = 1,
        appVersion:     String = APP_VERSION,
        userId:         String? = null,
    ) {
        val uid  = userId ?: getOrGenerateUserId()
        val json = """{"type":"app_usage","userId":"$uid","device":"${escapeJson(device.take(50))}","os":"${escapeJson(os.take(30))}","arch":"${escapeJson(arch.take(20))}","sessionMinutes":$sessionMinutes,"appVersion":"$appVersion"}"""
        post(json)
    }
}
