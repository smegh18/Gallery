package com.quizango.gallery.ui.discovery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.navigation.encodeRouteArg
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** The Place screen (MapActivity); [selectedKey] is the place the viewer's Show on map came from, "" from Explore. */
internal fun placeRoute(selectedKey: String = ""): String =
    "place?key=" + encodeRouteArg(selectedKey)

/** What an Allow-stripe permission result means: ask again later, or send the user to the app's settings. */
internal enum class LocationOutcome { GRANTED, RETRY, SETTINGS }

/** Fine or coarse is enough for My Location (the Android 12 "Approximate" choice grants only coarse). */
internal fun locationGranted(fine: Boolean, coarse: Boolean): Boolean = fine || coarse

/** [canAskAgain] is shouldShowRequestPermissionRationale; false after a result with no grant means permanently denied. */
internal fun locationOutcome(fine: Boolean, coarse: Boolean, canAskAgain: Boolean): LocationOutcome = when {
    locationGranted(fine, coarse) -> LocationOutcome.GRANTED
    canAskAgain -> LocationOutcome.RETRY
    else -> LocationOutcome.SETTINGS
}

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun hasLocationPermission(context: Context): Boolean = locationGranted(
    ContextCompat.checkSelfPermission(context, LOCATION_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED,
    ContextCompat.checkSelfPermission(context, LOCATION_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED
)

private tailrec fun Context.activity(): Activity? = this as? Activity ?: (this as? ContextWrapper)?.baseContext?.activity()

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
    val context = LocalContext.current
    val media by remember { mediaRepository.getMediaByFolderFlow("filter:place") }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    val groups by produceState<List<PlaceGroup>?>(null, media) {
        val all = media ?: return@produceState
        value = withContext(Dispatchers.Default) { groupPlaces(all) }
    }
    val background = colorResource(R.color.backgroundColor)
    var selected by rememberSaveable { mutableStateOf(selectedKey) }
    val gridState = rememberLazyGridState()

    // A marker tap (or the viewer's place) brings that card to the top of the sheet
    LaunchedEffect(selected, groups) {
        val index = groups?.indexOfFirst { it.key == selected } ?: -1
        if (index >= 0) gridState.animateScrollToItem(index)
    }

    var locationOk by remember { mutableStateOf(hasLocationPermission(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) { // coming back from the settings screen re-reads the grant
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) locationOk = hasLocationPermission(context) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val deniedToast = stringResource(R.string.home_permission_location)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val activity = context.activity()
        val canAskAgain = activity != null && LOCATION_PERMISSIONS.any { androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        when (locationOutcome(result[LOCATION_PERMISSIONS[0]] == true, result[LOCATION_PERMISSIONS[1]] == true, canAskAgain)) {
            LocationOutcome.GRANTED -> locationOk = true
            LocationOutcome.RETRY -> Unit
            LocationOutcome.SETTINGS -> {
                Toast.makeText(context, deniedToast, Toast.LENGTH_SHORT).show()
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().background(background).navigationBarsPadding()) {
        SharedMediaToolbar(title = stringResource(R.string.geographic_photo), onBack = onNavigateBack)
        if (!locationOk) LocationStripe(onAllow = { launcher.launch(LOCATION_PERMISSIONS) })
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
            sheetContent = { PlaceSheet(groups, gridState, onOpen = { onNavigate(placeMediaRoute(it)) }) }
        ) {
            PlaceMap(groups, selected, locationOk, onSelect = { selected = it }, Modifier.fillMaxSize())
        }
    }
}

/** layout_explore_allow_permission: green stripe, location icon, message and the Allow pill. */
@Composable
private fun LocationStripe(onAllow: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(colorResource(R.color.premium_explore_bg)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_location), contentDescription = null, modifier = Modifier.padding(3.sdp).size(18.sdp))
        Text(
            text = stringResource(R.string.allow_gallery_location),
            style = GalleryText.BodyXSmallSemiBold,
            color = colorResource(R.color.onlyWhite),
            modifier = Modifier.weight(1f).padding(start = 5.sdp, end = 10.sdp)
        )
        Row(
            Modifier.padding(end = 5.sdp)
                .clip(RoundedCornerShape(15.sdp))
                .background(colorResource(R.color.premium_explore_button_bg))
                .border(1.sdp, colorResource(R.color.premium_explore_button), RoundedCornerShape(15.sdp))
                .clickable(onClick = onAllow)
                .padding(horizontal = 10.sdp, vertical = 4.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.allow), style = GalleryText.BodySmallSemiBold, color = colorResource(R.color.premium_explore_button))
            Image(painterResource(R.drawable.ic_location_premium_arrow), contentDescription = null, modifier = Modifier.padding(start = 6.sdp))
        }
    }
}

/** dataLayout: spinner, "No items" or the item_location grid (imageRecycler, padding 5.sdp h / 30.sdp bottom). */
@Composable
private fun PlaceSheet(groups: List<PlaceGroup>?, gridState: LazyGridState, onOpen: (PlaceGroup) -> Unit) {
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
                state = gridState,
                contentPadding = PaddingValues(start = 5.sdp, end = 5.sdp, bottom = 30.sdp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groups, key = { it.key }) { group -> PlaceCard(group, onClick = { onOpen(group) }) }
            }
        }
    }
}

