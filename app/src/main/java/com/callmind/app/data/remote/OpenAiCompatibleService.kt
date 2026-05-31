package com.callmind.app.data.remote

import com.callmind.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
            ?: throw ConfigException("OpenAI-compatible API key not configured")
        val model = userPreferences.openAiModel.first()

        val payload = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
        }
        val requestBody = json.encodeToString(JsonObject.serializer(), payload)

        val url = "${baseUrl.trimEnd('/')}/chat/completions"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw IllegalStateException("Empty response from API")

            if (!response.isSuccessful) {
                throw IllegalStateException("API error ${response.code}: $body")
            }

            val parsed = json.decodeFromString<ChatCompletionResponse>(body)
            parsed.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("No content in response")
        }
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
