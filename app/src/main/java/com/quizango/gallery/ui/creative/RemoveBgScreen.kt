package com.quizango.gallery.ui.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.components.PhotoPicker
import com.quizango.gallery.ui.editor.DiscardChangesDialog
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Remove BG background behind the cut-out subject (de/c tiles). */
private sealed interface RemoveBgBackdrop {
    data object Transparent : RemoveBgBackdrop
    data class Solid(val color: Color) : RemoveBgBackdrop
    data class Picture(val bitmap: Bitmap) : RemoveBgBackdrop
}

/** array remove_bg_color: tile 0 Gallery, 2 colour picker, 4 transparent; the rest are swatches (black / white follow the theme). */
private const val TILE_GALLERY = 0
private const val TILE_PICKER = 2
private const val TILE_TRANSPARENT = 4
private val SWATCHES = listOf(
    -1, R.color.color_gray, -1, R.color.color_dark_gray, -1, R.color.color_light_pink, R.color.black, R.color.color_pink,
    R.color.white, R.color.color_dark_pink, R.color.yellow_color_picker, R.color.yellow_green_color_picker,
    R.color.sky_blue_color_picker, R.color.color_dark_green, R.color.color_dark_purple, R.color.sticker_hint_color
)

/**
 * activity_remove_bg (S272 RemoveBgActivity): guide on first use, ML Kit subject segmentation, then the subject as a
 * movable sticker over the chosen background. Done writes the result over the editor's working copy [imagePath].
 */
