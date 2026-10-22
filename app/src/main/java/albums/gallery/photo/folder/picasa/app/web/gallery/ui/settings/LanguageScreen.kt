package albums.gallery.photo.folder.picasa.app.web.gallery.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.datastore.AppPreferences
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val badgeLetter: String
)

private val APP_LANGUAGES = listOf(
    LanguageOption("en", "English", "English", "A"),
    LanguageOption("es", "Spanish", "Español", "Ñ"),
    LanguageOption("fr", "French", "Français", "Ç"),
    LanguageOption("de", "German", "Deutsch", "ß"),
    LanguageOption("hi", "Hindi", "हिन्दी", "अ"),
    LanguageOption("pt", "Portuguese", "Português", "P"),
    LanguageOption("ru", "Russian", "Русский", "Я"),
    LanguageOption("ar", "Arabic", "العربية", "ع"),
    LanguageOption("ja", "Japanese", "日本語", "日"),
    LanguageOption("ko", "Korean", "한국어", "한"),
    LanguageOption("zh", "Chinese", "中文", "中"),
    LanguageOption("bn", "Bangali", "Bangali", "ক"),
    LanguageOption("mr", "Marathi", "मराठी", "म"),
    LanguageOption("ta", "Tamil", "தமிழ்", "அ"),
    LanguageOption("tr", "Turkish", "Türkçe", "T"),
    LanguageOption("ur", "Urdu", "اردو", "ا"),
    LanguageOption("it", "Italian", "Italiano", "I"),
    LanguageOption("in", "Indonesian", "Indonesia", "I")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedLanguage by appPreferences.appLanguage.collectAsState(initial = "en")
    var selectedLanguage by remember { mutableStateOf("en") }

    LaunchedEffect(savedLanguage) {
        selectedLanguage = savedLanguage
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select App Language",
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
                actions = {
                    // Reference toolbarIvTick with bg_rounded_red and ic_done_bold
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(width = 54.dp, height = 30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFE11A1A))
                            .clickable {
                                scope.launch {
                                    appPreferences.setLanguage(selectedLanguage)
                                    onNavigateBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_done_bold),
                            contentDescription = "Apply Language",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                HorizontalDivider(
                    color = Color(0xFFE4E6E9),
                    thickness = 1.dp
                )
            }

            items(APP_LANGUAGES, key = { it.code }) { lang ->
                val isSelected = selectedLanguage == lang.code

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguage = lang.code }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circle Avatar with Badge Letter
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F6FC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.badgeLetter,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF373742)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3354)
                        )
                        Text(
                            text = lang.nativeName,
                            fontSize = 12.sp,
                            color = Color(0xFF707C93)
                        )
                    }

                    // Radio Checkmark: circle_checked / circle_unchecked
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.circle_checked else R.drawable.circle_unchecked
                        ),
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) GalleryBlue else Color(0xFF8C9BAE),
                        modifier = Modifier.size(22.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFF2F4F7),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
