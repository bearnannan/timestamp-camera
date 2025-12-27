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

enum class OverlayPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class OverlayConfig(
    val position: OverlayPosition = OverlayPosition.BOTTOM_RIGHT,
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
    val showResolution: Boolean = true,
    val showCustomText: Boolean = false,
    val showMap: Boolean = false,
    val showCompass: Boolean = false,
    val showIndex: Boolean = false,
    
    // Style Template (0=Classic, 1=Modern, 2=Minimal)
    val templateId: Int = 0,
    val compassTapeEnabled: Boolean = false,
    
    // Logo
    val logoBitmap: Bitmap? = null,
    
    // Static Map
    val mapBitmap: Bitmap? = null
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
            // ...
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
    fun drawOverlayOnBitmap(bitmap: Bitmap, config: OverlayConfig): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        drawOverlayOnCanvas(canvas, mutableBitmap.width, mutableBitmap.height, config)
        return mutableBitmap
    }

    /**
     * Draws the overlay on the provided Canvas.
     * Can be used for both Bitmaps (Photos) and Surface/OverlayEffect (Video).
     */
    fun drawOverlayOnCanvas(canvas: Canvas, width: Int, height: Int, config: OverlayConfig) {
        when (config.templateId) {
            1 -> drawModernLayout(canvas, width, height, config)
            2 -> {
                val paint = Paint().apply { isAntiAlias = true }
                 drawMinimalLayout(canvas, width, height, config, paint)
            }
            else -> drawClassicLayout(canvas, width, height, config)
        }
        
        if (config.compassTapeEnabled) {
            drawCompassTape(canvas, width, height, config)
        }
        
        if (config.logoBitmap != null) {
            drawLogo(canvas, width, height, config.logoBitmap)
        }
    }
    
    private fun drawLogo(canvas: Canvas, width: Int, height: Int, bitmap: Bitmap) {
        val targetWidth = width * 0.15f // 15% of screen width
        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = targetWidth * ratio
        
        val margin = 40f
        val x = width - targetWidth - margin
        val y = margin + 50f // Offset slightly for aesthetics
        
        val dst = RectF(x, y, x + targetWidth, y + targetHeight)
        canvas.drawBitmap(bitmap, null, dst, null)
    }

    private fun drawClassicLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig) {
        val paint = Paint().apply {
            color = config.textColor
            textSize = config.textSize
            alpha = config.alpha
            isAntiAlias = true
            style = Paint.Style.FILL
            if (config.textShadowEnabled) {
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
            }
            
            // Resolve Typeface (Pro Fallbacks)
            val baseTypeface = when (config.googleFontName) {
                "Oswald" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
                "Roboto Mono" -> Typeface.MONOSPACE
                "Playfair Display" -> Typeface.SERIF
                "Inter" -> Typeface.SANS_SERIF
                "Cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                else -> {
                    // Fallback to old logic or default
                     when (config.fontFamily) {
                        "serif" -> Typeface.SERIF
                        "monospace" -> Typeface.MONOSPACE
                        "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                        else -> Typeface.SANS_SERIF
                    }
                }
            }
            
            val style = when (config.textStyle) {
                1 -> Typeface.BOLD
                else -> Typeface.NORMAL
            }
            
            typeface = Typeface.create(baseTypeface, style)
        }

        // Prepare text lines based on config
        val allLines = mutableListOf<String>()
        
        if (config.showDate) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            allLines.add(dateFormat.format(config.date))
        }
        if (config.showAddress && config.address.isNotEmpty()) {
            allLines.add(config.address)
        }
        if (config.showLatLon && config.latLon.isNotEmpty()) {
            allLines.add(config.latLon)
        }
        if (config.altitudeEnabled) {
             allLines.add("Alt: %.1f m".format(config.altitude))
        }
        if (config.speedEnabled) {
             val speedKmh = config.speed * 3.6f // m/s to km/h
             allLines.add("Spd: %.1f km/h".format(speedKmh))
        }
        // Legacy support (optional)
        if (config.showAltitudeSpeed && config.altitudeSpeed.isNotEmpty() && !config.altitudeEnabled && !config.speedEnabled) {
            allLines.add(config.altitudeSpeed)
        }
        if (config.showResolution) {
            val resString = if (config.resolution.isNotEmpty()) config.resolution else "${width} x ${height}"
            allLines.add(resString)
        }
        if (config.showCustomText && config.customText.isNotEmpty()) {
            allLines.add(config.customText)
        }
        
        // Map Placeholder Logic (Removed - replaced by Graphic)
        // if (config.showMap) { ... } 

        if (allLines.isEmpty() && !config.showMap && !config.showCompass && !config.compassEnabled) return

        // Calculate Text Block Size
        var maxWidth = 0f
        var totalHeight = 0f
        val lineHeights = mutableListOf<Float>()
        val bounds = Rect()

        allLines.forEach { line ->
            paint.getTextBounds(line, 0, line.length, bounds)
            val width = paint.measureText(line)
            val height = bounds.height() + (config.textSize * 0.5f) 
            if (width > maxWidth) maxWidth = width
            lineHeights.add(height)
            totalHeight += height
        }

        val padding = 40f
        
        // Widget Sizes
        val widgetSize = config.textSize * 3.5f 
        val widgetPadding = 30f

        val detailTextSize = config.textSize * 0.7f
        val lineSpacing = detailTextSize * 0.4f
        
        // Calculate total Block Width (Text + Widgets)
        // Assume Widgets are stacked vertically if both are present, or just exist on one side.
        // For simplicity: If ANY widget is on, reserve space on the "Visual side".
        val hasWidgets = config.showMap || config.showCompass || config.compassEnabled
        val widgetsWidth = if (hasWidgets) widgetSize + widgetPadding else 0f
        
        // Total block size for positioning calculations
        val totalBlockWidth = maxWidth + widgetsWidth
        val totalBlockHeight = kotlin.math.max(totalHeight, if ((config.showMap) && (config.showCompass || config.compassEnabled)) widgetSize * 2 + widgetPadding else if (hasWidgets) widgetSize else 0f)

        // Calculate Start Position (Top-Left of the entire Group)
        var blockX = padding
        var blockY = padding

        when (config.position) {
            OverlayPosition.TOP_LEFT -> {
                blockX = padding
                blockY = padding
            }
            OverlayPosition.TOP_CENTER -> {
                blockX = (width - totalBlockWidth) / 2
                blockY = padding
            }
            OverlayPosition.TOP_RIGHT -> {
                blockX = width - totalBlockWidth - padding
                blockY = padding
            }
            OverlayPosition.CENTER_LEFT -> {
                blockX = padding
                blockY = (height - totalBlockHeight) / 2
            }
            OverlayPosition.CENTER -> {
                blockX = (width - totalBlockWidth) / 2
                blockY = (height - totalBlockHeight) / 2
            }
            OverlayPosition.CENTER_RIGHT -> {
                blockX = width - totalBlockWidth - padding
                blockY = (height - totalBlockHeight) / 2
            }
            OverlayPosition.BOTTOM_LEFT -> {
                blockX = padding
                blockY = height - totalBlockHeight - padding
            }
            OverlayPosition.BOTTOM_CENTER -> {
                blockX = (width - totalBlockWidth) / 2
                blockY = height - totalBlockHeight - padding
            }
            OverlayPosition.BOTTOM_RIGHT -> {
                blockX = width - totalBlockWidth - padding
                blockY = height - totalBlockHeight - padding
            }
        }
        
        // Determine Relative Layout: Text Left or Text Right?
        val textOnLeft = when (config.position) {
            OverlayPosition.TOP_LEFT, OverlayPosition.CENTER_LEFT, OverlayPosition.BOTTOM_LEFT -> true // Text Left, Widgets Right
            OverlayPosition.TOP_RIGHT, OverlayPosition.CENTER_RIGHT, OverlayPosition.BOTTOM_RIGHT -> false // Text Right, Widgets Left
            else -> true // Center defaults to Text Left
        }
        
        val textX = if (textOnLeft) blockX else blockX + widgetsWidth
        val widgetX = if (textOnLeft) blockX + maxWidth + widgetPadding else blockX
        
        // Draw Text Background Box (if enabled)
        if (config.textBackgroundEnabled && allLines.isNotEmpty()) {
             val bgPadding = 20f
             val bgRect = Rect(
                 (textX - bgPadding).toInt(),
                 (blockY - bgPadding).toInt(),
                 (textX + maxWidth + bgPadding).toInt(),
                 (blockY + totalHeight + bgPadding).toInt()
             )
             
             val bgPaint = Paint().apply {
                 color = Color.BLACK
                 alpha = 100 // Semi-transparent
                 style = Paint.Style.FILL
             }
             canvas.drawRect(bgRect, bgPaint)
        }
        
        // Draw Text
        // Pass 1: Stroke (Outline)
        if (config.textStrokeEnabled) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = config.textStrokeWidth
            paint.color = config.textStrokeColor
            
            var strokeY = blockY + (lineHeights.firstOrNull() ?: config.textSize)
            
            allLines.forEachIndexed { index, line ->
                // Apply alignment logic for stroke pass
                 if (!textOnLeft) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(line, textX + maxWidth, strokeY, paint)
                } else {
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(line, textX, strokeY, paint)
                }
                
                if (index < lineHeights.size -1) {
                    strokeY += lineHeights[index + 1]
                }
            }
        }
        
        // Pass 2: Fill (Main Text)
        paint.style = Paint.Style.FILL
        paint.color = config.textColor
        paint.textAlign = Paint.Align.LEFT 
        
        var currentY = blockY + (lineHeights.firstOrNull() ?: config.textSize) // Approx baseline for first line
        
        allLines.forEachIndexed { index, line ->
            // If we are Right Aligned, draw from textX + maxWidth? 
            // Let's stick to Left Align for the text lines inside the block for predictability
            // OR support Right Align if textOnLeft is false?
            if (!textOnLeft) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(line, textX + maxWidth, currentY, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(line, textX, currentY, paint)
            }
            
            if (index < lineHeights.size -1) {
                currentY += lineHeights[index + 1]
            }
        }
        
        // Draw Widgets
        var currentWidgetY = blockY + widgetSize/2 // Center of first widget
        if (allLines.isNotEmpty()) {
             // Center widgets vertically relative to text block if possible?
             // Or just start from top.
             currentWidgetY = blockY + widgetSize/2
        }
        
        if (config.showMap) {
            // Draw Map
            drawMinimap(canvas, widgetX + widgetSize/2, currentWidgetY, widgetSize, config.mapBitmap)
            currentWidgetY += widgetSize + widgetPadding
        }
        
        if (config.showCompass || config.compassEnabled) {
            // Draw Compass
            // Use compassHeading if enabled, else fallback to azimuth (legacy)
            val heading = if (config.compassEnabled) config.compassHeading else config.azimuth
            drawCompass(canvas, widgetX + widgetSize/2, currentWidgetY, widgetSize/2, heading, paint)
        }
        

        
        // Draw Logo (Top Right)
        config.logoBitmap?.let { logo ->
            val logoPadding = 30f
            // Scale logo to a reasonable size (e.g. 15% of width)
            val logoTargetWidth = width * 0.15f
            val scale = logoTargetWidth / logo.width
            val logoTargetHeight = logo.height * scale
            
            val logoX = width - logoTargetWidth - logoPadding
            val logoY = logoPadding
            
            val dstRect = Rect(logoX.toInt(), logoY.toInt(), (logoX + logoTargetWidth).toInt(), (logoY + logoTargetHeight).toInt())
            canvas.drawBitmap(logo, null, dstRect, null)
        }

    }
    
    private fun drawMinimap(canvas: Canvas, cx: Float, cy: Float, size: Float, mapBitmap: Bitmap?) {
        val halfSize = size / 2
        val left = cx - halfSize
        val top = cy - halfSize
        val right = cx + halfSize
        val bottom = cy + halfSize
        val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        
        if (mapBitmap != null) {
            // 1. Draw Real Map
             val srcRect = Rect(0, 0, mapBitmap.width, mapBitmap.height)
             val paint = Paint().apply { isAntiAlias = true }
             canvas.drawBitmap(mapBitmap, srcRect, rect, paint)
             
             // 2. Attribution (Required by OSM)
             val attributionPaint = Paint().apply {
                 color = Color.BLACK
                 alpha = 180
                 textSize = size * 0.08f // Small text
                 style = Paint.Style.FILL
                 isAntiAlias = true
             }
             // Draw background for text readability
             val text = "© OpenStreetMap"
             val textBounds = Rect()
             attributionPaint.getTextBounds(text, 0, text.length, textBounds)
             
             val textPadding = 4f
             val bgRect = Rect(
                 left.toInt(), 
                 (bottom - textBounds.height() - textPadding * 2).toInt(), 
                 (left + textBounds.width() + textPadding * 2).toInt(), 
                 bottom.toInt()
             )
             
             val bgPaint = Paint().apply {
                 color = Color.WHITE
                 alpha = 150
                 style = Paint.Style.FILL
             }
             canvas.drawRect(bgRect, bgPaint)
             canvas.drawText(text, left + textPadding, bottom - textPadding, attributionPaint)
             
        } else {
            // Fallback: Abstract Grid
            // 1. Background (Glassy)
            val bgPaint = Paint().apply {
                color = Color.BLACK
                alpha = 100
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, bgPaint)
            
            // 3. Grid Lines (Abstract)
            val gridPaint = Paint().apply {
                color = Color.WHITE
                alpha = 50
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            // Vertical grids
            for (i in 1..3) {
                val gx = left + (size * i / 4)
                canvas.drawLine(gx, top, gx, bottom, gridPaint)
            }
            // Horizontal grids
            for (i in 1..3) {
                val gy = top + (size * i / 4)
                canvas.drawLine(left, gy, right, gy, gridPaint)
            }
        }
        
        // Border (Common)
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(rect, borderPaint)
        
        // Current Location Dot (Gold) - Common
        val dotPaint = Paint().apply {
            color = Color.parseColor("#FFCC00") // Gold
            style = Paint.Style.FILL
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }
        canvas.drawCircle(cx, cy, size * 0.08f, dotPaint)
    }
    
    private fun drawCompass(canvas: Canvas, cx: Float, cy: Float, radius: Float, azimuth: Float, textPaint: Paint) {
        // ... (existing drawCompass implementation)
        val paint = Paint(textPaint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        
        // Draw Outer Circle
        canvas.drawCircle(cx, cy, radius, paint)
        
        // Draw Fill (Glassy)
        val fillPaint = Paint(paint)
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = Color.BLACK
        fillPaint.alpha = 100
        canvas.drawCircle(cx, cy, radius, fillPaint)
        
        // Directions Text (N, E, S, W) - Fixed Direction? Or Rotating?
        // User requested: "needle pointing North". 
        // This implies the dial N/E/S/W is FIXED to the screen (N is up), and NEEDLE rotates.
        // OR Dial rotates (so N points to actual North).
        // Let's implement STANDARD Compass: Dial Rotates. Top of dial is current heading.
        // Wait, Azimuth = 0 (North).
        // If Azimuth = 90 (East). The "E" mark should be at the Top?
        // Yes. So we Rotate the Canvas by -Azimuth?
        
        // Actually, "Needle pointing North" -> Needle rotates. Dial is fixed.
        // If I face East (Azimuth 90). The needle should point LEFT (North).
        // Angle = -90.
        // So Needle Angle = -Azimuth.
        
        // Draw Fixed N/E/S/W
        val textRadius = radius * 0.75f
        val letterPaint = Paint(textPaint)
        letterPaint.textAlign = Paint.Align.CENTER
        letterPaint.textSize = radius * 0.4f
        letterPaint.style = Paint.Style.FILL
        
        // N (Top)
        letterPaint.color = Color.RED
        canvas.drawText("N", cx, cy - textRadius + letterPaint.textSize/3, letterPaint)
        
        // E (Right)
        letterPaint.color = Color.WHITE
        canvas.drawText("E", cx + textRadius, cy + letterPaint.textSize/3, letterPaint)
        
        // S (Bottom)
        canvas.drawText("S", cx, cy + textRadius + letterPaint.textSize/3, letterPaint)
        
        // W (Left)
        canvas.drawText("W", cx - textRadius, cy + letterPaint.textSize/3, letterPaint)
        
        // Draw Needle (Rotated)
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)
        
        val needlePaint = Paint()
        needlePaint.color = Color.RED
        needlePaint.strokeWidth = 6f
        needlePaint.style = Paint.Style.STROKE
        needlePaint.strokeCap = Paint.Cap.ROUND
        
        // Draw Arrow pointing UP (relative to rotated canvas)
        canvas.drawLine(cx, cy, cx, cy - radius * 0.6f, needlePaint)
        
        // Contrast tip
        needlePaint.color = Color.WHITE
        canvas.drawLine(cx, cy, cx, cy + radius * 0.6f, needlePaint) // Tail
        
    }

    private fun drawModernLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig) {
        // Modern Layout: Vertical bar accent (Gradient), Glassy Background
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        
        // 1. Prepare Text
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(config.date)
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateText = dateFormat.format(config.date)
        
        val details = mutableListOf<String>()
        if (config.showAddress && config.address.isNotEmpty()) details.add(config.address)
        if (config.showLatLon && config.latLon.isNotEmpty()) details.add(config.latLon)
        
        // 2. Measure
        // Time: Large font
        paint.textSize = config.textSize * 1.6f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val timeBounds = Rect()
        paint.getTextBounds(timeText, 0, timeText.length, timeBounds)
        val timeHeight = timeBounds.height().toFloat()
        
        // Date: Medium font
        paint.textSize = config.textSize * 0.7f
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val dateWidth = paint.measureText(dateText)
        val dateHeight = paint.textSize
        
        // Details: Small font
        paint.textSize = config.textSize * 0.55f
        val detailHeight = paint.textSize
        var detailsWidth = 0f
        details.forEach { 
            val w = paint.measureText(it)
            if (w > detailsWidth) detailsWidth = w
        }
        
        // Total Box Size
        val padding = 40f
        val lineSpacing = 12f
        val barWidth = 12f
        val barSpacing = 20f
        
        val contentWidth = maxOf(paint.measureText(timeText), dateWidth, detailsWidth)
        val totalWidth = contentWidth + padding * 2 + barWidth + barSpacing
        var totalHeight = timeHeight + dateHeight + (details.size * (detailHeight + lineSpacing)) + padding * 2
        
        // Additional spacing if details exist
        if (details.isNotEmpty()) totalHeight += 10f
        
         // Position
        val margin = 50f
        val (x, y) = when (config.position) {
            OverlayPosition.TOP_LEFT -> Pair(margin, margin + totalHeight)
            OverlayPosition.TOP_RIGHT -> Pair(width - totalWidth - margin, margin + totalHeight)
            OverlayPosition.BOTTOM_LEFT -> Pair(margin, height - margin)
            OverlayPosition.BOTTOM_RIGHT -> Pair(width - totalWidth - margin, height - margin) // y is bottom
            OverlayPosition.CENTER -> Pair((width - totalWidth) / 2, (height + totalHeight) / 2)
            else -> Pair(width - totalWidth - margin, height - margin)
        }
        
        // Draw Background (Glassy Dark)
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 140
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bgRect = RectF(x, y - totalHeight, x + totalWidth, y)
        canvas.drawRoundRect(bgRect, 24f, 24f, bgPaint)
        
        // Draw Border (Subtle White Stroke)
        val borderPaint = Paint().apply {
            color = Color.WHITE
            alpha = 30
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(bgRect, 24f, 24f, borderPaint)
        
        // Draw Accent Bar (Gradient Gold)
        val barPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                x + padding, y - totalHeight + padding,
                x + padding, y - padding,
                intArrayOf(Color.parseColor("#FFD700"), Color.parseColor("#FF8C42")), // Gold to Orange
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
            isAntiAlias = true
            
            // Add slight glow
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#FFD700"))
        }
        val barRect = RectF(
            x + padding, 
            y - totalHeight + padding, 
            x + padding + barWidth, 
            y - padding
        )
        canvas.drawRoundRect(barRect, 6f, 6f, barPaint)
        
        // Draw Text
        // Reset Shadow for text visibility
        paint.reset()
        paint.isAntiAlias = true
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        
        var currentY = y - totalHeight + padding + timeHeight
        val textX = x + padding + barWidth + barSpacing
        
        // Time
        paint.color = Color.WHITE
        paint.textSize = config.textSize * 1.6f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        canvas.drawText(timeText, textX, currentY, paint)
        
        currentY += dateHeight + lineSpacing + 5f
        
        // Date
        paint.textSize = config.textSize * 0.7f
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawText(dateText, textX, currentY, paint)
        
        if (details.isNotEmpty()) {
             currentY += lineSpacing + 10f
             val separatorPaint = Paint().apply {
                 color = Color.WHITE
                 alpha = 50
                 strokeWidth = 1f
                 style = Paint.Style.STROKE
             }
             canvas.drawLine(textX, currentY - (detailHeight/2) - 5f, textX + contentWidth, currentY - (detailHeight/2) - 5f, separatorPaint)
        }
        
        currentY += detailHeight 
        
        // Details
        paint.textSize = config.textSize * 0.55f
        paint.color = Color.parseColor("#DDDDDD")
        details.forEach { line ->
             canvas.drawText(line, textX, currentY, paint)
             currentY += detailHeight + lineSpacing
        }
    }

    private fun drawMinimalLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig, paint: Paint) {
        // Minimal Layout: Single line strip with stroke border
        val sb = StringBuilder()
        
        // 1. Time & Date
        val dateFormat = SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault())
        sb.append(dateFormat.format(config.date))
        
        // 2. Location
        if (config.showAddress && config.address.isNotEmpty()) {
            sb.append(" • ").append(config.address.take(25)) // Increased limit slightly
            if (config.address.length > 25) sb.append("...")
        }
        
        // 3. Lat/Lon
        if (config.showLatLon && config.latLon.isNotEmpty()) {
             config.latLon.split(",").let {
                 if (it.size >= 2) {
                     val lat = it[0].trim().take(7)
                     val lon = it[1].trim().take(7)
                     sb.append(" • ").append("$lat, $lon")
                 } else {
                     sb.append(" • ").append(config.latLon)
                 }
             }
        }
        
        val text = sb.toString()
        
        // Measure
        paint.textSize = config.textSize * 0.85f // Slightly larger
        paint.color = config.textColor
        paint.alpha = config.alpha
        if (config.textShadowEnabled) {
            paint.setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        // Ensure Typeface is set nicely
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        
        // Padding
        val paddingX = 45f
        val paddingY = 25f
        
        val totalWidth = textWidth + (paddingX * 2)
        val totalHeight = textHeight + (paddingY * 2)
        
        // Position
        val margin = 40f
        val (x, y) = when (config.position) {
            OverlayPosition.TOP_LEFT -> Pair(margin, margin + totalHeight)
            OverlayPosition.TOP_RIGHT -> Pair(width - totalWidth - margin, margin + totalHeight)
            OverlayPosition.BOTTOM_LEFT -> Pair(margin, height - margin)
            OverlayPosition.BOTTOM_RIGHT -> Pair(width - totalWidth - margin, height - margin)
            OverlayPosition.CENTER -> Pair((width - totalWidth) / 2, (height + totalHeight) / 2)
            else -> Pair(width - totalWidth - margin, height - margin)
        }
        
        // Draw Background (Pill)
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 160 // Darker for contrast
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bgRect = RectF(x, y - totalHeight, x + totalWidth, y)
        canvas.drawRoundRect(bgRect, totalHeight / 2, totalHeight / 2, bgPaint)
        
        // Draw Stroke Border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            alpha = 80 // Visible but subtle
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        // Inset slightly so stroke stays inside or on edge
        val borderRect = RectF(bgRect)
        borderRect.inset(1.25f, 1.25f) 
        canvas.drawRoundRect(borderRect, totalHeight / 2, totalHeight / 2, borderPaint)
        
        // Draw Text (Vertically Centered)
        // Y position for text baseline = centerCY - (top + bottom)/2
        // Or simplified: bottomY - paddingY - descent
        val baselineY = y - paddingY - fontMetrics.descent
        
        canvas.drawText(text, x + paddingX, baselineY, paint) 
    }
    private fun drawCompassTape(canvas: Canvas, width: Int, height: Int, config: OverlayConfig) {
        val cx = width / 2f
        val cy = height * 0.15f // Position at top 15%
        val tapeHeight = 70f
        val tapeWidth = width * 0.85f // Widen slightly
        val pixelsPerDegree = tapeWidth / 80f // Show 80 degrees total (zoomed in slightly)
        
        val paint = Paint().apply { isAntiAlias = true }
        
        // Background (Glassy)
        paint.color = Color.BLACK
        paint.alpha = 80
        paint.style = Paint.Style.FILL
        val bgRect = RectF(cx - tapeWidth/2, cy - tapeHeight/2, cx + tapeWidth/2, cy + tapeHeight/2)
        canvas.drawRoundRect(bgRect, 15f, 15f, paint)
        
        // Ticks and Labels
        val heading = if (config.compassEnabled) config.compassHeading else config.azimuth
        val startDegree = (heading - 50).toInt()
        val endDegree = (heading + 50).toInt()
        
        canvas.save()
        canvas.clipRect(bgRect) // Clip content to background
        
        for (i in startDegree..endDegree) {
            val degree = i % 360
            val normalizedDegree = if (degree < 0) degree + 360 else degree
            
            // Calculate x relative to center
            val diff = i - heading
            val x = cx + (diff * pixelsPerDegree)
            
            // Skip if out of bounds
            if (x < bgRect.left || x > bgRect.right) continue
            
            if (i % 10 == 0) {
                // Major Tick + Label
                paint.strokeWidth = 3f
                paint.alpha = 255
                paint.color = Color.WHITE
                // Tall tick
                canvas.drawLine(x, cy - 15f, x, cy + 15f, paint)
                
                // Label
                val label = when (normalizedDegree) {
                    0 -> "N"
                    45 -> "NE"
                    90 -> "E"
                    135 -> "SE"
                    180 -> "S"
                    225 -> "SW"
                    270 -> "W"
                    315 -> "NW"
                    else -> normalizedDegree.toString()
                }
                
                paint.textAlign = Paint.Align.CENTER
                
                if (label.length <= 2) { // Cardinal
                     paint.typeface = Typeface.DEFAULT_BOLD
                     paint.color = if (label == "N") Color.RED else Color.WHITE
                     paint.textSize = 30f
                     // Draw slightly larger
                } else { // Number
                     paint.typeface = Typeface.DEFAULT
                     paint.color = Color.LTGRAY
                     paint.textSize = 24f
                }
                
                // Draw text BELOW tick to avoid overlap
                canvas.drawText(label, x, cy - 20f, paint)
                
            } else if (i % 5 == 0) {
                 // Medium Tick
                 paint.strokeWidth = 2f
                 paint.color = Color.LTGRAY
                 paint.alpha = 200
                 canvas.drawLine(x, cy - 10f, x, cy + 10f, paint)
            } else {
                // Minor Tick (1 degree) - Optional, might be too crowded
                // Let's add it but make it very subtle
                paint.strokeWidth = 1f
                paint.color = Color.GRAY
                paint.alpha = 100
                canvas.drawLine(x, cy - 5f, x, cy + 5f, paint)
            }
        }
        
        // Gradient Fade Masks (Left and Right)
        val fadeWidth = tapeWidth * 0.15f
        // Left Fade
        val leftFade = Paint().apply {
             shader = android.graphics.LinearGradient(
                bgRect.left, cy, bgRect.left + fadeWidth, cy,
                Color.BLACK, Color.TRANSPARENT,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRect(bgRect.left, bgRect.top, bgRect.left + fadeWidth, bgRect.bottom, leftFade)
        
        // Right Fade
        val rightFade = Paint().apply {
             shader = android.graphics.LinearGradient(
                bgRect.right - fadeWidth, cy, bgRect.right, cy,
                Color.TRANSPARENT, Color.BLACK,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRect(bgRect.right - fadeWidth, bgRect.top, bgRect.right, bgRect.bottom, rightFade)
        
        canvas.restore()
        
        // Center Indicator (Golden Triangle/Arrow)
        val indicatorPaint = Paint().apply {
            color = Color.parseColor("#FFD700") // Gold
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(8f, 0f, 0f, Color.rgb(255, 215, 0)) // Glow
        }
        val path = android.graphics.Path()
        // Point UP into the tape
        path.moveTo(cx, cy + tapeHeight/2 + 5f) // Base center
        path.lineTo(cx - 12f, cy + tapeHeight/2 + 25f) // Bottom Left
        path.lineTo(cx + 12f, cy + tapeHeight/2 + 25f) // Bottom Right
        path.close()
        canvas.drawPath(path, indicatorPaint)
        
        // Small tick on top of the tape identifying exact center
        indicatorPaint.strokeWidth = 4f
        indicatorPaint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy - 20f, cx, cy + 20f, indicatorPaint)
    }
}
