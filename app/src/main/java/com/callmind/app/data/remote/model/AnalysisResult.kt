package com.callmind.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AnalysisResult(
    val summary: String = "",
    val sentiment: String = "NEUTRAL",
    val topics: List<String> = emptyList(),
    @SerialName("action_items") val actionItems: List<String> = emptyList(),
    @SerialName("key_points") val keyPoints: List<String> = emptyList()
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun parse(text: String): AnalysisResult {
            // Gemini might wrap JSON in markdown code blocks, strip them
            val cleaned = text
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

            return try {
                json.decodeFromString<AnalysisResult>(cleaned)
            } catch (e: Exception) {
                // If parsing fails, treat the whole text as a summary
                AnalysisResult(summary = text.take(500))
            }
        }
    }
}
