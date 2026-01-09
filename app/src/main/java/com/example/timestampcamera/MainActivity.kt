package com.example.timestampcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController

import androidx.camera.view.CameraController
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timestampcamera.ui.CameraScreen
import com.example.timestampcamera.ui.CameraViewModel
import com.example.timestampcamera.ui.GalleryScreen
import com.example.timestampcamera.ui.SensorViewModel
import com.example.timestampcamera.ui.PermissionScreen
import com.example.timestampcamera.util.VideoWatermarkUtils
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.core.ImageCapture
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import com.example.timestampcamera.ui.theme.TimestampCameraTheme
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.app.ActivityCompat
import android.util.Log
import android.util.Size


class MainActivity : ComponentActivity() {
    // ========== VIDEO RECORDING LOGIC ==========
    private var activeRecording: androidx.camera.video.Recording? = null
    private var showRationaleDialog by mutableStateOf(false)
    private var showSettingsDialog by mutableStateOf(false)
    
    // Add pending action to track what to do after permission is granted
    private var pendingAudioPermissionAction: (() -> Unit)? = null

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Audio permission granted. You can now record video.", Toast.LENGTH_SHORT).show()
            // Execute pending action if any
            pendingAudioPermissionAction?.invoke()
            pendingAudioPermissionAction = null
        } else {
            pendingAudioPermissionAction = null // Clear pending action on denial
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                showSettingsDialog = true
            } else {
                Toast.makeText(this, "Audio permission is required for video recording", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSMDroid Removed
        
        // Set status bar to black
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        
        setContent {
            TimestampCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CameraApp(
                        showRationaleDialog = showRationaleDialog,
                        showSettingsDialog = showSettingsDialog,
                        onDismissRationale = { 
                            showRationaleDialog = false 
                            pendingAudioPermissionAction = null // Clear on dismiss
                        },
                        onDismissSettings = { showSettingsDialog = false },
                        onRequestAudioPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
            }
        }
    }

    fun checkAndRequestAudioPermission(onPermissionGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) -> {
                pendingAudioPermissionAction = onPermissionGranted
                showRationaleDialog = true
            }
            else -> {
                pendingAudioPermissionAction = onPermissionGranted
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    /**
     * Helper to ensure VideoCapture is bound and ready.
     * With LifecycleCameraController, we ensure the use case is enabled.
     */
    fun bindVideoCapture(cameraController: LifecycleCameraController) {
        cameraController.setEnabledUseCases(
            LifecycleCameraController.IMAGE_CAPTURE or
            LifecycleCameraController.VIDEO_CAPTURE
        )
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Intercept Volume Keys for Shutter
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val viewModel: CameraViewModel = androidx.lifecycle.ViewModelProvider(this)[CameraViewModel::class.java]
                val settings = viewModel.cameraSettingsFlow.value
                
                if (settings.volumeShutterEnabled) {
                    viewModel.triggerShutter()
                    true // Consume event
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
fun CameraApp(
    showRationaleDialog: Boolean,
    showSettingsDialog: Boolean,
    onDismissRationale: () -> Unit,
    onDismissSettings: () -> Unit,
    onRequestAudioPermission: () -> Unit
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    
    if (!permissionsGranted) {
        // Show Permission Screen first
        PermissionScreen(
            onAllPermissionsGranted = {
                permissionsGranted = true
            }
        )
    } else {
        // Show Camera Screen after permissions granted
        CameraContent(
            showRationaleDialog = showRationaleDialog,
            showSettingsDialog = showSettingsDialog,
            onDismissRationale = onDismissRationale,
            onDismissSettings = onDismissSettings,
            onRequestAudioPermission = onRequestAudioPermission
        )
    }
}

@Composable
fun CameraContent(
    showRationaleDialog: Boolean,
    showSettingsDialog: Boolean,
    onDismissRationale: () -> Unit,
    onDismissSettings: () -> Unit,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val viewModel: CameraViewModel = viewModel()
    val sensorViewModel: SensorViewModel = viewModel()
    
    // Camera Controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or
                LifecycleCameraController.VIDEO_CAPTURE
            )
            // Optimize for Quality
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            
            // 1. Resolution Strategy: Highest Available (Default 4:3)
            imageCaptureResolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                .setAspectRatioStrategy(androidx.camera.core.resolutionselector.AspectRatioStrategy(
                    androidx.camera.core.AspectRatio.RATIO_4_3,
                    androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
                ))
                .setResolutionStrategy(androidx.camera.core.resolutionselector.ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Ultra HDR Configuration (Android 14+)
                 try {
                     // Enable Ultra HDR (JPEG with Gainmap)
                     // Using property access syntax to avoid "Unresolved reference" for setter
                     // imageCaptureFormat = ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
                 } catch (e: Exception) {
                     Log.e("MainActivity", "Failed to set Ultra HDR", e)
                 }
        }
    }
    
    // EXTENSIONS: Initialize and Bind
    val currentCameraSelector by viewModel.cameraSelector.collectAsState()
    val activeExtensionMode by viewModel.activeExtensionMode.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    
    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                viewModel.initializeExtensions(context, provider)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to get CameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // Sync Selector & Use Cases: Update controller when ViewModel's selector changes
    // CRITICAL FIX: Disable VideoCapture when Extensions are active to prevent crashes (Bokeh/HDR do not support Video)
    LaunchedEffect(currentCameraSelector, activeExtensionMode) {
        if (cameraController.cameraSelector != currentCameraSelector) {
            cameraController.cameraSelector = currentCameraSelector
        }
        
        val isExtensionActive = activeExtensionMode != null && activeExtensionMode != 0
        if (isExtensionActive) {
            // Extensions active -> Image Capture ONLY
            cameraController.setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        } else {
            // Standard Mode -> Image + Video
            cameraController.setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.VIDEO_CAPTURE
            )
        }
    }

    // Camera2Interop Tuning (Pro Mode)
    // Goal: Reduce Noise & Fix Darkness using specific CaptureRequest keys
    LaunchedEffect(cameraController) {
        cameraController.cameraControl?.let { cameraControl ->
            try {
                val camera2Control = androidx.camera.camera2.interop.Camera2CameraControl.from(cameraControl)
                
                val captureRequestOptions = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
                    // 1. Reduce Noise: Lower FPS range allows longer exposure per frame
                    .setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        android.util.Range(15, 30) // Allow dropping to 15fps in dark
                    )
                    // 2. High Quality Noise Reduction
                    .setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.NOISE_REDUCTION_MODE,
                        android.hardware.camera2.CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    )
                    .build()
        
                camera2Control.addCaptureRequestOptions(captureRequestOptions)
            } catch (e: Exception) {
                // Ignore if failed to apply
                Log.e("CameraTuning", "Failed to apply Pro Tuning", e)
            }
        }
    }
    
    // Flash Mode State
    val flashMode by viewModel.flashMode.collectAsState()
    
    // Settings States from ViewModel

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    
    val cameraSettings by viewModel.cameraSettingsFlow.collectAsState()

    var selectedMode by remember { mutableStateOf("รูปถ่าย") }
    val mainExecutor = ContextCompat.getMainExecutor(context)
    
    // NAVIGATION STATE
    // "camera" or "gallery"
    var currentScreen by remember { mutableStateOf("camera") }
    
    // Lens Facing State
    var isFrontCamera by remember { mutableStateOf(false) }

    // Document Tree Picker for Custom Save Path
    val documentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist permission (optional but recommended for long term access)
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            // Save path string (Uri string)
            viewModel.updateCustomSavePath(uri.toString())
        }
    }

    // Sync Supported Resolutions
    LaunchedEffect(isFrontCamera) {
        val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        viewModel.loadSupportedResolutions(selector)
    }

    // ========== VIDEO WATERMARKING ==========
    // Moved to CameraScreen logic to bind only in Video Mode


    // Sync Resolution & Aspect Ratio (Unified to prevent conflicts)
    LaunchedEffect(cameraSettings.aspectRatio, cameraSettings.targetWidth, cameraSettings.targetHeight) {
        // Option A: Manual Fixed Resolution (Target Size)
        if (cameraSettings.targetWidth > 0 && cameraSettings.targetHeight > 0) {
            cameraController.imageCaptureTargetSize = androidx.camera.view.CameraController.OutputSize(
                Size(cameraSettings.targetWidth, cameraSettings.targetHeight)
            )
        } 
        // Option B: Aspect Ratio + Max Quality (Default)
        else {
            val ratio = cameraSettings.aspectRatio
            val targetAspectRatio = when (ratio) {
                "4:3" -> androidx.camera.core.AspectRatio.RATIO_4_3
                "16:9" -> androidx.camera.core.AspectRatio.RATIO_16_9
                else -> androidx.camera.core.AspectRatio.RATIO_4_3
            }

            // Force Highest Available Resolution for the selected Aspect Ratio
            cameraController.imageCaptureResolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    androidx.camera.core.resolutionselector.AspectRatioStrategy(
                        targetAspectRatio,
                        androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .setResolutionStrategy(androidx.camera.core.resolutionselector.ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()
        }
    }

    // ========== VIDEO QUALITY SELECTOR ==========
    LaunchedEffect(cameraSettings.videoQuality) {
        val quality = when (cameraSettings.videoQuality) {
            "4K 2160p" -> androidx.camera.video.Quality.UHD
            "FHD 1080p" -> androidx.camera.video.Quality.FHD
            "HD 720p" -> androidx.camera.video.Quality.HD
            else -> androidx.camera.video.Quality.FHD // Default
        }
        cameraController.videoCaptureQualitySelector = androidx.camera.video.QualitySelector.from(quality)
    }

    // ========== FLASH & TORCH INTEGRATION ==========
    LaunchedEffect(flashMode) {
        when (flashMode) {
            0 -> { // AUTO
                cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_AUTO
                cameraController.enableTorch(false)
            }
            1 -> { // ON
                cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_ON
                cameraController.enableTorch(false)
            }
            2 -> { // OFF
                cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
                cameraController.enableTorch(false)
            }
            3 -> { // TORCH
                cameraController.enableTorch(true)
            }
        }
    }

    // ========== VIDEO RECORDING LOGIC ==========
    var activeRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }
    
    fun toggleVideoRecording() {
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            viewModel.setRecording(false)
            return
        }

        // Final check before recording
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.checkAndRequestAudioPermission {
                activity.bindVideoCapture(cameraController) // Ensure use cases are enabled
                toggleVideoRecording() // Recursive call once granted
            }
            return
        }

        val name = "VIDEO_${System.currentTimeMillis()}.mp4"
        val outputOptions = androidx.camera.video.MediaStoreOutputOptions
            .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/TimestampCamera")
                }
            })
            .build()

        try {
            activeRecording = cameraController.startRecording(
                outputOptions,
                AudioConfig.create(true),
                mainExecutor
            ) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        viewModel.setRecording(true)
                    }
                    is VideoRecordEvent.Finalize -> {
                        viewModel.setRecording(false)
                        if (!event.hasError()) {
                            Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                        } else {
                            // If error is related to permission, it might trigger here too
                            Toast.makeText(context, "Video recording failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is VideoRecordEvent.Status -> {
                        val durationNanos = event.recordingStats.recordedDurationNanos
                        viewModel.updateRecordingDuration(durationNanos / 1_000_000_000L)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException when starting recording", e)
            Toast.makeText(context, "Permission error: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting recording", e)
            Toast.makeText(context, "Error starting recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== PERMISSION DIALOGS ==========
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = { Text("ต้องการสิทธิ์การใช้ไมโครโฟน") },
            text = { Text("เพื่อบันทึกเสียงในวิดีโอ แอพจำเป็นต้องขอสิทธิ์การเข้าถึงไมโครโฟนของคุณ") },
            confirmButton = {
                TextButton(onClick = {
                    onDismissRationale()
                    onRequestAudioPermission()
                }) {
                    Text("อนุญาต")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRationale) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = onDismissSettings,
            title = { Text("สิทธิ์การใช้ไมโครโฟนถูกปฏิเสธ") },
            text = { Text("คุณได้ปฏิเสธสิทธิ์การใช้ไมโครโฟนอย่างถาวร กรุณาไปที่ตั้่งค่าเพื่ออนุญาตสิทธิ์นี้เพื่อให้สามารถบันทึกวิดีโอพร้อมเสียงได้") },
            confirmButton = {
                TextButton(onClick = {
                    onDismissSettings()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("ไปที่ตั้งค่า")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSettings) {
                    Text("ยกเลิก")
                }
            }
        )
    }



    // ANIMATED TRANSITION
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "gallery") {
                // Camera -> Gallery: Slide Left
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                // Gallery -> Camera: Slide Right
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "ScreenNav"
    ) { targetScreen ->
        if (targetScreen == "camera") {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                cameraController = cameraController,
                flashMode = flashMode,
                activeExtensionMode = activeExtensionMode, // [NEW] Pass Extension Mode
                isCapturing = isCapturing, // [NEW] Strict Capture Lock
                onExtensionToggle = { viewModel.toggleExtensionMode() }, // [NEW] Toggle Action
                onFlashClick = { 
                    viewModel.toggleFlash(isFrontCamera)
                },
                onMenuClick = { 
                    // Settings handled in CameraScreen
                },
    
                onZoomChange = { zoom ->
                    cameraController.setZoomRatio(zoom)
                },
                onModeChange = { mode ->
                    selectedMode = mode
                    // TODO: Handle mode switch (e.g. stop photo preview if necessary)
                },
                cameraSettings = cameraSettings,
                onVideoQualityChange = { viewModel.setVideoQuality(it) },
                onAspectRatioChange = { ratio ->
                    viewModel.setAspectRatio(ratio)
                },
                onDateWatermarkChange = { viewModel.setDateWatermarkEnabled(it) },
                onShutterSoundChange = { viewModel.setShutterSoundEnabled(it) },
                onGridLinesChange = { viewModel.setGridLinesEnabled(it) },
                onFlipFrontPhotoChange = { viewModel.updateFlipFrontPhoto(it) },
                onImageFormatChange = { viewModel.updateImageFormat(it) },
                onCompressionQualityChange = { viewModel.updateCompressionQuality(it) },
                onSaveExifChange = { viewModel.updateSaveExif(it) },
    
                onCustomSavePathChange = {
                    // Open Document Tree Picker
                    // valid path logic is handled in ViewModel/Repo, here we trigger picker
                    documentTreeLauncher.launch(null)
                },
                onCustomNoteChange = { viewModel.setCustomNote(it) },
                onDateFormatChange = { viewModel.setDateFormat(it) },
                onThaiLanguageChange = { viewModel.setThaiLanguage(it) },
                onSwitchCameraClick = {
                    isFrontCamera = !isFrontCamera
                    // Delegate to ViewModel to check Extensions for new facing
                    viewModel.refreshCameraSelector(isFront = isFrontCamera)
                },
                // Resolution Switching
                supportedResolutions = viewModel.supportedResolutions.collectAsState().value,
                onTargetResolutionChange = { w, h -> viewModel.updateTargetResolution(w, h) },
                onGalleryClick = {
                    // Switch to Gallery View
                    // GalleryViewModel handles loading its own data now
                    currentScreen = "gallery"
                },
                // Macro & Sensor
    
                lastCapturedUri = viewModel.lastCapturedUri.collectAsState().value,
                sensorViewModel = sensorViewModel,
                onCaptureClick = {
                    // Only for Photo Mode (called after timer)
                    if (selectedMode == "รูปถ่าย") {
                        if (isCapturing) return@CameraScreen
                        
                        viewModel.playShutterSound()
                        cameraController.takePicture(
                            mainExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmap()
                                    // Extract EXIF before closing
                                    val exifAttributes = com.example.timestampcamera.util.ExifUtils.extractExifAttributes(image)
                                    val imageRotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    
                                    // CRITICAL: image.toBitmap() in CameraX 1.4+ ALREADY rotates the bitmap to be 
                                    // upright relative to the current display orientation.
                                    // Our sensorViewModel.uiRotation provides the device's physical orientation.
                                    // We only need to apply additional rotation if we want to force landscape/portrait 
                                    // aspect ratios in the final file regardless of how it was captured.
                                    
                                    val vmRotation = viewModel.uiRotation.value
                                    
                                    Log.d("CameraCapture", "Image rotationDegrees: $imageRotation, VM UI rotation: $vmRotation")
                                    
                                    // For now, we trust toBitmap()'s upright result, and use sensorRotation 
                                    // ONLY to compensate for what toBitmap might have already handled or missed.
                                    // Usually, captureRotation = 0 is correct if toBitmap() did its job.
                                    val captureRotation = 0f 
                                    
                                    viewModel.processAndSaveImage(bitmap, isFrontCamera, captureRotation, exifAttributes) { uri ->
                                        // Optionally refresh gallery list immediately if we want
                                    }
                                }
        
                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Capture Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                onStartRecording = { toggleVideoRecording() },
                onStopRecording = { toggleVideoRecording() },
                isRecording = isRecording,
                recordingDuration = recordingDuration,
                // Exposure Params
                exposureIndex = viewModel.exposureIndex.collectAsState().value,
                exposureRange = viewModel.exposureRange.collectAsState().value,
                onExposureChange = { viewModel.updateExposureIndex(it) },
                shutterEvent = viewModel.shutterEvent // Pass Flow
            )


        } else {
            // GALLERY SCREEN (Powered by GalleryViewModel)
            GalleryScreen(
                onBack = { currentScreen = "camera" }
            )
        }
    }
}

