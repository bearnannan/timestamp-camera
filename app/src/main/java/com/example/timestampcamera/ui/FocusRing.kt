package com.example.timestampcamera.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity

@Composable
fun FocusRing(
    offset: Offset,
    visible: Boolean
) {
    // Fade Out Animation
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "FocusRingAlpha"
    )

    if (alpha > 0f && offset != Offset.Zero) {
        val size = 72.dp // Fixed size for the box
        // Adjust offset to center the box on the tap point
        val density = LocalDensity.current
        val adjustedX = with(density) { offset.x.toDp() - (size / 2) }
        val adjustedY = with(density) { offset.y.toDp() - (size / 2) }

        Box(
            modifier = Modifier
                .offset(x = adjustedX, y = adjustedY)
                .size(size)
                .graphicsLayer(alpha = alpha) 
                .border(2.dp, Color(0xFF00FF00), RoundedCornerShape(2.dp)) // Green Box
        )
    }
}
