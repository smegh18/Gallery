package com.quizango.gallery.ui.home

import android.app.ActivityManager
import android.content.Context
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.quizango.gallery.R
import com.quizango.gallery.ui.discovery.placeRoute
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

/** The 11 tap targets of the 2.7.2 Explore page (fragment_explore, EF.onViewCreated / EC). */
enum class ExploreItem {
    SCAN, AI_EDITOR, COLLAGE, VAULT, FAVORITES, MOMENTS, PLACE, DUPLICATE_FINDER, QUICK_ORGANIZE, RECYCLE_BIN, SETTINGS
}

/** Where each Explore item goes (UI-SPEC 3.7); no interstitials. */
internal fun exploreRoute(item: ExploreItem, junkCleanedToday: Boolean): String = when (item) {
    ExploreItem.SCAN -> if (junkCleanedToday) Screen.Cleaner.route else Screen.RemoveJunk.route
    ExploreItem.AI_EDITOR -> Screen.Editor.createRoute()
    ExploreItem.COLLAGE -> Screen.CollageMaker.route
    ExploreItem.VAULT -> Screen.VaultLock.route
    ExploreItem.FAVORITES -> Screen.Favorites.route
    ExploreItem.MOMENTS -> Screen.Moments.route
    ExploreItem.PLACE -> placeRoute()
    ExploreItem.DUPLICATE_FINDER -> Screen.DuplicateWaiting.route
    ExploreItem.QUICK_ORGANIZE -> Screen.QuickOrganizer.route
    ExploreItem.RECYCLE_BIN -> Screen.RecycleBin.route
    ExploreItem.SETTINGS -> Screen.Settings.route
}

/** EF.onViewCreated: the "New" badge stays until Collage was opened or the app was launched 4 times. */
internal fun showsCollageNew(seen: Boolean, session: Int) = !seen && session < 4

/** s9/k.OooOo0o: Duplicate Finder needs Android 10+ and at least 3 GB of RAM. */
internal fun showsDuplicateFinder(sdkInt: Int, totalMemBytes: Long) = sdkInt >= 29 && totalMemBytes / 1024 / 1024 / 1024 >= 3

internal fun manageRows(showDuplicate: Boolean): List<ExploreItem> =
    listOfNotNull(
        ExploreItem.DUPLICATE_FINDER.takeIf { showDuplicate },
        ExploreItem.QUICK_ORGANIZE, ExploreItem.RECYCLE_BIN, ExploreItem.SETTINGS
    )

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
    val context = LocalContext.current
    val showDuplicate = remember {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        showsDuplicateFinder(Build.VERSION.SDK_INT, info.totalMem)
    }
    fun open(item: ExploreItem) = onNavigate(exploreRoute(item, junkCleanedToday))

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
                    modifier = Modifier.weight(1f), showNewBadge = showCollageNew,
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

            NativeAdSkeleton()

            SectionLabel(R.string.explore_section_manage)
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawableBackground(R.drawable.explore_ad_card_bg)
                    .padding(vertical = 2.sdp)
            ) {
                manageRows(showDuplicate).forEachIndexed { index, item ->
                    val row = MANAGE_ROW_SPECS.getValue(item)
                    ManageRow(row.icon, row.label, showDivider = index > 0) { open(item) }
                }
            }

            // Debug-only: 2.7.2 reaches these from notifications/search only (Phase 9), so testers need a way in
            if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                Column(Modifier.padding(top = 10.sdp)) {
                    listOf(
                        "Other Albums" to com.quizango.gallery.ui.navigation.Screen.AllAlbums.route,
                        "Animated" to com.quizango.gallery.ui.navigation.Screen.AnimatedMedia.route,
                        "Burst" to com.quizango.gallery.ui.navigation.Screen.BurstPhotos.route
                    ).forEach { (label, route) ->
                        androidx.compose.material3.TextButton(onClick = { onNavigate(route) }) { androidx.compose.material3.Text("[debug] $label") }
                    }
                }
            }
        }
    }
}

private class ManageRowSpec(@DrawableRes val icon: Int, @StringRes val label: Int)

private val MANAGE_ROW_SPECS = mapOf(
    ExploreItem.DUPLICATE_FINDER to ManageRowSpec(R.drawable.explore_tab_ic_duplicate, R.string.duplicate_finder),
    ExploreItem.QUICK_ORGANIZE to ManageRowSpec(R.drawable.explore_tab_ic_organize, R.string.organizer_explore_title),
    ExploreItem.RECYCLE_BIN to ManageRowSpec(R.drawable.explore_tab_ic_recycle_bin, R.string.str_home_Recycle_bin),
    ExploreItem.SETTINGS to ManageRowSpec(R.drawable.explore_tab_ic_settings, R.string.settings)
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
    modifier: Modifier, showNewBadge: Boolean = false, onClick: () -> Unit
) {
    Box(
        modifier
            .drawableBackground(R.drawable.explore_card_bg)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(11.sdp)) {
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
        if (showNewBadge) {
            // Tapping the badge acts as tapping the card (EF.OooO0o).
            Text(
                stringResource(R.string.album_collage_new),
                style = GalleryText.BodyXSmallSemiBold.copy(color = colorResource(R.color.onlyWhite)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.sdp, end = 8.sdp)
                    .background(colorResource(R.color.home_collage_new_btn), CircleShape)
                    .padding(horizontal = 4.sdp, vertical = 2.sdp)
            )
        }
    }
}

/** Static outline of the native ad slot (D5: no ads, but the 2.7.2 card keeps its size). */
@Composable
private fun NativeAdSkeleton() {
    Box(
        Modifier
            .padding(top = 14.sdp)
            .fillMaxWidth()
            .heightIn(min = 113.sdp)
            .drawableBackground(R.drawable.explore_ad_card_bg)
    ) {
        Box(
            Modifier
                .padding(start = 11.sdp, top = 11.sdp)
                .size(width = 167.sdp, height = 91.sdp)
                .drawableBackground(R.drawable.explore_ad_shimmer_media)
        )
        Column(Modifier.padding(start = 191.sdp, top = 15.sdp)) {
            Box(Modifier.padding(end = 53.sdp).fillMaxWidth().height(9.sdp).drawableBackground(R.drawable.explore_ad_shimmer_block))
            Box(Modifier.padding(top = 7.sdp).size(width = 16.sdp, height = 13.sdp).drawableBackground(R.drawable.explore_ad_shimmer_block))
            Box(Modifier.padding(top = 7.sdp, end = 13.sdp).fillMaxWidth().height(6.sdp).drawableBackground(R.drawable.explore_ad_shimmer_block))
            Box(Modifier.padding(top = 3.sdp, end = 48.sdp).fillMaxWidth().height(6.sdp).drawableBackground(R.drawable.explore_ad_shimmer_block))
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 191.sdp, end = 23.sdp, bottom = 14.sdp)
                .fillMaxWidth()
                .height(26.sdp)
                .drawableBackground(R.drawable.explore_ad_shimmer_cta)
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
