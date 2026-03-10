package com.callmind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_analysis",
    foreignKeys = [ForeignKey(
        entity = CallEntity::class,
        parentColumns = ["id"],
        childColumns = ["callId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("callId", unique = true)]
)
data class CallAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: Long,
    val summary: String,
    val sentiment: String, // POSITIVE, NEGATIVE, NEUTRAL, MIXED
    val topicsJson: String, // JSON array of topic strings
    val actionItemsJson: String, // JSON array of action item strings
    val keyPointsJson: String, // JSON array
    val modelUsed: String, // "gemini-2.0-flash", etc.
    val analyzedAt: Long = System.currentTimeMillis()
)
