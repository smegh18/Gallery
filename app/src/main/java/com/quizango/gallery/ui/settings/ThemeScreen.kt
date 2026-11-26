package com.quizango.gallery.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.applyNightMode
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch

/** activity_theme rows in order: (theme mode, label, picture). */
private val THEME_ROWS = listOf(
    Triple(0, R.string.day_mode, R.drawable.theme_group_day_mode),
    Triple(1, R.string.night_mode, R.drawable.theme_group_night_mode),
    Triple(2, R.string.system_mode, R.drawable.theme_group_system_mode)
)

/** activity_theme (ThemeActivity): picking another mode saves it and applies it at once; the same one does nothing. */
@Composable
fun ThemeScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val themeMode by appPreferences.themeMode.collectAsState(initial = 2)

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.theme_mode), onBack = onNavigateBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 5.sdp)
                .navigationBarsPadding()
        ) {
            THEME_ROWS.forEachIndexed { index, (mode, label, picture) ->
                val selected = themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.sdp, end = 12.sdp, top = if (index == 0) 8.sdp else 7.sdp)
                        .semantics { this.selected = selected }
                        .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), role = Role.RadioButton) {
                            if (!selected) scope.launch {
                                appPreferences.setThemeMode(mode)
                                applyNightMode(mode)
                            }
                        }
                        .padding(vertical = 5.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(picture), contentDescription = null, modifier = Modifier.padding(horizontal = 5.sdp))
                    Text(
                        text = stringResource(label),
                        style = GalleryText.BodyMediumMedium,
                        color = colorResource(R.color.theme_text),
                        modifier = Modifier.weight(1f).padding(start = 10.sdp)
                    )
                    Image(
                        painter = painterResource(if (selected) R.drawable.ic_theme_select else R.drawable.ic_theme_unselect),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 5.sdp)
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 10.sdp, end = 10.sdp, top = 2.sdp)
                        .height(1.dp)
                        .background(colorResource(R.color.settings_divider))
                )
            }
        }
    }
}
