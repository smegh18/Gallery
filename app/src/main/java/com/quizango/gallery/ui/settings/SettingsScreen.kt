package com.quizango.gallery.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val autoDeleteTrash by appPreferences.autoDeleteTrash.collectAsState(initial = true)
    var showRateUsDialog by remember { mutableStateOf(false) }

    fun shareApp() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "\nLet me recommend you this application\n\nhttps://play.google.com/store/apps/details?id=${context.packageName}\n"
                )
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share"))
        } catch (_: Exception) {
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.settings),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 1. Go Ads Free Banner (layout_settings_premium.xml)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFC78E00), Color(0xFFE4A324))
                        )
                    )
                    .border(1.dp, Color(0xFF873900), RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x90FFFFFF))
                        .clickable {
                            openUrl("https://play.google.com/store/account/subscriptions")
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.premium),
                        contentDescription = "Premium",
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.go_ads_free),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF873900),
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.unlock_everything_amp_enjoy_the_ultimate_ad_free_experience),
                            color = Color(0xFF873900),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Navigation List Items in exact reference order (activity_setting.xml)
            SettingNavRow(
                iconRes = R.drawable.ic_theme_mode,
                title = stringResource(id = R.string.theme_mode),
                showArrow = true,
                onClick = { onNavigate(Screen.Theme.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_unhide,
                title = stringResource(id = R.string.private_vault),
                showArrow = true,
                onClick = { onNavigate(Screen.VaultLock.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_app_language,
                title = stringResource(id = R.string.app_languages),
                showArrow = true,
                onClick = { onNavigate(Screen.Language.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_manage_subscription,
                title = stringResource(id = R.string.my_subscription),
                showArrow = true,
                onClick = { openUrl("https://play.google.com/store/account/subscriptions") }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_about,
                title = stringResource(id = R.string.about_us),
                showArrow = true,
                onClick = { onNavigate(Screen.About.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_rate,
                title = stringResource(id = R.string.rate_us_with_star),
                showArrow = false,
                onClick = { showRateUsDialog = true }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_setting_share,
                title = stringResource(id = R.string.share),
                showArrow = false,
                onClick = { shareApp() }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_game_setting,
                title = stringResource(id = R.string.play_games),
                showArrow = false,
                onClick = { openUrl("https://play.google.com/store/apps") }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_quiz_setting,
                title = stringResource(id = R.string.quiz),
                showArrow = false,
                onClick = { openUrl("https://play.google.com/store/apps") }
            )
            SettingDivider()

            // 3. Recycle Bin Section (activity_setting.xml)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(id = R.string.recycle_bin),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { appPreferences.setAutoDeleteTrash(!autoDeleteTrash) }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(id = R.string.delete_photos_after_days),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoDeleteTrash,
                    onCheckedChange = { checked ->
                        scope.launch { appPreferences.setAutoDeleteTrash(checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GalleryBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Rate Us Dialog (dialog_rate_us.xml)
    if (showRateUsDialog) {
        RateUsDialog(
            onDismiss = { showRateUsDialog = false },
            onSubmit = { rating ->
                showRateUsDialog = false
                openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
            }
        )
    }
}

@Composable
private fun SettingNavRow(
    iconRes: Int,
    title: String,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drawer_right_arrow),
                contentDescription = null,
                tint = if (MaterialTheme.colorScheme.isDark) Color.White else Color(0xFF8C9BAE),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.settingsDivider,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 10.dp)
    )
}

@Composable
private fun RateUsDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }

    val ratingLabels = mapOf(
        5 to stringResource(id = R.string.the_best_we_can_get),
        4 to "I like it",
        3 to "It is good",
        2 to "I don't like it",
        1 to "I hate it"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Text(
                    text = stringResource(id = R.string.thanks_for_your_support),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = stringResource(id = R.string.we_would_be_very_grateful_if_you_can_rate_us),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5 Star Rating Bar
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (i <= rating) R.drawable.ic_fill_star else R.drawable.ic_unfill_star
                                ),
                                contentDescription = "Star $i",
                                tint = if (i <= rating) Color(0xFFFFB800) else Color(0xFFCCCCCC),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rating Description text
                Text(
                    text = ratingLabels[rating] ?: "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF109142),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rate Us Button
                Button(
                    onClick = { onSubmit(rating) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.rate_us).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}


