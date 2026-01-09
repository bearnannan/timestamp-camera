package com.example.timestampcamera.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single Source of Truth for Watermark Drawing.
 * Used by both OverlayUtils (Photo) and VideoWatermarkUtils (Video).
 */
object WatermarkDrawer {

    /**
     * Draws the full watermark overlay (Widgets + Text) onto the canvas.
     */
    fun draw(canvas: Canvas, width: Int, height: Int, config: OverlayConfig, scale: Float = width / 1080f) {
        

        // 2. Draw Compass Graphic (Top-Left, Fixed Position)
        if (config.showCompass || config.compassEnabled) {
             val compassSize = 250f * scale
             val compassMargin = 50f * scale
             val cx = compassMargin + compassSize/2
             val cy = compassMargin + compassSize/2
             val heading = if (config.compassEnabled) config.compassHeading else config.azimuth
             drawCompass(canvas, cx, cy, compassSize/2, heading)
        }

        // 3. Draw Text Layouts
        when (config.templateId) {
            1 -> drawModernLayout(canvas, width, height, config, scale)
            2 -> {
                val paint = Paint().apply { isAntiAlias = true }
                 drawMinimalLayout(canvas, width, height, config, paint, scale)
            }
            else -> drawClassicLayout(canvas, width, height, config, scale)
        }
        
        // 4. Draw Logo (Top Right, Fixed Position)
        if (config.logoBitmap != null) {
            drawLogo(canvas, width, height, config.logoBitmap, scale)
        }
    }

    private fun drawLogo(canvas: Canvas, width: Int, height: Int, bitmap: Bitmap, scale: Float) {
        val targetWidth = width * 0.15f // 15% of screen width
        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = targetWidth * ratio
        
        val margin = 40f * scale
        val x = width - targetWidth - margin
        val y = margin + 50f * scale // Offset slightly for aesthetics
        
        val dst = RectF(x, y, x + targetWidth, y + targetHeight)
        canvas.drawBitmap(bitmap, null, dst, null)
    }

