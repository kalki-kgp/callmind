package com.callmind.app.ui.calldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class CallDetailUiState(
    val phoneNumber: String = "",
    val contactName: String? = null,
    val callType: String = "",
    val timestamp: Long = 0,
    val durationSeconds: Int? = null,
    val summary: String? = null,
    val sentiment: String? = null,
    val topics: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val transcript: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository
) : ViewModel() {

    private val callId: Long = savedStateHandle["callId"] ?: -1

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

            val topics = analysis?.topicsJson?.let { json ->
                try { Json.decodeFromString<List<String>>(json) } catch (_: Exception) { emptyList() }
            } ?: emptyList()

            val actionItems = analysis?.actionItemsJson?.let { json ->
                try { Json.decodeFromString<List<String>>(json) } catch (_: Exception) { emptyList() }
            } ?: emptyList()

            _uiState.value = CallDetailUiState(
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                callType = call.callType,
                timestamp = call.timestamp,
                durationSeconds = call.durationSeconds,
                summary = analysis?.summary,
                sentiment = analysis?.sentiment,
                topics = topics,
                actionItems = actionItems,
                transcript = transcript?.fullText,
                isLoading = false
            )
        }
    }
}
