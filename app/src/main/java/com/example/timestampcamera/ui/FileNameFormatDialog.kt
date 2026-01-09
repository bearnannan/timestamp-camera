package com.example.timestampcamera.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.timestampcamera.data.FileNameFormat
import com.example.timestampcamera.util.FileNameGenerator
import java.util.Date

@Composable
fun FileNameFormatDialog(
    currentFormat: FileNameFormat,
    onFormatSelected: (FileNameFormat) -> Unit,
    onDismiss: () -> Unit,
    // Preview Data
    sampleNote: String = "MyNote",
    sampleAddress: String = "Bangkok-Thailand"
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "รูปแบบชื่อไฟล์ (File Name Format)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Iterate all formats
                FileNameFormat.values().forEach { format ->
                    // Generate a preview
                    val previewText = FileNameGenerator.generateFileName(
                        format = format,
                        date = Date(),
                        note = sampleNote,
                        address = sampleAddress,
                        index = 1
                    ) + ".jpg"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatSelected(format) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (format == currentFormat),
                            onClick = { onFormatSelected(format) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