    private fun drawClassicLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig, scale: Float) {
        val paint = Paint().apply {
            color = config.textColor
            textSize = config.textSize * scale
            alpha = config.alpha
            isAntiAlias = true
            style = Paint.Style.FILL
            if (config.textShadowEnabled) {
                setShadowLayer(5f * scale, 2f * scale, 2f * scale, Color.BLACK)
            }
            
            // Resolve Typeface
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
            
            val style = when (config.textStyle) {
                1 -> Typeface.BOLD
                else -> Typeface.NORMAL
            }
            
            typeface = Typeface.create(baseTypeface, style)
        }

        // Prepare text lines based on config
        val allLines = mutableListOf<String>()
        
        if (config.showDate || config.showTime) {
            val pattern = if (config.showDate) config.datePattern else ""
            val fullPattern = if (config.showTime && config.showDate) "$pattern HH:mm:ss" else if (config.showTime) "HH:mm:ss" else pattern
            val dateText = OverlayUtils.getFormattedDate(config.date, fullPattern, config.useThaiLocale)
            allLines.add(dateText)
        }
        
        // Address (Moved up to match reference image)
        if (config.showAddress && config.address.isNotEmpty()) {
            config.address.split("\n").forEach { allLines.add(it) }
        }

        // Lat/Lon
        if (config.showLatLon && config.latLon.isNotEmpty()) {
            allLines.add(config.latLon)
        }
        
        // Heading Text
        if (config.showCompass || config.compassEnabled) {
             val heading = if (config.compassEnabled) config.compassHeading else config.azimuth
             val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
             val index = Math.round(((heading % 360) / 45)).toInt() % 8
             val direction = directions[index]
             allLines.add("Heading: ${heading.toInt()}° $direction")
        }
        
        // Altitude & Speed
        if (config.altitudeEnabled || config.speedEnabled) {
             val parts = mutableListOf<String>()
             if (config.altitudeEnabled) parts.add("Alt: %.1f m".format(config.altitude))
             if (config.speedEnabled) {
                 val speedKmh = config.speed * 3.6f 
                 parts.add("Spd: %.1f km/h".format(speedKmh))
             }
             if (parts.isNotEmpty()) allLines.add(parts.joinToString(" "))
        } else if (config.showAltitudeSpeed && config.altitudeSpeed.isNotEmpty()) {
            allLines.add(config.altitudeSpeed)
        }
        
        // Project Info
        if (config.projectName.isNotEmpty()) allLines.add("Project: ${config.projectName}")
        if (config.inspectorName.isNotEmpty()) allLines.add("Inspector: ${config.inspectorName}")
        if (config.tags.isNotEmpty()) allLines.add("Note: ${config.tags}")

        if (config.showResolution) {
            val resString = if (config.resolution.isNotEmpty()) config.resolution else "${width} x ${height}"
            allLines.add(resString)
        }
        if (config.showCustomText && config.customText.isNotEmpty()) {
            config.customText.split("\n").forEach { allLines.add(it) }
        }

        if (allLines.isEmpty()) return

        // Calculate Text Block Size
        var maxWidth = 0f
        var totalHeight = 0f
        val lineHeights = mutableListOf<Float>()
        val bounds = Rect()

        allLines.forEach { line ->
            paint.getTextBounds(line, 0, line.length, bounds)
            val measureWidth = paint.measureText(line)
            val height = bounds.height() + (config.textSize * 0.5f) 
            if (measureWidth > maxWidth) maxWidth = measureWidth
            lineHeights.add(height)
            totalHeight += height
        }

        val padding = 40f * scale
        val widgetsWidth = 0f
        val totalBlockWidth = maxWidth + widgetsWidth
        val totalBlockHeight = totalHeight

        // Calculate Start Position
        var blockX = padding
        var blockY = padding

        when (config.position) {
            OverlayPosition.TOP_LEFT -> { blockX = padding; blockY = padding }
            OverlayPosition.TOP_CENTER -> { blockX = (width - totalBlockWidth) / 2; blockY = padding }
            OverlayPosition.TOP_RIGHT -> { blockX = width - totalBlockWidth - padding; blockY = padding }
            OverlayPosition.CENTER_LEFT -> { blockX = padding; blockY = (height - totalBlockHeight) / 2 }
            OverlayPosition.CENTER -> { blockX = (width - totalBlockWidth) / 2; blockY = (height - totalBlockHeight) / 2 }
            OverlayPosition.CENTER_RIGHT -> { blockX = width - totalBlockWidth - padding; blockY = (height - totalBlockHeight) / 2 }
            OverlayPosition.BOTTOM_LEFT -> { blockX = padding; blockY = height - totalBlockHeight - padding }
            OverlayPosition.BOTTOM_CENTER -> { blockX = (width - totalBlockWidth) / 2; blockY = height - totalBlockHeight - padding }
            OverlayPosition.BOTTOM_RIGHT -> { blockX = width - totalBlockWidth - padding; blockY = height - totalBlockHeight - padding }
        }
        
        val textOnLeft = when (config.position) {
            OverlayPosition.TOP_LEFT, OverlayPosition.CENTER_LEFT, OverlayPosition.BOTTOM_LEFT -> true 
            else -> false 
        }
        
        val textX = if (textOnLeft) blockX else blockX + widgetsWidth

        // Draw Text Background Box
        if (config.textBackgroundEnabled && allLines.isNotEmpty()) {
             val bgPadding = 20f * scale
             val bgRect = Rect(
                 (textX - bgPadding).toInt(),
                 (blockY - bgPadding).toInt(),
                 (textX + maxWidth + bgPadding).toInt(),
                 (blockY + totalHeight + bgPadding).toInt()
             )
             
             val bgPaint = Paint().apply {
                 color = Color.BLACK
                 alpha = 100
                 style = Paint.Style.FILL
             }
             canvas.drawRect(bgRect, bgPaint)
        }
        
        // Draw Text
        // Pass 1: Stroke
        if (config.textStrokeEnabled) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = config.textStrokeWidth * scale
            paint.color = config.textStrokeColor
            
            var strokeY = blockY + (lineHeights.firstOrNull() ?: config.textSize)
            
            allLines.forEachIndexed { index, line ->
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
        
        // Pass 2: Fill
        paint.style = Paint.Style.FILL
        paint.color = config.textColor
        paint.textAlign = Paint.Align.LEFT 
        
        var currentY = blockY + (lineHeights.firstOrNull() ?: config.textSize)
        
        allLines.forEachIndexed { index, line ->
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
    }

    private fun drawModernLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig, scale: Float) {
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val locale = if (config.useThaiLocale) Locale("th", "TH") else Locale.US
        val timeFormat = SimpleDateFormat("HH:mm", locale)
        val timeText = if (config.showTime) timeFormat.format(config.date) else ""
        val dateText = OverlayUtils.getFormattedDate(config.date, config.datePattern, config.useThaiLocale)
        
        val details = mutableListOf<String>()
        if (config.showAddress && config.address.isNotEmpty()) details.add(config.address)
        if (config.showLatLon && config.latLon.isNotEmpty()) details.add(config.latLon)
        if (config.projectName.isNotEmpty()) details.add("Project: ${config.projectName}")
        if (config.inspectorName.isNotEmpty()) details.add("Inspector: ${config.inspectorName}")
        if (config.tags.isNotEmpty()) details.add("Note: ${config.tags}")
        
        paint.textSize = config.textSize * 1.6f * scale
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val timeBounds = Rect()
        paint.getTextBounds(timeText, 0, timeText.length, timeBounds)
        val timeHeight = timeBounds.height().toFloat()
        
        paint.textSize = config.textSize * 0.7f * scale
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val dateWidth = paint.measureText(dateText)
        val dateHeight = paint.textSize
        
        paint.textSize = config.textSize * 0.55f * scale
        val detailHeight = paint.textSize
        var detailsWidth = 0f
        details.forEach { 
            val w = paint.measureText(it)
            if (w > detailsWidth) detailsWidth = w
        }
        
        val padding = 40f * scale
        val lineSpacing = 12f * scale
        val barWidth = 12f * scale
        val barSpacing = 20f * scale
        
        val contentWidth = maxOf(paint.measureText(timeText), dateWidth, detailsWidth)
        val totalWidth = contentWidth + padding * 2 + barWidth + barSpacing
        var totalHeight = timeHeight + dateHeight + (details.size * (detailHeight + lineSpacing)) + padding * 2
        
        if (details.isNotEmpty()) totalHeight += 10f * scale
        
        val margin = 50f * scale
        val (x, y) = when (config.position) {
            OverlayPosition.TOP_LEFT -> Pair(margin, margin + totalHeight)
            OverlayPosition.TOP_RIGHT -> Pair(width - totalWidth - margin, margin + totalHeight)
            OverlayPosition.BOTTOM_LEFT -> Pair(margin, height - margin)
            OverlayPosition.BOTTOM_RIGHT -> Pair(width - totalWidth - margin, height - margin)
            OverlayPosition.CENTER -> Pair((width - totalWidth) / 2, (height + totalHeight) / 2)
            else -> Pair(width - totalWidth - margin, height - margin)
        }
        
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 140
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bgRect = RectF(x, y - totalHeight, x + totalWidth, y)
        canvas.drawRoundRect(bgRect, 24f * scale, 24f * scale, bgPaint)
        
        val borderPaint = Paint().apply {
            color = Color.WHITE
            alpha = 30
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
            isAntiAlias = true
        }
        canvas.drawRoundRect(bgRect, 24f * scale, 24f * scale, borderPaint)
        
        val barPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                x + padding, y - totalHeight + padding,
                x + padding, y - padding,
                intArrayOf(Color.parseColor("#FFD700"), Color.parseColor("#FF8C42")), 
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(8f * scale, 0f, 0f, Color.parseColor("#FFD700"))
        }
        val barRect = RectF(x + padding, y - totalHeight + padding, x + padding + barWidth, y - padding)
        canvas.drawRoundRect(barRect, 6f * scale, 6f * scale, barPaint)
        
        paint.reset()
        paint.isAntiAlias = true
        paint.setShadowLayer(4f * scale, 2f * scale, 2f * scale, Color.BLACK)
        
        var currentY = y - totalHeight + padding + timeHeight
        val textX = x + padding + barWidth + barSpacing
        
        if (config.showTime) {
            paint.color = Color.WHITE
            paint.textSize = config.textSize * 1.6f * scale
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText(timeText, textX, currentY, paint)
            currentY += dateHeight + lineSpacing + 5f
        }
        
        paint.textSize = config.textSize * 0.7f * scale
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
        
        paint.textSize = config.textSize * 0.55f * scale
        paint.color = Color.parseColor("#DDDDDD")
        details.forEach { line ->
             canvas.drawText(line, textX, currentY, paint)
             currentY += detailHeight + lineSpacing
        }
    }

    private fun drawMinimalLayout(canvas: Canvas, width: Int, height: Int, config: OverlayConfig, paint: Paint, scale: Float) {
        val sb = StringBuilder()
        val pattern = StringBuilder(config.datePattern)
        if (config.showTime) pattern.insert(0, "HH:mm ")
        sb.append(OverlayUtils.getFormattedDate(config.date, pattern.toString(), config.useThaiLocale))
        
        if (config.showAddress && config.address.isNotEmpty()) {
            sb.append(" • ").append(config.address.take(25))
            if (config.address.length > 25) sb.append("...")
        }
        
        if (config.showLatLon && config.latLon.isNotEmpty()) {
             config.latLon.split(",").let {
                 if (it.size >= 2) {
                     sb.append(" • ").append("${it[0].trim().take(7)}, ${it[1].trim().take(7)}")
                 } else {
                     sb.append(" • ").append(config.latLon)
                 }
             }
        }
        
        if (config.projectName.isNotEmpty()) sb.append(" • ").append(config.projectName)
        if (config.inspectorName.isNotEmpty()) sb.append(" • ").append(config.inspectorName)
        if (config.tags.isNotEmpty()) sb.append(" • ").append(config.tags)
        
        val text = sb.toString()
        paint.textSize = config.textSize * 0.85f * scale
        paint.color = config.textColor
        paint.alpha = config.alpha
        if (config.textShadowEnabled) paint.setShadowLayer(3f * scale, 1f * scale, 1f * scale, Color.BLACK)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        
        val paddingX = 45f * scale
        val paddingY = 25f * scale
        val totalWidth = textWidth + (paddingX * 2)
        val totalHeight = textHeight + (paddingY * 2)
        
        val margin = 40f * scale
        val (x, y) = when (config.position) {
            OverlayPosition.TOP_LEFT -> Pair(margin, margin + totalHeight)
            OverlayPosition.TOP_RIGHT -> Pair(width - totalWidth - margin, margin + totalHeight)
            OverlayPosition.BOTTOM_LEFT -> Pair(margin, height - margin)
            OverlayPosition.BOTTOM_RIGHT -> Pair(width - totalWidth - margin, height - margin)
            OverlayPosition.CENTER -> Pair((width - totalWidth) / 2, (height + totalHeight) / 2)
            else -> Pair(width - totalWidth - margin, height - margin)
        }
        
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 160
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bgRect = RectF(x, y - totalHeight, x + totalWidth, y)
        canvas.drawRoundRect(bgRect, totalHeight / 2, totalHeight / 2, bgPaint)
        
        val borderPaint = Paint().apply {
            color = Color.WHITE
            alpha = 80
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
            isAntiAlias = true
        }
        val borderRect = RectF(bgRect)
        borderRect.inset(1.25f, 1.25f) 
        canvas.drawRoundRect(borderRect, totalHeight / 2, totalHeight / 2, borderPaint)
        
        val baselineY = y - paddingY - fontMetrics.descent
        canvas.drawText(text, x + paddingX, baselineY, paint) 
    }


    fun drawCompass(canvas: Canvas, cx: Float, cy: Float, radius: Float, heading: Float) {
        val dialColor = 0xCC000000.toInt()
        val tickColor = Color.WHITE
        val tickColorMinor = Color.GRAY
        val cardinalColor = Color.WHITE
        val northColor = 0xFFFF5722.toInt()
        val centerTextColor = 0xFFFF5722.toInt()
        val centerSubTextColor = Color.WHITE

        // 1. Draw Background Dial
        val bgPaint = Paint().apply {
            color = dialColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // 2. Draw Indicator Triangle (Static at Top)
        val indicatorPaint = Paint().apply {
            color = northColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val indicatorSize = radius * 0.15f
        val indicatorPath = Path().apply {
            val tipY = cy - radius + (radius * 0.1f)
            moveTo(cx, tipY)
            lineTo(cx - indicatorSize/2, tipY + indicatorSize)
            lineTo(cx + indicatorSize/2, tipY + indicatorSize)
            close()
        }
        canvas.drawPath(indicatorPath, indicatorPaint)

        // 3. Draw Rotating Ticks and Labels
        canvas.save()
        canvas.rotate(-heading, cx, cy)

        val textPaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val tickPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        for (i in 0 until 360 step 2) {
            if (i % 5 != 0) continue

            val angleRad = Math.toRadians((i - 90).toDouble())
            val cosAngle = Math.cos(angleRad).toFloat()
            val sinAngle = Math.sin(angleRad).toFloat()

            val isCardinal = i % 90 == 0
            val isMajor = i % 30 == 0

            val tickLen = if (isCardinal) radius * 0.15f else if (isMajor) radius * 0.1f else radius * 0.05f
            val startRadius = radius - tickLen - (radius * 0.05f)
            val stopRadius = radius - (radius * 0.05f)

            val strokeScale = radius / 100f
            tickPaint.strokeWidth = if (isCardinal) 5f * strokeScale else if (isMajor) 3f * strokeScale else 1f * strokeScale
            tickPaint.color = if (isCardinal || isMajor) tickColor else tickColorMinor

            canvas.drawLine(
                cx + startRadius * cosAngle, cy + startRadius * sinAngle,
                cx + stopRadius * cosAngle, cy + stopRadius * sinAngle,
                tickPaint
            )

            // Labels
            if (isCardinal) {
                val label = when(i) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> ""
                }
                textPaint.color = if (i == 0) northColor else cardinalColor
                textPaint.textSize = radius * 0.25f
                val fontMetrics = textPaint.fontMetrics
                val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                canvas.drawText(label, cx + (startRadius - radius * 0.15f) * cosAngle, cy + (startRadius - radius * 0.15f) * sinAngle - offset, textPaint)
            } else if (isMajor) {
                textPaint.color = tickColor
                textPaint.textSize = radius * 0.15f
                val fontMetrics = textPaint.fontMetrics
                val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                canvas.drawText(i.toString(), cx + (startRadius - radius * 0.15f) * cosAngle, cy + (startRadius - radius * 0.15f) * sinAngle - offset, textPaint)
            }
        }
        canvas.restore()

        // 4. Draw Center Text (Static)
        val degrees = heading.toInt()
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = Math.round(((degrees % 360) / 45.0)).toInt() % 8
        val direction = directions[index]

        textPaint.color = centerTextColor
        textPaint.textSize = radius * 0.4f
        val fontMetrics = textPaint.fontMetrics
        val centerOffset = (fontMetrics.descent + fontMetrics.ascent) / 2
        canvas.drawText("$degrees°", cx, cy + (radius * -0.15f) - centerOffset, textPaint)

        textPaint.color = centerSubTextColor
        textPaint.textSize = radius * 0.35f
        canvas.drawText(direction, cx, cy + (radius * 0.25f) - centerOffset, textPaint)
    }
}
