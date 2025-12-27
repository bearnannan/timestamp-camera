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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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


class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = LocationManager(application)

    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()

    private val _overlayConfig = MutableStateFlow(OverlayConfig())
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    private val _lastCapturedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastCapturedUri: StateFlow<android.net.Uri?> = _lastCapturedUri.asStateFlow()

    // Flash Mode: 2 = Off, 1 = On, 0 = Auto (Matches CameraX ImageCapture.FLASH_MODE_*), 3 = Torch
    private val _flashMode = MutableStateFlow(2) // Default to OFF
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    // Transient State

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

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

    private val settingsRepository = SettingsRepository(application)
    val cameraSettingsFlow: StateFlow<CameraSettings> = settingsRepository.cameraSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CameraSettings())
    
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
                    showMap = settings.mapOverlayEnabled,
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

                    compassTapeEnabled = settings.compassTapeEnabled,
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
        viewModelScope.launch {
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
                    _overlayConfig.value = _overlayConfig.value.copy(azimuth = azimuth)
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
        
        _overlayConfig.value = _overlayConfig.value.copy(
            date = Date(),
            address = data.address,
            latLon = latLonStr,
            altitudeSpeed = "Alt: %.2f M, Speed: %.2f km/h".format(data.altitude, data.speed)
        )
        // Also update the stable display text
        updateStableOverlayText(data)
    }

    fun updateOverlayPosition(position: OverlayPosition) {
        viewModelScope.launch { settingsRepository.updateOverlayPosition(position.name) }
    }

    fun toggleFlash() {
        // Cycle: Off (2) -> On (1) -> Auto (0) -> Torch (3) -> Off (2)
        _flashMode.value = when (_flashMode.value) {
            2 -> 1 // Off -> On
            1 -> 0 // On -> Auto
            0 -> 3 // Auto -> Torch
            else -> 2 // Torch -> Off
        }
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
    fun setShowMap(show: Boolean) { _overlayConfig.value = _overlayConfig.value.copy(showMap = show) }
    fun setMapOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateMapOverlay(enabled) }
    }
    
    fun setCustomNote(note: String) {
        viewModelScope.launch { settingsRepository.updateCustomNote(note) }
    }
    
    fun setDateFormat(format: String) {
        viewModelScope.launch { settingsRepository.updateDateFormat(format) }
    }
    
    fun setThaiLanguage(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateThaiLanguage(enabled) }
    }

    fun setTextShadowEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTextShadow(enabled) }
    }

    fun setTextBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTextBackground(enabled) }
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

    fun updateGoogleFontName(name: String) {
        viewModelScope.launch { settingsRepository.updateGoogleFontName(name) }
    }

    fun updateMapBitmap(bitmap: android.graphics.Bitmap) {
        _overlayConfig.update { it.copy(mapBitmap = bitmap) }
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
    
    fun setShutterSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateShutterSound(enabled) }
    }
    
    fun setGridLinesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateGridLines(enabled) }
    }

    // Macro Logic Removed

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
        if (!recording) {
            _recordingDuration.value = "00:00"
        }
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

    fun loadSupportedResolutions(cameraSelector: CameraSelector) {
        viewModelScope.launch {
            _supportedResolutions.value = fetchSupportedResolutions(cameraSelector)
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

    fun processAndSaveImage(bitmap: Bitmap, isFrontCamera: Boolean, onSaved: (android.net.Uri?) -> Unit) {
        _isCapturing.value = true
        viewModelScope.launch {
            try {
                // 1. Prepare Overlay Config
                val currentConfig = _overlayConfig.value.copy(
                    date = Date(),
                    resolution = "${bitmap.width} x ${bitmap.height}"
                )
                
                // Fetch settings once
                val currentSettings = cameraSettingsFlow.value
                val location = locationManager.getLastLocation()
                
                // 2. Fetch Map (if enabled)
                var mapBitmap: Bitmap? = null
                if (currentSettings.mapOverlayEnabled) {
                     if (location != null) {
                         mapBitmap = fetchStaticMap(location.latitude, location.longitude)
                     }
                }
                
                val configWithMap = currentConfig.copy(mapBitmap = mapBitmap)
                
                // 3. Draw Overlays (in background)
                val overlayedBitmap = withContext(Dispatchers.Default) {
                    OverlayUtils.drawOverlayOnBitmap(bitmap, configWithMap)
                }
                
                // 4. Save Image (in IO)
                
                val uri = ImageSaver.saveImage(
                    bitmap = overlayedBitmap,
                    settings = currentSettings,
                    context = getApplication(),
                    isFrontCamera = isFrontCamera,
                    location = location
                )
                
                // 4. Update UI
                _lastCapturedUri.value = uri
                onSaved(uri)
                
                // Cleanup overlayed bitmap if different from original
                if (overlayedBitmap != bitmap) {
                    overlayedBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing image", e)
                onSaved(null)
            } finally {
                _isCapturing.value = false
                bitmap.recycle() // Recycle the original source bitmap from CameraX
            }
        }
    }
    
    private suspend fun fetchStaticMap(lat: Double, lon: Double): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Using OpenStreetMap.de Static Map API (Free, no key required)
                // Attribution required: © OpenStreetMap contributors
                val urlString = "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=15&size=400x400&maptype=mapnik"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: java.io.InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error fetching static map", e)
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
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
        
        // Use the generic process loop but with the imported bitmap
        // We clone the bitmap because processAndSaveImage recycles the input
        val bitmapCopy = bitmap.copy(bitmap.config, true)
        
        processAndSaveImage(bitmapCopy, isFrontCamera = false, onSaved = { uri ->
            onSaved(uri)
            closeEditor() // Exit editor on save
        })
    }
    fun updateTemplateId(id: Int) {
        viewModelScope.launch {
            settingsRepository.updateTemplateId(id)
        }
    }

    fun updateCompassTapeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCompassTapeEnabled(enabled)
        }
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
