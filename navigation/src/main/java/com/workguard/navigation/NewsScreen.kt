package com.workguard.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

@Composable
fun NewsScreen(
    state: NewsState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (NewsCategoryUi) -> Unit,
    onRetry: () -> Unit,
    onItemClick: (NewsItemUi) -> Unit,
    onFeaturedClick: (NewsItemUi) -> Unit
) {
    val headerFont = FontFamily.Serif
    val gradient = Brush.verticalGradient(
        colors = listOf(UiTokens.Soft, UiTokens.Bg)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "News",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = headerFont,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTokens.Text
                    )
                    Text(
                        text = "Kumpulan informasi dan pembaruan terkini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Muted
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari berita atau pengumuman") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = UiTokens.Muted
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = UiTokens.Surface,
                        unfocusedContainerColor = UiTokens.Surface,
                        disabledContainerColor = UiTokens.Surface,
                        focusedIndicatorColor = UiTokens.Accent,
                        unfocusedIndicatorColor = UiTokens.Divider,
                        focusedTextColor = UiTokens.Text,
                        unfocusedTextColor = UiTokens.Text,
                        focusedPlaceholderColor = UiTokens.Muted,
                        unfocusedPlaceholderColor = UiTokens.Muted
                    )
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.categories) { category ->
                        CategoryChip(
                            label = category.name,
                            selected = category == state.selectedCategory,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    ErrorBanner(message = state.errorMessage, onRetry = onRetry)
                }
            }

            item {
                FeaturedNewsCard(
                    item = state.featured,
                    onClick = onFeaturedClick
                )
            }

            item {
                Text(
                    text = "Berita pilihan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = UiTokens.Text
                )
            }

            if (state.isLoading && state.items.isEmpty()) {
                item {
                    LoadingRow()
                }
            } else if (state.items.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(state.items) { article ->
                    NewsItemCard(
                        item = article,
                        onClick = { onItemClick(article) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) UiTokens.Accent else UiTokens.Surface
    val textColor = if (selected) Color.White else UiTokens.Muted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun FeaturedNewsCard(
    item: NewsItemUi?,
    onClick: (NewsItemUi) -> Unit
) {
    val clickableModifier = if (item != null) {
        Modifier.clickable { onClick(item) }
    } else {
        Modifier
    }
    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            NewsCoverImage(
                coverUrl = item?.coverUrl,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0B1F24))
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item?.category ?: "Highlight",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item?.readTime?.takeIf { it.isNotBlank() } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item?.title ?: "Belum ada highlight hari ini",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item?.summary?.takeIf { it.isNotBlank() }
                        ?: "Tetap pantau informasi terbaru dari perusahaan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item?.time.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item?.time.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsItemCard(
    item: NewsItemUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NewsThumbnail(
                coverUrl = item.coverUrl,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = UiTokens.AccentDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(UiTokens.Soft)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = UiTokens.Muted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (item.readTime.isNotBlank()) {
                        Text(
                            text = item.readTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = UiTokens.Muted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = UiTokens.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewsCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier
) {
    NewsImage(
        coverUrl = coverUrl,
        modifier = modifier,
        placeholderBrush = Brush.linearGradient(colors = listOf(UiTokens.Accent, UiTokens.Blue)),
        placeholderIconTint = Color.White
    )
}

@Composable
private fun NewsThumbnail(
    coverUrl: String?,
    modifier: Modifier = Modifier
) {
    NewsImage(
        coverUrl = coverUrl,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        placeholderBrush = Brush.linearGradient(colors = listOf(UiTokens.Soft, UiTokens.Bg)),
        placeholderIconTint = UiTokens.Muted
    )
}

@Composable
private fun NewsImage(
    coverUrl: String?,
    modifier: Modifier,
    placeholderBrush: Brush,
    placeholderIconTint: Color
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.background(placeholderBrush),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = placeholderIconTint,
                modifier = Modifier.size(20.dp)
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = placeholderIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Memuat berita...",
            style = MaterialTheme.typography.bodySmall,
            color = UiTokens.Muted
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Belum ada berita untuk ditampilkan",
            style = MaterialTheme.typography.bodySmall,
            color = UiTokens.Muted
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Soft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.Text,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(text = "Coba lagi")
            }
        }
    }
}
