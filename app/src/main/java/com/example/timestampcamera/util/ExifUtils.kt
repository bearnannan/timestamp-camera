package com.example.timestampcamera.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoMetadata(
    val fileName: String = "Unknown",
    val fileSize: String = "Unknown", // "2.5 MB"
    val resolution: String = "Unknown", // "4000x3000"
    val dateTaken: String = "Unknown",
    val model: String = "Unknown",
    val aperture: String = "",
    val shutterSpeed: String = "",
    val iso: String = "",
    val focalLength: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasLocation: Boolean = false
)

object ExifUtils {

    // List of attributes to copy
    private val ATTRIBUTES_TO_COPY = arrayOf(
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_ISO_SPEED_RATINGS, // Fallback for ISO
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE
    )

    fun extractExifAttributes(image: androidx.camera.core.ImageProxy): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        try {
            // Check if format is JPEG to read EXIF from buffer
            if (image.format == android.graphics.ImageFormat.JPEG) {
                val buffer = image.planes[0].buffer
                val remaining = buffer.remaining()
                android.util.Log.d("ExifUtils", "Buffer format: JPEG, Remaining: $remaining")
                
                buffer.rewind() // Ensure we start from the beginning
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                buffer.rewind() // Restore for others
                
                // Use a temporary creation to parse EXIF from bytes
                val inputStream = java.io.ByteArrayInputStream(bytes)
                val exif = ExifInterface(inputStream)
                
                ATTRIBUTES_TO_COPY.forEach { tag ->
                     exif.getAttribute(tag)?.let { value ->
                         attributes[tag] = value
                         android.util.Log.d("ExifUtils", "Extracted $tag: $value")
                     }
                }
                android.util.Log.d("ExifUtils", "Extracted total attributes: ${attributes.size}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ExifUtils", "Error extracting EXIF", e)
        }
        return attributes
    }
    
    fun saveExifAttributes(context: Context, uri: Uri, attributes: Map<String, String>) {
         if (attributes.isEmpty()) {
             android.util.Log.d("ExifUtils", "No attributes to save")
             return
         }
         try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                attributes.forEach { (tag, value) ->
                    exif.setAttribute(tag, value)
                    android.util.Log.d("ExifUtils", "Setting $tag to $value")
                }
                exif.saveAttributes()
                android.util.Log.d("ExifUtils", "Saved attributes to $uri")
            }
        } catch (e: Exception) {
            e.printStackTrace()
             android.util.Log.e("ExifUtils", "Error saving EXIF", e)
        }
    }

    fun getExifMetadata(context: Context, uri: Uri): PhotoMetadata {
        var inputStream: InputStream? = null
        try {
            val contentResolver = context.contentResolver
            inputStream = contentResolver.openInputStream(uri) ?: return PhotoMetadata()
            
            val exif = ExifInterface(inputStream)
            
            // 1. Basic File Info
            // Name/Size usually comes from MediaStore, but we can approximate or get cursor info if needed.
            // For simplicity in this scope, we might rely on the GalleryScreen to pass Name/Size,
            // or we try to query it here. Let's query basic file info if possible, otherwise defaults.
            val fileInfo = getFileInfo(context, uri)

            // 2. Camera Details
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: exif.getAttribute(ExifInterface.TAG_MAKE) ?: "Unknown Device"
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: ""
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatExposureTime(it) } ?: ""
            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" } ?: ""
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" } ?: ""
            
            // 3. Resolution
            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) ?: "0"
            val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) ?: "0"
            val resolution = if (width != "0") "$width x $height" else "Unknown"

            // 4. Date
            // Try SubSecTimeOriginal first for precision, then DateTimeOriginal
            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) 
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            
            val formattedDate = parseExifDate(dateString)

            // 5. Location
            val latLong = exif.latLong
            
            return PhotoMetadata(
                fileName = fileInfo.first,
                fileSize = fileInfo.second,
                resolution = resolution,
                dateTaken = formattedDate,
                model = model,
                aperture = aperture,
                shutterSpeed = exposureTime,
                iso = iso,
                focalLength = focalLength,
                latitude = latLong?.get(0),
                longitude = latLong?.get(1),
                hasLocation = latLong != null
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return PhotoMetadata()
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, String> {
        var name = "Unknown"
        var sizeStr = "Unknown"
        
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    
                    if (nameIndex != -1) name = it.getString(nameIndex)
                    if (sizeIndex != -1) {
                        val sizeBytes = it.getLong(sizeIndex)
                        sizeStr = formatFileSize(sizeBytes)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        return Pair(name, sizeStr)
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$size B"
        }
    }

    private fun formatExposureTime(value: String): String {
        return try {
            val doubleVal = value.toDouble()
            if (doubleVal < 1.0 && doubleVal > 0) {
                 // Convert decimal to fraction (e.g., 0.005 -> 1/200)
                 val reciprocal = 1.0 / doubleVal
                 "1/${Math.round(reciprocal)}s"
            } else {
                "${value}s"
            }
        } catch (e: Exception) {
            "${value}s"
        }
    }

    private fun parseExifDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Unknown Date"
        return try {
            // Exif format: "2023:09:24 14:30:00"
            val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val date = inputFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    fun writeTagsToExif(context: Context, uri: Uri, tags: List<String>) {
        if (tags.isEmpty()) return
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                val userComment = tags.joinToString(", ")
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment)
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
