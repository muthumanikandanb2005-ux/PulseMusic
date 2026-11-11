package com.manikandan.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AudioQualityTest {

    @Test
    fun testQualityItemsContainSeparateFlacAndLossless() {
        val flacOption = QUALITY.items.find { it.toString().contains("FLAC", ignoreCase = true) }
        val losslessOption = QUALITY.items.find { it.toString().contains("Lossless", ignoreCase = true) && !it.toString().contains("FLAC", ignoreCase = true) }

        assertTrue(flacOption != null, "FLAC option must be present in QUALITY.items")
        assertTrue(losslessOption != null, "Lossless option must be present in QUALITY.items")
        assertNotEquals(flacOption, losslessOption, "FLAC and Lossless must be separate options")
    }

    @Test
    fun testFlacAndLosslessItagsAreDistinct() {
        val flacIndex = QUALITY.items.indexOfFirst { it.toString().contains("FLAC", ignoreCase = true) }
        val losslessIndex = QUALITY.items.indexOfFirst { it.toString().contains("Lossless", ignoreCase = true) && !it.toString().contains("FLAC", ignoreCase = true) }

        val flacItag = QUALITY.itags[flacIndex]
        val losslessItag = QUALITY.itags[losslessIndex]

        assertEquals(ITAG.AUDIO_FLAC_LOSSLESS, flacItag)
        assertEquals(ITAG.AUDIO_AAC_VERY_HIGH, losslessItag)
        assertEquals(999, flacItag)
        assertEquals(141, losslessItag)
    }

    @Test
    fun testNormalizeDiscerningFlacAndLossless() {
        val flacNormalized = QUALITY.normalize("FLAC Lossless (Hi-Res Audio)")
        val losslessNormalized = QUALITY.normalize("Lossless (Apple Music Quality)")

        assertTrue(flacNormalized.contains("FLAC", ignoreCase = true))
        assertTrue(losslessNormalized.contains("Apple Music", ignoreCase = true) || losslessNormalized.contains("Lossless", ignoreCase = true))
        assertNotEquals(flacNormalized, losslessNormalized)

        assertEquals(ITAG.AUDIO_FLAC_LOSSLESS, QUALITY.itagOf(flacNormalized))
        assertEquals(ITAG.AUDIO_AAC_VERY_HIGH, QUALITY.itagOf(losslessNormalized))
    }
}
