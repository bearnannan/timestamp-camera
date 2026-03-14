package com.example.timestampcamera.data

import android.graphics.Bitmap

data class EnhancementSettings(
    val autoEnhance: Boolean = true,
    val portraitMode: Boolean = false,
    val objectDetection: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val sharpening: Float = 0f
)

data class DetectionResult(
    val objects: List<String> = emptyList(),
    val confidence: Float = 0f,
    val isPortrait: Boolean = false,
    val mask: Bitmap? = null,
    val processingTime: Long = 0L
)

data class ProcessingStats(
    val averageProcessingTime: Long = 0L,
    val cacheHitRate: Float = 0f,
    val memoryUsage: Long = 0L,
    val totalProcessed: Int = 0
)
