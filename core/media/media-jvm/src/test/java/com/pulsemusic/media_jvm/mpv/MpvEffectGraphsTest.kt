package com.pulsemusic.media_jvm.mpv

import com.manikandan.domain.data.player.AudioEffects
import com.manikandan.domain.data.player.CinematicAudioEffect
import com.manikandan.domain.data.player.DelayEffect
import com.manikandan.domain.data.player.DolbyAtmosEffect
import com.manikandan.domain.data.player.DolbyAtmosProfile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MpvEffectGraphsTest {
    @Test
    fun `cinematicAudioGraph builds orbital 8D graph when enabled`() {
        val graph = cinematicAudioGraph(CinematicAudioEffect(speedSeconds = 10, depth = 0.7f))
        assertNotNull(graph)
        assertTrue(graph.contains("extrastereo="))
        assertTrue(graph.contains("aecho="))
        assertTrue(graph.contains("apulsator=mode=sine:hz=0.1000:amount=0.7000"))
    }

    @Test
    fun `cinematicAudioGraph builds wide stage without orbit when speedSeconds is 0`() {
        val graph = cinematicAudioGraph(CinematicAudioEffect(speedSeconds = 0, depth = 0.8f))
        assertNotNull(graph)
        assertTrue(graph.contains("extrastereo="))
        assertFalse(graph.contains("apulsator="))
    }

    @Test
    fun `cinematicAudioGraph clamps NaN depth and returns null when inactive`() {
        val nanGraph = cinematicAudioGraph(CinematicAudioEffect(speedSeconds = 12, depth = Float.NaN))
        assertNotNull(nanGraph)
        assertFalse(nanGraph.contains("NaN"))
        assertNull(cinematicAudioGraph(CinematicAudioEffect(speedSeconds = 0, depth = 0f)))
    }

    @Test
    fun `dolbyAtmosGraph builds valid filter graph for all profiles`() {
        DolbyAtmosProfile.entries.forEach { profile ->
            val graph = dolbyAtmosGraph(DolbyAtmosEffect(profile = profile, surroundStrength = 0.85f))
            assertTrue(graph.contains("volume=0.8913"), "Missing headroom gain for $profile")
            assertTrue(graph.contains("extrastereo="), "Missing soundstage width for $profile")
            assertTrue(graph.contains("lowshelf="), "Missing lowshelf for $profile")
            assertTrue(graph.contains("highshelf="), "Missing highshelf for $profile")
        }
        val nanAtmos = dolbyAtmosGraph(DolbyAtmosEffect(profile = DolbyAtmosProfile.CINEMA, surroundStrength = Float.NaN))
        assertFalse(nanAtmos.contains("NaN"))
    }

    @Test
    fun `bundled libmpv accepts 8D cinematic and Dolby Atmos filter graphs on Windows`() {
        val os = System.getProperty("os.name", "")
        if (!os.contains("Windows", ignoreCase = true)) return
        val candidates = listOf(
            File("../../desktopApp/build/compose/tmp/prepareAppResources/libmpv-2.dll"),
            File("../../../desktopApp/build/compose/tmp/prepareAppResources/libmpv-2.dll"),
            File("desktopApp/build/compose/tmp/prepareAppResources/libmpv-2.dll"),
        )
        val dll = candidates.firstOrNull { it.exists() } ?: return
        System.setProperty("jna.library.path", dll.parentFile.absolutePath)
        val player = MpvPlayer.create(audioOnly = true)
        assertNotNull(player, "Expected bundled Windows libmpv-2.dll to initialize cleanly")
        try {
            val eqAccepted = player.setEqualizer(listOf(2f, 1f, 0f, 0f, 0f, 1f, 2f, 2f, 1f, 0f), -1f)
            assertTrue(eqAccepted, "Expected equalizer chain to be accepted")

            val cinematicOnly = AudioEffects(
                delay = null,
                reverb = null,
                cinematic = CinematicAudioEffect(speedSeconds = 10, depth = 0.7f),
                dolbyAtmos = null,
            )
            assertTrue(
                player.setAudioEffects(cinematicOnly, reverbIrPath = null),
                "Expected bundled Windows libmpv-2.dll to accept 8D Cinematic filter graph",
            )

            val combined = AudioEffects(
                delay = DelayEffect(timeMs = 250, feedback = 0.35f, mix = 0.25f),
                reverb = null,
                cinematic = CinematicAudioEffect(speedSeconds = 8, depth = 0.8f),
                dolbyAtmos = DolbyAtmosEffect(profile = DolbyAtmosProfile.DYNAMIC, surroundStrength = 0.85f),
            )
            assertTrue(
                player.setAudioEffects(combined, reverbIrPath = null),
                "Expected bundled Windows libmpv-2.dll to accept combined Echo + 8D + Dolby Atmos filter graph",
            )
        } finally {
            player.release()
        }
    }
}
