package com.quizango.gallery.ui.editor

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp

/** ?selectableItemBackgroundBorderless */
@Composable
internal fun Modifier.borderlessClick(onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false), onClick = onClick)

/** ic_cancle / ic_done tinted ic_lock_color. */
@Composable
private fun HeaderIcon(@DrawableRes icon: Int, @StringRes description: Int, modifier: Modifier, onClick: () -> Unit) {
    Image(
        painter = painterResource(icon),
        contentDescription = stringResource(description),
        colorFilter = ColorFilter.tint(colorResource(R.color.ic_lock_color)),
        modifier = modifier.borderlessClick(onClick)
    )
}

/** A panel's top RelativeLayout: cancel at the start, the title centred, done at the end. */
@Composable
internal fun PanelHeader(
    onClose: () -> Unit,
    onDone: () -> Unit,
    padding: PaddingValues,
    iconTop: Int = 0,
    iconSide: Int = 0,
    center: @Composable BoxScope.() -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(padding)) {
        HeaderIcon(R.drawable.ic_cancle, R.string.cancel, Modifier.align(Alignment.TopStart).padding(top = iconTop.sdp, start = iconSide.sdp), onClose)
        Box(Modifier.align(Alignment.Center)) { center() }
        HeaderIcon(R.drawable.ic_done, R.string.done, Modifier.align(Alignment.TopEnd).padding(top = iconTop.sdp, end = iconSide.sdp), onDone)
    }
}

@Composable
internal fun PanelTitle(text: String) {
    Text(text, style = GalleryText.BodyLargeMedium, color = colorResource(R.color.titleColor))
}

/** rvTab of item_edit cells: icon, label and the "AI" / "New" badge at the icon's top end (cv/e). */
@Composable
internal fun ToolStrip(tools: List<EditTool>, isNew: (EditTool) -> Boolean, onTap: (EditTool) -> Unit, onPlaced: (EditTool, Float) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.sdp)) {
        items(tools, key = { it.name }) { tool ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .onGloballyPositioned { onPlaced(tool, it.positionInRoot().x + it.size.width / 2f) }
                    .borderlessClick { onTap(tool) }
            ) {
                Box(Modifier.padding(top = 15.sdp)) {
                    Image(painterResource(tool.icon), contentDescription = null)
                    val badge = when {
                        isNew(tool) -> R.string.new_edit to R.drawable.ic_edit_new_bg
                        tool.ai -> R.string.AI to R.drawable.ic_ai_edit_bg
                        else -> null
                    }
                    // Badge starts at the icon's end, 5.sdp below the cell top (10.sdp above the icon)
                    val lift = 10.sdp
                    if (badge != null) Box(
                        Modifier.align(Alignment.TopEnd).layout { measurable, _ ->
                            val p = measurable.measure(Constraints())
                            layout(0, 0) { p.place(0, -lift.roundToPx()) }
                        }
                    ) {
                        Text(
                            stringResource(badge.first),
                            style = GalleryText.BodyXXSmallMedium,
                            color = colorResource(R.color.onlyWhite),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.drawableBackground(badge.second).padding(horizontal = 3.sdp, vertical = 1.sdp)
                        )
                    }
                }
                Text(
                    stringResource(tool.label),
                    style = GalleryText.BodyXSmallMedium,
                    color = colorResource(R.color.titleColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 12.sdp, end = 12.sdp, top = 5.sdp, bottom = 10.sdp)
                )
            }
        }
    }
}

