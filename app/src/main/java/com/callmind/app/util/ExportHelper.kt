package com.callmind.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.local.db.entity.TranscriptEntity
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository
) {
    private val dateFormat = SimpleDateFormat("EEEE, MMM dd yyyy 'at' hh:mm a", Locale.getDefault())
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Export a single call's summary, transcript, and analysis as a shareable text file.
     */
    suspend fun exportCall(callId: Long): Intent? {
        val call = callRepository.getCallById(callId) ?: return null
        val transcript = callRepository.getTranscript(callId)
        val analysis = callRepository.getAnalysis(callId)

        val content = buildExportText(call, transcript, analysis)
        val fileName = "CallMind_${call.contactName ?: call.phoneNumber}_${call.id}.txt"

        return createShareIntent(content, fileName)
    }

    /**
     * Export all calls as a single text file.
     */
    suspend fun exportAllCalls(calls: List<CallEntity>): Intent? {
        val sb = StringBuilder()
        sb.appendLine("CallMind — All Call Summaries")
        sb.appendLine("Exported: ${dateFormat.format(Date())}")
        sb.appendLine("=" .repeat(60))

        for (call in calls) {
            val transcript = callRepository.getTranscript(call.id)
            val analysis = callRepository.getAnalysis(call.id)
            sb.appendLine()
            sb.append(buildExportText(call, transcript, analysis))
            sb.appendLine("-".repeat(60))
        }

        return createShareIntent(sb.toString(), "CallMind_Export_${System.currentTimeMillis()}.txt")
    }

    private fun buildExportText(
        call: CallEntity,
        transcript: TranscriptEntity?,
        analysis: CallAnalysisEntity?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Contact: ${call.contactName ?: "Unknown"}")
        sb.appendLine("Phone: ${call.phoneNumber}")
        sb.appendLine("Date: ${dateFormat.format(Date(call.timestamp))}")
        sb.appendLine("Type: ${call.callType}")
        call.durationSeconds?.let { sb.appendLine("Duration: ${it / 60}m ${it % 60}s") }
        sb.appendLine()

        if (analysis != null) {
            sb.appendLine("--- Summary ---")
            sb.appendLine(analysis.summary)
            sb.appendLine()
            sb.appendLine("Sentiment: ${analysis.sentiment}")

            val topics = try { json.decodeFromString<List<String>>(analysis.topicsJson) } catch (_: Exception) { emptyList() }
            if (topics.isNotEmpty()) {
                sb.appendLine("Topics: ${topics.joinToString(", ")}")
            }

            val actionItems = try { json.decodeFromString<List<String>>(analysis.actionItemsJson) } catch (_: Exception) { emptyList() }
            if (actionItems.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("--- Action Items ---")
                actionItems.forEach { sb.appendLine("  - $it") }
            }

            val keyPoints = try { json.decodeFromString<List<String>>(analysis.keyPointsJson) } catch (_: Exception) { emptyList() }
            if (keyPoints.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("--- Key Points ---")
                keyPoints.forEach { sb.appendLine("  - $it") }
            }
        }

        if (transcript != null && transcript.fullText.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("--- Transcript ---")
            sb.appendLine(transcript.fullText)
        }

        sb.appendLine()
        return sb.toString()
    }

    private fun createShareIntent(content: String, fileName: String): Intent {
        val dir = File(context.cacheDir, "exports")
        dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
