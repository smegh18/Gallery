package com.quizango.gallery.ui.discovery

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.android.gms.maps.MapView
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** The Place screen (MapActivity); [selectedKey] is the place the viewer's Show on map came from, "" from Explore. */
internal fun placeRoute(selectedKey: String = ""): String =
    "place?key=" + URLEncoder.encode(selectedKey, StandardCharsets.UTF_8.toString())

/** The key merged from local.properties into the manifest; blank means no map tiles are requested. */
private fun mapsApiKey(context: Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        .metaData?.getString("com.google.android.geo.API_KEY")
}.getOrNull().orEmpty()

/** MapActivity (§7): a Google map under a draggable sheet of 3-column place cards. Works without a Maps key. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceScreen(
    selectedKey: String,
    mediaRepository: MediaRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val media by remember { mediaRepository.getMediaByFolderFlow("filter:place") }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    val groups by produceState<List<PlaceGroup>?>(null, media) {
        val all = media ?: return@produceState
        value = withContext(Dispatchers.Default) { groupPlaces(all) }
    }
    val background = colorResource(R.color.backgroundColor)

    Column(Modifier.fillMaxSize().background(background)) {
        SharedMediaToolbar(title = stringResource(R.string.geographic_photo), onBack = onNavigateBack)
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(),
            sheetPeekHeight = 165.sdp, // GoogleBottomSheet minHeight _165sdp
            sheetMaxWidth = Dp.Unspecified,
            sheetShape = RoundedCornerShape(topStart = 20.sdp, topEnd = 20.sdp),
            sheetContainerColor = background,
            sheetShadowElevation = 0.sdp,
            sheetDragHandle = {
                Image(
                    painter = painterResource(R.drawable.ic_bottom_sheet_drag),
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 12.sdp)
                )
            },
            containerColor = background,
            sheetContent = { PlaceSheet(groups, onOpen = { onNavigate(placeMediaRoute(it)) }) }
        ) {
            PlaceMap(Modifier.fillMaxSize())
        }
    }
}

/** dataLayout: spinner, "No items" or the item_location grid (imageRecycler, padding 5.sdp h / 30.sdp bottom). */
@Composable
private fun PlaceSheet(groups: List<PlaceGroup>?, onOpen: (PlaceGroup) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when {
            groups == null -> CircularProgressIndicator(
                color = colorResource(R.color.colorPrimary),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.sdp)
            )
            groups.isEmpty() -> Text(
                text = stringResource(R.string.no_items),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.sdp)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 5.sdp, end = 5.sdp, bottom = 30.sdp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groups, key = { it.key }) { group -> PlaceCard(group, onClick = { onOpen(group) }) }
            }
        }
    }
}

/** item_location: margin 4.sdp, 10.sdp-rounded square cover, 2-line title, bare count (String.valueOf). */
@Composable
private fun PlaceCard(group: PlaceGroup, onClick: () -> Unit) {
    Column(Modifier.padding(4.sdp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.sdp))) {
            AsyncImage(
                model = group.items.first().contentUri,
                contentDescription = null,
                placeholder = painterResource(R.drawable.image_loading_albums),
                error = painterResource(R.drawable.image_loading_albums),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = group.key,
            style = GalleryText.BodyMediumMedium,
            color = colorResource(R.color.titleColor),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.sdp).padding(horizontal = 5.sdp)
        )
        Text(
            text = group.items.size.toString(),
            style = GalleryText.BodySmallMedium,
            color = colorResource(R.color.grayTitle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.sdp).padding(horizontal = 5.sdp)
        )
    }
}

/** The map region; without a Maps key nothing is initialised and the area stays backgroundColor. */
@Composable
private fun PlaceMap(modifier: Modifier) {
    val context = LocalContext.current
    val hasKey = remember { mapsApiKey(context).isNotBlank() }
    if (!hasKey) {
        Box(modifier)
        return
    }
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}
