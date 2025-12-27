package com.example.timestampcamera.util

import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.camera.core.CameraEffect
import androidx.camera.effects.OverlayEffect
import androidx.core.util.Consumer
import java.util.Date

/**
 * Utility to create a Video OverlayEffect for timestamp watermarking.
 */
object VideoWatermarkUtils {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Creates an OverlayEffect that draws the timestamp.
     */
    fun createOverlayEffect(
        configProvider: () -> OverlayConfig
    ): OverlayEffect {
        
        val overlayEffect = OverlayEffect(
            CameraEffect.VIDEO_CAPTURE,
            1, // Queue depth
            mainHandler,
            Consumer<Throwable> { it.printStackTrace() }
        )

        overlayEffect.setOnDrawListener { frame ->
            try {
                val canvas = frame.overlayCanvas
                val width = frame.size.width
                val height = frame.size.height
                
                // Clear canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                
                // Get current config with UPDATED TIME
                val baseConfig = configProvider()
                val currentConfig = baseConfig.copy(date = Date())
                
                // Use reusable drawing logic from OverlayUtils
                OverlayUtils.drawOverlayOnCanvas(canvas, width, height, currentConfig)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true // Signal that we have drawn the frame
        }

        return overlayEffect
    }
}
