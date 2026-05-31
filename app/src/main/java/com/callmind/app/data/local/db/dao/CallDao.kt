package com.callmind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callmind.app.data.local.db.entity.CallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE id = :callId")
    suspend fun getCallById(callId: Long): CallEntity?

    @Query("SELECT * FROM calls WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByPhoneNumber(phoneNumber: String): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE contactName = :contactName ORDER BY timestamp DESC")
    fun getCallsByContactName(contactName: String): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE isTranscribed = 0 AND recordingFilePath IS NOT NULL")
    suspend fun getUntranscribedCalls(): List<CallEntity>

    @Query("SELECT * FROM calls WHERE isAnalyzed = 0 AND isTranscribed = 1")
    suspend fun getUnanalyzedCalls(): List<CallEntity>

    @Query("SELECT * FROM calls WHERE isAnalyzed = 1")
    suspend fun getAnalyzedCalls(): List<CallEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallEntity): Long

    @Update
    suspend fun update(call: CallEntity)

    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteById(callId: Long)

    @Query("SELECT DISTINCT contactName FROM calls WHERE contactName IS NOT NULL ORDER BY contactName")
    fun getAllContacts(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM calls WHERE phoneNumber = :phoneNumber")
    suspend fun getCallCountForNumber(phoneNumber: String): Int

    @Query("SELECT COUNT(*) FROM calls WHERE recordingFilePath = :path")
    suspend fun countByRecordingPath(path: String): Int

    @Query("UPDATE calls SET processingError = :error WHERE id = :callId")
    suspend fun setProcessingError(callId: Long, error: String?)

    @Query("UPDATE calls SET processingError = NULL WHERE id = :callId")
    suspend fun clearProcessingError(callId: Long)
}
