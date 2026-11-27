package com.manikandan.spotify

import com.manikandan.spotify.auth.SpotApiVault
import com.manikandan.spotify.auth.SpotifyTotp
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Discovers live Spotify Web Player configuration, TOTP cipher parameters, and
 * persisted Pathfinder GraphQL query hashes directly from Spotify's HTML and JS bundles
 * (matching SpotAPI's BundleSession). Falls back silently to the obfuscated vault when needed.
 */
internal class SpotApiBundleSession(
    private val client: SpotifyClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val lock = Mutex()

    private var clientVersion: String = SpotApiVault.defaultAppVersion()
    private var buildVersion: String = ""
    private var discoveredLinks: LinkedHashSet<String> = linkedSetOf()
    private val queryHashes: MutableMap<String, String> = mutableMapOf()
    private val fetchedChunkUrls: MutableSet<String> = mutableSetOf()
    private var discoveredTotp: Pair<Int, List<Int>>? = null
    private var lastWarmMark: TimeMark? = null
    private var discoveryExhausted: Boolean = false

    private var lastHtmlWarmMark: TimeMark? = null
    private var lastEmbeddedAccessToken: String? = null

    fun currentClientVersion(): String = clientVersion

    fun currentTotpSecret(): Pair<Int, List<Int>>? = discoveredTotp

    /**
     * Fast single-request HTML warm: extracts clientVersion, embedded WebPlayer accessToken (when sp_dc
     * cookie is set), and bundle chunk links in one lightweight HTTP GET without downloading JS bundles.
     */
    suspend fun warmHtmlQuick(spdc: String = "", force: Boolean = false): BundleWarmResult = lock.withLock {
        val htmlMark = lastHtmlWarmMark
        if (!force && htmlMark != null && htmlMark.elapsedNow().inWholeMinutes < 15 && discoveredLinks.isNotEmpty()) {
            return@withLock BundleWarmResult(
                clientVersion = clientVersion,
                totpSecret = discoveredTotp,
                embeddedAccessToken = lastEmbeddedAccessToken,
            )
        }
        val html = runCatching { client.getSpotifyOpenHomeHtml(spdc).bodyAsText() }.getOrNull()
        var embeddedToken: String? = null
        if (!html.isNullOrBlank()) {
            parseAppServerConfig(html)
            embeddedToken = extractEmbeddedAccessToken(html)
            extractHtmlJsLinks(html).forEach { discoveredLinks.add(it) }
            if (!embeddedToken.isNullOrBlank()) {
                lastEmbeddedAccessToken = embeddedToken
            }
        }
        lastHtmlWarmMark = TimeSource.Monotonic.markNow()
        BundleWarmResult(
            clientVersion = clientVersion,
            totpSecret = discoveredTotp,
            embeddedAccessToken = embeddedToken,
        )
    }

    suspend fun warmIfNeeded(spdc: String = "", force: Boolean = false): BundleWarmResult = lock.withLock {
        val mark = lastWarmMark
        if (!force && mark != null && mark.elapsedNow().inWholeMinutes < 45 && discoveredTotp != null) {
            return@withLock BundleWarmResult(
                clientVersion = clientVersion,
                totpSecret = discoveredTotp,
                embeddedAccessToken = lastEmbeddedAccessToken,
            )
        }

        val html = runCatching { client.getSpotifyOpenHomeHtml(spdc).bodyAsText() }.getOrNull()
        var embeddedToken: String? = null
        if (!html.isNullOrBlank()) {
            parseAppServerConfig(html)
            embeddedToken = extractEmbeddedAccessToken(html)
            extractHtmlJsLinks(html).forEach { discoveredLinks.add(it) }
            if (!embeddedToken.isNullOrBlank()) {
                lastEmbeddedAccessToken = embeddedToken
            }
        }
        lastHtmlWarmMark = TimeSource.Monotonic.markNow()

        val primaryUrls = discoveredLinks
            .filter { it.contains("/web-player.") }
            .ifEmpty { discoveredLinks.take(2) }

        if (primaryUrls.isNotEmpty()) {
            val bundles = fetchBundleTexts(primaryUrls.take(2))
            bundles.forEach { (url, source) ->
                fetchedChunkUrls.add(url)
                ingestBundleSource(url, source)
                if (url.contains("/web-player.")) {
                    extractWebpackChunkLinks(source).forEach { discoveredLinks.add(it) }
                }
            }
        }

        lastWarmMark = TimeSource.Monotonic.markNow()
        BundleWarmResult(
            clientVersion = clientVersion,
            totpSecret = discoveredTotp,
            embeddedAccessToken = embeddedToken,
        )
    }

    suspend fun resolveQueryHash(
        operationName: String,
        spdc: String = "",
        forceScan: Boolean = false,
    ): String? {
        if (!forceScan) {
            queryHashes[operationName]?.let { return it }
            SpotApiVault.fallbackQueryHash(operationName)?.let { return it }
        }

        warmIfNeeded(spdc, force = forceScan)
        if (!forceScan) {
            queryHashes[operationName]?.let { return it }
        }

        lock.withLock {
            if (!forceScan) {
                queryHashes[operationName]?.let { return it }
            }
            if (!discoveryExhausted || forceScan) {
                val pending = discoveredLinks
                    .filter { it !in fetchedChunkUrls }
                    .sortedByDescending { chunkRelevanceScore(it, operationName) }
                // Scan top relevant chunks in bounded batches so route queries (playlist/library/home/search)
                // resolve cleanly without downloading dozens of unrelated locale/UI chunks.
                val candidateBatches = pending.take(12).chunked(6)
                for (batch in candidateBatches) {
                    val fetched = fetchBundleTexts(batch)
                    fetched.forEach { (url, src) ->
                        fetchedChunkUrls.add(url)
                        ingestBundleSource(url, src)
                    }
                    queryHashes[operationName]?.let { return it }
                }
                if (pending.size <= 12) {
                    discoveryExhausted = true
                }
            }
        }
        return queryHashes[operationName] ?: SpotApiVault.fallbackQueryHash(operationName)
    }

    private suspend fun fetchBundleTexts(urls: List<String>): Map<String, String> = coroutineScope {
        urls.map { url ->
            async {
                val body = runCatching { client.getPublicText(url) }.getOrNull()
                if (!body.isNullOrBlank()) url to body else null
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private fun parseAppServerConfig(html: String) {
        val encoded = APP_CONFIG_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return
        val decodedJson = runCatching { Base64.decode(encoded).decodeToString() }.getOrNull() ?: return
        val obj = runCatching { json.parseToJsonElement(decodedJson).jsonObject }.getOrNull() ?: return
        obj["clientVersion"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let {
            clientVersion = it
        }
        obj["buildVersion"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let {
            if (buildVersion != it) {
                buildVersion = it
                discoveryExhausted = false
            }
        }
    }

    private fun extractEmbeddedAccessToken(html: String): String? {
        val raw = SESSION_SCRIPT_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim()
            ?: APP_CONFIG_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim()?.let { encoded ->
                runCatching { Base64.decode(encoded).decodeToString() }.getOrNull()
            }
        val token = raw?.let {
            runCatching {
                json.parseToJsonElement(it).jsonObject["accessToken"]?.jsonPrimitive?.content
            }.getOrNull()
        } ?: ACCESS_TOKEN_FALLBACK_REGEX.find(html)?.groupValues?.getOrNull(1)
        return token?.takeIf { it.isNotBlank() && it.length > 20 }
    }

    private fun extractHtmlJsLinks(html: String): List<String> =
        JS_LINK_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.startsWith(CDN_PREFIX) && it.contains("spotify") }
            .distinct()
            .toList()

    internal fun ingestBundleSource(url: String, source: String) {
        QUERY_HASH_REGEX.findAll(source).forEach { match ->
            val opName = match.groupValues[1]
            val hash = match.groupValues[2]
            if (opName.isNotBlank() && hash.length == 64) {
                queryHashes[opName] = hash
            }
        }
        if (url.contains("/web-player.")) {
            val foundSecrets = TOTP_SECRET_REGEX.findAll(source).mapNotNull { m ->
                val rawSecret = unescapeJsString(m.groupValues[2])
                val version = m.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                if (rawSecret.isNotEmpty()) {
                    version to SpotifyTotp.decodeRawSecretString(rawSecret)
                } else {
                    null
                }
            }.toList()
            val best = foundSecrets.maxByOrNull { it.first }
            if (best != null && (discoveredTotp == null || best.first >= discoveredTotp!!.first)) {
                discoveredTotp = best
            }
        }
    }

    internal fun extractWebpackChunkLinks(jsCode: String): List<String> {
        val loaderMatch = WEBPACK_LOADER_REGEX.find(jsCode) ?: return emptyList()
        val mapMatches = CHUNK_MAP_REGEX.findAll(jsCode, loaderMatch.range.last + 1).take(3).toList()
        if (mapMatches.size < 2) return emptyList()

        val parsedMaps = mapMatches.map { m ->
            CHUNK_ENTRY_REGEX.findAll(m.value).associate { entry ->
                entry.groupValues[1] to entry.groupValues[2]
            }
        }.filter { it.isNotEmpty() }
        if (parsedMaps.size < 2) return emptyList()

        val hashMap = parsedMaps.maxByOrNull { map ->
            map.values.count { HEX_HASH_REGEX.matches(it) }
        } ?: return emptyList()
        if (hashMap.values.none { HEX_HASH_REGEX.matches(it) }) return emptyList()

        val nameMap = parsedMaps
            .filter { it !== hashMap }
            .maxByOrNull { map -> (map.keys intersect hashMap.keys).size }
            ?: emptyMap()

        return hashMap.entries.map { (chunkId, chunkHash) ->
            val chunkName = nameMap[chunkId] ?: chunkId
            "${CDN_PREFIX}${chunkName}.${chunkHash}.js"
        }
    }

    private fun chunkRelevanceScore(url: String, operationName: String): Int {
        val lowerUrl = url.lowercase()
        val lowerOp = operationName.lowercase()
        var score = 0
        if (lowerUrl.contains("xpui-routes-")) score += 50
        if (lowerOp.contains("playlist") && lowerUrl.contains("playlist")) score += 100
        if (lowerOp.contains("library") && lowerUrl.contains("library")) score += 100
        if (lowerOp.contains("search") && lowerUrl.contains("search")) score += 100
        if (lowerOp.contains("recent") && (lowerUrl.contains("recent") || lowerUrl.contains("history") || lowerUrl.contains("library"))) score += 90
        if (lowerOp.contains("home") && lowerUrl.contains("home")) score += 100
        if (lowerOp.contains("artist") && lowerUrl.contains("artist")) score += 100
        if (lowerOp.contains("album") && lowerUrl.contains("album")) score += 100
        if (lowerOp.contains("profile") && (lowerUrl.contains("profile") || lowerUrl.contains("account") || lowerUrl.contains("user"))) score += 80
        if (lowerOp.contains("top") && (lowerUrl.contains("artist") || lowerUrl.contains("collection") || lowerUrl.contains("profile") || lowerUrl.contains("library"))) score += 70
        if (lowerUrl.contains("collection") || lowerUrl.contains("xpui-snapshot")) score += 40
        return score
    }

    private fun unescapeJsString(raw: String): String {
        if (!raw.contains('\\')) return raw
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (val next = raw[i + 1]) {
                    '\\' -> { sb.append('\\'); i += 2 }
                    '\'' -> { sb.append('\''); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'x' if (i + 3 < raw.length) -> {
                        val code = raw.substring(i + 2, i + 4).toIntOrNull(16)
                        if (code != null) { sb.append(code.toChar()); i += 4 } else { sb.append(c); i++ }
                    }
                    'u' if (i + 5 < raw.length) -> {
                        val code = raw.substring(i + 2, i + 6).toIntOrNull(16)
                        if (code != null) { sb.append(code.toChar()); i += 6 } else { sb.append(c); i++ }
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    internal data class BundleWarmResult(
        val clientVersion: String,
        val totpSecret: Pair<Int, List<Int>>?,
        val embeddedAccessToken: String?,
    )

    companion object {
        private const val CDN_PREFIX = "https://open.spotifycdn.com/cdn/build/web-player/"
        private val APP_CONFIG_REGEX = Regex("""<script id="appServerConfig" type="text/plain">([\s\S]*?)</script>""")
        private val SESSION_SCRIPT_REGEX = Regex("""<script id="session"[^>]*>([\s\S]*?)</script>""")
        private val ACCESS_TOKEN_FALLBACK_REGEX = Regex(""""accessToken"\s*:\s*"([^"]+)"""")
        private val JS_LINK_REGEX = Regex("""["'](https?://[^"']+\.js(?:\?[^"']*)?)["']""", RegexOption.IGNORE_CASE)
        private val QUERY_HASH_REGEX = Regex(""""([A-Za-z0-9_]+)"\s*,\s*"(?:query|mutation)"\s*,\s*"([0-9a-f]{64})"""")
        private val TOTP_SECRET_REGEX = Regex("""secret\s*:\s*(["'])(.*?)\1\s*,?\s*version\s*:\s*(\d+)""", RegexOption.DOT_MATCHES_ALL)
        private val WEBPACK_LOADER_REGEX = Regex("""\.u\s*=\s*(?:[\w$]+\s*=>|function\s*\()""")
        private val CHUNK_MAP_REGEX = Regex("""\{(\d+:"[^"]+"(?:,\d+:"[^"]+")*)\}""")
        private val CHUNK_ENTRY_REGEX = Regex("""(\d+):"([^"]+)"""")
        private val HEX_HASH_REGEX = Regex("""[0-9a-f]{8,}""")
    }
}
