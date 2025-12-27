package com.example.timestampcamera.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.timestampcamera.data.CameraSettings
import com.example.timestampcamera.data.ImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageSaver {

    suspend fun saveImage(
        bitmap: Bitmap,
        settings: CameraSettings,
        context: Context,
        isFrontCamera: Boolean,
        location: Location? = null
    ): Uri? = withContext(Dispatchers.IO) {
        
        var processedBitmap = bitmap

        // 1. Flip photos on front camera
        if (isFrontCamera && settings.flipFrontPhoto) {
            val matrix = Matrix().apply { postScale(-1f, 1f) }
            processedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        }

        // 2. Prepare for saving
        val fileName = "IMG_${System.currentTimeMillis()}.${settings.imageFormat.name.lowercase()}"
        val compressFormat = when (settings.imageFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
        
        // CUSTOM PATH LOGIC (SAF)
        if (settings.customSavePath != null) {
            try {
                val treeUri = Uri.parse(settings.customSavePath)
                val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                if (dir != null && dir.exists()) {
                    val mimeType = "image/${settings.imageFormat.name.lowercase()}"
                    val file = dir.createFile(mimeType, fileName)
                    
                    if (file != null) {
                        context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                            processedBitmap.compress(compressFormat, settings.compressionQuality, outputStream)
                        }
                        return@withContext file.uri
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to default MediaStore if SAF fails
            }
        }

        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/${settings.imageFormat.name.lowercase()}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TimestampCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val uri = resolver.insert(imageCollection, contentValues)
            uri?.let { targetUri ->
                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    processedBitmap.compress(compressFormat, settings.compressionQuality, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(targetUri, contentValues, null, null)
                }

                // 3. Save EXIF data
                if (settings.saveExif) {
                    try {
                        // For MediaStore Uris, we might need to open a FileDescriptor in write mode
                        resolver.openFileDescriptor(targetUri, "rw")?.use { pfd ->
                            val exif = ExifInterface(pfd.fileDescriptor)
                            
                            // Set basic metadata
                            exif.setAttribute(ExifInterface.TAG_MODEL, "Timestamp Camera Pro")
                            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
                            
                            // Set Location (GPS)
                            location?.let {
                                exif.setGpsInfo(it)
                            }
                            
                            exif.saveAttributes()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                return@withContext targetUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Clean up mirrored bitmap if created
            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }
        }

        return@withContext null
    }
}
