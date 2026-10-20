package albums.gallery.photo.folder.picasa.app.web.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen

data class ExploreFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color,
    val route: String
)

@Composable
fun ExploreTab(
    onNavigate: (String) -> Unit
) {
    val features = listOf(
        ExploreFeature(
            title = "System Cleaner",
            subtitle = "Clean app cache, residual & junk files",
            icon = Icons.Outlined.CleaningServices,
            iconTint = Color(0xFF0066FF),
            bgTint = Color(0xFFE8F2FF),
            route = Screen.Cleaner.route
        ),
        ExploreFeature(
            title = "Duplicate Finder",
            subtitle = "Find & remove repeated media items",
            icon = Icons.Outlined.Difference,
            iconTint = Color(0xFF7C4DFF),
            bgTint = Color(0xFFF3E5F5),
            route = Screen.Duplicates.route
        ),
        ExploreFeature(
            title = "Private Vault",
            subtitle = "Password & pattern protected secret vault",
            icon = Icons.Outlined.Lock,
            iconTint = Color(0xFFFF9800),
            bgTint = Color(0xFFFFF3E0),
            route = Screen.VaultLock.route
        ),
        ExploreFeature(
            title = "Recycle Bin",
            subtitle = "Restore accidentally deleted media",
            icon = Icons.Outlined.Delete,
            iconTint = Color(0xFFF44336),
            bgTint = Color(0xFFFFEBEE),
            route = Screen.RecycleBin.route
        ),
        ExploreFeature(
            title = "Favorites",
            subtitle = "Your bookmarked photos and videos",
            icon = Icons.Outlined.Favorite,
            iconTint = Color(0xFFE91E63),
            bgTint = Color(0xFFFCE4EC),
            route = Screen.Favorites.route
        ),
        ExploreFeature(
            title = "Photo Editor",
            subtitle = "Crop, adjust, filter, and draw on photos",
            icon = Icons.Outlined.AutoFixHigh,
            iconTint = Color(0xFF4CAF50),
            bgTint = Color(0xFFE8F5E9),
            route = Screen.Editor.createRoute()
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(features) { feature ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(feature.route) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(feature.bgTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = feature.title,
                            tint = feature.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
