package com.maxrave.media3.audio

import com.manikandan.domain.data.player.AudioEffects
import com.manikandan.domain.data.player.DolbyAtmosEffect
import com.manikandan.domain.data.player.DolbyAtmosProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Real-time high-fidelity DSP kernel for Dolby Atmos spatial audio virtualization.
 *
 * Implements:
 * 1. Mid-Side Soundstage Spatialization: Widens perceived acoustic space beyond physical stereo boundaries
 *    while preserving phantom center focus and vocal clarity.
 * 2. Psychoacoustic Binaural Crossfeed with Acoustic Head-Shadow Diffraction:
 *    Simulates natural interaural time delay (~0.35 ms) combined with 1st-order head-shadow low-pass filtering
 *    (~4.0 kHz) to externalize sound without harsh comb filtering.
 * 3. Audiophile Acoustic Sculpting:
 *    - DYNAMIC: Wide dimensional soundstage, sub-bass punch (+1.8 dB @ 55 Hz), shimmering air (+1.6 dB @ 12 kHz).
 *    - MUSIC: Natural expansive stereo imaging, warm sub-bass (+1.8 dB @ 50 Hz), pristine highs (+1.5 dB @ 13.5 kHz).
 *    - CINEMA: Multi-dimensional soundstage, deep sub-bass impact (+3.0 dB @ 45 Hz), dialogue presence (+2.2 dB @ 2.4 kHz).
 *    - VOICE: Center-channel speech focus, low-end rumble cut (-3.5 dB @ 90 Hz), vocal presence boost (+3.2 dB @ 2.2 kHz).
 * 4. Transparent Headroom & Soft-Knee Peak Limiter:
 *    Maintains 100% linear high-definition response up to -0.48 dBFS (31000) with smooth continuous-derivative
 *    asymptotic limiting up to full-scale (32767), completely eliminating digital clipping and harshness.
 */
class DolbyAtmosKernel {
    private var sampleRate: Int = 44100
    private var channelCount: Int = 2

    var isBypassed: Boolean = true
        private set

    private var profile: DolbyAtmosProfile = DolbyAtmosProfile.DYNAMIC
    private var surroundStrength: Float = DolbyAtmosEffect.DEFAULT_SURROUND_STRENGTH

    // Crossfeed delay line ring buffers (~0.35 ms ITD delay: 17 samples at 48kHz, 15 at 44.1kHz)
    private val bufferMask = 63
    private val leftDelayBuffer = FloatArray(64)
    private val rightDelayBuffer = FloatArray(64)
    private var delayWriteIndex = 0
    private var delayReadOffset = 18

    // Head-shadow 1st-order low-pass diffraction filter on crossfeed (~4000 Hz)
    private var headShadowAlpha = 0.43
    private var crossfeedFilterL = 0.0
    private var crossfeedFilterR = 0.0

    // Biquad filter state for Low-Frequency (Bass) Shelf
    private var bassB0 = 1.0; private var bassB1 = 0.0; private var bassB2 = 0.0
    private var bassA1 = 0.0; private var bassA2 = 0.0
    private var bassX1L = 0.0; private var bassX2L = 0.0; private var bassY1L = 0.0; private var bassY2L = 0.0
    private var bassX1R = 0.0; private var bassX2R = 0.0; private var bassY1R = 0.0; private var bassY2R = 0.0

    // Biquad filter state for Mid-Presence / Dialogue Peaking
    private var midB0 = 1.0; private var midB1 = 0.0; private var midB2 = 0.0
    private var midA1 = 0.0; private var midA2 = 0.0
    private var midX1L = 0.0; private var midX2L = 0.0; private var midY1L = 0.0; private var midY2L = 0.0
    private var midX1R = 0.0; private var midX2R = 0.0; private var midY1R = 0.0; private var midY2R = 0.0

