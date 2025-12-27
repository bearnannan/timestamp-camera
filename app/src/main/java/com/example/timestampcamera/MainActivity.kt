package com.example.timestampcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.CameraController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    // Flash Mode State
    val flashMode by viewModel.flashMode.collectAsState()
    
    // Settings States from ViewModel
    val isCapturing by viewModel.isCapturing.collectAsState()
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
    LaunchedEffect(Unit) {
        val effect = VideoWatermarkUtils.createOverlayEffect {
            viewModel.overlayConfig.value
        }
        cameraController.setEffects(setOf(effect))
    }

    // Sync Photo Resolution to Controller
    LaunchedEffect(cameraSettings.targetWidth, cameraSettings.targetHeight) {
        if (cameraSettings.targetWidth > 0 && cameraSettings.targetHeight > 0) {
            cameraController.imageCaptureTargetSize = CameraController.OutputSize(
                Size(cameraSettings.targetWidth, cameraSettings.targetHeight)
            )
        } else {
            cameraController.imageCaptureTargetSize = null
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

    if (currentScreen == "camera") {
        CameraScreen(
            modifier = Modifier.fillMaxSize(),
            cameraController = cameraController,
            flashMode = flashMode,
            onFlashClick = { 
                viewModel.toggleFlash()
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
                // Update CameraX aspect ratio
                val targetRatio = when (ratio) {
                    "4:3" -> androidx.camera.core.AspectRatio.RATIO_4_3
                    "16:9" -> androidx.camera.core.AspectRatio.RATIO_16_9
                    else -> androidx.camera.core.AspectRatio.RATIO_4_3 // 1:1 handled by cropping in processing
                }
                cameraController.imageCaptureTargetSize = androidx.camera.view.CameraController.OutputSize(targetRatio)
            },
            onDateWatermarkChange = { viewModel.setDateWatermarkEnabled(it) },
            onShutterSoundChange = { viewModel.setShutterSoundEnabled(it) },
            onGridLinesChange = { viewModel.setGridLinesEnabled(it) },
            onMapOverlayChange = { viewModel.setMapOverlayEnabled(it) },
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
                cameraController.cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            },
            // Resolution Switching
            supportedResolutions = viewModel.supportedResolutions.collectAsState().value,
            onTargetResolutionChange = { w, h -> viewModel.updateTargetResolution(w, h) },
            onGalleryClick = {
                // Switch to Gallery View
                viewModel.loadRecentMedia()
                currentScreen = "gallery"
            },
            // Macro & Sensor
            isCapturing = isCapturing,
            lastCapturedUri = viewModel.lastCapturedUri.collectAsState().value,
            sensorViewModel = sensorViewModel,
            onCaptureClick = {
                if (selectedMode == "รูปถ่าย") {
                    if (isCapturing) return@CameraScreen
                    
                    viewModel.playShutterSound()
                    cameraController.takePicture(
                        mainExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                
                                viewModel.processAndSaveImage(bitmap, isFrontCamera) { uri ->
                                    // Optionally refresh gallery list immediately if we want
                                }
                            }
    
                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, "Capture Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    // Video Mode
                    toggleVideoRecording()
                }
            },
            isRecording = isRecording,
            recordingDuration = recordingDuration
        )
    } else {
        // GALLERY SCREEN
        val recentMedia by viewModel.recentMedia.collectAsState()
        
        GalleryScreen(
            mediaList = recentMedia,
            onBack = { currentScreen = "camera" },
            onDelete = { item -> viewModel.deleteMedia(item) }
        )
    }
}
