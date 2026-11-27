package com.quizango.gallery.ui.creative.collage

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp

/** A text placed on the collage; [x] / [y] are the centre as fractions of the collage, colours are ARGB. */
internal data class CollageText(
    val id: Long,
    val text: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val color: Int = 0xFF000000.toInt(),
    val alpha: Int = 255,
    val size: Int = 30,
    val font: Int = 0,
    val align: Int = 1, // 0 left, 1 centre, 2 right
    val case: TextCase = TextCase.AS_TYPED,
    val shadowColor: Int = 0,
    val shadowBlur: Int = 0,
    val shadowX: Int = 0,
    val shadowY: Int = 0,
    val bgColor: Int = 0,
    val bgAlpha: Int = 255,
    val bgRadius: Int = 0,
    val bgWidth: Int = 0,
    val bgHeight: Int = 0
)

/**
 * The text editor's font list. shortcut: platform families stand in for 2.7.2's 70 bundled assets/fonts files, which we
 * do not ship (third-party font licences); add licensed fonts here to widen the list.
 */
internal val COLLAGE_FONTS = listOf(Inter, FontFamily.SansSerif, FontFamily.Serif, FontFamily.Monospace, FontFamily.Cursive)

/** textTitle_array / textIcon_array (te/j): the option tabs under the input. */
private enum class TextTab(@DrawableRes val icon: Int, @StringRes val label: Int) {
    KEYBOARD(R.drawable.ic_keyboard, R.string.keyboard),
    FONT(R.drawable.ic_font_collage, R.string.font),
    SHADOW(R.drawable.ic_shadow_collage, R.string.shadow),
    COLOR(R.drawable.ic_color_picker_text, R.string.color),
    ADJUST(R.drawable.ic_adjust_collage, R.string.adjust),
    DONE(R.drawable.ic_saved_d, R.string.done)
}

/** A placed text as drawn on the collage and in the editor preview (tvText). */
@Composable
internal fun CollageTextLabel(t: CollageText, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val shadow = if (t.shadowColor != 0) {
        with(density) { Shadow(Color(t.shadowColor), Offset(t.shadowX.dp.toPx(), t.shadowY.dp.toPx()), t.shadowBlur.dp.toPx()) }
    } else null
    Text(
        text = t.case.apply(t.text),
        style = TextStyle(
            fontFamily = COLLAGE_FONTS[t.font.coerceIn(COLLAGE_FONTS.indices)],
            fontSize = t.size.coerceAtLeast(1).sp,
            color = Color(withAlpha(t.color, t.alpha)),
            textAlign = when (t.align) { 0 -> TextAlign.Start; 2 -> TextAlign.End; else -> TextAlign.Center },
            shadow = shadow
        ),
        modifier = modifier
            .background(Color(withAlpha(t.bgColor, if (t.bgColor == 0) 0 else t.bgAlpha)), RoundedCornerShape(t.bgRadius.dp))
            .padding(horizontal = t.bgWidth.dp, vertical = t.bgHeight.dp)
    )
}

/**
 * fragment_text_input (te/j "TextFragment"): the styled preview over the input, the option tabs and the 250.sdp tool area.
 * Done hands back the text; Back drops the edit. The field starts empty (te/j never reads its extra_input_text).
 */
