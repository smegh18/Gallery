package com.quizango.gallery.ui.creative.collage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.components.PhotoPicker
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One grid cell's photo and its cell-tool edits: quarter turns, the rotate ruler, flips, zoom and nudges (fractions of the cell). */
internal data class CellEdit(
    val path: String,
    val quarter: Int = 0,
    val tilt: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

internal sealed interface CollageBg {
    data class Solid(val color: Int) : CollageBg
    data class Blur(val radius: Int) : CollageBg
    data class Image(val path: String) : CollageBg
}

internal enum class BrushKind { PAINT, NEON, ERASE }

/** A brush stroke; points are fractions of the collage so the stroke survives a ratio change. */
internal data class BrushStroke(val points: List<Offset>, val color: Int, val size: Int, val kind: BrushKind)

/** Everything a panel can change, so Back / close restores the copy taken when the panel opened. */
internal data class CollageState(
    val cells: List<CellEdit>,
    val template: Int = 0,
    val ratio: Int = 0,
    val radius: Int = DEFAULT_RADIUS,
    val padding: Int = DEFAULT_PADDING,
    val background: CollageBg = CollageBg.Solid(WHITE),
    val strokes: List<BrushStroke> = emptyList(),
    val texts: List<CollageText> = emptyList()
)

private const val WHITE = 0xFFFFFFFF.toInt()
private const val BLACK = 0xFF000000.toInt()

/** rvTools (CollageScreenActivity 18917-19151). */
private enum class CollageTool(@DrawableRes val icon: Int, @StringRes val label: Int) {
    LAYOUT(R.drawable.ic_collage, R.string.layout),
    RATIO(R.drawable.ic_ratio, R.string.ratio),
    BACKGROUND(R.drawable.ic_bg, R.string.background),
    BORDER(R.drawable.ic_border, R.string.border),
    BRUSH(R.drawable.ic_paint, R.string.brush),
    TEXT(R.drawable.ic_text, R.string.text),
    STICKER(R.drawable.ic_sticker_collage, R.string.sticker)
}

/** The panel an open tool shows; its title goes in cToolbar. */
private enum class Panel(@StringRes val title: Int) {
    LAYOUT(R.string.layout), BORDER(R.string.border), BACKGROUND(R.string.background), RATIO(R.string.ratio),
    BRUSH(R.string.brush), TEXT(R.string.text), STICKER(R.string.sticker), ROTATE(R.string.rotate)
}

/** Panels opened from inside another one: Ratio/size from Layout, Blur Bg from Background. */
private enum class SubPanel { RATIO, BLUR }

private enum class PickFor { REPLACE, WALLPAPER }

/** A template cell in pixels: the gap between cells is [pad], and so is the outer margin. */
internal fun cellRect(c: UnitRect, width: Float, height: Float, pad: Float): Rect {
    val l = c.left * width + if (c.left == 0f) pad else pad / 2
    val t = c.top * height + if (c.top == 0f) pad else pad / 2
    val r = c.right * width - if (c.right == 1f) pad else pad / 2
    val b = c.bottom * height - if (c.bottom == 1f) pad else pad / 2
    return Rect(l, t, max(l, r), max(t, b))
}

/**
 * activity_collage_screen (CollageScreenActivity): the photo grid under the Collage toolbar, the tool strip and its
 * panels, cell tools on a tapped photo, the exit popup on Back and Save into Pictures/Gallery Collage.
 */
@Composable
internal fun CollageScreen(paths: List<String>, mediaRepository: MediaRepository, onExit: () -> Unit, onSaved: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = remember(paths.size) { collageTemplates(paths.size) }
    var state by remember { mutableStateOf(CollageState(paths.map { CellEdit(it) })) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var before by remember { mutableStateOf<CollageState?>(null) }
    var sub by remember { mutableStateOf<SubPanel?>(null) }
    var subBefore by remember { mutableStateOf<CollageState?>(null) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var swapFrom by remember { mutableStateOf<Int?>(null) }
    var showColors by remember { mutableStateOf(false) }
    var pickingColor by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf<CollageText?>(null) }
    var pickFor by remember { mutableStateOf<PickFor?>(null) }
    var exitAsked by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveReady by remember { mutableStateOf(false) }
    var brushTab by remember { mutableStateOf(BrushTab.PAINT) }
    var brushSize by remember { mutableIntStateOf(DEFAULT_BRUSH) }
    var brushColor by remember { mutableIntStateOf(COLLAGE_PAINT_COLORS.first()) }
    var redo by remember { mutableStateOf(emptyList<BrushStroke>()) }
    var drawing by remember { mutableStateOf(emptyList<Offset>()) }

    val bitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }
    val layer = rememberGraphicsLayer()
    val maxSide = remember(paths.size) { (1600 / ceil(sqrt(paths.size.toDouble()))).toInt() }
    val wanted = state.cells.map { it.path } + listOfNotNull((state.background as? CollageBg.Image)?.path)
    LaunchedEffect(wanted) {
        wanted.filter { it !in bitmaps }.forEach { p -> loadPhoto(context, p, maxSide)?.let { bitmaps[p] = it } }
    }
    val loaded = state.cells.all { it.path in bitmaps }
    LaunchedEffect(Unit) {
        delay(1000) // mSave stays at alpha 0.3 and unclickable for about a second
        saveReady = true
    }

    fun open(p: Panel) {
        before = state
        panel = p
        selected = null
        swapFrom = null
    }

    fun close(revert: Boolean) {
        if (revert) before?.let { state = it }
        panel = null
        before = null
        sub = null
        showColors = false
        pickingColor = false
        drawing = emptyList()
    }

    fun openSub(s: SubPanel) {
        subBefore = state
        sub = s
    }

    fun closeSub(revert: Boolean) {
        if (revert) subBefore?.let { state = it }
        sub = null
        subBefore = null
    }

    fun updateCell(i: Int, f: (CellEdit) -> CellEdit) {
        state = state.copy(cells = state.cells.toMutableList().also { it[i] = f(it[i]) })
    }

    fun back() {
        when {
            sub != null -> closeSub(true)
            panel != null -> close(true)
            swapFrom != null -> swapFrom = null
            selected != null -> selected = null
            else -> exitAsked = true
        }
    }
    BackHandler { if (!saving) back() }

    fun onTool(t: CollageTool) {
        when (t) {
            CollageTool.LAYOUT -> open(Panel.LAYOUT)
            CollageTool.RATIO -> open(Panel.RATIO)
            CollageTool.BACKGROUND -> open(Panel.BACKGROUND)
            CollageTool.BORDER -> open(Panel.BORDER)
            CollageTool.BRUSH -> {
                open(Panel.BRUSH)
                redo = emptyList()
            }
            CollageTool.TEXT -> {
                // The add-text panel only appears once a text is placed (ie/p)
                before = state
                selected = null
                editingText = CollageText(id = System.nanoTime())
            }
            CollageTool.STICKER -> open(Panel.STICKER)
        }
    }

    fun onCellTool(t: CellTool) {
        val i = selected ?: return
        when (t) {
            CellTool.REPLACE -> pickFor = PickFor.REPLACE
            CellTool.SWAP -> swapFrom = i
            CellTool.ROTATE -> {
                before = state
                panel = Panel.ROTATE
            }
            CellTool.VERTICAL -> updateCell(i) { it.copy(flipV = !it.flipV) }
            CellTool.HORIZONTAL -> updateCell(i) { it.copy(flipH = !it.flipH) }
            CellTool.RIGHT -> updateCell(i) { it.copy(quarter = (it.quarter + 1) % 4) }
        }
    }

    fun onRotateOption(i: Int, o: RotateOption) = updateCell(i) {
        when (o) {
            RotateOption.ROTATE_90 -> it.copy(quarter = (it.quarter + 1) % 4)
            RotateOption.ZOOM_IN -> it.copy(zoom = min(it.zoom * 1.1f, 4f))
            RotateOption.ZOOM_OUT -> it.copy(zoom = max(it.zoom / 1.1f, 0.5f))
            RotateOption.LEFT -> it.copy(panX = it.panX - 0.05f)
            RotateOption.RIGHT -> it.copy(panX = it.panX + 0.05f)
            RotateOption.UP -> it.copy(panY = it.panY - 0.05f)
            RotateOption.DOWN -> it.copy(panY = it.panY + 0.05f)
        }
    }

    fun onCellTap(i: Int?) {
        val from = swapFrom
        when {
            i == null -> {
                selected = null
                swapFrom = null
            }
            from != null -> {
                if (from != i) state = state.copy(cells = state.cells.toMutableList().also { val a = it[from]; it[from] = it[i]; it[i] = a })
                swapFrom = null
                selected = i
            }
            else -> selected = if (selected == i) null else i
        }
    }

    fun editText(t: CollageText) {
        if (panel == null) before = state
        selected = null
        editingText = t
    }

    fun save() {
        if (!saveReady || !loaded || saving) return
        selected = null
        swapFrom = null
        saving = true
        scope.launch {
            val shot = runCatching {
                val b = layer.toImageBitmap().asAndroidBitmap()
                if (Build.VERSION.SDK_INT >= 26 && b.config == Bitmap.Config.HARDWARE) b.copy(Bitmap.Config.ARGB_8888, false) else b
            }.getOrNull()
            val uri = shot?.let { saveCollage(context, it) }
            if (uri == null) {
                saving = false
                Toast.makeText(context, context.getString(R.string.Something_went_wrong_message), Toast.LENGTH_LONG).show()
                return@launch
            }
            mediaRepository.syncMediaStore()
            delay(3000) // hr/a opens the saved screen 3 s after the write
            onSaved(uri)
        }
    }

    fun sampleColor(at: Offset, size: IntSize) {
        pickingColor = false
        scope.launch {
            val b = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull() ?: return@launch
            val soft = if (Build.VERSION.SDK_INT >= 26 && b.config == Bitmap.Config.HARDWARE) b.copy(Bitmap.Config.ARGB_8888, false) else b
            val x = (at.x / size.width * soft.width).toInt().coerceIn(0, soft.width - 1)
            val y = (at.y / size.height * soft.height).toInt().coerceIn(0, soft.height - 1)
            state = state.copy(background = CollageBg.Solid(soft.getPixel(x, y)))
        }
    }

    val ink = colorResource(R.color.collage_header_text_color)
    val loader by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loader_animation))
    Box(Modifier.fillMaxSize().background(colorResource(R.color.picker_toolbar_color))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            CollageHeader(
                title = sub?.let { if (it == SubPanel.RATIO) R.string.ratio else null } ?: panel?.title,
                onBack = ::back,
                saveEnabled = saveReady && loaded,
                onSave = ::save,
                brush = panel == Panel.BRUSH,
                canUndo = state.strokes.size > (before?.strokes?.size ?: 0),
                canRedo = redo.isNotEmpty(),
                onUndo = {
                    state.strokes.lastOrNull()?.let { last ->
                        state = state.copy(strokes = state.strokes.dropLast(1))
                        redo = redo + last
                    }
                },
                onRedo = {
                    redo.lastOrNull()?.let { next ->
                        state = state.copy(strokes = state.strokes + next)
                        redo = redo.dropLast(1)
                    }
                }
            )
            Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border_collage)))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 5.sdp)) {
                val ratio = COLLAGE_RATIOS[state.ratio]
                val margin = 3.sdp
                val availW = maxWidth - margin * 2
                val availH = maxHeight - margin * 2
                val aspect = ratio.width.toFloat() / ratio.height
                val w = if (availW / availH > aspect) availH * aspect else availW
                val h = if (availW / availH > aspect) availH else availW / aspect
                val density = LocalDensity.current
                val radiusPx = with(density) { state.radius.dp.toPx() }
                val padPx = with(density) { state.padding.dp.toPx() }
                val cells = templates[state.template].cells()
                val first = bitmaps[state.cells.first().path]
                val blurRadius = (state.background as? CollageBg.Blur)?.radius
                val blurred = remember(first, blurRadius) { if (first != null && blurRadius != null) downscaleBlur(first, blurRadius) else null }
                val gridTouch = panel == null && !pickingColor
                val tap by rememberUpdatedState { at: Offset, size: IntSize ->
                    if (pickingColor) sampleColor(at, size)
                    else if (gridTouch) onCellTap(cells.indexOfFirst { cellRect(it, size.width.toFloat(), size.height.toFloat(), padPx).contains(at) }.takeIf { it >= 0 })
                }
                Box(Modifier.align(Alignment.TopCenter).padding(margin).size(w, h)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { tap(it, size) } }) {
                            drawCollage(state, cells, bitmaps, radiusPx, padPx, blurred)
                        }
                        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
                            drawStrokes(state.strokes, density.density)
                            if (drawing.isNotEmpty()) drawStrokes(listOf(BrushStroke(drawing, brushColor, brushSize, brushTab.kind)), density.density)
                        }
                        val textsMovable = panel == null || panel == Panel.TEXT
                        state.texts.forEach { t ->
                            CollageTextLabel(
                                t,
                                Modifier
                                    .centerAt(t.x, t.y)
                                    .pointerInput(t.id, textsMovable) {
                                        if (textsMovable) detectDragGestures { change, drag ->
                                            change.consume()
                                            state = state.copy(texts = state.texts.map {
                                                if (it.id == t.id) it.copy(
                                                    x = (it.x + drag.x / (w.toPx())).coerceIn(0f, 1f),
                                                    y = (it.y + drag.y / (h.toPx())).coerceIn(0f, 1f)
                                                ) else it
                                            })
                                        }
                                    }
                                    .pointerInput(t.id, textsMovable) { if (textsMovable) detectTapGestures { editText(t) } }
                            )
                        }
                    }
                    // PerfectGridView's selection frame, outside the recorded layer so it never lands in the saved image
                    val frame = colorResource(R.color.mainAppColor)
                    Canvas(Modifier.fillMaxSize()) {
                        listOfNotNull(selected, swapFrom).distinct().forEach { i ->
                            val r = cellRect(cells.getOrNull(i) ?: return@forEach, size.width, size.height, padPx)
                            val rr = min(radiusPx, min(r.width, r.height) / 2)
                            drawRoundRect(frame, r.topLeft, r.size, CornerRadius(rr), style = Stroke(2.dp.toPx()))
                        }
                    }
                    if (panel == Panel.BRUSH) Box(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { drawing = listOf(Offset(it.x / size.width, it.y / size.height)) },
                                onDragEnd = {
                                    if (drawing.isNotEmpty()) {
                                        state = state.copy(strokes = state.strokes + BrushStroke(drawing, brushColor, brushSize, brushTab.kind))
                                        redo = emptyList()
                                    }
                                    drawing = emptyList()
                                },
                                onDragCancel = { drawing = emptyList() }
                            ) { change, _ ->
                                change.consume()
                                drawing = drawing + Offset(change.position.x / size.width, change.position.y / size.height)
                            }
                        }
                    )
                }
                // iView: the loader while the photos decode
                if (!loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LottieAnimation(
                        loader,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(80.sdp).graphicsLayer(scaleX = 2f, scaleY = 2f)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border_collage)))
            when {
                sub == SubPanel.RATIO || panel == Panel.RATIO -> RatioPanel(
                    selected = state.ratio,
                    onSelect = { state = state.copy(ratio = it) },
                    onClose = { if (sub != null) closeSub(true) else close(true) },
                    onDone = { if (sub != null) closeSub(false) else close(false) }
                )
                sub == SubPanel.BLUR -> BlurPanel(
                    value = blurRadiusOf(state.background),
                    onChange = { state = state.copy(background = CollageBg.Blur(it)) },
                    onClose = { closeSub(true) },
                    onDone = { closeSub(false) }
                )
                panel == Panel.LAYOUT || panel == Panel.BORDER || panel == Panel.BACKGROUND -> LayoutTabsPanel(
                    tab = when (panel) {
                        Panel.BORDER -> LayoutTab.BORDER
                        Panel.BACKGROUND -> LayoutTab.BACKGROUND
                        else -> LayoutTab.LAYOUT
                    },
                    onTab = {
                        panel = when (it) {
                            LayoutTab.LAYOUT -> Panel.LAYOUT
                            LayoutTab.BORDER -> Panel.BORDER
                            LayoutTab.BACKGROUND -> Panel.BACKGROUND
                        }
                        showColors = false
                        pickingColor = false
                    },
                    onDone = { close(false) }
                ) {
                    when (panel) {
                        Panel.LAYOUT -> LayoutSection(templates, state.template, { state = state.copy(template = it) }) { openSub(SubPanel.RATIO) }
                        Panel.BORDER -> BorderSection(
                            state.radius,
                            state.padding,
                            { state = state.copy(radius = it) },
                            { state = state.copy(padding = it) }
                        )
                        else -> if (showColors) ColorSwatches(
                            colors = COLLAGE_BG_COLORS,
                            selected = (state.background as? CollageBg.Solid)?.color,
                            onBack = { showColors = false },
                            onPick = { state = state.copy(background = CollageBg.Solid(it)) }
                        ) else BackgroundSection(bgOptionOf(state.background)) { o ->
                            when (o) {
                                BgOption.BLUR -> {
                                    openSub(SubPanel.BLUR)
                                    state = state.copy(background = CollageBg.Blur(blurRadiusOf(state.background)))
                                }
                                BgOption.CUSTOM -> pickFor = PickFor.WALLPAPER
                                BgOption.SELECT -> pickingColor = true
                                BgOption.PNG -> state = state.copy(background = CollageBg.Solid(0))
                                BgOption.WHITE -> state = state.copy(background = CollageBg.Solid(WHITE))
                                BgOption.BLACK -> state = state.copy(background = CollageBg.Solid(BLACK))
                                BgOption.COLOR -> showColors = true
                            }
                        }
                    }
                }
                panel == Panel.BRUSH -> BrushPanel(
                    tab = brushTab,
                    onTab = { brushTab = it },
                    size = brushSize,
                    onSize = { brushSize = it },
                    color = brushColor,
                    onColor = { brushColor = it },
                    onDone = { close(false) }
                )
                panel == Panel.TEXT -> AddTextPanel(
                    onAdd = { editingText = CollageText(id = System.nanoTime()) },
                    onClose = { close(true) },
                    onDone = { close(false) }
                )
                panel == Panel.STICKER -> StickerOfflinePanel(onRetry = {})
                panel == Panel.ROTATE && selected != null -> {
                    val i = selected ?: 0
                    RotatePanel(
                        tilt = state.cells[i].tilt,
                        onTilt = { v -> updateCell(i) { it.copy(tilt = v) } },
                        onOption = { onRotateOption(i, it) },
                        onDone = { close(false) }
                    )
                }
                selected != null -> CellToolsBar(onClose = { selected = null; swapFrom = null }, onTool = ::onCellTool)
                else -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                    Row(Modifier.background(colorResource(R.color.bg_collage_main_bottom_option)).height(65.sdp), verticalAlignment = Alignment.CenterVertically) {
                        CollageTool.entries.forEach { t -> ToolItem(t.icon, t.label, ink) { onTool(t) } }
                    }
                }
            }
        }

        // frLoaderTransSave: blocks the screen from Save until the saved screen opens
        if (saving) Box(
            Modifier
                .fillMaxSize()
                .background(colorResource(R.color.black_trans_1))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(loader, iterations = LottieConstants.IterateForever, modifier = Modifier.size(80.sdp).graphicsLayer(scaleX = 2f, scaleY = 2f))
        }

        editingText?.let { t ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                CollageTextEditor(
                    initial = t,
                    onDone = { done ->
                        editingText = null
                        if (done.text.isBlank()) {
                            if (panel != Panel.TEXT) before = null
                        } else {
                            state = state.copy(texts = state.texts.filterNot { it.id == done.id } + done)
                            panel = Panel.TEXT
                        }
                    },
                    onCancel = {
                        editingText = null
                        if (panel != Panel.TEXT) before = null
                    }
                )
            }
        }

        pickFor?.let { target ->
            BackHandler { pickFor = null }
            PhotoPicker(mediaRepository, multi = false, onBack = { pickFor = null }) { items ->
                val p = items.first().filePath
                when (target) {
                    PickFor.REPLACE -> selected?.let { i -> updateCell(i) { CellEdit(p) } }
                    PickFor.WALLPAPER -> state = state.copy(background = CollageBg.Image(p))
                }
                pickFor = null
            }
        }

        if (exitAsked) CollageExitDialog(
            onExit = {
                exitAsked = false
                onExit()
            },
            onDismiss = { exitAsked = false }
        )
    }
}

