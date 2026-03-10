package com.callmind.app.data.local

import android.content.Context
import android.util.Log
import com.callmind.app.util.AudioConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voskModelManager: VoskModelManager
) {
    data class TranscriptionResult(
        val text: String,
        val language: String,
        val modelUsed: String
    )

    suspend fun transcribeAudio(audioFilePath: String): TranscriptionResult = withContext(Dispatchers.IO) {
        if (!voskModelManager.isModelDownloaded) {
            throw IllegalStateException("Vosk model not downloaded. Go to Settings to download it.")
        }

        val model = voskModelManager.getModel()

        // Convert audio to 16kHz mono PCM
        val cacheDir = File(context.cacheDir, "vosk_audio")
        cacheDir.mkdirs()
        val pcmPath = AudioConverter.convertToPcm16kMono(audioFilePath, cacheDir)

        Log.d(TAG, "Transcribing: $pcmPath")

        val recognizer = Recognizer(model, SAMPLE_RATE)
        val results = StringBuilder()

        try {
            FileInputStream(pcmPath).use { fis ->
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        val text = parseText(recognizer.result)
                        if (text.isNotBlank()) {
                            results.append(text).append(" ")
                        }
                    }
                }

                // Flush remaining
                val finalText = parseText(recognizer.finalResult)
                if (finalText.isNotBlank()) {
                    results.append(finalText)
                }
            }
        } finally {
            recognizer.close()
            // Cleanup temp PCM file if it's not the original
            if (pcmPath != audioFilePath) {
                File(pcmPath).delete()
            }
        }

        val transcript = results.toString().trim()
        if (transcript.isBlank()) {
            throw IllegalStateException("Vosk returned empty transcription")
        }

        TranscriptionResult(
            text = transcript,
            language = detectLanguage(transcript),
            modelUsed = VoskModelManager.CURRENT_MODEL_NAME
        )
    }

    private fun parseText(json: String): String {
        return try {
            JSONObject(json).optString("text", "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun detectLanguage(text: String): String {
        val hasDevanagari = text.any { it.code in 0x0900..0x097F }
        val hasLatin = text.any { it in 'a'..'z' || it in 'A'..'Z' }
        return when {
            hasDevanagari && hasLatin -> "hi-en"
            hasDevanagari -> "hi"
            else -> "en"
        }
    }

    companion object {
        private const val TAG = "VoskTranscription"
        private const val SAMPLE_RATE = 16000f
    }
}
