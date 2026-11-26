package com.quizango.gallery.ui.gs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.service.PackageMonitorService
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.launch

/** activity_gs_setting: "After Call Feature", Always ON / Always OFF for the Quick Info card (afterCallScreenEnabled). */
@Composable
fun GsSettingScreen(appPreferences: AppPreferences, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val enabled by appPreferences.afterCallScreenEnabled.collectAsState(initial = true)
    OnOffSettingPage(
        title = R.string.gs_feature, titleSize = 14.ssp,
        subtitle = R.string.call_sett_desc, subtitleMedium = true, subtitlePadding = 15,
        onSubtitle = R.string.show_gs, offSubtitle = R.string.disable_call_info,
        checkedTint = R.color.colorPrimary, on = enabled,
        onChange = { scope.launch { appPreferences.setAfterCallScreenEnabled(it) } },
        onNavigateBack = onNavigateBack
    )
}

/** activity_iu_setting: "Install/Uninstall Monitoring". Off also stops the monitor service, as 2.7.2's cards do. */
@Composable
fun IuSettingScreen(appPreferences: AppPreferences, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val on by appPreferences.iuAlwaysOn.collectAsState(initial = true)
    OnOffSettingPage(
        title = R.string.iu_monitoring, titleSize = 12.ssp,
        subtitle = R.string.choose_when_the_optimizing_screen_should_appear_after_installing_uninstalling_an_app,
        subtitleMedium = false, subtitlePadding = 20,
        onSubtitle = R.string.always_on_subtitle, offSubtitle = R.string.always_off_subtitle,
        checkedTint = R.color.titleColor, on = on,
        onChange = { value ->
            scope.launch {
                appPreferences.setIuAlwaysOn(value)
                if (value) PackageMonitorService.startIfAllowed(context) else PackageMonitorService.stop(context)
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun OnOffSettingPage(
    @StringRes title: Int,
    titleSize: TextUnit,
    @StringRes subtitle: Int,
    subtitleMedium: Boolean,
    subtitlePadding: Int,
    @StringRes onSubtitle: Int,
    @StringRes offSubtitle: Int,
    checkedTint: Int,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val black = colorResource(R.color.black_color)
    val sub = colorResource(R.color.iu_setting_sub_text)
    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.white))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(50.sdp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.viewer_back),
                colorFilter = ColorFilter.tint(black),
                modifier = Modifier
                    .padding(start = 10.sdp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onNavigateBack
                    )
                    .padding(start = 10.sdp, top = 3.sdp, end = 3.sdp, bottom = 3.sdp)
            )
            Text(
                stringResource(title), style = GalleryText.BodyLargeMedium, fontSize = titleSize, color = black,
                modifier = Modifier.weight(1f).padding(start = 5.sdp).padding(horizontal = 10.sdp)
            )
        }
        Text(
            stringResource(subtitle),
            style = if (subtitleMedium) GalleryText.BodyMediumMedium else GalleryText.BodyMediumRegular,
            fontSize = if (subtitleMedium) 10.ssp else TextUnit.Unspecified,
            color = sub, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = subtitlePadding.sdp)
        )
        OnOffCard(R.string.always_on, onSubtitle, on, checkedTint, Modifier.padding(top = 20.sdp)) { onChange(true) }
        OnOffCard(R.string.always_off, offSubtitle, !on, checkedTint, Modifier.padding(top = 10.sdp)) { onChange(false) }
    }
}

@Composable
private fun OnOffCard(@StringRes title: Int, @StringRes subtitle: Int, selected: Boolean, checkedTint: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.sdp)
    Row(
        modifier
            .padding(horizontal = 10.sdp)
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(1.sdp, colorResource(R.color.install_setting_card_stroke)), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 10.sdp, top = 12.sdp, bottom = 12.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 5.sdp)) {
            Text(stringResource(title), style = GalleryText.BodyLargeMedium, color = colorResource(R.color.black_color))
            Text(
                stringResource(subtitle), style = GalleryText.BodySmallMedium, color = colorResource(R.color.iu_setting_sub_text),
                modifier = Modifier.padding(top = 3.sdp)
            )
        }
        // MaterialRadioButton's own inset keeps the circle off the card edge (paddingEnd 0 in the layout)
        RadioButton(
            selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(checkedTint),
                unselectedColor = colorResource(R.color.iu_setting_sub_text)
            ),
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}
