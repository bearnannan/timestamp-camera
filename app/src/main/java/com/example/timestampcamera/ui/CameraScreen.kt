package com.example.timestampcamera.ui

import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharedFlow

import androidx.core.view.drawToBitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Bedtime
import androidx.camera.extensions.ExtensionMode
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.zIndex
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.livedata.observeAsState
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.timestampcamera.util.OverlayConfig
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


// Zoom helper functions removed - Logic moved to LaunchedEffect


// Color definitions
private val BlackBackground = Color(0xFF000000)
private val OrangeAccent = Color(0xFFFF9800) // Pixel perfect orange
private val WhiteColor = Color(0xFFFFFFFF)
private val SemiTransparentBlack = Color(0x66000000)
private val PillBackground = Color(0xFF1A1A1A).copy(alpha = 0.6f) // Darker pill background
private val DarkGray = Color(0xFF2A2A2A)
private val PlaceholderGray = Color(0xFF8A8A8A)

// Custom Icons/Assets would typically be loaded here, using standard icons for now


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    cameraController: LifecycleCameraController? = null,
    flashMode: Int = 2,
    onFlashClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onMacroClick: () -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    onModeChange: (String) -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onSwitchCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    // Settings from ViewModel
    onVideoQualityChange: (String) -> Unit = {},
    onAspectRatioChange: (String) -> Unit = {},
    onDateWatermarkChange: (Boolean) -> Unit = {},
    onShutterSoundChange: (Boolean) -> Unit = {},
    onGridLinesChange: (Boolean) -> Unit = {},
    // Sensor & Macro from ViewModel
    isCapturing: Boolean = false,
    isRecording: Boolean = false,
    recordingDuration: String = "00:00",
    lastCapturedUri: android.net.Uri? = null,
    activeExtensionMode: Int? = null, // [NEW] Extension Mode
    onExtensionToggle: () -> Unit = {}, // [NEW] Toggle Extension Mode
    // Exposure
    exposureIndex: Int = 0,
    exposureRange: android.util.Range<Int> = android.util.Range(0, 0),
    onExposureChange: (Int) -> Unit = {},

    // Video Callbacks
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    
    // Shutter Event
    shutterEvent: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,

    // Persistent Settings from ViewModel
    cameraSettings: com.example.timestampcamera.data.CameraSettings = com.example.timestampcamera.data.CameraSettings(),
    onFlipFrontPhotoChange: (Boolean) -> Unit = {},
    onImageFormatChange: (com.example.timestampcamera.data.ImageFormat) -> Unit = {},
    onCompressionQualityChange: (Int) -> Unit = {},
    onSaveExifChange: (Boolean) -> Unit = {},
    onCustomSavePathChange: (String?) -> Unit = {},
    // Battery Saver
    onBatterySaverModeChange: (Boolean) -> Unit = {},
    // Custom Notes & Localization
    onCustomNoteChange: (String) -> Unit = {},
    onDateFormatChange: (String) -> Unit = {},
    onThaiLanguageChange: (Boolean) -> Unit = {},
    // Address & Coords Visibility
    onAddressEnabledChange: (Boolean) -> Unit = {},
    onCoordinatesEnabledChange: (Boolean) -> Unit = {},
    // Resolution Switching
    supportedResolutions: List<android.util.Size> = emptyList(),
    onTargetResolutionChange: (Int, Int) -> Unit = { _, _ -> },
    sensorViewModel: SensorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    // Logo Callback
    onLogoClick: (android.net.Uri?) -> Unit = {}
) {
    // Listen for Shutter Event
    LaunchedEffect(Unit) {
        shutterEvent?.collect {
            onCaptureClick()
        }
    }

    // Observe OverlayConfig for Logo Status
    // Observe OverlayConfig for Logo Status
    val viewModel: com.example.timestampcamera.ui.CameraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    // val compassHeading by viewModel.compassHeading.collectAsState() // Removed to avoid conflict, using sensorViewModel below
    val compassHeading by sensorViewModel.compassHeading.collectAsState()
    val devicePitch by sensorViewModel.devicePitch.collectAsState()
    val deviceRoll by sensorViewModel.deviceRoll.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val hasLogo = overlayConfig.logoBitmap != null
    
    // UI State
    var selectedMode by rememberSaveable { mutableStateOf("รูปถ่าย") }
    var selectedZoom by rememberSaveable { mutableStateOf("1") }
    
    // Timer State
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Audio Permission State
    var hasAudioPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            // Retry recording if it was the trigger? For now just state update.
        }
    }
    
    // Timestamp Overlay Effect (Video)
    val timestampEffect = remember {
        com.example.timestampcamera.util.VideoWatermarkUtils.createOverlayEffect {
             // Return current config for capture (thread-safe copy)
             viewModel.getCurrentOverlayConfigForCapture()
        }
    }
    
    // Switch between ImageCapture and VideoCapture based on Mode
    LaunchedEffect(selectedMode, cameraController) {
        if (cameraController != null) {
            
            if (selectedMode == "วิดีโอ") {
                 // Video Mode
                 cameraController.setEnabledUseCases(androidx.camera.view.CameraController.VIDEO_CAPTURE)
                 
                 // Apply Overlay Effect
                 cameraController.setEffects(setOf(timestampEffect))
                 
                 viewModel.setRecording(false) // Reset state
            } else {
                 // Photo Mode (Default)
                 cameraController.setEnabledUseCases(androidx.camera.view.CameraController.IMAGE_CAPTURE)
                 cameraController.clearEffects()
            }
            
            // Rebind handled by CameraView automatically, 
            // but setEnabledUseCases triggers the internal update.
        }
    }

    


    
    val timerMode by viewModel.timerMode.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    
    // EDITOR MODE STATE
    val isEditorMode by viewModel.isEditorMode.collectAsState()
    val importedBitmap by viewModel.importedBitmap.collectAsState()

    // Import Picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.loadImportedImage(uri)
        }
    }

    // Logo Picker
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.onLogoSelected(uri)
        }
    }


    
    // ========== EV SLIDER & SETTINGS STATE ==========
    var showEvSlider by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    // var exposureIndex by remember { mutableStateOf(0) } // Replaced by param
    val scope = rememberCoroutineScope()
    
    // Observed orientation from SensorViewModel (Used for leveler)
    val rollAngle by sensorViewModel.horizonRotation.collectAsState()
    
    // Observed orientation from ViewModel (OrientationEventListener)
    val targetRotation by viewModel.uiRotation.collectAsState()

    val iconRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )
    
    
    // Display Rotation Correction for Compass
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val displayRotationDegrees = when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            // Usually 90 or 270. Since we don't handle configChanges, 
            // the system gives us a fixed orientation for the wide screen.
            90f 
        }
        else -> 0f
    }
    
    // Bottom Sheet State
    val settingsSheetState = rememberModalBottomSheetState()
    
    // Note History
    val noteHistory by viewModel.noteHistory.collectAsState()
    val tagsHistory by viewModel.tagsHistory.collectAsState()
    
    // ========== TWO-WAY ZOOM SYNC ==========
    // Observe camera's actual zoom state for pinch-to-zoom sync
    val zoomState = cameraController?.zoomState?.observeAsState()
    
    // Update UI when camera zoom changes (from pinch gesture or slider)
    // Update UI when camera zoom changes (from pinch gesture or slider)
    val currentZoomOptions by viewModel.zoomOptions.collectAsState()

    LaunchedEffect(zoomState?.value?.zoomRatio, currentZoomOptions) {
        val currentZoom = zoomState?.value?.zoomRatio ?: 1f
        
        // Find nearest preset using Dynamic Options
        // We look for a preset that matches within a small threshold
        val matchedOption = currentZoomOptions.find { config ->
            // Use larger threshold for Telephoto
            val threshold = if (config.ratio > 4f) config.ratio * 0.15f else 0.15f
            abs(currentZoom - config.ratio) < threshold
        }
        
        if (matchedOption != null) {
            selectedZoom = matchedOption.label
        }
    }
    
    // ========== REAL EV CALCULATION WITH REAL-TIME SYNC ==========
    // evText is derived from exposureIndex state for immediate UI updates
    
    // Get exposure step from camera (typically 1/6 or 1/3 EV)
    var exposureStep by remember { mutableStateOf(1f / 6f) } // Default step
    
    // Initialize exposure step from camera
    LaunchedEffect(cameraController) {
        cameraController?.cameraInfo?.exposureState?.let { state ->
            exposureStep = state.exposureCompensationStep.toFloat()
            // exposureIndex computed in ViewModel
        }
    }
    
    // Apply Exposure from ViewModel to Camera
    LaunchedEffect(exposureIndex) {
        try {
            cameraController?.cameraControl?.setExposureCompensationIndex(exposureIndex)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // Calculate EV text
    val evText = remember(exposureIndex, exposureStep) {
        val evValue = exposureIndex * exposureStep
        String.format("%+.1f EV", evValue)
    }
    
    // Legacy polling removed to avoid conflict with State Hoisting
    /*
    LaunchedEffect(cameraController) {
       ...
    }
    */
    
    // ========== FLASH EFFECT STATE ==========
    val flashAlpha = remember { Animatable(0f) }
    val shouldScreenFlash by viewModel.shouldScreenFlash.collectAsState()
    
    // Trigger flash when capturing starts (visual feedback)
    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            // If Front Camera Screen Flash is needed
            if (shouldScreenFlash) {
                // Max brightness white flash
                flashAlpha.snapTo(1.0f) 
                delay(100) // Hold brief moment
                flashAlpha.animateTo(0f, animationSpec = tween(400))
            } else {
                // Standard capture feedback (subtle)
                flashAlpha.snapTo(0.8f)
                flashAlpha.animateTo(0f, animationSpec = tween(300))
            }
        }
    }
    
    // ========== BATTERY SAVER LOGIC ==========
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showBatterySaverOverlay by remember { mutableStateOf(false) }
    
    // Monitor interaction and recording state
    LaunchedEffect(isRecording, cameraSettings.batterySaverMode, lastInteractionTime) {
        if (isRecording && cameraSettings.batterySaverMode) {
            while (true) {
                val idleTime = System.currentTimeMillis() - lastInteractionTime
                if (idleTime > 5000) { // 5 seconds timeout
                    showBatterySaverOverlay = true
                }
                delay(1000)
            }
        } else {
            showBatterySaverOverlay = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
    ) {
        // 1. TOP BAR AREA (Fixed)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlackBackground)
                    .zIndex(1f) // Ensure above preview if overlapping (though column handles it)
            ) {
                TopBar(
                    isEditorMode = isEditorMode,
                    flashMode = flashMode,
                    evValue = evText,
                    onFlashClick = onFlashClick,
                    onEvClick = { showEvSlider = !showEvSlider },
                    onMenuClick = { showSettingsSheet = true },
                    onImportClick = {
                        importLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCloseEditorClick = {
                        viewModel.closeEditor()
                    },
                    iconRotation = iconRotation, 
                    isNightMode = activeExtensionMode == ExtensionMode.NIGHT
                )
            }
        }

        // 2. VIEWFINDER AREA (Flexible Weight)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // ... Viewfinder Logic (CameraPreview, Import, etc.)
            // Determine aspect ratio modifier
            val viewfinderModifier = when (cameraSettings.aspectRatio) {
                "4:3" -> Modifier.aspectRatio(3f / 4f)
                "16:9" -> Modifier.aspectRatio(9f / 16f)
                "1:1" -> Modifier.aspectRatio(1f)
                else -> Modifier.fillMaxSize()
            }

            Box(modifier = viewfinderModifier) {
                if (isEditorMode && importedBitmap != null) {
                    importedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Imported Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (cameraController != null) {
                    // Tap to Focus State
                    var focusOffset by remember { mutableStateOf<Offset?>(null) }
                    var showFocusRing by remember { mutableStateOf(false) }

                    // Auto-hide Focus Ring
                    LaunchedEffect(focusOffset) {
                         if (focusOffset != null) {
                             showFocusRing = true
                             delay(2000) // Keep visible for 2 seconds
                             showFocusRing = false
                             focusOffset = null
                         }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Layer 1: Camera Preview (Bottom)
                        CameraPreview(
                            controller = cameraController,
                            modifier = Modifier.fillMaxSize(),
                            flashMode = flashMode,
                            videoQuality = cameraSettings.videoQuality,
                            photoAspectRatio = if (cameraSettings.aspectRatio == "16:9") 1 else 0
                        )
                        
                        // Layer 2: Transparent Gesture Detector + Focus Ring (Top)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoomChange, _ ->
                                        val currentZoom = zoomState?.value?.zoomRatio ?: 1f
                                        val newZoom = currentZoom * zoomChange
                                        onZoomChange(newZoom)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val x = offset.x
                                        val y = offset.y
                                        
                                        // 1. Update UI State
                                        focusOffset = offset
                                        
                                        // 2. Trigger Camera Focus
                                        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                            size.width.toFloat(), size.height.toFloat()
                                        )
                                        val point = factory.createPoint(x, y)
                                        val action = FocusMeteringAction.Builder(point).build()
                                        cameraController.cameraControl?.startFocusAndMetering(action)
                                    }
                                }
                        ) {
                            FocusRing(
                                offset = focusOffset ?: Offset.Zero,
                                visible = showFocusRing
                            )
                            
                            // Extension Badge (Top Center)
                            // Extension Badge (Top Center) - Always show to allow toggling
                            // Note: We show it if it's "Available" on the device, even if currently Standard.
                            // However, we don't have "available" state passed here yet.
                            // For now, let's just show it always if we are in a camera mode.
                            ExtensionBadge(
                                mode = activeExtensionMode,
                                onClick = onExtensionToggle,
                                modifier = Modifier
                                    .align(Alignment.TopCenter),
                                iconRotation = iconRotation
                            )
                        }
                    }
                } else {
                    CameraPreviewPlaceholder()
                }
                        

                
                // Horizon Level Indicator (Legacy - Enhanced)
                if (cameraSettings.virtualLevelerEnabled) {
                    LevelIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        rollAngle = rollAngle
                    )
                }

                // Grid Lines Overlay
                if (cameraSettings.gridLinesEnabled) {
                    GridLinesOverlay(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Virtual Leveler Overlay (Long Line) - REMOVED per user request

                // Compass Overlay (Center)
                
                // Macro Button Removed
                
                // Mini-Map Overlay Removed

                // Compass Overlay
                if (cameraSettings.compassEnabled) {
                    val compassAlignment = if (targetRotation == 90f) {
                        Alignment.TopEnd // Landscape Left -> Visual Top Left
                    } else if (targetRotation == -90f || targetRotation == 270f) {
                        Alignment.TopStart // Landscape Right -> Visual Top Right
                    } else {
                        // Portrait / Default: Respect the user's setting
                        if (cameraSettings.compassPosition == "CENTER") {
                            Alignment.Center
                        } else {
                            getRotationAwareAlignment(overlayConfig.compassPosition, 0f)
                        }
                    }
                    
                    CompassOverlay(
                        heading = (compassHeading + targetRotation + 360) % 360,
                        modifier = Modifier
                            .align(compassAlignment)
                            .padding(16.dp)
                            .rotate(iconRotation) // Rotate icon upright
                    )
                }
                
                // Camera Info Overlay
                val infoAlignment = if (targetRotation == 90f) {
                    Alignment.BottomStart // Landscape Left -> Visual Bottom Right
                } else if (targetRotation == -90f || targetRotation == 270f) {
                    Alignment.BottomEnd // Landscape Right -> Visual Bottom Left
                } else {
                    getRotationAwareAlignment(overlayConfig.position, 0f) // Default behavior
                }
                
                CameraInfoOverlay(
                    modifier = Modifier
                        .align(infoAlignment)
                        .padding(bottom = 80.dp) // Avoid Zoom Pill/Controls
                        .rotate(iconRotation), // Rotate with device
                    locationData = locationData,
                    compassHeading = compassHeading,
                    showDate = overlayConfig.showDate,
                    showTime = overlayConfig.showTime,
                    showCoordinates = overlayConfig.showLatLon,
                    showHeading = overlayConfig.showCompass,
                    showAddress = overlayConfig.showAddress,
                    showAltitude = overlayConfig.altitudeEnabled,
                    showNote = overlayConfig.showCustomText,
                    customNote = overlayConfig.customText,
                    dateFormat = overlayConfig.datePattern,
                    isThaiLanguage = overlayConfig.useThaiLocale,
                    formattedCoordinates = overlayConfig.latLon,
                    formattedAddress = overlayConfig.address, // Sync with Saved Image
                    // Professional Workflow
                    projectName = overlayConfig.projectName,
                    inspectorName = overlayConfig.inspectorName,
                    tags = overlayConfig.tags,
                    speed = overlayConfig.speed,
                    showSpeed = overlayConfig.speedEnabled,
                    customFields = overlayConfig.customFields, // Pass Custom Fields
                    textSize = overlayConfig.textSize, // Pass Text Size
                    textStyle = overlayConfig.textStyle, // Pass Text Style (Bold/Normal)
                    textColor = overlayConfig.textColor, // From Config
                    customTextOrder = cameraSettings.customTextOrder, // [NEW] Pass Custom Order
                    googleFontName = overlayConfig.googleFontName // Pass Font Name
                )

            }

            // *** Floating Controls (Zoom Pill + Mode Selector) ***
            // Overlaying the bottom of the Viewfinder
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                    // No rotation for Zoom/Mode controls
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Zoom Control Pill
                if (!isEditorMode) {
                    val currentZoomOptions by viewModel.zoomOptions.collectAsState()
                    
                    ZoomControlPill(
                        selectedZoom = selectedZoom,
                        zoomOptions = currentZoomOptions,
                        onZoomSelected = { config ->
                            selectedZoom = config.label
                            onZoomChange(config.ratio)
                        }
                    )
                }

                // 2. Mode Selector
                if (!isRecording && !isEditorMode) {
                    ModeSelector(
                        selectedMode = selectedMode,
                        onModeSelected = { mode ->
                            selectedMode = mode
                            onModeChange(mode)
                        }
                    )
                }
            }
        }

        // 3. BOTTOM ACTION AREA (Solid Black)
        // This is outside the Viewfinder Box, ensuring it is solid black and fixed at bottom
        Box(
             modifier = Modifier
                .fillMaxWidth()
                .background(BlackBackground)
                .navigationBarsPadding() // Handled here
                .padding(vertical = 24.dp) // Padding for the buttons inside the black bar
        ) {
            BottomControlBar(
                isEditorMode = isEditorMode,
                isCapturing = isCapturing || isTimerRunning,
                isRecording = isRecording,
                lastCapturedUri = lastCapturedUri,
                onCaptureClick = {
                     if (isEditorMode) {
                        viewModel.saveImportedImage {
                             android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else if (selectedMode == "วิดีโอ") {
                        if (isRecording) {
                            onStopRecording()
                        } else {
                            // Check Audio Permission
                            if (hasAudioPermission) {
                                onStartRecording()
                            } else {
                                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    } else if (!isTimerRunning) {
                        viewModel.startTimer {
                            onCaptureClick()
                        }
                    }
                },
                onSwitchCameraClick = onSwitchCameraClick,
                onGalleryClick = onGalleryClick,
                iconRotation = 0f // No rotation for shutter and bottom icons
            )
        }
    }


        // ========== FLASH OVERLAY ==========
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }
        
        // Grid Lines Overlay (Last to be on top of viewfinder, but below controls if we matched hierarchy)
        // But controls are in the Column. Let's put Grid Lines inside the Viewfinder Box or on top of it.
        // Currently it was missing from the Viewfinder Box in previous code (it was just SettingsItem and GridLinesOverlay definition below).
        // Let's ensure GridLinesOverlay is actually used if grid is enabled.
        // (Assuming logic for that exists or will be added, for now just placing Flash Effect on top of everything)
        // ========== EV SLIDER INLINE OVERLAY ==========
        if (showEvSlider) {
            // Auto-hide after 3 seconds of inactivity
            LaunchedEffect(exposureIndex) {
                delay(3000)
                showEvSlider = false
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkGray.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current EV Value - Large Orange text
                        Text(
                            text = evText.replace(" EV", ""), // Just the number
                            color = OrangeAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Exposure Slider (-12 to +12 steps)
                        Slider(
                            value = exposureIndex.toFloat(),
                            onValueChange = { newValue ->
                                onExposureChange(newValue.toInt())
                                // cameraController?.cameraControl?.setExposureCompensationIndex(exposureIndex) // Handled by LaunchedEffect
                            },
                            valueRange = exposureRange.lower.toFloat()..exposureRange.upper.toFloat(),
                            steps = (exposureRange.upper - exposureRange.lower),
                            colors = SliderDefaults.colors(
                                thumbColor = OrangeAccent,
                                activeTrackColor = OrangeAccent,
                                inactiveTrackColor = WhiteColor.copy(alpha = 0.3f)
                            )
                        )
                        
                        // Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("-2", color = WhiteColor.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text("0", color = WhiteColor.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text("+2", color = WhiteColor.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
            }
        }
        
        
        // ========== BATTERY SAVER OVERLAY ==========
        if (showBatterySaverOverlay && isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { 
                        lastInteractionTime = System.currentTimeMillis()
                        showBatterySaverOverlay = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FlashOff,
                        contentDescription = "Battery Saver",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "โหมดประหยัดพลังงานทำงานอยู่",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "แตะหน้าจอเพื่อปลุก",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Show Recording Indicator even in Black Screen
                     Row(
                        modifier = Modifier
                            .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recordingDuration,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

    
    // ========== SETTINGS BOTTOM SHEET ==========
    if (showSettingsSheet) {
        SettingsBottomSheet(
            sheetState = settingsSheetState,
            onDismiss = { showSettingsSheet = false },
            videoQuality = cameraSettings.videoQuality,
            onVideoQualityChange = onVideoQualityChange,
            aspectRatio = cameraSettings.aspectRatio,
            onAspectRatioChange = onAspectRatioChange,
            dateWatermarkEnabled = cameraSettings.dateWatermarkEnabled,
            onDateWatermarkChange = onDateWatermarkChange,
            timeWatermarkEnabled = cameraSettings.timeWatermarkEnabled,
            onTimeWatermarkChange = { viewModel.setTimeWatermarkEnabled(it) },
            shutterSoundEnabled = cameraSettings.shutterSoundEnabled,
            onShutterSoundChange = onShutterSoundChange,
            gridLinesEnabled = cameraSettings.gridLinesEnabled,
            onGridLinesChange = onGridLinesChange,

            virtualLevelerEnabled = cameraSettings.virtualLevelerEnabled,
            onVirtualLevelerChange = { viewModel.setVirtualLevelerEnabled(it) },
            volumeShutterEnabled = cameraSettings.volumeShutterEnabled,
            onVolumeShutterChange = { viewModel.setVolumeShutterEnabled(it) },
            gpsFormat = cameraSettings.gpsFormat,
            onGpsFormatChange = { viewModel.updateGpsFormat(it) },
            flipFrontPhoto = cameraSettings.flipFrontPhoto,
            onFlipFrontPhotoChange = onFlipFrontPhotoChange,
            imageFormat = cameraSettings.imageFormat,
            onImageFormatChange = onImageFormatChange,
            compressionQuality = cameraSettings.compressionQuality,
            onCompressionQualityChange = onCompressionQualityChange,
            saveExif = cameraSettings.saveExif,
            onSaveExifChange = onSaveExifChange,
            customSavePath = cameraSettings.customSavePath,
            onCustomSavePathChange = onCustomSavePathChange,
            batterySaverMode = cameraSettings.batterySaverMode,
            onBatterySaverModeChange = { viewModel.updateBatterySaverMode(it) },
            // Custom Notes & Localization
            customNote = cameraSettings.customNote,
            onCustomNoteChange = onCustomNoteChange,
            dateFormat = cameraSettings.dateFormat,
            onDateFormatChange = onDateFormatChange,
            useThaiLocale = cameraSettings.isThaiLanguage,
            onUseThaiLocaleChange = onThaiLanguageChange,
            // Resolution Switching
            targetWidth = cameraSettings.targetWidth,
            targetHeight = cameraSettings.targetHeight,
            supportedResolutions = supportedResolutions,
            onTargetResolutionChange = onTargetResolutionChange,
            // Styling
            textShadowEnabled = overlayConfig.textShadowEnabled,
            onTextShadowChange = { viewModel.setTextShadowEnabled(it) },
            textBackgroundEnabled = overlayConfig.textBackgroundEnabled,
            onTextBackgroundChange = { viewModel.setTextBackgroundEnabled(it) },
            textColor = overlayConfig.textColor,
            onTextColorChange = { viewModel.updateTextColor(it) },
            textSize = overlayConfig.textSize,
            onTextSizeChange = { viewModel.updateTextSize(it) },
            textStyle = overlayConfig.textStyle,
            onTextStyleChange = { viewModel.updateTextStyle(it) },
            // Phase 15: Pro Typography
            textAlpha = overlayConfig.alpha,
            onTextAlphaChange = { viewModel.updateTextAlpha(it) },
            fontFamily = overlayConfig.fontFamily,
            onFontFamilyChange = { viewModel.updateFontFamily(it) },
            overlayPosition = overlayConfig.position.name,
            onOverlayPositionChange = { pos -> 
                 try {
                     val p = com.example.timestampcamera.util.OverlayPosition.valueOf(pos)
                     viewModel.updateOverlayPosition(p)
                 } catch (e: Exception) {}
            },
            // Phase 16: Rich Data Overlays
            compassEnabled = cameraSettings.compassEnabled,
            onCompassChange = { viewModel.updateCompassEnabled(it) },
            altitudeEnabled = cameraSettings.altitudeEnabled,
            onAltitudeChange = { viewModel.updateAltitudeEnabled(it) },
            speedEnabled = cameraSettings.speedEnabled,
            onSpeedChange = { viewModel.updateSpeedEnabled(it) },
            // Custom Logo
            // Custom Logo
            hasLogo = overlayConfig.logoBitmap != null,
            onLogoSelect = { logoPickerLauncher.launch(arrayOf("image/png", "image/jpeg")) },
            onLogoRemove = { viewModel.onLogoRemove() },
            // Phase 17: Professional Workflow
            projectName = cameraSettings.projectName,
            onProjectNameChange = { viewModel.updateProjectName(it) },
            inspectorName = cameraSettings.inspectorName,
            onInspectorNameChange = { viewModel.updateInspectorName(it) },
            tags = cameraSettings.tags,
                onTagsChange = { viewModel.updateTags(it) },
                tagsHistory = tagsHistory.toList(),
                
                customFields = cameraSettings.customFields,
                onCustomFieldsChange = { viewModel.updateCustomFields(it) },

                textStrokeEnabled = cameraSettings.textStrokeEnabled,
                onTextStrokeEnabledChange = { viewModel.updateTextStrokeEnabled(it) },
            textStrokeWidth = cameraSettings.textStrokeWidth,
            onTextStrokeWidthChange = { viewModel.updateTextStrokeWidth(it) },
            textStrokeColor = cameraSettings.textStrokeColor,
            onTextStrokeColorChange = { viewModel.updateTextStrokeColor(it) },
            googleFontName = cameraSettings.googleFontName,
            onGoogleFontNameChange = { viewModel.updateGoogleFontName(it) },
            templateId = cameraSettings.templateId,
            onTemplateIdChange = { viewModel.updateTemplateId(it) },
            // Confirmed Removal of Compass Tape items
            compassPosition = overlayConfig.compassPosition.name,
            onCompassPositionChange = { viewModel.updateCompassPosition(it) },
            
            customTextOrder = cameraSettings.customTextOrder,
            onCustomTextOrderChange = { viewModel.updateTextOrder(it) },
            
            // Tag Management
            availableTags = cameraSettings.availableTags,
            onAddTag = { viewModel.addAvailableTag(it) },
            onRemoveTag = { viewModel.removeAvailableTag(it) },
            onClearTags = { viewModel.clearAvailableTags() },
            onImportTags = { viewModel.importAvailableTags(it) },

            // Address & Coords Visibility
            addressEnabled = cameraSettings.isAddressEnabled,
            onAddressEnabledChange = { viewModel.updateAddressEnabled(it) },
            coordinatesEnabled = cameraSettings.isCoordinatesEnabled,
            onCoordinatesEnabledChange = { viewModel.updateCoordinatesEnabled(it) },
            
            // Cloud Path
            cloudPath = cameraSettings.cloudPath,
            onCloudPathChange = { viewModel.setCloudPath(it) },
            
            addressResolution = cameraSettings.addressResolution,
            onAddressResolutionChange = { viewModel.updateAddressResolution(it) },
            currentLocation = locationData,
            
            noteHistory = noteHistory.toList(),
            
            saveOriginalPhoto = cameraSettings.saveOriginalPhoto,
            onSaveOriginalPhotoChange = { viewModel.updateSaveOriginalPhoto(it) },
            fileNameFormat = cameraSettings.fileNameFormat,
            onFileNameFormatChange = { viewModel.updateFileNameFormat(it) },
            uploadOnlyWifi = cameraSettings.uploadOnlyWifi,
            onUploadOnlyWifiChange = { viewModel.updateUploadOnlyWifi(it) },
            uploadLowBattery = cameraSettings.uploadLowBattery,
            onUploadLowBatteryChange = { viewModel.updateUploadLowBattery(it) }
        )
    }
}





@Composable
private fun TopBar(
    isEditorMode: Boolean,
    flashMode: Int,
    evValue: String,
    onFlashClick: () -> Unit,
    onEvClick: () -> Unit,
    onMenuClick: () -> Unit,
    onImportClick: () -> Unit = {},
    onCloseEditorClick: () -> Unit = {},
    iconRotation: Float = 0f,
    isNightMode: Boolean = false
) {
    if (isEditorMode) {
        // Simple Top Bar for Editor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseEditorClick) {
                Icon(
                    imageVector = Icons.Default.Close, 
                    contentDescription = "Close", 
                    tint = WhiteColor,
                    modifier = Modifier.rotate(iconRotation)
                )
            }
            Text("Editor Mode", color = WhiteColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
             IconButton(onClick = onImportClick) {
                Icon(
                    imageVector = Icons.Default.Image, 
                    contentDescription = "Import", 
                    tint = WhiteColor,
                    modifier = Modifier.rotate(iconRotation)
                )
            }
        }
    } else {
        // Pixel Perfect Camera Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Flash
            IconButton(
                onClick = onFlashClick,
                modifier = Modifier.rotate(iconRotation)
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        0 -> Icons.Default.FlashAuto
                        1 -> Icons.Default.FlashOn
                         else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = if (flashMode == 1) OrangeAccent else WhiteColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Center: EV Value (Orange) + Night Mode Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isNightMode) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Night Mode",
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = iconRotation }
                    )
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer { rotationZ = iconRotation }
                        .clickable(onClick = onEvClick)
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = evValue.replace(" EV", ""), // Just the number
                            color = OrangeAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "EV",
                            color = OrangeAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: Menu (Hamburger)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = WhiteColor,
                    modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = iconRotation }
                )
            }
        }
    }
}
@Composable
private fun CameraPreviewPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaceholderGray)
    ) {
        // This would be replaced with actual CameraPreview
        // For now, showing a placeholder
    }
}

