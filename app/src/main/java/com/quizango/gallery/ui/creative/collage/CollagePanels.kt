package com.quizango.gallery.ui.creative.collage

import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.quizango.gallery.R
import com.quizango.gallery.ui.creative.GalleryRulerView
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlin.math.roundToInt

/** Inter at a reference size: `if_inter_medium` is W500, `inter` alone W400 and `inter` + bold W700. */
@Composable
internal fun inter(size: Int, weight: FontWeight = FontWeight.W400) = TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.ssp)

/** rvTools / rvIcons / rvRotateOption entries (item_tools): 22.sdp icon tinted collage_header_text_color over an 8.ssp label. */
@Composable
internal fun ToolItem(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    tint: Color = colorResource(R.color.collage_header_text_color),
    tintIcon: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .padding(top = 2.sdp, bottom = 2.sdp, end = 2.sdp)
            .height(60.sdp)
            .widthIn(min = 52.sdp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(icon),
            contentDescription = null,
            colorFilter = if (tintIcon) ColorFilter.tint(tint) else null,
            modifier = Modifier.size(22.sdp)
        )
        Text(
            stringResource(label),
            style = inter(8, FontWeight.W500),
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.sdp, start = 2.sdp, end = 2.sdp)
        )
    }
}

/** SeekBar with pline / seek_bar_thmub: a 3.sdp track in seekbar_disable_line, mainAppColor progress and the 19.dp ringed thumb. */
@Composable
internal fun CollageSeekBar(value: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 0) {
    val thumb = painterResource(R.drawable.seek_bar_thmub)
    val track = colorResource(R.color.seekbar_disable_line)
    val progress = colorResource(R.color.mainAppColor)
    val trackHeight = 3.sdp
    val change by rememberUpdatedState(onChange)
    val range = (max - min).coerceAtLeast(1)
    fun seek(x: Float, width: Int, r: Float) {
        val f = ((x - r) / (width - 2 * r)).coerceIn(0f, 1f)
        change(min + (f * range).roundToInt())
    }
    Canvas(
        modifier
            .height(30.sdp)
            .pointerInput(min, max) { detectTapGestures { seek(it.x, size.width, 9.5.dp.toPx()) } }
            .pointerInput(min, max) {
                detectHorizontalDragGestures { c, _ ->
                    c.consume()
                    seek(c.position.x, size.width, 9.5.dp.toPx())
                }
            }
    ) {
        val r = 9.5.dp.toPx()
        val y = size.height / 2
        val h = trackHeight.toPx()
        val end = size.width - r
        val x = r + (end - r) * (value - min).coerceIn(0, range) / range
        drawRoundRect(track, Offset(r, y - h / 2), Size(end - r, h), CornerRadius(h / 2))
        drawRoundRect(progress, Offset(r, y - h / 2), Size(x - r, h), CornerRadius(h / 2))
        translate(x - r, y - r) { with(thumb) { draw(Size(2 * r, 2 * r)) } }
    }
}

/** txtRadiusValue / txtSb*Value: the bold Inter number box, 50.sdp wide. */
@Composable
internal fun ValueBox(value: Int, size: Int = 10) {
    Text(
        value.toString(),
        style = inter(size, FontWeight.W700),
        color = colorResource(R.color.collage_header_text_color),
        textAlign = TextAlign.Center,
        modifier = Modifier.width(50.sdp).padding(horizontal = 10.sdp)
    )
}

/** The 50.sdp label column of a slider row ("Size", "Opacity"...). */
@Composable
internal fun LabelBox(@StringRes label: Int, style: TextStyle = inter(10), align: TextAlign = TextAlign.Start) {
    Text(stringResource(label), style = style, color = colorResource(R.color.collage_header_text_color), textAlign = align, modifier = Modifier.width(50.sdp))
}

/** bg_black_vertical tinted bg_vertical_separator, between a row's label and its slider. */
@Composable
internal fun VerticalSeparator(vertical: Int = 10, horizontal: Int = 5) {
    Box(
        Modifier
            .padding(horizontal = horizontal.sdp, vertical = vertical.sdp)
            .width(2.sdp)
            .fillMaxHeight()
            .background(colorResource(R.color.bg_vertical_separator), RoundedCornerShape(50))
    )
}

