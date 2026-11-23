package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch

/** activity_vault_setting rows in reference order: (icon, title, subtitle). Rows 2 and 3 carry a switch, 1 and 4 an arrow. */
internal val vaultSettingRows = listOf(
    Triple(R.drawable.ic_change_password, R.string.change_password, R.string.set_a_new_passcode),
    Triple(R.drawable.ic_pattern, R.string.show_pattern, R.string.display_your_unique_pattern),
    Triple(R.drawable.ic_fingerprint, R.string.unlock_with_fingerprint, R.string.use_biometrics_for_security),
    Triple(R.drawable.ic_change_recovery_question, R.string.change_recovery_question, R.string.choose_another_recovery_question)
)

@Composable
fun VaultSettingScreen(
    appPreferences: AppPreferences,
    onNavigateToChangePin: () -> Unit,
    onNavigateToRecoveryQuestion: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isPatternMode by appPreferences.isPatternMode.collectAsState(initial = true)
    val isFingerprintEnabled by appPreferences.isFingerprintEnabled.collectAsState(initial = false)

    // Only switch on when a fingerprint can actually be used
    fun toggleFingerprint(enable: Boolean) {
        val reason = if (enable) VaultBiometric.unavailableReason(context) else null
        if (reason != null) {
            Toast.makeText(context, context.getString(reason), Toast.LENGTH_SHORT).show()
        } else {
            scope.launch { appPreferences.setFingerprintEnabled(enable) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.privacy_settings), onBack = onNavigateBack)
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            vaultSettingRows.forEachIndexed { index, (icon, title, subtitle) ->
                val label = stringResource(title)
                val onClick: () -> Unit = when (index) {
                    0 -> onNavigateToChangePin
                    1 -> ({ scope.launch { appPreferences.setPatternMode(!isPatternMode) } })
                    2 -> ({ toggleFingerprint(!isFingerprintEnabled) })
                    else -> onNavigateToRecoveryQuestion
                }
                VaultSettingRow(icon, label, stringResource(subtitle), onClick) {
                    if (index == 1 || index == 2) {
                        Switch(
                            checked = if (index == 1) isPatternMode else isFingerprintEnabled,
                            onCheckedChange = { if (index == 1) scope.launch { appPreferences.setPatternMode(it) } else toggleFingerprint(it) },
                            modifier = Modifier.padding(vertical = 10.sdp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_inside),
                            contentDescription = label,
                            modifier = Modifier.padding(10.sdp)
                        )
                    }
                }
            }
        }
    }
}

/** One flat row: no card, no divider, borderless ripple. */
@Composable
private fun VaultSettingRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    val titleColor = colorResource(R.color.titleColor)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                role = Role.Button,
                onClick = onClick
            )
            .padding(10.sdp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null, // decorative: the title names the row
            modifier = Modifier.padding(6.sdp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 5.sdp)) {
            Text(text = title, style = GalleryText.BodyMediumMedium, color = titleColor)
            Text(text = subtitle, style = GalleryText.BodySmallRegular, color = titleColor)
        }
        trailing()
    }
}
