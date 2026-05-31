package com.callmind.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val RECORDING_DIRECTORY = stringPreferencesKey("recording_directory")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val USE_LOCAL_STT = booleanPreferencesKey("use_local_stt")
        val WHISPER_MODEL = stringPreferencesKey("whisper_model")
        val AUTO_PROCESS = booleanPreferencesKey("auto_process")
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val EMBEDDING_PROVIDER = stringPreferencesKey("embedding_provider")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
    }

    val recordingDirectory: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECORDING_DIRECTORY] ?: "Music/Recordings/Call Recordings"
    }

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.GEMINI_API_KEY]
    }

    val useLocalStt: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.USE_LOCAL_STT] ?: false
    }

    val whisperModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WHISPER_MODEL] ?: "base"
    }

    val autoProcess: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PROCESS] ?: false
    }

    /** "gemini" or "openai_compatible" */
    val llmProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LLM_PROVIDER] ?: "gemini"
    }

    /** "cloud" (Gemini) or "local" (on-device) */
    val embeddingProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.EMBEDDING_PROVIDER] ?: "cloud"
    }

    val openAiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.OPENAI_BASE_URL] ?: "https://api.tokenfactory.us-central1.nebius.com/v1/"
    }

    val openAiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.OPENAI_API_KEY]
    }

    val openAiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.OPENAI_MODEL] ?: "deepseek-ai/DeepSeek-V3-0324-fast"
    }

    suspend fun setRecordingDirectory(path: String) {
        context.dataStore.edit { it[Keys.RECORDING_DIRECTORY] = path }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = key }
    }

    suspend fun setUseLocalStt(useLocal: Boolean) {
        context.dataStore.edit { it[Keys.USE_LOCAL_STT] = useLocal }
    }

    suspend fun setWhisperModel(model: String) {
        context.dataStore.edit { it[Keys.WHISPER_MODEL] = model }
    }

    suspend fun setAutoProcess(auto: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PROCESS] = auto }
    }

    suspend fun setLlmProvider(provider: String) {
        context.dataStore.edit { it[Keys.LLM_PROVIDER] = provider }
    }

    suspend fun setEmbeddingProvider(provider: String) {
        context.dataStore.edit { it[Keys.EMBEDDING_PROVIDER] = provider }
    }

    suspend fun setOpenAiBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.OPENAI_BASE_URL] = url }
    }

    suspend fun setOpenAiApiKey(key: String) {
        context.dataStore.edit { it[Keys.OPENAI_API_KEY] = key }
    }

    suspend fun setOpenAiModel(model: String) {
        context.dataStore.edit { it[Keys.OPENAI_MODEL] = model }
    }
}