/** Label, separator, slider and value: the shape every collage slider row shares. */
@Composable
internal fun SliderRow(
    @StringRes label: Int,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    height: Int = 50,
    min: Int = 0,
    labelStyle: TextStyle = inter(10),
    labelAlign: TextAlign = TextAlign.Start,
    valueSize: Int = 11
) {
    Row(Modifier.fillMaxWidth().height(height.sdp), verticalAlignment = Alignment.CenterVertically) {
        LabelBox(label, labelStyle, labelAlign)
        VerticalSeparator(vertical = 14)
        CollageSeekBar(value, max, onChange, Modifier.weight(1f), min)
        ValueBox(value, valueSize)
    }
}

/** btn*Close (ic_close_green_d) / title / btn*Done (ic_saved_d), 35 x 28.sdp each end. */
@Composable
internal fun CloseDoneHeader(@StringRes title: Int?, onClose: (() -> Unit)?, onDone: () -> Unit, titleStyle: TextStyle = inter(12, FontWeight.W700)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CornerButton(R.drawable.ic_close_green_d, onClose)
        Text(
            title?.let { stringResource(it) } ?: "",
            style = titleStyle,
            color = colorResource(R.color.collage_header_text_color),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).heightIn(min = 30.sdp).padding(top = 6.sdp)
        )
        CornerButton(R.drawable.ic_saved_d, onDone)
    }
}

/** A close/done corner button; a null action keeps its slot but hides it (btnRotateClose is invisible). */
@Composable
private fun CornerButton(@DrawableRes icon: Int, onClick: (() -> Unit)?) {
    val m = Modifier.size(width = 35.sdp, height = 28.sdp)
    if (onClick == null) Spacer(m)
    else Image(painterResource(icon), contentDescription = null, modifier = m.clickable(onClick = onClick))
}

/** layout_ratio_collage: the 20 presets as item_crop icons, the selected one in its _select art and mainAppColor. */
@Composable
internal fun RatioPanel(selected: Int, onSelect: (Int) -> Unit, onClose: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color)).padding(10.sdp)) {
        CloseDoneHeader(R.string.ratio, onClose, onDone)
        LazyRow(Modifier.fillMaxWidth().padding(top = 15.sdp, bottom = 10.sdp), verticalAlignment = Alignment.Bottom) {
            itemsIndexed(COLLAGE_RATIOS) { i, r ->
                val on = i == selected
                Column(
                    Modifier.clickable { onSelect(i) }.padding(horizontal = 4.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(if (on) r.selectedIcon else r.icon), contentDescription = null, modifier = Modifier.height(42.sdp))
                    Text(
                        if (r.labelRes != 0) stringResource(r.labelRes) else r.label,
                        style = inter(8, FontWeight.W700),
                        color = colorResource(if (on) R.color.mainAppColor else R.color.strokeColor),
                        modifier = Modifier.padding(top = 4.sdp)
                    )
                }
            }
        }
    }
}

/** The three ctxLayout tabs (Layout | Border | Background) on bg_layout_tabs, with the iSaved done button. */
internal enum class LayoutTab(@StringRes val label: Int) { LAYOUT(R.string.layout), BORDER(R.string.border), BACKGROUND(R.string.background) }

@Composable
internal fun LayoutTabsPanel(tab: LayoutTab, onTab: (LayoutTab) -> Unit, onDone: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color))) {
        Row(Modifier.fillMaxWidth().padding(top = 10.sdp).padding(vertical = 5.sdp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.sdp)
                    .drawableBackground(R.drawable.bg_layout_tabs)
            ) {
                LayoutTab.entries.forEach { t -> TabCell(stringResource(t.label), t == tab, 15) { onTab(t) } }
            }
            Image(
                painterResource(R.drawable.ic_saved_d),
                contentDescription = stringResource(R.string.done),
                modifier = Modifier.padding(end = 8.sdp).size(width = 35.sdp, height = 28.sdp).clickable(onClick = onDone)
            )
        }
        Column(Modifier.fillMaxWidth().heightIn(min = 90.sdp).padding(5.sdp), verticalArrangement = Arrangement.Center, content = content)
    }
}

