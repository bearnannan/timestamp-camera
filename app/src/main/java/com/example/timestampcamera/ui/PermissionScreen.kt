package com.example.timestampcamera.ui

import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Colors
private val OrangeAccent = Color(0xFFFF8C42)
private val OrangeLight = Color(0xFFFFAB76)
private val BlackBackground = Color(0xFF000000)
private val DarkGray = Color(0xFF1A1A1A)
private val GrayText = Color(0xFF888888)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Check if all required permissions are granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }
    
    // Check camera permission specifically (required)
    val cameraGranted = permissionsState.permissions.find { 
        it.permission == Manifest.permission.CAMERA 
    }?.status?.isGranted == true

    val audioGranted = permissionsState.permissions.find {
        it.permission == Manifest.permission.RECORD_AUDIO
    }?.status?.isGranted == true
    
    val locationGranted = permissionsState.permissions.find {
        it.permission == Manifest.permission.ACCESS_FINE_LOCATION
    }?.status?.isGranted == true
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(OrangeAccent, OrangeLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "ต้องการสิทธิ์การใช้งาน",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = "แอพกล้องลงเวลาต้องการสิทธิ์ต่อไปนี้\nเพื่อการทำงานที่สมบูรณ์",
                color = GrayText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Permission Items
            PermissionItem(
                icon = Icons.Default.CameraAlt,
                title = "กล้องถ่ายรูป",
                description = "เพื่อถ่ายภาพและบันทึกวิดีโอ",
                isGranted = cameraGranted,
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            PermissionItem(
                icon = Icons.Default.Mic,
                title = "ไมโครโฟน",
                description = "เพื่อบันทึกเสียงในวิดีโอ",
                isGranted = audioGranted,
                isRequired = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = "ตำแหน่งที่ตั้ง",
                description = "เพื่อแสดงพิกัดและที่อยู่บนภาพ",
                isGranted = locationGranted,
                isRequired = false
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Request Button
            Button(
                onClick = {
                    permissionsState.launchMultiplePermissionRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (cameraGranted) "ดำเนินการต่อ" else "อนุญาตสิทธิ์",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Skip for location only if basic permissions are granted
            if (cameraGranted && audioGranted && !locationGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onAllPermissionsGranted
                ) {
                    Text(
                        text = "ข้ามไปก่อน",
                        color = GrayText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isGranted) OrangeAccent.copy(alpha = 0.1f) else DarkGray,
        animationSpec = tween(300),
        label = "permBgColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isGranted) OrangeAccent else Color.White,
        animationSpec = tween(300),
        label = "permIconColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isGranted) OrangeAccent else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isGranted) Color.Black else iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Text
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isRequired) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "จำเป็น",
                        color = OrangeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = GrayText,
                fontSize = 14.sp
            )
        }
        
        // Status
        if (isGranted) {
            Text(
                text = "อนุญาตแล้ว",
                color = OrangeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