/** item_crop_layout: flip / rotate buttons over ratio_recycler (item_ratio, selected in dark_blue). */
@Composable
internal fun CropPanel(
    preset: CropPreset,
    onPreset: (CropPreset) -> Unit,
    onFlipX: () -> Unit,
    onFlipY: () -> Unit,
    onRotate: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.backgroundColor))) {
        PanelHeader(onClose, onDone, PaddingValues(horizontal = 10.sdp, vertical = 3.sdp), iconTop = 8, iconSide = 2) {
            PanelTitle(stringResource(R.string.crop))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CropButton(R.drawable.ic_vertical, R.string.flip_X, 0, onFlipX)
            CropButton(R.drawable.ic_horizon, R.string.flip_Y, 14, onFlipY)
            CropButton(R.drawable.ic_rotate, R.string.rotate_90, 0, onRotate)
        }
        LazyRow(Modifier.fillMaxWidth()) {
            items(CropPreset.entries, key = { it.name }) { p ->
                val tint = colorResource(if (p == preset) R.color.dark_blue else R.color.titleColor)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 10.sdp).borderlessClick { onPreset(p) }
                ) {
                    Box(Modifier.size(55.sdp), contentAlignment = Alignment.Center) {
                        p.background?.let { Image(painterResource(it), null, Modifier.padding(start = 8.sdp, end = 8.sdp, top = 4.sdp)) }
                        Image(painterResource(p.icon), null, colorFilter = ColorFilter.tint(tint))
                    }
                    Text(stringResource(p.label), style = GalleryText.BodyXSmallMedium, color = tint)
                }
            }
        }
    }
}

@Composable
private fun CropButton(@DrawableRes icon: Int, @StringRes label: Int, iconMargin: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(10.sdp).borderlessClick(onClick)
    ) {
        Image(painterResource(icon), null, Modifier.padding(horizontal = iconMargin.sdp).padding(2.sdp))
        Text(stringResource(label), style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.titleColor), modifier = Modifier.padding(top = 5.sdp))
    }
}

/** item_filter_layout: 45.sdp card thumbnails, the selected one stroked 4px in FilterBodarColor (cv/i). */
@Composable
internal fun FilterPanel(thumbs: List<Pair<EditorFilter, Bitmap>>, selected: EditorFilter, onSelect: (EditorFilter) -> Unit, onClose: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.white))) {
        PanelHeader(onClose, onDone, PaddingValues(horizontal = 15.sdp, vertical = 10.sdp)) { PanelTitle(stringResource(R.string.filters)) }
        val stroke = colorResource(R.color.FilterBodarColor)
        val strokeWidth = with(LocalDensity.current) { 4.toDp() } // cv/i setStrokeWidth(4) is in pixels
        val shape = RoundedCornerShape(4.dp) // MaterialCardView default corner
        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 5.sdp)) {
            items(thumbs, key = { it.first.name }) { (filter, thumb) ->
                Box(
                    Modifier.padding(bottom = 7.sdp).size(55.sdp).borderlessClick { onSelect(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = remember(thumb) { thumb.asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(45.sdp)
                            .clip(shape)
                            .then(if (filter == selected) Modifier.border(strokeWidth, stroke, shape) else Modifier)
                    )
                }
            }
        }
    }
}

/** item_adjust_layout: the parameter name, its value over the ruler, then adjust_bottom_navigation. */
@Composable
internal fun AdjustPanel(
    param: AdjustParam,
    value: Int,
    onParam: (AdjustParam) -> Unit,
    onValue: (Int) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.white)), horizontalAlignment = Alignment.CenterHorizontally) {
        PanelHeader(onClose, onDone, PaddingValues(start = 10.sdp, end = 10.sdp, top = 10.sdp, bottom = 5.sdp)) { PanelTitle(stringResource(param.label)) }
        Text(value.toString(), style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.colorPrimary))
        com.quizango.gallery.ui.creative.GalleryRulerView(
            value = value,
            onValueChange = onValue,
            minValue = param.min,
            maxValue = param.max,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Box(Modifier.fillMaxWidth().padding(start = 12.sdp, end = 12.sdp, bottom = 4.sdp).height(1.sdp).background(colorResource(R.color.edit_view_line)))
        Row(Modifier.fillMaxWidth().padding(bottom = 5.sdp).height(56.dp)) {
            AdjustParam.entries.forEach { p ->
                val active = p == param
                val tint = colorResource(if (active) R.color.colorPrimary else R.color.titleColor)
                Column(
                    Modifier.weight(1f).height(56.dp).borderlessClick { onParam(p) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(painterResource(p.icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(tint))
                    // BottomNavigationView labels: 14sp active, 12sp inactive
                    Text(stringResource(p.label), fontFamily = Inter, fontSize = if (active) 14.sp else 12.sp, color = tint, maxLines = 1)
                }
            }
        }
    }
}

/** ic_brush_progress_bg box holding a slider end label in dark_blue. */
@Composable
internal fun SliderEnd(text: String, modifier: Modifier = Modifier) {
    Box(modifier.size(28.sdp).drawableBackground(R.drawable.ic_brush_progress_bg), contentAlignment = Alignment.Center) {
        Text(text, style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.dark_blue))
    }
}

