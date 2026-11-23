package com.quizango.gallery.ui.discovery

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.recycle.calculateRemainingDays
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.home.formatDuration
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerRecycleScreen(
    mediaRepository: MediaRepository,
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recycle by mediaRepository.getRecycleBinFlow().collectAsState(initial = emptyList())
    val ownedPaths by appPreferences.organizerBinPaths.collectAsState(initial = emptySet())
    val autoDeleteEnabled by appPreferences.autoDeleteTrash.collectAsState(initial = false)
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var deleteTargets by remember { mutableStateOf(emptyList<String>()) }
    var restoreTargets by remember { mutableStateOf(emptyList<String>()) }
    var busy by remember { mutableStateOf(false) }
    val items = remember(recycle, ownedPaths) { recycle.filter { it.filePath in ownedPaths } }
    val paths = remember(items) { items.mapTo(LinkedHashSet()) { it.filePath } }
    val isSelecting = selected.isNotEmpty()

    LaunchedEffect(recycle, ownedPaths) {
        appPreferences.reconcileOrganizerBinPaths(recycle.mapTo(HashSet()) { it.filePath })
        selected = selected intersect items.mapTo(HashSet()) { it.filePath }
    }
    BackHandler {
        if (isSelecting) selected = emptySet() else onNavigateBack()
    }
    fun finishMutation(outcomes: List<PathMutationOutcome>, removeOwnership: Boolean) {
        val succeeded = outcomes.filter { it.succeeded }.mapTo(LinkedHashSet()) { it.path }
        if (removeOwnership && succeeded.isNotEmpty()) scope.launch { appPreferences.removeOrganizerBinPaths(succeeded) }
        selected = selected - succeeded
        val failed = outcomes.filterNot { it.succeeded }
        if (failed.isNotEmpty()) Toast.makeText(context, context.getString(R.string.organizer_move_failed), Toast.LENGTH_SHORT).show()
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(colorResource(R.color.backgroundColor))) {
            SharedMediaToolbar(
                title = if (isSelecting) stringResource(R.string.items_selected, selected.size) else stringResource(R.string.recycle_bin),
                selecting = isSelecting,
                allSelected = paths.isNotEmpty() && selected.containsAll(paths),
                onBack = onNavigateBack,
                onClose = { selected = emptySet() },
                onToggleAll = { selected = if (paths.isNotEmpty() && selected.containsAll(paths)) emptySet() else paths }
            )
            if (autoDeleteEnabled && items.isNotEmpty()) {
                Text(stringResource(R.string.auto_delete_trash_30_days), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), color = colorResource(R.color.titleColor))
            }
            if (items.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(R.drawable.ic_empty_img), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(90.dp))
                    Text(stringResource(R.string.no_items), color = colorResource(R.color.titleColor))
                    Text(stringResource(R.string.deleting_files), color = colorResource(R.color.fastScrollerColor))
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(items, key = { it.filePath }) { item ->
                        OrganizerRecycleCell(item, selected.contains(item.filePath), isSelecting, autoDeleteEnabled,
                            onClick = {
                                if (isSelecting) selected = if (item.filePath in selected) selected - item.filePath else selected + item.filePath
                                else onNavigate(Screen.MediaViewer.createRoute(item.filePath, "organizer:recycle", isRecycle = true))
                            },
                            onLongClick = { selected = selected + item.filePath }
                        )
                    }
                }
            }
            if (isSelecting) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 19.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = {
                        restoreTargets = selected.toList(); showRestoreDialog = true
                    }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.restore), color = GalleryBlue) }
                    Button(enabled = !busy, onClick = { deleteTargets = selected.toList(); showDeleteDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.cleaner_red))) { Text(stringResource(R.string.delete), color = Color.White) }
                }
            }
        }
    }
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.restore_items)) },
            text = { Text(stringResource(R.string.restore_items)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                val snapshot = restoreTargets
                showRestoreDialog = false; busy = true
                scope.launch { try { finishMutation(mediaRepository.restoreFromTrash(snapshot), true) } finally { busy = false } }
            }) { Text(stringResource(R.string.restore), color = GalleryBlue) } },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.organizer_dialog_title)) },
            text = { Text(stringResource(R.string.organizer_deleting_select_item)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                val snapshot = deleteTargets
                showDeleteDialog = false; busy = true
                scope.launch { try { finishMutation(mediaRepository.deletePermanently(snapshot), true) } finally { busy = false } }
            }) { Text(stringResource(R.string.permanently_delete), color = colorResource(R.color.cleaner_red)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun OrganizerRecycleCell(item: MediaItem, selected: Boolean, selecting: Boolean, showDays: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(1.5.dp).clip(RoundedCornerShape(3.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        AsyncImage(model = File(item.filePath), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (item.mediaType == MediaType.VIDEO) Row(Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_media_video), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(formatDuration(item.durationMs), color = Color.White, modifier = Modifier.padding(start = 3.dp))
        }
        if (showDays) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
            Text("${calculateRemainingDays(item.deletedDate)} ${stringResource(R.string.days)}", color = Color.White)
        }
        if (selecting) {
            Box(Modifier.fillMaxSize().background(if (selected) GalleryBlue.copy(alpha = .25f) else Color.Transparent))
            Icon(painterResource(if (selected) R.drawable.circle_checked else R.drawable.circle_unchecked), contentDescription = null, tint = if (selected) GalleryBlue else Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp))
        }
    }
}
