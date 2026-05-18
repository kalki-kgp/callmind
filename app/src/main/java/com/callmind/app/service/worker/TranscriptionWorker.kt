package com.callmind.app.service.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.callmind.app.R
import com.callmind.app.data.local.VoskTranscriptionService
import com.callmind.app.data.local.db.entity.TranscriptEntity
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.remote.GeminiTranscriptionService
import com.callmind.app.data.repository.CallRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository,
    private val geminiTranscriptionService: GeminiTranscriptionService,
    private val voskTranscriptionService: VoskTranscriptionService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        val call = callRepository.getCallById(callId) ?: return Result.failure()
        val recordingPath = call.recordingFilePath ?: return Result.failure()

        val useLocal = userPreferences.useLocalStt.first()
        val sttLabel = if (useLocal) "Vosk" else "Gemini"

        try {
            setForeground(createForegroundInfo("Transcribing ($sttLabel): ${call.contactName ?: call.phoneNumber}"))
        } catch (_: Exception) { }

        return try {
            callRepository.clearProcessingError(callId)
            Log.d(TAG, "Starting transcription for call $callId using $sttLabel")

            val (text, language, modelUsed) = if (useLocal) {
                val result = voskTranscriptionService.transcribeAudio(recordingPath)
                Triple(result.text, result.language, result.modelUsed)
            } else {
                val result = geminiTranscriptionService.transcribeAudio(recordingPath)
                Triple(result.text, result.language, result.modelUsed)
            }

            Log.d(TAG, "Transcription complete for call $callId: ${text.take(100)}...")

            val transcript = TranscriptEntity(
                callId = callId,
                fullText = text,
                language = language,
                modelUsed = modelUsed
            )
            callRepository.insertTranscript(transcript)
            callRepository.updateCall(call.copy(isTranscribed = true, processingError = null))

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed for call $callId (attempt $runAttemptCount): ${e.message}", e)

            // Don't retry config errors — they won't fix themselves
            val isConfigError = e is IllegalStateException && e.message?.let {
                it.contains("not configured") || it.contains("not downloaded")
            } == true

            if (isConfigError || runAttemptCount >= 3) {
                val errorMsg = e.message?.take(200) ?: "Transcription failed"
                callRepository.setProcessingError(callId, "STT ($sttLabel): $errorMsg")
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("CallMind — Transcribing")
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
        private const val TAG = "TranscriptionWorker"
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 2
    }
}
