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
 * PulseTelemetry - Lightweight, Zero-PII In-App Telemetry Client
 *
 * Transmits strictly non-personally identifiable usage metrics (songs listened,
 * search queries, stream requests, and session duration) to the owner-only dashboard.
 * All transmissions are asynchronous, fire-and-forget, and fail-safe.
 */
object PulseTelemetry {
    private const val ENDPOINT_VERCEL = "https://pulsemusic-landing.vercel.app/api/track"
    private const val ENDPOINT_NETLIFY = "https://pulsemusic-landing.vercel.app/.netlify/functions/track"

    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient by lazy {
        HttpClient(getEngine()) {
            followRedirects = true
            expectSuccess = false
        }
    }

    private var memoryUserId: String? = null

    fun getOrGenerateUserId(): String {
        memoryUserId?.let { return it }
        val chars = "0123456789abcdef"
        val suffix = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val id = "usr_$suffix"
        memoryUserId = id
        return id
    }

    fun setUserId(id: String) {
        if (id.isNotBlank()) {
            memoryUserId = id
        }
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun postTelemetry(jsonBody: String) {
        scope.launch {
            runCatching {
                try {
                    httpClient.post(ENDPOINT_VERCEL) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                } catch (_: Throwable) {
                    httpClient.post(ENDPOINT_NETLIFY) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                }
            }
        }
    }

    fun trackSongPlay(
        title: String,
        artist: String,
        durationSeconds: Long,
        listenedSeconds: Long,
        isFlac: Boolean,
        device: String = "Android Device",
        os: String = "Android",
        appVersion: String = "4.1.5",
        userId: String? = null,
    ) {
        val uid = userId ?: getOrGenerateUserId()
        val json = """{"type":"song_play","userId":"$uid","title":"${escapeJson(title.take(80))}","artist":"${escapeJson(artist.take(60))}","durationSeconds":$durationSeconds,"listenedSeconds":$listenedSeconds,"isFlac":$isFlac,"device":"${escapeJson(device.take(50))}","os":"${escapeJson(os.take(30))}","appVersion":"$appVersion"}"""
        postTelemetry(json)
    }

    fun trackSearch(
        query: String,
        category: String = "SONG",
        resultsCount: Int = 0,
        appVersion: String = "4.1.5",
        userId: String? = null,
    ) {
        if (query.isBlank()) return
        val uid = userId ?: getOrGenerateUserId()
        val json = """{"type":"search_query","userId":"$uid","query":"${escapeJson(query.take(100))}","category":"${escapeJson(category.take(30))}","resultsCount":$resultsCount,"appVersion":"$appVersion"}"""
        postTelemetry(json)
    }

    fun trackStreamRequest(
        endpoint: String,
        format: String = "AUDIO",
        latencyMs: Long = 0L,
        status: Int = 200,
        appVersion: String = "4.1.5",
        userId: String? = null,
    ) {
        val uid = userId ?: getOrGenerateUserId()
        val json = """{"type":"stream_request","userId":"$uid","endpoint":"${escapeJson(endpoint.take(80))}","format":"${escapeJson(format.take(40))}","latencyMs":$latencyMs,"status":$status,"appVersion":"$appVersion"}"""
        postTelemetry(json)
    }

    fun trackSession(
        device: String = "Android Device",
        os: String = "Android",
        arch: String = "arm64-v8a",
        sessionMinutes: Int = 1,
        appVersion: String = "4.1.5",
        userId: String? = null,
    ) {
        val uid = userId ?: getOrGenerateUserId()
        val json = """{"type":"app_usage","userId":"$uid","device":"${escapeJson(device.take(50))}","os":"${escapeJson(os.take(30))}","arch":"${escapeJson(arch.take(20))}","sessionMinutes":$sessionMinutes,"appVersion":"$appVersion"}"""
        postTelemetry(json)
    }
}
