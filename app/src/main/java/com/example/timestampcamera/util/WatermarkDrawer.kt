package com.example.timestampcamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Address
import android.location.Location
import com.example.timestampcamera.data.LocationFormat
import com.example.timestampcamera.data.WatermarkFontSize
import com.example.timestampcamera.data.WatermarkItemType
import com.example.timestampcamera.data.WatermarkSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class WatermarkDrawer(private val context: Context) {

    fun drawWatermark(
        originalBitmap: Bitmap,
        settings: WatermarkSettings,
        location: Location?,
        address: Address?,
        currentTimestamp: Long
    ): Bitmap {
        // 1. Create a mutable copy of the bitmap to draw on
        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val width = bitmap.width
        val height = bitmap.height

        // 2. Setup Paint
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = settings.fontColor.toInt()
        
        // Calculate dynamic font size based on bitmap width
        val baseSize = width / 25f // Responsive scale
        paint.textSize = when (settings.fontSize) {
            WatermarkFontSize.SMALL -> baseSize * 0.8f
            WatermarkFontSize.MEDIUM -> baseSize
            WatermarkFontSize.LARGE -> baseSize * 1.5f
            WatermarkFontSize.EXTRA_LARGE -> baseSize * 2.0f
        }
        
        // Default font
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        
        // Shadow / Stroke
        if (settings.textShadowEnabled) {
             paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        // 3. Generate Lines from activeItemsOrder
        val lines = mutableListOf<String>()
        val date = Date(currentTimestamp)
        
        settings.activeItemsOrder.forEach { itemType ->
            when (itemType) {
                WatermarkItemType.DATE_TIME -> {
                    val locale = if (settings.useThaiLocale) Locale("th", "TH") else Locale.US
                    val formatter = SimpleDateFormat(settings.dateFormat, locale)
                    lines.add(formatter.format(date))
                }
                WatermarkItemType.ADDRESS -> {
                    if (address != null && settings.locationFormat != LocationFormat.NONE) {
                        val addrStr = AddressFormatter.formatAddress(address, settings.locationFormat)
                        if (addrStr.isNotEmpty()) lines.add(addrStr)
                    }
                }
                WatermarkItemType.GPS -> {
                    if (location != null) {
                         // Default to standard Lat/Lon for now
                        val lat = Location.convert(location.latitude, Location.FORMAT_DEGREES)
                        val lon = Location.convert(location.longitude, Location.FORMAT_DEGREES)
                        lines.add("$lat, $lon")
                    }
                }
                WatermarkItemType.ALTITUDE_SPEED -> {
                    if (location != null) {
                        val alt = String.format("%.1f m", location.altitude)
                        val speed = String.format("%.1f km/h", location.speed * 3.6f)
                        lines.add("Alt: $alt | Spd: $speed")
                    }
                }
                WatermarkItemType.CUSTOM_TEXT -> {
                    val parts = mutableListOf<String>()
                    if (settings.projectName.isNotEmpty()) parts.add(settings.projectName)
                    if (settings.inspectorName.isNotEmpty()) parts.add("Insp: ${settings.inspectorName}")
                    if (settings.customText.isNotEmpty()) parts.add(settings.customText)
                    if (parts.isNotEmpty()) lines.add(parts.joinToString(" | "))
                }
                WatermarkItemType.TAGS -> {
                    if (settings.customTags.isNotEmpty()) {
                        lines.add(settings.customTags.joinToString(" "))
                    }
                }
                else -> {} // Handle specific logic for Compass/Logo elsewhere or skip
            }
        }

        // 4. Measure & Draw
        if (lines.isEmpty()) return bitmap

        val padding = width * 0.04f // 4% padding
        val lineSpacing = paint.textSize * 0.2f
        
        // Position: Bottom-Right default (Start from bottom and move up)
        var currentY = height - padding
        
        // Process in reverse to stack from bottom
        lines.reversed().forEach { line ->
             // Draw Text with optional Stroke
             if (settings.textStrokeEnabled) {
                 val strokePaint = Paint(paint)
                 strokePaint.style = Paint.Style.STROKE
                 strokePaint.strokeWidth = 3f // Or dynamic
                 strokePaint.color = Color.BLACK
                 strokePaint.setShadowLayer(0f, 0f, 0f, 0) // No shadow on stroke usually
                 
                 // Measure right alignment
                 val textWidth = strokePaint.measureText(line)
                 val x = width - textWidth - padding
                 canvas.drawText(line, x, currentY, strokePaint)
             }
             
             // Draw Main Text
             val textWidth = paint.measureText(line)
             val x = width - textWidth - padding // Right align
             canvas.drawText(line, x, currentY, paint)
             
             // Move up
             currentY -= (paint.textSize + lineSpacing)
        }

        return bitmap
    }
}
