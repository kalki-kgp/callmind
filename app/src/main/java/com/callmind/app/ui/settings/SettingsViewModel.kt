package com.callmind.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.VoskModelManager
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
    val useLocalStt: Boolean = false,
    val autoProcess: Boolean = false,
    val llmProvider: String = "gemini",
    val openAiBaseUrl: String = "https://api.tokenfactory.us-central1.nebius.com/v1/",
    val openAiApiKey: String = "",
    val openAiModel: String = "deepseek-ai/DeepSeek-V3-0324-fast",
    val isVoskModelDownloaded: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val modelDownloadProgress: Float = 0f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val voskModelManager: VoskModelManager
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
                autoProcess = userPreferences.autoProcess.first(),
                llmProvider = userPreferences.llmProvider.first(),
                openAiBaseUrl = userPreferences.openAiBaseUrl.first(),
                openAiApiKey = userPreferences.openAiApiKey.first() ?: "",
                openAiModel = userPreferences.openAiModel.first(),
                isVoskModelDownloaded = voskModelManager.isModelDownloaded
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

    fun onLlmProviderChanged(value: String) {
        _uiState.update { it.copy(llmProvider = value) }
    }

    fun onOpenAiBaseUrlChanged(value: String) {
        _uiState.update { it.copy(openAiBaseUrl = value) }
    }

    fun onOpenAiApiKeyChanged(value: String) {
        _uiState.update { it.copy(openAiApiKey = value) }
    }

    fun onOpenAiModelChanged(value: String) {
        _uiState.update { it.copy(openAiModel = value) }
    }

    fun downloadVoskModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingModel = true, modelDownloadProgress = 0f) }
            val success = voskModelManager.downloadModel { progress ->
                _uiState.update { it.copy(modelDownloadProgress = progress) }
            }
            _uiState.update {
                it.copy(
                    isDownloadingModel = false,
                    isVoskModelDownloaded = success,
                    modelDownloadProgress = if (success) 1f else 0f
                )
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            with(_uiState.value) {
                userPreferences.setRecordingDirectory(recordingDirectory)
                userPreferences.setGeminiApiKey(geminiApiKey)
                userPreferences.setUseLocalStt(useLocalStt)
                userPreferences.setAutoProcess(autoProcess)
                userPreferences.setLlmProvider(llmProvider)
                userPreferences.setOpenAiBaseUrl(openAiBaseUrl)
                userPreferences.setOpenAiApiKey(openAiApiKey)
                userPreferences.setOpenAiModel(openAiModel)
            }
        }
    }
}
