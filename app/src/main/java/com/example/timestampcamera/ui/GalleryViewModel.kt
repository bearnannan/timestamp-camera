package com.example.timestampcamera.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timestampcamera.data.GalleryRepository
import com.example.timestampcamera.data.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class GalleryFilter {
    ALL,
    WATERMARKED,
    ORIGINAL
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application)

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _selectedAlbum = MutableStateFlow<String?>(null) // Null = All Albums
    private val _currentFilter = MutableStateFlow(GalleryFilter.ALL)
    
    // Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode
    
    private val _selectedItems = MutableStateFlow<Set<MediaItem>>(emptySet())
    val selectedItems: StateFlow<Set<MediaItem>> = _selectedItems

    // Combined Filtering Logic
    val filteredMedia: StateFlow<List<MediaItem>> = combine(_allMedia, _selectedAlbum, _currentFilter) { media, album, filter ->
        var list = media
        
        // 1. Filter by Album (Bucket)
        if (album != null) {
            list = list.filter { it.bucketName == album }
        }
        
        // 2. Filter by Type (Original vs Watermarked)
        list = when (filter) {
            GalleryFilter.ALL -> list
            GalleryFilter.ORIGINAL -> list.filter { it.name.contains("_original", ignoreCase = true) }
            GalleryFilter.WATERMARKED -> list.filter { !it.name.contains("_original", ignoreCase = true) }
        }
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Available Albums
    val albums: StateFlow<List<String>> = _allMedia.combine(_allMedia) { media, _ -> 
        media.map { it.bucketName }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFilter: StateFlow<GalleryFilter> = _currentFilter
    val selectedAlbum: StateFlow<String?> = _selectedAlbum

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _allMedia.value = repository.getRecentMedia()
        }
    }
    
    fun setFilter(filter: GalleryFilter) {
        _currentFilter.value = filter
    }
    
    fun selectAlbum(albumName: String?) {
        _selectedAlbum.value = albumName
    }
    
    // Selection Logic
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedItems.value = emptySet()
        }
    }
    
    fun toggleItemSelection(item: MediaItem) {
        if (!_isSelectionMode.value) return
        
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _selectedItems.value = current
        
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }
    
    fun selectAll() {
        _selectedItems.value = filteredMedia.value.toSet()
    }
    
    fun deleteSelected() {
        viewModelScope.launch {
            val itemsToDelete = _selectedItems.value
            itemsToDelete.forEach { item ->
                try {
                    repository.deleteMedia(item)
                } catch (e: Exception) {
                    Log.e("GalleryViewModel", "Error deleting ${item.name}", e)
                }
            }
            _selectedItems.value = emptySet()
            _isSelectionMode.value = false
            loadMedia()
        }
    }
    
    fun shareSelected(context: android.content.Context) {
        val items = _selectedItems.value.toList()
        if (items.isEmpty()) return
        
        val uris = ArrayList(items.map { it.uri })
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*" // Allow mixed types
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }
}
