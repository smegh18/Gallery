package com.quizango.gallery.ui.cleaner

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/** SystemCleanerActivity entries in screen order: the item_remove_junk card, then the systemCleanerRecycler rows. */
internal enum class CleanerEntry(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val subtitle: Int) {
    REMOVE_JUNK(R.drawable.ic_remove_junk_list, R.string.remove_junk, R.string.txt_cleaner_sub),
    DUPLICATES(R.drawable.img_system_cleaner_duplicate, R.string.duplicate_images, R.string.txt_sub_duplicate),
    SCREENSHOTS(R.drawable.img_system_cleaner_screenshot, R.string.tool_bar_screenshot, R.string.txt_sub_screenshot),
    LARGE_VIDEO(R.drawable.img_system_cleaner_large_video, R.string.tool_bar_large_video, R.string.txt_sub_large_video);

    /** du/a: card and Scan now -> junk scan, rows pswitch 0/1/2 -> duplicates, screenshots, large videos. */
    fun <T> pick(junk: T, duplicates: T, screenshots: T, largeVideos: T): T = when (this) {
        REMOVE_JUNK -> junk
        DUPLICATES -> duplicates
        SCREENSHOTS -> screenshots
        LARGE_VIDEO -> largeVideos
    }
}

/** SystemCleanerActivity (activity_system_cleaner, item_system_cleaner; UI-SPEC §1). */
@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToLargeVideos: () -> Unit = {},
    onNavigateToScreenshots: () -> Unit = {},
    onNavigateToRemoveJunk: () -> Unit = {}
) {
    val open = { e: CleanerEntry ->
        e.pick(onNavigateToRemoveJunk, onNavigateToDuplicates, onNavigateToScreenshots, onNavigateToLargeVideos)()
    }
    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.tool_bar_system_cleaner), onBack = onNavigateBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            CleanerCard(CleanerEntry.REMOVE_JUNK, R.color.titleColor, top = 14.sdp, titleEnd = 2.sdp, subtitleEnd = 6.sdp, onClick = open) {
                val pill = RoundedCornerShape(25.dp)
                Box(
                    Modifier
                        .padding(start = 18.sdp)
                        .width(82.sdp)
                        .padding(vertical = 6.dp) // MaterialButton default insets
                        .heightIn(min = 36.dp)
                        .clip(pill)
                        .background(colorResource(R.color.cleaner_red))
                        .clickable { open(CleanerEntry.REMOVE_JUNK) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.scan_now),
                        style = GalleryText.BodyMediumSemiBold,
                        color = colorResource(R.color.onlyWhite),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            SystemCleanerRows(open)
        }
    }
}

/** systemCleanerRecycler rows; Junk cleared's rv_other_item (dy/d) shows the same three. */
internal val SYSTEM_CLEANER_ROWS = CleanerEntry.entries.drop(1)

/** item_system_cleaner rows: Duplicate Images, Screenshots, Large Video, each with the banner arrow. */
@Composable
internal fun SystemCleanerRows(onClick: (CleanerEntry) -> Unit) {
    SYSTEM_CLEANER_ROWS.forEach { entry ->
        CleanerCard(entry, R.color.ic_cleaner_expand_arrow_color, top = 9.sdp, titleEnd = 0.dp, subtitleEnd = 0.dp, onClick = onClick) {
            Image(
                painterResource(R.drawable.system_cleaner_banner_arrow),
                contentDescription = null,
                modifier = Modifier.padding(start = 15.sdp).padding(5.sdp)
            )
        }
    }
}

/** layout_shape card: icon centred, title + 2-line subtitle top-anchored, trailing control centred. */
@Composable
private fun CleanerCard(
    entry: CleanerEntry,
    @ColorRes titleColor: Int,
    top: Dp,
    titleEnd: Dp,
    subtitleEnd: Dp,
    onClick: (CleanerEntry) -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .padding(start = 14.sdp, end = 14.sdp, top = top)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.sdp))
            .drawableBackground(R.drawable.layout_shape)
            .clickable { onClick(entry) }
            .padding(14.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(entry.icon), contentDescription = null)
        Column(Modifier.weight(1f).align(Alignment.Top).padding(start = 12.sdp)) {
            Text(
                stringResource(entry.title),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(titleColor),
                modifier = Modifier.padding(top = 5.sdp, end = titleEnd)
            )
            Text(
                stringResource(entry.subtitle),
                style = GalleryText.BodySmallMedium,
                color = colorResource(R.color.item_cleaner_sub_txt),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.sdp, end = subtitleEnd)
            )
        }
        trailing()
    }
}
