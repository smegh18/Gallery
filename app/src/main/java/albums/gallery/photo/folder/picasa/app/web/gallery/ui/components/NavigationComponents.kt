package albums.gallery.photo.folder.picasa.app.web.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSortClick: (() -> Unit)? = null,
    onCameraClick: (() -> Unit)? = null,
    onProClick: (() -> Unit)? = null,
    onLockClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onSelectAllClick: () -> Unit = {},
    onCloseSelection: () -> Unit = {}
) {
    TopAppBar(
        title = {
            if (isSelectionMode) {
                Text(
                    text = "$selectedCount Selected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
                )
            } else if (canNavigateBack) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3354),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = onLockClick != null) {
                        onLockClick?.invoke()
                    }
                ) {
                    Text(
                        text = "GALLERY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GalleryBlue,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home_lock),
                        contentDescription = "Vault Lock",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCloseSelection) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Selection")
                }
            } else if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = Color.Unspecified
                    )
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Box {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_menu),
                            contentDescription = "Menu",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                TextButton(onClick = onSelectAllClick) {
                    Text(
                        text = "Select All",
                        fontWeight = FontWeight.SemiBold,
                        color = GalleryBlue
                    )
                }
            } else {
                if (onSortClick != null) {
                    IconButton(onClick = onSortClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_short),
                            contentDescription = "Sort",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (onCameraClick != null) {
                    IconButton(onClick = onCameraClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Camera",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_toolbar_search),
                        contentDescription = "Search",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { onProClick?.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pro_new),
                        contentDescription = "Pro",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun GalleryBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        val tabs = listOf(
            Triple(0, "Albums", Pair(R.drawable.ic_albums_fill, R.drawable.ic_albums_border)),
            Triple(1, "All Media", Pair(R.drawable.ic_all_media_fill, R.drawable.ic_all_media_border)),
            Triple(2, "Explore", Pair(R.drawable.ic_explore_fill, R.drawable.ic_explore_border))
        )

        tabs.forEach { (index, label, iconPair) ->
            val isSelected = selectedTab == index
            val currentIcon = if (isSelected) iconPair.first else iconPair.second
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        painter = painterResource(id = currentIcon),
                        contentDescription = label,
                        tint = if (isSelected) GalleryBlue else Color(0xFF1E3354),
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GalleryBlue else Color(0xFF1E3354),
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GalleryBlue.copy(alpha = 0.12f),
                    selectedIconColor = GalleryBlue,
                    selectedTextColor = GalleryBlue,
                    unselectedIconColor = Color(0xFF1E3354),
                    unselectedTextColor = Color(0xFF1E3354)
                )
            )
        }
    }
}

@Composable
fun GalleryDrawer(
    onNavigate: (String) -> Unit,
    onCreateAlbumClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // 1. Go Pro Banner (Reference layout_settings_premium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFFC78E00), Color(0xFFE4A324))
                            )
                        )
                        .clickable {
                            onCloseDrawer()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.premium),
                            contentDescription = "Go Ads Free",
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Go Ads Free!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF873900),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Unlock everything & enjoy the ultimate ad-free experience.",
                                color = Color(0xFF873900),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Reference Drawer Items
                val drawerItems = listOf(
                    DrawerItemData("Favorites", R.drawable.ic_favorite, "favorites", hasArrow = true),
                    DrawerItemData("Create Album", R.drawable.ic_create_album, "create_album", hasArrow = false),
                    DrawerItemData("Recycle Bin", R.drawable.ic_delete, "recycle", hasArrow = true),
                    DrawerItemData("System Cleaner", R.drawable.ic_drawer_cleaner, "cleaner", hasArrow = true, badgeText = "New"),
                    DrawerItemData("Duplicate Finder", R.drawable.ic_duplicate, "duplicates", hasArrow = true),
                    DrawerItemData("Private Vault", R.drawable.ic_unhide, "vault", hasArrow = true),
                    DrawerItemData("Ai Photo Editor", R.drawable.ic_drawer_editor, "editor", hasArrow = true),
                    DrawerItemData("Change Language", R.drawable.ic_app_language, "language", hasArrow = true),
                    DrawerItemData("Theme Mode", R.drawable.ic_theme_mode, "theme", hasArrow = true),
                    DrawerItemData("Settings", R.drawable.ic_settings, "settings", hasArrow = true, hasDot = true),
                    DrawerItemData("Games", R.drawable.ic_game, "games", hasArrow = false, isAdBadge = true),
                    DrawerItemData("More Apps", R.drawable.ic_more_apps, "more_apps", hasArrow = false, isAdBadge = true),
                    DrawerItemData("About Us", R.drawable.ic_about, "about", hasArrow = false)
                )

                drawerItems.forEach { item ->
                    DrawerRow(
                        item = item,
                        onClick = {
                            onCloseDrawer()
                            if (item.route == "create_album") {
                                onCreateAlbumClick()
                            } else {
                                onNavigate(item.route)
                            }
                        }
                    )
                }
            }

            // 3. Social Footer (Reference followUsLayout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Follow us on",
                    fontSize = 11.sp,
                    color = Color(0xFF8C9BAE),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Instagram link */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* YouTube link */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_youtube),
                            contentDescription = "YouTube",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* Facebook link */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            contentDescription = "Facebook",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerRow(
    item: DrawerItemData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.label,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            if (item.hasDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3354),
            modifier = Modifier.weight(1f)
        )
        if (item.badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF1414))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.badgeText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (item.isAdBadge) {
            Text(
                text = "AD",
                color = Color(0xFF597393),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        if (item.hasArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drawer_right_arrow),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private data class DrawerItemData(
    val label: String,
    val iconRes: Int,
    val route: String,
    val hasArrow: Boolean = true,
    val badgeText: String? = null,
    val isAdBadge: Boolean = false,
    val hasDot: Boolean = false
)
