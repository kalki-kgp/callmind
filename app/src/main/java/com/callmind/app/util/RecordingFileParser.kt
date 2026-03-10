package com.callmind.app.util

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import android.provider.MediaStore
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.repository.CallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRepository: CallRepository
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

    suspend fun checkForNewRecordings(recordingDir: String) {
        val newFiles = queryNewRecordings(recordingDir)
        for (file in newFiles) {
            val callInfo = matchToCallLog(file)
            if (callInfo != null) {
                callRepository.insertCall(callInfo)
            }
        }
    }

    private fun queryNewRecordings(recordingDir: String): List<RecordingFile> {
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
                        name = cursor.getString(nameCol),
                        path = cursor.getString(pathCol),
                        dateAdded = cursor.getLong(dateCol) * 1000, // seconds to millis
                        durationMs = cursor.getLong(durationCol)
                    )
                )
            }
        }
        return files
    }

    private fun matchToCallLog(recording: RecordingFile): CallEntity? {
        // Try to extract phone number from filename
        val phoneNumber = extractPhoneNumber(recording.name)

        // Match against call log within a time window
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
                val number = cursor.getString(0)
                val date = cursor.getLong(1)
                val duration = cursor.getInt(2)
                val type = cursor.getInt(3)
                val name = cursor.getString(4)

                val callType = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "UNKNOWN"
                }

                return CallEntity(
                    phoneNumber = number,
                    contactName = name,
                    callType = callType,
                    timestamp = date,
                    durationSeconds = duration,
                    recordingFilePath = recording.path
                )
            }
        }
        return null
    }

    private fun extractPhoneNumber(filename: String): String? {
        for (pattern in filenamePatterns) {
            val match = pattern.find(filename) ?: continue
            // Find the group that looks like a phone number (digits, +, -)
            for (i in 1..match.groupValues.lastIndex) {
                val group = match.groupValues[i]
                if (group.matches(Regex("""\+?\d{10,13}"""))) {
                    return group
                }
            }
        }
        return null
    }

    data class RecordingFile(
        val name: String,
        val path: String,
        val dateAdded: Long,
        val durationMs: Long
    )
}
