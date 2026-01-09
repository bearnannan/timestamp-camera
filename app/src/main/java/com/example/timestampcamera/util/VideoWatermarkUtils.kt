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
                
                // Get current config with UPDATED TIME & MAPPED HEADING (Legacy Compatibility)
                val baseConfig = configProvider()
                val currentConfig = baseConfig.copy(
                    date = Date(),
                    // Fallback: If ViewModel updates 'azimuth' (Old), map it to 'compassHeading' (New)
                    compassHeading = if (baseConfig.compassHeading == 0f && baseConfig.azimuth != 0f) baseConfig.azimuth else baseConfig.compassHeading
                )
                
                // EIS SAFE ZONE LOGIC
                // Video stabilization crops ~10-15% from edges.
                // We apply a 7.5% margin on ALL sides to ensure Compass (Top) and Text (Bottom) are visible.
                val marginFactor = 0.075f 
                
                if (width > height) {
                    // LANDSCAPE SENSOR (1920x1080) -> PORTRAIT OUTPUT
                    // Rotate -90 degrees
                    canvas.save()
                    canvas.translate(0f, height.toFloat())
                    canvas.rotate(-90f)
                    
                    // Logical Dimensions after rotation
                    val logicalWidth = height
                    val logicalHeight = width
                    
                    // Apply Safe Zone
                    val safeWidth = (logicalWidth * (1 - 2 * marginFactor)).toInt()
                    val safeHeight = (logicalHeight * (1 - 2 * marginFactor)).toInt()
                    val dx = logicalWidth * marginFactor
                    val dy = logicalHeight * marginFactor
                    
                    canvas.translate(dx, dy)
                    WatermarkDrawer.draw(canvas, safeWidth, safeHeight, currentConfig)
                    canvas.restore()
                } else {
                    // PORTRAIT/NORMAL BUFFER
                    canvas.save()
                    
                    val safeWidth = (width * (1 - 2 * marginFactor)).toInt()
                    val safeHeight = (height * (1 - 2 * marginFactor)).toInt()
                    val dx = width * marginFactor
                    val dy = height * marginFactor
                    
                    canvas.translate(dx, dy)
                    WatermarkDrawer.draw(canvas, safeWidth, safeHeight, currentConfig)
                    canvas.restore()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true // Signal that we have drawn the frame
        }

        return overlayEffect
    }
}
