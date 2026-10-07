package com.maxrave.media3.audio

import com.maxrave.domain.data.player.AudioEffects
import com.maxrave.domain.data.player.CinematicAudioEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time DSP kernel for Cinematic & 8D Spatial Audio.
 *
 * Implements:
 * 1. 360-degree Azimuth LFO Orbit: rotates the stereo sound field smoothly around the head.
 * 2. Equal-Power Binaural Panning: preserves constant acoustic power across orbits.
 * 3. Interaural Time Difference (ITD / Haas effect): microsecond delays (~0.6 ms) on the far ear.
 * 4. Head Shadow & Pinna Filter: subtle high-frequency roll-off when sound moves behind the listener.
 * 5. Cinematic Crossfeed: subtle out-of-phase cross-channel reflection providing wide cinema staging.
 */
class CinematicKernel {
    private var sampleRate: Int = 44100
    private var channelCount: Int = 2

    var isBypassed: Boolean = true
        private set

    private var speedSeconds: Int = CinematicAudioEffect.DEFAULT_SPEED_SECONDS
    private var depth: Float = CinematicAudioEffect.DEFAULT_DEPTH

    // Azimuth LFO phase (0 to 2*PI)
    private var phase: Double = 0.0
    private var phaseStep: Double = 0.0

    // ITD Delay line ring buffers (256 samples is ~5.3 ms at 48 kHz, well above ~0.6 ms max ITD)
    private val bufferMask = 255
    private val leftDelayBuffer = FloatArray(256)
    private val rightDelayBuffer = FloatArray(256)
    private var writeIndex = 0

    // Low-pass filter states for rear head-shadow
    private var rearLpLeft = 0.0
    private var rearLpRight = 0.0

    // Temporary storage for interleaved frame
    private var channelCursor = 0
    private var pendingLeft = 0.0
    private var outputLeft = 0
    private var outputRight = 0

    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = if (sampleRate > 0) sampleRate else 44100
        this.channelCount = channelCount
        updatePhaseStep()
        flush()
    }

    fun sync(effects: AudioEffects) {
        val cinematic = effects.cinematic
        if (cinematic == null || channelCount != 2) {
            isBypassed = true
            return
        }

        isBypassed = false
        if (speedSeconds != cinematic.speedSeconds || depth != cinematic.depth) {
            speedSeconds = cinematic.speedSeconds
            depth = cinematic.depth.coerceIn(0f, 1f)
            updatePhaseStep()
        }
    }

    private fun updatePhaseStep() {
        if (speedSeconds > 0 && sampleRate > 0) {
            phaseStep = (2.0 * PI) / (sampleRate.toDouble() * speedSeconds.toDouble())
        } else {
            phaseStep = 0.0
            phase = 0.0
        }
    }

    fun flush() {
        leftDelayBuffer.fill(0f)
        rightDelayBuffer.fill(0f)
        writeIndex = 0
        channelCursor = 0
        rearLpLeft = 0.0
        rearLpRight = 0.0
        pendingLeft = 0.0
        outputLeft = 0
        outputRight = 0
    }

    fun reset() {
        flush()
        phase = 0.0
    }

    /**
     * Called before processing each buffer.
     */
    fun beginBuffer() {
        channelCursor = 0
    }

    /**
     * Processes interleaved 16-bit PCM samples.
     * Expects Left, then Right channel sequence.
     */
    fun processInterleaved(sample: Int): Int {
        if (channelCursor == 0) {
            // Left channel
            pendingLeft = sample.toDouble()
            channelCursor = 1
            return outputLeft
        } else {
            // Right channel: process full stereo frame
            val inL = pendingLeft
            val inR = sample.toDouble()
            channelCursor = 0

            // 1. Azimuth angle and position
            val theta = phase
            val sinTheta = sin(theta) // Horizontal (-1 = left, +1 = right)
            val cosTheta = cos(theta) // Front/Back (+1 = front, -1 = back)

            val x = if (speedSeconds > 0) sinTheta else 0.0
            val y = if (speedSeconds > 0) cosTheta else 1.0

            // 2. Head-shadow pinna filter when behind listener (cosTheta < 0)
            val rearDamping = if (y < 0.0) (-y * depth * 0.45).coerceIn(0.0, 0.45) else 0.0
            val lpAlpha = 1.0 - rearDamping
            rearLpLeft += lpAlpha * (inL - rearLpLeft)
            rearLpRight += lpAlpha * (inR - rearLpRight)

            val stageInL = rearLpLeft
            val stageInR = rearLpRight

            // 3. Cinematic ambience & spatial crossfeed
            // Expands the stereo base slightly to simulate listening in an auditorium
            val crossfeedGain = 0.20 * depth.toDouble()
            val wideL = stageInL - crossfeedGain * stageInR
            val wideR = stageInR - crossfeedGain * stageInL

            // 4. Equal-power circular panning
            // Pan angle alpha ranges from pi/8 to 3*pi/8
            val panRatio = (x * depth.toDouble() * 0.80).coerceIn(-0.85, 0.85)
            val alpha = (PI / 4.0) * (1.0 + panRatio)
            val gainL = cos(alpha) * 1.41421356 // sqrt(2) normalizes equal power sum
            val gainR = sin(alpha) * 1.41421356

            val pannedL = (wideL * gainL).toFloat()
            val pannedR = (wideR * gainR).toFloat()

            // 5. Interaural Time Difference (ITD / Haas effect)
            // Max delay across skull: ~0.55 ms (~24 samples at 44.1 kHz)
            val maxDelaySamples = (sampleRate * 0.00055).coerceIn(10.0, 30.0)
            val delaySamples = kotlin.math.abs(x) * depth.toDouble() * maxDelaySamples

            // Store in circular delay buffer
            leftDelayBuffer[writeIndex] = pannedL
            rightDelayBuffer[writeIndex] = pannedR

            val finalL: Double
            val finalR: Double

            if (x > 0.0) {
                // Sound on the right: Left ear signal is delayed
                finalR = pannedR.toDouble()
                finalL = readDelayed(leftDelayBuffer, writeIndex, delaySamples)
            } else if (x < 0.0) {
                // Sound on the left: Right ear signal is delayed
                finalL = pannedL.toDouble()
                finalR = readDelayed(rightDelayBuffer, writeIndex, delaySamples)
            } else {
                finalL = pannedL.toDouble()
                finalR = pannedR.toDouble()
            }

            // Advance ring index
            writeIndex = (writeIndex + 1) and bufferMask

            // Advance LFO phase
            if (phaseStep > 0.0) {
                phase += phaseStep
                if (phase >= 2.0 * PI) {
                    phase -= 2.0 * PI
                }
            }

            // Normalization and soft clipping to PCM16
            outputLeft = clampPcm16(finalL)
            outputRight = clampPcm16(finalR)

            return outputRight
        }
    }

    private fun readDelayed(buffer: FloatArray, currentWrite: Int, delay: Double): Double {
        val delayInt = delay.toInt()
        val frac = delay - delayInt
        val idx0 = (currentWrite - 1 - delayInt) and bufferMask
        val idx1 = (currentWrite - 1 - delayInt - 1) and bufferMask
        return (1.0 - frac) * buffer[idx0] + frac * buffer[idx1]
    }

    private fun clampPcm16(value: Double): Int {
        return value.coerceIn(-32768.0, 32767.0).toInt()
    }
}
