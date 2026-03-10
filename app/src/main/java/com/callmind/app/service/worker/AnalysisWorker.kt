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
import com.callmind.app.data.remote.model.Content
import com.callmind.app.data.remote.model.GeminiRequest
import com.callmind.app.data.remote.model.Part
import com.callmind.app.data.remote.model.extractText
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.repository.CallRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository,
    private val geminiApiService: GeminiApiService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        val call = callRepository.getCallById(callId) ?: return Result.failure()
        val transcript = callRepository.getTranscript(callId) ?: return Result.failure()

        setForeground(createForegroundInfo("Analyzing: ${call.contactName ?: call.phoneNumber}"))

        return try {
            val apiKey = userPreferences.geminiApiKey.first() ?: return Result.failure()

            val prompt = buildAnalysisPrompt(transcript.fullText, call.contactName)
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val response = geminiApiService.generateContent(apiKey, request)
            val analysisText = response.extractText() ?: return Result.retry()

            // TODO: Parse the structured JSON response from Gemini
            val analysis = CallAnalysisEntity(
                callId = callId,
                summary = analysisText,
                sentiment = "NEUTRAL",
                topicsJson = "[]",
                actionItemsJson = "[]",
                keyPointsJson = "[]",
                modelUsed = "gemini-2.0-flash"
            )
            callRepository.insertAnalysis(analysis)
            callRepository.updateCall(call.copy(isAnalyzed = true))

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun buildAnalysisPrompt(transcript: String, contactName: String?): String {
        return """
            Analyze this phone call transcript and return a JSON response with:
            1. "summary": A 2-3 sentence summary of the call
            2. "sentiment": One of POSITIVE, NEGATIVE, NEUTRAL, MIXED
            3. "topics": Array of topic strings discussed
            4. "action_items": Array of action item strings
            5. "key_points": Array of key points from the conversation

            Contact: ${contactName ?: "Unknown"}
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
