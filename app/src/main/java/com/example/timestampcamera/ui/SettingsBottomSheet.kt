package com.example.timestampcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Colors
private val DarkGray = Color(0xFF2A2A2A)
private val OrangeAccent = Color(0xFFFF8C42)
private val WhiteColor = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    // Settings States
    videoQuality: String,
    onVideoQualityChange: (String) -> Unit,
    aspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    dateWatermarkEnabled: Boolean,
    onDateWatermarkChange: (Boolean) -> Unit,
    shutterSoundEnabled: Boolean,
    onShutterSoundChange: (Boolean) -> Unit,
    gridLinesEnabled: Boolean,
    onGridLinesChange: (Boolean) -> Unit,
    // New Persistent Settings
    mapOverlayEnabled: Boolean,
    onMapOverlayChange: (Boolean) -> Unit,
    gpsFormat: Int,
    onGpsFormatChange: (Int) -> Unit,
    flipFrontPhoto: Boolean,
    onFlipFrontPhotoChange: (Boolean) -> Unit,
    imageFormat: com.example.timestampcamera.data.ImageFormat,
    onImageFormatChange: (com.example.timestampcamera.data.ImageFormat) -> Unit,
    compressionQuality: Int,
    onCompressionQualityChange: (Int) -> Unit,
    saveExif: Boolean,
    onSaveExifChange: (Boolean) -> Unit,
    customSavePath: String?,
    onCustomSavePathChange: (String?) -> Unit,
    // Battery Saver
    batterySaverMode: Boolean,
    onBatterySaverModeChange: (Boolean) -> Unit,
    // Custom Notes & Localization
    customNote: String,
    onCustomNoteChange: (String) -> Unit,
    dateFormat: String,
    onDateFormatChange: (String) -> Unit,
    useThaiLocale: Boolean,
    onUseThaiLocaleChange: (Boolean) -> Unit,
    // Overlay Styling
    textShadowEnabled: Boolean,
    onTextShadowChange: (Boolean) -> Unit,
    textBackgroundEnabled: Boolean,
    onTextBackgroundChange: (Boolean) -> Unit,
    // Phase 14: Advanced Styling
    textColor: Int,
    onTextColorChange: (Int) -> Unit,
    textSize: Float,
    onTextSizeChange: (Float) -> Unit,
    textStyle: Int,
    onTextStyleChange: (Int) -> Unit,
    // Phase 15: Pro Typography
    textAlpha: Int,
    onTextAlphaChange: (Int) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    overlayPosition: String,
    onOverlayPositionChange: (String) -> Unit,
    // Phase 16: Rich Data Overlays
    compassEnabled: Boolean,
    onCompassChange: (Boolean) -> Unit,
    altitudeEnabled: Boolean,
    onAltitudeChange: (Boolean) -> Unit,
    speedEnabled: Boolean,
    onSpeedChange: (Boolean) -> Unit,
    // Resolution Switching
    targetWidth: Int,
    targetHeight: Int,
    supportedResolutions: List<android.util.Size>,
    onTargetResolutionChange: (Int, Int) -> Unit,
    // Custom Logo
    hasLogo: Boolean,
    onLogoSelect: () -> Unit,
    onLogoRemove: () -> Unit,
    // Phase 17: Professional Workflow
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    inspectorName: String,
    onInspectorNameChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,

    // Phase 18: Advanced Typography
    textStrokeEnabled: Boolean,
    onTextStrokeEnabledChange: (Boolean) -> Unit,
    textStrokeWidth: Float,
    onTextStrokeWidthChange: (Float) -> Unit,
    textStrokeColor: Int,
    onTextStrokeColorChange: (Int) -> Unit,
    googleFontName: String,
    onGoogleFontNameChange: (String) -> Unit,
    templateId: Int,
    onTemplateIdChange: (Int) -> Unit,
    compassTapeEnabled: Boolean,
    onCompassTapeChange: (Boolean) -> Unit
) {
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    
    // Dialog States
    var showNoteDialog by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showInspectorDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    
    // Temp Values
    var tempNote by remember { mutableStateOf("") }
    var tempProject by remember { mutableStateOf("") }
    var tempInspector by remember { mutableStateOf("") }
    var tempTags by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkGray,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ตั้งค่ากล้อง",
                        color = WhiteColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WhiteColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Video Quality Selector
                SettingsSelectorItem(
                    title = "คุณภาพวิดีโอ",
                    value = videoQuality,
                    onClick = { showVideoQualityDialog = true }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Aspect Ratio Selector
                SettingsSelectorItem(
                    title = "อัตราส่วนภาพ",
                    value = aspectRatio,
                    onClick = { showAspectRatioDialog = true }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Date Watermark Toggle
                SettingsToggleItem(
                    title = "ลายน้ำวันที่",
                    isEnabled = dateWatermarkEnabled,
                    onToggle = onDateWatermarkChange
                )
                
                if (dateWatermarkEnabled) {
                     // Date Format
                    SettingsSelectorItem(
                        title = "รูปแบบวันที่",
                        value = dateFormat,
                        onClick = { showDateFormatDialog = true }
                    )
                    
                    // Thai Language Toggle
                    SettingsToggleItem(
                        title = "พุทธศักราช (พ.ศ.)",
                        isEnabled = useThaiLocale,
                        onToggle = onUseThaiLocaleChange
                    )
                    
                    // Custom Note
                    SettingsSelectorItem(
                        title = "ข้อความเพิ่มเติม (Note)",
                        value = if (customNote.isNotEmpty()) customNote else "แตะเพื่อใส่ข้อความ...",
                        onClick = { 
                            tempNote = customNote 
                            showNoteDialog = true 
                        }
                    )
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "การแสดงผลลายน้ำ (Appearance)",
                        color = OrangeAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            item {
                // Text Shadow Toggle
                SettingsToggleItem(
                    title = "เงาตัวหนังสือ (Text Shadow)",
                    isEnabled = textShadowEnabled,
                    onToggle = onTextShadowChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Text Stroke Toggle
                SettingsToggleItem(
                    title = "เส้นขอบตัวหนังสือ (Text Stroke)",
                    isEnabled = textStrokeEnabled,
                    onToggle = onTextStrokeEnabledChange
                )
                
                if (textStrokeEnabled) {
                     SliderItem(
                        title = "ความหนาเส้นขอบ: ${textStrokeWidth.toInt()}",
                        value = textStrokeWidth,
                        valueRange = 1f..10f,
                        onValueChange = onTextStrokeWidthChange
                    )
                    
                    ColorPickerItem(
                        selectedColor = textStrokeColor,
                        onColorSelected = onTextStrokeColorChange
                    )
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Text Background Toggle
                SettingsToggleItem(
                    title = "กรอบพื้นหลัง (Background Box)",
                    isEnabled = textBackgroundEnabled,
                    onToggle = onTextBackgroundChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("โลโก้ (Custom Logo)", color = WhiteColor, fontSize = 16.sp)
                        if (hasLogo) {
                             Text("Logo Selected", color = OrangeAccent, fontSize = 12.sp)
                        } else {
                             Text("Import transparent PNG", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    
                    Row {
                        if (hasLogo) {
                            TextButton(onClick = onLogoRemove) {
                                Text("Remove", color = Color.Red)
                            }
                        }
                        Button(
                            onClick = onLogoSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                        ) {
                             Text(if (hasLogo) "Change" else "Import")
                        }
                    }
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Text Color Picker
                ColorPickerItem(
                    selectedColor = textColor,
                    onColorSelected = onTextColorChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Text Size Slider
                SliderItem(
                    title = "ขนาดตัวอักษร: ${textSize.toInt()}",
                    value = textSize,
                    valueRange = 20f..80f,
                    onValueChange = onTextSizeChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Font Style Selector
                SegmentedButtonItem(
                    title = "รูปแบบฟอนต์",
                    options = listOf("ปกติ", "หนา", "Mono"),
                    selectedIndex = textStyle,
                    onOptionSelected = onTextStyleChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                // Font Selector (Google Fonts)
                SettingsSelectorItem(
                    title = "รูปแบบฟอนต์ (Font Family)",
                    value = googleFontName,
                    onClick = { showFontDialog = true }
                )
                  Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Font Selector
                SegmentedButtonItem(
                    title = "ฟอนต์ (Font Family)",
                    options = listOf("Sans", "Serif", "Mono", "Cursive"),
                    selectedIndex = when(fontFamily) {
                        "serif" -> 1
                        "monospace" -> 2
                        "cursive" -> 3
                        else -> 0
                    },
                    onOptionSelected = { index ->
                         val family = when(index) {
                             1 -> "serif"
                             2 -> "monospace"
                             3 -> "cursive"
                             else -> "sans"
                         }
                         onFontFamilyChange(family)
                    }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Opacity Slider
                 SliderItem(
                    title = "ความโปร่งใส (Opacity): ${(textAlpha * 100 / 255)}%",
                    value = textAlpha.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { onTextAlphaChange(it.toInt()) }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Position Selector
                Text(
                     text = "ตำแหน่งลายน้ำ (Position)",
                     color = WhiteColor,
                     fontSize = 16.sp,
                     modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                PositionSelectorItem(
                    selectedPosition = overlayPosition,
                    onPositionSelected = onOverlayPositionChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, // Or LocationOn
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ข้อมูลเชิงลึก (Rich Data)",
                        color = OrangeAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            item {
                // Compass Toggle
                SettingsToggleItem(
                    title = "เข็มทิศ (Compass)",
                    isEnabled = compassEnabled,
                    onToggle = onCompassChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                SettingsToggleItem(
                    title = "แถบเข็มทิศ (Compass Tape)",
                    isEnabled = compassTapeEnabled,
                    onToggle = onCompassTapeChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                // Altitude Toggle
                SettingsToggleItem(
                    title = "ความสูง (Altitude)",
                    isEnabled = altitudeEnabled,
                    onToggle = onAltitudeChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                // Speed Toggle
                SettingsToggleItem(
                    title = "ความเร็ว (Speed)",
                    isEnabled = speedEnabled,
                    onToggle = onSpeedChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }


            
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person, // Or Build/Work
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "การทำงานมืออาชีพ (Professional Workflow)",
                        color = OrangeAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            item {
                // Project Name
                SettingsSelectorItem(
                    title = "ชื่อโครงการ (Project Name)",
                    value = if (projectName.isNotEmpty()) projectName else "ระบุชื่อโครงการ...",
                    onClick = {
                        tempProject = projectName
                        showProjectDialog = true
                    }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                // Inspector Name
                SettingsSelectorItem(
                    title = "ผู้ตรวจงาน (Inspector)",
                    value = if (inspectorName.isNotEmpty()) inspectorName else "ระบุชื่อผู้ตรวจ...",
                    onClick = {
                        tempInspector = inspectorName
                        showInspectorDialog = true
                    }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                // Tags
                SettingsSelectorItem(
                    title = "แท็ก (Tags)",
                    value = if (tags.isNotEmpty()) tags else "เช่น Site A, Defect...",
                    onClick = {
                        tempTags = tags
                        showTagsDialog = true
                    }
                )
                 Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            


            item {
                // Map Overlay Toggle
                SettingsToggleItem(
                    title = "แผนที่ (Map Overlay)",
                    isEnabled = mapOverlayEnabled,
                    onToggle = onMapOverlayChange
                )
                
                if (mapOverlayEnabled || true) { // Always show GPS format if location is relevant usually, but maybe better inside Map section or standalone
                     // GPS Format Selector
                    SettingsSelectorItem(
                        title = "รูปแบบพิกัด (GPS Format)",
                        value = when(gpsFormat) {
                            1 -> "DMS"
                            2 -> "UTM"
                            3 -> "MGRS"
                            else -> "Decimal"
                        },
                        onClick = {
                            // Cycle through formats: 0->1->2->3->0
                            val nextFormat = (gpsFormat + 1) % 4
                            onGpsFormatChange(nextFormat)
                        }
                    )
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Custom Logo
                SettingsSelectorItem(
                    title = "โลโก้ที่กำหนดเอง (Custom Logo)",
                    value = if (hasLogo) "ตั้งค่าแล้ว (แตะเพื่อลบ)" else "เลือกรูปภาพ...",
                    onClick = {
                        if (hasLogo) onLogoRemove() else onLogoSelect()
                    }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Shutter Sound Toggle
                SettingsToggleItem(
                    title = "เสียงชัตเตอร์",
                    isEnabled = shutterSoundEnabled,
                    onToggle = onShutterSoundChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Grid Lines Toggle
                SettingsToggleItem(
                    title = "เส้นตาราง (Grid Lines)",
                    isEnabled = gridLinesEnabled,
                    onToggle = onGridLinesChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings, // Or Save/Image
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "การบันทึกรูปภาพ (Image Saving)",
                        color = OrangeAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            item {
                // Battery Saver Toggle
                SettingsToggleItem(
                    title = "โหมดประหยัดพลังงาน (จอดำเมื่ออัดวิดีโอ)",
                    isEnabled = batterySaverMode,
                    onToggle = onBatterySaverModeChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Photo Resolution
                SettingsSelectorItem(
                    title = "ความละเอียดรูปภาพ",
                    value = if (targetWidth > 0) {
                        val currentSize = supportedResolutions.find { it.width == targetWidth && it.height == targetHeight }
                        if (currentSize != null) {
                            "${currentSize.width}x${currentSize.height}"
                        } else "${targetWidth}x${targetHeight}"
                    } else "อัตโนมัติ (สูงสุด)",
                    onClick = { showResolutionDialog = true }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Flip Front Photo
                SettingsToggleItem(
                    title = "กลับด้านรูปกล้องหน้า",
                    isEnabled = flipFrontPhoto,
                    onToggle = onFlipFrontPhotoChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Image Format
                SettingsSelectorItem(
                    title = "นามสกุลไฟล์",
                    value = imageFormat.name,
                    onClick = { showFormatDialog = true }
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Compression Quality
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(text = "คุณภาพการบีบอัด: $compressionQuality%", color = WhiteColor, fontSize = 16.sp)
                    Slider(
                        value = compressionQuality.toFloat(),
                        onValueChange = { onCompressionQualityChange(it.toInt()) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = WhiteColor,
                            activeTrackColor = OrangeAccent
                        )
                    )
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Save EXIF
                SettingsToggleItem(
                    title = "บันทึกข้อมูล EXIF (GPS/Metadata)",
                    isEnabled = saveExif,
                    onToggle = onSaveExifChange
                )
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Save Path
                SettingsSelectorItem(
                    title = "โฟลเดอร์ที่เก็บรูป",
                    value = customSavePath ?: "ในแกลเลอรี่ (แอป)",
                    onClick = { onCustomSavePathChange(null) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    // Video Quality Selection Dialog
    if (showVideoQualityDialog) {
        SelectionDialog(
            title = "เลือกคุณภาพวิดีโอ",
            options = listOf("4K 2160p", "FHD 1080p", "HD 720p"),
            selectedOption = videoQuality,
            onSelect = { 
                onVideoQualityChange(it)
                showVideoQualityDialog = false
            },
            onDismiss = { showVideoQualityDialog = false }
        )
    }
    
    // Aspect Ratio Selection Dialog
    if (showAspectRatioDialog) {
        SelectionDialog(
            title = "เลือกอัตราส่วนภาพ",
            options = listOf("16:9", "4:3", "1:1"),
            selectedOption = aspectRatio,
            onSelect = { 
                onAspectRatioChange(it)
                showAspectRatioDialog = false
            },
            onDismiss = { showAspectRatioDialog = false }
        )
    }

    if (showDateFormatDialog) {
        SelectionDialog(
            title = "เลือกรูปแบบวันที่",
            options = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "d MMM yyyy", "EEE, d MMM yyyy"),
            selectedOption = dateFormat,
            onSelect = { 
                onDateFormatChange(it)
                showDateFormatDialog = false
            },
            onDismiss = { showDateFormatDialog = false }
        )
    }

    if (showFormatDialog) {
        SelectionDialog(
            title = "เลือกนามสกุลไฟล์",
            options = com.example.timestampcamera.data.ImageFormat.values().map { it.name },
            selectedOption = imageFormat.name,
            onSelect = { 
                onImageFormatChange(com.example.timestampcamera.data.ImageFormat.valueOf(it))
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false }
        )
    }

    if (showNoteDialog) {
        TextInputDialog(
            title = "ใส่ข้อความเพิ่มเติม",
            initialValue = tempNote,
            onConfirm = { 
                onCustomNoteChange(it)
                showNoteDialog = false 
            },
            onDismiss = { showNoteDialog = false }
        )
    }
    
    if (showProjectDialog) {
        TextInputDialog(
            title = "ชื่อโครงการ (Project Name)",
            initialValue = tempProject,
            onConfirm = { 
                onProjectNameChange(it)
                showProjectDialog = false 
            },
            onDismiss = { showProjectDialog = false }
        )
    }
    
    if (showInspectorDialog) {
        TextInputDialog(
            title = "ผู้ตรวจงาน (Inspector Name)",
            initialValue = tempInspector,
            onConfirm = { 
                onInspectorNameChange(it)
                showInspectorDialog = false 
            },
            onDismiss = { showInspectorDialog = false }
        )
    }
    
    if (showTagsDialog) {
        TextInputDialog(
            title = "แท็ก (Tags - คั่นด้วยจุลภาค)",
            initialValue = tempTags,
            onConfirm = { 
                onTagsChange(it)
                showTagsDialog = false 
            },
            onDismiss = { showTagsDialog = false }
        )
    }
    
    if (showFontDialog) {
        SelectionDialog(
            title = "เลือกฟอนต์",
            options = listOf("Roboto", "Oswald", "Roboto Mono", "Playfair Display", "Inter", "Cursive"),
            selectedOption = googleFontName,
            onSelect = { 
                onGoogleFontNameChange(it)
                showFontDialog = false 
            },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showResolutionDialog) {
        val options = listOf("อัตโนมัติ (สูงสุด)") + supportedResolutions.map { "${it.width}x${it.height}" }
        val selectedOption = if (targetWidth > 0) "${targetWidth}x${targetHeight}" else "อัตโนมัติ (สูงสุด)"

        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            containerColor = DarkGray,
            title = { Text("เลือกความละเอียดรูปภาพ", color = WhiteColor, fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(options.size) { index ->
                        val option = options[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (option == "อัตโนมัติ (สูงสุด)") {
                                        onTargetResolutionChange(-1, -1)
                                    } else {
                                        val size = supportedResolutions[index - 1]
                                        onTargetResolutionChange(size.width, size.height)
                                    }
                                    showResolutionDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = option,
                                    color = if (option == selectedOption) OrangeAccent else WhiteColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                                )
                                if (index > 0) {
                                    val size = supportedResolutions[index - 1]
                                    val aspect = size.width.toFloat() / size.height.toFloat()
                                    val label = when {
                                        abs(aspect - 1.777f) < 0.1 -> "16:9"
                                        abs(aspect - 1.333f) < 0.1 -> "4:3"
                                        abs(aspect - 1.0f) < 0.1 -> "1:1"
                                        else -> String.format("%.2f:1", aspect)
                                    }
                                    Text(
                                        text = label,
                                        color = WhiteColor.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (option == selectedOption) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text("ยกเลิก", color = OrangeAccent)
                }
            }
        )
    }

    if (showTemplateDialog) {
        val options = listOf("ดั้งเดิม (Classic)", "ทันสมัย (Modern Pro)", "มินิมอล (Minimal)")
        val selectedOption = when(templateId) {
            1 -> "ทันสมัย (Modern Pro)"
            2 -> "มินิมอล (Minimal)"
            else -> "ดั้งเดิม (Classic)"
        }
        
        SelectionDialog(
            title = "เลือกรูปแบบลายน้ำ",
            options = options,
            selectedOption = selectedOption,
            onSelect = { option ->
                val newId = when(option) {
                    "ทันสมัย (Modern Pro)" -> 1
                    "มินิมอล (Minimal)" -> 2
                    else -> 0
                }
                onTemplateIdChange(newId)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}
}

@Composable
private fun SettingsSelectorItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = WhiteColor,
            fontSize = 16.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = OrangeAccent,
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = WhiteColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = WhiteColor,
            fontSize = 16.sp
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WhiteColor,
                checkedTrackColor = OrangeAccent,
                uncheckedThumbColor = WhiteColor.copy(alpha = 0.8f),
                uncheckedTrackColor = WhiteColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGray,
        title = {
            Text(
                text = title,
                color = WhiteColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            color = if (option == selectedOption) OrangeAccent else WhiteColor,
                            fontSize = 16.sp,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                        if (option == selectedOption) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = OrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = OrangeAccent)
            }
        }
    )
}

@Composable
private fun ColorPickerItem(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colors = listOf(
        Color.White, Color.Yellow, Color.Cyan, Color.Green, 
        Color.Magenta, Color.Red, Color(0xFFFF8C42), Color.Blue, Color.Black
    )

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text("สีตัวอักษร", color = WhiteColor, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors.size) { index ->
                val color = colors[index]
                val colorInt = android.graphics.Color.argb(
                    (color.alpha * 255).toInt(), 
                    (color.red * 255).toInt(), 
                    (color.green * 255).toInt(), 
                    (color.blue * 255).toInt()
                )
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(colorInt) }
                        .then(
                            if (selectedColor == colorInt) {
                                Modifier.border(2.dp, WhiteColor, CircleShape)
                            } else Modifier
                        )
                ) {
                    if (selectedColor == colorInt) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (color == Color.White) Color.Black else Color.White,
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, color = WhiteColor, fontSize = 16.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = WhiteColor,
                activeTrackColor = OrangeAccent
            )
        )
    }
}

@Composable
private fun SegmentedButtonItem(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, color = WhiteColor, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) OrangeAccent else Color.Transparent)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) WhiteColor else WhiteColor.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionSelectorItem(
    selectedPosition: String,
    onPositionSelected: (String) -> Unit
) {
    // 3x3 Grid
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PositionBox("TOP_LEFT", selectedPosition, onPositionSelected)
            PositionBox("TOP_CENTER", selectedPosition, onPositionSelected)
            PositionBox("TOP_RIGHT", selectedPosition, onPositionSelected)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Row 2
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             PositionBox("CENTER_LEFT", selectedPosition, onPositionSelected)
             PositionBox("CENTER", selectedPosition, onPositionSelected)
             PositionBox("CENTER_RIGHT", selectedPosition, onPositionSelected)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Row 3
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PositionBox("BOTTOM_LEFT", selectedPosition, onPositionSelected)
            PositionBox("BOTTOM_CENTER", selectedPosition, onPositionSelected)
            PositionBox("BOTTOM_RIGHT", selectedPosition, onPositionSelected)
        }
    }
}

@Composable
private fun PositionBox(
    position: String,
    selectedPosition: String,
    onClick: (String) -> Unit
) {
    val isSelected = position == selectedPosition
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OrangeAccent else WhiteColor.copy(alpha = 0.1f))
            .border(1.dp, if (isSelected) OrangeAccent else WhiteColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick(position) },
        contentAlignment = Alignment.Center
    ) {
        // Mini representation of the screen + dot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val align = when(position) {
                "TOP_LEFT" -> Alignment.TopStart
                "TOP_CENTER" -> Alignment.TopCenter
                "TOP_RIGHT" -> Alignment.TopEnd
                "CENTER_LEFT" -> Alignment.CenterStart
                "CENTER" -> Alignment.Center
                "CENTER_RIGHT" -> Alignment.CenterEnd
                "BOTTOM_LEFT" -> Alignment.BottomStart
                "BOTTOM_CENTER" -> Alignment.BottomCenter
                "BOTTOM_RIGHT" -> Alignment.BottomEnd
                else -> Alignment.Center
            }
            Box(
                 modifier = Modifier
                     .size(8.dp)
                     .clip(CircleShape)
                     .background(if (isSelected) WhiteColor else WhiteColor.copy(alpha = 0.5f))
                     .align(align)
            )
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGray,
        title = {
            Text(
                text = title,
                color = WhiteColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteColor,
                    unfocusedTextColor = WhiteColor,
                    cursorColor = OrangeAccent,
                    focusedBorderColor = OrangeAccent,
                    unfocusedBorderColor = WhiteColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("ตกลง", color = OrangeAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = OrangeAccent)
            }
        }
    )
}
