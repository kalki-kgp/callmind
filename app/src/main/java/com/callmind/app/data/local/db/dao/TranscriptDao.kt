package com.callmind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callmind.app.data.local.db.entity.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcripts WHERE callId = :callId")
    suspend fun getTranscriptForCall(callId: Long): TranscriptEntity?

    @Query("SELECT * FROM transcripts WHERE callId = :callId")
    fun observeTranscriptForCall(callId: Long): Flow<TranscriptEntity?>

    @Query("SELECT * FROM transcripts WHERE fullText LIKE '%' || :query || '%'")
    suspend fun searchTranscripts(query: String): List<TranscriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: TranscriptEntity): Long

    @Query("DELETE FROM transcripts WHERE callId = :callId")
    suspend fun deleteByCallId(callId: Long)
}