@Composable
private fun LevelIndicator(
    modifier: Modifier = Modifier,
    rollAngle: Float
) {
    // Determine if level (within 1 degree)
    val isLevel = abs(rollAngle) < 1f
    
    // Animated color
    val lineColor by animateColorAsState(
        targetValue = if (isLevel) Color(0xFF4CAF50) else Color.White,
        animationSpec = tween(200),
        label = "levelColor"
    )
    
    // Animated rotation (snap to 0 when level)
    val displayRotation by animateFloatAsState(
        targetValue = if (isLevel) 0f else -rollAngle,
        animationSpec = tween(100),
        label = "levelRotation"
    )

    // Background pill for better visibility
    Box(
        modifier = modifier
            .rotate(displayRotation)
            .width(240.dp) // Wider for better precision visibility
            .height(20.dp) // Touch target size (visual only)
            .background(
                color = Color.Black.copy(alpha = 0.2f), 
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // The actual level line
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Leave some padding inside the pill
                .height(if (isLevel) 4.dp else 2.dp) // Thicker when level
                .clip(CircleShape)
                .background(lineColor)
        )
        
        // Center Notch (optional, for reference)
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(8.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        )
    }
}


// MacroButton Removed

@Composable
private fun ZoomControlPill(
    selectedZoom: String,
    zoomOptions: List<com.example.timestampcamera.util.ZoomConfig>,
    onZoomSelected: (com.example.timestampcamera.util.ZoomConfig) -> Unit
) {
    // Fallback if options are empty (shouldn't happen with valid logic, but safety first)
    val actualOptions = if (zoomOptions.isEmpty()) {
        listOf(
             com.example.timestampcamera.util.ZoomConfig(1.0f, "1x"),
             com.example.timestampcamera.util.ZoomConfig(2.0f, "2x")
        )
    } else zoomOptions

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PillBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
             actualOptions.forEach { option ->
                val isSelected = selectedZoom == option.label
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) OrangeAccent else Color.Transparent)
                        .clickable { onZoomSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = option.label
                    
                    Text(
                        text = displayText,
                        color = if (isSelected) BlackBackground else WhiteColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
             }
        }
    }
}

