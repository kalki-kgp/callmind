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
 * Generates text embeddings using Gemini's text-embedding-004 model.
 * Each embedding is a 768-dimensional vector.
 */
@Singleton
class GeminiEmbeddingService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Generate embeddings for a list of text chunks.
     * Returns a list of FloatArrays (one per chunk).
     */
    suspend fun embed(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        val apiKey = userPreferences.geminiApiKey.first()
            ?: throw IllegalStateException("Gemini API key not configured")

        // Gemini batchEmbedContents endpoint
        val requests = texts.map { text ->
            """{"model":"models/text-embedding-004","content":{"parts":[{"text":"${ text.escapeJson() }"}]}}"""
        }

        val requestBody = """{"requests":[${requests.joinToString(",")}]}"""

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:batchEmbedContents?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response from Gemini embedding API")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini embedding API error ${response.code}: $body")
        }

        val parsed = json.decodeFromString<BatchEmbedResponse>(body)
        parsed.embeddings.map { it.values.toFloatArray() }
    }

    /**
     * Generate embedding for a single text.
     */
    suspend fun embedSingle(text: String): FloatArray {
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
