package com.manikandan.spotify.auth

import com.manikandan.spotify.SpotApiBundleSession
import com.manikandan.spotify.SpotifyClient
import com.manikandan.spotify.model.response.spotify.PersonalTokenResponse
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Handles Spotify Web Player TOTP authorization cleanly using SpotAPI's live bundle secret
 * extraction first, falling back silently to the obfuscated vault without emitting secrets to logs.
 */
class SpotifyAuth internal constructor(
    private val spotifyClient: SpotifyClient,
    private val bundleSession: SpotApiBundleSession? = null,
) {
    constructor(spotifyClient: SpotifyClient) : this(spotifyClient, null)
    var totpSecret: Pair<Int, List<Int>>? = null

    suspend fun getTotpSecret(): Result<Boolean> =
        runCatching {
            val bundleTotp = bundleSession?.warmIfNeeded()?.totpSecret
            if (bundleTotp != null) {
                totpSecret = bundleTotp
                return@runCatching true
            }
            val response = spotifyClient.getSpotifyLastestTotpSecret().body<Map<String, List<Int>>>()
            if (response.isNotEmpty()) {
                val latestDict = response.entries.last()
                val firstKey = latestDict.key.toInt()
                totpSecret = Pair(firstKey, latestDict.value)
                true
            } else {
                false
            }
        }

    /**
     * Refresh Spotify Web Player token using live bundle-extracted TOTP (or hidden vault fallback).
     * Accepts both authenticated (`sp_dc` cookie present) and anonymous public sessions (`sp_dc` empty).
     */
    suspend fun refreshToken(spDc: String): Result<PersonalTokenResponse> =
        runCatching {
            val cleanSpdc = spDc.removePrefix("sp_dc=").trim()
            val warmResult = runCatching { bundleSession?.warmIfNeeded(cleanSpdc) }.getOrNull()
            if (warmResult?.totpSecret != null) {
                totpSecret = warmResult.totpSecret
            } else if (totpSecret == null) {
                getTotpSecret()
            }

            val serverTimeResponse = spotifyClient.getSpotifyServerTime(cleanSpdc)
            val serverTimeJson = Json.parseToJsonElement(serverTimeResponse.body<String>()).jsonObject
            val serverTime =
                serverTimeJson["serverTime"]?.jsonPrimitive?.longOrNull
                    ?: throw Exception("Failed to obtain server time")

            val secretPair = totpSecret ?: SpotApiVault.fallbackTotpSecret()
            val otpValue = SpotifyTotp.at(serverTime * 1000L, secretPair)
            val sTime = "$serverTime"
            val cTime = "$serverTime"

            val attempts = listOf(
                "init" to "web-player",
                "transport" to "web-player",
                "transport" to "mobile-web-player",
                "init" to "mobile-web-player",
            )
            var resolved: PersonalTokenResponse? = null
            for ((reason, productType) in attempts) {
                val resp = runCatching {
                    spotifyClient.getSpotifyAccessToken(
                        spdc = cleanSpdc,
                        otpValue = otpValue,
                        reason = reason,
                        sTime = sTime,
                        cTime = cTime,
                        totpVersion = secretPair.first,
                        productType = productType,
                    ).body<PersonalTokenResponse>()
                }.getOrNull()
                if (resp != null && resp.accessToken.isNotBlank()) {
                    // Reject silent downgrade to anonymous only when caller explicitly supplied a cookie
                    // and an alternative transport might still succeed; accept as last resort otherwise.
                    if (cleanSpdc.isBlank() || !resp.isAnonymous) {
                        resolved = resp
                        break
                    } else if (resolved == null) {
                        resolved = resp
                    }
                }
            }

            if (resolved == null || resolved.accessToken.isBlank()) {
                val htmlToken = warmResult?.embeddedAccessToken?.takeIf { it.isNotBlank() }
                    ?: bundleSession?.warmIfNeeded(cleanSpdc, force = true)?.embeddedAccessToken
                if (!htmlToken.isNullOrBlank()) {
                    resolved = PersonalTokenResponse(
                        accessToken = htmlToken,
                        isAnonymous = cleanSpdc.isBlank(),
                        clientId = SpotApiVault.defaultClientId(),
                    )
                }
            }

            resolved?.takeIf { it.accessToken.isNotBlank() }
                ?: throw Exception("Unsuccessful Spotify token exchange")
        }
}
