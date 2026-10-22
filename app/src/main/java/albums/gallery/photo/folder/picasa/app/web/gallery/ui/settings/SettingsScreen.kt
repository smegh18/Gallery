package albums.gallery.photo.folder.picasa.app.web.gallery.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.datastore.AppPreferences
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
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

    fun shareApp() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out Gallery app: https://play.google.com/store/apps/details?id=${context.packageName}")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Gallery"))
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E3354)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 1. Go Premium Banner (layout_settings_premium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF7E6))
                    .clickable { }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pro_new),
                        contentDescription = "Pro",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Go Premium",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD48806),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Unlock all powerful features & remove ads",
                            color = Color(0xFF8C6B23),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_drawer_right_arrow),
                        contentDescription = null,
                        tint = Color(0xFFD48806),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Settings Items matching activity_setting.xml
            SettingNavRow(
                iconRes = R.drawable.ic_theme_mode,
                title = "Theme Mode",
                onClick = { onNavigate(Screen.Theme.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_unhide,
                title = "Private Vault",
                onClick = { onNavigate(Screen.VaultLock.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_app_language,
                title = "App Languages",
                onClick = { onNavigate(Screen.Language.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_iu_control,
                title = "IU Monitoring",
                showDot = true,
                onClick = { }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_manage_subscription,
                title = "My Subscription",
                onClick = { }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_about,
                title = "About Us",
                onClick = { onNavigate(Screen.About.route) }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_revoke_consent,
                title = "Revoke Consent",
                onClick = { }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_rate,
                title = "Rate Us with 5 Stars",
                showArrow = false,
                onClick = { }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_setting_share,
                title = "Share",
                showArrow = false,
                onClick = { shareApp() }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_game_setting,
                title = "Play Games",
                showArrow = false,
                onClick = { }
            )
            SettingDivider()

            SettingNavRow(
                iconRes = R.drawable.ic_quiz_setting,
                title = "Quiz",
                showArrow = false,
                onClick = { }
            )
            SettingDivider()

            // 3. Recycle Bin Section
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Recycle Bin",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF707C93),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto delete after 30 days",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3354)
                    )
                    Text(
                        text = "Remove items that have been in the Recycle bin for more than 30 days",
                        fontSize = 12.sp,
                        color = Color(0xFF707C93)
                    )
                }
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
}

@Composable
private fun SettingNavRow(
    iconRes: Int,
    title: String,
    showArrow: Boolean = true,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE11A1A))
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3354),
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drawer_right_arrow),
                contentDescription = null,
                tint = Color(0xFF8C9BAE),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        color = Color(0xFFF2F4F7),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}
