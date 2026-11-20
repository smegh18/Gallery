package com.quizango.gallery.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

/** The 11 tap targets of the 2.7.2 Explore page (fragment_explore, EF.onViewCreated / EC). */
enum class ExploreItem {
    SCAN, AI_EDITOR, COLLAGE, VAULT, FAVORITES, MOMENTS, PLACE, DUPLICATE_FINDER, QUICK_ORGANIZE, RECYCLE_BIN, SETTINGS
}

/** Where each Explore item goes (UI-SPEC 3.7); no interstitials. */
internal fun exploreRoute(item: ExploreItem, junkCleanedToday: Boolean, placeTitle: String): String = when (item) {
    ExploreItem.SCAN -> if (junkCleanedToday) Screen.Cleaner.route else Screen.RemoveJunk.route
    ExploreItem.AI_EDITOR -> Screen.Editor.createRoute()
    ExploreItem.COLLAGE -> Screen.CollageMaker.route
    ExploreItem.VAULT -> Screen.VaultLock.route
    ExploreItem.FAVORITES -> Screen.Favorites.route
    ExploreItem.MOMENTS -> Screen.Moments.route
    ExploreItem.PLACE -> Screen.AlbumDetail.createRoute("filter:place", placeTitle)
    ExploreItem.DUPLICATE_FINDER -> Screen.DuplicateWaiting.route
    ExploreItem.QUICK_ORGANIZE -> Screen.QuickOrganizer.route
    ExploreItem.RECYCLE_BIN -> Screen.RecycleBin.route
    ExploreItem.SETTINGS -> Screen.Settings.route
}

@Composable
private fun exploreText(size: Int, weight: FontWeight, color: Color, align: TextAlign = TextAlign.Unspecified) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.ssp, color = color, textAlign = align)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreTab(
    junkCleanedToday: Boolean,
    showCollageNew: Boolean,
    onNavigate: (String) -> Unit,
    onCollageOpened: () -> Unit
) {
    val placeTitle = stringResource(R.string.str_home_place)
    fun open(item: ExploreItem) = onNavigate(exploreRoute(item, junkCleanedToday, placeTitle))

    // Reference scrollView has overScrollMode=never.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.explore_screen_bg))
                .verticalScroll(rememberScrollState())
                .padding(start = 12.sdp, end = 12.sdp, top = 10.sdp, bottom = 58.sdp)
        ) {
            ExploreHero(onClick = { open(ExploreItem.SCAN) })

            SectionLabel(R.string.explore_section_create)
            Row(horizontalArrangement = Arrangement.spacedBy(8.sdp)) {
                FeatureCard(
                    icon = R.drawable.explore_tab_ic_ai_editor, chipBg = R.color.explore_chip_ai_bg, chipFg = R.color.explore_chip_ai_fg,
                    title = R.string.explore_tab_ai_editor, desc = R.string.explore_ai_editor_desc,
                    modifier = Modifier.weight(1f), onClick = { open(ExploreItem.AI_EDITOR) }
                )
                FeatureCard(
                    icon = R.drawable.explore_tab_ic_collage, chipBg = R.color.explore_chip_collage_bg, chipFg = R.color.explore_chip_collage_fg,
                    title = R.string.str_home_collage, desc = R.string.explore_collage_desc,
                    modifier = Modifier.weight(1f),
                    onClick = { onCollageOpened(); open(ExploreItem.COLLAGE) }
                )
            }

            SectionLabel(R.string.explore_section_library)
            Row {
                RoundTile(R.drawable.explore_tab_ic_vault, R.color.explore_chip_vault_bg, R.color.explore_chip_vault_fg, R.string.vault, Modifier.weight(1f)) { open(ExploreItem.VAULT) }
                RoundTile(R.drawable.explore_tab_ic_favorites, R.color.explore_chip_favorites_bg, R.color.explore_chip_favorites_fg, R.string.str_home_favourite, Modifier.weight(1f)) { open(ExploreItem.FAVORITES) }
                RoundTile(R.drawable.explore_tab_ic_moments, R.color.explore_chip_moments_bg, R.color.explore_chip_moments_fg, R.string.str_home_Moments, Modifier.weight(1f)) { open(ExploreItem.MOMENTS) }
                RoundTile(R.drawable.explore_tab_ic_place, R.color.explore_chip_place_bg, R.color.explore_chip_place_fg, R.string.str_home_place, Modifier.weight(1f)) { open(ExploreItem.PLACE) }
            }

            SectionLabel(R.string.explore_section_manage)
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawableBackground(R.drawable.explore_ad_card_bg)
                    .padding(vertical = 2.sdp)
            ) {
                MANAGE_ROWS.forEachIndexed { index, row ->
                    ManageRow(row.icon, row.label, showDivider = index > 0) { open(row.item) }
                }
            }
        }
    }
}

