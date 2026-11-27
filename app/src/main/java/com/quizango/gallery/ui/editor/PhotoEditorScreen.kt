package com.quizango.gallery.ui.editor

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.components.PhotoPicker
import com.quizango.gallery.ui.creative.ColorPickerDialog
import com.quizango.gallery.ui.creative.StickerScreen
import com.quizango.gallery.ui.editor.crop.CropGeometry
import com.quizango.gallery.ui.editor.crop.ImageCropView
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.viewer.isDecodable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.min
import kotlin.math.roundToInt

/** One brush stroke; points and width are fractions of the shown image, so they map onto the full bitmap. */
internal data class BrushStroke(val points: List<Offset>, val color: Int, val width: Float)

/** The fit-centre rectangle of a [w] x [h] image in a [boxW] x [boxH] box. */
internal fun fitRect(w: Int, h: Int, boxW: Float, boxH: Float): Rect {
    val s = min(boxW / w, boxH / h)
    val dw = w * s
    val dh = h * s
    return Rect((boxW - dw) / 2f, (boxH - dh) / 2f, (boxW + dw) / 2f, (boxH + dh) / 2f)
}

private fun aiToolsAvailable(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < AI_MIN_SDK) return false
    val info = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
    return info.totalMem >= AI_MIN_RAM
}

private fun renderStrokes(src: Bitmap, strokes: List<BrushStroke>): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { s ->
        paint.color = s.color
        paint.strokeWidth = (s.width * out.width).coerceAtLeast(1f)
        val path = AndroidPath()
        s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x * out.width, p.y * out.height) else path.lineTo(p.x * out.width, p.y * out.height) }
        if (s.points.size == 1) path.lineTo(s.points[0].x * out.width + 0.1f, s.points[0].y * out.height)
        canvas.drawPath(path, paint)
    }
    return out
}

/**
 * PhotoEditActivity (activity_photo_edit, #38). Without a photo it first opens the TYPE 3 single picker
 * (Explore AI Editor). Edits apply to a working copy in filesDir; Save writes a new Image_<millis>.png next to
 * the original and returns its path, Back with edits asks dialog_discard_changes.
 */
@Composable
fun PhotoEditorScreen(
    initialPath: String?,
    initialTab: String?,
    mediaRepository: MediaRepository,
    preferences: AppPreferences,
    toolEdited: Boolean,
    onToolEditConsumed: () -> Unit,
    onOpenTool: (String) -> Unit,
    onSaved: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var source by rememberSaveable { mutableStateOf(initialPath?.takeIf { it.isNotEmpty() }) }
    val picked = source
    if (picked == null) {
        PhotoPicker(mediaRepository, multi = false, onBack = onNavigateBack) { items ->
            val item = items.first()
            val refusal = editRefusal(item.fileName, isDecodable(item.filePath))
            if (refusal != null) Toast.makeText(context, context.getString(refusal), Toast.LENGTH_SHORT).show() else source = item.filePath
        }
        return
    }
    PhotoEditor(picked, initialTab, mediaRepository, preferences, toolEdited, onToolEditConsumed, onOpenTool, onSaved, onNavigateBack)
}

