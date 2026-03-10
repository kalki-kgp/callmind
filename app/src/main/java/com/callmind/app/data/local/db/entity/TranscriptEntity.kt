package com.callmind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcripts",
    foreignKeys = [ForeignKey(
        entity = CallEntity::class,
        parentColumns = ["id"],
        childColumns = ["callId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("callId", unique = true)]
)
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: Long,
    val fullText: String,
    val language: String? = null,
    val confidence: Float? = null,
    val modelUsed: String? = null, // "whisper-base", "gemini", etc.
    val transcribedAt: Long = System.currentTimeMillis()
)