/** A tab_selector text tab: mainAppColor with its tab_bottom_selector_line when selected, disableTabTextColor otherwise. */
@Composable
internal fun RowScope.TabCell(text: String, selected: Boolean, lineWidth: Int, onClick: () -> Unit) {
    Box(Modifier.weight(1f).clickable(onClick = onClick), contentAlignment = Alignment.BottomCenter) {
        Text(
            text,
            style = inter(10),
            color = colorResource(if (selected) R.color.mainAppColor else R.color.disableTabTextColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.sdp)
        )
        if (selected) Box(
            Modifier
                .offset(y = (-5).sdp)
                .size(width = lineWidth.sdp, height = 1.sdp)
                .background(colorResource(R.color.mainAppColor), RoundedCornerShape(6.sdp))
        )
    }
}

/** llLayoutSection: "Ratio/size" then the rvLayer template previews (item_layer, PerfectGridView lines). */
@Composable
internal fun LayoutSection(templates: List<CollageTemplate>, selected: Int, onSelect: (Int) -> Unit, onRatio: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.sdp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.clickable(onClick = onRatio).padding(horizontal = 4.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.ic_ratio),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResource(R.color.selectedTabTextColor)),
                modifier = Modifier.size(20.sdp)
            )
            Text(stringResource(R.string.ratio_size), style = inter(9), color = colorResource(R.color.collage_header_text_color))
        }
        VerticalSeparator(vertical = 10, horizontal = 8)
        val on = colorResource(R.color.selectedTabTextColor)
        val off = colorResource(R.color.seekbar_disable_line)
        LazyRow(Modifier.weight(1f)) {
            itemsIndexed(templates) { i, t ->
                val line = if (i == selected) on else off
                Canvas(
                    Modifier
                        .padding(horizontal = 4.sdp)
                        .size(width = 50.sdp, height = 55.sdp)
                        .clickable { onSelect(i) }
                ) {
                    val side = minOf(size.width, size.height)
                    val left = (size.width - side) / 2
                    val top = (size.height - side) / 2
                    val gap = 2.dp.toPx()
                    t.cells().forEach { c ->
                        drawRoundRect(
                            line,
                            Offset(left + c.left * side + gap, top + c.top * side + gap),
                            Size((c.right - c.left) * side - 2 * gap, (c.bottom - c.top) * side - 2 * gap),
                            CornerRadius(2.dp.toPx()),
                            style = Stroke(1.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/** llPadding: Radius (0..200, default 16) and Padding (0..50, default 6) rows. */
@Composable
internal fun BorderSection(radius: Int, padding: Int, onRadius: (Int) -> Unit, onPadding: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        IconSliderRow(R.drawable.ic_radius, R.string.radius, radius, MAX_RADIUS, onRadius)
        Spacer(Modifier.height(10.sdp))
        IconSliderRow(R.drawable.ic_padding, R.string.padding, padding, MAX_PADDING, onPadding)
    }
}

@Composable
private fun IconSliderRow(@DrawableRes icon: Int, @StringRes label: Int, value: Int, max: Int, onChange: (Int) -> Unit) {
    val ink = colorResource(R.color.collage_header_text_color)
    Row(Modifier.fillMaxWidth().height(40.sdp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(50.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(icon), contentDescription = null, colorFilter = ColorFilter.tint(ink), modifier = Modifier.size(18.sdp))
            Text(stringResource(label), style = inter(9), color = ink)
        }
        VerticalSeparator(vertical = 5, horizontal = 8)
        CollageSeekBar(value, max, onChange, Modifier.weight(1f))
        ValueBox(value)
    }
}

/** rvMosaic local entries (item_res, oCOLLAGE): Blur Bg / Custom / Select as icon buttons, then Png, White, Black and Color tiles. */
internal enum class BgOption(@DrawableRes val icon: Int, @StringRes val label: Int, val tile: Boolean) {
    BLUR(R.drawable.ic_blur_collage, R.string.blur_bg, false),
    CUSTOM(R.drawable.ic_gallery_custom, R.string.custom, false),
    SELECT(R.drawable.ic_color_piker, R.string.select, false),
    PNG(R.drawable.iv_trans, R.string.png, true),
    WHITE(R.drawable.iv_white, R.string.white, true),
    BLACK(R.drawable.iv_black, R.string.black, true),
    COLOR(R.drawable.iv_color, R.string.color, true)
}

@Composable
internal fun BackgroundSection(selected: BgOption?, onOption: (BgOption) -> Unit) {
    val ink = colorResource(R.color.collage_header_text_color)
    val icon = colorResource(R.color.txtColor)
    LazyRow(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(BgOption.entries) { _, o ->
            if (!o.tile) Column(
                Modifier.size(width = 50.sdp, height = 55.sdp).padding(top = 5.sdp).clickable { onOption(o) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(painterResource(o.icon), contentDescription = null, colorFilter = ColorFilter.tint(icon), modifier = Modifier.size(24.sdp))
                Text(stringResource(o.label), style = inter(8), color = icon, maxLines = 1)
            } else Column(Modifier.padding(start = 5.sdp).clickable { onOption(o) }, horizontalAlignment = Alignment.CenterHorizontally) {
                val stroke = if (o == selected) colorResource(R.color.selectedTabTextColor) else Color.Transparent
                Image(
                    painterResource(o.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .border(2.sdp, stroke, RoundedCornerShape(9.sdp))
                        .padding(2.sdp)
                        .size(45.sdp)
                        .clip(RoundedCornerShape(7.sdp))
                        .background(colorResource(R.color.itemsColor))
                )
                Text(stringResource(o.label), style = inter(8, FontWeight.W700), color = ink, modifier = Modifier.padding(top = 5.sdp))
            }
            // iLine: the separator after the last icon button
            if (o == BgOption.SELECT) Box(
                Modifier
                    .padding(horizontal = 9.sdp, vertical = 10.sdp)
                    .size(width = 2.sdp, height = 50.sdp)
                    .background(colorResource(R.color.bg_vertical_separator), RoundedCornerShape(50))
            )
        }
    }
}

/** The `list` swatches (item_color_collage): a back tile on itemsColor, then the je/i colours with ic_color_selected on the picked one. */
@Composable
internal fun ColorSwatches(colors: List<Int>, selected: Int?, onBack: (() -> Unit)?, onPick: (Int) -> Unit, width: Int = 35, height: Int = 45) {
    LazyRow(Modifier.fillMaxWidth().padding(vertical = 9.sdp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) item {
            Box(
                Modifier
                    .padding(start = 4.sdp, end = 16.sdp)
                    .size(width = width.sdp, height = height.sdp)
                    .clip(RoundedCornerShape(2.sdp))
                    .background(colorResource(R.color.itemsColor))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.viewer_back),
                    colorFilter = ColorFilter.tint(colorResource(R.color.onlyWhite)),
                    modifier = Modifier.padding(9.sdp)
                )
            }
        }
        itemsIndexed(colors) { _, c ->
            Box(
                Modifier
                    .padding(horizontal = 1.sdp)
                    .size(width = width.sdp, height = height.sdp)
                    .background(Color(c))
                    .border(0.5.dp, colorResource(R.color.border_gray_color_collage))
                    .clickable { onPick(c) },
                contentAlignment = Alignment.Center
            ) {
                if (c == selected) Image(painterResource(R.drawable.ic_color_selected), contentDescription = null, modifier = Modifier.size(20.sdp))
            }
        }
    }
}

/** layout_blur_bg_collage: close / "Blur Bg" / done over the 0..25 blur slider between its value boxes. */
@Composable
internal fun BlurPanel(value: Int, onChange: (Int) -> Unit, onClose: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color)).padding(10.sdp)) {
        CloseDoneHeader(R.string.blur_bg, onClose, onDone)
        Row(Modifier.fillMaxWidth().padding(top = 10.sdp).height(50.sdp), verticalAlignment = Alignment.CenterVertically) {
            ValueBox(value, 14)
            CollageSeekBar(value, MAX_BLUR, onChange, Modifier.weight(1f))
            ValueBox(MAX_BLUR, 14)
        }
    }
}

internal enum class RotateOption(@DrawableRes val icon: Int, @StringRes val label: Int) {
    ROTATE_90(R.drawable.ic_right_rotate_90, R.string.rotate_90),
    ZOOM_IN(R.drawable.ic_zoom_collage, R.string.zoom_in),
    ZOOM_OUT(R.drawable.ic_zoom_out, R.string.zoom_out),
    LEFT(R.drawable.ic_left_move, R.string.left),
    RIGHT(R.drawable.ic_right_move, R.string.right),
    UP(R.drawable.ic_up_move, R.string.up),
    DOWN(R.drawable.ic_down_move, R.string.down)
}

/** layout_rotate_collage: "Rotate" with done only, the rotation ruler and the rvRotateOption tools. */
@Composable
internal fun RotatePanel(tilt: Int, onTilt: (Int) -> Unit, onOption: (RotateOption) -> Unit, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color)).padding(vertical = 10.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(horizontal = 10.sdp)) { CloseDoneHeader(R.string.rotate, null, onDone, inter(11, FontWeight.W700)) }
        Image(painterResource(R.drawable.ic_rotation_arrow_mark), contentDescription = null, modifier = Modifier.size(10.sdp))
        GalleryRulerView(
            value = tilt,
            onValueChange = onTilt,
            minValue = -MAX_ROTATE,
            maxValue = MAX_ROTATE,
            modifier = Modifier.padding(bottom = 10.sdp),
            middleLineColor = colorResource(R.color.selectedTabTextColor),
            stickColor = colorResource(R.color.view_line)
        )
        Row(Modifier.padding(top = 15.sdp).horizontalScroll(rememberScrollState())) {
            RotateOption.entries.forEach { o -> ToolItem(o.icon, o.label) { onOption(o) } }
        }
    }
}

