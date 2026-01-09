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

enum class AddressResolution {
    NONE,
    COUNTRY,
    PROVINCE,
    DISTRICT,
    STREET,
    HOUSE_NO_STREET,
    HOUSE_NO,
    FULL_ADDRESS_NO_ZIP,
    FULL_ADDRESS,
    FULL_ADDRESS_BREAK_ZIP
}

enum class WatermarkItemType(val displayName: String) {
    DATE_TIME("Date & Time"),
    GPS("GPS Coordinates"),
    COMPASS("Compass Heading"),
    ADDRESS("Address"),
    ALTITUDE_SPEED("Altitude & Speed"),
    PROJECT_NAME("Project Name"),
    INSPECTOR_NAME("Inspector Name"),
    NOTE("Note"),
    TAGS("Tags"),

    CUSTOM_TEXT("Custom Text"), // Deprecated or kept for fallback
    LOGO("Logo")
}

enum class FileNameFormat {
    TIMESTAMP_PROJECT,
    TIMESTAMP_ADDRESS,
    INDEX_TIMESTAMP,
    UNIQUE_ID,
    BS_PROJECT_ADDRESS_TIMESTAMP,
    ADDRESS_TIMESTAMP,
    BS_PROJECT_TIMESTAMP,
    TAG_BS_PROJECT_TIMESTAMP,
    BS_PROJECT_TIMESTAMP_TAG,
    TIMESTAMP_UNDERSCORE
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
        WatermarkItemType.GPS,
        WatermarkItemType.COMPASS,
        WatermarkItemType.ADDRESS,
        WatermarkItemType.ALTITUDE_SPEED,
        WatermarkItemType.PROJECT_NAME,
        WatermarkItemType.INSPECTOR_NAME,
        WatermarkItemType.NOTE,
        WatermarkItemType.TAGS,

    ),
    val textShadowEnabled: Boolean = true,
    val textStrokeEnabled: Boolean = false,
    val textBackgroundEnabled: Boolean = false,
    
    // Additional data holders (can be populated by ViewModel before drawing)
    val customText: String = "",
    val projectName: String = "",
    val inspectorName: String = "",
    val addressResolution: AddressResolution = AddressResolution.FULL_ADDRESS
)
