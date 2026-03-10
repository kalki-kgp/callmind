package com.callmind.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.callmind.app.data.local.db.dao.AnalysisDao
import com.callmind.app.data.local.db.dao.CallDao
import com.callmind.app.data.local.db.dao.EmbeddingDao
import com.callmind.app.data.local.db.dao.TranscriptDao
import com.callmind.app.data.local.db.entity.ActionItemEntity
import com.callmind.app.data.local.db.entity.CallAnalysisEntity
import com.callmind.app.data.local.db.entity.CallEntity
import com.callmind.app.data.local.db.entity.EmbeddingEntity
import com.callmind.app.data.local.db.entity.TranscriptEntity

@Database(
    entities = [
        CallEntity::class,
        TranscriptEntity::class,
        CallAnalysisEntity::class,
        ActionItemEntity::class,
        EmbeddingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CallMindDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun embeddingDao(): EmbeddingDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calls ADD COLUMN processingError TEXT DEFAULT NULL")
            }
        }
    }
}
