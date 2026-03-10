package com.callmind.app.service.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.callmind.app.R
import com.callmind.app.data.local.db.entity.TranscriptEntity
import com.callmind.app.data.repository.CallRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        val call = callRepository.getCallById(callId) ?: return Result.failure()
        val recordingPath = call.recordingFilePath ?: return Result.failure()

        setForeground(createForegroundInfo("Transcribing: ${call.contactName ?: call.phoneNumber}"))

        return try {
            // TODO: Integrate whisper.cpp or cloud STT here
            // For now, this is the pipeline stub:
            // 1. Read audio file from recordingPath
            // 2. Run through whisper.cpp / cloud API
            // 3. Save transcript

            val transcript = TranscriptEntity(
                callId = callId,
                fullText = "", // Will be populated by STT engine
                modelUsed = "whisper-base"
            )
            callRepository.insertTranscript(transcript)
            callRepository.updateCall(call.copy(isTranscribed = true))

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
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
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 2
    }
}
