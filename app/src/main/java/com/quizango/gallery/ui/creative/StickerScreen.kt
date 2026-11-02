package com.quizango.gallery.ui.creative

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.ui.theme.GalleryBlue

data class StickerItem(
    val emoji: String,
    val category: String,
    val name: String
)

val SAMPLE_STICKERS = listOf(
    StickerItem("❤️", "Hearts", "Red Heart"),
    StickerItem("💖", "Hearts", "Sparkling Heart"),
    StickerItem("🔥", "Emoji", "Fire"),
    StickerItem("✨", "Emoji", "Sparkles"),
    StickerItem("🎉", "Party", "Party Popper"),
    StickerItem("🥳", "Party", "Party Face"),
    StickerItem("😎", "Emoji", "Cool"),
    StickerItem("🤩", "Emoji", "Star Struck"),
    StickerItem("🎂", "Party", "Cake"),
    StickerItem("👑", "Badges", "Crown"),
    StickerItem("⭐", "Badges", "Star"),
    StickerItem("🌟", "Badges", "Glowing Star"),
    StickerItem("🌸", "Nature", "Cherry Blossom"),
    StickerItem("🌻", "Nature", "Sunflower"),
    StickerItem("🌴", "Travel", "Palm Tree"),
    StickerItem("✈️", "Travel", "Airplane"),
    StickerItem("🏖️", "Summer", "Beach"),
    StickerItem("🕶️", "Summer", "Sunglasses"),
    StickerItem("🍉", "Summer", "Watermelon"),
    StickerItem("🚀", "Travel", "Rocket"),
    StickerItem("🏆", "Badges", "Trophy"),
    StickerItem("💯", "Badges", "Hundred"),
    StickerItem("🌈", "Nature", "Rainbow"),
    StickerItem("🍀", "Nature", "Four Leaf Clover")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerScreen(
    onStickerSelected: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Emoji", "Hearts", "Party", "Badges", "Nature", "Travel", "Summer")

    val filteredStickers = remember(searchQuery, selectedCategory) {
        SAMPLE_STICKERS.filter { item ->
            val matchesCat = if (selectedCategory == "All") true else item.category == selectedCategory
            val matchesQuery = if (searchQuery.isEmpty()) true else item.name.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stickers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search stickers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GalleryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GalleryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Stickers Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredStickers) { sticker ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                onStickerSelected?.invoke(sticker.emoji)
                                Toast.makeText(context, "${sticker.name} selected!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sticker.emoji,
                            fontSize = 36.sp
                        )
                    }
                }
            }
        }
    }
}
