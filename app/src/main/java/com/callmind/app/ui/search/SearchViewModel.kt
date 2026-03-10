package com.callmind.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.util.SemanticSearchEngine
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
    val matchingText: String,
    val score: Float? = null // semantic similarity score, null for text search
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val useSemanticSearch: Boolean = true,
    val searchError: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val semanticSearchEngine: SemanticSearchEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, searchError = null) }
        queryFlow.value = query

        if (query.isEmpty()) {
            _uiState.update { it.copy(results = emptyList()) }
        }
    }

    fun toggleSearchMode() {
        _uiState.update { it.copy(useSemanticSearch = !it.useSemanticSearch) }
        // Re-run search with new mode
        val query = _uiState.value.query
        if (query.length >= 2) {
            viewModelScope.launch { performSearch(query) }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, searchError = null) }

        try {
            val results = if (_uiState.value.useSemanticSearch) {
                performSemanticSearch(query)
            } else {
                performTextSearch(query)
            }
            _uiState.update { it.copy(results = results, isSearching = false) }
        } catch (e: Exception) {
            // Fall back to text search if semantic search fails
            val textResults = performTextSearch(query)
            _uiState.update {
                it.copy(
                    results = textResults,
                    isSearching = false,
                    searchError = if (it.useSemanticSearch) "Semantic search unavailable, showing text results" else null
                )
            }
        }
    }

    private suspend fun performSemanticSearch(query: String): List<SearchResult> {
        val matches = semanticSearchEngine.search(query, topK = 15, threshold = 0.25f)
        return matches.mapNotNull { match ->
            val call = callRepository.getCallById(match.callId) ?: return@mapNotNull null
            SearchResult(
                callId = call.id,
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                matchingText = match.chunkText.take(200),
                score = match.score
            )
        }
    }

    private suspend fun performTextSearch(query: String): List<SearchResult> {
        val transcripts = callRepository.searchTranscripts(query)
        return transcripts.mapNotNull { transcript ->
            val call = callRepository.getCallById(transcript.callId) ?: return@mapNotNull null
            SearchResult(
                callId = call.id,
                phoneNumber = call.phoneNumber,
                contactName = call.contactName,
                matchingText = transcript.fullText.take(200)
            )
        }
    }
}
