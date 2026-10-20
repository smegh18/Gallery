package albums.gallery.photo.folder.picasa.app.web.gallery.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class EditorFilter(val label: String) {
    NONE("Original"),
    GRAYSCALE("B&W"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    COOL("Cool"),
    WARM("Warm")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    initialPath: String?,
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf(initialPath) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(EditorFilter.NONE) }
    var isSaving by remember { mutableStateOf(false) }

    // Pick image if no path provided
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
            originalBitmap = bitmap
        }
    }

    LaunchedEffect(currentPath) {
        if (!currentPath.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                val file = File(currentPath!!)
                if (file.exists()) {
                    originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            }
        } else {
            pickerLauncher.launch("image/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (originalBitmap != null) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    val saved = saveEditedBitmap(
                                        context = context,
                                        original = originalBitmap!!,
                                        rotation = rotationDegrees,
                                        filter = selectedFilter
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
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (originalBitmap != null) {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // Filters row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            EditorFilter.values().forEach { filter ->
                                val isSelected = selectedFilter == filter
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedFilter = filter }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) GalleryBlue else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = GalleryBlue,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = filter.label.take(2).uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = filter.label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) GalleryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Editing Tools Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                                    Text("Rotate", fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { rotationDegrees = (rotationDegrees - 90f + 360f) % 360f }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left")
                                    Text("Rotate Left", fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = {
                                rotationDegrees = 0f
                                selectedFilter = EditorFilter.NONE
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                                    Text("Reset", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (originalBitmap != null) {
                // Apply filter and rotation preview
                val previewBitmap = remember(originalBitmap, rotationDegrees, selectedFilter) {
                    applyFilterAndRotation(originalBitmap!!, rotationDegrees, selectedFilter)
                }

                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Edited Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Select an image to edit",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun applyFilterAndRotation(src: Bitmap, rotation: Float, filter: EditorFilter): Bitmap {
    val matrix = Matrix().apply {
        if (rotation != 0f) postRotate(rotation)
    }
    val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    if (filter == EditorFilter.NONE) return rotated

    val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
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
        }
    }
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(rotated, 0f, 0f, paint)
    return result
}

private suspend fun saveEditedBitmap(
    context: Context,
    original: Bitmap,
    rotation: Float,
    filter: EditorFilter
): Boolean = withContext(Dispatchers.IO) {
    try {
        val resultBitmap = applyFilterAndRotation(original, rotation, filter)
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
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        true
    } catch (_: Exception) {
        false
    }
}
