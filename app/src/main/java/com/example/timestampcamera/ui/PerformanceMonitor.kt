package com.example.timestampcamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timestampcamera.data.ProcessingStats

// Colors
private val DarkGray = Color(0xFF2A2A2A)
private val OrangeAccent = Color(0xFFFF8C42)
private val WhiteColor = Color(0xFFFFFFFF)
private val GreenAccent = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMonitorBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    processingStats: ProcessingStats,
    onClearCache: () -> Unit,
    onOptimizeMemory: () -> Unit
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
                        Icons.Default.Speed,
                        contentDescription = "Performance",
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Performance Monitor",
                        color = OrangeAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = OrangeAccent)
                }
            }

            // Performance Stats
            PerformanceStatsCard(processingStats)

            // Cache Management
            CacheManagementCard(
                onClearCache = onClearCache,
                onOptimizeMemory = onOptimizeMemory
            )

            // Performance Tips
            PerformanceTipsCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PerformanceStatsCard(stats: ProcessingStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Performance Statistics",
                color = WhiteColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Average Processing Time
            StatRow(
                label = "Avg Processing Time",
                value = "${stats.averageProcessingTime}ms",
                color = if (stats.averageProcessingTime < 500) GreenAccent else 
                       if (stats.averageProcessingTime < 1000) OrangeAccent else RedAccent
            )

            // Cache Hit Rate
            StatRow(
                label = "Cache Hit Rate",
                value = "${(stats.cacheHitRate * 100).toInt()}%",
                color = if (stats.cacheHitRate > 0.7) GreenAccent else 
                       if (stats.cacheHitRate > 0.4) OrangeAccent else RedAccent
            )

            // Memory Usage
            val memoryMB = stats.memoryUsage / 1024 / 1024
            StatRow(
                label = "Memory Usage",
                value = "${memoryMB}MB",
                color = if (memoryMB < 100) GreenAccent else 
                       if (memoryMB < 200) OrangeAccent else RedAccent
            )

            // Total Processed
            StatRow(
                label = "Images Processed",
                value = "${stats.totalProcessed}",
                color = WhiteColor
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = WhiteColor.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CacheManagementCard(
    onClearCache: () -> Unit,
    onOptimizeMemory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Cache Management",
                color = WhiteColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangeAccent
                    ),
                    border = BorderStroke(1.dp, OrangeAccent)
                ) {
                    Text("Clear Cache")
                }

                Button(
                    onClick = onOptimizeMemory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = WhiteColor
                    )
                ) {
                    Text("Optimize")
                }
            }

            Text(
                "Clear cache to free up memory. Optimize to improve performance.",
                color = WhiteColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PerformanceTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Performance Tips",
                color = WhiteColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            val tips = listOf(
                "• Enable object detection only when needed" to "⚡",
                "• Use portrait mode sparingly" to "🖼️",
                "• Clear cache regularly" to "🗑️",
                "• Close background apps" to "📱",
                "• Use lower resolution for faster processing" to "📷"
            )

            tips.forEach { (tip, icon) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        icon,
                        color = OrangeAccent,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        tip,
                        color = WhiteColor.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceIndicator(
    stats: ProcessingStats,
    modifier: Modifier = Modifier
) {
    val performanceLevel = when {
        stats.averageProcessingTime < 500 && stats.cacheHitRate > 0.7 -> "Excellent"
        stats.averageProcessingTime < 1000 && stats.cacheHitRate > 0.4 -> "Good"
        else -> "Needs Optimization"
    }

    val indicatorColor = when (performanceLevel) {
        "Excellent" -> GreenAccent
        "Good" -> OrangeAccent
        else -> RedAccent
    }

    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(indicatorColor, CircleShape)
        )
        
        Column {
            Text(
                "Performance",
                color = WhiteColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                performanceLevel,
                color = indicatorColor,
                fontSize = 10.sp
            )
        }
    }
}
