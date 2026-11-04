package com.quizango.gallery.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.editor.crop.CropGeometry
import com.quizango.gallery.ui.editor.crop.ImageCropView
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Main editing tool tabs matching reference APK `item_edit.xml` and `PhotoEditActivity.java` (tabList).
 */
enum class EditorTab(val label: String, @DrawableRes val iconRes: Int) {
    CROP("Crop", R.drawable.ic_crop),
    ADJUST("Adjust", R.drawable.ic_adjust),
    FILTER("Filter", R.drawable.ic_filter),
    BRUSH("Brush", R.drawable.ic_brush),
    STICKER("Sticker", R.drawable.ic_sticker),
    TEXT("Text", R.drawable.ic_text)
}

/**
 * Adjustment parameters matching `adjust_bottom_nav.xml`.
 */
enum class AdjustParam(val label: String, @DrawableRes val iconRes: Int) {
    BRIGHTNESS("Brightness", R.drawable.ic_brightness),
    CONTRAST("Contrast", R.drawable.ic_contrast),
    SATURATION("Saturation", R.drawable.ic_saturation),
    SHARPEN("Sharpen", R.drawable.ic_sharpness)
}

/**
 * Filter presets matching reference APK GPUImage & color presets.
 */
enum class EditorFilter(val label: String) {
    NONE("Original"),
    GRAYSCALE("B&W"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    COOL("Cool"),
    WARM("Warm"),
    VIVID("Vivid"),
    FADE("Fade"),
    CYBERPUNK("Neon"),
    INVERT("Invert")
}

/**
 * Aspect ratio options matching reference APK `item_crop_layout.xml` (ratioList).
 */
enum class CropRatio(val label: String, @DrawableRes val iconRes: Int, val aspectX: Float, val aspectY: Float) {
    FREE("Free", R.drawable.ic_expand, 0f, 0f),
    ONE_ONE("1:1", R.drawable.ic_crop_insta, 1f, 1f),
    FOUR_FIVE("4:5", R.drawable.ic_crop_insta, 4f, 5f),
    NINE_SIXTEEN("Story", R.drawable.ic_crop_insta, 9f, 16f),
    SIXTEEN_NINE("Cover", R.drawable.ic_crop_fb, 16f, 9f),
    FIVE_FOUR("5:4", R.drawable.ic_crop_fb, 5f, 4f),
    FOUR_THREE("4:3", R.drawable.ic_crop_fb, 4f, 3f),
    THREE_FOUR("3:4", R.drawable.ic_youtube_crop, 3f, 4f)
}

/**
 * Drawn path representation for markup / brush.
 */
data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

/**
 * Interactive sticker placed on the canvas.
 */
data class PlacedSticker(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val sticker: String,
    var offset: Offset = Offset(200f, 200f)
)

/**
 * Interactive text placed on the canvas.
 */
data class PlacedText(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val text: String,
    var offset: Offset = Offset(200f, 200f),
    val color: Color = Color.White,
    val fontSize: Float = 24f,
    val opacity: Float = 1.0f
)

// 10 authentic brush palette colors
val BRUSH_COLORS = listOf(
    Color(0xFFE11A1A), // Red
    Color(0xFFFF9800), // Orange
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF4CAF50), // Green
    Color(0xFF00BCD4), // Cyan
    Color(0xFF247BF7), // Blue
    Color(0xFF9C27B0), // Purple
    Color(0xFFE91E63), // Pink
    Color(0xFFFFFFFF), // White
    Color(0xFF000000)  // Black
)

