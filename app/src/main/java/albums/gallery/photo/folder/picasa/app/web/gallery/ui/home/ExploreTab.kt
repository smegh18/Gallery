package albums.gallery.photo.folder.picasa.app.web.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen

@Composable
fun ExploreTab(
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. System Cleaner Hero Card (Reference item_system_cleaner)
        ExploreHeroCard(
            iconRes = R.drawable.system_cleaner_main_icon,
            title = "System Cleaner",
            subtitle = "Clear up space and optimize performance.",
            arrowRes = R.drawable.system_cleaner_banner_arrow_new_ornage,
            onClick = { onNavigate(Screen.Cleaner.route) }
        )

        // 2. Quick Organize Card (Reference item_organizer)
        ExploreHeroCard(
            iconRes = R.drawable.ic_organier_explore,
            title = "Quick Organize",
            subtitle = "Quickly organize your photo library.",
            arrowRes = R.drawable.system_cleaner_banner_arrow_new,
            onClick = { onNavigate(Screen.Duplicates.route) }
        )

        // 3. Collage Maker Card with NEW badge (Reference item_collage_maker)
        ExploreHeroCard(
            iconRes = R.drawable.ic_collage_explore,
            title = "Collage Maker",
            subtitle = "Create stunning photo collages in seconds.",
            arrowRes = R.drawable.system_cleaner_banner_arrow_new,
            hasNewBadge = true,
            onClick = { onNavigate(Screen.Editor.createRoute()) }
        )
    }
}

@Composable
private fun ExploreHeroCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    arrowRes: Int,
    hasNewBadge: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = Color(0xFFF0F0F0), shape = shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feature Icon
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3354),
                        fontSize = 17.sp
                    )

                    if (hasNewBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF4D4F))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "New",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7D90A3),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Trailing action arrow
            Icon(
                painter = painterResource(id = arrowRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
