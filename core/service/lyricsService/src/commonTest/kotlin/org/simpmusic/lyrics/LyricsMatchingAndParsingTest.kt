package org.simpmusic.lyrics

import org.simpmusic.lyrics.parser.parseSyncedLyrics
import org.simpmusic.lyrics.parser.parseUnsyncedLyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LyricsMatchingAndParsingTest {

    private val client = SimpMusicLyricsClient()

    @Test
    fun testParseUnsyncedLyricsEmptyAndWhitespace() {
        val empty = parseUnsyncedLyrics("")
        assertTrue(empty.lyrics?.lines.orEmpty().isEmpty(), "Empty string should return empty lines")

        val whitespace = parseUnsyncedLyrics("   \n\t\r\n  ")
        assertTrue(whitespace.lyrics?.lines.orEmpty().isEmpty(), "Whitespace should return empty lines")
    }

    @Test
    fun testParseUnsyncedLyricsMetadataOnly() {
        val metadata = """
            [ti:Song Title]
            [ar:Artist Name]
            [al:Album Name]
            [by:LrcMaker]
            [length: 03:45]
        """.trimIndent()
        val res = parseUnsyncedLyrics(metadata)
        assertTrue(res.lyrics?.lines.orEmpty().isEmpty(), "Metadata tags only should result in empty lines")
    }

    @Test
    fun testParseSyncedLyricsMetadataOnly() {
        val metadata = """
            [ti:Song Title]
            [ar:Artist Name]
            [al:Album Name]
        """.trimIndent()
        val res = parseSyncedLyrics(metadata)
        assertTrue(res.lyrics?.lines.orEmpty().isEmpty(), "LRC with only metadata tags should produce empty lines")
    }

    @Test
    fun testParseUnsyncedLyricsValidLines() {
        val lyrics = """
            First line of the song
            Second line of the song
        """.trimIndent()
        val res = parseUnsyncedLyrics(lyrics)
        val lines = res.lyrics?.lines.orEmpty()
        assertEquals(2, lines.size)
        assertEquals("First line of the song", lines[0].words)
        assertEquals("Second line of the song", lines[1].words)
    }

    @Test
    fun testTrackMatchingPreciseRules() {
        // Single-word titles must match exactly, never as substring of unrelated words
        assertFalse(client.isTrackMatch("One", "Someone"), "One should not match Someone")
        assertFalse(client.isTrackMatch("One", "Money"), "One should not match Money")
        assertFalse(client.isTrackMatch("Run", "Hit and Run"), "Run should not match Hit and Run")
        assertFalse(client.isTrackMatch("Stay", "Stay with me"), "Stay should not match Stay with me")
        assertFalse(client.isTrackMatch("All", "Falling"), "All should not match Falling")

        // Exact cleaned titles must match
        assertTrue(client.isTrackMatch("Run", "Run"), "Exact title should match")
        assertTrue(client.isTrackMatch("Blinding Lights (Official Music Video)", "Blinding Lights"), "Cleaned video title should match")
        assertTrue(client.isTrackMatch("Bohemian Rhapsody - Remastered 2011", "Bohemian Rhapsody"), "Remastered suffix should match")
        assertTrue(client.isTrackMatch("Shape of You [Lyric Video]", "Shape of You"), "Lyric video tag should match")
    }

    @Test
    fun testArtistMatchingRules() {
        assertTrue(client.isArtistMatch("The Weeknd", "Weeknd"), "Should match artist substring/tokens")
        assertTrue(client.isArtistMatch("Taylor Swift feat. Post Malone", "Taylor Swift"), "Featured artist should match")
        assertFalse(client.isArtistMatch("The Kid LAROI", "Rihanna"), "Unrelated artists should not match")
        assertFalse(client.isArtistMatch("Coldplay", "Radiohead"), "Unrelated artists should not match")
    }
}
