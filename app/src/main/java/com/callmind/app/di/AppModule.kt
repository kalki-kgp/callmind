package com.callmind.app.di

import android.content.Context
import androidx.room.Room
import com.callmind.app.data.local.db.CallMindDatabase
import com.callmind.app.data.local.db.dao.AnalysisDao
import com.callmind.app.data.local.db.dao.CallDao
import com.callmind.app.data.local.db.dao.EmbeddingDao
import com.callmind.app.data.local.db.dao.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallMindDatabase {
        return Room.databaseBuilder(
            context,
            CallMindDatabase::class.java,
            "callmind.db"
        ).fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideCallDao(db: CallMindDatabase): CallDao = db.callDao()

    @Provides
    fun provideTranscriptDao(db: CallMindDatabase): TranscriptDao = db.transcriptDao()

    @Provides
    fun provideAnalysisDao(db: CallMindDatabase): AnalysisDao = db.analysisDao()

    @Provides
    fun provideEmbeddingDao(db: CallMindDatabase): EmbeddingDao = db.embeddingDao()
}
