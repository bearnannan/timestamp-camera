package com.example.timestampcamera.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Path
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.graphics.PointF
import com.example.timestampcamera.data.CustomField

data class WatermarkLine(
    val text: String,
    val color: Int = Color.WHITE
)

data class WatermarkSnapshot(
    val lines: List<WatermarkLine>,
    val compassHeading: Float? = null,
    val compassPosition: PointF? = null
)

enum class OverlayPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class OverlayConfig(
    val position: OverlayPosition = OverlayPosition.BOTTOM_RIGHT,
    val compassPosition: OverlayPosition = OverlayPosition.CENTER, // New Field
    val date: Date = Date(),
    
    // Data Fields
    val address: String = "",
    val latLon: String = "",
    val altitudeSpeed: String = "",
    val resolution: String = "", 
    val customText: String = "",
    val azimuth: Float = 0f, // New field for Compass
    
    // Appearance
    val textColor: Int = Color.WHITE,
    val textSize: Float = 36f, // Slightly smaller for dense info
    val alpha: Int = 255,

    // Date/Time Formats
    val datePattern: String = "dd/MM/yyyy",
    val showTime: Boolean = true,
    val useThaiLocale: Boolean = false,
    
    // New Styling
    val textShadowEnabled: Boolean = false,
    val textBackgroundEnabled: Boolean = false,
    val textStyle: Int = 0, // 0=Default, 1=Bold
    val fontFamily: String = "sans", // sans, serif, monospace, cursive
    
    // Rich Data (Phase 16)
    val compassEnabled: Boolean = false,
    val altitudeEnabled: Boolean = false,
    val speedEnabled: Boolean = false,
    
    // Live Data
    val compassHeading: Float = 0f, // 0-360
    val altitude: Double = 0.0, // meters
    val speed: Float = 0f, // m/s
    
    // Phase 17: Professional Workflow
    val projectName: String = "",
    val inspectorName: String = "",
    val tags: String = "",
    val customFields: List<CustomField> = emptyList(), // New Feature


    
    // Phase 18: Advanced Typography
    val textStrokeEnabled: Boolean = false,
    val textStrokeWidth: Float = 3f,
    val textStrokeColor: Int = Color.BLACK,
    val googleFontName: String = "Roboto",


    // Toggles


    val showDate: Boolean = true,
    val showAddress: Boolean = true,
    val showLatLon: Boolean = true,
    val showAltitudeSpeed: Boolean = false,
    val showResolution: Boolean = false, // Changed to false by default
    val showCustomText: Boolean = false,
    val showCompass: Boolean = false,
    val showIndex: Boolean = false,
    
    // Style Template (0=Classic, 1=Modern, 2=Minimal)
    val templateId: Int = 0,
    // compassTapeEnabled Removed
    
    // Logo
    val logoBitmap: Bitmap? = null
) {
    fun getFormattedText(): String {
        return buildString {
             // ... (Keep existing logic for now, used by accessibility/preview)
             if (showDate) {
                val locale = if (useThaiLocale) Locale("th", "TH") else Locale.US
                val dateFormat = SimpleDateFormat(datePattern, locale)
                append("${dateFormat.format(date)}\n")
            }
            if (showAddress && address.isNotEmpty()) append("$address\n")
            if (showLatLon && latLon.isNotEmpty()) append("$latLon\n")
            if (showAltitudeSpeed && altitudeSpeed.isNotEmpty()) append("$altitudeSpeed\n")
            
            // Professional Workflow
            if (projectName.isNotEmpty()) append("Project: $projectName\n")
            if (inspectorName.isNotEmpty()) append("Inspector: $inspectorName\n")
            if (tags.isNotEmpty()) append("Tags: $tags\n")
            
            if (showCustomText && customText.isNotEmpty()) append("$customText\n")
        }.trim()
    }
}

object OverlayUtils {

    /**
     * Draws the overlay text and widgets on the provided bitmap.
     * 
     * ========== THREAD SAFETY ==========
     * This function performs heavy bitmap operations and MUST be called from a
     * background thread (e.g., Dispatchers.IO or Dispatchers.Default).
     * 
     * NEVER call this function from the Main/UI thread as it will cause:
     * - UI freezing during capture
     * - ANR (Application Not Responding) dialogs
     * - Poor user experience
     * 
     * @param bitmap The source bitmap to draw overlays on. A mutable copy will be created.
     * @param config The overlay configuration containing text, position, and style settings.
     * @return A new bitmap with the overlay drawn on it.
     * 
     * @see androidx.annotation.WorkerThread
     */
    @androidx.annotation.WorkerThread
    fun rotateBitmapIfNecessary(bitmap: Bitmap, exifAttributes: Map<String, String>): Bitmap {
        val orientation = exifAttributes[androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION]?.toIntOrNull() ?: 1
        val degrees = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        
        if (degrees == 0f) return bitmap
        
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @androidx.annotation.WorkerThread
    fun drawOverlayOnBitmap(bitmap: Bitmap, config: OverlayConfig): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        drawOverlayOnCanvas(canvas, mutableBitmap.width, mutableBitmap.height, config)
        return mutableBitmap
    }

