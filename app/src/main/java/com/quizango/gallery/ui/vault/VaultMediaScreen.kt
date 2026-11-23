package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.MediaSelectionBar
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.browse.isAllSelected
import com.quizango.gallery.ui.browse.sharedHeaderDate
import com.quizango.gallery.ui.browse.toggleAll
import com.quizango.gallery.ui.browse.vaultMediaActions
import com.quizango.gallery.ui.home.DeleteMediaDialog
import com.quizango.gallery.ui.home.HeaderKind
import com.quizango.gallery.ui.home.HideMediaDialog
import com.quizango.gallery.ui.home.ProgressTextDialog
import com.quizango.gallery.ui.home.dayStartOf
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.home.fileTypeLabel
import com.quizango.gallery.ui.home.formatDuration
import com.quizango.gallery.ui.home.headerKind
import com.quizango.gallery.ui.home.shareSelectedMedia
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Lives on the vault's back-stack entry: it is cleared when the entry is popped, not when the viewer, slideshow or
 * picker opens on top of it or the screen rotates, so decrypted copies live exactly as long as the vault (D6).
 */
private class VaultViewCache(private val repository: VaultRepository) : ViewModel() {
    override fun onCleared() {
        CoroutineScope(Dispatchers.IO).launch { repository.clearViewCache() }
    }
}

/** custom_toast_layout (§7): shield on `ic_toast_bg`; a Compose overlay because custom-view system toasts are deprecated. */
@Composable
private fun SecuredToast(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.drawableBackground(R.drawable.ic_toast_bg).padding(8.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_secure), contentDescription = null, modifier = Modifier.padding(end = 5.sdp))
        Text(
            text = stringResource(R.string.toast_private),
            style = GalleryText.BodyMediumMedium,
            color = colorResource(R.color.onlyWhite)
        )
    }
}

/** activity_vault_media (#33): toolbar, day-grouped 3-column grid of decrypted thumbnails, selection bar, empty state. */
@Composable
fun VaultMediaScreen(
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as GalleryApplication
    val mediaRepository = app.mediaRepository
    viewModel { VaultViewCache(vaultRepository) }
    // null until the first query returns, so the empty copy never flashes first
    val vaultItems by remember { vaultRepository.getVaultItemsFlow() }.collectAsState(initial = null)
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var pendingUnhide by remember { mutableStateOf<List<VaultItem>?>(null) }
    var pendingDelete by remember { mutableStateOf<List<VaultItem>?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var preparingShare by remember { mutableStateOf(false) }

    val selecting = selectedPaths.isNotEmpty()
    val paths = remember(vaultItems) { vaultItems?.map { it.filePath }.orEmpty() }
    fun toast(id: Int) = Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show()
    fun openPicker() = onNavigate(Screen.VaultAdd.route)

    BackHandler(enabled = selecting) { selectedPaths = emptySet() }

    // First open after setup: clear the flag before showing so it appears once, whatever happens next
    var showSecured by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (app.preferences.isPrivateToastShow.first()) {
            app.preferences.setPrivateToastShow(false)
            showSecured = true
            delay(3500) // Toast.LENGTH_LONG
            showSecured = false
        }
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            SharedMediaToolbar(
                title = if (selecting) stringResource(R.string.items_selected, selectedPaths.size) else stringResource(R.string.private_vault),
                selecting = selecting,
                allSelected = isAllSelected(selectedPaths, paths),
                onBack = onNavigateBack,
                onClose = { selectedPaths = emptySet() },
                onToggleAll = { selectedPaths = toggleAll(selectedPaths, paths) },
                onSettings = onNavigateToSettings
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val items = vaultItems
                when {
                    items == null -> Unit // the spinner layer below covers the first query
                    items.isEmpty() -> VaultEmptyState(onAdd = ::openPicker)
                    else -> {
                        VaultGrid(
                            items = items,
                            vaultRepository = vaultRepository,
                            selected = selectedPaths,
                            onOpen = { onNavigate(Screen.MediaViewer.createRoute(it.filePath, isVault = true)) },
                            onSelectedChange = { selectedPaths = it }
                        )
                        // addVaultButton: only while items exist
                        Image(
                            painter = painterResource(R.drawable.ic_add_icon),
                            contentDescription = stringResource(R.string.submit),
                            colorFilter = ColorFilter.tint(colorResource(R.color.onlyWhite)),
                            contentScale = ContentScale.Inside,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.sdp, bottom = 36.sdp)
                                .shadow(6.sdp, CircleShape)
                                .background(colorResource(R.color.dark_blue), CircleShape)
                                .size(56.sdp)
                                .clickable(onClick = ::openPicker)
                        )
                    }
                }
            }
            if (selecting) MediaSelectionBar(vaultMediaActions(), onAction = { action ->
                // actions run on a snapshot of the selection (T-07-20)
                val picked = vaultItems.orEmpty().filter { it.filePath in selectedPaths }
                if (picked.isNotEmpty()) when (action) {
                    MediaAction.SHARE -> scope.launch {
                        preparingShare = true
                        val files = picked.mapNotNull { vaultRepository.viewFile(it) }
                        preparingShare = false
                        shareSelectedMedia(context, files.map { it.absolutePath }.toSet(), context.getString(R.string.share))
                    }
                    MediaAction.UNHIDE -> pendingUnhide = picked
                    MediaAction.DELETE -> pendingDelete = picked
                    else -> Unit
                }
            })
            Spacer(Modifier.navigationBarsPadding())
        }

        // progressBar layer: blocks touches during the first query and while decrypted copies are prepared
        if (vaultItems == null || preparingShare) Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
        if (showSecured) SecuredToast(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp))
    }

    pendingUnhide?.let { items ->
        HideMediaDialog(
            title = R.string.unhide_items,
            message = R.string.unhide_item_permission,
            onConfirm = {
                pendingUnhide = null
                scope.launch {
                    progress = R.string.unhiding
                    val restored = items.count { vaultRepository.restoreFromVault(it) }
                    mediaRepository.syncMediaStore() // restore only re-indexes the file; the gallery table needs the batch sync (07-02)
                    progress = null
                    toast(if (restored > 0) R.string.selected_media_files_successfully_unhide else R.string.something_went_wrong)
                    selectedPaths = emptySet()
                }
            },
            onDismiss = { pendingUnhide = null }
        )
    }

    pendingDelete?.let { items ->
        DeleteMediaDialog(
            count = items.size,
            showMoveToBin = false, // vault files have no Recycle Bin (A6)
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    progress = R.string.deleting
                    items.forEach { vaultRepository.deleteFromVault(it) }
                    progress = null
                    toast(R.string.selected_media_files_successfully_deleted)
                    selectedPaths = emptySet()
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }

    progress?.let { ProgressTextDialog(stringResource(it)) }
}

