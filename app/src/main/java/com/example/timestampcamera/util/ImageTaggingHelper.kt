package com.example.timestampcamera.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ImageTaggingHelper {

    private val labeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f) // Initial filtering
            .build()
        ImageLabeling.getClient(options)
    }

    suspend fun analyzeImage(bitmap: Bitmap, rotationDegrees: Int = 0): List<String> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)
            
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    // Extract text and perform additional filtering if needed
                    val tags = labels.map { it.text }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    
                    continuation.resume(tags)
                }
                .addOnFailureListener { e ->
                    // Log error but generally return empty list to not block app flow
                    android.util.Log.e("ImageTaggingHelper", "Analysis failed", e)
                    continuation.resume(emptyList()) // Fail safe
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
