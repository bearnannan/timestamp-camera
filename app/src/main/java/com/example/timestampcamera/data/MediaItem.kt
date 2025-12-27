package com.example.timestampcamera.data

import android.net.Uri
import java.util.Date

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val dateTaken: Date,
    val name: String,
    val type: MediaType
)