@Composable
private fun ZoomButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Deprecated in favor of PilledZoomControl, keeping empty or redirected
}

@Composable
private fun ModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf("วิดีโอ", "รูปถ่าย")
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        // Sliding Pill Background
        // We need to know the width of a single item to animate the offset
        // For simplicity, we assume fixed width items or use a Row with weight
        
        // Strategy: Use a Row for layout, and a Box for the sliding pill
        // The sliding pill needs to know the target position. 
        // A simple way is to use a TabRow-like approach or fixed widths.
        // Let's use fixed width for items to make sliding easy without complex LayoutCoordinates for now.
        val itemWidth = 80.dp
        
        // Animate Offset
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "indicatorOffset"
        )
        
        // Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        )
        
        // Text Items
        Row {
            modes.forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onModeSelected(mode) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = animateColorAsState(
                            targetValue = if (isSelected) Color.Black else Color.White,
                            animationSpec = tween(200),
                            label = "textColor"
                        ).value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
// ModeButton is no longer used, removed.

@Composable
private fun BottomControlBar(
    isEditorMode: Boolean = false,
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    lastCapturedUri: android.net.Uri? = null,
    isCapturing: Boolean = false,
    isRecording: Boolean = false,
    iconRotation: Float = 0f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery (Left)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
             if (!isRecording && !isEditorMode) {
                 GalleryThumbnail(
                     onClick = onGalleryClick,
                     lastCapturedUri = lastCapturedUri,
                     iconRotation = iconRotation
                 )
             }
        }

        // Shutter Button (Center)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ShutterButton(
                isEditorMode = isEditorMode,
                onClick = onCaptureClick,
                isCapturing = isCapturing,
                isRecording = isRecording,
                iconRotation = iconRotation
            )
        }

        // Switch Camera (Right)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
             if (!isRecording && !isEditorMode) {
                SwitchCameraButton(
                    onClick = onSwitchCameraClick,
                    iconRotation = iconRotation
                )
             }
        }
    }
}

