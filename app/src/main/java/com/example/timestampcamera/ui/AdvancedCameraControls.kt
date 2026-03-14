package com.example.timestampcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors
private val DarkGray = Color(0xFF2A2A2A)
private val OrangeAccent = Color(0xFFFF8C42)
private val WhiteColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCameraControlsBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    // Camera Controls
    manualModeEnabled: Boolean,
    onManualModeChange: (Boolean) -> Unit,
    iso: Int,
    onIsoChange: (Int) -> Unit,
    exposureTime: Long,
    onExposureTimeChange: (Long) -> Unit,
    whiteBalance: String,
    onWhiteBalanceChange: (String) -> Unit,
    focusMode: String,
    onFocusModeChange: (String) -> Unit,
    // Enhancement Settings
    autoEnhanceEnabled: Boolean,
    onAutoEnhanceChange: (Boolean) -> Unit,
    portraitModeEnabled: Boolean,
    onPortraitModeChange: (Boolean) -> Unit,
    objectDetectionEnabled: Boolean,
    onObjectDetectionChange: (Boolean) -> Unit,
    brightnessAdjustment: Float,
    onBrightnessChange: (Float) -> Unit,
    contrastAdjustment: Float,
    onContrastChange: (Float) -> Unit,
    saturationAdjustment: Float,
    onSaturationChange: (Float) -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = DarkGray,
        contentColor = WhiteColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Advanced Controls",
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Advanced Camera Controls",
                        color = OrangeAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = OrangeAccent)
                }
            }

            // Manual Mode Toggle
            SettingsToggleItem(
                "Manual Mode",
                manualModeEnabled,
                onManualModeChange,
                description = "Enable professional camera controls"
            )

            if (manualModeEnabled) {
                // ISO Control
                Column {
                    Text(
                        "ISO: $iso",
                        color = WhiteColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    IsoSlider(
                        value = iso,
                        onValueChange = onIsoChange
                    )
                }

                // Exposure Time Control
                Column {
                    Text(
                        "Exposure: ${formatExposureTime(exposureTime)}",
                        color = WhiteColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposureSlider(
                        value = exposureTime,
                        onValueChange = onExposureTimeChange
                    )
                }

                // White Balance
                Column {
                    Text(
                        "White Balance",
                        color = WhiteColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    WhiteBalanceSelector(
                        selected = whiteBalance,
                        onSelected = onWhiteBalanceChange
                    )
                }

                // Focus Mode
                Column {
                    Text(
                        "Focus Mode",
                        color = WhiteColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FocusModeSelector(
                        selected = focusMode,
                        onSelected = onFocusModeChange
                    )
                }
            }

            Divider(color = WhiteColor.copy(alpha = 0.2f))

            // Image Enhancement Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Enhancement",
                    tint = OrangeAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Image Enhancement",
                    color = OrangeAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Enhancement Toggles
            SettingsToggleItem("Auto Enhance", autoEnhanceEnabled, onAutoEnhanceChange)
            SettingsToggleItem("Portrait Mode", portraitModeEnabled, onPortraitModeChange)
            SettingsToggleItem("Object Detection", objectDetectionEnabled, onObjectDetectionChange)

            // Adjustment Sliders
            if (autoEnhanceEnabled) {
                Column {
                    Text(
                        "Brightness: ${brightnessAdjustment.toInt()}",
                        color = WhiteColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AdjustmentSlider(
                        value = brightnessAdjustment,
                        onValueChange = onBrightnessChange,
                        range = -100f..100f,
                        label = "Brightness"
                    )
                }

                Column {
                    Text(
                        "Contrast: ${contrastAdjustment.toInt()}",
                        color = WhiteColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AdjustmentSlider(
                        value = contrastAdjustment,
                        onValueChange = onContrastChange,
                        range = -100f..100f,
                        label = "Contrast"
                    )
                }

                Column {
                    Text(
                        "Saturation: ${saturationAdjustment.toInt()}",
                        color = WhiteColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AdjustmentSlider(
                        value = saturationAdjustment,
                        onValueChange = onSaturationChange,
                        range = -100f..100f,
                        label = "Saturation"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    description: String = ""
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = WhiteColor, fontSize = 16.sp)
                if (description.isNotEmpty()) {
                    Text(
                        description,
                        color = WhiteColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WhiteColor,
                    checkedTrackColor = OrangeAccent,
                    uncheckedThumbColor = WhiteColor.copy(alpha = 0.8f),
                    uncheckedTrackColor = DarkGray
                )
            )
        }
    }
}

@Composable
private fun IsoSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val isoValues = listOf(100, 200, 400, 800, 1600, 3200)
    val index = isoValues.indexOf(value).coerceIn(0, isoValues.size - 1)
    
    Slider(
        value = index.toFloat(),
        onValueChange = { onValueChange(isoValues[it.toInt()]) },
        valueRange = 0f..(isoValues.size - 1).toFloat(),
        steps = isoValues.size - 2,
        colors = SliderDefaults.colors(
            thumbColor = OrangeAccent,
            activeTrackColor = OrangeAccent,
            inactiveTrackColor = DarkGray
        )
    )
}

@Composable
private fun ExposureSlider(
    value: Long,
    onValueChange: (Long) -> Unit
) {
    val exposureValues = listOf(
        1000000L,    // 1/1000s
        2000000L,    // 1/500s
        5000000L,    // 1/200s
        10000000L,   // 1/100s
        20000000L,   // 1/50s
        50000000L,   // 1/20s
        100000000L   // 1/10s
    )
    val index = exposureValues.indexOf(value).coerceIn(0, exposureValues.size - 1)
    
    Slider(
        value = index.toFloat(),
        onValueChange = { onValueChange(exposureValues[it.toInt()]) },
        valueRange = 0f..(exposureValues.size - 1).toFloat(),
        steps = exposureValues.size - 2,
        colors = SliderDefaults.colors(
            thumbColor = OrangeAccent,
            activeTrackColor = OrangeAccent,
            inactiveTrackColor = DarkGray
        )
    )
}

@Composable
private fun WhiteBalanceSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("AUTO", "DAYLIGHT", "CLOUDY", "FLUORESCENT", "INCANDESCENT")
    
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(option) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    option,
                    color = if (selected == option) OrangeAccent else WhiteColor,
                    fontSize = 14.sp
                )
                if (selected == option) {
                    Text("✓", color = OrangeAccent, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun FocusModeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("AUTO", "MANUAL", "MACRO", "INFINITY")
    
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(option) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    option,
                    color = if (selected == option) OrangeAccent else WhiteColor,
                    fontSize = 14.sp
                )
                if (selected == option) {
                    Text("✓", color = OrangeAccent, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    label: String
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = OrangeAccent,
            activeTrackColor = OrangeAccent,
            inactiveTrackColor = DarkGray
        )
    )
}

private fun formatExposureTime(time: Long): String {
    return when (time) {
        1000000L -> "1/1000s"
        2000000L -> "1/500s"
        5000000L -> "1/200s"
        10000000L -> "1/100s"
        20000000L -> "1/50s"
        50000000L -> "1/20s"
        100000000L -> "1/10s"
        else -> "${time / 1000000}s"
    }
}
