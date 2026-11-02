package com.quizango.gallery.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch

private val TextSubtitleColor = Color(0xFF373742)
private val TextDisclaimerColor = Color(0xFF7A8389)

@Composable
fun WelcomeScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Title: "Hello from Gallery!"
        Text(
            text = stringResource(id = R.string.hello_from_gallery),
            color = GalleryBlue,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle: "Find, lock, and share your photos easily"
        Text(
            text = stringResource(id = R.string.find_lock_and_share_your_photos_easily),
            color = TextSubtitleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Center Hero Graphic: ic_welcom (smartphone illustration)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_welcom),
                contentDescription = stringResource(id = R.string.hello_from_gallery),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Agree & Continue CTA Button (matching @dimen/_10sdp marginHorizontal = 12.5dp)
        Button(
            onClick = {
                scope.launch {
                    appPreferences.setCompletedWelcome(true)
                    onNavigate(Screen.Language.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
        ) {
            Text(
                text = stringResource(id = R.string.agree_continue),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Footer Disclaimer: By tapping agree & continue, you indicate that you have read our Privacy Policy.
        val prefix = stringResource(id = R.string.terms_prefix) + " "
        val privacyText = stringResource(id = R.string.privacy_policy)

        val annotatedString = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = TextDisclaimerColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            ) {
                append(prefix)
            }
            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy_policy")
            withStyle(
                style = SpanStyle(
                    color = GalleryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(privacyText)
            }
            pop()
        }

        @Suppress("DEPRECATION")
        ClickableText(
            text = annotatedString,
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                    .firstOrNull()?.let {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    }
            }
        )
    }
}
