package com.callmind.app.util

import android.content.Context
import android.provider.CallLog
import android.provider.MediaStore
import android.util.Log
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.service.PipelineOrchestrator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository,
    private val pipelineOrchestrator: PipelineOrchestrator
) {
    // Common OnePlus recording filename patterns
    private val filenamePatterns = listOf(
        // CallRecording_ContactName_+91XXXXXXXXXX_20260310_143022.wav
        Regex("""CallRecording[_-](.+?)[_-](\+?\d+)[_-](\d{8})[_-](\d{6})\.\w+"""),
        // Record_+91XXXXXXXXXX_20260310_143022.wav
        Regex("""Record[_-](\+?\d+)[_-](\d{8})[_-](\d{6})\.\w+"""),
        // ContactName(+91XXXXXXXXXX)_20260310_143022.wav
        Regex("""(.+?)\((\+?\d+)\)[_-](\d{8})[_-](\d{6})\.\w+"""),
    )

    /**
     * Query MediaStore for recordings in the configured directory,
     * skip already-processed ones, insert new calls, and trigger the pipeline.
     */
    suspend fun checkForNewRecordings(recordingDir: String) {
        val allFiles = queryRecordings(recordingDir)

        for (file in allFiles) {
            try {
                if (callRepository.isRecordingProcessed(file.path)) continue

                val callEntity = matchToCallLog(file) ?: createCallFromFilename(file)
                val callId = callRepository.insertCall(callEntity)

                pipelineOrchestrator.processCall(callId)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recording: ${file.name}", e)
            }
        }
    }

    private fun queryRecordings(recordingDir: String): List<RecordingFile> {
        val files = mutableListOf<RecordingFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("$recordingDir%")

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                files.add(
                    RecordingFile(
                        name = cursor.getString(nameCol) ?: continue,
                        path = cursor.getString(pathCol) ?: continue,
                        dateAdded = cursor.getLong(dateCol) * 1000, // seconds to millis
                        durationMs = cursor.getLong(durationCol)
                    )
                )
            }
        }
        return files
    }

    /**
     * Try to match recording to a call log entry by timestamp + phone number.
     */
    private fun matchToCallLog(recording: RecordingFile): CallEntity? {
        val phoneNumber = extractPhoneNumber(recording.name)

        val windowMs = 5 * 60 * 1000L
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.CACHED_NAME
        )

        val selection = buildString {
            append("${CallLog.Calls.DATE} BETWEEN ? AND ?")
            if (phoneNumber != null) {
                append(" AND ${CallLog.Calls.NUMBER} LIKE ?")
            }
        }

        val args = mutableListOf(
            (recording.dateAdded - windowMs).toString(),
            (recording.dateAdded + windowMs).toString()
        )
        if (phoneNumber != null) {
            args.add("%${phoneNumber.takeLast(10)}")
        }

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args.toTypedArray(),
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val number = cursor.getString(0) ?: return null
                val date = cursor.getLong(1)
                val duration = cursor.getInt(2)
                val type = cursor.getInt(3)
                val name = cursor.getString(4)

                return CallEntity(
                    phoneNumber = number,
                    contactName = name,
                    callType = mapCallType(type),
                    timestamp = date,
                    durationSeconds = duration,
                    recordingFilePath = recording.path
                )
            }
        }
        return null
    }

    /**
     * Fallback: create a CallEntity from filename alone when call log matching fails.
     */
    private fun createCallFromFilename(recording: RecordingFile): CallEntity {
        val phoneNumber = extractPhoneNumber(recording.name) ?: "Unknown"
        val contactName = extractContactName(recording.name)

        return CallEntity(
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = "UNKNOWN",
            timestamp = recording.dateAdded,
            durationSeconds = (recording.durationMs / 1000).toInt(),
            recordingFilePath = recording.path
        )
    }

    private fun extractPhoneNumber(filename: String): String? {
        for (pattern in filenamePatterns) {
            val match = pattern.find(filename) ?: continue
            for (i in 1..match.groupValues.lastIndex) {
                val group = match.groupValues[i]
                if (group.matches(Regex("""\+?\d{10,13}"""))) {
                    return group
                }
            }
        }
        return null
    }

    private fun extractContactName(filename: String): String? {
        val firstPattern = filenamePatterns[0]
        val match = firstPattern.find(filename) ?: return null
        val name = match.groupValues.getOrNull(1) ?: return null
        // Don't return if it looks like a phone number
        return if (name.matches(Regex("""\+?\d+"""))) null else name
    }

    private fun mapCallType(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE -> "MISSED"
        else -> "UNKNOWN"
    }

    data class RecordingFile(
        val name: String,
        val path: String,
        val dateAdded: Long,
        val durationMs: Long
    )

    companion object {
        private const val TAG = "RecordingFileParser"
    }
}
