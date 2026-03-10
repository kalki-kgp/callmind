package com.callmind.app.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.callmind.app.data.local.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Gemini's multimodal capability to transcribe audio files.
 * Gemini 2.0 Flash can process audio directly — no separate STT API needed.
 * This serves as the cloud STT option until whisper.cpp is integrated.
 */
@Singleton
class GeminiTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun transcribeAudio(audioFilePath: String): TranscriptionResult = withContext(Dispatchers.IO) {
        val apiKey = userPreferences.geminiApiKey.first()
            ?: throw IllegalStateException("Gemini API key not configured")

        val file = File(audioFilePath)
        if (!file.exists()) throw IllegalStateException("Audio file not found: $audioFilePath")

        val audioBytes = file.readBytes()
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val mimeType = getMimeType(file.extension)

        // Build the multimodal request with inline audio
        val requestBody = """
        {
            "contents": [{
                "parts": [
                    {
                        "inline_data": {
                            "mime_type": "$mimeType",
                            "data": "$base64Audio"
                        }
                    },
                    {
                        "text": "Transcribe this phone call recording accurately. Output ONLY the transcription text, nothing else. If you can identify different speakers, prefix their lines with Speaker 1: and Speaker 2: etc. Transcribe in the language spoken (could be English, Hindi, or mixed)."
                    }
                ]
            }]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Empty response from Gemini")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API error ${response.code}: $responseBody")
        }

        val geminiResponse = json.decodeFromString<GeminiResponseRaw>(responseBody)
        val text = geminiResponse.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No transcription in response")

        TranscriptionResult(
            text = text.trim(),
            language = detectLanguage(text),
            modelUsed = "gemini-2.0-flash"
        )
    }

    private fun getMimeType(extension: String): String = when (extension.lowercase()) {
        "wav" -> "audio/wav"
        "mp3" -> "audio/mp3"
        "m4a" -> "audio/m4a"
        "ogg" -> "audio/ogg"
        "amr" -> "audio/amr"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        else -> "audio/wav"
    }

    private fun detectLanguage(text: String): String {
        // Simple heuristic: if text contains Devanagari characters, it's Hindi or mixed
        val hasDevanagari = text.any { it.code in 0x0900..0x097F }
        val hasLatin = text.any { it in 'a'..'z' || it in 'A'..'Z' }
        return when {
            hasDevanagari && hasLatin -> "hi-en" // Hinglish
            hasDevanagari -> "hi"
            else -> "en"
        }
    }

    data class TranscriptionResult(
        val text: String,
        val language: String,
        val modelUsed: String
    )

    @Serializable
    private data class GeminiResponseRaw(
        val candidates: List<CandidateRaw>? = null
    )

    @Serializable
    private data class CandidateRaw(
        val content: ContentRaw? = null
    )

    @Serializable
    private data class ContentRaw(
        val parts: List<PartRaw>? = null
    )

    @Serializable
    private data class PartRaw(
        val text: String? = null
    )
}
