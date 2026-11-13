package com.quizango.gallery.ui.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

/**
 * Bottom-sheet style dialog matching reference 2.7.2 `dialog_permission_needed.xml`.
 */
@Composable
fun PermissionNeededDialog(
    onDismissRequest: () -> Unit,
    onGrantAccess: () -> Unit
) {
    val isNight = MaterialTheme.colorScheme.isDark

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.BottomCenter
        ) {
            val bgModifier = if (isNight) {
                Modifier.background(
                    color = colorResource(R.color.dialogBack),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                )
            } else {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFC4DCFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
                    ),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Consume clicks inside
                    .then(bgModifier)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .padding(20.sdp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration (ic_permission_needed 125dip x 120dip)
                Image(
                    painter = painterResource(id = R.drawable.ic_permission_needed),
                    contentDescription = null,
                    modifier = Modifier.size(width = 125.dp, height = 120.dp)
                )

                // Title
                Text(
                    text = stringResource(id = R.string.why_this_permission_needed),
                    style = GalleryText.BodyLargeSemiBold,
                    fontSize = 13.ssp,
                    color = colorResource(R.color.patternColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.sdp)
                )

                // Description
                Text(
                    text = stringResource(id = R.string.call_permission_need),
                    style = GalleryText.BodySmallMedium,
                    fontSize = 10.ssp,
                    color = colorResource(R.color.radioButtonUncheckedColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )

                // Steps 1 to 4
                Text(
                    text = stringResource(id = R.string.open_setting_dialog),
                    style = GalleryText.BodyMediumMedium,
                    fontSize = 9.ssp,
                    color = colorResource(R.color.radioButtonUncheckedColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                Text(
                    text = stringResource(id = R.string.tap_permission_dialog),
                    style = GalleryText.BodyMediumMedium,
                    fontSize = 9.ssp,
                    color = colorResource(R.color.radioButtonUncheckedColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.sdp)
                )
                Text(
                    text = stringResource(id = R.string.select_phone_call_dialog),
                    style = GalleryText.BodyMediumMedium,
                    fontSize = 9.ssp,
                    color = colorResource(R.color.radioButtonUncheckedColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.sdp)
                )
                Text(
                    text = stringResource(id = R.string.tap_allow_dialog),
                    style = GalleryText.BodyMediumMedium,
                    fontSize = 9.ssp,
                    color = colorResource(R.color.radioButtonUncheckedColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.sdp)
                )

                // Grant access CTA button
                Button(
                    onClick = {
                        onDismissRequest()
                        onGrantAccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(48.sdp),
                    shape = RoundedCornerShape(25.sdp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.colorPrimary)
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.grant_access),
                        style = GalleryText.BodyLargeSemiBold,
                        fontSize = 14.ssp,
                        color = colorResource(R.color.onlyWhite)
                    )
                }
            }
        }
    }
}
