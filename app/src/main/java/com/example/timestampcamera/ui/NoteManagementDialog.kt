package com.example.timestampcamera.ui

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.BufferedReader
import java.io.InputStreamReader

private val DarkGray = Color(0xFF1E1E1E)
private val WhiteColor = Color.White
private val OrangeAccent = Color(0xFFFF8C42)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteManagementDialog(
    currentNote: String,
    savedNotes: Set<String>,
    noteHistory: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onAddNote: (String) -> Unit,
    onRemoveNote: (String) -> Unit,
    onClearNotes: () -> Unit,
    onImportNotes: (Set<String>) -> Unit
) {
    var selectedNote by remember { mutableStateOf(currentNote) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    // File Picker for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val imported = mutableSetOf<String>()
                    reader.forEachLine { line ->
                        // Support comma-separated or line-separated
                        line.split(",").forEach { part ->
                            val clean = part.trim()
                            if (clean.isNotEmpty()) imported.add(clean)
                        }
                    }
                    onImportNotes(imported)
                    Toast.makeText(context, "นำเข้า ${imported.size} Notes สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "เกิดข้อผิดพลาดในการนำเข้า", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add Note Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = DarkGray,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("เพิ่ม Note ใหม่", color = WhiteColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WhiteColor,
                            unfocusedTextColor = WhiteColor,
                            cursorColor = OrangeAccent,
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = WhiteColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("ยกเลิก", color = OrangeAccent)
                        }
                        Button(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    onAddNote(newNoteText.trim())
                                    newNoteText = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                        ) {
                            Text("เพิ่ม", color = WhiteColor)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkGray,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "จัดการ Notes",
                    color = WhiteColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current Input
                OutlinedTextField(
                    value = selectedNote,
                    onValueChange = { selectedNote = it },
                    label = { Text("ข้อความปัจจุบัน", color = WhiteColor.copy(alpha = 0.7f)) },
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Note Button
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, OrangeAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ เพิ่ม Note ใหม่", color = OrangeAccent)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Saved Notes Section
                if (savedNotes.isNotEmpty()) {
                    Text(
                        "Notes ที่บันทึกไว้:",
                        color = WhiteColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        savedNotes.sorted().forEach { note ->
                            val isSelected = selectedNote == note
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedNote = note },
                                label = { Text(note, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Check, "Selected", modifier = Modifier.size(16.dp)) }
                                } else null,
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        "Delete",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveNote(note) },
                                        tint = Color.Red.copy(alpha = 0.7f)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = WhiteColor.copy(alpha = 0.1f),
                                    selectedContainerColor = OrangeAccent.copy(alpha = 0.3f),
                                    labelColor = WhiteColor,
                                    selectedLabelColor = WhiteColor
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Recent History
                if (noteHistory.isNotEmpty()) {
                    Text(
                        "ที่ใช้ล่าสุด (Recent):",
                        color = WhiteColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(noteHistory.size) { index ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WhiteColor.copy(alpha = 0.1f))
                                    .border(1.dp, WhiteColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { selectedNote = noteHistory[index] }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(noteHistory[index], color = WhiteColor, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (savedNotes.isEmpty() && noteHistory.isEmpty()) {
                    Text(
                        "ยังไม่มี Notes ที่บันทึกไว้ กดปุ่มด้านบนเพื่อเพิ่ม หรือ Import จากไฟล์",
                        color = WhiteColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Bottom Actions: Clear All | Import | Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onClearNotes,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("ล้างทั้งหมด")
                    }
                    
                    Row {
                        TextButton(onClick = { importLauncher.launch(arrayOf("text/plain")) }) {
                            Text("Import", color = OrangeAccent)
                        }
                        TextButton(onClick = {
                            // Export Notes
                            if (savedNotes.isNotEmpty()) {
                                try {
                                    val fileName = "notes_${System.currentTimeMillis()}.txt"
                                    val content = savedNotes.joinToString("\n")
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                        }
                                        val uri = context.contentResolver.insert(
                                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                            contentValues
                                        )
                                        uri?.let {
                                            context.contentResolver.openOutputStream(it)?.use { os ->
                                                os.write(content.toByteArray())
                                            }
                                            Toast.makeText(context, "Export สำเร็จ: Downloads/$fileName", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Export ต้องการ Android 10+", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "เกิดข้อผิดพลาดในการ Export", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "ไม่มี Notes ให้ Export", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Export", color = OrangeAccent)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // OK / Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ยกเลิก", color = OrangeAccent)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("ตกลง", color = WhiteColor)
                    }
                }
            }
        }
    }
}