/**
 * The Material Slider of the brush and text panels (thumb radius 8dp, halo 2dp, colorPrimary active track on
 * transparent_color), or a StartPointSeekBar when [fillFrom] is set (range filled from that value).
 */
@Composable
internal fun EditorSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    fillFrom: Float? = null,
    onRelease: () -> Unit = {}
) {
    val change by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)
    val active = colorResource(if (fillFrom == null) R.color.colorPrimary else R.color.dark_blue)
    val inactive = colorResource(if (fillFrom == null) R.color.transparent_color else R.color.seekbar_color_bg)
    val thumb = colorResource(R.color.colorPrimary)
    val span = range.endInclusive - range.start
    fun seek(x: Float, width: Int, r: Float) = change(range.start + ((x - r) / (width - 2 * r)).coerceIn(0f, 1f) * span)
    Canvas(
        modifier
            .height(32.dp)
            .pointerInput(range) { detectTapGestures(onTap = { seek(it.x, size.width, 10.dp.toPx()); release() }) }
            .pointerInput(range) {
                detectHorizontalDragGestures(onDragEnd = { release() }, onDragCancel = { release() }) { c, _ ->
                    c.consume()
                    seek(c.position.x, size.width, 10.dp.toPx())
                }
            }
    ) {
        val r = 10.dp.toPx()
        val y = size.height / 2
        val h = 4.dp.toPx()
        val w = size.width - 2 * r
        fun xOf(v: Float) = r + w * ((v - range.start) / span).coerceIn(0f, 1f)
        val x = xOf(value)
        drawRoundRect(inactive, Offset(r, y - h / 2), Size(w, h), CornerRadius(h / 2))
        val from = xOf(fillFrom ?: range.start)
        drawRoundRect(active, Offset(minOf(from, x), y - h / 2), Size(kotlin.math.abs(x - from), h), CornerRadius(h / 2))
        drawCircle(thumb, 8.dp.toPx(), Offset(x, y))
    }
}

/** item_color (cv/a): a 20.sdp dot of the colour; the selected one gets ic_color_ring tinted the same colour. */
@Composable
internal fun ColorSwatches(colors: List<Int>, selected: Int?, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier) {
        itemsIndexed(colors) { _, c ->
            Box(
                Modifier
                    .padding(horizontal = 2.sdp)
                    .then(if (c == selected) Modifier.drawableBackground(R.drawable.ic_color_ring, c) else Modifier)
                    .borderlessClick { onPick(c) }
            ) {
                Image(
                    painterResource(R.drawable.ic_color_bg),
                    null,
                    Modifier.padding(5.sdp).size(20.sdp),
                    colorFilter = ColorFilter.tint(Color(c))
                )
            }
        }
    }
}

/** item_brush_layout. */
@Composable
internal fun BrushPanel(
    size: Float,
    onSize: (Float) -> Unit,
    onSizeRelease: () -> Unit,
    colors: List<Int>,
    color: Int,
    onColor: (Int) -> Unit,
    eyedropper: Boolean,
    onEyedropper: () -> Unit,
    onPicker: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.white))) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.sdp, vertical = 2.sdp)) {
            HeaderIcon(R.drawable.ic_cancle, R.string.cancel, Modifier.align(Alignment.CenterStart).padding(3.sdp), onClose)
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_undo), stringResource(R.string.undo), Modifier.borderlessClick(onUndo).padding(3.sdp))
                Text(stringResource(R.string.brush), style = GalleryText.BodyLargeMedium, color = colorResource(R.color.titleColor), modifier = Modifier.padding(horizontal = 12.sdp))
                Image(painterResource(R.drawable.ic_redo), stringResource(R.string.editor_redo), Modifier.borderlessClick(onRedo).padding(3.sdp))
            }
            HeaderIcon(R.drawable.ic_done, R.string.done, Modifier.align(Alignment.CenterEnd), onDone)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.sdp, vertical = 5.sdp), verticalAlignment = Alignment.CenterVertically) {
            SliderEnd("0", Modifier.padding(end = 5.sdp))
            EditorSlider(size, 0f..100f, onSize, Modifier.weight(1f), onRelease = onSizeRelease)
            SliderEnd(stringResource(R.string.hundred), Modifier.padding(start = 5.sdp))
        }
        Box(Modifier.fillMaxWidth().padding(start = 12.sdp, end = 12.sdp, bottom = 10.sdp).height(1.sdp).background(colorResource(R.color.edit_view_line)))
        ColorRow(colors, color, eyedropper, onEyedropper, onPicker, onColor, Modifier.padding(bottom = 8.sdp))
    }
}

