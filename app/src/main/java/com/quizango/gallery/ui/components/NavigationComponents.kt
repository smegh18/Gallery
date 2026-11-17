package com.quizango.gallery.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryBlue

/** Top bar of the inner screens (album detail) and of Home while selecting. The Home bar proper is HomeTopBar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    onSelectAllClick: () -> Unit = {},
    onCloseSelection: () -> Unit = {}
) {
    Column {
    TopAppBar(
        title = {
            if (isSelectionMode) {
                Text(
                    text = stringResource(R.string.items_selected, selectedCount),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = title,
                    // Reference inner-screen titles (Videos, Favorites): medium weight, 18sp
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCloseSelection) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back", // no reference key; Phase 5
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                TextButton(onClick = onSelectAllClick) {
                    Text(
                        text = stringResource(if (isAllSelected) R.string.unselect_all else R.string.select_all),
                        fontWeight = FontWeight.SemiBold,
                        color = GalleryBlue,
                        fontSize = 14.sp
                    )
                }
            } else {
                IconButton(onClick = onSearchClick) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_toolbar_search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
    // Reference inner screens have a hairline under the toolbar
    if (canNavigateBack) HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
    }
}
