package com.example.timestampcamera.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassOverlay(
    heading: Float,
    modifier: Modifier = Modifier
) {
    // Reference Design: Dark circular dial, Orange accents.
    val dialColor = Color(0xCC000000) // Semi-transparent Black
    val tickColor = Color.White
    val tickColorMinor = Color.Gray
    val cardinalColor = Color.White
    val northColor = Color(0xFFFF5722) // Orange
    val centerTextColor = Color(0xFFFF5722) // Orange
    val centerSubTextColor = Color.White

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Draw Background Dial
            drawCircle(
                color = dialColor,
                radius = radius,
                center = center
            )
            
            // Draw Indicator Triangle (Static at Top)
            val indicatorSize = radius * 0.15f
            val indicatorPath = androidx.compose.ui.graphics.Path().apply {
                // Tip is at the top inner padding
                val tipY = center.y - radius + (radius * 0.1f)
                moveTo(center.x, tipY) 
                lineTo(center.x - indicatorSize/2, tipY + indicatorSize) 
                lineTo(center.x + indicatorSize/2, tipY + indicatorSize) 
                close()
            }
            drawPath(indicatorPath, northColor)

            drawContext.canvas.save()
            // Rotate around center: Translate to origin -> Rotate -> Translate back
            drawContext.canvas.translate(center.x, center.y)
            drawContext.canvas.rotate(-heading)
            drawContext.canvas.translate(-center.x, -center.y)

            // Draw Ticks and Labels
            val textPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.15f
                color = android.graphics.Color.WHITE
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            for (i in 0 until 360 step 2) {
                // i is angle in degrees
                val angleRad = Math.toRadians((i - 90).toDouble()) // Subtract 90 to start at Top (North)
                
                // Tick Lengths
                val isCardinal = i % 90 == 0
                val isMajor = i % 30 == 0
                
                // Skip drawing if not at least 5-degree step
                if (i % 5 != 0) continue

                val tickLen = if (isCardinal) radius * 0.15f else if (isMajor) radius * 0.1f else radius * 0.05f
                val startRadius = radius - tickLen - (radius * 0.05f) // Padding from edge
                val stopRadius = radius - (radius * 0.05f)

                val startX = (center.x + startRadius * Math.cos(angleRad)).toFloat()
                val startY = (center.y + startRadius * Math.sin(angleRad)).toFloat()
                val endX = (center.x + stopRadius * Math.cos(angleRad)).toFloat()
                val endY = (center.y + stopRadius * Math.sin(angleRad)).toFloat()

                // Stroke width relative to radius
                val strokeScale = radius / 100f // Normalize to ~100px base
                val strokeW = if (isCardinal) 5f * strokeScale else if (isMajor) 3f * strokeScale else 1f * strokeScale

                drawLine(
                    color = if (isCardinal || isMajor) tickColor else tickColorMinor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW
                )

                // Draw Letters / Numbers
                val textRadius = startRadius - (radius * 0.15f)
                val textX = (center.x + textRadius * Math.cos(angleRad)).toFloat()
                val textY = (center.y + textRadius * Math.sin(angleRad)).toFloat()
                
                drawContext.canvas.nativeCanvas.apply {
                    if (isCardinal) {
                        val label = when(i) {
                            0 -> "N"
                            90 -> "E"
                            180 -> "S"
                            270 -> "W"
                            else -> ""
                        }
                        textPaint.color = if (i == 0) northColor.toArgb() else cardinalColor.toArgb()
                        textPaint.textSize = radius * 0.25f
                        val fontMetrics = textPaint.fontMetrics
                        val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                        drawText(label, textX, textY - offset, textPaint)
                    } else if (isMajor) {
                        textPaint.color = tickColor.toArgb()
                        textPaint.textSize = radius * 0.15f
                        val fontMetrics = textPaint.fontMetrics
                        val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                        drawText(i.toString(), textX, textY - offset, textPaint)
                    }
                }
            }
            
            drawContext.canvas.restore()

            // Draw Center Text (Static)
            drawContext.canvas.nativeCanvas.apply {
                val degrees = heading.toInt()
                val direction = getDirectionLabel(degrees)
                
                // 1. Draw Degrees
                textPaint.color = centerTextColor.toArgb()
                textPaint.textSize = radius * 0.4f
                val fontMetricsDeg = textPaint.fontMetrics
                val centerOffsetDeg = (fontMetricsDeg.descent + fontMetricsDeg.ascent) / 2
                drawText("$degrees°", center.x, center.y + (radius * -0.15f) - centerOffsetDeg, textPaint)
                
                // 2. Draw Direction
                textPaint.color = centerSubTextColor.toArgb()
                textPaint.textSize = radius * 0.35f
                val fontMetricsDir = textPaint.fontMetrics
                val centerOffsetDir = (fontMetricsDir.descent + fontMetricsDir.ascent) / 2
                drawText(direction, center.x, center.y + (radius * 0.25f) - centerOffsetDir, textPaint)
            }
        }
    }
}

private fun getDirectionLabel(degrees: Int): String {
    // 0 = N, 45=NE, 90=E...
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val normalized = ((degrees % 360) + 360) % 360
    val index = Math.round((normalized / 45.0)).toInt() % 8
    return directions[index]
}
