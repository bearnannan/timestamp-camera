package com.example.timestampcamera.ui

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

import androidx.camera.core.ImageCapture

import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder

import androidx.camera.core.AspectRatio
import androidx.camera.view.CameraController

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
    flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    isFocusLocked: Boolean = false,
    videoQuality: String = "FHD",
    photoAspectRatio: Int = 0 // 0 = 4:3, 1 = 16:9
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Flash Mode Effect
    LaunchedEffect(flashMode) {
        if (flashMode == 3) {
             controller.enableTorch(true)
        } else {
             controller.enableTorch(false)
             controller.imageCaptureFlashMode = flashMode
        }
    }

    // Focus Lock Effect
    LaunchedEffect(isFocusLocked) {
        controller.isTapToFocusEnabled = !isFocusLocked
    }

    // Video Quality Effect
    LaunchedEffect(videoQuality) {
        val quality = when(videoQuality) {
            "UHD" -> Quality.UHD
            "FHD" -> Quality.FHD
            "HD" -> Quality.HD
            "SD" -> Quality.SD
            else -> Quality.FHD
        }
        controller.videoCaptureQualitySelector = QualitySelector.from(quality)
    }

    // Photo Ratio Effect
    LaunchedEffect(photoAspectRatio) {
        val targetRatio = if (photoAspectRatio == 1) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3
        controller.setImageCaptureTargetSize(CameraController.OutputSize(targetRatio))
    }

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
                // Ensure preview stays within bounds
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier
    )
}
