package com.callmind.app.service.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.callmind.app.R
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.remote.GeminiApiService
import com.callmind.app.data.remote.model.AnalysisResult
import com.callmind.app.data.remote.model.Content
import com.callmind.app.data.remote.model.GeminiRequest
import com.callmind.app.data.remote.model.Part
import com.callmind.app.data.remote.model.extractText
import com.callmind.app.data.local.db.entity.ActionItemEntity
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.repository.CallRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@HiltWorker
class AnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository,
    private val geminiApiService: GeminiApiService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    private val json = Json { encodeDefaults = true }

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        val call = callRepository.getCallById(callId) ?: return Result.failure()
        val transcript = callRepository.getTranscript(callId) ?: return Result.failure()

        if (transcript.fullText.isBlank()) return Result.failure()

        setForeground(createForegroundInfo("Analyzing: ${call.contactName ?: call.phoneNumber}"))

        return try {
            val apiKey = userPreferences.geminiApiKey.first() ?: return Result.failure()

            val prompt = buildAnalysisPrompt(transcript.fullText, call.contactName)
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val response = geminiApiService.generateContent(apiKey, request)
            val analysisText = response.extractText() ?: return Result.retry()

            // Parse structured response
            val parsed = AnalysisResult.parse(analysisText)

            val analysis = CallAnalysisEntity(
                callId = callId,
                summary = parsed.summary,
                sentiment = parsed.sentiment,
                topicsJson = json.encodeToString(ListSerializer(String.serializer()), parsed.topics),
                actionItemsJson = json.encodeToString(ListSerializer(String.serializer()), parsed.actionItems),
                keyPointsJson = json.encodeToString(ListSerializer(String.serializer()), parsed.keyPoints),
                modelUsed = "gemini-2.0-flash"
            )
            callRepository.insertAnalysis(analysis)

            // Save action items as separate entities for easy querying
            if (parsed.actionItems.isNotEmpty()) {
                val actionEntities = parsed.actionItems.map { item ->
                    ActionItemEntity(
                        callId = callId,
                        description = item
                    )
                }
                callRepository.insertActionItems(actionEntities)
            }

            callRepository.updateCall(call.copy(isAnalyzed = true))
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun buildAnalysisPrompt(transcript: String, contactName: String?): String {
        return """
You are analyzing a phone call transcript. Return ONLY valid JSON (no markdown, no explanation) with this exact structure:

{
  "summary": "2-3 sentence summary of the call",
  "sentiment": "POSITIVE or NEGATIVE or NEUTRAL or MIXED",
  "topics": ["topic1", "topic2"],
  "action_items": ["action item 1", "action item 2"],
  "key_points": ["key point 1", "key point 2"]
}

Rules:
- summary: concise, captures the main purpose and outcome of the call
- sentiment: overall emotional tone of the conversation
- topics: 2-5 short topic labels (e.g. "salary negotiation", "project deadline")
- action_items: specific commitments or tasks mentioned (who needs to do what)
- key_points: important facts or decisions from the conversation

Contact name: ${contactName ?: "Unknown"}

Transcript:
$transcript
        """.trimIndent()
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("CallMind — Analyzing")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        )
    }

    companion object {
        private const val CHANNEL_ID = "analysis"
        private const val NOTIFICATION_ID = 3
    }
}
