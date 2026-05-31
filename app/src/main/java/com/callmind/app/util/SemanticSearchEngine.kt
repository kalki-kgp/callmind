package com.callmind.app.util

import com.callmind.app.data.local.db.dao.EmbeddingDao
import com.callmind.app.data.local.db.entity.EmbeddingEntity
import com.callmind.app.data.remote.EmbeddingProviderRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class SemanticMatch(
    val callId: Long,
    val chunkText: String,
    val score: Float // cosine similarity 0..1
)

/**
 * Performs semantic search across all stored embeddings.
 *
 * 1. Embeds the query text via Gemini embedding API
 * 2. Loads all stored embeddings from Room
 * 3. Computes cosine similarity (brute-force — fast enough for hundreds of vectors)
 * 4. Returns top-K matches above threshold
 */
@Singleton
class SemanticSearchEngine @Inject constructor(
    private val embeddingDao: EmbeddingDao,
    private val embeddingProviderRegistry: EmbeddingProviderRegistry
) {
    suspend fun search(
        query: String,
        topK: Int = 10
    ): List<SemanticMatch> {
        val provider = embeddingProviderRegistry.current()

        // 1. Embed the query with the active provider
        val queryEmbedding = provider.embedSingle(query)

        // 2. Load stored embeddings produced by the SAME model — vectors from
        //    different models live in different spaces and aren't comparable.
        val allEmbeddings = embeddingDao.getAllEmbeddings()
            .filter { it.modelUsed == provider.modelName }
        if (allEmbeddings.isEmpty()) return emptyList()

        // 3. Compute cosine similarity for each
        val scored = allEmbeddings.map { entity ->
            val storedVector = entity.embedding.toFloatArray()
            val score = cosineSimilarity(queryEmbedding, storedVector)
            SemanticMatch(
                callId = entity.callId,
                chunkText = entity.chunkText,
                score = score
            )
        }

        // 4. Filter by the provider's calibrated threshold and sort
        return scored
            .filter { it.score >= provider.searchThreshold }
            .sortedByDescending { it.score }
            // Deduplicate by callId (keep highest score per call) BEFORE limiting,
            // otherwise dedup collapses the list below topK when one call dominates.
            .distinctBy { it.callId }
            .take(topK)
    }

    companion object {
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0f) 0f else dot / denominator
        }

        /**
         * Serialize FloatArray to ByteArray for Room storage.
         */
        fun FloatArray.toByteArray(): ByteArray {
            val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.asFloatBuffer().put(this)
            return buffer.array()
        }

        /**
         * Deserialize ByteArray back to FloatArray.
         */
        fun ByteArray.toFloatArray(): FloatArray {
            val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
            val floats = FloatArray(size / 4)
            buffer.asFloatBuffer().get(floats)
            return floats
        }
    }
}
