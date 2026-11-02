package com.quizango.gallery.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingScreen(
    appPreferences: AppPreferences,
    onNavigateToChangePin: () -> Unit,
    onNavigateToRecoveryQuestion: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isPatternMode by appPreferences.isPatternMode.collectAsState(initial = false)
    val isFingerprintEnabled by appPreferences.isFingerprintEnabled.collectAsState(initial = false)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Vault Settings",
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
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Change Password (matching activity_vault_setting.xml)
            VaultSettingRow(
                iconRes = R.drawable.ic_change_password,
                title = "Change Password",
                subtitle = "Set a new passcode",
                onClick = onNavigateToChangePin,
                trailingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_inside),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            // 2. Show Pattern (matching activity_vault_setting.xml)
            VaultSettingRow(
                iconRes = R.drawable.ic_pattern,
                title = "Show Pattern",
                subtitle = "Display your unique pattern",
                onClick = {
                    scope.launch { appPreferences.setPatternMode(!isPatternMode) }
                },
                trailingContent = {
                    Switch(
                        checked = isPatternMode,
                        onCheckedChange = { isChecked ->
                            scope.launch { appPreferences.setPatternMode(isChecked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GalleryBlue
                        )
                    )
                }
            )

            // 3. Unlock with Fingerprint (matching activity_vault_setting.xml)
            VaultSettingRow(
                iconRes = R.drawable.ic_fingerprint,
                title = "Unlock with Fingerprint",
                subtitle = "Use biometrics for security",
                onClick = {
                    scope.launch { appPreferences.setFingerprintEnabled(!isFingerprintEnabled) }
                },
                trailingContent = {
                    Switch(
                        checked = isFingerprintEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { appPreferences.setFingerprintEnabled(isChecked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GalleryBlue
                        )
                    )
                }
            )

            // 4. Change Recovery Question (matching activity_vault_setting.xml)
            VaultSettingRow(
                iconRes = R.drawable.ic_change_recovery_question,
                title = "Change Recovery Question",
                subtitle = "Choose another recovery question",
                onClick = onNavigateToRecoveryQuestion,
                trailingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_inside),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun VaultSettingRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (MaterialTheme.colorScheme.isDark) Color(0xFF2A2A2A) else Color(0xFFF2F4F7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailingContent()
        }
    }
}
