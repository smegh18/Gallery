package albums.gallery.photo.folder.picasa.app.web.gallery.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue

/**
 * Native Ad Container exactly matching reference APK gnt_home_bottom_template_view_light.xml in AlbumsFragment.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    onAdClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF1E1E1E))
            .border(width = 1.dp, color = Color(0xFF303030), shape = cardShape)
            .clickable(onClick = onAdClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App / Game Icon
            Image(
                painter = painterResource(id = R.drawable.ic_story_playzen),
                contentDescription = "Ad Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Ad badge + Title + Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .border(width = 0.8.dp, color = Color(0xFF8D8D8D), shape = RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "Ad",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8D8D8D)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Subway Surfers",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Play free online games instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF898888),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: CTA Button
            Button(
                onClick = onAdClick,
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Play Now",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = Color.White
                )
            }
        }
    }
}
