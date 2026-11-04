package com.quizango.gallery.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import kotlinx.coroutines.launch

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val badgeLetter: String
)

private val APP_LANGUAGES = listOf(
    LanguageOption("en", "English", "English", "A"),
    LanguageOption("ar", "Arabic", "العربية", "ع"),
    LanguageOption("bn", "Bangali", "Bangali", "ক"),
    LanguageOption("de", "German", "Deutsch", "ß"),
    LanguageOption("es", "Spanish", "Español", "Ñ"),
    LanguageOption("fa", "Persian", "فارسی", "Ơ"),
    LanguageOption("fil", "Filipino", "Filipino", "A"),
    LanguageOption("fr", "French", "Français", "É"),
    LanguageOption("gu", "Gujarati", "ગુજરાતી", "અ"),
    LanguageOption("hi", "Hindi", "हिन्दी", "अ"),
    LanguageOption("in", "Indonesian", "Bahasa Indonesia", "K"),
    LanguageOption("it", "Italian", "Italiano", "È"),
    LanguageOption("ja", "Japanese", "日本語", "あ"),
    LanguageOption("ko", "Korean", "한국어", "ㄱ"),
    LanguageOption("mr", "Marathi", "मराठी", "अ"),
    LanguageOption("ms", "Malay", "Bahasa Melayu", "U"),
    LanguageOption("my", "Burmese", "ဗမာစာ", "က"),
    LanguageOption("pl", "Polish", "Polski", "A"),
    LanguageOption("pt", "Portuguese", "Português", "Ç"),
    LanguageOption("ru", "Russian", "Русский", "А"),
    LanguageOption("sv", "Swedish", "Svenska", "A"),
    LanguageOption("ta", "Tamil", "தமிழ்", "அ"),
    LanguageOption("tr", "Turkish", "Turkish", "A"),
    LanguageOption("ur", "Urdu", "اردو", "ا"),
    LanguageOption("zh", "Chinese", "Chinese", "一个")
)

private val AvatarPastelColors = listOf(
    Color(0xFFFFF8FB),
    Color(0xFFFFF8FB),
    Color(0xFFFFFFF5),
    Color(0xFFFAFFFD),
    Color(0xFFF8F8FF),
    Color(0xFFFFFFF5),
    Color(0xFFF8F8FF),
    Color(0xFFF9FCFF)
)

private val ItemCardBg = Color(0xFFF2F6FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    appPreferences: AppPreferences,
    canNavigateBack: Boolean = true,
    showHandAnimation: Boolean = !canNavigateBack,
    onApply: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val savedLanguage by appPreferences.appLanguage.collectAsState(initial = "en")
    var selectedLanguage by remember { mutableStateOf("en") }

    LaunchedEffect(savedLanguage) {
        selectedLanguage = savedLanguage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Toolbar (matching ConstraintLayout @id/toolbar in activity_app_language.xml)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canNavigateBack) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = stringResource(id = R.string.select_app_language),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Confirm Tick Button (toolbarIvTick: width @dimen/_50sdp ~65dp, height @dimen/_28sdp ~36dp)
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(width = 65.dp, height = 36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(com.quizango.gallery.ui.theme.GalleryPrimaryBlue) // reference confirm button: colorPrimary #3478F6
                            .clickable {
                                scope.launch {
                                    appPreferences.setLanguage(selectedLanguage)
                                    onApply()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_done_bold),
                            contentDescription = "Apply Language",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = if (MaterialTheme.colorScheme.isDark) Color(0x20FFFFFF) else Color(0xFFE4E6E9)
            )

            // Language List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                itemsIndexed(APP_LANGUAGES, key = { _, lang -> lang.code }) { index, lang ->
                    val isSelected = selectedLanguage == lang.code
                    val isNight = MaterialTheme.colorScheme.isDark
                    // Light mode (reference): plain white rows, dark text and radios, no selected highlight
                    val cardBg = if (isNight) Color(0xFF35373B) else Color.Transparent
                    val cardBorder = if (isSelected && isNight) BorderStroke(1.5.dp, GalleryBlue) else null
                    val lightInk = Color(0xFF222933)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = if (isNight) 5.dp else 0.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { selectedLanguage = lang.code },
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = if (isNight) 8.dp else 16.dp), // reference 75dp rows
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circle Avatar with Badge Letter
                            val avatarBg = if (isNight) Color(0xFF26282B) else AvatarPastelColors[index % AvatarPastelColors.size]
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.badgeLetter,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isNight) Color.White else Color(0xFF373742)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Language Name & Native Name (e.g., "English (English)")
                            val textAnnotated = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = if (!isNight) lightInk else if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onBackground,
                                        fontSize = 15.sp,
                                        fontWeight = if (!isNight) FontWeight.Normal else if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                ) {
                                    append(lang.name)
                                    append(" ")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = if (!isNight) Color(0xFF7E7E7E) else if (isSelected) GalleryBlue.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("(${lang.nativeName})")
                                }
                            }

                            Text(
                                text = textAnnotated,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Radio Indicator: circle_checked vs circle_unchecked
                            Icon(
                                painter = painterResource(
                                    id = if (isSelected) R.drawable.circle_checked else R.drawable.circle_unchecked
                                ),
                                contentDescription = if (isSelected) "Selected" else "Unselected",
                                tint = if (!isNight) lightInk else if (isSelected) GalleryBlue else Color(0xFF8E8E93),
                                modifier = Modifier.size(if (isNight) 20.dp else 24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Lottie animated hand (lavFirstTime in activity_app_language.xml)
        if (showHandAnimation) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hand))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            val handWidth = 72.dp
            val handHeight = 120.dp
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 6.dp)
                    .offset(y = 56.dp - (handHeight / 2))
                    .width(handWidth)
                    .height(handHeight)
                    .padding(10.dp)
                    .clickable {
                        scope.launch {
                            appPreferences.setLanguage(selectedLanguage)
                            onApply()
                        }
                    }
            )
        }
    }
}
