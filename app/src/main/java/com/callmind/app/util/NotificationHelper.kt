package com.callmind.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.callmind.app.R
import com.callmind.app.data.local.db.entity.ProcessingStage
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
                CHANNEL_PROCESSING,
                "Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Live progress while a call is processed" },

            // Retained for backward compatibility with previously-created channels.
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

    /**
     * Builds the single, ongoing pipeline notification with a determinate
     * progress bar and the active stage. Reusing one [NOTIFICATION_ID_PROCESSING]
     * across the three workers means the chain updates one notification in place
     * rather than stacking three.
     */
    fun processingForegroundInfo(contactLabel: String, stage: ProcessingStage): ForegroundInfo {
        val percent = (stage.progress * 100).toInt().coerceIn(0, 100)
        val stepText = if (stage.isActive) "Step ${stage.step} of ${ProcessingStage.STEP_COUNT}" else stage.activeLabel

        val notification = NotificationCompat.Builder(context, CHANNEL_PROCESSING)
            .setContentTitle("${stage.activeLabel} · $contactLabel")
            .setContentText(stepText)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID_PROCESSING,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        )
    }

    fun showCompletion(callId: Long, contactLabel: String, summary: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, "processing_complete")
            .setContentTitle("Call analyzed: $contactLabel")
            .setContentText(summary.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        manager.notify(callId.hashCode() + 1000, notification)
    }

    companion object {
        const val CHANNEL_PROCESSING = "processing"
        const val NOTIFICATION_ID_PROCESSING = 1001
    }
}
