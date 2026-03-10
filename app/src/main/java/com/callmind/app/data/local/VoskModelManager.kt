package com.callmind.app.data.local

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedModel: Model? = null
    private val mutex = Mutex()

    private val modelsDir: File
        get() = File(context.filesDir, "vosk-models")

    val isModelDownloaded: Boolean
        get() {
            val modelDir = File(modelsDir, CURRENT_MODEL_NAME)
            return modelDir.exists() && modelDir.list()?.isNotEmpty() == true
        }

    suspend fun getModel(): Model = mutex.withLock {
        cachedModel?.let { return it }

        if (!isModelDownloaded) {
            throw IllegalStateException("Vosk model not downloaded. Call downloadModel() first.")
        }

        val modelDir = File(modelsDir, CURRENT_MODEL_NAME)
        val model = withContext(Dispatchers.IO) {
            Model(modelDir.absolutePath)
        }
        cachedModel = model
        model
    }

    suspend fun downloadModel(onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(context.cacheDir, "$CURRENT_MODEL_NAME.zip")

            // Download
            Log.i(TAG, "Downloading Vosk model: $CURRENT_MODEL_NAME")
            val url = URL("$MODEL_BASE_URL/$CURRENT_MODEL_NAME.zip")
            val connection = url.openConnection()
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            val totalBytes = connection.contentLengthLong.coerceAtLeast(1)

            connection.getInputStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead.toFloat() / totalBytes)
                    }
                }
            }

            // Extract
            Log.i(TAG, "Extracting model...")
            modelsDir.mkdirs()
            val targetDir = File(modelsDir, CURRENT_MODEL_NAME)
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Strip the top-level folder from zip (model name prefix)
                    val name = entry.name.substringAfter("/", entry.name)
                    if (name.isEmpty()) {
                        entry = zis.nextEntry
                        continue
                    }

                    val outFile = File(targetDir, name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            // Cleanup zip
            zipFile.delete()
            Log.i(TAG, "Model ready at: ${targetDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model", e)
            false
        }
    }

    fun releaseModel() {
        cachedModel?.close()
        cachedModel = null
    }

    companion object {
        private const val TAG = "VoskModelManager"
        private const val MODEL_BASE_URL = "https://alphacephei.com/vosk/models"
        const val CURRENT_MODEL_NAME = "vosk-model-small-en-in-0.4"
    }
}
