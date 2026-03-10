package com.callmind.app.ui.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ContactCallItem(
    val callId: Long,
    val callType: String,
    val timestamp: Long,
    val summary: String?
)

data class ContactUiState(
    val contactName: String = "",
    val totalCalls: Int = 0,
    val topics: List<String> = emptyList(),
    val calls: List<ContactCallItem> = emptyList()
)

@HiltViewModel
class ContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository
) : ViewModel() {

    private val contactName: String = savedStateHandle["contactName"] ?: ""

    val uiState = callRepository.getCallsByContact(contactName)
        .map { calls ->
            val allTopics = mutableSetOf<String>()
            val callItems = calls.map { call ->
                val analysis = callRepository.getAnalysis(call.id)
                analysis?.topicsJson?.let { json ->
                    try {
                        allTopics.addAll(Json.decodeFromString<List<String>>(json))
                    } catch (_: Exception) {}
                }
                ContactCallItem(
                    callId = call.id,
                    callType = call.callType,
                    timestamp = call.timestamp,
                    summary = analysis?.summary
                )
            }
            ContactUiState(
                contactName = contactName,
                totalCalls = calls.size,
                topics = allTopics.toList(),
                calls = callItems
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactUiState(contactName = contactName))
}
