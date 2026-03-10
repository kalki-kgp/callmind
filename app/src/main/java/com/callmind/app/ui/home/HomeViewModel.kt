package com.callmind.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.service.RecordingMonitorService
import com.callmind.app.util.RecordingFileParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallUiItem(
    val id: Long,
    val phoneNumber: String,
    val contactName: String?,
    val callType: String,
    val timestamp: Long,
    val durationSeconds: Int?,
    val summary: String?,
    val isProcessing: Boolean = false
)

data class HomeUiState(
    val calls: List<CallUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository,
    private val recordingFileParser: RecordingFileParser,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)

    val uiState = combine(
        callRepository.getAllCalls(),
        _isScanning
    ) { calls, scanning ->
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
                    summary = analysis?.summary,
                    isProcessing = !call.isAnalyzed && call.recordingFilePath != null
                )
            },
            isLoading = false,
            isScanning = scanning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun scanForRecordings() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val dir = userPreferences.recordingDirectory.first()
                recordingFileParser.checkForNewRecordings(dir)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun startMonitorService() {
        val intent = Intent(context, RecordingMonitorService::class.java)
        context.startForegroundService(intent)
    }
}
