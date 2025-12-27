package com.example.timestampcamera.ui

import androidx.compose.ui.viewinterop.AndroidView

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
import androidx.compose.runtime.*
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

// Zoom level presets
private val ZOOM_LEVELS = listOf(0.6f, 1f, 2f, 3f, 6f, 10f)
private val ZOOM_LEVEL_LABELS = listOf("0.6", "1", "2", "3", "6", "10")
private const val ZOOM_MATCH_THRESHOLD = 0.15f

/**
 * Find the nearest zoom preset label for a given zoom ratio.
 * Returns null if the zoom ratio is not close enough to any preset.
 */
private fun findNearestZoomPreset(zoomRatio: Float): String? {
    ZOOM_LEVELS.forEachIndexed { index, preset ->
        // Use relative threshold for larger zoom values
        val threshold = if (preset >= 6f) preset * 0.1f else ZOOM_MATCH_THRESHOLD
        if (abs(zoomRatio - preset) < threshold) {
            return ZOOM_LEVEL_LABELS[index]
        }
    }
    return null
}

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
    onMapOverlayChange: (Boolean) -> Unit = {},
    // Sensor & Macro from ViewModel
    isCapturing: Boolean = false,
    isRecording: Boolean = false,
    recordingDuration: String = "00:00",
    lastCapturedUri: android.net.Uri? = null,
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
    // Resolution Switching
    supportedResolutions: List<android.util.Size> = emptyList(),
    onTargetResolutionChange: (Int, Int) -> Unit = { _, _ -> },
    sensorViewModel: SensorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    // Logo Callback
    onLogoClick: (android.net.Uri?) -> Unit = {}
) {
    // Observe OverlayConfig for Logo Status
    // Observe OverlayConfig for Logo Status
    val viewModel: com.example.timestampcamera.ui.CameraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    val hasLogo = overlayConfig.logoBitmap != null
    var lastMapUpdate by remember { mutableLongStateOf(0L) }
    
    // Timer State
    val context = androidx.compose.ui.platform.LocalContext.current
    
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

    var selectedMode by remember { mutableStateOf("รูปถ่าย") }
    var selectedZoom by remember { mutableStateOf("1") }
    
    // ========== EV SLIDER & SETTINGS STATE ==========
    var showEvSlider by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var exposureIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // Observed orientation from SensorViewModel
    val rollAngle by sensorViewModel.horizonRotation.collectAsState()
    val targetRotation by sensorViewModel.uiRotation.collectAsState()
    val compassHeading by sensorViewModel.compassHeading.collectAsState()

    LaunchedEffect(compassHeading) {
         viewModel.updateCompassHeading(compassHeading)
    }

    val iconRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )
    
    // Bottom Sheet State
    val settingsSheetState = rememberModalBottomSheetState()
    
    // ========== TWO-WAY ZOOM SYNC ==========
    // Observe camera's actual zoom state for pinch-to-zoom sync
    val zoomState = cameraController?.zoomState?.observeAsState()
    
    // Update UI when camera zoom changes (from pinch gesture or slider)
    LaunchedEffect(zoomState?.value?.zoomRatio) {
        val currentZoom = zoomState?.value?.zoomRatio ?: 1f
        
        // Find nearest preset zoom level using threshold matching
        val nearestPreset = findNearestZoomPreset(currentZoom)
        
        // Update selected zoom if found a match
        if (nearestPreset != null) {
            selectedZoom = nearestPreset
        } else {
            // If zoom is between presets, could show exact value or keep last selection
            // For now, we'll keep the last selection to avoid jumpy UI
        }
    }
    
    // ========== REAL EV CALCULATION WITH REAL-TIME SYNC ==========
    // evText is derived from exposureIndex state for immediate UI updates
    // Also polls camera for external exposure changes
    
    // Get exposure step from camera (typically 1/6 or 1/3 EV)
    var exposureStep by remember { mutableStateOf(1f / 6f) } // Default step
    
    // Initialize exposure step from camera
    LaunchedEffect(cameraController) {
        cameraController?.cameraInfo?.exposureState?.let { state ->
            exposureStep = state.exposureCompensationStep.toFloat()
            exposureIndex = state.exposureCompensationIndex
        }
    }
    
    // Calculate EV text from current exposureIndex (reactive)
    val evText = remember(exposureIndex, exposureStep) {
        val evValue = exposureIndex * exposureStep
        String.format("%+.1f EV", evValue)
    }
    
    // Also poll camera for external exposure changes (e.g., from tap-to-focus)
    LaunchedEffect(cameraController) {
        val controller = cameraController
        if (controller != null) {
            while (true) {
                try {
                    val state = controller.cameraInfo?.exposureState
                    if (state != null) {
                        val currentIndex = state.exposureCompensationIndex
                        if (currentIndex != exposureIndex) {
                            exposureIndex = currentIndex
                        }
                        exposureStep = state.exposureCompensationStep.toFloat()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(200)
            }
        }
    }
    
    // ========== FLASH EFFECT STATE ==========
    val flashAlpha = remember { Animatable(0f) }
    
    // Trigger flash when capturing starts (visual feedback)
    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            flashAlpha.snapTo(0.8f) // Instant white
            flashAlpha.animateTo(0f, animationSpec = tween(300)) // Fade out
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
                    iconRotation = iconRotation
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
                    CameraPreview(
                        controller = cameraController,
                        modifier = Modifier.fillMaxSize(),
                        flashMode = flashMode
                    )
                } else {
                    CameraPreviewPlaceholder()
                }
                
                // Horizon Level Indicator
                LevelIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    rollAngle = rollAngle
                )
                
                // Grid Lines Overlay
                if (cameraSettings.gridLinesEnabled) {
                    GridLinesOverlay(modifier = Modifier.fillMaxSize())
                }
                
                // Macro Button Removed
                
                // Mini-Map Overlay Removed

            }

            // *** Floating Controls (Zoom Pill + Mode Selector) ***
            // Overlaying the bottom of the Viewfinder
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp), // Space from the very bottom of the PREVIEW area
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp) // Tight spacing between Pill and Mode
            ) {
                // 1. Zoom Control Pill
                if (!isEditorMode) {
                    ZoomControlPill(
                        selectedZoom = selectedZoom,
                        onZoomSelected = { zoom ->
                            selectedZoom = zoom
                            val zoomValue = when (zoom) {
                                "0.6" -> 0.6f
                                "1" -> 1f
                                "2" -> 2f
                                "3" -> 3f
                                "6" -> 6f
                                "10" -> 10f
                                else -> 1f
                            }
                            onZoomChange(zoomValue)
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
                    } else if (!isTimerRunning) {
                        viewModel.startTimer {
                            onCaptureClick()
                        }
                    }
                },
                onSwitchCameraClick = onSwitchCameraClick,
                onGalleryClick = onGalleryClick,
                iconRotation = iconRotation
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
                            text = evText,
                            color = OrangeAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Exposure Slider (-12 to +12 steps)
                        Slider(
                            value = exposureIndex.toFloat(),
                            onValueChange = { newValue ->
                                exposureIndex = newValue.toInt()
                                cameraController?.cameraControl?.setExposureCompensationIndex(exposureIndex)
                            },
                            valueRange = -12f..12f,
                            steps = 23,
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
            shutterSoundEnabled = cameraSettings.shutterSoundEnabled,
            onShutterSoundChange = onShutterSoundChange,
            gridLinesEnabled = cameraSettings.gridLinesEnabled,
            onGridLinesChange = onGridLinesChange,
            // New Persistent Settings
            mapOverlayEnabled = cameraSettings.mapOverlayEnabled,
            onMapOverlayChange = onMapOverlayChange,
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

            // Phase 18: Advanced Typography
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
            compassTapeEnabled = cameraSettings.compassTapeEnabled,
            onCompassTapeChange = { viewModel.updateCompassTapeEnabled(it) }
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
    iconRotation: Float = 0f
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
                Icon(Icons.Default.Close, "Close", tint = WhiteColor)
            }
            Text("Editor Mode", color = WhiteColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
             IconButton(onClick = onImportClick) {
                Icon(Icons.Default.Image, "Import", tint = WhiteColor)
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

            // Center: EV Value (Orange)
            Box(
                modifier = Modifier
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

            // Right: Menu (Hamburger)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = WhiteColor,
                    modifier = Modifier.size(28.dp)
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

    // Animated height (thicker when level)
    val lineHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isLevel) 3.dp else 1.5.dp,
        animationSpec = tween(200),
        label = "lineHeight"
    )
    
    // Level line UI - Wider for production feel
    Box(
        modifier = modifier
            .rotate(displayRotation)
            .width(150.dp)
            .height(lineHeight)
            .clip(RoundedCornerShape(1.dp))
            .background(lineColor.copy(alpha = 0.8f))
    )
}

// MacroButton Removed

@Composable
private fun ZoomControlPill(
    selectedZoom: String,
    onZoomSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PillBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp) // Slightly more horizontal padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between numbers
        ) {
             // Items: "0.6", "1", "2", "3", "6", "10"
             val items = listOf("0.6", "1", "2", "3", "6", "10")
             
             items.forEach { item ->
                val isSelected = selectedZoom == item
                
                Box(
                    modifier = Modifier
                        .size(32.dp) // Compact size for the circle
                        .clip(CircleShape)
                        .background(if (isSelected) OrangeAccent else Color.Transparent)
                        .clickable { onZoomSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = if (item == "0.6") ".6" else item
                    // Add 'x' only if selected
                    val textContent = if (isSelected) "${displayText}x" else displayText 
                    
                    Text(
                        text = textContent,
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
                isRecording = isRecording
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
    isEditorMode: Boolean = false,
    onClick: () -> Unit,
    isCapturing: Boolean = false,
    isRecording: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
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
        modifier = Modifier
            .size(84.dp)
            .scale(finalScale)
            .border(4.dp, WhiteColor, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(
                if (isEditorMode) Color(0xFF4CAF50) // Green for Save
                else if (isRecording) Color.Red 
                else if (isCapturing) OrangeAccent.copy(alpha = 0.5f) 
                else OrangeAccent
            )
            .clickable(
                enabled = !isCapturing,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    // Haptic Feedback
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            start = androidx.compose.ui.geometry.Offset(thirdWidth, 0f),
            end = androidx.compose.ui.geometry.Offset(thirdWidth, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(thirdWidth * 2, 0f),
            end = androidx.compose.ui.geometry.Offset(thirdWidth * 2, size.height),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines (divide into 3 rows)
        val thirdHeight = size.height / 3
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(0f, thirdHeight),
            end = androidx.compose.ui.geometry.Offset(size.width, thirdHeight),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(0f, thirdHeight * 2),
            end = androidx.compose.ui.geometry.Offset(size.width, thirdHeight * 2),
            strokeWidth = strokeWidth
        )
    }
}
// MiniMapOverlay Removed
