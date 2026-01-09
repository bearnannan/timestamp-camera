package com.example.timestampcamera.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timestampcamera.data.DriveRepository
import java.io.IOException

class FileUploadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val fileUriString = inputData.getString("FILE_PATH") ?: return Result.failure()
        val folderName = inputData.getString("FOLDER_NAME") ?: "TimestampCamera" 
        
        val repository = DriveRepository(context)
        
        // Don't notify on every retry attempt to avoid spam
        if (runAttemptCount == 0) {
             // Optional: Show "Starting Upload" if needed, but silent is better for background
        }
        
        try {
            val uri = android.net.Uri.parse(fileUriString)
            
            // Resolve Filename
            var fileName = "image_${System.currentTimeMillis()}.jpg"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                         // Simple sanitization
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            }

            // Open Input Stream
            // We open a fresh stream for each attempt
            val inputStream = context.contentResolver.openInputStream(uri) 
            if (inputStream == null) {
                // File might be deleted or inaccessible -> Fail permanently
                showNotification("Upload Failed", "File not found or inaccessible.")
                return Result.failure()
            }
            
            inputStream.use { stream ->
                // 1. Get/Create Folder ID
                val folderResult = repository.findOrCreateFolder(folderName)
                
                // Handle Folder Error
                if (folderResult.isFailure) {
                    val exception = folderResult.exceptionOrNull()
                    return handleException(exception, "Folder creation failed")
                }
                
                val folderId = folderResult.getOrNull() ?: return Result.failure() // Should not happen if success
                
                // 2. Upload File
                val uploadResult = repository.uploadFile(stream, fileName, folderId)
                
                if (uploadResult.isSuccess) {
                    showNotification("Upload Success", "Uploaded to: $folderName")
                    return Result.success()
                } else {
                    val exception = uploadResult.exceptionOrNull()
                    return handleException(exception, "Upload failed")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return handleException(e, "Unexpected error")
        }
    }
    
    private fun handleException(e: Throwable?, title: String): Result {
        return when (e) {
            is SecurityException -> {
                // Auth Error -> Fail permanently and ask user to login
                showNotification("Upload Failed", "Please Login to Google Drive in Settings")
                Result.failure()
            }
            is IOException -> {
                // Network Error -> Retry
                // Only notify if it's the last attempt or after several retries to avoid annoyance?
                // For now, silent retry is best. WorkManager handles backoff.
                 if (runAttemptCount > 3) {
                     showNotification("Uploading...", "Network unsafe, retrying later ($runAttemptCount)")
                 }
                Result.retry()
            }
            else -> {
                // Unknown -> Retry carefully or fail? 
                // Using retry for safety, but if it persists WorkManager will eventually give up
                showNotification("Upload Error", e?.message ?: "Unknown error")
                Result.failure()
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "upload_status_silent"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Upload Status", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows success or failure of Google Drive uploads"
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0) // No sound
            .setAutoCancel(true)
            .build()
            
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
