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
import com.callmind.app.util.RecordingFileParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class RecordingMonitorService : Service() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var recordingFileParser: RecordingFileParser

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var contentObserver: ContentObserver? = null

    // Serialize scans so two never run concurrently and race on check-then-insert.
    private val scanMutex = Mutex()
    // Coalesces rapid onChange bursts (one per file write) into a single delayed scan.
    private var debounceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        startMonitoring()
    }

    private fun startMonitoring() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                scheduleScan()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )

        // Run initial scan to discover new recordings (no auto-processing)
        scope.launch { runScan() }
    }

    private fun scheduleScan() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            runScan()
        }
    }

    private suspend fun runScan() = scanMutex.withLock {
        try {
            val recordingDir = userPreferences.recordingDirectory.first()
            recordingFileParser.checkForNewRecordings(recordingDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new recordings", e)
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
        private const val DEBOUNCE_MS = 1500L
    }
}
