package com.example.timestampcamera.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ImageFormat {
    JPEG, PNG, WEBP
}

data class CameraSettings(
    val flipFrontPhoto: Boolean = true,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val compressionQuality: Int = 90,
    val saveExif: Boolean = true,
    val customSavePath: String? = null,
    val targetWidth: Int = -1,
    val targetHeight: Int = -1,
    val videoQuality: String = "FHD 1080p",
    val aspectRatio: String = "4:3",
    val dateWatermarkEnabled: Boolean = true,
    val shutterSoundEnabled: Boolean = true,
    val gridLinesEnabled: Boolean = false,
    val mapOverlayEnabled: Boolean = false,
    val customNote: String = "",
    val dateFormat: String = "dd/MM/yyyy HH:mm",
    val isThaiLanguage: Boolean = false,
    val textShadowEnabled: Boolean = false,
    val textBackgroundEnabled: Boolean = false,
    // Phase 14: Advanced Styling
    val textColor: Int = android.graphics.Color.WHITE,
    val textSize: Float = 36f,
    val textStyle: Int = 0, // 0=Normal, 1=Bold, 2=Monospace
    val gpsFormat: Int = 0, // 0=Decimal, 1=DMS
    val batterySaverMode: Boolean = false,
    // Phase 15: Pro Typography
    val textAlpha: Int = 255, // 0-255
    val fontFamily: String = "sans", // sans, serif, monospace, cursive
    val overlayPosition: String = "BOTTOM_RIGHT", // Name of OverlayPosition enum
    // Phase 16: Rich Data Overlays
    val compassEnabled: Boolean = false,
    val altitudeEnabled: Boolean = false,
    val speedEnabled: Boolean = false,
    // Phase 17: Professional Workflow
    val projectName: String = "",
    val inspectorName: String = "",
    val tags: String = "", // Comma-separated

    val textStrokeEnabled: Boolean = false,
    val textStrokeWidth: Float = 3f,
    val textStrokeColor: Int = android.graphics.Color.BLACK,
    val googleFontName: String = "Roboto", // e.g. "Roboto", "Oswald"
    val templateId: Int = 0, // 0=Classic, 1=Modern, 2=Minimal
    val compassTapeEnabled: Boolean = false,
    val customLogoPath: String? = null // Path to locally saved PNG
)




