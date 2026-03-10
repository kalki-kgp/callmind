package com.callmind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_items",
    foreignKeys = [ForeignKey(
        entity = CallEntity::class,
        parentColumns = ["id"],
        childColumns = ["callId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("callId")]
)
data class ActionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: Long,
    val description: String,
    val assignedTo: String? = null,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM" // LOW, MEDIUM, HIGH
)
