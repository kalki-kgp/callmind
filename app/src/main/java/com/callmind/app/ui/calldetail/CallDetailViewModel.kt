package com.callmind.app.ui.calldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.util.ExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isLoading: Boolean = true
)

@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository,
    private val exportHelper: ExportHelper
) : ViewModel() {

    private val callId: Long = savedStateHandle["callId"] ?: -1
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(CallDetailUiState())
    val uiState: StateFlow<CallDetailUiState> = _uiState.asStateFlow()

    init {
        loadCallDetails()
    }

    private fun loadCallDetails() {
        viewModelScope.launch {
            val call = callRepository.getCallById(callId) ?: return@launch
            val transcript = callRepository.getTranscript(callId)
            val analysis = callRepository.getAnalysis(callId)

            val topics = analysis?.topicsJson?.decodeJsonList() ?: emptyList()
            val keyPoints = analysis?.keyPointsJson?.decodeJsonList() ?: emptyList()

            // Load action items from their own table
            _uiState.value = CallDetailUiState(
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                callType = call.callType,
                timestamp = call.timestamp,
                durationSeconds = call.durationSeconds,
                summary = analysis?.summary,
                sentiment = analysis?.sentiment,
                topics = topics,
                keyPoints = keyPoints,
                transcript = transcript?.fullText,
                isLoading = false
            )

            // Observe action items reactively
            callRepository.getActionItems(callId).collect { items ->
                _uiState.update { state ->
                    state.copy(actionItems = items.map { item ->
                        ActionItemUi(
                            id = item.id,
                            description = item.description,
                            isCompleted = item.isCompleted
                        )
                    })
                }
            }
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
