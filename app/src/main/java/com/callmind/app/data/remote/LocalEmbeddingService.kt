package com.callmind.app.data.remote

import android.content.Context
import com.callmind.app.data.local.EmbeddingModelManager
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device text embeddings via MediaPipe TextEmbedder. Fully offline once the
 * model is downloaded (see [EmbeddingModelManager]). Used as the privacy-preserving
 * alternative to [GeminiEmbeddingService].
 */
@Singleton
class LocalEmbeddingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: EmbeddingModelManager
) : EmbeddingProvider {

    override val modelName = EmbeddingModelManager.MODEL_NAME

    // Calibrated separately from the cloud model — different embedding space, different
    // floor. Conservative default; revisit once validated on real Hinglish transcripts.
    override val searchThreshold = 0.6f

    private var embedder: TextEmbedder? = null
    private val mutex = Mutex()

    private suspend fun getEmbedder(): TextEmbedder = mutex.withLock {
        embedder?.let { return it }
        if (!modelManager.isModelDownloaded) {
            throw IllegalStateException("Local embedding model not downloaded")
        }
        val created = withContext(Dispatchers.IO) {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelManager.modelFile.absolutePath)
                .build()
            val options = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .setL2Normalize(true)
                .build()
            TextEmbedder.createFromOptions(context, options)
        }
        embedder = created
        created
    }

    override suspend fun embed(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        if (texts.isEmpty()) return@withContext emptyList()
        val e = getEmbedder()
        texts.map { text ->
            val result = e.embed(text)
            result.embeddingResult().embeddings().first().floatEmbedding()
        }
    }

    override suspend fun embedSingle(text: String): FloatArray = embed(listOf(text)).first()
}