// Sticker tray emojis
val STICKER_LIST = listOf(
    "✨", "❤️", "⭐", "🎉", "🔥", "🌸", "👑", "👍",
    "🕶️", "🎨", "🌈", "🚀", "🐶", "🍕", "🎯", "💡",
    "😍", "🥳", "💯", "🎈", "🌺", "⚡", "🦋", "☕"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    initialPath: String?,
    initialTab: String? = null,
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    onNavigate: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf(initialPath) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Active sub-tool panel (null means root 6-tab strip)
    var activeTab by remember {
        mutableStateOf(
            when (initialTab?.lowercase()) {
                "crop" -> EditorTab.CROP
                "adjust" -> EditorTab.ADJUST
                "filter" -> EditorTab.FILTER
                "brush", "markup" -> EditorTab.BRUSH
                "sticker" -> EditorTab.STICKER
                "text" -> EditorTab.TEXT
                else -> null
            }
        )
    }

    // Transformations
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var flipX by remember { mutableStateOf(false) }
    var flipY by remember { mutableStateOf(false) }

    // Temporary Crop Mode State (isolated until confirmed with Done)
    var cropRotation by remember { mutableFloatStateOf(0f) }
    var cropFlipX by remember { mutableStateOf(false) }
    var cropFlipY by remember { mutableStateOf(false) }
    var selectedCropRatio by remember { mutableStateOf(CropRatio.FREE) }
    var latestCropRect by remember { mutableStateOf<Rect?>(null) }
    var latestCropImageBounds by remember { mutableStateOf<Rect?>(null) }

    // Adjustments
    var selectedAdjust by remember { mutableStateOf(AdjustParam.BRIGHTNESS) }
    var brightnessValue by remember { mutableFloatStateOf(0f) }
    var contrastValue by remember { mutableFloatStateOf(0f) }
    var saturationValue by remember { mutableFloatStateOf(0f) }
    var sharpenValue by remember { mutableFloatStateOf(0f) }

    // Filter
    var selectedFilter by remember { mutableStateOf(EditorFilter.NONE) }
    var filterBeforePanel by remember { mutableStateOf(EditorFilter.NONE) }
    LaunchedEffect(activeTab) { if (activeTab == EditorTab.FILTER) filterBeforePanel = selectedFilter }

    // Brush
    var brushStrokeWidth by remember { mutableFloatStateOf(15f) }
    var brushColor by remember { mutableStateOf(Color(0xFFE11A1A)) }
    var drawnPaths by remember { mutableStateOf<List<DrawnPath>>(emptyList()) }
    var undoStack by remember { mutableStateOf<List<DrawnPath>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<DrawnPath>>(emptyList()) }
    var currentDrawingPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Stickers
    var placedStickers by remember { mutableStateOf<List<PlacedSticker>>(emptyList()) }

    // Text
    var placedTexts by remember { mutableStateOf<List<PlacedText>>(emptyList()) }
    var showTextDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var inputTextColor by remember { mutableStateOf(Color.White) }

    // Undo/redo of the whole edit (image, rotation, flips, filter, adjustments, drawings, stickers, text)
    val history = remember { EditHistory<EditSnapshot>() }
    fun snapshot(): EditSnapshot? = originalBitmap?.let {
        EditSnapshot(
            it, rotationDegrees, flipX, flipY, selectedFilter,
            brightnessValue, contrastValue, saturationValue, sharpenValue,
            drawnPaths, placedStickers, placedTexts
        )
    }
    fun restore(e: EditSnapshot) {
        originalBitmap = e.bitmap
        rotationDegrees = e.rotation; flipX = e.flipX; flipY = e.flipY
        selectedFilter = e.filter
        brightnessValue = e.brightness; contrastValue = e.contrast
        saturationValue = e.saturation; sharpenValue = e.sharpen
        drawnPaths = e.paths; undoStack = e.paths; redoStack = emptyList()
        placedStickers = e.stickers; placedTexts = e.texts
    }
    // Record a step each time a tool panel closes (and for the first loaded image)
    LaunchedEffect(activeTab, originalBitmap) {
        if (activeTab == null) snapshot()?.let { history.record(it) }
    }

    // Save state
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(currentPath) {
        if (!currentPath.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                val file = File(currentPath!!)
                if (file.exists()) {
                    originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            }
        }
    }

    // Reference opens an in-app photo picker (InternalSelectMediaActivity) when no photo is given.
    if (currentPath.isNullOrEmpty()) {
        EditorPhotoPicker(mediaRepository, onNavigateBack) { currentPath = it }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.screenBackground,
        topBar = {
            // Authentic Top Toolbar (matches layout_toolbar.xml & activity_photo_edit.xml)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
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
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = Color.Unspecified
                            )
                        }
                        Text(
                            text = stringResource(R.string.edit),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                        if (activeTab == null) {
                            IconButton(onClick = { history.undo()?.let { restore(it) } }, enabled = history.canUndo) {
                                Icon(
                                    painterResource(R.drawable.ic_undo),
                                    contentDescription = "Undo",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (history.canUndo) 1f else 0.3f)
                                )
                            }
                            IconButton(onClick = { history.redo()?.let { restore(it) } }, enabled = history.canRedo) {
                                Icon(
                                    painterResource(R.drawable.ic_redo),
                                    contentDescription = "Redo",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (history.canRedo) 1f else 0.3f)
                                )
                            }
                        }
                        // Save Button (Matches save_btn_bg: blue pill with white text)
                        Button(
                            onClick = {
                                if (originalBitmap != null && !isSaving) {
                                    scope.launch {
                                        isSaving = true
                                        val saved = saveEditedBitmap(
                                            context = context,
                                            original = originalBitmap!!,
                                            rotation = rotationDegrees,
                                            flipX = flipX,
                                            flipY = flipY,
                                            filter = selectedFilter,
                                            brightness = brightnessValue,
                                            contrast = contrastValue,
                                            saturation = saturationValue,
                                            sharpen = sharpenValue,
                                            drawnPaths = drawnPaths,
                                            placedStickers = placedStickers,
                                            placedTexts = placedTexts
                                        )
                                        isSaving = false
                                        if (saved) {
                                            Toast.makeText(context, "Photo saved to Gallery", Toast.LENGTH_SHORT).show()
                                            mediaRepository.syncMediaStore()
                                            onNavigateBack()
                                        } else {
                                            Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving && originalBitmap != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF247BF7),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFB0CFFF),
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(25.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.save),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)
                }
            }
        },
        bottomBar = {
            if (originalBitmap != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                                )
                            )
                    ) {
                        if (activeTab == null) {
                            // Root Bottom Tab Bar (Matches rvTab in activity_photo_edit.xml)
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 6 Core tools
                                EditorTab.values().forEach { tab ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { activeTab = tab }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(tab.iconRes),
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tab.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // 3 Background / AI options from reference APK
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (onNavigate != null && currentPath != null) {
                                                onNavigate(Screen.RemoveBg.createRoute(currentPath!!))
                                            } else {
                                                Toast.makeText(context, "Remove Background applied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_remove_background),
                                        contentDescription = "Remove BG",
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Remove BG",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (onNavigate != null && currentPath != null) {
                                                onNavigate(Screen.BlurBg.createRoute(currentPath!!))
                                            } else {
                                                Toast.makeText(context, "Blur Background applied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_blur),
                                        contentDescription = "Blur BG",
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Blur BG",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (onNavigate != null && currentPath != null) {
                                                onNavigate(Screen.ColorSplash.createRoute(currentPath!!))
                                            } else {
                                                Toast.makeText(context, "Color Splash applied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_color_splash_bg),
                                        contentDescription = "Color BG",
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Color BG",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            // Sub-Tool Panel Layout
                            when (activeTab!!) {
                                EditorTab.CROP -> {
                                    // Crop Panel (Matches item_crop_layout.xml)
                                    CropPanel(
                                        flipX = cropFlipX,
                                        flipY = cropFlipY,
                                        onFlipXToggle = { cropFlipX = !cropFlipX },
                                        onFlipYToggle = { cropFlipY = !cropFlipY },
                                        onRotate90 = { cropRotation = (cropRotation + 90f) % 360f },
                                        selectedRatio = selectedCropRatio,
                                        onRatioSelected = { selectedCropRatio = it },
                                        onClose = {
                                            cropRotation = 0f
                                            cropFlipX = false
                                            cropFlipY = false
                                            selectedCropRatio = CropRatio.FREE
                                            activeTab = null
                                        },
                                        onDone = {
                                            if (originalBitmap != null && latestCropRect != null && latestCropImageBounds != null) {
                                                val cropped = CropGeometry.cropBitmap(
                                                    source = originalBitmap!!,
                                                    rotationDegrees = cropRotation,
                                                    flipX = cropFlipX,
                                                    flipY = cropFlipY,
                                                    cropRect = latestCropRect!!,
                                                    imageBounds = latestCropImageBounds!!
                                                )
                                                originalBitmap = cropped
                                                cropRotation = 0f
                                                cropFlipX = false
                                                cropFlipY = false
                                                selectedCropRatio = CropRatio.FREE
                                                activeTab = null
                                                Toast.makeText(context, "Image cropped", Toast.LENGTH_SHORT).show()
                                            } else {
                                                activeTab = null
                                            }
                                        }
                                    )
                                }

                                EditorTab.ADJUST -> {
                                    // Adjust Panel (Matches item_adjust_layout.xml)
                                    AdjustPanel(
                                        selectedParam = selectedAdjust,
                                        onParamSelected = { selectedAdjust = it },
                                        brightness = brightnessValue,
                                        onBrightnessChange = { brightnessValue = it },
                                        contrast = contrastValue,
                                        onContrastChange = { contrastValue = it },
                                        saturation = saturationValue,
                                        onSaturationChange = { saturationValue = it },
                                        sharpen = sharpenValue,
                                        onSharpenChange = { sharpenValue = it },
                                        onClose = { activeTab = null },
                                        onDone = { activeTab = null }
                                    )
                                }

                                EditorTab.FILTER -> {
                                    // Filter Panel (Matches item_filter_layout.xml)
                                    FilterPanel(
                                        source = originalBitmap,
                                        selectedFilter = selectedFilter,
                                        onFilterSelected = { selectedFilter = it },
                                        // Cancel keeps the filter you had before opening the panel
                                        onClose = { selectedFilter = filterBeforePanel; activeTab = null },
                                        onDone = { activeTab = null }
                                    )
                                }

                                EditorTab.BRUSH -> {
                                    // Brush Panel (Matches item_brush_layout.xml)
                                    BrushPanel(
                                        strokeWidth = brushStrokeWidth,
                                        onStrokeWidthChange = { brushStrokeWidth = it },
                                        selectedColor = brushColor,
                                        onColorSelected = { brushColor = it },
                                        canUndo = undoStack.isNotEmpty(),
                                        canRedo = redoStack.isNotEmpty(),
                                        onUndo = {
                                            if (undoStack.isNotEmpty()) {
                                                val last = undoStack.last()
                                                undoStack = undoStack.dropLast(1)
                                                redoStack = redoStack + last
                                                drawnPaths = undoStack
                                            }
                                        },
                                        onRedo = {
                                            if (redoStack.isNotEmpty()) {
                                                val next = redoStack.last()
                                                redoStack = redoStack.dropLast(1)
                                                undoStack = undoStack + next
                                                drawnPaths = undoStack
                                            }
                                        },
                                        onClose = {
                                            // Discard current brush session changes if closed
                                            activeTab = null
                                        },
                                        onDone = { activeTab = null }
                                    )
                                }

                                EditorTab.STICKER -> {
                                    // Sticker Panel (Matches item_sticker_layout.xml)
                                    StickerPanel(
                                        onStickerSelected = { emoji ->
                                            placedStickers = placedStickers + PlacedSticker(sticker = emoji)
                                        },
                                        onClose = { activeTab = null },
                                        onDone = { activeTab = null }
                                    )
                                }

                                EditorTab.TEXT -> {
                                    // Text Panel (Matches item_text_layout.xml)
                                    TextPanel(
                                        onAddTextClick = {
                                            inputText = ""
                                            showTextDialog = true
                                        },
                                        onClose = { activeTab = null },
                                        onDone = { activeTab = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // Main Editing Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.screenBackground),
            contentAlignment = Alignment.Center
        ) {
            if (originalBitmap != null) {
                if (activeTab == EditorTab.CROP) {
                    // Interactive Crop View (Matches CanHub CropImageView and reference APK item_crop_layout)
                    ImageCropView(
                        bitmap = originalBitmap!!,
                        rotationDegrees = cropRotation,
                        flipX = cropFlipX,
                        flipY = cropFlipY,
                        selectedRatio = selectedCropRatio,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onCropRectChanged = { rect, bounds ->
                            latestCropRect = rect
                            latestCropImageBounds = bounds
                        }
                    )
                } else {
                    // Dynamically transformed & filtered preview bitmap
                    val previewBitmap = remember(
                        originalBitmap,
                        rotationDegrees,
                        flipX,
                        flipY,
                        selectedFilter,
                        brightnessValue,
                        contrastValue,
                        saturationValue,
                        sharpenValue
                    ) {
                        applyAllTransformations(
                            src = originalBitmap!!,
                            rotation = rotationDegrees,
                            flipX = flipX,
                            flipY = flipY,
                            filter = selectedFilter,
                            brightness = brightnessValue,
                            contrast = contrastValue,
                            saturation = saturationValue,
                            sharpen = sharpenValue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Image display
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Edited Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Brush drawing layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeTab, brushStrokeWidth, brushColor) {
                                    if (activeTab == EditorTab.BRUSH) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentDrawingPath = listOf(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentDrawingPath = currentDrawingPath + change.position
                                            },
                                            onDragEnd = {
                                                if (currentDrawingPath.isNotEmpty()) {
                                                    val newPath = DrawnPath(
                                                        points = currentDrawingPath,
                                                        color = brushColor,
                                                        strokeWidth = brushStrokeWidth
                                                    )
                                                    drawnPaths = drawnPaths + newPath
                                                    undoStack = undoStack + newPath
                                                    redoStack = emptyList()
                                                    currentDrawingPath = emptyList()
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            // Render confirmed paths
                            drawnPaths.forEach { path ->
                                if (path.points.size > 1) {
                                    drawPoints(
                                        points = path.points,
                                        pointMode = PointMode.Polygon,
                                        color = path.color,
                                        strokeWidth = path.strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            // Render in-progress path
                            if (currentDrawingPath.size > 1) {
                                drawPoints(
                                    points = currentDrawingPath,
                                    pointMode = PointMode.Polygon,
                                    color = brushColor,
                                    strokeWidth = brushStrokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Interactive Stickers layer
                        placedStickers.forEach { placedSticker ->
                            var currentOffset by remember { mutableStateOf(placedSticker.offset) }
                            Text(
                                text = placedSticker.sticker,
                                fontSize = 36.sp,
                                modifier = Modifier
                                    .offset(x = (currentOffset.x / 3).dp, y = (currentOffset.y / 3).dp)
                                    .pointerInput(placedSticker.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            currentOffset += dragAmount
                                            placedSticker.offset = currentOffset
                                        }
                                    }
                            )
                        }

                        // Interactive Text overlay layer
                        placedTexts.forEach { placedText ->
                            var textOffset by remember { mutableStateOf(placedText.offset) }
                            Text(
                                text = placedText.text,
                                fontSize = placedText.fontSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = placedText.color.copy(alpha = placedText.opacity),
                                modifier = Modifier
                                    .offset(x = (textOffset.x / 3).dp, y = (textOffset.y / 3).dp)
                                    .pointerInput(placedText.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            textOffset += dragAmount
                                            placedText.offset = textOffset
                                        }
                                    }
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = GalleryBlue)
            }
        }
    }

    // Add Text Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = {
                Text(
                    text = "Add Text Caption",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Enter text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Text Color:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(Color.White, Color.Black, Color(0xFFE11A1A), Color(0xFF247BF7), Color(0xFFFFEB3B), Color(0xFF4CAF50)).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (inputTextColor == col) 2.dp else 1.dp,
                                        color = if (inputTextColor == col) GalleryBlue else MaterialTheme.colorScheme.settingsDivider,
                                        shape = CircleShape
                                    )
                                    .clickable { inputTextColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            placedTexts = placedTexts + PlacedText(
                                text = inputText,
                                color = inputTextColor
                            )
                        }
                        showTextDialog = false
                    }
                ) {
                    Text("Add", color = GalleryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Sub-panel for Crop tool (Matches item_crop_layout.xml).
 */
@Composable
private fun CropPanel(
    flipX: Boolean,
    flipY: Boolean,
    onFlipXToggle: () -> Unit,
    onFlipYToggle: () -> Unit,
    onRotate90: () -> Unit,
    selectedRatio: CropRatio,
    onRatioSelected: (CropRatio) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sub-tool Header: Cancel, Title, Done
        SubToolHeader(
            title = stringResource(R.string.crop),
            onClose = onClose,
            onDone = onDone
        )

        // Transform Row: Flip X, Flip Y, Rotate 90
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flip X
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFlipXToggle() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_vertical),
                    contentDescription = "Flip X",
                    colorFilter = ColorFilter.tint(if (flipX) GalleryBlue else MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.flip_X),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (flipX) GalleryBlue else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Flip Y
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFlipYToggle() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_horizon),
                    contentDescription = "Flip Y",
                    colorFilter = ColorFilter.tint(if (flipY) GalleryBlue else MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.flip_Y),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (flipY) GalleryBlue else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Rotate 90
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRotate90() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_rotate),
                    contentDescription = "Rotate 90",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.rotate_90),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Aspect Ratios Row (Matches ratio_recycler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CropRatio.values().forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) (if (MaterialTheme.colorScheme.isDark) Color(0xFF1E2D42) else Color(0xFFF0F9FF)) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) GalleryBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onRatioSelected(ratio) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Image(
                        painter = painterResource(ratio.iconRes),
                        contentDescription = ratio.label,
                        colorFilter = ColorFilter.tint(if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ratio.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Sub-panel for Adjust tool (Matches item_adjust_layout.xml).
 */
@Composable
private fun AdjustPanel(
    selectedParam: AdjustParam,
    onParamSelected: (AdjustParam) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    sharpen: Float,
    onSharpenChange: (Float) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    val currentValue = when (selectedParam) {
        AdjustParam.BRIGHTNESS -> brightness
        AdjustParam.CONTRAST -> contrast
        AdjustParam.SATURATION -> saturation
        AdjustParam.SHARPEN -> sharpen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sub-tool Header
        SubToolHeader(
            title = selectedParam.label,
            onClose = onClose,
            onDone = onDone
        )

        // Adjustment Value Text (Matches adjustRulerText)
        Text(
            text = "${if (currentValue > 0) "+" else ""}${currentValue.toInt()}",
            color = GalleryBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Slider / Ruler (Matches adjustRuler)
        Slider(
            value = currentValue,
            onValueChange = { newVal ->
                when (selectedParam) {
                    AdjustParam.BRIGHTNESS -> onBrightnessChange(newVal)
                    AdjustParam.CONTRAST -> onContrastChange(newVal)
                    AdjustParam.SATURATION -> onSaturationChange(newVal)
                    AdjustParam.SHARPEN -> onSharpenChange(newVal)
                }
            },
            valueRange = if (selectedParam == AdjustParam.SHARPEN) 0f..100f else -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = GalleryBlue,
                activeTrackColor = GalleryBlue,
                inactiveTrackColor = MaterialTheme.colorScheme.cardBorder
            ),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)

        // Bottom sub-navigation bar (Matches adjust_bottom_navigation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdjustParam.values().forEach { param ->
                val isSelected = selectedParam == param
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onParamSelected(param) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Image(
                        painter = painterResource(param.iconRes),
                        contentDescription = param.label,
                        colorFilter = ColorFilter.tint(if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = param.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Sub-panel for Filter tool (Matches item_filter_layout.xml).
 */
@Composable
private fun FilterPanel(
    source: Bitmap?,
    selectedFilter: EditorFilter,
    onFilterSelected: (EditorFilter) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    val thumb = remember(source) {
        source?.let {
            val scale = 160f / maxOf(it.width, it.height)
            Bitmap.createScaledBitmap(it, (it.width * scale).toInt().coerceAtLeast(1), (it.height * scale).toInt().coerceAtLeast(1), true)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sub-tool Header
        SubToolHeader(
            title = stringResource(R.string.filters),
            onClose = onClose,
            onDone = onDone
        )

        // Filter Recycler Row (Matches filterRecycler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onFilterSelected(filter) }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.settingsDivider,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Real preview: the photo's thumbnail with this filter applied
                        val preview = remember(thumb, filter) {
                            thumb?.let { applyAllTransformations(it, 0f, false, false, filter, 0f, 0f, 0f).asImageBitmap() }
                        }
                        if (preview != null) {
                            Image(bitmap = preview, contentDescription = filter.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filter.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Sub-panel for Brush / Markup tool (Matches item_brush_layout.xml).
 */
@Composable
private fun BrushPanel(
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sub-tool Header: Cancel, Undo, Title "Brush", Redo, Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Image(
                    painter = painterResource(R.drawable.ic_cancle),
                    contentDescription = "Cancel",
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onUndo, enabled = canUndo) {
                Image(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = "Undo",
                    colorFilter = ColorFilter.tint(if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.textTertiary),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.brush),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onRedo, enabled = canRedo) {
                Image(
                    painter = painterResource(R.drawable.ic_redo),
                    contentDescription = "Redo",
                    colorFilter = ColorFilter.tint(if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.textTertiary),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDone) {
                Image(
                    painter = painterResource(R.drawable.ic_done),
                    contentDescription = "Done",
                    colorFilter = ColorFilter.tint(GalleryBlue),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Brush Stroke Size Row (Matches brushMinSize, brushSizeSeekbar, brushMaxSize)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (MaterialTheme.colorScheme.isDark) Color(0xFF1E2D42) else Color(0xFFF0F9FF))
                    .border(1.5.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFF2A3D58) else Color(0xFFDEF2FF), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GalleryBlue
                )
            }

            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = GalleryBlue,
                    activeTrackColor = GalleryBlue,
                    inactiveTrackColor = MaterialTheme.colorScheme.cardBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (MaterialTheme.colorScheme.isDark) Color(0xFF1E2D42) else Color(0xFFF0F9FF))
                    .border(1.5.dp, if (MaterialTheme.colorScheme.isDark) Color(0xFF2A3D58) else Color(0xFFDEF2FF), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+100",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GalleryBlue
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)

        // Color Palette Row (Matches brushColorRv)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_picker),
                contentDescription = "Color Picker",
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 4.dp)
            )

            BRUSH_COLORS.forEach { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.settingsDivider,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Sub-panel for Sticker tool (Matches item_sticker_layout.xml).
 */
@Composable
private fun StickerPanel(
    onStickerSelected: (String) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubToolHeader(
            title = stringResource(R.string.sticker),
            onClose = onClose,
            onDone = onDone
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            STICKER_LIST.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStickerSelected(emoji) }
                        .padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Sub-panel for Text tool (Matches item_text_layout.xml).
 */
@Composable
private fun TextPanel(
    onAddTextClick: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubToolHeader(
            title = stringResource(R.string.text),
            onClose = onClose,
            onDone = onDone
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddTextClick,
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Add Text", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Common Sub-Tool Header with Cancel (`ic_cancle`), Title, and Done (`ic_done`).
 */
@Composable
private fun SubToolHeader(
    title: String,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Image(
                painter = painterResource(R.drawable.ic_cancle),
                contentDescription = "Cancel",
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onDone) {
            Image(
                painter = painterResource(R.drawable.ic_done),
                contentDescription = "Done",
                colorFilter = ColorFilter.tint(GalleryBlue),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Transforms and filters a bitmap with high accuracy.
 */
private fun applyAllTransformations(
    src: Bitmap,
    rotation: Float,
    flipX: Boolean,
    flipY: Boolean,
    filter: EditorFilter,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpen: Float = 0f
): Bitmap {
    val matrix = Matrix().apply {
        if (flipX) postScale(-1f, 1f)
        if (flipY) postScale(1f, -1f)
        if (rotation != 0f) postRotate(rotation)
    }
    val transformed = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    val result = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    val paint = Paint()

    val cm = ColorMatrix().apply {
        when (filter) {
            EditorFilter.NONE -> {}
            EditorFilter.GRAYSCALE -> setSaturation(0f)
            EditorFilter.SEPIA -> {
                set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.VINTAGE -> {
                set(floatArrayOf(
                    0.9f, 0.1f, 0.1f, 0f, 10f,
                    0.1f, 0.8f, 0.1f, 0f, 10f,
                    0.1f, 0.1f, 0.7f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.COOL -> {
                set(floatArrayOf(
                    0.8f, 0f, 0f, 0f, 0f,
                    0f, 0.9f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.WARM -> {
                set(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 20f,
                    0f, 1.0f, 0f, 0f, 10f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.VIVID -> {
                val satMatrix = ColorMatrix().apply { setSaturation(1.4f) }
                postConcat(satMatrix)
            }
            EditorFilter.FADE -> {
                set(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 25f,
                    0f, 0.9f, 0f, 0f, 25f,
                    0f, 0f, 0.9f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.INVERT -> {
                set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            EditorFilter.CYBERPUNK -> {
                set(floatArrayOf(
                    1.2f, 0f, 0.5f, 0f, 10f,
                    0f, 0.8f, 0.8f, 0f, 0f,
                    0.5f, 0f, 1.4f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }

        if (brightness != 0f || contrast != 0f || saturation != 0f) {
            val adjCm = ColorMatrix()
            val scale = 1f + (contrast / 100f)
            // Contrast pivots on mid-grey (128) so it doesn't also brighten
            val translate = brightness * 1.5f + (1f - scale) * 128f
            adjCm.set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            if (saturation != 0f) {
                val satCm = ColorMatrix().apply { setSaturation(1f + (saturation / 100f)) }
                adjCm.postConcat(satCm)
            }
            postConcat(adjCm)
        }
    }

    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(transformed, 0f, 0f, paint)
    return ImageSharpen.sharpen(result, sharpen)
}

/**
 * Saves composite of transformed base image + markup + stickers + text to MediaStore.
 */
private suspend fun saveEditedBitmap(
    context: Context,
    original: Bitmap,
    rotation: Float,
    flipX: Boolean,
    flipY: Boolean,
    filter: EditorFilter,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpen: Float,
    drawnPaths: List<DrawnPath>,
    placedStickers: List<PlacedSticker>,
    placedTexts: List<PlacedText>
): Boolean = withContext(Dispatchers.IO) {
    try {
        val baseFiltered = applyAllTransformations(
            src = original,
            rotation = rotation,
            flipX = flipX,
            flipY = flipY,
            filter = filter,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            sharpen = sharpen
        )

        val finalBitmap = Bitmap.createBitmap(baseFiltered.width, baseFiltered.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(finalBitmap)
        canvas.drawBitmap(baseFiltered, 0f, 0f, null)

        // Draw brush markup
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val scaleX = baseFiltered.width.toFloat() / 1080f
        val scaleY = baseFiltered.height.toFloat() / 1920f
        val avgScale = (scaleX + scaleY) / 2f

        drawnPaths.forEach { path ->
            if (path.points.size > 1) {
                paint.color = path.color.toArgb()
                paint.strokeWidth = path.strokeWidth * avgScale
                for (i in 0 until path.points.size - 1) {
                    val p1 = path.points[i]
                    val p2 = path.points[i + 1]
                    canvas.drawLine(p1.x * scaleX, p1.y * scaleY, p2.x * scaleX, p2.y * scaleY, paint)
                }
            }
        }

        // Draw placed stickers
        val stickerPaint = Paint().apply {
            textSize = 64f * avgScale
            isAntiAlias = true
        }
        placedStickers.forEach { sticker ->
            canvas.drawText(sticker.sticker, sticker.offset.x * scaleX, sticker.offset.y * scaleY, stickerPaint)
        }

        // Draw placed texts
        placedTexts.forEach { textItem ->
            val textPaint = Paint().apply {
                color = textItem.color.toArgb()
                textSize = textItem.fontSize * 2f * avgScale
                isAntiAlias = true
                alpha = (textItem.opacity * 255).toInt()
            }
            canvas.drawText(textItem.text, textItem.offset.x * scaleX, textItem.offset.y * scaleY, textPaint)
        }

        val filename = "Edited_${System.currentTimeMillis()}.jpg"

        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Gallery")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { resolver.openOutputStream(it) }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val galleryDir = File(picturesDir, "Gallery").also { if (!it.exists()) it.mkdirs() }
            val file = File(galleryDir, filename)
            FileOutputStream(file)
        }

        outputStream?.use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        true
    } catch (_: Exception) {
        false
    }
}


@Composable
private fun EditorPhotoPicker(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    onPick: (String) -> Unit
) {
    val media by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    val photos = remember(media) { media.filter { it.mediaType != com.quizango.gallery.domain.model.MediaType.VIDEO } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "All Photos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos.size, key = { photos[it].id }) { i ->
                    coil.compose.AsyncImage(
                        model = photos[i].contentUri,
                        contentDescription = photos[i].fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .clickable { onPick(photos[i].filePath) }
                    )
                }
            }
        }
    }
}


/** One undoable editor state. Bitmaps are never mutated, so keeping references is safe. */
data class EditSnapshot(
    val bitmap: Bitmap,
    val rotation: Float,
    val flipX: Boolean,
    val flipY: Boolean,
    val filter: EditorFilter,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val sharpen: Float,
    val paths: List<DrawnPath>,
    val stickers: List<PlacedSticker>,
    val texts: List<PlacedText>
)