internal enum class BrushTab(@DrawableRes val icon: Int, val kind: BrushKind) {
    PAINT(R.drawable.ic_brush_collage, BrushKind.PAINT),
    NEON(R.drawable.ic_brush_neon, BrushKind.NEON),
    ERASE(R.drawable.ic_eraser_brush, BrushKind.ERASE)
}

/** layout_brush_collage: Paint / Neon / Eraser tabs with done, then the size row and, for the two brushes, the rj/c colours. */
@Composable
internal fun BrushPanel(
    tab: BrushTab,
    onTab: (BrushTab) -> Unit,
    size: Int,
    onSize: (Int) -> Unit,
    color: Int,
    onColor: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color)).padding(top = 5.sdp).padding(5.sdp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.sdp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(15.sdp, Alignment.CenterHorizontally)) {
                BrushTab.entries.forEach { t ->
                    Image(
                        painterResource(t.icon),
                        contentDescription = null,
                        colorFilter = if (t == tab) null else ColorFilter.tint(colorResource(R.color.disableTabTextColor)),
                        modifier = Modifier.size(35.sdp).clickable { onTab(t) }.padding(5.sdp)
                    )
                }
            }
            Image(
                painterResource(R.drawable.ic_saved_d),
                contentDescription = stringResource(R.string.done),
                modifier = Modifier.size(width = 35.sdp, height = 28.sdp).clickable(onClick = onDone)
            )
        }
        SliderRow(R.string.size, size, MAX_BRUSH, onSize, labelStyle = inter(11, FontWeight.W500), labelAlign = TextAlign.Center)
        if (tab != BrushTab.ERASE) ColorSwatches(COLLAGE_PAINT_COLORS, color, null, onColor, width = 18, height = 25)
    }
}

