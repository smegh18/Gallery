package com.quizango.gallery.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.screenBackground

/**
 * ExploreTab reconstructed with 100% visual and functional parity to reference fragment_explore.xml
 * Features 3 prominent rounded hero cards (layout_shape):
 * 1. item_system_cleaner -> System Cleaner screen
 * 2. item_organizer -> Quick Organizer screen
 * 3. item_collage_maker -> Collage Maker screen (with "New" badge)
 */
@Composable
fun ExploreTab(
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // reference: plain page background
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. System Cleaner Hero Card (item_system_cleaner)
        ExploreHeroCard(
            title = stringResource(R.string.tool_bar_system_cleaner),
            subtitle = stringResource(R.string.txt_cleaner_sub),
            iconRes = R.drawable.system_cleaner_main_icon,
            arrowRes = R.drawable.system_cleaner_banner_arrow_new_ornage,
            onClick = { onNavigate(Screen.Cleaner.route) }
        )

        // 2. Quick Organize Hero Card (item_organizer)
        ExploreHeroCard(
            title = stringResource(R.string.organizer_explore_title),
            subtitle = stringResource(R.string.organizer_explore_sub_title),
            iconRes = R.drawable.ic_organier_explore,
            arrowRes = R.drawable.system_cleaner_banner_arrow_new,
            onClick = { onNavigate(Screen.QuickOrganizer.route) }
        )

        // 3. Collage Maker Hero Card (item_collage_maker with "New" badge)
        ExploreHeroCard(
            title = stringResource(R.string.collage_maker_explore_title),
            subtitle = stringResource(R.string.collage_maker_explore_sub_title),
            iconRes = R.drawable.ic_collage_explore,
            arrowRes = R.drawable.system_cleaner_banner_arrow_new,
            badgeText = stringResource(R.string.album_collage_new),
            onClick = { onNavigate(Screen.CollageMaker.route) }
        )
    }
}

@Composable
private fun ExploreHeroCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    arrowRes: Int,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.cardBackground)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.cardBorder, shape = cardShape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp) // reference icon size
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Image(
                painter = painterResource(id = arrowRes),
                contentDescription = null,
                modifier = Modifier.size(width = 35.dp, height = 24.dp)
            )
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp))
                    .background(Color(0xFFFF3B30))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
