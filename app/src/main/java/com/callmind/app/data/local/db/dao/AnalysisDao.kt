package com.callmind.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callmind.app.data.local.db.entity.ActionItemEntity
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM call_analysis WHERE callId = :callId")
    suspend fun getAnalysisForCall(callId: Long): CallAnalysisEntity?

    @Query("SELECT * FROM call_analysis WHERE callId = :callId")
    fun observeAnalysisForCall(callId: Long): Flow<CallAnalysisEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: CallAnalysisEntity): Long

    // Action Items
    @Query("SELECT * FROM action_items WHERE callId = :callId")
    fun getActionItemsForCall(callId: Long): Flow<List<ActionItemEntity>>

    @Query("SELECT * FROM action_items WHERE isCompleted = 0 ORDER BY priority DESC")
    fun getPendingActionItems(): Flow<List<ActionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItem(actionItem: ActionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItems(actionItems: List<ActionItemEntity>)

    @Query("UPDATE action_items SET isCompleted = :completed WHERE id = :id")
    suspend fun updateActionItemCompletion(id: Long, completed: Boolean)

    @Query("SELECT * FROM call_analysis")
    fun getAllAnalyses(): Flow<List<CallAnalysisEntity>>
}