/** layout_add_text_collage: after a text is placed, close / done and the "Add New Text" pill. */
@Composable
internal fun AddTextPanel(onAdd: () -> Unit, onClose: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color)).padding(vertical = 10.sdp)) {
        Box(Modifier.padding(horizontal = 10.sdp)) { CloseDoneHeader(null, onClose, onDone) }
        Pill(R.string.add_new_text_label, Modifier.padding(start = 10.sdp, end = 10.sdp, top = 10.sdp, bottom = 5.sdp).height(35.sdp), inter(12, FontWeight.W500), onAdd)
    }
}

/** bg_circle tinted mainAppColor with white text (Add New Text, Exit, Try again). */
@Composable
internal fun Pill(@StringRes text: Int, modifier: Modifier, style: TextStyle, onClick: () -> Unit, textColor: Color = colorResource(R.color.onlyWhite)) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(colorResource(R.color.mainAppColor))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(text), style = style, color = textColor)
    }
}

/** cStickers with noNetSticker: 2.7.2 loads stickers from its server, and offline it shows this card. */
@Composable
internal fun StickerOfflinePanel(onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.picker_toolbar_color))
            .border(0.5.dp, colorResource(R.color.white_only))
            .padding(vertical = 25.sdp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.please_turn_on_internet_and_try_again),
                style = inter(9),
                color = colorResource(R.color.black),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(200.sdp)
            )
            Text(
                stringResource(R.string.try_again),
                style = inter(9),
                color = colorResource(R.color.buttonTextColor),
                modifier = Modifier
                    .padding(top = 12.sdp)
                    .clip(RoundedCornerShape(50))
                    .background(colorResource(R.color.mainAppColor))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.sdp, vertical = 7.sdp)
            )
        }
    }
}

