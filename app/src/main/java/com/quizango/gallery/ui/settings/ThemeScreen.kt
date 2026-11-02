package com.quizango.gallery.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val themeMode by appPreferences.themeMode.collectAsState(initial = 2)

    val options = listOf(
        Triple(0, R.string.day_mode, R.drawable.theme_group_day_mode),
        Triple(1, R.string.night_mode, R.drawable.theme_group_night_mode),
        Triple(2, R.string.system_mode, R.drawable.theme_group_system_mode)
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.theme_mode),
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
                HorizontalDivider(thickness = 0.5.dp, color = Color(0x1A071427))
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
            Spacer(modifier = Modifier.height(8.dp))

            options.forEach { (mode, labelRes, iconRes) ->
                val isSelected = themeMode == mode
                val label = stringResource(id = labelRes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { appPreferences.setThemeMode(mode) }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF010101),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.ic_theme_select else R.drawable.ic_theme_unselect
                        ),
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFF2F9FF),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }
}
