package com.callmind.app.service.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.callmind.app.R
import com.callmind.app.data.local.db.dao.EmbeddingDao
import com.callmind.app.data.local.db.entity.EmbeddingEntity
import com.callmind.app.data.remote.GeminiEmbeddingService
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.util.SemanticSearchEngine.Companion.toByteArray
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Generates embeddings for a call's transcript and stores them in Room.
 * Chunks the transcript into ~500 char pieces for granular search.
 */
@HiltWorker
class EmbeddingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository,
    private val embeddingDao: EmbeddingDao,
    private val embeddingService: GeminiEmbeddingService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val callId = inputData.getLong("call_id", -1)
        if (callId == -1L) return Result.failure()

        // Skip if embeddings already exist
        if (embeddingDao.countEmbeddingsForCall(callId) > 0) return Result.success()

        val transcript = callRepository.getTranscript(callId) ?: return Result.failure()
        if (transcript.fullText.isBlank()) return Result.failure()

        val call = callRepository.getCallById(callId)
        try {
            setForeground(createForegroundInfo("Indexing: ${call?.contactName ?: call?.phoneNumber ?: "call"}"))
        } catch (_: Exception) { }

        return try {
            // Chunk the transcript
            val chunks = chunkText(transcript.fullText)
            if (chunks.isEmpty()) return Result.success()

            // Generate embeddings in batch
            val vectors = embeddingService.embed(chunks)

            // Store embeddings
            val entities = chunks.mapIndexed { index, chunk ->
                EmbeddingEntity(
                    callId = callId,
                    chunkIndex = index,
                    chunkText = chunk,
                    embedding = vectors[index].toByteArray()
                )
            }
            embeddingDao.insertAll(entities)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                callRepository.setProcessingError(callId, e.message?.take(200) ?: "Embedding failed")
                Result.failure()
            }
        }
    }

    /**
     * Split transcript into chunks of ~500 chars, breaking at sentence boundaries.
     */
    private fun chunkText(text: String, maxChunkSize: Int = 500): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[.!?।])\\s+"))
        val current = StringBuilder()

        for (sentence in sentences) {
            if (current.length + sentence.length > maxChunkSize && current.isNotEmpty()) {
                chunks.add(current.toString().trim())
                current.clear()
            }
            current.append(sentence).append(" ")
        }
        if (current.isNotBlank()) {
            chunks.add(current.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "analysis")
            .setContentTitle("CallMind — Indexing")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            4,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        )
    }
}
