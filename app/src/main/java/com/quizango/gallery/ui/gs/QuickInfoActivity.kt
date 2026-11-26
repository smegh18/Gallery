package com.quizango.gallery.ui.gs

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.components.NativeAdCard
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.GalleryTheme
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 2.7.2 GsActivity ("Quick Info" after a call). Its own task (affinity, excluded from recents) so Back returns to the
 * dialer, which is why it is an activity and not a NavHost route. Every action opens the app on a route and finishes.
 */
class QuickInfoActivity : AppCompatActivity() {
    private var wentToBackground = false
    private val refresh = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        NotificationManagerCompat.from(this).cancel(GS_NOTIFICATION_ID)
        val app = application as GalleryApplication
        setContent {
            val themeMode by app.preferences.themeMode.collectAsState(initial = 2)
            GalleryTheme(darkTheme = if (themeMode == 2) androidx.compose.foundation.isSystemInDarkTheme() else themeMode == 1) {
                QuickInfoScreen(
                    app = app,
                    refreshKey = refresh.intValue,
                    onOpen = { route ->
                        startActivity(appRouteIntent(this, route))
                        finish()
                    },
                    onExternal = { intent -> runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish() } }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationManagerCompat.from(this).cancel(GS_NOTIFICATION_ID)
        // A later call reopens the card: fresh duration and back to the first tab
        if (wentToBackground) refresh.intValue++
    }

    override fun onResume() {
        super.onResume()
        wentToBackground = false
    }

    override fun onStop() {
        super.onStop()
        wentToBackground = true
    }
}

/** One fragment_gs_media card: 2.7.2 shows each smart album only while it has items and a cover. */
internal data class GsAlbum(val id: String, @StringRes val title: Int, val count: Int, val cover: Any?)

internal fun gsAlbums(albums: List<Album>, favorites: List<MediaItem>): List<GsAlbum> {
    val smart = albums.associateBy { it.id }
    val cards = listOf(
        "filter:all_photos" to R.string.all_photos,
        "filter:videos" to R.string.videos,
        "filter:camera" to R.string.camera,
        "filter:screenshots" to R.string.screenshot
    ).mapNotNull { (id, title) ->
        smart[id]?.let { GsAlbum(id, title, it.itemCount, if (it.coverPath.isNotEmpty()) it.coverUri else null) }
    } +
        GsAlbum(Screen.Favorites.route, R.string.favorite, favorites.size, favorites.firstOrNull()?.contentUri)
    return cards.filter { it.count > 0 && it.cover != null }
}

/** Where a card opens: All Photos goes Home (2.7.2 "Photos"), Favorite its screen, the rest their media list. */
internal fun gsAlbumRoute(album: GsAlbum, title: String): String = when (album.id) {
    "filter:all_photos" -> Screen.Home.route
    Screen.Favorites.route -> Screen.Favorites.route
    else -> Screen.AlbumDetail.createRoute(album.id, title)
}

@Composable
private fun QuickInfoScreen(app: GalleryApplication, refreshKey: Int, onOpen: (String) -> Unit, onExternal: (Intent) -> Unit) {
    val context = LocalContext.current
    val times by app.preferences.gsCallTimes.collectAsState(initial = null)
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refreshKey) { if (refreshKey > 0) pager.scrollToPage(0) }
    val black = colorResource(R.color.black_color)

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor)).navigationBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.gs_top_bg), RoundedCornerShape(bottomStart = 12.sdp, bottomEnd = 12.sdp))
                .statusBarsPadding()
                .padding(top = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_gs_avatar), null, Modifier.padding(start = 13.sdp).size(37.sdp))
                Column(Modifier.weight(1f).padding(start = 8.sdp, end = 6.sdp)) {
                    Text(
                        stringResource(R.string.caller_private_number), style = GalleryText.BodyLargeSemiBold, fontSize = 11.ssp,
                        color = black, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val (start, end) = times ?: (0L to 0L)
                    Text(
                        stringResource(R.string.incoming_call_with_duration, callDurationText(start, end)),
                        style = GalleryText.BodySmallRegular, fontSize = 9.ssp, color = black, maxLines = 1,
                        modifier = Modifier.padding(top = 2.sdp)
                    )
                }
                CircleAction(R.drawable.ic_gs_call, R.color.gs_call_green, Modifier.padding(end = 10.sdp)) {
                    onExternal(Intent(Intent.ACTION_DIAL))
                }
                CircleAction(R.drawable.ic_gs_message, R.color.gs_message_blue, Modifier.padding(end = 13.sdp)) {
                    onExternal(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING))
                }
            }
            Row(Modifier.padding(horizontal = 13.sdp).padding(top = 16.sdp, bottom = 6.sdp)) {
                HeaderButton(R.drawable.ic_gs_photo, R.string.gs_photos, Modifier.weight(1f)) { onOpen(Screen.Home.route) }
                Spacer(Modifier.width(8.sdp))
                HeaderButton(R.drawable.ic_gs_setting, R.string.title_settings, Modifier.weight(1f)) { onOpen(Screen.Settings.route) }
            }
            GsTabs(pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        }
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 2,
            modifier = Modifier.weight(1f).padding(bottom = 8.sdp)
        ) { page ->
            when (page) {
                0 -> GsAlbumsPage(app) { album -> onOpen(gsAlbumRoute(album, context.getString(album.title))) }
                1 -> GsMorePage(onOpen)
                // shortcut: 2.7.2 fills Games with remote cross-promo ads; we show its empty state, revisit only if real content exists
                else -> GsEmpty()
            }
        }
        NativeAdCard()
    }
}

