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
    }

    val recordingDirectory: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECORDING_DIRECTORY] ?: "Music/Recordings/Call Recordings"
    }

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.GEMINI_API_KEY]
    }

    val useLocalStt: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.USE_LOCAL_STT] ?: true
    }

    val whisperModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WHISPER_MODEL] ?: "base"
    }

    val autoProcess: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PROCESS] ?: true
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
}
