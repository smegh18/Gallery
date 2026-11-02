package com.quizango.gallery.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                            text = stringResource(id = R.string.about_us),
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
                                tint = if (MaterialTheme.colorScheme.isDark) Color.White else Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.settingsDivider)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.about_gallery_logo),
                contentDescription = "Gallery Logo",
                modifier = Modifier.size(75.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name
            Text(
                text = "Gallery",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = GalleryBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Current Version
            Text(
                text = stringResource(id = R.string.current_version, "2.6.7", 146),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // About Detail Description
            Text(
                text = stringResource(id = R.string.about_detail),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 35.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Privacy Policy Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 45.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GalleryBlue)
                    .clickable {
                        openUrl("https://appspecials.blogspot.com/2023/08/apps-specials-privacy-policy.html")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_policy),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Terms & Conditions Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 45.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GalleryBlue)
                    .clickable {
                        openUrl("https://appspecials.blogspot.com/2023/08/apps-specials-terms-conditions.html")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.terms_conditions),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Follow Us Section
            Text(
                text = stringResource(id = R.string.follow_us_on),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.settingsDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 60.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Social Icons Row 1: Instagram, YouTube, Facebook
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialMediaItem(
                    iconRes = R.drawable.ic_about_insta,
                    label = stringResource(id = R.string.instagram),
                    onClick = { openUrl("https://www.instagram.com/photogalleryvault/") }
                )
                Spacer(modifier = Modifier.width(20.dp))
                SocialMediaItem(
                    iconRes = R.drawable.ic_about_youttube,
                    label = stringResource(id = R.string.youtube),
                    onClick = { openUrl("https://www.youtube.com/@appspecials4546") }
                )
                Spacer(modifier = Modifier.width(20.dp))
                SocialMediaItem(
                    iconRes = R.drawable.ic_about_facebook,
                    label = stringResource(id = R.string.facebook),
                    onClick = { openUrl("https://www.facebook.com/photogallerybyappspecials") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Social Icons Row 2: Pinterest, TikTok
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialMediaItem(
                    iconRes = R.drawable.ic_about_pinterest,
                    label = stringResource(id = R.string.pinterest),
                    onClick = { openUrl("https://in.pinterest.com/appphotogallery/") }
                )
                Spacer(modifier = Modifier.width(20.dp))
                SocialMediaItem(
                    iconRes = R.drawable.ic_about_tiktok,
                    label = stringResource(id = R.string.tiktok),
                    onClick = { openUrl("https://www.tiktok.com/@photogalleryapp?lang=en") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SocialMediaItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
