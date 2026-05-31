package com.callmind.app.util

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Converts audio files to headerless raw PCM (16-bit LE, 16kHz, mono) for Vosk.
 * Vosk's Recognizer.acceptWaveForm consumes raw PCM shorts directly, so no
 * RIFF/WAV container is written. Uses MediaExtractor + MediaCodec for decoding.
 */
object AudioConverter {

    private const val TAG = "AudioConverter"
    private const val TARGET_SAMPLE_RATE = 16000

    /**
     * Convert an audio file to headerless 16kHz mono raw PCM suitable for Vosk.
     * Returns the path to the converted file, or the original path if already suitable.
     */
    fun convertToPcm16kMono(inputPath: String, cacheDir: File): String {
        val inputFile = File(inputPath)
        val outputFile = File(cacheDir, "vosk_${inputFile.nameWithoutExtension}.pcm")

        if (outputFile.exists()) outputFile.delete()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputPath)

            // Find audio track
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                throw IllegalStateException("No audio track found in $inputPath")
            }

            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
            val sourceSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val sourceChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.d(TAG, "Input: $mime, ${sourceSampleRate}Hz, ${sourceChannels}ch")

            // Check if it's already raw PCM at 16kHz mono
            if (mime == "audio/raw" && sourceSampleRate == TARGET_SAMPLE_RATE && sourceChannels == 1) {
                return inputPath
            }

            // Decode using MediaCodec
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val allSamples = mutableListOf<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10_000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Read output
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val samples = ShortArray(shortBuffer.remaining())
                    shortBuffer.get(samples)

                    // Convert to mono if stereo
                    val monoSamples = if (sourceChannels > 1) {
                        toMono(samples, sourceChannels)
                    } else {
                        samples
                    }

                    allSamples.addAll(monoSamples.toList())

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()

            // Resample to 16kHz if needed
            val finalSamples = if (sourceSampleRate != TARGET_SAMPLE_RATE) {
                resample(allSamples.toShortArray(), sourceSampleRate, TARGET_SAMPLE_RATE)
            } else {
                allSamples.toShortArray()
            }

            // Write raw PCM (16-bit LE)
            FileOutputStream(outputFile).use { fos ->
                val byteBuffer = ByteBuffer.allocate(finalSamples.size * 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                byteBuffer.asShortBuffer().put(finalSamples)
                fos.write(byteBuffer.array())
            }

            Log.d(TAG, "Converted to ${finalSamples.size} samples at ${TARGET_SAMPLE_RATE}Hz mono")
            return outputFile.absolutePath

        } finally {
            extractor.release()
        }
    }

    private fun toMono(samples: ShortArray, channels: Int): ShortArray {
        val monoLength = samples.size / channels
        val mono = ShortArray(monoLength)
        for (i in 0 until monoLength) {
            var sum = 0L
            for (ch in 0 until channels) {
                sum += samples[i * channels + ch]
            }
            mono[i] = (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return mono
    }

    private fun resample(input: ShortArray, inputRate: Int, outputRate: Int): ShortArray {
        if (inputRate == outputRate || input.isEmpty()) return input
        val ratio = inputRate.toDouble() / outputRate
        val outputLength = (input.size / ratio).toInt()
        val output = ShortArray(outputLength)

        // Linear interpolation between adjacent source samples reduces the
        // aliasing artefacts that pure nearest-neighbour index-picking produced.
        for (i in 0 until outputLength) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val frac = srcPos - srcIndex
            val s0 = input[srcIndex.coerceIn(0, input.size - 1)].toInt()
            val s1 = input[(srcIndex + 1).coerceIn(0, input.size - 1)].toInt()
            val interpolated = s0 + (s1 - s0) * frac
            output[i] = interpolated.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return output
    }
}
