package com.quizango.gallery.ui.creative

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.CommonDialog
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** dialog_set_wallpaper rows (r0/b): Lock Screen 1, Home Screen 2, Home & Lock Screen 3. */
internal const val WALLPAPER_LOCK = 1
internal const val WALLPAPER_HOME = 2
internal const val WALLPAPER_BOTH = 3

/** WallpaperActivity.OoooOoo: 1 -> FLAG_LOCK, 2 -> FLAG_SYSTEM, anything else -> both. */
internal fun wallpaperFlag(choice: Int) = when (choice) {
    WALLPAPER_LOCK -> WallpaperManager.FLAG_LOCK
    WALLPAPER_HOME -> WallpaperManager.FLAG_SYSTEM
    else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
}

/**
 * activity_wallpaper (S272 WallpaperActivity): the photo full screen with system bars hidden, the scrollable and 2x zoom
 * toggles either side of Apply. Apply asks Lock / Home / both; not scrollable it sets what is on screen, scrollable the
 * whole photo, each scaled to the launcher's desired height.
 */
@Composable
fun WallpaperScreen(imagePath: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var zoomed by remember { mutableStateOf(false) }
    var scrollable by remember { mutableStateOf(false) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var frame by remember { mutableStateOf(IntSize.Zero) }
    var choosing by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val scale = if (zoomed) 2f else 1f

    LaunchedEffect(imagePath) { photo = withContext(Dispatchers.IO) { ImageSegmentationHelper.decodeUpright(imagePath, PREVIEW_SIDE) } }
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    fun apply(choice: Int) {
        val shown = photo ?: return
        applying = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val wm = WallpaperManager.getInstance(context)
                    val source = (if (scrollable) ImageSegmentationHelper.decodeUpright(imagePath) else onScreen(shown, frame, scale, pan))
                        ?: error("undecodable")
                    val target = wm.desiredMinimumHeight.takeIf { it > 0 } ?: source.height
                    val bmp = Bitmap.createScaledBitmap(source, (source.width * target / source.height.toFloat()).toInt().coerceAtLeast(1), target, true)
                    wm.setBitmap(bmp, null, true, wallpaperFlag(choice))
                }.isSuccess
            }
            applying = false
            if (ok) {
                Toast.makeText(context, context.getString(R.string.apply_set_wallpaper), Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        photo?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { frame = it }
                    .pointerInput(zoomed) {
                        // GestureImageView with zoom, double tap and rotation off: only a zoomed photo pans, inside its edges
                        detectDragGestures { change, drag ->
                            change.consume()
                            pan = clampPan(pan + drag, bmp, frame, scale)
                        }
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y }
            )
        }
        Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.sdp)) {
            ToggleButton(R.drawable.ic_img_scrollable, scrollable, Modifier.align(Alignment.BottomStart)) { scrollable = !scrollable }
            Text(
                text = stringResource(R.string.apply),
                style = GalleryText.BodyLargeMedium,
                color = colorResource(R.color.wallpaper_apply_button),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 55.sdp)
                    .fillMaxWidth()
                    .shadow(5.sdp, RoundedCornerShape(25.sdp))
                    .drawableBackground(R.drawable.btn_apply_wallpaper)
                    .clip(RoundedCornerShape(25.sdp))
                    .clickable(enabled = !applying) { choosing = true }
                    .padding(10.sdp)
            )
            ToggleButton(R.drawable.ic_zoom, zoomed, Modifier.align(Alignment.BottomEnd)) {
                zoomed = !zoomed
                pan = Offset.Zero // OoooOoO: the zoom toggles around the centre
            }
        }
        if (applying) CircularProgressIndicator(Modifier.align(Alignment.Center), color = colorResource(R.color.colorPrimary))
    }
    if (choosing) SetWallpaperDialog(onPick = { choosing = false; apply(it) }, onDismiss = { choosing = false })
}

/** imgScrollable / zoomImg: centred in the 55sdp gutter, ic_wallpaper_trans_bg while on, ic_wallpaper_bg while off. */
@Composable
private fun ToggleButton(icon: Int, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.width(55.sdp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .shadow(5.sdp, CircleShape)
                .drawableBackground(if (on) R.drawable.ic_wallpaper_trans_bg else R.drawable.ic_wallpaper_bg)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(10.sdp)
        )
    }
}

/** dialog_set_wallpaper: three targets, a divider, Cancel. */
@Composable
private fun SetWallpaperDialog(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    CommonDialog(onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(20.sdp)
        ) {
            DialogRow(R.string.lock_screen, Modifier.padding(top = 5.sdp)) { onPick(WALLPAPER_LOCK) }
            DialogRow(R.string.home_screen, Modifier.padding(vertical = 20.sdp)) { onPick(WALLPAPER_HOME) }
            DialogRow(R.string.home_lock_screen, Modifier.padding(bottom = 10.sdp)) { onPick(WALLPAPER_BOTH) }
            Box(Modifier.padding(horizontal = 1.sdp, vertical = 20.sdp).fillMaxWidth().height(1.sdp).background(colorResource(R.color.dividerColor)))
            DialogRow(R.string.cancel, Modifier, onDismiss)
        }
    }
}

@Composable
private fun DialogRow(@StringRes text: Int, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = stringResource(text),
        style = GalleryText.BodyLargeMedium,
        color = colorResource(R.color.titleColor),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}

/** The photo's fitted size times [scale] may overhang the frame; the pan stays within that overhang. */
private fun clampPan(pan: Offset, bmp: Bitmap, frame: IntSize, scale: Float): Offset {
    if (frame.width == 0) return Offset.Zero
    val fit = minOf(frame.width / bmp.width.toFloat(), frame.height / bmp.height.toFloat())
    val maxX = ((bmp.width * fit * scale - frame.width) / 2).coerceAtLeast(0f)
    val maxY = ((bmp.height * fit * scale - frame.height) / 2).coerceAtLeast(0f)
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

/** What the screen shows (the photo fitted, zoomed about the centre, panned) as a frame-sized bitmap. */
private fun onScreen(bmp: Bitmap, frame: IntSize, scale: Float, pan: Offset): Bitmap {
    val out = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
    val fit = minOf(frame.width / bmp.width.toFloat(), frame.height / bmp.height.toFloat()) * scale
    val w = bmp.width * fit
    val h = bmp.height * fit
    val left = (frame.width - w) / 2 + pan.x
    val top = (frame.height - h) / 2 + pan.y
    Canvas(out).drawBitmap(bmp, null, RectF(left, top, left + w, top + h), Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

private const val PREVIEW_SIDE = 2048