private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "camera_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val FLIP_FRONT_PHOTO = booleanPreferencesKey("flip_front_photo")
        val IMAGE_FORMAT = stringPreferencesKey("image_format")
        val COMPRESSION_QUALITY = intPreferencesKey("compression_quality")
        val SAVE_EXIF = booleanPreferencesKey("save_exif")
        val CUSTOM_SAVE_PATH = stringPreferencesKey("custom_save_path")
        val TARGET_WIDTH = intPreferencesKey("target_width")
        val TARGET_HEIGHT = intPreferencesKey("target_height")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val DATE_WATERMARK = booleanPreferencesKey("date_watermark")
        val SHUTTER_SOUND = booleanPreferencesKey("shutter_sound")
        val GRID_LINES = booleanPreferencesKey("grid_lines")
        val MAP_OVERLAY = booleanPreferencesKey("map_overlay")
        val CUSTOM_NOTE = stringPreferencesKey("custom_note")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val IS_THAI_LANGUAGE = booleanPreferencesKey("is_thai_language")
        val TEXT_SHADOW = booleanPreferencesKey("text_shadow")
        val TEXT_BACKGROUND = booleanPreferencesKey("text_background")
        val TEXT_COLOR = intPreferencesKey("text_color")
        val TEXT_SIZE = floatPreferencesKey("text_size")
        val TEXT_STYLE = intPreferencesKey("text_style")
        val GPS_FORMAT = intPreferencesKey("gps_format")
        val BATTERY_SAVER_MODE = booleanPreferencesKey("battery_saver_mode")
        val TEXT_ALPHA = intPreferencesKey("text_alpha")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        val COMPASS_ENABLED = booleanPreferencesKey("compass_enabled")
        val ALTITUDE_ENABLED = booleanPreferencesKey("altitude_enabled")
        val SPEED_ENABLED = booleanPreferencesKey("speed_enabled")
        val PROJECT_NAME = stringPreferencesKey("project_name")
        val INSPECTOR_NAME = stringPreferencesKey("inspector_name")
        val TAGS = stringPreferencesKey("tags")

        val TEXT_STROKE_ENABLED = booleanPreferencesKey("text_stroke_enabled")
        val TEXT_STROKE_WIDTH = floatPreferencesKey("text_stroke_width")
        val TEXT_STROKE_COLOR = intPreferencesKey("text_stroke_color")
        val GOOGLE_FONT_NAME = stringPreferencesKey("google_font_name")
        val TEMPLATE_ID = intPreferencesKey("template_id")
        val COMPASS_TAPE_ENABLED = booleanPreferencesKey("compass_tape_enabled")
        val CUSTOM_LOGO_PATH = stringPreferencesKey("custom_logo_path")
    }


    val cameraSettingsFlow: Flow<CameraSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val formatStr = preferences[PreferencesKeys.IMAGE_FORMAT] ?: ImageFormat.JPEG.name
            val imageFormat = try {
                ImageFormat.valueOf(formatStr)
            } catch (e: Exception) {
                ImageFormat.JPEG
            }

            CameraSettings(
                flipFrontPhoto = preferences[PreferencesKeys.FLIP_FRONT_PHOTO] ?: true,
                imageFormat = imageFormat,
                compressionQuality = preferences[PreferencesKeys.COMPRESSION_QUALITY] ?: 90,
                saveExif = preferences[PreferencesKeys.SAVE_EXIF] ?: true,
                customSavePath = preferences[PreferencesKeys.CUSTOM_SAVE_PATH],
                targetWidth = preferences[PreferencesKeys.TARGET_WIDTH] ?: -1,
                targetHeight = preferences[PreferencesKeys.TARGET_HEIGHT] ?: -1,
                videoQuality = preferences[PreferencesKeys.VIDEO_QUALITY] ?: "FHD 1080p",
                aspectRatio = preferences[PreferencesKeys.ASPECT_RATIO] ?: "4:3",
                dateWatermarkEnabled = preferences[PreferencesKeys.DATE_WATERMARK] ?: true,
                shutterSoundEnabled = preferences[PreferencesKeys.SHUTTER_SOUND] ?: true,
                gridLinesEnabled = preferences[PreferencesKeys.GRID_LINES] ?: false,
                mapOverlayEnabled = preferences[PreferencesKeys.MAP_OVERLAY] ?: false,
                customNote = preferences[PreferencesKeys.CUSTOM_NOTE] ?: "",
                dateFormat = preferences[PreferencesKeys.DATE_FORMAT] ?: "dd/MM/yyyy HH:mm",
                isThaiLanguage = preferences[PreferencesKeys.IS_THAI_LANGUAGE] ?: false,
                textShadowEnabled = preferences[PreferencesKeys.TEXT_SHADOW] ?: false,
                textBackgroundEnabled = preferences[PreferencesKeys.TEXT_BACKGROUND] ?: false,
                textColor = preferences[PreferencesKeys.TEXT_COLOR] ?: android.graphics.Color.WHITE,
                textSize = preferences[PreferencesKeys.TEXT_SIZE] ?: 36f,
                textStyle = preferences[PreferencesKeys.TEXT_STYLE] ?: 0,
                gpsFormat = preferences[PreferencesKeys.GPS_FORMAT] ?: 0,
                batterySaverMode = preferences[PreferencesKeys.BATTERY_SAVER_MODE] ?: false,
                textAlpha = preferences[PreferencesKeys.TEXT_ALPHA] ?: 255,
                fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "sans",
                overlayPosition = preferences[PreferencesKeys.OVERLAY_POSITION] ?: "BOTTOM_RIGHT",
                compassEnabled = preferences[PreferencesKeys.COMPASS_ENABLED] ?: false,
                altitudeEnabled = preferences[PreferencesKeys.ALTITUDE_ENABLED] ?: false,
                speedEnabled = preferences[PreferencesKeys.SPEED_ENABLED] ?: false,
                projectName = preferences[PreferencesKeys.PROJECT_NAME] ?: "",
                inspectorName = preferences[PreferencesKeys.INSPECTOR_NAME] ?: "",
                tags = preferences[PreferencesKeys.TAGS] ?: "",

                textStrokeEnabled = preferences[PreferencesKeys.TEXT_STROKE_ENABLED] ?: false,
                textStrokeWidth = preferences[PreferencesKeys.TEXT_STROKE_WIDTH] ?: 3f,
                textStrokeColor = preferences[PreferencesKeys.TEXT_STROKE_COLOR] ?: android.graphics.Color.BLACK,
                googleFontName = preferences[PreferencesKeys.GOOGLE_FONT_NAME] ?: "Roboto",
                templateId = preferences[PreferencesKeys.TEMPLATE_ID] ?: 0,
                compassTapeEnabled = preferences[PreferencesKeys.COMPASS_TAPE_ENABLED] ?: false,
                customLogoPath = preferences[PreferencesKeys.CUSTOM_LOGO_PATH]
            )
        }



    suspend fun updateFlipFrontPhoto(flip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLIP_FRONT_PHOTO] = flip
        }
    }

    suspend fun updateImageFormat(format: ImageFormat) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_FORMAT] = format.name
        }
    }

    suspend fun updateCompressionQuality(quality: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPRESSION_QUALITY] = quality.coerceIn(10, 100)
        }
    }

    suspend fun updateSaveExif(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_EXIF] = save
        }
    }

    suspend fun updateCustomSavePath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(PreferencesKeys.CUSTOM_SAVE_PATH)
            } else {
                preferences[PreferencesKeys.CUSTOM_SAVE_PATH] = path
            }
        }
    }

    suspend fun updateTargetResolution(width: Int, height: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_WIDTH] = width
            preferences[PreferencesKeys.TARGET_HEIGHT] = height
        }
    }

    suspend fun updateVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_QUALITY] = quality
        }
    }

    suspend fun updateAspectRatio(ratio: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASPECT_RATIO] = ratio
        }
    }

    suspend fun updateDateWatermark(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATE_WATERMARK] = enabled
        }
    }

    suspend fun updateShutterSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHUTTER_SOUND] = enabled
        }
    }

    suspend fun updateGridLines(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_LINES] = enabled
        }
    }

    suspend fun updateMapOverlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAP_OVERLAY] = enabled
        }
    }

    suspend fun updateCustomNote(note: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_NOTE] = note
        }
    }

    suspend fun updateDateFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATE_FORMAT] = format
        }
    }

    suspend fun updateTemplateId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEMPLATE_ID] = id
        }
    }

    suspend fun updateCompassTapeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPASS_TAPE_ENABLED] = enabled
        }
    }

    suspend fun updateCustomLogoPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(PreferencesKeys.CUSTOM_LOGO_PATH)
            } else {
                preferences[PreferencesKeys.CUSTOM_LOGO_PATH] = path
            }
        }
    }

    suspend fun updateThaiLanguage(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_THAI_LANGUAGE] = enabled
        }
    }

    suspend fun updateTextShadow(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_SHADOW] = enabled
        }
    }

    suspend fun updateTextBackground(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_BACKGROUND] = enabled
        }
    }

    suspend fun updateTextColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_COLOR] = color
        }
    }

    suspend fun updateTextSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_SIZE] = size
        }
    }

    suspend fun updateTextStyle(style: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_STYLE] = style
        }
    }

    suspend fun updateGpsFormat(format: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GPS_FORMAT] = format
        }
    }

    suspend fun updateBatterySaverMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BATTERY_SAVER_MODE] = enabled
        }
    }

    suspend fun updateTextAlpha(alpha: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_ALPHA] = alpha.coerceIn(0, 255)
        }
    }

    suspend fun updateFontFamily(fontFamily: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_FAMILY] = fontFamily
        }
    }

    suspend fun updateOverlayPosition(positionName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_POSITION] = positionName
        }
    }

    suspend fun updateCompassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPASS_ENABLED] = enabled
        }
    }

    suspend fun updateAltitudeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALTITUDE_ENABLED] = enabled
        }
    }

    suspend fun updateSpeedEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEED_ENABLED] = enabled
        }
    }

    suspend fun updateProjectName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROJECT_NAME] = name
        }
    }

    suspend fun updateInspectorName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INSPECTOR_NAME] = name
        }
    }

    suspend fun updateTags(tags: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAGS] = tags
        }
    }



    suspend fun updateTextStrokeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_STROKE_ENABLED] = enabled
        }
    }

    suspend fun updateTextStrokeWidth(width: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_STROKE_WIDTH] = width
        }
    }

    suspend fun updateTextStrokeColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_STROKE_COLOR] = color
        }
    }

    suspend fun updateGoogleFontName(fontName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_FONT_NAME] = fontName
        }
    }
}

