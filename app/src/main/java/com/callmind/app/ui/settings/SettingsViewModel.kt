package com.callmind.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.VoskModelManager
import com.callmind.app.data.local.VoskTranscriptionService
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.remote.GeminiTranscriptionService
import com.callmind.app.data.remote.OpenAiCompatibleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
    val modelDownloadProgress: Float = 0f,
    val sttTestResult: String? = null,
    val isSttTesting: Boolean = false,
    val llmTestResult: String? = null,
    val isLlmTesting: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val voskModelManager: VoskModelManager,
    private val voskTranscriptionService: VoskTranscriptionService,
    private val geminiTranscriptionService: GeminiTranscriptionService,
    private val openAiCompatibleService: OpenAiCompatibleService
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

    fun testStt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSttTesting = true, sttTestResult = null) }
            try {
                // Save current settings first so the services pick them up
                saveSettingsInternal()

                val useLocal = _uiState.value.useLocalStt
                if (useLocal) {
                    if (!voskModelManager.isModelDownloaded) {
                        _uiState.update { it.copy(isSttTesting = false, sttTestResult = "FAIL: Vosk model not downloaded") }
                        return@launch
                    }
                    // Just verify the model loads
                    voskModelManager.getModel()
                    _uiState.update { it.copy(isSttTesting = false, sttTestResult = "OK: Vosk model loaded successfully") }
                } else {
                    val apiKey = _uiState.value.geminiApiKey
                    if (apiKey.isBlank()) {
                        _uiState.update { it.copy(isSttTesting = false, sttTestResult = "FAIL: Gemini API key is empty") }
                        return@launch
                    }
                    // Test Gemini by making a simple text-only request to check key validity
                    val okhttp = okhttp3.OkHttpClient()
                    val testBody = """{"contents":[{"parts":[{"text":"Say OK"}]}]}"""
                    val request = okhttp3.Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                        .post(testBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = okhttp.newCall(request).execute()
                    if (response.isSuccessful) {
                        _uiState.update { it.copy(isSttTesting = false, sttTestResult = "OK: Gemini API key valid") }
                    } else {
                        _uiState.update { it.copy(isSttTesting = false, sttTestResult = "FAIL: Gemini ${response.code} — ${response.body?.string()?.take(100)}") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "STT test failed", e)
                _uiState.update { it.copy(isSttTesting = false, sttTestResult = "FAIL: ${e.message?.take(150)}") }
            }
        }
    }

    fun testLlm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLlmTesting = true, llmTestResult = null) }
            try {
                saveSettingsInternal()

                val provider = _uiState.value.llmProvider
                if (provider == "openai_compatible") {
                    val apiKey = _uiState.value.openAiApiKey
                    if (apiKey.isBlank()) {
                        _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "FAIL: API key is empty") }
                        return@launch
                    }
                    val result = openAiCompatibleService.generateContent("Say 'Hello from CallMind' in exactly those words.")
                    _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "OK: ${result.take(100)}") }
                } else {
                    val apiKey = _uiState.value.geminiApiKey
                    if (apiKey.isBlank()) {
                        _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "FAIL: Gemini API key is empty") }
                        return@launch
                    }
                    val okhttp = okhttp3.OkHttpClient()
                    val testBody = """{"contents":[{"parts":[{"text":"Say 'Hello from CallMind' in exactly those words."}]}]}"""
                    val request = okhttp3.Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                        .post(testBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = okhttp.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "OK: Gemini responded (${response.code})") }
                    } else {
                        _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "FAIL: ${response.code} — ${body.take(100)}") }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM test failed", e)
                _uiState.update { it.copy(isLlmTesting = false, llmTestResult = "FAIL: ${e.message?.take(150)}") }
            }
        }
    }

    private suspend fun saveSettingsInternal() {
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

    fun saveSettings() {
        viewModelScope.launch { saveSettingsInternal() }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
