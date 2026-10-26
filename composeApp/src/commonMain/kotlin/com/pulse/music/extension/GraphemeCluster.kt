package com.pulse.music.extension

/**
 * Checks whether a Unicode code point is a combining mark, diacritic, vowel sign,
 * virama/halant, or joiner that should stay attached to its preceding base character.
 */
private fun isCombiningCodePoint(cp: Int): Boolean {
    return when (cp) {
        // Combining Diacritical Marks (accents, umlauts, tildes, etc.)
        in 0x0300..0x036F -> true
        in 0x1AB0..0x1AFF -> true
        in 0x1DC0..0x1DFF -> true
        in 0x20D0..0x20FF -> true
        in 0xFE20..0xFE2F -> true

        // Hebrew & Arabic diacritics / points
        in 0x0591..0x05BD -> true
        in 0x05BF..0x05C7 -> true
        in 0x0610..0x061A -> true
        in 0x064B..0x065F -> true
        0x0670 -> true

        // Indic vowel signs, viramas, matras, anusvara, visarga (Devanagari, Bengali, Gurmukhi, Gujarati, Oriya, Tamil, Telugu, Kannada, Malayalam, Sinhala)
        in 0x0901..0x0903, in 0x093E..0x094F, in 0x0951..0x0957, in 0x0962..0x0963 -> true // Devanagari
        in 0x0981..0x0983, in 0x09BE..0x09CD, in 0x09D7..0x09D7, in 0x09E2..0x09E3 -> true // Bengali
        in 0x0A01..0x0A03, in 0x0A3E..0x0A4D, in 0x0A70..0x0A75 -> true                     // Gurmukhi
        in 0x0A81..0x0A83, in 0x0ABE..0x0ACD, in 0x0AE2..0x0AE3 -> true                     // Gujarati
        in 0x0B01..0x0B03, in 0x0B3E..0x0B4D, in 0x0B56..0x0B57, in 0x0B62..0x0B63 -> true // Oriya
        in 0x0B82..0x0B82, in 0x0BBE..0x0BCD, 0x0BD7 -> true                                 // Tamil
        in 0x0C00..0x0C03, in 0x0C3E..0x0C4D, in 0x0C55..0x0C56, in 0x0C62..0x0C63 -> true // Telugu
        in 0x0C81..0x0C83, in 0x0CBE..0x0CCD, in 0x0CD5..0x0CD6, in 0x0CE2..0x0CE3 -> true // Kannada
        in 0x0D01..0x0D03, in 0x0D3E..0x0D4D, in 0x0D57..0x0D57, in 0x0D62..0x0D63 -> true // Malayalam
        in 0x0D82..0x0D83, in 0x0DCF..0x0DDF, in 0x0DF2..0x0DF3 -> true                     // Sinhala

        // Thai, Lao, Khmer, Myanmar combining vowels and tones
        in 0x0E31..0x0E3A, in 0x0E47..0x0E4E -> true
        in 0x0EB1..0x0EBC, in 0x0EC8..0x0ECD -> true
        in 0x102B..0x103E, in 0x1056..0x1059 -> true
        in 0x17B6..0x17D3, 0x17DD -> true

        // Zero-width joiner & non-joiner (used in Indic conjuncts, emojis)
        0x200C, 0x200D -> true

        // Variation selectors (text/emoji styles)
        in 0xFE00..0xFE0F -> true
        in 0xE0100..0xE01EF -> true

        else -> false
    }
}

/**
 * Splits any string into its true visual letters / grapheme clusters without splitting
 * combining diacritics, surrogate pairs, or Indic vowel signs/viramas.
 *
 * Ensures lyrics light animation progresses letter-by-letter across all world languages
 * (English, Tamil, Hindi, Japanese, Chinese, Korean, Spanish, Vietnamese, etc.) without error.
 */
fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val clusters = mutableListOf<String>()
    val sb = StringBuilder()
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            sb.append(c).append(this[i + 1])
            i += 2
        } else {
            sb.append(c)
            i += 1
        }

        // Attach all immediately following combining marks, diacritics, or joiners
        while (i < length) {
            val nextC = this[i]
            val nextCodePoint = if (nextC.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
                (nextC.code - 0xD800 shl 10) + (this[i + 1].code - 0xDC00) + 0x10000
            } else {
                nextC.code
            }

            if (isCombiningCodePoint(nextCodePoint)) {
                if (nextC.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
                    sb.append(nextC).append(this[i + 1])
                    i += 2
                } else {
                    sb.append(nextC)
                    i += 1
                }
            } else {
                break
            }
        }

        clusters.add(sb.toString())
        sb.setLength(0)
    }
    return clusters
}
