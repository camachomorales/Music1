package com.example.music.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.music.data.model.AppMode
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun HomeScreen(
    songs: List<Song>,
    streamingSongs: List<StreamingSong>,
    appMode: AppMode,
    onSongClick: (Song) -> Unit,
    onStreamingSongClick: (StreamingSong) -> Unit,
    onToggleMode: () -> Unit,
    onSearch: (String) -> Unit,
    searchQuery: String = "",
    isSearching: Boolean = false,
    // ✅ SEPARAR FAVORITOS: Local vs Streaming
    isFavorite: (Long) -> Boolean = { false },
    onToggleFavorite: (Song) -> Unit = {},
    isStreamingFavorite: (String) -> Boolean = { false },  // ✅ NUEVO: Para StreamingSong
    onToggleStreamingFavorite: (StreamingSong) -> Unit = {}  // ✅ NUEVO
) {

    val isOnlineMode = appMode == AppMode.STREAMING
    val greeting = getGreeting()

    // ✅ Determinar si hay resultados de búsqueda activos
    val hasSearchResults = searchQuery.isNotBlank() && streamingSongs.isNotEmpty()


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())

            Column {
                Text(
                    text = greeting,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isOnlineMode) "🌐 Streaming Mode" else "📱 Local Music",
                    fontSize = 14.sp,
                    color = Color(0xFF00D9FF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (isOnlineMode) {
            // ✅ MOSTRAR RESULTADOS DE BÚSQUEDA SI HAY QUERY
            if (hasSearchResults) {
                item {
                    Text(
                        text = "Search Results for \"$searchQuery\"",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "${streamingSongs.size} songs found",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // ✅ Lista de resultados de búsqueda CON FAVORITOS
                items(streamingSongs) { streamingSong ->
                    StreamingSongListItem(
                        streamingSong = streamingSong,
                        onClick = onStreamingSongClick,
                        isFavorite = isStreamingFavorite(streamingSong.id),  // ✅
                        onToggleFavorite = { onToggleStreamingFavorite(streamingSong) }  // ✅
                    )
                }
            }
            // ✅ MOSTRAR LOADING SI ESTÁ BUSCANDO
            else if (isSearching) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF00D9FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching for \"$searchQuery\"...",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            // ✅ MOSTRAR CONTENIDO NORMAL (TRENDING) SOLO SI NO HAY BÚSQUEDA
            else {
                item {
                    Text(
                        text = "Trending Now",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (streamingSongs.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(streamingSongs.take(5)) { streamingSong ->
                                StreamingSongCard(
                                    streamingSong = streamingSong,
                                    onClick = onStreamingSongClick
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00D9FF),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading trending songs...",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Made for you",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listOf("Night Driving", "Weekly Mix", "Top Songs")) { title ->
                            MadeForYouCard(
                                title = title,
                                onClick = { /* Implementar más tarde */ }
                            )
                        }
                    }
                }

                if (streamingSongs.size > 5) {
                    item {
                        Text(
                            text = "All Songs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                    }

                    // ✅ TODOS los streamingSongs CON FAVORITOS
                    items(streamingSongs.drop(5)) { streamingSong ->
                        StreamingSongListItem(
                            streamingSong = streamingSong,
                            onClick = onStreamingSongClick,
                            isFavorite = isStreamingFavorite(streamingSong.id),  // ✅
                            onToggleFavorite = { onToggleStreamingFavorite(streamingSong) }  // ✅
                        )
                    }
                }
            }

        } else {
            // MODO OFFLINE (canciones locales)
            item {
                Text(
                    text = "Recently played",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (songs.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(songs.take(3)) { song ->
                            RecentlyPlayedCard(
                                title = song.title,
                                artist = song.artist,
                                backgroundColor = getRandomColor(),
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No local songs found",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            if (songs.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Music (${songs.size} songs)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                // ✅ Canciones locales CON FAVORITOS
                items(songs.take(50)) { song ->
                    SongItemCard(
                        song = song,
                        onClick = { onSongClick(song) },
                        isFavorite = isFavorite(song.id),  // ✅
                        onToggleFavorite = { onToggleFavorite(song) }  // ✅
                    )
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No music found on your device",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingSongCard(
    streamingSong: StreamingSong,
    onClick: (StreamingSong) -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A3A4A), Color(0xFF051E26))))
            .clickable { onClick(streamingSong) },
        contentAlignment = Alignment.BottomStart
    ) {
        // ✅ Mostrar imagen si existe
        streamingSong.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradiente oscuro en la parte inferior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = streamingSong.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = streamingSong.artist,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StreamingSongListItem(
    streamingSong: StreamingSong,
    onClick: (StreamingSong) -> Unit,
    // ✅ AGREGAR FAVORITOS PARA STREAMING
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
                    onClick(streamingSong)
                    scope.launch {
                        delay(500)
                        clickEnabled = true
                    }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Mostrar thumbnail si existe
        if (streamingSong.thumbnailUrl != null) {
            AsyncImage(
                model = streamingSong.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A4A5A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streamingSong.title.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D9FF)
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
            Text(
                text = "${streamingSong.artist} • ${streamingSong.provider.displayName}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ✅ BOTÓN DE FAVORITOS PARA STREAMING
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

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color(0xFF00D9FF).copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RecentlyPlayedCard(
    title: String,
    artist: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (artist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MadeForYouCard(title: String, onClick: () -> Unit) {
    val gradient = when (title) {
        "Night Driving" -> Brush.verticalGradient(listOf(Color(0xFF1A3A4A), Color(0xFF051E26)))
        "Weekly Mix" -> Brush.verticalGradient(listOf(Color(0xFFFF8A5B), Color(0xFFB85C3E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF2A5A6A), Color(0xFF1A3A4A)))
    }

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SongItemCard(
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
                .background(Color(0xFF1A4A5A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = song.title.take(1).uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D9FF)
            )
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
    }
}

private fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun getRandomColor(): Color {
    val colors = listOf(
        Color(0xFF8B4049), Color(0xFFB89BA8), Color(0xFF6B8B9B),
        Color(0xFF4A7B8C), Color(0xFF5A8B6A), Color(0xFF8B6B4A)
    )
    return colors.random()
}