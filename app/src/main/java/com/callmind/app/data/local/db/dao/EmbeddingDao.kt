package com.callmind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callmind.app.data.local.db.entity.EmbeddingEntity

@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM embeddings WHERE callId = :callId ORDER BY chunkIndex")
    suspend fun getEmbeddingsForCall(callId: Long): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddings(): List<EmbeddingEntity>

    @Query("SELECT COUNT(*) FROM embeddings WHERE callId = :callId")
    suspend fun countEmbeddingsForCall(callId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<EmbeddingEntity>)

    @Query("DELETE FROM embeddings WHERE callId = :callId")
    suspend fun deleteByCallId(callId: Long)

    @Query("DELETE FROM embeddings")
    suspend fun deleteAll()
}
