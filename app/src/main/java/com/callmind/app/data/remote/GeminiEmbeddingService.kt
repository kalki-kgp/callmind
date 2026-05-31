package com.callmind.app.data.remote

import com.callmind.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates text embeddings using Gemini's gemini-embedding-001 model.
 * Vectors are requested at 768 dimensions (Matryoshka truncation) to keep storage small.
 */
@Singleton
class GeminiEmbeddingService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) : EmbeddingProvider {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override val modelName = MODEL_NAME

    // gemini-embedding-001 @768-dim has a high similarity floor: unrelated text scores
    // ~0.46, relevant ~0.52+. 0.5 keeps junk out without dropping real matches.
    override val searchThreshold = 0.5f

    companion object {
        const val MODEL_NAME = "gemini-embedding-001"
        private const val MODEL = "models/gemini-embedding-001"
        private const val OUTPUT_DIM = 768
        private const val MAX_BATCH_SIZE = 100
    }

    /**
     * Generate embeddings for a list of text chunks.
     * Returns a list of FloatArrays (one per chunk).
     * batchEmbedContents caps at 100 texts per request, so split into sub-batches.
     */
    override suspend fun embed(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()
        val apiKey = userPreferences.geminiApiKey.first()
            ?: throw IllegalStateException("Gemini API key not configured")

        texts.chunked(MAX_BATCH_SIZE).flatMap { batch -> embedBatch(batch, apiKey) }
    }

    private fun embedBatch(texts: List<String>, apiKey: String): List<FloatArray> {
        val requests = texts.map { text ->
            """{"model":"$MODEL","content":{"parts":[{"text":"${ text.escapeJson() }"}]},"outputDimensionality":$OUTPUT_DIM}"""
        }
        val requestBody = """{"requests":[${requests.joinToString(",")}]}"""

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/$MODEL:batchEmbedContents?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response from Gemini embedding API")

        if (!response.isSuccessful) {
            // 429 (rate limit) and 5xx bubble up so the WorkManager job retries with back-off.
            throw IllegalStateException("Gemini embedding API error ${response.code}: $body")
        }

        val parsed = json.decodeFromString<BatchEmbedResponse>(body)
        return parsed.embeddings.map { it.values.toFloatArray() }
    }

    /**
     * Generate embedding for a single text.
     */
    override suspend fun embedSingle(text: String): FloatArray {
        return embed(listOf(text)).first()
    }

    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    @Serializable
    private data class BatchEmbedResponse(
        val embeddings: List<EmbeddingValue>
    )

    @Serializable
    private data class EmbeddingValue(
        val values: List<Float>
    )
}
