package com.callmind.app.ui.calldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.callmind.app.data.local.db.entity.ProcessingStage
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.service.PipelineOrchestrator
import com.callmind.app.util.ExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ActionItemUi(
    val id: Long,
    val description: String,
    val isCompleted: Boolean
)

data class CallDetailUiState(
    val phoneNumber: String = "",
    val contactName: String? = null,
    val callType: String = "",
    val timestamp: Long = 0,
    val durationSeconds: Int? = null,
    val summary: String? = null,
    val sentiment: String? = null,
    val topics: List<String> = emptyList(),
    val actionItems: List<ActionItemUi> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val transcript: String? = null,
    val processingError: String? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val processingStage: ProcessingStage? = null,
    val hasRecording: Boolean = false
)

@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository,
    private val exportHelper: ExportHelper,
    private val pipelineOrchestrator: PipelineOrchestrator
) : ViewModel() {

    private val callId: Long = savedStateHandle["callId"] ?: -1
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(CallDetailUiState())
    val uiState: StateFlow<CallDetailUiState> = _uiState.asStateFlow()

    init {
        loadCallDetails()
    }

    private fun loadCallDetails() {
        // Observe call + transcript + analysis + action items reactively so the
        // screen reflects pipeline progress live while it is open.
        viewModelScope.launch {
            combine(
                callRepository.observeCallById(callId),
                callRepository.observeTranscript(callId),
                callRepository.observeAnalysis(callId),
                callRepository.getActionItems(callId)
            ) { call, transcript, analysis, actionItems ->
                if (call == null) return@combine null

                val stage = ProcessingStage.fromName(call.processingStage)
                val isProcessing = call.processingError == null && stage?.isActive == true

                CallDetailUiState(
                    phoneNumber = call.phoneNumber,
                    contactName = call.contactName,
                    callType = call.callType,
                    timestamp = call.timestamp,
                    durationSeconds = call.durationSeconds,
                    summary = analysis?.summary,
                    sentiment = analysis?.sentiment,
                    topics = analysis?.topicsJson?.decodeJsonList() ?: emptyList(),
                    keyPoints = analysis?.keyPointsJson?.decodeJsonList() ?: emptyList(),
                    actionItems = actionItems.map { item ->
                        ActionItemUi(
                            id = item.id,
                            description = item.description,
                            isCompleted = item.isCompleted
                        )
                    },
                    transcript = transcript?.fullText,
                    processingError = call.processingError,
                    isLoading = false,
                    isProcessing = isProcessing,
                    processingStage = if (isProcessing) stage else null,
                    hasRecording = call.recordingFilePath != null
                )
            }.collect { state ->
                if (state != null) _uiState.value = state
            }
        }
    }

    fun processCall() {
        _uiState.update {
            it.copy(isProcessing = true, processingStage = ProcessingStage.QUEUED, processingError = null)
        }
        viewModelScope.launch {
            callRepository.clearProcessingError(callId)
            callRepository.setProcessingStage(callId, ProcessingStage.QUEUED)
            pipelineOrchestrator.processCall(callId)
        }
    }

    fun exportCall(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = exportHelper.exportCall(callId) ?: return@launch
            onReady(intent)
        }
    }

    fun toggleActionItem(id: Long, completed: Boolean) {
        viewModelScope.launch {
            callRepository.toggleActionItem(id, completed)
        }
    }

    private fun String.decodeJsonList(): List<String> {
        return try {
            json.decodeFromString<List<String>>(this)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
