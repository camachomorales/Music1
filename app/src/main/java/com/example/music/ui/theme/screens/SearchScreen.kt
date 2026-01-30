package com.example.music.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    localSearchResults: List<Song>,
    streamingSearchResults: List<StreamingSong>,
    isOnlineMode: Boolean,
    isSearching: Boolean,
    onSongClick: (Song) -> Unit,
    onStreamingSongClick: (StreamingSong) -> Unit,
    // ✅ SEPARAR FAVORITOS: Local vs Streaming
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Song) -> Unit = {},
    isStreamingFavorite: (String) -> Boolean = { false },  // ✅ NUEVO: Para StreamingSong
    onToggleStreamingFavorite: (StreamingSong) -> Unit = {}  // ✅ NUEVO
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Search",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isOnlineMode) "🌐 Searching streaming services" else "📱 Searching local music",
                fontSize = 14.sp,
                color = Color(0xFF00D9FF),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(28.dp)),
            placeholder = {
                Text(
                    if (isOnlineMode)
                        "Search streaming music..."
                    else
                        "Search your local music...",
                    color = Color.White.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = Color(0xFF00D9FF),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color(0xFF00D9FF)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contenido
        if (searchQuery.isEmpty()) {
            // Estado vacío
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RotatingVinyl()

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Search Music",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isOnlineMode)
                        "Find music from streaming services"
                    else
                        "Find your local music",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else if (isSearching) {
            // Cargando
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00D9FF),
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )

                    Text(
                        text = "Searching...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Text(
                        text = if (isOnlineMode)
                            "Searching 3 music sources"
                        else
                            "Searching your library",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Resultados
            val hasResults = if (isOnlineMode)
                streamingSearchResults.isNotEmpty()
            else
                localSearchResults.isNotEmpty()

            if (!hasResults) {
                // Sin resultados
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No results found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Try searching with different keywords",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Lista de resultados
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val count = if (isOnlineMode)
                            streamingSearchResults.size
                        else
                            localSearchResults.size

                        Text(
                            text = "$count results",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (isOnlineMode) {
                        // ✅ STREAMING RESULTS CON FAVORITOS
                        items(streamingSearchResults, key = { it.id }) { streamingSong ->
                            StreamingSearchResultItem(
                                streamingSong = streamingSong,
                                onClick = { onStreamingSongClick(streamingSong) },
                                isFavorite = isStreamingFavorite(streamingSong.id),  // ✅
                                onToggleFavorite = { onToggleStreamingFavorite(streamingSong) }  // ✅
                            )
                        }
                    } else {
                        // ✅ LOCAL RESULTS CON FAVORITOS
                        items(localSearchResults, key = { it.id }) { song ->
                            SearchResultItem(
                                song = song,
                                onClick = { onSongClick(song) },
                                isFavorite = isFavorite(song.id),  // ✅
                                onToggleFavorite = { onToggleFavorite(song) }  // ✅
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotatingVinyl() {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color(0xFF1A3A4A)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B2A3A))
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF051E26)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun StreamingSearchResultItem(
    streamingSong: StreamingSong,
    onClick: () -> Unit,
    // ✅ AGREGAR FAVORITOS
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var clickEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(
                enabled = clickEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (clickEnabled) {
                    clickEnabled = false
                    onClick()
                    scope.launch {
                        delay(500)
                        clickEnabled = true
                    }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Thumbnail con fondo semi-transparente y ícono por defecto
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            // ✅ SIEMPRE mostrar el ícono de fondo
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )

            // ✅ Imagen encima (si existe)
            if (streamingSong.thumbnailUrl != null) {
                AsyncImage(
                    model = streamingSong.thumbnailUrl,
                    contentDescription = streamingSong.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = streamingSong.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = streamingSong.artist,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ✅ BOTÓN DE FAVORITOS
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) Color(0xFFFF006E) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(streamingSong.duration),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SearchResultItem(
    song: Song,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var clickEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(
                enabled = clickEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (clickEnabled) {
                    clickEnabled = false
                    onClick()
                    scope.launch {
                        delay(500)
                        clickEnabled = true
                    }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )

            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ✅ BOTÓN DE FAVORITOS
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) Color(0xFFFF006E) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(song.duration),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}