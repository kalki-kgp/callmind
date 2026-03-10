package com.callmind.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "embeddings",
    foreignKeys = [ForeignKey(
        entity = CallEntity::class,
        parentColumns = ["id"],
        childColumns = ["callId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("callId")]
)
data class EmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: Long,
    val chunkIndex: Int, // which chunk of the transcript
    val chunkText: String, // the text this embedding represents
    val embedding: ByteArray, // serialized FloatArray (768 dims for text-embedding-004)
    val modelUsed: String = "text-embedding-004",
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
