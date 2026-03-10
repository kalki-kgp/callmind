package com.callmind.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val recordingDirectory: String = "Music/Recordings/Call Recordings",
    val geminiApiKey: String = "",
    val useLocalStt: Boolean = true,
    val autoProcess: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                recordingDirectory = userPreferences.recordingDirectory.first(),
                geminiApiKey = userPreferences.geminiApiKey.first() ?: "",
                useLocalStt = userPreferences.useLocalStt.first(),
                autoProcess = userPreferences.autoProcess.first()
            )
        }
    }

    fun onRecordingDirectoryChanged(value: String) {
        _uiState.update { it.copy(recordingDirectory = value) }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(geminiApiKey = value) }
    }

    fun onUseLocalSttChanged(value: Boolean) {
        _uiState.update { it.copy(useLocalStt = value) }
    }

    fun onAutoProcessChanged(value: Boolean) {
        _uiState.update { it.copy(autoProcess = value) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            with(_uiState.value) {
                userPreferences.setRecordingDirectory(recordingDirectory)
                userPreferences.setGeminiApiKey(geminiApiKey)
                userPreferences.setUseLocalStt(useLocalStt)
                userPreferences.setAutoProcess(autoProcess)
            }
        }
    }
}
