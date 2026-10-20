package albums.gallery.photo.folder.picasa.app.web.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCloseSelection) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Selection")
                }
            } else if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Box {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                TextButton(onClick = onSelectAllClick) {
                    Text(text = "Select All", fontWeight = FontWeight.SemiBold)
                }
            } else {
                if (onSortClick != null) {
                    IconButton(onClick = onSortClick) {
                        Icon(imageVector = Icons.Outlined.Sort, contentDescription = "Sort")
                    }
                }
                if (onCameraClick != null) {
                    IconButton(onClick = onCameraClick) {
                        Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = "Camera")
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
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
            Triple(0, "Albums", Icons.Outlined.Folder),
            Triple(1, "Photos", Icons.Outlined.Photo),
            Triple(2, "Explore", Icons.Outlined.Explore)
        )

        tabs.forEach { (index, label, icon) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GalleryBlue.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun GalleryDrawer(
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GalleryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = GalleryBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v2.6.7",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        val menuItems = listOf(
            DrawerItem("Favorites", Icons.Outlined.Favorite, "favorites"),
            DrawerItem("Recycle Bin", Icons.Outlined.Delete, "recycle"),
            DrawerItem("Duplicate Finder", Icons.Outlined.Difference, "duplicates"),
            DrawerItem("Private Vault", Icons.Outlined.Lock, "vault"),
            DrawerItem("System Cleaner", Icons.Outlined.CleaningServices, "cleaner"),
            DrawerItem("Photo Editor", Icons.Outlined.AutoFixHigh, "editor"),
            DrawerItem("Language", Icons.Outlined.Language, "language"),
            DrawerItem("Theme", Icons.Outlined.BrightnessMedium, "theme"),
            DrawerItem("Settings", Icons.Outlined.Settings, "settings")
        )

        menuItems.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = item.label, tint = MaterialTheme.colorScheme.onBackground) },
                label = { Text(item.label, fontWeight = FontWeight.Medium) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onNavigate(item.route)
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

private data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)
