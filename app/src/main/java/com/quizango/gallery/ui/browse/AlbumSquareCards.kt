package com.quizango.gallery.ui.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * item_grid_square_album's single-cover face (`mainLayout`, AllAlbum `bx/g` and Burst `be/b` both only show it)
 * and item_search_sub (Moments): a 10.sdp-rounded square cover, a 2-line title and the bare count (`String.valueOf`).
 * [titleTop] is 8.sdp for item_grid_square_album, 4.sdp for item_search_sub. [selectionMark] is null outside selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumSquareCard(
    title: String,
    count: Int,
    cover: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleTop: Dp = 8.sdp,
    selectionMark: Boolean? = null,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier
            .then(if (selectionMark != null) Modifier.semantics { selected = selectionMark } else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.sdp))) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                placeholder = painterResource(R.drawable.image_loading_albums),
                error = painterResource(R.drawable.image_loading_albums),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (selectionMark != null) Image(
                painter = painterResource(if (selectionMark) R.drawable.ic_check else R.drawable.ic_un_check),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.sdp).size(24.sdp)
            )
        }
        Text(
            text = title,
            style = GalleryText.BodyMediumMedium,
            color = colorResource(R.color.titleColor),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = titleTop).padding(horizontal = 5.sdp)
        )
        Text(
            text = count.toString(),
            style = GalleryText.BodyXSmallMedium,
            color = colorResource(R.color.countTitle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.sdp).padding(horizontal = 5.sdp)
        )
    }
}
