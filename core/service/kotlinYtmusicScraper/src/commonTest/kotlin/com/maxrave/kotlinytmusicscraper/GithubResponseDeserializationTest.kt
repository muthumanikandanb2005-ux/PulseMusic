package com.maxrave.kotlinytmusicscraper

import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GithubResponseDeserializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun testLargeIdsAndSizesExceedingIntMax() {
        val largeIdJson = """
            {
                "id": 4183735280,
                "tag_name": "v3.1.8",
                "name": "Pulse Music v3.1.8",
                "published_at": "2026-10-20T13:00:00Z",
                "body": "Release 3.1.8",
                "author": {
                    "id": 5000000000,
                    "login": "octocat"
                },
                "assets": [
                    {
                        "id": 6592729960,
                        "name": "PulseMusic-Universal.apk",
                        "size": 5368709120,
                        "browser_download_url": "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.1.8/PulseMusic-Universal.apk",
                        "uploader": {
                            "id": 5000000000,
                            "login": "octocat"
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<GithubResponse>(largeIdJson)
        assertEquals(4183735280L, response.id)
        assertEquals("v3.1.8", response.tagName)
        assertEquals(5000000000L, response.author?.id)

        val asset = response.assets?.firstOrNull()
        assertNotNull(asset)
        assertEquals(6592729960L, asset.id)
        assertEquals(5368709120L, asset.size)
        assertEquals(5000000000L, asset.uploader?.id)
        assertEquals("PulseMusic-Universal.apk", asset.name)
    }

    @Test
    fun testMissingFieldsDefaultToNull() {
        val minimalJson = """
            {
                "tag_name": "v3.1.8"
            }
        """.trimIndent()

        val response = json.decodeFromString<GithubResponse>(minimalJson)
        assertEquals("v3.1.8", response.tagName)
        assertNull(response.id)
        assertNull(response.body)
        assertNull(response.assets)
        assertNull(response.author)
        assertNull(response.reactions)
    }

    @Test
    fun testUnknownKeysIgnoredGracefully() {
        val jsonWithUnknownKeys = """
            {
                "id": 12345,
                "tag_name": "v3.1.8",
                "unknown_future_field": "some_value",
                "immutable": false,
                "assets": [
                    {
                        "id": 67890,
                        "name": "PulseMusic.apk",
                        "size": 1024,
                        "digest": "sha256:abcdef",
                        "browser_download_url": "https://example.com/download.apk",
                        "unknown_nested_field": 42
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<GithubResponse>(jsonWithUnknownKeys)
        assertEquals(12345L, response.id)
        assertEquals("v3.1.8", response.tagName)
        val asset = response.assets?.firstOrNull()
        assertNotNull(asset)
        assertEquals(67890L, asset.id)
        assertEquals(1024L, asset.size)
    }
}
