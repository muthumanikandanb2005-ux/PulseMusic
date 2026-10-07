package com.maxrave.media3.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.maxrave.domain.data.player.AudioEffects
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 [AudioProcessor] applying Dolby Atmos spatial audio virtualization and profile equalisation.
 *
 * Positioned in the audio sink chain ahead of the crossfade filter and sleep attenuation
 * so crossfade ramps smoothly fade the virtualized sound field between tracks.
 */
@UnstableApi
class DolbyAtmosAudioProcessor(
    private val effects: () -> AudioEffects,
) : BaseAudioProcessor() {
    private val kernel = DolbyAtmosKernel()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        kernel.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        return inputAudioFormat
    }

    override fun onFlush() {
        kernel.flush()
    }

    override fun onReset() {
        kernel.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)
        kernel.sync(effects())

        if (kernel.isBypassed) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        kernel.beginBuffer()
        while (inputBuffer.remaining() >= 2) {
            output.putShort(kernel.processInterleaved(inputBuffer.short.toInt()).toShort())
        }
        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }

        output.flip()
    }
}
