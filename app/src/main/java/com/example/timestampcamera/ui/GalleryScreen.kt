package com.example.timestampcamera.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.timestampcamera.data.MediaItem
import com.example.timestampcamera.data.MediaType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    mediaList: List<MediaItem>,
    onBack: () -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    var viewMode by remember { mutableStateOf(GalleryViewMode.GRID) }
    var selectedIndex by remember { mutableStateOf(0) }

    Crossfade(targetState = viewMode, label = "GalleryTransition") { mode ->
        when (mode) {
            GalleryViewMode.GRID -> {
                GalleryGridView(
                    mediaList = mediaList,
                    onBack = onBack,
                    onMediaClick = { index ->
                        selectedIndex = index
                        viewMode = GalleryViewMode.PAGER
                    }
                )
            }
            GalleryViewMode.PAGER -> {
                GalleryPagerView(
                    mediaList = mediaList,
                    initialIndex = selectedIndex,
                    onBack = { viewMode = GalleryViewMode.GRID },
                    onDelete = { item ->
                        onDelete(item)
                        if (mediaList.size <= 1) { 
                            viewMode = GalleryViewMode.GRID
                        }
                    }
                )
            }
        }
    }
}

enum class GalleryViewMode {
    GRID, PAGER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryGridView(
    mediaList: List<MediaItem>,
    onBack: () -> Unit,
    onMediaClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("แกลเลอรี่", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("ยังไม่มีรูปภาพ", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(mediaList) { index, item ->
                    Box(modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onMediaClick(index) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (item.type == MediaType.VIDEO) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryPagerView(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    if (mediaList.isEmpty()) {
        onBack() // Safety fallback
        return
    }

    val safeIndex = initialIndex.coerceIn(0, mediaList.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { mediaList.size })
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val currentItem = mediaList.getOrNull(pagerState.currentPage)
                    Column {
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(currentItem?.dateTaken ?: Date()),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                val currentItem = mediaList.getOrNull(pagerState.currentPage)
                
                IconButton(onClick = {
                    currentItem?.let { item ->
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, item.uri)
                            type = if (item.type == MediaType.VIDEO) "video/*" else "image/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                
                Spacer(Modifier.weight(1f))
                
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            val item = mediaList[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.type == MediaType.VIDEO) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play Video",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(item.uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(playIntent)
                            }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("ลบรูปภาพ?", color = Color.White) },
            text = { Text("คุณต้องการลบรูปภาพนี้ใช่หรือไม่?", color = Color.White) },
            containerColor = Color(0xFF2A2A2A),
            confirmButton = {
                TextButton(onClick = {
                    val currentItem = mediaList.getOrNull(pagerState.currentPage)
                    currentItem?.let { onDelete(it) }
                    showDeleteDialog = false
                }) {
                    Text("ลบ", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ยกเลิก", color = Color.White)
                }
            }
        )
    }
}
