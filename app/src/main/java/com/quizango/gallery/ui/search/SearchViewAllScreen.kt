package com.quizango.gallery.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.theme.sdp
import java.net.URLDecoder

/**
 * The route arguments as typed values. The nav host hands over the argument text; decoding never throws (an
 * already-decoded "100%" is kept as is) and a section outside Albums / Places / Moments is null, which shows the empty state.
 */
internal fun viewAllArgs(sectionArg: String?, queryArg: String?): Pair<SearchSection?, String> {
    val query = queryArg?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }.orEmpty()
    return SearchSection.of(sectionArg?.toIntOrNull()) to query
}

/**
 * SearchViewAllActivity (§11.4): the toolbar titled with the section and a 3-column grid of every match for the
 * query. Results are recomputed from the current albums, places and moment labels, never handed through the route.
 */
@Composable
fun SearchViewAllScreen(
    sectionArg: String?,
    queryArg: String?,
    mediaRepository: MediaRepository,
    moments: MomentScanner,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val (section, query) = remember(sectionArg, queryArg) { viewAllArgs(sectionArg, queryArg) }
    val index = rememberSearchIndex(mediaRepository, moments)
    val cards = remember(index, section, query) { index?.let { sectionCards(it, section, query) } }

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = section?.let { stringResource(it.title) }.orEmpty(), onBack = onNavigateBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                cards == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                cards.isEmpty() -> PhotosEmptyState(R.string.oops, R.string.search_deleted)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 2.sdp, vertical = 4.sdp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(cards, key = { it.route }) { card -> SearchCardView(card, { onNavigate(card.route) }) }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
