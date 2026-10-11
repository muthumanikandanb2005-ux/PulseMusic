package com.maxrave.media3.audio

import com.maxrave.domain.data.player.AudioEffects
import com.maxrave.domain.data.player.DolbyAtmosEffect
import com.maxrave.domain.data.player.DolbyAtmosProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Real-time DSP kernel for Dolby Atmos spatial audio virtualization.
 *
 * Implements:
 * 1. Mid-Side Soundstage Spatialization: Widens perceived acoustic space beyond physical stereo boundaries.
 * 2. Psychoacoustic Binaural Crossfeed: Micro-delay interaural reflection creating 3D head-externalized sound.
 * 3. Profile-Specific Acoustic Sculpting:
 *    - DYNAMIC: Balanced 3D soundstage, adaptive bass presence & high air.
 *    - MUSIC: Wide stereo field, transparent mids, warm lows, refined highs.
 *    - CINEMA: Immersive multi-dimensional envelopment, dialogue clarity, deep sub-bass impact.
 *    - VOICE: Focused center channel, speech band presence (300Hz-3.5kHz), sub-bass rumble reduction.
 * 4. Transparent Dynamic Soft-Knee Limiter: Prevents digital clipping and maintains max dynamic range.
 */
class DolbyAtmosKernel {
    private var sampleRate: Int = 44100
    private var channelCount: Int = 2

    var isBypassed: Boolean = true
        private set

    private var profile: DolbyAtmosProfile = DolbyAtmosProfile.DYNAMIC
    private var surroundStrength: Float = DolbyAtmosEffect.DEFAULT_SURROUND_STRENGTH

    // Crossfeed delay line ring buffers (~0.35 ms ITD delay: 32 samples at 48kHz is ~0.66 ms)
    private val bufferMask = 63
    private val leftDelayBuffer = FloatArray(64)
    private val rightDelayBuffer = FloatArray(64)
    private var delayWriteIndex = 0
    private var delayReadOffset = 18

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

    // Interleaved sample state
    private var channelCursor = 0
    private var pendingLeft = 0.0
    private var outputLeft = 0
    private var outputRight = 0

    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = if (sampleRate > 0) sampleRate else 44100
        this.channelCount = channelCount
        // ~0.35 ms ITD delay
        delayReadOffset = ((sampleRate * 0.00035).toInt()).coerceIn(1, bufferMask - 1)
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
                sideGain = 1.0f + 0.35f * surroundStrength
                crossfeedGain = 0.18f * surroundStrength
                // Bass boost +1.5 dB at 80 Hz
                designLowShelf(80.0, 1.5, 0.71, fs)
                // Mid presence flat
                designFlatMid()
                // High air +2.0 dB at 10 kHz
                designHighShelf(10000.0, 2.0, 0.71, fs)
            }
            DolbyAtmosProfile.MUSIC -> {
                sideGain = 1.0f + 0.50f * surroundStrength
                crossfeedGain = 0.22f * surroundStrength
                // Warm bass +2.0 dB at 60 Hz
                designLowShelf(60.0, 2.0, 0.71, fs)
                // Transparent mid
                designFlatMid()
                // Silky air +1.5 dB at 12 kHz
                designHighShelf(12000.0, 1.5, 0.71, fs)
            }
            DolbyAtmosProfile.CINEMA -> {
                sideGain = 1.0f + 0.65f * surroundStrength
                crossfeedGain = 0.28f * surroundStrength
                // Deep sub-bass impact +3.5 dB at 70 Hz
                designLowShelf(70.0, 3.5, 0.71, fs)
                // Dialogue clarity boost +2.0 dB at 2500 Hz
                designPeaking(2500.0, 2.0, 1.0, fs)
                // Cinema stage high-shelf +1.0 dB at 8 kHz
                designHighShelf(8000.0, 1.0, 0.71, fs)
            }
            DolbyAtmosProfile.VOICE -> {
                // Focus center channel, reduce wide reflections
                sideGain = 0.45f + 0.20f * surroundStrength
                crossfeedGain = 0.08f * surroundStrength
                // Cut low-end rumble -3.5 dB at 100 Hz
                designLowShelf(100.0, -3.5, 0.71, fs)
                // Speech presence boost +3.5 dB at 2200 Hz
                designPeaking(2200.0, 3.5, 0.9, fs)
                // High frequency smoothing -1.0 dB at 8 kHz
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
        highB2 = (a * ((a + 1.0) + (a - 1.0) * cosw0 - twoSqrtAAlpha)) / a0
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
        channelCursor = 0
        pendingLeft = 0.0
        outputLeft = 0
        outputRight = 0

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
     * Processes one 16-bit PCM sample in interleaved stereo format (L, R, L, R...).
     */
    fun processInterleaved(sample: Int): Int {
        if (channelCursor == 0) {
            pendingLeft = sample.toDouble()
            channelCursor = 1
            return outputLeft
        } else {
            val inLeft = pendingLeft
            val inRight = sample.toDouble()
            channelCursor = 0

            // 1. Mid-Side Soundstage Expansion
            val mid = (inLeft + inRight) * 0.5
            val side = (inLeft - inRight) * 0.5 * sideGain

            val spatLeft = mid + side
            val spatRight = mid - side

            // 2. Psychoacoustic Binaural Delay / Crossfeed
            val readIdx = (delayWriteIndex - delayReadOffset + 64) and bufferMask
            val delayedR = rightDelayBuffer[readIdx].toDouble()
            val delayedL = leftDelayBuffer[readIdx].toDouble()

            leftDelayBuffer[delayWriteIndex] = spatLeft.toFloat()
            rightDelayBuffer[delayWriteIndex] = spatRight.toFloat()
            delayWriteIndex = (delayWriteIndex + 1) and bufferMask

            // Crossfeed opposite channel with micro-delay for externalized spatial stage
            var leftProc = spatLeft + delayedR * crossfeedGain
            var rightProc = spatRight + delayedL * crossfeedGain

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

            // 4. Soft Clipper / Limiter (headroom protection)
            outputLeft = softClip(hL)
            outputRight = softClip(hR)

            return outputRight
        }
    }

    private fun softClip(x: Double): Int {
        // Fast, smooth tanh-like compression above 24000 (~ -2.7 dBFS) to prevent digital wrap/clip
        val threshold = 24000.0
        val maxAmp = 32767.0
        val absX = if (x < 0.0) -x else x
        val sign = if (x < 0.0) -1.0 else 1.0

        val clamped = if (absX <= threshold) {
            x
        } else {
            val excess = (absX - threshold) / (maxAmp - threshold)
            val compressed = threshold + (maxAmp - threshold) * tanh(excess)
            sign * compressed
        }

        return clamped.toInt().coerceIn(-32768, 32767)
    }
}