@Composable
private fun GalleryThumbnail(
    onClick: () -> Unit,
    lastCapturedUri: android.net.Uri? = null,
    iconRotation: Float = 0f
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PlaceholderGray)
            .border(
                width = 2.dp,
                color = WhiteColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (lastCapturedUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(lastCapturedUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Latest Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(iconRotation)
            )
        }
    }
}

@Composable
private fun ShutterButton(
    modifier: Modifier = Modifier,
    isEditorMode: Boolean = false,
    onClick: () -> Unit,
    isCapturing: Boolean = false,
    isRecording: Boolean = false,
    iconRotation: Float = 0f
) {
    val haptic = LocalHapticFeedback.current
    val view = androidx.compose.ui.platform.LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    
    // Elastic Scale Animation (Press)
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shutter_scale"
    )

    // Breathing Animation (Recording)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )
    
    // Combine scales: if recording, use breathing. If pressing, override with press scale (or combine)
    // Priority: Press > Recording
    val finalScale = if (isPressed) pressedScale else if (isRecording) breathingScale else 1f

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }

    Box(
        modifier = modifier
            .size(84.dp)
            .scale(finalScale)
            .graphicsLayer { rotationZ = iconRotation } // Add rotation to Shutter
            .border(4.dp, WhiteColor, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(
                animateColorAsState(
                    targetValue = if (isEditorMode) Color(0xFF4CAF50) // Green for Save
                    else if (isRecording) Color.Red 
                    else if (isCapturing) OrangeAccent.copy(alpha = 0.5f) 
                    else OrangeAccent,
                    animationSpec = tween(300),
                    label = "shutterColor"
                ).value
            )
            .clickable(
                enabled = !isCapturing,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    // Haptic Feedback
                    // Haptic Feedback (Pro Feel)
                    val feedbackConstant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (isEditorMode) android.view.HapticFeedbackConstants.CONFIRM 
                        else android.view.HapticFeedbackConstants.GESTURE_END // Crisp tick
                    } else {
                        android.view.HapticFeedbackConstants.LONG_PRESS
                    }
                    view.performHapticFeedback(feedbackConstant)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Square stop icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
            )
        } else if (isEditorMode) {
             Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = WhiteColor,
                modifier = Modifier.size(40.dp)
            )
        } else if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = WhiteColor,
                strokeWidth = 3.dp
            )
        }
    }
}