@Composable
private fun PhotoEditor(
    source: String,
    initialTab: String?,
    mediaRepository: MediaRepository,
    preferences: AppPreferences,
    toolEdited: Boolean,
    onToolEditConsumed: () -> Unit,
    onOpenTool: (String) -> Unit,
    onSaved: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val aiAvailable = remember { aiToolsAvailable(context) }
    val tools = remember(aiAvailable) { editTools(aiAvailable) }
    val fromSuggestion = remember { isSuggestionTool(initialTab) }
    val working = remember(source) { File(context.filesDir, workingCopyName(File(source).name)) }

    // Survive the AI tools' screens (this destination leaves composition while they are on top)
    var prepared by rememberSaveable { mutableStateOf(false) }
    var edited by rememberSaveable { mutableStateOf(false) }
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    var tool by rememberSaveable { mutableStateOf<EditTool?>(null) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    var tipHidden by remember { mutableStateOf(false) }
    val stripX = remember { mutableStateMapOf<EditTool, Float>() }

    fun toast(@StringRes id: Int) = Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show()

    // isFreshEdit: copy the original once, then (re)load the working copy every time this screen is composed
    LaunchedEffect(Unit) {
        val failure = withContext(Dispatchers.IO) {
            if (!prepared) {
                val src = File(source)
                if (!src.exists()) return@withContext R.string.file_not_found
                if (runCatching { src.copyTo(working, overwrite = true) }.isFailure) return@withContext R.string.toast_corrupted_image
            }
            try {
                decodeForEdit(working)?.let { bmp -> withContext(Dispatchers.Main) { bitmap = bmp }; null } ?: R.string.toast_corrupted_image
            } catch (_: OutOfMemoryError) {
                R.string.outofmemory_msg
            }
        }
        if (failure != null) {
            toast(failure)
            onNavigateBack()
        } else prepared = true
    }
    LaunchedEffect(toolEdited) {
        if (toolEdited) {
            edited = true // the AI tool rewrote the working copy, which the load above picks up
            onToolEditConsumed()
        }
    }

    val newFlags = EditTool.entries.mapNotNull { it.newKey }.associateWith { key -> preferences.oneTimeFlag(key).collectAsState(initial = true).value }
    val tipFlags = EditTool.entries.mapNotNull { it.tipKey }.associateWith { key -> preferences.oneTimeFlag(key).collectAsState(initial = true).value }

    // Panel state
    var cropPreset by remember { mutableStateOf(CropPreset.FREE) }
    var cropRotation by remember { mutableFloatStateOf(0f) }
    var cropFlipX by remember { mutableStateOf(false) }
    var cropFlipY by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    var cropBounds by remember { mutableStateOf<Rect?>(null) }
    var adjustParam by remember { mutableStateOf(AdjustParam.BRIGHTNESS) }
    val adjustValues = remember { mutableStateMapOf<AdjustParam, Int>() }
    var filter by remember { mutableStateOf(EditorFilter.NORMAL) }
    var filterThumbs by remember { mutableStateOf<List<Pair<EditorFilter, Bitmap>>>(emptyList()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    val primary = colorResource(R.color.colorPrimary).toArgb()
    var brushSize by remember { mutableFloatStateOf(40f) }
    var brushSizing by remember { mutableStateOf(false) }
    var brushColor by remember { mutableIntStateOf(primary) }
    val brushHistory = remember { EditHistory<List<BrushStroke>>() }
    var strokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    var drawing by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var eyedropper by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var texts by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var selectedText by remember { mutableStateOf<Long?>(null) }
    var textSub by remember { mutableStateOf(TextSubTool.NONE) }
    var gradientMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf<TextItem?>(null) } // the sticker being typed; id 0 = a new one
    var nextTextId by remember { mutableLongStateOf(1L) }
    var stickerPicker by remember { mutableStateOf(false) }
    val textSizes = remember { mutableMapOf<Long, Pair<Int, Int>>() }
    var shownWidth by remember { mutableFloatStateOf(1f) }

    val white = colorResource(R.color.onlyWhite).toArgb()
    val shadowColor = colorResource(R.color.blackTran1).toArgb()
    val swatches = listOf(
        R.color.color_gray, R.color.color_dark_gray, R.color.color_light_pink, R.color.color_pink,
        R.color.color_dark_pink, R.color.color_pink, R.color.color_dark_green, R.color.color_dark_purple
    ).map { colorResource(it).toArgb() } // array brush_color

    fun closePanel() {
        tool = null
        preview = null
        eyedropper = false
    }

    fun open(t: EditTool) {
        tipHidden = true
        if (t.ai) {
            scope.launch { listOfNotNull(t.newKey, t.tipKey).forEach { preferences.setOneTimeFlag(it) } }
            onOpenTool(
                when (t) {
                    EditTool.REMOVE_BG -> Screen.RemoveBg.createRoute(working.path)
                    EditTool.COLOR_SPLASH -> Screen.ColorSplash.createRoute(working.path)
                    else -> Screen.BlurBg.createRoute(working.path)
                }
            )
            return
        }
        tool = t
        when (t) {
            EditTool.CROP -> { cropPreset = CropPreset.FREE; cropRotation = 0f; cropFlipX = false; cropFlipY = false }
            EditTool.ADJUST -> { adjustParam = AdjustParam.BRIGHTNESS; adjustValues.clear() }
            EditTool.FILTER -> {
                filter = EditorFilter.NORMAL
                filterThumbs = emptyList()
                bitmap?.let { bmp ->
                    scope.launch {
                        filterThumbs = withContext(Dispatchers.Default) {
                            val small = scaledDown(bmp, 160)
                            EditorFilter.entries.map { it to applyFilter(small, it) }
                        }
                    }
                }
            }
            EditTool.BRUSH -> { brushSize = 40f; brushColor = primary; strokes = emptyList(); brushHistory.clear(); brushHistory.record(emptyList()) }
            EditTool.TEXT -> { texts = emptyList(); selectedText = null; textSub = TextSubTool.NONE; textInput = TextItem(0, "", white) }
            EditTool.STICKER -> stickerPicker = true
            else -> Unit
        }
    }

    LaunchedEffect(bitmap) {
        if (bitmap != null && !autoOpened) {
            autoOpened = true
            initialTool(initialTab)?.let { if (!it.ai || aiAvailable) open(it) }
        }
    }

    // Live preview of the filter / adjust panel on a screen-sized copy
    LaunchedEffect(tool, filter, adjustValues.toMap(), bitmap) {
        val bmp = bitmap ?: return@LaunchedEffect
        preview = when (tool) {
            EditTool.FILTER -> if (filter == EditorFilter.NORMAL) null else withContext(Dispatchers.Default) { applyFilter(scaledDown(bmp, 1280), filter) }
            EditTool.ADJUST -> if (adjustValues.isEmpty()) null else withContext(Dispatchers.Default) { applyAdjust(scaledDown(bmp, 1280), adjustValues.toMap()) }
            else -> null
        }
    }

    fun commit(transform: (Bitmap) -> Bitmap) {
        val bmp = bitmap ?: return
        busy = true
        scope.launch {
            val out = withContext(Dispatchers.Default) { runCatching { transform(bmp) }.getOrNull() }
            val written = out != null && withContext(Dispatchers.IO) { writePng(out, working) }
            if (written) {
                bitmap = out
                edited = true
            } else toast(R.string.something_went_wrong)
            busy = false
            closePanel()
        }
    }

    fun exit() {
        if (edited) askDiscard = true else onNavigateBack()
    }

    fun cancelPanel() {
        val t = tool
        if (t == EditTool.TEXT && textSub != TextSubTool.NONE) {
            textSub = TextSubTool.NONE
            return
        }
        closePanel()
        if (fromSuggestion && (t == EditTool.CROP || t == EditTool.BRUSH)) exit() // isFromSuggestion: cancel leaves
    }

    fun done() {
        when (tool) {
            EditTool.CROP -> {
                val rect = cropRect
                val bounds = cropBounds
                if (rect == null || bounds == null) closePanel()
                else commit { CropGeometry.cropBitmap(it, cropRotation, cropFlipX, cropFlipY, rect, bounds) }
            }
            EditTool.ADJUST -> if (adjustValues.all { (p, v) -> p.isNeutral(v) }) closePanel() else {
                val values = adjustValues.toMap()
                commit { applyAdjust(it, values) }
            }
            EditTool.FILTER -> if (filter == EditorFilter.NORMAL) closePanel() else {
                val f = filter
                commit { applyFilter(it, f) }
            }
            EditTool.BRUSH -> if (strokes.isEmpty()) closePanel() else {
                val s = strokes
                commit { renderStrokes(it, s) }
            }
            EditTool.TEXT -> {
                val list = texts.filter { it.text.isNotBlank() }
                if (list.isEmpty()) closePanel() else {
                    val w = shownWidth
                    val d = density.density
                    val fs = density.fontScale
                    commit { renderTexts(it, list, w, d, fs, shadowColor) }
                }
            }
            else -> closePanel()
        }
    }

    fun save() {
        if (busy) return
        if (!edited) return onNavigateBack() // PE.o0000O: nothing changed, just leave
        busy = true
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    savedImageFile(File(source).parent, pictures, System.currentTimeMillis()).also {
                        it.parentFile?.mkdirs()
                        working.copyTo(it)
                    }
                }.getOrNull()
            }
            if (out == null) {
                busy = false
                toast(R.string.something_went_wrong)
                return@launch
            }
            suspendCancellableCoroutine { cont -> MediaScannerConnection.scanFile(context, arrayOf(out.path), arrayOf("image/png")) { _, _ -> cont.resume(Unit) } }
            mediaRepository.syncMediaStore()
            busy = false
            onSaved(out.path)
        }
    }

    BackHandler {
        when {
            busy -> Unit
            stickerPicker -> stickerPicker = false
            textInput != null -> textInput = null
            tool != null && !fromSuggestion -> cancelPanel()
            else -> exit()
        }
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            EditorToolbar(
                title = stringResource(if (tool == EditTool.STICKER) R.string.sticker else R.string.edit),
                showSave = tool == null,
                onBack = { if (!busy) { if (tool != null && !fromSuggestion) cancelPanel() else exit() } },
                onSave = ::save
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val bmp = bitmap
                if (bmp != null) {
                    val boxW = constraints.maxWidth.toFloat()
                    val boxH = constraints.maxHeight.toFloat()
                    if (tool == EditTool.CROP) {
                        ImageCropView(
                            bitmap = bmp,
                            rotationDegrees = cropRotation,
                            flipX = cropFlipX,
                            flipY = cropFlipY,
                            aspectX = cropPreset.aspectX.toFloat(),
                            aspectY = cropPreset.aspectY.toFloat(),
                            dimColor = colorResource(R.color.crop_image_view_background),
                            modifier = Modifier.fillMaxSize(),
                            onCropRectChanged = { rect, bounds -> cropRect = rect; cropBounds = bounds }
                        )
                    } else {
                        val fit = fitRect(bmp.width, bmp.height, boxW, boxH)
                        shownWidth = fit.width
                        val shown = preview ?: bmp
                        Image(
                            bitmap = remember(shown) { shown.asImageBitmap() },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        fun fraction(p: Offset) = Offset((p.x - fit.left) / fit.width, (p.y - fit.top) / fit.height)
                        if (eyedropper) Box(
                            Modifier.fillMaxSize().pointerInput(fit) {
                                detectTapGestures { p ->
                                    val f = fraction(p)
                                    if (f.x in 0f..1f && f.y in 0f..1f) {
                                        val c = bmp.getPixel((f.x * (bmp.width - 1)).roundToInt(), (f.y * (bmp.height - 1)).roundToInt())
                                        if (tool == EditTool.BRUSH) brushColor = c
                                        else selectedText?.let { id -> texts = texts.map { if (it.id == id) it.copy(color = c, gradient = null) else it } }
                                    }
                                    eyedropper = false
                                }
                            }
                        )
                        if (tool == EditTool.BRUSH) {
                            val all = strokes
                            val current = drawing
                            Canvas(
                                Modifier.fillMaxSize().then(
                                    if (eyedropper) Modifier else Modifier.pointerInput(fit, brushSize, brushColor) {
                                        detectDragGestures(
                                            onDragStart = { drawing = listOf(fraction(it)) },
                                            onDrag = { change, _ -> change.consume(); drawing = drawing + fraction(change.position) },
                                            onDragEnd = {
                                                if (drawing.isNotEmpty()) {
                                                    strokes = strokes + BrushStroke(drawing, brushColor, brushSize / fit.width)
                                                    brushHistory.record(strokes)
                                                }
                                                drawing = emptyList()
                                            }
                                        )
                                    }
                                )
                            ) {
                                fun pathOf(points: List<Offset>) = Path().apply {
                                    points.forEachIndexed { i, p ->
                                        val x = fit.left + p.x * fit.width
                                        val y = fit.top + p.y * fit.height
                                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                                    }
                                }
                                all.forEach { s ->
                                    drawPath(pathOf(s.points), Color(s.color), style = Stroke(width = (s.width * fit.width).coerceAtLeast(1f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                                if (current.size > 1) drawPath(pathOf(current), Color(brushColor), style = Stroke(width = brushSize.coerceAtLeast(1f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                // CustomDrawingView brushSize: the brush preview while the size slider moves
                                if (brushSizing) drawCircle(Color(brushColor), brushSize.coerceAtLeast(1f) / 2f, Offset(boxW / 2f, boxH / 2f))
                            }
                        }
                        if (tool == EditTool.TEXT) {
                            val handlePx = with(density) { 22.sdp.toPx() }
                            Box(
                                Modifier.fillMaxSize().then(
                                    if (eyedropper) Modifier else Modifier
                                        .pointerInput(fit, texts, selectedText) {
                                            detectTapGestures { p ->
                                                val hit = texts.asReversed().firstNotNullOfOrNull { t ->
                                                    val (w, h) = textSizes[t.id] ?: return@firstNotNullOfOrNull null
                                                    val c = Offset(fit.left + t.cx * fit.width, fit.top + t.cy * fit.height)
                                                    textHit(p, c, w.toFloat(), h.toFloat(), t.rotation, handlePx, t.id == selectedText)?.let { t to it }
                                                }
                                                when (hit?.second) {
                                                    null -> selectedText = null
                                                    TextHit.CLOSE -> { texts = texts.filterNot { it.id == hit.first.id }; selectedText = null }
                                                    TextHit.EDIT -> textInput = hit.first
                                                    else -> selectedText = hit.first.id
                                                }
                                            }
                                        }
                                        .pointerInput(fit, texts.size, selectedText) {
                                            var target: TextItem? = null
                                            var part: TextHit? = null
                                            var from = Offset.Zero
                                            detectDragGestures(
                                                onDragStart = { p ->
                                                    from = p
                                                    val hit = texts.asReversed().firstNotNullOfOrNull { t ->
                                                        val (w, h) = textSizes[t.id] ?: return@firstNotNullOfOrNull null
                                                        val c = Offset(fit.left + t.cx * fit.width, fit.top + t.cy * fit.height)
                                                        textHit(p, c, w.toFloat(), h.toFloat(), t.rotation, handlePx, t.id == selectedText)?.let { t to it }
                                                    }
                                                    target = hit?.first
                                                    part = hit?.second
                                                    hit?.first?.let { selectedText = it.id }
                                                },
                                                onDrag = { change, delta ->
                                                    val t = texts.firstOrNull { it.id == target?.id } ?: return@detectDragGestures
                                                    change.consume()
                                                    val c = Offset(fit.left + t.cx * fit.width, fit.top + t.cy * fit.height)
                                                    val to = change.position
                                                    val moved = when (part) {
                                                        TextHit.ZOOM -> t.copy(scale = zoomedScale(t.scale, c, from, to))
                                                        TextHit.ROTATE -> t.copy(rotation = rotatedBy(t.rotation, c, from, to))
                                                        else -> t.copy(cx = (t.cx + delta.x / fit.width).coerceIn(0f, 1f), cy = (t.cy + delta.y / fit.height).coerceIn(0f, 1f))
                                                    }
                                                    from = to
                                                    texts = texts.map { if (it.id == t.id) moved else it }
                                                }
                                            )
                                        }
                                )
                            ) {
                                texts.forEach { t ->
                                    TextSticker(
                                        item = t,
                                        center = Offset(fit.left + t.cx * fit.width, fit.top + t.cy * fit.height),
                                        selected = t.id == selectedText
                                    ) { w, h -> textSizes[t.id] = w to h }
                                }
                            }
                        }
                    }
                }
                // flTooltipContainer: the first-run AI tooltip over its strip item
                val tip = remember(tipFlags, aiAvailable) { firstTooltip(aiAvailable, initialTool(initialTab) == null) { tipFlags[it] ?: true } }
                if (tool == null && bmp != null && !tipHidden && tip != null) {
                    stripX[tip]?.let { x -> AiTooltip(tip, x, Modifier.align(Alignment.BottomStart)) { open(tip) } }
                }
            }
            // bottomView: the tool strip on ic_edit_navigation_bg, or the open tool's panel
            Box(
                Modifier
                    .fillMaxWidth()
                    .shadow(10.sdp, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.sdp, topEnd = 20.sdp))
                    .drawableBackground(R.drawable.ic_edit_navigation_bg)
                    .navigationBarsPadding()
            ) {
                when (tool) {
                    null -> if (bitmap != null) ToolStrip(
                        tools = tools,
                        isNew = { t -> t.newKey?.let { newFlags[it] == false } ?: false },
                        onTap = ::open,
                        onPlaced = { t, x -> stripX[t] = x }
                    )
                    EditTool.CROP -> CropPanel(
                        preset = cropPreset,
                        onPreset = { cropPreset = it },
                        onFlipX = { cropFlipX = !cropFlipX },
                        onFlipY = { cropFlipY = !cropFlipY },
                        onRotate = { cropRotation = (cropRotation + 90f) % 360f },
                        onClose = ::cancelPanel,
                        onDone = ::done
                    )
                    EditTool.FILTER -> FilterPanel(filterThumbs, filter, { filter = it }, ::cancelPanel, ::done)
                    EditTool.ADJUST -> AdjustPanel(
                        param = adjustParam,
                        value = adjustValues[adjustParam] ?: adjustParam.default,
                        onParam = { adjustParam = it },
                        onValue = { adjustValues[adjustParam] = it },
                        onClose = ::cancelPanel,
                        onDone = ::done
                    )
                    EditTool.BRUSH -> BrushPanel(
                        size = brushSize,
                        onSize = { brushSize = it; brushSizing = true },
                        onSizeRelease = { brushSizing = false },
                        colors = swatches,
                        color = brushColor,
                        onColor = { brushColor = it },
                        eyedropper = eyedropper,
                        onEyedropper = { eyedropper = !eyedropper },
                        onPicker = { colorDialog = true },
                        onUndo = { brushHistory.undo()?.let { strokes = it } },
                        onRedo = { brushHistory.redo()?.let { strokes = it } },
                        onClose = ::cancelPanel,
                        onDone = ::done
                    )
                    EditTool.STICKER -> StickerPanel(onAdd = { stickerPicker = true }, onDone = ::done)
                    EditTool.TEXT -> TextPanel(
                        sub = textSub,
                        onSub = { textSub = it },
                        onPlus = { textInput = TextItem(0, "", white) },
                        selected = texts.firstOrNull { it.id == selectedText } ?: texts.lastOrNull(),
                        onChange = { changed -> texts = texts.map { if (it.id == changed.id) changed else it } },
                        colors = swatches,
                        eyedropper = eyedropper,
                        onEyedropper = { eyedropper = !eyedropper },
                        onPicker = { colorDialog = true },
                        gradientMode = gradientMode,
                        onGradientMode = { gradientMode = it },
                        onClose = ::cancelPanel,
                        onDone = ::done
                    )
                    else -> Unit
                }
            }
        }

        if (bitmap == null || busy) Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
        if (stickerPicker) StickerScreen(onBack = { stickerPicker = false })
    }

    textInput?.let { editing ->
        TextInputDialog(
            initial = editing.text,
            initialAlign = editing.align,
            onDone = { value, align ->
                textInput = null
                if (value.isNotEmpty()) {
                    if (editing.id == 0L) {
                        val item = editing.copy(id = nextTextId++, text = value, align = align)
                        texts = texts + item
                        selectedText = item.id
                    } else texts = texts.map { if (it.id == editing.id) it.copy(text = value, align = align) else it }
                }
            },
            onDismiss = { textInput = null }
        )
    }
    if (colorDialog) ColorPickerDialog(
        initial = Color(if (tool == EditTool.BRUSH) brushColor else texts.firstOrNull { it.id == selectedText }?.color ?: white),
        onPick = { c ->
            colorDialog = false
            val argb = c.toArgb()
            if (tool == EditTool.BRUSH) brushColor = argb
            else selectedText?.let { id -> texts = texts.map { if (it.id == id) it.copy(color = argb, gradient = null) else it } }
        },
        onDismiss = { colorDialog = false }
    )
    if (askDiscard) DiscardChangesDialog(
        onDiscard = { askDiscard = false; onNavigateBack() },
        onDismiss = { askDiscard = false }
    )
}

/** layout_toolbar as the editor shows it: back, the title and the Save pill (save_btn_bg), then the divider. */
@Composable
private fun EditorToolbar(title: String, showSave: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(2.sdp)
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 4.sdp, end = 10.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.viewer_back),
                modifier = Modifier.borderlessClick(onBack).padding(6.sdp)
            )
            Text(
                title,
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 4.sdp)
            )
            if (showSave) Text(
                stringResource(R.string.save),
                style = GalleryText.BodyMediumSemiBold,
                color = colorResource(R.color.onlyWhite),
                modifier = Modifier
                    .drawableBackground(R.drawable.save_btn_bg)
                    .clickable(onClick = onSave)
                    .padding(horizontal = 18.sdp, vertical = 8.sdp)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colorResource(R.color.actionbarDividerColor)))
    }
}

