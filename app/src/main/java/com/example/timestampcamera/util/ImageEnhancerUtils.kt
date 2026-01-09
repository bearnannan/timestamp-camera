package com.example.timestampcamera.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Image Processing Engine v2.00
 * Handles pixel-level adjustments: Brightness, Contrast, Saturation.
 */
object ImageEnhancerUtils {

    /**
     * Enhances the bitmap with specified Brightness, Contrast, and Saturation.
     * Uses ColorMatrix for high-performance processing.
     *
     * @param sourceBitmap The original bitmap.
     * @param brightness Brightness scale (1.0 = normal, 0.0 = black, >1.0 = brighter).
     * @param contrast Contrast scale (1.0 = normal, 0.0 = gray, >1.0 = high contrast).
     * @param saturation Saturation scale (1.0 = normal, 0.0 = grayscale, >1.0 = oversaturated).
     * @return A new Bitmap with applied effects.
     */
    fun enhanceImage(
        sourceBitmap: Bitmap,
        brightness: Float = 1.0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f
    ): Bitmap {
        // Create a mutable copy to draw on
        val destBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            sourceBitmap.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(destBitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        
        // 1. Saturation
        colorMatrix.setSaturation(saturation)

        // 2. Contrast & Brightness (Scale)
        // Android's ColorMatrix doesn't have direct "Brightness" method like Saturation.
        // Usually:
        //   [ R_scale, 0, 0, 0, R_offset ]
        //   [ 0, G_scale, 0, 0, G_offset ]
        //   [ 0, 0, B_scale, 0, B_offset ]
        //   ...
        // Contrast acts as a Scale centered around gray.
        // Brightness acts as a Scale (Exposure) or Offset.
        // Here we'll treat Brightness as a simple multiplier (Scale) like Contrast, combined.
        
        // However, standard ColorMatrix setSaturation resets the matrix.
        // We need to concat other matrices.
        
        val contrastMatrix = ColorMatrix()
        // Contrast formula: 
        // scale = contrast
        // translate = (-.5f * contrast + .5f) * 255f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        contrastMatrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Concat Contrast
        colorMatrix.postConcat(contrastMatrix)
        
        // Brightness (Exposure)
        // We can just scale RGB by brightness factor
        val brightnessMatrix = ColorMatrix().apply {
            setScale(brightness, brightness, brightness, 1f)
        }
        colorMatrix.postConcat(brightnessMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)

        return destBitmap
    }
}
