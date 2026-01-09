package com.example.timestampcamera.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.timestampcamera.data.AddressResolution
import com.example.timestampcamera.data.LocationData
import com.example.timestampcamera.util.AddressFormatter

@Composable
fun AddressResolutionDialog(
    currentResolution: AddressResolution,
    currentLocation: LocationData,
    onResolutionSelected: (AddressResolution) -> Unit,
    onDismiss: () -> Unit
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
                    text = "รูปแบบที่อยู่ (Address Format)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                AddressResolution.values().forEach { resolution ->
                    val formatted = AddressFormatter.formatAddress(currentLocation, resolution)
                    val displayText = if (resolution == AddressResolution.NONE) "ไม่มี (None)"
                                      else if (formatted.isBlank()) "รอสักครู่... (Waiting for location)" 
                                      else formatted
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResolutionSelected(resolution) }
                            .padding(vertical = 12.dp), // Increased padding for touch target
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (resolution == currentResolution),
                            onClick = { onResolutionSelected(resolution) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
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