/** brushColorCustomPicker (on-image eyedropper), brushColorPicker (Choose color dialog) and the swatch list. */
@Composable
internal fun ColorRow(
    colors: List<Int>,
    color: Int?,
    eyedropper: Boolean,
    onEyedropper: () -> Unit,
    onPicker: () -> Unit,
    onColor: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.ic_picker),
            null,
            Modifier
                .padding(start = 5.sdp)
                .then(if (eyedropper) Modifier.background(colorResource(R.color.transparent_color), RoundedCornerShape(50)) else Modifier)
                .borderlessClick(onEyedropper)
                .padding(5.sdp)
        )
        Image(painterResource(R.drawable.ic_color_picker), stringResource(R.string.choose_color), Modifier.borderlessClick(onPicker).padding(5.sdp))
        ColorSwatches(colors, color, onColor, Modifier.weight(1f))
    }
}

/** item_sticker_layout: "Add Sticker" and "Done" halves on ic_edit_navigation_bg. */
@Composable
internal fun StickerPanel(onAdd: () -> Unit, onDone: () -> Unit) {
    Row(Modifier.fillMaxWidth().drawableBackground(R.drawable.ic_edit_navigation_bg)) {
        StickerHalf(R.drawable.ic_plus, R.string.add_sticker, onAdd)
        StickerHalf(R.drawable.ic_done, R.string.done, onDone)
    }
}

@Composable
private fun RowScope.StickerHalf(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).borderlessClick(onClick).padding(vertical = 10.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(icon), null)
        Text(stringResource(label), style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.titleColor), modifier = Modifier.padding(top = 2.sdp))
    }
}

/** The text panel's sub-tool (textTypeName): none, Font Styles, Colors (Color | Gradient), Opacity or Shadow. */
internal enum class TextSubTool { NONE, FONT, COLOR, OPACITY, SHADOW }

