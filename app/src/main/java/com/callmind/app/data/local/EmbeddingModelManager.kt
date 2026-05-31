package com.callmind.app.data.local

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and tracks the on-device text-embedding model used by
 * [com.callmind.app.data.remote.LocalEmbeddingService]. Mirrors the runtime
 * download approach of [VoskModelManager] — a single .tflite file kept in
 * filesDir so it survives reinstalls of the model without re-bundling the APK.
 *
 * Default model is the MediaPipe Universal Sentence Encoder text embedder, which
 * is the reliably-shippable on-device option today. Swapping in EmbeddingGemma
 * (better Hindi/Hinglish quality, needs the LiteRT runtime) is tracked in
 * tobeimplemented.md.
 */
@Singleton
class EmbeddingModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir: File
        get() = File(context.filesDir, "embedding-models")

    val modelFile: File
        get() = File(modelDir, MODEL_FILE_NAME)

    val isModelDownloaded: Boolean
        get() = modelFile.exists() && modelFile.length() > 0

    suspend fun downloadModel(onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            modelDir.mkdirs()
            val tmp = File(modelDir, "$MODEL_FILE_NAME.part")
            Log.i(TAG, "Downloading embedding model: $MODEL_NAME")

            val connection = URL(MODEL_URL).openConnection()
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            val totalBytes = connection.contentLengthLong.coerceAtLeast(1)

            connection.getInputStream().use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead.toFloat() / totalBytes)
                    }
                }
            }

            if (modelFile.exists()) modelFile.delete()
            if (!tmp.renameTo(modelFile)) {
                tmp.copyTo(modelFile, overwrite = true)
                tmp.delete()
            }
            Log.i(TAG, "Embedding model ready at: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download embedding model", e)
            false
        }
    }

    companion object {
        private const val TAG = "EmbeddingModelManager"
        const val MODEL_NAME = "universal-sentence-encoder"
        private const val MODEL_FILE_NAME = "universal_sentence_encoder.tflite"
        private const val MODEL_URL =
            "https://storage.googleapis.com/mediapipe-models/text_embedder/" +
                "universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
    }
}
