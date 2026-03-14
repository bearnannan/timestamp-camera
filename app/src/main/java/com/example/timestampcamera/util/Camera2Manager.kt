package com.example.timestampcamera.util

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.*
import android.hardware.camera2.CameraMetadata.*
import android.util.Range
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class Camera2Capabilities(
    val supportedIsoRanges: List<IntRange>,
    val supportedExposureRanges: List<LongRange>,
    val supportedWhiteBalanceModes: List<Int>,
    val supportedFocusModes: List<Int>,
    val minFocusDistance: Float,
    val hasManualSensor: Boolean,
    val hasAutoFocus: Boolean
)

data class Camera2Settings(
    val iso: Int = 100,
    val exposureTime: Long = 10000000L, // nanoseconds
    val whiteBalance: String = "AUTO",
    val focusMode: String = "AUTO",
    val focusDistance: Float = 0f // 0 = infinity, >0 = specific distance
)

@ExperimentalCamera2Interop
class Camera2Manager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var camera2CameraControl: Camera2CameraControl? = null
    private var camera2CameraInfo: Camera2CameraInfo? = null
    
    private var currentCapabilities: Camera2Capabilities? = null
    
    suspend fun initialize() = withContext(Dispatchers.Main) {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Bind to lifecycle to get camera control
            cameraProvider?.unbindAll()
            // Note: This would normally be done with a PreviewView, but for our case
            // we'll just get the camera control without binding a use case
            
        } catch (e: Exception) {
            throw Exception("Failed to initialize Camera2: ${e.message}")
        }
    }
    
    fun setCameraControl(control: CameraControl, info: CameraInfo) {
        this.cameraControl = control
        this.cameraInfo = info
        
        // Get Camera2 interop
        camera2CameraControl = Camera2CameraControl.from(control)
        camera2CameraInfo = Camera2CameraInfo.from(info)
        
        // Extract capabilities
        currentCapabilities = extractCapabilities()
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    private fun extractCapabilities(): Camera2Capabilities? {
        val camera2Info = camera2CameraInfo ?: return null
        
        return try {
            // ISO ranges
            val isoRanges = mutableListOf<IntRange>()
            camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { range: Range<Int> ->
                isoRanges.add(IntRange(range.lower, range.upper))
            }
            
            // Exposure time ranges
            val exposureRanges = mutableListOf<LongRange>()
            camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { range: Range<Long> ->
                exposureRanges.add(LongRange(range.lower, range.upper))
            }
            
            // White balance modes
            val wbModes = mutableListOf<Int>()
            camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.let { modes: IntArray ->
                wbModes.addAll(modes.toList())
            }
            
            // Focus modes
            val focusModes = mutableListOf<Int>()
            camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.let { modes: IntArray ->
                focusModes.addAll(modes.toList())
            }
            
            // Minimum focus distance
            val minFocusDistance = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            
            // Capabilities flags
            val capabilities = camera2Info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val hasManualSensor = capabilities?.any { it == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR } ?: false
            val hasAutoFocus = focusModes.isNotEmpty()
            
            Camera2Capabilities(
                supportedIsoRanges = isoRanges,
                supportedExposureRanges = exposureRanges,
                supportedWhiteBalanceModes = wbModes,
                supportedFocusModes = focusModes,
                minFocusDistance = minFocusDistance,
                hasManualSensor = hasManualSensor,
                hasAutoFocus = hasAutoFocus
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCapabilities(): Camera2Capabilities? = currentCapabilities
    
    @SuppressLint("UnsafeOptInUsageError")
    suspend fun applyManualSettings(settings: Camera2Settings): Boolean = withContext(Dispatchers.IO) {
        val capabilities = currentCapabilities ?: return@withContext false
        val control = camera2CameraControl ?: return@withContext false
        
        try {
            // Check if manual sensor is supported
            if (!capabilities.hasManualSensor) {
                return@withContext false
            }
            
            // Apply ISO
            if (capabilities.supportedIsoRanges.isNotEmpty()) {
                val isoRange = capabilities.supportedIsoRanges.first()
                val clampedIso = settings.iso.coerceIn(isoRange.first, isoRange.last)
                
                val isoOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)
                    .build()
                
                control.addCaptureRequestOptions(isoOptions)
            }
            
            // Apply Exposure Time
            if (capabilities.supportedExposureRanges.isNotEmpty()) {
                val exposureRange = capabilities.supportedExposureRanges.first()
                val clampedExposure = settings.exposureTime.coerceIn(exposureRange.first, exposureRange.last)
                
                val exposureOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedExposure)
                    .build()
                
                control.addCaptureRequestOptions(exposureOptions)
            }
            
            // Apply White Balance
            val wbMode = mapWhiteBalanceToCamera2(settings.whiteBalance)
            if (wbMode != null && capabilities.supportedWhiteBalanceModes.contains(wbMode)) {
                val wbOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, wbMode)
                    .build()
                
                control.addCaptureRequestOptions(wbOptions)
            }
            
            // Apply Focus Mode
            val focusMode = mapFocusModeToCamera2(settings.focusMode)
            if (focusMode != null && capabilities.supportedFocusModes.contains(focusMode)) {
                val focusOptionsBuilder = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, focusMode)
                
                if (focusMode == CaptureRequest.CONTROL_AF_MODE_OFF && settings.focusDistance > 0) {
                    focusOptionsBuilder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, settings.focusDistance)
                }
                
                control.addCaptureRequestOptions(focusOptionsBuilder.build())
            }
            
            // Set control mode to manual when all manual settings are applied
            if (settings.iso > 100 || settings.exposureTime != 10000000L) {
                val manualModeOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                    .build()
                
                control.addCaptureRequestOptions(manualModeOptions)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    suspend fun resetToAuto(): Boolean = withContext(Dispatchers.IO) {
        val control = camera2CameraControl ?: return@withContext false
        
        try {
            // Reset all settings to auto
            val autoOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 100)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 10000000L)
                .build()
            
            control.clearCaptureRequestOptions()
            control.addCaptureRequestOptions(autoOptions)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun mapWhiteBalanceToCamera2(wb: String): Int? {
        return when (wb.uppercase()) {
            "AUTO" -> CONTROL_AWB_MODE_AUTO
            "DAYLIGHT" -> CONTROL_AWB_MODE_DAYLIGHT
            "CLOUDY" -> CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
            "FLUORESCENT" -> CONTROL_AWB_MODE_FLUORESCENT
            "INCANDESCENT" -> CONTROL_AWB_MODE_INCANDESCENT
            "TWILIGHT" -> CONTROL_AWB_MODE_TWILIGHT
            "SHADE" -> CONTROL_AWB_MODE_SHADE
            else -> null
        }
    }
    
    private fun mapFocusModeToCamera2(mode: String): Int? {
        return when (mode.uppercase()) {
            "AUTO" -> CONTROL_AF_MODE_AUTO
            "MANUAL" -> CONTROL_AF_MODE_OFF
            "MACRO" -> CONTROL_AF_MODE_MACRO
            "INFINITY" -> CaptureRequest.CONTROL_AF_MODE_AUTO // No explicit infinity in Camera2, use Auto or Manual 0
            "CONTINUOUS" -> CONTROL_AF_MODE_CONTINUOUS_PICTURE
            "EDOF" -> CONTROL_AF_MODE_EDOF
            else -> null
        }
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    fun getCurrentExposureSettings(): Pair<Int, Long>? {
        val camera2Info = camera2CameraInfo ?: return null
        
        return try {
            camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            // Get current ISO and exposure time from the last capture request
            // Note: This would require access to the actual capture result, which is complex
            // For now, we'll return the default values
            Pair(100, 10000000L)
        } catch (e: Exception) {
            null
        }
    }
    
    fun isManualModeSupported(): Boolean {
        return currentCapabilities?.hasManualSensor ?: false
    }
    
    fun getAvailableIsoValues(): List<Int> {
        val capabilities = currentCapabilities ?: return emptyList()
        return capabilities.supportedIsoRanges.flatMap { range ->
            // Generate some sample ISO values within the range
            listOf(range.first, range.first * 2, range.first * 4, range.last / 4, range.last / 2, range.last)
                .distinct()
                .sorted()
        }
    }
    
    fun getAvailableExposureTimes(): List<Long> {
        val capabilities = currentCapabilities ?: return emptyList()
        return capabilities.supportedExposureRanges.flatMap { range ->
            // Generate some sample exposure times within the range (in nanoseconds)
            listOf(
                1000000L,   // 1/1000s
                2000000L,   // 1/500s
                5000000L,   // 1/200s
                10000000L,  // 1/100s
                20000000L,  // 1/50s
                50000000L,  // 1/20s
                100000000L  // 1/10s
            ).filter { it in range }
        }.distinct().sorted()
    }
    
    fun getAvailableWhiteBalanceModes(): List<String> {
        val capabilities = currentCapabilities ?: return emptyList()
        return capabilities.supportedWhiteBalanceModes.mapNotNull { mode ->
            when (mode) {
                CONTROL_AWB_MODE_AUTO -> "AUTO"
                CONTROL_AWB_MODE_DAYLIGHT -> "DAYLIGHT"
                CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "CLOUDY"
                CONTROL_AWB_MODE_FLUORESCENT -> "FLUORESCENT"
                CONTROL_AWB_MODE_INCANDESCENT -> "INCANDESCENT"
                CONTROL_AWB_MODE_TWILIGHT -> "TWILIGHT"
                CONTROL_AWB_MODE_SHADE -> "SHADE"
                else -> null
            }
        }
    }
    
    fun getAvailableFocusModes(): List<String> {
        val capabilities = currentCapabilities ?: return emptyList()
        return capabilities.supportedFocusModes.mapNotNull { mode ->
            when (mode) {
                CONTROL_AF_MODE_AUTO -> "AUTO"
                CONTROL_AF_MODE_OFF -> "MANUAL"
                CONTROL_AF_MODE_MACRO -> "MACRO"
                CaptureRequest.CONTROL_AF_MODE_AUTO -> "AUTO" // Mapping back
                CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "CONTINUOUS"
                CONTROL_AF_MODE_EDOF -> "EDOF"
                else -> null
            }
        }
    }
    
    fun cleanup() {
        try {
            cameraProvider?.unbindAll()
            (cameraExecutor as? java.util.concurrent.ExecutorService)?.shutdown()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
