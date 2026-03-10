package com.callmind.app.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.callmind.app.service.worker.AnalysisWorker
import com.callmind.app.service.worker.EmbeddingWorker
import com.callmind.app.service.worker.TranscriptionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the processing pipeline for call recordings.
 * Chains: Transcription → Analysis → Embedding via WorkManager.
 */
@Singleton
class PipelineOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueue the full pipeline: transcribe → analyze → generate embeddings.
     */
    fun processCall(callId: Long) {
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val transcriptionWork = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf("call_id" to callId))
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("transcription")
            .addTag("call_$callId")
            .build()

        val analysisWork = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(workDataOf("call_id" to callId))
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("analysis")
            .addTag("call_$callId")
            .build()

        val embeddingWork = OneTimeWorkRequestBuilder<EmbeddingWorker>()
            .setInputData(workDataOf("call_id" to callId))
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("embedding")
            .addTag("call_$callId")
            .build()

        // Chain: transcription → analysis → embedding
        workManager.beginUniqueWork(
            "pipeline_$callId",
            ExistingWorkPolicy.KEEP,
            transcriptionWork
        ).then(analysisWork)
            .then(embeddingWork)
            .enqueue()
    }

    /**
     * Re-run analysis only (e.g., if transcription exists but analysis failed).
     */
    fun analyzeCall(callId: Long) {
        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val analysisWork = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(workDataOf("call_id" to callId))
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("analysis")
            .addTag("call_$callId")
            .build()

        workManager.enqueueUniqueWork(
            "analysis_$callId",
            ExistingWorkPolicy.KEEP,
            analysisWork
        )
    }

    fun cancelProcessing(callId: Long) {
        workManager.cancelUniqueWork("pipeline_$callId")
        workManager.cancelUniqueWork("analysis_$callId")
    }
}
