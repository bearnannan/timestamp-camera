package com.example.timestampcamera.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class GalleryRepository(private val context: Context) {

    suspend fun getRecentMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        
        // Define columns
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA
        )

        // Query params - Select only Images and Videos
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        // Use Files content URI to get both images and videos
        // Note: Without READ_EXTERNAL_STORAGE, this returns only files created by this app (Scoped Storage)
        val queryUri = MediaStore.Files.getContentUri("external")

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val dateModColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    var dateTaken = cursor.getLong(dateColumn)
                    
                    // Fallback to Date Modified if Date Taken is missing/invalid (e.g. 1970)
                    if (dateTaken <= 0 && dateModColumn >= 0) {
                        dateTaken = cursor.getLong(dateModColumn) * 1000 // Convert seconds to millis
                    }
                    
                    val typeInt = cursor.getInt(typeColumn)
                    val bucketName = if (bucketColumn >= 0) cursor.getString(bucketColumn) ?: "Unknown" else "Unknown"
                    val path = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                    
                    val contentUri: Uri = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                         ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                         ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    
                    val mediaType = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.IMAGE

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            dateTaken = Date(dateTaken),
                            name = name,
                            type = mediaType,
                            path = path,
                            bucketName = bucketName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext mediaList
    }
    
    suspend fun deleteMedia(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(mediaItem.uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
