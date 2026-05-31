package com.callmind.app.data.remote

/**
 * A source of text embeddings. Implemented by the cloud Gemini path and the
 * on-device MediaPipe path so the rest of the app can stay provider-agnostic.
 *
 * Vectors from different providers are NOT comparable (different models, often
 * different dimensions), so [modelName] is stamped on every stored row and used
 * to keep search confined to vectors produced by the same model.
 */
interface EmbeddingProvider {
    /** Stable identifier stored on each embedding row (e.g. "gemini-embedding-001"). */
    val modelName: String

    /**
     * Cosine-similarity cutoff below which a match is considered irrelevant.
     * Calibrated per model — embedding spaces have very different similarity floors.
     */
    val searchThreshold: Float

    suspend fun embed(texts: List<String>): List<FloatArray>

    suspend fun embedSingle(text: String): FloatArray
}
