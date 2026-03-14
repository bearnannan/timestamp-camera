package com.example.timestampcamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.segmentation.Segmenter
import com.example.timestampcamera.data.EnhancementSettings
import com.example.timestampcamera.data.DetectionResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data classes moved to com.example.timestampcamera.data

class ImageEnhancementManager(private val context: Context) {
    
    private val selfieSegmenter: Segmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
    )
    
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .build()
    )
    
    suspend fun analyzeImage(bitmap: Bitmap): DetectionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        var detectedObjects = emptyList<String>()
        var maxConfidence = 0f
        var isPortrait = false
        var mask: Bitmap? = null
        
        try {
            // Object Detection
            val objects = objectDetector.process(image).await()
            detectedObjects = objects.mapNotNull { obj ->
                obj.labels.firstOrNull()?.let { label ->
                    maxConfidence = maxOf(maxConfidence, label.confidence)
                    "${label.text} (${(label.confidence * 100).toInt()}%)"
                }
            }
            
            // Portrait Segmentation
            if (detectedObjects.any { it.contains("Person", ignoreCase = true) }) {
                val segmentationMask = selfieSegmenter.process(image).await()
                mask = createMaskBitmap(segmentationMask, bitmap.width, bitmap.height)
                isPortrait = mask != null
            }
            
        } catch (e: Exception) {
            // Handle errors gracefully
        }
        
        return DetectionResult(
            objects = detectedObjects,
            confidence = maxConfidence,
            isPortrait = isPortrait,
            mask = mask
        )
    }
    
    fun enhanceImage(bitmap: Bitmap, settings: EnhancementSettings): Bitmap {
        val enhancedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint().apply { isAntiAlias = true }
        
        // Apply basic enhancements
        if (settings.brightness != 0f || settings.contrast != 0f || settings.saturation != 0f) {
            val colorMatrix = ColorMatrix().apply {
                // Brightness
                set(floatArrayOf(
                    1f, 0f, 0f, 0f, settings.brightness,
                    0f, 1f, 0f, 0f, settings.brightness,
                    0f, 0f, 1f, 0f, settings.brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                // Contrast
                val contrast = (settings.contrast + 1f) * (settings.contrast + 1f) * 255f
                postConcat(ColorMatrix(floatArrayOf(
                    contrast / 255f, 0f, 0f, 0f, 128f - contrast / 2f,
                    0f, contrast / 255f, 0f, 0f, 128f - contrast / 2f,
                    0f, 0f, contrast / 255f, 0f, 128f - contrast / 2f,
                    0f, 0f, 0f, 1f, 0f
                )))
                
                // Saturation
                val saturation = settings.saturation + 1f
                postConcat(ColorMatrix(floatArrayOf(
                    saturation, 0f, 0f, 0f, (1f - saturation) * 128f,
                    0f, saturation, 0f, 0f, (1f - saturation) * 128f,
                    0f, 0f, saturation, 0f, (1f - saturation) * 128f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            paint.colorFilter = null
        }
        
        // Auto Enhancement
        if (settings.autoEnhance) {
            applyAutoEnhancement(canvas, enhancedBitmap, paint)
        }
        
        return enhancedBitmap
    }
    
    private fun applyAutoEnhancement(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
        // Simple auto-enhancement: adjust levels automatically
        val colorMatrix = ColorMatrix().apply {
            // Slight contrast boost
            set(floatArrayOf(
                1.1f, 0f, 0f, 0f, -12.75f,
                0f, 1.1f, 0f, 0f, -12.75f,
                0f, 0f, 1.1f, 0f, -12.75f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
    
    private fun createMaskBitmap(mask: SegmentationMask, width: Int, height: Int): Bitmap {
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskBuffer = mask.buffer
        val pixels = IntArray(width * height)
        
        for (i in 0 until width * height) {
            val confidence = maskBuffer.float
            val alpha = (confidence * 255).toInt()
            pixels[i] = (alpha shl 24) or 0xFFFFFF // White with varying alpha
        }
        
        maskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        maskBuffer.rewind()
        
        return maskBitmap
    }
    
    fun applyPortraitMode(bitmap: Bitmap, mask: Bitmap): Bitmap {
        // Create background blur effect
        val blurredBackground = createBlurredBackground(bitmap)
        
        // Combine with original using mask
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply { isAntiAlias = true }
        
        // Draw blurred background
        canvas.drawBitmap(blurredBackground, 0f, 0f, paint)
        
        // Draw original person using mask
        paint.colorFilter = null
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        
        return result
    }
    
    private fun createBlurredBackground(bitmap: Bitmap): Bitmap {
        // Simple blur implementation (in production, use RenderScript or other blur library)
        val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = 180 // Semi-transparent for blur effect
        }
        
        // Simple box blur (simplified - in production use proper blur algorithm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return blurred
    }
    
    fun cleanup() {
        objectDetector.close()
    }
}
