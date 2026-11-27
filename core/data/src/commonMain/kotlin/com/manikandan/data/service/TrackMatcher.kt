package com.manikandan.data.service

import com.manikandan.domain.data.model.searchResult.songs.SongsResult
import com.manikandan.domain.extension.decodeHtmlEntities
import kotlin.math.abs

/**
 * Shared track matching and ranking utility so Spotify/Saavn/YouTube/Canvas/Lyrics cross-source
 * resolution handles dual-title tracks, phonetic transliterations, album hints, and artist checks
 * without ever playing or displaying a wrong song.
 */
internal object TrackMatcher {
    private const val MIN_ACCEPTABLE_SCORE = 0.52

    private val parenSuffixRegex =
        Regex("""[\(\[](?:feat\.?|ft\.?|with|prod\.?|explicit|clean|remaster(?:ed)?|official|audio|video|lyric|lyrics|visualizer|hd|hq|4k|from\s+["']?[^\)\]]*|original\s+motion\s+picture.*?|soundtrack.*?|version|edit|mix|ost).*?[\)\]]""", RegexOption.IGNORE_CASE)
    private val dashSuffixRegex =
        Regex("""\s+-\s+(?:from\s+["']?.*|remaster(?:ed)?.*|single\s+version.*|radio\s+edit.*|explicit.*)$""", RegexOption.IGNORE_CASE)
    private val featInlineRegex =
        Regex("""\s+(?:feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE)
    private val fromMovieExtractRegex =
        Regex("""(?i)(?:\(|\[|-\s+)from\s+["']?([^"'\)\]]+)["']?[\)\]]?""")
    private val dualTitleSplitRegex = Regex("""\s+(?:-|/|\\|\|)\s+""")
    private val nonAlnumRegex = Regex("""[^\p{L}\p{N}\s]""")
    private val multiSpaceRegex = Regex("""\s+""")

    private val variantTokens = setOf(
        "remix", "lofi", "slowed", "reverb", "sped", "instrumental",
        "karaoke", "cover", "acoustic", "live", "unplugged", "mashup",
        "nightcore", "8d", "bass", "ringtone", "bgm", "teaser",
        "trailer", "whatsapp", "status", "parody", "tribute",
    )

    private val subtitleNoiseTokens = setOf(
        "from", "original", "motion", "picture", "soundtrack", "ost",
        "single", "version", "edit", "explicit", "clean", "remastered",
        "remaster", "official", "audio", "video", "lyric", "lyrics", "visualizer",
    )

    private val artistSplitRegex = Regex("""[,&×/•·]|(?:\s+(?:feat\.?|ft\.?|with|x|and)\s+)""", RegexOption.IGNORE_CASE)
    private val artistNoiseRegex = Regex("""\b(?:topic|vevo|official|music)\b""", RegexOption.IGNORE_CASE)

    fun normalizeTitle(raw: String): String {
        val decoded = decodeHtmlEntities(raw)
        var s = parenSuffixRegex.replace(decoded, " ")
        s = dashSuffixRegex.replace(s, " ")
        s = featInlineRegex.replace(s, " ")
        return normalizeRawSegment(s)
    }

    private fun normalizeRawSegment(raw: String): String {
        val cleaned = nonAlnumRegex.replace(raw, " ")
        return multiSpaceRegex.replace(cleaned.trim(), " ").lowercase()
    }

    /**
     * Generates normalized full title plus meaningful dual-title sub-segments
     * (e.g. `"Arabic Kuthu - Halamithi Habibo (From \"Beast\")"` ->
     * `["arabic kuthu halamithi habibo", "arabic kuthu", "halamithi habibo"]`).
     */
    fun titleVariants(raw: String): List<String> {
        val decoded = decodeHtmlEntities(raw)
        var stripped = parenSuffixRegex.replace(decoded, " ")
        stripped = dashSuffixRegex.replace(stripped, " ")
        stripped = featInlineRegex.replace(stripped, " ").trim()

        val full = normalizeRawSegment(stripped)
        if (full.isBlank()) return emptyList()

        val variants = linkedSetOf(full)
        val parts = stripped.split(dualTitleSplitRegex)
        if (parts.size >= 2) {
            for (part in parts) {
                val pNorm = normalizeRawSegment(part)
                if (pNorm.length < 3 || pNorm == full) continue
                val pTokens = tokenize(pNorm)
                if (pTokens.isNotEmpty() && !pTokens.all { it in subtitleNoiseTokens }) {
                    variants.add(pNorm)
                }
            }
        }
        return variants.toList()
    }

    fun extractAlbumHintFromTitle(raw: String): String? {
        val decoded = decodeHtmlEntities(raw)
        val match = fromMovieExtractRegex.find(decoded) ?: return null
        return normalizeRawSegment(match.groupValues[1]).takeIf { it.isNotBlank() }
    }

    fun normalizeArtist(raw: String): String {
        val decoded = decodeHtmlEntities(raw)
        val noNoise = artistNoiseRegex.replace(decoded, " ")
        return multiSpaceRegex.replace(nonAlnumRegex.replace(noNoise, " ").trim(), " ").lowercase()
    }

    fun tokenize(normalized: String): Set<String> =
        normalized.split(' ').filter { it.isNotBlank() }.toSet()

    fun extractArtistTokens(rawArtists: String?): Set<String> {
        if (rawArtists.isNullOrBlank()) return emptySet()
        val out = linkedSetOf<String>()
        rawArtists.split(artistSplitRegex).forEach { part ->
            val norm = normalizeArtist(part)
            if (norm.isNotBlank()) {
                out.add(norm)
                norm.split(' ').filter { it.length >= 2 }.forEach { out.add(it) }
            }
        }
        return out
    }

    private fun canonicalizePhonetic(token: String): String {
        val collapsedDigraphs = token
            .lowercase()
            .replace("aa", "a")
            .replace("ee", "i")
            .replace("ii", "i")
            .replace("oo", "u")
            .replace("uu", "u")
            .replace("th", "t")
            .replace("dh", "d")
            .replace("bh", "b")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("ph", "f")
        val sb = StringBuilder(collapsedDigraphs.length)
        var prev = '\u0000'
        for (ch in collapsedDigraphs) {
            if (ch != prev) {
                sb.append(ch)
                prev = ch
            }
        }
        return sb.toString()
    }

    private fun damerauLevenshteinAtMost1(a: String, b: String): Boolean {
        if (a == b) return true
        if (abs(a.length - b.length) > 1) return false
        if (a.length == b.length) {
            var diff1 = -1
            var diff2 = -1
            for (i in a.indices) {
                if (a[i] != b[i]) {
                    if (diff1 == -1) diff1 = i
                    else if (diff2 == -1) diff2 = i
                    else return false
                }
            }
            if (diff2 == -1) return true
            return diff2 == diff1 + 1 && a[diff1] == b[diff2] && a[diff2] == b[diff1]
        }
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length < b.length) b else a
        var i = 0
        var j = 0
        var skipped = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else if (!skipped) {
                skipped = true
                j++
            } else {
                return false
            }
        }
        return true
    }

    private fun tokenSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val minLen = minOf(a.length, b.length)
        if (minLen < 5) return 0.0
        if (canonicalizePhonetic(a) == canonicalizePhonetic(b)) return 0.92
        val sameStart = a[0] == b[0]
        if ((sameStart || minLen >= 7) && damerauLevenshteinAtMost1(a, b)) {
            return 0.90
        }
        return 0.0
    }

    private fun softIntersectionWeight(expTokens: List<String>, candTokens: List<String>): Double {
        if (expTokens.isEmpty() || candTokens.isEmpty()) return 0.0
        val usedCand = BooleanArray(candTokens.size)
        var total = 0.0
        for (e in expTokens) {
            var bestIdx = -1
            var bestWeight = 0.0
            for (j in candTokens.indices) {
                if (usedCand[j]) continue
                val sim = tokenSimilarity(e, candTokens[j])
                if (sim > bestWeight) {
                    bestWeight = sim
                    bestIdx = j
                }
            }
            if (bestIdx >= 0 && bestWeight > 0.0) {
                usedCand[bestIdx] = true
                total += bestWeight
            }
        }
        return total
    }

    /**
     * Returns `-1.0` when [candidateTitle] / [candidateArtists] clearly refer to a different
     * song from [expectedTitle] / [expectedArtist], or a confidence score `[0.0 .. 1.4]` otherwise.
     */
    fun scoreCandidate(
        expectedTitle: String,
        expectedArtist: String?,
        candidateTitle: String,
        candidateArtists: String?,
        expectedDurationSec: Int? = null,
        candidateDurationSec: Int? = null,
        expectedAlbum: String? = null,
        candidateAlbum: String? = null,
    ): Double {
        val expVariants = titleVariants(expectedTitle)
        val candVariants = titleVariants(candidateTitle)
        if (expVariants.isEmpty() || candVariants.isEmpty()) return -1.0

        val expFull = expVariants.first()
        val candFull = candVariants.first()
        val expFullTokens = tokenize(expFull)

        var bestTitleScore = -1.0
        var bestExact = false
        for ((ei, ev) in expVariants.withIndex()) {
            val evList = ev.split(' ').filter { it.isNotBlank() }
            if (evList.isEmpty()) continue
            for ((ci, cv) in candVariants.withIndex()) {
                val cvList = cv.split(' ').filter { it.isNotBlank() }
                if (cvList.isEmpty()) continue

                val exact = ev == cv
                val weight = softIntersectionWeight(evList, cvList)
                val recall = weight / evList.size.toDouble()
                val precision = weight / cvList.size.toDouble()
                val containsCleanly =
                    (cv.contains(ev) || ev.contains(cv)) &&
                        minOf(ev.length, cv.length) >= 4 &&
                        recall >= 0.65

                if (!exact && !containsCleanly && (recall < 0.65 || precision < 0.45)) {
                    continue
                }
                val subPenalty = if (ei > 0 || ci > 0) 0.03 else 0.0
                val candidateScore =
                    (when {
                        exact -> 0.72
                        containsCleanly -> 0.56 * recall + 0.10 * precision
                        else -> 0.50 * recall + 0.14 * precision
                    }) - subPenalty

                if (candidateScore > bestTitleScore) {
                    bestTitleScore = candidateScore
                    bestExact = exact && ei == 0 && ci == 0
                }
            }
        }
        if (bestTitleScore < 0.0) return -1.0

        // Reject unwanted remix/lofi/karaoke/cover/ringtone substitutions unless requested
        val rawExpLower = decodeHtmlEntities(expectedTitle).lowercase()
        val rawCandLower = decodeHtmlEntities(candidateTitle).lowercase()
        var variantPenalty = 0.0
        for (variant in variantTokens) {
            if (Regex("""\b${Regex.escape(variant)}\b""").containsMatchIn(rawCandLower) &&
                !Regex("""\b${Regex.escape(variant)}\b""").containsMatchIn(rawExpLower)
            ) {
                variantPenalty = 0.45
                break
            }
        }

        // Artist matching
        val expArtistTokens = extractArtistTokens(expectedArtist)
        val candArtistTokens = extractArtistTokens(candidateArtists)
        var artistScore = 0.0
        if (expArtistTokens.isNotEmpty() && candArtistTokens.isNotEmpty()) {
            val sharedArtist = expArtistTokens.count { it in candArtistTokens }
            artistScore =
                if (sharedArtist > 0) {
                    (sharedArtist.toDouble() / minOf(expArtistTokens.size, 3).toDouble()).coerceAtMost(1.0) * 0.28
                } else {
                    val artistInTitle = expArtistTokens.any { it.length >= 3 && rawCandLower.contains(it) }
                    if (artistInTitle) {
                        0.15
                    } else if (!bestExact || expFullTokens.size <= 2) {
                        return -1.0
                    } else {
                        -0.38
                    }
                }
        }

        // Album / Movie soundtrack consistency
        var albumScore = 0.0
        val effExpectedAlbum =
            expectedAlbum?.let { normalizeTitle(it) }?.takeIf { it.isNotBlank() && it != "album" }
                ?: extractAlbumHintFromTitle(expectedTitle)
        if (!effExpectedAlbum.isNullOrBlank()) {
            val candAlbumNorm = candidateAlbum?.let { normalizeTitle(it) }?.takeIf { it.isNotBlank() && it != "album" }.orEmpty()
            val expAlbTokens = tokenize(effExpectedAlbum).toList()
            val candAlbTokens = tokenize(candAlbumNorm).toList()
            if (expAlbTokens.isNotEmpty()) {
                val candCombinedAlb = if (candAlbTokens.isNotEmpty()) candAlbTokens else tokenize(candFull).toList()
                val albOverlap = softIntersectionWeight(expAlbTokens, candCombinedAlb) / expAlbTokens.size.toDouble()
                if (albOverlap >= 0.70) {
                    albumScore = 0.12
                } else if (candAlbTokens.isNotEmpty() && !candAlbTokens.containsAll(expFullTokens)) {
                    albumScore = -0.06
                }
            }
        }

        // Duration consistency
        var durationScore = 0.0
        if (expectedDurationSec != null && expectedDurationSec > 0 && candidateDurationSec != null && candidateDurationSec > 0) {
            val diff = abs(expectedDurationSec - candidateDurationSec)
            when {
                diff <= 5 -> durationScore = 0.14
                diff <= 12 -> durationScore = 0.06
                diff > 40 -> return -1.0
                diff > 20 -> durationScore = -0.22
            }
        }

        return (bestTitleScore + artistScore + albumScore + durationScore - variantPenalty)
    }

    fun isAcceptableMatch(score: Double): Boolean = score >= MIN_ACCEPTABLE_SCORE

    /**
     * Ranks combined cross-provider search results (e.g. YouTube + JioSaavn) so exact/close
     * matches to [query] always rank ahead of unrelated top-hit results.
     */
    fun rankSearchResults(query: String, candidates: List<SongsResult>): List<SongsResult> {
        if (candidates.size <= 1) return candidates
        val qNorm = normalizeTitle(query)
        val qTokens = qNorm.split(' ').filter { it.isNotBlank() }
        if (qTokens.isEmpty()) return candidates

        return candidates.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<SongsResult>> { iv ->
                    val item = iv.value
                    val itemTitle = item.title.orEmpty()
                    val tVariants = titleVariants(itemTitle)
                    val tNorm = tVariants.firstOrNull().orEmpty()
                    val aNorm = item.artists?.joinToString(" ") { normalizeArtist(it.name) }.orEmpty()
                    val albNorm = item.album?.name?.let { normalizeTitle(it) }.orEmpty()
                    val combinedTokens = tokenize("$tNorm $aNorm $albNorm").toList()
                    val matchedWeight = softIntersectionWeight(qTokens, combinedTokens)
                    val tokenCoverage = matchedWeight / qTokens.size.toDouble()
                    val exactTitle = tVariants.any { it == qNorm }
                    val ytSongBonus = if (!item.videoId.startsWith("saavn:") && matchedWeight > 0.0) 0.03 else 0.0
                    var penalty = 0.0
                    val rawTitleLower = decodeHtmlEntities(itemTitle).lowercase()
                    val rawQueryLower = query.lowercase()
                    for (variant in variantTokens) {
                        if (Regex("""\b${Regex.escape(variant)}\b""").containsMatchIn(rawTitleLower) &&
                            !Regex("""\b${Regex.escape(variant)}\b""").containsMatchIn(rawQueryLower)
                        ) {
                            penalty = 0.35
                            break
                        }
                    }
                    (if (exactTitle) 1.0 else 0.0) + tokenCoverage * 0.8 + ytSongBonus - penalty
                }.thenBy { it.index },
            ).map { it.value }
    }
}
