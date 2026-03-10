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
 * OpenAI-compatible API service for LLM analysis.
 * Works with Nebius, Together, OpenRouter, etc.
 */
@Singleton
class OpenAiCompatibleService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val baseUrl = userPreferences.openAiBaseUrl.first()
        val apiKey = userPreferences.openAiApiKey.first()
            ?: throw IllegalStateException("OpenAI-compatible API key not configured")
        val model = userPreferences.openAiModel.first()

        val escapedPrompt = prompt.escapeJson()

        val requestBody = """
        {
            "model": "$model",
            "messages": [
                {
                    "role": "user",
                    "content": "$escapedPrompt"
                }
            ]
        }
        """.trimIndent()

        val url = "${baseUrl.trimEnd('/')}/chat/completions"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response from API")

        if (!response.isSuccessful) {
            throw IllegalStateException("API error ${response.code}: $body")
        }

        val parsed = json.decodeFromString<ChatCompletionResponse>(body)
        parsed.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No content in response")
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
    private data class ChatCompletionResponse(
        val choices: List<Choice> = emptyList()
    )

    @Serializable
    private data class Choice(
        val message: Message? = null
    )

    @Serializable
    private data class Message(
        val role: String? = null,
        val content: String? = null
    )
}