@Composable
private fun CircleAction(@DrawableRes icon: Int, color: Int, modifier: Modifier, onClick: () -> Unit) {
    Image(
        painterResource(icon), null,
        modifier
            .size(35.sdp)
            .clip(CircleShape)
            .background(colorResource(color))
            .clickable(onClick = onClick)
            .padding(9.sdp)
    )
}

@Composable
private fun HeaderButton(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier, onClick: () -> Unit) {
    val black = colorResource(R.color.black_color)
    Row(
        modifier
            .height(33.sdp)
            .clip(RoundedCornerShape(4.sdp))
            .background(colorResource(R.color.gs_header_button_bg))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.padding(end = 7.sdp).size(19.sdp), colorFilter = ColorFilter.tint(black))
        Text(stringResource(label), style = GalleryText.BodyLargeRegular, fontSize = 11.ssp, color = black)
    }
}

private val GS_TABS = listOf(R.string.albums, R.string.more, R.string.games)

/** TabLayout (fixed, fill) with the 53 x 3 sdp bg_tab_indicator bar under the selected tab. */
@Composable
private fun GsTabs(selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.sdp).padding(bottom = 2.sdp)) {
        val tabWidth = maxWidth / GS_TABS.size
        val barWidth = 53.sdp
        val barOffset by animateDpAsState(tabWidth * selected + (tabWidth - barWidth) / 2, label = "gsTabIndicator")
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            GS_TABS.forEachIndexed { index, title ->
                Box(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(index) }, contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(title), style = GalleryText.BodyMediumMedium, fontSize = 8.ssp, maxLines = 1,
                        color = colorResource(if (index == selected) R.color.colorPrimary else R.color.gs_tab_unselected)
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = barOffset)
                .size(barWidth, 3.sdp)
                .background(colorResource(R.color.colorPrimary), RoundedCornerShape(10.sdp))
        )
    }
}

@Composable
private fun GsAlbumsPage(app: GalleryApplication, onClick: (GsAlbum) -> Unit) {
    val albums by remember {
        combine(app.mediaRepository.getAlbumsFlow(), app.mediaRepository.getFavoritesFlow(), ::gsAlbums)
    }.collectAsState(initial = null)
    val list = albums
    when {
        list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
        list.isEmpty() -> GsEmpty()
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 10.sdp, top = 15.sdp, end = 10.sdp, bottom = 8.sdp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list, key = { it.id }) { album ->
                Column(Modifier.clickable { onClick(album) }.padding(4.sdp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(album.cover).size(200, 200).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.sdp))
                            .background(colorResource(R.color.gs_menu_card_color))
                    )
                    Text(
                        stringResource(album.title), style = GalleryText.BodyMediumRegular, fontSize = 8.ssp,
                        color = colorResource(R.color.black_color), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.sdp)
                    )
                    Text(
                        album.count.toString(), style = GalleryText.BodySmallRegular, fontSize = 7.ssp,
                        color = colorResource(R.color.gs_tab_unselected), maxLines = 1
                    )
                }
            }
        }
    }
}

/** fragment_gs_menu rows: (icon, title, route). */
private val GS_MORE_ROWS = listOf(
    Triple(R.drawable.ic_gs_more_moments, R.string.gs_view_moments, Screen.Moments.route),
    Triple(R.drawable.ic_gs_more_favorite, R.string.favorite, Screen.Favorites.route),
    Triple(R.drawable.ic_gs_more_collage, R.string.collage_maker_explore_title, Screen.CollageMaker.route),
    Triple(R.drawable.ic_gs_more_videos, R.string.videos, null)
)

@Composable
private fun GsMorePage(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 13.sdp, top = 19.sdp, end = 13.sdp)) {
        GS_MORE_ROWS.forEach { (icon, title, route) ->
            // GsMoreRow has no padding: the 18 sdp icon sets the row height
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.sdp)
                    .clip(RoundedCornerShape(6.sdp))
                    .background(colorResource(R.color.gs_menu_card_color))
                    .clickable { onOpen(route ?: Screen.AlbumDetail.createRoute("filter:videos", context.getString(title))) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(icon), null, Modifier.padding(end = 10.sdp).size(18.sdp))
                Text(
                    stringResource(title), style = GalleryText.BodyLargeRegular, fontSize = 11.ssp,
                    color = colorResource(R.color.black), modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GsEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_empty_img), null)
            Text(
                stringResource(R.string.no_items), style = GalleryText.BodyLargeSemiBold, color = colorResource(R.color.black),
                modifier = Modifier.padding(top = 20.sdp)
            )
        }
    }
}
