package com.callmind.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callmind.app.R
import com.callmind.app.data.local.preferences.UserPreferences
import com.callmind.app.data.repository.CallRepository
import com.callmind.app.util.RecordingFileParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordingMonitorService : Service() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var recordingFileParser: RecordingFileParser
    @Inject lateinit var callRepository: CallRepository
    @Inject lateinit var pipelineOrchestrator: PipelineOrchestrator

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var contentObserver: ContentObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        processBacklog()
    }

    private fun startMonitoring() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                scope.launch {
                    try {
                        val recordingDir = userPreferences.recordingDirectory.first()
                        recordingFileParser.checkForNewRecordings(recordingDir)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking for new recordings", e)
                    }
                }
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )

        // Also run initial scan immediately
        scope.launch {
            try {
                val recordingDir = userPreferences.recordingDirectory.first()
                recordingFileParser.checkForNewRecordings(recordingDir)
            } catch (e: Exception) {
                Log.e(TAG, "Error in initial scan", e)
            }
        }
    }

    /**
     * Re-process any calls that were transcribed but not analyzed,
     * or have recordings but were never transcribed.
     */
    private fun processBacklog() {
        scope.launch {
            try {
                // Retry unanalyzed calls
                val unanalyzed = callRepository.getUnanalyzedCalls()
                for (call in unanalyzed) {
                    pipelineOrchestrator.analyzeCall(call.id)
                }

                // Retry untranscribed calls
                val untranscribed = callRepository.getUntranscribedCalls()
                for (call in untranscribed) {
                    pipelineOrchestrator.processCall(call.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing backlog", e)
            }
        }
    }

    override fun onDestroy() {
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recording Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors for new call recordings"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CallMind")
            .setContentText("Monitoring for new call recordings")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "RecordingMonitor"
        private const val CHANNEL_ID = "recording_monitor"
        private const val NOTIFICATION_ID = 1
    }
}
