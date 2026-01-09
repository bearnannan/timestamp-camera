package com.example.timestampcamera.domain.usecase

import android.graphics.Bitmap
import com.example.timestampcamera.utils.ImageEnhancerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase to handle image processing asynchronously.
 * Ensures the main thread is not blocked by heavy bitmap operations.
 */
class ProcessImageUseCase {

    /**
     * Processes the captured bitmap with auto-enhancement values.
     *
     * @param inputBitmap The raw bitmap captured from the camera.
     * @return Result containing the enhanced bitmap or an error.
     */
    suspend operator fun invoke(inputBitmap: Bitmap): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // "Auto-Enhancement" values
            // Slight boost to make photos look "popping"
            val targetAdjustment = Triple(
                1.05f, // Brightness: Just a tad brighter
                1.1f,  // Contrast: Deeper blacks
                1.1f   // Saturation: More vivid colors
            )

            val processedBitmap = ImageEnhancerEngine.process(
                source = inputBitmap,
                brightness = targetAdjustment.first,
                contrast = targetAdjustment.second,
                saturation = targetAdjustment.third
            )
            
            Result.success(processedBitmap)
        } catch (e: OutOfMemoryError) {
            // Handle OOM gracefully - simplistic fallback could be returning original,
            // but usually we want to know it failed.
            e.printStackTrace()
            Result.failure(Exception("Not enough memory to process image", e))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
