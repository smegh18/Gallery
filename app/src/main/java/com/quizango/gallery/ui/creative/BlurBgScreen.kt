package com.quizango.gallery.ui.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.editor.DiscardChangesDialog
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** activity_blur_bg (S272 BlurBgActivity): the background blurred behind the sharp subject, ruler 0..25. */
@Composable
fun BlurBgScreen(imagePath: String, preferences: AppPreferences, onSaved: () -> Unit, onNavigateBack: () -> Unit) =
    BackgroundEffectScreen(AiTool.BLUR, R.string.blur_background_guide, imagePath, preferences, onSaved, onNavigateBack)

/**
 * activity_blur_bg / activity_color_splash (identical but for the sheet title): guide on first use, ML Kit segmentation,
 * remove_bg_loader for 1000 ms, then the effect on `imageView` under the subject `imageObject`. Done writes the result
 * over the editor's working copy [imagePath] and sets the guide flag.
 */
@Composable
internal fun BackgroundEffectScreen(
    tool: AiTool,
    title: Int,
    imagePath: String,
    preferences: AppPreferences,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guideSeen by remember { preferences.oneTimeFlag(tool.guideKey) }.collectAsState(initial = null)
    var started by rememberSaveable { mutableStateOf(false) }
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var subject by remember { mutableStateOf<Bitmap?>(null) }
    var effect by remember { mutableStateOf<Bitmap?>(null) }
    var segmenting by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }
    var value by remember { mutableIntStateOf(RULER_MAX) } // BB / CS: ruler 0..25, default 25
    var saving by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) } // decode or segmentation failed; Done starts a new attempt
    var attempt by remember { mutableIntStateOf(0) }
    val showGuide = !started && showsGuide(guideSeen)

    LaunchedEffect(guideSeen) { if (guideSeen == true) started = true }
    LaunchedEffect(attempt) {
        if (original != null) return@LaunchedEffect
        val src = withContext(Dispatchers.IO) { ImageSegmentationHelper.decodeUpright(imagePath) }
        if (src == null) {
            segmenting = false
            failed = true
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        original = src
        val scale = minOf(1f, PREVIEW_SIDE / maxOf(src.width, src.height).toFloat())
        preview = Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
    }
    LaunchedEffect(started, original, attempt) {
        val src = original ?: return@LaunchedEffect
        if (!started || subject != null) return@LaunchedEffect
        val fg = ImageSegmentationHelper.extractSubject(context, src)
        segmenting = false
        if (fg == null) {
            failed = true
            val msg = if (ImageSegmentationHelper.isOnline(context)) R.string.ml_kit_module_error else R.string.please_turn_on_internet_and_try_again
            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        subject = fg
        delay(1000) // bc/e: remove_bg_loader plays one second before the effect shows
        ready = true
    }
    LaunchedEffect(ready, value, preview) {
        val p = preview ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        effect = withContext(Dispatchers.Default) {
            if (tool == AiTool.BLUR) ImageSegmentationHelper.blur(p, value) else ImageSegmentationHelper.splash(p, splashIntensity(value))
        }
    }

    fun save() {
        if (failed) {
            failed = false
            segmenting = true
            attempt++
            return
        }
        val src = original ?: return
        val fg = subject
        val small = preview
        if (!ready || fg == null || small == null || effect == null) {
            Toast.makeText(context, context.getString(R.string.progress_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (saving) return
        saving = true
        val v = value // the ruler at Done, not whatever preview finished last
        scope.launch {
            val out = withContext(Dispatchers.Default) {
                // llMainLayout drawn at the original size: the blurred preview scales up, the splash is redone at full size
                val bg = if (tool == AiTool.BLUR) ImageSegmentationHelper.blur(small, v) else ImageSegmentationHelper.splash(src, splashIntensity(v))
                Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888).also { out ->
                    val canvas = Canvas(out)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(bg, null, Rect(0, 0, src.width, src.height), paint)
                    canvas.drawBitmap(fg, null, Rect(0, 0, src.width, src.height), paint)
                }
            }
            if (ImageSegmentationHelper.writePng(out, imagePath)) {
                runCatching { preferences.setOneTimeFlag(tool.guideKey) } // bc/o, bc/aj: saving also marks the guide seen; a failed flag write must not hide the save
                onSaved()
            } else {
                saving = false
                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = !showGuide) { if (!saving) discarding = true } // the working copy may already be rewritten mid-save

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            SharedMediaToolbar(title = stringResource(title), onBack = { discarding = true })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val src = original
                    if (src != null) {
                        val density = LocalDensity.current
                        val fit = minOf(constraints.maxWidth / src.width.toFloat(), constraints.maxHeight / src.height.toFloat())
                        Box(Modifier.size(with(density) { (src.width * fit).toDp() }, with(density) { (src.height * fit).toDp() })) {
                            val bg = if (ready) effect ?: preview else preview
                            bg?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
                            val fg = subject
                            if (ready && fg != null) Image(fg.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                            if (fg != null && !ready) RemoveBgLoader(120, Modifier.align(Alignment.Center))
                        }
                    }
                }
                if (segmenting && started) SegmentingBar(Modifier.align(Alignment.TopCenter))
            }
            ToolSheet {
                ToolSheetHeader(
                    title = stringResource(title),
                    onClose = { discarding = true },
                    onDone = ::save,
                    modifier = Modifier.padding(vertical = 5.sdp)
                )
                Text(
                    text = rulerLabel(value).toString(),
                    style = GalleryText.BodySmallRegular,
                    color = colorResource(R.color.colorPrimary),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                GalleryRulerView(
                    value = value,
                    onValueChange = { value = it },
                    minValue = 0,
                    maxValue = RULER_MAX,
                    enabled = ready,
                    modifier = Modifier.padding(bottom = 15.dp).height(40.dp)
                )
            }
        }
        if (saving) SavingLayer()
        if (showGuide) AiToolGuide(
            tool = tool,
            onContinue = { scope.launch { preferences.setOneTimeFlag(tool.guideKey) }; started = true },
            onCancel = onNavigateBack
        )
    }
    if (discarding) DiscardChangesDialog(onDiscard = { discarding = false; onNavigateBack() }, onDismiss = { discarding = false })
}

private const val RULER_MAX = 25
private const val PREVIEW_SIDE = 1080f
