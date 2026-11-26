package com.manikandan.spotify.auth

/**
 * Obfuscated credential and persisted-query fallback vault for Spotify's
 * internal web-player protocol. Keys and persisted hashes are stored as
 * salted XOR byte tables so plaintext tokens/hashes do not appear in source
 * constants, crash traces, or logs.
 */
internal object SpotApiVault {
    private val kClientId = intArrayOf(
        62, 83, 29, 184, 251, 203, 249, 228, 218, 151, 54, 34, 18, 84, 122, 60,
        82, 30, 233, 170, 159, 136, 181, 215, 147, 55, 118, 21, 15, 112, 105, 13,
    )
    private val kAppVer = intArrayOf(
        13, 99, 108, 65, 182, 163, 140, 135, 243, 227, 200, 144, 58, 120, 78, 13, 41, 106, 8, 76,
    )
    private val kDeviceId = intArrayOf(
        69, 228, 247, 148, 214, 241, 227, 208, 212, 104, 41, 20, 15, 99, 107, 73, 179, 165,
        142, 141, 243, 227, 223, 213, 63, 126, 30, 13, 44, 108, 91, 229, 164, 154, 209, 243,
    )
    private val kTotpCipher = intArrayOf(
        97, 99, 116, 72, 47, 88, 85, 12, 201, 198, 161, 245, 217, 201, 228, 250, 227, 162, 224, 151, 232, 152, 145, 190, 181, 178,
    )
    private const val TOTP_MASKED_VER = 112

    private val queryTable: Map<String, Pair<Int, IntArray>> = mapOf(
        "searchTracks" to (
            0x29 to intArrayOf(
                75, 89, 122, 63, 12, 76, 233, 195, 213, 242, 177, 133, 196, 54, 38, 27,
                88, 122, 61, 15, 69, 182, 250, 134, 162, 177, 215, 146, 52, 47, 23, 89,
                47, 111, 91, 77, 181, 171, 158, 165, 226, 134, 146, 98, 113, 21, 82, 121,
                60, 12, 67, 185, 165, 154, 143, 226, 216, 197, 53, 45, 17, 2, 117, 96,
            )
        ),
        "searchDesktop" to (
            0x43 to intArrayOf(
                39, 54, 83, 71, 181, 171, 145, 131, 252, 232, 137, 204, 56, 24, 2, 123,
                50, 85, 70, 176, 244, 145, 221, 169, 235, 222, 159, 104, 123, 82, 36, 97,
                5, 21, 231, 161, 145, 139, 252, 188, 217, 205, 110, 120, 23, 117, 104, 85,
                69, 225, 247, 192, 209, 241, 184, 219, 158, 105, 47, 22, 14, 99, 85, 71,
            )
        ),
        "getAlbum" to (
            0x67 to intArrayOf(
                87, 26, 191, 249, 206, 143, 248, 239, 222, 101, 32, 64, 3, 118, 48, 82,
                22, 188, 169, 204, 218, 245, 232, 218, 203, 32, 71, 11, 122, 102, 4, 21,
                229, 253, 145, 143, 251, 228, 221, 155, 109, 19, 1, 122, 50, 2, 76, 183,
                174, 156, 138, 248, 233, 136, 202, 62, 39, 86, 34, 99, 90, 69, 177, 240,
            )
        ),
        "getTrack" to (
            0x89 to intArrayOf(
                191, 171, 153, 137, 245, 235, 142, 101, 33, 20, 81, 37, 97, 85, 66, 233,
                253, 152, 141, 255, 235, 215, 199, 39, 17, 86, 38, 53, 4, 19, 181, 171,
                203, 143, 168, 228, 213, 206, 63, 65, 3, 119, 101, 7, 17, 190, 246, 157,
                142, 175, 235, 212, 152, 106, 123, 83, 115, 103, 2, 71, 178, 164, 158, 140,
            )
        ),
        "queryArtistOverview" to (
            0xA2 to intArrayOf(
                199, 210, 162, 183, 215, 193, 58, 41, 78, 12, 116, 110, 11, 28, 245, 144,
                131, 251, 177, 210, 196, 97, 122, 79, 95, 47, 57, 95, 28, 191, 144, 135,
                246, 231, 220, 198, 99, 39, 26, 0, 46, 104, 15, 76, 239, 254, 214, 160,
                180, 130, 197, 102, 36, 67, 93, 121, 63, 89, 73, 236, 166, 150, 165, 230,
            )
        ),
        "queryArtistRelated" to (
            0xB5 to intArrayOf(
                134, 162, 231, 219, 200, 110, 45, 79, 95, 124, 109, 17, 179, 243, 194, 131,
                166, 238, 131, 202, 57, 41, 79, 8, 116, 60, 91, 185, 245, 196, 132, 247,
                228, 128, 194, 48, 123, 27, 94, 126, 106, 87, 70, 243, 194, 129, 251, 176,
                220, 206, 48, 46, 77, 15, 115, 107, 90, 79, 238, 193, 135, 244, 181, 215,
            )
        ),
        "fetchExtractedColors" to (
            0xC1 to intArrayOf(
                242, 228, 134, 205, 53, 112, 68, 89, 44, 59, 91, 76, 233, 170, 152, 163,
                231, 219, 198, 98, 118, 67, 4, 121, 97, 93, 79, 233, 251, 203, 221, 226,
                212, 195, 58, 118, 28, 1, 35, 108, 10, 30, 187, 249, 200, 143, 174, 130,
                151, 96, 39, 66, 13, 34, 52, 81, 74, 190, 163, 153, 132, 248, 186, 145,
            )
        ),
        "similarAlbumsBasedOnThisTrack" to (
            0xD3 to intArrayOf(
                226, 128, 196, 96, 46, 27, 88, 125, 104, 91, 73, 183, 167, 209, 165, 177,
                130, 198, 102, 46, 30, 10, 42, 109, 88, 29, 235, 166, 152, 166, 178, 210,
                145, 52, 32, 20, 86, 46, 60, 94, 30, 189, 252, 157, 140, 179, 216, 202,
                58, 33, 17, 6, 36, 107, 91, 78, 186, 175, 201, 216, 174, 133, 192, 53,
            )
        ),
    )

    private fun decodeString(encoded: IntArray, salt: Int): String {
        val out = ByteArray(encoded.size) { i ->
            (encoded[i] xor ((salt + i * 17) and 0xFF)).toByte()
        }
        return out.decodeToString()
    }

    fun defaultClientId(): String = decodeString(kClientId, 0x5A)

    fun defaultAppVersion(): String = decodeString(kAppVer, 0x3C)

    fun defaultDeviceId(): String = decodeString(kDeviceId, 0x71)

    fun fallbackTotpSecret(): Pair<Int, List<Int>> =
        (TOTP_MASKED_VER xor 0x4D) to kTotpCipher.mapIndexed { i, v -> v xor ((0x4D + i * 7) and 0xFF) }

    fun fallbackQueryHash(operationName: String): String? =
        queryTable[operationName]?.let { (salt, data) -> decodeString(data, salt) }
}
