package com.callmind.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CallUiItem(
    val id: Long,
    val phoneNumber: String,
    val contactName: String?,
    val callType: String,
    val timestamp: Long,
    val durationSeconds: Int?,
    val summary: String?
)

data class HomeUiState(
    val calls: List<CallUiItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    val uiState = callRepository.getAllCalls()
        .map { calls ->
            HomeUiState(
                calls = calls.map { call ->
                    val analysis = callRepository.getAnalysis(call.id)
                    CallUiItem(
                        id = call.id,
                        phoneNumber = call.phoneNumber,
                        contactName = call.contactName,
                        callType = call.callType,
                        timestamp = call.timestamp,
                        durationSeconds = call.durationSeconds,
                        summary = analysis?.summary
                    )
                },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
