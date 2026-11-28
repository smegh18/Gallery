package com.quizango.gallery.ui.floating

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quizango.gallery.R
import com.quizango.gallery.data.ads.AdManager
import com.quizango.gallery.ui.gs.SettingThemeBars
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

/**
 * FloatingSettingActivity (activity_floating_setting): Always ON asks for the overlay permission and turns the button
 * on; Always OFF (a rewarded-ad step in 2.7.2, a no-op placeholder here) turns it off. Re-read on every resume.
 */
@Composable
fun FloatingSettingScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    if (!FloatingButtons.available()) {
        LaunchedEffect(Unit) { onNavigateBack() } // 2.7.2 closes the screen where the feature does not exist
        return
    }
    var on by remember { mutableStateOf(FloatingButtons.isOn(context)) }
    LifecycleResumeEffect(Unit) {
        on = FloatingButtons.isOn(context)
        onPauseOrDispose { }
    }
    val enable = rememberOverlayPermissionFlow(onGranted = { on = true }, onDenied = { on = FloatingButtons.isOn(context) })
    val titleColor = colorResource(R.color.titleColor)
    SettingThemeBars()
    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.white))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().height(50.sdp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_back),
                stringResource(R.string.viewer_back),
                Modifier
                    .padding(start = 10.sdp)
                    .fillMaxHeight()
                    .clickable(remember { MutableInteractionSource() }, ripple(bounded = false)) { onNavigateBack() }
                    .padding(start = 10.sdp, top = 3.sdp, end = 3.sdp, bottom = 3.sdp),
                colorFilter = ColorFilter.tint(titleColor)
            )
            Text(
                stringResource(R.string.quick_access_settings),
                style = GalleryText.BodyLargeMedium.copy(fontSize = 12.ssp),
                color = titleColor,
                modifier = Modifier.weight(1f).padding(start = 5.sdp).padding(horizontal = 10.sdp)
            )
        }
        SettingCard(R.string.always_on, R.string.at_always_on_description, selected = on, reward = false) {
            if (!on) enable()
        }
        SettingCard(R.string.always_off, R.string.at_always_off_description, selected = !on, reward = true) {
            if (!FloatingButtons.prefs(context).getBoolean(FloatingButtons.KEY_ENABLED, false)) on = false
            else AdManager.showRewarded(context as? Activity, onRewardEarned = {
                FloatingButtons.disable(context)
                on = false
            }, onDismissOrSkipped = {})
        }
    }
}

/** A stroked MaterialCardView row: title (Always OFF carries the reward icon), description and a radio at the end. */
@Composable
private fun SettingCard(title: Int, description: Int, selected: Boolean, reward: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.sdp)
    Row(
        Modifier
            .padding(start = 10.sdp, top = 10.sdp, end = 10.sdp)
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(R.color.iu_bg)) // MaterialCardView default fill ?colorSurface: white / #121212
            .border(BorderStroke(1.sdp, colorResource(R.color.install_setting_card_stroke)), shape)
            .clickable(onClick = onClick)
            .padding(start = 10.sdp, top = 12.sdp, bottom = 12.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 5.sdp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(title), style = GalleryText.BodyLargeMedium, color = colorResource(R.color.titleColor))
                if (reward) Image(painterResource(R.drawable.at_reward_icon), null, Modifier.padding(start = 6.sdp))
            }
            Text(
                stringResource(description),
                style = GalleryText.BodySmallMedium,
                color = colorResource(R.color.iu_setting_sub_text),
                modifier = Modifier.padding(top = 3.sdp)
            )
        }
        // BlueRadioButton: colorPrimary when checked, colorOnSurface (@color/black) at 54 % otherwise; not clickable itself.
        // MaterialRadioButton draws its 32dp drawable at the start of its 48dp box: circle centre 16dp in from the box start.
        Box(Modifier.size(48.dp), contentAlignment = Alignment.CenterStart) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorResource(R.color.colorPrimary),
                    unselectedColor = colorResource(R.color.black).copy(alpha = 0.54f)
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
