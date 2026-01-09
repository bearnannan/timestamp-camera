package com.example.timestampcamera.util

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.util.Log
import android.util.Range

data class ZoomConfig(
    val ratio: Float,
    val label: String
)

object ZoomUtils {
    private const val TAG = "ZoomUtils"

    /**
     * Calculates zoom configs based on the Camera's Zoom Ratio Range and available focal lengths.
     * Uses a robust preset-based approach to ensure usability while approximating hardware lenses.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun calculateZoomConfig(camera2CameraInfo: Camera2CameraInfo): List<ZoomConfig> {
        return try {
            // 1. Get the Supported Zoom Ratio Range (Min/Max)
            // This is the source of truth for what the camera supports.
            val zoomRange = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            
            // 2. Get Focal Lengths (for reference/logging, helps verify if we have actual tele/wide lenses)
            val focalLengths = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            
            if (zoomRange == null) {
                Log.w(TAG, "No zoom range found, defaulting")
                return getDefaultZoomConfig()
            }

            val minZoom = zoomRange.lower
            val maxZoom = zoomRange.upper
            
            Log.d(TAG, "Zoom Range: $minZoom - $maxZoom")
            Log.d(TAG, "Focal Lengths: ${focalLengths?.joinToString()}")

            val configs = mutableListOf<ZoomConfig>()

            // --- Strategy: Range-Based Presets ---
            // Instead of guessing exact sensor math, we provide standard industry presets
            // that fall within the valid range. CameraX handles the lens switching internally.

            // 1. Ultra Wide (if supported)
            if (minZoom < 1.0f) {
                // Usually 0.6x or 0.5x
                // If the min is very close to 0.6 (e.g. 0.58), snap to 0.6 label but keep ratio
                val label = if (minZoom <= 0.65f) ".6" else "0.8" // Simplified labeling
                configs.add(ZoomConfig(minZoom, label)) 
            }

            // 2. Standard Wide (Always 1.0x)
            configs.add(ZoomConfig(1.0f, "1"))

            // 3. Telephoto / Digital Zoom Steps
            // We generate steps: 2x, 3x, 5x, 10x, 30x...
            val potentialSteps = listOf(2.0f, 3.0f, 5.0f, 10.0f, 20.0f, 30.0f, 100.0f)
            
            for (step in potentialSteps) {
                if (step <= maxZoom) {
                    // Only add if it's significantly distinct from the previous one
                    val prev = configs.last().ratio
                    if (step / prev >= 1.4f) { // Ensure at least 40% jump
                         configs.add(ZoomConfig(step, step.toInt().toString()))
                    }
                }
            }
            
            // 4. Ensure Max Zoom is represented if it's a "Hero" number (like 100x) 
            // and wasn't covered (e.g. max is 8x, our loop stopped at 5x)
            val last = configs.last().ratio
            if (maxZoom > last && maxZoom / last > 1.2f) {
                 // For max zoom, label nicely
                 val maxLabel = if (maxZoom >= 10) maxZoom.toInt().toString() else String.format("%.1f", maxZoom)
                 configs.add(ZoomConfig(maxZoom, maxLabel))
            }

            Log.d(TAG, "Final Zoom Configs: ${configs.map { "${it.label}(${it.ratio})" }}")
            return configs

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating zoom config", e)
            return getDefaultZoomConfig()
        }
    }

    private fun getDefaultZoomConfig(): List<ZoomConfig> {
        return listOf(
            ZoomConfig(1.0f, "1"),
            ZoomConfig(2.0f, "2"),
            ZoomConfig(5.0f, "5")
        )
    }
}