/**
 * custom_remove_bg / custom_blur_bg / custom_color_splash: the blue bubble and the remove_bg_tooltip hand over
 * the tool's strip item (centre [itemX] in root px). Tapping it opens the tool.
 */
@Composable
private fun AiTooltip(tool: EditTool, itemX: Float, modifier: Modifier, onTap: () -> Unit) {
    val density = LocalDensity.current
    val (bubble, text, width) = when (tool) {
        EditTool.REMOVE_BG -> Triple(R.drawable.custom_remove_bg, R.string.tap_to_auto_remove_bg, 240)
        EditTool.BLUR_BG -> Triple(R.drawable.custom_blur_bg, R.string.tap_to_blur_bg, 180)
        else -> Triple(R.drawable.custom_blur_bg, R.string.tap_to_color_splash_bg, 270)
    }
    // Remove BG's bubble points down at its left, where the hand sits; the others centre the hand
    val handLeft = tool == EditTool.REMOVE_BG
    val handCenterFromLeft = with(density) { (if (handLeft) (24 + 22 + 36).dp else (width / 2).dp).toPx() }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.remove_bg_tooltip))
    Column(
        modifier
            .offset { IntOffset((itemX - handCenterFromLeft).roundToInt().coerceAtLeast(0), with(density) { 82.dp.roundToPx() }) }
            .wrapContentSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        horizontalAlignment = if (handLeft) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(horizontal = if (handLeft) 24.dp else 5.sdp)
                .width(width.dp)
                .drawableBackground(bubble),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(text),
                style = GalleryText.BodyMediumSemiBold,
                color = colorResource(R.color.onlyWhite),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(start = 3.sdp, end = 3.sdp, top = 3.sdp, bottom = 8.sdp)
            )
        }
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.padding(horizontal = if (handLeft) 22.dp else 0.dp, vertical = 5.sdp).size(72.dp)
        )
    }
}
