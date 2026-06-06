package com.callmind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: String, // INCOMING, OUTGOING, MISSED
    val timestamp: Long,
    val durationSeconds: Int? = null,
    val recordingFilePath: String? = null,
    val isTranscribed: Boolean = false,
    val isAnalyzed: Boolean = false,
    val processingError: String? = null,
    /** Active pipeline stage name (see [ProcessingStage]); null when idle. */
    val processingStage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
