package com.quizango.gallery.ui.creative

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.ui.search.SearchToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * activity_sticker (StickerActivity), the editor's "Add Sticker" picker: the search toolbar with the
 * "Explore Stickers" hint over a 3-column grid of stickers. 2.7.2 fetches them from its sticker API
 * (getStickers) and shows emptyLayout when that returns nothing; we make no network calls, so the grid is
 * always empty and the screen shows "No Stickers", as 2.7.2 does offline. Back returns with no sticker.
 */
@Composable
fun StickerScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SearchToolbar(query = query, onQueryChange = { query = it }, onBack = onBack, hint = R.string.explore_stickers)
        Box(Modifier.weight(1f).navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.ic_empty_img), contentDescription = null)
                Text(
                    stringResource(R.string.no_stickers),
                    style = GalleryText.BodyLargeSemiBold,
                    color = colorResource(R.color.titleColor),
                    modifier = Modifier.padding(top = 20.sdp, bottom = 10.sdp)
                )
            }
        }
    }
}
