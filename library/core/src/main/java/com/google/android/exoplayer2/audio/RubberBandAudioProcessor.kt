package com.google.android.exoplayer2.audio

import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log.v
import com.breakfastquay.rubberband.RubberBandStretcher
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioProcessor.EMPTY_BUFFER
import com.google.android.exoplayer2.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.min


private const val TAG = "RBAS"
private const val SHORT_SIZE = 2

private data class RBParameters(val speed: Double, val pitch: Double, val audioFormat: AudioProcessor.AudioFormat,
                                val options: Int, val bufferSize: Int)

class RubberBandAudioProcessor : AudioProcessor {

    private var rubberBandStretcher: RubberBandStretcher? = null
    private var inputBytes = 0
    private var outputBuffer = ByteBuffer.allocate(0).order(ByteOrder.nativeOrder())
    private var shortBuffer = outputBuffer.asShortBuffer()
    private lateinit var rbParameters: RBParameters
    private var recreate = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        Log.i(TAG, "${inputAudioFormat.sampleRate} ${inputAudioFormat.bytesPerFrame} ${inputAudioFormat.channelCount}")
        val bufferSize = AudioTrack.getMinBufferSize(inputAudioFormat.sampleRate,
                if (inputAudioFormat.channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 4
        rbParameters = RBParameters(1.0, 1.0, inputAudioFormat, RubberBandStretcher.OptionProcessRealTime +
                RubberBandStretcher.OptionStretchPrecise, bufferSize)
        recreate = true
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_16BIT);
    }

    override fun isActive(): Boolean {
        return true
        return rbParameters.speed != 1.0 || rbParameters.pitch != 1.0
    }

    override fun queueInput(buffer: ByteBuffer) {
        if (buffer.remaining() > 0) {
            inputBytes += buffer.remaining()
            val floats = buffer.toFloats()
            rubberBandStretcher?.process(floats, false)
            buffer.position(buffer.position() + buffer.remaining())
        }
    }

    override fun queueEndOfStream() {
        // no rubber band methods to do this..
    }

    override fun getOutput(): ByteBuffer {

        return rubberBandStretcher?.let { rbs ->
            val availableFrames = rbs.available()
            if (availableFrames > 0) {
                val buffer = (0 until rbParameters.audioFormat.channelCount).map { FloatArray(availableFrames) }.toTypedArray()
                val retrievedFrames = rbs.retrieve(buffer)

                val requiredBytes = availableFrames * SHORT_SIZE * rbParameters.audioFormat.channelCount

                if (outputBuffer.capacity() < requiredBytes) {
                    outputBuffer = ByteBuffer.allocate(requiredBytes).order(ByteOrder.nativeOrder())
                    shortBuffer = outputBuffer.asShortBuffer()
                } else {
                    outputBuffer.clear()
                    shortBuffer.clear()
                }

                (0 until retrievedFrames).forEach { idx ->
                    (0 until rbParameters.audioFormat.channelCount).forEach { channel ->
                        var float = buffer[channel][idx]
                        if (float > 1.0f) float = 1.0f
                        if (float < -1.0f) float = -1.0f
                        val out = (float * 32767).toInt().toShort()
                        shortBuffer.put(out)
                    }
                }
                outputBuffer.limit(requiredBytes)
                val retBuffer = outputBuffer
                outputBuffer = EMPTY_BUFFER
                retBuffer

            } else {
                EMPTY_BUFFER
            }
        } ?: EMPTY_BUFFER
    }

    override fun isEnded(): Boolean {
        return rubberBandStretcher?.available() == 0
    }

    override fun flush() {
        if (recreate) {
            rubberBandStretcher = RubberBandStretcher(rbParameters.audioFormat.sampleRate,
                    rbParameters.audioFormat.channelCount,
                    RubberBandStretcher.OptionProcessRealTime +
                            RubberBandStretcher.OptionStretchPrecise,
                    rbParameters.speed, rbParameters.pitch)
            recreate = false
        }
    }

    override fun reset() {
        inputBytes = 0
        rubberBandStretcher?.reset()
    }

    fun setSpeed(speed: Float) {
        rbParameters = rbParameters.copy(speed = 1.0f / speed.toDouble())
        recreate = true
    }

    fun setPitch(pitch: Float) {
        rbParameters = rbParameters.copy(pitch = pitch.toDouble())
        recreate = true
    }

    fun getMediaDuration(playoutDuration: Long): Long {
        return ((rubberBandStretcher?.timeRatio ?: 1.0) * playoutDuration).toLong()
    }

    private fun ByteBuffer.toFloats(): Array<out FloatArray> {
        val shortBuffer = asShortBuffer()

        val inputFloatArray = (0 until rbParameters.audioFormat.channelCount).map { c ->
            FloatArray(shortBuffer.remaining() / rbParameters.audioFormat.channelCount)
        }.toTypedArray()

        shortBuffer.rewind()

        (0 until shortBuffer.remaining() / rbParameters.audioFormat.channelCount).forEach { idx ->
            (0 until rbParameters.audioFormat.channelCount).forEach { channel ->
                val short = shortBuffer.get()
                val value = short.toFloat() / 32768

                inputFloatArray[channel][idx] = value
            }
        }

        return inputFloatArray
    }

}