package com.callmind.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callmind.app.data.local.db.dao.AnalysisDao
import com.callmind.app.data.local.db.dao.CallDao
import com.callmind.app.data.local.db.dao.TranscriptDao
import com.callmind.app.data.local.db.entity.ActionItemEntity
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.local.db.entity.TranscriptEntity

@Database(
    entities = [
        CallEntity::class,
        TranscriptEntity::class,
        CallAnalysisEntity::class,
        ActionItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CallMindDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun analysisDao(): AnalysisDao
}
