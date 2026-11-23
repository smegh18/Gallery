package com.quizango.gallery.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.sdp

/** activity_duplicate_delete_complete: every displayed total comes from successful process outcomes. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DuplicateCompleteScreen(
    freedFilesCount: Int,
    freedSizeBytes: Long,
    onDoneClick: () -> Unit
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.dm_success))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
    val removedCopy = context.resources.getQuantityString(R.plurals.dup_delete_files_removed, freedFilesCount, freedFilesCount)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dup_delete_complete_title),
                            style = GalleryText.BodyLargeSemiBold,
                            color = colorResource(R.color.titleColor)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDoneClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.viewer_back),
                                tint = colorResource(R.color.titleColor),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.backgroundColor))
                )
                HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.actionbarDividerColor))
            }
        },
        containerColor = colorResource(R.color.backgroundColor)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.sdp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(5.sdp))
            LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.height(100.sdp))
            Spacer(Modifier.height(18.sdp))
            Text(
                text = stringResource(R.string.task_completed),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(7.sdp))
            Text(
                text = stringResource(R.string.your_device_is_ready_to_use),
                style = GalleryText.BodyMediumRegular,
                color = colorResource(R.color.subtitleColor),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(15.sdp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.sdp),
                color = colorResource(R.color.backgroundColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorResource(R.color.seekbar_color_bg))
            ) {
                Column(Modifier.padding(14.sdp)) {
                    Text(
                        text = removedCopy,
                        style = GalleryText.BodyMediumSemiBold,
                        color = colorResource(R.color.titleColor)
                    )
                    Spacer(Modifier.height(12.sdp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.sdp))
                            .background(MaterialTheme.colorScheme.cardBackground)
                            .border(1.5.dp, colorResource(R.color.colorPrimary), RoundedCornerShape(8.sdp))
                            .padding(horizontal = 14.sdp, vertical = 12.sdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.img_system_cleaner_duplicate),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.height(35.sdp)
                        )
                        Spacer(Modifier.width(12.sdp))
                        Column {
                            Text(
                                text = stringResource(R.string.dup_delete_storage_freed),
                                style = GalleryText.BodyMediumSemiBold,
                                color = colorResource(R.color.titleColor)
                            )
                            Spacer(Modifier.height(5.sdp))
                            Text(
                                text = stringResource(R.string.dup_delete_space_freed, formatCleanerSize(freedSizeBytes)),
                                style = GalleryText.BodyMediumSemiBold,
                                color = colorResource(R.color.colorPrimary)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.sdp))
        }
    }
}