/** `toolbar` (back, "Collage", the save_collage_bg Save pill) or, while a tool is open, cToolbar (back, the tool's name, brush undo/redo). */
@Composable
private fun CollageHeader(
    @StringRes title: Int?,
    onBack: () -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    brush: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val ink = colorResource(R.color.collage_header_text_color)
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.viewer_back),
            colorFilter = if (title != null) ColorFilter.tint(ink) else null,
            modifier = Modifier.size(56.dp).clickable(onClick = onBack).padding(if (title != null) 13.sdp else 16.dp)
        )
        Text(
            stringResource(title ?: R.string.collage),
            style = inter(14, FontWeight.W500),
            color = ink,
            modifier = Modifier.weight(1f).padding(start = if (title != null) 12.sdp else 2.sdp)
        )
        if (title == null) Row(
            Modifier
                .padding(end = 10.sdp)
                .alpha(if (saveEnabled) 1f else 0.3f)
                .clip(RoundedCornerShape(14.sdp))
                .drawableBackground(R.drawable.save_collage_bg)
                .clickable(enabled = saveEnabled, onClick = onSave)
                .padding(horizontal = 12.sdp, vertical = 5.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_clg_save), contentDescription = null, modifier = Modifier.padding(end = 5.sdp))
            Text(stringResource(R.string.save), style = inter(12, FontWeight.W500), color = colorResource(R.color.onlyWhite))
        }
        if (brush) {
            UndoRedo(R.drawable.ic_undo_collage, canUndo, onUndo)
            UndoRedo(R.drawable.ic_redo_collage, canRedo, onRedo)
        }
    }
}

