package com.callmind.app.data.repository

import com.callmind.app.data.local.db.dao.AnalysisDao
import com.callmind.app.data.local.db.dao.CallDao
import com.callmind.app.data.local.db.dao.EmbeddingDao
import com.callmind.app.data.local.db.dao.TranscriptDao
import com.callmind.app.data.local.db.entity.ActionItemEntity
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.local.db.entity.TranscriptEntity
import com.callmind.app.service.PipelineOrchestrator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val callDao: CallDao,
    private val transcriptDao: TranscriptDao,
    private val analysisDao: AnalysisDao,
    private val embeddingDao: EmbeddingDao,
    private val pipelineOrchestrator: PipelineOrchestrator
) {
    // Calls
    fun getAllCalls(): Flow<List<CallEntity>> = callDao.getAllCalls()
    fun getCallsByContact(contactName: String): Flow<List<CallEntity>> = callDao.getCallsByContactName(contactName)
    fun getAllContacts(): Flow<List<String>> = callDao.getAllContacts()
    suspend fun getCallById(id: Long): CallEntity? = callDao.getCallById(id)
    suspend fun insertCall(call: CallEntity): Long = callDao.insert(call)
    suspend fun updateCall(call: CallEntity) = callDao.update(call)
    suspend fun deleteCall(id: Long) = callDao.deleteById(id)
    suspend fun getUntranscribedCalls(): List<CallEntity> = callDao.getUntranscribedCalls()
    suspend fun getUnanalyzedCalls(): List<CallEntity> = callDao.getUnanalyzedCalls()
    suspend fun isRecordingProcessed(path: String): Boolean = callDao.countByRecordingPath(path) > 0

    // Transcripts
    suspend fun getTranscript(callId: Long): TranscriptEntity? = transcriptDao.getTranscriptForCall(callId)
    fun observeTranscript(callId: Long): Flow<TranscriptEntity?> = transcriptDao.observeTranscriptForCall(callId)
    suspend fun insertTranscript(transcript: TranscriptEntity): Long = transcriptDao.insert(transcript)
    suspend fun searchTranscripts(query: String): List<TranscriptEntity> = transcriptDao.searchTranscripts(query)

    // Analysis
    suspend fun getAnalysis(callId: Long): CallAnalysisEntity? = analysisDao.getAnalysisForCall(callId)
    fun observeAnalysis(callId: Long): Flow<CallAnalysisEntity?> = analysisDao.observeAnalysisForCall(callId)
    suspend fun insertAnalysis(analysis: CallAnalysisEntity): Long = analysisDao.insertAnalysis(analysis)
    fun getAllAnalyses(): Flow<List<CallAnalysisEntity>> = analysisDao.getAllAnalyses()

    // Action Items
    fun getActionItems(callId: Long): Flow<List<ActionItemEntity>> = analysisDao.getActionItemsForCall(callId)
    fun getPendingActionItems(): Flow<List<ActionItemEntity>> = analysisDao.getPendingActionItems()
    suspend fun insertActionItems(items: List<ActionItemEntity>) = analysisDao.insertActionItems(items)
    suspend fun deleteActionItemsForCall(callId: Long) = analysisDao.deleteActionItemsForCall(callId)
    suspend fun toggleActionItem(id: Long, completed: Boolean) = analysisDao.updateActionItemCompletion(id, completed)

    // Embeddings
    /**
     * Wipes the entire vector index and re-enqueues embedding for every analyzed call.
     * Used when the embedding provider changes — vectors aren't comparable across models.
     */
    suspend fun reindexAllEmbeddings() {
        embeddingDao.deleteAll()
        callDao.getAnalyzedCalls().forEach { pipelineOrchestrator.embedCall(it.id) }
    }

    // Processing errors
    suspend fun setProcessingError(callId: Long, error: String) = callDao.setProcessingError(callId, error)
    suspend fun clearProcessingError(callId: Long) = callDao.clearProcessingError(callId)
}
