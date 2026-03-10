package com.callmind.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createAllChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val channels = listOf(
            NotificationChannel(
                "recording_monitor",
                "Recording Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitors for new call recordings" },

            NotificationChannel(
                "transcription",
                "Transcription",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Transcription progress" },

            NotificationChannel(
                "analysis",
                "Analysis",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "AI analysis progress" },

            NotificationChannel(
                "processing_complete",
                "Processing Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when call processing is done" }
        )

        manager.createNotificationChannels(channels)
    }
}
