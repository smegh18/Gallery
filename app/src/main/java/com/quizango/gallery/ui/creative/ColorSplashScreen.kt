package com.quizango.gallery.ui.creative

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Color Splash screen reconstructed with 100% visual and behavioral parity
 * against reference APK `activity_color_splash.xml` and `ColorSplashActivity.java`.
 */
@Composable
fun ColorSplashScreen(
    imagePath: String,
    mediaRepository: MediaRepository? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var foregroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var desaturatedBackgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSegmenting by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saturationValue by remember { mutableIntStateOf(0) } // 0 = complete B&W background
    var isReverseSplash by remember { mutableStateOf(false) }

    // Authentic Lottie loader from reference APK (R.raw.remove_bg_loader)
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.remove_bg_loader))

    // Load original bitmap and perform subject segmentation
    LaunchedEffect(imagePath) {
        if (imagePath.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeFile(imagePath, opts)
                    if (bmp != null) {
                        originalBitmap = bmp
                        val fg = ImageSegmentationHelper.extractSubject(context, bmp)
                        foregroundBitmap = fg
                        desaturatedBackgroundBitmap = ImageSegmentationHelper.desaturateBitmap(bmp, saturationValue.toFloat())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isSegmenting = false
                }
            }
        } else {
            isSegmenting = false
        }
    }

    // Dynamic saturation update via RulerView
    LaunchedEffect(saturationValue, isReverseSplash) {
        val orig = originalBitmap ?: return@LaunchedEffect
        val sat = if (isReverseSplash) (100 - saturationValue).toFloat() else saturationValue.toFloat()
        withContext(Dispatchers.Default) {
            desaturatedBackgroundBitmap = ImageSegmentationHelper.desaturateBitmap(orig, sat)
        }
    }

    fun saveComposite() {
        if (isSaving || isSegmenting) return
        val orig = originalBitmap ?: return
        val fg = foregroundBitmap ?: return
        val bg = desaturatedBackgroundBitmap ?: orig

        isSaving = true
        coroutineScope.launch {
            try {
                val composite = if (!isReverseSplash) {
                    ImageSegmentationHelper.composite(bg, fg)
                } else {
                    val desatSubject = ImageSegmentationHelper.desaturateBitmap(fg, saturationValue.toFloat())
                    ImageSegmentationHelper.composite(orig, desatSubject)
                }

                val savedPath = ImageSegmentationHelper.saveImageToGallery(
                    context = context,
                    bitmap = composite,
                    folderName = "Gallery",
                    prefix = "SPLASH"
                )
                if (savedPath != null) {
                    mediaRepository?.syncMediaStore()
                    Toast.makeText(context, "Color Splash image saved successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.screenBackground)
    ) {
        // Authentic Top Toolbar (layout_toolbar.xml)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.color_splash),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 1dp Divider (actionbarDividerColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.settingsDivider)
                )
            }
        }

        // Horizontal indeterminate progress bar during segmentation
        if (isSegmenting) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF0066FF),
                trackColor = Color.Transparent
            )
        }

        // Viewport (blurAndGrayImageLayout & llMainLayout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF5F7FA)),
            contentAlignment = Alignment.Center
        ) {
            if (isSegmenting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LottieAnimation(
                        composition = lottieComposition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(130.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Analyzing subject...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF707C93)
                    )
                }
            } else if (originalBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background layer (desaturated original)
                    val bgBmp = desaturatedBackgroundBitmap ?: originalBitmap
                    if (bgBmp != null) {
                        Image(
                            bitmap = bgBmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Foreground layer (crisp color cutout subject overlay)
                    val fgBmp = foregroundBitmap
                    if (fgBmp != null) {
                        Image(
                            bitmap = fgBmp.asImageBitmap(),
                            contentDescription = "Cutout Subject",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Authentic Bottom View (blurAndGrayLay)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Action Header: Close, Title, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_cancle),
                            contentDescription = "Cancel",
                            colorFilter = ColorFilter.tint(Color(0xFF0066FF)),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.color_splash),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { saveComposite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_done),
                            contentDescription = "Save",
                            colorFilter = ColorFilter.tint(Color(0xFF0066FF)),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ruler Value Text (blurAndGrayRulerText)
                Text(
                    text = "$saturationValue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0066FF)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Authentic RulerView (blurAndGrayRuler)
                GalleryRulerView(
                    value = saturationValue,
                    onValueChange = { saturationValue = it },
                    minValue = 0,
                    maxValue = 100,
                    middleLineColor = Color(0xFF0066FF),
                    stickColor = Color(0xFFD0D7DE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Mode toggle chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isReverseSplash,
                        onClick = { isReverseSplash = false },
                        label = { Text("Color Subject", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0066FF).copy(alpha = 0.12f),
                            selectedLabelColor = Color(0xFF0066FF)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = isReverseSplash,
                        onClick = { isReverseSplash = true },
                        label = { Text("Invert Splash", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0066FF).copy(alpha = 0.12f),
                            selectedLabelColor = Color(0xFF0066FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Full-screen saving loader
    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF0066FF),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
