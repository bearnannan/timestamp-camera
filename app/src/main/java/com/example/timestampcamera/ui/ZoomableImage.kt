package com.example.timestampcamera.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.positionChanged
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.net.Uri
import kotlin.math.PI
import kotlin.math.abs

@Composable
fun ZoomableImage(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Reset state when URI changes
    LaunchedEffect(uri) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size
                        
                        // IF we are not zoomed AND it is a single finger touch:
                        // Do NOT consume. Let the parent Pager handle the swipe.
                        if (scale == 1f && pointerCount == 1) {
                             // Allow Pager to intercept.
                             // We don't verify positionChanged here, we just ignore loop.
                             continue
                        }

                        // OTHERWISE (Zoomed OR Multi-touch):
                        // We are in Gesture Mode. Calculate and Consume.
                        
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) continue

                        // Calculate Transform
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        
                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                            scale = newScale
                            
                            // Only pan if zoomed in
                            if (scale > 1f) {
                                val newOffset = offset + panChange
                                // Add bounds logic if needed, for now free pan
                                // Ideally limit pan so strict edges don't fly off
                                val maxPanX = (scale - 1f) * 500f // Approximate bounds
                                val maxPanY = (scale - 1f) * 800f
                                
                                offset = Offset(
                                    newOffset.x.coerceIn(-maxPanX, maxPanX),
                                    newOffset.y.coerceIn(-maxPanY, maxPanY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                        
                        // CONSUME events to prevent Pager from scrolling while we are zooming/panning
                        event.changes.forEach { 
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(androidx.compose.ui.Alignment.Center)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
