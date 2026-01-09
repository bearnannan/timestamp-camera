package com.example.timestampcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timestampcamera.data.LocationData
import java.text.SimpleDateFormat
import java.util.Date
import com.example.timestampcamera.data.CustomField
import java.util.Locale

@Composable
fun CameraInfoOverlay(
    modifier: Modifier = Modifier,
    locationData: LocationData,
    compassHeading: Float,
    date: Date = Date(),
    showDate: Boolean = true,
    showTime: Boolean = true,
    showCoordinates: Boolean = true,
    showHeading: Boolean = false, // From compassEnabled
    showAddress: Boolean = true,
    showAltitude: Boolean = false,
    formattedCoordinates: String = "", // Formatted from ViewModel (DMS/UTM/MGRS)
    formattedAddress: String = "", // Formatted from ViewModel
    showNote: Boolean = false,
    customNote: String = "",
    // Phase 17: Professional Workflow
    projectName: String = "",
    inspectorName: String = "",
    tags: String = "", // Mapped to "Note:"
    speed: Float = 0f,
    showSpeed: Boolean = false,
    dateFormat: String = "dd/MM/yyyy",
    // Duplicates removed
    isThaiLanguage: Boolean = false,
    customFields: List<CustomField> = emptyList(),
    textSize: Float = 36f, // From OverlayConfig
    textStyle: Int = 0, // 0=Normal, 1=Bold
    textColor: Int = android.graphics.Color.WHITE, // From OverlayConfig
    googleFontName: String = "Roboto", // [RESTORED]
    customTextOrder: List<com.example.timestampcamera.data.WatermarkItemType> = emptyList() // [NEW]
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End // Right aligned as requested
    ) {
        // Resolve Font Family
        val fontFamily = when (googleFontName) {
            "Oswald" -> androidx.compose.ui.text.font.FontFamily(
                android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
            )
            "Roboto Mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
            "Playfair Display" -> androidx.compose.ui.text.font.FontFamily.Serif
            "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
            "Inter" -> androidx.compose.ui.text.font.FontFamily.SansSerif
            else -> androidx.compose.ui.text.font.FontFamily.SansSerif
        }

        val shadowStyle = TextStyle(
            fontFamily = fontFamily,
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )
        
        val fontWeight = if (textStyle == 1) FontWeight.Bold else FontWeight.Normal
        val contentColor = Color(textColor)
        val baseFontSize = (textSize / 3f).sp
        val dateFontSize = (baseFontSize.value + 2f).sp

        // Helper Lambda for Text Item
        val TextItem = @Composable { text: String, isDate: Boolean ->
             Text(
                text = text,
                color = contentColor,
                fontSize = if (isDate) dateFontSize else baseFontSize,
                style = shadowStyle,
                fontWeight = fontWeight
            )
        }

        // Default order if empty (fallback)
        val orderToUse = if (customTextOrder.isNotEmpty()) customTextOrder else listOf(
            com.example.timestampcamera.data.WatermarkItemType.DATE_TIME,
            com.example.timestampcamera.data.WatermarkItemType.GPS,
            com.example.timestampcamera.data.WatermarkItemType.COMPASS,
            com.example.timestampcamera.data.WatermarkItemType.ADDRESS,
            com.example.timestampcamera.data.WatermarkItemType.ALTITUDE_SPEED,
            com.example.timestampcamera.data.WatermarkItemType.PROJECT_NAME,
            com.example.timestampcamera.data.WatermarkItemType.INSPECTOR_NAME,
            com.example.timestampcamera.data.WatermarkItemType.NOTE,
            com.example.timestampcamera.data.WatermarkItemType.TAGS
        )

        orderToUse.forEach { item ->
            when (item) {
                com.example.timestampcamera.data.WatermarkItemType.DATE_TIME -> {
                    if (showDate || showTime) {
                         val pattern = if (showDate) dateFormat else ""
                         val fullPattern = if (showTime && showDate) "$pattern HH:mm:ss" else if (showTime) "HH:mm:ss" else pattern
                         val formattedDate = getFormattedDate(date, fullPattern, isThaiLanguage)
                         TextItem(formattedDate, true)
                    }
                }
                com.example.timestampcamera.data.WatermarkItemType.GPS -> {
                    if (showCoordinates) {
                         val coordsText = if (formattedCoordinates.isNotEmpty()) formattedCoordinates else "Lat: %.5f, Lon: %.5f".format(locationData.latitude, locationData.longitude)
                         TextItem(coordsText, false)
                    }
                }
                com.example.timestampcamera.data.WatermarkItemType.COMPASS -> {
                    if (showHeading) {
                         val direction = getDirectionLabel(compassHeading)
                         TextItem("${compassHeading.toInt()}° $direction", false)
                    }
                }
                com.example.timestampcamera.data.WatermarkItemType.ADDRESS -> {
                    if (showAddress && formattedAddress.isNotBlank()) {
                         formattedAddress.split("\n").forEach { TextItem(it, false) }
                    } else if (showAddress && locationData.street.isNotBlank()) {
                         TextItem(locationData.street, false)
                    }
                }
                com.example.timestampcamera.data.WatermarkItemType.ALTITUDE_SPEED -> {
                    val parts = mutableListOf<String>()
                    if (showAltitude) parts.add("Alt: %.1f m".format(locationData.altitude))
                    if (showSpeed) parts.add("Spd: %.1f km/h".format(speed * 3.6f))
                    if (parts.isNotEmpty()) TextItem(parts.joinToString(" "), false)
                }
                com.example.timestampcamera.data.WatermarkItemType.PROJECT_NAME -> {
                    if (projectName.isNotEmpty()) TextItem("Project: $projectName", false)
                }
                com.example.timestampcamera.data.WatermarkItemType.INSPECTOR_NAME -> {
                    if (inspectorName.isNotEmpty()) TextItem("Inspector: $inspectorName", false)
                }
                com.example.timestampcamera.data.WatermarkItemType.NOTE -> {
                     if (showNote && customNote.isNotBlank()) TextItem(customNote, false)
                }
                com.example.timestampcamera.data.WatermarkItemType.TAGS -> {
                     if (tags.isNotEmpty()) TextItem(tags, false)
                     // Also handle custom tags from history if needed, but 'tags' param generally covers active tags
                }
                com.example.timestampcamera.data.WatermarkItemType.CUSTOM_TEXT -> {
                    // Fallback
                    if (showNote && customNote.isNotBlank()) TextItem(customNote, false) 
                }
                com.example.timestampcamera.data.WatermarkItemType.LOGO -> { /* Handle Logo Separately or ignore in text list */ }
                else -> {}
            }
        }
    }
}

// Helper for Thai Date
private fun getFormattedDate(date: Date, pattern: String, useThaiLocale: Boolean): String {
    val locale = if (useThaiLocale) Locale("th", "TH") else Locale.US
    val simpleDateFormat = SimpleDateFormat(pattern, locale)
    var formattedDate = simpleDateFormat.format(date)

    if (useThaiLocale) {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        val yearAD = calendar.get(java.util.Calendar.YEAR)
        val yearBE = yearAD + 543
        
        if (formattedDate.contains(yearAD.toString())) {
            formattedDate = formattedDate.replace(yearAD.toString(), yearBE.toString())
        }
    }
    return formattedDate
}

private fun getDirectionLabel(azimuth: Float): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = Math.round(((azimuth % 360) / 45)).toInt() % 8
    return directions[index]
}
