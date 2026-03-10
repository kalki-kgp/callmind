package com.callmind.app.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.service.PipelineOrchestrator
import com.callmind.app.service.RecordingMonitorService
import com.callmind.app.util.RecordingFileParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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
    private val userPreferences: UserPreferences,
    private val pipelineOrchestrator: PipelineOrchestrator
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

    /**
     * Import a manually selected audio file and start processing.
     */
    fun importRecording(uri: Uri) {
        viewModelScope.launch {
            // Copy the file to app's internal storage
            val fileName = getFileName(uri) ?: "imported_${System.currentTimeMillis()}.wav"
            val importDir = File(context.filesDir, "imports")
            importDir.mkdirs()
            val destFile = File(importDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@launch

            // Create a call entry for the imported file
            val call = CallEntity(
                phoneNumber = "Imported",
                contactName = fileName.substringBeforeLast("."),
                callType = "IMPORTED",
                timestamp = System.currentTimeMillis(),
                recordingFilePath = destFile.absolutePath
            )
            val callId = callRepository.insertCall(call)
            pipelineOrchestrator.processCall(callId)
        }
    }

    fun startMonitorService() {
        val intent = Intent(context, RecordingMonitorService::class.java)
        context.startForegroundService(intent)
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