/** item_location: margin 4.sdp, 10.sdp-rounded square face (cover or mosaic), 2-line title, bare count. */
@Composable
private fun PlaceCard(group: PlaceGroup, onClick: () -> Unit) {
    Column(Modifier.padding(4.sdp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.sdp))
                .background(colorResource(R.color.other_album_bg_color))
        ) {
            val rows = placeMosaicRows(group.items.size)
            if (rows.isEmpty()) {
                PlaceCover(group.items.first(), Modifier.fillMaxSize())
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    rows.forEach { row ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { i -> PlaceCover(group.items[i], Modifier.weight(1f).fillMaxSize()) }
                        }
                    }
                }
            }
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

@Composable
private fun PlaceCover(item: MediaItem, modifier: Modifier) = AsyncImage(
    model = item.contentUri,
    contentDescription = null,
    placeholder = painterResource(R.drawable.image_loading_albums),
    error = painterResource(R.drawable.image_loading_albums),
    contentScale = ContentScale.Crop,
    modifier = modifier
)

/** The map region; without a Maps key nothing is initialised and the area stays backgroundColor. */
@Composable
private fun PlaceMap(
    groups: List<PlaceGroup>?,
    selectedKey: String,
    locationOk: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
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

    var map by remember { mutableStateOf<GoogleMap?>(null) }
    LaunchedEffect(mapView) { map = suspendCancellableCoroutine { c -> mapView.getMapAsync { c.resume(it) } } }

    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(map) { map?.setOnMarkerClickListener { m -> (m.tag as? String)?.let(select); true } }

    // My Location only after the runtime grant; a revoked grant surfaces as SecurityException and leaves the map usable
    LaunchedEffect(map, locationOk) {
        val m = map ?: return@LaunchedEffect
        if (locationOk) try {
            m.isMyLocationEnabled = true
            m.uiSettings.isMyLocationButtonEnabled = true
        } catch (_: SecurityException) {
        }
    }

    val density = LocalDensity.current
    val spec = MarkerSpec(thumb = with(density) { 60.sdp.toPx() }, pad = with(density) { 6.sdp.toPx() }, label = with(density) { 10.ssp.toPx() })
    val backgroundArgb = colorResource(R.color.backgroundColor).toArgb()
    val labelArgb = colorResource(R.color.onlyBlack).toArgb()
    LaunchedEffect(map, groups) {
        val m = map ?: return@LaunchedEffect
        m.clear()
        for (group in groups.orEmpty()) {
            val anchor = group.anchor ?: continue
            val thumb = loadThumb(context, anchor.contentUri, spec.thumb.toInt())
            val bitmap = withContext(Dispatchers.Default) { markerBitmap(thumb, group.key, spec, backgroundArgb, labelArgb) }
            m.addMarker(
                MarkerOptions().position(LatLng(anchor.latitude.toDouble(), anchor.longitude.toDouble()))
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)).anchor(0.5f, 1f)
            )?.tag = group.key
        }
    }

    // Selecting a place animates the camera to it (newLatLngZoom); with none selected, open on the newest place
    LaunchedEffect(map, groups, selectedKey) {
        val m = map ?: return@LaunchedEffect
        val all = groups.orEmpty()
        val target = all.firstOrNull { it.key == selectedKey }?.anchor
        if (target != null) m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude.toDouble(), target.longitude.toDouble()), 14f))
        else all.firstNotNullOfOrNull { it.anchor }?.let { m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude.toDouble(), it.longitude.toDouble()), 5f)) }
    }
}

private class MarkerSpec(val thumb: Float, val pad: Float, val label: Float)

private suspend fun loadThumb(context: Context, data: Any, size: Int): Bitmap? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context).data(data).size(size).allowHardware(false).build()
    (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
}

/**
 * item_map_marker as a bitmap: the thumbnail in a backgroundColor bubble (rounded, bottom-start corner small) with
 * the place name below it. Drawn with plain Canvas because a map marker icon cannot be a composable.
 */
private fun markerBitmap(thumb: Bitmap?, name: String, spec: MarkerSpec, bubbleColor: Int, labelColor: Int): Bitmap {
    val card = spec.thumb + 2 * spec.pad
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = labelColor; textSize = spec.label; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val label = TextUtils.ellipsize(name, android.text.TextPaint(text), card * 2, TextUtils.TruncateAt.END).toString()
    val labelHeight = text.fontMetrics.let { it.descent - it.ascent }
    val width = maxOf(card, text.measureText(label)).toInt()
    val bitmap = Bitmap.createBitmap(width, (card + spec.pad + labelHeight).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val left = (width - card) / 2f
    val big = card / 2f
    val bubble = Path().apply {
        addRoundRect(RectF(left, 0f, left + card, card), floatArrayOf(big, big, big, big, big, big, spec.pad, spec.pad), Path.Direction.CW)
    }
    canvas.drawPath(bubble, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bubbleColor })
    if (thumb != null) {
        val inner = RectF(left + spec.pad, spec.pad, left + spec.pad + spec.thumb, spec.pad + spec.thumb)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(inner, spec.thumb / 2f, spec.thumb / 2f, Path.Direction.CW) })
        val side = minOf(thumb.width, thumb.height)
        val src = android.graphics.Rect((thumb.width - side) / 2, (thumb.height - side) / 2, (thumb.width + side) / 2, (thumb.height + side) / 2)
        canvas.drawBitmap(thumb, src, inner, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }
    text.textAlign = Paint.Align.CENTER
    canvas.drawText(label, width / 2f, card + spec.pad - text.fontMetrics.ascent, text)
    return bitmap
}
