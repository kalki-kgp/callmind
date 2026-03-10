package com.callmind.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val callId: Long,
    val phoneNumber: String,
    val contactName: String?,
    val matchingText: String
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query

        if (query.isEmpty()) {
            _uiState.update { it.copy(results = emptyList()) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }

        // Text-based search for now; semantic search will be added later
        val transcripts = callRepository.searchTranscripts(query)
        val results = transcripts.mapNotNull { transcript ->
            val call = callRepository.getCallById(transcript.callId) ?: return@mapNotNull null
            SearchResult(
                callId = call.id,
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                matchingText = transcript.fullText.take(200)
            )
        }

        _uiState.update { it.copy(results = results, isSearching = false) }
    }
}