@Composable
private fun UndoRedo(@DrawableRes icon: Int, enabled: Boolean, onClick: () -> Unit) {
    Image(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier
            .padding(end = 4.sdp)
            .width(56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.sdp)
    )
}

private fun blurRadiusOf(bg: CollageBg) = (bg as? CollageBg.Blur)?.radius ?: 0

/** Which background tile shows as picked. */
private fun bgOptionOf(bg: CollageBg): BgOption? = when (bg) {
    is CollageBg.Solid -> when (bg.color) {
        0 -> BgOption.PNG
        WHITE -> BgOption.WHITE
        BLACK -> BgOption.BLACK
        else -> BgOption.COLOR
    }
    else -> null
}

/** Places the child with its centre at ([x], [y]) fractions of the parent. */
private fun Modifier.centerAt(x: Float, y: Float) = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(constraints.maxWidth, constraints.maxHeight) {
        p.place((x * constraints.maxWidth - p.width / 2f).roundToInt(), (y * constraints.maxHeight - p.height / 2f).roundToInt())
    }
}

/** PerfectGridView: the background, then every cell's photo centre-cropped into its rounded cell with that cell's edits. */
private fun DrawScope.drawCollage(
    state: CollageState,
    cells: List<UnitRect>,
    bitmaps: Map<String, ImageBitmap>,
    radius: Float,
    pad: Float,
    blurred: ImageBitmap?
) {
    when (val bg = state.background) {
        is CollageBg.Solid -> drawRect(Color(bg.color))
        is CollageBg.Blur -> blurred?.let { drawCover(it, Rect(Offset.Zero, size)) }
        is CollageBg.Image -> bitmaps[bg.path]?.let { drawCover(it, Rect(Offset.Zero, size)) }
    }
    cells.forEachIndexed { i, c ->
        val edit = state.cells.getOrNull(i) ?: return@forEachIndexed
        val bmp = bitmaps[edit.path] ?: return@forEachIndexed
        val r = cellRect(c, size.width, size.height, pad)
        val rr = min(radius, min(r.width, r.height) / 2)
        clipPath(Path().apply { addRoundRect(RoundRect(r, CornerRadius(rr))) }) {
            val turned = edit.quarter % 2 == 1
            val s = max(r.width / (if (turned) bmp.height else bmp.width), r.height / (if (turned) bmp.width else bmp.height)) * edit.zoom
            withTransform({
                translate(r.center.x + edit.panX * r.width, r.center.y + edit.panY * r.height)
                scale(if (edit.flipH) -1f else 1f, if (edit.flipV) -1f else 1f, pivot = Offset.Zero)
                rotate(edit.quarter * 90f + edit.tilt, pivot = Offset.Zero)
                scale(s, s, pivot = Offset.Zero)
            }) {
                drawImage(bmp, Offset(-bmp.width / 2f, -bmp.height / 2f))
            }
        }
    }
}

