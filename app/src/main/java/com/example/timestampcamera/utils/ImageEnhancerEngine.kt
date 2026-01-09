package com.example.timestampcamera.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Core engine for applying aesthetic adjustments to images.
 * Uses Android's native ColorMatrix for high-performance processing.
 */
object ImageEnhancerEngine {

    /**
     * Applies brightness, contrast, and saturation adjustments to the source bitmap.
     * returns a NEW immutable bitmap with the effects applied.
     *
     * @param source The input bitmap.
     * @param brightness Brightness scale (1.0f = no change, >1.0f brighter, <1.0f darker).
     * @param contrast Contrast scale (1.0f = no change, >1.0f higher contrast).
     * @param saturation Saturation scale (1.0f = no change, 0.0f grayscale, >1.0f supersaturated).
     * @return A new Bitamp with the effects applied.
     */
    fun process(
        source: Bitmap,
        brightness: Float = 1.0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f
    ): Bitmap {
        // Create a mutable copy to draw upon
        // Usage of ARGB_8888 is standard for high quality processing
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Create matrices for each adjustment
        val brightnessMatrix = getBrightnessMatrix(brightness)
        val contrastMatrix = getContrastMatrix(contrast)
        val saturationMatrix = getSaturationMatrix(saturation)

        // 2. Combine them: final = saturation * contrast * brightness
        // Order matters for ColorMatrix concatenation (postConcat).
        // Usually: Brightness -> Contrast -> Saturation is a safe order to mimic photo editors,
        // but combining them into one matrix is purely mathematical.
        val combinedMatrix = ColorMatrix()
        combinedMatrix.postConcat(brightnessMatrix)
        combinedMatrix.postConcat(contrastMatrix)
        combinedMatrix.postConcat(saturationMatrix)

        // 3. Apply to Paint
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
            isAntiAlias = true
            isFilterBitmap = true
        }

        // 4. Draw
        canvas.drawBitmap(source, 0f, 0f, paint)

        return result
    }

    private fun getBrightnessMatrix(value: Float): ColorMatrix {
        // Brightness is a translation usually, but scaling RGB channels works for gain.
        // Standard "Translation" approach for ColorMatrix sets the offset (last column).
        // However, standard "Brightness" controls often act as Exposure (Gain).
        // Let's implement simpler gain-based brightness for "Adjustment" feel.
        // Matrix:
        // [ b, 0, 0, 0, 0 ]
        // [ 0, b, 0, 0, 0 ]
        // [ 0, 0, b, 0, 0 ]
        // [ 0, 0, 0, 1, 0 ]
        val matrix = ColorMatrix()
        matrix.setScale(value, value, value, 1f)
        return matrix
    }

    private fun getContrastMatrix(value: Float): ColorMatrix {
        // Contrast formula:
        // scale = value
        // translate = (-.5f * scale + .5f) * 255f
        // [ s, 0, 0, 0, t ]
        // [ 0, s, 0, 0, t ]
        // [ 0, 0, s, 0, t ]
        // [ 0, 0, 0, 1, 0 ]
        val scale = value
        val translate = (-0.5f * scale + 0.5f) * 255f
        val mat = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(mat)
    }

    private fun getSaturationMatrix(value: Float): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(value)
        return matrix
    }
}