/** emptyMessageLayout: the secure-storage sentence over the Add Items button. */
@Composable
private fun VaultEmptyState(onAdd: () -> Unit) {
    val body = GalleryText.BodyMediumRegular
    val extra = with(LocalDensity.current) { 3.sdp.toSp() } // lineSpacingExtra
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.hide_photo_private_permission),
            style = body.copy(lineHeight = TextUnit(body.fontSize.value * 1.2f + extra.value, TextUnitType.Sp)),
            color = colorResource(R.color.graySubTitle),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 20.sdp, end = 20.sdp, top = 5.sdp)
        )
        Text(
            text = stringResource(R.string.add_item),
            style = GalleryText.BodyMediumSemiBold,
            color = colorResource(R.color.onlyWhite),
            modifier = Modifier
                .padding(top = 20.sdp)
                .drawableBackground(R.drawable.btn_permission)
                .clickable(onClick = onAdd)
                .padding(horizontal = 40.sdp, vertical = 11.sdp)
        )
    }
}

/** Day headers (Phase 3 Today / Yesterday keys) over 3-column cells; tap opens, long-press selects. */
@Composable
private fun VaultGrid(
    items: List<VaultItem>,
    vaultRepository: VaultRepository,
    selected: Set<String>,
    onOpen: (VaultItem) -> Unit,
    onSelectedChange: (Set<String>) -> Unit
) {
    val now = remember(items) { System.currentTimeMillis() }
    val days = remember(items) { items.groupBy { dayStartOf(it.dateHidden) } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // A7
        contentPadding = PaddingValues(horizontal = 2.sdp, vertical = 4.sdp),
        modifier = Modifier.fillMaxSize()
    ) {
        days.forEach { (day, group) ->
            item(key = "header_$day", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = sharedHeaderDate(day, now) ?: stringResource(
                        if (headerKind(day, now) == HeaderKind.TODAY) R.string.today else R.string.yesterday
                    ),
                    style = GalleryText.BodySmallMedium,
                    color = colorResource(R.color.black),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.sdp, vertical = 6.sdp)
                )
            }
            items(group.size, key = { group[it].filePath }) { i ->
                val item = group[i]
                val isSelected = item.filePath in selected
                // Vault files are encrypted; thumbnails come from a decrypted cache copy
                val viewFile by produceState<File?>(null, item.filePath) { value = vaultRepository.viewFile(item) }
                VaultGridItem(
                    item = item,
                    viewFile = viewFile,
                    isSelected = isSelected,
                    selecting = selected.isNotEmpty(),
                    onClick = {
                        if (selected.isEmpty()) onOpen(item)
                        else onSelectedChange(if (isSelected) selected - item.filePath else selected + item.filePath)
                    },
                    onLongClick = { onSelectedChange(selected + item.filePath) }
                )
            }
        }
    }
}

/** item_grid_gallery on the shared cell tokens (square, 0.5.dp margin, check badge, video row, GIF/RAW label). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultGridItem(
    item: VaultItem,
    viewFile: File?,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val white = colorResource(R.color.onlyWhite)
    val isVideo = item.mediaType == MediaType.VIDEO
    Box(
        Modifier
            .padding(0.5.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(viewFile).size(300, 300).allowRgb565(true).build(),
            contentDescription = item.fileName,
            placeholder = painterResource(R.drawable.image_loading_albums),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isVideo) Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 3.sdp, end = 3.sdp, bottom = 3.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_media_video), contentDescription = null)
            Text(
                text = if (item.durationMs > 0) formatDuration(item.durationMs) else "00:00",
                style = GalleryText.BodyXSmallMediumGrid.copy(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)),
                color = white,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 3.sdp)
            )
        }
        fileTypeLabel(item.fileName, isVideo)?.let { label ->
            Text(
                text = stringResource(label),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 12.ssp, shadow = Shadow(Color.Black, blurRadius = 4f)),
                color = white,
                modifier = Modifier.align(Alignment.TopStart).padding(6.sdp)
            )
        }
        if (selecting) Image(
            painterResource(if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(1.sdp)
        )
    }
}
