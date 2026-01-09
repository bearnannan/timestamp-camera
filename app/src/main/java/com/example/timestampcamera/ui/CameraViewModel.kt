package com.example.timestampcamera.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timestampcamera.data.LocationManager
import com.example.timestampcamera.data.LocationData
import com.example.timestampcamera.util.OverlayConfig
import com.example.timestampcamera.util.OverlayPosition
import com.example.timestampcamera.util.OverlayUtils
import com.example.timestampcamera.util.ExifUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import android.net.Uri
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import android.media.MediaActionSound
import android.content.Intent
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import android.hardware.camera2.CaptureRequest
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import com.example.timestampcamera.data.SettingsRepository
import com.example.timestampcamera.data.CameraSettings
import com.example.timestampcamera.data.ImageFormat
import com.example.timestampcamera.util.ImageSaver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.graphics.ImageFormat as AndroidImageFormat
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import com.example.timestampcamera.data.GalleryRepository
import com.example.timestampcamera.data.MediaItem
import com.example.timestampcamera.data.CustomField
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.view.LifecycleCameraController


class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = LocationManager(application)
    
    // ORIENTATION STATE
    private val _uiRotation = MutableStateFlow(0f)
    val uiRotation: StateFlow<Float> = _uiRotation.asStateFlow()
    
    private var orientationEventListener: OrientationEventListener? = null
    private var lastSnappedRotation = 0
    
    init {
        setupOrientationListener(application)
    }

    private fun setupOrientationListener(context: android.content.Context) {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                // Snap to 0, 90, 180, 270
                val rotation = when {
                    orientation >= 315 || orientation < 45 -> 0
                    orientation in 45 until 135 -> 270 // 90 deg CW
                    orientation in 135 until 225 -> 180
                    orientation in 225 until 315 -> 90 // 90 deg CCW
                    else -> 0
                }
                
                if (rotation != lastSnappedRotation) {
                    lastSnappedRotation = rotation
                    _uiRotation.value = when(rotation) {
                        90 -> 90f
                        180 -> 180f
                        270 -> -90f
                        else -> 0f
                    }
                    Log.d("CameraViewModel", "Orientation changed: $orientation, Snapped: $rotation")
                }
            }
        }
        orientationEventListener?.enable()
    }



    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()

    private val _overlayConfig = MutableStateFlow(OverlayConfig())
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    private val _lastCapturedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastCapturedUri: StateFlow<android.net.Uri?> = _lastCapturedUri.asStateFlow()

    // Capture State (Lock UI during processing)
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // CAMERA EXTENSIONS STATE
    private var extensionsManager: androidx.camera.extensions.ExtensionsManager? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    // Default to standard selector
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()
    
    // Available Extension Mode (null = None, or ExtensionMode.HDR, etc.)
    private val _activeExtensionMode = MutableStateFlow<Int?>(null)
    val activeExtensionMode: StateFlow<Int?> = _activeExtensionMode.asStateFlow()

    // Available Extension Modes for current camera
    private val _availableExtensionModes = MutableStateFlow<List<Int>>(emptyList())
    
    // User selected mode (null = None/Standard)
    // Note: We default to null (Standard) to avoid forcing modes the user didn't ask for.
    private val _targetExtensionMode = MutableStateFlow<Int?>(null)

    fun initializeExtensions(context: android.content.Context, cameraProvider: ProcessCameraProvider) {
        this.cameraProvider = cameraProvider
        if (extensionsManager != null) {
            refreshCameraSelector(isFront = false) // Default logic
            return
        }
        
        val future = androidx.camera.extensions.ExtensionsManager.getInstanceAsync(context, cameraProvider)
        future.addListener({
            try {
                extensionsManager = future.get()
                // Once initialized, re-calculate selector for current facing
                refreshCameraSelector(isFront = false)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize ExtensionsManager", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }
    
    fun toggleExtensionMode() {
        val available = _availableExtensionModes.value
        if (available.isEmpty()) return
        
        // Cycle Logic:
        // Null (Smart Priority) -> Next Available Mode (skipping current smart choice) -> ... -> -1 -> Null
        
        val currentTarget = _targetExtensionMode.value
        val activeMode = _activeExtensionMode.value
        
        val nextTarget: Int? = if (currentTarget == null) {
            // Currently in Smart Mode.
            // Find what mode effectively ran, and skip to the NEXT one.
            // If activeMode is Night, we want to skip Explicit Night and go to HDR (if exists) or Standard (-1).
            
            val currentIndex = if (activeMode != null) available.indexOf(activeMode) else -1
            
            if (currentIndex != -1 && currentIndex < available.size - 1) {
                 available[currentIndex + 1]
            } else {
                 -1 // Standard
            }
        } else if (currentTarget == -1) {
            // From Standard -> Smart
            null
        } else {
            // From Explicit Mode -> Next Explicit or Standard
            val currentIndex = available.indexOf(currentTarget)
             if (currentIndex != -1 && currentIndex < available.size - 1) {
                 available[currentIndex + 1]
            } else {
                -1 // Explicit Standard
            }
        }
        
        _targetExtensionMode.value = nextTarget
        // Trigger refresh to apply
        refreshCameraSelector()
    }
    
    fun refreshCameraSelector(isFront: Boolean? = null) {
        val facing = if (isFront == true) CameraSelector.LENS_FACING_FRONT 
                     else if (isFront == false) CameraSelector.LENS_FACING_BACK
                     else {
                         // Derive from current
                         val current = _cameraSelector.value
                         if (current == CameraSelector.DEFAULT_FRONT_CAMERA) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                     }
                     
        val baseSelector = CameraSelector.Builder().requireLensFacing(facing).build()
        val manager = extensionsManager
        val provider = cameraProvider
        
        if (manager == null || provider == null) {
            _cameraSelector.value = baseSelector
            _activeExtensionMode.value = null
            _availableExtensionModes.value = emptyList()
            return
        }
        
        // 1. Discovery Phase: Find all supported modes for this lens
        val supportedModes = mutableListOf<Int>()
        
        // Check and Log for Debugging
        val isAuto = manager.isExtensionAvailable(baseSelector, androidx.camera.extensions.ExtensionMode.AUTO)
        val isHdr = manager.isExtensionAvailable(baseSelector, androidx.camera.extensions.ExtensionMode.HDR)
        val isNight = manager.isExtensionAvailable(baseSelector, androidx.camera.extensions.ExtensionMode.NIGHT)
        // val isBokeh = manager.isExtensionAvailable(baseSelector, androidx.camera.extensions.ExtensionMode.BOKEH) // REMOVED
        // val isFaceRetouch = manager.isExtensionAvailable(baseSelector, androidx.camera.extensions.ExtensionMode.FACE_RETOUCH) // REMOVED

        Log.d("CameraViewModel", "Extension Availability: Auto=$isAuto, HDR=$isHdr, Night=$isNight for facing=$facing")
        
        if (isAuto) supportedModes.add(androidx.camera.extensions.ExtensionMode.AUTO)
        if (isHdr) supportedModes.add(androidx.camera.extensions.ExtensionMode.HDR)
        if (isNight) supportedModes.add(androidx.camera.extensions.ExtensionMode.NIGHT)
        // if (isBokeh) supportedModes.add(androidx.camera.extensions.ExtensionMode.BOKEH) // REMOVED
        // if (isFaceRetouch) supportedModes.add(androidx.camera.extensions.ExtensionMode.FACE_RETOUCH) // REMOVED
        
        _availableExtensionModes.value = supportedModes
        
        // 2. Selection Phase: logic
        // Target = -1 -> Explicit Standard (Off)
        // Target = null -> Smart Priority (Night > HDR > Auto)
        // Target = Mode -> Specific Mode
        val target = _targetExtensionMode.value
        
        val modeToUse = when {
            target == -1 -> null // Explicit Standard requested
            target != null && supportedModes.contains(target) -> target // Specific Valid Mode
            target == null -> {
                // Priority Selection: Night > HDR > Auto
                when {
                    supportedModes.contains(androidx.camera.extensions.ExtensionMode.NIGHT) -> androidx.camera.extensions.ExtensionMode.NIGHT
                    supportedModes.contains(androidx.camera.extensions.ExtensionMode.HDR) -> androidx.camera.extensions.ExtensionMode.HDR
                    supportedModes.contains(androidx.camera.extensions.ExtensionMode.AUTO) -> androidx.camera.extensions.ExtensionMode.AUTO
                    else -> null // Standard
                }
            }
            else -> null // Fallback
        }
        
        val finalSelector = if (modeToUse != null) {
            manager.getExtensionEnabledCameraSelector(baseSelector, modeToUse)
        } else {
            baseSelector
        }
        
        _cameraSelector.value = finalSelector
        _activeExtensionMode.value = modeToUse
        Log.d("CameraViewModel", "Extension Mode: ${if(modeToUse != null) "Active ($modeToUse)" else "Standard"} (Target: $target, Auto-Selected: ${target == null})")
        
        // 3. Fetch Exposure Range for the selected camera
        viewModelScope.launch {
            try {
               val cameraInfo = provider.availableCameraInfos.find { 
                    finalSelector.filter(listOf(it)).isNotEmpty() 
                }
                if (cameraInfo != null) {
                    val exposureState = cameraInfo.exposureState
                    val range = exposureState.exposureCompensationRange
                    _exposureRange.value = android.util.Range(range.lower, range.upper)
                    // Reset index to 0 when switching cameras/modes unless we want to persist
                    _exposureIndex.value = 0 
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error fetching exposure range", e)
            }
        }
    }

    // Flash Mode: 2 = Off, 1 = On, 0 = Auto (Matches CameraX ImageCapture.FLASH_MODE_*), 3 = Torch
    private val _flashMode = MutableStateFlow(2) // Default to OFF
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    // Determine if we need a UI Screen Flash (Front Camera + Flash ON/AUTO)
    // Note: Auto (0) on front camera usually behaves like ON for screen flash if light is low, 
    // but for simplicity we can trigger it for ON (1) and arguably AUTO (0).
    // Let's implement specifically for ON (1) to start, or both.
    // Logic: If Front Camera AND (Flash ON OR Flash Auto) -> Screen Flash
    private val _shouldScreenFlash = MutableStateFlow(false)
    val shouldScreenFlash: StateFlow<Boolean> = _shouldScreenFlash.asStateFlow()

    fun updateScreenFlashState(isFrontCamera: Boolean) {
        val mode = _flashMode.value
        // Screen Flash if Front Camera AND (On or Auto)
        _shouldScreenFlash.value = isFrontCamera && (mode == 1 || mode == 0)
    }



    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Shutter Event Flow
    private val _shutterEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val shutterEvent = _shutterEvent.asSharedFlow()

    private val _recordingDuration = MutableStateFlow("00:00")
    val recordingDuration: StateFlow<String> = _recordingDuration.asStateFlow()

    private val shutterSound = MediaActionSound()

    // Timer Mode (0=Off, 3=3s, 10=10s)
    private val _timerMode = MutableStateFlow(0)
    val timerMode: StateFlow<Int> = _timerMode.asStateFlow()
    
    // Countdown State
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
    
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // EDITOR MODE STATE
    private val _isEditorMode = MutableStateFlow(false)
    val isEditorMode: StateFlow<Boolean> = _isEditorMode.asStateFlow()
    
    private val _importedBitmap = MutableStateFlow<Bitmap?>(null)
    val importedBitmap: StateFlow<Bitmap?> = _importedBitmap.asStateFlow()

    // ========== PERFORMANCE OPTIMIZATION: Separated Time State ==========
    // This state updates every second independently, preventing main overlay config
    // from triggering recomposition of the entire CameraContent composable.
    private val _currentDisplayTime = MutableStateFlow("")
    val currentDisplayTime: StateFlow<String> = _currentDisplayTime.asStateFlow()

    // Stable overlay text for display (excludes fast-changing time)
    private val _stableOverlayText = MutableStateFlow("")
    val stableOverlayText: StateFlow<String> = _stableOverlayText.asStateFlow()

    private val _supportedResolutions = MutableStateFlow<List<Size>>(emptyList())
    val supportedResolutions: StateFlow<List<Size>> = _supportedResolutions.asStateFlow()

    // Smart Zoom (Hardware Lens Detection)
    private val _zoomOptions = MutableStateFlow<List<com.example.timestampcamera.util.ZoomConfig>>(emptyList())
    val zoomOptions: StateFlow<List<com.example.timestampcamera.util.ZoomConfig>> = _zoomOptions.asStateFlow()

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    // Exposure Compensation
    private val _exposureIndex = MutableStateFlow(0)
    val exposureIndex: StateFlow<Int> = _exposureIndex.asStateFlow()
    
    private val _exposureRange = MutableStateFlow<android.util.Range<Int>>(android.util.Range(0, 0))
    val exposureRange: StateFlow<android.util.Range<Int>> = _exposureRange.asStateFlow()

    fun updateExposureIndex(index: Int) {
        _exposureIndex.value = index
    }

    // Image Processing Engine (v2.00)
    private val _imageBrightness = MutableStateFlow(1.0f)
    val imageBrightness: StateFlow<Float> = _imageBrightness.asStateFlow()
    
    private val _imageContrast = MutableStateFlow(1.0f)
    val imageContrast: StateFlow<Float> = _imageContrast.asStateFlow()
    
    private val _imageSaturation = MutableStateFlow(1.0f)
    val imageSaturation: StateFlow<Float> = _imageSaturation.asStateFlow()

    fun updateImageEnhancement(brightness: Float, contrast: Float, saturation: Float) {
        _imageBrightness.value = brightness
        _imageContrast.value = contrast
        _imageSaturation.value = saturation
    }

    private val settingsRepository = SettingsRepository(application)
    val cameraSettingsFlow: StateFlow<CameraSettings> = settingsRepository.cameraSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraSettings())

    val noteHistory: StateFlow<Set<String>> = settingsRepository.customNoteHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val tagsHistory: StateFlow<Set<String>> = settingsRepository.tagsHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    // Gallery
    private val galleryRepository = GalleryRepository(application)
    private val _recentMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentMedia: StateFlow<List<MediaItem>> = _recentMedia.asStateFlow()

    init {
        loadLogo()
        startLocationUpdates()
        startCompassUpdates()
        startTimeUpdates()
        loadRecentMedia() // Load gallery thumbnail on startup

        // Sync persistent settings to overlay config for preview
        viewModelScope.launch {
            cameraSettingsFlow.collect { settings ->
                _overlayConfig.value = _overlayConfig.value.copy(
                    showDate = settings.dateWatermarkEnabled,
                    showTime = settings.timeWatermarkEnabled,
                    showAddress = settings.isAddressEnabled,
                    showLatLon = settings.isCoordinatesEnabled,
                    customText = settings.customNote,
                    showCustomText = settings.customNote.isNotEmpty(),
                    datePattern = settings.dateFormat,
                    useThaiLocale = settings.isThaiLanguage,
                    textShadowEnabled = settings.textShadowEnabled, // Styling
                    textBackgroundEnabled = settings.textBackgroundEnabled, // Styling
                    textColor = settings.textColor,
                    textSize = settings.textSize,
                    textStyle = settings.textStyle,
                    alpha = settings.textAlpha,
                    fontFamily = settings.fontFamily,
                    position = try {
                         OverlayPosition.valueOf(settings.overlayPosition)
                    } catch (e: Exception) {
                         OverlayPosition.BOTTOM_RIGHT
                    },
                    compassEnabled = settings.compassEnabled,
                    showCompass = settings.compassEnabled,
                    compassPosition = try {
                         OverlayPosition.valueOf(settings.compassPosition)
                    } catch (e: Exception) {
                         // Default fallback if parsing fails (or if not set)
                         OverlayPosition.BOTTOM_LEFT 
                    },
                    altitudeEnabled = settings.altitudeEnabled,
                    speedEnabled = settings.speedEnabled,
                    projectName = settings.projectName,
                    inspectorName = settings.inspectorName,
                    tags = settings.tags,

                    textStrokeEnabled = settings.textStrokeEnabled,
                    textStrokeWidth = settings.textStrokeWidth,
                    textStrokeColor = settings.textStrokeColor,
                    googleFontName = settings.googleFontName,

                    templateId = settings.templateId,

                    logoBitmap = settings.customLogoPath?.let { path ->
                         try {
                             BitmapFactory.decodeFile(path)
                         } catch (e: Exception) {
                             null
                         }
                    }
                )
            }
        }
    }


    
    private fun loadLogo() {
        val logoFile = File(getApplication<Application>().filesDir, "custom_logo.png")
            val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            _overlayConfig.value = _overlayConfig.value.copy(logoBitmap = bitmap)
        }

    fun setLogo(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uri == null) {
                // Remove Logo
                val logoFile = File(getApplication<Application>().filesDir, "custom_logo.png")
                if (logoFile.exists()) logoFile.delete()
                _overlayConfig.value = _overlayConfig.value.copy(logoBitmap = null)
            } else {
                // Save and Set Logo
                try {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    val logoFile = File(getApplication<Application>().filesDir, "custom_logo.png")
                    val outputStream = FileOutputStream(logoFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    _overlayConfig.value = _overlayConfig.value.copy(logoBitmap = bitmap)
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Error saving logo", e)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            var lastUpdate = 0L
            locationManager.getLocationUpdates().collectLatest {
                _locationData.value = it
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) { // Throttle UI updates to 500ms
                    lastUpdate = now
                    updateOverlayText(it)
                    // Update Raw Data for Rich Overlays
                    _overlayConfig.update { current -> 
                        current.copy(
                            altitude = it.altitude,
                            speed = it.speed
                        )
                    }
                    

                }
            }
        }
    }
    
    private fun startCompassUpdates() {
        viewModelScope.launch {
            var lastUpdate = 0L
            locationManager.getCompassUpdates().collectLatest { azimuth ->
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 100) { // Throttle Compass to 100ms
                    lastUpdate = now
                    _compassHeading.value = azimuth
                    _overlayConfig.value = _overlayConfig.value.copy(
                        azimuth = azimuth,
                        compassHeading = azimuth // Fix: Update both fields
                    )
                }
            }
        }
    }

    // ========== PERFORMANCE OPTIMIZATION: Time Updates ==========
    // Updates display time independently every second without touching overlayConfig.
    // This prevents CameraPreview recomposition on every time tick.
    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                val config = _overlayConfig.value
                if (config.showDate) {
                    var pattern = config.datePattern
                    if (config.showTime) {
                        pattern += " HH:mm:ss"
                    }
                    val locale = if (config.useThaiLocale) java.util.Locale("th", "TH") else java.util.Locale.US
                    val dateFormat = java.text.SimpleDateFormat(pattern, locale)
                    _currentDisplayTime.value = dateFormat.format(Date())
                }
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    // Updates stable overlay text (location data that doesn't change every second)
    private fun updateStableOverlayText(data: LocationData) {
        val config = _overlayConfig.value
        val settings = cameraSettingsFlow.value
        
        val latLonStr = try {
            when (settings.gpsFormat) {
                1 -> LocationManager.toDMS(data.latitude, data.longitude)
                2 -> com.example.timestampcamera.util.GeoUtils.toUTM(data.latitude, data.longitude)
                3 -> com.example.timestampcamera.util.GeoUtils.toMGRS(data.latitude, data.longitude)
                else -> "Lat: %.6f, Lon: %.6f".format(data.latitude, data.longitude)
            }
        } catch (e: Exception) {
            "Lat: %.6f, Lon: %.6f".format(data.latitude, data.longitude)
        }

        _stableOverlayText.value = buildString {
            if (config.showAddress && data.address.isNotEmpty()) append("${data.address}\n")
            if (config.showLatLon) append("$latLonStr\n")
            if (config.showAltitudeSpeed) append("Alt: %.2f M, Speed: %.2f km/h\n".format(data.altitude, data.speed))
            if (config.showCustomText && config.customText.isNotEmpty()) append(config.customText)
        }.trim()
    }

    /**
     * Returns a snapshot of the overlay config with the EXACT capture timestamp.
     * Call this at the moment of capture to ensure accurate timestamping.
     * Thread-safe: Can be called from any thread.
     */
    fun getCurrentOverlayConfigForCapture(): OverlayConfig {
        return _overlayConfig.value.copy(date = Date())
    }

    private fun updateOverlayText(data: LocationData) {
        val settings = cameraSettingsFlow.value
        val latLonStr = try {
            when (settings.gpsFormat) {
                1 -> LocationManager.toDMS(data.latitude, data.longitude)
                2 -> com.example.timestampcamera.util.GeoUtils.toUTM(data.latitude, data.longitude)
                3 -> com.example.timestampcamera.util.GeoUtils.toMGRS(data.latitude, data.longitude)
                else -> "Lat: %.6f, Lon: %.6f".format(data.latitude, data.longitude)
            }
        } catch (e: Exception) {
            "Lat: %.6f, Lon: %.6f".format(data.latitude, data.longitude)
        }
        
        
        val addressStr = com.example.timestampcamera.util.AddressFormatter.formatAddress(data, settings.addressResolution)

        _overlayConfig.value = _overlayConfig.value.copy(
            date = Date(),
            address = addressStr,
            latLon = latLonStr,
            altitudeSpeed = "Alt: %.2f M, Speed: %.2f km/h".format(data.altitude, data.speed)
        )
        // Also update the stable display text
        updateStableOverlayText(data)
    }

    fun updateOverlayPosition(position: OverlayPosition) {
        viewModelScope.launch { settingsRepository.updateOverlayPosition(position.name) }
    }

    fun toggleFlash(isFrontCamera: Boolean) {
        // Cycle: Off (2) -> On (1) -> Auto (0) -> Torch (3) -> Off (2)
        // Note: For Front Camera, we might want to skip Torch if not supported, but UI handles icon.
        _flashMode.value = when (_flashMode.value) {
            2 -> 1 // Off -> On
            1 -> 0 // On -> Auto
            0 -> 3 // Auto -> Torch
            else -> 2 // Torch -> Off
        }
        updateScreenFlashState(isFrontCamera)
    }
    
    fun toggleTimer() {
        val modes = listOf(0, 3, 10)
        val currentIndex = modes.indexOf(_timerMode.value)
        val nextIndex = (currentIndex + 1) % modes.size
        _timerMode.value = modes[nextIndex]
    }
    
    fun startTimer(onFinished: () -> Unit) {
        if (_timerMode.value == 0) {
            onFinished()
            return
        }
        
        viewModelScope.launch {
            _isTimerRunning.value = true
            _timerSeconds.value = _timerMode.value
            
            while (_timerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _timerSeconds.value -= 1
            }
            
            _isTimerRunning.value = false
            onFinished()
        }
    }
    fun setShowDate(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showDate = show) }
    fun setShowAddress(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showAddress = show) }
    fun setShowLatLon(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showLatLon = show) }
    fun setShowAltitudeSpeed(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showAltitudeSpeed = show) }
    fun setShowResolution(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showResolution = show) }
    fun updateUploadOnlyWifi(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateUploadOnlyWifi(enabled) }
    }
    fun updateUploadLowBattery(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateUploadLowBattery(enabled) }
    }
    fun updateAddressResolution(resolution: com.example.timestampcamera.data.AddressResolution) {
        viewModelScope.launch { settingsRepository.updateAddressResolution(resolution) }
    }

    // Fix imports collision if any, using fully qualified names where necessary


    fun updateAddressEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAddressEnabled(enabled) }
    }

    fun updateCoordinatesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateCoordinatesEnabled(enabled) }
    }


    
    fun setCustomNote(note: String) {
        viewModelScope.launch { settingsRepository.updateCustomNote(note) }
    }
    
    fun setDateFormat(format: String) {
        viewModelScope.launch { settingsRepository.updateDateFormat(format) }
    }
    
    fun setGridLinesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateGridLines(enabled) }
    }



    fun setVirtualLevelerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateVirtualLevelerEnabled(enabled) }
    }

    fun setVolumeShutterEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateVolumeShutterEnabled(enabled) }
    }

    fun triggerShutter() {
        viewModelScope.launch {
            _shutterEvent.emit(Unit)
        }
    }

    fun setTextBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTextBackground(enabled) }
    }
    
    fun setThaiLanguage(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateThaiLanguage(enabled) }
    }

    fun setTextShadowEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTextShadow(enabled) }
    }


    fun setShowCompass(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showCompass = show) }
    fun setShowIndex(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showIndex = show) }
    


    // Appearance Settings
    fun updateTextColor(color: Int) {
        viewModelScope.launch { settingsRepository.updateTextColor(color) }
    }

    fun updateTextSize(size: Float) {
        viewModelScope.launch { settingsRepository.updateTextSize(size) }
    }

    fun updateTextStyle(style: Int) {
        viewModelScope.launch { settingsRepository.updateTextStyle(style) }
    }

    fun updateGpsFormat(format: Int) {
        viewModelScope.launch { settingsRepository.updateGpsFormat(format) }
    }
    
    fun updateTextAlpha(alpha: Int) {
        viewModelScope.launch { settingsRepository.updateTextAlpha(alpha) }
    }
    
    fun updateFontFamily(family: String) {
        viewModelScope.launch { settingsRepository.updateFontFamily(family) }
    }
    
    fun updateCompassEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateCompassEnabled(enabled) }
    }

    fun updateAltitudeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAltitudeEnabled(enabled) }
    }

    fun updateSpeedEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSpeedEnabled(enabled) }
    }
    
    fun updateCompassHeading(heading: Float) {
        _overlayConfig.value = _overlayConfig.value.copy(compassHeading = heading)
    }

    fun updateProjectName(name: String) {
        viewModelScope.launch { settingsRepository.updateProjectName(name) }
    }

    fun updateInspectorName(name: String) {
        viewModelScope.launch { settingsRepository.updateInspectorName(name) }
    }

    fun updateTags(tags: String) {
        viewModelScope.launch { settingsRepository.updateTags(tags) }
    }



    fun updateBatterySaverMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBatterySaverMode(enabled) }
    }

    fun updateTextStrokeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTextStrokeEnabled(enabled) }
    }

    fun updateTextStrokeWidth(width: Float) {
        viewModelScope.launch { settingsRepository.updateTextStrokeWidth(width) }
    }

    fun updateTextStrokeColor(color: Int) {
        viewModelScope.launch { settingsRepository.updateTextStrokeColor(color) }
    }

    fun updateCompassPosition(position: String) {
        viewModelScope.launch { settingsRepository.updateCompassPosition(position) }
    }

    fun updateCustomFields(fields: List<CustomField>) {
        viewModelScope.launch { settingsRepository.updateCustomFields(fields) }
    }
    
    fun addToNoteHistory(note: String) {
        viewModelScope.launch { settingsRepository.addToNoteHistory(note) }
    }

    fun addToTagsHistory(tag: String) {
        viewModelScope.launch { settingsRepository.addToTagsHistory(tag) }
    }

    fun updateGoogleFontName(name: String) {
        viewModelScope.launch { settingsRepository.updateGoogleFontName(name) }
    }


    // Date Format Settings


    fun setVideoQuality(quality: String) {
        viewModelScope.launch { settingsRepository.updateVideoQuality(quality) }
    }

    fun setAspectRatio(ratio: String) {
        viewModelScope.launch { settingsRepository.updateAspectRatio(ratio) }
    }
    
    fun setDateWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDateWatermark(enabled) }
    }
    
    fun setTimeWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTimeWatermark(enabled) }
    }
    
    fun setShutterSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateShutterSound(enabled) }
    }
    


    // Macro Logic Removed

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
        if (!recording) {
            _recordingDuration.value = "00:00"
        }
    }

    private var activeRecording: androidx.camera.video.Recording? = null

    @OptIn(androidx.annotation.OptIn::class)
    fun startRecording(
        context: android.content.Context,
        videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>,
        onVideoSaved: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        if (activeRecording != null || _isRecording.value) return
        
        // Ensure Audio Permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onError("Audio permission missing")
            return
        }

        val name = "VID_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())}.mp4"
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                 put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TimestampCamera")
            }
        }

        val mediaStoreOutputOptions = androidx.camera.video.MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
            
        // Use PendingRecording to start
        // Enable Audio
        activeRecording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled() 
            .start(androidx.core.content.ContextCompat.getMainExecutor(context)) { recordEvent ->
                when(recordEvent) {
                    is androidx.camera.video.VideoRecordEvent.Start -> {
                        _isRecording.value = true
                    }
                    is androidx.camera.video.VideoRecordEvent.Status -> {
                         // Update duration
                         val stats = recordEvent.recordingStats
                         val seconds = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
                         updateRecordingDuration(seconds)
                    }
                    is androidx.camera.video.VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        activeRecording = null
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            _lastCapturedUri.value = uri
                            onVideoSaved(uri)
                        } else {
                            activeRecording?.close()
                            activeRecording = null
                            onError("Video capture failed: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null // cleanup
        _isRecording.value = false
    }

    fun updateRecordingDuration(seconds: Long) {

        val mins = seconds / 60
        val secs = seconds % 60
        _recordingDuration.value = String.format("%02d:%02d", mins, secs)
    }

    fun playShutterSound() {
        if (cameraSettingsFlow.value.shutterSoundEnabled) {
            shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    // ========== SETTINGS UPDATES ==========
    fun updateFlipFrontPhoto(flip: Boolean) {
        viewModelScope.launch { settingsRepository.updateFlipFrontPhoto(flip) }
    }

    fun updateImageFormat(format: ImageFormat) {
        viewModelScope.launch { settingsRepository.updateImageFormat(format) }
    }

    fun updateCompressionQuality(quality: Int) {
        viewModelScope.launch { settingsRepository.updateCompressionQuality(quality) }
    }

    fun updateSaveExif(save: Boolean) {
        viewModelScope.launch { settingsRepository.updateSaveExif(save) }
    }

    fun updateCustomSavePath(path: String?) {
        viewModelScope.launch { settingsRepository.updateCustomSavePath(path) }
    }

    fun updateTargetResolution(width: Int, height: Int) {
        viewModelScope.launch { settingsRepository.updateTargetResolution(width, height) }
    }

    fun setCloudPath(path: String) {
        viewModelScope.launch { settingsRepository.updateCloudPath(path) }
    }

    fun loadSupportedResolutions(cameraSelector: CameraSelector) {
        viewModelScope.launch {
            _supportedResolutions.value = fetchSupportedResolutions(cameraSelector)
            _zoomOptions.value = fetchZoomOptions(cameraSelector)
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun fetchZoomOptions(cameraSelector: CameraSelector): List<com.example.timestampcamera.util.ZoomConfig> {
        return withContext(Dispatchers.IO) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(getApplication()).get()
                val cameraInfo = cameraProvider.availableCameraInfos.find { 
                    cameraSelector.filter(listOf(it)).isNotEmpty() 
                } ?: return@withContext emptyList()
                
                val characteristics = Camera2CameraInfo.from(cameraInfo)
                com.example.timestampcamera.util.ZoomUtils.calculateZoomConfig(characteristics)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error fetching zoom options", e)
                emptyList()
            }
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun fetchSupportedResolutions(cameraSelector: CameraSelector): List<Size> {
        return withContext(Dispatchers.IO) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(getApplication()).get()
                // Find CameraInfo for the given selector
                val cameraInfo = cameraProvider.availableCameraInfos.find { 
                    cameraSelector.filter(listOf(it)).isNotEmpty() 
                } ?: throw Exception("Camera not found for selector")
                
                // Use Camera2 interop to query hardware-supported sizes
                val characteristics = Camera2CameraInfo.from(cameraInfo)
                val map = characteristics.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes = map?.getOutputSizes(AndroidImageFormat.JPEG) ?: emptyArray()
                
                sizes.toList()
                    .filter { it.width * it.height >= 1280 * 720 } // Filter reasonable minimum (HD+)
                    .sortedWith(compareByDescending<Size> { it.width * it.height }
                        .thenByDescending { it.width }
                    )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error fetching resolutions", e)
                emptyList()
            }
        }
    }
    
    // Helper to format Size for UI
    fun formatSize(size: Size): String {
        val aspect = size.width.toFloat() / size.height.toFloat()
        val label = when {
            abs(aspect - 1.777f) < 0.1 -> "16:9"
            abs(aspect - 1.333f) < 0.1 -> "4:3"
            abs(aspect - 1.0f) < 0.1 -> "1:1"
            else -> String.format("%.2f:1", aspect)
        }
        val mp = (size.width * size.height) / 1_000_000f
        return "${size.width}x${size.height} ($label, ${String.format("%.1f", mp)}MP)"
    }
    

    fun openGallery(context: android.content.Context) {
        val uri = _lastCapturedUri.value
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            // Open general gallery if no last image
            val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            context.startActivity(intent)
        }
    }

    fun processAndSaveImage(
        bitmap: Bitmap, 
        isFrontCamera: Boolean, 
        rotation: Float = 0f, 
        exifAttributes: Map<String, String> = emptyMap(), 
        onSaved: (android.net.Uri?) -> Unit
    ) {
        _isCapturing.value = true
        // Offload entire pipeline to IO
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Prepare Overlay Config
                val currentConfig = _overlayConfig.value.copy(
                    date = Date(),
                    resolution = "${bitmap.width} x ${bitmap.height}",
                    compassHeading = _compassHeading.value, // Fix: Capture exact heading
                    azimuth = _compassHeading.value
                )
                
                // Fetch settings once
                val currentSettings = cameraSettingsFlow.value
                val location = locationManager.getLastLocation()
                
                // 1.5 Update Config with latest Project Info from Settings
                val configWithProjectInfo = currentConfig.copy(
                    projectName = currentSettings.projectName,
                    inspectorName = currentSettings.inspectorName,
                    customText = currentSettings.customNote,
                    tags = currentSettings.tags,
                    showCustomText = currentSettings.customNote.isNotEmpty()
                )

                
                
                // 2. Prepare Bitmap (Rotate and Flip)
                // 2a. Rotation Logic: Use manual rotation if provided, otherwise check Exif
                var workingBitmap = if (rotation != 0f) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
                    try {
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        rotatedBitmap // Return the new bitmap
                    } catch (e: Exception) {
                        Log.e("CameraViewModel", "Failed to rotate bitmap manually", e)
                        bitmap
                    }
                } else {
                    // Check Exif attributes for orientation (important for imported files)
                    OverlayUtils.rotateBitmapIfNecessary(bitmap, exifAttributes)
                }

                // 2.5. Flip Bitmap (if Front Camera) - AFTER Physical Rotation
                if (isFrontCamera && currentSettings.flipFrontPhoto) {
                    val matrix = android.graphics.Matrix().apply { postScale(-1f, 1f) }
                    try {
                        val flippedBitmap = Bitmap.createBitmap(
                            workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true
                        )
                        if (flippedBitmap != workingBitmap) {
                             // Note: Original recycled in finally block
                             workingBitmap = flippedBitmap
                        }
                    } catch (e: Exception) {
                        Log.e("CameraViewModel", "Error flipping bitmap", e)
                    }
                }
                
                // 2.6. IMPORTANT: If we rotated/flipped, we must tell ExifUtils 
                // to ignore the original orientation tag, otherwise modern galleries 
                // might rotate it BACK to Portrait.
                val finalExifAttributes = if (rotation != 0f || (isFrontCamera && currentSettings.flipFrontPhoto)) {
                    exifAttributes.toMutableMap().apply {
                        put(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, "1") // Normal
                    }
                } else {
                    exifAttributes
                }
                
                // 3. Build Snapshot (Unified Data)
                val lines = mutableListOf<com.example.timestampcamera.util.WatermarkLine>()
                
                // Add lines based on Custom Order
                currentSettings.customTextOrder.forEach { item ->
                    when (item) {
                        com.example.timestampcamera.data.WatermarkItemType.DATE_TIME -> {
                            val dateFormat = java.text.SimpleDateFormat(
                                if (configWithProjectInfo.showTime) "${configWithProjectInfo.datePattern} HH:mm:ss" else configWithProjectInfo.datePattern,
                                if (configWithProjectInfo.useThaiLocale) java.util.Locale("th", "TH") else java.util.Locale.US
                            )
                            lines.add(com.example.timestampcamera.util.WatermarkLine(OverlayUtils.getFormattedDate(Date(), dateFormat.toPattern(), configWithProjectInfo.useThaiLocale)))
                        }
                        com.example.timestampcamera.data.WatermarkItemType.ADDRESS -> {
                            if (configWithProjectInfo.showAddress) {
                                val addressStr = com.example.timestampcamera.util.AddressFormatter.formatAddress(_locationData.value, currentSettings.addressResolution)
                                addressStr.split("\n").forEach {
                                    lines.add(com.example.timestampcamera.util.WatermarkLine(it))
                                }
                            }
                        }
                        com.example.timestampcamera.data.WatermarkItemType.GPS -> {
                            if (configWithProjectInfo.showLatLon && configWithProjectInfo.latLon.isNotEmpty()) {
                                lines.add(com.example.timestampcamera.util.WatermarkLine(configWithProjectInfo.latLon))
                            }
                        }
                        com.example.timestampcamera.data.WatermarkItemType.COMPASS -> {
                             if (configWithProjectInfo.showCompass) {
                                val h = configWithProjectInfo.azimuth
                                val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                                val index = Math.round(((h % 360) / 45)).toInt() % 8
                                lines.add(com.example.timestampcamera.util.WatermarkLine("${h.toInt()}° ${directions[index]}"))
                            }
                        }
                        com.example.timestampcamera.data.WatermarkItemType.ALTITUDE_SPEED -> {
                            val altSpeedParts = mutableListOf<String>()
                            if (configWithProjectInfo.altitudeEnabled) altSpeedParts.add("Alt: %.1f m".format(configWithProjectInfo.altitude))
                            if (configWithProjectInfo.speedEnabled) altSpeedParts.add("Spd: %.1f km/h".format(configWithProjectInfo.speed * 3.6f))
                            if (altSpeedParts.isNotEmpty()) {
                                lines.add(com.example.timestampcamera.util.WatermarkLine(altSpeedParts.joinToString(" ")))
                            } else if (configWithProjectInfo.showAltitudeSpeed && configWithProjectInfo.altitudeSpeed.isNotEmpty()) {
                                lines.add(com.example.timestampcamera.util.WatermarkLine(configWithProjectInfo.altitudeSpeed))
                            }
                        }
                        com.example.timestampcamera.data.WatermarkItemType.PROJECT_NAME -> {
                             if (configWithProjectInfo.projectName.isNotEmpty()) lines.add(com.example.timestampcamera.util.WatermarkLine("Project: ${configWithProjectInfo.projectName}"))
                        }
                        com.example.timestampcamera.data.WatermarkItemType.INSPECTOR_NAME -> {
                             if (configWithProjectInfo.inspectorName.isNotEmpty()) lines.add(com.example.timestampcamera.util.WatermarkLine("Inspector: ${configWithProjectInfo.inspectorName}"))
                        }
                        com.example.timestampcamera.data.WatermarkItemType.NOTE -> {
                             if (configWithProjectInfo.customText.isNotEmpty()) lines.add(com.example.timestampcamera.util.WatermarkLine(configWithProjectInfo.customText))
                        }
                        com.example.timestampcamera.data.WatermarkItemType.TAGS -> {
                             if (configWithProjectInfo.tags.isNotEmpty()) lines.add(com.example.timestampcamera.util.WatermarkLine(configWithProjectInfo.tags))
                        }

                        com.example.timestampcamera.data.WatermarkItemType.CUSTOM_TEXT -> {
                            // Fallback for generic custom text if needed, or alias to Note
                             if (configWithProjectInfo.customText.isNotEmpty()) lines.add(com.example.timestampcamera.util.WatermarkLine(configWithProjectInfo.customText))
                        }
                        com.example.timestampcamera.data.WatermarkItemType.LOGO -> {
                            // Logo is handled by OverlayUtils.drawOverlayFromSnapshot directly via config.logoBitmap/Path
                            // No text line needed for logo in this list
                        }
                    }
                }
                
                val snapshot = com.example.timestampcamera.util.WatermarkSnapshot(
                    lines = lines,
                    compassHeading = if (configWithProjectInfo.compassEnabled) configWithProjectInfo.compassHeading else null
                )

                // 4. Draw Overlays (using Snapshot)
                val overlayedBitmap = withContext(Dispatchers.Default) {
                     OverlayUtils.drawOverlayFromSnapshot(workingBitmap, snapshot, configWithProjectInfo)
                }
                
                // 4. Save Image (in IO)
                
                // Generate Dynamic Filename
                val generatedFileName = com.example.timestampcamera.util.FileNameGenerator.generateFileName(
                    format = currentSettings.fileNameFormat,
                    date = Date(),
                    note = if (configWithProjectInfo.customText.isNotEmpty()) configWithProjectInfo.customText else "Note",
                    address = if (_locationData.value.address.isNotEmpty()) _locationData.value.address else "location",
                    index = 1 // Basic index for now
                )

                // 4a. Save Original if enabled
                var originalUri: Uri? = null
                if (currentSettings.saveOriginalPhoto) {
                   originalUri = ImageSaver.saveImage(
                        bitmap = workingBitmap, // Clean version (flipped if needed)
                        settings = currentSettings,
                        context = getApplication(),
                        isFrontCamera = isFrontCamera,
                        location = location,
                        customFileName = generatedFileName + "_original"
                   )
                }

                // 4b. Save Watermarked
                val uri = ImageSaver.saveImage(
                    bitmap = overlayedBitmap,
                    settings = currentSettings,
                    context = getApplication(),
                    isFrontCamera = isFrontCamera,
                    location = location,
                    customFileName = generatedFileName
                )
                
                // Write EXIF Tags & AI Analysis
                if (uri != null) {
                    // 1. Manual Tags
                    val manualTags = currentSettings.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    
                    // 2. AI Tags (Analyze original bitmap for best quality)
                    try {
                         // Run analysis (suspend)
                         // Note: We use the *original* bitmap (before overlay) for better detection, 
                         // or we could use 'workingBitmap' which is already flipped if needed.
                         val aiTags = com.example.timestampcamera.util.ImageTaggingHelper.analyzeImage(workingBitmap)
                         if (aiTags.isNotEmpty()) {
                             manualTags.addAll(aiTags)
                             android.util.Log.d("CameraViewModel", "AI Tags Added: $aiTags")
                             
                             // Optional: Show toast for feedback?
                             withContext(Dispatchers.Main) {
                                 android.widget.Toast.makeText(getApplication(), "AI Tags: ${aiTags.joinToString()}", android.widget.Toast.LENGTH_SHORT).show()
                             }
                         }
                    } catch (e: Exception) {
                        android.util.Log.e("CameraViewModel", "AI Analysis Error", e)
                    }

                    // Write All Tags
                    com.example.timestampcamera.util.ExifUtils.writeTagsToExif(getApplication(), uri, manualTags)
                    com.example.timestampcamera.util.ExifUtils.saveExifAttributes(getApplication(), uri, finalExifAttributes)
                    
                    // 3. Trigger Cloud Sync (if Note or Cloud Path is set)
                    val note = currentSettings.customNote.trim()
                    val cloudPath = currentSettings.cloudPath.trim()
                    
                    if (note.isNotEmpty() || cloudPath.isNotEmpty()) {
                        try {
                        // Construct Full Target Path: e.g. "Work/2024" + "Site A" -> "Work/2024/Site A"
                        // If Note is empty -> "Work/2024"
                        // If CloudPath is empty -> "Site A"
                        val fullDrivePath = if (cloudPath.isNotEmpty()) {
                            if (note.isNotEmpty()) "$cloudPath/$note" else cloudPath
                        } else {
                            note
                        }

                            // Build Constraints
                            val networkType = if (currentSettings.uploadOnlyWifi) androidx.work.NetworkType.UNMETERED else androidx.work.NetworkType.CONNECTED
                            val constraintBuilder = androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(networkType)
                            
                            if (currentSettings.uploadLowBattery) {
                                constraintBuilder.setRequiresBatteryNotLow(true)
                            }
                            val uploadConstraints = constraintBuilder.build()

                            // Pass URI directly to Worker (safe for Scoped Storage)
                            val uploadRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.timestampcamera.worker.FileUploadWorker>()
                                .setInputData(
                                    androidx.work.workDataOf(
                                        "FILE_PATH" to uri.toString(),
                                        "FOLDER_NAME" to fullDrivePath
                                    )
                                )
                                .setConstraints(uploadConstraints)
                                // Intelligent Retry: Exponential Backoff starting at 10 seconds
                                .setBackoffCriteria(
                                    androidx.work.BackoffPolicy.EXPONENTIAL,
                                    10,
                                    java.util.concurrent.TimeUnit.SECONDS
                                )
                                .build()
                            
                            // 3b. Prepare Original Photo Upload (if exists)
                            var uploadOriginalRequest: androidx.work.OneTimeWorkRequest? = null
                        
                            if (originalUri != null) {
                                try {
                                    val originalDrivePath = "$fullDrivePath/Original"
                                    uploadOriginalRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.timestampcamera.worker.FileUploadWorker>()
                                        .setInputData(
                                            androidx.work.workDataOf(
                                                "FILE_PATH" to originalUri.toString(),
                                                "FOLDER_NAME" to originalDrivePath
                                            )
                                        )
                                        .setConstraints(uploadConstraints)
                                        .setBackoffCriteria(
                                            androidx.work.BackoffPolicy.EXPONENTIAL,
                                            10,
                                            java.util.concurrent.TimeUnit.SECONDS
                                        )
                                        .build()
                                } catch (e: Exception) {
                                    android.util.Log.e("CameraViewModel", "Error prep original upload", e)
                                }
                            }
                            
                            // Execute Chained Work to prevent Race Condition on Folder Creation
                            // 1. Upload Normal -> Creates "Site A"
                            // 2. Upload Original -> Finds "Site A", Creates "Original" inside
                            val workManager = androidx.work.WorkManager.getInstance(getApplication())
                            var chain = workManager.beginWith(uploadRequest)
                            
                            if (uploadOriginalRequest != null) {
                                chain = chain.then(uploadOriginalRequest)
                            }
                            
                            chain.enqueue()
                            android.util.Log.d("CameraViewModel", "Enqueued Upload Chain. Original: ${originalUri != null}")
                        } catch (e: Exception) {
                            android.util.Log.e("CameraViewModel", "Error in cloud upload work", e)
                        }
                    }
                
                // 4. Update UI (Main Thread)
                withContext(Dispatchers.Main) {
                    _lastCapturedUri.value = uri
                    // Refresh gallery list to include the new photo
                    loadRecentMedia()
                    onSaved(uri)
                }
                } // End if (uri != null)
                
                // Cleanup overlayed bitmap if different from original
                if (overlayedBitmap != bitmap) {
                    overlayedBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing image", e)
                withContext(Dispatchers.Main) {
                    onSaved(null)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isCapturing.value = false
                }
                if (!bitmap.isRecycled) {
                    bitmap.recycle() // Recycle the original source bitmap from CameraX
                }
            }
        }
    }
    

    override fun onCleared() {
        super.onCleared()
        orientationEventListener?.disable()
        orientationEventListener = null
        shutterSound.release()
    }
    
    // Gallery Methods
    fun loadRecentMedia() {
        viewModelScope.launch {
            _recentMedia.value = galleryRepository.getRecentMedia()
            // Update last captured URI from gallery if available
            if (_recentMedia.value.isNotEmpty()) {
                _lastCapturedUri.value = _recentMedia.value.first().uri
            }
        }
    }
    
    fun deleteMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            try {
                galleryRepository.deleteMedia(mediaItem)
                // Reload list
                loadRecentMedia()
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error deleting media", e)
            }
        }
    }
    
    // EDITOR MODE LOGIC
    fun loadImportedImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    _importedBitmap.value = bitmap
                    _isEditorMode.value = true
                    
                    // Reset overlay config to show current time for editing (fresh start)
                    _overlayConfig.value = _overlayConfig.value.copy(date = Date())
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error loading imported image", e)
            }
        }
    }
    
    fun closeEditor() {
        _isEditorMode.value = false
        _importedBitmap.value?.recycle()
        _importedBitmap.value = null
    }
    
    fun saveImportedImage(onSaved: (Uri?) -> Unit) {
        val bitmap = _importedBitmap.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            // Use the generic process loop but with the imported bitmap
            // We clone the bitmap because processAndSaveImage recycles the input
            val bitmapCopy = bitmap.copy(bitmap.config, true)
            
            // processAndSaveImage handles its own IO dispatch, but calling it here is fine
            processAndSaveImage(bitmapCopy, isFrontCamera = false, onSaved = { uri ->
                onSaved(uri)
                closeEditor() // Exit editor on save (will be called on Main from processAndSaveImage)
            })
        }
    }
    fun updateTemplateId(id: Int) {
        viewModelScope.launch {
            settingsRepository.updateTemplateId(id)
        }
    }

    fun updateTextOrder(order: List<com.example.timestampcamera.data.WatermarkItemType>) {
        viewModelScope.launch {
            settingsRepository.updateTextOrder(order)
        }
    }

    // Tag Management
    fun addAvailableTag(tag: String) {
        viewModelScope.launch { settingsRepository.addAvailableTag(tag) }
    }

    fun removeAvailableTag(tag: String) {
        viewModelScope.launch { settingsRepository.removeAvailableTag(tag) }
    }

    fun clearAvailableTags() {
        viewModelScope.launch { settingsRepository.clearAvailableTags() }
    }

    fun importAvailableTags(tags: Set<String>) {
        viewModelScope.launch { settingsRepository.updateAvailableTags(tags) }
    }

    fun updateSaveOriginalPhoto(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSaveOriginalPhoto(enabled) }
    }

    fun updateFileNameFormat(format: com.example.timestampcamera.data.FileNameFormat) {
        viewModelScope.launch { settingsRepository.updateFileNameFormat(format) }
    }



    fun onLogoSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "custom_logo.png")
                val outputStream = FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                settingsRepository.updateCustomLogoPath(file.absolutePath)
                Log.d("CameraViewModel", "Logo saved to: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to save logo", e)
            }
        }
    }

    fun onLogoRemove() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val file = File(context.filesDir, "custom_logo.png")
                if (file.exists()) {
                     file.delete()
                }
                settingsRepository.updateCustomLogoPath(null)
            } catch (e: Exception) {
                 Log.e("CameraViewModel", "Failed to remove logo", e)
            }
        }
    }
}
