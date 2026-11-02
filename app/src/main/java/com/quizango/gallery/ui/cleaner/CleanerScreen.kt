package com.quizango.gallery.ui.cleaner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.screenBackground

private val ScanNowRed = Color(0xFFE11A1A)

/**
 * SystemCleaner Screen (activity_system_cleaner.xml / SystemCleanerActivity)
 * Features:
 * 1. Top bar: System Cleaner with Back button
 * 2. Remove Junk card with "Scan Now" button -> launches Storage Clean (RemoveJunkScreen)
 * 3. Categories list (systemCleanerRecycler):
 *    - Duplicate Images -> DuplicateWaitingScreen
 *    - Screenshots -> ScreenShortScreen
 *    - Large Video -> LargeVideoScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToLargeVideos: () -> Unit = {},
    onNavigateToScreenshots: () -> Unit = {},
    onNavigateToRemoveJunk: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_bar_system_cleaner),
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.screenBackground)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Remove Junk Hero Card (item_remove_junk in activity_system_cleaner.xml)
            val cardShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.cardBackground)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.cardBorder, shape = cardShape)
                    .clickable(onClick = onNavigateToRemoveJunk)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_remove_junk_list),
                    contentDescription = "Remove Junk",
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.remove_junk),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.txt_cleaner_sub),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .background(ScanNowRed)
                        .clickable(onClick = onNavigateToRemoveJunk)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.explore_scan_now),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // 2. Categories List (systemCleanerRecycler in activity_system_cleaner.xml)
            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_duplicate,
                title = "Duplicate Images",
                subtitle = "Find and remove identical photos.",
                onClick = onNavigateToDuplicates
            )

            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_screenshot,
                title = "Screenshots",
                subtitle = "Manage and delete unwanted screenshots.",
                onClick = onNavigateToScreenshots
            )

            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_large_video,
                title = "Large Video",
                subtitle = "Free up storage space for more memories.",
                onClick = onNavigateToLargeVideos
            )
        }
    }
}

@Composable
private fun CleanerCategoryCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.cardBackground)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.cardBorder, shape = shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.system_cleaner_banner_arrow),
            contentDescription = null,
            tint = if (MaterialTheme.colorScheme.isDark) Color.White else Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}