@Composable
internal fun CollageTextEditor(initial: CollageText, onDone: (CollageText) -> Unit, onCancel: () -> Unit) {
    var t by remember { mutableStateOf(initial) }
    var tab by remember { mutableStateOf(TextTab.KEYBOARD) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val ink = colorResource(R.color.collage_header_text_color)
    BackHandler(onBack = onCancel)
    LaunchedEffect(tab) {
        if (tab == TextTab.KEYBOARD) {
            focus.requestFocus()
            keyboard?.show()
        } else keyboard?.hide()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(10.sdp), contentAlignment = Alignment.Center) {
            if (t.text.isNotEmpty()) CollageTextLabel(t, Modifier.padding(12.sdp))
        }
        Column(Modifier.fillMaxWidth().background(colorResource(R.color.picker_toolbar_color))) {
            BasicTextField(
                value = t.text,
                onValueChange = { t = t.copy(text = it) },
                textStyle = inter(13, FontWeight.W500).copy(color = ink, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colorResource(R.color.mainAppColor)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(7.sdp)
                    .height(38.sdp)
                    .drawableBackground(R.drawable.bg_text_sticker_input)
                    .padding(5.sdp)
                    .focusRequester(focus),
                decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() } }
            )
            Row(
                Modifier
                    .background(colorResource(R.color.bg_text_editor_option))
                    .horizontalScroll(rememberScrollState())
            ) {
                TextTab.entries.forEach { o ->
                    val tint = colorResource(
                        when {
                            o == TextTab.DONE -> R.color.mainAppColor
                            o == tab -> R.color.selectedTabTextColor
                            else -> R.color.disableTabTextColor
                        }
                    )
                    ToolItem(o.icon, o.label, tint, tintIcon = o != TextTab.DONE) { if (o == TextTab.DONE) onDone(t) else tab = o }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border_collage)))
            if (tab != TextTab.KEYBOARD) Box(Modifier.fillMaxWidth().height(250.sdp).padding(vertical = 4.sdp)) {
                when (tab) {
                    TextTab.FONT -> FontList(t.font) { t = t.copy(font = it) }
                    TextTab.SHADOW -> ShadowSection(t) { t = it }
                    TextTab.COLOR -> ColorSection(t) { t = it }
                    TextTab.ADJUST -> AdjustSection(t) { t = it }
                    else -> Unit
                }
            }
        }
    }
}

/** rvFont (item_font): the sample word in each family on text_sticker_input_bg, the picked one ticked. */
@Composable
private fun FontList(selected: Int, onPick: (Int) -> Unit) {
    val ink = colorResource(R.color.collage_header_text_color)
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(COLLAGE_FONTS) { i, family ->
            Box(
                Modifier
                    .padding(horizontal = 12.sdp, vertical = 4.sdp)
                    .fillMaxWidth()
                    .height(33.sdp)
                    .drawableBackground(R.drawable.bg_text_sticker_input)
                    .clickable { onPick(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.hellow_label), style = inter(10).copy(fontFamily = family), color = ink)
                if (i == selected) Image(
                    painterResource(R.drawable.ic_text_font_selected),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.sdp).size(18.sdp)
                )
            }
        }
    }
}

/** The bold "Color" heading over the decorative picker box and a swatch row. */
@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    Text(
        stringResource(R.string.color),
        style = inter(10, FontWeight.W700),
        color = colorResource(R.color.collage_header_text_color),
        modifier = Modifier.padding(start = 3.sdp, top = 5.sdp, bottom = 5.sdp)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        // shortcut: the custom colour picker behind this box is not ported, the swatches cover the reference palette
        Image(
            painterResource(R.drawable.ic_color_piker),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorResource(R.color.collage_header_text_color)),
            modifier = Modifier.width(35.sdp).height(25.sdp).drawableBackground(R.drawable.bg_color_picker).padding(4.sdp)
        )
        Box(Modifier.padding(start = 8.sdp)) { ColorSwatches(COLLAGE_BG_COLORS.drop(1), selected, null, onPick, width = 18, height = 25) }
    }
}

/** svShadow: colour, then Blur / Horizontal / Vertical offsets. */
@Composable
private fun ShadowSection(t: CollageText, onChange: (CollageText) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.sdp)) {
        ColorRow(t.shadowColor) { onChange(t.copy(shadowColor = it)) }
        SliderRow(R.string.blur, t.shadowBlur, MAX_BLUR, { onChange(t.copy(shadowBlur = it)) }, labelAlign = TextAlign.Center, valueSize = 10)
        SliderRow(R.string.horizontal, t.shadowX, 20, { onChange(t.copy(shadowX = it)) }, min = -20, labelAlign = TextAlign.Center, valueSize = 10)
        SliderRow(R.string.vertical, t.shadowY, 20, { onChange(t.copy(shadowY = it)) }, min = -20, labelAlign = TextAlign.Center, valueSize = 10)
    }
}

