package com.manikandan.data.service

import com.manikandan.ktorext.getEngine
import com.manikandan.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Invisible backend stream fallback resolver for JioSaavn.
 * When YouTube Music has missing songs, geo-restrictions, or playback errors,
 * this service automatically queries JioSaavn and resolves a direct, high-bitrate (320kbps/160kbps)
 * audio stream completely in the background without any UI exposure.
 */
object JioSaavnService {
    private const val TAG = "JioSaavnService"

    private val client by lazy {
        HttpClient(getEngine()) {
            expectSuccess = false
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Resolves a direct playable audio stream URL for a given song title and artist.
     */
    suspend fun resolveStream(title: String, artist: String? = null): String? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val cleanTitle = title
            .replace(Regex("""(?i)\(?(feat\.|ft\.|official|video|audio|lyric|remix|version)\)?.*"""), "")
            .trim()
        val cleanArtist = artist?.replace(Regex("""(?i)\b(topic|vevo)\b"""), "")?.trim()
        val query = if (!cleanArtist.isNullOrBlank()) "$cleanTitle $cleanArtist" else cleanTitle

        Logger.d(TAG, "Resolving fallback audio stream from JioSaavn for: $query")

        // 1. Try public cyberboysumanjay JioSaavn API / Saavn endpoints
        tryFromSaavnDev(query)
            ?: tryFromSaavnDirect(query)
            ?: tryFromVercelApi(query)
    }

    private suspend fun tryFromSaavnDev(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://saavn.dev/api/search/songs?query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonObject
                val results = data?.get("results")?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject
                val downloadUrls = firstSong?.get("downloadUrl")?.jsonArray
                // Prefer 320kbps or 160kbps
                val bestUrl = downloadUrls?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?: downloadUrls?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                if (!bestUrl.isNullOrBlank()) {
                    Logger.w(TAG, "Resolved stream via saavn.dev: $bestUrl")
                    return bestUrl
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "saavn.dev resolution skipped: ${e.message}")
            null
        }
    }

    private suspend fun tryFromSaavnDirect(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&n=5&p=1&q=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject

                // Check media_preview_url: replace preview.saavncdn with aac.saavncdn and _96_p.mp4 with _320.mp4
                val previewUrl = firstSong?.get("media_preview_url")?.jsonPrimitive?.content
                if (!previewUrl.isNullOrBlank()) {
                    val streamUrl = previewUrl
                        .replace("preview.saavncdn.com", "aac.saavncdn.com")
                        .replace("_96_p.mp4", "_320.mp4")
                    Logger.w(TAG, "Resolved stream via direct JioSaavn preview synthesis: $streamUrl")
                    return streamUrl
                }

                // Check encrypted_media_url if DES available
                val encrypted = firstSong?.get("encrypted_media_url")?.jsonPrimitive?.content
                if (!encrypted.isNullOrBlank()) {
                    val decrypted = decryptDes(encrypted)
                    if (!decrypted.isNullOrBlank()) {
                        val streamUrl = decrypted
                            .replace("_96.mp4", "_320.mp4")
                            .replace("_160.mp4", "_320.mp4")
                        Logger.w(TAG, "Resolved stream via JioSaavn DES decrypt: $streamUrl")
                        return streamUrl
                    }
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "jiosaavn.com direct resolution skipped: ${e.message}")
            null
        }
    }

    private suspend fun tryFromVercelApi(query: String): String? {
        return try {
            val encoded = query.encodeURLParameter()
            val url = "https://jiosaavn-api-privatetesting.vercel.app/search?query=$encoded"
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0")
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: root["data"]?.jsonArray
                val firstSong = results?.firstOrNull()?.jsonObject
                val mediaUrl = firstSong?.get("media_url")?.jsonPrimitive?.content
                    ?: firstSong?.get("download_url")?.jsonPrimitive?.content
                if (!mediaUrl.isNullOrBlank()) {
                    val highRes = mediaUrl.replace("_96.mp4", "_320.mp4")
                    Logger.w(TAG, "Resolved stream via vercel JioSaavn API: $highRes")
                    return highRes
                }
            }
            null
        } catch (e: Exception) {
            Logger.d(TAG, "vercel jiosaavn resolution skipped: ${e.message}")
            null
        }
    }

    private fun decryptDes(encryptedBase64: String): String? {
        return try {
            // JVM/Android standard DES decryption
            val cipherClass = Class.forName("javax.crypto.Cipher")
            val secretKeySpecClass = Class.forName("javax.crypto.spec.SecretKeySpec")
            val base64Class = Class.forName("java.util.Base64")

            val getDecoderMethod = base64Class.getMethod("getDecoder")
            val decoder = getDecoderMethod.invoke(null)
            val decodeMethod = decoder.javaClass.getMethod("decode", String::class.java)
            val encryptedBytes = decodeMethod.invoke(decoder, encryptedBase64) as ByteArray

            val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
            val keySpec = secretKeySpecClass.getConstructor(ByteArray::class.java, String::class.java)
                .newInstance(keyBytes, "DES")

            val getInstanceMethod = cipherClass.getMethod("getInstance", String::class.java)
            val cipher = getInstanceMethod.invoke(null, "DES/ECB/PKCS5Padding")

            val initMethod = cipher.javaClass.getMethod("init", Int::class.javaPrimitiveType, Class.forName("java.security.Key"))
            initMethod.invoke(cipher, 2 /* DECRYPT_MODE */, keySpec)

            val doFinalMethod = cipher.javaClass.getMethod("doFinal", ByteArray::class.java)
            val decryptedBytes = doFinalMethod.invoke(cipher, encryptedBytes) as ByteArray

            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            null
        }
    }
}
