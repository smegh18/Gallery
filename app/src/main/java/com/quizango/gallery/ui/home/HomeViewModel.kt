package com.quizango.gallery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortOrder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File

// Pager pages (2.7.2 HOME_TAB_ORDER [1,0,2]): Photos first, and the default.
const val PAGE_PHOTOS = 0
const val PAGE_ALBUMS = 1
const val PAGE_EXPLORE = 2

/** 2.7.2 Photos grid: minSpanCount 3, maxSpanCount 5. */
internal fun clampColumnCount(n: Int) = n.coerceIn(3, 5)

/** True when every file in [paths] already sits directly in [folder]. */
internal fun allInFolder(paths: Collection<String>, folder: String): Boolean =
    paths.all { File(it).parentFile == File(folder) }

/** Marks the pinned device folders; virtual albums are never pinned. */
internal fun withPins(albums: List<Album>, pinned: Set<String>): List<Album> =
    albums.map { it.copy(isPinned = albumIsSelectable(it) && it.path in pinned) }

/** The picked cover of a folder, applied only while that file is still in the folder (a stale entry shows the default). */
internal fun withCovers(albums: List<Album>, covers: Map<String, String>, media: List<MediaItem>): List<Album> =
    albums.map { album ->
        val picked = covers[album.path]?.let { path -> media.firstOrNull { it.filePath == path && it.folderPath == album.path } }
        if (picked == null) album
        else album.copy(coverPath = picked.filePath, coverId = picked.id, isVideoCover = picked.mediaType == MediaType.VIDEO)
    }

/** Every file whose folder is one of the selected albums. */
internal fun albumMediaPaths(selected: Set<String>, media: List<MediaItem>): List<String> =
    media.filter { it.folderPath in selected }.map { it.filePath }

