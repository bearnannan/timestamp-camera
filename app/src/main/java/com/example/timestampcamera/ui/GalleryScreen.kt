package com.example.timestampcamera.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.timestampcamera.data.MediaItem
import com.example.timestampcamera.data.MediaType
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

enum class GalleryViewMode {
    GRID, PAGER
}

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val mediaList by viewModel.filteredMedia.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    var viewMode by remember { mutableStateOf(GalleryViewMode.GRID) }
    var initialPagerIndex by remember { mutableIntStateOf(0) }
    
    // Refresh Gallery when screen is entered
    LaunchedEffect(Unit) {
        viewModel.loadMedia()
    }

    // Handle Back Press
    BackHandler {
        if (viewMode == GalleryViewMode.PAGER) {
            viewMode = GalleryViewMode.GRID
        } else if (isSelectionMode) {
            viewModel.toggleSelectionMode()
        } else {
            onBack()
        }
    }

    Crossfade(targetState = viewMode, label = "GalleryTransition") { mode ->
        when (mode) {
            GalleryViewMode.GRID -> {
                GalleryGridView(
                    mediaList = mediaList,
                    albums = albums,
                    selectedAlbum = selectedAlbum,
                    currentFilter = currentFilter,
                    isSelectionMode = isSelectionMode,
                    selectedItems = selectedItems,
                    onBack = {
                        if (isSelectionMode) viewModel.toggleSelectionMode() else onBack()
                    },
                    onMediaClick = { item, index ->
                        if (isSelectionMode) {
                            viewModel.toggleItemSelection(item)
                        } else {
                            initialPagerIndex = index
                            viewMode = GalleryViewMode.PAGER
                        }
                    },
                    onMediaLongClick = { item ->
                         if (!isSelectionMode) {
                             viewModel.toggleSelectionMode()
                             viewModel.toggleItemSelection(item)
                         }
                    },
                    onAlbumSelect = { viewModel.selectAlbum(it) },
                    onFilterSelect = { viewModel.setFilter(it) },
                    onSelectionModeToggle = { viewModel.toggleSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                    onShareSelected = { context -> viewModel.shareSelected(context) },
                    onSelectAll = { viewModel.selectAll() }
                )
            }
            GalleryViewMode.PAGER -> {
                GalleryPagerView(
                    mediaList = mediaList,
                    initialIndex = initialPagerIndex,
                    onBack = { viewMode = GalleryViewMode.GRID }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryGridView(
    mediaList: List<MediaItem>,
    albums: List<String>,
    selectedAlbum: String?,
    currentFilter: GalleryFilter,
    isSelectionMode: Boolean,
    selectedItems: Set<MediaItem>,
    onBack: () -> Unit,
    onMediaClick: (MediaItem, Int) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onAlbumSelect: (String?) -> Unit,
    onFilterSelect: (GalleryFilter) -> Unit,
    onSelectionModeToggle: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: (android.content.Context) -> Unit,
    onSelectAll: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(Modifier.background(Color.Black)) {
                TopAppBar(
                    title = { 
                        if (isSelectionMode) {
                            Text("${selectedItems.size} Selected", color = Color.White)
                        } else {
                            Text("Gallery", color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = onSelectAll) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                            }
                            IconButton(onClick = { onShareSelected(context) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        } else {
                            IconButton(onClick = onSelectionModeToggle) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Select", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
                
                // Filters Row
                if (!isSelectionMode) {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        // 1. Albums (Horizontal Scroll)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedAlbum == null,
                                    onClick = { onAlbumSelect(null) },
                                    label = { Text("All Albums") }
                                )
                            }
                            items(albums) { album ->
                                FilterChip(
                                    selected = selectedAlbum == album,
                                    onClick = { onAlbumSelect(album) },
                                    label = { Text(album) }
                                )
                            }
                        }
                        
                        // 2. Types (Horizontal Scroll)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                             item {
                                FilterChip(
                                    selected = currentFilter == GalleryFilter.ALL,
                                    onClick = { onFilterSelect(GalleryFilter.ALL) },
                                    label = { Text("All Photos") }
                                )
                            }
                             item {
                                FilterChip(
                                    selected = currentFilter == GalleryFilter.WATERMARKED,
                                    onClick = { onFilterSelect(GalleryFilter.WATERMARKED) },
                                    label = { Text("Watermarked") }
                                )
                            }
                             item {
                                FilterChip(
                                    selected = currentFilter == GalleryFilter.ORIGINAL,
                                    onClick = { onFilterSelect(GalleryFilter.ORIGINAL) },
                                    label = { Text("Originals") }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No photos found in this album.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(mediaList) { index, item ->
                    val isSelected = selectedItems.contains(item)
                    
                    Box(modifier = Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onMediaClick(item, index) },
                            onLongClick = { onMediaLongClick(item) }
                        )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .size(300) // Thumbnail size optimization
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Selection Overlay
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (isSelected) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Select",
                                    tint = if (isSelected) Color.Green else Color.White,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                        
                        if (item.type == MediaType.VIDEO) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... Pager View with Info Sheet Support ...
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryPagerView(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaList.size })
    var showInfoSheet by remember { mutableStateOf(false) }
    val currentItem = mediaList.getOrNull(pagerState.currentPage)
    val context = LocalContext.current
    
    // Metadata State
    var photoMetadata by remember { mutableStateOf<com.example.timestampcamera.util.PhotoMetadata?>(null) }
    
    // Fetch Metadata when Info Sheet is opened
    LaunchedEffect(showInfoSheet, currentItem) {
        if (showInfoSheet && currentItem != null) {
            // Assuming image for now. For video, we might show basic file info.
             if (currentItem.type == MediaType.VIDEO) {
                 // Basic video info (can be improved later)
                 photoMetadata = com.example.timestampcamera.util.PhotoMetadata(
                     fileName = currentItem.name,
                     dateTaken = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(currentItem.dateTaken)
                 )
             } else {
                 val meta = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                     com.example.timestampcamera.util.ExifUtils.getExifMetadata(context, currentItem.uri)
                 }
                 photoMetadata = meta
             }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                     Column {
                        Text(
                            text = if (currentItem != null) SimpleDateFormat("dd/MM/yy HH:mm", Locale.US).format(currentItem.dateTaken) else "",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        if (currentItem != null && currentItem.bucketName.isNotEmpty()) {
                            Text(currentItem.bucketName, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { showInfoSheet = true }) {
                         Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            pageSpacing = 16.dp
        ) { page ->

            val item = mediaList[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (item.type == MediaType.VIDEO) {
                    // Use the new In-App Video Player
                    VideoPlayer(
                        uri = item.uri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Use the Zoomable Image Viewer
                    ZoomableImage(
                        uri = item.uri,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        if (showInfoSheet && currentItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = Color(0xFF1E1E1E)
            ) {
                GalleryInfoContent(
                    item = currentItem, 
                    metadata = photoMetadata
                )
            }
        }
    }
}

@Composable
fun GalleryInfoContent(
    item: MediaItem, 
    metadata: com.example.timestampcamera.util.PhotoMetadata?
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .navigationBarsPadding() // Handled by bottom sheet usually, but safety
    ) {
        // Header
        Text(
            text = "Details", 
            color = Color.White, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        if (metadata != null) {
            // 1. File Info Section
            SectionHeader("File Info")
            InfoRow("Name", metadata.fileName)
            InfoRow("Date", metadata.dateTaken)
            InfoRow("Size", metadata.fileSize)
            InfoRow("Resolution", metadata.resolution)
            
            // 2. Camera Info Section (Only for Images)
            if (item.type == MediaType.IMAGE) {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Camera")
                InfoRow("Device", metadata.model)
                if (metadata.aperture.isNotEmpty()) {
                    // Combine Exposure settings into one row? or separate?
                    // Let's do separate for clarity
                    InfoRow("Aperture", metadata.aperture)
                    InfoRow("Shutter", metadata.shutterSpeed)
                    InfoRow("ISO", metadata.iso)
                }
            }
            
            // 3. Location Section
            if (metadata.hasLocation && metadata.latitude != null && metadata.longitude != null) {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Location")
                InfoRow("Coordinates", String.format("%.5f, %.5f", metadata.latitude, metadata.longitude))
                
                // Optional: Button to open map
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val uri = android.net.Uri.parse("geo:${metadata.latitude},${metadata.longitude}?q=${metadata.latitude},${metadata.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            // Fallback to browser
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on Map")
                }
            }
        } else {
             // Loading State
             Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Color(0xFFFF9800))
             }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFFF9800), // Orange Accent
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label, 
            color = Color.Gray, 
            fontSize = 15.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value, 
            color = Color.White.copy(alpha = 0.9f), 
            fontSize = 15.sp, 
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