    /**
     * Draws the overlay using a WatermarkSnapshot.
     * Guaranteed content and order from the snapshot.
     */
    @androidx.annotation.WorkerThread
    fun drawOverlayFromSnapshot(bitmap: Bitmap, snapshot: WatermarkSnapshot, config: OverlayConfig): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        val width = mutableBitmap.width
        val height = mutableBitmap.height
        
        // WYSIWYG Scaling: Reference width 1080px
        // If image is 4000px, everything should be 4x larger
        val scale = width / 1080f
        
        
        // 2. Draw Compass Graphic (Top-Left, Fixed Position for Saved Image)
        if (config.showCompass || config.compassEnabled) {
             val compassSize = 250f * scale
             val compassMargin = 50f * scale
             val cx = compassMargin + compassSize/2
             val cy = compassMargin + compassSize/2
             
             WatermarkDrawer.drawCompass(canvas, cx, cy, compassSize/2, snapshot.compassHeading ?: config.azimuth)
        }
        
        // 3. Draw Text Lines (Bottom-Right, Right Aligned)
        val paint = Paint().apply {
            textSize = config.textSize * scale // Apply Scale
            setShadowLayer(4f * scale, 2f * scale, 2f * scale, Color.BLACK)
            
            // Resolve Typeface (Matching CameraInfoOverlay / drawClassicLayout)
            val baseTypeface = when (config.googleFontName) {
                "Oswald" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
                "Roboto Mono" -> Typeface.MONOSPACE
                "Playfair Display" -> Typeface.SERIF
                "Inter" -> Typeface.SANS_SERIF
                "Cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                else -> {
                     when (config.fontFamily) {
                        "serif" -> Typeface.SERIF
                        "monospace" -> Typeface.MONOSPACE
                        "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                        else -> Typeface.SANS_SERIF
                    }
                }
            }
            val style = if (config.textStyle == 1) Typeface.BOLD else Typeface.NORMAL
            typeface = Typeface.create(baseTypeface, style)
        }
        
        // Measure and Layout
        val bounds = Rect()
        val lineSpacing = config.textSize * 0.5f * scale
        
        val lineHeights = snapshot.lines.map { line ->
             paint.getTextBounds(line.text, 0, line.text.length, bounds)
             bounds.height() + lineSpacing
        }
        
        val totalHeight = lineHeights.sum()
        val margin = 50f * scale
        var currentY = height - totalHeight - margin // Bottom margin
        val rightX = width - margin // Right margin
        
        snapshot.lines.forEachIndexed { index, line ->
            paint.textAlign = Paint.Align.RIGHT
            paint.color = line.color
            
            canvas.drawText(line.text, rightX, currentY, paint)
            
            if (index < lineHeights.size - 1) {
                currentY += lineHeights[index+1]
            }
        }
        
        return mutableBitmap
    }

    fun getFormattedDate(date: Date, pattern: String, useThaiLocale: Boolean): String {
        val locale = if (useThaiLocale) Locale("th", "TH") else Locale.US
        val simpleDateFormat = SimpleDateFormat(pattern, locale)
        var formattedDate = simpleDateFormat.format(date)

        if (useThaiLocale) {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            val yearAD = calendar.get(java.util.Calendar.YEAR)
            val yearBE = yearAD + 543
            
            // Replace full year (yyyy)
            if (formattedDate.contains(yearAD.toString())) {
                formattedDate = formattedDate.replace(yearAD.toString(), yearBE.toString())
            } else {
                // Handle short year (yy)
                val shortYearAD = yearAD.toString().takeLast(2)
                val shortYearBE = yearBE.toString().takeLast(2)
                // Use regex to replace ONLY if it looks like a year part (riskier, but needed if yyyy is not present)
                // For safety, let's only do simple replacement if pattern implies year.
                // Or simply rely on user checking.
                 if (formattedDate.contains(shortYearAD)) {
                     // Check if pattern actually requested year
                     if (pattern.contains("yy")) {
                         formattedDate = formattedDate.replace(shortYearAD, shortYearBE)
                     }
                 }
            }
        }
        return formattedDate
    }

    /**
     * Draws the overlay on the provided Canvas.
     * Can be used for both Bitmaps (Photos) and Surface/OverlayEffect (Video).
     */
    fun drawOverlayOnCanvas(canvas: Canvas, width: Int, height: Int, config: OverlayConfig) {
        WatermarkDrawer.draw(canvas, width, height, config)
    }
    
    // drawLogo, drawClassicLayout, drawModernLayout, drawMinimalLayout, drawMinimap, drawCompass
    // have been moved to WatermarkDrawer.kt to ensure consistency across Photo and Video.
}