data class HomeUiState(
    val selectedTab: Int = PAGE_PHOTOS, // PAGE_PHOTOS (default), PAGE_ALBUMS, PAGE_EXPLORE
    val mediaList: List<MediaItem> = emptyList(),
    val albumsList: List<Album> = emptyList(),
    val favoriteAlbum: Album = favoritesCard(emptyList()),
    val deletedAlbum: Album = deletedCard(emptyList()),
    val columnCount: Int = 3,
    val pinchTipSeen: Boolean = true, // true until the preference loads, so the tutorial never flashes
    val sessionCount: Int = 0,
    val vaultHintSeen: Boolean = true, // true until the preference loads, so the hint never flashes
    val vaultHintClosedSession: Int = 0,
    val junkCleanedToday: Boolean = false,
    val collageNewSeen: Boolean = true, // true until the preference loads, so the badge never flashes
    val selectedPaths: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedAlbums: Set<String> = emptySet(), // folder paths of the selected device folders
    val isSyncing: Boolean = false,
    val initialSyncDone: Boolean = false, // stays true once the first MediaStore sync returns
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
                _uiState.update { it.copy(columnCount = clampColumnCount(count)) }
            }
        }

        viewModelScope.launch {
            preferences.isPinchInOutToolTip.collect { seen ->
                _uiState.update { it.copy(pinchTipSeen = seen) }
            }
        }

        combine(preferences.sessionCount, preferences.isVaultHintToolTip, preferences.vaultHintClosedSession) { session, seen, closed ->
            _uiState.update { it.copy(sessionCount = session, vaultHintSeen = seen, vaultHintClosedSession = closed) }
        }.launchIn(viewModelScope)

        combine(preferences.isJunkCleanedForToday, preferences.collageMakerNewButtonExplore) { junk, seen ->
            _uiState.update { it.copy(junkCleanedToday = junk, collageNewSeen = seen) }
        }.launchIn(viewModelScope)

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
            combine(
                mediaRepository.getAlbumsFlow(), preferences.pinnedAlbums, preferences.albumCovers, mediaRepository.getActiveMediaFlow()
            ) { albums, pinned, covers, media -> withCovers(withPins(albums, pinned), covers, media) }.collect { albums ->
                _uiState.update { it.copy(albumsList = albums) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getFavoritesFlow().collect { items ->
                _uiState.update { it.copy(favoriteAlbum = favoritesCard(items)) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getRecycleBinFlow().collect { items ->
                _uiState.update { it.copy(deletedAlbum = deletedCard(items)) }
            }
        }

        syncMedia()
    }

    fun markCollageNewSeen() {
        viewModelScope.launch { preferences.setCollageMakerNewButtonExplore(true) }
    }

    fun syncMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                mediaRepository.syncMediaStore()
            } finally {
                _uiState.update { it.copy(isSyncing = false, initialSyncDone = true) }
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, isSelectionMode = false, selectedPaths = emptySet(), selectedAlbums = emptySet()) }
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
        val clamped = clampColumnCount(count)
        viewModelScope.launch {
            preferences.setMediaColCount(clamped)
        }
    }

    fun dismissPinchTip() {
        viewModelScope.launch { preferences.setPinchInOutToolTip(true) }
    }

    /** HA.o000O0O: closed for this session only. */
    fun closeVaultHint() {
        val session = _uiState.value.sessionCount
        _uiState.update { it.copy(vaultHintClosedSession = session) }
        viewModelScope.launch { preferences.setVaultHintClosedSession(session) }
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

    private var mediaSelectionGen = 0
    private var albumSelectionGen = 0

    fun selectAll() {
        _uiState.update { state ->
            val allPaths = state.mediaList.map { it.filePath }.toSet()
            state.copy(selectedPaths = allPaths, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        mediaSelectionGen++
        _uiState.update { it.copy(selectedPaths = emptySet(), isSelectionMode = false) }
    }

    fun toggleAlbum(path: String) {
        _uiState.update { it.copy(selectedAlbums = if (path in it.selectedAlbums) it.selectedAlbums - path else it.selectedAlbums + path) }
    }

    fun selectAllAlbums() {
        _uiState.update { state -> state.copy(selectedAlbums = state.albumsList.filter(::albumIsSelectable).map { it.path }.toSet()) }
    }

    fun clearAlbumSelection() {
        albumSelectionGen++
        _uiState.update { it.copy(selectedAlbums = emptySet()) }
    }

    /** Pin to top / Unpin (HA.o0OO00O): acts for exactly one album, then leaves selection (AF.OooOOoo). */
    fun pinSelectedAlbum() {
        val state = _uiState.value
        val path = state.selectedAlbums.singleOrNull() ?: return
        val pinned = state.albumsList.firstOrNull { it.path == path }?.isPinned == true
        viewModelScope.launch { preferences.setAlbumsPinned(setOf(path), !pinned) }
        clearAlbumSelection()
    }

    /** Change cover acts on the first selected album, and only when exactly one is selected. */
    fun changeCoverTarget(): Album? =
        _uiState.value.let { st -> st.selectedAlbums.singleOrNull()?.let { path -> st.albumsList.firstOrNull { it.path == path } } }

    fun setSelectedAlbumCover(item: MediaItem) {
        val folder = changeCoverTarget()?.path ?: return
        viewModelScope.launch { preferences.setAlbumCover(folder, item.filePath) }
        clearAlbumSelection()
    }

    /** Rename acts only when exactly one album is selected (2.7.2 AF returns for more). Keys follow the folder on success. */
    fun renameSelectedAlbum(newName: String, onResult: (Boolean) -> Unit) {
        val album = changeCoverTarget() ?: return onResult(false)
        viewModelScope.launch {
            val newPath = mediaRepository.renameFolder(album.path, newName)
            if (newPath != null) {
                preferences.moveAlbumKeys(album.path, newPath)
                clearAlbumSelection()
            }
            onResult(newPath != null)
        }
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
        val mediaGen = mediaSelectionGen
        viewModelScope.launch {
            items.forEach { item ->
                mediaRepository.setFavorite(item.filePath, targetFav)
            }
            if (mediaGen == mediaSelectionGen) clearSelection()
        }
    }

    /** Runs after the delete confirmation; the checkbox state reaches here only through deleteActionFor. */
    internal fun deleteSelected(action: DeleteAction) {
        val paths = _uiState.value.selectedPaths.toList()
        val mediaGen = mediaSelectionGen
        viewModelScope.launch {
            when (action) {
                DeleteAction.MOVE_TO_RECYCLE_BIN -> paths.forEach { mediaRepository.moveToTrash(it) }
                DeleteAction.DELETE_PERMANENTLY -> mediaRepository.deletePermanently(paths)
            }
            if (mediaGen == mediaSelectionGen) clearSelection()
        }
    }

    /**
     * Copies or moves the selection into [folderPath] and reports how many succeeded. Returns false, doing nothing,
     * for a move into the folder that already holds every selected item (S272/ca/br Oooo0O0).
     */
    fun addSelectedToFolder(folderPath: String, move: Boolean, onDone: (Int) -> Unit): Boolean {
        val paths = _uiState.value.selectedPaths.toList()
        if (move && allInFolder(paths, folderPath)) return false
        val mediaGen = mediaSelectionGen
        val albumGen = albumSelectionGen
        viewModelScope.launch { copyOrMove(paths, folderPath, move, mediaGen, albumGen, onDone) }
        return true
    }

    /** Same for every file of the selected albums; refuses a move when the only selected album is the target. */
    fun addSelectedAlbumsToFolder(folderPath: String, move: Boolean, onDone: (Int) -> Unit): Boolean {
        val albums = _uiState.value.selectedAlbums
        if (move && albums.all { it == folderPath }) return false
        val mediaGen = mediaSelectionGen
        val albumGen = albumSelectionGen
        viewModelScope.launch {
            copyOrMove(albumMediaPaths(albums, mediaRepository.getActiveMediaFlow().first()), folderPath, move, mediaGen, albumGen, onDone)
        }
        return true
    }

    private suspend fun copyOrMove(
        paths: List<String>,
        folderPath: String,
        move: Boolean,
        mediaGen: Int,
        albumGen: Int,
        onDone: (Int) -> Unit
    ) {
        val done = paths.count {
            if (move) mediaRepository.moveMediaToFolder(it, folderPath) else mediaRepository.copyMediaToFolder(it, folderPath)
        }
        mediaRepository.syncMediaStore()
        if (mediaGen == mediaSelectionGen) clearSelection()
        if (albumGen == albumSelectionGen) clearAlbumSelection()
        onDone(done)
    }

    /** Album Delete, after the confirmation: every file of the selected folders, to the Recycle Bin or for good. */
    internal fun deleteSelectedAlbums(action: DeleteAction) {
        val albums = _uiState.value.selectedAlbums
        val albumGen = albumSelectionGen
        viewModelScope.launch {
            val paths = albumMediaPaths(albums, mediaRepository.getActiveMediaFlow().first())
            when (action) {
                DeleteAction.MOVE_TO_RECYCLE_BIN -> paths.forEach { mediaRepository.moveToTrash(it) }
                DeleteAction.DELETE_PERMANENTLY -> mediaRepository.deletePermanently(paths)
            }
            if (albumGen == albumSelectionGen) clearAlbumSelection()
        }
    }

    fun moveSelectedToVault() {
        val paths = _uiState.value.selectedPaths
        val items = _uiState.value.mediaList.filter { it.filePath in paths }
        val mediaGen = mediaSelectionGen
        viewModelScope.launch {
            items.forEach { item ->
                vaultRepository.moveToVault(item)
            }
            if (mediaGen == mediaSelectionGen) clearSelection()
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