/** item_text_layout. */
@Composable
internal fun TextPanel(
    sub: TextSubTool,
    onSub: (TextSubTool) -> Unit,
    onPlus: () -> Unit,
    selected: TextItem?,
    onChange: (TextItem) -> Unit,
    colors: List<Int>,
    eyedropper: Boolean,
    onEyedropper: () -> Unit,
    onPicker: () -> Unit,
    gradientMode: Boolean,
    onGradientMode: (Boolean) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.white))) {
        PanelHeader(onClose, onDone, PaddingValues(horizontal = 10.sdp, vertical = 5.sdp)) {
            when (sub) {
                TextSubTool.NONE -> PanelTitle(stringResource(R.string.text))
                TextSubTool.FONT -> PanelTitle(stringResource(R.string.font_styles))
                TextSubTool.OPACITY -> PanelTitle(stringResource(R.string.opacity))
                TextSubTool.SHADOW -> PanelTitle(stringResource(R.string.shadow))
                TextSubTool.COLOR -> Segmented(gradientMode, onGradientMode)
            }
        }
        when (sub) {
            TextSubTool.FONT -> LazyRow(Modifier.fillMaxWidth()) {
                itemsIndexed(EDITOR_FONTS) { i, face ->
                    Box(Modifier.padding(8.sdp).size(28.sdp).drawableBackground(R.drawable.ic_brush_progress_bg).borderlessClick {
                        selected?.let { onChange(it.copy(font = i)) }
                    }, contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ab), fontFamily = FontFamily(face), fontSize = 18.sp, color = colorResource(R.color.black))
                    }
                }
            }
            TextSubTool.OPACITY -> Row(Modifier.fillMaxWidth().padding(horizontal = 10.sdp, vertical = 5.sdp), verticalAlignment = Alignment.CenterVertically) {
                SliderEnd("0", Modifier.padding(end = 5.sdp))
                EditorSlider(selected?.opacity ?: 1f, 0f..1f, { v -> selected?.let { onChange(it.copy(opacity = v)) } }, Modifier.weight(1f))
                SliderEnd(stringResource(R.string.hundred), Modifier.padding(start = 5.sdp))
            }
            TextSubTool.SHADOW -> Row(Modifier.fillMaxWidth().padding(horizontal = 10.sdp, vertical = 5.sdp), verticalAlignment = Alignment.CenterVertically) {
                val shadow = selected?.shadow ?: 0f
                SliderEnd("0", Modifier.padding(end = 5.sdp))
                EditorSlider(shadow, -30f..30f, { v -> selected?.let { onChange(it.copy(shadow = v)) } }, Modifier.width(210.sdp), fillFrom = 0f)
                SliderEnd(shadow.toInt().toString(), Modifier.padding(start = 5.sdp))
            }
            TextSubTool.COLOR -> if (!gradientMode) {
                ColorRow(colors, selected?.color, eyedropper, onEyedropper, onPicker, { c -> selected?.let { onChange(it.copy(color = c, gradient = null)) } }, Modifier.padding(bottom = 5.sdp))
            } else Row(Modifier.fillMaxWidth().padding(bottom = 5.sdp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                val g = selected?.gradient ?: TextGradient(colors[0], colors[colors.size - 1], 0.5f)
                GradientDot(g.start)
                EditorSlider(g.split, 0f..1f, { v -> selected?.let { onChange(it.copy(gradient = g.copy(split = v))) } }, Modifier.width(230.sdp), fillFrom = 0f)
                GradientDot(g.end)
            }
            TextSubTool.NONE -> Unit
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.sdp).height(1.sdp).background(colorResource(R.color.edit_view_line)))
        Row(Modifier.fillMaxWidth().padding(10.sdp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_plus), stringResource(R.string.editor_add_text), Modifier.borderlessClick(onPlus))
            Box(Modifier.padding(start = 10.sdp).width(1.sdp).height(30.sdp).background(colorResource(R.color.text_line_color)))
            TextTab(R.drawable.ic_font, R.string.font) { onSub(TextSubTool.FONT) }
            TextTab(R.drawable.ic_color, R.string.color) { onSub(TextSubTool.COLOR) }
            TextTab(R.drawable.ic_opacity, R.string.opacity) { onSub(TextSubTool.OPACITY) }
            TextTab(R.drawable.ic_shadow, R.string.shadow) { onSub(TextSubTool.SHADOW) }
        }
    }
}

@Composable
private fun GradientDot(color: Int) {
    Image(painterResource(R.drawable.ic_color_bg), null, Modifier.padding(5.sdp).size(20.sdp), colorFilter = ColorFilter.tint(Color(color)))
}

@Composable
private fun RowScope.TextTab(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    Column(Modifier.weight(1f).borderlessClick(onClick).padding(vertical = 5.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(icon), null)
        Text(stringResource(label), style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.titleColor), modifier = Modifier.padding(top = 2.sdp))
    }
}

/** gradientSegmentedViewBtn: Color | Gradient, the selected half on dark_blue with onlyWhite text. */
@Composable
private fun Segmented(gradient: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(8.sdp)
    Row(Modifier.padding(bottom = 2.sdp).width(105.sdp).height(22.sdp).clip(shape).background(colorResource(R.color.transparent_color))) {
        listOf(false to R.string.color, true to R.string.gradient).forEach { (isGradient, label) ->
            val on = isGradient == gradient
            Box(
                Modifier
                    .weight(1f)
                    .height(22.sdp)
                    .then(if (on) Modifier.background(colorResource(R.color.dark_blue), shape) else Modifier)
                    .clickable { onChange(isGradient) },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(label), fontFamily = Inter, fontSize = 12.sp, color = colorResource(if (on) R.color.onlyWhite else R.color.black))
            }
        }
    }
}