@Composable
fun RemoveBgScreen(
    imagePath: String,
    mediaRepository: MediaRepository,
    preferences: AppPreferences,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guideSeen by remember { preferences.oneTimeFlag(AiTool.REMOVE_BG.guideKey) }.collectAsState(initial = null)
    var started by rememberSaveable { mutableStateOf(false) }
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var subject by remember { mutableStateOf<Pair<Bitmap, IntArray>?>(null) }
    var segmenting by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) } // after the 1000 ms loader
    var backdrop by remember { mutableStateOf<RemoveBgBackdrop>(RemoveBgBackdrop.Transparent) }
    var selectedTile by remember { mutableIntStateOf(TILE_TRANSPARENT) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var turn by remember { mutableFloatStateOf(0f) }
    var fit by remember { mutableFloatStateOf(1f) } // screen px per original px
    var saving by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    var pickingImage by remember { mutableStateOf(false) }
    var pickingColor by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val showGuide = !started && showsGuide(guideSeen)

    LaunchedEffect(guideSeen) { if (guideSeen == true) started = true }
    LaunchedEffect(Unit) { original = withContext(Dispatchers.IO) { ImageSegmentationHelper.decodeUpright(imagePath) } }
    LaunchedEffect(started, original) {
        val src = original ?: return@LaunchedEffect
        if (!started || subject != null) return@LaunchedEffect
        val cut = ImageSegmentationHelper.extractSubject(context, src)?.let { withContext(Dispatchers.Default) { ImageSegmentationHelper.trim(it) } }
        segmenting = false
        if (cut == null) {
            val msg = if (ImageSegmentationHelper.isOnline(context)) R.string.ml_kit_module_error else R.string.please_turn_on_internet_and_try_again
            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        subject = cut
        delay(1000) // dd/g: remove_bg_loader plays one second before the transparent backdrop shows
        ready = true
    }

    fun save() {
        val src = original ?: return
        val cut = subject
        if (!ready || cut == null) {
            Toast.makeText(context, context.getString(R.string.progress_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (saving) return
        saving = true
        scope.launch {
            val out = withContext(Dispatchers.Default) {
                when (val b = backdrop) {
                    RemoveBgBackdrop.Transparent -> cut.first // dd/v: the trimmed cut-out alone
                    else -> renderOnBackdrop(src.width, src.height, b, cut, pan.x / fit, pan.y / fit, zoom, turn)
                }
            }
            if (ImageSegmentationHelper.writePng(out, imagePath)) onSaved() else {
                saving = false
                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = !showGuide) { discarding = true }
    BackHandler(enabled = pickingImage) { pickingImage = false }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            SharedMediaToolbar(title = stringResource(R.string.removed_background), onBack = { discarding = true })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val src = original
                    if (src != null) {
                        val density = LocalDensity.current
                        val scale = minOf(constraints.maxWidth / src.width.toFloat(), constraints.maxHeight / src.height.toFloat())
                        SideEffect { fit = scale }
                        val w = with(density) { (src.width * scale).toDp() }
                        val h = with(density) { (src.height * scale).toDp() }
                        Box(
                            Modifier
                                .size(w, h)
                                .pointerInput(ready) {
                                    if (ready) detectTransformGestures { _, move, scale, rotation ->
                                        pan += move; zoom *= scale; turn += rotation
                                    }
                                }
                        ) {
                            val cut = subject
                            if (!ready || cut == null) {
                                Image(src.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                            } else {
                                when (val b = backdrop) {
                                    RemoveBgBackdrop.Transparent -> Image(
                                        painterResource(R.drawable.transparent_background), contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(colorResource(R.color.onlyWhite))
                                    )
                                    is RemoveBgBackdrop.Solid -> Box(Modifier.fillMaxSize().background(b.color))
                                    is RemoveBgBackdrop.Picture -> Image(b.bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Image(
                                    bitmap = cut.first.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .offset { IntOffset((cut.second[0] * scale).toInt(), (cut.second[1] * scale).toInt()) }
                                        .size(with(density) { (cut.first.width * scale).toDp() }, with(density) { (cut.first.height * scale).toDp() })
                                        .graphicsLayer { translationX = pan.x; translationY = pan.y; scaleX = zoom; scaleY = zoom; rotationZ = turn }
                                )
                            }
                            if (subject != null && !ready) RemoveBgLoader(230, Modifier.align(Alignment.Center))
                        }
                    }
                }
                if (segmenting && started) SegmentingBar(Modifier.align(Alignment.TopCenter))
            }
            ToolSheet {
                ToolSheetHeader(
                    title = stringResource(R.string.add_background),
                    onClose = { discarding = true },
                    onDone = ::save,
                    modifier = Modifier.padding(top = 5.sdp)
                )
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.sdp).height(116.sdp) // two rows of 48sdp tiles with 5sdp margins
                ) {
                    items(SWATCHES.size) { i ->
                        val swatch = if (SWATCHES[i] == -1) null else colorResource(SWATCHES[i])
                        BackdropTile(i, swatch, selected = i == selectedTile, enabled = ready) {
                            when (i) {
                                TILE_GALLERY -> pickingImage = true
                                TILE_PICKER -> pickingColor = true
                                else -> {
                                    backdrop = swatch?.let { RemoveBgBackdrop.Solid(it) } ?: RemoveBgBackdrop.Transparent
                                    selectedTile = i
                                }
                            }
                        }
                    }
                }
            }
        }
        if (saving) SavingLayer()
        if (pickingImage) PhotoPicker(mediaRepository, multi = false, onBack = { pickingImage = false }) { picked ->
            pickingImage = false
            scope.launch {
                withContext(Dispatchers.IO) { ImageSegmentationHelper.decodeUpright(picked.first().filePath) }?.let {
                    backdrop = RemoveBgBackdrop.Picture(it)
                    selectedTile = -1
                }
            }
        }
        if (showGuide) AiToolGuide(
            tool = AiTool.REMOVE_BG,
            onContinue = { scope.launch { preferences.setOneTimeFlag(AiTool.REMOVE_BG.guideKey) }; started = true },
            onCancel = onNavigateBack
        )
    }

    LaunchedEffect(ready) {
        // RB isRemoveBgScrollToolTip: once, 2000 ms in, the swatches scroll to the end and 800 ms later back
        if (!ready || preferences.oneTimeFlag(SCROLL_HINT_KEY).first()) return@LaunchedEffect
        preferences.setOneTimeFlag(SCROLL_HINT_KEY)
        delay(2000)
        gridState.animateScrollToItem(SWATCHES.lastIndex)
        delay(800)
        gridState.animateScrollToItem(0)
    }

    if (pickingColor) {
        val current = (backdrop as? RemoveBgBackdrop.Solid)?.color ?: colorResource(R.color.onlyWhite)
        ColorPickerDialog(current, onPick = { backdrop = RemoveBgBackdrop.Solid(it); selectedTile = TILE_PICKER; pickingColor = false }) { pickingColor = false }
    }
    if (discarding) DiscardChangesDialog(onDiscard = { discarding = false; onNavigateBack() }, onDismiss = { discarding = false })
}

private const val SCROLL_HINT_KEY = "isRemoveBgScrollToolTip"

/** item_remove_bg_color / item_remove_bg_gallery: 48sdp square with the selection border on top. */
@Composable
private fun BackdropTile(index: Int, swatch: Color?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.sdp)
    if (index == TILE_GALLERY) {
        Column(
            Modifier
                .padding(5.sdp)
                .size(48.sdp)
                .drawableBackground(R.drawable.ic_color_square_border_default)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painterResource(R.drawable.ic_gallery), contentDescription = null, modifier = Modifier.size(24.sdp).padding(2.dp))
            Text(stringResource(R.string.gallery), style = GalleryText.BodyXXSmallMedium, color = colorResource(R.color.titleColor))
        }
        return
    }
    Box(Modifier.padding(5.sdp).size(48.sdp).clip(shape).clickable(enabled = enabled, onClick = onClick)) {
        when (index) {
            TILE_PICKER -> TileImage(R.drawable.ic_remove_bg_color_picker)
            TILE_TRANSPARENT -> TileImage(R.drawable.ic_transparent_round)
            else -> Box(Modifier.fillMaxSize().background(swatch ?: Color.Transparent))
        }
        Box(Modifier.fillMaxSize().drawableBackground(if (selected) R.drawable.ic_color_square_border else R.drawable.ic_color_square_default))
    }
}

@Composable
private fun TileImage(@DrawableRes id: Int) =
    Image(painterResource(id), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

/** dd/v colour / image save: clImagesHolder redrawn at the original size, the subject with the user's move, zoom and turn. */
private fun renderOnBackdrop(
    width: Int,
    height: Int,
    backdrop: RemoveBgBackdrop,
    cut: Pair<Bitmap, IntArray>,
    dx: Float,
    dy: Float,
    zoom: Float,
    turn: Float
): Bitmap {
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    when (backdrop) {
        is RemoveBgBackdrop.Solid -> canvas.drawColor(backdrop.color.toArgb())
        is RemoveBgBackdrop.Picture -> canvas.drawBitmap(backdrop.bitmap, centerCrop(backdrop.bitmap, width, height), Rect(0, 0, width, height), paint)
        RemoveBgBackdrop.Transparent -> Unit
    }
    val (bmp, at) = cut
    val cx = at[0] + bmp.width / 2f
    val cy = at[1] + bmp.height / 2f
    val m = Matrix().apply {
        postTranslate(at[0].toFloat(), at[1].toFloat())
        postScale(zoom, zoom, cx, cy)
        postRotate(turn, cx, cy)
        postTranslate(dx, dy)
    }
    canvas.drawBitmap(bmp, m, paint)
    return out
}

/** The source rectangle ContentScale.Crop shows of [bmp] in a [w] x [h] frame. */
private fun centerCrop(bmp: Bitmap, w: Int, h: Int): Rect {
    val scale = maxOf(w / bmp.width.toFloat(), h / bmp.height.toFloat())
    val sw = (w / scale).toInt()
    val sh = (h / scale).toInt()
    val l = (bmp.width - sw) / 2
    val t = (bmp.height - sh) / 2
    return Rect(l, t, l + sw, t + sh)
}