@Composable
private fun SwitchCameraButton(onClick: () -> Unit, iconRotation: Float = 0f) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(DarkGray)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = WhiteColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Camera",
            tint = WhiteColor,
            modifier = Modifier.size(28.dp).rotate(iconRotation)
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { /* TODO: Handle setting change */ }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = WhiteColor,
            fontSize = 16.sp
        )
        Text(
            text = value,
            color = OrangeAccent,
            fontSize = 14.sp
        )
    }
    Divider(color = WhiteColor.copy(alpha = 0.1f))
}

@Composable
private fun GridLinesOverlay(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx()
        val lineColor = WhiteColor.copy(alpha = 0.5f)
        
        // Vertical lines (divide into 3 columns)
        val thirdWidth = size.width / 3
        drawLine(
            color = lineColor,
            start = Offset(thirdWidth, 0f),
            end = Offset(thirdWidth, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = lineColor,
            start = Offset(thirdWidth * 2, 0f),
            end = Offset(thirdWidth * 2, size.height),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines (divide into 3 rows)
        val thirdHeight = size.height / 3
        drawLine(
            color = lineColor,
            start = Offset(0f, thirdHeight),
            end = Offset(size.width, thirdHeight),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, thirdHeight * 2),
            end = Offset(size.width, thirdHeight * 2),
            strokeWidth = strokeWidth
        )
    }
}
// MiniMapOverlay Removed


