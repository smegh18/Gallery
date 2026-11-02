package com.quizango.gallery.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.screenBackground

// Reference App Explore Theme Colors
private val HeroBg = Color(0xFF0066FF)
private val HeroBtnText = Color(0xFF0047B3)
private val HeroSubtext = Color(0xFFD9E6FF)

// Chip Accent Colors matching Reference fragment_explore.xml
private val AiChipBg = Color(0xFFEEEDFE)
private val AiChipFg = Color(0xFF534AB7)
private val CollageChipBg = Color(0xFFFBEAF0)
private val CollageChipFg = Color(0xFF993556)
private val VaultChipBg = Color(0xFFE1F5EE)
private val VaultChipFg = Color(0xFF0F6E56)
private val FavChipBg = Color(0xFFFCEBEB)
private val FavChipFg = Color(0xFFA32D2D)
private val MomentsChipBg = Color(0xFFFAEEDA)
private val MomentsChipFg = Color(0xFF854F0B)
private val PlaceChipBg = Color(0xFFE6F1FB)
private val PlaceChipFg = Color(0xFF185FA5)
private val NewBadgeRed = Color(0xFFFF4D4F)

/**
 * ExploreTab reconstructed with 100% visual and functional parity to reference APK
 * Layout Structure:
 * 1. Top Hero Card: "Free up space" with sparkles illustration and "Scan now" button -> opens Storage Clean
 * 2. Section "Create": 2 side-by-side feature cards: AI Editor & Collage Maker (with "New" badge)
 * 3. Section "Library": 4 horizontal rounded circular tile buttons: Vault, Favorites, Moments, Place
 * 4. Section "Manage": Grouped card with Duplicate Finder, Quick Organize, Recycle Bin, Settings
 */
@Composable
fun ExploreTab(
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.screenBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 1. Top Hero Card (tileCleaner in fragment_explore.xml / explore_tab_hero.xml)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HeroBg)
                .clickable { onNavigate(Screen.RemoveJunk.route) }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.explore_free_up_space),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.explore_free_up_space_desc),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = HeroSubtext
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.explore_scan_now),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeroBtnText
                        )
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.explore_ic_sparkles),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .padding(start = 8.dp)
                )
            }
        }

        // 2. Section "Create"
        SectionLabel(text = stringResource(R.string.explore_section_create))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Editor Card (tileAiEditor)
            FeatureCard(
                title = stringResource(R.string.explore_tab_ai_editor),
                subtitle = stringResource(R.string.explore_ai_editor_desc),
                iconRes = R.drawable.explore_tab_ic_ai_editor,
                chipBg = AiChipBg,
                chipFg = AiChipFg,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.Editor.createRoute()) }
            )

            // Collage Maker Card (tileCollage with "New" badge)
            Box(modifier = Modifier.weight(1f)) {
                FeatureCard(
                    title = stringResource(R.string.str_home_collage),
                    subtitle = stringResource(R.string.explore_collage_desc),
                    iconRes = R.drawable.explore_tab_ic_collage,
                    chipBg = CollageChipBg,
                    chipFg = CollageChipFg,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(Screen.CollageMaker.route) }
                )

                // "New" Badge (collage_btn_new)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NewBadgeRed)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.album_collage_new),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Section "Library"
        SectionLabel(text = stringResource(R.string.explore_section_library))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TileButton(
                label = stringResource(R.string.vault),
                iconRes = R.drawable.explore_tab_ic_vault,
                chipBg = VaultChipBg,
                chipFg = VaultChipFg,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.VaultLock.route) }
            )

            TileButton(
                label = stringResource(R.string.str_home_favourite),
                iconRes = R.drawable.explore_tab_ic_favorites,
                chipBg = FavChipBg,
                chipFg = FavChipFg,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.Favorites.route) }
            )

            TileButton(
                label = stringResource(R.string.str_home_Moments),
                iconRes = R.drawable.explore_tab_ic_moments,
                chipBg = MomentsChipBg,
                chipFg = MomentsChipFg,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.Moments.route) }
            )

            TileButton(
                label = stringResource(R.string.str_home_place),
                iconRes = R.drawable.explore_tab_ic_place,
                chipBg = PlaceChipBg,
                chipFg = PlaceChipFg,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.AlbumDetail.createRoute("filter:place", "Places")) }
            )
        }

        // 4. Section "Manage"
        SectionLabel(text = stringResource(R.string.explore_section_manage))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.cardBackground)
                .border(1.dp, MaterialTheme.colorScheme.cardBorder, RoundedCornerShape(16.dp))
        ) {
            ManageRow(
                label = stringResource(R.string.duplicate_finder),
                iconRes = R.drawable.explore_tab_ic_duplicate,
                onClick = { onNavigate(Screen.DuplicateWaiting.route) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.cardBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 48.dp)
            )

            ManageRow(
                label = stringResource(R.string.organizer_explore_title),
                iconRes = R.drawable.explore_tab_ic_organize,
                onClick = { onNavigate(Screen.QuickOrganizer.route) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.cardBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 48.dp)
            )

            ManageRow(
                label = stringResource(R.string.str_home_Recycle_bin),
                iconRes = R.drawable.explore_tab_ic_recycle_bin,
                onClick = { onNavigate(Screen.RecycleBin.route) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.cardBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 48.dp)
            )

            ManageRow(
                label = stringResource(R.string.settings),
                iconRes = R.drawable.explore_tab_ic_settings,
                onClick = { onNavigate(Screen.Settings.route) }
            )
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    chipBg: Color,
    chipFg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val finalChipBg = if (isDark) chipBg.copy(alpha = 0.25f) else chipBg
    val finalChipFg = if (isDark) Color.White else chipFg

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.cardBackground)
            .border(1.dp, MaterialTheme.colorScheme.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(finalChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = finalChipFg,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TileButton(
    label: String,
    iconRes: Int,
    chipBg: Color,
    chipFg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val finalChipBg = if (isDark) chipBg.copy(alpha = 0.25f) else chipBg
    val finalChipFg = if (isDark) Color.White else chipFg

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(finalChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = finalChipFg,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ManageRow(
    label: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(id = R.drawable.explore_tab_ic_chevron),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
