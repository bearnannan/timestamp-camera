package com.example.timestampcamera.data

import com.example.timestampcamera.auth.GoogleAuthManager
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.io.IOException

class DriveRepository(private val context: Context) {

    private fun getDriveService(): Drive? {
        val credential = GoogleAuthManager.getCredential(context) ?: return null
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("Timestamp Camera Pro").build()
    }

    /**
     * Finds or creates a folder hierarchy.
     * Returns:
     * - Result.success(folderId)
     * - Result.failure(exception) with specific types for Auth vs Network
     */
    suspend fun findOrCreateFolder(folderPath: String): Result<String> = withContext(Dispatchers.IO) {
        val service = getDriveService() 
            ?: return@withContext Result.failure(SecurityException("User not signed in"))
        
        // Split path into parts (e.g. "Work/SiteA" -> ["Work", "SiteA"])
        val parts = folderPath.split("/").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return@withContext Result.success("root")

        var parentId = "root"

        try {
            for (partName in parts) {
                // 1. Check if folder exists in current parent
                val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$partName' and '$parentId' in parents and trashed = false"
                val result = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                if (result.files.isNotEmpty()) {
                    // Found, descend into it
                    parentId = result.files[0].id
                } else {
                    // 2. Create in current parent
                    val folderMetadata = File().apply {
                        name = partName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = Collections.singletonList(parentId)
                    }
                    
                    val folder = service.files().create(folderMetadata)
                        .setFields("id")
                        .execute()
                    
                    parentId = folder.id
                }
            }
            return@withContext Result.success(parentId)
        } catch (e: UserRecoverableAuthIOException) {
            e.printStackTrace()
            return@withContext Result.failure(SecurityException("Auth required: ${e.message}"))
        } catch (e: IOException) {
            e.printStackTrace()
            // Network or IO error -> Retryable in Worker
            return@withContext Result.failure(IOException("Network error: ${e.message}", e))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    suspend fun uploadFile(inputStream: java.io.InputStream, fileName: String, folderId: String, mimeType: String = "image/jpeg"): Result<String> = withContext(Dispatchers.IO) {
        val service = getDriveService() 
            ?: return@withContext Result.failure(SecurityException("User not signed in"))
        
        try {
            val fileMetadata = File().apply {
                name = fileName
                parents = Collections.singletonList(folderId)
            }
            
            val mediaContent = com.google.api.client.http.InputStreamContent(mimeType, inputStream)
            
            val file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
                
            return@withContext Result.success(file.id)
        } catch (e: UserRecoverableAuthIOException) {
            return@withContext Result.failure(SecurityException("Auth required"))
        } catch (e: IOException) {
            return@withContext Result.failure(IOException("Network error", e))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }
}