@Composable
private fun ExtensionBadge(
    mode: Int?, // Nullable: null/0 = Standard/Off
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f
) {
    val (text, color, iconColor) = when (mode) {
        androidx.camera.extensions.ExtensionMode.HDR -> Triple("HDR: On", Color(0xFFFF9800), Color.White)
        androidx.camera.extensions.ExtensionMode.NIGHT -> Triple("Night: On", Color(0xFF9C27B0), Color.White)
        androidx.camera.extensions.ExtensionMode.AUTO -> Triple("Auto Enhancer", Color(0xFF4CAF50), Color.White)


        null, 0 -> Triple("Standard", Color.Gray, Color.LightGray) // Explicit Off
        else -> return // Should not happen for unused modes
    }

    Surface(
        modifier = modifier
            .padding(top = 16.dp)
            .rotate(iconRotation)
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = color.copy(alpha = 0.3f), // Slightly more opaque
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (mode == null || mode == 0) Color.LightGray else iconColor, CircleShape)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Maps a visual OverlayPosition to a physical Compose Alignment based on device rotation.
 * This ensures "Bottom Right" always stays at the visual bottom-right corner of the viewfinder.
 */
@androidx.compose.runtime.Composable
private fun getRotationAwareAlignment(
    position: com.example.timestampcamera.util.OverlayPosition,
    rotation: Float
): Alignment {
    val rot = ((rotation.toInt() % 360) + 360) % 360
    
    // Identity mapping for Portrait (0°)
    if (rot == 0) {
        return when (position) {
            com.example.timestampcamera.util.OverlayPosition.TOP_LEFT -> Alignment.TopStart
            com.example.timestampcamera.util.OverlayPosition.TOP_CENTER -> Alignment.TopCenter
            com.example.timestampcamera.util.OverlayPosition.TOP_RIGHT -> Alignment.TopEnd
            com.example.timestampcamera.util.OverlayPosition.CENTER_LEFT -> Alignment.CenterStart
            com.example.timestampcamera.util.OverlayPosition.CENTER -> Alignment.Center
            com.example.timestampcamera.util.OverlayPosition.CENTER_RIGHT -> Alignment.CenterEnd
            com.example.timestampcamera.util.OverlayPosition.BOTTOM_LEFT -> Alignment.BottomStart
            com.example.timestampcamera.util.OverlayPosition.BOTTOM_CENTER -> Alignment.BottomCenter
            com.example.timestampcamera.util.OverlayPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
    }

    // For rotations, we map the visual corner to the physical corner
    return when (rot) {
        90 -> { // Landscape Left (Top of phone points LEFT)
            // Visually: 
            // - Top-Right (Scene) is now Physical Bottom-Right
            // - Bottom-Right (Scene) is now Physical Bottom-Left
            // - Bottom-Left (Scene) is now Physical Top-Left
            // - Top-Left (Scene) is now Physical Top-Right
            when (position) {
                com.example.timestampcamera.util.OverlayPosition.TOP_LEFT -> Alignment.TopEnd
                com.example.timestampcamera.util.OverlayPosition.TOP_CENTER -> Alignment.CenterEnd
                com.example.timestampcamera.util.OverlayPosition.TOP_RIGHT -> Alignment.BottomEnd
                com.example.timestampcamera.util.OverlayPosition.CENTER_LEFT -> Alignment.TopCenter
                com.example.timestampcamera.util.OverlayPosition.CENTER -> Alignment.Center
                com.example.timestampcamera.util.OverlayPosition.CENTER_RIGHT -> Alignment.BottomCenter
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_LEFT -> Alignment.TopStart
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_CENTER -> Alignment.CenterStart
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_RIGHT -> Alignment.BottomStart
            }
        }
        180 -> { // Reverse Portrait (Upside Down)
            when (position) {
                com.example.timestampcamera.util.OverlayPosition.TOP_LEFT -> Alignment.BottomEnd
                com.example.timestampcamera.util.OverlayPosition.TOP_CENTER -> Alignment.BottomCenter
                com.example.timestampcamera.util.OverlayPosition.TOP_RIGHT -> Alignment.BottomStart
                com.example.timestampcamera.util.OverlayPosition.CENTER_LEFT -> Alignment.CenterEnd
                com.example.timestampcamera.util.OverlayPosition.CENTER -> Alignment.Center
                com.example.timestampcamera.util.OverlayPosition.CENTER_RIGHT -> Alignment.CenterStart
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_LEFT -> Alignment.TopEnd
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_CENTER -> Alignment.TopCenter
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_RIGHT -> Alignment.TopStart
            }
        }
        270, -90 -> { // Landscape Right (Top of phone points RIGHT)
            // Visually:
            // - Top-Right (Scene) is now Physical Top-Left
            // - Bottom-Right (Scene) is now Physical Top-Right
            // - Bottom-Left (Scene) is now Physical Bottom-Right
            // - Top-Left (Scene) is now Physical Bottom-Left
            when (position) {
                com.example.timestampcamera.util.OverlayPosition.TOP_LEFT -> Alignment.BottomStart
                com.example.timestampcamera.util.OverlayPosition.TOP_CENTER -> Alignment.CenterStart
                com.example.timestampcamera.util.OverlayPosition.TOP_RIGHT -> Alignment.TopStart
                com.example.timestampcamera.util.OverlayPosition.CENTER_LEFT -> Alignment.BottomCenter
                com.example.timestampcamera.util.OverlayPosition.CENTER -> Alignment.Center
                com.example.timestampcamera.util.OverlayPosition.CENTER_RIGHT -> Alignment.TopCenter
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_LEFT -> Alignment.BottomEnd
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_CENTER -> Alignment.CenterEnd
                com.example.timestampcamera.util.OverlayPosition.BOTTOM_RIGHT -> Alignment.TopEnd
            }
        }
        else -> Alignment.BottomEnd // Fallback
    }
}
