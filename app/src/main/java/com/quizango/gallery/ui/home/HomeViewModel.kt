package com.quizango.gallery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortOrder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class HomeUiState(
    val selectedTab: Int = 0, // 0 = Albums (Default), 1 = Photos (All Media), 2 = Explore
    val mediaList: List<MediaItem> = emptyList(),
    val albumsList: List<Album> = emptyList(),
    val columnCount: Int = 4,
    val selectedPaths: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isSyncing: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val searchQuery: String = ""
)

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val vaultRepository: VaultRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.mediaColCount.collect { count ->
                _uiState.update { it.copy(columnCount = count) }
            }
        }

        combine(
            mediaRepository.getActiveMediaFlow(),
            _uiState.map { it.searchQuery }.distinctUntilChanged(),
            _uiState.map { it.sortOrder }.distinctUntilChanged()
        ) { media, query, sort ->
            applyFilterAndSort(media, query, sort)
        }.flowOn(Dispatchers.Default)
        .onEach { filtered ->
            _uiState.update { it.copy(mediaList = filtered) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            mediaRepository.getAlbumsFlow().collect { albums ->
                _uiState.update { it.copy(albumsList = albums) }
            }
        }

        syncMedia()
    }

    fun syncMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                mediaRepository.syncMediaStore()
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, isSelectionMode = false, selectedPaths = emptySet()) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun setColumnCount(count: Int) {
        val clamped = count.coerceIn(2, 5)
        viewModelScope.launch {
            preferences.setMediaColCount(clamped)
        }
    }

    fun toggleSelection(filePath: String) {
        _uiState.update { state ->
            val updated = if (state.selectedPaths.contains(filePath)) {
                state.selectedPaths - filePath
            } else {
                state.selectedPaths + filePath
            }
            state.copy(
                selectedPaths = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPaths = state.mediaList.map { it.filePath }.toSet()
            state.copy(selectedPaths = allPaths, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPaths = emptySet(), isSelectionMode = false) }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.setFavorite(item.filePath, !item.isFavorite)
        }
    }

    fun toggleFavoriteSelected() {
        val paths = _uiState.value.selectedPaths
        val items = _uiState.value.mediaList.filter { it.filePath in paths }
        val targetFav = items.any { !it.isFavorite }
        viewModelScope.launch {
            items.forEach { item ->
                mediaRepository.setFavorite(item.filePath, targetFav)
            }
            clearSelection()
        }
    }

    fun moveSelectedToTrash() {
        val paths = _uiState.value.selectedPaths.toList()
        viewModelScope.launch {
            paths.forEach { path ->
                mediaRepository.moveToTrash(path)
            }
            clearSelection()
        }
    }

    fun moveSelectedToVault() {
        val paths = _uiState.value.selectedPaths
        val items = _uiState.value.mediaList.filter { it.filePath in paths }
        viewModelScope.launch {
            items.forEach { item ->
                vaultRepository.moveToVault(item)
            }
            clearSelection()
        }
    }

    private fun applyFilterAndSort(list: List<MediaItem>, query: String, sortOrder: SortOrder): List<MediaItem> {
        val filtered = if (query.isBlank()) list else list.filter {
            it.fileName.contains(query, ignoreCase = true) || it.folderName.contains(query, ignoreCase = true)
        }

        return when (sortOrder.field) {
            SortField.DATE_MODIFIED, SortField.DATE_TAKEN -> {
                if (sortOrder.direction == SortDirection.DESCENDING)
                    filtered.sortedByDescending { it.dateAdded }
                else
                    filtered.sortedBy { it.dateAdded }
            }
            SortField.NAME -> {
                if (sortOrder.direction == SortDirection.DESCENDING)
                    filtered.sortedByDescending { it.fileName.lowercase() }
                else
                    filtered.sortedBy { it.fileName.lowercase() }
            }
            SortField.SIZE -> {
                if (sortOrder.direction == SortDirection.DESCENDING)
                    filtered.sortedByDescending { it.fileSize }
                else
                    filtered.sortedBy { it.fileSize }
            }
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val vaultRepository: VaultRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(mediaRepository, vaultRepository, preferences) as T
        }
    }
}