/** iColor: Text | Background tabs (layout_text_color_tab / layout_background_color_tab). */
@Composable
private fun ColorSection(t: CollageText, onChange: (CollageText) -> Unit) {
    var background by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 15.sdp)) {
        Row(Modifier.padding(horizontal = 30.sdp).fillMaxWidth().height(35.sdp).drawableBackground(R.drawable.bg_layout_tabs)) {
            TabCell(stringResource(R.string.text), background == 0, 20) { background = 0 }
            TabCell(stringResource(R.string.background), background == 1, 20) { background = 1 }
        }
        Column(Modifier.padding(8.sdp).padding(top = 10.sdp)) {
            if (background == 0) {
                ColorRow(t.color) { onChange(t.copy(color = it)) }
                SliderRow(R.string.opacity, t.alpha, 255, { onChange(t.copy(alpha = it)) })
            } else {
                ColorRow(t.bgColor) { onChange(t.copy(bgColor = it)) }
                SliderRow(R.string.opacity, t.bgAlpha, 255, { onChange(t.copy(bgAlpha = it)) })
                SliderRow(R.string.radius, t.bgRadius, 30, { onChange(t.copy(bgRadius = it)) })
            }
        }
    }
}

/** layout_adjust_text_section: case and alignment pills, Size and Color opacity, then Bg Width / Bg Height. */
@Composable
private fun AdjustSection(t: CollageText, onChange: (CollageText) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.sdp, vertical = 5.sdp).padding(top = 10.sdp)) {
        Row(Modifier.fillMaxWidth()) {
            IconPill(
                listOf(
                    R.drawable.ic_letter_case_lower to TextCase.LOWER,
                    R.drawable.ic_letter_case to TextCase.CAPITAL,
                    R.drawable.ic_letter_case_upper to TextCase.UPPER,
                    R.drawable.ic_letter_case_toggle to TextCase.TOGGLE
                ),
                t.case,
                Modifier.weight(1.5f).padding(end = 8.sdp)
            ) { onChange(t.copy(case = if (t.case == it) TextCase.AS_TYPED else it)) }
            IconPill(
                listOf(R.drawable.ic_align_left to 0, R.drawable.ic_align_center to 1, R.drawable.ic_align_right to 2),
                t.align,
                Modifier.weight(1f).padding(start = 8.sdp)
            ) { onChange(t.copy(align = it)) }
        }
        Box(Modifier.padding(top = 20.sdp)) { SliderRow(R.string.size, t.size, 60, { onChange(t.copy(size = it)) }, height = 40) }
        SliderRow(R.string.color, t.alpha, 255, { onChange(t.copy(alpha = it)) }, height = 40)
        Row(Modifier.fillMaxWidth().padding(top = 15.sdp)) {
            HalfSlider(R.string.bg_width, t.bgWidth, Modifier.weight(1f)) { onChange(t.copy(bgWidth = it)) }
            HalfSlider(R.string.bg_height, t.bgHeight, Modifier.weight(1f)) { onChange(t.copy(bgHeight = it)) }
        }
    }
}

/** ad_btn_bg_white pill of 28.sdp icon buttons; the active one in mainAppColor. */
@Composable
private fun <T> IconPill(items: List<Pair<Int, T>>, selected: T, modifier: Modifier, onPick: (T) -> Unit) {
    Row(
        modifier.drawableBackground(R.drawable.ad_btn_bg_white).padding(horizontal = 15.sdp, vertical = 1.sdp),
        horizontalArrangement = Arrangement.spacedBy(5.sdp)
    ) {
        items.forEach { (icon, value) ->
            Image(
                painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResource(if (value == selected) R.color.mainAppColor else R.color.collage_header_text_color)),
                modifier = Modifier.weight(1f).height(28.sdp).clip(RoundedCornerShape(4.sdp)).clickable { onPick(value) }.padding(3.sdp)
            )
        }
    }
}

@Composable
private fun HalfSlider(@StringRes label: Int, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(label), style = inter(10, FontWeight.W700), color = colorResource(R.color.collage_header_text_color))
        CollageSeekBar(value, 50, onChange, Modifier.fillMaxWidth().height(25.sdp))
    }
}
