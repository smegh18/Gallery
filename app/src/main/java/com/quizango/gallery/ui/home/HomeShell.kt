package com.quizango.gallery.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/** One Home tab: the title shown in the top bar and the label/icons shown in the tab bar (HA.o000o0oO / o000Oo00). */
internal data class HomeTab(
    @StringRes val title: Int,
    @StringRes val label: Int,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int
)

/** In pager-page order: PAGE_PHOTOS, PAGE_ALBUMS, PAGE_EXPLORE. */
internal val HOME_TABS = listOf(
    HomeTab(R.string.home_shell_photos, R.string.home_shell_photos, R.drawable.ic_tab_photos_fill, R.drawable.ic_tab_photos_outline),
    HomeTab(R.string.home_shell_album, R.string.albums, R.drawable.ic_tab_albums_fill, R.drawable.ic_tab_albums_outline),
    HomeTab(R.string.explore, R.string.explore, R.drawable.ic_tab_explore_fill, R.drawable.ic_tab_explore_outline)
)

@Composable
internal fun HomeTopBar(page: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
            .height(42.sdp)
            .padding(end = 7.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(HOME_TABS[page].title),
            style = GalleryText.BodyXXLargeSemiBold,
            color = colorResource(R.color.home_shell_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 13.sdp)
        )
        Spacer(Modifier.weight(1f))
    }
}

/** No banner-ad slot: the reference slot is 0 high with no ad SDK (decision D5). */
@Composable
internal fun HomeTabBar(selectedPage: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.home_shell_tab_bg))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.home_shell_tab_divider))
        Row(modifier = Modifier.fillMaxWidth().height(44.sdp)) {
            HOME_TABS.forEachIndexed { page, tab ->
                val selected = page == selectedPage
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = { onSelect(page) }
                        )
                        .padding(top = 7.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                        contentDescription = null,
                        modifier = Modifier.size(19.sdp)
                    )
                    // SemiBold on every tab: the reference re-applies the selected typeface to all labels (HA.o000Oo0O)
                    Text(
                        text = stringResource(tab.label),
                        style = GalleryText.BodySmallSemiBold,
                        color = colorResource(if (selected) R.color.home_shell_tab_selected else R.color.home_shell_tab_unselected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.sdp, start = 3.sdp, end = 3.sdp)
                    )
                }
            }
        }
    }
}