/** Centre-crops [bmp] into [dst]. */
private fun DrawScope.drawCover(bmp: ImageBitmap, dst: Rect) {
    val s = max(dst.width / bmp.width, dst.height / bmp.height)
    val sw = (dst.width / s).roundToInt().coerceIn(1, bmp.width)
    val sh = (dst.height / s).roundToInt().coerceIn(1, bmp.height)
    drawImage(
        bmp,
        srcOffset = IntOffset((bmp.width - sw) / 2, (bmp.height - sh) / 2),
        srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
        dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
        filterQuality = FilterQuality.High
    )
}

/** Paint, Neon (a blurred glow under a white core) and Eraser, which clears this offscreen layer only. */
private fun DrawScope.drawStrokes(strokes: List<BrushStroke>, density: Float) {
    strokes.forEach { s ->
        val path = Path()
        s.points.forEachIndexed { i, p ->
            val x = p.x * size.width
            val y = p.y * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (s.points.size == 1) path.lineTo(s.points[0].x * size.width + 0.5f, s.points[0].y * size.height)
        val width = brushStrokeSize(s.size) / 2f * density
        val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (s.kind) {
            BrushKind.PAINT -> drawPath(path, Color(s.color), style = stroke)
            BrushKind.ERASE -> drawPath(path, Color.Black, style = stroke, blendMode = BlendMode.Clear)
            BrushKind.NEON -> {
                drawIntoCanvas { canvas ->
                    val glow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = width
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        color = s.color
                        maskFilter = BlurMaskFilter(width / 2, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), glow)
                }
                drawPath(path, Color.White, style = Stroke(width / 3, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** shortcut: blurs by drawing a down-scaled copy back up with filtering; switch to a real blur if banding shows. */
private fun downscaleBlur(src: ImageBitmap, radius: Int): ImageBitmap {
    if (radius <= 0) return src
    val a = src.asAndroidBitmap()
    val f = radius + 1
    return Bitmap.createScaledBitmap(a, max(1, a.width / f), max(1, a.height / f), true).asImageBitmap()
}

/** Decodes a photo near [side] px with its EXIF turn applied, as a software bitmap the capture can read. */
private suspend fun loadPhoto(context: Context, path: String, side: Int): ImageBitmap? {
    val request = ImageRequest.Builder(context).data(File(path)).size(side).allowHardware(false).build()
    val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable
    return drawable?.bitmap?.asImageBitmap()
}

/** k9/a.OooOO0o: PNG at quality 100 into Pictures/Gallery Collage; MediaStore on API 29+, a scanned file below. */
private suspend fun saveCollage(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val name = collageFileName()
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + COLLAGE_FOLDER)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("insert")
            try {
                resolver.openOutputStream(uri)?.use { if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) error("encode") } ?: error("stream")
            } catch (e: Exception) {
                resolver.delete(uri, null, null) // no half-written collage left behind
                throw e
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), COLLAGE_FOLDER).apply { mkdirs() }
            val file = File(dir, name)
            try {
                FileOutputStream(file).use { if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) error("encode") }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf("image/png"), null)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }.getOrNull()
}
