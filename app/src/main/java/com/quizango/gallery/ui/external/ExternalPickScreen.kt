package com.quizango.gallery.ui.external

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.SharedMediaCell
import com.quizango.gallery.ui.browse.sharedHeaderDate
import com.quizango.gallery.ui.browse.sharedMediaRows
import com.quizango.gallery.ui.browse.sharedRowKey
import com.quizango.gallery.ui.browse.sharedRowSpan
import com.quizango.gallery.ui.home.HeaderKind
import com.quizango.gallery.ui.home.PhotoRow
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.headerKind
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/** Which media an outside GET_CONTENT / PICK asked for (its mime type), and whether it takes several. */
internal data class PickRequest(val kind: PickKind, val multiple: Boolean)

internal enum class PickKind { IMAGES, VIDEOS, ALL }

/** Image types (and vnd.android.cursor.dir/image) pick photos, the video forms videos, anything else both. */
internal fun pickKind(mimeType: String?): PickKind {
    val type = mimeType.orEmpty().lowercase()
    return when {
        type.startsWith("image/") || type == "vnd.android.cursor.dir/image" -> PickKind.IMAGES
        type.startsWith("video/") || type == "vnd.android.cursor.dir/video" -> PickKind.VIDEOS
        else -> PickKind.ALL
    }
}

/** Only MediaStore rows can be handed out as content URIs; vault or plain files are never returned. */
internal fun pickable(item: MediaItem, kind: PickKind): Boolean = item.id > 0 && when (kind) {
    PickKind.IMAGES -> item.mediaType != MediaType.VIDEO
    PickKind.VIDEOS -> item.mediaType == MediaType.VIDEO
    PickKind.ALL -> true
}

/**
 * ExternalSelectMediaActivity (activity_select with layout_toolbar): close, "Choose media" and, when the caller allows
 * several, the done tick; a 4-column grid with day headers. One tap returns a single pick; [onPicked] gets the
 * MediaStore URIs (s9/ao.OooOOOo), or an empty list for a cancel.
 */
@Composable
internal fun ExternalPickScreen(request: PickRequest, mediaRepository: MediaRepository, onPicked: (List<Uri>) -> Unit) {
    val context = LocalContext.current
    val all by remember { mediaRepository.getActiveMediaFlow() }.collectAsState(initial = null)
    val items = remember(all, request.kind) { all?.filter { pickable(it, request.kind) } }
    var picked by remember { mutableStateOf(emptyList<MediaItem>()) }
    var synced by remember { mutableStateOf(!checkStoragePermissions(context)) }
    // Opened straight from another app, the media index may not be built yet
    LaunchedEffect(Unit) {
        if (!synced) runCatching { mediaRepository.syncMediaStore() }
        synced = true
    }

    fun uriOf(item: MediaItem) = item.contentUri as Uri

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        PickToolbar(
            multiple = request.multiple,
            selectedCount = picked.size,
            onClose = { onPicked(emptyList()) },
            onDone = {
                if (picked.isEmpty()) Toast.makeText(context, R.string.please_select_items, Toast.LENGTH_SHORT).show()
                else onPicked(picked.map(::uriOf))
            }
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val list = items
            when {
                list == null || (list.isEmpty() && !synced) -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                list.isEmpty() -> PhotosEmptyState()
                else -> {
                    val rows = remember(list) { sharedMediaRows(list) }
                    val now = remember(list) { System.currentTimeMillis() }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4), // GridLayoutManager(ctx, 4)
                        contentPadding = PaddingValues(horizontal = 2.sdp, vertical = 4.sdp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = rows.size,
                            key = { sharedRowKey(rows[it]) },
                            span = { GridItemSpan(sharedRowSpan(rows[it], maxLineSpan)) }
                        ) { i ->
                            when (val row = rows[i]) {
                                PhotoRow.Ad -> Unit
                                is PhotoRow.Header -> Text(
                                    text = sharedHeaderDate(row.dayStart, now) ?: stringResource(
                                        if (headerKind(row.dayStart, now) == HeaderKind.TODAY) R.string.today else R.string.yesterday
                                    ),
                                    style = GalleryText.BodySmallMedium,
                                    color = colorResource(R.color.black),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.sdp, vertical = 6.sdp)
                                )
                                is PhotoRow.Media -> {
                                    val item = row.item
                                    val tap = {
                                        if (!request.multiple) onPicked(listOf(uriOf(item)))
                                        else picked = if (item in picked) picked - item else picked + item
                                    }
                                    Box(Modifier.clickable(onClick = tap)) {
                                        SharedMediaCell(
                                            item = item,
                                            isSelected = item in picked,
                                            selecting = request.multiple,
                                            onClick = tap,
                                            onLongClick = tap
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** layout_toolbar as ExternalSelectMediaActivity sets it: closeBtn, title, selectBtn only for multiple picks. */
@Composable
private fun PickToolbar(multiple: Boolean, selectedCount: Int, onClose: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(2.sdp)
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(55.dp).padding(start = 4.sdp, end = 10.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                modifier = Modifier
                    .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), onClick = onClose)
                    .padding(6.sdp)
            )
            Text(
                text = if (selectedCount > 0) stringResource(R.string.items_selected, selectedCount) else stringResource(R.string.select_items),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.sdp)
            )
            if (multiple) Image(
                painter = painterResource(R.drawable.ic_done),
                contentDescription = stringResource(R.string.done),
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, ripple(bounded = false), onClick = onDone)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.actionbarDividerColor))
    }
}