    // Biquad filter state for High-Shelf (Air)
    private var highB0 = 1.0; private var highB1 = 0.0; private var highB2 = 0.0
    private var highA1 = 0.0; private var highA2 = 0.0
    private var highX1L = 0.0; private var highX2L = 0.0; private var highY1L = 0.0; private var highY2L = 0.0
    private var highX1R = 0.0; private var highX2R = 0.0; private var highY1R = 0.0; private var highY2R = 0.0

    // Multipliers
    private var sideGain: Float = 1.0f
    private var crossfeedGain: Float = 0.18f

    // Output shorts (synchronously updated per stereo pair)
    var outputLeftShort: Short = 0
        private set
    var outputRightShort: Short = 0
        private set

    // Legacy interleaved state tracker
    private var channelCursor = 0
    private var pendingLeft = 0

    companion object {
        // Headroom factor ~ -1.0 dB to guarantee pristine dynamic range without clipping
        private const val HEADROOM_FACTOR = 0.89125
        // Linear threshold ~95% full scale (-0.48 dBFS)
        private const val LIMIT_THRESHOLD = 31000.0
        private const val LIMIT_MAX = 32767.0
        private const val LIMIT_SPAN = LIMIT_MAX - LIMIT_THRESHOLD
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = if (sampleRate > 0) sampleRate else 44100
        this.channelCount = channelCount
        // ~0.35 ms ITD delay
        delayReadOffset = ((this.sampleRate * 0.00035).toInt()).coerceIn(1, bufferMask - 1)

        // Head-shadow lowpass cutoff at 4000 Hz
        val w = 2.0 * PI * 4000.0 / this.sampleRate.toDouble()
        headShadowAlpha = 1.0 - exp(-w)

        recalculateCoefficients()
        flush()
    }

    fun sync(effects: AudioEffects) {
        val atmos = effects.dolbyAtmos
        if (atmos == null || channelCount != 2) {
            isBypassed = true
            return
        }

        isBypassed = false
        if (profile != atmos.profile || surroundStrength != atmos.surroundStrength) {
            profile = atmos.profile
            surroundStrength = atmos.surroundStrength.coerceIn(0f, 1f)
            recalculateCoefficients()
        }
    }

    private fun recalculateCoefficients() {
        val fs = sampleRate.toDouble()
        if (fs <= 0.0) return

        when (profile) {
            DolbyAtmosProfile.DYNAMIC -> {
                sideGain = 1.0f + 0.38f * surroundStrength
                crossfeedGain = 0.16f * surroundStrength
                // Warm sub-bass punch +1.8 dB at 55 Hz
                designLowShelf(55.0, 1.8, 0.71, fs)
                // Mid presence transparent/uncolored
                designFlatMid()
                // Shimmering spatial air +1.6 dB at 12 kHz
                designHighShelf(12000.0, 1.6, 0.71, fs)
            }
            DolbyAtmosProfile.MUSIC -> {
                sideGain = 1.0f + 0.32f * surroundStrength
                crossfeedGain = 0.14f * surroundStrength
                // Deep acoustic low-end +1.8 dB at 50 Hz
                designLowShelf(50.0, 1.8, 0.71, fs)
                // Audiophile transparent mid
                designFlatMid()
                // Silky airy highs +1.5 dB at 13.5 kHz
                designHighShelf(13500.0, 1.5, 0.71, fs)
            }
            DolbyAtmosProfile.CINEMA -> {
                sideGain = 1.0f + 0.50f * surroundStrength
                crossfeedGain = 0.22f * surroundStrength
                // Deep cinematic sub-bass impact +3.0 dB at 45 Hz
                designLowShelf(45.0, 3.0, 0.71, fs)
                // Dialogue clarity boost +2.2 dB at 2.4 kHz
                designPeaking(2400.0, 2.2, 0.9, fs)
                // Cinematic stage air +1.2 dB at 9.0 kHz
                designHighShelf(9000.0, 1.2, 0.71, fs)
            }
            DolbyAtmosProfile.VOICE -> {
                // Focus center channel, tighten stereo reflections
                sideGain = 0.50f + 0.15f * surroundStrength
                crossfeedGain = 0.06f * surroundStrength
                // Cut low-end rumble -3.5 dB at 90 Hz
                designLowShelf(90.0, -3.5, 0.71, fs)
                // Speech presence clarity +3.2 dB at 2.2 kHz
                designPeaking(2200.0, 3.2, 0.85, fs)
                // Smooth high sibilance -1.0 dB at 8.0 kHz
                designHighShelf(8000.0, -1.0, 0.71, fs)
            }
        }
    }

