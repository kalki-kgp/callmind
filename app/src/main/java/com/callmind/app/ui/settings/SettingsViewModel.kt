package com.callmind.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callmind.app.data.local.EmbeddingModelManager
import com.callmind.app.data.local.VoskModelManager
import com.callmind.app.data.local.VoskTranscriptionService
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.remote.GeminiEmbeddingService
import com.callmind.app.data.remote.GeminiTranscriptionService
import com.callmind.app.data.remote.LocalEmbeddingService
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
    val embeddingProvider: String = "cloud",
    val openAiBaseUrl: String = "https://api.tokenfactory.us-central1.nebius.com/v1/",
    val openAiApiKey: String = "",
    val openAiModel: String = "deepseek-ai/DeepSeek-V3-0324-fast",
    val isVoskModelDownloaded: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val modelDownloadProgress: Float = 0f,
    val isEmbeddingModelDownloaded: Boolean = false,
    val isDownloadingEmbeddingModel: Boolean = false,
    val embeddingModelDownloadProgress: Float = 0f,
    val testResults: List<ConnectionTestResult> = emptyList(),
    val runningTestId: String? = null
)

data class ConnectionTestResult(
    val id: String,
    val label: String,
    val success: Boolean,
    val detail: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val voskModelManager: VoskModelManager,
    private val voskTranscriptionService: VoskTranscriptionService,
    private val geminiTranscriptionService: GeminiTranscriptionService,
    private val openAiCompatibleService: OpenAiCompatibleService,
    private val embeddingModelManager: EmbeddingModelManager,
    private val geminiEmbeddingService: GeminiEmbeddingService,
    private val localEmbeddingService: LocalEmbeddingService
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
                embeddingProvider = userPreferences.embeddingProvider.first(),
                openAiBaseUrl = userPreferences.openAiBaseUrl.first(),
                openAiApiKey = userPreferences.openAiApiKey.first() ?: "",
                openAiModel = userPreferences.openAiModel.first(),
                isVoskModelDownloaded = voskModelManager.isModelDownloaded,
                isEmbeddingModelDownloaded = embeddingModelManager.isModelDownloaded
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

    fun onEmbeddingProviderChanged(value: String) {
        _uiState.update { it.copy(embeddingProvider = value) }
    }

    fun downloadEmbeddingModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingEmbeddingModel = true, embeddingModelDownloadProgress = 0f) }
            val success = embeddingModelManager.downloadModel { progress ->
                _uiState.update { it.copy(embeddingModelDownloadProgress = progress) }
            }
            _uiState.update {
                it.copy(
                    isDownloadingEmbeddingModel = false,
                    isEmbeddingModelDownloaded = success,
                    embeddingModelDownloadProgress = if (success) 1f else 0f
                )
            }
        }
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

    // --- Connection tests -------------------------------------------------
    // Each test targets one method explicitly (cloud vs local) so failures are
    // isolated. Results carry full, untruncated detail so they can be copied.

    fun clearTestResults() {
        _uiState.update { it.copy(testResults = emptyList()) }
    }

    fun runAllTests() {
        testSttCloud(); testSttLocal()
        testLlmCloud(); testLlmOpenAi()
        testEmbedCloud(); testEmbedLocal()
    }

    fun testSttCloud() = runTest(TEST_STT_CLOUD, "STT · Cloud (Gemini)") {
        val apiKey = _uiState.value.geminiApiKey
        require(apiKey.isNotBlank()) { "Gemini API key is empty" }
        val body = geminiGenerate(apiKey, "Reply with the single word: OK")
        "Reachable — model gemini-flash-lite-latest responded.\n" +
            "Note: validates the key + model, not audio transcription.\n\n$body"
    }

    fun testSttLocal() = runTest(TEST_STT_LOCAL, "STT · Local (Vosk)") {
        require(voskModelManager.isModelDownloaded) {
            "Vosk model not downloaded. Download it under Speech-to-Text first."
        }
        voskModelManager.getModel()
        "Vosk model loaded successfully (${VoskModelManager.CURRENT_MODEL_NAME})."
    }

    fun testLlmCloud() = runTest(TEST_LLM_CLOUD, "LLM · Cloud (Gemini)") {
        val apiKey = _uiState.value.geminiApiKey
        require(apiKey.isNotBlank()) { "Gemini API key is empty" }
        val body = geminiGenerate(apiKey, "Reply with the single word: pong")
        "Gemini responded.\n\n$body"
    }

    fun testLlmOpenAi() = runTest(TEST_LLM_OPENAI, "LLM · OpenAI-compatible") {
        require(_uiState.value.openAiApiKey.isNotBlank()) {
            "OpenAI-compatible API key is empty"
        }
        val result = openAiCompatibleService.generateContent("Reply with the single word: pong")
        "Endpoint responded:\n$result"
    }

    fun testEmbedCloud() = runTest(TEST_EMBED_CLOUD, "Embeddings · Cloud (Gemini)") {
        require(_uiState.value.geminiApiKey.isNotBlank()) { "Gemini API key is empty" }
        val v = geminiEmbeddingService.embedSingle("CallMind embedding connection test")
        "OK — produced a ${v.size}-dim vector.\nFirst values: ${v.take(5).joinToString(", ")}"
    }

    fun testEmbedLocal() = runTest(TEST_EMBED_LOCAL, "Embeddings · Local (on-device)") {
        require(embeddingModelManager.isModelDownloaded) {
            "On-device embedding model not downloaded. Download it under Search Embeddings first."
        }
        val v = localEmbeddingService.embedSingle("CallMind embedding connection test")
        "OK — produced a ${v.size}-dim vector.\nFirst values: ${v.take(5).joinToString(", ")}"
    }

    /** Runs [block], saving settings first, and records the outcome with full detail. */
    private fun runTest(id: String, label: String, block: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(runningTestId = id) }
            saveSettingsInternal()
            val result = try {
                ConnectionTestResult(id, label, success = true, detail = block())
            } catch (e: Exception) {
                Log.e(TAG, "$label test failed", e)
                ConnectionTestResult(id, label, success = false, detail = formatError(e))
            }
            _uiState.update { state ->
                val others = state.testResults.filterNot { it.id == id }
                state.copy(
                    runningTestId = if (state.runningTestId == id) null else state.runningTestId,
                    testResults = (others + result).sortedBy { TEST_ORDER.indexOf(it.id) }
                )
            }
        }
    }

    /** Calls Gemini generateContent; returns the raw body on success, throws with code+body on failure. */
    private fun geminiGenerate(apiKey: String, prompt: String): String {
        val okhttp = okhttp3.OkHttpClient()
        val testBody = """{"contents":[{"parts":[{"text":"$prompt"}]}]}"""
        val request = okhttp3.Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=$apiKey")
            .post(testBody.toRequestBody("application/json".toMediaType()))
            .build()
        okhttp.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} ${response.message}\n$body")
            }
            return body
        }
    }

    /** Full, copyable error text: type + message + cause chain. */
    private fun formatError(e: Throwable): String {
        val sb = StringBuilder()
        var cur: Throwable? = e
        var depth = 0
        while (cur != null && depth < 5) {
            if (depth > 0) sb.append("\nCaused by: ")
            sb.append(cur::class.java.simpleName)
            cur.message?.let { sb.append(": ").append(it) }
            cur = cur.cause
            depth++
        }
        return sb.toString()
    }

    private suspend fun saveSettingsInternal() {
        with(_uiState.value) {
            userPreferences.setRecordingDirectory(recordingDirectory)
            userPreferences.setGeminiApiKey(geminiApiKey)
            userPreferences.setUseLocalStt(useLocalStt)
            userPreferences.setAutoProcess(autoProcess)
            userPreferences.setLlmProvider(llmProvider)
            userPreferences.setEmbeddingProvider(embeddingProvider)
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

        private const val TEST_STT_CLOUD = "stt_cloud"
        private const val TEST_STT_LOCAL = "stt_local"
        private const val TEST_LLM_CLOUD = "llm_cloud"
        private const val TEST_LLM_OPENAI = "llm_openai"
        private const val TEST_EMBED_CLOUD = "embed_cloud"
        private const val TEST_EMBED_LOCAL = "embed_local"

        private val TEST_ORDER = listOf(
            TEST_STT_CLOUD, TEST_STT_LOCAL,
            TEST_LLM_CLOUD, TEST_LLM_OPENAI,
            TEST_EMBED_CLOUD, TEST_EMBED_LOCAL
        )
    }
}