private class ManageRowSpec(val item: ExploreItem, @DrawableRes val icon: Int, @StringRes val label: Int)

private val MANAGE_ROWS = listOf(
    ManageRowSpec(ExploreItem.DUPLICATE_FINDER, R.drawable.explore_tab_ic_duplicate, R.string.duplicate_finder),
    ManageRowSpec(ExploreItem.QUICK_ORGANIZE, R.drawable.explore_tab_ic_organize, R.string.organizer_explore_title),
    ManageRowSpec(ExploreItem.RECYCLE_BIN, R.drawable.explore_tab_ic_recycle_bin, R.string.str_home_Recycle_bin),
    ManageRowSpec(ExploreItem.SETTINGS, R.drawable.explore_tab_ic_settings, R.string.settings)
)

@Composable
private fun ExploreHero(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawableBackground(R.drawable.explore_hero_bg)
            .clickable(onClick = onClick)
            .padding(start = 14.sdp, end = 10.sdp, top = 13.sdp, bottom = 13.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.explore_free_up_space), style = exploreText(13, FontWeight.W600, colorResource(R.color.explore_hero_text)))
            Text(
                stringResource(R.string.explore_free_up_space_desc),
                style = exploreText(9, FontWeight.W400, colorResource(R.color.explore_hero_subtext)),
                modifier = Modifier.padding(top = 3.sdp)
            )
            // Not separately clickable: the whole card is the target.
            Text(
                stringResource(R.string.explore_scan_now),
                style = exploreText(9, FontWeight.W600, colorResource(R.color.explore_hero_btn_text)),
                modifier = Modifier
                    .padding(top = 10.sdp)
                    .drawableBackground(R.drawable.explore_hero_btn_bg)
                    .padding(horizontal = 12.sdp, vertical = 5.sdp)
            )
        }
        Image(painterResource(R.drawable.explore_ic_sparkles), contentDescription = null, modifier = Modifier.padding(start = 8.sdp).size(44.sdp))
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = exploreText(10, FontWeight.W500, colorResource(R.color.explore_subtext)),
        modifier = Modifier.padding(start = 2.sdp, top = 16.sdp, bottom = 8.sdp)
    )
}

@Composable
private fun FeatureCard(
    @DrawableRes icon: Int, @androidx.annotation.ColorRes chipBg: Int, @androidx.annotation.ColorRes chipFg: Int,
    @StringRes title: Int, @StringRes desc: Int,
    modifier: Modifier, onClick: () -> Unit
) {
    Column(
        modifier
            .drawableBackground(R.drawable.explore_card_bg)
            .clickable(onClick = onClick)
            .padding(11.sdp)
    ) {
        Box(
            Modifier.size(30.sdp).background(colorResource(chipBg), RoundedCornerShape(9.sdp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(17.sdp), colorFilter = ColorFilter.tint(colorResource(chipFg)))
        }
        Text(
            stringResource(title), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = exploreText(11, FontWeight.W600, colorResource(R.color.explore_text)),
            modifier = Modifier.padding(top = 8.sdp)
        )
        Text(
            stringResource(desc), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = exploreText(9, FontWeight.W400, colorResource(R.color.explore_subtext)),
            modifier = Modifier.padding(top = 3.sdp)
        )
    }
}

@Composable
private fun RoundTile(
    @DrawableRes icon: Int, @androidx.annotation.ColorRes bg: Int, @androidx.annotation.ColorRes fg: Int,
    @StringRes label: Int, modifier: Modifier, onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(40.sdp).background(colorResource(bg), CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(19.sdp), colorFilter = ColorFilter.tint(colorResource(fg)))
        }
        Text(
            stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = exploreText(9, FontWeight.W500, colorResource(R.color.explore_text), TextAlign.Center),
            modifier = Modifier.padding(top = 6.sdp)
        )
    }
}

@Composable
private fun ManageRow(@DrawableRes icon: Int, @StringRes label: Int, showDivider: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(42.sdp).clickable(onClick = onClick)) {
        if (showDivider) {
            // 1 px hairline, inset 40.sdp from the start (explore_tab_item_row)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 40.sdp)
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { 1.toDp() })
                    .background(colorResource(R.color.explore_divider))
            )
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(icon), contentDescription = null,
                modifier = Modifier.padding(start = 12.sdp).size(20.sdp),
                colorFilter = ColorFilter.tint(colorResource(R.color.explore_text))
            )
            Text(
                stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = exploreText(11, FontWeight.W500, colorResource(R.color.explore_text)),
                modifier = Modifier.weight(1f).padding(start = 8.sdp, end = 7.sdp)
            )
            Image(
                painterResource(R.drawable.explore_tab_ic_chevron), contentDescription = null,
                modifier = Modifier.padding(end = 10.sdp).size(16.sdp),
                colorFilter = ColorFilter.tint(colorResource(R.color.explore_subtext))
            )
        }
    }
}
