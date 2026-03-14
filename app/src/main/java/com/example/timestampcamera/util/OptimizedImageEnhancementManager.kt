package com.example.timestampcamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.LruCache
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.Segmenter
import com.example.timestampcamera.data.EnhancementSettings
import com.example.timestampcamera.data.DetectionResult
import com.example.timestampcamera.data.ProcessingStats
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

// Data classes moved to com.example.timestampcamera.data

class OptimizedImageEnhancementManager(private val context: Context) {
    
    // Thread pool for background processing
    private val processingExecutor = Executors.newFixedThreadPool(2)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // ML Kit instances (lazy initialization)
    private val selfieSegmenter: Segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }
    
    private val objectDetector by lazy {
        com.google.mlkit.vision.objects.ObjectDetection.getClient(
            com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions.Builder()
                .setDetectorMode(com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableClassification()
                .build()
        )
    }
    
    // Memory cache for results
    private val detectionCache = LruCache<String, DetectionResult>(10)
    private val enhancementCache = LruCache<String, Bitmap>(5)
    
    // Performance tracking
    private val _processingStats = MutableStateFlow(ProcessingStats())
    val processingStats: StateFlow<ProcessingStats> = _processingStats.asStateFlow()
    
    // Processing queue for batch operations
    private val processingQueue = Channel<Bitmap>(Channel.UNLIMITED)
    
    // Performance metrics
    private var totalProcessingTime = 0L
    private var cacheHits = 0
    private var totalRequests = 0
    private var processedCount = 0
    
    init {
        // Start background processing
        startBackgroundProcessor()
    }
    
    suspend fun analyzeImage(bitmap: Bitmap, forceRefresh: Boolean = false): DetectionResult {
        val cacheKey = generateCacheKey(bitmap)
        
        // Check cache first
        if (!forceRefresh) {
            detectionCache.get(cacheKey)?.let { cachedResult ->
                cacheHits++
                updateStats()
                return cachedResult
            }
        }
        
        totalRequests++
        var result: DetectionResult = DetectionResult()
        val processingTime = measureTimeMillis {
            result = try {
                val image = InputImage.fromBitmap(bitmap, 0)
                var detectedObjects = emptyList<String>()
                var maxConfidence = 0f
                var mask: Bitmap? = null

                // Parallel processing of object detection and segmentation
                coroutineScope {
                    val deferredObjectDetection = async(processingExecutor.asCoroutineDispatcher()) {
                        try {
                            val objects = objectDetector.process(image).await()
                            objects.mapNotNull { obj ->
                                obj.labels.firstOrNull()?.let { label ->
                                    maxConfidence = maxOf(maxConfidence, label.confidence)
                                    "${label.text} (${(label.confidence * 100).toInt()}%)"
                                }
                            }
                        } catch (e: Exception) {
                            emptyList<String>()
                        }
                    }

                    val deferredSegmentation = async(processingExecutor.asCoroutineDispatcher()) {
                        try {
                            // Only run segmentation if object detection finds a person
                            val objects = deferredObjectDetection.await()
                            if (objects.any { it.contains("Person", ignoreCase = true) }) {
                                val segMask = selfieSegmenter.process(image).await()
                                createOptimizedMaskBitmap(segMask, bitmap.width, bitmap.height)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Wait for both operations
                    detectedObjects = deferredObjectDetection.await()
                    mask = deferredSegmentation.await()
                }

                val isPortrait = mask != null

                DetectionResult(
                    objects = detectedObjects,
                    confidence = maxConfidence,
                    isPortrait = isPortrait,
                    mask = mask
                )
            } catch (e: Exception) {
                DetectionResult()
            }
        }

        val finalResult = result.copy(processingTime = processingTime)
        detectionCache.put(cacheKey, finalResult)
        processedCount++
        updateStats()
        return finalResult
    }
    
    fun enhanceImage(bitmap: Bitmap, settings: EnhancementSettings): Bitmap {
        val cacheKey = generateEnhancementCacheKey(bitmap, settings)
        
        // Check cache first
        enhancementCache.get(cacheKey)?.let { cachedBitmap ->
            cacheHits++
            updateStats()
            return cachedBitmap
        }
        
        totalRequests++
        val processingTime = measureTimeMillis {
            try {
                val enhancedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(enhancedBitmap)
                val paint = Paint().apply { isAntiAlias = true }
                
                // Apply basic enhancements
                if (settings.brightness != 0f || settings.contrast != 0f || settings.saturation != 0f) {
                    val colorMatrix = createOptimizedColorMatrix(settings)
                    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    paint.colorFilter = null
                }
                
                // Auto Enhancement
                if (settings.autoEnhance) {
                    applyOptimizedAutoEnhancement(canvas, enhancedBitmap, paint)
                }
                
                // Cache result
                enhancementCache.put(cacheKey, enhancedBitmap)
                processedCount++
                updateStats()
                
                enhancedBitmap
                
            } catch (e: Exception) {
                bitmap // Return original if enhancement fails
            }
        }
        
        return bitmap
    }
    
    fun applyPortraitMode(bitmap: Bitmap, mask: Bitmap): Bitmap {
        return try {
            // Use background thread for heavy processing
            runBlocking(processingExecutor.asCoroutineDispatcher()) {
                createOptimizedPortraitEffect(bitmap, mask)
            }
        } catch (e: Exception) {
            bitmap // Fallback to original
        }
    }
    
    private suspend fun createOptimizedPortraitEffect(bitmap: Bitmap, mask: Bitmap): Bitmap {
        return withContext(Dispatchers.Default) {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply { isAntiAlias = true }
            
            // Draw blurred background (simplified for performance)
            val blurredBackground = createFastBlurBackground(bitmap)
            canvas.drawBitmap(blurredBackground, 0f, 0f, paint)
            
            // Draw original person using mask
            paint.colorFilter = null
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            canvas.drawBitmap(mask, 0f, 0f, paint)
            
            result
        }
    }
    
    private fun createOptimizedColorMatrix(settings: EnhancementSettings): ColorMatrix {
        return ColorMatrix().apply {
            // Combined matrix for better performance
            val brightness = settings.brightness / 255f
            val contrast = (settings.contrast + 1f) * (settings.contrast + 1f) * 255f
            val saturation = settings.saturation + 1f
            
            // Combined transformation matrix
            set(floatArrayOf(
                // Brightness + Contrast
                contrast / 255f, 0f, 0f, 0f, brightness * 255f - contrast / 2f,
                0f, contrast / 255f, 0f, 0f, brightness * 255f - contrast / 2f,
                0f, 0f, contrast / 255f, 0f, brightness * 255f - contrast / 2f,
                0f, 0f, 0f, 1f, 0f
            ))
            
            // Apply saturation
            postConcat(ColorMatrix(floatArrayOf(
                saturation, 0f, 0f, 0f, (1f - saturation) * 128f,
                0f, saturation, 0f, 0f, (1f - saturation) * 128f,
                0f, 0f, saturation, 0f, (1f - saturation) * 128f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    }
    
    private fun applyOptimizedAutoEnhancement(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
        // Pre-allocated color matrix for better performance
        val autoEnhanceMatrix = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -12.75f,
            0f, 1.1f, 0f, 0f, -12.75f,
            0f, 0f, 1.1f, 0f, -12.75f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(autoEnhanceMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
    
    private fun createOptimizedMaskBitmap(mask: SegmentationMask, width: Int, height: Int): Bitmap {
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskBuffer = mask.buffer
        val pixels = IntArray(width * height)
        
        // Optimized buffer processing
        maskBuffer.rewind()
        for (i in 0 until width * height) {
            val confidence = maskBuffer.float
            val alpha = (confidence * 255).toInt()
            pixels[i] = (alpha shl 24) or 0xFFFFFF
        }
        
        maskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return maskBitmap
    }
    
    private fun createFastBlurBackground(bitmap: Bitmap): Bitmap {
        // Simplified blur for better performance
        val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = 180 // Semi-transparent for blur effect
        }
        
        // Simple box blur approximation
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return blurred
    }
    
    private fun generateCacheKey(bitmap: Bitmap): String {
        // Simple hash based on dimensions and some pixels
        return "${bitmap.width}x${bitmap.height}_${bitmap.getPixel(0, 0)}_${bitmap.getPixel(bitmap.width/2, bitmap.height/2)}"
    }
    
    private fun generateEnhancementCacheKey(bitmap: Bitmap, settings: EnhancementSettings): String {
        return "${generateCacheKey(bitmap)}_${settings.autoEnhance}_${settings.brightness}_${settings.contrast}_${settings.saturation}"
    }
    
    private fun updateStats() {
        val avgTime = if (processedCount > 0) totalProcessingTime / processedCount else 0L
        val hitRate = if (totalRequests > 0) cacheHits.toFloat() / totalRequests else 0f
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        _processingStats.value = ProcessingStats(
            averageProcessingTime = avgTime,
            cacheHitRate = hitRate,
            memoryUsage = memoryUsage,
            totalProcessed = processedCount
        )
    }
    
    private fun startBackgroundProcessor() {
        coroutineScope.launch {
            for (bitmap in processingQueue) {
                try {
                    // Process in background
                    analyzeImage(bitmap)
                } catch (e: Exception) {
                    // Handle errors silently
                }
            }
        }
    }
    
    // Batch processing for multiple images
    suspend fun analyzeBatch(images: List<Bitmap>): List<DetectionResult> {
        return withContext(Dispatchers.IO) {
            images.map { bitmap ->
                async(processingExecutor.asCoroutineDispatcher()) {
                    analyzeImage(bitmap)
                }
            }.awaitAll()
        }
    }
    
    // Memory management
    fun clearCache() {
        detectionCache.evictAll()
        enhancementCache.evictAll()
        System.gc() // Suggest garbage collection
    }
    
    // Performance monitoring
    fun getPerformanceReport(): String {
        val stats = _processingStats.value
        return """
            Performance Report:
            - Average Processing Time: ${stats.averageProcessingTime}ms
            - Cache Hit Rate: ${(stats.cacheHitRate * 100).toInt()}%
            - Memory Usage: ${stats.memoryUsage / 1024 / 1024}MB
            - Total Processed: ${stats.totalProcessed}
            - Cache Size: ${detectionCache.size()} detections, ${enhancementCache.size()} enhancements
        """.trimIndent()
    }
    
    fun cleanup() {
        try {
            coroutineScope.cancel()
            processingExecutor.shutdown()
            objectDetector.close()
            clearCache()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
