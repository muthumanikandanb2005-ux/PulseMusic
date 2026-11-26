package com.manikandan.spotify.auth

import io.ktor.utils.io.core.*
import kotlin.io.encoding.Base64

/**
 * Time-based One-Time Password generator for Spotify web-player authorization.
 * Implements live bundle cipher decoding matching SpotAPI without emitting logs.
 */
object SpotifyTotp {
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    val TOTP_SECRET_LATEST: Pair<Int, List<Int>>
        get() = SpotApiVault.fallbackTotpSecret()
    val TOTP_SECRET_V22: Pair<Int, List<Int>>
        get() = SpotApiVault.fallbackTotpSecret()

    fun decodeRawSecretString(rawSecret: String): List<Int> = rawSecret.map { it.code }

    fun at(timestamp: Long, totpSecret: Pair<Int, List<Int>>?): String = generate(timestamp, totpSecret)

    private fun generate(timestamp: Long, totpSecret: Pair<Int, List<Int>>?): String {
        val secret = generateSecret(totpSecret ?: TOTP_SECRET_LATEST)
        return generateTotp(secret, timestamp)
    }

    private fun generateSecret(totpSecret: Pair<Int, List<Int>>): String {
        val secretCipherBytes = totpSecret.second
        val transformed = secretCipherBytes.mapIndexed { index, byte ->
            byte xor ((index % 33) + 9)
        }
        val joined = transformed.joinToString("")
        val hexStr = joined.toByteArray().toHexString()
        return base64ToBase32(Base64.encode(hexStr.hexToByteArray())).trimEnd('=')
    }

    private fun base64ToBase32(base64: String): String = base32Encode(Base64.decode(base64))

    private fun base32Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val result = StringBuilder()
        var bits = 0
        var value = 0

        for (byte in data) {
            value = (value shl 8) or (byte.toInt() and 0xFF)
            bits += 8

            while (bits >= 5) {
                result.append(BASE32_ALPHABET[(value shr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }

        if (bits > 0) {
            result.append(BASE32_ALPHABET[(value shl (5 - bits)) and 0x1F])
        }

        while (result.length % 8 != 0) {
            result.append('=')
        }

        return result.toString()
    }
}

expect fun generateTotp(secret: String, timestamp: Long): String
