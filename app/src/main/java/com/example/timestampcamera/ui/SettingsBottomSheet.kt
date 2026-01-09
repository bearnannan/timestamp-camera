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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.timestampcamera.data.CustomField
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
    timeWatermarkEnabled: Boolean,
    onTimeWatermarkChange: (Boolean) -> Unit,
    shutterSoundEnabled: Boolean,
    onShutterSoundChange: (Boolean) -> Unit,
    gridLinesEnabled: Boolean,
    onGridLinesChange: (Boolean) -> Unit,

    virtualLevelerEnabled: Boolean,
    onVirtualLevelerChange: (Boolean) -> Unit,
    volumeShutterEnabled: Boolean,
    onVolumeShutterChange: (Boolean) -> Unit,
    // New Persistent Settings
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
    // Address & Coords Visibility
    addressEnabled: Boolean,
    onAddressEnabledChange: (Boolean) -> Unit,
    coordinatesEnabled: Boolean,
    onCoordinatesEnabledChange: (Boolean) -> Unit,
    // Cloud Path
    cloudPath: String,
    onCloudPathChange: (String) -> Unit,
    addressResolution: com.example.timestampcamera.data.AddressResolution,
    onAddressResolutionChange: (com.example.timestampcamera.data.AddressResolution) -> Unit,
    currentLocation: com.example.timestampcamera.data.LocationData = com.example.timestampcamera.data.LocationData(), // Default
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
    noteHistory: List<String> = emptyList(),
    tagsHistory: List<String> = emptyList(),
    
    customFields: List<CustomField> = emptyList(),
    onCustomFieldsChange: (List<CustomField>) -> Unit,

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
    // compassTapeEnabled Removed
    // onCompassTapeChange Removed
    // Compass
    compassPosition: String,
    onCompassPositionChange: (String) -> Unit,
    // Text Order
    customTextOrder: List<com.example.timestampcamera.data.WatermarkItemType>,
    onCustomTextOrderChange: (List<com.example.timestampcamera.data.WatermarkItemType>) -> Unit,
    // Tag Management
    availableTags: Set<String> = emptySet(),
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onClearTags: () -> Unit = {},
    onImportTags: (Set<String>) -> Unit = {},
    // Advanced Saving
    saveOriginalPhoto: Boolean,
    onSaveOriginalPhotoChange: (Boolean) -> Unit,
    fileNameFormat: com.example.timestampcamera.data.FileNameFormat,
    onFileNameFormatChange: (com.example.timestampcamera.data.FileNameFormat) -> Unit,
    // Advanced Sync Constraints
    uploadOnlyWifi: Boolean,
    onUploadOnlyWifiChange: (Boolean) -> Unit,
    uploadLowBattery: Boolean,
    onUploadLowBatteryChange: (Boolean) -> Unit
) {
    var showFileNameDialog by remember { mutableStateOf(false) }
    var showCloudPathDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }

    if (showReorderDialog) {
        ReorderTextDialog(
            currentOrder = customTextOrder,
            onSave = { 
                onCustomTextOrderChange(it)
                showReorderDialog = false // Close explicitly or rely on dismiss
            },
            onDismiss = { showReorderDialog = false }
        )
    }
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
    var showCompassPosDialog by remember { mutableStateOf(false) }
    
    // Temp Values
    var tempNote by remember { mutableStateOf("") }
    var tempProject by remember { mutableStateOf("") }
    var tempInspector by remember { mutableStateOf("") }
    var tempTags by remember { mutableStateOf("") }
    
    var showCustomFieldDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<CustomField?>(null) }
    
    var showAddressResolutionDialog by remember { mutableStateOf(false) }

    // Google Auth State
    val context = androidx.compose.ui.platform.LocalContext.current
    var signedInEmail by remember { mutableStateOf<String?>(null) }
    
    // Check status on load
    LaunchedEffect(Unit) {
        val account = com.example.timestampcamera.auth.GoogleAuthManager.getSignedInAccount(context)
        signedInEmail = account?.email
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            signedInEmail = account?.email
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Sign In Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
                        text = "การตั้งค่า (Settings)",
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ==========================================
            // 1. PROJECT & WORKFLOW (Top Priority)
            // ==========================================
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ข้อมูลงาน (Project Info)", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SettingsSelectorItem("ชื่อโครงการ (Project)", if (projectName.isNotEmpty()) projectName else "ระบุ...", onClick = { tempProject = projectName; showProjectDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                SettingsSelectorItem("ผู้ตรวจงาน (Inspector)", if (inspectorName.isNotEmpty()) inspectorName else "ระบุ...", onClick = { tempInspector = inspectorName; showInspectorDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                SettingsSelectorItem("ข้อความ/หมายเหตุ (Note)", if (customNote.isNotEmpty()) customNote else "ระบุ...", onClick = { tempNote = customNote; showNoteDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                SettingsSelectorItem("แท็ก (Tags)", if (tags.isNotEmpty()) tags else "ระบุ...", onClick = { tempTags = tags; showTagsDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                SettingsSelectorItem("จัดลำดับข้อความ (Reorder Text)", "แก้ไข >", onClick = { showReorderDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            // ==========================================
            // CLOUD SYNC (Google Drive)
            // ==========================================
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cloud Sync (Google Drive)", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (signedInEmail != null) "เชื่อมต่อแล้ว (Connected)" else "ยังไม่เชื่อมต่อ (Not Connected)",
                            color = WhiteColor,
                            fontSize = 14.sp
                        )
                        if (signedInEmail != null) {
                            Text(
                                text = signedInEmail!!,
                                color = WhiteColor.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (signedInEmail != null) {
                                com.example.timestampcamera.auth.GoogleAuthManager.signOut(context) {
                                    signedInEmail = null
                                }
                            } else {
                                val signInIntent = com.example.timestampcamera.auth.GoogleAuthManager.getSignInIntent(context)
                                signInLauncher.launch(signInIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (signedInEmail != null) Color.Red.copy(alpha = 0.8f) else OrangeAccent
                        ),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (signedInEmail != null) "Sign Out" else "Sign In",
                            color = WhiteColor
                        )
                    }
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            // Cloud Path Input
            if (signedInEmail != null) {
                item {
                    SettingsSelectorItem(
                        title = "โฟลเดอร์หลัก (Main Folder Path)",
                        value = if (cloudPath.isEmpty()) "ระบุ..." else cloudPath,
                        onClick = { showCloudPathDialog = true }
                    )
                    
                    Text(
                        text = "เช่น: 'Work/2024'. รูปจะถูกเก็บใน 'Work/2024/[Note]'",
                        color = WhiteColor.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                    
                    Divider(color = WhiteColor.copy(alpha = 0.1f))

                    // Wi-Fi Only Toggle
                    SettingsToggleItem(
                        title = "อัปโหลดผ่าน Wi-Fi เท่านั้น",
                        isEnabled = uploadOnlyWifi,
                        onToggle = onUploadOnlyWifiChange
                    )
                    
                    // Battery Aware Toggle
                    SettingsToggleItem(
                        title = "รอให้แบตเตอรี่ > 20% (Battery Aware)",
                        isEnabled = uploadLowBattery,
                        onToggle = onUploadLowBatteryChange
                    )
                    
                    Text(
                        text = "ช่วยประหยัดเน็ตมือถือและรักษาอายุการใช้งานแบตเตอรี่",
                        color = WhiteColor.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                    
                    Divider(color = WhiteColor.copy(alpha = 0.1f))
                }
            }

            // Custom Fields List (Dynamic)
            items(customFields.size) { index ->
                val field = customFields[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                             editingField = field
                             showCustomFieldDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = field.label, color = WhiteColor.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(text = field.value, color = WhiteColor, fontSize = 14.sp)
                    }
                    IconButton(onClick = { 
                        // Delete logic
                        val newList = customFields.toMutableList()
                        newList.removeAt(index)
                        onCustomFieldsChange(newList)
                    }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            editingField = null 
                            showCustomFieldDialog = true 
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Icon(Icons.Default.Add, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("เพิ่มฟิลด์ข้อมูล (Add Field)", color = OrangeAccent, fontSize = 14.sp)
                }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ==========================================
            // 2. WATERMARK DESIGN (Visuals)
            // ==========================================
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("รูปแบบลายน้ำ (Design)", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Template Selector
                val templateName = when(templateId) {
                    1 -> "ทันสมัย (Modern)"
                    2 -> "มินิมอล (Minimal)"
                    else -> "ดั้งเดิม (Classic)"
                }
                SettingsSelectorItem("เทมเพลต (Template)", templateName, onClick = { showTemplateDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                // Custom Logo
                SettingsSelectorItem("โลโก้ (Logo)", if (hasLogo) "เลือกแล้ว" else "ไม่มี", onClick = { if (hasLogo) onLogoRemove() else onLogoSelect() })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            // Only show detailed font settings if customization is desired (maybe hide for minimal templates? keep for now)
            item {
                SettingsSelectorItem("ฟอนต์ (Font)", googleFontName, onClick = { showFontDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                 // Combined Text Style
                 Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text("ตัวหนา (Bold)", color = WhiteColor, fontSize = 16.sp)
                     Switch(
                        checked = textStyle == 1,
                        onCheckedChange = { onTextStyleChange(if (it) 1 else 0) },
                        colors = SwitchDefaults.colors(checkedThumbColor = WhiteColor, checkedTrackColor = OrangeAccent)
                     )
                 }
                 Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                ColorPickerItem(selectedColor = textColor, onColorSelected = onTextColorChange)
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                SliderItem("ขนาดข้อความ: ${textSize.toInt()}", textSize, 20f..80f, onTextSizeChange)
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                Text("ตำแหน่ง (Position)", color = WhiteColor, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                PositionSelectorItem(overlayPosition, onOverlayPositionChange)
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ==========================================
            // 3. DISPLAY CONTENT (Toggles)
            // ==========================================
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ข้อมูลที่แสดง (Content)", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingsToggleItem("วันที่และเวลา", dateWatermarkEnabled, onDateWatermarkChange)
                 if (dateWatermarkEnabled) {
                     Row(modifier = Modifier.padding(start = 16.dp)) {
                         SettingsSelectorItem("รูปแบบวันที่", dateFormat, onClick = { showDateFormatDialog = true })
                     }
                     Row(modifier = Modifier.padding(start = 16.dp)) {
                         SettingsToggleItem("แสดงเวลา (Show Time)", timeWatermarkEnabled, onTimeWatermarkChange)
                     }
                     Row(modifier = Modifier.padding(start = 16.dp)) {
                         SettingsToggleItem("ใช้ พ.ศ. (Thai Year)", useThaiLocale, onUseThaiLocaleChange)
                     }
                 }
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item {
                SettingsToggleItem("ที่อยู่ (Address)", addressEnabled, onAddressEnabledChange)
                if (addressEnabled) {
                    SettingsSelectorItem("รูปแบบที่อยู่", addressResolution.name) { showAddressResolutionDialog = true }
                }
                SettingsToggleItem("พิกัด (Coordinates)", coordinatesEnabled, onCoordinatesEnabledChange)
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                 // Rich Data Group
                 SettingsToggleItem("ระดับน้ำ (Leveler)", virtualLevelerEnabled, onVirtualLevelerChange)
                 SettingsToggleItem("เข็มทิศ (Compass)", compassEnabled, onCompassChange)
                 if (compassEnabled) {
                     // Add simple selector
                     SettingsSelectorItem("ตำแหน่งเข็มทิศ", compassPosition, onClick = {
                         showCompassPosDialog = true
                     })
                 }
                 // Compass Tape Removed
                 SettingsToggleItem("ความสูง + ความเร็ว", altitudeEnabled && speedEnabled) { 
                     onAltitudeChange(it)
                     onSpeedChange(it)
                 }
                 Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                 // GPS Format
                 SettingsSelectorItem("รูปแบบพิกัด (GPS)", when(gpsFormat) { 1->"DMS"; 2->"UTM"; 3->"MGRS"; else->"Decimal" }) {
                     onGpsFormatChange((gpsFormat + 1) % 4)
                 }
                 Divider(color = WhiteColor.copy(alpha = 0.1f))
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ==========================================
            // 4. CAMERA CONFIG (Hardware/System)
            // ==========================================
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ตั้งค่ากล้อง (Camera Config)", color = OrangeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SettingsSelectorItem("ความละเอียด (Resolution)", "${targetWidth}x${targetHeight}", onClick = { showResolutionDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                SettingsSelectorItem("อัตราส่วน (Ratio)", aspectRatio, onClick = { showAspectRatioDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            item {
                 SettingsToggleItem("ปุ่มถ่ายภาพด้วยปุ่มเสียง (Volume Key Shutter)", volumeShutterEnabled, onVolumeShutterChange)
                 SettingsToggleItem("เสียงชัตเตอร์", shutterSoundEnabled, onShutterSoundChange)
                 SettingsToggleItem("เส้นตาราง (Grid)", gridLinesEnabled, onGridLinesChange)
                 SettingsToggleItem("กลับด้านรูปกล้องหน้า (Flip Front)", flipFrontPhoto, onFlipFrontPhotoChange)
                 SettingsToggleItem("โหมดประหยัดแบต (Black Screen)", batterySaverMode, onBatterySaverModeChange)
                 SettingsToggleItem("บันทึก EXIF", saveExif, onSaveExifChange)
                 SettingsToggleItem("เก็บภาพต้นฉบับ (Save Original)", saveOriginalPhoto, onSaveOriginalPhotoChange)
                 Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                SettingsSelectorItem("รูปแบบชื่อไฟล์ (File Name)", fileNameFormat.name, onClick = { showFileNameDialog = true })
                Divider(color = WhiteColor.copy(alpha = 0.1f))
            }
            
            item {
                 SettingsSelectorItem("ที่เก็บไฟล์", customSavePath ?: "แกลเลอรี่", onClick = { onCustomSavePathChange(null) })
            }
            
            item { Spacer(modifier = Modifier.height(48.dp)) }

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

    if (showCustomFieldDialog) {
        AddCustomFieldDialog(
            initialField = editingField,
            onConfirm = { newField ->
                val activeList = customFields.toMutableList()
                if (editingField != null) {
                    val index = activeList.indexOfFirst { it.id == editingField!!.id }
                    if (index != -1) {
                        activeList[index] = newField
                    }
                } else {
                    activeList.add(newField)
                }
                onCustomFieldsChange(activeList)
                showCustomFieldDialog = false
                editingField = null
            },
            onDismiss = { 
                showCustomFieldDialog = false 
                editingField = null
            }
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
            onDismiss = { showNoteDialog = false },
            suggestions = noteHistory
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

    if (showCloudPathDialog) {
        TextInputDialog(
            title = "โฟลเดอร์หลัก (Cloud Path)",
            initialValue = cloudPath,
            onConfirm = { 
                onCloudPathChange(it)
                showCloudPathDialog = false 
            },
            onDismiss = { showCloudPathDialog = false },
            placeholder = "e.g. MyWork/2024"
        )
    }

    if (showCompassPosDialog) {
        val options = listOf("TOP_LEFT", "TOP_CENTER", "TOP_RIGHT", "CENTER_LEFT", "CENTER", "CENTER_RIGHT", "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT")
        SelectionDialog(
            title = "ตำแหน่งเข็มทิศ",
            options = options,
            selectedOption = compassPosition,
            onSelect = { 
                onCompassPositionChange(it)
                showCompassPosDialog = false
            },
            onDismiss = { showCompassPosDialog = false }
        )
    }

    if (showFileNameDialog) {
        FileNameFormatDialog(
            currentFormat = fileNameFormat,
            onFormatSelected = {
                onFileNameFormatChange(it)
                showFileNameDialog = false
            },
            onDismiss = { showFileNameDialog = false },
            sampleNote = if (customNote.isNotBlank()) customNote else "MyNote",
            sampleAddress = if (currentLocation.address.isNotBlank()) currentLocation.address else "Bangkok"
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
        TagManagementDialog(
            initialTags = tags,
            availableTags = availableTags,
            onDismiss = { showTagsDialog = false },
            onConfirm = { 
                onTagsChange(it)
                showTagsDialog = false 
            },
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onClearTags = onClearTags,
            onImportTags = onImportTags
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

    if (showAddressResolutionDialog) {
        AddressResolutionDialog(
            currentResolution = addressResolution,
            currentLocation = currentLocation,
            onResolutionSelected = { 
                onAddressResolutionChange(it)
                showAddressResolutionDialog = false
            },
            onDismiss = { showAddressResolutionDialog = false }
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
    onDismiss: () -> Unit,
    suggestions: List<String> = emptyList(),
    placeholder: String? = null
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
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = if (placeholder != null) { { Text(placeholder, color = WhiteColor.copy(alpha = 0.3f)) } } else null,
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
                
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ที่ใช้ล่าสุด (Recent):", color = WhiteColor.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions.size) { index ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { text = suggestions[index] }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(suggestions[index], color = WhiteColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
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

@Composable
private fun AddCustomFieldDialog(
    initialField: CustomField?,
    onConfirm: (field: CustomField) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialField?.label ?: "") }
    var value by remember { mutableStateOf(initialField?.value ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGray,
        title = {
            Text(
                text = if (initialField == null) "เพิ่มข้อมูล (Add Field)" else "แก้ไขข้อมูล (Edit Field)",
                color = WhiteColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("ชื่อหัวข้อ (Label)", color = WhiteColor.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WhiteColor,
                        unfocusedTextColor = WhiteColor,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = WhiteColor.copy(alpha = 0.5f),
                        cursorColor = OrangeAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("ข้อมูล (Value)", color = WhiteColor.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WhiteColor,
                        unfocusedTextColor = WhiteColor,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = WhiteColor.copy(alpha = 0.5f),
                        cursorColor = OrangeAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (label.isNotBlank()) {
                        val newField = initialField?.copy(label = label, value = value) 
                            ?: CustomField(label = label, value = value)
                        onConfirm(newField)
                    }
                },
                enabled = label.isNotBlank()
            ) {
                Text("ตกลง", color = if (label.isNotBlank()) OrangeAccent else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = OrangeAccent)
            }
        }
    )
}
