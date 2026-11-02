package com.quizango.gallery.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryBlue

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
    onAddAlbumClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onProClick: (() -> Unit)? = null,
    onLockClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    onSelectAllClick: () -> Unit = {},
    onCloseSelection: () -> Unit = {}
) {
    TopAppBar(
        title = {
            if (isSelectionMode) {
                Text(
                    text = "$selectedCount Selected",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E3354)
                )
            } else if (title == "GALLERY" || title.isEmpty() || title == "Photos" || title == "Album") {
                // Reference home title: GALLERY in primary blue with lock icon next to it
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLockClick?.invoke() }
                ) {
                    Text(
                        text = "GALLERY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GalleryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_home_lock),
                        contentDescription = "Private Vault",
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.Top)
                            .padding(top = 2.dp)
                    )
                }
            } else {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF14161C),
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
                        contentDescription = "Close Selection",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (title != "Explore") {
                // Home drawer button with red dot badge (hidden on Explore tab matching reference app)
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenDrawer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nav_menu),
                        contentDescription = "Menu",
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                    )
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                TextButton(onClick = onSelectAllClick) {
                    Text(
                        text = if (isAllSelected) "Unselect All" else "Select All",
                        fontWeight = FontWeight.SemiBold,
                        color = GalleryBlue,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Actions ordered matching reference activity_home.xml: [ivPro] [searchBtn] [cameraBtn/mediaShortBtn]
                if (onProClick != null) {
                    IconButton(onClick = onProClick) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pro_new),
                            contentDescription = "Pro",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_toolbar_search),
                        contentDescription = "Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (onCameraClick != null) {
                    IconButton(onClick = onCameraClick) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Camera",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (onSortClick != null) {
                    IconButton(onClick = onSortClick) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_short),
                            contentDescription = "Sort",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (onAddAlbumClick != null) {
                    IconButton(onClick = onAddAlbumClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Album",
                            tint = Color(0xFF1E3354),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF1E3354)
        )
    )
}

@Composable
fun GalleryBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(color = Color(0xFFE4E6E9), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple(0, "Albums", Pair(R.drawable.ic_albums_fill, R.drawable.ic_albums_border)),
                    Triple(1, "All Media", Pair(R.drawable.ic_all_media_fill, R.drawable.ic_all_media_border)),
                    Triple(2, "Explore", Pair(R.drawable.ic_explore_fill, R.drawable.ic_explore_border))
                )

                val unselectedTextColor = Color(0xFF7A8389)
                tabs.forEach { (index, label, iconPair) ->
                    val isSelected = selectedTab == index
                    val currentIcon = if (isSelected) iconPair.first else iconPair.second

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = currentIcon),
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) GalleryBlue else unselectedTextColor,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
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
        modifier = Modifier.width(348.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                // 1. Go Pro Banner (Reference layout_settings_premium + premium_bg)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, Color(0xFF873900), RoundedCornerShape(13.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFFC78E00), Color(0xFFE4A324))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x90FFFFFF))
                            .clickable {
                                onCloseDrawer()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.premium),
                                contentDescription = "Go Ads Free",
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Go Ads Free!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF873900),
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unlock everything & enjoy the ultimate ad-free experience",
                                    color = Color(0xFF873900),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Reference Primary Drawer Items
                val primaryItems = listOf(
                    DrawerItemData("Favorites", R.drawable.ic_favorite, "favorites", hasArrow = true),
                    DrawerItemData("Create Album", R.drawable.ic_create_album, "create_album", hasArrow = false),
                    DrawerItemData("Recycle Bin", R.drawable.ic_delete, "recycle", hasArrow = true),
                    DrawerItemData("System Cleaner", R.drawable.ic_drawer_cleaner, "cleaner", hasArrow = true, badgeText = "New"),
                    DrawerItemData("Duplicate Finder", R.drawable.ic_duplicate, "duplicates", hasArrow = true),
                    DrawerItemData("Private Vault", R.drawable.ic_unhide, "vault", hasArrow = true),
                    DrawerItemData("Ai Photo Editor", R.drawable.ic_drawer_editor, "editor", hasArrow = true),
                    DrawerItemData("Change Language", R.drawable.ic_app_language, "language", hasArrow = true),
                    DrawerItemData("Theme Mode", R.drawable.ic_theme_mode, "theme", hasArrow = true),
                    DrawerItemData("Settings", R.drawable.ic_settings, "settings", hasArrow = true, hasDot = true)
                )

                primaryItems.forEach { item ->
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

                // Reference drawerDivider (@color/text_line_color: #50bfd4e4)
                HorizontalDivider(
                    color = Color(0x50BFD4E4),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                // 3. Secondary Items (Games, More Apps, About Us)
                val secondaryItems = listOf(
                    DrawerItemData("Games", R.drawable.ic_game, "games", hasArrow = false, isAdBadge = true),
                    DrawerItemData("More Apps", R.drawable.ic_more_apps, "more_apps", hasArrow = false, isAdBadge = true),
                    DrawerItemData("About Us", R.drawable.ic_about, "about", hasArrow = false)
                )

                secondaryItems.forEach { item ->
                    DrawerRow(
                        item = item,
                        onClick = {
                            onCloseDrawer()
                            onNavigate(item.route)
                        }
                    )
                }
            }

            // 4. Social Footer (Reference followUsLayout with @color/drawer_bottom_trans_bg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3EFF8))
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Follow us on",
                    fontSize = 11.5.sp,
                    color = Color(0xFF597393),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Instagram link */ }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            contentDescription = "Instagram",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { /* YouTube link */ }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_youtube),
                            contentDescription = "YouTube",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { /* Facebook link */ }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            contentDescription = "Facebook",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
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
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.label,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            if (item.hasDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = (-1).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E3354),
                fontSize = 14.sp
            )
            if (item.isAdBadge) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AD",
                    color = Color(0xFF597393),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (item.badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF1414))
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.badgeText,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
