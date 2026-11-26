package com.manikandan.data.service

import com.manikandan.domain.data.model.searchResult.songs.SongsResult
import com.manikandan.domain.extension.decodeHtmlEntities

/**
 * Shared track matching and ranking utility so Spotify/Saavn/YouTube cross-source
 * resolution never plays the wrong song for a given title + artist.
 */
internal object TrackMatcher {
    private const val MIN_ACCEPTABLE_SCORE = 0.52

    private val parenSuffixRegex =
        Regex("""[\(\[](?:feat\.?|ft\.?|with|prod\.?|explicit|clean|remaster(?:ed)?|official|audio|video|lyric|lyrics|visualizer|hd|hq|4k|from\s+["']?[^\)\]]*|version|edit|mix|ost).*?[\)\]]""", RegexOption.IGNORE_CASE)
    private val dashSuffixRegex =
        Regex("""\s+-\s+(?:from\s+["']?.*|remaster(?:ed)?.*|single\s+version.*|radio\s+edit.*|explicit.*)$""", RegexOption.IGNORE_CASE)
    private val featInlineRegex =
        Regex("""\s+(?:feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE)
    private val nonAlnumRegex = Regex("""[^\p{L}\p{N}\s]""")
    private val multiSpaceRegex = Regex("""\s+""")

    private val variantTokens = setOf(
        "remix", "lofi", "slowed", "reverb", "sped", "instrumental",
        "karaoke", "cover", "acoustic", "live", "unplugged", "mashup",
        "nightcore", "8d", "bass",
    )

    private val artistSplitRegex = Regex("""[,&×/•·]|(?:\s+(?:feat\.?|ft\.?|with|x|and)\s+)""", RegexOption.IGNORE_CASE)
    private val artistNoiseRegex = Regex("""\b(?:topic|vevo|official|music)\b""", RegexOption.IGNORE_CASE)

    fun normalizeTitle(raw: String): String {
        val decoded = decodeHtmlEntities(raw)
        var s = parenSuffixRegex.replace(decoded, " ")
        s = dashSuffixRegex.replace(s, " ")
        s = featInlineRegex.replace(s, " ")
        s = nonAlnumRegex.replace(s, " ")
        return multiSpaceRegex.replace(s.trim(), " ").lowercase()
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

    /**
     * Returns `-1.0` when [candidateTitle] / [candidateArtists] clearly refer to a different
     * song from [expectedTitle] / [expectedArtist], or a confidence score `[0.0 .. 1.3]` otherwise.
     */
    fun scoreCandidate(
        expectedTitle: String,
        expectedArtist: String?,
        candidateTitle: String,
        candidateArtists: String?,
        expectedDurationSec: Int? = null,
        candidateDurationSec: Int? = null,
    ): Double {
        val expTitleNorm = normalizeTitle(expectedTitle)
        val candTitleNorm = normalizeTitle(candidateTitle)
        if (expTitleNorm.isBlank() || candTitleNorm.isBlank()) return -1.0

        val expTokens = tokenize(expTitleNorm)
        val candTokens = tokenize(candTitleNorm)
        if (expTokens.isEmpty() || candTokens.isEmpty()) return -1.0

        val exactTitleMatch = expTitleNorm == candTitleNorm
        val intersectionCount = expTokens.count { it in candTokens }
        val recall = intersectionCount.toDouble() / expTokens.size.toDouble()
        val precision = intersectionCount.toDouble() / candTokens.size.toDouble()

        val containsCleanly =
            (candTitleNorm.contains(expTitleNorm) || expTitleNorm.contains(candTitleNorm)) &&
                minOf(expTitleNorm.length, candTitleNorm.length) >= 4 &&
                recall >= 0.65

        if (!exactTitleMatch && !containsCleanly && (recall < 0.65 || precision < 0.45)) {
            return -1.0
        }

        // Reject unwanted remix/lofi/karaoke/cover substitutions when the user/Spotify didn't ask for one
        val rawExpLower = decodeHtmlEntities(expectedTitle).lowercase()
        val rawCandLower = decodeHtmlEntities(candidateTitle).lowercase()
        var variantPenalty = 0.0
        for (variant in variantTokens) {
            if (rawCandLower.contains(variant) && !rawExpLower.contains(variant)) {
                variantPenalty = 0.35
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
                    // Check if candidateTitle itself embeds the expected artist (common on YouTube)
                    val candTitleLower = rawCandLower
                    val artistInTitle = expArtistTokens.any { it.length >= 3 && candTitleLower.contains(it) }
                    if (artistInTitle) {
                        0.15
                    } else if (!exactTitleMatch || expTokens.size <= 2) {
                        // Common/short titles with conflicting artists are almost always wrong songs
                        return -1.0
                    } else {
                        -0.25
                    }
                }
        }

        // Duration consistency
        var durationScore = 0.0
        if (expectedDurationSec != null && expectedDurationSec > 0 && candidateDurationSec != null && candidateDurationSec > 0) {
            val diff = kotlin.math.abs(expectedDurationSec - candidateDurationSec)
            when {
                diff <= 5 -> durationScore = 0.14
                diff <= 12 -> durationScore = 0.06
                diff > 40 -> return -1.0
                diff > 20 -> durationScore = -0.22
            }
        }

        val titleScore =
            when {
                exactTitleMatch -> 0.72
                containsCleanly -> 0.56 * recall + 0.10 * precision
                else -> 0.50 * recall + 0.14 * precision
            }

        return (titleScore + artistScore + durationScore - variantPenalty)
    }

    fun isAcceptableMatch(score: Double): Boolean = score >= MIN_ACCEPTABLE_SCORE

    /**
     * Ranks combined cross-provider search results (e.g. YouTube + JioSaavn) so exact/close
     * matches to [query] always rank ahead of unrelated top-hit results.
     */
    fun rankSearchResults(query: String, candidates: List<SongsResult>): List<SongsResult> {
        if (candidates.size <= 1) return candidates
        val qNorm = normalizeTitle(query)
        val qTokens = tokenize(qNorm)
        if (qTokens.isEmpty()) return candidates

        return candidates.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<SongsResult>> { iv ->
                    val item = iv.value
                    val itemTitle = item.title.orEmpty()
                    val tNorm = normalizeTitle(itemTitle)
                    val aNorm = item.artists?.joinToString(" ") { normalizeArtist(it.name) }.orEmpty()
                    val combinedTokens = tokenize("$tNorm $aNorm")
                    val matchedTokens = qTokens.count { it in combinedTokens }
                    val tokenCoverage = matchedTokens.toDouble() / qTokens.size.toDouble()
                    val exactTitle = tNorm == qNorm
                    val ytSongBonus = if (!item.videoId.startsWith("saavn:") && matchedTokens > 0) 0.03 else 0.0
                    var penalty = 0.0
                    val rawTitleLower = itemTitle.lowercase()
                    val rawQueryLower = query.lowercase()
                    for (variant in variantTokens) {
                        if (rawTitleLower.contains(variant) && !rawQueryLower.contains(variant)) {
                            penalty = 0.25
                            break
                        }
                    }
                    (if (exactTitle) 1.0 else 0.0) + tokenCoverage * 0.8 + ytSongBonus - penalty
                }.thenBy { it.index },
            ).map { it.value }
    }
}
