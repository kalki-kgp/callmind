package com.callmind.app.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callmind.app.data.local.VoskTranscriptionService
import com.callmind.app.data.local.db.entity.ProcessingStage
import com.callmind.app.data.local.db.entity.TranscriptEntity
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.remote.ConfigException
import com.callmind.app.data.remote.GeminiTranscriptionService
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.util.NotificationHelper
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
    private val userPreferences: UserPreferences,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        val call = callRepository.getCallById(callId) ?: return Result.failure()
        val recordingPath = call.recordingFilePath ?: return Result.failure()

        val useLocal = userPreferences.useLocalStt.first()
        val sttLabel = if (useLocal) "Vosk" else "Gemini"
        val contactLabel = call.contactName ?: call.phoneNumber

        callRepository.setProcessingStage(callId, ProcessingStage.TRANSCRIBING)
        try {
            setForeground(notificationHelper.processingForegroundInfo(contactLabel, ProcessingStage.TRANSCRIBING))
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
            callRepository.markTranscribed(callId)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed for call $callId (attempt $runAttemptCount): ${e.message}", e)

            // Don't retry config errors — they won't fix themselves
            if (e is ConfigException || runAttemptCount >= 3) {
                val errorMsg = e.message?.take(200) ?: "Transcription failed"
                callRepository.setProcessingError(callId, "STT ($sttLabel): $errorMsg")
                callRepository.setProcessingStage(callId, ProcessingStage.FAILED)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "TranscriptionWorker"
    }
}