    private fun designLowShelf(f0: Double, gainDb: Double, q: Double, fs: Double) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) + (a - 1.0) * cosw0 + twoSqrtAAlpha
        bassB0 = (a * ((a + 1.0) - (a - 1.0) * cosw0 + twoSqrtAAlpha)) / a0
        bassB1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosw0)) / a0
        bassB2 = (a * ((a + 1.0) - (a - 1.0) * cosw0 - twoSqrtAAlpha)) / a0
        bassA1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosw0)) / a0
        bassA2 = ((a + 1.0) + (a - 1.0) * cosw0 - twoSqrtAAlpha) / a0
    }

    private fun designHighShelf(f0: Double, gainDb: Double, q: Double, fs: Double) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) - (a - 1.0) * cosw0 + twoSqrtAAlpha
        highB0 = (a * ((a + 1.0) + (a - 1.0) * cosw0 + twoSqrtAAlpha)) / a0
        highB1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cosw0)) / a0
        highB2 = (a * ((a + 1.0) - (a - 1.0) * cosw0 - twoSqrtAAlpha)) / a0
        highA1 = (2.0 * ((a - 1.0) - (a + 1.0) * cosw0)) / a0
        highA2 = ((a + 1.0) - (a - 1.0) * cosw0 - twoSqrtAAlpha) / a0
    }

    private fun designPeaking(f0: Double, gainDb: Double, q: Double, fs: Double) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q)

        val a0 = 1.0 + alpha / a
        midB0 = (1.0 + alpha * a) / a0
        midB1 = (-2.0 * cosw0) / a0
        midB2 = (1.0 - alpha * a) / a0
        midA1 = (-2.0 * cosw0) / a0
        midA2 = (1.0 - alpha / a) / a0
    }

    private fun designFlatMid() {
        midB0 = 1.0; midB1 = 0.0; midB2 = 0.0
        midA1 = 0.0; midA2 = 0.0
    }

    fun flush() {
        leftDelayBuffer.fill(0f)
        rightDelayBuffer.fill(0f)
        delayWriteIndex = 0
        crossfeedFilterL = 0.0
        crossfeedFilterR = 0.0
        channelCursor = 0
        pendingLeft = 0
        outputLeftShort = 0
        outputRightShort = 0

        bassX1L = 0.0; bassX2L = 0.0; bassY1L = 0.0; bassY2L = 0.0
        bassX1R = 0.0; bassX2R = 0.0; bassY1R = 0.0; bassY2R = 0.0

        midX1L = 0.0; midX2L = 0.0; midY1L = 0.0; midY2L = 0.0
        midX1R = 0.0; midX2R = 0.0; midY1R = 0.0; midY2R = 0.0

        highX1L = 0.0; highX2L = 0.0; highY1L = 0.0; highY2L = 0.0
        highX1R = 0.0; highX2R = 0.0; highY1R = 0.0; highY2R = 0.0
    }

    fun reset() {
        flush()
    }

    fun beginBuffer() {
        channelCursor = 0
    }

    /**
     * Processes a synchronous 16-bit stereo pair (L, R) simultaneously.
     * Guarantees zero interchannel phase delay.
     */
    fun processStereo(inLeft: Int, inRight: Int) {
        val dLeft = inLeft.toDouble() * HEADROOM_FACTOR
        val dRight = inRight.toDouble() * HEADROOM_FACTOR

        // 1. Mid-Side Soundstage Expansion
        val mid = (dLeft + dRight) * 0.5
        val side = (dLeft - dRight) * 0.5 * sideGain

        val spatLeft = mid + side
        val spatRight = mid - side

        // 2. Head-shadow binaural crossfeed with micro-delay (ITD)
        val readIdx = (delayWriteIndex - delayReadOffset + 64) and bufferMask
        val delayedRawR = rightDelayBuffer[readIdx].toDouble()
        val delayedRawL = leftDelayBuffer[readIdx].toDouble()

        leftDelayBuffer[delayWriteIndex] = spatLeft.toFloat()
        rightDelayBuffer[delayWriteIndex] = spatRight.toFloat()
        delayWriteIndex = (delayWriteIndex + 1) and bufferMask

        // Apply 1st-order head-shadow acoustic diffraction filter (~4 kHz) to delayed crossfeed
        crossfeedFilterL += headShadowAlpha * (delayedRawR - crossfeedFilterL)
        crossfeedFilterR += headShadowAlpha * (delayedRawL - crossfeedFilterR)

        // Crossfeed opposite channel with micro-delay for externalized spatial stage
        val leftProc = spatLeft + crossfeedFilterL * crossfeedGain
        val rightProc = spatRight + crossfeedFilterR * crossfeedGain

        // 3. Acoustic Filter Chain: Bass Shelf -> Mid Peaking -> High Shelf
        // Left channel
        val bL = bassB0 * leftProc + bassB1 * bassX1L + bassB2 * bassX2L - bassA1 * bassY1L - bassA2 * bassY2L
        bassX2L = bassX1L; bassX1L = leftProc; bassY2L = bassY1L; bassY1L = bL

        val mL = midB0 * bL + midB1 * midX1L + midB2 * midX2L - midA1 * midY1L - midA2 * midY2L
        midX2L = midX1L; midX1L = bL; midY2L = midY1L; midY1L = mL

        val hL = highB0 * mL + highB1 * highX1L + highB2 * highX2L - highA1 * highY1L - highA2 * highY2L
        highX2L = highX1L; highX1L = mL; highY2L = highY1L; highY1L = hL

        // Right channel
        val bR = bassB0 * rightProc + bassB1 * bassX1R + bassB2 * bassX2R - bassA1 * bassY1R - bassA2 * bassY2R
        bassX2R = bassX1R; bassX1R = rightProc; bassY2R = bassY1R; bassY1R = bR

        val mR = midB0 * bR + midB1 * midX1R + midB2 * midX2R - midA1 * midY1R - midA2 * midY2R
        midX2R = midX1R; midX1R = bR; midY2R = midY1R; midY1R = mR

        val hR = highB0 * mR + highB1 * highX1R + highB2 * highX2R - highA1 * highY1R - highA2 * highY2R
        highX2R = highX1R; highX1R = mR; highY2R = highY1R; highY1R = hR

        // 4. Transparent soft-knee limiter
        outputLeftShort = limitSample(hL)
        outputRightShort = limitSample(hR)
    }

    /**
     * Interleaved sample compatibility method.
     */
    fun processInterleaved(sample: Int): Int {
        if (channelCursor == 0) {
            pendingLeft = sample
            channelCursor = 1
            return outputLeftShort.toInt()
        } else {
            channelCursor = 0
            processStereo(pendingLeft, sample)
            return outputRightShort.toInt()
        }
    }

    private fun limitSample(x: Double): Short {
        val absX = if (x < 0.0) -x else x
        val sign = if (x < 0.0) -1.0 else 1.0

        val clamped = if (absX <= LIMIT_THRESHOLD) {
            x
        } else {
            val excess = (absX - LIMIT_THRESHOLD) / LIMIT_SPAN
            val compressed = LIMIT_THRESHOLD + LIMIT_SPAN * tanh(excess)
            sign * compressed
        }

        return clamped.toInt().coerceIn(-32768, 32767).toShort()
    }
}