internal enum class CellTool(@DrawableRes val icon: Int, @StringRes val label: Int) {
    REPLACE(R.drawable.ic_picker_collage, R.string.replace),
    SWAP(R.drawable.ic_swap, R.string.swap),
    ROTATE(R.drawable.ic_rotate, R.string.rotate),
    VERTICAL(R.drawable.ic_vertical_collage, R.string.vertical),
    HORIZONTAL(R.drawable.ic_horizontal, R.string.horizontal),
    RIGHT(R.drawable.ic_right_collage, R.string.right)
}

/** cIcons: the ivDown close knob over a grid_select_bg strip of the selected cell's tools (rvIcons). */
@Composable
internal fun CellToolsBar(onClose: () -> Unit, onTool: (CellTool) -> Unit) {
    val bg = colorResource(R.color.grid_select_bg)
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.close),
            colorFilter = ColorFilter.tint(colorResource(R.color.collage_header_text_color)),
            modifier = Modifier
                .offset(y = 2.sdp)
                .size(35.sdp)
                .drawableBackground(R.drawable.bg_black_round)
                .clickable(onClick = onClose)
                .padding(7.sdp)
        )
        Box(Modifier.size(width = 12.sdp, height = 7.sdp).background(bg))
        Box(Modifier.fillMaxWidth().height(2.sdp).background(bg))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
            CellTool.entries.forEach { t -> ToolItem(t.icon, t.label) { onTool(t) } }
        }
    }
}

/**
 * dialog_exit_popup (me/d.HISPj7KHQ7): a full-width sheet at the bottom; Exit leaves without saving, Cancel and outside
 * taps keep editing.
 */
@Composable
internal fun CollageExitDialog(onExit: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.run {
                setGravity(Gravity.BOTTOM)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        val ink = colorResource(R.color.collage_header_text_color)
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.picker_toolbar_color))
                .padding(10.sdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().height(40.sdp).padding(bottom = 0.sdp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exit_without_saving), style = inter(16, FontWeight.W700), color = ink, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(9.sdp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.sdp).height(40.sdp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.are_you_sure_you_want_to_discard_changes_message),
                    style = inter(13, FontWeight.W500),
                    color = ink,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(9.sdp))
            Pill(R.string.exit, Modifier.padding(horizontal = 16.sdp).height(40.sdp), inter(14), onExit)
            Box(Modifier.fillMaxWidth().height(45.sdp).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.cancel), style = inter(14), color = colorResource(R.color.disableTabTextColor))
            }
        }
    }
}

/** A colour as ARGB with its alpha replaced. */
internal fun withAlpha(color: Int, alpha: Int) = Color(color).copy(alpha = alpha.coerceIn(0, 255) / 255f).toArgb()
