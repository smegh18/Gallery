package albums.gallery.photo.folder.picasa.app.web.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue

/**
 * Native Ad Container matching reference APK flAd in AlbumsFragment.
 *
 * Implements:
 * 1. Prominent "Ad" badge.
 * 2. Headline and call to action.
 * 3. 12dp rounded corners and subtle border matching reference cards.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(cardShape)
            .background(Color(0xFFFAFAFA))
            .border(width = 1.dp, color = Color(0xFFF0F0F0), shape = cardShape)
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFFFB300))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Featured Recommendation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Discover enhanced photo tools, cloud backups, and smart categorization.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D90A3),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { /* Simulated native ad CTA */ },
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Install",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}
