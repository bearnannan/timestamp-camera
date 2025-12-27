package com.example.timestampcamera.data

import android.location.Address
import androidx.compose.ui.graphics.Color

enum class WatermarkFontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

enum class LocationFormat {
    FULL_ADDRESS,
    SHORT_ADDRESS, // e.g. Soi, District, Province
    CITY_ONLY,
    LAT_LON_ONLY,
    NONE
}

enum class WatermarkItemType {
    DATE_TIME,
    GPS, // Lat/Lon or UTM
    ADDRESS,
    COMPASS,
    ALTITUDE_SPEED,
    CUSTOM_TEXT, // Project, Inspector, Note
    TAGS,
    INDEX_NUMBER,
    LOGO
}

data class WatermarkSettings(
    val fontColor: Long = 0xFFFFFFFF, // ARGB Long
    val fontSize: WatermarkFontSize = WatermarkFontSize.MEDIUM,
    val dateFormat: String = "dd/MM/yyyy HH:mm:ss",
    val useThaiLocale: Boolean = false,
    val locationFormat: LocationFormat = LocationFormat.FULL_ADDRESS,
    val customTags: List<String> = emptyList(),
    val activeItemsOrder: List<WatermarkItemType> = listOf(
        WatermarkItemType.DATE_TIME,
        WatermarkItemType.ADDRESS,
        WatermarkItemType.GPS,
        WatermarkItemType.CUSTOM_TEXT,
        WatermarkItemType.TAGS
    ),
    val textShadowEnabled: Boolean = true,
    val textStrokeEnabled: Boolean = false,
    val textBackgroundEnabled: Boolean = false,
    
    // Additional data holders (can be populated by ViewModel before drawing)
    val customText: String = "",
    val projectName: String = "",
    val inspectorName: String = ""
)
